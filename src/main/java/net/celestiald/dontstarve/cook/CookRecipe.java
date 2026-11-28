package net.celestiald.dontstarve.cook;

import java.util.List;

/**
 * One Crock Pot recipe (a preparedfoods.lua / preparednonfoods.lua entry): the test over the
 * ingredient names and tags, the priority and weight that pick it, and what the dish is.
 * Times are DS seconds, food values DS points.
 */
public final class CookRecipe {

    /** The recipe test: ingredient counts by prefab and summed tag values, 0 where Lua has nil. */
    @FunctionalInterface
    public interface Test {
        boolean test(Cooking.Counts names, Cooking.Counts tags);
    }

    private final String name;
    private final Test test;
    private double priority;
    private double weight = 1;
    private String foodType;
    private String secondaryFoodType;
    private double health;
    private double hunger;
    private double sanity;
    private double perishTime;
    private double cookpotPerishTime = -1;
    private double cookTime = 1;
    private double temperature;
    private double temperatureDuration;
    private int stackSize = 1;
    private List<String> tags = List.of();
    private boolean noPrefab;

    CookRecipe(String name, Test test) {
        this.name = name;
        this.test = test;
    }

    CookRecipe priority(double v) {
        priority = v;
        return this;
    }

    CookRecipe weight(double v) {
        weight = v;
        return this;
    }

    CookRecipe foodType(String v) {
        foodType = v;
        return this;
    }

    CookRecipe secondaryFoodType(String v) {
        secondaryFoodType = v;
        return this;
    }

    CookRecipe health(double v) {
        health = v;
        return this;
    }

    CookRecipe hunger(double v) {
        hunger = v;
        return this;
    }

    CookRecipe sanity(double v) {
        sanity = v;
        return this;
    }

    CookRecipe perishTime(double v) {
        perishTime = v;
        return this;
    }

    CookRecipe cookpotPerishTime(double v) {
        cookpotPerishTime = v;
        return this;
    }

    CookRecipe cookTime(double v) {
        cookTime = v;
        return this;
    }

    CookRecipe temperature(double delta, double duration) {
        temperature = delta;
        temperatureDuration = duration;
        return this;
    }

    CookRecipe stackSize(int v) {
        stackSize = v;
        return this;
    }

    CookRecipe tags(String... v) {
        tags = List.of(v);
        return this;
    }

    CookRecipe withoutPrefab() {
        noPrefab = true;
        return this;
    }

    /** The prefab name, also the product item's registry path. */
    public String name() {
        return name;
    }

    public boolean test(Cooking.Counts names, Cooking.Counts tags) {
        return test.test(names, tags);
    }

    public double priority() {
        return priority;
    }

    public double weight() {
        return weight;
    }

    /** FOODTYPE.* name (MEAT, VEGGIE, GOODIES, ROUGHAGE, ELEMENTAL ...), null for wet goop. */
    public String foodType() {
        return foodType;
    }

    public String secondaryFoodType() {
        return secondaryFoodType;
    }

    public double health() {
        return health;
    }

    public double hunger() {
        return hunger;
    }

    public double sanity() {
        return sanity;
    }

    /** Perish time in DS seconds; 0 = not perishable. */
    public double perishTime() {
        return perishTime;
    }

    /** How long the finished dish lasts in the pot (stewer.lua: cookpot_perishtime or perishtime). */
    public double potPerishTime() {
        return cookpotPerishTime >= 0 ? cookpotPerishTime : perishTime;
    }

    /** Cook time in units of TUNING.BASE_COOK_TIME. */
    public double cookTime() {
        return cookTime;
    }

    /** edible.temperaturedelta: degrees added in the belly, 0 = none. */
    public double temperature() {
        return temperature;
    }

    /** edible.temperatureduration in DS seconds. */
    public double temperatureDuration() {
        return temperatureDuration;
    }

    /** Items per harvest (jellybean 3). */
    public int stackSize() {
        return stackSize;
    }

    public List<String> tags() {
        return tags;
    }

    /** The product is defined elsewhere (a hat), not as a food prefab. */
    public boolean noPrefab() {
        return noPrefab;
    }
}
