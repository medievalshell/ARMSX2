package kr.co.iefriends.pcsx2.core.util;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import kr.co.iefriends.pcsx2.R;
import kr.co.iefriends.pcsx2.adapters.GameScanner;
import kr.co.iefriends.pcsx2.data.model.GameEntry;
import kr.co.iefriends.pcsx2.viewmodels.GameListViewModel;

public class CoversUtils {
    private static final java.util.Map<String, File> sLocalCoverFiles = java.util.Collections.synchronizedMap(new java.util.HashMap<>());
    private static final java.util.Set<String> sLocalCoverMissing = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
    private static final Object coverPrefetchLock = new Object();
    private static volatile boolean coverPrefetchRunning;
    public static File getCoversCacheDir(Context ctx) {
        if (ctx == null) {
            return null;
        }
        Context appCtx = ctx.getApplicationContext();
        if (appCtx == null) {
            appCtx = ctx;
        }
        File base = kr.co.iefriends.pcsx2.core.util.DataDirectoryManager.getDataRoot(appCtx);
        if (base == null) {
            return null;
        }
        File dir = new File(base, "armsx2_covers");
        if (!dir.exists() && !dir.mkdirs()) {
            try { kr.co.iefriends.pcsx2.core.util.DebugLog.e("Covers", "Failed to create cover cache directory: " + dir); } catch (Throwable ignored) {}
            return null;
        }
        return dir;
    }

    public static File findExistingCoverFile(File dir, String baseName) {
        if (dir == null || TextUtils.isEmpty(baseName)) {
            return null;
        }
        String prefix = baseName.toLowerCase(Locale.US);
        File[] files = dir.listFiles();
        if (files == null) {
            return null;
        }
        for (File child : files) {
            if (child == null || !child.isFile()) continue;
            String name = child.getName();
            if (name == null) continue;
            String lower = name.toLowerCase(Locale.US);
            if (lower.equals(prefix) || lower.startsWith(prefix + ".")) {
                return child;
            }
        }
        return null;
    }

    public static void clearLocalCoverCache() {
        sLocalCoverFiles.clear();
        sLocalCoverMissing.clear();
    }

    private static String coverKey(GameEntry entry) {
        if (entry == null) {
            return null;
        }
        if (entry.uri != null) {
            return entry.uri.toString();
        }
        String fallback = entry.title;
        if (TextUtils.isEmpty(fallback)) {
            fallback = entry.fileTitleNoExt();
        }
        return fallback;
    }

    public static void registerCachedCover(GameEntry entry, File file) {
        if (entry == null || file == null || !file.exists()) {
            return;
        }
        String key = coverKey(entry);
        if (TextUtils.isEmpty(key)) {
            return;
        }
        sLocalCoverFiles.put(key, file);
        sLocalCoverMissing.remove(key);
    }

    public static File findCachedCoverFile(Context ctx, GameEntry entry) {
        if (ctx == null || entry == null || entry.uri == null) {
            return null;
        }
        String key = coverKey(entry);
        if (TextUtils.isEmpty(key)) {
            return null;
        }
        File cached = sLocalCoverFiles.get(key);
        if (cached != null && cached.exists()) {
            return cached;
        }
        if (sLocalCoverMissing.contains(key)) {
            return null;
        }
        File cacheDir = CoversUtils.getCoversCacheDir(ctx);
        if (cacheDir == null) {
            sLocalCoverMissing.add(key);
            return null;
        }
        String baseName = computeCoverBaseName(entry);
        File coverFile = CoversUtils.findExistingCoverFile(cacheDir, baseName);
        if (coverFile != null && coverFile.isFile() && coverFile.length() > 0) {
            sLocalCoverFiles.put(key, coverFile);
            sLocalCoverMissing.remove(key);
            return coverFile;
        }
        sLocalCoverMissing.add(key);
        return null;
    }

