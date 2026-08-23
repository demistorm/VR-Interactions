package win.demistorm.template.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import win.demistorm.template.DemistormTemplate;

@Mod(DemistormTemplate.MOD_ID)
public final class DemistormTemplateNeoForge {

    public DemistormTemplateNeoForge(IEventBus modEventBus) {
        DemistormTemplate.LOGGER.info("Demistorm Template (NEOFORGE) starting!");
        DemistormTemplate.initialize();

        if (FMLEnvironment.getDist().isClient()) {
            NeoClient.initialize();
        }

        DemistormTemplate.LOGGER.info("Demistorm Template (NEOFORGE) initialization complete!");
    }
}
