package win.demistorm.vr_interactions.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.LevelRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import win.demistorm.vr_interactions.client.render.HorseRopeRenderer;

// Horse rein rope world rendering
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow
    @Final
    private RenderBuffers renderBuffers;

    @Inject(method = "submitEntities", at = @At("TAIL"))
    private void vr_interactions$renderHorseRope(PoseStack poseStack, LevelRenderState levelRenderState,
                                                 SubmitNodeCollector submitNodeCollector, CallbackInfo ci) {
        HorseRopeRenderer.render(levelRenderState.cameraRenderState.pos, this.renderBuffers.bufferSource());
    }
}
