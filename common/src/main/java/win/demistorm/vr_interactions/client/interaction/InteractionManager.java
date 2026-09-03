package win.demistorm.vr_interactions.client.interaction;

import org.joml.Vector3dc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

// Central hand manager (pick what gets a hand and features decide what to do with the input)
public final class InteractionManager {

    public static final InteractionManager INSTANCE = new InteractionManager();

    private static final Logger log = LoggerFactory.getLogger(InteractionManager.class);

    // Time between debug perf logs
    private static final int PERF_LOG_INTERVAL_TICKS = 600;

    public enum HandState { IDLE, GRABBABLE, GRABBED }

    private final List<Interaction> interactions = new ArrayList<>();
    private final List<AmbientChecker> ambientCheckers = new ArrayList<>();
    private final EnumMap<Hand, PerHand> hands = new EnumMap<>(Hand.class);
    private final List<InteractZone> currentZones = new ArrayList<>();

    private Haptics haptics = Haptics.NOOP;
    private TickContext lastContext = null;
    private long tick = 0;
    private boolean stealMainInput = false;
    private boolean stealOffhandInput = false;
    private boolean hoverBuzz = true;
    private boolean perfLog = false;
    private long perfTicks = 0;
    private long perfNanos = 0;
    private long perfMaxNanos = 0;
    private int perfMaxZones = 0;

    private InteractionManager() {
        for (Hand hand : Hand.values()) {
            hands.put(hand, new PerHand());
        }
    }

    public void register(Interaction interaction) {
        interactions.add(interaction);
    }

    public void registerAmbient(AmbientChecker checker) {
        ambientCheckers.add(checker);
    }

    public void setHaptics(Haptics haptics) {
        this.haptics = haptics;
    }

    public void setPerfLogEnabled(boolean enabled) {
        this.perfLog = enabled;
        clearPerfWindow();
    }

    public boolean stealsMainInput() {
        return stealMainInput;
    }

    public boolean stealsOffhandInput() {
        return stealOffhandInput;
    }

    // Zones checked on the last tick (for debug rendering)
    public List<InteractZone> currentZones() {
        return Collections.unmodifiableList(currentZones);
    }

    public HandState stateOf(Hand hand) {
        return hands.get(hand).state;
    }

    // Zone the hand is hovering or grabbed on (null when idle)
    public InteractZone zoneOf(Hand hand) {
        return hands.get(hand).zone;
    }

    public void tick(TickContext ctx) {
        long perfStart = perfLog ? System.nanoTime() : 0L;

        lastContext = ctx;
        tick = ctx.tick();

        currentZones.clear();
        for (Interaction interaction : interactions) {
            if (!interaction.enabled()) {
                continue;
            }
            try {
                currentZones.addAll(interaction.zones(ctx));
            } catch (Throwable t) {
                log.error("Interaction {} threw while publishing zones", interaction.id(), t);
            }
        }
        currentZones.sort(Comparator.comparingInt(InteractZone::priority));

        for (AmbientChecker checker : ambientCheckers) {
            if (!checker.enabled()) {
                continue;
            }
            try {
                checker.tick(ctx);
            } catch (Throwable t) {
                log.error("AmbientChecker threw during tick", t);
            }
        }

        for (Hand hand : Hand.values()) {
            processHand(hand, hands.get(hand), ctx);
        }
        stealMainInput = wantsInput(Hand.MAIN, hands.get(Hand.MAIN));
        stealOffhandInput = wantsInput(Hand.OFF, hands.get(Hand.OFF));

        if (perfLog) {
            trackPerf(System.nanoTime() - perfStart);
        }
    }

    private void trackPerf(long elapsedNanos) {
        perfTicks++;
        perfNanos += elapsedNanos;
        perfMaxNanos = Math.max(perfMaxNanos, elapsedNanos);
        perfMaxZones = Math.max(perfMaxZones, currentZones.size());
        if (perfTicks >= PERF_LOG_INTERVAL_TICKS) {
            log.info("Core perf: {} µs/tick avg, {} µs max, {} zones peak ({} ticks)",
                    String.format("%.1f", perfNanos / 1000.0 / perfTicks),
                    String.format("%.1f", perfMaxNanos / 1000.0),
                    perfMaxZones, perfTicks);
            clearPerfWindow();
        }
    }

    private void clearPerfWindow() {
        perfTicks = 0;
        perfNanos = 0;
        perfMaxNanos = 0;
        perfMaxZones = 0;
    }

    // Ends every active session (Switched out of VR, left game, etc), features get onInteractEnd with RESET
    public void reset() {
        for (PerHand perHand : hands.values()) {
            if (perHand.state == HandState.GRABBED && perHand.session != null) {
                release(perHand, ReleaseReason.RESET);
            }
            perHand.state = HandState.IDLE;
            perHand.zone = null;
            perHand.session = null;
            perHand.prevInteractDown = false;
            perHand.cooldownOwner = null;
            perHand.cooldownUntilTick = 0;
        }
        stealMainInput = false;
        stealOffhandInput = false;
        currentZones.clear();
        for (AmbientChecker checker : ambientCheckers) {
            try {
                checker.reset();
            } catch (Throwable t) {
                log.error("AmbientChecker threw during reset", t);
            }
        }
        clearPerfWindow();
    }

