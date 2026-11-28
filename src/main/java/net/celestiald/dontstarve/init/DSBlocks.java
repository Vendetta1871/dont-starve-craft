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

    public static final DeferredBlock<SkyPortalBlock> SKY_PORTAL =
            BLOCKS.register("sky_portal", () -> new SkyPortalBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.QUARTZ)
                            .strength(3.0F, -1.0F)
                            .sound(SoundType.AMETHYST)
                            .lightLevel(state -> state.getValue(SkyPortalBlock.LIT) ? 7 : 0)
                            .noOcclusion()));

    // Prototypers (prefabs/researchlab*.lua) with TUNING.PROTOTYPER_TREES. Not hand-breakable:
    // DS structures come down with a hammer.
    public static final DeferredBlock<net.celestiald.dontstarve.craft.PrototyperBlock> RESEARCHLAB =
            BLOCKS.register("researchlab", () -> prototyper(MapColor.METAL,
                    java.util.Map.of("SCIENCE", 1, "MAGIC", 1), net.minecraft.sounds.SoundEvents.NOTE_BLOCK_CHIME.value()));
    public static final DeferredBlock<net.celestiald.dontstarve.craft.PrototyperBlock> RESEARCHLAB2 =
            BLOCKS.register("researchlab2", () -> prototyper(MapColor.COLOR_ORANGE,
                    java.util.Map.of("SCIENCE", 2, "MAGIC", 1), net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BELL.value()));
    public static final DeferredBlock<net.celestiald.dontstarve.craft.PrototyperBlock> RESEARCHLAB4 =
            BLOCKS.register("researchlab4", () -> prototyper(MapColor.COLOR_BLACK,
                    java.util.Map.of("MAGIC", 2), net.minecraft.sounds.SoundEvents.ILLUSIONER_CAST_SPELL));
    public static final DeferredBlock<net.celestiald.dontstarve.craft.PrototyperBlock> RESEARCHLAB3 =
            BLOCKS.register("researchlab3", () -> prototyper(MapColor.COLOR_PURPLE,
                    java.util.Map.of("MAGIC", 3), net.minecraft.sounds.SoundEvents.EVOKER_CAST_SPELL));

    private static net.celestiald.dontstarve.craft.PrototyperBlock prototyper(
            MapColor color, java.util.Map<String, Integer> trees, net.minecraft.sounds.SoundEvent ding) {
        return new net.celestiald.dontstarve.craft.PrototyperBlock(BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(-1.0F, 6.0F)
                .sound(SoundType.WOOD)
                .pushReaction(PushReaction.BLOCK)
                .noOcclusion(), trees, ding);
    }

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

    /** Moon rock boulder (DST "rock_moon", name "Boulder"): rocks, flint, moon rock (rocks.lua:111-118, 378-388). */
    public static final DeferredBlock<Block> ROCK_MOON =
            BLOCKS.register("rock_moon", () -> new Block(boulderProps().mapColor(MapColor.QUARTZ)));

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
