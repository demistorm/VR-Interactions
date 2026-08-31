package win.demistorm.vr_interactions.client.interaction;

import java.util.List;

// A zone-based interaction feature
// Publishes zones each tick, gets interact events when one of its zones wins a hand
public interface Interaction {

    // NONE: zones are purely spatial, no interact callbacks (presence-only features)
    // PRESS: onInteract fires once per press inside a zone
    // HOLD: full lifecycle (press, hold, release)
    enum InputMode { NONE, PRESS, HOLD }

    String id();

    // Whether this interaction is enabled
    default boolean enabled() {
        return true;
    }

    default InputMode inputMode() {
        return InputMode.HOLD;
    }

    // Whether the offhand bind's key gets stolen from other consumers while the offhand hovers a zone
    default boolean consumesOffhand() {
        return true;
    }

    // Zones this interaction offers this tick, may be empty
    List<InteractZone> zones(TickContext ctx);

    // HOLD: called when the interact key is pressed while a hand is inside one of this interaction's zones
    // PRESS: called once per press, no session lifecycle afterwards
    void onInteract(TickContext ctx, InteractSession session);

    // HOLD only: called every tick while the key stays held
    boolean onInteractTick(TickContext ctx, InteractSession session);

    // HOLD only: always called when a session ends, also on VR shutdown
    void onInteractEnd(TickContext ctx, InteractSession session, ReleaseReason reason);

    // Ticks before the same hand may interact with this interaction again after a release
    default int cooldownTicks() {
        return 0;
    }
}
