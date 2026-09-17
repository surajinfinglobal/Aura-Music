package com.example.auramusic.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.auramusic.MainActivity
import com.example.auramusic.R
import com.example.auramusic.model.Song
import com.example.auramusic.player.AudioPlayerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicPlaybackService : Service() {

    companion object {
        const val CHANNEL_ID = "aura_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY_PAUSE = "com.example.auramusic.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.auramusic.ACTION_NEXT"
        const val ACTION_PREV = "com.example.auramusic.ACTION_PREV"
        const val ACTION_STOP = "com.example.auramusic.ACTION_STOP"
        const val ACTION_SYNC = "com.example.auramusic.ACTION_SYNC"

        fun startService(context: Context) {
            val intent = Intent(context, MusicPlaybackService::class.java).apply {
                action = ACTION_SYNC
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, MusicPlaybackService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var mediaSession: MediaSession? = null
    private var isForeground = false
    private var currentArtworkBitmap: Bitmap? = null
    private var lastCoverUrl: String? = null
    private var artworkJob: Job? = null

    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var resumeOnFocusGain = false

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                AudioPlayerManager.getInstance(applicationContext).pause()
            }
        }
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        val player = AudioPlayerManager.getInstance(applicationContext)
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                resumeOnFocusGain = false
                player.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                resumeOnFocusGain = player.isPlaying.value
                player.pause()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (resumeOnFocusGain) {
                    resumeOnFocusGain = false
                    player.resume()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initMediaSession()
        setupAudioFocus()
        registerBecomingNoisyReceiver()

        // Start in foreground immediately with initial notification to satisfy Android OS requirement
        val initialNotification = buildNotification(null, false, null)
        startServiceInForeground(initialNotification)

        // Observe player state changes
        val player = AudioPlayerManager.getInstance(applicationContext)
        serviceScope.launch {
            combine(
                player.currentSong,
                player.isPlaying
            ) { song, isPlaying ->
                Pair(song, isPlaying)
            }.collectLatest { (song, isPlaying) ->
                handlePlaybackStateChanged(song, isPlaying)
            }
        }
    }

    private fun startServiceInForeground(notification: Notification) {
        if (!isForeground) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceCompat.startForeground(
                        this,
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                isForeground = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handlePlaybackStateChanged(song: Song?, isPlaying: Boolean) {
        val player = AudioPlayerManager.getInstance(applicationContext)
        updateMediaSession(song, isPlaying, player.currentPositionMs.value, player.durationMs.value)

        if (song == null) {
            if (!isPlaying) {
                stopForegroundService()
            }
            return
        }

        // Manage audio focus
        if (isPlaying) {
            requestAudioFocus()
        }

        // Check if artwork changed
        val coverUrl = song.getEffectiveCover(song.id)
        if (coverUrl != lastCoverUrl) {
            lastCoverUrl = coverUrl
            loadArtwork(coverUrl)
        }

        val notification = buildNotification(song, isPlaying, currentArtworkBitmap)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun loadArtwork(url: String) {
        artworkJob?.cancel()
        artworkJob = serviceScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val loader = Coil.imageLoader(applicationContext)
                    val req = ImageRequest.Builder(applicationContext)
                        .data(url)
                        .allowHardware(false) // required for notifications
                        .build()
                    val result = (loader.execute(req) as? SuccessResult)?.drawable
                    (result as? BitmapDrawable)?.bitmap
                } catch (e: Exception) {
                    null
                }
            }
            currentArtworkBitmap = bitmap
            val player = AudioPlayerManager.getInstance(applicationContext)
            val song = player.currentSong.value
            val isPlaying = player.isPlaying.value
            if (song != null) {
                val notification = buildNotification(song, isPlaying, currentArtworkBitmap)
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val player = AudioPlayerManager.getInstance(applicationContext)
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                player.togglePlayPause()
            }
            ACTION_NEXT -> {
                player.skipNext()
            }
            ACTION_PREV -> {
                player.skipPrevious()
            }
            ACTION_STOP -> {
                player.pause()
                stopForegroundService()
            }
            ACTION_SYNC -> {
                // Ensure foreground status and refresh
                val song = player.currentSong.value
                val isPlaying = player.isPlaying.value
                val notification = buildNotification(song, isPlaying, currentArtworkBitmap)
                startServiceInForeground(notification)
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Aura Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background music playback controls and status"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun initMediaSession() {
        mediaSession = MediaSession(this, "AuraMusicSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    AudioPlayerManager.getInstance(applicationContext).resume()
                }

                override fun onPause() {
                    AudioPlayerManager.getInstance(applicationContext).pause()
                }

                override fun onSkipToNext() {
                    AudioPlayerManager.getInstance(applicationContext).skipNext()
                }

                override fun onSkipToPrevious() {
                    AudioPlayerManager.getInstance(applicationContext).skipPrevious()
                }

                override fun onSeekTo(pos: Long) {
                    AudioPlayerManager.getInstance(applicationContext).seekTo(pos)
                }

                override fun onStop() {
                    AudioPlayerManager.getInstance(applicationContext).pause()
                    stopForegroundService()
                }
            })
            isActive = true
        }
    }

    private fun updateMediaSession(
        song: Song?,
        isPlaying: Boolean,
        currentPos: Long,
        duration: Long
    ) {
        val session = mediaSession ?: return
        val state = if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
        val actions = PlaybackState.ACTION_PLAY or
            PlaybackState.ACTION_PAUSE or
            PlaybackState.ACTION_PLAY_PAUSE or
            PlaybackState.ACTION_SKIP_TO_NEXT or
            PlaybackState.ACTION_SKIP_TO_PREVIOUS or
            PlaybackState.ACTION_SEEK_TO or
            PlaybackState.ACTION_STOP

        session.setPlaybackState(
            PlaybackState.Builder()
                .setActions(actions)
                .setState(state, currentPos, 1.0f)
                .build()
        )

        if (song != null) {
            session.setMetadata(
                MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, song.title)
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, song.artist.replace(" - Topic", ""))
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, song.album)
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, duration)
                    .build()
            )
        }
    }

    private fun buildActionPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, MusicPlaybackService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildNotification(
        song: Song?,
        isPlaying: Boolean,
        artwork: Bitmap?
    ): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = song?.title ?: "Aura Music"
        val artist = song?.artist?.replace(" - Topic", "") ?: "Background Playback"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText(song?.album ?: "Aura")
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(
                R.drawable.ic_skip_previous,
                "Previous",
                buildActionPendingIntent(ACTION_PREV, 1)
            )
            .addAction(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                if (isPlaying) "Pause" else "Play",
                buildActionPendingIntent(ACTION_PLAY_PAUSE, 2)
            )
            .addAction(
                R.drawable.ic_skip_next,
                "Next",
                buildActionPendingIntent(ACTION_NEXT, 3)
            )
            .addAction(
                R.drawable.ic_close,
                "Stop",
                buildActionPendingIntent(ACTION_STOP, 4)
            )

        if (artwork != null) {
            builder.setLargeIcon(artwork)
        } else {
            try {
                val fallbackLogo = BitmapFactory.decodeResource(resources, R.drawable.ic_aura_logo)
                if (fallbackLogo != null) {
                    builder.setLargeIcon(fallbackLogo)
                }
            } catch (e: Exception) {
                // Ignore fallback decoding error
            }
        }

        return builder.build()
    }

    private fun setupAudioFocus() {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    private fun requestAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attr)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest?.let { am.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    private fun registerBecomingNoisyReceiver() {
        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(becomingNoisyReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(becomingNoisyReceiver, filter)
        }
    }

    private fun stopForegroundService() {
        isForeground = false
        abandonAudioFocus()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        try {
            unregisterReceiver(becomingNoisyReceiver)
        } catch (e: Exception) {
            // Ignore if already unregistered
        }
        abandonAudioFocus()
    }
}
