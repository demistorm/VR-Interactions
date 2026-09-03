package win.demistorm.vr_interactions.client.feature.horse;

import org.joml.Vector3d;
import org.joml.Vector3dc;

// Horse logic (no Minecraft code so as to hopefully make porting simpler)
public final class HorseRingsLogic {

    private static final int BUFFER_SIZE = 32;

    private boolean active = false;
    private boolean needAnchor = true;

    // Worldspace sample history plus the current tick's frame axes
    private final double[] worldX = new double[BUFFER_SIZE];
    private final double[] worldY = new double[BUFFER_SIZE];
    private final double[] worldZ = new double[BUFFER_SIZE];
    private double frameSin = 0.0;
    private double frameCos = 1.0;
    private int bufferPos = 0;
    private int bufferCount = 0;

    // Lateral zero anchor (where the rider's hands rest centered)
    private double lateralZero = 0.0;

    // Steering
    private boolean turnEngaged = false;
    private int sustainTicks = 0;
    private int turnGraceTicks = 0;
    private double lastTurnRate = 0.0;
    private double lastLateral = 0.0;

    // Yaw recenter (rotate the horse to the rider's look direction when steering is inactive)
    private int recenterIdleTicks = 0;
    private boolean recenterEngaged = false;
    private double lastRecenterDiff = 0.0;

    // Gestures (one action at a time, cooldown blocks overlaps)
    private int gestureCooldown = 0;
    private int jumpPending = 0;
    private boolean jumpFireNow = false;

    // Speed ladder and stop ramp
    private int tier = 0;
    private int sprintTicks = 0;
    private int ticksSinceGiddy = 0;
    private boolean stopping = false;
    private int stopRampRemaining = 0;
    private int stopRampTotal = 1;
    private double stopMultStart = 1.0;

    private RideCommand command = RideCommand.INACTIVE;

    public RideCommand command() {
        return command;
    }

