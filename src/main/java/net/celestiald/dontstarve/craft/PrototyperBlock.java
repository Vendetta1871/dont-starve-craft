package net.celestiald.dontstarve.craft;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * A DST prototyper (components/prototyper.lua): standing within
 * RESEARCH_MACHINE_DIST of it grants its tech trees (TUNING.PROTOTYPER_TREES).
 * Structures are not hand-breakable in DS; the hammer takes them down (DS-3).
 */
public class PrototyperBlock extends Block {

    private final Map<String, Integer> trees;
    private final SoundEvent ding;

    public PrototyperBlock(Properties properties, Map<String, Integer> trees, SoundEvent ding) {
        super(properties);
        this.trees = Map.copyOf(trees);
        this.ding = ding;
    }

    public Map<String, Integer> trees() {
        return trees;
    }

    /** prototyper:Activate: the machine reacts to a prototype being built at it. */
    public void activate(Level level, BlockPos pos) {
        level.playSound(null, pos, ding, SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}
