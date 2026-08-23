package win.demistorm.template;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public final class DemistormTemplate {
    public static final String MOD_ID = "demistorm_template";
    public static final Logger LOGGER = LogManager.getLogger(DemistormTemplate.class);

    public static final boolean debugMode = false;

    static {
        Configurator.setLevel(MOD_ID, debugMode ? Level.DEBUG : Level.INFO);
    }

    public static void initialize() {
        LOGGER.info("Demistorm Template loaded!");
    }
}
