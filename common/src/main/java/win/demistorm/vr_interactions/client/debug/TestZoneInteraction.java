package win.demistorm.vr_interactions.client.debug;

import org.joml.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import win.demistorm.vr_interactions.client.interaction.HeadInfo;
import win.demistorm.vr_interactions.client.interaction.InteractSession;
import win.demistorm.vr_interactions.client.interaction.InteractZone;
import win.demistorm.vr_interactions.client.interaction.Interaction;
import win.demistorm.vr_interactions.client.interaction.ReleaseReason;
import win.demistorm.vr_interactions.client.interaction.TickContext;
import win.demistorm.vr_interactions.client.interaction.ZoneShape;

import java.util.List;

// Test Interaction Zone (2d interactable circle)
// Registered only while VRInteractions.debugMode is on (delete when features are added)
public final class TestZoneInteraction implements Interaction {

    private static final Logger log = LoggerFactory.getLogger(TestZoneInteraction.class);

    @Override
    public String id() {
        return "test_zone";
    }

    @Override
    public List<InteractZone> zones(TickContext ctx) {
        HeadInfo head = ctx.head();
        Vector3d center = new Vector3d(head.pos())
                .add(new Vector3d(head.dir()).mul(0.5))
                .sub(0.0, 0.2, 0.0);
        return List.of(new InteractZone("face_sphere", this, 1000, new ZoneShape.Sphere(center, 0.2)));
    }

    @Override
    public void onInteract(TickContext ctx, InteractSession session) {
        log.info("Interact with {} at {}", session.hand(), session.currentInfo().pos());
        ctx.haptics().pulse(session.hand(), 0.05f);
    }

    @Override
    public boolean onInteractTick(TickContext ctx, InteractSession session) {
        return true;
    }

    @Override
    public void onInteractEnd(TickContext ctx, InteractSession session, ReleaseReason reason) {
        log.info("Released ({}) after {} ticks, displacement {}", reason, session.ticks(), session.displacement());
    }
}
