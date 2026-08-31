package win.demistorm.vr_interactions.neoforge;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;

import java.util.function.BooleanSupplier;

// NeoForge implementations of the Platform hooks
@SuppressWarnings("unused")
public final class PlatformImpl {

    private PlatformImpl() {}

    public static void registerClientTickEvent(Runnable runnable) {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> runnable.run());
    }

    public static void registerClientInputCancellation(BooleanSupplier cancelAttack, BooleanSupplier cancelUse) {
        // Cancel at the input level so no interaction packets reach the server
        NeoForge.EVENT_BUS.addListener((InputEvent.InteractionKeyMappingTriggered event) -> {
            Minecraft mc = Minecraft.getInstance();
            if (event.getKeyMapping() == mc.options.keyAttack && cancelAttack.getAsBoolean()) {
                event.setCanceled(true);
            }
            if (event.getKeyMapping() == mc.options.keyUse && cancelUse.getAsBoolean()) {
                event.setCanceled(true);
            }
        });

        // Suppress the arm swing that already started before the input event fired
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && cancelUse.getAsBoolean() && mc.options.keyUse.isDown()) {
                mc.player.swingingArm = null;
            }
        });
    }
}
