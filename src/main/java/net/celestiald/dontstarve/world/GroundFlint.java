package net.celestiald.dontstarve.world;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.celestiald.dontstarve.DSRefs;
import net.celestiald.dontstarve.init.DSAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * DST worldgen scatters {@code flint} items on the ground (distributeprefabs flint in the forest
 * and grass rooms). Entities cannot be created while a chunk is generated, so the first time an
 * islands chunk is loaded it is queued and, on the next server tick, flint items are dropped on
 * the positions of {@link IslandChunkGenerator#groundFlint}. Loose flint never despawns (a DST
 * ground item does not time out), and each chunk is marked so it is scattered once.
 */
public final class GroundFlint {

    private static final Queue<ChunkPos> PENDING = new ConcurrentLinkedQueue<>();

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide()
                && level.dimension().equals(DSRefs.ISLANDS)
                && !event.getChunk().getData(DSAttachments.FLINT_SCATTERED.get())) {
            PENDING.add(event.getChunk().getPos());
        }
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel islands = event.getServer().getLevel(DSRefs.ISLANDS);
        if (islands == null) {
            PENDING.clear();
            return;
        }
        for (ChunkPos pos = PENDING.poll(); pos != null; pos = PENDING.poll()) {
            LevelChunk chunk = islands.getChunkSource().getChunkNow(pos.x, pos.z);
            if (chunk == null || chunk.getData(DSAttachments.FLINT_SCATTERED.get())) {
                continue;
            }
            chunk.setData(DSAttachments.FLINT_SCATTERED.get(), true);
            for (BlockPos at : IslandChunkGenerator.groundFlint(pos)) {
                ItemEntity item = new ItemEntity(islands, at.getX() + 0.5, at.getY(), at.getZ() + 0.5,
                        new ItemStack(Items.FLINT));
                item.setUnlimitedLifetime();
                item.setDeltaMovement(0.0, 0.0, 0.0);
                islands.addFreshEntity(item);
            }
        }
    }

    private GroundFlint() {
    }
}
