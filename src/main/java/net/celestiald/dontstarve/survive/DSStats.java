package net.celestiald.dontstarve.survive;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A player's DS hunger and sanity, in DS units (Wilson: 150 hunger, 200 sanity,
 * tuning.lua:37-40). Health is the vanilla health bar (150 DS = 20 HP). "sane"
 * follows sanity.lua's hysteresis: insane at or below 15 %, sane again at 17.5 %.
 * Temperature in degrees (temperature.lua): STARTING_TEMP 35, clamped to -20..90,
 * freezing below 0, overheating above 70.
 */
public record DSStats(float hunger, float sanity, boolean sane, float temperature) {

    public static final float MAX_HUNGER = 150.0F;
    public static final float MAX_SANITY = 200.0F;
    /** SANITY_BECOME_INSANE_THRESH / SANITY_BECOME_SANE_THRESH. */
    public static final float INSANE_THRESH = 0.15F;
    public static final float SANE_THRESH = 0.175F;

    public static final float STARTING_TEMP = 35.0F;
    public static final float MIN_TEMP = -20.0F;
    public static final float MAX_TEMP = 90.0F;
    public static final float OVERHEAT_TEMP = 70.0F;

    public static final DSStats FULL = new DSStats(MAX_HUNGER, MAX_SANITY, true, STARTING_TEMP);

    public static final Codec<DSStats> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.fieldOf("hunger").forGetter(DSStats::hunger),
            Codec.FLOAT.fieldOf("sanity").forGetter(DSStats::sanity),
            Codec.BOOL.fieldOf("sane").forGetter(DSStats::sane),
            Codec.FLOAT.optionalFieldOf("temperature", STARTING_TEMP).forGetter(DSStats::temperature)
    ).apply(i, DSStats::new));

    public DSStats withHunger(float value) {
        return new DSStats(Math.max(0.0F, Math.min(MAX_HUNGER, value)), sanity, sane, temperature);
    }

    public DSStats withSanity(float value) {
        float v = Math.max(0.0F, Math.min(MAX_SANITY, value));
        boolean nowSane = sane;
        if (sane && v / MAX_SANITY <= INSANE_THRESH) {
            nowSane = false;
        } else if (!sane && v / MAX_SANITY >= SANE_THRESH) {
            nowSane = true;
        }
        return new DSStats(hunger, v, nowSane, temperature);
    }

    public DSStats withTemperature(float value) {
        return new DSStats(hunger, sanity, sane, Math.max(MIN_TEMP, Math.min(MAX_TEMP, value)));
    }
}
