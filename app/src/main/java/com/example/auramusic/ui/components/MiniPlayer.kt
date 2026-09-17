package com.example.auramusic.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.auramusic.model.Song

/**
 * A collapsible mini-player component that docks persistently at the bottom of the screen
 * (e.g. while navigating search results, playlists, and library), allowing full playback control
 * without interrupting the music.
 */
@Composable
fun MiniPlayer(
    song: Song?,
    isPlaying: Boolean,
    isLoading: Boolean,
    progressMs: Long,
    durationMs: Long,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
    isCollapsed: Boolean = false,
    onToggleCollapse: (() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = song != null,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ),
        modifier = modifier
    ) {
        if (song == null) return@AnimatedVisibility

        var localCollapsed by rememberSaveable { mutableStateOf(false) }
        val effectiveCollapsed = if (onToggleCollapse != null) isCollapsed else localCollapsed
        val toggleCollapse = onToggleCollapse ?: { localCollapsed = !localCollapsed }

        val accent = MaterialTheme.colorScheme.primary
        val progress = if (durationMs > 0) (progressMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

        val containerShape = if (effectiveCollapsed) RoundedCornerShape(22.dp) else RoundedCornerShape(16.dp)

        Box(
            modifier = Modifier
                .padding(horizontal = if (effectiveCollapsed) 16.dp else 12.dp, vertical = 5.dp)
                .shadow(12.dp, containerShape, spotColor = accent.copy(alpha = 0.45f))
                .clip(containerShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), containerShape)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
                .testTag("mini_player")
        ) {
            if (effectiveCollapsed) {
                // COLLAPSED MODE: Sleek compact capsule giving maximum screen space for search results
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = toggleCollapse)
                        .testTag("mini_player_collapsed")
                ) {
                    // Slim Progress Line
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = accent,
                        trackColor = Color.Transparent
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mini Artwork with Equalizer Badge
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(accent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = song.getEffectiveCover(song.id),
                                contentDescription = "${song.title} cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Compact Song Info + Visualizer
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist.replace(" - Topic", ""),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            MiniVisualizerBars(
                                isPlaying = isPlaying,
                                tint = accent,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Controls: Play/Pause, Next & Expand
                        val (playPauseSource, playPauseModifier) = rememberSpringPress(targetPressedScale = 0.88f)
                        val (nextSource, nextModifier) = rememberSpringPress(targetPressedScale = 0.85f)
                        val (expandSource, expandModifier) = rememberSpringPress(targetPressedScale = 0.85f)

                        // Play/Pause
                        Box(
                            modifier = Modifier.size(34.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = accent,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                IconButton(
                                    onClick = onTogglePlayPause,
                                    interactionSource = playPauseSource,
                                    modifier = Modifier
                                        .then(playPauseModifier)
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(accent.copy(alpha = 0.25f))
                                        .testTag("mini_play_pause_button")
                                ) {
                                    AnimatedContent(
                                        targetState = isPlaying,
                                        transitionSpec = {
                                            (scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeIn())
                                                .togetherWith(scaleOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut())
                                        },
                                        label = "miniCollapsedPlayPauseIconAnim"
                                    ) { playing ->
                                        Icon(
                                            imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (playing) "Pause" else "Play",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Skip Next
                        IconButton(
                            onClick = onSkipNext,
                            interactionSource = nextSource,
                            modifier = Modifier
                                .then(nextModifier)
                                .size(32.dp)
                                .testTag("mini_skip_next_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next Track",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Expand Button
                        IconButton(
                            onClick = toggleCollapse,
                            interactionSource = expandSource,
                            modifier = Modifier
                                .then(expandModifier)
                                .size(30.dp)
                                .testTag("mini_player_expand_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Expand mini player",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            } else {
                // EXPANDED MODE: Rich mini-player card with full details and quick collapse toggle
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenNowPlaying)
                        .testTag("mini_player_expanded")
                ) {
                    // Top Slim Progress Bar
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp),
                        color = accent,
                        trackColor = Color.Transparent
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Artwork Thumbnail
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(accent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = song.getEffectiveCover(song.id),
                                contentDescription = "${song.title} cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Title & Artist with Live Equalizer
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                MiniVisualizerBars(isPlaying = isPlaying, tint = accent)
                            }
                            Text(
                                text = song.artist.replace(" - Topic", ""),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Controls: Collapse, Play/Pause & Next with tactile spring feedback
                        val (collapseSource, collapseModifier) = rememberSpringPress(targetPressedScale = 0.85f)
                        val (miniPlayPauseSource, miniPlayPauseModifier) = rememberSpringPress(targetPressedScale = 0.88f)
                        val (miniNextSource, miniNextModifier) = rememberSpringPress(targetPressedScale = 0.85f)

                        // Collapse Button to minimize mini-player while browsing
                        IconButton(
                            onClick = toggleCollapse,
                            interactionSource = collapseSource,
                            modifier = Modifier
                                .then(collapseModifier)
                                .size(34.dp)
                                .testTag("mini_player_collapse_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Collapse mini player",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Play/Pause Button
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = accent,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                IconButton(
                                    onClick = onTogglePlayPause,
                                    interactionSource = miniPlayPauseSource,
                                    modifier = Modifier
                                        .then(miniPlayPauseModifier)
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(accent.copy(alpha = 0.25f))
                                        .testTag("mini_play_pause_button")
                                ) {
                                    AnimatedContent(
                                        targetState = isPlaying,
                                        transitionSpec = {
                                            (scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeIn())
                                                .togetherWith(scaleOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut())
                                        },
                                        label = "miniPlayPauseIconAnim"
                                    ) { playing ->
                                        Icon(
                                            imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (playing) "Pause" else "Play",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Skip Next Button
                        IconButton(
                            onClick = onSkipNext,
                            interactionSource = miniNextSource,
                            modifier = Modifier
                                .then(miniNextModifier)
                                .size(38.dp)
                                .testTag("mini_skip_next_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next Track",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3-bar subtle animated equalizer indicator reflecting active playback.
 */
@Composable
private fun MiniVisualizerBars(
    isPlaying: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mini_viz_bars")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), AnimRepeatMode.Reverse),
        label = "bar1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(360, easing = LinearEasing), AnimRepeatMode.Reverse),
        label = "bar2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), AnimRepeatMode.Reverse),
        label = "bar3"
    )

    Row(
        modifier = modifier.height(13.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val bar1Height = if (isPlaying) h1 else 0.35f
        val bar2Height = if (isPlaying) h2 else 0.65f
        val bar3Height = if (isPlaying) h3 else 0.45f

        Box(
            modifier = Modifier
                .width(2.5.dp)
                .fillMaxHeight(bar1Height)
                .clip(RoundedCornerShape(1.dp))
                .background(tint)
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .fillMaxHeight(bar2Height)
                .clip(RoundedCornerShape(1.dp))
                .background(tint)
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .fillMaxHeight(bar3Height)
                .clip(RoundedCornerShape(1.dp))
                .background(tint)
        )
    }
}
