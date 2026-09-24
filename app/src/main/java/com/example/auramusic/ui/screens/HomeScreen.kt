package com.example.auramusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.auramusic.data.ArtistGroup
import com.example.auramusic.data.Playlist
import com.example.auramusic.model.Song
import com.example.auramusic.ui.components.SongListItem
import com.example.auramusic.ui.theme.AuraBorder
import com.example.auramusic.ui.theme.AuraCardBackground
import com.example.auramusic.ui.theme.AuraDarkBackground
import com.example.auramusic.ui.theme.AuraPrimary
import com.example.auramusic.ui.theme.AuraTextMuted
import com.example.auramusic.ui.theme.AuraTextPrimary
import com.example.auramusic.ui.theme.AuraTextSecondary
import java.util.Calendar

@Composable
fun HomeScreen(
    songs: List<Song>,
    isLoading: Boolean,
    currentSong: Song?,
    isPlaying: Boolean,
    favorites: Set<Int>,
    downloadedSongIds: Set<Int> = emptySet(),
    recentlyPlayed: List<Song>,
    trendingSongs: List<Song>,
    playlists: List<Playlist>,
    artists: List<ArtistGroup>,
    selectedGenre: String,
    onGenreSelect: (String) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onFavoriteToggle: (Int) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onShuffleRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good morning 🌅"
            in 12..16 -> "Good afternoon ☀️"
            in 17..21 -> "Good evening ✨"
            else -> "Good night 🌙"
        }
    }

    val genres = listOf(
        "All",
        "Romantic",
        "Pop",
        "Bollywood",
        "90s Retro",
        "Hip-Hop",
        "Lo-Fi",
        "Indie",
        "Party",
        "Soulful"
    )

    val isFiltered = !selectedGenre.equals("All", ignoreCase = true)
    val filteredGenreSongs = remember(songs, selectedGenre) {
        if (!isFiltered) emptyList()
        else {
            val clean = selectedGenre.trim()
            songs.filter { song ->
                song.genre.any { it.equals(clean, ignoreCase = true) } ||
                song.genre.any { it.replace("-", " ").contains(clean.replace("-", " "), ignoreCase = true) } ||
                song.genre.any { it.replace("-", "").contains(clean.replace("-", ""), ignoreCase = true) } ||
                song.title.contains(clean, ignoreCase = true) ||
                song.album.contains(clean, ignoreCase = true)
            }
        }
    }

    if (isLoading && songs.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading music library...",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Top Greeting Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                color = AuraTextPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Aura Music • By Suraj",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = AuraPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    // Actions: Randomize/Shuffle button & Avatar Circle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Shuffle / Randomize button to get new songs on screen anytime
                        IconButton(
                            onClick = onShuffleRefresh,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AuraCardBackground)
                                .border(1.dp, AuraBorder, CircleShape)
                                .testTag("home_shuffle_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle Random Songs",
                                tint = AuraPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Avatar Circle
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(AuraPrimary, Color(0xFFEC4899))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "S",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Quick Search Bar Affordance
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(AuraCardBackground)
                        .border(1.dp, AuraBorder, RoundedCornerShape(14.dp))
                        .clickable(onClick = onNavigateToSearch)
                        .padding(horizontal = 16.dp, vertical = 13.dp)
                        .testTag("home_search_bar"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = AuraTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Songs, artists, playlists...",
                        style = MaterialTheme.typography.bodyMedium.copy(color = AuraTextMuted)
                    )
                }
            }
        }

        // Genre Filter Tabs below Search Bar
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(genres) { genre ->
                    val isSelected = selectedGenre.equals(genre, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected && genre != "All") {
                                onGenreSelect("All")
                            } else {
                                onGenreSelect(genre)
                            }
                        },
                        label = {
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        leadingIcon = if (genre != "All") {
                            {
                                val iconEmoji = when (genre) {
                                    "Romantic" -> "❤️"
                                    "Pop" -> "⚡"
                                    "Bollywood" -> "🎬"
                                    "90s Retro" -> "📻"
                                    "Hip-Hop" -> "🎧"
                                    "Lo-Fi" -> "☕"
                                    "Indie" -> "🎸"
                                    "Party" -> "🎉"
                                    "Soulful" -> "🕊️"
                                    else -> "🎵"
                                }
                                Text(text = iconEmoji, fontSize = 12.sp)
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AuraPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = AuraCardBackground,
                            labelColor = AuraTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) AuraPrimary else AuraBorder
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("genre_chip_$genre")
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Conditionally render filtered songs or full Home sections
        if (isFiltered) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "$selectedGenre Hits",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = AuraTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "${filteredGenreSongs.size} tracks curated for you",
                                style = MaterialTheme.typography.labelSmall.copy(color = AuraTextMuted)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { onGenreSelect("All") },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AuraPrimary),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(AuraPrimary.copy(alpha = 0.5f))
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Clear", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            if (filteredGenreSongs.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { onPlaySong(filteredGenreSongs.first(), filteredGenreSongs) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AuraPrimary,
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play All",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Play All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            if (filteredGenreSongs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No songs found for $selectedGenre",
                                style = MaterialTheme.typography.titleMedium.copy(color = AuraTextPrimary)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onGenreSelect("All") },
                                colors = ButtonDefaults.buttonColors(containerColor = AuraPrimary)
                            ) {
                                Text("Show All Songs")
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(filteredGenreSongs) { idx, song ->
                    Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                        SongListItem(
                            song = song,
                            index = idx,
                            isCurrent = currentSong?.id == song.id,
                            isPlaying = isPlaying && currentSong?.id == song.id,
                            isFavorite = favorites.contains(song.id),
                            isDownloaded = downloadedSongIds.contains(song.id),
                            onSongClick = { onPlaySong(song, filteredGenreSongs) },
                            onFavoriteToggle = { onFavoriteToggle(song.id) }
                        )
                    }
                }
            }
        } else {
            // 🔥 Trending Now Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔥 Trending Now",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = AuraTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "See all",
                            style = MaterialTheme.typography.labelMedium.copy(color = AuraPrimary),
                            modifier = Modifier
                                .clickable(onClick = onNavigateToLibrary)
                                .padding(4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(trendingSongs) { index, song ->
                            TrendingSongCard(
                                song = song,
                                index = index,
                                isCurrent = currentSong?.id == song.id,
                                isPlaying = isPlaying && currentSong?.id == song.id,
                                onPlay = { onPlaySong(song, trendingSongs) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // 🎵 Recommended Playlists
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎵 Curated Playlists",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = AuraTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(playlists) { pl ->
                            PlaylistCard(
                                playlist = pl,
                                onSelect = {
                                    val plSongs = songs.filter { s -> pl.songIds.contains(s.id) }
                                    if (plSongs.isNotEmpty()) {
                                        onPlaySong(plSongs.first(), plSongs)
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // 🎤 Popular Artists
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "🎤 Popular Artists",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = AuraTextPrimary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(artists) { artist ->
                            ArtistCard(
                                artist = artist,
                                onClick = {
                                    if (artist.songs.isNotEmpty()) {
                                        onPlaySong(artist.songs.first(), artist.songs)
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // 🕓 Recently Played or Catalog Preview
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    val listToShow = if (recentlyPlayed.isNotEmpty()) recentlyPlayed else songs.take(10)
                    val sectionTitle = if (recentlyPlayed.isNotEmpty()) "🕓 Recently Played" else "✨ Featured Catalog"

                    Text(
                        text = sectionTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = AuraTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    listToShow.forEachIndexed { idx, song ->
                        SongListItem(
                            song = song,
                            index = idx,
                            isCurrent = currentSong?.id == song.id,
                            isPlaying = isPlaying && currentSong?.id == song.id,
                            isFavorite = favorites.contains(song.id),
                            isDownloaded = downloadedSongIds.contains(song.id),
                            onSongClick = { onPlaySong(song, listToShow) },
                            onFavoriteToggle = { onFavoriteToggle(song.id) }
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // App Author & Credits Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AuraCardBackground)
                            .border(1.dp, AuraBorder, RoundedCornerShape(16.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Aura Music Player",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AuraTextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Author & Developer: Suraj",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = AuraPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Crafted with ❤️ • All rights & music credits to respective artists",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = AuraTextMuted
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrendingSongCard(
    song: Song,
    index: Int,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit
) {
    val accent = song.getAccentColor(index)

    Box(
        modifier = Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AuraCardBackground)
            .border(1.dp, if (isCurrent) accent else AuraBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onPlay)
            .testTag("trending_card_${song.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Album art with Play button overlay
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.2f))
            ) {
                AsyncImage(
                    model = song.getEffectiveCover(index),
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Quick Play circular button
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(36.dp)
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(if (isCurrent && isPlaying) accent else AuraDarkBackground.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCurrent) accent else AuraTextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = song.artist.replace(" - Topic", ""),
                style = MaterialTheme.typography.bodyMedium.copy(color = AuraTextSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PlaylistCard(
    playlist: Playlist,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AuraCardBackground)
            .border(1.dp, AuraBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect)
            .testTag("playlist_card_${playlist.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            AsyncImage(
                model = playlist.coverUrl,
                contentDescription = playlist.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = playlist.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = AuraTextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = playlist.description,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = AuraTextSecondary,
                    fontSize = 11.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ArtistCard(
    artist: ArtistGroup,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(90.dp)
            .clickable(onClick = onClick)
            .testTag("artist_card_${artist.name}")
    ) {
        AsyncImage(
            model = artist.sampleCover,
            contentDescription = artist.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .border(2.dp, AuraBorder, CircleShape)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = AuraTextPrimary
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "${artist.trackCount} tracks",
            style = MaterialTheme.typography.labelMedium.copy(
                color = AuraTextMuted,
                fontSize = 11.sp
            )
        )
    }
}
