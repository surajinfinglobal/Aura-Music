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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auramusic.model.Song
import com.example.auramusic.ui.components.SongListItem
import com.example.auramusic.ui.theme.AuraBorder
import com.example.auramusic.ui.theme.AuraCardBackground
import com.example.auramusic.ui.theme.AuraDarkBackground
import com.example.auramusic.ui.theme.AuraPrimary
import com.example.auramusic.ui.theme.AuraTextMuted
import com.example.auramusic.ui.theme.AuraTextPrimary
import com.example.auramusic.ui.theme.AuraTextSecondary

@Composable
fun SearchScreen(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedGenre: String,
    onGenreSelect: (String) -> Unit,
    searchResults: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    favorites: Set<Int>,
    downloadedSongIds: Set<Int> = emptySet(),
    onPlaySong: (Song, List<Song>) -> Unit,
    onFavoriteToggle: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val genreFilterOptions = listOf(
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("search_screen")
    ) {
        // Top Title & Search Field
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 10.dp)
        ) {
            Text(
                text = "Search",
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = AuraTextPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input_field"),
                placeholder = {
                    Text(
                        text = "Search by song, artist, album, genre...",
                        style = MaterialTheme.typography.bodyMedium.copy(color = AuraTextMuted)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon",
                        tint = AuraPrimary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = AuraTextMuted
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AuraPrimary,
                    unfocusedBorderColor = AuraBorder,
                    focusedContainerColor = AuraCardBackground,
                    unfocusedContainerColor = AuraCardBackground,
                    focusedTextColor = AuraTextPrimary,
                    unfocusedTextColor = AuraTextPrimary,
                    cursorColor = AuraPrimary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
            )
        }

        // Genre Filter Chips below Search Bar
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(genreFilterOptions) { genre ->
                val isSelected = selectedGenre.equals(genre, ignoreCase = true)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        // Toggle back to All if clicked again
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
                    modifier = Modifier.testTag("search_filter_$genre")
                )
            }
        }

        // Results Header with Quick Play Controls
        val isFilteredByGenre = !selectedGenre.equals("All", ignoreCase = true)
        val headerTitle = when {
            searchQuery.isNotBlank() && isFilteredByGenre -> "$selectedGenre matching \"$searchQuery\""
            isFilteredByGenre -> "$selectedGenre Tracks"
            searchQuery.isNotBlank() -> "Results for \"$searchQuery\""
            else -> "All Tracks"
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = headerTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = AuraTextPrimary,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Text(
                    text = "${searchResults.size} tracks available",
                    style = MaterialTheme.typography.labelSmall.copy(color = AuraTextMuted)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isFilteredByGenre || searchQuery.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            onGenreSelect("All")
                            onSearchQueryChange("")
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AuraPrimary
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(AuraPrimary.copy(alpha = 0.5f))
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Clear", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (searchResults.isNotEmpty()) {
                    Button(
                        onClick = { onPlaySong(searchResults.first(), searchResults) },
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

        // Search Results List
        if (searchResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = AuraTextMuted,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No tracks found",
                        style = MaterialTheme.typography.titleMedium.copy(color = AuraTextPrimary)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isFilteredByGenre) "No tracks match '$selectedGenre'. Try selecting 'All' or another category." else "Try searching with a different title or artist",
                        style = MaterialTheme.typography.bodyMedium.copy(color = AuraTextMuted)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            onGenreSelect("All")
                            onSearchQueryChange("")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AuraPrimary)
                    ) {
                        Text("Reset Filters")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 40.dp)
            ) {
                itemsIndexed(searchResults) { index, song ->
                    SongListItem(
                        song = song,
                        index = index,
                        isCurrent = currentSong?.id == song.id,
                        isPlaying = isPlaying && currentSong?.id == song.id,
                        isFavorite = favorites.contains(song.id),
                        isDownloaded = downloadedSongIds.contains(song.id),
                        onSongClick = { onPlaySong(song, searchResults) },
                        onFavoriteToggle = { onFavoriteToggle(song.id) }
                    )
                }
            }
        }
    }
}
