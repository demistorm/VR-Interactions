package win.demistorm.vr_interactions.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import win.demistorm.vr_interactions.client.VRInteractionsClient;

public final class FabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        VRInteractionsClient.initializeClient();
    }
}
