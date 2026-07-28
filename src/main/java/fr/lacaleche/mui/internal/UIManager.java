/*
 * Modern UI.
 * Copyright (C) 2019-2026 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * Modified and reduced to a Fragment-screen host for ModernUI MC Lite, 2026.
 */
package fr.lacaleche.mui.internal;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTextureView;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import fr.lacaleche.mui.MuiRuntime;
import fr.lacaleche.mui.ScreenCallback;
import fr.lacaleche.mui.internal.b3d.WrappedGlTexture;
import fr.lacaleche.mui.internal.fabric.ModernUIFabric;
import fr.lacaleche.mui.internal.fabric.SimpleScreen;
import icyllis.arc3d.core.ColorInfo;
import icyllis.arc3d.core.ColorSpaces;
import icyllis.arc3d.core.ImageInfo;
import icyllis.arc3d.core.RefCnt;
import icyllis.arc3d.engine.Engine;
import icyllis.arc3d.engine.ImageProxy;
import icyllis.arc3d.engine.ImmediateContext;
import icyllis.arc3d.granite.GraniteSurface;
import icyllis.arc3d.granite.Recording;
import icyllis.arc3d.granite.RecordingContext;
import icyllis.arc3d.opengl.GLDevice;
import icyllis.arc3d.opengl.GLTexture;
import icyllis.modernui.ModernUI;
import icyllis.modernui.R;
import icyllis.modernui.annotation.MainThread;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.RenderThread;
import icyllis.modernui.annotation.UiThread;
import icyllis.modernui.core.Core;
import icyllis.modernui.core.Handler;
import icyllis.modernui.core.Looper;
import icyllis.modernui.core.Message;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.fragment.FragmentContainerView;
import icyllis.modernui.fragment.FragmentController;
import icyllis.modernui.fragment.FragmentHostCallback;
import icyllis.modernui.fragment.FragmentTransaction;
import icyllis.modernui.fragment.OnBackPressedDispatcher;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.LightingInfo;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.graphics.pipeline.ArcCanvas;
import icyllis.modernui.lifecycle.Lifecycle;
import icyllis.modernui.lifecycle.LifecycleOwner;
import icyllis.modernui.lifecycle.LifecycleRegistry;
import icyllis.modernui.lifecycle.ViewModelStore;
import icyllis.modernui.lifecycle.ViewModelStoreOwner;
import icyllis.modernui.resources.TypedValue;
import icyllis.modernui.text.Editable;
import icyllis.modernui.text.Selection;
import icyllis.modernui.util.DisplayMetrics;
import icyllis.modernui.view.KeyEvent;
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.view.PointerIcon;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewRoot;
import icyllis.modernui.view.WindowGroup;
import icyllis.modernui.view.WindowManager;
import icyllis.modernui.view.menu.ContextMenuBuilder;
import icyllis.modernui.view.menu.MenuHelper;
import icyllis.modernui.widget.EditText;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix3x2f;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.system.MemoryUtil;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetMouseButton;
import static org.lwjgl.glfw.GLFW.glfwSetCursor;

/** Owns the ModernUI thread, Fragment lifecycle, Arc3D surface, and Minecraft composition. */
public final class UIManager implements LifecycleOwner {

    private static final int FRAGMENT_CONTAINER_ID = 0x01020007;
    private static volatile UIManager instance;

    private final Minecraft minecraft = Minecraft.getInstance();
    private final Window window = this.minecraft.getWindow();
    private final Thread uiThread;
    private final AtomicBoolean shutdown = new AtomicBoolean();
    private final OnBackPressedDispatcher backDispatcher =
            new OnBackPressedDispatcher(() -> this.minecraft.schedule(this::onBackPressed));
    private final StringBuilder charInput = new StringBuilder();
    private final Runnable commitCharInput = this::commitCharInput;

    private volatile boolean running;
    private volatile Looper looper;
    private volatile HostViewRoot root;
    private volatile SimpleScreen screen;
    private WindowGroup decor;
    private LifecycleRegistry lifecycle;
    private ViewModelStore viewModels;
    private FragmentController fragments;
    private long frameTimeNanos;
    private long lastPurgeNanos;
    private WrappedGlTexture layerTexture;
    private GlTextureView layerTextureView;

