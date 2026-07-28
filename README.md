# ModernUI MC Lite

Minimal client-only Fabric binding for displaying ModernUI 3.13.0 `Fragment` screens on Minecraft
1.21.8. It records ModernUI into an Arc3D 2026.2.0 OpenGL surface and submits that surface to the
Blaze3D GUI pipeline.

## Usage

Call `MuiApi.openScreen(fragment)` on the Minecraft client thread, or construct a screen with
`MuiApi.createScreen(fragment, previousScreen)` and pass it to `Minecraft#setScreen`.

## Distribution

The remapped mod jar intentionally does not shade ModernUI or Arc3D. A distribution must put
`modernui-core:3.13.0` and the Arc3D `core`, `sketch`, `engine`, `granite`, `opengl`, `vulkan`, and
`compiler` artifacts at version `2026.2.0` on the game classpath. Gradle module metadata and the
published POM retain these normal dependencies. This is preferable to embedding LGPL libraries
because downstream launchers can replace compatible library versions. Fabric metadata cannot
express non-mod Maven libraries, so copying only the remapped Fabric jar is not a complete runtime
distribution.

The binding and LGPL-derived host files are licensed under LGPL-3.0-or-later. See `LICENSES/NOTICE`
for upstream attribution and license locations.
