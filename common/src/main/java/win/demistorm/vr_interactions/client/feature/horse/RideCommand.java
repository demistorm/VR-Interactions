package win.demistorm.vr_interactions.client.feature.horse;

// What the horse should do this tick (read by AbstractHorse mixin through HorseRingsFeature)
public record RideCommand(
        boolean active,
        float yawDeg,
        float forwardImpulse,
        float speedMultiplier,
        int tier,
        int sprintTicksRemaining,
        boolean jumpFired,
        int events,
        int activeHands,
        double lateralOffset,
        boolean turnEngaged,
        int sustainRemaining,
        int decayTicksRemaining,
        int stopRampRemaining,
        int jumpPending
) {

    public static final int EVENT_TURN_ENGAGED = 1;
    public static final int EVENT_TIER_UP = 2;
    public static final int EVENT_STOP = 4;
    public static final int EVENT_JUMP = 8;
    public static final int EVENT_TIER_SUSTAIN = 16;

    public static final RideCommand INACTIVE = new RideCommand(false, 0.0f, 0.0f, 1.0f,
            0, 0, false, 0, 0, 0.0, false, 0, 0, 0, 0);
}
