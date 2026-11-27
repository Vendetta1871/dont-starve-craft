package net.celestiald.dontstarve.init;

import java.util.function.Supplier;
import net.celestiald.dontstarve.DontStarveCraft;
import net.celestiald.dontstarve.entity.DSSpider;
import net.celestiald.dontstarve.entity.Hound;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DSEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, DontStarveCraft.MODID);

    /** Hound (DST "hound"): the hound-wave hunter. */
    public static final Supplier<EntityType<Hound>> HOUND = ENTITIES.register("hound",
            () -> EntityType.Builder.of(Hound::new, MobCategory.MONSTER)
                    .sized(0.7F, 0.85F)
                    .clientTrackingRange(10)
                    .build("hound"));

    /** Spider (DST "spider"): the spider den's children. */
    public static final Supplier<EntityType<DSSpider>> SPIDER = ENTITIES.register("spider",
            () -> EntityType.Builder.of(DSSpider::new, MobCategory.MONSTER)
                    .sized(1.4F, 0.9F)
                    .clientTrackingRange(8)
                    .build("spider"));

    private DSEntities() {
    }
}