    // Debug stats
    public String debugState() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("y=%+.3f z=%+.3f lat=%+.3f", vertical(0), longitudinal(0), lastLateral));
        int window = HorseRingsTuning.GESTURE_WINDOW_TICKS;
        int lookback = HorseRingsTuning.STOP_QUIET_LOOKBACK_TICKS;
        if (bufferCount > window) {
            double peak = maxVertical(window);
            sb.append(String.format(" rise=%.3f fall=%.3f lonB=%.3f",
                    peak - vertical(window), peak - vertical(0), longitudinal(window) - longitudinal(0)));
        } else {
            sb.append(" rise=- fall=- lonB=-");
        }
        if (bufferCount > lookback) {
            sb.append(String.format(" quiet=%.3f latQ=%.3f",
                    maxVertical(lookback) - vertical(lookback), lateralRange(lookback)));
        }
        sb.append(" tier=").append(tier)
                .append(" sprint=").append(sprintTicks)
                .append(" cd=").append(gestureCooldown)
                .append(" pend=").append(jumpPending)
                .append(" turn=").append(turnEngaged ? 1 : 0)
                .append(" grace=").append(turnGraceTicks)
                .append(String.format(" rc=%.0f", lastRecenterDiff));
        return sb.toString();
    }

    public void deactivate() {
        active = false;
        command = RideCommand.INACTIVE;
    }

    public void tick(RideSample sample) {
        if (!active) {
            active = true;
            resetGestureState();
            tier = 0;
            sprintTicks = 0;
            ticksSinceGiddy = 0;
            stopping = false;
            lastTurnRate = 0.0;
        } else if (sample.activeHands() != command.activeHands()) {
            resetGestureState();
        }
        pushSample(sample);

        int events = 0;
        double lat = anchorLateral();

        updateZeroAnchor(lat);

        boolean wasTurnEngaged = turnEngaged;
        double turnRate = tickSteering(lat);
        if (!wasTurnEngaged && turnEngaged) {
            events |= RideCommand.EVENT_TURN_ENGAGED;
        }
        events |= tickGestures();
        boolean fired = jumpFireNow;
        jumpFireNow = false;
        fired |= tickJumpPending();
        if (fired) {
            events |= RideCommand.EVENT_JUMP;
        }
        boolean giddyUp = giddyUpAccepted;
        giddyUpAccepted = false;
        tickLadder(giddyUp);
        if (giddyUp) {
            events |= giddyEscalated ? RideCommand.EVENT_TIER_UP : RideCommand.EVENT_TIER_SUSTAIN;
        }

        double impulse = forwardImpulse();
        double mult = speedMultiplier();
        double yaw = wrapDegrees(sample.horseYawDeg() + turnRate);
        yaw = tickRecenter(yaw, sample.playerYawDeg());

        command = new RideCommand(true, (float) yaw, (float) impulse, (float) mult,
                tier, sprintTicks, fired, events, sample.activeHands(),
                lat, turnEngaged, sustainTicks, ladderTicksRemaining(),
                stopping ? stopRampRemaining : 0, jumpPending);
    }

    // No active hands (horse keeps its last command until decay stops it)
    public void tickIdle(double horseYawDeg, double playerYawDeg) {
        if (!active) {
            active = true;
            resetGestureState();
            tier = 0;
            sprintTicks = 0;
            ticksSinceGiddy = 0;
            stopping = false;
            lastTurnRate = 0.0;
        }
        turnEngaged = false;
        sustainTicks = 0;
        if (turnGraceTicks > 0) {
            turnGraceTicks--;
        }
        tickJumpPending();
        tickLadder(false);

        double impulse = forwardImpulse();
        double mult = speedMultiplier();
        double yaw = wrapDegrees(horseYawDeg + lastTurnRate);
        yaw = tickRecenter(yaw, playerYawDeg);

        command = new RideCommand(true, (float) yaw, (float) impulse, (float) mult,
                tier, sprintTicks, false, 0, 0,
                lastLateral, turnEngaged, sustainTicks, ladderTicksRemaining(),
                stopping ? stopRampRemaining : 0, 0);
    }

    // Movement ticks left before the ladder decays to a stop
    private int ladderTicksRemaining() {
        return tier > 0 ? sprintTicks + (tier - 1) * HorseRingsTuning.SPRINT_GRANT_TICKS : 0;
    }

    // Sustained lateral offset past the threshold turns until offset returns inside the disengage window
    // (a rapid reengage does not trigger sustain so weaving feels fluid)
    private double tickSteering(double lat) {
        lastLateral = lat;
        double absLat = Math.abs(lat);
        if (turnEngaged) {
            if (absLat <= HorseRingsTuning.TURN_DISENGAGE_OFFSET) {
                turnEngaged = false;
                sustainTicks = 0;
                lastTurnRate = 0.0;
                turnGraceTicks = HorseRingsTuning.TURN_GRACE_TICKS;
            } else {
                double rate = Math.min(HorseRingsTuning.MAX_TURN_RATE_DEG,
                        HorseRingsTuning.TURN_RATE_SCALE * Math.max(0.0, absLat - HorseRingsTuning.LATERAL_DEADZONE));
                lastTurnRate = Math.copySign(rate, lat);
            }
        } else {
            if (absLat >= HorseRingsTuning.TURN_ENGAGE_OFFSET) {
                if (turnGraceTicks > 0) {
                    turnEngaged = true;
                    sustainTicks = 0;
                    turnGraceTicks = 0;
                } else {
                    sustainTicks++;
                    int required = HorseRingsTuning.TURN_SUSTAIN_BASE_TICKS + tier * HorseRingsTuning.TURN_SUSTAIN_PER_TIER;
                    sustainTicks = Math.min(sustainTicks, required);
                    if (sustainTicks >= required) {
                        turnEngaged = true;
                        sustainTicks = 0;
                    }
                }
            } else {
                sustainTicks = 0;
                if (turnGraceTicks > 0) {
                    turnGraceTicks--;
                }
            }
            lastTurnRate = 0.0;
        }
        return lastTurnRate;
    }

    // Yaw recenter (rotate the horse to the rider's look direction when steering is inactive)
    private double tickRecenter(double yaw, double playerYawDeg) {
        if (turnEngaged || turnGraceTicks > 0 || sustainTicks > 0) {
            recenterIdleTicks = 0;
            recenterEngaged = false;
        } else {
            recenterIdleTicks++;
        }
        double diff = wrapDegrees(playerYawDeg - yaw);
        lastRecenterDiff = diff;
        if (recenterIdleTicks < HorseRingsTuning.RECENTER_IDLE_TICKS) {
            return yaw;
        }
        if (!recenterEngaged && Math.abs(diff) > HorseRingsTuning.RECENTER_MIN_DIFF_DEG) {
            recenterEngaged = true;
        }
        if (recenterEngaged) {
            if (Math.abs(diff) <= HorseRingsTuning.RECENTER_TOLERANCE_DEG) {
                recenterEngaged = false;
            } else {
                yaw = wrapDegrees(yaw + Math.copySign(
                        Math.min(Math.abs(diff), HorseRingsTuning.RECENTER_RATE_DEG), diff));
            }
        }
        return yaw;
    }

    // All gestures, returns event bits (accepting one gesture starts the cooldown)
    private boolean giddyUpAccepted = false;

    private int tickGestures() {
        int window = HorseRingsTuning.GESTURE_WINDOW_TICKS;
        if (bufferCount <= window) {
            return 0;
        }
        if (gestureCooldown > 0) {
            gestureCooldown--;
        }
        // One action at a time, a pending jump fire also blocks new gestures
        if (gestureCooldown > 0 || jumpPending > 0) {
            return 0;
        }

        double peak = maxVertical(window);
        double verticalSpan = peak - vertical(window);

        // Jump: Offset above JUMP_RISE in the window jumps immediately
        if (verticalSpan > HorseRingsTuning.JUMP_RISE) {
            int delay = HorseRingsTuning.JUMP_REACTION_BASE_TICKS + tier * HorseRingsTuning.JUMP_REACTION_PER_TIER;
            if (delay > 0) {
                jumpPending = delay;
            } else {
                jumpFireNow = true;
            }
            gestureCooldown = HorseRingsTuning.GESTURE_COOLDOWN_TICKS;
            return 0;
        }

        // Stop: Sharp backward pull toward the player (blocked if gesture moves too vertical/horizontal)
        int lookback = HorseRingsTuning.STOP_QUIET_LOOKBACK_TICKS;
        if (tier > 0 && !stopping && bufferCount > lookback
                && maxVertical(lookback) - vertical(lookback) < HorseRingsTuning.STOP_VERTICAL_QUIET
                && lateralRange(lookback) < HorseRingsTuning.STOP_LATERAL_QUIET
                && longitudinal(window) - longitudinal(0) >= HorseRingsTuning.STOP_BACKWARD) {
            triggerStop(HorseRingsTuning.STOP_RAMP_BASE_TICKS + tier * HorseRingsTuning.STOP_RAMP_PER_TIER);
            gestureCooldown = HorseRingsTuning.GESTURE_COOLDOWN_TICKS;
            return RideCommand.EVENT_STOP;
        }

        // Giddy-up: Medium sizes up/down pump motion (has a longer gesture window since it can take longer)
        int gWindow = HorseRingsTuning.GIDDY_UP_WINDOW_TICKS;
        if (bufferCount > gWindow) {
            double gPeak = maxVertical(gWindow);
            double gRise = gPeak - vertical(gWindow);
            double gFall = gPeak - vertical(0);
            if (gRise >= HorseRingsTuning.GIDDY_UP_RISE && gRise < HorseRingsTuning.GIDDY_UP_MAX_RISE
                    && gFall >= HorseRingsTuning.GIDDY_UP_FALL) {
                giddyUpAccepted = true;
                gestureCooldown = HorseRingsTuning.GESTURE_COOLDOWN_TICKS;
            }
        }
        return 0;
    }

    private boolean tickJumpPending() {
        if (jumpPending > 0) {
            jumpPending--;
            if (jumpPending == 0) {
                return true;
            }
        }
        return false;
    }

    private boolean giddyEscalated = false;

    private void tickLadder(boolean giddyUp) {
        ticksSinceGiddy++;
        giddyEscalated = false;
        if (giddyUp) {
            stopping = false;
            boolean chained = ticksSinceGiddy <= HorseRingsTuning.GIDDY_CHAIN_TICKS;
            ticksSinceGiddy = 0;
            if (tier == 0) {
                tier = 1;
                giddyEscalated = true;
            } else if (chained) {
                // Quick consecutive pumps climb tiers
                tier = Math.min(3, tier + 1);
                giddyEscalated = true;
            }
            sprintTicks = HorseRingsTuning.SPRINT_GRANT_TICKS;
            return;
        }
        if (stopping) {
            stopRampRemaining--;
            if (stopRampRemaining <= 0) {
                stopping = false;
                stopRampRemaining = 0;
            }
            return;
        }
        if (tier > 0) {
            sprintTicks--;
            if (sprintTicks <= 0) {
                tier--;
                if (tier > 0) {
                    sprintTicks = HorseRingsTuning.SPRINT_GRANT_TICKS;
                } else {
                    triggerStop(HorseRingsTuning.STOP_RAMP_BASE_TICKS);
                }
            }
        }
    }

    private void triggerStop(int rampTicks) {
        if (stopping) {
            return;
        }
        stopping = true;
        stopRampTotal = Math.max(1, rampTicks);
        stopRampRemaining = stopRampTotal;
        stopMultStart = tierMultiplier(tier);
        tier = 0;
        sprintTicks = 0;
    }

    private double forwardImpulse() {
        if (stopping) {
            double progress = 1.0 - (double) stopRampRemaining / stopRampTotal;
            return 1.0 - progress;
        }
        return tier > 0 ? 1.0 : 0.0;
    }

    private double speedMultiplier() {
        if (stopping) {
            double progress = 1.0 - (double) stopRampRemaining / stopRampTotal;
            return stopMultStart + (1.0 - stopMultStart) * progress;
        }
        return tierMultiplier(tier);
    }

    private double tierMultiplier(int tier) {
        return switch (tier) {
            case 1 -> HorseRingsTuning.WALK_MULTIPLIER;
            case 2 -> HorseRingsTuning.TROT_MULTIPLIER;
            case 3 -> HorseRingsTuning.GALLOP_MULTIPLIER;
            default -> 1.0;
        };
    }

    // Rezeros anchor if hands aren't moving much (keeps things in sync)
    private void updateZeroAnchor(double lat) {
        if (jumpPending > 0 || turnEngaged) {
            return;
        }
        int window = HorseRingsTuning.GESTURE_WINDOW_TICKS;
        if (bufferCount <= window
                || Math.abs(vertical(0) - vertical(window)) >= 0.03
                || Math.abs(lateral(0) - lateral(window)) >= 0.03) {
            return;
        }
        if (Math.abs(lat) < HorseRingsTuning.TURN_ENGAGE_OFFSET) {
            lateralZero += HorseRingsTuning.LATERAL_ZERO_EMA * (lateral(0) - lateralZero);
        }
    }

    private void resetGestureState() {
        bufferPos = 0;
        bufferCount = 0;
        needAnchor = true;
        turnEngaged = false;
        sustainTicks = 0;
        turnGraceTicks = 0;
        lastTurnRate = 0.0;
        gestureCooldown = 0;
        jumpPending = 0;
        jumpFireNow = false;
        giddyUpAccepted = false;
        giddyEscalated = false;
        recenterIdleTicks = 0;
        recenterEngaged = false;
    }

    private void pushSample(RideSample sample) {
        Vector3dc rel = sample.handAvg().sub(sample.anchorPos(), new Vector3d());
        worldX[bufferPos] = rel.x();
        worldY[bufferPos] = rel.y();
        worldZ[bufferPos] = rel.z();
        bufferPos = (bufferPos + 1) % BUFFER_SIZE;
        bufferCount = Math.min(bufferCount + 1, BUFFER_SIZE);
        double yawRad = Math.toRadians(sample.horseYawDeg());
        frameSin = Math.sin(yawRad);
        frameCos = Math.cos(yawRad);
    }

    private double anchorLateral() {
        if (needAnchor && bufferCount > 0) {
            lateralZero = lateral(0);
            needAnchor = false;
            return 0.0;
        }
        return lateral(0) - lateralZero;
    }

    private double lateral(int ticksBack) {
        double x = worldX[index(ticksBack)];
        double z = worldZ[index(ticksBack)];
        return -frameCos * x - frameSin * z;
    }

    private double vertical(int ticksBack) {
        return bufferCount == 0 ? 0.0 : worldY[index(ticksBack)];
    }

    private double longitudinal(int ticksBack) {
        double x = worldX[index(ticksBack)];
        double z = worldZ[index(ticksBack)];
        return -frameSin * x + frameCos * z;
    }

    private double maxVertical(int ticksBack) {
        double max = vertical(0);
        for (int i = 1; i <= ticksBack; i++) {
            max = Math.max(max, vertical(i));
        }
        return max;
    }

    private double lateralRange(int ticksBack) {
        double min = lateral(0);
        double max = min;
        for (int i = 1; i <= ticksBack; i++) {
            double v = lateral(i);
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        return max - min;
    }

    private int index(int ticksBack) {
        return (bufferPos - 1 - ticksBack + BUFFER_SIZE * 2) % BUFFER_SIZE;
    }

    private static double wrapDegrees(double deg) {
        deg = deg % 360.0;
        if (deg >= 180.0) {
            deg -= 360.0;
        } else if (deg < -180.0) {
            deg += 360.0;
        }
        return deg;
    }
}
