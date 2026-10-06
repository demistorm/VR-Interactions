package win.demistorm.vr_interactions.mixin;

import net.minecraft.client.renderer.entity.state.EquineRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import win.demistorm.vr_interactions.client.feature.horse.SaddleReinsHider;

@Mixin(EquineRenderState.class)
public abstract class EquineRenderStateMixin implements SaddleReinsHider {

    @Unique
    private boolean vr_interactions$hideSaddleReins;

    @Override
    public void vr_interactions$hideSaddleReins(boolean hide) {
        this.vr_interactions$hideSaddleReins = hide;
    }

    @Override
    public boolean vr_interactions$hideSaddleReins() {
        return this.vr_interactions$hideSaddleReins;
    }
}