    private UIManager() {
        this.running = true;
        this.uiThread = new Thread(this::run, "ModernUI MC Lite UI");
        this.uiThread.setDaemon(false);
        this.uiThread.start();
    }

    @RenderThread
    public static void initialize() {
        Core.checkRenderThread();
        if (instance != null) throw new IllegalStateException("UI manager initialized twice");
        instance = new UIManager();
    }

    public static UIManager getInstance() {
        UIManager manager = instance;
        if (manager == null) throw new IllegalStateException("UI manager is not initialized");
        return manager;
    }

    public static UIManager getIfInitialized() {
        return instance;
    }

    @Override
    public Lifecycle getLifecycle() {
        return this.lifecycle;
    }

    public WindowGroup getDecorView() {
        WindowGroup view = this.decor;
        if (view == null) throw new IllegalStateException("ModernUI decor is not ready");
        return view;
    }

    @MainThread
    public void open(@NonNull Fragment fragment, net.minecraft.client.gui.screens.Screen previousScreen) {
        if (!this.minecraft.isSameThread()) throw new IllegalStateException("Not called from client thread");
        this.minecraft.setScreen(new SimpleScreen(this, fragment, previousScreen));
    }

    @MainThread
    public void initScreen(SimpleScreen target) {
        if (this.screen != null && this.screen != target) {
            throw new IllegalStateException("A ModernUI screen is already active");
        }
        if (this.screen == null) {
            this.fragments.getFragmentManager().beginTransaction()
                    .add(FRAGMENT_CONTAINER_ID, target.fragment(), "main")
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .setReorderingAllowed(true)
                    .commit();
        }
        this.screen = target;
        this.resize(this.window.getWidth(), this.window.getHeight());
    }

    @MainThread
    public void removeScreen(SimpleScreen target) {
        if (this.screen != target) return;
        this.fragments.getFragmentManager().beginTransaction()
                .remove(target.fragment())
                .setReorderingAllowed(true)
                .commit();
        this.screen = null;
        glfwSetCursor(this.window.getWindow(), MemoryUtil.NULL);
    }

    public void onHoverMove() {
        HostViewRoot viewRoot = this.root;
        if (!this.running || viewRoot == null || this.screen == null) return;
        long now = Core.timeNanos();
        float x = this.pointerX();
        float y = this.pointerY();
        viewRoot.enqueueInputEvent(MotionEvent.obtain(now, MotionEvent.ACTION_HOVER_MOVE, x, y, 0));
        if (viewRoot.buttonState != 0) {
            viewRoot.enqueueInputEvent(MotionEvent.obtain(now, MotionEvent.ACTION_MOVE, 0, x, y, 0,
                    viewRoot.buttonState, 0));
        }
    }

    public void onScroll(double horizontal, double vertical) {
        HostViewRoot viewRoot = this.root;
        if (!this.running || viewRoot == null || this.screen == null) return;
        MotionEvent event = MotionEvent.obtain(Core.timeNanos(), MotionEvent.ACTION_SCROLL,
                this.pointerX(), this.pointerY(), 0);
        event.setAxisValue(MotionEvent.AXIS_HSCROLL, (float) horizontal);
        event.setAxisValue(MotionEvent.AXIS_VSCROLL, (float) vertical);
        viewRoot.enqueueInputEvent(event);
    }

