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
package fr.lacaleche.mui;

import fr.lacaleche.mui.internal.UIManager;
import fr.lacaleche.mui.internal.fabric.SimpleScreen;
import icyllis.modernui.annotation.MainThread;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.fragment.Fragment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.Objects;
import java.util.Optional;

/** Client-only entry point for opening ModernUI fragment screens. */
public final class MuiApi {

    private MuiApi() {
    }

    @MainThread
    public static void openScreen(@NonNull Fragment fragment) {
        Objects.requireNonNull(fragment, "fragment");
        MuiRuntime.requireReady();
        UIManager.getInstance().open(fragment, null);
    }

    @MainThread
    public static void openScreen(@NonNull Fragment fragment, @Nullable Screen previousScreen) {
        Objects.requireNonNull(fragment, "fragment");
        MuiRuntime.requireReady();
        UIManager.getInstance().open(fragment, previousScreen);
    }

    @NonNull
    public static Screen createScreen(@NonNull Fragment fragment, @Nullable Screen previousScreen) {
        Objects.requireNonNull(fragment, "fragment");
        MuiRuntime.requireReady();
        return new SimpleScreen(UIManager.getInstance(), fragment, previousScreen);
    }

    public static MuiRuntime.State state() {
        return MuiRuntime.state();
    }

    public static Optional<Throwable> failure() {
        return MuiRuntime.failure();
    }

    public static boolean isReady() {
        return MuiRuntime.state() == MuiRuntime.State.READY;
    }
}
