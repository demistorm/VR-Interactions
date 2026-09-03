package win.demistorm.vr_interactions.client.render;

import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3dc;
import win.demistorm.vr_interactions.VRInteractions;
import win.demistorm.vr_interactions.client.VRAbstraction;
import win.demistorm.vr_interactions.client.feature.horse.HorseRingsFeature;
import win.demistorm.vr_interactions.client.feature.horse.RideCommand;

public final class HorseRingsDebugRenderer {

    private static final int COLOR_IDLE = 0xFF9E9E9E;
    private static final int COLOR_TURNING = 0xFFFF9E00;
    private static final int COLOR_MOVING = 0xFF33FF33;

    private HorseRingsDebugRenderer() {}

    public static void tick() {
        if (!VRInteractions.debugMode || !VRAbstraction.isVRActive()) {
            return;
        }
        AbstractHorse horse = HorseRingsFeature.debugHorse();
        RideCommand cmd = horse == null ? null : HorseRingsFeature.commandFor(horse);
        if (horse == null || cmd == null || !cmd.active()) {
            return;
        }

        double yawRad = Math.toRadians(horse.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
        Vec3 billboardPos = horse.position().add(forward.scale(1.8)).add(0.0, 1.5, 0.0);
        int stateColor = cmd.turnEngaged() ? COLOR_TURNING : cmd.tier() > 0 ? COLOR_MOVING : COLOR_IDLE;

        String line1 = "HORSE " + tierName(cmd.tier()) + " sprint=" + cmd.sprintTicksRemaining()
                + " hands=" + cmd.activeHands();
        String line2 = "lat=" + String.format("%+.2f", cmd.lateralOffset())
                + (cmd.turnEngaged() ? " TURN" : cmd.sustainRemaining() > 0 ? " arm=" + cmd.sustainRemaining() : "")
                + (cmd.stopRampRemaining() > 0 ? " stop=" + cmd.stopRampRemaining() : "")
                + (cmd.jumpFired() ? " JUMP" : cmd.jumpPending() > 0 ? " jmp=" + cmd.jumpPending() : "")
                + (cmd.activeHands() == 0 ? " decay=" + cmd.decayTicksRemaining() : "");

        Gizmos.billboardText(line1, billboardPos, TextGizmo.Style.forColorAndCentered(stateColor).withScale(0.35f))
                .setAlwaysOnTop();
        Gizmos.billboardText(line2, billboardPos.add(0.0, -0.35, 0.0), TextGizmo.Style.forColorAndCentered(COLOR_IDLE).withScale(0.35f))
                .setAlwaysOnTop();

        Vector3dc handAvg = HorseRingsFeature.debugHandAvg();
        if (handAvg != null) {
            Gizmos.line(toVec3(handAvg), horse.position(), stateColor);
        }
    }

    private static String tierName(int tier) {
        return switch (tier) {
            case 1 -> "WALK";
            case 2 -> "TROT";
            case 3 -> "GALLOP";
            default -> "STOP";
        };
    }

    private static Vec3 toVec3(Vector3dc vec) {
        return new Vec3(vec.x(), vec.y(), vec.z());
    }
}
