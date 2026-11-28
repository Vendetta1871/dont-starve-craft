package net.celestiald.dontstarve.cook;

/**
 * The TUNING.lua values the Crock Pot data uses (scripts/tuning.lua), in DS units:
 * total_day_time 480 s (seg_time 30 x 16), night_time 60 s (2 segments), calories_per_day 75,
 * perish_warp 1. Names and arithmetic follow tuning.lua so the generated recipe data reads
 * like preparedfoods.lua.
 */
final class T {

    private static final double TOTAL_DAY_TIME = 480.0;
    private static final double NIGHT_TIME = 60.0;
    private static final double CALORIES_PER_DAY = 75.0;
    private static final double PERISH_WARP = 1.0;

    // Health (tuning.lua:2021-2030)
    static final double HEALING_TINY = 1;
    static final double HEALING_SMALL = 3;
    static final double HEALING_MEDSMALL = 8;
    static final double HEALING_MED = 20;
    static final double HEALING_MEDLARGE = 30;
    static final double HEALING_LARGE = 40;
    static final double HEALING_HUGE = 60;
    static final double HEALING_MOREHUGE = 75;
    static final double HEALING_SUPERHUGE = 100;

    // Sanity (tuning.lua:2033-2038)
    static final double SANITY_TINY = 5;
    static final double SANITY_SMALL = 10;
    static final double SANITY_MED = 15;
    static final double SANITY_MEDLARGE = 20;
    static final double SANITY_LARGE = 33;
    static final double SANITY_HUGE = 50;

    // Perish times (tuning.lua:2043-2051)
    static final double PERISH_SUPERFAST = 3 * TOTAL_DAY_TIME * PERISH_WARP;
    static final double PERISH_FAST = 6 * TOTAL_DAY_TIME * PERISH_WARP;
    static final double PERISH_MED = 10 * TOTAL_DAY_TIME * PERISH_WARP;
    static final double PERISH_SLOW = 15 * TOTAL_DAY_TIME * PERISH_WARP;
    static final double PERISH_PRESERVED = 20 * TOTAL_DAY_TIME * PERISH_WARP;

    // Calories (tuning.lua:2058-2065)
    static final double CALORIES_TINY = CALORIES_PER_DAY / 8;
    static final double CALORIES_SMALL = CALORIES_PER_DAY / 6;
    static final double CALORIES_MEDSMALL = CALORIES_PER_DAY / 4;
    static final double CALORIES_MED = CALORIES_PER_DAY / 3;
    static final double CALORIES_LARGE = CALORIES_PER_DAY / 2;
    static final double CALORIES_HUGE = CALORIES_PER_DAY;
    static final double CALORIES_MOREHUGE = CALORIES_PER_DAY * 4 / 3;
    static final double CALORIES_SUPERHUGE = CALORIES_PER_DAY * 2;

    /** Food temperature (tuning.lua:2326-2332). */
    static final double HOT_FOOD_BONUS_TEMP = 40;
    static final double COLD_FOOD_BONUS_TEMP = -40;
    static final double FOOD_TEMP_BRIEF = 5;
    static final double FOOD_TEMP_AVERAGE = 10;
    static final double FOOD_TEMP_LONG = 15;

    /** tuning.lua:194-196: the first tick of the jellybean health regeneration. */
    static final double JELLYBEAN_TICK_VALUE = 2;
    /** tuning.lua:5645. */
    static final double HUNGERREGEN_TICK_VALUE = 5 * (CALORIES_PER_DAY * 2.5) / (0.5 * TOTAL_DAY_TIME * PERISH_WARP);

    /** tuning.lua:2096: night_time * .3333. */
    static final double BASE_COOK_TIME = NIGHT_TIME * .3333;

    private T() {
    }
}
