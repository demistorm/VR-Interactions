package win.demistorm.vr_interactions.client.feature.horse;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import win.demistorm.vr_interactions.client.visual.RopeBox;

// Rein anchors and collision geometry gathered from the baked model of the horse variant (need to test with FA: Extensions)
final class HorseGeometry {

    // Cube paths inside AbstractEquineModel
    private static final String MOUTH_PATH = "/head_parts/upper_mouth";
    private static final String HEAD_PREFIX = "/head_parts";
    private static final String BODY_PATH = "/body";
    private static final String BODY_PREFIX = "/body/";

    private static final double MODEL_Y_OFFSET = -EntityModel.MODEL_Y_OFFSET;
    private static final float HEAD_YAW_CLAMP_DEG = 20.0F;
    private static final long RETRY_NANOS = 10_000_000_000L;
    private static final double MAX_PART_HALF = 4.0;
    private static long nextCubeWarnNanos;

    private static final Logger log = LoggerFactory.getLogger(HorseGeometry.class);

    private record Cuboid(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {}

    private record MeshBox(boolean head, double cx, double cy, double cz,
                           double xx, double xy, double xz,
                           double yx, double yy, double yz,
                           double zx, double zy, double zz) {}

    private ModelLayerLocation bakedLayer;
    private boolean baked;
    private long retryAtNanos;
    private float rootScale;
    private float rootY;
    private PartPose headPose;
    private Cuboid mouthCube;
    private double pivotX;
    private double pivotY;
    private double pivotZ;
    private MeshBox[] mesh = new MeshBox[0];
    private final double[] scratch = new double[3];

    // Last computed values (kept for debug renderer)
    final double[] mouthL = new double[3];
    final double[] mouthR = new double[3];
    RopeBox[] boxes = new RopeBox[0];

    // False when the layer's parts are missing (heavily restructured resource pack)
    boolean mouthAnchors(AbstractHorse horse, float partialTick, double[] outLeft, double[] outRight) {
        if (!ensureBaked(horse)) {
            return false;
        }

        double pitch = headPose.xRot() + Math.toRadians(horse.getXRot(partialTick));
        headPoint(horse, partialTick, pitch, mouthCube.maxX, mouthCube.maxY, mouthCube.minZ, outLeft);
        headPoint(horse, partialTick, pitch, mouthCube.minX, mouthCube.maxY, mouthCube.minZ, outRight);
        System.arraycopy(outLeft, 0, mouthL, 0, 3);
        System.arraycopy(outRight, 0, mouthR, 0, 3);
        return true;
    }

    boolean buildBoxes(AbstractHorse horse, float partialTick) {
        if (!ensureBaked(horse)) {
            return false;
        }
        float bodyYaw = Mth.rotLerp(partialTick, horse.yBodyRotO, horse.yBodyRot);
        float headYaw = Mth.rotLerp(partialTick, horse.yHeadRotO, horse.yHeadRot);
        float relYaw = Mth.clamp(Mth.wrapDegrees(headYaw - bodyYaw), -HEAD_YAW_CLAMP_DEG, HEAD_YAW_CLAMP_DEG);
        double pitch = Math.toRadians(horse.getXRot(partialTick));
        double cosP = Math.cos(pitch);
        double sinP = Math.sin(pitch);
        double cosH = Math.cos(Math.toRadians(relYaw));
        double sinH = Math.sin(Math.toRadians(relYaw));
        double cosB = Math.cos(Math.toRadians(bodyYaw));
        double sinB = Math.sin(Math.toRadians(bodyYaw));
        // Lerped entity position (the tick path passes partialTick 0.0 to match the sim frame)
        double ex = Mth.lerp(partialTick, horse.xo, horse.getX());
        double ey = Mth.lerp(partialTick, horse.yo, horse.getY());
        double ez = Mth.lerp(partialTick, horse.zo, horse.getZ());

        double[] axisOut = new double[9];
        for (int i = 0; i < mesh.length; i++) {
            MeshBox mb = mesh[i];
            double x = mb.cx();
            double y = mb.cy();
            double z = mb.cz();
            if (mb.head()) {
                scratch[0] = x - pivotX;
                scratch[1] = y - pivotY;
                scratch[2] = z - pivotZ;
                rotateHead(scratch, cosP, sinP, cosH, sinH);
                x = scratch[0] + pivotX;
                y = scratch[1] + pivotY;
                z = scratch[2] + pivotZ;
            }
            axisWorld(mb.xx(), mb.xy(), mb.xz(), mb.head(), cosP, sinP, cosH, sinH, cosB, sinB, axisOut, 0);
            axisWorld(mb.yx(), mb.yy(), mb.yz(), mb.head(), cosP, sinP, cosH, sinH, cosB, sinB, axisOut, 3);
            axisWorld(mb.zx(), mb.zy(), mb.zz(), mb.head(), cosP, sinP, cosH, sinH, cosB, sinB, axisOut, 6);
            double cx = ex + x * cosB + z * sinB;
            double cy = ey + MODEL_Y_OFFSET - y;
            double cz = ez + x * sinB - z * cosB;
            double hx = len(mb.xx(), mb.xy(), mb.xz());
            double hy = len(mb.yx(), mb.yy(), mb.yz());
            double hz = len(mb.zx(), mb.zy(), mb.zz());
            if (!Double.isFinite(cx) || !Double.isFinite(cy) || !Double.isFinite(cz)
                    || !Double.isFinite(hx) || !Double.isFinite(hy) || !Double.isFinite(hz)
                    || Math.abs(cx - ex) > MAX_PART_HALF || Math.abs(cy - ey) > MAX_PART_HALF
                    || Math.abs(cz - ez) > MAX_PART_HALF
                    || hx > MAX_PART_HALF || hy > MAX_PART_HALF || hz > MAX_PART_HALF) {
                warnBrokenCube(i, cx, cy, cz, hx, hy, hz);
                boxes[i] = null;
                continue;
            }
            boxes[i] = new RopeBox(cx, cy, cz,
                    axisOut[0], axisOut[1], axisOut[2],
                    axisOut[3], axisOut[4], axisOut[5],
                    axisOut[6], axisOut[7], axisOut[8],
                    hx, hy, hz);
        }
        return true;
    }

    private static void warnBrokenCube(int i, double cx, double cy, double cz, double hx, double hy, double hz) {
        long now = System.nanoTime();
        if (now < nextCubeWarnNanos) {
            return;
        }
        nextCubeWarnNanos = now + RETRY_NANOS;
        log.warn("Dropping broken horse collider {}: center=({},{},{}) half=({},{},{}), sane parts stay within {} blocks",
                i, String.format("%.2f", cx), String.format("%.2f", cy), String.format("%.2f", cz),
                String.format("%.2f", hx), String.format("%.2f", hy), String.format("%.2f", hz),
                String.format("%.1f", MAX_PART_HALF));
    }

    // Transforms a head_parts local point to world space
    private void headPoint(AbstractHorse horse, float partialTick, double pitch, double xUnits, double yUnits,
                           double zUnits, double[] out) {
        double cosP = Math.cos(pitch);
        double sinP = Math.sin(pitch);
        double yPrime = yUnits * cosP - zUnits * sinP;
        double zPrime = yUnits * sinP + zUnits * cosP;
        double authoredY = headPose.y() + yPrime;
        double authoredZ = headPose.z() + zPrime;
        double lat = xUnits * rootScale / 16.0;
        double fwd = -authoredZ * rootScale / 16.0;
        double worldY = MODEL_Y_OFFSET - rootY / 16.0 - authoredY * rootScale / 16.0;

        // Head yaw rotates the point around the head pivot
        double pivotFwd = -headPose.z() * rootScale / 16.0;
        double latRel = lat;
        double fwdRel = fwd - pivotFwd;
        float bodyYaw = Mth.rotLerp(partialTick, horse.yBodyRotO, horse.yBodyRot);
        float headYaw = Mth.rotLerp(partialTick, horse.yHeadRotO, horse.yHeadRot);
        float relYaw = Mth.clamp(Mth.wrapDegrees(headYaw - bodyYaw), -HEAD_YAW_CLAMP_DEG, HEAD_YAW_CLAMP_DEG);
        double relYawRad = Math.toRadians(relYaw);
        double cosH = Math.cos(relYawRad);
        double sinH = Math.sin(relYawRad);
        double latRot = latRel * cosH - fwdRel * sinH;
        double fwdRot = latRel * sinH + fwdRel * cosH;

        toWorld(horse, partialTick, bodyYaw, latRot, fwdRot + pivotFwd, worldY, out);
    }

    private void toWorld(AbstractHorse horse, float partialTick, float bodyYaw, double lat, double fwd,
                         double worldY, double[] out) {
        double yawRad = Math.toRadians(bodyYaw);
        double cosB = Math.cos(yawRad);
        double sinB = Math.sin(yawRad);
        double ex = Mth.lerp(partialTick, horse.xo, horse.getX());
        double ey = Mth.lerp(partialTick, horse.yo, horse.getY());
        double ez = Mth.lerp(partialTick, horse.zo, horse.getZ());
        out[0] = ex + lat * cosB - fwd * sinB;
        out[1] = ey + worldY;
        out[2] = ez + lat * sinB + fwd * cosB;
    }

    private static void rotateHead(double[] v, double cosP, double sinP, double cosH, double sinH) {
        double y = v[1] * cosP - v[2] * sinP;
        double z = v[1] * sinP + v[2] * cosP;
        double x = v[0] * cosH + z * sinH;
        v[2] = -v[0] * sinH + z * cosH;
        v[0] = x;
        v[1] = y;
    }

    private void axisWorld(double vx, double vy, double vz, boolean head, double cosP, double sinP,
                           double cosH, double sinH, double cosB, double sinB, double[] out, int off) {
        if (head) {
            scratch[0] = vx;
            scratch[1] = vy;
            scratch[2] = vz;
            rotateHead(scratch, cosP, sinP, cosH, sinH);
            vx = scratch[0];
            vy = scratch[1];
            vz = scratch[2];
        }
        double wx = vx * cosB + vz * sinB;
        double wy = -vy;
        double wz = vx * sinB - vz * cosB;
        double h = len(wx, wy, wz);
        out[off] = wx / h;
        out[off + 1] = wy / h;
        out[off + 2] = wz / h;
    }

    private static double len(double x, double y, double z) {
        return Math.max(Math.sqrt(x * x + y * y + z * z), 1.0E-9);
    }

    // Bakes the variant's layer once and grabs geometry
    private boolean ensureBaked(AbstractHorse horse) {
        ModelLayerLocation want = layerFor(horse);
        boolean sameLayer = want.equals(bakedLayer);
        if (sameLayer && baked) {
            return true;
        }
        long now = System.nanoTime();
        if (sameLayer && now < retryAtNanos) {
            return false;
        }

        ModelPart root = Minecraft.getInstance().getEntityModels().bakeLayer(want);
        boolean complete = root.hasChild("head_parts");
        Cuboid[] mouthRef = new Cuboid[1];
        List<MeshBox> found = new ArrayList<>();
        if (complete) {
            PartPose rootPose = root.getInitialPose();
            rootScale = rootPose.xScale();
            rootY = rootPose.y();
            headPose = root.getChild("head_parts").getInitialPose();

            PoseStack rootPs = new PoseStack();
            applyPose(rootPs, rootPose);
            Vector3f pivot = rootPs.last().pose()
                    .transformPosition(headPose.x() / 16.0F, headPose.y() / 16.0F, headPose.z() / 16.0F,
                            new Vector3f());
            pivotX = pivot.x;
            pivotY = pivot.y;
            pivotZ = pivot.z;

            List<ModelPart> parts = root.getAllParts();
            float[][] live = new float[parts.size()][];
            for (int i = 0; i < parts.size(); i++) {
                ModelPart part = parts.get(i);
                live[i] = new float[] {part.x, part.y, part.z, part.xRot, part.yRot, part.zRot,
                        part.xScale, part.yScale, part.zScale};
                part.resetPose();
            }
            root.visit(new PoseStack(), (pose, path, index, cube) -> {
                if (MOUTH_PATH.equals(path)) {
                    mouthRef[0] = new Cuboid(cube.minX, cube.minY, cube.minZ, cube.maxX, cube.maxY, cube.maxZ);
                }
                boolean head = path.startsWith(HEAD_PREFIX);
                boolean body = path.equals(BODY_PATH) || path.startsWith(BODY_PREFIX);
                if (head || body) {
                    MeshBox meshBox = meshBox(pose.pose(), cube, head);
                    if (meshBox != null) {
                        found.add(meshBox);
                    }
                }
            });
            for (int i = 0; i < parts.size(); i++) {
                ModelPart part = parts.get(i);
                float[] p = live[i];
                part.x = p[0];
                part.y = p[1];
                part.z = p[2];
                part.xRot = p[3];
                part.yRot = p[4];
                part.zRot = p[5];
                part.xScale = p[6];
                part.yScale = p[7];
                part.zScale = p[8];
            }
            complete = mouthRef[0] != null && !found.isEmpty();
        }
        if (!complete) {
            baked = false;
            bakedLayer = want;
            retryAtNanos = now + RETRY_NANOS;
            log.warn("Missing equine model parts in layer {}, rein visuals disabled (retrying every 10s)", want);
            return false;
        }
        boolean recovered = sameLayer;
        mouthCube = mouthRef[0];
        mesh = found.toArray(new MeshBox[0]);
        boxes = new RopeBox[mesh.length];
        baked = true;
        bakedLayer = want;
        if (recovered) {
            log.info("Equine model layer {} usable again, rein visuals back", want);
        }
        return true;
    }

    // Cube bounds and the composed baked transform
    private static MeshBox meshBox(Matrix4f matrix, ModelPart.Cube cube, boolean head) {
        float cx = (cube.minX + cube.maxX) * 0.5F / 16.0F;
        float cy = (cube.minY + cube.maxY) * 0.5F / 16.0F;
        float cz = (cube.minZ + cube.maxZ) * 0.5F / 16.0F;
        Vector3f center = matrix.transformPosition(cx, cy, cz, new Vector3f());
        float hx = (cube.maxX - cube.minX) * 0.5F / 16.0F;
        float hy = (cube.maxY - cube.minY) * 0.5F / 16.0F;
        float hz = (cube.maxZ - cube.minZ) * 0.5F / 16.0F;
        Vector3f ax = matrix.transformDirection(hx, 0.0F, 0.0F, new Vector3f());
        Vector3f ay = matrix.transformDirection(0.0F, hy, 0.0F, new Vector3f());
        Vector3f az = matrix.transformDirection(0.0F, 0.0F, hz, new Vector3f());
        return new MeshBox(head, center.x, center.y, center.z,
                ax.x, ax.y, ax.z, ay.x, ay.y, ay.z, az.x, az.y, az.z);
    }

    // Matches ModelPart.translateAndRotate
    private static void applyPose(PoseStack ps, PartPose pose) {
        ps.translate(pose.x() / 16.0F, pose.y() / 16.0F, pose.z() / 16.0F);
        ps.mulPose(new Quaternionf().rotationZYX(pose.zRot(), pose.yRot(), pose.xRot()));
        ps.scale(pose.xScale(), pose.yScale(), pose.zScale());
    }

    private static ModelLayerLocation layerFor(AbstractHorse horse) {
        EntityType<?> type = horse.getType();
        if (type == EntityType.DONKEY) {
            return ModelLayers.DONKEY;
        }
        if (type == EntityType.MULE) {
            return ModelLayers.MULE;
        }
        if (type == EntityType.SKELETON_HORSE) {
            return ModelLayers.SKELETON_HORSE;
        }
        if (type == EntityType.ZOMBIE_HORSE) {
            return ModelLayers.ZOMBIE_HORSE;
        }
        return ModelLayers.HORSE;
    }
}
