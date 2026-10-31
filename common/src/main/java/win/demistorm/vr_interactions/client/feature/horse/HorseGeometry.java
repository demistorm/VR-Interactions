package win.demistorm.vr_interactions.client.feature.horse;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import win.demistorm.vr_interactions.client.visual.RopeBox;

// Rein anchors from the leash point plus collision boxes created from whatever model renders (works with FA now)
final class HorseGeometry {

    private static final double MODEL_Y_OFFSET = -EntityModel.MODEL_Y_OFFSET;
    private static final double MAX_PART_HALF = 4.0;
    private static final float MIN_CUBE_PX = 2.0F;

    private static final Logger log = LoggerFactory.getLogger(HorseGeometry.class);
    private static long nextBoxWarnNanos;

    // (DEBUG) Last computed anchors in world space
    final double[] mouthL = new double[3];
    final double[] mouthR = new double[3];
    // Model's leash attach point (same math as the leash render state (for tuning and debug renderer))
    final double[] leashBase = new double[3];
    // Collision boxes in the horse's local frame from the last live capture
    RopeBox[] localBoxes = new RopeBox[0];

    private final ArrayList<RopeBox> captured = new ArrayList<>();

    // Model's leash point plus tuning offsets in the body frame
    void mouthAnchors(AbstractHorse horse, float partialTick, double[] outLeft, double[] outRight) {
        Vec3 base = ((Leashable) horse).getLeashOffset(partialTick);
        float bodyRot = horse.getPreciseBodyRotation(partialTick) * ((float) Math.PI / 180F);
        Vec3 leash = horse.getPosition(partialTick).add(base.yRot(-bodyRot));
        Vec3 off = new Vec3(HorseRingsTuning.ANCHOR_SPREAD, HorseRingsTuning.ANCHOR_UP, HorseRingsTuning.ANCHOR_FORWARD);
        store(leashBase, leash);
        store(outLeft, leash.add(off.yRot(-bodyRot)));
        store(outRight, leash.add(off.multiply(-1.0, 1.0, 1.0).yRot(-bodyRot)));
        System.arraycopy(outLeft, 0, mouthL, 0, 3);
        System.arraycopy(outRight, 0, mouthR, 0, 3);
    }

    private static void store(double[] out, Vec3 v) {
        out[0] = v.x;
        out[1] = v.y;
        out[2] = v.z;
    }

    // Captures the rendered model with its live transforms into collision boxes
    void captureModel(ModelPart root) {
        captured.clear();
        root.visit(new PoseStack(), (pose, path, index, cube) -> addBox(pose.pose(), cube));
        localBoxes = captured.toArray(new RopeBox[0]);
    }

    // Turns one model cube at its live transform into a local frame collider
    private void addBox(Matrix4f matrix, ModelPart.Cube cube) {
        float w = cube.maxX - cube.minX;
        float h = cube.maxY - cube.minY;
        float d = cube.maxZ - cube.minZ;
        if (w < MIN_CUBE_PX && h < MIN_CUBE_PX && d < MIN_CUBE_PX) {
            return;
        }
        Vector3f c = toLocal(matrix.transformPosition(
                (cube.minX + cube.maxX) * 0.5F / 16.0F,
                (cube.minY + cube.maxY) * 0.5F / 16.0F,
                (cube.minZ + cube.maxZ) * 0.5F / 16.0F, new Vector3f()));
        Vector3f u = toLocal(matrix.transformDirection(1.0F, 0.0F, 0.0F, new Vector3f()));
        Vector3f v = toLocal(matrix.transformDirection(0.0F, 1.0F, 0.0F, new Vector3f()));
        Vector3f s = toLocal(matrix.transformDirection(0.0F, 0.0F, 1.0F, new Vector3f()));
        double ly = MODEL_Y_OFFSET + c.y;
        double ul = axisLengthAndNormalize(u);
        double vl = axisLengthAndNormalize(v);
        double sl = axisLengthAndNormalize(s);
        double hx = ul * (w * 0.5F / 16.0F);
        double hy = vl * (h * 0.5F / 16.0F);
        double hz = sl * (d * 0.5F / 16.0F);
        // A single NaN or Infinity poisons the sum
        if (!Double.isFinite(c.x + ly + c.z + hx + hy + hz)
                || Math.abs(c.x) > MAX_PART_HALF || Math.abs(ly) > MAX_PART_HALF || Math.abs(c.z) > MAX_PART_HALF
                || hx > MAX_PART_HALF || hy > MAX_PART_HALF || hz > MAX_PART_HALF) {
            warnBrokenBox(c.x, ly, c.z, hx, hy, hz);
            return;
        }
        captured.add(new RopeBox(c.x, ly, c.z, u, v, s, hx, hy, hz));
    }

    // Flips a model space vector into the horse local frame (x stays, y and z negate)
    private static Vector3f toLocal(Vector3f vec) {
        return vec.set(vec.x, -vec.y, -vec.z);
    }

    // Normalizes an axis in-place and returns its original length (floored so a zero axis can't NaN)
    private static double axisLengthAndNormalize(Vector3f axis) {
        double len = Math.max(axis.length(), 1.0E-9);
        axis.div((float) len);
        return len;
    }

    private static void warnBrokenBox(double cx, double cy, double cz, double hx, double hy, double hz) {
        long now = System.nanoTime();
        if (now < nextBoxWarnNanos) {
            return;
        }
        nextBoxWarnNanos = now + 10_000_000_000L;
        log.warn("Dropping broken horse collider center=({},{},{}) half=({},{},{}), reasonable parts stay within {} blocks",
                String.format("%.2f", cx), String.format("%.2f", cy), String.format("%.2f", cz),
                String.format("%.2f", hx), String.format("%.2f", hy), String.format("%.2f", hz),
                String.format("%.1f", MAX_PART_HALF));
    }
}