    public static void storeCoverBytes(Context ctx, GameEntry entry, byte[] data, String extension) {
        if (ctx == null || entry == null || data == null || data.length == 0) {
            return;
        }
        File cacheDir = CoversUtils.getCoversCacheDir(ctx);
        if (cacheDir == null) {
            return;
        }
        String baseName = computeCoverBaseName(entry);
        if (TextUtils.isEmpty(baseName)) {
            return;
        }
        String ext = extension;
        if (TextUtils.isEmpty(ext)) {
            ext = ".jpg";
        }
        if (!ext.startsWith(".")) {
            ext = "." + ext;
        }
        File target = new File(cacheDir, baseName + ext);
        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            return;
        }
        File temp = new File(cacheDir, baseName + "_tmp" + ext);
        try (FileOutputStream fos = new FileOutputStream(temp)) {
            fos.write(data);
            fos.flush();
        } catch (IOException ignored) {
            temp.delete();
            return;
        }
        if (!temp.renameTo(target)) {
            temp.delete();
            return;
        }
        CoversUtils.registerCachedCover(entry, target);
        try { kr.co.iefriends.pcsx2.core.util.DebugLog.d("Covers", "Stored cover cache file: " + target.getAbsolutePath()); } catch (Throwable ignored) {}
    }

    private static String safeUrlPart(String s) {
        if (s == null) {
            return "";
        }
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    private static String hyphenizeAlphaDigits(String s) {
        if (s == null) {
            return "";
        }
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("^([A-Za-z]+)[-_]?([0-9]{3,})$").matcher(s);
            if (m.find()) {
                return (m.group(1).toUpperCase(Locale.US) + "-" + m.group(2));
            }
        } catch (Exception ignored) {}
        return s;
    }

    private static List<String> makeTitleVariants(String base) {
        java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>();
        if (base == null) base = "";
        String b0 = base.trim();
        if (!b0.isEmpty()) set.add(b0);
        String b1 = b0.replace('_', ' ').trim(); if (!b1.isEmpty()) set.add(b1);
        String b2 = b1.replace(":", " - ").replaceAll("\\s+", " ").trim(); if (!b2.isEmpty()) set.add(b2);
        try {
            String b3 = b1.replaceAll("(?i)(?<=\\w) \\–|\\u2014| - (?=\\w)", ": ");
            b3 = b3.replace(" - ", ": ");
            b3 = b3.replaceAll("\\s+", " ").trim();
            if (!b3.isEmpty()) set.add(b3);
        } catch (Throwable ignored) {}
        return new ArrayList<>(set);
    }

    public static List<String> buildCoverCandidateUrls(GameEntry entry, String template) {
        if (entry == null || TextUtils.isEmpty(template)) {
            return Collections.emptyList();
        }
        String fileBase = entry.fileTitleNoExt();
        String hyphenized = hyphenizeAlphaDigits(fileBase);
        List<String> variants = makeTitleVariants(fileBase);
        java.util.LinkedHashSet<String> urls = new java.util.LinkedHashSet<>();
        if (template.contains("${filetitle}")) {
            for (String v : variants) {
                urls.add(template.replace("${filetitle}", safeUrlPart(v))
                        .replace("${serial}", "")
                        .replace("${title}", ""));
            }
        }
        if (template.contains("${serial}")) {
            if (!TextUtils.isEmpty(entry.serial)) {
                urls.add(template.replace("${serial}", safeUrlPart(entry.serial))
                        .replace("${filetitle}", "")
                        .replace("${title}", ""));
            }
            if (!TextUtils.isEmpty(hyphenized) && !hyphenized.equals(fileBase)) {
                urls.add(template.replace("${serial}", safeUrlPart(hyphenized))
                        .replace("${filetitle}", "")
                        .replace("${title}", ""));
            }
            for (String v : variants) {
                urls.add(template.replace("${serial}", safeUrlPart(v))
                        .replace("${filetitle}", "")
                        .replace("${title}", ""));
            }
        }
        if (template.contains("${title}")) {
            String resolvedTitle = !TextUtils.isEmpty(entry.gameTitle) ? entry.gameTitle : fileBase;
            java.util.LinkedHashSet<String> titleVariants = new java.util.LinkedHashSet<>(makeTitleVariants(resolvedTitle));
            if (!TextUtils.isEmpty(entry.gameTitle) && !TextUtils.isEmpty(fileBase) && !entry.gameTitle.equals(fileBase)) {
                titleVariants.addAll(makeTitleVariants(fileBase));
            }
            for (String v : titleVariants) {
                urls.add(template.replace("${title}", safeUrlPart(v))
                        .replace("${serial}", "")
                        .replace("${filetitle}", ""));
            }
        }
        return new ArrayList<>(urls);
    }

    private static String sanitizeCoverFileComponent(String input) {
        if (TextUtils.isEmpty(input)) {
            return "";
        }
        String normalized = input.trim();
        normalized = normalized.replaceAll("[\\\\/:*?\"<>|]", " ");
        normalized = normalized.replaceAll("[^A-Za-z0-9._-]", "_");
        normalized = normalized.replaceAll("_+", "_");
        normalized = normalized.replaceAll("^_+|_+$", "");
        return normalized;
    }

    public static String computeCoverBaseName(GameEntry entry) {
        String candidate = entry != null ? entry.serial : null;
        if (TextUtils.isEmpty(candidate) && entry != null) {
            candidate = entry.gameTitle;
        }
        if (TextUtils.isEmpty(candidate) && entry != null) {
            candidate = entry.fileTitleNoExt();
        }
        String sanitized = sanitizeCoverFileComponent(candidate);
        if (TextUtils.isEmpty(sanitized) && entry != null) {
            String fallback = entry.title != null ? entry.title : "cover";
            sanitized = sanitizeCoverFileComponent("cover_" + Integer.toHexString(fallback.hashCode()));
        }
        if (TextUtils.isEmpty(sanitized)) {
            sanitized = "cover";
        }
        return sanitized;
    }

    public static String guessImageExtension(String url, String contentType) {
        if (contentType != null) {
            String type = contentType.toLowerCase(Locale.US);
            if (type.contains("png")) return ".png";
            if (type.contains("webp")) return ".webp";
            if (type.contains("gif")) return ".gif";
            if (type.contains("jpeg") || type.contains("jpg")) return ".jpg";
        }
        if (url != null) {
            String path = url;
            int query = path.indexOf('?');
            if (query >= 0) {
                path = path.substring(0, query);
            }
            int dot = path.lastIndexOf('.');
            if (dot >= 0 && dot > path.lastIndexOf('/')) {
                String ext = path.substring(dot).toLowerCase(Locale.US);
                if (ext.matches("\\.(jpg|jpeg|png|webp|gif)")) {
                    return ext.equals(".jpeg") ? ".jpg" : ext;
                }
            }
        }
        return ".jpg";
    }
    public static String gameKeyFromEntry(GameEntry e) {
        if (e == null) return "";
        String key = (e.uri != null ? e.uri.toString() : ("file://" + e.title));
        return key;
    }

    private static boolean downloadCoverToDirectory(File coversDir, String url, String baseName) {
        HttpURLConnection connection = null;
        InputStream in = null;
        OutputStream out = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(4000);
            connection.setReadTimeout(6000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("GET");
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                return false;
            }
            String extension = CoversUtils.guessImageExtension(url, connection.getContentType());
            String fileName = baseName + extension;
            File existing = CoversUtils.findExistingCoverFile(coversDir, baseName);
            if (existing != null && existing.length() > 0) {
                return false;
            }
            if (existing != null) {
                if (!existing.delete()) {
                    return false;
                }
            }
            File file = new File(coversDir, fileName);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                return false;
            }
            out = new FileOutputStream(file);
            in = connection.getInputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
            return true;
        } catch (Exception ignored) {
            return false;
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException ignored) {}
            }
            if (out != null) {
                try { out.close(); } catch (IOException ignored) {}
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static boolean ensureCoverCachedForEntry(Context ctx, File coversDir, GameEntry entry, String template, Set<String> attemptedUrls) {
        List<String> urls = CoversUtils.buildCoverCandidateUrls(entry, template);
        File cacheDir = CoversUtils.getCoversCacheDir(ctx);
        if (cacheDir == null) {
            return false;
        }
        if (urls.isEmpty()) {
            return false;
        }
        String baseName = CoversUtils.computeCoverBaseName(entry);
        if (TextUtils.isEmpty(baseName)) {
            return false;
        }
        File existing = findExistingCoverFile(coversDir, baseName);
        if (existing != null && existing.length() > 0) {
            return false;
        }
        for (String url : urls) {
            if (TextUtils.isEmpty(url) || url.contains("${")) {
                continue;
            }
            if (attemptedUrls != null && !attemptedUrls.add(url)) {
                continue;
            }
            if (downloadCoverToDirectory(coversDir, url, baseName)) {
                File coverFile = CoversUtils.findExistingCoverFile(cacheDir, baseName);
                if (coverFile != null && coverFile.isFile()) {
                    CoversUtils.registerCachedCover(entry, coverFile);
                }
                try { kr.co.iefriends.pcsx2.core.util.DebugLog.d("Covers", "Cached cover for " + baseName + " from " + url); } catch (Throwable ignored) {}
                return true;
            }
        }
        return false;
    }

    public static LinkedHashSet<Uri> collectGameRootUris(Context ctx, @Nullable Uri currentGamesFolderUri) {
        LinkedHashSet<Uri> roots = new LinkedHashSet<>();
        if (currentGamesFolderUri != null) {
            roots.add(currentGamesFolderUri);
        }
        final String PREFS = "armsx2";
        final String PREF_GAMES_URI = "games_folder_uri";
        final String PREF_SECONDARY_DIRS = "secondary_game_dirs";
        android.content.SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String savedPrimary = prefs.getString(PREF_GAMES_URI, null);
        if (currentGamesFolderUri == null && !TextUtils.isEmpty(savedPrimary)) {
            try { roots.add(Uri.parse(savedPrimary)); } catch (Exception ignored) {}
        }
        java.util.Set<String> secondary = prefs.getStringSet(PREF_SECONDARY_DIRS, null);
        if (secondary != null) {
            for (String uriString : secondary) {
                if (TextUtils.isEmpty(uriString)) continue;
                try { roots.add(Uri.parse(uriString)); } catch (Exception ignored) {}
            }
        }
        return roots;
    }

    private static int prefetchCoversForRoot(Context ctx, Uri root, String template, File cacheDir) {
        if (root == null) {
            return 0;
        }
        if (cacheDir == null) {
            return 0;
        }
        List<GameEntry> entries = GameScanner.scanFolder(ctx, root);
        if (entries == null || entries.isEmpty()) {
            return 0;
        }
        GameListViewModel.resolveMetadataForEntries(ctx.getContentResolver(), entries);
        int downloaded = 0;
        Set<String> attempted = new HashSet<>();
        for (GameEntry entry : entries) {
            if (ensureCoverCachedForEntry(ctx, cacheDir, entry, template, attempted)) {
                downloaded++;
            }
        }
        return downloaded;
    }

    public static void prefetchCoversAsync(Context ctx, String template, @Nullable Uri currentGamesFolderUri) {
        final Context context = ctx;
        if (TextUtils.isEmpty(template)) {
            return;
        }
        LinkedHashSet<Uri> roots = CoversUtils.collectGameRootUris(ctx, currentGamesFolderUri);
        CoversUtils.clearLocalCoverCache();
        File cacheDir = CoversUtils.getCoversCacheDir(ctx);
        if (cacheDir == null) {
            try { Toast.makeText(ctx, R.string.cover_prefetch_none, Toast.LENGTH_SHORT).show(); } catch (Throwable ignored) {}
            return;
        }
        if (roots.isEmpty()) {
            try { Toast.makeText(ctx, R.string.cover_prefetch_none, Toast.LENGTH_SHORT).show(); } catch (Throwable ignored) {}
            return;
        }
        if (!NetworkUtils.hasInternetConnection(ctx)) {
            try { Toast.makeText(ctx, R.string.cover_prefetch_no_connection, Toast.LENGTH_SHORT).show(); } catch (Throwable ignored) {}
            return;
        }
        synchronized (coverPrefetchLock) {
            if (coverPrefetchRunning) {
                try { Toast.makeText(ctx, R.string.cover_prefetch_running, Toast.LENGTH_SHORT).show(); } catch (Throwable ignored) {}
                return;
            }
            coverPrefetchRunning = true;
        }
        try { Toast.makeText(ctx, R.string.cover_prefetch_start, Toast.LENGTH_SHORT).show(); } catch (Throwable ignored) {}
        new Thread(() -> {
            int downloaded = 0;
            try {
                for (Uri root : roots) {
                    downloaded += prefetchCoversForRoot(context,root, template, cacheDir);
                }
            } finally {
                synchronized (coverPrefetchLock) {
                    coverPrefetchRunning = false;
                }
            }
            final int total = downloaded;
            ((Activity) ctx).runOnUiThread(() -> {
                try {
                    if (total > 0) {
                        Toast.makeText(ctx, ctx.getString(R.string.cover_prefetch_done, total), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ctx, R.string.cover_prefetch_none, Toast.LENGTH_SHORT).show();
                    }
                } catch (Throwable ignored) {}
            });
        }, "CoverPrefetch").start();
    }
}
