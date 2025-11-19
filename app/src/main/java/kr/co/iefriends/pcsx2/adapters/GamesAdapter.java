package kr.co.iefriends.pcsx2.adapters;

import static kr.co.iefriends.pcsx2.core.util.CoversUtils.clearLocalCoverCache;
import static kr.co.iefriends.pcsx2.core.util.SettingsUtils.getManualCoverUri;

import android.content.Context;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import kr.co.iefriends.pcsx2.R;
import kr.co.iefriends.pcsx2.activities.MainActivity;
import kr.co.iefriends.pcsx2.core.util.CoversUtils;
import kr.co.iefriends.pcsx2.core.util.NetworkUtils;
import kr.co.iefriends.pcsx2.data.model.GameEntry;

// Recycler adapter
public class GamesAdapter extends RecyclerView.Adapter<GamesAdapter.VH> {
    interface OnClick {
        void onClick(GameEntry e);

        String getCoversUrlTemplate();

        String getManualCoverUri(String gameKey);

        void showGameOptionsDialog(GameEntry e);
    }

    public static class VH extends RecyclerView.ViewHolder {
        final TextView tv;
        final android.widget.ImageView img;
        final TextView tvOverlay;

        VH(View v) {
            super(v);
            this.tv = v.findViewById(R.id.tv_title);
            this.img = v.findViewById(R.id.img_cover);
            this.tvOverlay = v.findViewById(R.id.tv_cover_fallback);
        }
    }

    private final List<GameEntry> data;
    private final List<GameEntry> filtered = new ArrayList<>();
    private final OnClick onClick;
    private boolean listMode = false;
    // Lightweight in-memory cache for cover bitmaps
    private static final android.util.LruCache<String, android.graphics.Bitmap> sCoverCache;
    private static final java.util.Set<String> sNegativeCache = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
    private static final java.util.concurrent.ExecutorService sExec = java.util.concurrent.Executors.newFixedThreadPool(3);

