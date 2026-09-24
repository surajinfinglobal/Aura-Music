package com.example.auramusic.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.example.auramusic.model.Song
import com.example.auramusic.service.MusicPlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

enum class RepeatMode {
    OFF, ALL, ONE
}

class AudioPlayerManager(private val context: Context) {
    companion object {
        @Volatile
        private var instance: AudioPlayerManager? = null

        fun getInstance(context: Context): AudioPlayerManager {
            return instance ?: synchronized(this) {
                instance ?: AudioPlayerManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main)
    private var mediaPlayer: MediaPlayer? = null
    private var currentAssetFd: android.content.res.AssetFileDescriptor? = null
    private var progressJob: Job? = null
    private var downloadJob: Job? = null
    private var isPrepared = false

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _queueIndex = MutableStateFlow(0)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isPlayingFromLocalStorage = MutableStateFlow(false)
    val isPlayingFromLocalStorage: StateFlow<Boolean> = _isPlayingFromLocalStorage.asStateFlow()

    var onSongChangedListener: ((Song) -> Unit)? = null
    var localFileResolver: ((Int) -> java.io.File?)? = null

    fun playSong(
        song: Song,
        newQueue: List<Song> = emptyList(),
        startIndex: Int = -1
    ) {
        if (newQueue.isNotEmpty()) {
            _queue.value = newQueue
            val index = if (startIndex >= 0) startIndex else newQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            _queueIndex.value = index
        } else if (!_queue.value.any { it.id == song.id }) {
            _queue.value = _queue.value + song
            _queueIndex.value = _queue.value.lastIndex
        } else {
            _queueIndex.value = _queue.value.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        }

        // If clicking the current song which is already prepared, toggle playback smoothly
        if (_currentSong.value?.id == song.id && mediaPlayer != null && isPrepared) {
            togglePlayPause()
            return
        }

        _currentSong.value = song
        onSongChangedListener?.invoke(song)
        startPlayback(song)
    }

    private fun startPlayback(song: Song) {
        _isLoading.value = true
        _isPlaying.value = false
        _currentPositionMs.value = 0L
        _durationMs.value = (song.duration * 1000L).coerceAtLeast(1000L)
        _errorMessage.value = null
        stopProgressUpdates()
        isPrepared = false

        // Keep foreground service active for background playback
        try {
            MusicPlaybackService.startService(context)
        } catch (e: Exception) {
            // Ignore if service cannot be started
        }

        // Safely dispose old player and asset descriptor
        val oldPlayer = mediaPlayer
        mediaPlayer = null
        oldPlayer?.let {
            try {
                it.reset()
                it.release()
            } catch (e: Exception) {
                // Ignore safe cleanup
            }
        }
        try {
            currentAssetFd?.close()
        } catch (e: Exception) {
            // Ignore
        }
        currentAssetFd = null

        try {
            ensureAudioDeviceVolume()

            val localFile = localFileResolver?.invoke(song.id)
            val useLocal = localFile != null && localFile.exists() && localFile.length() > 0
            _isPlayingFromLocalStorage.value = useLocal

            val player = MediaPlayer()
            mediaPlayer = player

            // Safe wake lock attempt (does not crash if permission is denied)
            try {
                player.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
            } catch (e: Throwable) {
                // Ignore wake lock failure gracefully
            }

            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                    .build()
            )
            player.setVolume(1.0f, 1.0f)

            if (useLocal && localFile != null) {
                player.setDataSource(localFile.absolutePath)
            } else if (song.file.startsWith("music/") || !song.file.startsWith("http")) {
                // Play bundled audio file from assets.
                val assetPath = if (song.file.startsWith("music/")) song.file else "music/${song.file}"
                val cachedAssetFile = getOrCopyAssetToCache(assetPath)
                if (cachedAssetFile != null && cachedAssetFile.exists() && cachedAssetFile.length() > 0) {
                    player.setDataSource(cachedAssetFile.absolutePath)
                } else {
                    val afd = context.assets.openFd(assetPath)
                    currentAssetFd = afd
                    player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
            } else {
                val cleanUrl = song.file.trim().replace(" ", "%20")
                if (cleanUrl.isBlank()) {
                    _isLoading.value = false
                    _errorMessage.value = "Audio file not available"
                    return
                }
                val cachedRemoteFile = File(context.cacheDir, "stream_${song.id}.mp3")
                if (cachedRemoteFile.exists() && cachedRemoteFile.length() > 50_000L) {
                    player.setDataSource(cachedRemoteFile.absolutePath)
                } else {
                    val headers = mapOf(
                        "User-Agent" to "Mozilla/5.0 (Linux; Android 14; Mobile; rv:109.0)",
                        "Accept" to "*/*",
                        "Accept-Encoding" to "identity"
                    )
                    player.setDataSource(context, Uri.parse(cleanUrl), headers)
                    cacheSongInBackground(song, cleanUrl, cachedRemoteFile)
                }
            }

            player.setOnPreparedListener { mp ->
                if (mediaPlayer !== mp) return@setOnPreparedListener
                isPrepared = true
                _isLoading.value = false
                _isPlaying.value = true
                try {
                    val actualDuration = mp.duration.toLong()
                    if (actualDuration > 0) {
                        _durationMs.value = actualDuration
                    }
                    requestAudioFocus()
                    ensureAudioDeviceVolume()
                    mp.setVolume(1.0f, 1.0f)
                    mp.start()
                    startProgressUpdates()
                } catch (e: Exception) {
                    _isPlaying.value = false
                    _errorMessage.value = e.localizedMessage ?: "Playback failed"
                }
            }

            player.setOnCompletionListener { mp ->
                if (mediaPlayer !== mp) return@setOnCompletionListener
                handleTrackCompletion()
            }

            player.setOnErrorListener { mp, what, extra ->
                if (mediaPlayer === mp) {
                    Log.e("AudioPlayerManager", "MediaPlayer error: what=$what extra=$extra on song ${song.title}")
                    
                    // If online streaming had an issue (e.g. network/Stagefright glitch), download and play the exact original song!
                    val cachedRemoteFile = File(context.cacheDir, "stream_${song.id}.mp3")
                    if (song.file.startsWith("http") && (!cachedRemoteFile.exists() || cachedRemoteFile.length() < 50_000L)) {
                        Log.i("AudioPlayerManager", "Direct streaming had an issue, downloading original song: ${song.title}")
                        downloadAndPlayOriginalTrack(song)
                        return@setOnErrorListener true
                    }
                    
                    isPrepared = false
                    _isLoading.value = false
                    _isPlaying.value = false
                    _errorMessage.value = "Playback error ($what, $extra). Please retry."
                    stopProgressUpdates()
                }
                true
            }

            player.prepareAsync()
        } catch (e: Exception) {
            isPrepared = false
            _isLoading.value = false
            _isPlaying.value = false
            _errorMessage.value = e.localizedMessage ?: "Could not load audio stream"
        }
    }

    private fun downloadAndPlayOriginalTrack(song: Song) {
        downloadJob?.cancel()
        _isLoading.value = true
        _errorMessage.value = null

        downloadJob = scope.launch(Dispatchers.IO) {
            try {
                val cleanUrl = song.file.trim().replace(" ", "%20")
                val cachedFile = File(context.cacheDir, "stream_${song.id}.mp3")
                val tempFile = File(context.cacheDir, "stream_${song.id}.tmp")

                val url = URL(cleanUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile; rv:109.0)")
                conn.connectTimeout = 15000
                conn.readTimeout = 25000
                conn.connect()

                if (conn.responseCode in 200..299) {
                    conn.inputStream.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (tempFile.exists() && tempFile.length() > 50_000L) {
                        tempFile.renameTo(cachedFile)
                        withContext(Dispatchers.Main) {
                            if (_currentSong.value?.id == song.id) {
                                startPlayback(song)
                            }
                        }
                    } else {
                        tempFile.delete()
                        withContext(Dispatchers.Main) {
                            _isLoading.value = false
                            _errorMessage.value = "Audio stream incomplete"
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _isLoading.value = false
                        _errorMessage.value = "Server returned ${conn.responseCode}"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                    _errorMessage.value = "Could not stream audio: ${e.localizedMessage}"
                }
            }
        }
    }

    private fun cacheSongInBackground(song: Song, cleanUrl: String, cachedFile: File) {
        scope.launch(Dispatchers.IO) {
            try {
                if (cachedFile.exists() && cachedFile.length() > 50_000L) return@launch
                val tempFile = File(context.cacheDir, "stream_${song.id}.tmp")
                val url = URL(cleanUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile; rv:109.0)")
                conn.connectTimeout = 15000
                conn.readTimeout = 25000
                conn.connect()
                if (conn.responseCode in 200..299) {
                    conn.inputStream.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (tempFile.exists() && tempFile.length() > 50_000L) {
                        tempFile.renameTo(cachedFile)
                    } else {
                        tempFile.delete()
                    }
                }
            } catch (e: Throwable) {
                // Non-fatal background cache error
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attr = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                    .build()
                val request = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(attr)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener { /* Focus change listener handled */ }
                    .build()
                audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
        } catch (e: Throwable) {
            false
        }
    }

    fun ensureAudioDeviceVolume() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                // Ensure audio mode is normal so audio routes directly to speaker
                audioManager.mode = AudioManager.MODE_NORMAL

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
                    } catch (e: Throwable) {
                        // Ignore
                    }
                }
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val targetVol = (maxVol * 0.95f).toInt().coerceAtLeast(1)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
            }
        } catch (e: Throwable) {
            Log.e("AudioPlayerManager", "Failed to adjust volume: ${e.message}")
        }
    }

