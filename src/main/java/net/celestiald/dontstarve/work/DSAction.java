package net.celestiald.dontstarve.work;

/**
 * DST work actions (actions.lua) that a tool performs on a workable target.
 * {@code hitTicks} is how long one swing takes, from the player stategraph
 * (SGwilson.lua): chop, mine and hammer repeat every 14 frames, dig every 35
 * (1 frame = 1/30 s). Minecraft adds a 5-tick delay after each completed
 * "break", so the mining bar fills in the rest.
 */
public enum DSAction {
    CHOP(14),
    MINE(14),
    HAMMER(14),
    DIG(35),
    /** Pitchfork turf digging: not ported until turfs exist. */
    TERRAFORM(35),
    /** Razor: shaving beefalo. Not ported until beefalo exist. */
    SHAVE(14);

    /** Ticks of the mining bar per swing (swing period minus Minecraft's 5-tick destroy delay). */
    public final int hitTicks;

    DSAction(int frames) {
        this.hitTicks = Math.max(1, Math.round(frames / 30.0F * 20.0F) - 5);
    }
}
