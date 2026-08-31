package win.demistorm.vr_interactions.client.interaction;

import org.joml.Quaternionfc;
import org.joml.Vector3dc;

// Immutable per-tick head pose in world space (anchor for body-attached zones)
public record HeadInfo(Vector3dc pos, Vector3dc dir, Quaternionfc rotation) {
}
