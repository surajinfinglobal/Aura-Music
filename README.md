# Aura Music (Android)

Aura Music is a modern music streaming and player Android app rewritten with **Kotlin** and **Jetpack Compose**, inspired by the Aura Music web application.

## Features

- **Dynamic Ambient Theme**: Radiant accent glow matching each track's album art and vibe.
- **Rich Audio Playback Engine**: Powered by Android's `MediaPlayer` with streaming support, seek slider, elapsed/total timers, shuffle mode, and repeat options (Off, All, One).
- **Curated Catalog**: Over 480 tracks with title, artist, album, genre, duration, and cover artwork loaded directly from bundled assets.
- **Home Hub**:
  - Contextual time-of-day greeting
  - Quick search bar
  - Genre filter chips (Pop, Romantic, Hip-Hop, Electronic, Bollywood, etc.)
  - Horizontal scrolling sections: Trending Now, Curated Playlists, and Popular Artists
  - Recently played history
- **Interactive Search**: Real-time filtering across song titles, artists, albums, and genres.
- **Library**: Catalog breakdown with quick Shuffle All, organized by songs, artists, and albums.
- **Favorites**: Heart your favorite tracks with local persistent storage (`SharedPreferences`) and one-tap Play All / Shuffle.
- **Persistent Mini-Player**: Floats above navigation with track progress, play/pause, and skip controls, expanding smoothly to the fullscreen Now Playing screen.
- **Immersive Now Playing View**:
  - High-res artwork with radiant glow
  - Live animated audio visualizer waves
  - Full playback controls, seek bar, and an expandable Up-Next queue drawer

## Tech Stack

- **UI**: Jetpack Compose (Material 3)
- **Language**: Kotlin 2.2.10
- **Build System**: Gradle 9.3.1 (Kotlin DSL) with Android Gradle Plugin (AGP) 9.1.1
- **Image Loading**: Coil Compose
- **Serialization**: Kotlinx Serialization JSON
- **Target SDK**: Android 36 (Min SDK 26)
