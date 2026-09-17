package com.example.auramusic.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
data class Song(
    val id: Int,
    val title: String,
    val artist: String,
    val album: String = "",
    val genre: List<String> = emptyList(),
    val year: String = "",
    val duration: Int = 0,
    val durationText: String = "0:00",
    val file: String = "",
    val cover: String = "",
    val bitrate: Double? = null,
    val sampleRate: Int? = null
) {
    fun getEffectiveCover(index: Int = id): String {
        if (cover.isNotBlank()) {
            return cover.replace(" ", "%20")
        }
        val fallbackIndex = (if (index >= 0) index else id) % ART_POOL.size
        return ART_POOL[fallbackIndex]
    }

    fun getAccentColor(index: Int = id): Color {
        val poolIndex = ((if (index >= 0) index else id) % ACCENT_POOL.size + ACCENT_POOL.size) % ACCENT_POOL.size
        return ACCENT_POOL[poolIndex]
    }

    val primaryGenre: String
        get() {
            val specific = genre.firstOrNull { 
                it.isNotBlank() && 
                !it.equals("Bollywood", ignoreCase = true) && 
                !it.equals("Music", ignoreCase = true) 
            }
            return specific ?: genre.firstOrNull { it.isNotBlank() } ?: "Music"
        }

    companion object {
        val ART_POOL = listOf(
            "https://images.unsplash.com/photo-1571330735066-03aaa9429d89?w=600&h=600&fit=crop",
            "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&h=600&fit=crop",
            "https://images.unsplash.com/photo-1510915361894-db8b60106cb1?w=600&h=600&fit=crop",
            "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&h=600&fit=crop",
            "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=600&h=600&fit=crop",
            "https://images.unsplash.com/photo-1488459716781-31db52582fe9?w=600&h=600&fit=crop"
        )

        val ACCENT_POOL = listOf(
            Color(0xFFC084FC), Color(0xFF60A5FA), Color(0xFFF472B6), Color(0xFFFBBF24), Color(0xFF34D399),
            Color(0xFFF87171), Color(0xFFA78BFA), Color(0xFF38BDF8), Color(0xFFFB923C), Color(0xFF4ADE80),
            Color(0xFF22D3EE), Color(0xFF818CF8), Color(0xFFE879F9), Color(0xFFFB7185), Color(0xFFFACC15),
            Color(0xFF2DD4BF), Color(0xFFA3E635), Color(0xFFF97316), Color(0xFFEF4444), Color(0xFF06B6D4),
            Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF14B8A6), Color(0xFFEAB308), Color(0xFF10B981),
            Color(0xFF3B82F6), Color(0xFFD946EF), Color(0xFFF43F5E), Color(0xFF0EA5E9), Color(0xFF84CC16),
            Color(0xFF7C3AED), Color(0xFF2563EB), Color(0xFFDB2777), Color(0xFFEA580C), Color(0xFF16A34A)
        )
    }
}
