package net.celestiald.dontstarve.fire;

import net.celestiald.dontstarve.DSRefs;
import net.celestiald.dontstarve.init.DSBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The fueled component (components/fueled.lua) of a campfire or fire pit: current fuel in DS
 * seconds, consumed at rate 1 per DS second while the fire burns (the rain rate is not ported:
 * the mod has no rain yet), fuel sections, and the section callbacks of campfire.lua / firepit.lua
 * (relight, extinguish, charcoal queueing, burning out).
 */
public class FueledFireBlockEntity extends BlockEntity {

    /** fueled.lua period: fuel is consumed once per DS second. */
    private static final int PERIOD_TICKS = 50;

    /** Current fuel in DS seconds (fueled.currentfuel). */
    private double fuel = -1;
    /** The section last reported to the section callback. */
    private int lastSection;
    /** campfire.lua queued_charcoal: the fire reached the top section, so it leaves charcoal. */
    private boolean queuedCharcoal;
    private int acc;
    private long lastTick = -1;
    private boolean started;

    public FueledFireBlockEntity(BlockPos pos, BlockState state) {
        super(DSBlockEntities.FUELED_FIRE.get(), pos, state);
    }

    private FueledFireBlock.Kind kind() {
        return ((FueledFireBlock) getBlockState().getBlock()).kind;
    }

    public double fuel() {
        return fuel < 0 ? kind().startFuel : fuel;
    }

    public double maxFuel() {
        return kind().maxFuel;
    }

    /** fueled:GetCurrentSection. */
    public int section() {
        double f = fuel();
        if (f <= 0) {
            return 0;
        }
        return Math.min((int) Math.floor(f / maxFuel() * FueledFireBlock.SECTIONS) + 1, FueledFireBlock.SECTIONS);
    }

    /** fueled:GetSectionPercent. */
    public double sectionPercent() {
        return Math.max(0.0, fuel() / maxFuel() * FueledFireBlock.SECTIONS - section() + 1);
    }

    public boolean queuedCharcoal() {
        return queuedCharcoal;
    }

    /** fueled:TakeFuelItem: fuel value x bonusmult is added, clamped at the maximum. */
    public boolean takeFuel(ServerLevel level, BlockPos pos, double fuelValue) {
        if (fuel < 0) {
            fuel = kind().startFuel;
        }
        fuel = Math.max(0, Math.min(maxFuel(), fuel + fuelValue * kind().bonusMult));
        refresh(level, pos);
        return true;
    }

    /** fueled:InitializeFuelLevel. */
    public void initializeFuel(ServerLevel level, BlockPos pos, double value) {
        fuel = value;
        refresh(level, pos);
    }

    /** Test and debug: set the fuel directly (DoDelta). */
    public void setFuel(ServerLevel level, BlockPos pos, double value) {
        fuel = Math.max(0, Math.min(maxFuel(), value));
        refresh(level, pos);
    }

    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (!level.dimension().equals(DSRefs.ISLANDS)) {
            return;
        }
        long now = level.getGameTime();
        if (!started) {
            started = true;
            if (fuel < 0) {
                fuel = kind().startFuel; // InitializeFuelLevel(START) reports the first section
            } else if (lastTick >= 0 && now > lastTick && fuel > 0) {
                // The fire kept burning while its chunk was unloaded (fueled LongUpdate).
                fuel = Math.max(0, fuel - (now - lastTick) * DSFire.DS_PER_TICK);
            }
            refresh(level, pos);
            if (level.getBlockEntity(pos) != this) {
                return;
            }
            if (section() > 0 && !DSFire.isBurning(level, pos)) {
                DSFire.ignite(level, pos, null);
            }
        }
        lastTick = now;
        if (fuel > 0 && ++acc >= PERIOD_TICKS) {
            acc = 0;
            fuel = Math.max(0, fuel - 1.0);
            refresh(level, pos);
            setChanged();
        }
    }

    /** Re-reads the section and light from the fuel, updates the block state and runs the section callback. */
    private void refresh(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FueledFireBlock block)) {
            return;
        }
        int section = section();
        BlockState want = state.setValue(FueledFireBlock.SECTION, section)
                .setValue(FueledFireBlock.LIGHT, FueledFireBlock.lightFor(section, sectionPercent()));
        if (want != state) {
            level.setBlock(pos, want, Block.UPDATE_ALL);
        }
        setChanged();
        if (section != lastSection) {
            int old = lastSection;
            lastSection = section;
            onSectionChange(level, pos, block, section, old);
        }
    }

    /** campfire.lua / firepit.lua onfuelchange. */
    private void onSectionChange(ServerLevel level, BlockPos pos, FueledFireBlock block, int section, int old) {
        if (section <= 0) {
            DSFire.extinguish(level, pos);
            if (block.kind.burnsOut) {
                // The campfire leaves charcoal or ash and is gone.
                FueledFireBlock.dropCampfireLeftovers(level, pos, queuedCharcoal);
                level.removeBlock(pos, false);
            } else if (queuedCharcoal) {
                // The fire pit spits out its charcoal and stays.
                FueledFireBlock.dropFirepitCharcoal(level, pos);
            }
            queuedCharcoal = false;
        } else {
            if (!DSFire.isBurning(level, pos)) {
                DSFire.ignite(level, pos, null);
            }
            if (section == FueledFireBlock.SECTIONS) {
                queuedCharcoal = true;
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("fuel", fuel());
        tag.putInt("section", lastSection);
        tag.putBoolean("queued_charcoal", queuedCharcoal);
        tag.putLong("last_tick", lastTick);
        tag.putInt("acc", acc);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("fuel")) {
            fuel = tag.getDouble("fuel");
            lastSection = tag.getInt("section");
            queuedCharcoal = tag.getBoolean("queued_charcoal");
            lastTick = tag.getLong("last_tick");
            acc = tag.getInt("acc");
        }
    }
}
