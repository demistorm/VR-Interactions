package win.demistorm.vr_interactions.client.render;

import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3dc;
import win.demistorm.vr_interactions.VRInteractions;
import win.demistorm.vr_interactions.client.VRAbstraction;
import win.demistorm.vr_interactions.client.interaction.Hand;
import win.demistorm.vr_interactions.client.interaction.InteractZone;
import win.demistorm.vr_interactions.client.interaction.InteractionManager;
import win.demistorm.vr_interactions.client.interaction.ZoneShape;

import java.util.List;

// Draws manager's current zones through the vanilla gizmo system
// Colors: idle=grey, interactable=green, grabbed=orange
public final class ZoneDebugRenderer {

    private static final int COLOR_IDLE = 0xFF9E9E9E;
    private static final int COLOR_GRABBABLE = 0xFF33FF33;
    private static final int COLOR_GRABBED = 0xFFFF9E00;

    private ZoneDebugRenderer() {}

    // Registered on the client tick event (gizmos submitted during the tick render that frame)
    public static void tick() {
        if (!VRInteractions.debugMode || !VRAbstraction.isVRActive()) {
            return;
        }
        InteractionManager manager = InteractionManager.INSTANCE;
        List<InteractZone> zones = manager.currentZones();
        if (zones.isEmpty()) {
            return;
        }
        for (InteractZone zone : zones) {
            if (!(zone.shape() instanceof ZoneShape.Sphere sphere)) {
                continue;
            }
            int color = colorFor(manager, zone);
            Vec3 center = toVec3(sphere.center());
            Gizmos.circle(center, (float) sphere.radius(), GizmoStyle.stroke(color));
            Gizmos.billboardText(zone.id(), center, TextGizmo.Style.forColor(color));
            for (Hand hand : Hand.values()) {
                if (manager.zoneOf(hand) == zone && manager.stateOf(hand) != InteractionManager.HandState.IDLE) {
                    Vec3 handPos = VRAbstraction.getPreTickHandPos(toInteractionHand(hand));
                    if (handPos != null) {
                        Gizmos.line(handPos, center, color);
                    }
                }
            }
        }
    }

    private static int colorFor(InteractionManager manager, InteractZone zone) {
        for (Hand hand : Hand.values()) {
            if (manager.zoneOf(hand) == zone) {
                if (manager.stateOf(hand) == InteractionManager.HandState.GRABBED) {
                    return COLOR_GRABBED;
                }
                if (manager.stateOf(hand) == InteractionManager.HandState.GRABBABLE) {
                    return COLOR_GRABBABLE;
                }
            }
        }
        return COLOR_IDLE;
    }

    private static Vec3 toVec3(Vector3dc vec) {
        return new Vec3(vec.x(), vec.y(), vec.z());
    }

    private static InteractionHand toInteractionHand(Hand hand) {
        return hand == Hand.MAIN ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }
}
