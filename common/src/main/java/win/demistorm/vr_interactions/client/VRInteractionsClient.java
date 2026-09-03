package win.demistorm.vr_interactions.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import win.demistorm.vr_interactions.Platform;
import win.demistorm.vr_interactions.VRInteractions;
import win.demistorm.vr_interactions.client.feature.horse.HorseRingsFeature;
import win.demistorm.vr_interactions.client.input.InputStealer;
import win.demistorm.vr_interactions.client.interaction.InteractionManager;
import win.demistorm.vr_interactions.client.keybind.InteractBindings;
import win.demistorm.vr_interactions.client.render.HorseRingsDebugRenderer;
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

        InteractionManager.INSTANCE.registerAmbient(new HorseRingsFeature());
        log.info("Horse rings feature registered");

        if (VRInteractions.debugMode) {
            InteractionManager.INSTANCE.setPerfLogEnabled(true);
            Platform.registerClientTickEvent(HorseRingsDebugRenderer::tick);
            log.debug("Debug log level active");
            log.info("Debug mode on, perf log + horse rings debug renderer registered");
        }
    }
}
