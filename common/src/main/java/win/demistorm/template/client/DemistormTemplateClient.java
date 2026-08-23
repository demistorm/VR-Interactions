package win.demistorm.template.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DemistormTemplateClient {

    private static final Logger log = LogManager.getLogger(DemistormTemplateClient.class);

    public static void initializeClient() {
        log.info("Demistorm Template (CLIENT) starting!");

        if (VivecraftGate.isVivecraftPresent()) {
            log.info("Vivecraft detected, VR active: {}", VivecraftGate.isVRActive());
        } else {
            log.info("No Vivecraft found, skipping VR features");
        }
    }
}
