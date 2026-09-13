 package win.demistorm.vr_interactions.client.feature.horse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import win.demistorm.vr_interactions.VRInteractions;
import win.demistorm.vr_interactions.client.VRAbstraction;
import win.demistorm.vr_interactions.client.interaction.AmbientChecker;
import win.demistorm.vr_interactions.client.interaction.Hand;
import win.demistorm.vr_interactions.client.interaction.TickContext;

// Gates horse logic and applies commands to the ridden horse
public final class HorseRingsFeature implements AmbientChecker {

    private static final Logger log = LoggerFactory.getLogger(HorseRingsFeature.class);

    private static volatile RideCommand command = RideCommand.INACTIVE;
    private static volatile AbstractHorse trackedHorse = null;
    private static volatile Vector3dc debugHandAvg = null;
    private static volatile float riddenPitch = Float.NaN;
    private static boolean pitchFollowing = false;

    // Anchor input deltas between ticks (for tracking anchor drift in VR)
    private boolean geoInit = false;
    private double geoX;
    private double geoY;
    private double geoZ;
    private float geoPitch;
    private float geoBodyYaw;
    private float geoHeadYaw;
    private double geoMouthX;
    private double geoMouthY;
    private double geoMouthZ;

    private final HorseRingsLogic logic = new HorseRingsLogic();
    private final HorseRingsVisuals visuals = new HorseRingsVisuals();
    private int secondStopPulse = 0;
    private boolean wasLogging = false;

    // Null unless the entity is the horse this currently commands
    public static RideCommand commandFor(AbstractHorse horse) {
        if (trackedHorse != horse) {
            return null;
        }
        RideCommand cmd = command;
        return cmd.active() ? cmd : null;
    }

    public static AbstractHorse debugHorse() {
        return trackedHorse;
    }

    public static Vector3dc debugHandAvg() {
        return debugHandAvg;
    }

    public static Float riddenHeadPitch() {
        return Float.isNaN(riddenPitch) ? null : riddenPitch;
    }

    @Override
    public void tick(TickContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !(player.getVehicle() instanceof AbstractHorse horse)
                || !horse.isSaddled() || horse.getControllingPassenger() != player) {
            deactivate();
            return;
        }

        boolean mainFree = player.getMainHandItem().isEmpty();
        boolean offFree = player.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty();
        int activeHands = (mainFree ? 1 : 0) + (offFree ? 1 : 0);

        Float headYaw = VRAbstraction.getHeadYaw(player);
        float lookYaw = headYaw != null ? headYaw : player.getYRot();
        Float headPitch = VRAbstraction.getHeadPitch(player);
        updateRiddenPitch(headPitch != null ? headPitch : player.getXRot());

        if (activeHands == 0) {
            debugHandAvg = null;
            logic.tickIdle(horse.getYRot(), lookYaw);
        } else {
            Vector3dc handAvg = handAverage(ctx, mainFree, offFree);
            debugHandAvg = handAvg;
            logic.tick(new RideSample(handAvg, activeHands, toVector(player.position()),
                    horse.getYRot(), lookYaw, toVector(horse.getDeltaMovement())));
        }

        RideCommand cmd = logic.command();
        if (cmd.jumpFired()) {
            fireJump(mc, player, horse);
        }
        pulseEvents(ctx, cmd, mainFree, offFree);
        visuals.tick(horse, mainFree, offFree,
                mainFree ? ctx.hand(Hand.MAIN).pos() : null,
                offFree ? ctx.hand(Hand.OFF).pos() : null);
        double roomOffset = VRAbstraction.applyRidingRoomLock(HorseRingsTuning.ROOM_LOCK_WINDOW);

        trackedHorse = horse;
        command = cmd;

