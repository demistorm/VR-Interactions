package win.demistorm.vr_interactions.client.vivecraft;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.Nullable;
import org.vivecraft.api.client.Tracker;
import org.vivecraft.api.client.VRClientAPI;
import win.demistorm.vr_interactions.client.VRAbstraction;
import win.demistorm.vr_interactions.client.input.InputStealer;
import win.demistorm.vr_interactions.client.interaction.Hand;
import win.demistorm.vr_interactions.client.interaction.HandInfo;
import win.demistorm.vr_interactions.client.interaction.HeadInfo;
import win.demistorm.vr_interactions.client.interaction.Haptics;
import win.demistorm.vr_interactions.client.interaction.InteractionManager;
import win.demistorm.vr_interactions.client.interaction.TickContext;
import win.demistorm.vr_interactions.client.keybind.InteractBindings;

// Only classloaded when Vivecraft is present (registered behind VRAbstraction.isVRAvailable())
public final class ViveTracker implements Tracker {

    // Window for average hand velocity in HandInfo
    private static final int VELOCITY_WINDOW_TICKS = 2;

    private static final Haptics HAPTICS = (hand, duration, frequency, amplitude) ->
            VRAbstraction.pulse(toInteractionHand(hand), duration, frequency, amplitude);

    private long tickCounter = 0;

    // Call once from client init before game loop starts
    public static void register() {
        InteractionManager.INSTANCE.setHaptics(HAPTICS);
        VRClientAPI.instance().addClientRegistrationHandler(event ->
                event.registerTrackers(new ViveTracker()));
    }

    @Override
    public ProcessType processType() {
        return ProcessType.PER_TICK;
    }

    @Override
    public boolean isActive(@Nullable LocalPlayer player) {
        return player != null && VRAbstraction.isVRPlayer(player);
    }

    @Override
    public void inactiveProcess(@Nullable LocalPlayer player) {
        InteractionManager.INSTANCE.reset();
        InputStealer.clear();
    }

    @Override
    public void activeProcess(@Nullable LocalPlayer player) {
        TickContext ctx = buildContext();
        if (ctx != null) {
            InteractionManager.INSTANCE.tick(ctx);
            InputStealer.update();
        }
    }

    @Nullable
    private TickContext buildContext() {
        HandInfo main = handInfo(InteractionHand.MAIN_HAND);
        HandInfo off = handInfo(InteractionHand.OFF_HAND);
        HeadInfo head = headInfo();
        if (main == null || off == null || head == null) {
            return null;
        }
        return new TickContext(tickCounter++, main, off, head,
                InteractBindings.isInteractDown(Hand.MAIN), InteractBindings.isInteractDown(Hand.OFF), HAPTICS);
    }

    @Nullable
    private static HandInfo handInfo(InteractionHand hand) {
        Vec3 pos = VRAbstraction.getPreTickHandPos(hand);
        Vec3 dir = VRAbstraction.getPreTickHandDir(hand);
        Quaternionfc rotation = VRAbstraction.getPreTickHandRotation(hand);
        if (pos == null || dir == null || rotation == null) {
            return null;
        }
        Vector3d velocity = new Vector3d();
        Vec3 vel = VRAbstraction.getLocalHandVelocity(hand, VELOCITY_WINDOW_TICKS);
        if (vel != null) {
            velocity.set(vel.x, vel.y, vel.z);
        }
        return new HandInfo(toVector(pos), toVector(dir), rotation, velocity);
    }

    @Nullable
    private static HeadInfo headInfo() {
        Vec3 pos = VRAbstraction.getPreTickHeadPos();
        Vec3 dir = VRAbstraction.getPreTickHeadDir();
        Quaternionfc rotation = VRAbstraction.getPreTickHeadRotation();
        if (pos == null || dir == null || rotation == null) {
            return null;
        }
        return new HeadInfo(toVector(pos), toVector(dir), rotation);
    }

    private static Vector3dc toVector(Vec3 vec) {
        return new Vector3d(vec.x, vec.y, vec.z);
    }

    private static InteractionHand toInteractionHand(Hand hand) {
        return hand == Hand.MAIN ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }
}
