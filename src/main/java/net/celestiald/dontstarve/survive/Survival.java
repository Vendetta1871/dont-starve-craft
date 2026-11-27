package net.celestiald.dontstarve.survive;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.celestiald.dontstarve.DSRefs;
import net.celestiald.dontstarve.DSScale;
import net.celestiald.dontstarve.DontStarveCraft;
import net.celestiald.dontstarve.clock.DSClock;
import net.celestiald.dontstarve.clock.RunClock;
import net.celestiald.dontstarve.init.DSAttachments;
import net.celestiald.dontstarve.item.DSFoodItem;
import net.celestiald.dontstarve.network.StatsPayload;
import net.celestiald.dontstarve.world.IslandState;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * DS survival stats for members of the current run on the islands, with Wilson's
 * DST numbers (tuning.lua, components/hunger.lua, sanity.lua, grue.lua):
 *
 * <ul>
 *   <li>Hunger burns WILSON_HUNGER_RATE 75/480 per DS s; at 0 the player starves
 *       for WILSON_HEALTH / STARVE_KILL_TIME = 1.25 health per DS s.</li>
 *   <li>Sanity: no change by day (SANITY_DAY_GAIN 0); at dusk and night by light —
 *       dark -0.833/s, otherwise -0.083/s; plus monster auras falling off with
 *       distance squared within 10 blocks, and the ghost drain of dead teammates.</li>
 *   <li>The grue (Charlie) hunts a player in darkness: first hit after 5-10 s, then
 *       every 5-11 s, 100 health and -20 sanity per hit (real seconds).</li>
 *   <li>Temperature (temperature.lua) moves towards the world temperature at up to
 *       1 degree per DS s (5 when thawing from below 0); below 0 the player freezes
 *       for WILSON_HEALTH / FREEZING_KILL_TIME = 1.25 health per DS s, above 70 overheats.</li>
 *   <li>Vanilla hunger is held still on the islands; only DS foods can be eaten.</li>
 * </ul>
 * World-time rates are per DS second; one Minecraft tick is 1/50 DS s.
 */
public final class Survival {

    private static final float DS_SECONDS_PER_TICK = 1.0F / (20.0F * DSScale.TIME);
    private static final float HUNGER_RATE = 75.0F / 480.0F;
    private static final float STARVE_HEALTH_PER_SECOND = 150.0F / 120.0F;
    private static final float SANITY_NIGHT_DARK = -100.0F / (30 * 4);
    private static final float SANITY_NIGHT_DIM = -100.0F / (30 * 40);
    private static final float SANITY_GHOST_PLAYER_DRAIN = -100.0F / (30 * 60);
    private static final int MAX_GHOST_DRAIN_MULT = 3;
    private static final double SANITY_EFFECT_RANGE = 10.0;
    private static final float GRUE_DAMAGE = 150.0F * 0.667F;
    private static final float GRUE_SANITY = 20.0F;
    private static final float FREEZE_HEALTH_PER_SECOND = 150.0F / 120.0F;
    private static final float WARM_DEGREES_PER_SEC = 1.0F;
    private static final float THAW_DEGREES_PER_SEC = 5.0F;

    public static final ResourceKey<DamageType> DARKNESS =
            ResourceKey.create(Registries.DAMAGE_TYPE, DontStarveCraft.rl("darkness"));

    /** Per-player grue state (components/grue.lua): null level = not hunting. */
    private static final class Grue {
        boolean inLight = true;
        Integer level;
        float nextHit;
        float nextSound;
    }

    private static final Map<UUID, Grue> GRUES = new HashMap<>();
    private static final Map<UUID, Float> STARVE_DEBT = new HashMap<>();
    private static final Map<UUID, Float> TEMPERATURE_DEBT = new HashMap<>();

    public static DSStats stats(Player player) {
        return player.getData(DSAttachments.STATS.get());
    }

    public static void set(Player player, DSStats stats) {
        player.setData(DSAttachments.STATS.get(), stats);
    }

    /** A new run: full stats, full health, like a freshly spawned DST character. */
    public static void freshStart(ServerPlayer player) {
        set(player, DSStats.FULL);
        player.setHealth(player.getMaxHealth());
        GRUES.remove(player.getUUID());
        STARVE_DEBT.remove(player.getUUID());
        TEMPERATURE_DEBT.remove(player.getUUID());
    }

