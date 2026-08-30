package win.demistorm.vr_interactions.fabric;

import net.fabricmc.api.ModInitializer;
import win.demistorm.vr_interactions.VRInteractions;

public final class VRInteractionsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        VRInteractions.initialize();
    }
}
