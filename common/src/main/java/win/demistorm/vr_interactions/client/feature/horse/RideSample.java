package win.demistorm.vr_interactions.client.feature.horse;

import org.joml.Vector3dc;

// One tick of horse-relative input data, the logic keeps its own history of these
// handAvg is the world space average of the empty hands, anchorPos the rider position it derives from (same tick, keeps rider motion out of the local frame)
public record RideSample(
        Vector3dc handAvg,
        int activeHands,
        Vector3dc anchorPos,
        double horseYawDeg,
        double playerYawDeg,
        Vector3dc horseVelocity
) {
}
