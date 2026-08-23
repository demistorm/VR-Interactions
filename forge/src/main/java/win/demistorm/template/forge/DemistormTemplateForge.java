package win.demistorm.template.forge;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import win.demistorm.template.DemistormTemplate;

@Mod(DemistormTemplate.MOD_ID)
public class DemistormTemplateForge {

    public DemistormTemplateForge() {
        DemistormTemplate.LOGGER.info("Demistorm Template (FORGE) starting!");
        DemistormTemplate.initialize();

        if (FMLEnvironment.dist.isClient()) {
            ForgeClient.initialize();
        }

        DemistormTemplate.LOGGER.info("Demistorm Template (FORGE) initialization complete!");
    }
}
