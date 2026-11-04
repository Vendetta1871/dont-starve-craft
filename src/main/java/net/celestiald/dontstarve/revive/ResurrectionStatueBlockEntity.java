package net.celestiald.dontstarve.revive;

import java.util.UUID;
import javax.annotation.Nullable;
import net.celestiald.dontstarve.init.DSBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Remembers which living player attuned this Meat Effigy with their blood.
 * Only that player's ghost may resurrect here (DST resurrectionstatue.lua).
 */
public class ResurrectionStatueBlockEntity extends BlockEntity {

    @Nullable
    private UUID attunedTo;
    @Nullable
    private String attunedName;

    public ResurrectionStatueBlockEntity(BlockPos pos, BlockState state) {
        super(DSBlockEntities.RESURRECTION_STATUE.get(), pos, state);
    }

    @Nullable
    public UUID getAttunedTo() {
        return attunedTo;
    }

    @Nullable
    public String getAttunedName() {
        return attunedName;
    }

    public void setAttunement(@Nullable UUID player, @Nullable String name) {
        attunedTo = player;
        attunedName = name;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (attunedTo != null) {
            tag.putUUID("AttunedTo", attunedTo);
            if (attunedName != null) {
                tag.putString("AttunedName", attunedName);
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        attunedTo = tag.hasUUID("AttunedTo") ? tag.getUUID("AttunedTo") : null;
        attunedName = tag.contains("AttunedName") ? tag.getString("AttunedName") : null;
    }
}
