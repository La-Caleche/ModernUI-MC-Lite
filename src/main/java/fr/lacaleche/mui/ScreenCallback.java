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

import icyllis.modernui.annotation.MainThread;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.RenderThread;
import icyllis.modernui.annotation.UiThread;
import icyllis.modernui.view.KeyEvent;

/** Optional fragment callback for screen close, pause, and background policy. */
public interface ScreenCallback {

    @UiThread
    default boolean isBackKey(int keyCode, @NonNull KeyEvent event) {
        return keyCode == KeyEvent.KEY_ESCAPE;
    }

    @MainThread
    default boolean shouldClose() {
        return true;
    }

    @MainThread
    default boolean isPauseScreen() {
        return false;
    }

    @RenderThread
    default boolean hasDefaultBackground() {
        return true;
    }
}
