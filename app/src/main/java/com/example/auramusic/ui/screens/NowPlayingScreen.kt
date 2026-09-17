package com.example.auramusic.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.auramusic.model.Song
import com.example.auramusic.player.RepeatMode
import com.example.auramusic.ui.components.AudioVisualizer
import com.example.auramusic.ui.components.rememberSpringPress
import com.example.auramusic.ui.theme.AuraBorder
import com.example.auramusic.ui.theme.AuraCardBackground
import com.example.auramusic.ui.theme.AuraDarkBackground
import com.example.auramusic.ui.theme.AuraSecondary
import com.example.auramusic.ui.theme.AuraTertiary
import com.example.auramusic.ui.theme.AuraTextMuted
import com.example.auramusic.ui.theme.AuraTextPrimary
import com.example.auramusic.ui.theme.AuraTextSecondary
import java.util.Locale

@Composable
fun NowPlayingScreen(
    song: Song?,
    isPlaying: Boolean,
    isLoading: Boolean,
    progressMs: Long,
    durationMs: Long,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    isFavorite: Boolean,
    isDownloaded: Boolean = false,
    isDownloading: Boolean = false,
    isPlayingFromLocal: Boolean = false,
    queue: List<Song>,
    queueIndex: Int,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: (Int) -> Unit,
    onToggleDownload: () -> Unit = {},
    onSelectQueueItem: (Song, Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (song == null) return

    var isQueueVisible by remember { mutableStateOf(false) }
    val accent = song.getAccentColor(song.id)

    // Seek Slider dragging state
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }

    val currentSliderValue = if (isDragging) {
        dragPosition
    } else {
        if (durationMs > 0) (progressMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AuraDarkBackground)
            .testTag("now_playing_screen")
    ) {
        // Ambient Radial Background Glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.28f),
                            AuraDarkBackground.copy(alpha = 0.95f),
                            AuraDarkBackground
                        ),
                        radius = 1200f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 28.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("close_now_playing_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Minimize",
                        tint = AuraTextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isPlayingFromLocal || isDownloaded) "OFFLINE • LOCAL STORAGE" else "PLAYING FROM PLAYLIST",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 10.sp,
                            color = if (isPlayingFromLocal || isDownloaded) AuraSecondary else AuraTextMuted,
                            letterSpacing = 1.5.sp,
                            fontWeight = if (isPlayingFromLocal || isDownloaded) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                    Text(
                        text = song.primaryGenre,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = accent
                        )
                    )
                }

                IconButton(
                    onClick = { isQueueVisible = !isQueueVisible },
                    modifier = Modifier.testTag("queue_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = "Queue",
                        tint = if (isQueueVisible) accent else AuraTextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Album Artwork Card
            Box(
                modifier = Modifier
                    .size(270.dp)
                    .shadow(32.dp, RoundedCornerShape(24.dp), spotColor = accent.copy(alpha = 0.6f))
                    .clip(RoundedCornerShape(24.dp))
                    .border(2.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .background(AuraCardBackground),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = song.getEffectiveCover(song.id),
                    contentDescription = "${song.title} album art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Audio Visualizer Bar
            AudioVisualizer(
                isPlaying = isPlaying,
                primaryColor = accent,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Song Info & Favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = AuraTextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = song.artist.replace(" - Topic", ""),
                        style = MaterialTheme.typography.bodyLarge.copy(color = AuraTextSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (song.album.isNotBlank()) {
                        Text(
                            text = song.album,
                            style = MaterialTheme.typography.labelMedium.copy(color = AuraTextMuted),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleDownload,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("now_playing_download_button")
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = accent,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isDownloaded) Icons.Filled.CheckCircle else Icons.Filled.Download,
                                contentDescription = if (isDownloaded) "Saved to local storage" else "Save to local storage",
                                tint = if (isDownloaded) AuraSecondary else AuraTextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { onToggleFavorite(song.id) },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("now_playing_fav_button")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) AuraTertiary else AuraTextSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Slider & Timestamps
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = currentSliderValue,
                    onValueChange = {
                        isDragging = true
                        dragPosition = it
                    },
                    onValueChangeFinished = {
                        isDragging = false
                        val targetMs = (dragPosition * durationMs).toLong()
                        onSeek(targetMs)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = accent,
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("now_playing_seek_bar")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val displayedProgress = if (isDragging) (dragPosition * durationMs).toLong() else progressMs
                    Text(
                        text = formatTime(displayedProgress),
                        style = MaterialTheme.typography.labelMedium.copy(color = AuraTextMuted)
                    )
                    Text(
                        text = formatTime(durationMs),
                        style = MaterialTheme.typography.labelMedium.copy(color = AuraTextMuted)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary Playback Controls with tactile spring animations
            val (shuffleSource, shuffleModifier) = rememberSpringPress(targetPressedScale = 0.85f)
            val (prevSource, prevModifier) = rememberSpringPress(targetPressedScale = 0.85f)
            val (playPauseSource, playPauseModifier) = rememberSpringPress(targetPressedScale = 0.90f)
            val (nextSource, nextModifier) = rememberSpringPress(targetPressedScale = 0.85f)
            val (repeatSource, repeatModifier) = rememberSpringPress(targetPressedScale = 0.85f)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button
                IconButton(
                    onClick = onToggleShuffle,
                    interactionSource = shuffleSource,
                    modifier = Modifier
                        .then(shuffleModifier)
                        .testTag("shuffle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) accent else AuraTextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous Button
                IconButton(
                    onClick = onSkipPrevious,
                    interactionSource = prevSource,
                    modifier = Modifier
                        .then(prevModifier)
                        .testTag("skip_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = AuraTextPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Play / Pause Circle with spring press effect
                Box(
                    modifier = Modifier
                        .then(playPauseModifier)
                        .size(68.dp)
                        .shadow(16.dp, CircleShape, spotColor = accent)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(accent, accent.copy(alpha = 0.8f))
                            )
                        )
                        .clickable(
                            interactionSource = playPauseSource,
                            indication = androidx.compose.material3.ripple(),
                            onClick = onTogglePlayPause
                        )
                        .testTag("now_playing_play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
                        AnimatedContent(
                            targetState = isPlaying,
                            transitionSpec = {
                                (scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeIn())
                                    .togetherWith(scaleOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut())
                            },
                            label = "nowPlayingPlayPauseIconAnim"
                        ) { playing ->
                            Icon(
                                imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playing) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                // Next Button
                IconButton(
                    onClick = onSkipNext,
                    interactionSource = nextSource,
                    modifier = Modifier
                        .then(nextModifier)
                        .testTag("skip_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = AuraTextPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Repeat Button
                IconButton(
                    onClick = onToggleRepeat,
                    interactionSource = repeatSource,
                    modifier = Modifier
                        .then(repeatModifier)
                        .testTag("repeat_button")
                ) {
                    val (icon, tint) = when (repeatMode) {
                        RepeatMode.OFF -> Pair(Icons.Default.Repeat, AuraTextMuted)
                        RepeatMode.ALL -> Pair(Icons.Default.Repeat, accent)
                        RepeatMode.ONE -> Pair(Icons.Default.RepeatOne, accent)
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "Repeat",
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Queue Overlay Sheet
        AnimatedVisibility(
            visible = isQueueVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { isQueueVisible = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(450.dp)
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(AuraDarkBackground)
                        .border(1.dp, AuraBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .clickable(enabled = false) {}
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Playing Queue",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AuraTextPrimary
                                )
                            )
                            Text(
                                text = "${queue.size} songs in queue",
                                style = MaterialTheme.typography.labelMedium.copy(color = AuraTextSecondary)
                            )
                        }

                        IconButton(onClick = { isQueueVisible = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Queue",
                                tint = AuraTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(queue) { index, qSong ->
                            val isQCurrent = index == queueIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isQCurrent) AuraCardBackground else Color.Transparent)
                                    .clickable {
                                        onSelectQueueItem(qSong, index)
                                        isQueueVisible = false
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = if (isQCurrent) accent else AuraTextMuted
                                    ),
                                    modifier = Modifier.width(28.dp),
                                    textAlign = TextAlign.Center
                                )

                                AsyncImage(
                                    model = qSong.getEffectiveCover(index),
                                    contentDescription = qSong.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = qSong.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isQCurrent) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isQCurrent) accent else AuraTextPrimary
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = qSong.artist.replace(" - Topic", ""),
                                        style = MaterialTheme.typography.labelMedium.copy(color = AuraTextSecondary),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = qSong.durationText,
                                    style = MaterialTheme.typography.labelMedium.copy(color = AuraTextMuted)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
