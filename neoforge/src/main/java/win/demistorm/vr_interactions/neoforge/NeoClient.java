package win.demistorm.vr_interactions.neoforge;

import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import win.demistorm.vr_interactions.client.keybind.InteractBindings;

public class NeoClient {

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(InteractBindings.INTERACT_MAIN);
        event.register(InteractBindings.INTERACT_OFF);
    }

    public static void initialize() {
        win.demistorm.vr_interactions.client.VRInteractionsClient.initializeClient();
    }
}
