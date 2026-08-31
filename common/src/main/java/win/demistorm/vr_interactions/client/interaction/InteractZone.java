package win.demistorm.vr_interactions.client.interaction;

// An interactable region published each tick (lower priority wins, ties keep registration order)
public record InteractZone(String id, Interaction owner, int priority, ZoneShape shape) {
}
