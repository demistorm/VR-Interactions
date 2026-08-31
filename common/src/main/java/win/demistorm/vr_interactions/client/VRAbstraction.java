package win.demistorm.vr_interactions.client;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;
import org.vivecraft.api.VRAPI;
import org.vivecraft.api.client.VRClientAPI;
import org.vivecraft.api.data.VRBodyPart;
import org.vivecraft.api.data.VRPose;
import org.vivecraft.api.data.VRPoseHistory;
import org.vivecraft.api.data.VRBodyPartData;

// Abstraction class so that adding Visor support will (hopefully) be trivial later
public final class VRAbstraction {

    private VRAbstraction() {}

    public static boolean isVRAvailable() {
        return VivecraftGate.isVivecraftPresent();
    }

    public static boolean isVRActive() {
        return VivecraftGate.isVivecraftPresent() && Impl.isVRActive();
    }

    public static boolean isVRPlayer(Player player) {
        return VivecraftGate.isVivecraftPresent() && Impl.isVRPlayer(player);
    }

    @Nullable
    public static Vec3 getHandPos(Player player, InteractionHand hand) {
        if (!VivecraftGate.isVivecraftPresent()) {
            return null;
        }
        return Impl.getHandPos(player, hand);
    }

    @Nullable
    public static Vec3 getHandPosTicksAgo(Player player, InteractionHand hand, int ticksBack) {
        if (!VivecraftGate.isVivecraftPresent()) {
            return null;
        }
        return Impl.getHandPosTicksAgo(player, hand, ticksBack);
    }

    public static double getHandAverageSpeed(Player player, InteractionHand hand, int ticksBack) {
        if (!VivecraftGate.isVivecraftPresent()) {
            return 0.0;
        }
        return Impl.getHandAverageSpeed(player, hand, ticksBack);
    }

    @Nullable
    public static Vec3 getHeadPos(Player player) {
        if (!VivecraftGate.isVivecraftPresent()) {
            return null;
        }
        return Impl.getHeadPos(player);
    }

    @Nullable
    public static Vec3 getPreTickHandPos(InteractionHand hand) {
        if (!VivecraftGate.isVivecraftPresent()) {
            return null;
        }
        return Impl.getPreTickHandPos(hand);
    }

    @Nullable
    public static Vec3 getPreTickHandDir(InteractionHand hand) {
        if (!VivecraftGate.isVivecraftPresent()) {
            return null;
        }
        return Impl.getPreTickHandDir(hand);
    }

    @Nullable
    public static Quaternionfc getPreTickHandRotation(InteractionHand hand) {
        if (!VivecraftGate.isVivecraftPresent()) {
            return null;
        }
        return Impl.getPreTickHandRotation(hand);
    }

    @Nullable
    public static Vec3 getPreTickHeadPos() {
        if (!VivecraftGate.isVivecraftPresent()) {
            return null;
        }
        return Impl.getPreTickHeadPos();
    }

    @Nullable
    public static Vec3 getPreTickHeadDir() {
        if (!VivecraftGate.isVivecraftPresent()) {
            return null;
        }
        return Impl.getPreTickHeadDir();
    }

    @Nullable
    public static Quaternionfc getPreTickHeadRotation() {
        if (!VivecraftGate.isVivecraftPresent()) {
            return null;
        }
        return Impl.getPreTickHeadRotation();
    }

    // Average hand velocity in blocks per tick for the local player over a given window
    @Nullable
    public static Vec3 getLocalHandVelocity(InteractionHand hand, int ticksBack) {
        if (!VivecraftGate.isVivecraftPresent()) {
            return null;
        }
        return Impl.getLocalHandVelocity(hand, ticksBack);
    }

    public static void pulse(InteractionHand hand, float duration, float frequency, float amplitude) {
        if (VivecraftGate.isVivecraftPresent()) {
            Impl.pulse(hand, duration, frequency, amplitude);
        }
    }

