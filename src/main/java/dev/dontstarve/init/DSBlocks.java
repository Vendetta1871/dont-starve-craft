package dev.dontstarve.init;

import dev.dontstarve.DontStarveCraft;
import dev.dontstarve.revive.ResurrectionStatueBlock;
import dev.dontstarve.grave.GraveBlock;
import dev.dontstarve.portal.MarbleBlock;
import dev.dontstarve.portal.MarblePortalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DSBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(DontStarveCraft.MODID);

    public static final DeferredBlock<MarbleBlock> MARBLE =
            BLOCKS.register("marble", () -> new MarbleBlock(marbleProps()));

    public static final DeferredBlock<MarbleBlock> POLISHED_MARBLE =
            BLOCKS.register("polished_marble", () -> new MarbleBlock(marbleProps()));

    public static final DeferredBlock<MarbleBlock> MARBLE_BRICKS =
            BLOCKS.register("marble_bricks", () -> new MarbleBlock(marbleProps()));

    public static final DeferredBlock<MarblePortalBlock> MARBLE_PORTAL =
            BLOCKS.register("marble_portal", () -> new MarblePortalBlock(
                    BlockBehaviour.Properties.of()
                            .noCollission()
                            .noOcclusion()
                            .instabreak()
                            .lightLevel(state -> 11)
                            .sound(SoundType.AMETHYST)
                            .noLootTable()));

    public static final DeferredBlock<GraveBlock> GRAVE =
            BLOCKS.register("grave", () -> new GraveBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.MUD)));

    public static final DeferredBlock<ResurrectionStatueBlock> RESURRECTION_STATUE =
            BLOCKS.register("resurrectionstatue", () -> new ResurrectionStatueBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.QUARTZ)
                            .strength(1.5F, 4.0F)
                            .sound(SoundType.BONE_BLOCK)));

    private static BlockBehaviour.Properties marbleProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(1.5F, 6.0F);
    }

    private DSBlocks() {
    }
}
