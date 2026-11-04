package net.celestiald.dontstarve.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import java.util.List;

/**
 * Hairy Hound (DST "hound"): the shaggy red hound variant that, true to its
 * DS joke, is the practical source of beard hair — the effigy ingredient.
 * In 1.21.1 the hound family is just the wolf, so this is a wolf with its own
 * loot table and coat. DST hounds have 150 health and 20 damage
 * (tuning.lua:1490-1491); scaled to Minecraft's 20-health player that is a
 * 20-health, 4-damage attacker that keeps hunting until it or its prey dies.
 */
public class HairyHound extends Wolf {

    public HairyHound(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Wolf.createAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.325);
    }

    @Override
    public boolean isBaby() {
        return false; // hound waves never bring puppies
    }

    @Override
    protected void customServerAiStep() {
        // DST hounds never give up the chase: re-acquire the nearest survivor.
        if (getTarget() == null && !level().isClientSide) {
            List<Player> prey = level().getEntitiesOfClass(Player.class,
                    getBoundingBox().inflate(24.0),
                    e -> e.isAlive() && !e.isSpectator());
            Player nearest = null;
            double best = Double.MAX_VALUE;
            for (Player player : prey) {
                double distSq = distanceToSqr(player);
                if (distSq < best) {
                    best = distSq;
                    nearest = player;
                }
            }
            if (nearest != null) {
                setTarget(nearest);
            }
        }
        super.customServerAiStep();
    }
}
