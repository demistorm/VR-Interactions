package win.demistorm.template.fabric;

import net.fabricmc.api.ModInitializer;
import win.demistorm.template.DemistormTemplate;

public final class DemistormTemplateFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        DemistormTemplate.initialize();
    }
}
