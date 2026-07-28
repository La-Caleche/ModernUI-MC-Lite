/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation; either version 3 of
 * the license, or (at your option) any later version.
 *
 * Modified for ModernUI MC Lite, 2026.
 */
package fr.lacaleche.mui.internal.fabric;

import fr.lacaleche.mui.ScreenCallback;
import fr.lacaleche.mui.internal.UIManager;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.fragment.Fragment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;

/** Vanilla screen shell that delegates lifecycle, rendering, and input to ModernUI. */
public final class SimpleScreen extends Screen {

    private final UIManager host;
    private final Fragment fragment;
    private final Screen previousScreen;
    private final ScreenCallback callback;

    public SimpleScreen(UIManager host, Fragment fragment, @Nullable Screen previousScreen) {
        super(CommonComponents.EMPTY);
        this.host = host;
        this.fragment = fragment;
        this.previousScreen = previousScreen;
        this.callback = fragment instanceof ScreenCallback screenCallback ? screenCallback : null;
    }

    @Override
    protected void init() {
        super.init();
        this.host.initScreen(this);
    }

    @Override
    public void renderBackground(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float deltaTick) {
        if ((this.callback == null || this.callback.hasDefaultBackground())
                && this.minecraft != null && this.minecraft.level == null) {
            super.renderBackground(graphics, mouseX, mouseY, deltaTick);
        }
    }

    @Override
    public void render(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float deltaTick) {
        this.host.render(graphics);
    }

    @Override
    public void removed() {
        super.removed();
        this.host.removeScreen(this);
    }

    @Override
    public boolean isPauseScreen() {
        return this.callback != null && this.callback.isPauseScreen();
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        this.host.onHoverMove();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        this.host.onScroll(deltaX, deltaY);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        this.host.onKey(keyCode, scanCode, modifiers, true);
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        this.host.onKey(keyCode, scanCode, modifiers, false);
        return true;
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        return this.host.onCharTyped(character);
    }

    public Fragment fragment() {
        return this.fragment;
    }

    public Screen previousScreen() {
        return this.previousScreen;
    }

    public ScreenCallback callback() {
        return this.callback;
    }
}
