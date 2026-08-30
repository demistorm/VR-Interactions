package win.demistorm.vr_interactions.forge;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import win.demistorm.vr_interactions.VRInteractions;

@Mod(VRInteractions.MOD_ID)
public class VRInteractionsForge {

    public VRInteractionsForge() {
        VRInteractions.LOGGER.info("VR Interactions (FORGE) starting!");
        VRInteractions.initialize();

        if (FMLEnvironment.dist.isClient()) {
            ForgeClient.initialize();
        }

        VRInteractions.LOGGER.info("VR Interactions (FORGE) initialization complete!");
    }
}
