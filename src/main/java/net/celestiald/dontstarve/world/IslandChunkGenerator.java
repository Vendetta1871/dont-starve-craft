package net.celestiald.dontstarve.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.celestiald.dontstarve.init.DSBlocks;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * Generates small Don't Starve islands as discs floating in the void, one per
 * run, centered at {@code x = index * IslandState.SPACING, z = 0}. A flat
 * polished-marble plaza at a fixed height sits in the center so the arrival
 * position is deterministic without querying heightmaps.
 */
public final class IslandChunkGenerator extends ChunkGenerator {

    public static final MapCodec<IslandChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(gen -> gen.biomeSource)
            ).apply(instance, IslandChunkGenerator::new));

    public IslandChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState,
                                                       StructureManager structureManager, ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int wx = chunkPos.getMinBlockX() + lx;
                int wz = chunkPos.getMinBlockZ() + lz;
                int surface = islandSurfaceHeight(wx, wz);
                if (surface < 0) {
                    continue;
                }
                long index = Math.round((double) wx / IslandState.SPACING);
                double meteorDist = distanceToMeteorite(index, wx, wz);
                int top = meteorDist <= 1.0 ? surface + 1 : surface;
                for (int y = 0; y <= top; y++) {
                    BlockState meteorite = meteoriteBlock(meteorDist, y, surface);
                    BlockState state = meteorite != null ? meteorite
                            : (y <= surface ? columnBlock(wx, wz, y, surface) : null);
                    if (state == null) {
                        continue;
                    }
                    cursor.set(wx, y, wz);
                    chunk.setBlockState(cursor, state, true);
                }
                // Surface scatter: DS grass tufts and boulders.
                if (surface >= 0 && isGrassSurface(wx, wz, surface)) {
                    BlockState scatter = scatterBlock(wx, wz, surface);
                    if (scatter != null) {
                        cursor.set(wx, surface + 1, wz);
                        chunk.setBlockState(cursor, scatter, true);
                    }
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    /**
     * Every island carries one moon meteorite — the only source of Moon Rock,
     * and therefore of the escape route. Its center is deterministic per island
     * index (golden-angle spiral, 40-64 blocks from the plaza, clear of the
     * beach ring).
     */
    private static BlockPos meteoriteCenter(long islandIndex) {
        double angle = islandIndex * 2.39996323;
        double dist = 40.0 + hashNoise((int) (islandIndex * 7919L), 1337) * 24.0;
        return new BlockPos((int) Math.round(islandIndex * IslandState.SPACING + Math.cos(angle) * dist),
                0, (int) Math.round(Math.sin(angle) * dist));
    }

    private static double distanceToMeteorite(long islandIndex, int wx, int wz) {
        BlockPos center = meteoriteCenter(islandIndex);
        double dx = wx - center.getX();
        double dz = wz - center.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * The meteorite impact: DST large meteors leave "rock_moon" boulders
     * (shadowmeteor.lua:245-256), here a small cluster on the surface.
     */
    @Nullable
    private static BlockState meteoriteBlock(double dist, int y, int surface) {
        return dist <= 1.0 && y == surface + 1 ? DSBlocks.ROCK_MOON.get().defaultBlockState() : null;
    }

    /** Y of the top solid block of the island column, or -1 for void. */
    private static int islandSurfaceHeight(int wx, int wz) {
        long index = Math.round((double) wx / IslandState.SPACING);
        double dx = wx - index * (double) IslandState.SPACING;
        double radius = Math.sqrt(dx * dx + (double) wz * wz);
        if (radius > IslandState.RADIUS) {
            return -1;
        }
        double t = radius / IslandState.RADIUS;
        // Flat spawn area, then a gentle dome with a little noise.
        double ramp = Mth.clamp((radius - 24.0) / 24.0, 0.0, 1.0);
        ramp = ramp * ramp * (3.0 - 2.0 * ramp);
        return (int) Math.floor(IslandState.PLAZA_Y
                + (12.0 * (1.0 - t * t) + hashNoise(wx, wz) * 3.0) * ramp);
    }

    private static BlockState columnBlock(int wx, int wz, int y, int surface) {
        long index = Math.round((double) wx / IslandState.SPACING);
        double dx = wx - index * (double) IslandState.SPACING;
        double radius = Math.sqrt(dx * dx + (double) wz * wz);
        boolean beach = radius > IslandState.RADIUS * 0.85;
        boolean plaza = radius <= 5.0;
        if (y == surface) {
            if (plaza) {
                return DSBlocks.POLISHED_MARBLE.get().defaultBlockState();
            }
            return beach ? Blocks.SAND.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
        }
        if (y >= surface - 3) {
            return beach ? Blocks.SANDSTONE.defaultBlockState() : Blocks.DIRT.defaultBlockState();
        }
        return Blocks.STONE.defaultBlockState();
    }

    /** Cheap deterministic value noise in [0, 1). */
    private static double hashNoise(int x, int z) {
        long hash = (long) x * 341873128712L + (long) z * 132897987541L;
        hash = (hash ^ (hash >>> 12)) * 1274126177L;
        hash ^= hash >>> 16;
        return (hash & 0xFFFF) / 65536.0;
    }

    private static boolean isGrassSurface(int wx, int wz, int surface) {
        return columnBlock(wx, wz, surface, surface).is(Blocks.GRASS_BLOCK);
    }

    /**
     * Density of the generic grassland room BGGrass (map/rooms/forest/terrain_grass.lua, the room
     * the island meadow stands for) in distributeprefabs units: grass 0.2, berrybush 0.05,
     * carrot_planted 0.05, flower 0.112, flint 0.05 (also rock1/rock2 0.01 each in the burnt variant).
     * The island's grass density of 14 % of grass columns stands for the DST grass 0.2, so every
     * other prefab is scaled by the same factor 0.14 / 0.2 = 0.7 and keeps its DST ratio to grass:
     * berry bush 3.5 %, carrot 3.5 %, flower 7.84 %, flint 3.5 % of the grass columns. Berry
     * bushes are the plain "berrybush" (rooms never place berrybush2); berrybush_juicy (0.025)
     * is not ported.
     */
    private static final double DST_TO_ISLAND = 0.14 / 0.2;
    private static final double GRASS_P = 0.2 * DST_TO_ISLAND;
    private static final double BERRYBUSH_P = 0.05 * DST_TO_ISLAND;
    private static final double CARROT_P = 0.05 * DST_TO_ISLAND;
    private static final double FLOWER_P = 0.112 * DST_TO_ISLAND;
    private static final double FLINT_P = 0.05 * DST_TO_ISLAND;

    /**
     * Single-block surface scatter: DS grass tufts (the cutgrass source), berry bushes, carrots
     * and flowers (one roll split into consecutive bands, see {@link #DST_TO_ISLAND}) and
     * boulders (rocks, flint, nitre; one in five gold-veined).
     */
    @Nullable
    private static BlockState scatterBlock(int wx, int wz, int surface) {
        double radius = radiusToCenter(wx, wz);
        if (radius < 10.0 || radius > IslandState.RADIUS - 4.0) {
            return null; // keep the plaza and the beach shore clean
        }
        double roll = hashNoise(wx * 5 + 11, wz * 5 + 23);
        if (roll < GRASS_P) {
            return DSBlocks.GRASS.get().defaultBlockState();
        }
        roll -= GRASS_P;
        if (roll < BERRYBUSH_P) {
            return DSBlocks.BERRYBUSH.get().defaultBlockState();
        }
        roll -= BERRYBUSH_P;
        if (roll < CARROT_P) {
            return DSBlocks.CARROT_PLANTED.get().defaultBlockState();
        }
        roll -= CARROT_P;
        if (roll < FLOWER_P) {
            return DSBlocks.FLOWER.get().randomVariant(RandomSource.create(wx * 31L + wz * 7919L));
        }
        if (hashNoise(wx * 5 + 11, wz * 5 + 23) > 0.992) {
            return (hashNoise(wx * 7 + 3, wz * 7 + 1) < 0.2 ? DSBlocks.ROCK2 : DSBlocks.ROCK1).get().defaultBlockState();
        }
        return null;
    }

    /**
     * Where DST would scatter loose flint (distributeprefabs flint, {@link #FLINT_P} of the grass
     * columns that hold no other scatter), as item positions on top of the surface of one chunk.
     */
    public static List<BlockPos> groundFlint(ChunkPos chunkPos) {
        List<BlockPos> flint = new java.util.ArrayList<>();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int wx = chunkPos.getMinBlockX() + lx;
                int wz = chunkPos.getMinBlockZ() + lz;
                int surface = islandSurfaceHeight(wx, wz);
                if (surface < 0 || !isGrassSurface(wx, wz, surface) || scatterBlock(wx, wz, surface) != null) {
                    continue;
                }
                double radius = radiusToCenter(wx, wz);
                if (radius >= 10.0 && radius <= IslandState.RADIUS - 4.0 && hashNoise(wx * 3 + 17, wz * 3 + 41) < FLINT_P) {
                    flint.add(new BlockPos(wx, surface + 1, wz));
                }
            }
        }
        return flint;
    }

    private static double radiusToCenter(int wx, int wz) {
        long index = Math.round((double) wx / IslandState.SPACING);
        double dx = wx - index * (double) IslandState.SPACING;
        return Math.sqrt(dx * dx + (double) wz * wz);
    }

    /**
     * Trees and spider dens live here rather than in fillFromNoise because
     * their canopies cross chunk borders; the decoration stage can see the
     * neighbouring chunks. Placement is deterministic per grid cell, so every
     * chunk decorates exactly its own candidates.
     */
    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structures) {
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        // Trees: one candidate per 7x7 cell, kept fully inside the chunk.
        int cell = 7;
        int startX = chunkPos.getMinBlockX();
        int startZ = chunkPos.getMinBlockZ();
        int firstCellX = Math.floorDiv(startX, cell);
        int firstCellZ = Math.floorDiv(startZ, cell);
        for (int cx = firstCellX; cx * cell < startX + 16; cx++) {
            for (int cz = firstCellZ; cz * cell < startZ + 16; cz++) {
                int jitterX = (int) (hashNoise(cx * 31 + 7, cz * 13 + 3) * cell);
                int jitterZ = (int) (hashNoise(cx * 17 + 91, cz * 29 + 5) * cell);
                int wx = cx * cell + jitterX;
                int wz = cz * cell + jitterZ;
                if (wx < startX || wx >= startX + 16 || wz < startZ || wz >= startZ + 16) {
                    continue; // belongs to (and is placed by) the other chunk
                }
                if (hashNoise(cx * 53 + 1, cz * 71 + 2) > 0.45) {
                    continue; // ~45 % of cells grow a tree
                }
                placeSaplingOrTree(level, cursor, wx, wz);
            }
        }
        // Spider dens: sparse landmark grid, away from the plaza.
        int denCell = 48;
        for (int cx = Math.floorDiv(startX, denCell); cx * denCell < startX + 16; cx++) {
            for (int cz = Math.floorDiv(startZ, denCell); cz * denCell < startZ + 16; cz++) {
                if (hashNoise(cx * 977 + 13, cz * 613 + 29) > 0.4) {
                    continue;
                }
                int wx = cx * denCell + (int) (hashNoise(cx * 3 + 1, cz * 3 + 2) * denCell);
                int wz = cz * denCell + (int) (hashNoise(cx * 5 + 3, cz * 5 + 4) * denCell);
                if (wx < startX || wx >= startX + 16 || wz < startZ || wz >= startZ + 16) {
                    continue; // a den cell spans several chunks; only the owning one places it
                }
                double radius = radiusToCenter(wx, wz);
                if (radius < 30.0 || radius > IslandState.RADIUS - 10.0) {
                    continue;
                }
                int surface = islandSurfaceHeight(wx, wz);
                if (surface < 0 || !columnBlock(wx, wz, surface, surface).is(Blocks.GRASS_BLOCK)) {
                    continue;
                }
                cursor.set(wx, surface + 1, wz);
                if (level.getBlockState(cursor).isAir()) {
                    level.setBlock(cursor, DSBlocks.SPIDER_DEN.get().defaultBlockState(),
                            Block.UPDATE_KNOWN_SHAPE);
                }
            }
        }
    }

    /** An evergreen most of the time, a twig sapling otherwise (DST forest rooms mix both). */
    private void placeSaplingOrTree(WorldGenLevel level, BlockPos.MutableBlockPos cursor, int wx, int wz) {
        int surface = islandSurfaceHeight(wx, wz);
        if (surface < 0 || !columnBlock(wx, wz, surface, surface).is(Blocks.GRASS_BLOCK)) {
            return;
        }
        double radius = radiusToCenter(wx, wz);
        if (radius < 14.0 || radius > IslandState.RADIUS - 6.0) {
            return;
        }
        // Keep the canopy inside this chunk: decoration may not touch neighbours.
        // floorMod, not %: negative coordinates (the north half of every island)
        // would otherwise always fail the check and grow no trees at all.
        int localX = Math.floorMod(wx, 16);
        int localZ = Math.floorMod(wz, 16);
        if (localX < 3 || localX > 12 || localZ < 3 || localZ > 12) {
            return;
        }
        cursor.set(wx, surface + 1, wz);
        if (!level.getBlockState(cursor).isAir()) {
            return;
        }
        if (hashNoise(wx * 13 + 5, wz * 13 + 9) < 0.75) {
            // DST "evergreen" spawns at a random stage 1..3 (evergreens.lua growable:SetStage(math.random(1, 3)));
            // forest rooms pick between "evergreen" and "evergreen_sparse" (the lumpy build): one in three here.
            int stage = (int) (hashNoise(wx * 19 + 11, wz * 23 + 17) * 3.0) % 3;
            boolean lumpy = hashNoise(wx * 7 + 29, wz * 37 + 31) < 1.0 / 3.0;
            EvergreenTree.place(level, cursor.immutable(), stage, lumpy);
        } else {
            level.setBlock(cursor, DSBlocks.SAPLING.get().defaultBlockState(), Block.UPDATE_KNOWN_SHAPE);
        }
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState,
                             BiomeManager biomeManager, StructureManager structureManager,
                             ChunkAccess chunk, GenerationStep.Carving carving) {
        // No carvers.
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structureManager,
                             RandomState randomState, ChunkAccess chunk) {
        // Blocks are placed directly in fillFromNoise.
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        // No natural mob spawns.
    }

    @Override
    public void createStructures(RegistryAccess registryAccess, ChunkGeneratorStructureState structureState,
                                 StructureManager structureManager, ChunkAccess chunk,
                                 StructureTemplateManager templateManager) {
        // No vanilla structures on the islands.
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        info.add("Island generator");
    }

    @Override
    public int getGenDepth() {
        return 128;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor levelHeight,
                             RandomState randomState) {
        int surface = islandSurfaceHeight(x, z);
        return Math.max(surface + 1, levelHeight.getMinBuildHeight());
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor levelHeight,
                                     RandomState randomState) {
        BlockState[] column = new BlockState[levelHeight.getHeight()];
        int surface = islandSurfaceHeight(x, z);
        for (int i = 0; i < column.length; i++) {
            int y = i + levelHeight.getMinBuildHeight();
            column[i] = y <= surface ? columnBlock(x, z, y, surface) : Blocks.AIR.defaultBlockState();
        }
        return new NoiseColumn(levelHeight.getMinBuildHeight(), column);
    }
}
