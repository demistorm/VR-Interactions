package win.demistorm.vr_interactions.client.input;

import net.minecraft.client.KeyMapping;

import java.util.Set;
import java.util.function.BooleanSupplier;

public final class KeySuppressor {

    private static Set<KeyMapping> suppressed = Set.of();
    private static boolean bypass;

    private KeySuppressor() {}

    public static void setSuppressed(Set<KeyMapping> keys) {
        suppressed = Set.copyOf(keys);
    }

    public static boolean isSuppressed(KeyMapping key) {
        return !bypass && suppressed.contains(key);
    }

    // Reads a key state as if nothing was suppressed (used by the interaction input sampling)
    public static boolean unsuppressed(BooleanSupplier read) {
        boolean prev = bypass;
        bypass = true;
        try {
            return read.getAsBoolean();
        } finally {
            bypass = prev;
        }
    }
}
