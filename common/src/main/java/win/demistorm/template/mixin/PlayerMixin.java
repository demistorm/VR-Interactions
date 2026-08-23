package win.demistorm.template.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import win.demistorm.template.DemistormTemplate;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Unique
    private int demistorm_template$ticksAlive = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void demistorm_template$logEveryMinute(CallbackInfo ci) {
        if (++this.demistorm_template$ticksAlive % 1200 == 0) {
            DemistormTemplate.LOGGER.info("Player mixin still ticking, common mixins work!");
        }
    }
}
