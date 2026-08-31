package win.demistorm.vr_interactions.client.vivecraft;

import net.minecraft.client.KeyMapping;
import org.vivecraft.client.VivecraftVRMod;
import org.vivecraft.client_vr.provider.MCVR;
import org.vivecraft.client_vr.provider.openvr_lwjgl.VRInputAction;

import java.util.List;

// Vivecraft-side input sources (only classloaded when Vivecraft is present)
public final class VivecraftInput {

    private VivecraftInput() {}

    public static boolean offhandTriggerDown() {
        try {
            VRInputAction action = teleportAction();
            return action != null && action.isButtonPressed();
        } catch (Throwable t) {
            return false;
        }
    }

    public static List<KeyMapping> teleportKeys() {
        try {
            VivecraftVRMod mod = VivecraftVRMod.INSTANCE;
            return List.of(mod.keyTeleport, mod.keyTeleportFallback);
        } catch (Throwable t) {
            return List.of();
        }
    }

    @SuppressWarnings("unused")
    private static VRInputAction teleportAction() {
        MCVR vr = MCVR.get();
        VRInputAction action = vr.getInputActionByName("ingame/in/vivecraft.key.teleport");
        if (action == null) {
            action = vr.getInputActionByName("/actions/ingame/in/vivecraft.key.teleport");
        }
        return action;
    }
}
