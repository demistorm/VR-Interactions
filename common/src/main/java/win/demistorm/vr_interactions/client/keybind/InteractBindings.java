package win.demistorm.vr_interactions.client.keybind;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import win.demistorm.vr_interactions.VRInteractions;
import win.demistorm.vr_interactions.client.input.KeySuppressor;
import win.demistorm.vr_interactions.client.interaction.Hand;

import java.util.function.BooleanSupplier;

// Interact binds per hand
public final class InteractBindings {

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(VRInteractions.MOD_ID, "interactions"));

    public static final KeyMapping INTERACT_MAIN = new KeyMapping(
            "key." + VRInteractions.MOD_ID + ".interact_main",
            -1, // Uses vanilla attack key unless rebound
            CATEGORY);

    public static final KeyMapping INTERACT_OFF = new KeyMapping(
            "key." + VRInteractions.MOD_ID + ".interact_off",
            -1, // Uses Vivecraft's teleport key unless rebound
            CATEGORY);

    private static BooleanSupplier offhandFallback = () -> false;

    private InteractBindings() {}

    // Set from vivecraft (reads the offhand trigger through the teleport key)
    public static void setOffhandFallback(BooleanSupplier fallback) {
        offhandFallback = fallback;
    }

    public static boolean isInteractDown(Hand hand) {
        if (hand == Hand.MAIN) {
            return !INTERACT_MAIN.isDefault() ? INTERACT_MAIN.isDown()
                    : KeySuppressor.unsuppressed(() -> Minecraft.getInstance().options.keyAttack.isDown());
        }
        return !INTERACT_OFF.isDefault() ? INTERACT_OFF.isDown()
                : offhandFallback.getAsBoolean();
    }
}
