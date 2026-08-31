package win.demistorm.vr_interactions.client.interaction;

import org.joml.Vector3d;

// One interaction from press to release, controlled by InteractionManager and read by the feature
public final class InteractSession {

    private final Hand hand;
    private final InteractZone zone;
    private final HandInfo startInfo;
    private HandInfo currentInfo;
    private long ticks;

    InteractSession(Hand hand, InteractZone zone, HandInfo info) {
        this.hand = hand;
        this.zone = zone;
        this.startInfo = info;
        this.currentInfo = info;
    }

    // Called by manager before each onHoldTick
    void advance(HandInfo info) {
        this.currentInfo = info;
        this.ticks++;
    }

    public Hand hand() {
        return hand;
    }

    public InteractZone zone() {
        return zone;
    }

    // Hand pose on the tick the grab started
    public HandInfo startInfo() {
        return startInfo;
    }

    public HandInfo currentInfo() {
        return currentInfo;
    }

    // Ticks since the grab started, 0 on the grab tick itself
    public long ticks() {
        return ticks;
    }

    // Hand movement since the grab started, in world space
    public Vector3d displacement() {
        return new Vector3d(currentInfo.pos()).sub(startInfo.pos());
    }
}
