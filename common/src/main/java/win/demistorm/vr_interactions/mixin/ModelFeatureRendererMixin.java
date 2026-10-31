package win.demistorm.vr_interactions.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import win.demistorm.vr_interactions.client.feature.horse.RenderedEquineContext;

@Mixin(ModelFeatureRenderer.class)
public abstract class ModelFeatureRendererMixin {

    @Inject(method = "renderModel", at = @At("HEAD"))
    private void vr_interactions$onRenderModelHead(SubmitNodeStorage.ModelSubmit<?> modelSubmit, RenderType renderType,
                                                   VertexConsumer vertexConsumer, OutlineBufferSource outlineBufferSource,
                                                   MultiBufferSource.BufferSource bufferSource, CallbackInfo ci) {
        RenderedEquineContext.beginModelDraw(modelSubmit.model(), modelSubmit.state());
    }

    @Inject(method = "renderModel", at = @At("TAIL"))
    private void vr_interactions$onRenderModelTail(SubmitNodeStorage.ModelSubmit<?> modelSubmit, RenderType renderType,
                                                   VertexConsumer vertexConsumer, OutlineBufferSource outlineBufferSource,
                                                   MultiBufferSource.BufferSource bufferSource, CallbackInfo ci) {
        RenderedEquineContext.endModelDraw(modelSubmit.model(), modelSubmit.state());
    }
}
