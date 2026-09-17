package win.demistorm.vr_interactions.client.feature.horse;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.joml.Vector3dc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import win.demistorm.vr_interactions.client.visual.RopeBox;
import win.demistorm.vr_interactions.client.visual.VerletRope;

// Reins loop running mouth to hand to hand to mouth (simulated in the horse's local frame so moving/turns don't fling the rope)
public final class HorseRingsVisuals {

    private static volatile HorseRingsVisuals active;

    private final VerletRope rope;
    private final HorseGeometry geometry = new HorseGeometry();
    private final double[] renderPts;
    private final double[] localRenderPts;
    private final int handLIndex;
    private final int handRIndex;

    private volatile AbstractHorse horse;
    private volatile int handMask;
    private boolean mainOnLeftSlot;
    private Vector3dc mainPos;
    private Vector3dc offPos;
    private boolean renderedOnce;
    private double frameX;
    private double frameY;
    private double frameZ;
    private double frameCos;
    private double frameSin;
    private RopeBox[] localBoxes = new RopeBox[0];
    private static final double PIN_SANITY_BLOCKS = 8.0;

    private static final Logger log = LoggerFactory.getLogger(HorseRingsVisuals.class);
    private static long nextAlphaWarnNanos;
    private static long lastReseedLogNanos;
    private static long nextOutlierWarnNanos;

    public static HorseRingsVisuals active() {
        return active;
    }

    public HorseRingsVisuals() {
        int rein = HorseRingsTuning.REIN_SEGMENTS;
        int bridge = HorseRingsTuning.BRIDGE_SEGMENTS;
        int points = 2 * rein + bridge + 1;
        this.handLIndex = rein;
        this.handRIndex = rein + bridge;
        this.rope = new VerletRope(points, HorseRingsTuning.ROPE_TOTAL_LENGTH / (points - 1),
                HorseRingsTuning.ROPE_RADIUS);
        this.renderPts = new double[points * 3];
        this.localRenderPts = new double[points * 3];
    }

    public AbstractHorse horse() {
        return horse;
    }

    // Debug access to the last computed geometry
    HorseGeometry geometry() {
        return geometry;
    }

    public int handLIndex() {
        return handLIndex;
    }

    public int handRIndex() {
        return handRIndex;
    }

    public double[] renderPoints() {
        return renderPts;
    }

    public double[] debugMouthL() {
        return geometry.mouthL;
    }

    public double[] debugMouthR() {
        return geometry.mouthR;
    }

    public RopeBox[] debugBoxes() {
        return geometry.boxes;
    }

    public String debugRopeStats() {
        return rope.debugStats(localBoxes);
    }

    public double[] debugRopePoints() {
        return renderedOnce ? renderPts : null;
    }

    public void tick(AbstractHorse horse, boolean mainFree, boolean offFree, Vector3dc mainPos, Vector3dc offPos) {
        int mask = (mainFree ? 1 : 0) | (offFree ? 2 : 0);
        if (mask == 0) {
            deactivate();
            return;
        }

        boolean reseed = active != this || mask != handMask;
        this.horse = horse;
        this.handMask = mask;
        // Null unusable pose samples
        this.mainPos = sanePose(mainPos) ? mainPos : null;
        this.offPos = sanePose(offPos) ? offPos : null;

        // Anchor on the horse's previous tick pose to match the frame pre-tick VR hand data was captured in (newer pins the hands a full tick behind in local space)
        double yawRad = Math.toRadians(horse.yBodyRotO);
        frameX = horse.xo;
        frameY = horse.yo;
        frameZ = horse.zo;
        frameCos = Math.cos(yawRad);
        frameSin = Math.sin(yawRad);

        double[] mouthL = new double[3];
        double[] mouthR = new double[3];
        if (!geometry.mouthAnchors(horse, 0.0f, mouthL, mouthR)) {
            deactivate();
            return;
        }
        double[] handL = new double[3];
        double[] handR = new double[3];
        handPin(true, handL);
        handPin(false, handR);

        if (reseed) {
            if (mask == 3 && this.mainPos != null && this.offPos != null) {
                mainOnLeftSlot = isMainOnLeft(horse, this.mainPos, this.offPos);
            }
            if (!reseed(mouthL, mouthR, handL, handR, mask)) {
                return;
            }
            active = this;
        }

        rope.beginTick();

        if (sanePin(mouthL)) {
            setLocalPin(0, mouthL);
        }
        if (sanePin(handL)) {
            setLocalPin(handLIndex, handL);
        }
        if (sanePin(handR)) {
            setLocalPin(handRIndex, handR);
        }
        if (sanePin(mouthR)) {
            setLocalPin(rope.pointCount() - 1, mouthR);
        }

        if (!geometry.buildBoxes(horse, 0.0f)) {
            deactivate();
            return;
        }
        toLocalBoxes();
        rope.step(1.0, -HorseRingsTuning.ROPE_GRAVITY, HorseRingsTuning.ROPE_DAMPING,
                HorseRingsTuning.ROPE_MAX_SPEED, HorseRingsTuning.ROPE_SUBSTEPS, HorseRingsTuning.ROPE_ITERATIONS,
                localBoxes, HorseRingsTuning.ROPE_FRICTION);
        renderedOnce = true;
        String outliers = rope.debugOutliers(PIN_SANITY_BLOCKS);
        if (outliers != null) {
            warnOutliers(outliers);
        }
    }