    private fun getOrCopyAssetToCache(assetPath: String): File? {
        return try {
            val fileName = assetPath.substringAfterLast("/")
            val cacheFile = File(context.cacheDir, "asset_$fileName")
            if (!cacheFile.exists() || cacheFile.length() == 0L) {
                context.assets.open(assetPath).use { input ->
                    FileOutputStream(cacheFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            if (cacheFile.exists() && cacheFile.length() > 0) cacheFile else null
        } catch (e: Throwable) {
            Log.e("AudioPlayerManager", "Failed to cache asset $assetPath: ${e.message}")
            null
        }
    }

    private fun handleTrackCompletion() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                try {
                    mediaPlayer?.seekTo(0)
                    mediaPlayer?.start()
                    _isPlaying.value = true
                    startProgressUpdates()
                } catch (e: Exception) {
                    _currentSong.value?.let { startPlayback(it) }
                }
            }
            RepeatMode.ALL -> {
                skipNext()
            }
            RepeatMode.OFF -> {
                val q = _queue.value
                val idx = _queueIndex.value
                if (idx < q.lastIndex) {
                    skipNext()
                } else {
                    _isPlaying.value = false
                    _currentPositionMs.value = _durationMs.value
                    stopProgressUpdates()
                }
            }
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer
        if (player == null || !isPrepared) {
            val cur = _currentSong.value
            if (cur != null) {
                startPlayback(cur)
            }
            return
        }

        try {
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
                stopProgressUpdates()
            } else {
                ensureAudioDeviceVolume()
                player.setVolume(1.0f, 1.0f)
                player.start()
                _isPlaying.value = true
                startProgressUpdates()
            }
        } catch (e: Exception) {
            // State mismatch, re-initialize cleanly
            val cur = _currentSong.value
            if (cur != null) {
                startPlayback(cur)
            }
        }
    }

