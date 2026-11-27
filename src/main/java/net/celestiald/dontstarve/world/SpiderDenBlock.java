package net.celestiald.dontstarve.world;

import javax.annotation.Nullable;
import net.celestiald.dontstarve.entity.DSSpider;
import net.celestiald.dontstarve.init.DSBlockEntities;
import net.celestiald.dontstarve.init.DSEntities;
import net.celestiald.dontstarve.DSScale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Spider Den, tier 1 (DST "spiderden", prefabs/spiderden.lua). A childspawner
 * that holds up to 3 spiders, regrows one every SPIDERDEN_REGEN_TIME 90 s and
 * lets them out one per 5 s only when it is not day (spiderden.lua:526-545,
 * 736-754). At dawn its spiders walk home and are stored again. Hitting the
 * den releases up to 2 defenders; destroying it releases everyone inside.
 * DS seconds are scaled to the Minecraft day (x2.5). Tiers 2-3, growth and
 * the queen are not ported yet.
 */
public class SpiderDenBlock extends Block implements EntityBlock {

    public static final int MAX_CHILDREN = 3;
    private static final int REGEN_TICKS = DSScale.ticks(90);
    private static final int RELEASE_TICKS = DSScale.ticks(5);
    private static final int ON_HIT_RELEASE = 2;

    public SpiderDenBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpiderDenBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide ? null
                : (lvl, pos, st, be) -> SpiderDenBlockEntity.tick(lvl, pos, (SpiderDenBlockEntity) be);
    }

    /** DST: a den under attack sends out defenders. */
    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof SpiderDenBlockEntity den) {
            for (int i = 0; i < ON_HIT_RELEASE && den.inside > 0; i++) {
                den.release(server, pos);
            }
        }
    }

    /** DST OnKilled: a destroyed den releases all its children. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server
                && level.getBlockEntity(pos) instanceof SpiderDenBlockEntity den) {
            while (den.inside > 0) {
                den.release(server, pos);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    /** A den spider coming home at dawn; false when there is no den to take it. */
    public static boolean takeBack(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof SpiderDenBlockEntity den) {
            den.inside = Math.min(MAX_CHILDREN, den.inside + 1);
            den.setChanged();
            return true;
        }
        return false;
    }

    public static class SpiderDenBlockEntity extends BlockEntity {

        /** Spiders stored inside the den; DST childspawners start full. */
        private int inside = MAX_CHILDREN;
        private int regenTimer = REGEN_TICKS;
        private int releaseTimer = RELEASE_TICKS;

        public SpiderDenBlockEntity(BlockPos pos, BlockState state) {
            super(DSBlockEntities.SPIDER_DEN.get(), pos, state);
        }

        public int inside() {
            return inside;
        }

        public static void tick(Level level, BlockPos pos, SpiderDenBlockEntity den) {
            if (!(level instanceof ServerLevel server)) {
                return;
            }
            if (--den.regenTimer <= 0) {
                den.regenTimer = REGEN_TICKS;
                if (den.inside + den.outside(server, pos) < MAX_CHILDREN) {
                    den.inside++;
                    den.setChanged();
                }
            }
            if (--den.releaseTimer <= 0) {
                den.releaseTimer = RELEASE_TICKS;
                if (!server.isDay() && den.inside > 0) {
                    den.release(server, pos);
                }
            }
        }

        private int outside(ServerLevel level, BlockPos pos) {
            return level.getEntitiesOfClass(DSSpider.class, new net.minecraft.world.phys.AABB(pos).inflate(48),
                    spider -> pos.equals(spider.getHome())).size();
        }

        void release(ServerLevel level, BlockPos pos) {
            BlockPos spot = findSpawnSpot(level, pos, level.getRandom());
            DSSpider spider = DSEntities.SPIDER.get().create(level);
            if (spot == null || spider == null) {
                return;
            }
            spider.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5,
                    level.getRandom().nextFloat() * 360.0F, 0.0F);
            spider.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), MobSpawnType.SPAWNER, null);
            spider.setHome(pos.immutable());
            level.addFreshEntity(spider);
            inside--;
            setChanged();
        }

        @Nullable
        private static BlockPos findSpawnSpot(ServerLevel level, BlockPos den, RandomSource random) {
            for (int attempt = 0; attempt < 12; attempt++) {
                BlockPos candidate = den.offset(random.nextInt(5) - 2, random.nextInt(3) - 1, random.nextInt(5) - 2);
                if (candidate.equals(den)) {
                    continue;
                }
                if (level.getBlockState(candidate.below()).isSolidRender(level, candidate.below())
                        && level.getBlockState(candidate).getCollisionShape(level, candidate).isEmpty()
                        && level.getBlockState(candidate.above()).getCollisionShape(level, candidate.above()).isEmpty()) {
                    return candidate;
                }
            }
            return null;
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putInt("Inside", inside);
            tag.putInt("RegenTimer", regenTimer);
            tag.putInt("ReleaseTimer", releaseTimer);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            inside = tag.contains("Inside") ? tag.getInt("Inside") : MAX_CHILDREN;
            regenTimer = tag.contains("RegenTimer") ? tag.getInt("RegenTimer") : REGEN_TICKS;
            releaseTimer = tag.contains("ReleaseTimer") ? tag.getInt("ReleaseTimer") : RELEASE_TICKS;
        }
    }
}
