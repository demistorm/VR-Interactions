package win.demistorm.vr_interactions.mixin;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import win.demistorm.vr_interactions.client.feature.horse.HorseRingsFeature;

// Vivecraft writes the horse's body yaw from the headset mid-aiStep, this TAIL mixin re-asserts the gesture rotation
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void vr_interactions$assertGestureRotation(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (player.getVehicle() instanceof AbstractHorse horse
                && HorseRingsFeature.commandFor(horse) != null) {
            horse.yBodyRot = horse.getYRot();
            horse.yHeadRot = horse.getYRot();
        }
    }
}
