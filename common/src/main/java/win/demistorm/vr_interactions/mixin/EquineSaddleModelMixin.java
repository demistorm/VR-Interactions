package win.demistorm.vr_interactions.mixin;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.animal.equine.EquineSaddleModel;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import win.demistorm.vr_interactions.client.feature.horse.SaddleReinsHider;

// ridingParts are the vanilla rein lines (vanilla writes their isRidden visibility right before)
@Mixin(EquineSaddleModel.class)
public abstract class EquineSaddleModelMixin {

    @Shadow
    @Final
    private ModelPart[] ridingParts;

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void vr_interactions$hideReinLines(EquineRenderState state, CallbackInfo ci) {
        if (((SaddleReinsHider) state).vr_interactions$hideSaddleReins()) {
            for (ModelPart part : ridingParts) {
                part.visible = false;
            }
        }
    }
}
