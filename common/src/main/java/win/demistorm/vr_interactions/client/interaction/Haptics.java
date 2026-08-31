package win.demistorm.vr_interactions.client.interaction;

// Controller vibration (abstracted to try and make future visor support easier)
public interface Haptics {

    // Amplitude 0 to 1, 160hz as default frequency (same as Vivecraft)
    void pulse(Hand hand, float durationSeconds, float frequency, float amplitude);

    default void pulse(Hand hand, float durationSeconds) {
        pulse(hand, durationSeconds, 160.0f, 1.0f);
    }

    Haptics NOOP = (hand, durationSeconds, frequency, amplitude) -> {};
}
