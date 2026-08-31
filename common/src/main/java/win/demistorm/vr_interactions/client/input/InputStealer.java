package win.demistorm.vr_interactions.client.input;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import win.demistorm.vr_interactions.client.interaction.InteractionManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

// Runs after every manager tick while VR is active, cleared whenever the manager resets
public final class InputStealer {

    private static Supplier<List<KeyMapping>> teleportKeys = List::of;

    private InputStealer() {}

    // Set from vivecraft (the teleport keymappings only exist within Vivecraft, will need to look into what visor offers))
    public static void setTeleportKeys(Supplier<List<KeyMapping>> keys) {
        teleportKeys = keys;
    }

    public static void update() {
        Set<KeyMapping> keys = new HashSet<>();
        Options options = Minecraft.getInstance().options;
        if (options != null && InteractionManager.INSTANCE.stealsMainInput()) {
            keys.add(options.keyAttack);
        }
        if (InteractionManager.INSTANCE.stealsOffhandInput()) {
            keys.addAll(teleportKeys.get());
        }
        KeySuppressor.setSuppressed(keys);
    }

    public static void clear() {
        KeySuppressor.setSuppressed(Set.of());
    }
}
