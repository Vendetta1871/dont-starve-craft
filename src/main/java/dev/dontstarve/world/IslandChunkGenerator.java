package dev.dontstarve.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dontstarve.init.DSBlocks;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.biome.BiomeManager;import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
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
                for (int y = 0; y <= surface; y++) {
                    cursor.set(wx, y, wz);
                    chunk.setBlockState(cursor, columnBlock(wx, wz, y, surface), true);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
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
