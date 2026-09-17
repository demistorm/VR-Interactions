package win.demistorm.vr_interactions.client.visual;

// Box collider for VerletRope
public final class RopeBox {
    public final double cx;
    public final double cy;
    public final double cz;
    public final double ux;
    public final double uy;
    public final double uz;
    public final double vx;
    public final double vy;
    public final double vz;
    public final double wx;
    public final double wy;
    public final double wz;
    public final double hx;
    public final double hy;
    public final double hz;

    public RopeBox(double cx, double cy, double cz,
                   double ux, double uy, double uz,
                   double vx, double vy, double vz,
                   double wx, double wy, double wz,
                   double hx, double hy, double hz) {
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
        this.ux = ux;
        this.uy = uy;
        this.uz = uz;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        this.wx = wx;
        this.wy = wy;
        this.wz = wz;
        this.hx = hx;
        this.hy = hy;
        this.hz = hz;
    }
}