    // Only loaded when Vivecraft is around
    private static final class Impl {

        static boolean isVRActive() {
            return VRClientAPI.instance().isVRActive();
        }

        static boolean isVRPlayer(Player player) {
            return VRAPI.instance().isVRPlayer(player);
        }

        @Nullable
        static Vec3 getHandPos(Player player, InteractionHand hand) {
            return handPosFromPose(VRAPI.instance().getVRPose(player), hand);
        }

        @Nullable
        static Vec3 getHandPosTicksAgo(Player player, InteractionHand hand, int ticksBack) {
            VRPoseHistory history = VRAPI.instance().getHistoricalVRPoses(player);
            if (history == null) {
                return null;
            }
            try {
                return handPosFromPose(history.getHistoricalData(ticksBack, false), hand);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        static double getHandAverageSpeed(Player player, InteractionHand hand, int ticksBack) {
            VRPoseHistory history = VRAPI.instance().getHistoricalVRPoses(player);
            if (history == null) {
                return 0.0;
            }
            try {
                return history.averageSpeed(VRBodyPart.fromInteractionHand(hand), ticksBack);
            } catch (IllegalArgumentException e) {
                return 0.0;
            }
        }

        @Nullable
        static Vec3 getHeadPos(Player player) {
            VRPose pose = VRAPI.instance().getVRPose(player);
            if (pose == null || pose.getHead() == null) {
                return null;
            }
            return pose.getHead().getPos();
        }

        @Nullable
        static Vec3 getPreTickHandPos(InteractionHand hand) {
            VRBodyPartData part = preTickHand(hand);
            return part != null ? part.getPos() : null;
        }

        @Nullable
        static Vec3 getPreTickHandDir(InteractionHand hand) {
            VRBodyPartData part = preTickHand(hand);
            return part != null ? part.getDir() : null;
        }

        @Nullable
        static Quaternionfc getPreTickHandRotation(InteractionHand hand) {
            VRBodyPartData part = preTickHand(hand);
            return part != null ? part.getRotation() : null;
        }

        @Nullable
        static Vec3 getPreTickHeadPos() {
            VRBodyPartData head = preTickHead();
            return head != null ? head.getPos() : null;
        }

        @Nullable
        static Vec3 getPreTickHeadDir() {
            VRBodyPartData head = preTickHead();
            return head != null ? head.getDir() : null;
        }

        @Nullable
        static Quaternionfc getPreTickHeadRotation() {
            VRBodyPartData head = preTickHead();
            return head != null ? head.getRotation() : null;
        }

        @Nullable
        static Vec3 getLocalHandVelocity(InteractionHand hand, int ticksBack) {
            VRPoseHistory history = VRClientAPI.instance().getHistoricalVRPoses();
            if (history == null) {
                return null;
            }
            try {
                return history.averageVelocity(VRBodyPart.fromInteractionHand(hand), ticksBack);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        static void pulse(InteractionHand hand, float duration, float frequency, float amplitude) {
            VRClientAPI.instance().triggerHapticPulse(
                    VRBodyPart.fromInteractionHand(hand), duration, frequency, amplitude, 0.0f);
        }

        @Nullable
        private static VRBodyPartData preTickHand(InteractionHand hand) {
            VRPose pose = VRClientAPI.instance().getPreTickWorldPose();
            if (pose == null) {
                return null;
            }
            return pose.getHand(hand);
        }

        @Nullable
        private static VRBodyPartData preTickHead() {
            VRPose pose = VRClientAPI.instance().getPreTickWorldPose();
            if (pose == null) {
                return null;
            }
            return pose.getHead();
        }

        @Nullable
        private static Vec3 handPosFromPose(@Nullable VRPose pose, InteractionHand hand) {
            if (pose == null || pose.getHand(hand) == null) {
                return null;
            }
            return pose.getHand(hand).getPos();
        }
    }
}
