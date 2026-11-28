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

    // Prototypers (prefabs/scienceprototyper.lua etc.) with TUNING.PROTOTYPER_TREES. Not
    // hand-breakable: 4 hammer hits bring them down (PrototyperBlock).
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

    // Fueled fires (prefabs/campfire.lua, firepit.lua): fuel by right-click, light and warmth while burning.
    public static final DeferredBlock<net.celestiald.dontstarve.fire.FueledFireBlock> CAMPFIRE =
            BLOCKS.register("campfire", () -> new net.celestiald.dontstarve.fire.FueledFireBlock(
                    net.celestiald.dontstarve.fire.FueledFireBlock.properties(MapColor.WOOD, SoundType.WOOD),
                    net.celestiald.dontstarve.fire.FueledFireBlock.Kind.CAMPFIRE));
    public static final DeferredBlock<net.celestiald.dontstarve.fire.FueledFireBlock> FIREPIT =
            BLOCKS.register("firepit", () -> new net.celestiald.dontstarve.fire.FueledFireBlock(
                    net.celestiald.dontstarve.fire.FueledFireBlock.properties(MapColor.STONE, SoundType.STONE),
                    net.celestiald.dontstarve.fire.FueledFireBlock.Kind.FIREPIT));

    private static net.celestiald.dontstarve.craft.PrototyperBlock prototyper(
            MapColor color, java.util.Map<String, Integer> trees, net.minecraft.sounds.SoundEvent ding) {
        return new net.celestiald.dontstarve.craft.PrototyperBlock(BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(2.0F, 6.0F)
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
    public static final DeferredBlock<net.celestiald.dontstarve.world.BoulderBlock> ROCK1 =
            BLOCKS.register("rock1", () -> new net.celestiald.dontstarve.world.BoulderBlock(boulderProps()));

    /** Gold-veined boulder (DST "rock2"): rocks, gold and flint (rocks.lua:76-85). */
    public static final DeferredBlock<net.celestiald.dontstarve.world.BoulderBlock> ROCK2 =
            BLOCKS.register("rock2", () -> new net.celestiald.dontstarve.world.BoulderBlock(boulderProps()));

    /** Moon rock boulder (DST "rock_moon", name "Boulder"): rocks, flint, moon rock (rocks.lua:111-118, 378-388). */
    public static final DeferredBlock<net.celestiald.dontstarve.world.BoulderBlock> ROCK_MOON =
            BLOCKS.register("rock_moon", () -> new net.celestiald.dontstarve.world.BoulderBlock(boulderProps().mapColor(MapColor.QUARTZ)));

    public static final DeferredBlock<SpiderDenBlock> SPIDER_DEN =
            BLOCKS.register("spider_den", () -> new SpiderDenBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_LIGHT_GRAY)
                            .strength(2.0F, 4.0F)
                            .sound(SoundType.WOOL)));

    // DST evergreens (prefabs/evergreens.lua): trunk (state = growable stage), crown, burnt tree and stump
    // for the normal and the lumpy ("sparse") build; saplings planted from pinecones (planted_tree.lua).
    public static final DeferredBlock<net.celestiald.dontstarve.world.EvergreenBlock> EVERGREEN =
            BLOCKS.register("evergreen", () -> new net.celestiald.dontstarve.world.EvergreenBlock(treeProps(), false));
    public static final DeferredBlock<net.celestiald.dontstarve.world.EvergreenBlock> EVERGREEN_SPARSE =
            BLOCKS.register("evergreen_sparse", () -> new net.celestiald.dontstarve.world.EvergreenBlock(treeProps(), true));
    public static final DeferredBlock<net.celestiald.dontstarve.world.EvergreenCrownBlock> EVERGREEN_CROWN =
            BLOCKS.register("evergreen_crown", () -> new net.celestiald.dontstarve.world.EvergreenCrownBlock(crownProps(), false));
    public static final DeferredBlock<net.celestiald.dontstarve.world.EvergreenCrownBlock> EVERGREEN_SPARSE_CROWN =
            BLOCKS.register("evergreen_sparse_crown", () -> new net.celestiald.dontstarve.world.EvergreenCrownBlock(crownProps(), true));
    public static final DeferredBlock<net.celestiald.dontstarve.world.BurntEvergreenBlock> EVERGREEN_BURNT =
            BLOCKS.register("evergreen_burnt", () -> new net.celestiald.dontstarve.world.BurntEvergreenBlock(treeProps().mapColor(MapColor.COLOR_BLACK), false));
    public static final DeferredBlock<net.celestiald.dontstarve.world.BurntEvergreenBlock> EVERGREEN_SPARSE_BURNT =
            BLOCKS.register("evergreen_sparse_burnt", () -> new net.celestiald.dontstarve.world.BurntEvergreenBlock(treeProps().mapColor(MapColor.COLOR_BLACK), true));
    public static final DeferredBlock<net.celestiald.dontstarve.world.EvergreenStumpBlock> EVERGREEN_STUMP =
            BLOCKS.register("evergreen_stump", () -> new net.celestiald.dontstarve.world.EvergreenStumpBlock(treeProps()));
    public static final DeferredBlock<net.celestiald.dontstarve.world.EvergreenStumpBlock> EVERGREEN_SPARSE_STUMP =
            BLOCKS.register("evergreen_sparse_stump", () -> new net.celestiald.dontstarve.world.EvergreenStumpBlock(treeProps()));
    public static final DeferredBlock<net.celestiald.dontstarve.world.TreeSaplingBlock> PINECONE_SAPLING =
            BLOCKS.register("pinecone_sapling", () -> new net.celestiald.dontstarve.world.TreeSaplingBlock(saplingProps(), false));
    public static final DeferredBlock<net.celestiald.dontstarve.world.TreeSaplingBlock> LUMPY_SAPLING =
            BLOCKS.register("lumpy_sapling", () -> new net.celestiald.dontstarve.world.TreeSaplingBlock(saplingProps(), true));

    /** Trees are worked by DS chops (EvergreenBlock), not by Minecraft's mining speed. */
    private static BlockBehaviour.Properties treeProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .instrument(NoteBlockInstrument.BASS)
                .strength(2.0F)
                .sound(SoundType.WOOD);
    }

    private static BlockBehaviour.Properties crownProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .strength(0.2F)
                .sound(SoundType.GRASS)
                .noOcclusion()
                .isViewBlocking((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false);
    }

    private static BlockBehaviour.Properties saplingProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .strength(0.0F)
                .sound(SoundType.GRASS)
                .pushReaction(PushReaction.DESTROY);
    }

    // DS-3 forest flora.
    /** Berry bush (DST "berrybush", berrybush.lua): berries, BERRY_REGROW_TIME 1440 s + 240 s per used cycle + up to 960 s, BERRYBUSH_CYCLES 3 + random(2); large burnable (30 s), medium propagator. */
    public static final DeferredBlock<PickableBlock> BERRYBUSH =
            BLOCKS.register("berrybush", DSBlocks::berryBush);

    /** The second look of the berry bush (DST "berrybush2"); same behaviour, dig gives dug_berrybush2. */
    public static final DeferredBlock<PickableBlock> BERRYBUSH2 =
            BLOCKS.register("berrybush2", DSBlocks::berryBush);

    /** Planted carrot (DST "carrot_planted", carrot.lua): removed when picked, small burnable (10 s). */
    public static final DeferredBlock<PickableBlock> CARROT_PLANTED =
            BLOCKS.register("carrot_planted", () -> new PickableBlock(plantProps(), DSItems.CARROT, 0, 0)
                    .removedWhenPicked().regrowth("carrot_planted").notDiggable()
                    .burnable(10.0F, net.celestiald.dontstarve.fire.DSPropagator.SMALL));

    /** Flower (DST "flower", flower.lua): petals, removed when picked, small burnable (10 s). */
    public static final DeferredBlock<net.celestiald.dontstarve.world.FlowerBlock> FLOWER =
            BLOCKS.register("flower", () -> {
                net.celestiald.dontstarve.world.FlowerBlock flower =
                        new net.celestiald.dontstarve.world.FlowerBlock(plantProps(), DSItems.PETALS);
                flower.removedWhenPicked().regrowth("flower").notDiggable()
                        .burnable(10.0F, net.celestiald.dontstarve.fire.DSPropagator.SMALL);
                return flower;
            });

    /** Reeds (DST "reeds", reeds.lua): cutreeds every REEDS_REGROW_TIME 1440 s, not diggable, burnable for SMALL_FUEL 15 s. */
    public static final DeferredBlock<PickableBlock> REEDS =
            BLOCKS.register("reeds", () -> new PickableBlock(plantProps(), DSItems.CUTREEDS, 1440, 0)
                    .regrowth("reeds").notDiggable()
                    .burnable(15.0F, net.celestiald.dontstarve.fire.DSPropagator.SMALL));

    private static PickableBlock berryBush() {
        return new PickableBlock(plantProps(), DSItems.BERRIES, 1440, 3).extraCycles(2)
                .regrow((passed, random) -> net.celestiald.dontstarve.DSScale.ticks(
                        1440.0 + 240.0 * passed + 960.0 * random.nextFloat()))
                .burnable(30.0F, net.celestiald.dontstarve.fire.DSPropagator.MEDIUM);
    }

    private static BlockBehaviour.Properties plantProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .strength(0.3F)
                .sound(SoundType.GRASS)
                .offsetType(BlockBehaviour.OffsetType.XZ)
                .pushReaction(PushReaction.DESTROY);
    }

    /** Boulders take ROCKS_MINE 6 pickaxe hits (BoulderBlock). */
    private static BlockBehaviour.Properties boulderProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .instrument(NoteBlockInstrument.BASEDRUM)
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
