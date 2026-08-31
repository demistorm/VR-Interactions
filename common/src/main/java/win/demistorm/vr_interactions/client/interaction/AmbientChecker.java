package win.demistorm.vr_interactions.client.interaction;

// Receives every tick of hand data without claiming interaction (non-zone interacts, like horse riding, arm blocking, etc)
public interface AmbientChecker {

    default boolean enabled() {
        return true;
    }

    void tick(TickContext ctx);
}
