package net.celestiald.dontstarve.world;

import net.celestiald.dontstarve.init.DSBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import javax.annotation.Nullable;

/**
 * Spider Den (DST "spiderden"): a landmark that keeps the island stocked with
 * spiders, the source of glands (reviver) and string (silk stand-in). Like DST
 * it only maintains a small population nearby instead of spawning endlessly.
 */
public class SpiderDenBlock extends Block implements EntityBlock {

    /** DST spiderden keeps roughly this many spiders around the den. */
    private static final int MAX_SPIDERS_NEARBY = 3;
    /** Roughly every 40 seconds the den checks its larder. */
    private static final int CHECK_INTERVAL_TICKS = 40 * 20;
    private static final int POPULATION_RADIUS = 12;

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
                : (lvl, pos, st, be) -> SpiderDenBlockEntity.tick(lvl, pos, st, (SpiderDenBlockEntity) be);
    }

    public static class SpiderDenBlockEntity extends BlockEntity {

        private int cooldown = CHECK_INTERVAL_TICKS / 2;

        public SpiderDenBlockEntity(BlockPos pos, BlockState state) {
            super(DSBlockEntities.SPIDER_DEN.get(), pos, state);
        }

        public static void tick(Level level, BlockPos pos, BlockState state, SpiderDenBlockEntity den) {
            if (!(level instanceof ServerLevel server) || --den.cooldown > 0) {
                den.setChanged();
                return;
            }
            den.cooldown = CHECK_INTERVAL_TICKS;
            den.setChanged();
            if (countSpiders(server, pos) >= MAX_SPIDERS_NEARBY) {
                return;
            }
            RandomSource random = server.getRandom();
            BlockPos spawnPos = findSpawnSpot(server, pos, random);
            if (spawnPos == null) {
                return;
            }
            Spider spider = EntityType.SPIDER.create(server);
            if (spider == null) {
                return;
            }
            spider.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                    random.nextFloat() * 360.0F, 0.0F);
            spider.finalizeSpawn(server, server.getCurrentDifficultyAt(spawnPos),
                    MobSpawnType.SPAWNER, null);
            // DST den spiders belong to the den, not to the despawn rules.
            spider.setPersistenceRequired();
            server.addFreshEntityWithPassengers(spider);
        }

        private static int countSpiders(ServerLevel level, BlockPos center) {
            AABB area = new AABB(center).inflate(POPULATION_RADIUS);
            return level.getEntitiesOfClass(Spider.class, area, LivingEntity::isAlive).size();
        }

        @Nullable
        private static BlockPos findSpawnSpot(ServerLevel level, BlockPos den, RandomSource random) {
            for (int attempt = 0; attempt < 8; attempt++) {
                BlockPos candidate = den.offset(
                        random.nextInt(POPULATION_RADIUS) - POPULATION_RADIUS / 2,
                        random.nextInt(3) - 1,
                        random.nextInt(POPULATION_RADIUS) - POPULATION_RADIUS / 2);
                if (candidate.equals(den)) {
                    continue;
                }
                BlockState floor = level.getBlockState(candidate.below());
                BlockState spot = level.getBlockState(candidate);
                BlockState head = level.getBlockState(candidate.above());
                if (floor.isSolidRender(level, candidate.below())
                        && (spot.isAir() || spot.canBeReplaced())
                        && head.isAir()) {
                    return candidate;
                }
            }
            return null;
        }
    }
}
