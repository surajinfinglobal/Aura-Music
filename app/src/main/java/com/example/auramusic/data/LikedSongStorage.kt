package com.example.auramusic.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.auramusic.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class LikedSongEntity(
    val id: Int,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val duration: Int,
    val durationText: String,
    val fileUrl: String,
    val coverUrl: String,
    val localFilePath: String?,
    val isDownloaded: Boolean,
    val fileSize: Long,
    val likedAt: Long
)

class LikedSongsDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    companion object {
        const val DATABASE_NAME = "aura_liked_songs.db"
        const val DATABASE_VERSION = 1

        const val TABLE_LIKED_SONGS = "liked_songs"
        const val COLUMN_ID = "id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_ARTIST = "artist"
        const val COLUMN_ALBUM = "album"
        const val COLUMN_GENRE = "genre"
        const val COLUMN_DURATION = "duration"
        const val COLUMN_DURATION_TEXT = "duration_text"
        const val COLUMN_FILE_URL = "file_url"
        const val COLUMN_COVER_URL = "cover_url"
        const val COLUMN_LOCAL_PATH = "local_file_path"
        const val COLUMN_IS_DOWNLOADED = "is_downloaded"
        const val COLUMN_FILE_SIZE = "file_size"
        const val COLUMN_LIKED_AT = "liked_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableSql = """
            CREATE TABLE $TABLE_LIKED_SONGS (
                $COLUMN_ID INTEGER PRIMARY KEY,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_ARTIST TEXT NOT NULL,
                $COLUMN_ALBUM TEXT,
                $COLUMN_GENRE TEXT,
                $COLUMN_DURATION INTEGER,
                $COLUMN_DURATION_TEXT TEXT,
                $COLUMN_FILE_URL TEXT,
                $COLUMN_COVER_URL TEXT,
                $COLUMN_LOCAL_PATH TEXT,
                $COLUMN_IS_DOWNLOADED INTEGER DEFAULT 0,
                $COLUMN_FILE_SIZE INTEGER DEFAULT 0,
                $COLUMN_LIKED_AT INTEGER NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTableSql)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LIKED_SONGS")
        onCreate(db)
    }

    fun insertOrUpdate(
        song: Song,
        localPath: String? = null,
        isDownloaded: Boolean = false,
        fileSize: Long = 0L
    ) {
        val values = ContentValues().apply {
            put(COLUMN_ID, song.id)
            put(COLUMN_TITLE, song.title)
            put(COLUMN_ARTIST, song.artist)
            put(COLUMN_ALBUM, song.album)
            put(COLUMN_GENRE, song.genre.joinToString(","))
            put(COLUMN_DURATION, song.duration)
            put(COLUMN_DURATION_TEXT, song.durationText)
            put(COLUMN_FILE_URL, song.file)
            put(COLUMN_COVER_URL, song.cover)
            put(COLUMN_LOCAL_PATH, localPath)
            put(COLUMN_IS_DOWNLOADED, if (isDownloaded) 1 else 0)
            put(COLUMN_FILE_SIZE, fileSize)
            put(COLUMN_LIKED_AT, System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict(
            TABLE_LIKED_SONGS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun deleteLikedSong(songId: Int) {
        writableDatabase.delete(
            TABLE_LIKED_SONGS,
            "$COLUMN_ID = ?",
            arrayOf(songId.toString())
        )
    }

    fun isSongLiked(songId: Int): Boolean {
        val cursor = readableDatabase.query(
            TABLE_LIKED_SONGS,
            arrayOf(COLUMN_ID),
            "$COLUMN_ID = ?",
            arrayOf(songId.toString()),
            null,
            null,
            null
        )
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }

    fun getAllLikedSongs(): List<LikedSongEntity> {
        val list = mutableListOf<LikedSongEntity>()
        val cursor = readableDatabase.query(
            TABLE_LIKED_SONGS,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_LIKED_AT DESC"
        )
        while (cursor.moveToNext()) {
            val entity = LikedSongEntity(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
                artist = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ARTIST)),
                album = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ALBUM)) ?: "",
                genre = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENRE)) ?: "",
                duration = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DURATION)),
                durationText = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DURATION_TEXT)) ?: "0:00",
                fileUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FILE_URL)) ?: "",
                coverUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COVER_URL)) ?: "",
                localFilePath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCAL_PATH)),
                isDownloaded = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_DOWNLOADED)) == 1,
                fileSize = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_FILE_SIZE)),
                likedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LIKED_AT))
            )
            list.add(entity)
        }
        cursor.close()
        return list
    }

    fun updateDownloadInfo(songId: Int, localPath: String?, isDownloaded: Boolean, fileSize: Long) {
        val values = ContentValues().apply {
            put(COLUMN_LOCAL_PATH, localPath)
            put(COLUMN_IS_DOWNLOADED, if (isDownloaded) 1 else 0)
            put(COLUMN_FILE_SIZE, fileSize)
        }
        writableDatabase.update(
            TABLE_LIKED_SONGS,
            values,
            "$COLUMN_ID = ?",
            arrayOf(songId.toString())
        )
    }

    fun getTotalStorageUsed(): Long {
        val cursor = readableDatabase.rawQuery(
            "SELECT SUM($COLUMN_FILE_SIZE) FROM $TABLE_LIKED_SONGS WHERE $COLUMN_IS_DOWNLOADED = 1",
            null
        )
        var total = 0L
        if (cursor.moveToFirst()) {
            total = cursor.getLong(0)
        }
        cursor.close()
        return total
    }
}

class LikedSongStorageManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val dbHelper = LikedSongsDatabaseHelper(context)
    private val prefs = context.getSharedPreferences("aura_storage_prefs", Context.MODE_PRIVATE)

    private val storageDir: File = File(context.filesDir, "liked_songs").apply {
        if (!exists()) mkdirs()
    }

    private val _likedSongs = MutableStateFlow<List<LikedSongEntity>>(emptyList())
    val likedSongs: StateFlow<List<LikedSongEntity>> = _likedSongs.asStateFlow()

    private val _likedSongIds = MutableStateFlow<Set<Int>>(emptySet())
    val likedSongIds: StateFlow<Set<Int>> = _likedSongIds.asStateFlow()

    private val _downloadedSongIds = MutableStateFlow<Set<Int>>(emptySet())
    val downloadedSongIds: StateFlow<Set<Int>> = _downloadedSongIds.asStateFlow()

    private val _totalStorageBytes = MutableStateFlow(0L)
    val totalStorageBytes: StateFlow<Long> = _totalStorageBytes.asStateFlow()

    private val _isAutoSaveEnabled = MutableStateFlow(
        prefs.getBoolean("auto_save_liked_to_storage", true)
    )
    val isAutoSaveEnabled: StateFlow<Boolean> = _isAutoSaveEnabled.asStateFlow()

    private val _downloadingIds = MutableStateFlow<Set<Int>>(emptySet())
    val downloadingIds: StateFlow<Set<Int>> = _downloadingIds.asStateFlow()

    init {
        refreshState()
    }

    fun refreshState() {
        scope.launch {
            val list = dbHelper.getAllLikedSongs()
            // Verify files on disk still exist
            val verifiedList = list.map { entity ->
                if (entity.isDownloaded && entity.localFilePath != null) {
                    val file = File(entity.localFilePath)
                    if (!file.exists() || file.length() == 0L) {
                        dbHelper.updateDownloadInfo(entity.id, null, false, 0L)
                        entity.copy(localFilePath = null, isDownloaded = false, fileSize = 0L)
                    } else {
                        entity
                    }
                } else {
                    entity
                }
            }

            _likedSongs.value = verifiedList
            _likedSongIds.value = verifiedList.map { it.id }.toSet()
            _downloadedSongIds.value = verifiedList.filter { it.isDownloaded }.map { it.id }.toSet()
            _totalStorageBytes.value = dbHelper.getTotalStorageUsed()
        }
    }

    fun setAutoSaveEnabled(enabled: Boolean) {
        _isAutoSaveEnabled.value = enabled
        prefs.edit().putBoolean("auto_save_liked_to_storage", enabled).apply()
    }

    fun toggleLikeSong(song: Song) {
        scope.launch {
            if (dbHelper.isSongLiked(song.id)) {
                // Remove like
                dbHelper.deleteLikedSong(song.id)
                // Also remove downloaded local file if any
                val localFile = File(storageDir, "${song.id}.mp3")
                if (localFile.exists()) {
                    localFile.delete()
                }
                refreshState()
            } else {
                // Add like to SQLite database
                val localFile = File(storageDir, "${song.id}.mp3")
                val isAlreadyDownloaded = localFile.exists() && localFile.length() > 0
                dbHelper.insertOrUpdate(
                    song = song,
                    localPath = if (isAlreadyDownloaded) localFile.absolutePath else null,
                    isDownloaded = isAlreadyDownloaded,
                    fileSize = if (isAlreadyDownloaded) localFile.length() else 0L
                )
                refreshState()

                // Auto save audio to local storage if enabled
                if (_isAutoSaveEnabled.value && !isAlreadyDownloaded) {
                    saveSongAudioToLocalStorage(song)
                }
            }
        }
    }

    fun saveSongAudioToLocalStorage(song: Song) {
        if (_downloadingIds.value.contains(song.id)) return

        scope.launch {
            _downloadingIds.value = _downloadingIds.value + song.id
            try {
                val targetFile = File(storageDir, "${song.id}.mp3")

                // Download from URL to local storage file
                val success = withContext(Dispatchers.IO) {
                    try {
                        val url = URL(song.file)
                        val connection = (url.openConnection() as HttpURLConnection).apply {
                            connectTimeout = 15000
                            readTimeout = 30000
                            instanceFollowRedirects = true
                        }
                        connection.connect()

                        if (connection.responseCode in 200..299) {
                            connection.inputStream.use { input ->
                                FileOutputStream(targetFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            targetFile.exists() && targetFile.length() > 0
                        } else {
                            false
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        false
                    }
                }

                if (success) {
                    dbHelper.updateDownloadInfo(
                        songId = song.id,
                        localPath = targetFile.absolutePath,
                        isDownloaded = true,
                        fileSize = targetFile.length()
                    )
                }
            } finally {
                _downloadingIds.value = _downloadingIds.value - song.id
                refreshState()
            }
        }
    }

    fun removeSongAudioFromLocalStorage(songId: Int) {
        scope.launch {
            val targetFile = File(storageDir, "$songId.mp3")
            if (targetFile.exists()) {
                targetFile.delete()
            }
            dbHelper.updateDownloadInfo(
                songId = songId,
                localPath = null,
                isDownloaded = false,
                fileSize = 0L
            )
            refreshState()
        }
    }

    fun downloadAllLikedSongs(allSongs: List<Song>) {
        val songMap = allSongs.associateBy { it.id }
        val toDownload = _likedSongIds.value.filter { !_downloadedSongIds.value.contains(it) }

        for (id in toDownload) {
            val s = songMap[id]
            if (s != null) {
                saveSongAudioToLocalStorage(s)
            }
        }
    }

    fun getLocalAudioFile(songId: Int): File? {
        val file = File(storageDir, "$songId.mp3")
        return if (file.exists() && file.length() > 0) file else null
    }

    fun getFormattedStorageSize(): String {
        val bytes = _totalStorageBytes.value
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f GB", bytes / (1024f * 1024f * 1024f))
            bytes >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024f * 1024f))
            bytes >= 1024 -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024f)
            else -> "$bytes B"
        }
    }
}
