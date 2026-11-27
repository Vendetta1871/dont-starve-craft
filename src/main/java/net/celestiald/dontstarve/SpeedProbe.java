package net.celestiald.dontstarve;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Test tool behind {@code /ds speed <mob> <x> <z> <ticks>}: the mob walks towards
 * (x, z) through its own navigation at speed modifier 1.0, and after a warm-up its
 * real horizontal speed over {@code ticks} is logged. Calibrates movement-speed
 * attributes against DST speeds (DS units/s = blocks/s at our scale).
 */
public final class SpeedProbe {

    /** Ticks to reach full speed before measuring. */
    private static final int WARMUP = 20;

    private static final class Probe {
        final Mob mob;
        final double x;
        final double z;
        final int ticks;
        int age;
        Vec3 start;

        Probe(Mob mob, double x, double z, int ticks) {
            this.mob = mob;
            this.x = x;
            this.z = z;
            this.ticks = ticks;
        }
    }

    private static final List<Probe> PROBES = new ArrayList<>();

    public static void start(Mob mob, double x, double z, int ticks) {
        PROBES.add(new Probe(mob, x, z, ticks));
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        for (Iterator<Probe> it = PROBES.iterator(); it.hasNext(); ) {
            Probe probe = it.next();
            probe.mob.setTarget(null);
            probe.mob.getNavigation().moveTo(probe.x, probe.mob.getY(), probe.z, 1.0);
            probe.age++;
            if (probe.age == WARMUP) {
                probe.start = probe.mob.position();
            }
            if (probe.age < WARMUP + probe.ticks) {
                continue;
            }
            it.remove();
            Vec3 end = probe.mob.position();
            double dx = end.x - probe.start.x;
            double dz = end.z - probe.start.z;
            double blocks = Math.sqrt(dx * dx + dz * dz);
            double attribute = probe.mob.getAttributeValue(
                    net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            server.sendSystemMessage(Component.literal(String.format(Locale.ROOT,
                    "%s (speed attribute %.3f) moved %.2f blocks in %d ticks: %.3f blocks/s",
                    probe.mob.getName().getString(), attribute, blocks, probe.ticks,
                    blocks * 20.0 / probe.ticks)));
        }
    }

    private SpeedProbe() {
    }
}