    public void onMouseButton(int button, int action, int modifiers) {
        HostViewRoot viewRoot = this.root;
        if (!this.running || viewRoot == null || this.screen == null || this.minecraft.getOverlay() != null) return;
        int buttonState = 0;
        for (int index = 0; index < 5; index++) {
            if (glfwGetMouseButton(this.window.getWindow(), index) == GLFW_PRESS) buttonState |= 1 << index;
        }
        viewRoot.buttonState = buttonState;
        int actionButton = 1 << button;
        int buttonAction = action == GLFW_PRESS ? MotionEvent.ACTION_BUTTON_PRESS : MotionEvent.ACTION_BUTTON_RELEASE;
        int touchAction = action == GLFW_PRESS ? MotionEvent.ACTION_DOWN : MotionEvent.ACTION_UP;
        long now = Core.timeNanos();
        float x = this.pointerX();
        float y = this.pointerY();
        viewRoot.enqueueInputEvent(MotionEvent.obtain(now, buttonAction, actionButton, x, y,
                modifiers, buttonState, 0));
        if ((touchAction == MotionEvent.ACTION_DOWN && (buttonState ^ actionButton) == 0)
                || (touchAction == MotionEvent.ACTION_UP && buttonState == 0)) {
            viewRoot.enqueueInputEvent(MotionEvent.obtain(now, touchAction, actionButton, x, y,
                    modifiers, buttonState, 0));
        }
    }

    public void onKey(int keyCode, int scanCode, int modifiers, boolean pressed) {
        HostViewRoot viewRoot = this.root;
        if (!this.running || viewRoot == null) return;
        int action = pressed ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP;
        viewRoot.enqueueInputEvent(KeyEvent.obtain(Core.timeNanos(), action, keyCode, 0,
                modifiers, scanCode, 0));
    }

    @MainThread
    public boolean onCharTyped(char character) {
        if (!this.running || character == '\0' || character == '\u007f') return false;
        this.charInput.append(character);
        Core.postOnMainThread(this.commitCharInput);
        return true;
    }

    @RenderThread
    public void render(GuiGraphics graphics) {
        HostViewRoot viewRoot = this.root;
        if (!this.running || viewRoot == null) return;
        Core.flushMainCalls();
        ImmediateContext context = Core.requireImmediateContext();
        Pair<Recording, ImageProxy> frame = viewRoot.swapFrameTask();
        Recording recording = frame.getLeft();
        ImageProxy surface = frame.getRight();
        try {
            try {
                if (recording != null) {
                    boolean added;
                    try {
                        added = context.addTask(recording);
                    } finally {
                        recording.close();
                    }
                    if (!added) ModernUIFabric.LOGGER.error("Arc3D rejected a ModernUI recording");
                }
                ((GLDevice) context.getDevice()).flushRenderCalls();
                if (recording != null) context.submit(); else context.checkForFinishedWork();
            } finally {
                this.restoreBlazeState();
            }

            if (surface != null && surface.getImage() instanceof GLTexture layer) {
                if (this.layerTexture == null || !this.layerTexture.wraps(layer)) {
                    this.closeLayerTexture();
                    layer.ref();
                    this.layerTexture = new WrappedGlTexture(layer);
                    this.layerTextureView = (GlTextureView) RenderSystem.getDevice()
                            .createTextureView(this.layerTexture);
                } else {
                    this.layerTexture.retainForFrame();
                }
                graphics.nextStratum();
                graphics.guiRenderState.submitGuiElement(new BlitRenderState(
                        RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                        TextureSetup.singleTexture(this.layerTextureView),
                        new Matrix3x2f().scale(1.0F / this.window.getGuiScale()),
                        0, 0, this.window.getWidth(), this.window.getHeight(),
                        0.0F, 1.0F, 0.0F, 1.0F, ~0, null));
            }
        } finally {
            RefCnt.move(surface);
        }
    }

    @RenderThread
    public void onFrameStart() {
        this.frameTimeNanos = Core.timeNanos();
    }

    @RenderThread
    public void onFrameEnd() {
        ImmediateContext context = Core.requireImmediateContext();
        if (this.frameTimeNanos - this.lastPurgeNanos >= 20_000_000_000L) {
            this.lastPurgeNanos = this.frameTimeNanos;
            context.performDeferredCleanup(120_000);
        }
        if (this.layerTexture != null) this.layerTexture.close();
    }

