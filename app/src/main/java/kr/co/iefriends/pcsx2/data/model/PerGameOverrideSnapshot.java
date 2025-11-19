package kr.co.iefriends.pcsx2.data.model;

import androidx.annotation.Nullable;

public final class PerGameOverrideSnapshot {
    public final String enableCheats;
    public final String widescreen;
    public final String noInterlacing;
    public final String loadTextures;
    public final String asyncTextures;
    public final String precacheTextures;
    public final String showFps;
    public final String renderer;
    public final String aspectRatio;

    public PerGameOverrideSnapshot(@Nullable String enableCheats,
                                   @Nullable String widescreen,
                                   @Nullable String noInterlacing,
                                   @Nullable String loadTextures,
                                   @Nullable String asyncTextures,
                                   @Nullable String precacheTextures,
                                   @Nullable String showFps,
                                   @Nullable String renderer,
                                   @Nullable String aspectRatio) {
        this.enableCheats = enableCheats;
        this.widescreen = widescreen;
        this.noInterlacing = noInterlacing;
        this.loadTextures = loadTextures;
        this.asyncTextures = asyncTextures;
        this.precacheTextures = precacheTextures;
        this.showFps = showFps;
        this.renderer = renderer;
        this.aspectRatio = aspectRatio;
    }
}
