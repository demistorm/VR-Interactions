package win.demistorm.vr_interactions.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import win.demistorm.vr_interactions.Platform;
import win.demistorm.vr_interactions.VRInteractions;
import win.demistorm.vr_interactions.client.debug.TestZoneInteraction;
import win.demistorm.vr_interactions.client.input.InputStealer;
import win.demistorm.vr_interactions.client.interaction.InteractionManager;
import win.demistorm.vr_interactions.client.keybind.InteractBindings;
import win.demistorm.vr_interactions.client.render.ZoneDebugRenderer;
import win.demistorm.vr_interactions.client.vivecraft.ViveTracker;
import win.demistorm.vr_interactions.client.vivecraft.VivecraftInput;

public class VRInteractionsClient {

    private static final Logger log = LogManager.getLogger(VRInteractionsClient.class);

    public static void initializeClient() {
        log.info("VR Interactions (CLIENT) starting!");

        if (VRAbstraction.isVRAvailable()) {
            log.info("Vivecraft detected, VR active: {}", VRAbstraction.isVRActive());
            ViveTracker.register();
            InteractBindings.setOffhandFallback(VivecraftInput::offhandTriggerDown);
            InputStealer.setTeleportKeys(VivecraftInput::teleportKeys);
        } else {
            log.info("No Vivecraft found, skipping VR features");
        }

        Platform.registerClientInputCancellation(
                InteractionManager.INSTANCE::stealsMainInput,
                InteractionManager.INSTANCE::stealsMainInput);
        Platform.registerClientTickEvent(ZoneDebugRenderer::tick);

        if (VRInteractions.debugMode) {
            InteractionManager.INSTANCE.register(new TestZoneInteraction());
            InteractionManager.INSTANCE.setPerfLogEnabled(true);
            log.info("Debug mode on, test zone interaction registered");
        }
    }
}
