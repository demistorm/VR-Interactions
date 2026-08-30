package win.demistorm.vr_interactions.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VRInteractionsClient {

    private static final Logger log = LogManager.getLogger(VRInteractionsClient.class);

    public static void initializeClient() {
        log.info("VR Interactions (CLIENT) starting!");

        if (VRAbstraction.isVRAvailable()) {
            log.info("Vivecraft detected, VR active: {}", VRAbstraction.isVRActive());
        } else {
            log.info("No Vivecraft found, skipping VR features");
        }
    }
}
