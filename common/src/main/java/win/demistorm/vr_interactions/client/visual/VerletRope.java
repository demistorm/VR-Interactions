package win.demistorm.vr_interactions.client.visual;

import java.util.Arrays;

public final class VerletRope {

    private final int pointCount;

    private final double[] restLen;
    private final double collisionRadius;
    private final double[] cur;
    private final double[] prev;

    private final double[] tickStart;
    private final double[] debugStart;
    private final boolean[] pinned;
    private final double[] pinX;
    private final double[] pinY;
    private final double[] pinZ;

    public VerletRope(int pointCount, double segmentLength, double collisionRadius) {
        this.pointCount = pointCount;
        this.restLen = new double[pointCount - 1];
        Arrays.fill(restLen, segmentLength);
        this.collisionRadius = collisionRadius;
        this.cur = new double[pointCount * 3];
        this.prev = new double[pointCount * 3];
        this.tickStart = new double[pointCount * 3];
        this.debugStart = new double[pointCount * 3];
        this.pinned = new boolean[pointCount];
        this.pinX = new double[pointCount];
        this.pinY = new double[pointCount];
        this.pinZ = new double[pointCount];
    }

    public int pointCount() {
        return pointCount;
    }

    public void seed(int index, double x, double y, double z) {
        int i = index * 3;
        cur[i] = x;
        cur[i + 1] = y;
        cur[i + 2] = z;
        prev[i] = x;
        prev[i + 1] = y;
        prev[i + 2] = z;
        tickStart[i] = x;
        tickStart[i + 1] = y;
        tickStart[i + 2] = z;
        debugStart[i] = x;
        debugStart[i + 1] = y;
        debugStart[i + 2] = z;
    }

    public void setPin(int index, double x, double y, double z) {
        pinned[index] = true;
        pinX[index] = x;
        pinY[index] = y;
        pinZ[index] = z;
    }

    public void clearPin(int index) {
        pinned[index] = false;
    }

    public void beginTick() {
        System.arraycopy(cur, 0, tickStart, 0, cur.length);
        System.arraycopy(cur, 0, debugStart, 0, cur.length);
    }

    public void step(double dt, double gravityY, double damping, double maxSpeed, int substeps, int iterations,
                      RopeBox[] boxes, double friction) {
        double dtSq = dt * dt;
        for (int p = 0; p < pointCount; p++) {
            if (pinned[p]) {
                continue;
            }
            int i = p * 3;
            double vx = (cur[i] - prev[i]) * damping;
            double vy = (cur[i + 1] - prev[i + 1]) * damping;
            double vz = (cur[i + 2] - prev[i + 2]) * damping;
            double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
            if (speed > maxSpeed) {
                double scale = maxSpeed / speed;
                vx *= scale;
                vy *= scale;
                vz *= scale;
            }
            prev[i] = cur[i];
            prev[i + 1] = cur[i + 1];
            prev[i + 2] = cur[i + 2];
            cur[i] += vx;
            cur[i + 1] += vy;
            cur[i + 2] += vz;
            cur[i + 1] += gravityY * dtSq;
        }
        for (int s = 0; s < substeps; s++) {
            solve(cur, iterations, boxes, s == substeps - 1 ? friction : 0.0);
        }
        clampImpliedVelocity(maxSpeed);
    }

    private void clampImpliedVelocity(double maxSpeed) {
        double maxSq = maxSpeed * maxSpeed;
        for (int p = 0; p < pointCount; p++) {
            if (pinned[p]) {
                continue;
            }
            int i = p * 3;
            double vx = cur[i] - prev[i];
            double vy = cur[i + 1] - prev[i + 1];
            double vz = cur[i + 2] - prev[i + 2];
            double speedSq = vx * vx + vy * vy + vz * vz;
            if (speedSq > maxSq) {
                double scale = maxSpeed / Math.sqrt(speedSq);
                prev[i] = cur[i] - vx * scale;
                prev[i + 1] = cur[i + 1] - vy * scale;
                prev[i + 2] = cur[i + 2] - vz * scale;
            }
        }
    }

    public void renderLerp(float alpha, double[] out) {
        for (int i = 0; i < pointCount * 3; i++) {
            out[i] = tickStart[i] + (cur[i] - tickStart[i]) * alpha;
        }
    }

