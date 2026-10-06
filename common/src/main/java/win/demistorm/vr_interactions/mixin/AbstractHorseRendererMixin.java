package win.demistorm.vr_interactions.mixin;

import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import win.demistorm.vr_interactions.client.feature.horse.HorseRingsVisuals;
import win.demistorm.vr_interactions.client.feature.horse.SaddleReinsHider;

// Flags state every extract
@Mixin(AbstractHorseRenderer.class)
public abstract class AbstractHorseRendererMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void vr_interactions$flagSaddleReins(AbstractHorse horse, EquineRenderState state, float partialTick,
                                                 CallbackInfo ci) {
        ((SaddleReinsHider) state).vr_interactions$hideSaddleReins(HorseRingsVisuals.hideVanillaReins(horse));
    }
}