    // Converts a world point into the horse frame and anchors it there
    private void setLocalPin(int index, double[] world) {
        double dx = world[0] - frameX;
        double dy = world[1] - frameY;
        double dz = world[2] - frameZ;
        rope.setPin(index, dx * frameCos + dz * frameSin, dy, -dx * frameSin + dz * frameCos);
    }

    private double[] toLocal(double[] world) {
        double dx = world[0] - frameX;
        double dy = world[1] - frameY;
        double dz = world[2] - frameZ;
        return new double[] {dx * frameCos + dz * frameSin, dy, -dx * frameSin + dz * frameCos};
    }

    // Sim copy of the collision boxes in horse's local space (geometry keeps the world copy for debug)
    private void toLocalBoxes() {
        RopeBox[] src = geometry.boxes;
        if (localBoxes.length < src.length) {
            localBoxes = new RopeBox[src.length];
        }
        for (int i = 0; i < src.length; i++) {
            RopeBox b = src[i];
            localBoxes[i] = b == null ? null : new RopeBox(
                    (b.cx - frameX) * frameCos + (b.cz - frameZ) * frameSin,
                    b.cy - frameY,
                    -(b.cx - frameX) * frameSin + (b.cz - frameZ) * frameCos,
                    b.ux * frameCos + b.uz * frameSin, b.uy, -(b.ux * frameSin) + b.uz * frameCos,
                    b.vx * frameCos + b.vz * frameSin, b.vy, -(b.vx * frameSin) + b.vz * frameCos,
                    b.wx * frameCos + b.wz * frameSin, b.wy, -(b.wx * frameSin) + b.wz * frameCos,
                    b.hx, b.hy, b.hz);
        }
    }

    public void deactivate() {
        if (active == this) {
            active = null;
        }
        horse = null;
        handMask = 0;
        renderedOnce = false;
    }