    private void processHand(Hand hand, PerHand hs, TickContext ctx) {
        boolean down = ctx.interactDown(hand);
        boolean pressed = down && !hs.prevInteractDown;

        if (hs.state == HandState.GRABBED) {
            // A grabbed hand keeps its interaction no matter what other zones appear
            if (!down) {
                release(hs, ReleaseReason.KEY_RELEASED);
            } else {
                refreshZone(hs);
                hs.session.advance(ctx.hand(hand));
                boolean keepGoing;
                try {
                    keepGoing = hs.zone.owner().onInteractTick(ctx, hs.session);
                } catch (Throwable t) {
                    log.error("Interaction {} threw during onInteractTick, releasing", hs.zone.owner().id(), t);
                    keepGoing = false;
                }
                if (!keepGoing) {
                    release(hs, ReleaseReason.FEATURE_DONE);
                }
            }
        } else {
            InteractZone best = findZone(hs, ctx.hand(hand).pos());
            if (best != null) {
                boolean zoneChanged = hs.state == HandState.IDLE || hs.zone == null
                        || !hs.zone.id().equals(best.id()) || hs.zone.owner() != best.owner();
                if (zoneChanged && hoverBuzz && best.owner().inputMode() != Interaction.InputMode.NONE) {
                    haptics.pulse(hand, 0.03f);
                }
                hs.state = HandState.GRABBABLE;
                hs.zone = best;
                if (pressed && best.owner().inputMode() != Interaction.InputMode.NONE) {
                    handlePress(hand, hs, best, ctx);
                }
            } else {
                hs.state = HandState.IDLE;
                hs.zone = null;
            }
        }

        hs.prevInteractDown = down;
    }

    private void handlePress(Hand hand, PerHand hs, InteractZone zone, TickContext ctx) {
        Interaction owner = zone.owner();
        InteractSession session = new InteractSession(hand, zone, ctx.hand(hand));
        boolean failed = false;
        try {
            owner.onInteract(ctx, session);
        } catch (Throwable t) {
            log.error("Interaction {} threw during onInteract", owner.id(), t);
            failed = true;
        }
        if (owner.inputMode() == Interaction.InputMode.HOLD) {
            if (failed) {
                hs.state = HandState.IDLE;
                hs.zone = null;
            } else {
                hs.session = session;
                hs.state = HandState.GRABBED;
            }
        } else if (!failed) {
            // PRESS: single click (cooldown still applies to avoid double fires while hovering)
            hs.cooldownOwner = owner;
            hs.cooldownUntilTick = tick + owner.cooldownTicks();
        }
    }

    // Zones are republished every tick, a held session must follow the fresh instance of its zone
    private void refreshZone(PerHand hs) {
        for (InteractZone zone : currentZones) {
            if (zone.id().equals(hs.zone.id()) && zone.owner() == hs.zone.owner()) {
                hs.zone = zone;
                return;
            }
        }
    }

    // First zone (by priority) that contains the hand, skipping zones still on cooldown
    private InteractZone findZone(PerHand hs, Vector3dc pos) {
        for (InteractZone zone : currentZones) {
            if (hs.cooldownOwner == zone.owner() && tick < hs.cooldownUntilTick) {
                continue;
            }
            if (zone.shape().contains(pos)) {
                return zone;
            }
        }
        return null;
    }

    // Hovering/engaged with a zone that listens for input (NONE zones never steal)
    private boolean wantsInput(Hand hand, PerHand hs) {
        if (hs.state == HandState.IDLE || hs.zone == null) {
            return false;
        }
        Interaction owner = hs.zone.owner();
        if (owner.inputMode() == Interaction.InputMode.NONE) {
            return false;
        }
        return hand == Hand.MAIN || owner.consumesOffhand();
    }

    private void release(PerHand hs, ReleaseReason reason) {
        InteractSession session = hs.session;
        Interaction owner = hs.zone != null ? hs.zone.owner() : null;
        if (session != null && owner != null) {
            try {
                owner.onInteractEnd(lastContext, session, reason);
            } catch (Throwable t) {
                log.error("Interaction {} threw during onInteractEnd", owner.id(), t);
            }
            hs.cooldownOwner = owner;
            hs.cooldownUntilTick = tick + owner.cooldownTicks();
        }
        hs.session = null;
        hs.zone = null;
        hs.state = HandState.IDLE;
    }

    private static final class PerHand {
        HandState state = HandState.IDLE;
        InteractZone zone = null;
        InteractSession session = null;
        boolean prevInteractDown = false;
        Interaction cooldownOwner = null;
        long cooldownUntilTick = 0;
    }
}
