package win.demistorm.vr_interactions.mixin;

import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ModelPart.class)
public interface ModelPartAccessor {

    @Accessor("children")
    Map<String, ModelPart> vr_interactions$children();
}
