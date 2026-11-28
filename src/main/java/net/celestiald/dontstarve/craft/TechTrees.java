package net.celestiald.dontstarve.craft;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Which tech a builder has where it stands (builder.lua EvaluateTechTrees, widgetutil.lua). */
public final class TechTrees {

    /** TUNING.RESEARCH_MACHINE_DIST 4 units, 1 unit = 1 block. */
    public static final double RESEARCH_MACHINE_DIST = 4.0;

    /** The trees of the closest prototyper in range (only that one is turned on); no prototyper = none. */
    public record Access(Map<String, Integer> trees, BlockPos prototyper) {
        public static final Access NONE = new Access(Map.of(), null);

        public int get(String tree) {
            return trees.getOrDefault(tree, 0);
        }
    }

    public static Access at(Level level, Vec3 pos) {
        int r = (int) Math.ceil(RESEARCH_MACHINE_DIST);
        BlockPos centre = BlockPos.containing(pos);
        double best = RESEARCH_MACHINE_DIST * RESEARCH_MACHINE_DIST;
        Access found = Access.NONE;
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-r, -r, -r), centre.offset(r, r, r))) {
            if (level.getBlockState(p).getBlock() instanceof PrototyperBlock proto
                    && !level.getBlockState(p).getValue(PrototyperBlock.BURNT)) {
                double d = pos.distanceToSqr(Vec3.atBottomCenterOf(p));
                if (d <= best) {
                    best = d;
                    found = new Access(proto.trees(), p.immutable());
                }
            }
        }
        return found;
    }

    /** CanPrototypeRecipe: every level the recipe needs is available. */
    public static boolean canPrototype(Map<String, Integer> level, Access access) {
        for (var en : level.entrySet()) {
            if (en.getValue() > access.get(en.getKey())) {
                return false;
            }
        }
        return true;
    }

    /** ShouldHintRecipe: the recipe is at most one level beyond what is available. */
    public static boolean shouldHint(Map<String, Integer> level, Access access) {
        for (var en : level.entrySet()) {
            if (en.getValue() > access.get(en.getKey()) + 1) {
                return false;
            }
        }
        return true;
    }

    private TechTrees() {
    }
}
