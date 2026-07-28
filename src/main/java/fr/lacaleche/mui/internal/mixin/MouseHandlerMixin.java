/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * Modified for ModernUI MC Lite, 2026.
 */
package fr.lacaleche.mui.internal.mixin;

import fr.lacaleche.mui.internal.UIManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Routes raw mouse button state to ModernUI after vanilla screen dispatch. */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "onPress", at = @At("TAIL"))
    private void muiLite$mouseButton(long window, int button, int action, int modifiers,
                                     CallbackInfo callbackInfo) {
        UIManager manager = UIManager.getIfInitialized();
        if (window == this.minecraft.getWindow().getWindow() && manager != null) {
            manager.onMouseButton(button, action, modifiers);
        }
    }
}
