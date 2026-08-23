package win.demistorm.template.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import win.demistorm.template.DemistormTemplate;
import win.demistorm.template.client.VivecraftGate;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Unique
    private static boolean demistorm_template$saidHello = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void demistorm_template$sayHelloOnce(CallbackInfo ci) {
        if (demistorm_template$saidHello) {
            return;
        }
        demistorm_template$saidHello = true;
        DemistormTemplate.LOGGER.info("Client mixin works! Vivecraft: {}, VR active: {}",
            VivecraftGate.isVivecraftPresent(), VivecraftGate.isVRActive());
    }
}
