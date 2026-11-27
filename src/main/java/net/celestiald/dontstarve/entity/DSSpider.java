package net.celestiald.dontstarve.entity;

import net.celestiald.dontstarve.DSScale;

import javax.annotation.Nullable;
import net.celestiald.dontstarve.survive.GhostState;
import net.celestiald.dontstarve.world.SpiderDenBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import java.util.EnumSet;

/**
 * Spider (DST prefab "spider", prefabs/spider.lua). Our own mob — model and
 * texture are placeholders until DS art exists.
 *
 * Stats scale DST numbers by the player's health ratio (Wilson 150 HP ->
 * 20 HP, x2/15): health 100 -> 13.3, damage 20 -> 2.7, attack period 3 s
 * (spider.lua:765-768). Runs at SPIDER_RUN_SPEED 5 b/s, wanders at
 * SPIDER_WALK_SPEED 3 (spider.lua:771-772). Retargets players within
 * SPIDER_TARGET_DIST 4 units = 4 blocks
 * (spider.lua:227). A spider without a den sleeps by day (spider.lua:252-263);
 * a den spider goes home at dawn and is stored back in its den.
 * Loot: one weighted item — monstermeat 50 %, silk 25 %, spidergland 25 %.
 */
public class DSSpider extends Monster implements net.celestiald.dontstarve.survive.SanityAura {

    private static final double TARGET_DIST = DSScale.blocks(4);
    private static final double RUN_SPEED = 5;
    /** -SANITYAURA_SMALL (spider.lua:774): 100/(30*8) sanity per DS s at distance 1. */
    private static final float AURA = -100.0F / (30 * 8);

    @Override
    public float sanityAura(Player observer) {
        return AURA;
    }
    private static final double WALK_SPEED = 3;

    @Nullable
    private BlockPos home;

    public DSSpider(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, DSScale.stat(100))
                .add(Attributes.ATTACK_DAMAGE, DSScale.stat(20))
                .add(Attributes.MOVEMENT_SPEED, DSScale.mobSpeed(RUN_SPEED))
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    public void setHome(@Nullable BlockPos home) {
        this.home = home;
    }

    @Nullable
    public BlockPos getHome() {
        return home;
    }

    /** DST: homeless spiders sleep through the day unless something wakes them. */
    public boolean isSleepy() {
        return home == null && net.celestiald.dontstarve.clock.RunClock.isDay(level()) && getTarget() == null && getLastHurtByMob() == null;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new SpiderAttackGoal(this));
        goalSelector.addGoal(3, new GoHomeGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, DSScale.speedModifier(WALK_SPEED, RUN_SPEED)) {
            @Override
            public boolean canUse() {
                return !isSleepy() && super.canUse();
            }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 20, false, false,
                entity -> entity instanceof Player player && !GhostState.isGhost(player)) {
            @Override
            protected double getFollowDistance() {
                return TARGET_DIST; // acquisition radius; the chase uses FOLLOW_RANGE
            }

            @Override
            public boolean canUse() {
                return !isSleepy() && super.canUse();
            }
        });
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) {
            tag.put("Home", NbtUtils.writeBlockPos(home));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        home = NbtUtils.readBlockPos(tag, "Home").orElse(null);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SPIDER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SPIDER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SPIDER_DEATH;
    }

    /** Melee with DST's 3 s attack period. */
    private static final class SpiderAttackGoal extends MeleeAttackGoal {

        SpiderAttackGoal(DSSpider spider) {
            super(spider, 1.0, true);
        }

        @Override
        protected int getAttackInterval() {
            return adjustedTickDelay(60);
        }
    }

    /** At dawn a den spider walks home and is stored back inside its den. */
    private static final class GoHomeGoal extends Goal {

        private final DSSpider spider;

        GoHomeGoal(DSSpider spider) {
            this.spider = spider;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return spider.home != null && net.celestiald.dontstarve.clock.RunClock.isDay(spider.level()) && spider.getTarget() == null;
        }

        @Override
        public void tick() {
            BlockPos home = spider.home;
            if (home == null) {
                return;
            }
            if (spider.blockPosition().closerThan(home, 2.5)) {
                if (SpiderDenBlock.takeBack(spider.level(), home)) {
                    spider.discard();
                } else {
                    spider.home = null; // the den is gone: a homeless spider now
                }
                return;
            }
            if (spider.getNavigation().isDone()) {
                spider.getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.0);
            }
        }
    }
}
