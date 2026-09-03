package win.demistorm.vr_interactions;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public final class VRInteractions {
    public static final String MOD_ID = "vr_interactions";
    public static final Logger LOGGER = LogManager.getLogger(VRInteractions.class);

    public static final boolean debugMode = true;

    static {
        Configurator.setLevel("win.demistorm.vr_interactions", debugMode ? Level.DEBUG : Level.INFO);
    }

    public static void initialize() {
        LOGGER.info("VR Interactions loaded!");
    }
}