    private void solve(double[] pts, int iterations, RopeBox[] boxes, double friction) {
        for (int it = 0; it < iterations; it++) {
            applyPins(pts);
            relaxSegments(pts);
            collide(pts, boxes, it == iterations - 1 ? friction : 0.0);
        }
        applyPins(pts);
    }

    private void applyPins(double[] pts) {
        for (int p = 0; p < pointCount; p++) {
            if (pinned[p]) {
                int i = p * 3;
                pts[i] = pinX[p];
                pts[i + 1] = pinY[p];
                pts[i + 2] = pinZ[p];
            }
        }
    }

    private void relaxSegments(double[] pts) {
        for (int p = 0; p < pointCount - 1; p++) {
            int i = p * 3;
            int j = i + 3;
            double dx = pts[j] - pts[i];
            double dy = pts[j + 1] - pts[i + 1];
            double dz = pts[j + 2] - pts[i + 2];
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1.0E-9) {
                continue;
            }
            double err = (len - restLen[p]) / len;
            boolean aPinned = pinned[p];
            boolean bPinned = pinned[p + 1];
            if (aPinned && bPinned) {
                continue;
            }
            if (aPinned) {
                pts[j] -= dx * err;
                pts[j + 1] -= dy * err;
                pts[j + 2] -= dz * err;
            } else if (bPinned) {
                pts[i] += dx * err;
                pts[i + 1] += dy * err;
                pts[i + 2] += dz * err;
            } else {
                double half = err * 0.5;
                pts[i] += dx * half;
                pts[i + 1] += dy * half;
                pts[i + 2] += dz * half;
                pts[j] -= dx * half;
                pts[j + 1] -= dy * half;
                pts[j + 2] -= dz * half;
            }
        }
    }

    public void setSectionRestLength(int fromIdx, int toIdx, double total) {
        double per = total / (toIdx - fromIdx);
        for (int p = fromIdx; p < toIdx; p++) {
            restLen[p] = per;
        }
    }

    private void collide(double[] pts, RopeBox[] boxes, double friction) {
        for (int p = 0; p < pointCount; p++) {
            if (pinned[p]) {
                continue;
            }
            int i = p * 3;
            for (RopeBox box : boxes) {
                if (box == null) {
                    continue;
                }
                double dx = pts[i] - box.cx;
                double dy = pts[i + 1] - box.cy;
                double dz = pts[i + 2] - box.cz;
                double a = dx * box.ux + dy * box.uy + dz * box.uz;
                double b = dx * box.vx + dy * box.vy + dz * box.vz;
                double c = dx * box.wx + dy * box.wy + dz * box.wz;
                double ac = clamp(a, -box.hx, box.hx);
                double bc = clamp(b, -box.hy, box.hy);
                double cc = clamp(c, -box.hz, box.hz);
                double nx;
                double ny;
                double nz;
                double push;
                if (a != ac || b != bc || c != cc) {
                    double qx = box.cx + box.ux * ac + box.vx * bc + box.wx * cc;
                    double qy = box.cy + box.uy * ac + box.vy * bc + box.wy * cc;
                    double qz = box.cz + box.uz * ac + box.vz * bc + box.wz * cc;
                    nx = pts[i] - qx;
                    ny = pts[i + 1] - qy;
                    nz = pts[i + 2] - qz;
                    double distSq = nx * nx + ny * ny + nz * nz;
                    if (distSq >= collisionRadius * collisionRadius || distSq < 1.0E-12) {
                        continue;
                    }
                    double dist = Math.sqrt(distSq);
                    nx /= dist;
                    ny /= dist;
                    nz /= dist;
                    push = collisionRadius - dist;
                } else {
                    double pu = box.hx - Math.abs(a);
                    double pv = box.hy - Math.abs(b);
                    double pw = box.hz - Math.abs(c);
                    if (pu <= pv && pu <= pw) {
                        double s = a < 0.0 ? -1.0 : 1.0;
                        nx = box.ux * s;
                        ny = box.uy * s;
                        nz = box.uz * s;
                        push = pu + collisionRadius;
                    } else if (pv <= pw) {
                        double s = b < 0.0 ? -1.0 : 1.0;
                        nx = box.vx * s;
                        ny = box.vy * s;
                        nz = box.vz * s;
                        push = pv + collisionRadius;
                    } else {
                        double s = c < 0.0 ? -1.0 : 1.0;
                        nx = box.wx * s;
                        ny = box.wy * s;
                        nz = box.wz * s;
                        push = pw + collisionRadius;
                    }
                }
                pts[i] += nx * push;
                pts[i + 1] += ny * push;
                pts[i + 2] += nz * push;
                if (friction > 0.0) {
                    double vx = pts[i] - prev[i];
                    double vy = pts[i + 1] - prev[i + 1];
                    double vz = pts[i + 2] - prev[i + 2];
                    double vn = vx * nx + vy * ny + vz * nz;
                    prev[i] += (vx - nx * vn) * friction;
                    prev[i + 1] += (vy - ny * vn) * friction;
                    prev[i + 2] += (vz - nz * vn) * friction;
                }
            }
        }
    }

    private static double clamp(double v, double min, double max) {
        return v < min ? min : Math.min(v, max);
    }

    public String debugOutliers(double limit) {
        StringBuilder sb = null;
        double limitSq = limit * limit;
        for (int p = 1; p < pointCount; p++) {
            int i = p * 3;
            double dx = cur[i] - cur[0];
            double dy = cur[i + 1] - cur[1];
            double dz = cur[i + 2] - cur[2];
            if (dx * dx + dy * dy + dz * dz > limitSq) {
                if (sb == null) {
                    sb = new StringBuilder("Rope outliers after step:");
                }
                sb.append(String.format(" p%d cur=(%.2f,%.2f,%.2f) prev=(%.2f,%.2f,%.2f) rest=%s/%s pinned=%b",
                        p, cur[i], cur[i + 1], cur[i + 2], prev[i], prev[i + 1], prev[i + 2],
                        String.format("%.3f", restLen[p - 1]),
                        p < pointCount - 1 ? String.format("%.3f", restLen[p]) : "-",
                        pinned[p]));
            }
        }
        return sb == null ? null : sb.toString();
    }

    public String debugStats(RopeBox[] boxes) {
        double maxDispSq = 0.0;
        int maxIdx = -1;
        boolean nan = false;
        int inside = 0;
        StringBuilder insideIdx = new StringBuilder();
        for (int p = 0; p < pointCount; p++) {
            int i = p * 3;
            if (!Double.isFinite(cur[i]) || !Double.isFinite(cur[i + 1]) || !Double.isFinite(cur[i + 2])) {
                nan = true;
            }
            double dx = cur[i] - debugStart[i];
            double dy = cur[i + 1] - debugStart[i + 1];
            double dz = cur[i + 2] - debugStart[i + 2];
            double dispSq = dx * dx + dy * dy + dz * dz;
            if (dispSq > maxDispSq) {
                maxDispSq = dispSq;
                maxIdx = p;
            }
            if (!pinned[p] && insideCore(cur[i], cur[i + 1], cur[i + 2], boxes)) {
                inside++;
                if (inside <= 3) {
                    if (!insideIdx.isEmpty()) {
                        insideIdx.append(',');
                    }
                    insideIdx.append(p);
                }
            }
        }
        return "maxDisp=" + String.format("%.3f", Math.sqrt(maxDispSq)) + "@" + maxIdx
                + " inside=" + inside + (!insideIdx.isEmpty() ? "[" + insideIdx + "]" : "")
                + " nan=" + nan;
    }

    private boolean insideCore(double x, double y, double z, RopeBox[] boxes) {
        for (RopeBox box : boxes) {
            if (box == null) {
                continue;
            }
            double dx = x - box.cx;
            double dy = y - box.cy;
            double dz = z - box.cz;
            double a = dx * box.ux + dy * box.uy + dz * box.uz;
            if (Math.abs(a) > box.hx) {
                continue;
            }
            double b = dx * box.vx + dy * box.vy + dz * box.vz;
            if (Math.abs(b) > box.hy) {
                continue;
            }
            double c = dx * box.wx + dy * box.wy + dz * box.wz;
            if (Math.abs(c) <= box.hz) {
                return true;
            }
        }
        return false;
    }
}
