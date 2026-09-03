package win.demistorm.vr_interactions.client.feature.horse;

// All horse rings threshold tuneables in one place for convenience
public final class HorseRingsTuning {

    // Steering
    public static final double LATERAL_DEADZONE = 0.04;         // No turn below this offset from the zero anchor
    public static final double TURN_ENGAGE_OFFSET = 0.10;       // Offset that starts a turn once sustained
    public static final double TURN_DISENGAGE_OFFSET = 0.06;    // Turning stops once back inside this window
    public static final double TURN_RATE_SCALE = 23.4;          // Deg per tick per block of offset beyond the deadzone
    public static final double MAX_TURN_RATE_DEG = 5.85;        // Proportional rate cap, deg per tick
    public static final int TURN_SUSTAIN_BASE_TICKS = 4;        // Ticks the offset must hold before a turn engages
    public static final int TURN_SUSTAIN_PER_TIER = 2;          // Extra sustain ticks per sprint tier (turning is harder at higher tiers)
    public static final int TURN_GRACE_TICKS = 30;              // How long a turn after a disengage can start again without requiring another sustain

    // Window gestures (displacement over a window, one gesture at a time)
    public static final int GESTURE_WINDOW_TICKS = 5;           // Window for stop and jump checks
    public static final int GESTURE_COOLDOWN_TICKS = 5;         // Blocks overlapping gestures after completed one
    public static final double STOP_BACKWARD = 0.40;            // Backward distance in blocks in the window that stops the horse
    public static final double STOP_VERTICAL_QUIET = 0.35;      // Stop blocked while a vertical gesture this big is in the window
    public static final double STOP_LATERAL_QUIET = 0.30;       // Stop blocked while a lateral gesture this big is in the window
    public static final int STOP_QUIET_LOOKBACK_TICKS = 10;     // Checks vertical/lateral quiet movement this far back before a stop to prevent accidental gestures
    public static final int GIDDY_UP_WINDOW_TICKS = 10;         // Giddys have their own window since the gesture can take longer
    public static final double GIDDY_UP_RISE = 0.20;            // Minimum up leg of a pump
    public static final double GIDDY_UP_MAX_RISE = 0.60;        // Maximum up leg of a pump (jump above this)
    public static final double GIDDY_UP_FALL = 0.20;            // Minimum return down from the peak
    public static final double JUMP_RISE = 0.60;                // Rise in the window that fires a jump

    // Jump
    public static final double JUMP_CHARGE = 0.95;              // Jump strength (0 to 1)
    public static final int JUMP_REACTION_BASE_TICKS = 0;       // Gesture to jump delay (base)
    public static final int JUMP_REACTION_PER_TIER = 1;         // Extra delay per sprint tier

    // Stop speed ramp down
    public static final int STOP_RAMP_BASE_TICKS = 3;           // Forward impulse ramp down time at walk
    public static final int STOP_RAMP_PER_TIER = 4;             // Extra ramp ticks per sprint tier

    // Sprint ladder
    public static final int GIDDY_CHAIN_TICKS = 40;             // Another pump in this many ticks raises the speed tier
    public static final int SPRINT_GRANT_TICKS = 200;           // Ticks each giddy-up grants before the tier drops
    public static final double WALK_MULTIPLIER = 0.60;          // Speed multiplier at walk
    public static final double TROT_MULTIPLIER = 1.00;          // Speed multiplier at trot (1.00 is vanilla speed)
    public static final double GALLOP_MULTIPLIER = 1.30;        // Speed multiplier at gallop

    // Calibration
    public static final double LATERAL_ZERO_EMA = 0.02;         // Lateral zero anchor adaptation rate (recenters hand average when not performing turns or gestures, helps with roomscale drift still feeling good)
    public static final double ROOM_LOCK_WINDOW = 0.30;         // Distanc in blocks the player can move in roomscale before it locks the view

    // Yaw recenter (horse slowly rotates to the player's look direction when steering has been quiet)
    public static final int RECENTER_IDLE_TICKS = 30;           // No turn activity before recentering starts
    public static final double RECENTER_MIN_DIFF_DEG = 20.0;    // Divergence that starts a recenter
    public static final double RECENTER_TOLERANCE_DEG = 8.0;    // Divergence that ends it
    public static final double RECENTER_RATE_DEG = 1.2;         // Deg per tick while recentering (speed of recentering)
}
