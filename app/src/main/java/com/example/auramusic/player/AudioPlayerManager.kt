package com.example.auramusic.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
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
    private var progressJob: Job? = null
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

        // Safely dispose old player
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
                    .build()
            )

            if (useLocal && localFile != null) {
                player.setDataSource(context, Uri.fromFile(localFile))
            } else {
                val cleanUrl = song.file.trim().replace(" ", "%20")
                if (cleanUrl.isBlank()) {
                    _isLoading.value = false
                    _errorMessage.value = "Audio file not available"
                    return
                }
                player.setDataSource(context, Uri.parse(cleanUrl))
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
                    isPrepared = false
                    _isLoading.value = false
                    _isPlaying.value = false
                    _errorMessage.value = "Playback error ($what, $extra)"
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
            MusicPlaybackService.stopService(context)
        } catch (e: Exception) {
            // Ignore
        }
    }
}
