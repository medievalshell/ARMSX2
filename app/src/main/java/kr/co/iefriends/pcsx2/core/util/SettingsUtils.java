package kr.co.iefriends.pcsx2.core.util;

import android.content.Context;

public class SettingsUtils {
    private static final String PREFS = "PCSX2";
    private static final String PREF_MANUAL_COVER_PREFIX = "manual_cover_";
    public static void setManualCoverUri(Context ctx, String gameKey, String uri) {
        try {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(PREF_MANUAL_COVER_PREFIX + gameKey, uri)
                    .apply();
        } catch (Throwable ignored) {}
    }
    public static void removeManualCoverUri(Context ctx, String gameKey) {
        try {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .remove(PREF_MANUAL_COVER_PREFIX + gameKey)
                    .apply();
        } catch (Throwable ignored) {}
    }
    public static String getManualCoverUri(Context ctx, String gameKey) {
        try {
            return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getString(PREF_MANUAL_COVER_PREFIX + gameKey, null);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
