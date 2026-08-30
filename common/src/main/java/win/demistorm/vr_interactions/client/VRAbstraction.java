package win.demistorm.vr_interactions.client;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.vivecraft.api.VRAPI;
import org.vivecraft.api.client.VRClientAPI;
import org.vivecraft.api.data.VRBodyPart;
import org.vivecraft.api.data.VRPose;
import org.vivecraft.api.data.VRPoseHistory;

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
        private static Vec3 handPosFromPose(@Nullable VRPose pose, InteractionHand hand) {
            if (pose == null || pose.getHand(hand) == null) {
                return null;
            }
            return pose.getHand(hand).getPos();
        }
    }
}
