import json
import os
import re
from difflib import SequenceMatcher

BASE_DIR = os.path.dirname(os.path.abspath(__file__))

SONGS_FILE = os.path.join(BASE_DIR, "songs.json")
COVERS_DIR = os.path.join(BASE_DIR, "output", "covers")


def clean(text):
    text = text.lower()
    text = re.sub(r'[^a-z0-9 ]', ' ', text)
    text = re.sub(r'\s+', ' ', text)
    return text.strip()


def similarity(a, b):
    return SequenceMatcher(None, a, b).ratio()


with open(SONGS_FILE, "r", encoding="utf-8") as f:
    songs = json.load(f)

cover_files = [
    f for f in os.listdir(COVERS_DIR)
    if f.lower().endswith((".jpg", ".jpeg", ".png", ".webp"))
]

for song in songs:

    song_title = clean(song["title"])

    best_match = None
    best_score = 0

    for cover in cover_files:

        cover_name = clean(os.path.splitext(cover)[0])

        score = similarity(song_title, cover_name)

        # Word containment bonus
        if song_title in cover_name or cover_name in song_title:
            score += 0.3

        if score > best_score:
            best_score = score
            best_match = cover

    if best_match and best_score > 0.5:

        song["cover"] = f"covers/{best_match}"

        print(
            f"✓ {song['title']} -> {best_match}"
        )

    else:

        song["cover"] = "covers/default.jpg"

        print(
            f"✗ No Match: {song['title']}"
        )

with open(
    os.path.join(BASE_DIR, "songs_with_covers.json"),
    "w",
    encoding="utf-8"
) as f:

    json.dump(
        songs,
        f,
        ensure_ascii=False,
        indent=2
    )

print("\nFinished")