    static {
        int maxMem = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = Math.max(1024 * 8, Math.min(1024 * 64, maxMem / 16));
        sCoverCache = new android.util.LruCache<String, android.graphics.Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, android.graphics.Bitmap value) {
                return value.getByteCount() / 1024;
            }
        };
    }

    public GamesAdapter(List<GameEntry> d, OnClick oc) {
        data = d;
        filtered.addAll(d);
        onClick = oc;
        setHasStableIds(true);
    }

    public void update(List<GameEntry> d) {
        clearLocalCoverCache();
        data.clear();
        data.addAll(d);
        applyFilter(currentFilter);
    }

    int getItemCountTotal() {
        return data.size();
    }

    private String currentFilter = "";

    public void setFilter(String q) {
        currentFilter = q == null ? "" : q.trim();
        applyFilter(currentFilter);
    }

    private void applyFilter(String q) {
        filtered.clear();
        if (TextUtils.isEmpty(q)) {
            filtered.addAll(data);
        } else {
            String needle = q.toLowerCase();
            for (GameEntry e : data) {
                String t = e != null && e.title != null ? e.title.toLowerCase() : "";
                String s = e != null && e.serial != null ? e.serial.toLowerCase() : "";
                if (t.contains(needle) || s.contains(needle)) filtered.add(e);
            }
        }
        notifyDataSetChanged();
    }

    public void setListMode(boolean list) {
        this.listMode = list;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return listMode ? 1 : 0;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = (viewType == 1) ? R.layout.item_game_list : R.layout.item_game;
        View v = getLayoutInflater(parent).inflate(layout, parent, false);
        return new VH(v);
    }

    @Override
    public long getItemId(int position) {
        try {
            GameEntry e = data.get(position);
            String key = (e.uri != null ? e.uri.toString() : e.title) + "|" + (e.title != null ? e.title : "");
            return (long) key.hashCode();
        } catch (Throwable ignored) {
            return position;
        }
    }

    @Override
    public void onViewRecycled(@NonNull VH holder) {
        super.onViewRecycled(holder);
        try {
            holder.img.setTag(R.id.tag_request_key, null);
            holder.img.setImageDrawable(null);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        GameEntry e = filtered.get(position);
        String tpl = null;
        if (onClick != null) {
            tpl = onClick.getCoversUrlTemplate();
        } else {
            // Aggiungere un log qui aiuterebbe in futuro, ma procediamo per non rompere il codice
        }
        boolean loaded = false;
        try {
            holder.img.setImageDrawable(null);
        } catch (Throwable ignored) {
        }
        try {
            holder.img.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        } catch (Throwable ignored) {
        }
        if (holder.tvOverlay != null) holder.tvOverlay.setVisibility(View.GONE);
        try {
            String gameKey = CoversUtils.gameKeyFromEntry(e);
            String manual = getManualCoverUri(holder.itemView.getContext(), gameKey);
            if (manual != null && !manual.isEmpty()) {
                android.net.Uri mu = android.net.Uri.parse(manual);
                try (java.io.InputStream is = holder.itemView.getContext().getContentResolver().openInputStream(mu)) {
                    if (is != null) {
                        android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(is);
                        if (bmp != null) {
                            holder.img.setImageBitmap(bmp);
                            loaded = true;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        if (!loaded) {
            File cachedLocal = CoversUtils.findCachedCoverFile(holder.itemView.getContext(), e);
            if (cachedLocal != null && cachedLocal.exists()) {
                String localKey = cachedLocal.getAbsolutePath();
                android.graphics.Bitmap cachedBmp = sCoverCache.get(localKey);
                if (cachedBmp != null) {
                    holder.img.setImageBitmap(cachedBmp);
                    loaded = true;
                } else {
                    android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeFile(localKey);
                    if (bmp != null) {
                        holder.img.setImageBitmap(bmp);
                        sCoverCache.put(localKey, bmp);
                        loaded = true;
                    }
                }
            }
        }
        boolean online = NetworkUtils.hasInternetConnection(holder.itemView.getContext());
        if (!loaded && online && tpl != null && !tpl.isEmpty()) {
            List<String> urls = CoversUtils.buildCoverCandidateUrls(e, tpl);
            String requestKey = (e.uri != null ? e.uri.toString() : e.title) + "|" + (e.serial != null ? e.serial : "") + "|" + (e.title != null ? e.title : "");
            holder.img.setTag(R.id.tag_request_key, requestKey);
            for (String u : urls) {
                if (u == null || u.isEmpty() || u.contains("${")) continue;
                android.graphics.Bitmap cached = sCoverCache.get(u);
                if (cached != null) {
                    loaded = true;
                    holder.img.setImageBitmap(cached);
                    break;
                }
            }
            if (!loaded && !urls.isEmpty())
                loadImageWithFallback(holder.img, holder.tvOverlay, holder.itemView.getContext(), e, urls, requestKey);
        }
        holder.img.setVisibility(View.VISIBLE);
        if (listMode) {
            holder.tv.setVisibility(View.VISIBLE);
            holder.tv.setText(e.gameTitle != null ? e.gameTitle : e.title);
            if (holder.tvOverlay != null) holder.tvOverlay.setVisibility(View.GONE);
        } else {
            if (loaded) {
                if (holder.tvOverlay != null) holder.tvOverlay.setVisibility(View.GONE);
                holder.tv.setVisibility(View.GONE);
            } else {
                holder.tv.setVisibility(View.GONE);
                if (holder.tvOverlay != null) {
                    holder.tvOverlay.setText(e.gameTitle != null ? e.gameTitle : e.title);
                    holder.tvOverlay.setVisibility(View.VISIBLE);
                    holder.tvOverlay.bringToFront();
                }
            }
        }
        if (onClick != null) {
            holder.itemView.setOnClickListener(v -> onClick.onClick(e));
            holder.itemView.setOnLongClickListener(v -> {
                try {
                    onClick.showGameOptionsDialog(e);
                } catch (Throwable ignored) {
                }
                return true;
            });
        } else {
            holder.itemView.setOnClickListener(null);
            holder.itemView.setOnLongClickListener(null);
        }
        holder.itemView.setOnClickListener(v -> onClick.onClick(e));
        holder.itemView.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            RecyclerView rv = (RecyclerView) holder.itemView.getParent();
            RecyclerView.LayoutManager lm = rv.getLayoutManager();
            if (!(lm instanceof GridLayoutManager)) return false;
            int span = ((GridLayoutManager) lm).getSpanCount();
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return false;
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (pos % span > 0) {
                        int target = pos - 1;
                        rv.smoothScrollToPosition(target);
                        rv.post(() -> {
                            RecyclerView.ViewHolder tvh = rv.findViewHolderForAdapterPosition(target);
                            if (tvh != null) tvh.itemView.requestFocus();
                        });
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (pos + 1 < getItemCount()) {
                        int target = pos + 1;
                        rv.smoothScrollToPosition(target);
                        rv.post(() -> {
                            RecyclerView.ViewHolder tvh = rv.findViewHolderForAdapterPosition(target);
                            if (tvh != null) tvh.itemView.requestFocus();
                        });
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (pos - span >= 0) {
                        int target = pos - span;
                        rv.smoothScrollToPosition(target);
                        rv.post(() -> {
                            RecyclerView.ViewHolder tvh = rv.findViewHolderForAdapterPosition(target);
                            if (tvh != null) tvh.itemView.requestFocus();
                        });
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (pos + span < getItemCount()) {
                        int target = pos + span;
                        rv.smoothScrollToPosition(target);
                        rv.post(() -> {
                            RecyclerView.ViewHolder tvh = rv.findViewHolderForAdapterPosition(target);
                            if (tvh != null) tvh.itemView.requestFocus();
                        });
                    }
                    return true;
                case KeyEvent.KEYCODE_BUTTON_A:
                case KeyEvent.KEYCODE_BUTTON_START:
                case KeyEvent.KEYCODE_ENTER:
                    v.performClick();
                    return true;
            }
            return false;
        });
        //holder.itemView.setOnLongClickListener(v -> {
            //try {
                //onClick.showGameOptionsDialog(e);
            //} catch (Throwable ignored) {
            //}
            //return true;
        //});
    }

    @Override
    public int getItemCount() {
        return filtered.size();
    }

    private static android.view.LayoutInflater getLayoutInflater(ViewGroup parent) {
        return android.view.LayoutInflater.from(parent.getContext());
    }

    private void loadImageWithFallback(android.widget.ImageView iv, TextView overlayView, Context ctx, GameEntry entry, List<String> urls, String requestKey) {
        try {
            sExec.execute(() -> {
                try {
                    android.graphics.Bitmap bmp = null;
                    String hitUrl = null;
                    byte[] downloadedBytes = null;
                    String downloadExtension = null;
                    for (String ustr : urls) {
                        if (ustr == null || ustr.isEmpty() || ustr.contains("${")) continue;
                        Object tag = iv.getTag(R.id.tag_request_key);
                        if (!(requestKey.equals(tag))) {
                            break;
                        }
                        if (sNegativeCache.contains(ustr)) continue;
                        android.graphics.Bitmap cached = sCoverCache.get(ustr);
                        if (cached != null) {
                            bmp = cached;
                            hitUrl = ustr;
                            break;
                        }
                        try {
                            java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(ustr).openConnection();
                            c.setConnectTimeout(4000);
                            c.setReadTimeout(6000);
                            c.setInstanceFollowRedirects(true);
                            c.setRequestMethod("GET");
                            int code = c.getResponseCode();
                            if (code == 200) {
                                try (java.io.InputStream is = c.getInputStream();
                                     java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
                                    byte[] buffer = new byte[8192];
                                    int read;
                                    while ((read = is.read(buffer)) != -1) {
                                        baos.write(buffer, 0, read);
                                    }
                                    byte[] data = baos.toByteArray();
                                    if (data.length > 0) {
                                        android.graphics.Bitmap candidate = android.graphics.BitmapFactory.decodeByteArray(data, 0, data.length);
                                        if (candidate != null) {
                                            bmp = candidate;
                                            downloadedBytes = data;
                                            downloadExtension = CoversUtils.guessImageExtension(ustr, c.getContentType());
                                            hitUrl = ustr;
                                            break;
                                        }
                                    }
                                }
                            } else if (code == 404) {
                                sNegativeCache.add(ustr);
                                continue;
                            } else {
                                try {
                                    kr.co.iefriends.pcsx2.core.util.DebugLog.d("Covers", "HTTP " + code + " for " + ustr);
                                } catch (Throwable ignored) {
                                }
                            }
                        } catch (Exception ex) {
                            try {
                                kr.co.iefriends.pcsx2.core.util.DebugLog.d("Covers", "Error loading cover: " + ex.getMessage());
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                    if (downloadedBytes != null && downloadedBytes.length > 0 && entry != null && ctx != null) {
                        try {
                            CoversUtils.storeCoverBytes(ctx, entry, downloadedBytes, downloadExtension);
                        } catch (Throwable ignored) {
                        }
                    }
                    final android.graphics.Bitmap fb = bmp;
                    final String fUrl = hitUrl;
                    iv.post(() -> {
                        Object tagNow = iv.getTag(R.id.tag_request_key);
                        if (requestKey.equals(tagNow) && fb != null) {
                            iv.setImageBitmap(fb);
                            if (fUrl != null) sCoverCache.put(fUrl, fb);
                            if (overlayView != null) overlayView.setVisibility(View.GONE);
                        }
                    });
                } catch (Throwable ignored) {
                }
            });
        } catch (Throwable ignored) {
        }
    }

}
