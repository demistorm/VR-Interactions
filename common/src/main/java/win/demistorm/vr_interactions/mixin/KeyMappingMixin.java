package win.demistorm.vr_interactions.mixin;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import win.demistorm.vr_interactions.client.input.KeySuppressor;

// Input stealing: suppressed keys read as released to all but the interaction core
@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {

    @Shadow
    private int clickCount;

    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    private void vr_interactions$suppressDown(CallbackInfoReturnable<Boolean> cir) {
        if (KeySuppressor.isSuppressed((KeyMapping) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "consumeClick", at = @At("HEAD"), cancellable = true)
    private void vr_interactions$suppressClicks(CallbackInfoReturnable<Boolean> cir) {
        if (KeySuppressor.isSuppressed((KeyMapping) (Object) this)) {
            // Eat pending clicks so nothing fires when suppression ends mid-hold
            if (this.clickCount > 0) {
                this.clickCount--;
            }
            cir.setReturnValue(false);
        }
    }
}
