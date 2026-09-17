package com.example.auramusic

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auramusic.data.SongRepository
import com.example.auramusic.model.Song
import com.example.auramusic.player.AudioPlayerManager
import com.example.auramusic.ui.components.MiniPlayer
import com.example.auramusic.ui.screens.FavoritesScreen
import com.example.auramusic.ui.screens.HomeScreen
import com.example.auramusic.ui.screens.LibraryScreen
import com.example.auramusic.ui.screens.MusicPlayerScreen
import com.example.auramusic.ui.screens.NowPlayingScreen
import com.example.auramusic.ui.screens.SearchScreen
import com.example.auramusic.ui.theme.AuraBorder
import com.example.auramusic.ui.theme.AuraCardBackground
import com.example.auramusic.ui.theme.AuraDarkBackground
import com.example.auramusic.ui.theme.AuraMusicTheme
import com.example.auramusic.ui.theme.AuraPrimary
import com.example.auramusic.ui.theme.AuraTextMuted
import com.example.auramusic.ui.theme.AuraTextSecondary
import com.example.auramusic.ui.theme.BRAND_PALETTES
import com.example.auramusic.ui.theme.BrandColor

enum class NavDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    SEARCH("Search", Icons.Filled.Search, Icons.Outlined.Search, "nav_search"),
    PLAYER("Player", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircleOutline, "nav_player"),
    LIBRARY("Library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic, "nav_library"),
    FAVORITES("Favorites", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder, "nav_favorites")
}

