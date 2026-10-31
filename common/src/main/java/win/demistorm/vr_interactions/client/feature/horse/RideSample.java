package win.demistorm.vr_interactions.client.feature.horse;

import org.joml.Vector3dc;

public record RideSample(
        Vector3dc handAvg,
        int activeHands,
        Vector3dc anchorPos,
        double horseYawDeg,
        double playerYawDeg,
        Vector3dc horseVelocity
) {
}
