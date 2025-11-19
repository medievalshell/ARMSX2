package kr.co.iefriends.pcsx2.activities;

import static kr.co.iefriends.pcsx2.core.input.InputManager.clamp01;
import static kr.co.iefriends.pcsx2.core.input.InputManager.requestControllerRumble;
import static kr.co.iefriends.pcsx2.core.input.InputManager.sVibrationEnabled;
import static kr.co.iefriends.pcsx2.core.input.InputManager.setVibrationPreference;
import static kr.co.iefriends.pcsx2.core.input.InputManager.updateLastControllerDeviceId;
import static kr.co.iefriends.pcsx2.core.util.EmulatorSettingsUtils.readBoolSetting;
import static kr.co.iefriends.pcsx2.core.util.GameSettingsApplier.restorePerGameOverrides;

import kr.co.iefriends.pcsx2.adapters.GameScanner;
import kr.co.iefriends.pcsx2.adapters.GamesAdapter;
import kr.co.iefriends.pcsx2.core.util.CoversUtils;
import kr.co.iefriends.pcsx2.core.util.GameSettingsApplier;
import kr.co.iefriends.pcsx2.core.util.SettingsUtils;
import kr.co.iefriends.pcsx2.data.model.GameEntry;
import kr.co.iefriends.pcsx2.core.util.EmulatorSettingsUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.net.Uri;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.util.SparseIntArray;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.app.GameManager;
import android.app.GameState;
import android.os.Build;
import android.provider.Settings;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.CompoundButton;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.os.SystemClock;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import kr.co.iefriends.pcsx2.BuildConfig;
import kr.co.iefriends.pcsx2.data.repositories.RedumpDB;
import kr.co.iefriends.pcsx2.core.input.ControllerMappingDialog;
import kr.co.iefriends.pcsx2.core.input.view.DPadView;
import kr.co.iefriends.pcsx2.core.util.DataDirectoryManager;
import kr.co.iefriends.pcsx2.core.util.DiscordBridge;
import kr.co.iefriends.pcsx2.core.util.GameSpecificSettingsManager;
import kr.co.iefriends.pcsx2.hid.HIDDeviceManager;
import kr.co.iefriends.pcsx2.core.input.view.JoystickView;
import kr.co.iefriends.pcsx2.core.util.LogcatRecorder;
import kr.co.iefriends.pcsx2.NativeApp;
import kr.co.iefriends.pcsx2.core.input.view.PSButtonView;
import kr.co.iefriends.pcsx2.core.input.view.PSShoulderButtonView;
import kr.co.iefriends.pcsx2.R;
import kr.co.iefriends.pcsx2.core.util.RetroAchievementsBridge;
import kr.co.iefriends.pcsx2.core.util.SDLControllerManager;
import kr.co.iefriends.pcsx2.core.util.SDLSurface;
import kr.co.iefriends.pcsx2.core.util.DebugLog;
import kr.co.iefriends.pcsx2.core.util.DeviceProfiles;
import kr.co.iefriends.pcsx2.core.input.ControllerMappingManager;
import kr.co.iefriends.pcsx2.viewmodels.GameListViewModel;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    private String m_szGamefile = "";

    private HIDDeviceManager mHIDDeviceManager;
    private Thread mEmulationThread = null;
    private static final int RUMBLE_DURATION_MS = 160;
    public static volatile int sLastControllerDeviceId = -1;

    // UI groups for on-screen controls
    private View llPadSelectStart;
    private View llPadRight;
    private DrawerLayout inGameDrawer;
    private FloatingActionButton drawerToggle;
    private FloatingActionButton drawerPauseButton;
    private FloatingActionButton drawerFastForwardButton;
    private MaterialSwitch drawerWidescreenSwitch;
    private View drawerRaSection;
    private TextView drawerRaTitle;
    private TextView drawerRaSubtitle;
    private android.widget.ImageView drawerRaIcon;
    private TextView drawerRaLabel;
    private RetroAchievementsBridge.State currentRetroAchievementsState;
    private boolean lastRetroAchievementsLoggedIn = false;
    private int lastRetroAchievementsGameId = -1;
    private String lastRetroAchievementsIconPath = "";
    private boolean isVmPaused = false;
    private final Runnable hideDrawerToggleRunnable = () -> hideDrawerToggle();
    private boolean isFastForwardEnabled = false;
    private final CompoundButton.OnCheckedChangeListener drawerWidescreenListener =
            (buttonView, isChecked) ->
                    NativeApp.setSetting("EmuCore", "EnableWideScreenPatches", "bool", isChecked ? "true" : "false");
    boolean globalCheats = readBoolSetting("EmuCore", "EnableCheats", false);
    boolean globalWidescreen = readBoolSetting("EmuCore", "EnableWideScreenPatches", false);
    boolean globalNoInterlacing = readBoolSetting("EmuCore", "EnableNoInterlacingPatches", false);
    boolean globalLoadTextures = readBoolSetting("EmuCore/GS", "LoadTextureReplacements", false);
    boolean globalAsyncTextures = readBoolSetting("EmuCore/GS", "LoadTextureReplacementsAsync", false);
    boolean globalPrecache = readBoolSetting("EmuCore/GS", "PrecacheTextureReplacements", false);
    boolean globalShowFps = readBoolSetting("EmuCore/GS", "OsdShowFPS", false);

    // Home UI
    private DrawerLayout drawerLayout;
    private View homeContainer;
    private View emptyContainer;
    private android.widget.EditText etSearch;
    private android.widget.ImageView bgImage;
    private RecyclerView rvGames;
    private GridLayoutManager gamesGridLayoutManager;
    private SpacingDecoration gameSpacingDecoration;
    private TextView tvEmpty;
    private GamesAdapter gamesAdapter;
    private boolean listMode = false;
    private Uri gamesFolderUri;
    private boolean storagePromptShown = false;
    private String pendingChdCachePath;
    private String pendingChdDisplayName;
    private AlertDialog dataDirProgressDialog;
    private static final String PREFS = "armsx2";
    private static final String PREF_GAMES_URI = "games_folder_uri";
    private static final String PREF_ONBOARDING_COMPLETE = "onboarding_complete";
    private static final String PREF_ONSCREEN_UI_STYLE = "on_screen_ui_style";
    private static final String PREF_UI_SCALE_MULTIPLIER = "onscreen_ui_scale_multiplier";
    private static final String STYLE_DEFAULT = "default";
    public static final String STYLE_NETHER = "nether";
    private static final float ONSCREEN_UI_SCALE_MIN = 0.2f;
    private static final float ONSCREEN_UI_SCALE_MAX = 4.0f;
    // Preflight
    private Uri pendingGameUri = null;
    private boolean onboardingLaunched = false;
    private boolean postOnboardingChecksRun = false;
    public String currentOnScreenUiStyle = STYLE_DEFAULT;
    private float onScreenUiScaleMultiplier = 1.0f;
    private float faceButtonsBaseScale = 1.0f;



    // Auto-hide state
    public enum InputSource { TOUCH, CONTROLLER }
    public InputSource lastInput = InputSource.TOUCH;
    public long lastTouchTimeMs = 0L;
    private long lastControllerTimeMs = 0L;
    // 0 = never hide; seconds otherwise
    private long hideDelayMs = 2500L;
    private static final String PREF_HIDE_CONTROLS_SECONDS = "onscreen_timeout_seconds";




    private boolean disableTouchControls;
    
    private int currentControllerMode = 0; // 0=2 Sticks, 1=1 Stick+Face, 2=D-Pad Only
    private kr.co.iefriends.pcsx2.core.input.InputManager mInputManager;
    public kr.co.iefriends.pcsx2.core.input.InputManager getInputManager() {
        return mInputManager;
    }

    private final RetroAchievementsBridge.Listener retroAchievementsListener = new RetroAchievementsBridge.Listener() {
        @Override
        public void onStateUpdated(RetroAchievementsBridge.State state) {
            handleRetroAchievementsStateChanged(state);
        }

        @Override
        public void onLoginRequested(int reason) {
            // No in-drawer prompt; handled in settings flow.
        }

        @Override
        public void onLoginSuccess(String username, int points, int softPoints, int unreadMessages) {
            // State refresh will surface the appropriate toast.
        }

        @Override
        public void onHardcoreModeChanged(boolean enabled) {
            RetroAchievementsBridge.refreshState();
        }
    };

    private boolean isThread() {
        if (mEmulationThread != null) {
            Thread.State _thread_state = mEmulationThread.getState();
            return _thread_state == Thread.State.BLOCKED
                    || _thread_state == Thread.State.RUNNABLE
                    || _thread_state == Thread.State.TIMED_WAITING
                    || _thread_state == Thread.State.WAITING;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DiscordBridge.updateEngineActivity(this);
        setContentView(R.layout.activity_main);
        disableTouchControls = DeviceProfiles.isTvOrDesktop(this);
	// Keep screen awake during gameplay
	getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                GameManager gm = (GameManager) getSystemService(Context.GAME_SERVICE);
                if (gm != null) {
                    gm.setGameState(new GameState(false, GameState.MODE_GAMEPLAY_INTERRUPTIBLE));
                }
            } catch (Throwable ignored) {}
        }
        try {
            if (NativeApp.isFullscreenUIEnabled()) {
                setOnScreenControlsVisible(false);
            }
        } catch (Throwable ignored) {}
        // Hide title/action bar explicitly
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Force immersive fullscreen
        applyFullscreen();
        DataDirectoryManager.copyAssetAll(getApplicationContext(), "resources");
    Initialize();
    mInputManager = new kr.co.iefriends.pcsx2.core.input.InputManager(this);
    ControllerMappingManager.init(this);
    refreshVibrationPreference();
    // Load on-screen controls hide timeout
    loadHideTimeoutFromPrefs();
    loadOnScreenUiScalePreference();
    currentOnScreenUiStyle = resolveOnScreenUiStylePreference();
    if (!disableTouchControls) {
        mInputManager.makeButtonTouch();
    }
    setSurfaceView(new SDLSurface(this));

        maybeStartOnboardingFlow();
    // Cache on-screen pad containers
    llPadSelectStart = findViewById(R.id.ll_pad_select_start);
    llPadRight = findViewById(R.id.ll_pad_right);
    JoystickView joystickLeft = findViewById(R.id.joystick_left);
    DPadView dpadView = findViewById(R.id.dpad_view);
    setupInGameDrawer();
    setupTouchRevealOverlay();
    // Home UI
    drawerLayout = findViewById(R.id.drawer_root);
    homeContainer = findViewById(R.id.home_container);
    rvGames = findViewById(R.id.rv_games);
    emptyContainer = findViewById(R.id.empty_container);
    tvEmpty = findViewById(R.id.tv_empty);
    etSearch = findViewById(R.id.et_search);
    bgImage = findViewById(R.id.bg_image);
    if (rvGames != null) {
        gamesGridLayoutManager = new GridLayoutManager(this, getGameGridSpanCount());
        rvGames.setLayoutManager(gamesGridLayoutManager);
        gamesAdapter = new GamesAdapter(new ArrayList<>(), null);
        rvGames.setAdapter(gamesAdapter);
        // Controller navigation
    rvGames.setFocusable(true);
    rvGames.setFocusableInTouchMode(true);
        rvGames.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        gameSpacingDecoration = new SpacingDecoration(getResources().getDimensionPixelSize(R.dimen.game_selector_tile_spacing));
        rvGames.addItemDecoration(gameSpacingDecoration);
        rvGames.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && gamesAdapter.getItemCount() > 0) {
                rvGames.post(() -> {
                    RecyclerView.ViewHolder vh = rvGames.findViewHolderForAdapterPosition(0);
                    if (vh != null) vh.itemView.requestFocus();
                });
            }
        });
        applyGameGridConfig();
    }
        enforceTouchControlsPolicy();
        // Search text change -> filter
        if (etSearch != null) {
            etSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(android.text.Editable s) {
                    if (gamesAdapter != null) gamesAdapter.setFilter(s != null ? s.toString() : "");
                }
            });
        }
        // FAB actions: convert ISO to CHD 
        com.google.android.material.floatingactionbutton.FloatingActionButton fab = findViewById(R.id.fab_actions);
        if (fab != null) {
            fab.setOnClickListener(v -> {
                androidx.appcompat.widget.PopupMenu pm = new androidx.appcompat.widget.PopupMenu(this, v);
                pm.getMenuInflater().inflate(R.menu.menu_fab_actions, pm.getMenu());
                pm.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.menu_convert_iso_chd) {
                        startPickIsoForChd();
                        return true;
                    }
                    return false;
                });
                pm.show();
            });
        }
        MaterialButton btnChooseFolder = findViewById(R.id.btn_choose_folder);
        if (btnChooseFolder != null) btnChooseFolder.setOnClickListener(v -> pickGamesFolder());
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            String displayName = DeviceProfiles.getProductDisplayName(this, getString(R.string.app_name));
            toolbar.setTitle(displayName + " Game Selector");
            try {
                androidx.appcompat.graphics.drawable.DrawerArrowDrawable dd = new androidx.appcompat.graphics.drawable.DrawerArrowDrawable(this);
                dd.setProgress(0f); 
                toolbar.setNavigationIcon(dd);
            } catch (Throwable ignored) {}
            toolbar.setNavigationOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
            });
            try {
                toolbar.inflateMenu(R.menu.menu_toolbar_home);
                Menu menu = toolbar.getMenu();
                if (menu != null) {
                    MenuItem rnItem = menu.findItem(R.id.action_open_rn);
                    if (rnItem != null) {
                        rnItem.setVisible(BuildConfig.ENABLE_RN);
                        rnItem.setEnabled(BuildConfig.ENABLE_RN);
                    }
                }
                toolbar.setOnMenuItemClickListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.action_toggle_search) {
                        toggleSearchBar();
                        return true;
                    } else if (itemId == R.id.action_toggle_view) {
                        listMode = !listMode;
                        if (rvGames != null) {
                            if (listMode) {
                                rvGames.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
                                item.setIcon(R.drawable.ic_view_grid_24);
                            } else {
                                if (gamesGridLayoutManager == null) {
                                    gamesGridLayoutManager = new GridLayoutManager(this, getGameGridSpanCount());
                                }
                                gamesGridLayoutManager.setSpanCount(getGameGridSpanCount());
                                rvGames.setLayoutManager(gamesGridLayoutManager);
                                item.setIcon(R.drawable.ic_view_list_24);
                            }
                            if (gamesAdapter != null) gamesAdapter.setListMode(listMode);
                        }
                        return true;
                    } else if (itemId == R.id.action_open_rn) {
                        if (!BuildConfig.ENABLE_RN) {
                            return true;
                        }
                        try {
                            Class<?> rnClass = Class.forName("kr.co.iefriends.pcsx2.RNActivity");
                            startActivity(new Intent(this, rnClass));
                        } catch (Throwable t) {
                            try { Toast.makeText(this, "React Native screen unavailable", Toast.LENGTH_SHORT).show(); } catch (Throwable ignored) {}
                        }
                        return true;
                    }
                    return false;
                });
            } catch (Throwable ignored) {}
        }
    // Navigation drawer menus
        NavigationView navStart = findViewById(R.id.nav_view_start);
        NavigationView.OnNavigationItemSelectedListener listener = item -> {
            int id = item.getItemId();
            if (id == R.id.menu_boot_bios) {
                bootBios();
            } else if (id == R.id.menu_manage_bios) {
                showBiosManagerDialog();
            } else if (id == R.id.menu_open_settings) {
                Intent si = new Intent(this, SettingsActivity.class);
                startActivityForResult(si, 7722);
            } else if (id == R.id.menu_choose_folder) {
                pickGamesFolder();
        } else if (id == R.id.menu_refresh) {
            if (gamesFolderUri != null) scanGamesFolder(gamesFolderUri);
            else try { Toast.makeText(this, "Choose a games folder first", Toast.LENGTH_SHORT).show(); } catch (Throwable ignored) {}
        } else if (id == R.id.menu_covers) {
            promptForCoversUrl();
        } else if (id == R.id.menu_clear_cover_url) {
            setCoversUrlTemplate("");
            try { Toast.makeText(this, "Cover URL cleared.", Toast.LENGTH_SHORT).show(); } catch (Throwable ignored) {}
            if (gamesFolderUri != null) scanGamesFolder(gamesFolderUri);
        } else if (id == R.id.menu_bg_landscape) {
            pickBackgroundImage(false);
        } else if (id == R.id.menu_bg_portrait) {
            pickBackgroundImage(true);
        } else if (id == R.id.menu_bg_clear) {
            clearBackgroundImages();
        }
        if (drawerLayout != null) drawerLayout.closeDrawers();
        return true;
    };
    if (navStart != null) navStart.setNavigationItemSelectedListener(listener);
    try { if (drawerLayout != null) drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END); } catch (Throwable ignored) {}

    try {
        if (navStart != null && navStart.getHeaderCount() > 0) {
            View header = navStart.getHeaderView(0);
            View img = header.findViewById(R.id.header_image);
            View imgBlur = header.findViewById(R.id.header_image_blur);
            android.graphics.Bitmap bmp = loadHeaderBitmapFromAssets();
            android.graphics.Bitmap blurBmp = loadHeaderBlurBitmapFromAssets();
            if (img instanceof android.widget.ImageView && bmp != null) {
                ((android.widget.ImageView) img).setImageBitmap(bmp);
            }
            android.graphics.Bitmap useForBlur = blurBmp != null ? blurBmp : bmp;
            if (imgBlur instanceof android.widget.ImageView && useForBlur != null) {
                ((android.widget.ImageView) imgBlur).setImageBitmap(useForBlur);
                if (android.os.Build.VERSION.SDK_INT >= 31) {
                    try {
                        imgBlur.setRenderEffect(android.graphics.RenderEffect.createBlurEffect(18f, 18f, android.graphics.Shader.TileMode.CLAMP));
                    } catch (Throwable ignored) {}
                }
            }
        }
    } catch (Throwable ignored) {}

    showHome(true);
    if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);

    try {
        android.content.SharedPreferences sp = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String saved = sp.getString(PREF_GAMES_URI, null);
        if (saved != null) {
            gamesFolderUri = Uri.parse(saved);
            scanGamesFolder(gamesFolderUri);
        }
        applySavedBackground();
    } catch (Throwable ignored) {}

    boolean handledLaunch = false;
    try {
        handledLaunch = handleLaunchIntent(getIntent());
    } catch (Throwable ignored) {}
    if (!handledLaunch) {
        try {
            if (getIntent() != null && getIntent().getBooleanExtra("BOOT_BIOS", false)) {
                bootBios();
            }
        } catch (Throwable ignored) {}
    }
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        try {
            handleLaunchIntent(intent);
        } catch (Throwable ignored) {}
    }

    private boolean handleLaunchIntent(@Nullable Intent intent) {
        if (intent == null) {
            return false;
        }
        Uri dataUri = null;
        try {
            dataUri = intent.getData();
        } catch (Throwable ignored) {}
        if (dataUri == null) {
            try {
                Object stream = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (stream instanceof Uri) {
                    dataUri = (Uri) stream;
                } else if (stream instanceof String) {
                    dataUri = Uri.parse((String) stream);
                }
            } catch (Throwable ignored) {}
            if (dataUri == null) {
                String streamText = intent.getStringExtra(Intent.EXTRA_STREAM);
                if (!TextUtils.isEmpty(streamText)) {
                    try {
                        dataUri = Uri.parse(streamText);
                    } catch (Throwable ignored) {}
                }
            }
        }
        if (dataUri == null) {
            ClipData clipData = intent.getClipData();
            if (clipData != null && clipData.getItemCount() > 0) {
                ClipData.Item item = clipData.getItemAt(0);
                if (item != null) {
                    dataUri = item.getUri();
                    if (dataUri == null && item.getIntent() != null) {
                        dataUri = item.getIntent().getData();
                    }
                }
            }
        }
        if (dataUri == null) {
            try {
                java.util.ArrayList<Uri> streams = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                if (streams != null && !streams.isEmpty()) {
                    dataUri = streams.get(0);
                }
            } catch (Throwable ignored) {}
        }
        if (dataUri == null) {
            String extraText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (!TextUtils.isEmpty(extraText)) {
                try {
                    dataUri = Uri.parse(extraText);
                } catch (Throwable ignored) {}
            }
        }
        if (dataUri == null) {
            return false;
        }
        String action = intent.getAction();
        if (!TextUtils.isEmpty(action)) {
            if (!(Intent.ACTION_VIEW.equals(action)
                    || Intent.ACTION_SEND.equals(action)
                    || Intent.ACTION_SEND_MULTIPLE.equals(action)
                    || Intent.ACTION_MAIN.equals(action))) {
                return false;
            }
        }
        if (TextUtils.isEmpty(dataUri.getScheme())) {
            String path = dataUri.getPath();
            if (!TextUtils.isEmpty(path)) {
                try {
                    dataUri = Uri.fromFile(new File(path));
                } catch (Throwable ignored) {}
            }
        }
        launchGameWithPreflight(dataUri);
        return true;
    }
    private void toggleSearchBar() {
        if (etSearch == null) return;
        boolean nowVisible = etSearch.getVisibility() != View.VISIBLE;
        etSearch.setVisibility(nowVisible ? View.VISIBLE : View.GONE);
        if (nowVisible) {
            etSearch.requestFocus();
            try {
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(etSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            } catch (Throwable ignored) {}
        } else {
            try {
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
            } catch (Throwable ignored) {}
            etSearch.clearFocus();
        }
    }

    // region Covers
    private static final String PREF_COVERS_URL = "covers_url_template";
    private String getCoversUrlTemplate() {
        return getSharedPreferences(PREFS, MODE_PRIVATE).getString(PREF_COVERS_URL, "");
    }
    private void setCoversUrlTemplate(String s) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(PREF_COVERS_URL, s == null ? "" : s).apply();
    }
    private void promptForCoversUrl() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cover_template, null);
        TextInputLayout inputLayout = dialogView.findViewById(R.id.input_layout_cover_template);
        TextInputEditText input = dialogView.findViewById(R.id.input_cover_template);
        String previous = getCoversUrlTemplate();
        if (previous != null && input != null) {
            input.setText(previous);
            input.setSelection(previous.length());
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.cover_template_dialog_title)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, (d, which) -> d.dismiss())
                .setPositiveButton(R.string.action_save, null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dlg -> {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
                String value = "";
                if (input != null && input.getText() != null) {
                    value = input.getText().toString().trim();
                }
                setCoversUrlTemplate(value);
                try { Toast.makeText(this, R.string.cover_template_saved_toast, Toast.LENGTH_SHORT).show(); } catch (Throwable ignored) {}
                if (gamesFolderUri != null) {
                    scanGamesFolder(gamesFolderUri);
                }
                dialog.dismiss();
                if (!TextUtils.isEmpty(value) && !TextUtils.equals(previous, value)) {
                    CoversUtils.prefetchCoversAsync(this, value, gamesFolderUri);
                }
            });
        });
        dialog.show();
        if (inputLayout != null) {
            inputLayout.requestFocus();
        }
    }

    // region Manual cover selection

    private String pendingManualCoverGameKey;

    private final ActivityResultLauncher<Intent> startActivityResultPickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    Uri img = data.getData();
                    if (img != null) {
                        try {
                            final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            getContentResolver().takePersistableUriPermission(img, takeFlags);
                        } catch (SecurityException ignored) {}
                        String pendingKey = pendingManualCoverGameKey;
                        pendingManualCoverGameKey = null;
                        if (pendingKey != null) {
                            SettingsUtils.setManualCoverUri(this, pendingKey, img.toString());
                            if (gamesFolderUri != null) scanGamesFolder(gamesFolderUri);
                        }
                    }
                }
            });

    private final ActivityResultLauncher<Intent> startActivityResultSaveChd = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (pendingChdCachePath == null) {
                    android.util.Log.w("ARMSX2_CHD", "Save handler invoked with no pending CHD path");
                    return;
                }

                File chdFile = new File(pendingChdCachePath);
                String cachePath = pendingChdCachePath;
                pendingChdCachePath = null;
                String displayName = pendingChdDisplayName;
                pendingChdDisplayName = null;

                if (!chdFile.exists()) {
                    android.util.Log.e("ARMSX2_CHD", "Pending CHD file missing from cache: " + cachePath);
                    showConversionResult(false, "Could not locate the converted CHD file. Please try converting again.");
                    return;
                }

                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    Uri destinationUri = result.getData().getData();
                    android.util.Log.d("ARMSX2_CHD", "User selected destination URI: " + destinationUri);
                    boolean saved = saveChdToUri(chdFile, destinationUri);
                    if (saved) {
                        if (!chdFile.delete()) {
                            android.util.Log.w("ARMSX2_CHD", "Failed to delete cached CHD after saving: " + cachePath);
                        } else {
                            android.util.Log.d("ARMSX2_CHD", "Deleted cached CHD after successful save");
                        }
                        showConversionResult(true, "CHD saved to the selected location.");
                    } else {
                        showConversionResult(false, "Failed to save CHD. The converted file is still available in the app cache:\n" + cachePath);
                    }
                } else {
                    android.util.Log.i("ARMSX2_CHD", "User cancelled CHD save dialog");
                    showConversionResult(false, "Save cancelled. The converted CHD remains in the app cache:\n" + cachePath);
                }
            });

    private void showGameOptionsDialog(GameEntry e) {
        if (e == null) return;
        String key = CoversUtils.gameKeyFromEntry(e);
        String existing = SettingsUtils.getManualCoverUri(this, key);
        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);
        container.setBackgroundColor(0xEE222222);
        android.widget.TextView title = new android.widget.TextView(this);
        title.setText(e.gameTitle != null ? e.gameTitle : e.title);
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(18);
        title.setPadding(0, 0, 0, pad / 2);
        container.addView(title);

    float density = getResources().getDisplayMetrics().density;
    int primary = resolveThemeColor(android.R.attr.colorPrimary);
    int onPrimary = resolveThemeColor(android.R.attr.textColorPrimary);
    int surfaceVariant = resolveThemeColor(android.R.attr.colorBackground);
    int onSurface = resolveThemeColor(android.R.attr.textColorPrimary);
    int secondary = resolveThemeColor(com.google.android.material.R.attr.colorSecondary);
    int onSecondary = resolveThemeColor(com.google.android.material.R.attr.colorOnSecondary);
    int spacing = (int) (8f * density);

    MaterialButton pick = new MaterialButton(this);
    LinearLayout.LayoutParams pickParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    pickParams.topMargin = spacing;
    pick.setLayoutParams(pickParams);
    pick.setText(getString(R.string.cover_action_choose));
    pick.setBackgroundTintList(ColorStateList.valueOf(primary));
    pick.setTextColor(onPrimary);
    container.addView(pick);

        MaterialAlertDialogBuilder mBuilder = new MaterialAlertDialogBuilder(this)
                .setView(container)
                .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss());
        AlertDialog dlg = mBuilder.create();

        pick.setOnClickListener(v -> {
            dlg.dismiss();
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            pendingManualCoverGameKey = key;
            startActivityResultPickImage.launch(intent);
        });

        if (existing != null) {
            MaterialButton remove = new MaterialButton(this);
            LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            removeParams.topMargin = spacing;
            remove.setLayoutParams(removeParams);
            remove.setText(getString(R.string.cover_action_remove));
            remove.setBackgroundTintList(ColorStateList.valueOf(surfaceVariant));
            remove.setTextColor(onSurface);
            container.addView(remove);
            remove.setOnClickListener(v -> {
                dlg.dismiss();
                SettingsUtils.removeManualCoverUri(this, key);
                if (gamesFolderUri != null) scanGamesFolder(gamesFolderUri);
            });
        }

        MaterialButton perGame = new MaterialButton(this);
        LinearLayout.LayoutParams perGameParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        perGameParams.topMargin = spacing * 2;
        perGame.setLayoutParams(perGameParams);
        perGame.setText(getString(R.string.per_game_settings_button));
        perGame.setBackgroundTintList(ColorStateList.valueOf(secondary));
        perGame.setTextColor(onSecondary);
        container.addView(perGame);

        perGame.setOnClickListener(v -> {
            dlg.dismiss();
            showPerGameSettingsDialog(e);
        });

        dlg.show();
    }

    private void showPerGameSettingsDialog(GameEntry entry) {
        if (entry == null) return;
        String gameKey = CoversUtils.gameKeyFromEntry(entry);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_game_specific_settings, null);

        MaterialSwitch switchEnabled = dialogView.findViewById(R.id.per_game_switch_enabled);
        ViewGroup settingsGroup = dialogView.findViewById(R.id.per_game_settings_group);
        Spinner rendererSpinner = dialogView.findViewById(R.id.per_game_spinner_renderer);
        Spinner aspectSpinner = dialogView.findViewById(R.id.per_game_spinner_aspect_ratio);
        MaterialSwitch switchWidescreen = dialogView.findViewById(R.id.per_game_switch_widescreen);
        MaterialSwitch switchCheats = dialogView.findViewById(R.id.per_game_switch_enable_cheats);
        MaterialSwitch switchNoInterlacing = dialogView.findViewById(R.id.per_game_switch_no_interlacing);
        MaterialSwitch switchLoadTextures = dialogView.findViewById(R.id.per_game_switch_load_textures);
        MaterialSwitch switchAsyncTextures = dialogView.findViewById(R.id.per_game_switch_async_textures);
        MaterialSwitch switchPrecache = dialogView.findViewById(R.id.per_game_switch_precache_textures);
        MaterialSwitch switchShowFps = dialogView.findViewById(R.id.per_game_switch_show_fps);

        int globalRenderer = EmulatorSettingsUtils.getCurrentRendererValue(this);
        String globalAspect = EmulatorSettingsUtils.getCurrentAspectRatioValue(this);

        GameSpecificSettingsManager.GameSettings existing = GameSpecificSettingsManager.getSettings(this, gameKey);

        boolean initialEnabled = existing != null;
        boolean initialCheats = existing != null && existing.enableCheats != null ? existing.enableCheats : globalCheats;
        boolean initialWidescreen = existing != null && existing.widescreen != null ? existing.widescreen : globalWidescreen;
        boolean initialNoInterlacing = existing != null && existing.noInterlacing != null ? existing.noInterlacing : globalNoInterlacing;
        boolean initialLoadTextures = existing != null && existing.loadTextures != null ? existing.loadTextures : globalLoadTextures;
        boolean initialAsyncTextures = existing != null && existing.asyncTextures != null ? existing.asyncTextures : globalAsyncTextures;
        boolean initialPrecache = existing != null && existing.precacheTextures != null ? existing.precacheTextures : globalPrecache;
        boolean initialShowFps = existing != null && existing.showFps != null ? existing.showFps : globalShowFps;
        int initialRenderer = existing != null && existing.renderer != null ? existing.renderer : globalRenderer;
        String initialAspect = existing != null && !TextUtils.isEmpty(existing.aspectRatio) ? existing.aspectRatio : globalAspect;

        switchEnabled.setChecked(initialEnabled);
        switchCheats.setChecked(initialCheats);
        switchWidescreen.setChecked(initialWidescreen);
        switchNoInterlacing.setChecked(initialNoInterlacing);
        switchLoadTextures.setChecked(initialLoadTextures);
        switchAsyncTextures.setChecked(initialAsyncTextures);
        switchPrecache.setChecked(initialPrecache);
        switchShowFps.setChecked(initialShowFps);

        rendererSpinner.setSelection(rendererSpinnerPositionForValue(initialRenderer), false);

        String[] aspectOptions = getResources().getStringArray(R.array.aspect_ratios);
        int aspectIndex = 0;
        for (int i = 0; i < aspectOptions.length; i++) {
            if (TextUtils.equals(aspectOptions[i], initialAspect)) {
                aspectIndex = i;
                break;
            }
        }
        aspectSpinner.setSelection(aspectIndex, false);

        setGroupEnabled(settingsGroup, initialEnabled);

        switchEnabled.setOnCheckedChangeListener((button, isChecked) -> setGroupEnabled(settingsGroup, isChecked));

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(entry.gameTitle != null ? entry.gameTitle : entry.title)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
                .setPositiveButton(R.string.action_save, null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dlg -> {
            android.widget.Button saveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            if (saveButton == null) {
                return;
            }
            saveButton.setOnClickListener(v -> {
                if (!switchEnabled.isChecked()) {
                    GameSpecificSettingsManager.removeSettings(this, gameKey);
                    try { Toast.makeText(this, R.string.per_game_settings_cleared_toast, Toast.LENGTH_SHORT).show(); } catch (Throwable ignored) {}
                    dialog.dismiss();
                    return;
                }

                GameSpecificSettingsManager.GameSettings toSave = new GameSpecificSettingsManager.GameSettings();

                boolean cheatsValue = switchCheats.isChecked();
                if (cheatsValue != globalCheats) toSave.enableCheats = cheatsValue;

                boolean widescreenValue = switchWidescreen.isChecked();
                if (widescreenValue != globalWidescreen) toSave.widescreen = widescreenValue;

                boolean noInterlacingValue = switchNoInterlacing.isChecked();
                if (noInterlacingValue != globalNoInterlacing) toSave.noInterlacing = noInterlacingValue;

                boolean loadTexturesValue = switchLoadTextures.isChecked();
                if (loadTexturesValue != globalLoadTextures) toSave.loadTextures = loadTexturesValue;

                boolean asyncTexturesValue = switchAsyncTextures.isChecked();
                if (asyncTexturesValue != globalAsyncTextures) toSave.asyncTextures = asyncTexturesValue;

                boolean precacheValue = switchPrecache.isChecked();
                if (precacheValue != globalPrecache) toSave.precacheTextures = precacheValue;

                boolean showFpsValue = switchShowFps.isChecked();
                if (showFpsValue != globalShowFps) toSave.showFps = showFpsValue;

                int rendererValue = rendererValueForSpinnerPosition(rendererSpinner.getSelectedItemPosition());
                if (rendererValue != globalRenderer) toSave.renderer = rendererValue;

                String aspectValue = aspectOptions[aspectSpinner.getSelectedItemPosition()];
                if (!TextUtils.equals(aspectValue, globalAspect)) toSave.aspectRatio = aspectValue;

                if (toSave.hasOverrides()) {
                    GameSpecificSettingsManager.saveSettings(this, gameKey, toSave);
                    try { Toast.makeText(this, R.string.per_game_settings_saved_toast, Toast.LENGTH_SHORT).show(); } catch (Throwable ignored) {}
                } else {
                    GameSpecificSettingsManager.removeSettings(this, gameKey);
                    try { Toast.makeText(this, R.string.per_game_settings_cleared_toast, Toast.LENGTH_SHORT).show(); } catch (Throwable ignored) {}
                }
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void setGroupEnabled(@Nullable ViewGroup group, boolean enabled) {
        if (group == null) {
            return;
        }
        group.setEnabled(enabled);
        group.setAlpha(enabled ? 1f : 0.38f);
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            child.setEnabled(enabled);
            if (child instanceof ViewGroup) {
                setGroupEnabled((ViewGroup) child, enabled);
            }
        }
    }

    private int rendererSpinnerPositionForValue(int value) {
        switch (value) {
            case 12:
                return 1;
            case 13:
                return 2;
            case 14:
                return 3;
            default:
                return 0;
        }
    }

    private int rendererValueForSpinnerPosition(int position) {
        switch (position) {
            case 1:
                return 12;
            case 2:
                return 13;
            case 3:
                return 14;
            default:
                return -1;
        }
    }



    // endregion Manual cover selection

    

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyFullscreen();
        if (Build.VERSION.SDK_INT >= 33 && hasFocus) {
            try {
                GameManager gm = (GameManager) getSystemService(Context.GAME_SERVICE);
                if (gm != null) gm.setGameState(new GameState(false, GameState.MODE_GAMEPLAY_INTERRUPTIBLE));
            } catch (Throwable ignored) {}
        }
    }

    private void applyFullscreen() {
        boolean fullscreen = !isHomeVisible();
        WindowCompat.setDecorFitsSystemWindows(getWindow(), !fullscreen);
        View decorView = getWindow().getDecorView();
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), decorView);
        if (fullscreen) {
            controller.hide(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
            controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            decorView.setOnTouchListener((v, e) -> {
                if (disableTouchControls) return false;
                if (e.getAction() == MotionEvent.ACTION_DOWN || e.getAction() == MotionEvent.ACTION_MOVE) {
                    lastInput = InputSource.TOUCH;
                    lastTouchTimeMs = System.currentTimeMillis();
                    if (mEmulationThread != null) {
                        setOnScreenControlsVisible(true);
                        maybeAutoHideControls();
                    }
                    v.performClick();
                }
                return false;
            });
        } else {
            controller.show(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
            controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_DEFAULT);
            decorView.setOnTouchListener(null);
        }
    }

    public void onSurfaceReady() {
    }

    private void ensureBiosPresent() {
    if (!hasBios()) {
        Toast.makeText(this, "ARMSX2 no bios found!", Toast.LENGTH_LONG).show();
        new MaterialAlertDialogBuilder(this)
            .setMessage("No PS2 BIOS found. Please choose a BIOS file.")
            .setCancelable(true)
            .setNegativeButton("Close", (d, w) -> d.dismiss())
            .setPositiveButton("Choose BIOS", (d, w) -> openBiosPicker())
            .show();
        } else {
            // BIOS is present, signal we’re gameplay-ready.
            if (Build.VERSION.SDK_INT >= 33) {
                try {
                    GameManager gm = (GameManager) getSystemService(Context.GAME_SERVICE);
                    if (gm != null) gm.setGameState(new GameState(false, GameState.MODE_GAMEPLAY_INTERRUPTIBLE));
                } catch (Throwable ignored) {}
            }
        }
    }

    private void openBiosPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setType("application/octet-stream");
        String[] mimeTypes = new String[]{"application/octet-stream", "application/x-binary"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityResultPickBios.launch(intent);
    }

    private boolean hasBios() {
        File base = DataDirectoryManager.getDataRoot(getApplicationContext());
        File biosDir = new File(base, "bios");
        if (!biosDir.exists()) return false;
        File[] files = biosDir.listFiles((dir, name) -> name != null && name.toLowerCase().endsWith(".bin"));
        return files != null && files.length > 0;
    }

    private void saveBiosFromUri(Uri uri) {
        Context ctx = getApplicationContext();
        File base = DataDirectoryManager.getDataRoot(ctx);
        File biosDir = new File(base, "bios");
        if (!biosDir.exists()) biosDir.mkdirs();

        String outName = "ps2_bios.bin"; 
        File outFile = new File(biosDir, outName);

        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(outFile)) {
            if (in == null) throw new IOException("Unable to open BIOS Uri");
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            out.flush();
            Toast.makeText(this, "BIOS saved", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to save BIOS", Toast.LENGTH_LONG).show();
        }
    }

    private void importBiosFromUri(Uri uri) {
        Context ctx = getApplicationContext();
        File base = DataDirectoryManager.getDataRoot(ctx);
        File biosDir = new File(base, "bios");
        if (!biosDir.exists()) biosDir.mkdirs();

        String name = "ps2_bios.bin";
        try {
            if ("content".equalsIgnoreCase(uri.getScheme())) {
                try (android.database.Cursor c = getContentResolver().query(uri, new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                    if (c != null && c.moveToFirst()) {
                        String dn = c.getString(0);
                        if (dn != null && !dn.trim().isEmpty()) name = dn.trim();
                    }
                }
            } else {
                String p = uri.getPath();
                if (p != null) {
                    int idx = p.lastIndexOf('/');
                    if (idx >= 0 && idx + 1 < p.length()) name = p.substring(idx + 1);
                }
            }
        } catch (Throwable ignored) {}
        if (!name.toLowerCase().endsWith(".bin")) name = name + ".bin";

        // Avoid overwrite
        File outFile = new File(biosDir, name);
        int suffix = 1;
        while (outFile.exists()) {
            String baseName = name;
            String stem = baseName;
            String ext = "";
            int dot = baseName.lastIndexOf('.');
            if (dot > 0) { stem = baseName.substring(0, dot); ext = baseName.substring(dot); }
            outFile = new File(biosDir, stem + " (" + suffix + ")" + ext);
            suffix++;
        }

        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(outFile)) {
            if (in == null) throw new IOException("Unable to open BIOS Uri");
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            out.flush();
            Toast.makeText(this, "Imported BIOS: " + outFile.getName(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to import BIOS", Toast.LENGTH_LONG).show();
        }
    }

    private void showBiosManagerDialog() {
        Context ctx = getApplicationContext();
        File base = DataDirectoryManager.getDataRoot(ctx);
        File biosDir = new File(base, "bios");
        if (!biosDir.exists()) biosDir.mkdirs();
        File[] files = biosDir.listFiles((dir, name) -> name != null && name.toLowerCase().endsWith(".bin"));
        java.util.List<File> biosList = new java.util.ArrayList<>();
        if (files != null) java.util.Collections.addAll(biosList, files);

        final String[] names = new String[biosList.size()];
        for (int i = 0; i < biosList.size(); i++) names[i] = biosList.get(i).getName();
        int checked = -1;
        try {
            String cur = NativeApp.getSetting("Filenames", "BIOS", "string");
            if (cur != null && !cur.isEmpty()) {
                for (int i = 0; i < biosList.size(); i++) {
                    if (new File(cur).getAbsolutePath().equals(biosList.get(i).getAbsolutePath())) {
                        checked = i;
                        break;
                    }
                }
            }
        } catch (Throwable ignored) {}

        MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(this)
                .setTitle("BIOS Selection")
                .setSingleChoiceItems(names, checked, (d, which) -> {
                    try {
                        String path = biosList.get(which).getAbsolutePath();
                        NativeApp.setSetting("Filenames", "BIOS", "string", path);
                        Toast.makeText(this, "Current BIOS: " + biosList.get(which).getName(), Toast.LENGTH_SHORT).show();
                    } catch (Throwable ignored) {}
                })
                .setNegativeButton("Close", (d, w) -> d.dismiss())
                .setPositiveButton("Import", (d, w) -> openBiosImportForManager());
        b.show();
    }

    private void openBiosImportForManager() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.setType("application/octet-stream");
        String[] mimeTypes = new String[]{"application/octet-stream", "application/x-binary"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityResultImportBios.launch(intent);
    }

    // Buttons
    void configureOnClickListener(@IdRes int id, View.OnClickListener onClickListener) {
        View view = findViewById(id);
        if (view != null) {
            view.setOnClickListener(onClickListener);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    void configureOnTouchListener(@IdRes int id, int... keyCodes) {
        View view = findViewById(id);
        if (view != null) {
            view.setOnTouchListener((v, event) -> {
                for (int keyCode : keyCodes) {
                    sendKeyAction(v, event.getAction(), keyCode);
                }
                return true;
            });
        }
    }

    private void setupInGameDrawer() {
        inGameDrawer = findViewById(R.id.drawer_in_game);
        drawerToggle = findViewById(R.id.btn_drawer_toggle);
        if (drawerToggle != null) {
            drawerToggle.setVisibility(View.GONE);
            drawerToggle.setOnClickListener(v -> {
                hideDrawerToggle();
                toggleInGameDrawer();
            });
        }
        if (inGameDrawer != null) {
            try {
                inGameDrawer.setDrawerElevation(0f);
            } catch (Throwable ignored) {}
            inGameDrawer.setDrawerLockMode(disableTouchControls ? DrawerLayout.LOCK_MODE_LOCKED_CLOSED : DrawerLayout.LOCK_MODE_UNLOCKED);
            inGameDrawer.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
                @Override
                public void onDrawerOpened(@NonNull View drawerView) {
                    lastInput = InputSource.TOUCH;
                    lastTouchTimeMs = System.currentTimeMillis();
                    setOnScreenControlsVisible(true);
                    hideDrawerToggle();
                    try {
                        getWindow().getDecorView().removeCallbacks(hideRunnable);
                    } catch (Throwable ignored) {}
                    updateWidescreenToggleVisibility();
                }

                @Override
                public void onDrawerClosed(@NonNull View drawerView) {
                    lastInput = InputSource.TOUCH;
                    lastTouchTimeMs = System.currentTimeMillis();
                    maybeAutoHideControls();
                }
            });
        }

        drawerPauseButton = findViewById(R.id.drawer_btn_pause);
        if (drawerPauseButton != null) {
            drawerPauseButton.setOnClickListener(v -> toggleVmPause());
        }

        drawerFastForwardButton = findViewById(R.id.drawer_btn_fast_forward);
        if (drawerFastForwardButton != null) {
            drawerFastForwardButton.setOnClickListener(v -> toggleFastForward());
            updateFastForwardButtonState();
        }

        FloatingActionButton btnReboot = findViewById(R.id.drawer_btn_reboot);
        if (btnReboot != null) {
            btnReboot.setOnClickListener(v -> {
                restartEmuThread();
                isVmPaused = false;
                updatePauseButtonIcon();
                closeInGameDrawer();
            });
        }

        FloatingActionButton btnPower = findViewById(R.id.drawer_btn_power);
        if (btnPower != null) {
            btnPower.setOnClickListener(v -> {
                shutdownVmToHome();
                closeInGameDrawer();
            });
        }

        MaterialButton btnGames = findViewById(R.id.drawer_btn_games);
        if (btnGames != null) {
            btnGames.setOnClickListener(v -> {
                NativeApp.pause();
                isVmPaused = true;
                updatePauseButtonIcon();
                showHome(true);
                closeInGameDrawer();
            });
        }

        MaterialButton btnGameState = findViewById(R.id.drawer_btn_game_state);
        if (btnGameState != null) {
            btnGameState.setOnClickListener(v -> {
                closeInGameDrawer();
                showGameStateDialog();
            });
        }

        MaterialButton btnTestController = findViewById(R.id.drawer_btn_test_controller);
        if (btnTestController != null) {
            btnTestController.setOnClickListener(v -> {
                closeInGameDrawer();
                new ControllerMappingDialog().show(getSupportFragmentManager(), "controller_mapping");
            });
        }

        MaterialButton btnSettingsDrawer = findViewById(R.id.drawer_btn_settings);
        if (btnSettingsDrawer != null) {
            btnSettingsDrawer.setOnClickListener(v -> {
                closeInGameDrawer();
                startActivity(new Intent(this, SettingsActivity.class));
            });
        }

        MaterialButton btnAbout = findViewById(R.id.drawer_btn_about);
        if (btnAbout != null) {
            btnAbout.setOnClickListener(v -> {
                closeInGameDrawer();
                showAboutDialog();
            });
        }

        MaterialButton btnImportCheats = findViewById(R.id.drawer_btn_import_cheats);
        if (btnImportCheats != null) {
            btnImportCheats.setOnClickListener(v -> {
                closeInGameDrawer();
                launchCheatImportPicker();
            });
        }

        MaterialButton btnImportTextures = findViewById(R.id.drawer_btn_import_textures);
        if (btnImportTextures != null) {
            btnImportTextures.setOnClickListener(v -> {
                closeInGameDrawer();
                launchTextureImportPicker();
            });
        }

        setupRetroAchievementsDrawerSection();
        setupRendererToggleGroup();
        setupDrawerSpinners();
        setupControllerModeSpinner();
        setupDrawerSwitches();
        Slider uiScaleSlider = findViewById(R.id.drawer_slider_ui_scale);
        TextView uiScaleValue = findViewById(R.id.drawer_ui_scale_value);
        if (uiScaleSlider != null) {
            uiScaleSlider.setValue(onScreenUiScaleMultiplier);
            updateOnScreenUiScaleLabel(uiScaleValue);
            uiScaleSlider.addOnChangeListener((slider, value, fromUser) -> {
                float clamped = Math.max(ONSCREEN_UI_SCALE_MIN, Math.min(ONSCREEN_UI_SCALE_MAX, value));
                if (Math.abs(clamped - value) > 0.001f) {
                    slider.setValue(clamped);
                }
                if (Math.abs(onScreenUiScaleMultiplier - clamped) > 0.001f) {
                    onScreenUiScaleMultiplier = clamped;
                    saveOnScreenUiScalePreference(clamped);
                    updateOnScreenUiScaleLabel(uiScaleValue);
                    applyUserUiScale();
                }
            });
        } else {
            updateOnScreenUiScaleLabel(uiScaleValue);
        }
        updatePauseButtonIcon();
    }

    private void setupTouchRevealOverlay() {
        View root = findViewById(R.id.in_game_root);
        if (root == null) {
            return;
        }
        root.setOnTouchListener((v, event) -> {
            if (disableTouchControls) {
                return false;
            }
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                lastInput = InputSource.TOUCH;
                lastTouchTimeMs = System.currentTimeMillis();
                setOnScreenControlsVisible(true);
                maybeAutoHideControls();
                showDrawerToggleTemporarily();
            }
            return false;
        });
    }

    private void toggleVmPause() {
        if (isVmPaused) {
            NativeApp.resume();
            isVmPaused = false;
        } else {
            NativeApp.pause();
            isVmPaused = true;
        }
        updatePauseButtonIcon();
    }

    private void toggleFastForward() {
        setFastForwardEnabled(!isFastForwardEnabled);
    }

    private void setFastForwardEnabled(boolean enabled) {
        if (isFastForwardEnabled == enabled) {
            updateFastForwardButtonState();
            return;
        }
        isFastForwardEnabled = enabled;
        try {
            NativeApp.speedhackLimitermode(enabled ? 3 : 0);
        } catch (Throwable ignored) {}
        updateFastForwardButtonState();
    }

    private void toggleInGameDrawer() {
        if (inGameDrawer == null) {
            return;
        }
        if (inGameDrawer.isDrawerOpen(GravityCompat.START)) {
            inGameDrawer.closeDrawer(GravityCompat.START);
        } else {
            lastInput = InputSource.TOUCH;
            lastTouchTimeMs = System.currentTimeMillis();
            setOnScreenControlsVisible(true);
            inGameDrawer.openDrawer(GravityCompat.START);
        }
    }

    private void closeInGameDrawer() {
        if (inGameDrawer != null && inGameDrawer.isDrawerOpen(GravityCompat.START)) {
            inGameDrawer.closeDrawer(GravityCompat.START);
        }
    }

    private void updatePauseButtonIcon() {
        if (drawerPauseButton == null) {
            return;
        }
        if (isVmPaused) {
            drawerPauseButton.setImageResource(R.drawable.ic_play_circle);
            drawerPauseButton.setContentDescription(getString(R.string.drawer_resume_content_description));
        } else {
            drawerPauseButton.setImageResource(R.drawable.ic_pause_circle);
            drawerPauseButton.setContentDescription(getString(R.string.drawer_pause_content_description));
        }
    }

    private void updateFastForwardButtonState() {
        if (drawerFastForwardButton == null) {
            return;
        }
        int surfaceVariant = resolveThemeColor(android.R.attr.colorBackground);
        int onSurface = resolveThemeColor(android.R.attr.textColorPrimary);
        int primary = resolveThemeColor(android.R.attr.colorPrimary);
        int onPrimary = resolveThemeColor(android.R.attr.textColorPrimary);

        if (isFastForwardEnabled) {
            drawerFastForwardButton.setBackgroundTintList(ColorStateList.valueOf(primary));
            drawerFastForwardButton.setImageTintList(ColorStateList.valueOf(onPrimary));
            drawerFastForwardButton.setContentDescription(getString(R.string.drawer_fast_forward_on_content_description));
        } else {
            drawerFastForwardButton.setBackgroundTintList(ColorStateList.valueOf(surfaceVariant));
            drawerFastForwardButton.setImageTintList(ColorStateList.valueOf(onSurface));
            drawerFastForwardButton.setContentDescription(getString(R.string.drawer_fast_forward_content_description));
        }
    }

    private int resolveThemeColor(int attrRes) {
        TypedValue value = new TypedValue();
        if (getTheme().resolveAttribute(attrRes, value, true)) {
            if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                return value.data;
            }
            if (value.resourceId != 0) {
                return ContextCompat.getColor(this, value.resourceId);
            }
        }
        return Color.WHITE;
    }

    private void setupRetroAchievementsDrawerSection() {
        drawerRaSection = findViewById(R.id.drawer_ra_section);
        drawerRaLabel = findViewById(R.id.drawer_ra_label);
        if (drawerRaSection == null) {
            return;
        }

        drawerRaTitle = drawerRaSection.findViewById(R.id.drawer_ra_title);
        drawerRaSubtitle = drawerRaSection.findViewById(R.id.drawer_ra_subtitle);
        drawerRaIcon = drawerRaSection.findViewById(R.id.drawer_ra_icon);

        drawerRaSection.setOnClickListener(v -> showRetroAchievementsGameDialog());
        updateRetroAchievementsDrawer(currentRetroAchievementsState);
    }

    private void handleRetroAchievementsStateChanged(RetroAchievementsBridge.State state) {
        currentRetroAchievementsState = state;
        updateRetroAchievementsDrawer(state);

        if (state == null) {
            lastRetroAchievementsLoggedIn = false;
            lastRetroAchievementsGameId = -1;
            lastRetroAchievementsIconPath = "";
            return;
        }

        if (state.loggedIn && !lastRetroAchievementsLoggedIn) {
            String name = !TextUtils.isEmpty(state.displayName) ? state.displayName : state.username;
            if (!TextUtils.isEmpty(name)) {
                showRetroAchievementsToast(getString(R.string.drawer_ra_toast_connected, name));
            } else {
                showRetroAchievementsToast(getString(R.string.drawer_ra_toast_tracking_generic));
            }
        }

        if (state.hasActiveGame && state.gameId != 0 && state.gameId != lastRetroAchievementsGameId) {
            if (!TextUtils.isEmpty(state.gameTitle)) {
                showRetroAchievementsToast(getString(R.string.drawer_ra_toast_tracking, state.gameTitle));
            } else {
                showRetroAchievementsToast(getString(R.string.drawer_ra_toast_tracking_generic));
            }
        }

        lastRetroAchievementsLoggedIn = state.loggedIn;
        lastRetroAchievementsGameId = state.hasActiveGame ? state.gameId : -1;
    }

    private void updateRetroAchievementsDrawer(RetroAchievementsBridge.State state) {
        if (drawerRaSection == null) {
            return;
        }

        boolean shouldShow = state != null && state.achievementsEnabled && state.loggedIn && state.hasActiveGame
                && !TextUtils.isEmpty(state.gameTitle);
        int visibility = shouldShow ? View.VISIBLE : View.GONE;
        drawerRaSection.setVisibility(visibility);
        if (drawerRaLabel != null) {
            drawerRaLabel.setVisibility(visibility);
        }

        if (!shouldShow) {
            if (drawerRaIcon != null) {
                drawerRaIcon.setImageDrawable(null);
                drawerRaIcon.setVisibility(View.GONE);
            }
            lastRetroAchievementsIconPath = "";
            return;
        }

        if (drawerRaTitle != null) {
            drawerRaTitle.setText(state.gameTitle);
        }

        if (drawerRaSubtitle != null) {
            String subtitle = null;
            if (!TextUtils.isEmpty(state.richPresence)) {
                subtitle = state.richPresence;
            } else if (state.totalAchievements > 0) {
                subtitle = getString(R.string.drawer_ra_dialog_progress, state.unlockedAchievements, state.totalAchievements);
            }
            if (!TextUtils.isEmpty(subtitle)) {
                drawerRaSubtitle.setText(subtitle);
                drawerRaSubtitle.setVisibility(View.VISIBLE);
            } else {
                drawerRaSubtitle.setText("");
                drawerRaSubtitle.setVisibility(View.GONE);
            }
        }

        if (drawerRaIcon != null) {
            boolean iconVisible = false;
            if (!TextUtils.isEmpty(state.gameIconPath)) {
                File iconFile = new File(state.gameIconPath);
                if (iconFile.exists() && iconFile.isFile()) {
                    if (!state.gameIconPath.equals(lastRetroAchievementsIconPath)) {
                        Bitmap bitmap = BitmapFactory.decodeFile(iconFile.getAbsolutePath());
                        if (bitmap != null) {
                            drawerRaIcon.setImageBitmap(bitmap);
                            lastRetroAchievementsIconPath = state.gameIconPath;
                        } else {
                            drawerRaIcon.setImageDrawable(null);
                            lastRetroAchievementsIconPath = "";
                        }
                    }
                    iconVisible = drawerRaIcon.getDrawable() != null;
                } else {
                    drawerRaIcon.setImageDrawable(null);
                    lastRetroAchievementsIconPath = "";
                }
            } else {
                drawerRaIcon.setImageDrawable(null);
                lastRetroAchievementsIconPath = "";
            }
            drawerRaIcon.setVisibility(iconVisible ? View.VISIBLE : View.GONE);
        }
    }

    private void showRetroAchievementsGameDialog() {
        RetroAchievementsBridge.State state = currentRetroAchievementsState;
        if (state == null || !state.achievementsEnabled || !state.loggedIn || !state.hasActiveGame) {
            return;
        }

        String title = !TextUtils.isEmpty(state.gameTitle) ? state.gameTitle : getString(R.string.drawer_ra_dialog_title);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setPositiveButton(android.R.string.ok, null);

        StringBuilder message = new StringBuilder();
        if (!TextUtils.isEmpty(state.richPresence)) {
            message.append(state.richPresence.trim());
        }
        if (state.totalAchievements > 0) {
            if (message.length() > 0) {
                message.append("\n\n");
            }
            message.append(getString(R.string.drawer_ra_dialog_progress, state.unlockedAchievements, state.totalAchievements));
        }
        if (state.totalPoints > 0) {
            if (message.length() > 0) {
                message.append("\n");
            }
            message.append(getString(R.string.drawer_ra_dialog_points, state.unlockedPoints, state.totalPoints));
        }
        if (state.hasLeaderboards) {
            if (message.length() > 0) {
                message.append("\n");
            }
            message.append(getString(R.string.drawer_ra_dialog_leaderboards));
        }

        if (message.length() > 0) {
            builder.setMessage(message.toString());
        }

        if (drawerRaIcon != null) {
            Drawable drawable = drawerRaIcon.getDrawable();
            if (drawable != null) {
                builder.setIcon(drawable);
            }
        }

        builder.show();
    }

    private void showRetroAchievementsToast(String message) {
        if (TextUtils.isEmpty(message)) {
            return;
        }
        try {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {}
    }

    private void setupRendererToggleGroup() {
        MaterialButtonToggleGroup rendererGroup = findViewById(R.id.drawer_tg_renderer);
        if (rendererGroup == null) {
            return;
        }

        int initialValue = -1;
        try {
            String renderer = NativeApp.getSetting("EmuCore/GS", "Renderer", "int");
            if (renderer != null && !renderer.isEmpty()) {
                initialValue = Integer.parseInt(renderer);
            }
        } catch (Exception ignored) {}

        int initialButton = rendererButtonForValue(initialValue);
        rendererGroup.check(initialButton);
        rendererGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            int value = rendererValueForButton(checkedId);
            NativeApp.renderGpu(value);
        });
    }

    private void setupDrawerSpinners() {
		Spinner aspectSpinner = findViewById(R.id.drawer_sp_aspect_ratio);
		if (aspectSpinner != null) {
			ArrayAdapter<CharSequence> aspectAdapter = ArrayAdapter.createFromResource(this, R.array.aspect_ratios, android.R.layout.simple_spinner_item);
			aspectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			aspectSpinner.setAdapter(aspectAdapter);
			final String[] aspectChoices = getResources().getStringArray(R.array.aspect_ratios);
			int current = 0;
			try {
				String aspect = NativeApp.getSetting("EmuCore/GS", "AspectRatio", "string");
				if (aspect != null && !aspect.isEmpty()) {
					for (int i = 0; i < aspectChoices.length; i++) {
						if (aspect.equalsIgnoreCase(aspectChoices[i])) {
							current = i;
							break;
						}
					}
				}
			} catch (Exception ignored) {}
			if (current < 0 || current >= aspectAdapter.getCount()) current = 0;
			aspectSpinner.setSelection(current, false);
			aspectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
					if (position < 0 || position >= aspectChoices.length)
						return;
					String value = aspectChoices[position];
					NativeApp.setSetting("EmuCore/GS", "AspectRatio", "string", value);
					NativeApp.setAspectRatio(position);
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {
				}
            });
        }

        Spinner scaleSpinner = findViewById(R.id.drawer_sp_scale);
        if (scaleSpinner != null) {
            ArrayAdapter<CharSequence> scaleAdapter = ArrayAdapter.createFromResource(this, R.array.resolution_scales, android.R.layout.simple_spinner_item);
            scaleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            scaleSpinner.setAdapter(scaleAdapter);
            int current = 0;
            try {
                String value = NativeApp.getSetting("EmuCore/GS", "upscale_multiplier", "float");
                if (value != null && !value.isEmpty()) {
                    float parsed = Float.parseFloat(value);
                    current = Math.max(1, Math.min(scaleAdapter.getCount(), Math.round(parsed))) - 1;
                }
            } catch (Exception ignored) {}
            if (current < 0 || current >= scaleAdapter.getCount()) current = 0;
            scaleSpinner.setSelection(current, false);
            scaleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    NativeApp.setSetting("EmuCore/GS", "upscale_multiplier", "float", String.valueOf(position + 1));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

        Spinner blendSpinner = findViewById(R.id.drawer_sp_blending_accuracy);
        if (blendSpinner != null) {
            ArrayAdapter<CharSequence> blendAdapter = ArrayAdapter.createFromResource(this, R.array.acc_blending, android.R.layout.simple_spinner_item);
            blendAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            blendSpinner.setAdapter(blendAdapter);
            int current = 0;
            try {
                String value = NativeApp.getSetting("EmuCore/GS", "accurate_blending_unit", "int");
                if (value != null && !value.isEmpty()) {
                    current = Integer.parseInt(value);
                }
            } catch (Exception ignored) {}
            if (current < 0 || current >= blendAdapter.getCount()) current = 0;
            blendSpinner.setSelection(current, false);
            blendSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    NativeApp.setSetting("EmuCore/GS", "accurate_blending_unit", "int", Integer.toString(position));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }
    }

    private void setupControllerModeSpinner() {
        Spinner modeSpinner = findViewById(R.id.drawer_sp_controller_mode);
        if (modeSpinner != null) {
            String[] modes = new String[]{"2 Sticks", "1 Stick + Face Buttons", "D-Pad Only"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, modes);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            modeSpinner.setAdapter(adapter);
            
            // Load saved mode (default is 0 = 2 Sticks)
            int savedMode = getSharedPreferences(PREFS, MODE_PRIVATE).getInt("controller_mode", 0);
            modeSpinner.setSelection(savedMode, false);
            
            modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt("controller_mode", position).apply();
                    applyControllerMode(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            
            applyControllerMode(savedMode);
        }
    }

    private void applyControllerMode(int mode) {
        currentControllerMode = mode;
        
        JoystickView joystickLeft = findViewById(R.id.joystick_left);
        JoystickView joystickRight = findViewById(R.id.joystick_right);
        DPadView dpadView = findViewById(R.id.dpad_view);
        View llPadRight = findViewById(R.id.ll_pad_right);
        
        if (joystickLeft == null || joystickRight == null || dpadView == null || llPadRight == null) {
            return;
        }
        
        switch (mode) {
            case 0: // 2 Sticks 
                joystickLeft.setVisibility(View.VISIBLE);
                joystickRight.setVisibility(View.VISIBLE);
                dpadView.setVisibility(View.VISIBLE);
                llPadRight.setVisibility(View.VISIBLE);
                
                ViewGroup.LayoutParams leftParams = joystickLeft.getLayoutParams();
                leftParams.width = dpToPx(140);
                leftParams.height = dpToPx(140);
                joystickLeft.setLayoutParams(leftParams);
                
                ViewGroup.LayoutParams rightParams = joystickRight.getLayoutParams();
                rightParams.width = dpToPx(140);
                rightParams.height = dpToPx(140);
                joystickRight.setLayoutParams(rightParams);
                
                ViewGroup.LayoutParams dpadParams = dpadView.getLayoutParams();
                dpadParams.width = dpToPx(105);
                dpadParams.height = dpToPx(105);
                dpadView.setLayoutParams(dpadParams);
                
                llPadRight.setScaleX(1.0f);
                llPadRight.setScaleY(1.0f);
                if (llPadRight.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams faceParams = (ViewGroup.MarginLayoutParams) llPadRight.getLayoutParams();
                    faceParams.bottomMargin = dpToPx(1); 
                    llPadRight.setLayoutParams(faceParams);
                }
                faceButtonsBaseScale = 1.0f;
                break;
                
            case 1: // 1 Stick + Face Buttons 
                joystickLeft.setVisibility(View.VISIBLE);
                joystickRight.setVisibility(View.GONE); 
                dpadView.setVisibility(View.GONE); 
                llPadRight.setVisibility(View.VISIBLE);
                
                ViewGroup.LayoutParams leftParams1 = joystickLeft.getLayoutParams();
                leftParams1.width = dpToPx(140);
                leftParams1.height = dpToPx(140);
                joystickLeft.setLayoutParams(leftParams1);
                
                ViewGroup.LayoutParams dpadParams1 = dpadView.getLayoutParams();
                dpadParams1.width = dpToPx(105);
                dpadParams1.height = dpToPx(105);
                dpadView.setLayoutParams(dpadParams1);
                
                llPadRight.setScaleX(1.4f);
                llPadRight.setScaleY(1.4f);
                
                if (llPadRight.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams faceParams1 = (ViewGroup.MarginLayoutParams) llPadRight.getLayoutParams();
                    faceParams1.bottomMargin = dpToPx(6) + dpToPx(11); 
                    llPadRight.setLayoutParams(faceParams1);
                }
                faceButtonsBaseScale = 1.4f;
                break;
                
            case 2: // D-Pad Only 
                joystickLeft.setVisibility(View.GONE);
                joystickRight.setVisibility(View.GONE);
                dpadView.setVisibility(View.VISIBLE);
                llPadRight.setVisibility(View.VISIBLE);
                
                ViewGroup.LayoutParams dpadParams2 = dpadView.getLayoutParams();
                dpadParams2.width = dpToPx(140);
                dpadParams2.height = dpToPx(140);
                dpadView.setLayoutParams(dpadParams2);
                
                ViewGroup.LayoutParams rightParams2 = joystickRight.getLayoutParams();
                rightParams2.width = dpToPx(140);
                rightParams2.height = dpToPx(140);
                joystickRight.setLayoutParams(rightParams2);
                
                llPadRight.setScaleX(1.4f);
                llPadRight.setScaleY(1.4f);
                
                if (llPadRight.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams faceParams2 = (ViewGroup.MarginLayoutParams) llPadRight.getLayoutParams();
                    faceParams2.bottomMargin = dpToPx(6) + dpToPx(11); 
                    llPadRight.setLayoutParams(faceParams2);
                }
                faceButtonsBaseScale = 1.4f;
                break;
        }
        applyJoystickStyle(joystickLeft);
        applyJoystickStyle(joystickRight);
        applyDpadStyle(dpadView);
        applyUserUiScale();
    }

    private void setupDrawerSwitches() {
        MaterialSwitch swEnableCheats = findViewById(R.id.drawer_sw_enable_cheats);
        if (swEnableCheats != null) {
            swEnableCheats.setChecked(readBoolSetting("EmuCore", "EnableCheats", false));
            swEnableCheats.setOnCheckedChangeListener((buttonView, isChecked) ->
            {
                    NativeApp.setEnableCheats(isChecked);
                    try {
                        DebugLog.d("Cheats", "EnableCheats=" + isChecked);
                    } catch (Throwable ignored) {}
            });
        }

        drawerWidescreenSwitch = findViewById(R.id.drawer_sw_widescreen);
        updateWidescreenToggleVisibility();

        MaterialSwitch swNoInterlacing = findViewById(R.id.drawer_sw_no_interlacing);
        if (swNoInterlacing != null) {
            swNoInterlacing.setChecked(readBoolSetting("EmuCore", "EnableNoInterlacingPatches", false));
            swNoInterlacing.setOnCheckedChangeListener((buttonView, isChecked) ->
                    NativeApp.setSetting("EmuCore", "EnableNoInterlacingPatches", "bool", isChecked ? "true" : "false"));
        }

        MaterialSwitch swLoadTextures = findViewById(R.id.drawer_sw_load_textures);
        if (swLoadTextures != null) {
            swLoadTextures.setChecked(readBoolSetting("EmuCore/GS", "LoadTextureReplacements", false));
            swLoadTextures.setOnCheckedChangeListener((buttonView, isChecked) -> {
                NativeApp.setSetting("EmuCore/GS", "LoadTextureReplacements", "bool", isChecked ? "true" : "false");
                try {
                    DebugLog.d("Textures", "LoadTextureReplacements=" + isChecked);
                } catch (Throwable ignored) {}
            });
        }

        MaterialSwitch swAsyncTextures = findViewById(R.id.drawer_sw_async_textures);
        if (swAsyncTextures != null) {
            swAsyncTextures.setChecked(readBoolSetting("EmuCore/GS", "LoadTextureReplacementsAsync", false));
            swAsyncTextures.setOnCheckedChangeListener((buttonView, isChecked) -> {
                NativeApp.setSetting("EmuCore/GS", "LoadTextureReplacementsAsync", "bool", isChecked ? "true" : "false");
                try {
                    DebugLog.d("Textures", "LoadTextureReplacementsAsync=" + isChecked);
                } catch (Throwable ignored) {}
            });
        }

        MaterialSwitch swPrecacheTextures = findViewById(R.id.drawer_sw_precache_textures);
        if (swPrecacheTextures != null) {
            swPrecacheTextures.setChecked(readBoolSetting("EmuCore/GS", "PrecacheTextureReplacements", false));
            swPrecacheTextures.setOnCheckedChangeListener((buttonView, isChecked) -> {
                NativeApp.setSetting("EmuCore/GS", "PrecacheTextureReplacements", "bool", isChecked ? "true" : "false");
                try {
                    DebugLog.d("Textures", "PrecacheTextureReplacements=" + isChecked);
                } catch (Throwable ignored) {}
            });
        }

        MaterialSwitch swDevHud = findViewById(R.id.drawer_sw_dev_hud);
        if (swDevHud != null) {
            swDevHud.setChecked(readBoolSetting("EmuCore/GS", "OsdShowFPS", false));
            swDevHud.setOnCheckedChangeListener((buttonView, isChecked) ->
                    NativeApp.setSetting("EmuCore/GS", "OsdShowFPS", "bool", isChecked ? "true" : "false"));
        }
    }

    private void updateWidescreenToggleVisibility() {
        if (drawerWidescreenSwitch == null) {
            return;
        }
        boolean hasPatch = false;
        try {
            hasPatch = NativeApp.hasWidescreenPatch();
        } catch (Throwable ignored) {}
        if (!hasPatch) {
            drawerWidescreenSwitch.setVisibility(View.GONE);
            drawerWidescreenSwitch.setOnCheckedChangeListener(null);
            return;
        }
        drawerWidescreenSwitch.setVisibility(View.VISIBLE);
        drawerWidescreenSwitch.setText(R.string.drawer_apply_widescreen_patch);
        drawerWidescreenSwitch.setOnCheckedChangeListener(null);
        drawerWidescreenSwitch.setChecked(readBoolSetting("EmuCore", "EnableWideScreenPatches", false));
        drawerWidescreenSwitch.setOnCheckedChangeListener(drawerWidescreenListener);
    }



    private @IdRes int rendererButtonForValue(int value) {
        switch (value) {
            case 12:
                return R.id.drawer_tb_gl;
            case 13:
                return R.id.drawer_tb_sw;
            case 14:
                return R.id.drawer_tb_vk;
            default:
                return R.id.drawer_tb_at;
        }
    }

    private int rendererValueForButton(@IdRes int buttonId) {
        if (buttonId == R.id.drawer_tb_gl) return 12;
        if (buttonId == R.id.drawer_tb_sw) return 13;
        if (buttonId == R.id.drawer_tb_vk) return 14;
        return -1;
    }

    private void applyRendererSelection(int rendererValue) {
        MaterialButtonToggleGroup rendererGroup = findViewById(R.id.drawer_tg_renderer);
        if (rendererGroup != null) {
            rendererGroup.check(rendererButtonForValue(rendererValue));
        } else {
            NativeApp.renderGpu(rendererValue);
        }
    }

    private void showGameStateDialog() {
        CharSequence[] items = new CharSequence[]{
                "Save state (slot 1)",
                "Load state (slot 1)"
        };
    new MaterialAlertDialogBuilder(this)
        .setTitle("Game State")
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        pauseVmForStateOperation();
                        boolean ok = NativeApp.saveStateToSlot(1);
                        try {
                            Toast.makeText(this, ok ? "State saved" : "Failed to save state", Toast.LENGTH_SHORT).show();
                        } catch (Throwable ignored) {}
                        resumeVmAfterStateOperation();
                    } else if (which == 1) {
                        pauseVmForStateOperation();
                        boolean ok = NativeApp.loadStateFromSlot(1);
                        try {
                            Toast.makeText(this, ok ? "State loaded" : "Failed to load state", Toast.LENGTH_SHORT).show();
                        } catch (Throwable ignored) {}
                        if (!ok) {
                            resumeVmAfterStateOperation();
                            return;
                        }
                        resumeVmAfterStateOperation();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showAboutDialog() {
        String versionName = "";
        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception ignored) {}
    String message = "ARMSX2 (" + versionName + ")\n" +
        "by ARMSX2 team\n\n" +
        "Core contributors:\n" +
        "- MoonPower — App developer\n" +
        "- jpolo — Management\n" +
        "- Medievalshell — Web developer\n" +
        "- set l — Web developer\n" +
        "- Alex — QA tester\n" +
        "- Yua — QA tester\n\n" +
        "Thanks to:\n" +
        "- pontos2024 (emulator base)\n" +
        "- PCSX2 v2.3.430 (core emulator)\n" +
        "- SDL (SDL3)\n" +
        "- Fffathur (icon design)\n" +
        "- vivimagic0 (icon design)";
    new MaterialAlertDialogBuilder(this)
        .setTitle("About")
        .setMessage(message)
        .setPositiveButton(android.R.string.ok, (d, w) -> d.dismiss())
        .show();
    }

    private String resolveOnScreenUiStylePreference() {
        String value = getSharedPreferences(PREFS, MODE_PRIVATE).getString(PREF_ONSCREEN_UI_STYLE, STYLE_DEFAULT);
        if (STYLE_NETHER.equalsIgnoreCase(value)) {
            return STYLE_NETHER;
        }
        return STYLE_DEFAULT;
    }

    private void refreshOnScreenUiStyleIfNeeded() {
        String pref = resolveOnScreenUiStylePreference();
        if (!pref.equals(currentOnScreenUiStyle)) {
            currentOnScreenUiStyle = pref;
            mInputManager.makeButtonTouch();
        }
    }

    private Drawable loadNetherDrawable(String assetName) {
        try (InputStream is = getAssets().open("app_icons/controller_icons_nether/" + assetName)) {
            Drawable drawable = Drawable.createFromStream(is, assetName);
            if (drawable != null) {
                drawable = drawable.mutate();
            }
            return drawable;
        } catch (IOException e) {
            try { DebugLog.e("OnScreenUI", "Failed to load Nether icon " + assetName + ": " + e.getMessage()); } catch (Throwable ignored) {}
            return null;
        }
    }

    public void applyButtonIcon(PSButtonView view, @DrawableRes int defaultResId, String netherAssetName) {
        if (view == null) {
            return;
        }
        if (STYLE_NETHER.equals(currentOnScreenUiStyle)) {
            Drawable drawable = loadNetherDrawable(netherAssetName);
            if (drawable != null) {
                view.setIconDrawable(drawable);
                return;
            }
        }
        view.setIconResource(defaultResId);
    }

    public void applyShoulderIcon(PSShoulderButtonView view, @DrawableRes int defaultResId, String netherAssetName) {
        if (view == null) {
            return;
        }
        if (STYLE_NETHER.equals(currentOnScreenUiStyle)) {
            Drawable drawable = loadNetherDrawable(netherAssetName);
            if (drawable != null) {
                view.setIconDrawable(drawable);
                return;
            }
        }
        view.setIconResource(defaultResId);
    }

    public void applyJoystickStyle(JoystickView joystick) {
        if (joystick == null) {
            return;
        }
        if (STYLE_NETHER.equals(currentOnScreenUiStyle)) {
            Drawable base = loadNetherDrawable("ic_controller_analog_base.png");
            Drawable knob = loadNetherDrawable("ic_controller_analog_stick.png");
            if (base != null && knob != null) {
                joystick.setDrawables(base, knob);
                joystick.setKnobScaleFactor(1.2f);
                return;
            }
        }
        joystick.setDrawables(null, null);
        joystick.setKnobScaleFactor(1.0f);
    }

    public void applyDpadStyle(DPadView dpadView) {
        if (dpadView == null) {
            return;
        }
        if (STYLE_NETHER.equals(currentOnScreenUiStyle)) {
            Drawable up = loadNetherDrawable("ic_controller_up_button.png");
            Drawable down = loadNetherDrawable("ic_controller_down_button.png");
            Drawable left = loadNetherDrawable("ic_controller_left_button.png");
            Drawable right = loadNetherDrawable("ic_controller_right_button.png");
            dpadView.setDrawables(null, null);
            dpadView.setDirectionalDrawables(up, down, left, right);
        } else {
            dpadView.setDrawables(null, null);
            dpadView.setDirectionalDrawables(null, null, null, null);
        }
    }

    private void loadOnScreenUiScalePreference() {
        float value = 1.0f;
        try {
            value = getSharedPreferences(PREFS, MODE_PRIVATE).getFloat(PREF_UI_SCALE_MULTIPLIER, 1.0f);
        } catch (Exception ignored) {}
        if (value < ONSCREEN_UI_SCALE_MIN) value = ONSCREEN_UI_SCALE_MIN;
        if (value > ONSCREEN_UI_SCALE_MAX) value = ONSCREEN_UI_SCALE_MAX;
        onScreenUiScaleMultiplier = value;
    }

    private void saveOnScreenUiScalePreference(float value) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putFloat(PREF_UI_SCALE_MULTIPLIER, value).apply();
    }

    public void applyUserUiScale() {
        float multiplier = Math.max(ONSCREEN_UI_SCALE_MIN, Math.min(ONSCREEN_UI_SCALE_MAX, onScreenUiScaleMultiplier));
        onScreenUiScaleMultiplier = multiplier;
        applyScaleWithPivot(llPadSelectStart, multiplier, multiplier, 0.5f, 1f);
        View padRight = llPadRight != null ? llPadRight : findViewById(R.id.ll_pad_right);
        float faceScale = faceButtonsBaseScale * multiplier;
        applyScaleWithPivot(padRight, faceScale, faceScale, 1f, 1f);
        View leftShoulders = findViewById(R.id.ll_pad_shoulders_left);
        applyScaleWithPivot(leftShoulders, multiplier, multiplier, 0f, 0f);
        View rightShoulders = findViewById(R.id.ll_pad_shoulders_right);
        applyScaleWithPivot(rightShoulders, multiplier, multiplier, 1f, 0f);
        JoystickView joystickLeft = findViewById(R.id.joystick_left);
        applyScaleWithPivot(joystickLeft, multiplier, multiplier, 0f, 1f);
        JoystickView joystickRight = findViewById(R.id.joystick_right);
        applyScaleWithPivot(joystickRight, multiplier, multiplier, 1f, 1f);
        DPadView dpadView = findViewById(R.id.dpad_view);
        applyScaleWithPivot(dpadView, multiplier, multiplier, 0f, 1f);
    }

    private void applyScaleWithPivot(View view, float scaleX, float scaleY, float pivotXF, float pivotYF) {
        if (view == null) {
            return;
        }
        Runnable apply = () -> {
            float pivotX = view.getWidth() * pivotXF;
            float pivotY = view.getHeight() * pivotYF;
            view.setPivotX(pivotX);
            view.setPivotY(pivotY);
            view.setScaleX(scaleX);
            view.setScaleY(scaleY);
        };
        if (view.getWidth() == 0 || view.getHeight() == 0) {
            view.post(apply);
        } else {
            apply.run();
        }
    }

    private void pauseVmForStateOperation() {
        try {
            NativeApp.pause();
            SystemClock.sleep(50);
            NativeApp.resetKeyStatus();
        } catch (Throwable ignored) {}
    }

    private void resumeVmAfterStateOperation() {
        try {
            SystemClock.sleep(30);
            NativeApp.resume();
            isVmPaused = false;
            updatePauseButtonIcon();
        } catch (Throwable ignored) {}
    }

    private void updateOnScreenUiScaleLabel(TextView label) {
        if (label != null) {
            label.setText(getString(R.string.drawer_ui_scale_value, onScreenUiScaleMultiplier));
        }
    }

    private void refreshOnScreenUiScaleIfNeeded() {
        float stored = 1.0f;
        try {
            stored = getSharedPreferences(PREFS, MODE_PRIVATE).getFloat(PREF_UI_SCALE_MULTIPLIER, 1.0f);
        } catch (Exception ignored) {}
        if (stored < ONSCREEN_UI_SCALE_MIN) stored = ONSCREEN_UI_SCALE_MIN;
        if (stored > ONSCREEN_UI_SCALE_MAX) stored = ONSCREEN_UI_SCALE_MAX;
        if (Math.abs(stored - onScreenUiScaleMultiplier) > 0.001f) {
            onScreenUiScaleMultiplier = stored;
            applyUserUiScale();
            Slider slider = findViewById(R.id.drawer_slider_ui_scale);
            if (slider != null && Math.abs(slider.getValue() - stored) > 0.001f) {
                slider.setValue(stored);
            }
            TextView label = findViewById(R.id.drawer_ui_scale_value);
            updateOnScreenUiScaleLabel(label);
        }
    }



    private boolean importMemcardToSlot1(Uri uri) {
        try {
            File base = DataDirectoryManager.getDataRoot(getApplicationContext());
            File memDir = new File(base, "memcards");
            if (!memDir.exists() && !memDir.mkdirs()) return false;
            File out = new File(memDir, "Mcd001.ps2");
            try (InputStream in = getContentResolver().openInputStream(uri);
                 OutputStream os = new FileOutputStream(out)) {
                if (in == null) return false;
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
                os.flush();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void importCheatFile(Uri uri) {
        if (uri == null) {
            return;
        }
        new Thread(() -> {
            boolean success = false;
            String targetName = null;
            String errorReason = null;
            File dataRoot = DataDirectoryManager.getDataRoot(getApplicationContext());
            if (dataRoot == null) {
                errorReason = "Cheat import unavailable: data directory not resolved.";
            } else {
                File cheatsDir = new File(dataRoot, "cheats");
                if (!cheatsDir.exists() && !cheatsDir.mkdirs()) {
                    errorReason = "Unable to create cheats directory: " + cheatsDir;
                    try { DebugLog.e("Cheats", errorReason); } catch (Throwable ignored) {}
                } else {
                    String displayName = getDisplayNameForUri(uri);
                    if (TextUtils.isEmpty(displayName)) {
                        displayName = "custom_cheats.pnach";
                    }
                    if (!displayName.toLowerCase(Locale.US).endsWith(".pnach")) {
                        displayName = displayName + ".pnach";
                    }
                    File destination = createUniqueFile(cheatsDir, displayName);
                    try (InputStream in = getContentResolver().openInputStream(uri);
                         OutputStream out = new FileOutputStream(destination)) {
                        if (in == null) {
                            throw new IOException("Cheat source stream unavailable.");
                        }
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                        out.flush();
                        success = true;
                        targetName = destination.getName();
                    } catch (Exception e) {
                        errorReason = e.getMessage();
                        if (errorReason == null || errorReason.trim().isEmpty()) {
                            errorReason = e.getClass().getSimpleName();
                        }
                        try { DebugLog.e("Cheats", "Import failed: " + errorReason); } catch (Throwable ignored) {}
                    }
                }
            }

            boolean finalSuccess = success;
            String finalName = targetName;
            String finalError = errorReason;
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this,
                        finalSuccess
                                ? getString(R.string.drawer_toast_cheats_import_success, finalName)
                                : getString(R.string.drawer_toast_cheats_import_failed),
                        Toast.LENGTH_SHORT).show();
                if (finalSuccess) {
                    try {
                        NativeApp.setEnableCheats(true);
                    } catch (Throwable ignored) {}
                } else {
                    showDrawerImportFailureDialog(R.string.drawer_error_import_cheats_title, finalError);
                }
            });
        }).start();
    }

    private void importTextureArchive(Uri uri) {
        if (uri == null) {
            return;
        }
        new Thread(() -> {
            boolean success = false;
            String errorReason = null;
            File dataRoot = DataDirectoryManager.getDataRoot(getApplicationContext());
            if (dataRoot == null) {
                errorReason = "Texture import unavailable: data directory not resolved.";
            } else {
                File texturesDir = new File(dataRoot, "textures");
                if (!texturesDir.exists() && !texturesDir.mkdirs()) {
                    errorReason = "Unable to create textures directory: " + texturesDir;
                    try { DebugLog.e("Textures", errorReason); } catch (Throwable ignored) {}
                } else {
                    try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                        if (inputStream == null) {
                            throw new IOException("Texture archive stream unavailable.");
                        }
                        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(inputStream))) {
                            byte[] buffer = new byte[8192];
                            ZipEntry entry;
                            while ((entry = zis.getNextEntry()) != null) {
                                File outFile = new File(texturesDir, entry.getName());
                                if (!isFileInsideBase(texturesDir, outFile)) {
                                    zis.closeEntry();
                                    continue;
                                }
                                if (entry.isDirectory()) {
                                    if (!outFile.exists() && !outFile.mkdirs()) {
                                        throw new IOException("Failed to create directory " + outFile);
                                    }
                                } else {
                                    File parent = outFile.getParentFile();
                                    if (parent != null && !parent.exists() && !parent.mkdirs()) {
                                        throw new IOException("Failed to create parent " + parent);
                                    }
                                    try (OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile))) {
                                        int count;
                                        while ((count = zis.read(buffer)) != -1) {
                                            out.write(buffer, 0, count);
                                        }
                                        out.flush();
                                    }
                                }
                                zis.closeEntry();
                            }
                            success = true;
                        }
                    } catch (Exception e) {
                        errorReason = e.getMessage();
                        if (errorReason == null || errorReason.trim().isEmpty()) {
                            errorReason = e.getClass().getSimpleName();
                        }
                        try { DebugLog.e("Textures", "Import failed: " + errorReason); } catch (Throwable ignored) {}
                    }
                }
            }

            boolean finalSuccess = success;
            String finalError = errorReason;
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this,
                        finalSuccess
                                ? getString(R.string.drawer_toast_textures_import_success)
                                : getString(R.string.drawer_toast_textures_import_failed),
                        Toast.LENGTH_SHORT).show();
                if (finalSuccess) {
                    try {
                        NativeApp.setSetting("EmuCore/GS", "LoadTextureReplacements", "bool", "true");
                        NativeApp.setSetting("EmuCore/GS", "LoadTextureReplacementsAsync", "bool", "true");
                    } catch (Throwable ignored) {}
                } else {
                    showDrawerImportFailureDialog(R.string.drawer_error_import_textures_title, finalError);
                }
            });
        }).start();
    }

    private String getDisplayNameForUri(Uri uri) {
        if (uri == null) {
            return null;
        }
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    return cursor.getString(index);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private File createUniqueFile(File directory, String name) {
        File candidate = new File(directory, name);
        if (!candidate.exists()) {
            return candidate;
        }
        String baseName = name;
        String extension = "";
        int dot = name.lastIndexOf('.');
        if (dot >= 0) {
            baseName = name.substring(0, dot);
            extension = name.substring(dot);
        }
        int index = 1;
        while (candidate.exists()) {
            candidate = new File(directory, baseName + "_" + index + extension);
            index++;
        }
        return candidate;
    }

    private boolean isFileInsideBase(File base, File target) {
        try {
            String basePath = base.getCanonicalPath();
            String targetPath = target.getCanonicalPath();
            return targetPath.startsWith(basePath + File.separator);
        } catch (IOException e) {
            return false;
        }
    }

    private void persistUriPermission(Uri uri) {
        if (uri == null) {
            return;
        }
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {}
    }

    private void showDrawerImportFailureDialog(@StringRes int titleRes, String details) {
        if (isFinishing()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed()) {
            return;
        }
        String message = (details != null && !details.trim().isEmpty())
                ? details.trim()
                : getString(R.string.drawer_error_import_unknown);
        new MaterialAlertDialogBuilder(this)
                .setTitle(titleRes)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showSettingsDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_settings, null);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .create();

        View btnClose = view.findViewById(R.id.btn_close);
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        // Aspect ratio options
        View rg = view.findViewById(R.id.rg_aspect);
        if (rg instanceof android.widget.RadioGroup) {
            ((android.widget.RadioGroup) rg).setOnCheckedChangeListener((group, checkedId) -> {
                int type = 1; // default to Auto
                if (checkedId == R.id.rb_ar_4_3) type = 2;
                else if (checkedId == R.id.rb_ar_16_9) type = 3;
                else type = 1;
                NativeApp.setAspectRatio(type);
            });
        }

        // OSD toggles
        View swFps = view.findViewById(R.id.switch_osd_fps);
        if (swFps instanceof android.widget.Switch) {
            ((android.widget.Switch) swFps).setChecked(false);
            ((android.widget.Switch) swFps).setOnCheckedChangeListener((buttonView, isChecked) ->
                    NativeApp.setSetting("EmuCore/GS", "OsdShowFPS", "bool", isChecked ? "true" : "false"));
        }
        View swRes = view.findViewById(R.id.switch_osd_res);
        if (swRes != null) swRes.setVisibility(View.GONE);
        View swStats = view.findViewById(R.id.switch_osd_stats);
        if (swStats != null) swStats.setVisibility(View.GONE);

        View swHw = view.findViewById(R.id.switch_hw_readbacks);
        if (swHw instanceof android.widget.Switch) {
            ((android.widget.Switch) swHw).setChecked(true);
            ((android.widget.Switch) swHw).setOnCheckedChangeListener((buttonView, isChecked) ->
                    NativeApp.setSetting("EmuCore/GS", "HardwareReadbacks", "bool", isChecked ? "true" : "false"));
        }

        View btnImportMc = view.findViewById(R.id.btn_import_memcard);
        if (btnImportMc != null) {
            btnImportMc.setOnClickListener(v -> {
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
                i.setType("application/octet-stream");
                String[] types = new String[]{"application/octet-stream", "application/x-binary"};
                i.putExtra(Intent.EXTRA_MIME_TYPES, types);
                startActivityForResult(Intent.createChooser(i, "Select memory card"), 9911);
            });
        }

    View btnAbout = view.findViewById(R.id.btn_about);
    if (btnAbout != null) {
        btnAbout.setOnClickListener(v -> {
        String versionName = "";
        try { versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName; } catch (Exception ignored) {}
        String message = "ARMSX2 (" + versionName + ")\n" +
            "by ARMSX2 team\n\n" +
            "Core contributors:\n" +
            "- MoonPower — App developer\n" +
            "- jpolo — Management\n" +
            "- Medievalshell — Web developer\n" +
            "- set l — Web developer\n" +
            "- Alex — QA tester\n" +
            "- Yua — QA tester\n\n" +
            "Thanks to:\n" +
            "- pontos2024 (emulator base)\n" +
            "- PCSX2 v2.3.430 (core emulator)\n" +
            "- SDL (SDL3)\n" +
            "- Fffathur (icon design)\n" +
            "- vivimagic0 (icon design)";
        new MaterialAlertDialogBuilder(this)
            .setTitle("About")
            .setMessage(message)
            .setPositiveButton("OK", (d, w) -> d.dismiss())
            .show();
        });
    }

        dialog.show();
    }

    public final ActivityResultLauncher<Intent> startActivityResultLocalFilePlay = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    try {
                        Intent _intent = result.getData();
                        if(_intent != null) {
                            Uri picked = _intent.getData();
                            if (picked != null) {
                                GameSettingsApplier.applyPerGameSettingsForUri(picked, this);
                                m_szGamefile = picked.toString();
                                if(!TextUtils.isEmpty(m_szGamefile)) {
                                    handleSelectedGameUri(picked);
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
            });

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 7722 && resultCode == Activity.RESULT_OK && data != null) {
            if (data.hasExtra("SET_RENDERER")) {
                int r = data.getIntExtra("SET_RENDERER", -1000);
                if (r != -1000) {
                    applyRendererSelection(r);
                }
            }
        }
        if (requestCode == 9911 && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
            if (importMemcardToSlot1(uri)) {
                NativeApp.setSetting("MemoryCards", "Slot1_Enable", "bool", "false");
                NativeApp.setSetting("MemoryCards", "Slot1_Filename", "string", "Mcd001.ps2");
                NativeApp.setSetting("MemoryCards", "Slot1_Enable", "bool", "true");
                Toast.makeText(this, "Memory card inserted (Slot 1)", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to import memory card", Toast.LENGTH_LONG).show();
            }
        }
    }

    private final ActivityResultLauncher<Intent> startActivityResultPickBios = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    saveBiosFromUri(result.getData().getData());
                }
            });

    private final ActivityResultLauncher<Intent> startActivityResultImportBios = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    Uri uri = result.getData().getData();
                    try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
                    importBiosFromUri(uri);
                    showBiosManagerDialog();
                }
            });
    private final ActivityResultLauncher<Intent> startActivityResultOnboarding =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                onboardingLaunched = false;
                if (result.getResultCode() == Activity.RESULT_OK) {
                    setOnboardingComplete();
                    runPostOnboardingPrompts();
                } else if (!isOnboardingComplete()) {
                    maybeStartOnboardingFlow();
                }
            });

    @Override
    public void onConfigurationChanged(@NonNull Configuration p_newConfig) {
        super.onConfigurationChanged(p_newConfig);
        applyGameGridConfig();
    }

    @Override
    protected void onPause() {
        RetroAchievementsBridge.setListener(null);
        NativeApp.pause();
        isVmPaused = true;
        updatePauseButtonIcon();
        super.onPause();
        ////
        if (mHIDDeviceManager != null) {
            mHIDDeviceManager.setFrozen(true);
        }
    }

    @Override
    protected void onResume() {
        NativeApp.resume();
        isVmPaused = false;
        updatePauseButtonIcon();
        super.onResume();
        RetroAchievementsBridge.setListener(retroAchievementsListener);
        RetroAchievementsBridge.refreshState();
        DiscordBridge.updateEngineActivity(this);
        ////
        if (mHIDDeviceManager != null) {
            mHIDDeviceManager.setFrozen(false);
        }
        // Re-apply immersive fullscreen when resuming
        applyFullscreen();
        loadHideTimeoutFromPrefs();
        refreshOnScreenUiStyleIfNeeded();
        refreshOnScreenUiScaleIfNeeded();
    }

	@Override
	protected void onDestroy() {
		stopEmuThread();
		LogcatRecorder.shutdown();
		super.onDestroy();
		////
		if (mHIDDeviceManager != null) {
			HIDDeviceManager.release(mHIDDeviceManager);
			mHIDDeviceManager = null;
        }
        ////
        mEmulationThread = null;
    }

    /// ///////////////////////////////////////////////////////////////////////////////////////////

    public void Initialize() {
        File dataDir = DataDirectoryManager.getDataRoot(getApplicationContext());
        if (dataDir != null) {
            NativeApp.setDataRootOverride(dataDir.getAbsolutePath());
        }
        NativeApp.initializeOnce(getApplicationContext());
        LogcatRecorder.initialize(getApplicationContext());
        boolean recordLogs = false;
        try {
            String current = NativeApp.getSetting("Logging", "RecordAndroidLog", "bool");
            recordLogs = "true".equalsIgnoreCase(current);
        } catch (Exception ignored) {}
        LogcatRecorder.setEnabled(recordLogs);

		// Set up JNI
		SDLControllerManager.nativeSetupJNI();

		// Initialize state
        SDLControllerManager.initialize();

        mHIDDeviceManager = HIDDeviceManager.acquire(this);
    }

    private boolean isOnboardingComplete() {
        return getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(PREF_ONBOARDING_COMPLETE, false);
    }

    private void setOnboardingComplete() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(PREF_ONBOARDING_COMPLETE, true).apply();
    }

    private void maybeStartOnboardingFlow() {
        if (postOnboardingChecksRun) {
            return;
        }
        if (isOnboardingComplete()) {
            runPostOnboardingPrompts();
            return;
        }
        if (onboardingLaunched) {
            return;
        }
        try {
            Intent onboardingIntent = new Intent(this, OnboardingActivity.class);
            startActivityResultOnboarding.launch(onboardingIntent);
            onboardingLaunched = true;
        } catch (Throwable t) {
            setOnboardingComplete();
            runPostOnboardingPrompts();
        }
    }

    private void runPostOnboardingPrompts() {
        if (postOnboardingChecksRun) {
            return;
        }
        postOnboardingChecksRun = true;
        ensureBiosPresent();
        maybeShowDataDirectoryPrompt();
    }

    private void maybeShowDataDirectoryPrompt() {
        if (storagePromptShown) {
            return;
        }
        if (DataDirectoryManager.isPromptDone(this)) {
            return;
        }
        storagePromptShown = true;
        new MaterialAlertDialogBuilder(this)
                .setTitle("Storage location")
                .setMessage("Do you wish to change where the emulator stores its data?")
                .setNegativeButton("Cancel", (dialog, which) -> {
                    DataDirectoryManager.markPromptDone(this);
                    dialog.dismiss();
                })
                .setPositiveButton("Choose", (dialog, which) -> {
                    dialog.dismiss();
                    launchDataDirectoryPicker();
                })
                .setOnDismissListener(dialog -> storagePromptShown = true)
                .show();
    }

    private void launchDataDirectoryPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityResultPickDataDir.launch(intent);
    }

    private void handleDataDirectorySelection(@NonNull Uri tree) {
        String resolvedPath = DataDirectoryManager.resolveTreeUriToPath(this, tree);
        if (resolvedPath == null || resolvedPath.trim().isEmpty()) {
            try {
                Toast.makeText(this, "Unable to use selected folder", Toast.LENGTH_LONG).show();
            } catch (Throwable ignored) {}
            storagePromptShown = false;
            maybeShowDataDirectoryPrompt();
            return;
        }
        File targetDir = new File(resolvedPath);
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            try {
                Toast.makeText(this, "Cannot create folders in the selected location", Toast.LENGTH_LONG).show();
            } catch (Throwable ignored) {}
            storagePromptShown = false;
            maybeShowDataDirectoryPrompt();
            return;
        }
        if (!DataDirectoryManager.canUseDirectFileAccess(targetDir)) {
            showStorageAccessError(targetDir);
            storagePromptShown = false;
            maybeShowDataDirectoryPrompt();
            return;
        }
        File currentDir = DataDirectoryManager.getDataRoot(getApplicationContext());
        if (currentDir != null && currentDir.getAbsolutePath().equals(targetDir.getAbsolutePath())) {
            DataDirectoryManager.storeCustomDataRoot(getApplicationContext(), targetDir.getAbsolutePath(), tree.toString());
            NativeApp.setDataRootOverride(targetDir.getAbsolutePath());
            DataDirectoryManager.markPromptDone(this);
            storagePromptShown = true;
            try {
                Toast.makeText(this, "Already using that folder", Toast.LENGTH_SHORT).show();
            } catch (Throwable ignored) {}
            return;
        }
        beginDataDirectoryMigration(currentDir, targetDir, tree.toString());
    }

    private void beginDataDirectoryMigration(@NonNull File currentDir, @NonNull File targetDir, @NonNull String uriString) {
        showDataDirProgressDialog();
        NativeApp.pause();
        NativeApp.shutdown();
        new Thread(() -> {
            boolean success = DataDirectoryManager.migrateData(currentDir, targetDir);
			if (success) {
				DataDirectoryManager.storeCustomDataRoot(getApplicationContext(), targetDir.getAbsolutePath(), uriString);
				NativeApp.setDataRootOverride(targetDir.getAbsolutePath());
				NativeApp.reinitializeDataRoot(targetDir.getAbsolutePath());
				LogcatRecorder.handleDataRootChanged();
				DataDirectoryManager.copyAssetAll(getApplicationContext(), "resources");
			}
            runOnUiThread(() -> {
                dismissDataDirProgressDialog();
                if (success) {
                    DataDirectoryManager.markPromptDone(this);
                    storagePromptShown = true;
                    try {
                        Toast.makeText(this, "Data location updated", Toast.LENGTH_LONG).show();
                    } catch (Throwable ignored) {}
                } else {
                    try {
                        Toast.makeText(this, "Failed to move data", Toast.LENGTH_LONG).show();
                    } catch (Throwable ignored) {}
                    storagePromptShown = false;
                    maybeShowDataDirectoryPrompt();
                }
            });
        }, "DataDirMigration").start();
    }

    private void showDataDirProgressDialog() {
        runOnUiThread(() -> {
            if (dataDirProgressDialog != null && dataDirProgressDialog.isShowing()) {
                return;
            }
            ProgressBar progressBar = new ProgressBar(this);
            int padding = dpToPx(24);
            progressBar.setPadding(padding, padding, padding, padding);
            dataDirProgressDialog = new MaterialAlertDialogBuilder(this)
                    .setTitle("Moving data")
                    .setMessage("Moving emulator data to the selected folder…")
                    .setView(progressBar)
                    .setCancelable(false)
                    .create();
            dataDirProgressDialog.show();
        });
    }

    private void dismissDataDirProgressDialog() {
        runOnUiThread(() -> {
            if (dataDirProgressDialog != null) {
                dataDirProgressDialog.dismiss();
                dataDirProgressDialog = null;
            }
        });
    }

    private void setSurfaceView(Object p_value) {
        FrameLayout fl_board = findViewById(R.id.fl_board);
        if (fl_board != null) {
            if (fl_board.getChildCount() > 0) {
                fl_board.removeAllViews();
            }
            ////
            if (p_value instanceof SDLSurface) {
                fl_board.addView((SDLSurface) p_value);
            }
        }
    }

    public synchronized void startEmuThread() {
        if (!hasBios()) {
            ensureBiosPresent();
            return;
        }
        stopEmuThread(false);
        for (int attempts = 0; attempts < 40 && NativeApp.hasValidVm(); attempts++) {
            SystemClock.sleep(50);
        }
        if (NativeApp.hasValidVm()) {
            NativeApp.shutdown();
            SystemClock.sleep(100);
            if (NativeApp.hasValidVm()) {
                DebugLog.w("VM", "VM still reporting active after shutdown; proceeding with clean boot");
            }
        }
        try { NativeApp.resetKeyStatus(); } catch (Throwable ignored) {}
        if (isThread()) {
            return;
        }
        isVmPaused = false;
        updatePauseButtonIcon();
        mEmulationThread = new Thread(() -> {
            runOnUiThread(() -> {
                try { if (NativeApp.isFullscreenUIEnabled()) setOnScreenControlsVisible(true); } catch (Throwable ignored) {}
                try {
                    String p = m_szGamefile;
                    if (p != null && !p.isEmpty()) {
                        Toast.makeText(this, "Launching: " + p, Toast.LENGTH_SHORT).show();
                    }
                } catch (Throwable ignored) {}
            });
            NativeApp.runVMThread(m_szGamefile);
        });
        mEmulationThread.start();
    }

    private void stopEmuThread() {
        stopEmuThread(true);
    }

    private synchronized void stopEmuThread(boolean forceShutdown) {
        if (mEmulationThread != null) {
            NativeApp.shutdown();
            try {
                mEmulationThread.join();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            mEmulationThread = null;
        } else if (forceShutdown) {
            NativeApp.shutdown();
        }
        try { NativeApp.resetKeyStatus(); } catch (Throwable ignored) {}
        setFastForwardEnabled(false);
        isVmPaused = false;
        updatePauseButtonIcon();
    }

    private void restartEmuThread() {
        startEmuThread();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////

    private void handleSelectedGameUri(@NonNull Uri uri) {
        String scheme = uri.getScheme();
        String lastSeg = uri.getLastPathSegment();
        String mime = null;
        try { mime = getContentResolver().getType(uri); } catch (Exception ignored) {}
        boolean hasChdSuffix = (lastSeg != null && lastSeg.toLowerCase().endsWith(".chd")) ||
                (m_szGamefile.toLowerCase().endsWith(".chd"));

        boolean headerSaysChd = false;
        byte[] header = readFirstBytes(uri, 16);
        if (header != null && header.length >= 8) {
            String hv = new String(header, 0, 8);
            headerSaysChd = "MComprHD".equals(hv);
        }

        if ("content".equals(scheme)) {
            try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Throwable ignored) {}
            m_szGamefile = uri.toString();
            try { lastInput = InputSource.TOUCH; lastTouchTimeMs = System.currentTimeMillis(); setOnScreenControlsVisible(true); } catch (Throwable ignored) {}
            showHome(false);
            restartEmuThread();
            return;
        }

        m_szGamefile = uri.toString();
        try { lastInput = InputSource.TOUCH; lastTouchTimeMs = System.currentTimeMillis(); setOnScreenControlsVisible(true); } catch (Throwable ignored) {}
        showHome(false);
        restartEmuThread();
    }

    private String queryOpenableDisplayName(@NonNull Uri uri) {
        try (android.database.Cursor c = getContentResolver().query(uri, new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) return c.getString(idx);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String copyToCache(@NonNull Uri uri, @NonNull String fileName) {
        java.io.InputStream in = null;
        java.io.FileOutputStream out = null;
        try {
            java.io.File dir = new java.io.File(getCacheDir(), "games");
            if (!dir.exists()) 
                dir.mkdirs();
            String ext = "";
            int dot = fileName.lastIndexOf('.');
            if (dot > 0) ext = fileName.substring(dot);
            String base = java.util.Objects.toString(Integer.toHexString(uri.toString().hashCode()));
            java.io.File dst = new java.io.File(dir, base + ext);
            in = getContentResolver().openInputStream(uri);
            if (in == null) return null;
            out = new java.io.FileOutputStream(dst, false);
            byte[] buf = new byte[1024 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            out.flush();
            return dst.getAbsolutePath();
        } catch (Exception ignored) {
            return null;
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            try { if (out != null) out.close(); } catch (Exception ignored) {}
        }
    }

    private byte[] readFirstBytes(Uri uri, int count) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            if (in == null) return null;
            byte[] buf = new byte[count];
            int read = in.read(buf);
            in.close();
            if (read <= 0) return null;
            if (read < count) {
                byte[] cut = new byte[read];
                System.arraycopy(buf, 0, cut, 0, read);
                return cut;
            }
            return buf;
        } catch (Exception ignored) { return null; }
    }


    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        updateLastControllerDeviceId(event.getDeviceId());
        if (SDLControllerManager.isDeviceSDLJoystick(event.getDeviceId())) {
            SDLControllerManager.handleJoystickMotionEvent(event);
            if (mInputManager != null) {
                mInputManager.handleGamepadMotion(event);
            }
        }

        return super.onGenericMotionEvent(event);
    }

    @Override
    public boolean onKeyDown(int p_keyCode, KeyEvent p_event) {
        if ((p_event.getSource() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
            if (p_event.getRepeatCount() == 0) {
                updateLastControllerDeviceId(p_event.getDeviceId());
                SDLControllerManager.onNativePadDown(p_event.getDeviceId(), p_keyCode);
                forwardKeyToPad(true, p_keyCode);
                lastInput = InputSource.CONTROLLER;
                lastControllerTimeMs = System.currentTimeMillis();
                maybeAutoHideControls();
                return true;
            }
        } else {
            if (p_keyCode == KeyEvent.KEYCODE_BACK) {
                if (!isHomeVisible()) {
                    shutdownVmToHome();
                } else {
                    finish();
                }
                return true;
            }
        }
        return super.onKeyDown(p_keyCode, p_event);
    }

    @Override
    public void onBackPressed() {
        if (!isHomeVisible()) {
            shutdownVmToHome();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onKeyUp(int p_keyCode, KeyEvent p_event) {
        if ((p_event.getSource() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
            if (p_event.getRepeatCount() == 0) {
                updateLastControllerDeviceId(p_event.getDeviceId());
                SDLControllerManager.onNativePadUp(p_event.getDeviceId(), p_keyCode);
                forwardKeyToPad(false, p_keyCode);
                lastInput = InputSource.CONTROLLER;
                lastControllerTimeMs = System.currentTimeMillis();
                maybeAutoHideControls();
                return true;
            }
        }
        return super.onKeyUp(p_keyCode, p_event);
    }

    public void sendKeyAction(View p_view, int p_action, int p_keycode) {
        if(p_action == MotionEvent.ACTION_DOWN) {
            p_view.setPressed(true);
            int pad_force = 0;
            if (p_keycode >= 110) {
                float _abs = 90; 
                _abs = Math.min(_abs, 100);
                pad_force = (int) (_abs * 32766.0f / 100);
            }
            NativeApp.setPadButton(p_keycode, pad_force, true);
            lastInput = InputSource.TOUCH;
            lastTouchTimeMs = System.currentTimeMillis();
            maybeAutoHideControls();
        } else if(p_action == MotionEvent.ACTION_UP || p_action == MotionEvent.ACTION_CANCEL) {
            p_view.setPressed(false);
            NativeApp.setPadButton(p_keycode, 0, false);
        }
    }

    public void maybeAutoHideControls() {
        if (disableTouchControls) {
            setOnScreenControlsVisible(false);
            return;
        }
        if (lastInput == InputSource.CONTROLLER) {
            setOnScreenControlsVisible(false);
        } else {
            setOnScreenControlsVisible(true);
            getWindow().getDecorView().removeCallbacks(hideRunnable);
            if (hideDelayMs != 0L)
                getWindow().getDecorView().postDelayed(hideRunnable, hideDelayMs);
        }
    }

    private final Runnable hideRunnable = new Runnable() {
        @Override public void run() {
            if (hideDelayMs != 0L && lastInput == InputSource.TOUCH) {
                long dt = System.currentTimeMillis() - lastTouchTimeMs;
                if (dt >= hideDelayMs) setOnScreenControlsVisible(false);
            }
        }
    };

    private void setOnScreenControlsVisible(boolean visible) {
        if (disableTouchControls) {
            visible = false;
        }
        int vis = visible ? View.VISIBLE : View.GONE;
        if (llPadSelectStart != null) llPadSelectStart.setVisibility(vis);
        if (llPadRight != null) llPadRight.setVisibility(vis);
        View leftShoulders = findViewById(R.id.ll_pad_shoulders_left);
        if (leftShoulders != null) leftShoulders.setVisibility(vis);
        View rightShoulders = findViewById(R.id.ll_pad_shoulders_right);
        if (rightShoulders != null) rightShoulders.setVisibility(vis);
        
        JoystickView joystickLeft = findViewById(R.id.joystick_left);
        JoystickView joystickRight = findViewById(R.id.joystick_right);
        DPadView dpadView = findViewById(R.id.dpad_view);
        
        if (joystickLeft != null) {
            if (currentControllerMode == 2) {
                joystickLeft.setVisibility(View.GONE);
            } else {
                joystickLeft.setVisibility(vis);
            }
        }
        
        if (joystickRight != null) {
            if (currentControllerMode == 1 || currentControllerMode == 2) {
                joystickRight.setVisibility(View.GONE);
            } else {
                joystickRight.setVisibility(vis);
            }
        }
        
        if (dpadView != null) {
            if (currentControllerMode == 1) {
                dpadView.setVisibility(View.GONE);
            } else {
                dpadView.setVisibility(vis);
            }
        }
        
        if (!visible) {
            hideDrawerToggle();
        }
        if (!disableTouchControls && visible) {
            try {
                getWindow().getDecorView().removeCallbacks(hideRunnable);
                if (hideDelayMs != 0L && lastInput == InputSource.TOUCH) {
                    getWindow().getDecorView().postDelayed(hideRunnable, hideDelayMs);
                }
            } catch (Throwable ignored) {}
        } else if (disableTouchControls) {
            try {
                getWindow().getDecorView().removeCallbacks(hideRunnable);
            } catch (Throwable ignored) {}
        }
    }

    private void showDrawerToggleTemporarily() {
        if (disableTouchControls) {
            return;
        }
        if (drawerToggle == null) {
            return;
        }
        drawerToggle.removeCallbacks(hideDrawerToggleRunnable);
        drawerToggle.setVisibility(View.VISIBLE);
        long delay = hideDelayMs != 0L ? hideDelayMs : 2000L;
        drawerToggle.postDelayed(hideDrawerToggleRunnable, delay);
    }

    private void hideDrawerToggle() {
        if (drawerToggle == null) {
            return;
        }
        drawerToggle.removeCallbacks(hideDrawerToggleRunnable);
        drawerToggle.setVisibility(View.GONE);
    }

    static class SpacingDecoration extends RecyclerView.ItemDecoration {
        private int spacePx;
        SpacingDecoration(int spacePx) { this.spacePx = spacePx; }
        void updateSpacing(int spacePx) { this.spacePx = spacePx; }
        @Override public void getItemOffsets(@NonNull android.graphics.Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            outRect.left = spacePx;
            outRect.right = spacePx;
            outRect.top = spacePx;
            outRect.bottom = spacePx;
        }
    }

    private void applyGameGridConfig() {
        if (rvGames == null) return;
        final int span = getGameGridSpanCount();
        if (!listMode) {
            if (gamesGridLayoutManager == null) {
                gamesGridLayoutManager = new GridLayoutManager(this, span);
                rvGames.setLayoutManager(gamesGridLayoutManager);
            } else {
                gamesGridLayoutManager.setSpanCount(span);
                if (rvGames.getLayoutManager() != gamesGridLayoutManager) {
                    rvGames.setLayoutManager(gamesGridLayoutManager);
                }
            }
        }
        if (gameSpacingDecoration != null) {
            gameSpacingDecoration.updateSpacing(getResources().getDimensionPixelSize(R.dimen.game_selector_tile_spacing));
            rvGames.invalidateItemDecorations();
        }
        final int padding = getResources().getDimensionPixelSize(R.dimen.game_selector_grid_padding);
        rvGames.setPadding(padding, padding, padding, padding);
    }

    private int getGameGridSpanCount() {
        return getResources().getInteger(R.integer.game_selector_span_count);
    }

    private int dpToPx(int dp) { return Math.round(dp * getResources().getDisplayMetrics().density); }

    private void showStorageAccessError(File targetDir) {
        boolean canGrant = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !DataDirectoryManager.hasAllFilesAccess();
        String message = "Android denied direct file access for:\n" + targetDir.getAbsolutePath() +
                "\n\nGrant 'Allow access to all files' in system settings or choose a folder inside ARMSX2's storage.";
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
            .setTitle("Permission required")
            .setMessage(message)
            .setNegativeButton("OK", (d, w) -> d.dismiss());
        if (canGrant) {
            builder.setPositiveButton("Open settings", (d, w) -> {
                d.dismiss();
                openAllFilesAccessSettings();
            });
        }
        builder.show();
    }

    private void openAllFilesAccessSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception ignored) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                } catch (Exception ignored2) {}
            }
        }
    }

    private final ActivityResultLauncher<Intent> startActivityResultPickIso = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    Uri uri = result.getData().getData();
                    String name = queryOpenableDisplayName(uri);
                    String low = name != null ? name.toLowerCase() : uri.toString().toLowerCase();
                    if (!low.endsWith(".iso")) {
                        try { new MaterialAlertDialogBuilder(this).setTitle("Not an ISO").setMessage("Please select a .iso file.").setPositiveButton("OK", (d,w)-> d.dismiss()).show(); } catch (Throwable ignored) {}
                        return;
                    }
                    performIsoToChd(uri, name);
                }
            });

    private void startPickIsoForChd() {
        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            // ENSURE we find this shit
            i.setType("*/*");
            String[] mimeTypes = {
                "application/octet-stream",
                "application/x-iso9660-image", 
                "application/x-cd-image",
                "application/x-raw-disk-image"
            };
            i.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            startActivityResultPickIso.launch(i);
        } catch (Throwable t) {
            try { Toast.makeText(this, "Unable to open file picker", Toast.LENGTH_SHORT).show(); } catch (Throwable ignored) {}
        }
    }

    private void performIsoToChd(Uri isoUri, String isoDisplayName) {
        if (!NativeApp.hasNativeTools) {
            String errorMsg = "ARMSX2 Native Tools library could not be called, it was probably not bundled with the app please rebuild the app with the library in place.";
            android.util.Log.e("ARMSX2_CHD", "Library not available: " + errorMsg);
            try {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Library Not Available")
                        .setMessage(errorMsg)
                        .setPositiveButton("OK", (d, w) -> d.dismiss())
                        .show();
            } catch (Throwable ignored) {}
            return;
        }

        new Thread(() -> {
            String inputPath = null;
            String outputPath = null;
            String resultMessage = null;
            boolean success = false;

            try {
                // Get real file path from URI
                android.util.Log.i("ARMSX2_CHD", "Starting ISO to CHD conversion for: " + isoDisplayName);
                android.util.Log.i("ARMSX2_CHD", "Input URI: " + isoUri.toString());
                
                inputPath = getFilePathFromUri(isoUri);
                if (inputPath == null) {
                    resultMessage = "Could not access the selected ISO file. Please ensure the file is accessible.";
                    android.util.Log.e("ARMSX2_CHD", "Failed to get file path from URI: " + isoUri.toString());
                    return;
                }
                android.util.Log.i("ARMSX2_CHD", "Input path resolved to: " + inputPath);

                // Generate output CHD path to match what Rust will create 
                outputPath = inputPath.replaceAll("\\.iso$", ".chd");
                android.util.Log.i("ARMSX2_CHD", "Expected output path: " + outputPath);

                // Call native conversion 
                android.util.Log.i("ARMSX2_CHD", "Calling native conversion...");
                try {
                    android.util.Log.d("ARMSX2_CHD", "Input path bytes: " + java.util.Arrays.toString(inputPath.getBytes("UTF-8")));
                    android.util.Log.d("ARMSX2_CHD", "Input path length: " + inputPath.length());
                    android.util.Log.d("ARMSX2_CHD", "Input path string: '" + inputPath + "'");
                } catch (java.io.UnsupportedEncodingException e) {
                    android.util.Log.e("ARMSX2_CHD", "Failed to encode path as UTF-8: " + e.getMessage());
                }
                int result = NativeApp.convertIsoToChd(inputPath);
                android.util.Log.i("ARMSX2_CHD", "Native conversion returned code: " + result);
                
                success = handleConversionResult(result, inputPath, outputPath);
                
                if (success) {
                    final String chdCachePath = outputPath;
                    final String chdDisplayName = isoDisplayName;
                    android.util.Log.i("ARMSX2_CHD", "Conversion succeeded. Prompting user to choose CHD save location.");
                    runOnUiThread(() -> promptForChdSave(chdCachePath, chdDisplayName));
                    resultMessage = null;
                } else {
                    resultMessage = getErrorMessage(result) + "\n\nInput: " + inputPath + "\nOutput: " + outputPath;
                    android.util.Log.e("ARMSX2_CHD", "Conversion failed with code " + result + ": " + getErrorMessage(result));
                    android.util.Log.e("ARMSX2_CHD", "Input: " + inputPath);
                    android.util.Log.e("ARMSX2_CHD", "Output: " + outputPath);
                }

            } catch (Throwable e) {
                resultMessage = "Conversion failed with exception: " + e.getMessage() + 
                              "\n\nInput: " + inputPath + "\nOutput: " + outputPath;
                android.util.Log.e("ARMSX2_CHD", "Conversion exception: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
                android.util.Log.e("ARMSX2_CHD", "Input: " + inputPath);
                android.util.Log.e("ARMSX2_CHD", "Output: " + outputPath);
            } finally {
                if (inputPath != null) {
                    File tempFile = new File(inputPath);
                    if (tempFile.exists() && tempFile.getParent().equals(getCacheDir().getAbsolutePath())) {
                        if (tempFile.delete()) {
                            android.util.Log.d("ARMSX2_CHD", "Cleaned up temporary file: " + inputPath);
                        } else {
                            android.util.Log.w("ARMSX2_CHD", "Failed to clean up temporary file: " + inputPath);
                        }
                    }
                }
                
                final String finalMessage = resultMessage;
                final boolean finalSuccess = success;
                runOnUiThread(() -> {
                    if (finalMessage != null) {
                        showConversionResult(finalSuccess, finalMessage);
                    }
                });
            }
        }, "IsoToChdConverter").start();
    }

    private String getFilePathFromUri(Uri uri) {
        android.util.Log.d("ARMSX2_CHD", "getFilePathFromUri called with: " + uri.toString());
        try {
            // Get display name from content resolver
            android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int displayNameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                        if (displayNameIndex >= 0) {
                            String displayName = cursor.getString(displayNameIndex);
                            
                            File cacheDir = getCacheDir();
                            File tempFile = new File(cacheDir, displayName);
                            android.util.Log.d("ARMSX2_CHD", "Creating temporary file: " + tempFile.getAbsolutePath());
                            
                            try (java.io.InputStream input = getContentResolver().openInputStream(uri);
                                 java.io.FileOutputStream output = new java.io.FileOutputStream(tempFile)) {
                                
                                if (input != null) {
                                    byte[] buffer = new byte[8192];
                                    int bytesRead;
                                    long totalBytes = 0;
                                    while ((bytesRead = input.read(buffer)) != -1) {
                                        output.write(buffer, 0, bytesRead);
                                        totalBytes += bytesRead;
                                    }
                                    android.util.Log.d("ARMSX2_CHD", "Copied " + totalBytes + " bytes to cache");
                                    return tempFile.getAbsolutePath();
                                }
                            }
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Throwable e) {
            android.util.Log.e("ARMSX2_CHD", "Exception in getFilePathFromUri: " + e.getMessage(), e);
        }
        android.util.Log.w("ARMSX2_CHD", "getFilePathFromUri returning null - failed to resolve path");
        return null;
    }

    private void promptForChdSave(String chdCachePath, String displayName) {
        File chdFile = new File(chdCachePath);
        if (!chdFile.exists()) {
            android.util.Log.e("ARMSX2_CHD", "CHD file missing in cache, cannot prompt for save: " + chdCachePath);
            showConversionResult(false, "Converted file could not be found. Please try converting again.");
            return;
        }

        pendingChdCachePath = chdCachePath;
        pendingChdDisplayName = displayName;

        String baseName = displayName;
        if (baseName == null || baseName.trim().isEmpty()) {
            baseName = chdFile.getName();
        }
        String lower = baseName.toLowerCase(Locale.US);
        if (lower.endsWith(".iso")) {
            baseName = baseName.substring(0, baseName.length() - 4);
            lower = baseName.toLowerCase(Locale.US);
        }
        if (!lower.endsWith(".chd")) {
            baseName = baseName + ".chd";
        }

        android.util.Log.d("ARMSX2_CHD", "Prompting user to save CHD as: " + baseName);

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, baseName);

        startActivityResultSaveChd.launch(intent);
    }

    private boolean saveChdToUri(File chdFile, Uri destinationUri) {
        android.util.Log.d("ARMSX2_CHD", "Saving CHD from cache to destination: " + destinationUri);
        try (java.io.FileInputStream input = new java.io.FileInputStream(chdFile);
             java.io.OutputStream output = getContentResolver().openOutputStream(destinationUri, "w")) {

            if (output == null) {
                android.util.Log.e("ARMSX2_CHD", "Content resolver returned null output stream for destination");
                return false;
            }

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }
            output.flush();
            android.util.Log.d("ARMSX2_CHD", "Wrote " + totalBytes + " bytes to destination URI");
            return true;
        } catch (Throwable e) {
            android.util.Log.e("ARMSX2_CHD", "Failed to copy CHD to destination: " + e.getMessage(), e);
            return false;
        }
    }

    private boolean handleConversionResult(int result, String inputPath, String outputPath) {
        return result == 0; // All good!
    }

    private String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case -1: return "Error: Null pointer provided to conversion function";
            case -2: return "Error: Invalid UTF-8 encoding in file paths";
            case -3: return "Error: Input ISO file not found";
            case -4: return "Error: Input path is not a regular file";
            case -5: return "Error: Failed to create output CHD file";
            case -6: return "Error: I/O error during conversion";
            case -7: return "Error: Too many hunks for CHD format";
            case -8: return "Error: Numeric overflow during conversion";
            case -9: return "Error: Unexpected end of ISO data";
            case -100: return "Error: Internal conversion error";
            default: return "Error: Unknown conversion error (code: " + errorCode + ")";
        }
    }

    private void showConversionResult(boolean success, String message) {
        try {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(success ? "Conversion Successful" : "Conversion Failed")
                    .setMessage(message)
                    .setPositiveButton("OK", (d, w) -> d.dismiss())
                    .show();
        } catch (Throwable ignored) {}
    }

    private void loadHideTimeoutFromPrefs() {
        try {
            android.content.SharedPreferences sp = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            int sec = sp.getInt(PREF_HIDE_CONTROLS_SECONDS, 10); 
            if (sec < 0) sec = 0;
            if (sec > 60) sec = 60;
            hideDelayMs = (sec == 0) ? 0L : sec * 1000L;
        } catch (Throwable ignored) { hideDelayMs = 2500L; }
    }

    private void forwardKeyToPad(boolean down, int keycode) {
        int mapped = ControllerMappingManager.getPadCodeForKey(keycode);
        if (mapped == ControllerMappingManager.NO_MAPPING) {
            mapped = keycode;
        }
        NativeApp.setPadButton(mapped, 0, down);
    }







    private void refreshVibrationPreference() {
        boolean enabled = true;
        try {
            String vibration = NativeApp.getSetting("Pad1", "Vibration", "bool");
            if (vibration != null && !vibration.isEmpty()) {
                enabled = !"false".equalsIgnoreCase(vibration);
            } else {
                NativeApp.setSetting("Pad1", "Vibration", "bool", "true");
                enabled = true;
            }
        } catch (Exception ignored) {}
        setVibrationPreference(enabled);
    }

    private final ActivityResultLauncher<Intent> startActivityResultPickDataDir = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    Uri tree = data.getData();
                    if (tree != null) {
                        final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        try {
                            getContentResolver().takePersistableUriPermission(tree, takeFlags);
                        } catch (SecurityException ignored) {}
                        handleDataDirectorySelection(tree);
                        return;
                    }
                }
                storagePromptShown = false;
                maybeShowDataDirectoryPrompt();
            });

    //////////////////////////////////////////////////////////////////////////////////////////////

    // HOME FLOW
    private final ActivityResultLauncher<Intent> startActivityResultPickFolder = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    Uri tree = data.getData();
                    if (tree != null) {
                        try {
                            final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            getContentResolver().takePersistableUriPermission(tree, takeFlags);
                        } catch (SecurityException ignored) {}
                        gamesFolderUri = tree;
                        try {
                            getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                                .edit().putString(PREF_GAMES_URI, tree.toString()).apply();
                        } catch (Throwable ignored) {}
                        scanGamesFolder(tree);
                    }
                }
            });

    private final ActivityResultLauncher<Intent> startActivityResultImportCheats = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        persistUriPermission(uri);
                        importCheatFile(uri);
                    }
                }
            });

    private final ActivityResultLauncher<Intent> startActivityResultImportTextures = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        persistUriPermission(uri);
                        importTextureArchive(uri);
                    }
                }
            });

    private void pickGamesFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityResultPickFolder.launch(intent);
    }

    private void launchCheatImportPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.drawer_import_cheats_picker_title));
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/x-pnach", "application/octet-stream", "text/plain"});
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityResultImportCheats.launch(intent);
    }

    private void launchTextureImportPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.drawer_import_textures_picker_title));
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/zip", "application/x-zip-compressed"});
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityResultImportTextures.launch(intent);
    }

    private void scanGamesFolder(Uri folder) {
    List<GameEntry> entries = GameScanner.scanFolder(this, folder);
        try {
            java.util.Collections.sort(entries, (a, b) -> {
                String ta = a != null ? (a.title != null ? a.title : "") : "";
                String tb = b != null ? (b.title != null ? b.title : "") : "";
                int ga = sortGroup(ta);
                int gb = sortGroup(tb);
                if (ga != gb) return Integer.compare(ga, gb);
                return ta.compareToIgnoreCase(tb);
            });
        } catch (Throwable ignored) {}
    gamesAdapter.update(entries);
        final List<GameEntry> toResolve = new ArrayList<>();
        for (GameEntry ge : entries) {
            try {
                if (ge != null && (ge.serial == null || ge.serial.isEmpty())) {
                    String name = ge.title != null ? ge.title.toLowerCase() : "";
                    if (name.endsWith(".iso") || name.endsWith(".img") || name.endsWith(".bin"))
                        toResolve.add(ge);
                }
            } catch (Throwable ignored) {}
        }
        if (!toResolve.isEmpty()) {
            new Thread(() -> {
                android.content.ContentResolver cr = getContentResolver();
                int n = 0;
                for (GameEntry ge : toResolve) {
                    try {
                        RedumpDB.Result rd = RedumpDB.lookupByFile(cr, ge.uri);
                        if (rd != null && rd.serial != null && !rd.serial.isEmpty()) {
                            ge.serial = rd.serial;
                            ge.gameTitle = rd.name;
                            n++;
                            if (n % 2 == 1) {
                                runOnUiThread(() -> gamesAdapter.notifyDataSetChanged());
                            }
                        }
                    } catch (Throwable ignored) {}
                }
                if (n > 0) runOnUiThread(() -> gamesAdapter.notifyDataSetChanged());
            }, "RedumpResolve").start();
        }
        if (etSearch != null && etSearch.getText() != null && etSearch.length() > 0) {
            gamesAdapter.setFilter(etSearch.getText().toString());
        }
        if (rvGames != null && gamesAdapter.getItemCount() > 0) {
            rvGames.post(() -> {
                rvGames.requestFocus(); 
                rvGames.postDelayed(() -> {
                    RecyclerView.ViewHolder vh = rvGames.findViewHolderForAdapterPosition(0);
                    if (vh != null && vh.itemView != null) {
                        vh.itemView.requestFocus();
                    }
                }, 100); 
            });
        }
        boolean empty = entries.isEmpty();
    try { Toast.makeText(this, "Found " + entries.size() + " game(s)", Toast.LENGTH_SHORT).show(); } catch (Throwable ignored) {}
        if (tvEmpty != null) {
            tvEmpty.setText(empty ? "No games detected in this folder" : "");
            tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        }
        if (emptyContainer != null) emptyContainer.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (rvGames != null) rvGames.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (!empty) showHome(true);

    }

    private static int sortGroup(String title) {
        if (title == null) return 2;
        String t = title.trim();
        if (t.isEmpty()) return 2;
        char c = t.charAt(0);
        if (c >= '0' && c <= '9') return 0; 
        if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) return 1; 
        return 2;
    }

    private void showHome(boolean show) {
        if (show) {
            restorePerGameOverrides();
        }

        if (homeContainer != null) {
            homeContainer.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (drawerLayout != null) {
            drawerLayout.setVisibility(show ? View.VISIBLE : View.GONE);
            try {
                drawerLayout.setDrawerLockMode(show ? DrawerLayout.LOCK_MODE_UNLOCKED : DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                drawerLayout.setScrimColor(android.graphics.Color.TRANSPARENT);
            } catch (Throwable ignored) {}
        }
        if (inGameDrawer != null) {
            if (show) {
                try {
                    inGameDrawer.closeDrawer(GravityCompat.START);
                } catch (Throwable ignored) {}
                inGameDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            } else {
                inGameDrawer.setDrawerLockMode(disableTouchControls ? DrawerLayout.LOCK_MODE_LOCKED_CLOSED : DrawerLayout.LOCK_MODE_UNLOCKED);
            }
        }
        if (show) {
            setFastForwardEnabled(false);
            if (rvGames != null && rvGames.getVisibility() == View.VISIBLE && gamesAdapter != null && gamesAdapter.getItemCount() > 0) {
                rvGames.post(() -> {
                    rvGames.requestFocus();
                    RecyclerView.ViewHolder vh = rvGames.findViewHolderForAdapterPosition(0);
                    if (vh != null && vh.itemView != null) {
                        vh.itemView.requestFocus();
                    }
                });
            }
        }
        if (show || disableTouchControls) {
            hideDrawerToggle();
        }
        int vis = show ? View.GONE : View.VISIBLE;
        setOnScreenControlsVisible(!show);
        if (llPadSelectStart != null) llPadSelectStart.setVisibility(vis);
        if (llPadRight != null) llPadRight.setVisibility(vis);
        View j = findViewById(R.id.joystick_left);
        if (j != null) j.setVisibility(vis);
        View jr = findViewById(R.id.joystick_right);
        if (jr != null) jr.setVisibility(vis);
        View d = findViewById(R.id.dpad_view);
        if (d != null) d.setVisibility(vis);
        try {
            if (show) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
            } else {
                if (TextUtils.isEmpty(m_szGamefile)) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                }
            }
        } catch (Throwable ignored) {}
        if (show && tvEmpty != null && gamesFolderUri == null) {
            tvEmpty.setText("Choose a games folder");
            tvEmpty.setVisibility(View.VISIBLE);
            if (emptyContainer != null) emptyContainer.setVisibility(View.VISIBLE);
            if (rvGames != null) rvGames.setVisibility(View.GONE);
        }
        if (bgImage != null) {
            if (show) {
                android.graphics.drawable.Drawable ddraw = bgImage.getDrawable();
                bgImage.setVisibility(ddraw != null ? View.VISIBLE : View.GONE);
            } else {
                bgImage.setVisibility(View.GONE);
            }
        }
        applyFullscreen();
    }

    private boolean isHomeVisible() {
        return homeContainer == null || homeContainer.getVisibility() == View.VISIBLE;
    }

    private void shutdownVmToHome() {
        pendingGameUri = null;
        try {
            getWindow().getDecorView().removeCallbacks(pendingLaunchRunnable);
        } catch (Throwable ignored) {}
        stopEmuThread();
        m_szGamefile = "";
        showHome(true);
        lastInput = InputSource.TOUCH;
        lastTouchTimeMs = System.currentTimeMillis();
        setOnScreenControlsVisible(false);
        applyFullscreen();
        requestControllerRumble(0f, 0f);
        isVmPaused = false;
        updatePauseButtonIcon();
        setFastForwardEnabled(false);
    }

    private void enforceTouchControlsPolicy() {
        if (!disableTouchControls) {
            return;
        }
        setOnScreenControlsVisible(false);
        View joystick = findViewById(R.id.joystick_left);
        if (joystick != null) joystick.setVisibility(View.GONE);
        View joystickRight = findViewById(R.id.joystick_right);
        if (joystickRight != null) joystickRight.setVisibility(View.GONE);
        View dpad = findViewById(R.id.dpad_view);
        if (dpad != null) dpad.setVisibility(View.GONE);
        View padLeft = findViewById(R.id.ll_pad_select_start);
        if (padLeft != null) padLeft.setVisibility(View.GONE);
        View padRight = findViewById(R.id.ll_pad_right);
        if (padRight != null) padRight.setVisibility(View.GONE);
        View leftShoulders = findViewById(R.id.ll_pad_shoulders_left);
        if (leftShoulders != null) leftShoulders.setVisibility(View.GONE);
        View rightShoulders = findViewById(R.id.ll_pad_shoulders_right);
        if (rightShoulders != null) rightShoulders.setVisibility(View.GONE);
        hideDrawerToggle();
        setFastForwardEnabled(false);
        if (inGameDrawer != null) {
            try {
                inGameDrawer.closeDrawer(GravityCompat.START);
            } catch (Throwable ignored) {}
            inGameDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
    }

    // region Background image picker
    private static final String PREF_BG_L = "bg_landscape";
    private static final String PREF_BG_P = "bg_portrait";
    private void pickBackgroundImage(boolean portrait) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        i.setType("image/*");
        i.putExtra("PORTRAIT_BG", portrait);
        startActivityResultPickBg.launch(i);
    }
    private final ActivityResultLauncher<Intent> startActivityResultPickBg = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    Uri img = data.getData();
                    boolean portrait = data.getBooleanExtra("PORTRAIT_BG", false);
                    if (img != null) {
                        try {
                            final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            getContentResolver().takePersistableUriPermission(img, takeFlags);
                        } catch (SecurityException ignored) {}
                        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                                .putString(portrait ? PREF_BG_P : PREF_BG_L, img.toString())
                                .apply();
                        applySavedBackground();
                    }
                }
            });
    private void applySavedBackground() {
        if (bgImage == null) return;
        android.content.SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        String l = sp.getString(PREF_BG_L, null);
        String p = sp.getString(PREF_BG_P, null);
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        String use = isPortrait ? (p != null ? p : l) : (l != null ? l : p);
        if (use == null || use.isEmpty()) { bgImage.setImageDrawable(null); bgImage.setVisibility(View.GONE); return; }
        try (InputStream is = getContentResolver().openInputStream(Uri.parse(use))) {
            if (is != null) {
                android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(is);
                if (bmp != null) {
                    bgImage.setImageBitmap(bmp);
                    bgImage.setVisibility(View.VISIBLE);
                    if (android.os.Build.VERSION.SDK_INT >= 31) {
                        try {
                            bgImage.setRenderEffect(android.graphics.RenderEffect.createBlurEffect(0f, 8f, android.graphics.Shader.TileMode.CLAMP));
                        } catch (Throwable ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {}
    }
    private void clearBackgroundImages() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove(PREF_BG_L).remove(PREF_BG_P).apply();
        if (bgImage != null) { bgImage.setImageDrawable(null); bgImage.setVisibility(View.GONE); }
        try { Toast.makeText(this, "Background cleared.", Toast.LENGTH_SHORT).show(); } catch (Throwable ignored) {}
    }
    // endregion Background image picker

    private void onGameSelected(GameEntry entry) {
        launchGameWithPreflight(entry.uri);
    }

    // Cheap but effective: if emulator isn't running yet, boot BIOS first, then load the game like the File button flow.
    private void launchGameWithPreflight(@NonNull Uri uri) {
        GameSettingsApplier.applyPerGameSettingsForUri(uri, this);
        if (isThread()) {
            handleSelectedGameUri(uri);
            return;
        }
        // Start BIOS first
        try { Toast.makeText(this, "Preflight: booting BIOS…", Toast.LENGTH_SHORT).show(); } catch (Throwable ignored) {}
        pendingGameUri = uri;
        int pendingLaunchRetries = 0;
        bootBios();
        getWindow().getDecorView().postDelayed(pendingLaunchRunnable, 900);
        schedulePreflightFallback();
    }

    private final Runnable pendingLaunchRunnable = new Runnable() {
        @Override public void run() {
            if (pendingGameUri == null) return;
            try { Toast.makeText(MainActivity.this, "Preflight: launching selected game…", Toast.LENGTH_SHORT).show(); } catch (Throwable ignored) {}
            Uri toLaunch = pendingGameUri;
            pendingGameUri = null;
            handleSelectedGameUri(toLaunch);
        }
    };

    private void schedulePreflightFallback() {
        try {
            getWindow().getDecorView().postDelayed(() -> {
                if (pendingGameUri != null) {
                    Uri toLaunch = pendingGameUri;
                    pendingGameUri = null;
                    handleSelectedGameUri(toLaunch);
                }
            }, 2000);
        } catch (Throwable ignored) {}
    }

    private void bootBios() {
        m_szGamefile = "";
        showHome(false);
        try { setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR); } catch (Throwable ignored) {}
        try {
            lastInput = InputSource.TOUCH;
            lastTouchTimeMs = System.currentTimeMillis();
            setOnScreenControlsVisible(true);
        } catch (Throwable ignored) {}
        if (isThread()) {
            restartEmuThread();
        } else {
            startEmuThread();
        }
    }

    private android.graphics.Bitmap loadHeaderBitmapFromAssets() {
        try (java.io.InputStream is = getAssets().open("icon.png")) {
            return android.graphics.BitmapFactory.decodeStream(is);
        } catch (Exception ignored) {
            try (java.io.InputStream is2 = getAssets().open("app_icons/icon.png")) {
                return android.graphics.BitmapFactory.decodeStream(is2);
            } catch (Exception ignored2) { return null; }
        }
    }


    private android.graphics.Bitmap loadHeaderBlurBitmapFromAssets() {
        try (java.io.InputStream is = getAssets().open("app_icons/icon-old.png")) {
            return android.graphics.BitmapFactory.decodeStream(is);
        } catch (Exception ignored) {
            try (java.io.InputStream is2 = getAssets().open("app_icons/icon.png")) {
                return android.graphics.BitmapFactory.decodeStream(is2);
            } catch (Exception ignored2) {
                try (java.io.InputStream is3 = getAssets().open("icon.png")) {
                    return android.graphics.BitmapFactory.decodeStream(is3);
                } catch (Exception ignored3) { return null; }
            }
        }
    }

    public void dispatchControllerRumble(float large, float small) {
        if (!sVibrationEnabled) {
            mInputManager.stopControllerRumble();
            return;
        }
        final float clampedLarge = clamp01(large);
        final float clampedSmall = clamp01(small);
        final float combined = Math.max(clampedLarge, clampedSmall);
        final int deviceId = sLastControllerDeviceId;

        if (combined <= 0f) {
            mInputManager.stopControllerRumble();
            return;
        }

        boolean usedController = false;
        if (deviceId >= 0 && kr.co.iefriends.pcsx2.core.util.SDLControllerManager.isDeviceSDLJoystick(deviceId)) {
            usedController = true;
            try {
                kr.co.iefriends.pcsx2.core.util.SDLControllerManager.hapticRumble(deviceId, clampedLarge, clampedSmall, RUMBLE_DURATION_MS);
            } catch (Throwable ignored) {}
        }

        final int vibratorServiceId = 999999;
        if (!usedController) {
            try {
                kr.co.iefriends.pcsx2.core.util.SDLControllerManager.hapticRun(vibratorServiceId, combined, RUMBLE_DURATION_MS);
            } catch (Throwable ignored) {}
        }
    }
    public void stopControllerRumble() {
        kr.co.iefriends.pcsx2.core.input.InputManager.stopControllerRumbleStatic();
    }

}
