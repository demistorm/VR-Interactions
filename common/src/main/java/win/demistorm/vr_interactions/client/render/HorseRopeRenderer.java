package win.demistorm.vr_interactions.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import win.demistorm.vr_interactions.client.feature.horse.HorseRingsTuning;
import win.demistorm.vr_interactions.client.feature.horse.HorseRingsVisuals;

// Draw the reins as a textured 4-sided tube through a Catmull-Rom resample of the solver chain
public final class HorseRopeRenderer {

    public static final String VERSION = "v4";

    private static final Logger log = LoggerFactory.getLogger(HorseRopeRenderer.class);

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("vr_interactions",
            "textures/entity/horse_reins.png");
    private static final double RING_GATE_BLOCKS = 10.0; // Rings farther than this from the horse are skipped

    // Temp buffers (grown once to rope size and reused)
    private static double[] ctrl = new double[0];
    private static double[] ring = new double[0];
    private static double[] arc = new double[0];
    private static float[] tan = new float[0];
    private static float[] side = new float[0];
    private static float[] up = new float[0];
    private static boolean[] gate = new boolean[0];
    private static float[] cornersA = new float[0];
    private static float[] cornersB = new float[0];
    private static float[] cosT = new float[0];
    private static float[] sinT = new float[0];
    private static int ringCount;
    private static boolean built;
    private static boolean loggedVersion;
    private static long nextGateLogNanos;
    private static int lightSample = 0xF000F0;

    private HorseRopeRenderer() {}

    public static void render(Vec3 cameraPos, MultiBufferSource.BufferSource buffers) {
        HorseRingsVisuals visuals = HorseRingsVisuals.active();
        if (visuals == null || visuals.horse() == null) {
            return;
        }
        float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        if (!visuals.renderTick(partialTick)) {
            return;
        }
        double[] solverPts = visuals.renderPoints();
        int count = solverPts.length / 3;
        if (count < 4) {
            return;
        }
        if (!loggedVersion) {
            log.info("Horse rein rope renderer {} drawing", VERSION);
            loggedVersion = true;
        }
        if (!buildRings(solverPts, count)) {
            return;
        }
        gateRings(visuals, partialTick);
        emitTube(buffers, cameraPos);
    }