    public static void destroy() {
        UIManager manager = instance;
        if (manager == null || !manager.shutdown.compareAndSet(false, true)) return;
        MuiRuntime.stopping();
        manager.running = false;
        HostViewRoot viewRoot = manager.root;
        if (viewRoot != null) {
            viewRoot.wakeFrameWait();
            viewRoot.mHandler.post(manager::finish);
        } else {
            Looper activeLooper = manager.looper;
            if (activeLooper != null) activeLooper.quitSafely();
        }
        boolean interrupted = false;
        try {
            manager.uiThread.join(5000);
        } catch (InterruptedException exception) {
            interrupted = true;
        }
        if (manager.uiThread.isAlive()) {
            manager.uiThread.interrupt();
            if (viewRoot != null) viewRoot.wakeFrameWait();
        }
        while (manager.uiThread.isAlive()) {
            try {
                manager.uiThread.join();
            } catch (InterruptedException exception) {
                interrupted = true;
            }
        }
        manager.closeLayerTexture();
        Core.requireImmediateContext().unref();
        instance = null;
        MuiRuntime.stopped();
        if (interrupted) Thread.currentThread().interrupt();
        ModernUIFabric.LOGGER.info("ModernUI MC Lite stopped");
    }

    @UiThread
    private void run() {
        boolean recordingContextCreated = false;
        try {
            this.looper = Core.initUiThread();
            recordingContextCreated = true;
            this.initializeViews();
            if (this.running) {
                MuiRuntime.ready();
                Looper.loop();
            } else {
                this.finish();
            }
        } catch (Throwable throwable) {
            this.running = false;
            MuiRuntime.failed(throwable);
            ModernUIFabric.LOGGER.error("ModernUI thread failed", throwable);
            if (this.minecraft.isRunning()) {
                this.minecraft.delayCrashRaw(CrashReport.forThrowable(throwable, "Exception on ModernUI thread"));
            }
        } finally {
            HostViewRoot viewRoot = this.root;
            if (viewRoot != null) viewRoot.releaseUiResources();
            if (recordingContextCreated) Core.requireUiRecordingContext().unref();
        }
    }

    @UiThread
    private void initializeViews() {
        this.root = new HostViewRoot();
        this.decor = new WindowGroup(ModernUI.getInstance());
        this.decor.setWillNotDraw(true);
        this.decor.setId(R.id.content);
        FragmentContainerView container = new FragmentContainerView(ModernUI.getInstance());
        container.setLayoutParams(new WindowManager.LayoutParams());
        container.setWillNotDraw(true);
        container.setId(FRAGMENT_CONTAINER_ID);
        this.decor.addView(container);
        this.decor.setIsRootNamespace(true);
        this.root.setView(this.decor);
        this.resize(this.window.getWidth(), this.window.getHeight());

        this.lifecycle = new LifecycleRegistry(this);
        this.viewModels = new ViewModelStore();
        this.fragments = FragmentController.createController(new HostCallbacks());
        this.fragments.attachHost(null);
        this.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        this.fragments.dispatchCreate();
        this.fragments.dispatchActivityCreated();
        this.fragments.execPendingActions();
        this.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
        this.fragments.dispatchStart();
        this.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        this.fragments.dispatchResume();
        ModernUIFabric.LOGGER.info("ModernUI thread ready");
    }

    @UiThread
    private void finish() {
        if (this.fragments != null) {
            this.fragments.dispatchStop();
            this.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
            this.fragments.dispatchDestroy();
            this.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
        }
        if (this.looper != null) this.looper.quitSafely();
    }

    private void resize(int width, int height) {
        HostViewRoot viewRoot = this.root;
        if (viewRoot != null) viewRoot.mHandler.post(() -> viewRoot.setFrame(width, height));
    }

    private void onBackPressed() {
        SimpleScreen activeScreen = this.screen;
        if (activeScreen == null) return;
        ScreenCallback callback = activeScreen.callback();
        if (callback != null && !callback.shouldClose()) return;
        this.minecraft.setScreen(activeScreen.previousScreen());
    }

