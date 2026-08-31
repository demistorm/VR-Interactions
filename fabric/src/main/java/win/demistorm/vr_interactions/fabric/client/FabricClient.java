package win.demistorm.vr_interactions.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import win.demistorm.vr_interactions.client.VRInteractionsClient;
import win.demistorm.vr_interactions.client.keybind.InteractBindings;

public final class FabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(InteractBindings.INTERACT_MAIN);
        KeyBindingHelper.registerKeyBinding(InteractBindings.INTERACT_OFF);

        VRInteractionsClient.initializeClient();
    }
}
