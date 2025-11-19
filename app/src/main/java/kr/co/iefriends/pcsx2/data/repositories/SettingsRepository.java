package kr.co.iefriends.pcsx2.data.repositories;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import androidx.preference.PreferenceManager;

/**
 * Gestisce l'accesso alle preferenze utente (SharedPreferences) e le impostazioni native.
 * Tutti i metodi sono specifici per un'impostazione.
 */
public class SettingsRepository {

    private final SharedPreferences prefs;

    // --- CHIAVI DELLE PREFERENZE ESTRATTE ---
    // Queste restano private/static/final perché non dovrebbero essere usate direttamente
    // da ViewModels o Fragments, solo dal Repository stesso.
    private static final String PREF_SHOW_FPS_COUNT = "show_fps_count";
    private static final String PREF_HIDE_CONTROLS_SECONDS = "hide_controls_seconds";
    private static final String PREF_GAMES_FOLDER_URI = "games_folder_uri";
    // ... tutte le altre chiavi tagliate dalla MainActivity

    // --- Costruttore ---

    public SettingsRepository(Context context) {
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    // =========================================================
    // --- METODI PARLANTI PER LA LISTA DEI GIOCHI ---
    // =========================================================

    /**
     * @return L'URI della cartella dove l'utente ha salvato i giochi, o null.
     */
    public Uri getGamesFolderUri() {
        String uriString = prefs.getString(PREF_GAMES_FOLDER_URI, null);
        return uriString != null ? Uri.parse(uriString) : null;
    }

    /**
     * Salva il nuovo URI della cartella dei giochi.
     */
    public void saveGamesFolderUri(Uri uri) {
        prefs.edit().putString(PREF_GAMES_FOLDER_URI, uri.toString()).apply();
    }

    // =========================================================
    // --- METODI PARLANTI PER LE IMPOSTAZIONI IN-GAME ---
    // =========================================================

    /**
     * @return true se il contatore degli FPS deve essere mostrato.
     */
    public boolean shouldShowFpsCount() {
        // Estrai il valore predefinito dalla tua logica originale (es. false)
        return prefs.getBoolean(PREF_SHOW_FPS_COUNT, false); 
    }

    /**
     * Imposta se il contatore degli FPS deve essere mostrato.
     */
    public void setShowFpsCount(boolean show) {
        prefs.edit().putBoolean(PREF_SHOW_FPS_COUNT, show).apply();
    }

    /**
     * @return Il ritardo in secondi prima che i controlli virtuali scompaiano.
     */
    public int getControlsAutoHideSeconds() {
        // Estrai il valore predefinito
        return prefs.getInt(PREF_HIDE_CONTROLS_SECONDS, 5); 
    }

    // ... e così via per tutte le altre costanti che hai tagliato (es. getRenderResolutionX(), setRenderResolutionY(), ecc.)
}