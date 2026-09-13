package win.demistorm.vr_interactions.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import win.demistorm.vr_interactions.client.VRAbstraction;
import win.demistorm.vr_interactions.client.feature.horse.HorseRingsFeature;
import win.demistorm.vr_interactions.client.feature.horse.RideCommand;

// Horse rings: Adds gesture commands to control horse speed/turning/jumps
@Mixin(AbstractHorse.class)
public abstract class AbstractHorseMixin {

    @Inject(method = "getRiddenRotation", at = @At("HEAD"), cancellable = true)
    private void vr_interactions$gestureRotation(LivingEntity rider, CallbackInfoReturnable<Vec2> cir) {
        RideCommand cmd = HorseRingsFeature.commandFor((AbstractHorse) (Object) this);
        if (cmd != null) {
            // Head pitch from the pose
            float pitch = rider.getXRot();
            if (rider instanceof Player player) {
                Float headPitch = HorseRingsFeature.riddenHeadPitch();
                if (headPitch == null) {
                    headPitch = VRAbstraction.getHeadPitch(player);
                }
                if (headPitch != null) {
                    pitch = headPitch;
                }
            }
            cir.setReturnValue(new Vec2(pitch * 0.5F, cmd.yawDeg()));
        }
    }

    @Inject(method = "getRiddenInput", at = @At("HEAD"), cancellable = true)
    private void vr_interactions$gestureInput(Player rider, Vec3 vanillaInput, CallbackInfoReturnable<Vec3> cir) {
        RideCommand cmd = HorseRingsFeature.commandFor((AbstractHorse) (Object) this);
        if (cmd != null) {
            cir.setReturnValue(new Vec3(0.0, 0.0, cmd.forwardImpulse()));
        }
    }

    @Inject(method = "getRiddenSpeed", at = @At("HEAD"), cancellable = true)
    private void vr_interactions$gestureSpeed(Player rider, CallbackInfoReturnable<Float> cir) {
        AbstractHorse horse = (AbstractHorse) (Object) this;
        RideCommand cmd = HorseRingsFeature.commandFor(horse);
        if (cmd != null) {
            float base = (float) horse.getAttributeValue(Attributes.MOVEMENT_SPEED);
            cir.setReturnValue(base * cmd.speedMultiplier());
        }
    }
}