    /** Becoming a ghost (player_common_extensions.lua): sanity to 50 %, hunger to 2/3. */
    public static void onBecomeGhost(ServerPlayer player) {
        set(player, stats(player).withHunger(DSStats.MAX_HUNGER * 2 / 3).withSanity(DSStats.MAX_SANITY * 0.5F)
                .withTemperature(DSStats.STARTING_TEMP));
        GRUES.remove(player.getUUID());
    }

    private static boolean simulated(ServerPlayer player, IslandState island) {
        return island.isCurrentRun(player) && !GhostState.isGhost(player) && player.isAlive()
                && !player.isCreative() && !player.isSpectator();
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel islands = server.getLevel(DSRefs.ISLANDS);
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (islands == null || overworld == null) {
            return;
        }
        IslandState island = IslandState.get(overworld);
        DSClock.State clock = RunClock.state(server);
        List<ServerPlayer> crew = islands.players().stream().filter(island::isCurrentRun).toList();
        int ghosts = (int) crew.stream().filter(GhostState::isGhost).count();
        int alive = crew.size() - ghosts;
        boolean sendStats = server.getTickCount() % 5 == 0;
        for (ServerPlayer player : islands.players()) {
            holdVanillaHunger(player.getFoodData());
            if (!simulated(player, island)) {
                if (!player.isAlive() || GhostState.isGhost(player)) {
                    GRUES.remove(player.getUUID());
                }
                if (sendStats) {
                    send(player, 0.0F);
                }
                continue;
            }
            DSStats stats = stats(player);
            float dt = DS_SECONDS_PER_TICK;

            // Hunger, then starvation once empty (hunger.lua DoDec).
            if (stats.hunger() > 0.0F) {
                stats = stats.withHunger(stats.hunger() - HUNGER_RATE * dt);
            } else {
                hurtOverTime(player, STARVE_DEBT, STARVE_HEALTH_PER_SECOND * dt, player.damageSources().starve());
            }

            // Sanity (sanity.lua Recalc).
            float light = DSLight.value(player);
            float lightDelta = 0.0F;
            if (!clock.isDay()) {
                lightDelta = light < DSLight.SANITY_LOW_LIGHT ? SANITY_NIGHT_DARK : SANITY_NIGHT_DIM;
            }
            float auraDelta = aura(player);
            float ghostDelta = 0.0F;
            if (ghosts > 0) {
                float groupResist = alive > ghosts ? 1.0F - ghosts / (float) alive : 0.0F;
                ghostDelta = SANITY_GHOST_PLAYER_DRAIN * Math.min(ghosts, MAX_GHOST_DRAIN_MULT)
                        * (1.0F - groupResist * groupResist);
            }
            float rate = lightDelta + auraDelta + ghostDelta;
            stats = stats.withSanity(stats.sanity() + rate * dt);

            // Temperature (temperature.lua OnUpdate; no heaters or insulation yet).
            float ambient = (float) net.celestiald.dontstarve.clock.WorldTemperature.at(clock, island.islandIndex());
            stats = stats.withTemperature(stats.temperature() + temperatureRate(stats.temperature(), ambient) * dt);
            set(player, stats);
            if (stats.temperature() < 0.0F) {
                hurtOverTime(player, TEMPERATURE_DEBT, FREEZE_HEALTH_PER_SECOND * dt, player.damageSources().freeze());
            } else if (stats.temperature() > DSStats.OVERHEAT_TEMP) {
                hurtOverTime(player, TEMPERATURE_DEBT, FREEZE_HEALTH_PER_SECOND * dt, player.damageSources().onFire());
            }

            grue(player, light);
            if (sendStats) {
                send(player, rate);
            }
        }
    }

    /** temperature.lua: the rate towards the ambient temperature without insulation. */
    static float temperatureRate(float current, float ambient) {
        float delta = ambient - current;
        if (ambient >= DSStats.STARTING_TEMP) {
            return delta > 0.0F ? Math.min(delta, 1.0F)
                    : Math.max(delta, current >= DSStats.OVERHEAT_TEMP ? -THAW_DEGREES_PER_SEC : -WARM_DEGREES_PER_SEC);
        }
        if (delta < 0.0F) {
            return Math.max(delta, -1.0F);
        }
        return Math.min(delta, current <= 0.0F ? THAW_DEGREES_PER_SEC : WARM_DEGREES_PER_SEC);
    }