    public boolean renderTick(float rawAlpha) {
        if (active != this || horse == null || !renderedOnce) {
            return false;
        }
        float alpha = rawAlpha;
        if (rawAlpha < 0.0F || rawAlpha > 1.0F) {
            warnBadAlpha(rawAlpha);
            alpha = Mth.clamp(rawAlpha, 0.0F, 1.0F);
        }
        rope.renderLerp(alpha, localRenderPts);
        AbstractHorse h = horse;
        double ox = Mth.lerp(alpha, h.xo, h.getX());
        double oy = Mth.lerp(alpha, h.yo, h.getY());
        double oz = Mth.lerp(alpha, h.zo, h.getZ());
        double yawRad = Math.toRadians(Mth.rotLerp(alpha, h.yBodyRotO, h.yBodyRot));
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);
        for (int i = 0; i < renderPts.length; i += 3) {
            double lx = localRenderPts[i];
            double lz = localRenderPts[i + 2];
            renderPts[i] = ox + lx * cos - lz * sin;
            renderPts[i + 1] = oy + localRenderPts[i + 1];
            renderPts[i + 2] = oz + lx * sin + lz * cos;
        }
        return true;
    }

    private static void warnBadAlpha(float rawAlpha) {
        long now = System.nanoTime();
        if (now < nextAlphaWarnNanos) {
            return;
        }
        nextAlphaWarnNanos = now + 10_000_000_000L;
        log.warn("Rein render alpha out of range: {}, clamped", String.format("%.4f", rawAlpha));
    }

    private void handPin(boolean leftSlot, double[] out) {
        boolean dual = handMask == 3;
        Vector3dc pos = dual
                ? (leftSlot == mainOnLeftSlot ? mainPos : offPos)
                : ((handMask & 1) != 0 ? mainPos : offPos);
        if (pos == null) {
            return;
        }
        if (dual) {
            out[0] = pos.x();
            out[1] = pos.y();
            out[2] = pos.z();
        } else {
            spread(pos.x(), pos.y(), pos.z(), horse.yBodyRot, leftSlot ? 1.0 : -1.0,
                    HorseRingsTuning.GATHER_OFFSET, out);
        }
    }

    private static void spread(double x, double y, double z, float bodyYaw, double side, double offset, double[] out) {
        spread(x, y, z, bodyYaw, side, offset, out, 0);
    }

    private static void spread(double x, double y, double z, float bodyYaw, double side, double offset,
                               double[] out, int offsetIdx) {
        double yawRad = Math.toRadians(bodyYaw);
        double lat = side * offset;
        out[offsetIdx] = x + lat * Math.cos(yawRad);
        out[offsetIdx + 1] = y;
        out[offsetIdx + 2] = z + lat * Math.sin(yawRad);
    }

    private static boolean isMainOnLeft(AbstractHorse horse, Vector3dc mainPos, Vector3dc offPos) {
        double yawRad = Math.toRadians(horse.yBodyRot);
        double leftX = Math.cos(yawRad);
        double leftZ = Math.sin(yawRad);
        double mainLat = (mainPos.x() - horse.getX()) * leftX + (mainPos.z() - horse.getZ()) * leftZ;
        double offLat = (offPos.x() - horse.getX()) * leftX + (offPos.z() - horse.getZ()) * leftZ;
        return mainLat >= offLat;
    }

    private boolean reseed(double[] mouthL, double[] mouthR, double[] handL, double[] handR, int mask) {
        long now = System.nanoTime();
        if (now - lastReseedLogNanos > 1_000_000_000L) {
            lastReseedLogNanos = now;
            log.info("Rein reseed anchors: mouthL={} handL={} handR={} mouthR={} spans L={} bridge={} R={}",
                    fmt(mouthL), fmt(handL), fmt(handR), fmt(mouthR),
                    String.format("%.2f", span(mouthL, handL)),
                    String.format("%.2f", span(handL, handR)),
                    String.format("%.2f", span(handR, mouthR)));
        }
        if (!sanePin(mouthL) || !sanePin(mouthR) || !sanePin(handL) || !sanePin(handR)) {
            return false;
        }
        rope.setSectionRestLength(0, handLIndex, span(mouthL, handL) + HorseRingsTuning.REIN_SLACK_BLOCKS);
        rope.setSectionRestLength(handLIndex, handRIndex,
                Math.max(span(handL, handR), HorseRingsTuning.BRIDGE_MIN_LENGTH));
        rope.setSectionRestLength(handRIndex, rope.pointCount() - 1,
                span(handR, mouthR) + HorseRingsTuning.REIN_SLACK_BLOCKS);
        seedSection(0, handLIndex, toLocal(mouthL), toLocal(handL),
                HorseRingsTuning.SEED_SAG, HorseRingsTuning.SEED_LATERAL_PUSH);
        seedSection(handLIndex, handRIndex, toLocal(handL), toLocal(handR),
                mask == 3 ? HorseRingsTuning.SEED_SAG : HorseRingsTuning.SEED_BRIDGE_SAG, 0.0);
        seedSection(handRIndex, rope.pointCount() - 1, toLocal(handR), toLocal(mouthR),
                HorseRingsTuning.SEED_SAG, -HorseRingsTuning.SEED_LATERAL_PUSH);
        return true;
    }

    private boolean sanePin(double[] pos) {
        return Double.isFinite(pos[0]) && Double.isFinite(pos[1]) && Double.isFinite(pos[2])
                && saneDist(pos[0], pos[1], pos[2]);
    }

    private boolean sanePose(Vector3dc pos) {
        return pos != null && Double.isFinite(pos.x()) && Double.isFinite(pos.y()) && Double.isFinite(pos.z())
                && saneDist(pos.x(), pos.y(), pos.z());
    }

    private boolean saneDist(double x, double y, double z) {
        AbstractHorse h = horse;
        if (h == null) {
            return false;
        }
        double dx = x - h.getX();
        double dy = y - h.getY();
        double dz = z - h.getZ();
        return dx * dx + dy * dy + dz * dz < PIN_SANITY_BLOCKS * PIN_SANITY_BLOCKS;
    }

    private void seedSection(int fromIdx, int toIdx, double[] from, double[] to, double sag, double bulge) {
        int steps = toIdx - fromIdx;
        for (int s = 0; s <= steps; s++) {
            double t = (double) s / steps;
            double wave = Math.sin(Math.PI * t);
            rope.seed(fromIdx + s,
                    from[0] + (to[0] - from[0]) * t + wave * bulge,
                    from[1] + (to[1] - from[1]) * t - wave * sag,
                    from[2] + (to[2] - from[2]) * t);
        }
    }

    private static void warnOutliers(String outliers) {
        long now = System.nanoTime();
        if (now < nextOutlierWarnNanos) {
            return;
        }
        nextOutlierWarnNanos = now + 2_000_000_000L;
        log.warn("{}", outliers);
    }

    private static String fmt(double[] pos) {
        return String.format("(%.2f, %.2f, %.2f)", pos[0], pos[1], pos[2]);
    }

    private static double span(double[] a, double[] b) {
        double dx = a[0] - b[0];
        double dy = a[1] - b[1];
        double dz = a[2] - b[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
