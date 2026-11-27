package net.celestiald.dontstarve.init;

import net.celestiald.dontstarve.DontStarveCraft;
import net.celestiald.dontstarve.revive.ResurrectionStatueBlock;
import net.celestiald.dontstarve.grave.GraveBlock;
import net.celestiald.dontstarve.portal.SkyPortalBlock;
import net.celestiald.dontstarve.world.PickableBlock;
import net.celestiald.dontstarve.world.SpiderDenBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DSBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(DontStarveCraft.MODID);

    public static final DeferredBlock<Block> MARBLE =
            BLOCKS.register("marble", () -> new Block(marbleProps()));

    public static final DeferredBlock<Block> POLISHED_MARBLE =
            BLOCKS.register("polished_marble", () -> new Block(marbleProps()));

    public static final DeferredBlock<Block> MARBLE_BRICKS =
            BLOCKS.register("marble_bricks", () -> new Block(marbleProps()));

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

    // Moon meteorite debris. Deliberately hand-mineable: the escape route must
    // work for a player who arrived with nothing.
    public static final DeferredBlock<Block> MOONROCK_CLUSTER =
            BLOCKS.register("moonrock_cluster", () -> new Block(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.QUARTZ)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .strength(1.2F, 3.0F)
                            .sound(SoundType.CALCITE)));

    public static final DeferredBlock<Block> MOON_CRYSTAL =
            BLOCKS.register("moon_crystal", () -> new Block(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.QUARTZ)
                            .strength(1.2F, 3.0F)
                            .sound(SoundType.AMETHYST)
                            .lightLevel(state -> 4)));

    public static final DeferredBlock<SkyPortalBlock> SKY_PORTAL =
            BLOCKS.register("sky_portal", () -> new SkyPortalBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.QUARTZ)
                            .strength(3.0F, -1.0F)
                            .sound(SoundType.AMETHYST)
                            .lightLevel(state -> state.getValue(SkyPortalBlock.LIT) ? 7 : 0)
                            .noOcclusion()));

    // Island flora — DS pickables (pick by hand, dig with a shovel).
    /** Grass tuft (DST "grass"): cutgrass, GRASS_REGROW_TIME 1440 s, GRASS_CYCLES 20 (grass.lua:248-254). */
    public static final DeferredBlock<PickableBlock> GRASS =
            BLOCKS.register("grass", () -> new PickableBlock(plantProps(), DSItems.CUTGRASS, 1440, 20));

    /** Sapling (DST "sapling"): a twig bush, twigs every SAPLING_REGROW_TIME 1920 s, unlimited (sapling.lua:141). */
    public static final DeferredBlock<PickableBlock> SAPLING =
            BLOCKS.register("sapling", () -> new PickableBlock(plantProps(), DSItems.TWIGS, 1920, 0));

    /** Boulder (DST "rock1"): rocks, nitre and flint (rocks.lua:65-74). */
    public static final DeferredBlock<Block> ROCK1 =
            BLOCKS.register("rock1", () -> new Block(boulderProps()));

    /** Gold-veined boulder (DST "rock2"): rocks, gold and flint (rocks.lua:76-85). */
    public static final DeferredBlock<Block> ROCK2 =
            BLOCKS.register("rock2", () -> new Block(boulderProps()));

    public static final DeferredBlock<SpiderDenBlock> SPIDER_DEN =
            BLOCKS.register("spider_den", () -> new SpiderDenBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_LIGHT_GRAY)
                            .strength(2.0F, 4.0F)
                            .sound(SoundType.WOOL)));

    private static BlockBehaviour.Properties plantProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .strength(0.3F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.GRASS)
                .offsetType(BlockBehaviour.OffsetType.XZ)
                .pushReaction(PushReaction.DESTROY);
    }

    /** ROCKS_MINE 6 pickaxe hits in DS; needs a pickaxe here too. */
    private static BlockBehaviour.Properties boulderProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(3.0F, 6.0F)
                .sound(SoundType.STONE);
    }

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
