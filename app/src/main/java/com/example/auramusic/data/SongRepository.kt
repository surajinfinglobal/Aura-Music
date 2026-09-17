package com.example.auramusic.data

import android.content.Context
import com.example.auramusic.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

data class Playlist(
    val id: String,
    val title: String,
    val description: String,
    val coverUrl: String,
    val songIds: List<Int>
)

data class ArtistGroup(
    val name: String,
    val trackCount: Int,
    val sampleCover: String,
    val songs: List<Song>
)

class SongRepository(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val prefs = context.getSharedPreferences("auramusic_prefs", Context.MODE_PRIVATE)
    val likedStorageManager = LikedSongStorageManager(context)

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val favorites: StateFlow<Set<Int>> = likedStorageManager.likedSongIds
    val downloadedSongIds: StateFlow<Set<Int>> = likedStorageManager.downloadedSongIds

    private val _recentSongIds = MutableStateFlow<List<Int>>(emptyList())
    val recentSongIds: StateFlow<List<Int>> = _recentSongIds.asStateFlow()

    init {
        loadSavedState()
        loadSongsFromAssets()
    }

    private fun loadSavedState() {
        val recentString = prefs.getString("recently_played", "") ?: ""
        if (recentString.isNotBlank()) {
            _recentSongIds.value = recentString.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
        }
    }

    private fun loadSongsFromAssets() {
        scope.launch {
            try {
                val jsonString = withContext(Dispatchers.IO) {
                    context.assets.open("songs.json").bufferedReader().use { it.readText() }
                }
                val parsed = jsonParser.decodeFromString<List<Song>>(jsonString)
                _songs.value = parsed

                // Migration from legacy SharedPreferences if database is empty
                val favStrings = prefs.getStringSet("favorites", emptySet()) ?: emptySet()
                if (favStrings.isNotEmpty() && likedStorageManager.likedSongIds.value.isEmpty()) {
                    val favIds = favStrings.mapNotNull { it.toIntOrNull() }.toSet()
                    val toMigrate = parsed.filter { favIds.contains(it.id) }
                    for (song in toMigrate) {
                        likedStorageManager.toggleLikeSong(song)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleFavorite(songId: Int) {
        val song = _songs.value.find { it.id == songId }
        if (song != null) {
            likedStorageManager.toggleLikeSong(song)
        }
    }

    fun toggleFavorite(song: Song) {
        likedStorageManager.toggleLikeSong(song)
    }

    fun addToRecent(song: Song) {
        val current = _recentSongIds.value.toMutableList()
        current.remove(song.id)
        current.add(0, song.id)
        val trimmed = current.take(30)
        _recentSongIds.value = trimmed
        prefs.edit().putString("recently_played", trimmed.joinToString(",")).apply()
    }

    fun getTrendingSongs(): List<Song> {
        val all = _songs.value
        if (all.isEmpty()) return emptyList()
        // Curated trending songs (mix of popular hits from catalog)
        return all.take(15)
    }

    fun getPlaylists(): List<Playlist> {
        val all = _songs.value
        if (all.isEmpty()) return emptyList()

        return listOf(
            Playlist(
                id = "pl_aura_hits",
                title = "Aura Top Hits",
                description = "Most loved and streamed tracks of the season",
                coverUrl = all.getOrNull(0)?.getEffectiveCover(0) ?: Song.ART_POOL[0],
                songIds = all.take(20).map { it.id }
            ),
            Playlist(
                id = "pl_bollywood_melodies",
                title = "Bollywood Romance",
                description = "Heartwarming romantic hits and soulful melodies",
                coverUrl = all.getOrNull(5)?.getEffectiveCover(5) ?: Song.ART_POOL[1],
                songIds = all.filter { s ->
                    s.genre.any { g -> g.contains("Bollywood", true) || g.contains("Music", true) }
                }.take(20).map { it.id }
            ),
            Playlist(
                id = "pl_night_drive",
                title = "Midnight Serenade",
                description = "Chill and ambient soundscapes for the quiet hours",
                coverUrl = all.getOrNull(10)?.getEffectiveCover(10) ?: Song.ART_POOL[2],
                songIds = all.drop(15).take(20).map { it.id }
            ),
            Playlist(
                id = "pl_feel_good",
                title = "Feel Good Vibes",
                description = "Energetic, rhythmic, and uplifting melodies",
                coverUrl = all.getOrNull(15)?.getEffectiveCover(15) ?: Song.ART_POOL[3],
                songIds = all.drop(35).take(20).map { it.id }
            )
        )
    }

    fun getArtists(): List<ArtistGroup> {
        val all = _songs.value
        if (all.isEmpty()) return emptyList()

        return all.groupBy { it.artist.replace(" - Topic", "").trim() }
            .filter { it.key.isNotBlank() && it.value.isNotEmpty() }
            .map { (artistName, songsList) ->
                ArtistGroup(
                    name = artistName,
                    trackCount = songsList.size,
                    sampleCover = songsList.first().getEffectiveCover(songsList.first().id),
                    songs = songsList
                )
            }
            .sortedByDescending { it.trackCount }
            .take(15)
    }

    fun searchSongs(query: String, selectedGenre: String = "All"): List<Song> {
        val all = _songs.value
        val cleanQuery = query.trim()
        val cleanGenre = selectedGenre.trim()

        return all.filter { song ->
            val matchesQuery = cleanQuery.isBlank() ||
                    song.title.contains(cleanQuery, ignoreCase = true) ||
                    song.artist.contains(cleanQuery, ignoreCase = true) ||
                    song.album.contains(cleanQuery, ignoreCase = true) ||
                    song.genre.any { it.contains(cleanQuery, ignoreCase = true) }

            val matchesGenre = cleanGenre.equals("All", ignoreCase = true) ||
                    song.genre.any { it.equals(cleanGenre, ignoreCase = true) } ||
                    song.genre.any { it.replace("-", " ").contains(cleanGenre.replace("-", " "), ignoreCase = true) } ||
                    song.genre.any { it.replace("-", "").contains(cleanGenre.replace("-", ""), ignoreCase = true) } ||
                    song.title.contains(cleanGenre, ignoreCase = true) ||
                    song.album.contains(cleanGenre, ignoreCase = true)

            matchesQuery && matchesGenre
        }
    }

    fun getSongsByGenre(selectedGenre: String): List<Song> {
        return searchSongs(query = "", selectedGenre = selectedGenre)
    }
}