    fun pause() {
        if (!isPrepared) return
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.pause()
                    _isPlaying.value = false
                    stopProgressUpdates()
                }
            } catch (e: Exception) {
                // Ignore safe error
            }
        }
    }

    fun resume() {
        if (!isPrepared) {
            _currentSong.value?.let { startPlayback(it) }
            return
        }
        mediaPlayer?.let {
            try {
                if (!it.isPlaying) {
                    ensureAudioDeviceVolume()
                    it.setVolume(1.0f, 1.0f)
                    it.start()
                    _isPlaying.value = true
                    startProgressUpdates()
                    try {
                        MusicPlaybackService.startService(context)
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            } catch (e: Exception) {
                _currentSong.value?.let { song -> startPlayback(song) }
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, _durationMs.value.coerceAtLeast(1L))
        _currentPositionMs.value = clamped
        if (isPrepared) {
            mediaPlayer?.let {
                try {
                    it.seekTo(clamped.toInt())
                } catch (e: Exception) {
                    // Ignore safe error
                }
            }
        }
    }

    fun seekBy(deltaMs: Long) {
        val target = (_currentPositionMs.value + deltaMs).coerceIn(0L, _durationMs.value.coerceAtLeast(1L))
        seekTo(target)
    }

    fun skipNext() {
        val q = _queue.value
        if (q.isEmpty()) return

        val nextIndex = if (_isShuffle.value && q.size > 1) {
            var r = (0 until q.size).random()
            while (r == _queueIndex.value && q.size > 1) {
                r = (0 until q.size).random()
            }
            r
        } else {
            (_queueIndex.value + 1) % q.size
        }

        _queueIndex.value = nextIndex
        val nextSong = q[nextIndex]
        _currentSong.value = nextSong
        onSongChangedListener?.invoke(nextSong)
        startPlayback(nextSong)
    }

    fun skipPrevious() {
        // If track played for more than 3 seconds, restart current track
        if (_currentPositionMs.value > 3000L) {
            seekTo(0)
            return
        }

        val q = _queue.value
        if (q.isEmpty()) return

        val prevIndex = if (_queueIndex.value - 1 < 0) {
            q.lastIndex
        } else {
            _queueIndex.value - 1
        }

        _queueIndex.value = prevIndex
        val prevSong = q[prevIndex]
        _currentSong.value = prevSong
        onSongChangedListener?.invoke(prevSong)
        startPlayback(prevSong)
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    private fun startProgressUpdates() {
        stopProgressUpdates()
        progressJob = scope.launch {
            while (isActive) {
                try {
                    if (isPrepared) {
                        mediaPlayer?.let { mp ->
                            if (mp.isPlaying) {
                                _currentPositionMs.value = mp.currentPosition.toLong()
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Never let MediaPlayer state exceptions crash the coroutine
                }
                delay(250)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressUpdates()
        isPrepared = false
        val p = mediaPlayer
        mediaPlayer = null
        p?.let {
            try {
                it.reset()
                it.release()
            } catch (e: Exception) {
                // Ignore safe cleanup
            }
        }
        try {
            currentAssetFd?.close()
        } catch (e: Exception) {
            // Ignore
        }
        currentAssetFd = null
        try {
            MusicPlaybackService.stopService(context)
        } catch (e: Exception) {
            // Ignore
        }
    }
}
