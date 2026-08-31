package win.demistorm.vr_interactions.client.interaction;

import org.joml.Quaternionfc;
import org.joml.Vector3dc;

// Immutable per-tick hand pose in world space (velocity is averaged over a small window)
public record HandInfo(Vector3dc pos, Vector3dc dir, Quaternionfc rotation, Vector3dc velocity) {

    // Average speed in blocks per tick
    public double speed() {
        return velocity.length();
    }
}
