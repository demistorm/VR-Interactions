package win.demistorm.vr_interactions.client.render;

import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3dc;
import win.demistorm.vr_interactions.VRInteractions;
import win.demistorm.vr_interactions.client.VRAbstraction;
import win.demistorm.vr_interactions.client.feature.horse.HorseRingsFeature;
import win.demistorm.vr_interactions.client.feature.horse.HorseRingsTuning;
import win.demistorm.vr_interactions.client.feature.horse.HorseRingsVisuals;
import win.demistorm.vr_interactions.client.feature.horse.RideCommand;
import win.demistorm.vr_interactions.client.visual.RopeBox;

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
                + (cmd.activeHands() == 0 ? " decay=" + cmd.decayTicksRemaining() : "")
                + " rope=" + HorseRopeRenderer.VERSION;

        Gizmos.billboardText(line1, billboardPos, TextGizmo.Style.forColorAndCentered(stateColor).withScale(0.35f))
                .setAlwaysOnTop();
        Gizmos.billboardText(line2, billboardPos.add(0.0, -0.35, 0.0), TextGizmo.Style.forColorAndCentered(COLOR_IDLE).withScale(0.35f))
                .setAlwaysOnTop();

        Vector3dc handAvg = HorseRingsFeature.debugHandAvg();
        if (handAvg != null) {
            Gizmos.line(toVec3(handAvg), horse.position(), stateColor);
        }

        drawGeometryGizmos();
    }

    private static void drawGeometryGizmos() {
        HorseRingsVisuals visuals = HorseRingsVisuals.active();
        if (visuals == null) {
            return;
        }
        int[] colors = {0xFF22AA22, 0xFF22AAAA, 0xFFAA2222};
        RopeBox[] boxes = visuals.debugBoxes();
        for (int i = 0; i < boxes.length; i++) {
            if (boxes[i] != null) {
                drawBoxEdges(boxes[i], colors[i % colors.length]);
            }
        }
        drawAnchorCross(visuals.debugMouthL(), 0xFFFFFF22);
        drawAnchorCross(visuals.debugMouthR(), 0xFFFFFF22);

        double[] ropePts = visuals.debugRopePoints();
        if (ropePts != null) {
            int left = visuals.handLIndex();
            int right = visuals.handRIndex();
            for (int p = 0; p < ropePts.length / 3; p++) {
                int color = p <= left ? 0xFFFFFF22 : p <= right ? 0xFF22AAAA : 0xFFAA22AA;
                drawPointCross(ropePts, p * 3, color);
            }
        }

        double[] rings = HorseRopeRenderer.debugRings();
        float[] fs = HorseRopeRenderer.debugRingSides();
        float[] fu = HorseRopeRenderer.debugRingUps();
        if (rings != null && fs != null && fu != null) {
            int n = HorseRopeRenderer.debugRingCount();
            for (int k = 0; k < n; k += HorseRingsTuning.ROPE_SPLINE_SUBDIV) {
                int i = Math.min(k, n - 2) * 3;
                Vec3 center = new Vec3(rings[k * 3], rings[k * 3 + 1], rings[k * 3 + 2]);
                Gizmos.line(center, center.add(fs[i] * 0.1, fs[i + 1] * 0.1, fs[i + 2] * 0.1), 0xFFAA7700);
                Gizmos.line(center, center.add(fu[i] * 0.1, fu[i + 1] * 0.1, fu[i + 2] * 0.1), 0xFFEEEEEE);
            }
        }
    }

    private static void drawPointCross(double[] pts, int i, int color) {
        Vec3 center = new Vec3(pts[i], pts[i + 1], pts[i + 2]);
        double r = 0.03;
        Gizmos.line(center.add(-r, 0, 0), center.add(r, 0, 0), color);
        Gizmos.line(center.add(0, -r, 0), center.add(0, r, 0), color);
        Gizmos.line(center.add(0, 0, -r), center.add(0, 0, r), color);
    }

    private static void drawBoxEdges(RopeBox box, int color) {
        Vec3 ax = new Vec3(box.ux * box.hx, box.uy * box.hx, box.uz * box.hx);
        Vec3 ay = new Vec3(box.vx * box.hy, box.vy * box.hy, box.vz * box.hy);
        Vec3 az = new Vec3(box.wx * box.hz, box.wy * box.hz, box.wz * box.hz);
        Vec3[] corner = new Vec3[8];
        for (int i = 0; i < 8; i++) {
            Vec3 v = new Vec3(box.cx, box.cy, box.cz);
            v = (i & 1) == 0 ? v.add(ax.scale(-1.0)) : v.add(ax);
            v = (i & 2) == 0 ? v.add(ay.scale(-1.0)) : v.add(ay);
            v = (i & 4) == 0 ? v.add(az.scale(-1.0)) : v.add(az);
            corner[i] = v;
        }
        for (int i = 0; i < 8; i++) {
            for (int bit = 1; bit < 8; bit <<= 1) {
                int j = i | bit;
                if (j > i) {
                    Gizmos.line(corner[i], corner[j], color);
                }
            }
        }
    }

    private static void drawAnchorCross(double[] pos, int color) {
        Vec3 center = new Vec3(pos[0], pos[1], pos[2]);
        double r = 0.08;
        Gizmos.line(center.add(-r, 0, 0), center.add(r, 0, 0), color);
        Gizmos.line(center.add(0, -r, 0), center.add(0, r, 0), color);
        Gizmos.line(center.add(0, 0, -r), center.add(0, 0, r), color);
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
