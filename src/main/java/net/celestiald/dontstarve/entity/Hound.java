package net.celestiald.dontstarve.entity;

import net.celestiald.dontstarve.DSScale;

import net.celestiald.dontstarve.survive.GhostState;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Hound (DST prefab "hound", prefabs/hound.lua): the always-hostile pack
 * hunter of the hound waves. Its own mob, not a wolf — model, texture and
 * sounds are placeholders until DS art exists.
 *
 * Stats scale DST numbers by the player's health ratio (Wilson 150 HP ->
 * Minecraft 20 HP, x2/15): HOUND_HEALTH 150 -> 20, HOUND_DAMAGE 20 -> 2.67, HOUND_TARGET_DIST 20 units -> 20 blocks,
 * HOUND_ATTACK_PERIOD 2 s (tuning.lua:1490-1492). Runs at HOUND_SPEED 10 b/s,
 * wanders at the locomotor default walk speed 4 (components/locomotor.lua:300).
 * Loot: monstermeat 100 %, houndstooth 12.5 % (hound.lua:110-113).
 */
public class Hound extends Monster implements net.celestiald.dontstarve.survive.SanityAura {

    private static final double RUN_SPEED = 10;
    /** -SANITYAURA_MED (hound.lua:553): 100/(30*5) sanity per DS s at distance 1. */
    private static final float AURA = -100.0F / (30 * 5);

    @Override
    public float sanityAura(Player observer) {
        return AURA;
    }
    private static final double WALK_SPEED = 4;

    public Hound(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, DSScale.stat(150))
                .add(Attributes.ATTACK_DAMAGE, DSScale.stat(20))
                .add(Attributes.MOVEMENT_SPEED, DSScale.mobSpeed(RUN_SPEED))
                .add(Attributes.FOLLOW_RANGE, DSScale.blocks(20));
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.4F));
        goalSelector.addGoal(3, new HoundAttackGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, DSScale.speedModifier(WALK_SPEED, RUN_SPEED)));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // DST hounds retarget any living player in range; ghosts are not prey.
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false,
                entity -> entity instanceof Player player && !GhostState.isGhost(player)));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WOLF_GROWL;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOLF_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOLF_DEATH;
    }

    /** Melee with DST's 2 s attack period instead of vanilla's 1 s. */
    private static final class HoundAttackGoal extends MeleeAttackGoal {

        HoundAttackGoal(Hound hound) {
            super(hound, 1.0, true);
        }

        @Override
        protected int getAttackInterval() {
            return adjustedTickDelay(40);
        }
    }
}
