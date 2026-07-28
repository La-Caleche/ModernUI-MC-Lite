/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * Modified for ModernUI MC Lite, 2026. The upstream assertOnRenderThread overwrite is not included.
 */
package fr.lacaleche.mui.internal.mixin;

import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.RenderSystem;
import fr.lacaleche.mui.MuiRuntime;
import fr.lacaleche.mui.internal.UIManager;
import icyllis.arc3d.engine.ContextOptions;
import icyllis.modernui.core.Core;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiFunction;

/** Initializes Arc3D after Minecraft has created its OpenGL renderer. */
@Mixin(RenderSystem.class)
public class RenderSystemMixin {

    @Inject(method = "initRenderer", at = @At("TAIL"), remap = false)
    private static void muiLite$initialize(long window, int debugLevel, boolean debugSync,
                                           BiFunction<ResourceLocation, ShaderType, String> shaderSource,
                                           boolean debugLabels, CallbackInfo callbackInfo) {
        MuiRuntime.starting();
        try {
            Core.initialize();
            if (!Core.initOpenGL(new ContextOptions())) {
                throw new IllegalStateException("Failed to initialize the Arc3D OpenGL backend");
            }
            UIManager.initialize();
        } catch (Throwable throwable) {
            MuiRuntime.failed(throwable);
            throw throwable;
        }
    }
}
