package win.demistorm.vr_interactions.client.interaction;

// Why an InteractSession ended
public enum ReleaseReason {
    // Interact key was let go
    KEY_RELEASED,
    // The feature completed on its own (returned false from onInteractTick)
    FEATURE_DONE,
    // Switched out of VR, left the world, so on
    RESET
}
