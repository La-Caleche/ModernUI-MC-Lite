/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * Modified for ModernUI MC Lite, 2026.
 */
package fr.lacaleche.mui.internal.b3d;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import icyllis.arc3d.engine.Engine;
import icyllis.arc3d.opengl.GLTexture;

/** Blaze3D view of an Arc3D texture; the wrapper owns one Arc3D reference. */
public final class WrappedGlTexture extends GlTexture {

    private final GLTexture source;

    public WrappedGlTexture(GLTexture source) {
        super(USAGE_COPY_SRC | USAGE_TEXTURE_BINDING
                        | (source.isRenderable() ? USAGE_RENDER_ATTACHMENT : 0),
                source.getLabel(),
                source.getGLFormat() == GlConst.GL_RGBA8 ? TextureFormat.RGBA8 : TextureFormat.RED8,
                source.getWidth(), source.getHeight(), 1, source.getMipLevelCount(), source.getHandle());
        if (source.getImageType() != Engine.ImageType.k2D || source.getDepth() != 1
                || source.getArraySize() != 1 || source.getSampleCount() != 1) {
            throw new IllegalArgumentException("Arc3D texture is not a single-sample 2D texture");
        }
        this.source = source;
    }

    public boolean wraps(GLTexture texture) {
        return this.source == texture;
    }

    public void retainForFrame() {
        if (this.closed) {
            this.closed = false;
            this.source.ref();
        }
    }

    @Override
    public void close() {
        if (this.closed) return;
        this.closed = true;
        this.source.unref();
    }

    @Override
    public void addViews() {
    }

    @Override
    public void removeViews() {
    }
}
