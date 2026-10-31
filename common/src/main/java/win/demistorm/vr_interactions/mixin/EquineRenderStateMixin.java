package win.demistorm.vr_interactions.mixin;

import net.minecraft.client.renderer.entity.state.EquineRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import win.demistorm.vr_interactions.client.feature.horse.ReinsRenderState;

@Mixin(EquineRenderState.class)
public abstract class EquineRenderStateMixin implements ReinsRenderState {

    @Unique
    private boolean vr_interactions$hideSaddleReins;

    @Unique
    private int vr_interactions$trackedHorseId;

    @Override
    public void vr_interactions$hideSaddleReins(boolean hide) {
        this.vr_interactions$hideSaddleReins = hide;
    }

    @Override
    public boolean vr_interactions$hideSaddleReins() {
        return this.vr_interactions$hideSaddleReins;
    }

    @Override
    public void vr_interactions$trackedHorseId(int id) {
        this.vr_interactions$trackedHorseId = id;
    }

    @Override
    public int vr_interactions$trackedHorseId() {
        return this.vr_interactions$trackedHorseId;
    }
}
