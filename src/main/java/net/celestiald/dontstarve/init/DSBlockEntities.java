package net.celestiald.dontstarve.init;

import net.celestiald.dontstarve.DontStarveCraft;
import net.celestiald.dontstarve.grave.GraveBlockEntity;
import net.celestiald.dontstarve.revive.ResurrectionStatueBlockEntity;
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

    private DSBlockEntities() {
    }
}