class MainActivity : ComponentActivity() {
    private lateinit var songRepository: SongRepository
    private lateinit var audioPlayerManager: AudioPlayerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        try {
            songRepository = SongRepository(applicationContext)
            audioPlayerManager = AudioPlayerManager.getInstance(applicationContext).apply {
                localFileResolver = { songId ->
                    try {
                        songRepository.likedStorageManager.getLocalAudioFile(songId)
                    } catch (e: Throwable) {
                        null
                    }
                }
                onSongChangedListener = { song ->
                    try {
                        songRepository.addToRecent(song)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            songRepository = SongRepository(applicationContext)
            audioPlayerManager = AudioPlayerManager.getInstance(applicationContext)
        }

        setContent {
            val currentSong by audioPlayerManager.currentSong.collectAsState()
            var selectedBrandColor by remember { mutableStateOf(BRAND_PALETTES.first()) }
            var dynamicArtThemeEnabled by remember { mutableStateOf(true) }

            val themeSeedColor = remember(currentSong, dynamicArtThemeEnabled, selectedBrandColor) {
                if (dynamicArtThemeEnabled && currentSong != null) {
                    currentSong!!.getAccentColor(currentSong!!.id)
                } else {
                    selectedBrandColor.color
                }
            }

            AuraMusicTheme(seedColor = themeSeedColor) {
                AuraApp(
                    repository = songRepository,
                    playerManager = audioPlayerManager,
                    selectedBrandColor = selectedBrandColor,
                    dynamicArtThemeEnabled = dynamicArtThemeEnabled,
                    onSelectBrandColor = { selectedBrandColor = it },
                    onToggleDynamicArtTheme = { dynamicArtThemeEnabled = it }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Do not release player if music is playing or paused with active track so background playback persists
        if (isFinishing && !audioPlayerManager.isPlaying.value && audioPlayerManager.currentSong.value == null) {
            audioPlayerManager.release()
        }
    }
}

@Composable
fun AuraApp(
    repository: SongRepository,
    playerManager: AudioPlayerManager,
    selectedBrandColor: BrandColor,
    dynamicArtThemeEnabled: Boolean,
    onSelectBrandColor: (BrandColor) -> Unit,
    onToggleDynamicArtTheme: (Boolean) -> Unit
) {
    var currentDestination by remember { mutableStateOf(NavDestination.HOME) }
    var isNowPlayingExpanded by remember { mutableStateOf(false) }

    // Repository states
    val songs by repository.songs.collectAsState()
    val isLoadingCatalog by repository.isLoading.collectAsState()
    val favorites by repository.favorites.collectAsState()
    val downloadedSongIds by repository.downloadedSongIds.collectAsState()
    val downloadingIds by repository.likedStorageManager.downloadingIds.collectAsState()
    val isAutoSaveEnabled by repository.likedStorageManager.isAutoSaveEnabled.collectAsState()
    val recentSongIds by repository.recentSongIds.collectAsState()

    // Player states
    val currentSong by playerManager.currentSong.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val isLoadingAudio by playerManager.isLoading.collectAsState()
    val currentPositionMs by playerManager.currentPositionMs.collectAsState()
    val durationMs by playerManager.durationMs.collectAsState()
    val isShuffle by playerManager.isShuffle.collectAsState()
    val repeatMode by playerManager.repeatMode.collectAsState()
    val queue by playerManager.queue.collectAsState()
    val queueIndex by playerManager.queueIndex.collectAsState()
    val isPlayingFromLocalStorage by playerManager.isPlayingFromLocalStorage.collectAsState()

    // Search & Filter states
    var searchQuery by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf("All") }
    var isMiniPlayerCollapsed by rememberSaveable { mutableStateOf(false) }

    val searchResults = remember(searchQuery, selectedGenre, songs) {
        repository.searchSongs(searchQuery, selectedGenre)
    }

    val trendingSongs = remember(songs) {
        repository.getTrendingSongs()
    }

    val playlists = remember(songs) {
        repository.getPlaylists()
    }

    val artists = remember(songs) {
        repository.getArtists()
    }

    val favoriteSongs = remember(songs, favorites) {
        songs.filter { favorites.contains(it.id) }
    }

    val recentlyPlayedSongs = remember(songs, recentSongIds) {
        val songMap = songs.associateBy { it.id }
        recentSongIds.mapNotNull { songMap[it] }
    }

    BackHandler(enabled = isNowPlayingExpanded) {
        isNowPlayingExpanded = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraDarkBackground)
    ) {
        Scaffold(
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    androidx.compose.foundation.layout.Column {
                        // Persistent Mini Player above Bottom Bar
                        if (!isNowPlayingExpanded && currentDestination != NavDestination.PLAYER) {
                            MiniPlayer(
                                song = currentSong,
                                isPlaying = isPlaying,
                                isLoading = isLoadingAudio,
                                progressMs = currentPositionMs,
                                durationMs = durationMs,
                                isCollapsed = isMiniPlayerCollapsed,
                                onToggleCollapse = { isMiniPlayerCollapsed = !isMiniPlayerCollapsed },
                                onTogglePlayPause = { playerManager.togglePlayPause() },
                                onSkipNext = { playerManager.skipNext() },
                                onOpenNowPlaying = { isNowPlayingExpanded = true }
                            )
                        }

                        // Bottom Navigation Bar
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(0.dp))
                                .testTag("bottom_nav_bar")
                        ) {
                            NavDestination.entries.forEach { dest ->
                                val selected = currentDestination == dest
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentDestination = dest },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.title,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = dest.title,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontSize = 11.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    ),
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentDestination) {
                    NavDestination.HOME -> {
                        HomeScreen(
                            songs = songs,
                            isLoading = isLoadingCatalog,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            favorites = favorites,
                            downloadedSongIds = downloadedSongIds,
                            recentlyPlayed = recentlyPlayedSongs,
                            trendingSongs = trendingSongs,
                            playlists = playlists,
                            artists = artists,
                            selectedGenre = selectedGenre,
                            onGenreSelect = { selectedGenre = it },
                            onPlaySong = { song, queueList ->
                                playerManager.playSong(song, queueList)
                            },
                            onFavoriteToggle = { repository.toggleFavorite(it) },
                            onNavigateToSearch = { currentDestination = NavDestination.SEARCH },
                            onNavigateToLibrary = { currentDestination = NavDestination.LIBRARY }
                        )
                    }
                    NavDestination.SEARCH -> {
                        SearchScreen(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            selectedGenre = selectedGenre,
                            onGenreSelect = { selectedGenre = it },
                            searchResults = searchResults,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            favorites = favorites,
                            downloadedSongIds = downloadedSongIds,
                            onPlaySong = { song, queueList ->
                                playerManager.playSong(song, queueList)
                            },
                            onFavoriteToggle = { repository.toggleFavorite(it) }
                        )
                    }
                    NavDestination.PLAYER -> {
                        MusicPlayerScreen(
                            song = currentSong,
                            isPlaying = isPlaying,
                            isLoading = isLoadingAudio,
                            currentPositionMs = currentPositionMs,
                            durationMs = durationMs,
                            isShuffle = isShuffle,
                            repeatMode = repeatMode,
                            isFavorite = currentSong?.let { favorites.contains(it.id) } ?: false,
                            isDownloaded = currentSong?.let { downloadedSongIds.contains(it.id) } ?: false,
                            isDownloading = currentSong?.let { downloadingIds.contains(it.id) } ?: false,
                            isPlayingFromLocal = isPlayingFromLocalStorage,
                            queue = queue,
                            queueIndex = queueIndex,
                            dynamicArtThemeEnabled = dynamicArtThemeEnabled,
                            selectedBrandColor = selectedBrandColor,
                            onTogglePlayPause = { playerManager.togglePlayPause() },
                            onSeekTo = { playerManager.seekTo(it) },
                            onSeekBy = { playerManager.seekBy(it) },
                            onSkipPrevious = { playerManager.skipPrevious() },
                            onSkipNext = { playerManager.skipNext() },
                            onToggleShuffle = { playerManager.toggleShuffle() },
                            onToggleRepeat = { playerManager.toggleRepeat() },
                            onToggleFavorite = { repository.toggleFavorite(it) },
                            onToggleDownload = {
                                currentSong?.let { song ->
                                    if (downloadedSongIds.contains(song.id)) {
                                        repository.likedStorageManager.removeSongAudioFromLocalStorage(song.id)
                                    } else {
                                        repository.likedStorageManager.saveSongAudioToLocalStorage(song)
                                    }
                                }
                            },
                            onSelectQueueItem = { qSong, qIdx ->
                                playerManager.playSong(qSong, queue, qIdx)
                            },
                            onToggleDynamicArtTheme = onToggleDynamicArtTheme,
                            onSelectBrandColor = onSelectBrandColor,
                            onPlayRecommended = {
                                if (songs.isNotEmpty()) playerManager.playSong(songs.first(), songs)
                            },
                            onCollapse = null
                        )
                    }
                    NavDestination.LIBRARY -> {
                        LibraryScreen(
                            songs = songs,
                            artists = artists,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            favorites = favorites,
                            downloadedSongIds = downloadedSongIds,
                            onPlaySong = { song, queueList ->
                                playerManager.playSong(song, queueList)
                            },
                            onShuffleAll = {
                                if (songs.isNotEmpty()) {
                                    val shuffled = songs.shuffled()
                                    playerManager.playSong(shuffled.first(), shuffled)
                                }
                            },
                            onFavoriteToggle = { repository.toggleFavorite(it) }
                        )
                    }
                    NavDestination.FAVORITES -> {
                        FavoritesScreen(
                            favoriteSongs = favoriteSongs,
                            downloadedSongIds = downloadedSongIds,
                            downloadingIds = downloadingIds,
                            isAutoSaveEnabled = isAutoSaveEnabled,
                            storageUsedText = repository.likedStorageManager.getFormattedStorageSize(),
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            onPlaySong = { song, queueList ->
                                playerManager.playSong(song, queueList)
                            },
                            onFavoriteToggle = { repository.toggleFavorite(it) },
                            onToggleAutoSave = { repository.likedStorageManager.setAutoSaveEnabled(it) },
                            onDownloadAllLiked = { repository.likedStorageManager.downloadAllLikedSongs(songs) },
                            onDownloadSongToggle = { song ->
                                if (downloadedSongIds.contains(song.id)) {
                                    repository.likedStorageManager.removeSongAudioFromLocalStorage(song.id)
                                } else {
                                    repository.likedStorageManager.saveSongAudioToLocalStorage(song)
                                }
                            },
                            onExploreClick = { currentDestination = NavDestination.HOME }
                        )
                    }
                }
            }
        }

        // Fullscreen Now Playing Overlay
        AnimatedVisibility(
            visible = isNowPlayingExpanded && currentSong != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            MusicPlayerScreen(
                song = currentSong,
                isPlaying = isPlaying,
                isLoading = isLoadingAudio,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                isShuffle = isShuffle,
                repeatMode = repeatMode,
                isFavorite = currentSong?.let { favorites.contains(it.id) } ?: false,
                isDownloaded = currentSong?.let { downloadedSongIds.contains(it.id) } ?: false,
                isDownloading = currentSong?.let { downloadingIds.contains(it.id) } ?: false,
                isPlayingFromLocal = isPlayingFromLocalStorage,
                queue = queue,
                queueIndex = queueIndex,
                dynamicArtThemeEnabled = dynamicArtThemeEnabled,
                selectedBrandColor = selectedBrandColor,
                onTogglePlayPause = { playerManager.togglePlayPause() },
                onSeekTo = { playerManager.seekTo(it) },
                onSeekBy = { playerManager.seekBy(it) },
                onSkipPrevious = { playerManager.skipPrevious() },
                onSkipNext = { playerManager.skipNext() },
                onToggleShuffle = { playerManager.toggleShuffle() },
                onToggleRepeat = { playerManager.toggleRepeat() },
                onToggleFavorite = { repository.toggleFavorite(it) },
                onToggleDownload = {
                    currentSong?.let { song ->
                        if (downloadedSongIds.contains(song.id)) {
                            repository.likedStorageManager.removeSongAudioFromLocalStorage(song.id)
                        } else {
                            repository.likedStorageManager.saveSongAudioToLocalStorage(song)
                        }
                    }
                },
                onSelectQueueItem = { qSong, qIdx ->
                    playerManager.playSong(qSong, queue, qIdx)
                },
                onToggleDynamicArtTheme = onToggleDynamicArtTheme,
                onSelectBrandColor = onSelectBrandColor,
                onPlayRecommended = {
                    if (songs.isNotEmpty()) playerManager.playSong(songs.first(), songs)
                },
                onCollapse = { isNowPlayingExpanded = false }
            )
        }
    }
}
