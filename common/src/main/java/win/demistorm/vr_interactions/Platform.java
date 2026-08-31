package win.demistorm.vr_interactions;

import dev.architectury.injectables.annotations.ExpectPlatform;

import java.util.function.BooleanSupplier;

// Loader abstraction for the hooks the client needs
public final class Platform {

    private Platform() {}

    @ExpectPlatform
    public static void registerClientTickEvent(Runnable runnable) {
        throw new RuntimeException("@ExpectPlatform should have replaced this");
    }

    // Suppresses vanilla attack/use while the suppliers return true (interact sessions)
    @ExpectPlatform
    public static void registerClientInputCancellation(BooleanSupplier cancelAttack, BooleanSupplier cancelUse) {
        throw new RuntimeException("@ExpectPlatform should have replaced this");
    }
}