    private void commitCharInput() {
        HostViewRoot viewRoot = this.root;
        if (viewRoot == null || this.charInput.isEmpty()) return;
        String input = this.charInput.toString();
        this.charInput.setLength(0);
        Message message = Message.obtain(viewRoot.mHandler, () -> {
            if (this.decor.findFocus() instanceof EditText editText) {
                Editable content = editText.getText();
                int start = editText.getSelectionStart();
                int end = editText.getSelectionEnd();
                if (start >= 0 && end >= 0) {
                    Selection.setSelection(content, Math.max(start, end));
                    content.replace(Math.min(start, end), Math.max(start, end), input);
                }
            }
        });
        message.setAsynchronous(true);
        message.sendToTarget();
    }

    private float pointerX() {
        return (float) (this.minecraft.mouseHandler.xpos() * this.window.getWidth() / this.window.getScreenWidth());
    }

    private float pointerY() {
        return (float) (this.minecraft.mouseHandler.ypos() * this.window.getHeight() / this.window.getScreenHeight());
    }

    @RenderThread
    private void restoreBlazeState() {
        for (int index = 0; index <= 3; index++) GL33C.glBindSampler(index, 0);
        GL33C.glDisable(GL33C.GL_STENCIL_TEST);
        GlStateManager._disableScissorTest();
        GL33C.glDisable(GL33C.GL_SCISSOR_TEST);
        GlStateManager._enableBlend();
        GL33C.glEnable(GL33C.GL_BLEND);
        GlStateManager._disableDepthTest();
        GL33C.glDisable(GL33C.GL_DEPTH_TEST);
        GlStateManager._depthFunc(GL33C.GL_LEQUAL);
        GL33C.glDepthFunc(GL33C.GL_LEQUAL);
        GlStateManager._depthMask(true);
        GL33C.glDepthMask(true);
        for (int index = 3; index >= 0; index--) {
            GlStateManager._activeTexture(GL33C.GL_TEXTURE0 + index);
            GlStateManager._bindTexture(0);
        }
        GL33C.glActiveTexture(GL33C.GL_TEXTURE0);
        GlStateManager._disableCull();
    }

    private void closeLayerTexture() {
        if (this.layerTextureView != null) this.layerTextureView.close();
        if (this.layerTexture != null) this.layerTexture.close();
        this.layerTextureView = null;
        this.layerTexture = null;
    }

    @UiThread
    private final class HostViewRoot extends ViewRoot {

        private final Rect focusBounds = new Rect();
        private GraniteSurface surface;
        private Recording lastRecording;
        private ContextMenuBuilder contextMenu;
        private MenuHelper contextMenuHelper;
        private long lastCleanupNanos;
        private int buttonState;

