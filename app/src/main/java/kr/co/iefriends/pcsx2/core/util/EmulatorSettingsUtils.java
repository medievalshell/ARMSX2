package kr.co.iefriends.pcsx2.core.util;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import kr.co.iefriends.pcsx2.NativeApp;
import kr.co.iefriends.pcsx2.R;
import kr.co.iefriends.pcsx2.data.model.PerGameOverrideSnapshot;

public class EmulatorSettingsUtils {
    @Nullable
    private static String safeGetSetting(String section, String key, String type) {
        try {
            return NativeApp.getSetting(section, key, type);
        } catch (Exception ignored) {
            return null;
        }
    }
    public static boolean readBoolSetting(String section, String key, boolean defaultValue) {
        try {
            String value = NativeApp.getSetting(section, key, "bool");
            if (value == null || value.isEmpty()) {
                return defaultValue;
            }
            return "1".equals(value) || "true".equalsIgnoreCase(value);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }
    public static String boolToString(boolean value) {
        return value ? "true" : "false";
    }
    public static int getCurrentRendererValue(Context ctx) {
        int initialValue = -1;
        try {
            String renderer = NativeApp.getSetting("EmuCore/GS", "Renderer", "int");
            if (!TextUtils.isEmpty(renderer)) {
                initialValue = Integer.parseInt(renderer);
            }
        } catch (Exception ignored) {}
        return initialValue;
    }
    public static String getCurrentAspectRatioValue(Context ctx) {
        if (ctx == null) {
            return "4:3";
        }
        android.content.res.Resources res = ctx.getResources();
        String[] aspectOptions = res.getStringArray(R.array.aspect_ratios);
        String defaultValue = aspectOptions.length > 1 ? aspectOptions[1] : aspectOptions[0];
        try {
            String aspect = NativeApp.getSetting("EmuCore/GS", "AspectRatio", "string");
            if (!TextUtils.isEmpty(aspect)) {
                return aspect;
            }
        } catch (Exception ignored) {}
        return defaultValue;
    }
    public static PerGameOverrideSnapshot captureCurrentPerGameSnapshot(Context ctx) {
        String cheats = safeGetSetting("EmuCore", "EnableCheats", "bool");
        if (cheats == null) {
            cheats = boolToString(readBoolSetting("EmuCore", "EnableCheats", false));
        }

        String widescreen = safeGetSetting("EmuCore", "EnableWideScreenPatches", "bool");
        if (widescreen == null) {
            widescreen = boolToString(readBoolSetting("EmuCore", "EnableWideScreenPatches", false));
        }

        String noInterlacing = safeGetSetting("EmuCore", "EnableNoInterlacingPatches", "bool");
        if (noInterlacing == null) {
            noInterlacing = boolToString(readBoolSetting("EmuCore", "EnableNoInterlacingPatches", false));
        }

        String loadTextures = safeGetSetting("EmuCore/GS", "LoadTextureReplacements", "bool");
        if (loadTextures == null) {
            loadTextures = boolToString(readBoolSetting("EmuCore/GS", "LoadTextureReplacements", false));
        }

        String asyncTextures = safeGetSetting("EmuCore/GS", "LoadTextureReplacementsAsync", "bool");
        if (asyncTextures == null) {
            asyncTextures = boolToString(readBoolSetting("EmuCore/GS", "LoadTextureReplacementsAsync", false));
        }

        String precache = safeGetSetting("EmuCore/GS", "PrecacheTextureReplacements", "bool");
        if (precache == null) {
            precache = boolToString(readBoolSetting("EmuCore/GS", "PrecacheTextureReplacements", false));
        }

        String showFps = safeGetSetting("EmuCore/GS", "OsdShowFPS", "bool");
        if (showFps == null) {
            showFps = boolToString(readBoolSetting("EmuCore/GS", "OsdShowFPS", false));
        }

        String renderer = safeGetSetting("EmuCore/GS", "Renderer", "int");
        if (renderer == null) {
            renderer = Integer.toString(EmulatorSettingsUtils.getCurrentRendererValue(ctx));
        }

        String aspect = safeGetSetting("EmuCore/GS", "AspectRatio", "string");
        if (aspect == null) {
            aspect = EmulatorSettingsUtils.getCurrentAspectRatioValue(ctx);
        }

        return new PerGameOverrideSnapshot(cheats, widescreen, noInterlacing, loadTextures, asyncTextures, precache, showFps, renderer, aspect);
    }

    static void setNativeSetting(String section, String key, String type, @Nullable String value) {
        if (value == null) {
            return;
        }
        try {
            NativeApp.setSetting(section, key, type, value);
        } catch (Exception ignored) {
        }
    }
}