    // Resamples the solver chain through a spline
    private static boolean buildRings(double[] solverPts, int count) {
        int subdiv = HorseRingsTuning.ROPE_SPLINE_SUBDIV;
        int rings = (count - 1) * subdiv + 1;
        if (ring.length < rings * 3) {
            ctrl = new double[count * 3];
            ring = new double[rings * 3];
            arc = new double[rings];
            tan = new float[rings * 3];
            side = new float[rings * 3];
            up = new float[rings * 3];
            gate = new boolean[rings];
        } else if (ctrl.length < count * 3) {
            ctrl = new double[count * 3];
        }
        System.arraycopy(solverPts, 0, ctrl, 0, count * 3);
        ring[0] = ctrl[0];
        ring[1] = ctrl[1];
        ring[2] = ctrl[2];
        for (int i = 0; i < count - 1; i++) {
            int ia = Math.max(i - 1, 0);
            int id = Math.min(i + 2, count - 1);
            for (int k = 1; k <= subdiv; k++) {
                splinePoint(ia, i, i + 1, id, (double) k / subdiv, (i * subdiv + k) * 3);
            }
        }
        arc[0] = 0.0;
        for (int k = 1; k < rings; k++) {
            double dx = ring[k * 3] - ring[k * 3 - 3];
            double dy = ring[k * 3 + 1] - ring[k * 3 - 2];
            double dz = ring[k * 3 + 2] - ring[k * 3 - 1];
            arc[k] = arc[k - 1] + Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
        ringCount = rings;
        buildFrames();
        built = true;
        return true;
    }

    private static void splinePoint(int ia, int ib, int ic, int id, double f, int out) {
        double t1 = Math.sqrt(Math.max(ctrlDist(ia, ib), 1.0E-8));
        double t2 = t1 + Math.sqrt(Math.max(ctrlDist(ib, ic), 1.0E-8));
        double t3 = t2 + Math.sqrt(Math.max(ctrlDist(ic, id), 1.0E-8));
        double t = t1 + (t2 - t1) * f;
        double a1w0 = (t1 - t) / t1;
        double a1w1 = 1.0 - a1w0;
        double a2w0 = (t2 - t) / (t2 - t1);
        double a2w1 = 1.0 - a2w0;
        double a3w0 = (t3 - t) / (t3 - t2);
        double a3w1 = 1.0 - a3w0;
        double b1w0 = (t2 - t) / t2;
        double b1w1 = 1.0 - b1w0;
        double b2w0 = (t3 - t) / (t3 - t1);
        double b2w1 = 1.0 - b2w0;
        double cw0 = (t2 - t) / (t2 - t1);
        double cw1 = 1.0 - cw0;
        for (int j = 0; j < 3; j++) {
            double a1 = ctrl[ia * 3 + j] * a1w0 + ctrl[ib * 3 + j] * a1w1;
            double a2 = ctrl[ib * 3 + j] * a2w0 + ctrl[ic * 3 + j] * a2w1;
            double a3 = ctrl[ic * 3 + j] * a3w0 + ctrl[id * 3 + j] * a3w1;
            double b1 = a1 * b1w0 + a2 * b1w1;
            double b2 = a2 * b2w0 + a3 * b2w1;
            ring[out + j] = b1 * cw0 + b2 * cw1;
        }
    }

    private static double ctrlDist(int a, int b) {
        if (a == b) {
            return 0.0;
        }
        double dx = ctrl[a * 3] - ctrl[b * 3];
        double dy = ctrl[a * 3 + 1] - ctrl[b * 3 + 1];
        double dz = ctrl[a * 3 + 2] - ctrl[b * 3 + 2];
        return dx * dx + dy * dy + dz * dz;
    }

    // Tangents by central differences
    private static void buildFrames() {
        int n = ringCount;
        for (int k = 0; k < n; k++) {
            int a = Math.max(k - 1, 0) * 3;
            int b = Math.min(k + 1, n - 1) * 3;
            float dx = (float) (ring[b] - ring[a]);
            float dy = (float) (ring[b + 1] - ring[a + 1]);
            float dz = (float) (ring[b + 2] - ring[a + 2]);
            float inv = invSqrt(dx * dx + dy * dy + dz * dz);
            if (inv <= 0.0F && k > 0) {
                tan[k * 3] = tan[k * 3 - 3];
                tan[k * 3 + 1] = tan[k * 3 - 2];
                tan[k * 3 + 2] = tan[k * 3 - 1];
                continue;
            }
            if (inv <= 0.0F) {
                tan[0] = 0.0F;
                tan[1] = 0.0F;
                tan[2] = 1.0F;
                continue;
            }
            tan[k * 3] = dx * inv;
            tan[k * 3 + 1] = dy * inv;
            tan[k * 3 + 2] = dz * inv;
        }

        float tx = tan[0];
        float ty = tan[1];
        float tz = tan[2];
        float sx = -tz;
        float sy = 0.0F;
        float sz = tx;
        float sInv = invSqrt(sx * sx + sy * sy + sz * sz);
        if (sInv <= 0.0F) {
            sx = 1.0F;
            sy = 0.0F;
            sz = 0.0F;
        } else {
            sx *= sInv;
            sz *= sInv;
        }
        side[0] = sx;
        side[1] = sy;
        side[2] = sz;

        for (int k = 1; k < n; k++) {
            float px = tan[k * 3 - 3];
            float py = tan[k * 3 - 2];
            float pz = tan[k * 3 - 1];
            float cx = py * tz - pz * ty;
            float cy = pz * tx - px * tz;
            float cz = px * ty - py * tx;
            float lenSq = cx * cx + cy * cy + cz * cz;
            if (lenSq > 1.0E-12F) {
                float inv = invSqrt(lenSq);
                cx *= inv;
                cy *= inv;
                cz *= inv;
                float c = Math.max(-1.0F, Math.min(1.0F, px * tx + py * ty + pz * tz));
                float angle = (float) Math.atan2((float) Math.sqrt(lenSq), c);
                float sinA = (float) Math.sin(angle);
                float cosA = (float) Math.cos(angle);
                float dot = cx * sx + cy * sy + cz * sz;
                float rx = sx * cosA + (cy * sz - cz * sy) * sinA + cx * dot * (1.0F - cosA);
                float ry = sy * cosA + (cz * sx - cx * sz) * sinA + cy * dot * (1.0F - cosA);
                float rz = sz * cosA + (cx * sy - cy * sx) * sinA + cz * dot * (1.0F - cosA);
                sx = rx;
                sy = ry;
                sz = rz;
            }
            tx = tan[k * 3];
            ty = tan[k * 3 + 1];
            tz = tan[k * 3 + 2];
            float dot = sx * tx + sy * ty + sz * tz;
            sx -= tx * dot;
            sy -= ty * dot;
            sz -= tz * dot;
            float oInv = invSqrt(sx * sx + sy * sy + sz * sz);
            if (oInv <= 0.0F) {
                setPerpendicular(tx, ty, tz, k);
                sx = side[k * 3];
                sy = side[k * 3 + 1];
                sz = side[k * 3 + 2];
            } else {
                side[k * 3] = sx * oInv;
                side[k * 3 + 1] = sy * oInv;
                side[k * 3 + 2] = sz * oInv;
                sx = side[k * 3];
                sy = side[k * 3 + 1];
                sz = side[k * 3 + 2];
            }
        }

        for (int k = 0; k < n; k++) {
            int i = k * 3;
            up[i] = tan[i + 1] * side[i + 2] - tan[i + 2] * side[i + 1];
            up[i + 1] = tan[i + 2] * side[i] - tan[i] * side[i + 2];
            up[i + 2] = tan[i] * side[i + 1] - tan[i + 1] * side[i];
        }
    }

    // Per ring draw decision (helps when incorrect hand data is used)
    private static void gateRings(HorseRingsVisuals visuals, float partialTick) {
        AbstractHorse horse = visuals.horse();
        float hx = (float) Mth.lerp(partialTick, horse.xo, horse.getX());
        float hy = (float) Mth.lerp(partialTick, horse.yo, horse.getY());
        float hz = (float) Mth.lerp(partialTick, horse.zo, horse.getZ());
        // Light sample at the horse
        lightSample = LevelRenderer.getLightColor(horse.level(), BlockPos.containing(hx, hy + 1.0F, hz));
        double gateSq = RING_GATE_BLOCKS * RING_GATE_BLOCKS;
        int gated = 0;
        for (int k = 0; k < ringCount; k++) {
            int i = k * 3;
            boolean ok = Double.isFinite(ring[i]) && Double.isFinite(ring[i + 1]) && Double.isFinite(ring[i + 2]);
            if (ok) {
                double dx = ring[i] - hx;
                double dy = ring[i + 1] - hy;
                double dz = ring[i + 2] - hz;
                ok = dx * dx + dy * dy + dz * dz <= gateSq;
            }
            gate[k] = ok;
            if (!ok) {
                gated++;
            }
        }
        if (gated > 0) {
            warnGated(gated);
        }
    }

    private static void warnGated(int gated) {
        long now = System.nanoTime();
        if (now < nextGateLogNanos) {
            return;
        }
        nextGateLogNanos = now + 10_000_000_000L;
        log.warn("Rein tube gating {} of {} rings (far or non-finite sim data), drawing the rest", gated, ringCount);
    }

    private static void emitTube(MultiBufferSource.BufferSource buffers, Vec3 cameraPos) {
        int sides = HorseRingsTuning.ROPE_SIDES;
        if (cosT.length < sides) {
            cosT = new float[sides];
            sinT = new float[sides];
            cornersA = new float[sides * 3];
            cornersB = new float[sides * 3];
            for (int s = 0; s < sides; s++) {
                double a = Math.PI * 2.0 * s / sides;
                cosT[s] = (float) Math.cos(a);
                sinT[s] = (float) Math.sin(a);
            }
        }
        Matrix4f matrix = new Matrix4f().translate(
                (float) (ring[0] - cameraPos.x),
                (float) (ring[1] - cameraPos.y),
                (float) (ring[2] - cameraPos.z));
        VertexConsumer consumer = buffers.getBuffer(RenderTypes.entitySolid(TEXTURE));
        float r = (float) HorseRingsTuning.ROPE_RENDER_RADIUS;
        double texScale = 1.0 / HorseRingsTuning.ROPE_TEX_REPEAT_BLOCKS;

        for (int k = 0; k < ringCount - 1; k++) {
            if (!gate[k] || !gate[k + 1]) {
                continue;
            }
            fillCorners(k, r, cornersA);
            fillCorners(k + 1, r, cornersB);
            float v0 = (float) (arc[k] * texScale);
            float v1 = (float) (arc[k + 1] * texScale);
            for (int s = 0; s < sides; s++) {
                int s1 = (s + 1) % sides;
                float u0 = s / (float) sides;
                float u1 = (s + 1) / (float) sides;
                float nx = side[k * 3] * cosT[s] + up[k * 3] * sinT[s];
                float ny = side[k * 3 + 1] * cosT[s] + up[k * 3 + 1] * sinT[s];
                float nz = side[k * 3 + 2] * cosT[s] + up[k * 3 + 2] * sinT[s];
                emitQuad(consumer, matrix, cornersA, s * 3, cornersA, s1 * 3, cornersB, s1 * 3, cornersB, s * 3,
                        u0, v0, u1, v0, u1, v1, u0, v1, nx, ny, nz);
                emitQuad(consumer, matrix, cornersA, s * 3, cornersB, s * 3, cornersB, s1 * 3, cornersA, s1 * 3,
                        u0, v0, u0, v1, u1, v1, u1, v0, nx, ny, nz);
            }
        }
    }

    private static void fillCorners(int k, float r, float[] out) {
        int i = k * 3;
        double px = ring[i];
        double py = ring[i + 1];
        double pz = ring[i + 2];
        for (int s = 0; s < cosT.length; s++) {
            out[s * 3] = (float) (px - ring[0]) + side[i] * cosT[s] * r + up[i] * sinT[s] * r;
            out[s * 3 + 1] = (float) (py - ring[1]) + side[i + 1] * cosT[s] * r + up[i + 1] * sinT[s] * r;
            out[s * 3 + 2] = (float) (pz - ring[2]) + side[i + 2] * cosT[s] * r + up[i + 2] * sinT[s] * r;
        }
    }

    private static void emitQuad(VertexConsumer consumer, Matrix4f matrix,
                                 float[] a, int ai, float[] b, int bi, float[] c, int ci, float[] d, int di,
                                 float au, float av, float bu, float bv, float cu, float cv, float du, float dv,
                                 float nx, float ny, float nz) {
        consumer.addVertex(matrix, a[ai], a[ai + 1], a[ai + 2])
                .setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(au, av)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightSample).setNormal(nx, ny, nz);
        consumer.addVertex(matrix, b[bi], b[bi + 1], b[bi + 2])
                .setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(bu, bv)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightSample).setNormal(nx, ny, nz);
        consumer.addVertex(matrix, c[ci], c[ci + 1], c[ci + 2])
                .setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(cu, cv)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightSample).setNormal(nx, ny, nz);
        consumer.addVertex(matrix, d[di], d[di + 1], d[di + 2])
                .setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(du, dv)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightSample).setNormal(nx, ny, nz);
    }

    // Fallback side (cross the tangent with the least aligned world axis)
    private static void setPerpendicular(float tx, float ty, float tz, int k) {
        float ax;
        float ay;
        float az;
        if (Math.abs(tx) <= Math.abs(ty) && Math.abs(tx) <= Math.abs(tz)) {
            ax = 1.0F;
            ay = 0.0F;
            az = 0.0F;
        } else if (Math.abs(ty) <= Math.abs(tz)) {
            ax = 0.0F;
            ay = 1.0F;
            az = 0.0F;
        } else {
            ax = 0.0F;
            ay = 0.0F;
            az = 1.0F;
        }
        float cx = ty * az - tz * ay;
        float cy = tz * ax - tx * az;
        float cz = tx * ay - ty * ax;
        float inv = invSqrt(cx * cx + cy * cy + cz * cz);
        side[k * 3] = cx * inv;
        side[k * 3 + 1] = cy * inv;
        side[k * 3 + 2] = cz * inv;
    }

    // Debug renderer access to the last built rings (null before the first render pass)
    public static double[] debugRings() {
        return built ? ring : null;
    }

    public static float[] debugRingSides() {
        return built ? side : null;
    }

    public static float[] debugRingUps() {
        return built ? up : null;
    }

    public static int debugRingCount() {
        return built ? ringCount : 0;
    }

    private static float invSqrt(float lenSq) {
        return lenSq < 1.0E-10F ? 0.0F : (float) (1.0 / Math.sqrt(lenSq));
    }
}