        @Override
        protected boolean dispatchTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN && decor.findFocus() instanceof EditText editText) {
                editText.getGlobalVisibleRect(this.focusBounds);
                if (!this.focusBounds.contains((int) event.getRawX(), (int) event.getRawY())) editText.clearFocus();
            }
            return super.dispatchTouchEvent(event);
        }

        @Override
        protected void onKeyEvent(KeyEvent event) {
            SimpleScreen activeScreen = screen;
            if (activeScreen == null || event.getAction() != KeyEvent.ACTION_DOWN) return;
            ScreenCallback callback = activeScreen.callback();
            boolean back = callback != null
                    ? callback.isBackKey(event.getKeyCode(), event)
                    : event.getKeyCode() == KeyEvent.KEY_ESCAPE;
            if (!back) return;
            View focused = decor.findFocus();
            if (focused instanceof EditText && event.getKeyCode() == KeyEvent.KEY_ESCAPE) focused.clearFocus();
            else backDispatcher.onBackPressed();
        }

        @Override
        public void setFrame(int width, int height) {
            super.setFrame(width, height);
            if (width <= 0 || height <= 0) return;
            DisplayMetrics metrics = ModernUI.getInstance().getResources().getDisplayMetrics();
            float ratio = Math.min(width, height) / (450.0F * metrics.density);
            float lightZ = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DP, 500, metrics)
                    * ((ratio + 2.0F) / 3.0F);
            float radius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DP, 800, metrics);
            LightingInfo.setLightGeometry(width / 2.0F, 0, lightZ, radius);
        }

        @Override
        protected Canvas beginDrawLocked(int width, int height) {
            synchronized (this.mRenderLock) {
                if (this.surface == null || this.surface.getWidth() != width || this.surface.getHeight() != height) {
                    if (width > 0 && height > 0) {
                        this.surface = RefCnt.move(this.surface, GraniteSurface.makeRenderTarget(
                                Core.requireUiRecordingContext(),
                                ImageInfo.make(width, height, ColorInfo.CT_RGBA_8888,
                                        ColorInfo.AT_PREMUL, ColorSpaces.SRGB),
                                false, Engine.SurfaceOrigin.kUpperLeft, null));
                    }
                }
                return this.surface != null && width > 0 && height > 0
                        ? new ArcCanvas(this.surface.getCanvas()) : null;
            }
        }

        @Override
        protected void endDrawLocked(@NonNull Canvas canvas) {
            canvas.restoreToCount(1);
            Recording task = Core.requireUiRecordingContext().snap();
            synchronized (this.mRenderLock) {
                if (this.lastRecording != null) this.lastRecording.close();
                this.lastRecording = task;
                while (running && this.lastRecording == task) {
                    try {
                        this.mRenderLock.wait();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                if (this.lastRecording != null) this.lastRecording.close();
                this.lastRecording = null;
            }
            RecordingContext context = Core.requireUiRecordingContext();
            long now = System.nanoTime();
            if (now - this.lastCleanupNanos >= 20_000_000_000L) {
                this.lastCleanupNanos = now;
                context.performDeferredCleanup(120_000);
            }
        }

        @RenderThread
        private Pair<Recording, ImageProxy> swapFrameTask() {
            synchronized (this.mRenderLock) {
                ImageProxy layer = this.surface == null ? null : RefCnt.create(this.surface.getBackingTarget());
                Recording recording = this.lastRecording;
                this.lastRecording = null;
                this.mRenderLock.notifyAll();
                return Pair.of(recording, layer);
            }
        }

        private void wakeFrameWait() {
            synchronized (this.mRenderLock) {
                this.mRenderLock.notifyAll();
            }
        }

        @UiThread
        private void releaseUiResources() {
            synchronized (this.mRenderLock) {
                if (this.lastRecording != null) this.lastRecording.close();
                this.lastRecording = null;
                this.surface = RefCnt.move(this.surface);
                this.mRenderLock.notifyAll();
            }
        }

        @Override
        public void playSoundEffect(int effectId) {
        }

        @Override
        public boolean performHapticFeedback(int effectId, boolean always) {
            return false;
        }

        @Override
        protected void applyPointerIcon(int pointerType) {
            long handle = PointerIcon.getSystemIcon(pointerType).getHandle();
            minecraft.schedule(() -> glfwSetCursor(window.getWindow(), handle));
        }

        @Override
        public boolean showContextMenuForChild(View originalView, float x, float y) {
            if (this.contextMenuHelper != null) this.contextMenuHelper.dismiss();
            if (this.contextMenu == null) this.contextMenu = new ContextMenuBuilder(ModernUI.getInstance());
            else this.contextMenu.clearAll();
            this.contextMenuHelper = this.contextMenu.showPopup(ModernUI.getInstance(), originalView,
                    Float.isNaN(x) ? 0 : x, Float.isNaN(y) ? 0 : y);
            return this.contextMenuHelper != null;
        }
    }

    @UiThread
    private final class HostCallbacks extends FragmentHostCallback<Object>
            implements ViewModelStoreOwner, icyllis.modernui.fragment.OnBackPressedDispatcherOwner {

        private HostCallbacks() {
            super(ModernUI.getInstance(), new Handler(Looper.myLooper()));
        }

        @Override
        public Object onGetHost() {
            return null;
        }

        @Override
        public View onFindViewById(int id) {
            return decor.findViewById(id);
        }

        @Override
        public ViewModelStore getViewModelStore() {
            return viewModels;
        }

        @Override
        public OnBackPressedDispatcher getOnBackPressedDispatcher() {
            return backDispatcher;
        }

        @Override
        public Lifecycle getLifecycle() {
            return lifecycle;
        }
    }
}
