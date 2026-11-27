package net.celestiald.dontstarve.init;

import com.mojang.serialization.Codec;
import net.celestiald.dontstarve.DontStarveCraft;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public final class DSDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, DontStarveCraft.MODID);

    /** Experience sealed inside a grave; survives break/replace via copy_components. */
    public static final Supplier<DataComponentType<Integer>> GRAVE_XP = DATA_COMPONENTS.register("grave_xp",
            () -> new DataComponentType.Builder<Integer>()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
                    .build());

    /** DS seconds of freshness left (components/perishable.lua perishremainingtime); absent = fully fresh. */
    public static final Supplier<DataComponentType<Float>> PERISH = DATA_COMPONENTS.register("perish",
            () -> new DataComponentType.Builder<Float>()
                    .persistent(Codec.FLOAT)
                    .networkSynchronized(ByteBufCodecs.FLOAT)
                    .build());

    private DSDataComponents() {
    }
}
