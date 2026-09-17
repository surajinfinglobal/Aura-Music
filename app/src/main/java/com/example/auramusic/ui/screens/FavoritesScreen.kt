package com.example.auramusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auramusic.model.Song
import com.example.auramusic.ui.components.SongListItem
import com.example.auramusic.ui.theme.AuraBorder
import com.example.auramusic.ui.theme.AuraCardBackground
import com.example.auramusic.ui.theme.AuraDarkBackground
import com.example.auramusic.ui.theme.AuraPrimary
import com.example.auramusic.ui.theme.AuraSecondary
import com.example.auramusic.ui.theme.AuraTertiary
import com.example.auramusic.ui.theme.AuraTextMuted
import com.example.auramusic.ui.theme.AuraTextPrimary
import com.example.auramusic.ui.theme.AuraTextSecondary

@Composable
fun FavoritesScreen(
    favoriteSongs: List<Song>,
    downloadedSongIds: Set<Int>,
    downloadingIds: Set<Int>,
    isAutoSaveEnabled: Boolean,
    storageUsedText: String,
    currentSong: Song?,
    isPlaying: Boolean,
    onPlaySong: (Song, List<Song>) -> Unit,
    onFavoriteToggle: (Int) -> Unit,
    onToggleAutoSave: (Boolean) -> Unit,
    onDownloadAllLiked: () -> Unit,
    onDownloadSongToggle: (Song) -> Unit,
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val downloadedCount = favoriteSongs.count { downloadedSongIds.contains(it.id) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("favorites_screen")
    ) {
        // Top Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = AuraTertiary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Favorites",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = AuraTextPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }

            Text(
                text = "${favoriteSongs.size} favorite tracks in local catalog",
                style = MaterialTheme.typography.bodyMedium.copy(color = AuraTextSecondary)
            )

            // Local Storage Card
            Spacer(modifier = Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AuraCardBackground)
                    .border(1.dp, AuraBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
                    .testTag("local_storage_panel")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SdStorage,
                            contentDescription = "Local Storage",
                            tint = AuraSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Local Storage",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = AuraTextPrimary
                                )
                            )
                            Text(
                                text = "$downloadedCount saved offline • $storageUsedText",
                                style = MaterialTheme.typography.labelSmall.copy(color = AuraTextMuted)
                            )
                        }
                    }

                    if (favoriteSongs.isNotEmpty() && downloadedCount < favoriteSongs.size) {
                        Button(
                            onClick = onDownloadAllLiked,
                            colors = ButtonDefaults.buttonColors(containerColor = AuraSecondary.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("download_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download All",
                                tint = AuraSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Save All",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = AuraSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Auto-save liked songs for offline play",
                        style = MaterialTheme.typography.bodySmall.copy(color = AuraTextSecondary)
                    )
                    Switch(
                        checked = isAutoSaveEnabled,
                        onCheckedChange = onToggleAutoSave,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AuraPrimary,
                            uncheckedThumbColor = AuraTextMuted,
                            uncheckedTrackColor = AuraBorder
                        ),
                        modifier = Modifier.testTag("auto_save_switch")
                    )
                }
            }

            if (favoriteSongs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (favoriteSongs.isNotEmpty()) {
                                onPlaySong(favoriteSongs.first(), favoriteSongs)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AuraPrimary),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("fav_play_all_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play All",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play All", fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = {
                            if (favoriteSongs.isNotEmpty()) {
                                val shuffled = favoriteSongs.shuffled()
                                onPlaySong(shuffled.first(), shuffled)
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(AuraBorder)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("fav_shuffle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = AuraTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Shuffle", color = AuraTextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (favoriteSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = AuraTextMuted.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No favorites yet",
                        style = MaterialTheme.typography.titleMedium.copy(color = AuraTextPrimary)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap the heart icon on any track to save it in local storage for offline playback",
                        style = MaterialTheme.typography.bodyMedium.copy(color = AuraTextMuted),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onExploreClick,
                        colors = ButtonDefaults.buttonColors(containerColor = AuraPrimary),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Explore Music")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                itemsIndexed(favoriteSongs) { index, song ->
                    val isDownloaded = downloadedSongIds.contains(song.id)
                    val isDownloading = downloadingIds.contains(song.id)

                    SongListItem(
                        song = song,
                        index = index,
                        isCurrent = currentSong?.id == song.id,
                        isPlaying = isPlaying && currentSong?.id == song.id,
                        isFavorite = true,
                        isDownloaded = isDownloaded,
                        isDownloading = isDownloading,
                        onSongClick = { onPlaySong(song, favoriteSongs) },
                        onFavoriteToggle = { onFavoriteToggle(song.id) },
                        onDownloadToggle = { onDownloadSongToggle(song) }
                    )
                }
            }
        }
    }
}
