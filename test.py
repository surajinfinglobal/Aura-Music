import json
import re
import shutil

JSON_FILE = "songs.json"

# Backup
shutil.copy(JSON_FILE, "songs_backup.json")

BAD_ARTISTS = {
    "t-series",
    "4kmusic-topic",
    "topic",
    "vevo",
    "zee music company",
    "sony music india",
    "tips official",
    "saregama music",
    "wave music",
    "music",
    "official",
    "audio",
    "video"
}

REMOVE_WORDS = [
    r"\(.*?\)",
    r"\[.*?\]",
    r"official video",
    r"official audio",
    r"full song",
    r"lyrical",
    r"lyrics",
    r"audio",
    r"video song",
    r"hd",
    r"4k",
    r"new punjabi songs?",
    r"new hindi songs?",
    r"latest hindi songs?",
    r"latest punjabi songs?",
]

def clean_title(title):
    original = title

    for pattern in REMOVE_WORDS:
        title = re.sub(pattern, "", title, flags=re.I)

    if ":" in title:
        title = title.split(":")[0]

    if "|" in title:
        title = title.split("|")[0]

    title = re.sub(r"\s+", " ", title)
    title = title.strip(" -_|")

    return title.strip()

def extract_artists(title, current_artist):
    if current_artist:
        ca = current_artist.strip()
        if ca.lower() not in BAD_ARTISTS and len(ca) > 2:
            return ca

    artists = []

    if ":" in title:
        metadata = title.split(":", 1)[1]

        for part in metadata.split("|"):
            part = part.strip()

            if not part:
                continue

            if len(part) > 40:
                continue

            low = part.lower()

            if low in BAD_ARTISTS:
                continue

            if any(x in low for x in [
                "official",
                "audio",
                "video",
                "song",
                "album",
                "movie",
                "film",
                "lyrics",
                "music"
            ]):
                continue

            artists.append(part)

    clean = []

    for a in artists:
        if a not in clean:
            clean.append(a)

    return ", ".join(clean[:4])

def guess_year(song):
    year = str(song.get("year", "")).strip()

    if re.match(r"^\d{4}$", year):
        return year

    title = song.get("title", "")

    m = re.search(r"(19\d{2}|20\d{2})", title)

    if m:
        return m.group(1)

    return ""

with open(JSON_FILE, "r", encoding="utf-8") as f:
    songs = json.load(f)

updated = 0

for song in songs:

    original_title = song.get("title", "")
    original_artist = song.get("artist", "")

    short_title = clean_title(original_title)
    artist = extract_artists(original_title, original_artist)

    if short_title:
        song["title"] = short_title
        song["album"] = short_title

    if artist:
        song["artist"] = artist

    song["year"] = guess_year(song)

    updated += 1

with open(JSON_FILE, "w", encoding="utf-8") as f:
    json.dump(songs, f, indent=2, ensure_ascii=False)

print(f"✅ Done! {updated} songs cleaned.")
print("📦 Backup saved as music_backup.json")