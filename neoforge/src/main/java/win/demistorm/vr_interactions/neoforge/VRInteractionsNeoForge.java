package win.demistorm.vr_interactions.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import win.demistorm.vr_interactions.VRInteractions;

@Mod(VRInteractions.MOD_ID)
public final class VRInteractionsNeoForge {

    public VRInteractionsNeoForge(IEventBus modEventBus) {
        VRInteractions.LOGGER.info("VR Interactions (NEOFORGE) starting!");
        VRInteractions.initialize();

        if (FMLEnvironment.getDist().isClient()) {
            modEventBus.addListener(NeoClient::registerKeyMappings);
            NeoClient.initialize();
        }

        VRInteractions.LOGGER.info("VR Interactions (NEOFORGE) initialization complete!");
    }
}