        if (!wasLogging) {
            log.debug("HR ACTIVATE");
            wasLogging = true;
        }
        if (VRInteractions.debugMode) {
            log.debug("HRDBG t={} {} room={}", ctx.tick(), logic.debugState(),
                    String.format("%.2f", roomOffset));
            logGeoDeltas(horse);
            log.debug("HRROPE {}", visuals.debugRopeStats());
        }
    }

    // DEBUG deltas (remove at publish)
    private void logGeoDeltas(AbstractHorse horse) {
        double[] mouth = visuals.debugMouthL();
        if (!geoInit) {
            seedGeo(horse, mouth);
            return;
        }
        log.debug("HRGEO dPos={},{},{} dPitch={} dBodyYaw={} dHeadYaw={} dMouth={},{},{}",
                String.format("%.3f", horse.getX() - geoX),
                String.format("%.3f", horse.getY() - geoY),
                String.format("%.3f", horse.getZ() - geoZ),
                String.format("%.2f", horse.getXRot() - geoPitch),
                String.format("%.2f", Mth.wrapDegrees(horse.yBodyRot - geoBodyYaw)),
                String.format("%.2f", Mth.wrapDegrees(horse.yHeadRot - geoHeadYaw)),
                String.format("%.3f", mouth[0] - geoMouthX),
                String.format("%.3f", mouth[1] - geoMouthY),
                String.format("%.3f", mouth[2] - geoMouthZ));
        seedGeo(horse, mouth);
    }

    private void seedGeo(AbstractHorse horse, double[] mouth) {
        geoX = horse.getX();
        geoY = horse.getY();
        geoZ = horse.getZ();
        geoPitch = horse.getXRot();
        geoBodyYaw = horse.yBodyRot;
        geoHeadYaw = horse.yHeadRot;
        geoMouthX = mouth[0];
        geoMouthY = mouth[1];
        geoMouthZ = mouth[2];
        geoInit = true;
    }

    @Override
    public void reset() {
        deactivate();
    }

    // Filters headset movement noise for horse head pitch
    private static void updateRiddenPitch(float raw) {
        if (Float.isNaN(riddenPitch)) {
            riddenPitch = raw;
            return;
        }
        float delta = raw - riddenPitch;
        if (!pitchFollowing && Math.abs(delta) >= 2.5F) {
            pitchFollowing = true;
        }
        if (pitchFollowing) {
            riddenPitch = Mth.lerp(0.35F, riddenPitch, raw);
            if (Math.abs(raw - riddenPitch) <= 0.75F) {
                riddenPitch = raw;
                pitchFollowing = false;
            }
        }
    }

    private void deactivate() {
        logic.deactivate();
        visuals.deactivate();
        trackedHorse = null;
        command = RideCommand.INACTIVE;
        debugHandAvg = null;
        secondStopPulse = 0;
        geoInit = false;
        if (wasLogging) {
            log.debug("HR DEACTIVATE");
            wasLogging = false;
        }
    }

    // Charges the horse jump and sends jump to server
    private void fireJump(Minecraft mc, LocalPlayer player, AbstractHorse horse) {
        int charge = (int) (HorseRingsTuning.JUMP_CHARGE * 100.0);
        horse.onPlayerJump(charge);
        if (mc.getConnection() != null) {
            mc.getConnection().send(new ServerboundPlayerCommandPacket(
                    player, ServerboundPlayerCommandPacket.Action.START_RIDING_JUMP, charge));
        }
        if (VRInteractions.debugMode) {
            log.debug("Horse rings: jump fired at charge {}", charge);
        }
    }

    private void pulseEvents(TickContext ctx, RideCommand cmd, boolean mainFree, boolean offFree) {
        if (secondStopPulse > 0) {
            secondStopPulse--;
            if (secondStopPulse == 0) {
                pulse(ctx, mainFree, offFree, 0.06f);
            }
        }
        int events = cmd.events();
        if ((events & RideCommand.EVENT_TURN_ENGAGED) != 0 && VRInteractions.debugMode) {
            log.debug("Horse rings: turn engaged, lateral {}", String.format("%.2f", cmd.lateralOffset()));
        }
        if ((events & RideCommand.EVENT_TIER_UP) != 0) {
            pulse(ctx, mainFree, offFree, 0.06f + cmd.tier() * 0.015f);
            if (VRInteractions.debugMode) {
                log.debug("Horse rings: tier up to {}", cmd.tier());
            }
        }
        if ((events & RideCommand.EVENT_TIER_SUSTAIN) != 0) {
            pulse(ctx, mainFree, offFree, 0.04f);
            if (VRInteractions.debugMode) {
                log.debug("Horse rings: tier extend at {}", cmd.tier());
            }
        }
        if ((events & RideCommand.EVENT_STOP) != 0) {
            pulse(ctx, mainFree, offFree, 0.06f);
            secondStopPulse = 3;
            if (VRInteractions.debugMode) {
                log.debug("Horse rings: stop engaged");
            }
        }
        if ((events & RideCommand.EVENT_JUMP) != 0) {
            pulse(ctx, mainFree, offFree, 0.08f);
        }
    }

    private void pulse(TickContext ctx, boolean mainFree, boolean offFree, float duration) {
        if (mainFree) {
            ctx.haptics().pulse(Hand.MAIN, duration);
        }
        if (offFree) {
            ctx.haptics().pulse(Hand.OFF, duration);
        }
    }

    private static Vector3dc handAverage(TickContext ctx, boolean mainFree, boolean offFree) {
        Vector3dc main = ctx.hand(Hand.MAIN).pos();
        Vector3dc off = ctx.hand(Hand.OFF).pos();
        if (mainFree && offFree) {
            return main.add(off, new Vector3d()).mul(0.5);
        }
        return mainFree ? main : off;
    }

    private static Vector3dc toVector(Vec3 vec) {
        return new Vector3d(vec.x, vec.y, vec.z);
    }
}
