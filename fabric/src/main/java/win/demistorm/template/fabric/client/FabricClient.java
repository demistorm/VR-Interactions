package win.demistorm.template.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import win.demistorm.template.client.DemistormTemplateClient;

public final class FabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        DemistormTemplateClient.initializeClient();
    }
}
