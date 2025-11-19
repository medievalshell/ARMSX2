package kr.co.iefriends.pcsx2.core.util;

import static kr.co.iefriends.pcsx2.core.util.EmulatorSettingsUtils.boolToString;
import static kr.co.iefriends.pcsx2.core.util.EmulatorSettingsUtils.setNativeSetting;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.Nullable;


import kr.co.iefriends.pcsx2.data.model.GameEntry;
import kr.co.iefriends.pcsx2.data.model.PerGameOverrideSnapshot;

public class GameSettingsApplier {
    private static boolean perGameOverridesActive = false;
    @Nullable
    private static PerGameOverrideSnapshot lastPerGameOverrideSnapshot = null;
    @Nullable
    private static String lastPerGameOverrideKey = null;

    public static void applyPerGameSettingsForEntry(@Nullable GameEntry entry, Context ctx) {
        if (entry == null) {
            return;
        }
        applyPerGameSettingsForKey(CoversUtils.gameKeyFromEntry(entry), ctx);
    }

    public static void applyPerGameSettingsForUri(@Nullable Uri uri, Context ctx) {
        applyPerGameSettingsForKey(uri != null ? uri.toString() : null, ctx);
    }

    public static void restorePerGameOverrides() {
        if (!perGameOverridesActive) {
            lastPerGameOverrideSnapshot = null;
            lastPerGameOverrideKey = null;
            return;
        }
        PerGameOverrideSnapshot snapshot = lastPerGameOverrideSnapshot;
        perGameOverridesActive = false;
        lastPerGameOverrideSnapshot = null;
        lastPerGameOverrideKey = null;
        if (snapshot == null) {
            return;
        }

        setNativeSetting("EmuCore", "EnableCheats", "bool", snapshot.enableCheats);
        setNativeSetting("EmuCore", "EnableWideScreenPatches", "bool", snapshot.widescreen);
        setNativeSetting("EmuCore", "EnableNoInterlacingPatches", "bool", snapshot.noInterlacing);
        setNativeSetting("EmuCore/GS", "LoadTextureReplacements", "bool", snapshot.loadTextures);
        setNativeSetting("EmuCore/GS", "LoadTextureReplacementsAsync", "bool", snapshot.asyncTextures);
        setNativeSetting("EmuCore/GS", "PrecacheTextureReplacements", "bool", snapshot.precacheTextures);
        setNativeSetting("EmuCore/GS", "OsdShowFPS", "bool", snapshot.showFps);
        setNativeSetting("EmuCore/GS", "Renderer", "int", snapshot.renderer);
        setNativeSetting("EmuCore/GS", "AspectRatio", "string", snapshot.aspectRatio);
    }

    private static void applyPerGameSettingsForKey(@Nullable String gameKey, Context ctx) {
        restorePerGameOverrides();
        if (TextUtils.isEmpty(gameKey)) {
            return;
        }
        kr.co.iefriends.pcsx2.core.util.GameSpecificSettingsManager.GameSettings settings = kr.co.iefriends.pcsx2.core.util.GameSpecificSettingsManager.getSettings(ctx, gameKey);
        if (settings == null || !settings.hasOverrides()) {
            return;
        }

        PerGameOverrideSnapshot snapshot = EmulatorSettingsUtils.captureCurrentPerGameSnapshot(ctx);
        boolean applied = false;

        if (settings.enableCheats != null) {
            setNativeSetting("EmuCore", "EnableCheats", "bool", boolToString(settings.enableCheats));
            applied = true;
        }
        if (settings.widescreen != null) {
            setNativeSetting("EmuCore", "EnableWideScreenPatches", "bool", boolToString(settings.widescreen));
            applied = true;
        }
        if (settings.noInterlacing != null) {
            setNativeSetting("EmuCore", "EnableNoInterlacingPatches", "bool", boolToString(settings.noInterlacing));
            applied = true;
        }
        if (settings.loadTextures != null) {
            setNativeSetting("EmuCore/GS", "LoadTextureReplacements", "bool", boolToString(settings.loadTextures));
            applied = true;
        }
        if (settings.asyncTextures != null) {
            setNativeSetting("EmuCore/GS", "LoadTextureReplacementsAsync", "bool", boolToString(settings.asyncTextures));
            applied = true;
        }
        if (settings.precacheTextures != null) {
            setNativeSetting("EmuCore/GS", "PrecacheTextureReplacements", "bool", boolToString(settings.precacheTextures));
            applied = true;
        }
        if (settings.showFps != null) {
            setNativeSetting("EmuCore/GS", "OsdShowFPS", "bool", boolToString(settings.showFps));
            applied = true;
        }
        if (settings.renderer != null) {
            setNativeSetting("EmuCore/GS", "Renderer", "int", Integer.toString(settings.renderer));
            applied = true;
        }
        if (!TextUtils.isEmpty(settings.aspectRatio)) {
            setNativeSetting("EmuCore/GS", "AspectRatio", "string", settings.aspectRatio);
            applied = true;
        }

        if (applied) {
            perGameOverridesActive = true;
            lastPerGameOverrideSnapshot = snapshot;
            lastPerGameOverrideKey = gameKey;
        }
    }

}
