package net.celestiald.dontstarve.init;

import net.celestiald.dontstarve.DontStarveCraft;
import net.celestiald.dontstarve.grave.GraveBlockEntity;
import net.celestiald.dontstarve.revive.ResurrectionStatueBlockEntity;
import net.celestiald.dontstarve.world.SpiderDenBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public final class DSBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DontStarveCraft.MODID);

    public static final Supplier<BlockEntityType<GraveBlockEntity>> GRAVE = BLOCK_ENTITIES.register("grave",
            () -> BlockEntityType.Builder.of(GraveBlockEntity::new, DSBlocks.GRAVE.get()).build(null));

    public static final Supplier<BlockEntityType<ResurrectionStatueBlockEntity>> RESURRECTION_STATUE =
            BLOCK_ENTITIES.register("resurrectionstatue",
                    () -> BlockEntityType.Builder.of(ResurrectionStatueBlockEntity::new,
                            DSBlocks.RESURRECTION_STATUE.get()).build(null));

    public static final Supplier<BlockEntityType<SpiderDenBlock.SpiderDenBlockEntity>> SPIDER_DEN =
            BLOCK_ENTITIES.register("spider_den",
                    () -> BlockEntityType.Builder.of(SpiderDenBlock.SpiderDenBlockEntity::new,
                            DSBlocks.SPIDER_DEN.get()).build(null));

    public static final Supplier<BlockEntityType<net.celestiald.dontstarve.fire.FueledFireBlockEntity>> FUELED_FIRE =
            BLOCK_ENTITIES.register("fueled_fire",
                    () -> BlockEntityType.Builder.of(net.celestiald.dontstarve.fire.FueledFireBlockEntity::new,
                            DSBlocks.CAMPFIRE.get(), DSBlocks.FIREPIT.get()).build(null));

    private DSBlockEntities() {
    }
}
