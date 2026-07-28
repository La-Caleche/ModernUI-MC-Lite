package fr.lacaleche.mui.internal.mixin;

import fr.lacaleche.mui.internal.UIManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Drives frame timing and releases submitted texture references. */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void muiLite$frameStart(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo callbackInfo) {
        UIManager manager = UIManager.getIfInitialized();
        if (manager != null) manager.onFrameStart();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void muiLite$frameEnd(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo callbackInfo) {
        UIManager manager = UIManager.getIfInitialized();
        if (manager != null) manager.onFrameEnd();
    }
}