    /** DS damage over time (DS health), dealt once a full DS second has accumulated. */
    private static void hurtOverTime(ServerPlayer player, Map<UUID, Float> debts, float dsHealth, DamageSource source) {
        float debt = debts.getOrDefault(player.getUUID(), 0.0F) + DSScale.stat(dsHealth);
        if (debt >= DSScale.stat(1.25F)) {
            player.hurt(source, debt);
            debt = 0.0F;
        }
        debts.put(player.getUUID(), debt);
    }

    private static void send(ServerPlayer player, float sanityRate) {
        DSStats stats = stats(player);
        PacketDistributor.sendToPlayer(player, new StatsPayload(stats.hunger(), stats.sanity(), stats.sane(), sanityRate, stats.temperature()));
    }

    /** Vanilla hunger stands still on the islands: no regeneration, no starvation, sprinting allowed. */
    private static void holdVanillaHunger(FoodData food) {
        food.setFoodLevel(17);
        food.setSaturation(0.0F);
        food.setExhaustion(0.0F);
    }

    private static float aura(ServerPlayer player) {
        float total = 0.0F;
        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(SANITY_EFFECT_RANGE),
                e -> e instanceof SanityAura && e.isAlive())) {
            double distSq = entity.distanceToSqr(player);
            if (distSq <= SANITY_EFFECT_RANGE * SANITY_EFFECT_RANGE) {
                total += ((SanityAura) entity).sanityAura(player) / (float) Math.max(1.0, distSq);
            }
        }
        return total;
    }

    private static void grue(ServerPlayer player, float light) {
        Grue grue = GRUES.computeIfAbsent(player.getUUID(), id -> new Grue());
        if (light > DSLight.LIGHT_THRESH) {
            grue.inLight = true;
        } else if (light < DSLight.DARK_THRESH) {
            grue.inLight = false;
        }
        RandomSource random = player.getRandom();
        if (grue.inLight) {
            grue.level = null;
            return;
        }
        if (grue.level == null) {
            grue.level = 0;
            grue.nextHit = 5.0F + random.nextFloat() * 5.0F;
            grue.nextSound = grue.nextHit * (0.4F + random.nextFloat() * 0.4F);
        }
        float dt = 1.0F / 20.0F;
        if (grue.nextSound > 0.0F) {
            grue.nextSound -= dt;
            if (grue.nextSound <= 0.0F) {
                player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.AMBIENT_CAVE.value(),
                        SoundSource.HOSTILE, 1.0F, 0.6F);
            }
        }
        grue.nextHit -= dt;
        if (grue.nextHit <= 0.0F) {
            grue.level++;
            grue.nextHit = 5.0F + random.nextFloat() * 6.0F;
            grue.nextSound = grue.nextHit * (0.4F + random.nextFloat() * 0.4F);
            DamageSource darkness = new DamageSource(
                    player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DARKNESS));
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.WARDEN_ATTACK_IMPACT,
                    SoundSource.HOSTILE, 1.0F, 0.8F);
            DontStarveCraft.LOGGER.info("The grue attacks {}", player.getGameProfile().getName());
            player.hurt(darkness, DSScale.stat(GRUE_DAMAGE));
            set(player, stats(player).withSanity(stats(player).sanity() - GRUE_SANITY));
        }
    }

    /** Applies a DS food's values; the test hook of /ds eat. */
    public static void eat(ServerPlayer player, ItemStack stack) {
        if (stack.getItem() instanceof DSFoodItem food) {
            eat(player, net.celestiald.dontstarve.item.Perishable.effective(stack, food.edible()));
        }
    }

    public static void eat(ServerPlayer player, DSFoodItem.Edible edible) {
        set(player, stats(player).withHunger(stats(player).hunger() + edible.hunger())
                .withSanity(stats(player).sanity() + edible.sanity()));
        float health = DSScale.stat(edible.health());
        if (health > 0.0F) {
            player.heal(health);
        } else if (health < 0.0F) {
            player.hurt(player.damageSources().magic(), -health);
        }
    }

    /** On the islands only DS foods are edible. */
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player player && player.level().dimension().equals(DSRefs.ISLANDS)
                && event.getItem().has(net.minecraft.core.component.DataComponents.FOOD)
                && !(event.getItem().getItem() instanceof DSFoodItem)) {
            event.setCanceled(true);
        }
    }

    public static void onUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level().dimension().equals(DSRefs.ISLANDS)) {
            eat(player, event.getItem());
        }
    }

    private Survival() {
    }
}
