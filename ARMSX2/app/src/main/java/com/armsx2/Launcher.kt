package com.armsx2

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object Launcher {
    data class GameEntry(val uri: Uri, val displayName: String)

    private const val PREFS = "armsx2_launcher"
    private const val KEY_GAMES_FOLDER = "games_folder_uri"
    private const val KEY_LAST_PLAYED = "last_played"

    private val ps2Exts = setOf(
        "iso", "bin", "chd", "cso", "gz", "mdf", "nrg", "elf", "ciso", "bz2", "img", "dump"
    )

    val biosName = mutableStateOf<String?>(null)
    val biosReadable = mutableStateOf(false)
    val gamesFolderName = mutableStateOf<String?>(null)
    val games = mutableStateOf<List<GameEntry>>(emptyList())
    val selectedGame = mutableStateOf<GameEntry?>(null)
    val busy = mutableStateOf(false)
    val message = mutableStateOf<String?>(null)
    val lastPlayed = mutableStateOf<Map<String, Long>>(emptyMap())

    private lateinit var prefs: SharedPreferences
    private lateinit var appContext: Context

    lateinit var pickBios: ActivityResultLauncher<Intent>
    lateinit var pickFolder: ActivityResultLauncher<Intent>

    fun init(activity: ComponentActivity) {
        appContext = activity.applicationContext
        prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        pickBios = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { r: ActivityResult ->
            if (r.resultCode == Activity.RESULT_OK) {
                r.data?.data?.let { importBios(it) }
            }
        }
        pickFolder = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { r: ActivityResult ->
            if (r.resultCode == Activity.RESULT_OK) {
                r.data?.data?.let { setGamesFolder(it) }
            }
        }

        refreshBiosStatus()
        lastPlayed.value = loadLastPlayed()

        val stored = prefs.getString(KEY_GAMES_FOLDER, null)
        if (stored != null) {
            val uri = Uri.parse(stored)
            val hasPerm = try {
                appContext.contentResolver.persistedUriPermissions.any {
                    it.uri == uri && it.isReadPermission
                }
            } catch (_: Exception) { false }
            if (hasPerm) {
                gamesFolderName.value = queryTreeDisplayName(uri) ?: uri.lastPathSegment
                scanGames(uri)
            } else {
                prefs.edit { remove(KEY_GAMES_FOLDER) }
            }
        }
    }

    fun openBiosPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        pickBios.launch(intent)
    }

    fun refreshGames() {
        val stored = prefs.getString(KEY_GAMES_FOLDER, null) ?: return
        val uri = Uri.parse(stored)
        val hasPerm = try {
            appContext.contentResolver.persistedUriPermissions.any {
                it.uri == uri && it.isReadPermission
            }
        } catch (_: Exception) { false }
        if (hasPerm) scanGames(uri)
    }

    fun openFolderPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }
        pickFolder.launch(intent)
    }

    private fun importBios(src: Uri) {
        busy.value = true
        try {
            val biosDir = File(appContext.getExternalFilesDir(null), "bios")
            if (!biosDir.exists()) biosDir.mkdirs()

            val srcName = queryDocumentDisplayName(src) ?: "bios.bin"
            val base = if (srcName.endsWith(".bin", ignoreCase = true)) srcName else "$srcName.bin"
            val dst = File(biosDir, sanitizeName(base))

            appContext.contentResolver.openInputStream(src).use { input ->
                if (input == null) {
                    message.value = "Cannot open BIOS file"
                    return
                }
                FileOutputStream(dst).use { out ->
                    input.copyTo(out, bufferSize = 64 * 1024)
                }
            }

            dst.setReadable(true, false)

            refreshBiosStatus()
            message.value = "BIOS imported: ${dst.name}"
        } catch (e: Exception) {
            message.value = "BIOS import error: ${e.message}"
        } finally {
            busy.value = false
        }
    }

    private fun refreshBiosStatus() {
        val biosDir = File(appContext.getExternalFilesDir(null), "bios")
        val candidates = biosDir.listFiles { f ->
            f.isFile && f.length() in 4L * 1024 * 1024..8L * 1024 * 1024
        } ?: emptyArray()
        val readable = candidates.firstOrNull { it.canRead() }
        if (readable != null) {
            biosName.value = readable.name
            biosReadable.value = true
        } else {
            val unreadable = candidates.firstOrNull()
            biosName.value = unreadable?.name
            biosReadable.value = false
        }
    }

    private fun setGamesFolder(uri: Uri) {
        try {
            appContext.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {}
        prefs.edit { putString(KEY_GAMES_FOLDER, uri.toString()) }
        gamesFolderName.value = queryTreeDisplayName(uri) ?: uri.lastPathSegment
        scanGames(uri)
    }

    private fun scanGames(treeUri: Uri) {
        busy.value = true
        games.value = emptyList()
        selectedGame.value = null
        Thread {
            val collected = mutableListOf<GameEntry>()
            try {
                val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
                scanRecursive(treeUri, rootDocId, collected)
                val sorted = collected.sortedBy { it.displayName.lowercase() }
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    games.value = sorted
                    busy.value = false
                }
            } catch (e: Exception) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    message.value = "Games scan error: ${e.message}"
                    busy.value = false
                }
            }
        }.start()
    }

    private fun scanRecursive(treeUri: Uri, parentDocId: String, out: MutableList<GameEntry>) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val cr = appContext.contentResolver
        cr.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null, null, null
        )?.use { c ->
            while (c.moveToNext()) {
                val docId = c.getString(0) ?: continue
                val name = c.getString(1) ?: continue
                val mime = c.getString(2) ?: ""
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    scanRecursive(treeUri, docId, out)
                } else {
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (ext in ps2Exts) {
                        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        out.add(GameEntry(docUri, name))
                    }
                }
            }
        }
    }

    private fun queryDocumentDisplayName(uri: Uri): String? {
        return appContext.contentResolver.query(
            uri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null
        )?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }

    private fun queryTreeDisplayName(tree: Uri): String? {
        val docId = DocumentsContract.getTreeDocumentId(tree)
        val docUri = DocumentsContract.buildDocumentUriUsingTree(tree, docId)
        return queryDocumentDisplayName(docUri)
    }

    private fun sanitizeName(name: String): String {
        return name.replace(Regex("[^A-Za-z0-9._-]"), "_")
    }

    fun markPlayed(uriString: String) {
        if (uriString.isEmpty()) return
        val updated = lastPlayed.value.toMutableMap().also {
            it[uriString] = System.currentTimeMillis()
        }
        lastPlayed.value = updated
        saveLastPlayed(updated)
    }

    fun recentGames(max: Int): List<GameEntry> {
        val map = lastPlayed.value
        if (map.isEmpty()) return emptyList()
        return games.value
            .filter { it.uri.toString() in map }
            .sortedByDescending { map[it.uri.toString()] ?: 0L }
            .take(max)
    }

    private fun loadLastPlayed(): Map<String, Long> {
        val json = prefs.getString(KEY_LAST_PLAYED, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(json)
            obj.keys().asSequence().associateWith { obj.getLong(it) }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun saveLastPlayed(map: Map<String, Long>) {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit { putString(KEY_LAST_PLAYED, obj.toString()) }
    }
}
