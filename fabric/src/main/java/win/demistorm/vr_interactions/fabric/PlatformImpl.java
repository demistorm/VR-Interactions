package win.demistorm.vr_interactions.fabric;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.world.InteractionResult;

import java.util.function.BooleanSupplier;

// Fabric implementations of the Platform hooks
@SuppressWarnings("unused")
public final class PlatformImpl {

    private PlatformImpl() {}

    public static void registerClientTickEvent(Runnable runnable) {
        ClientTickEvents.END_CLIENT_TICK.register(client -> runnable.run());
    }

    public static void registerClientInputCancellation(BooleanSupplier cancelAttack, BooleanSupplier cancelUse) {
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) ->
                cancelAttack.getAsBoolean() ? InteractionResult.FAIL : InteractionResult.PASS);

        UseBlockCallback.EVENT.register((player, level, hand, hitResult) ->
                cancelUse.getAsBoolean() ? InteractionResult.FAIL : InteractionResult.PASS);

        UseItemCallback.EVENT.register((player, level, hand) ->
                cancelUse.getAsBoolean() ? InteractionResult.FAIL : InteractionResult.PASS);
    }
}
