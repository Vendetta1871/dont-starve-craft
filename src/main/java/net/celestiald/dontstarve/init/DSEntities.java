package net.celestiald.dontstarve.init;

import net.celestiald.dontstarve.DontStarveCraft;
import net.celestiald.dontstarve.entity.HairyHound;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public final class DSEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, DontStarveCraft.MODID);

    /** Hairy Hound (DST "hound"): beard-hair-bearing wave crasher. */
    public static final Supplier<EntityType<HairyHound>> HAIRY_HOUND = ENTITIES.register("hairy_hound",
            () -> EntityType.Builder.of(HairyHound::new, MobCategory.MONSTER)
                    .sized(0.9F, 0.9F)
                    .build("hairy_hound"));

    private DSEntities() {
    }
}
