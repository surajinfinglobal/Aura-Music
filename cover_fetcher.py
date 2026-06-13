import os
import re
import requests
from urllib.parse import quote
from mutagen.mp3 import MP3
from mutagen.id3 import ID3, APIC

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MUSIC_FOLDER = os.path.join(BASE_DIR, "output", "music")

LASTFM_API_KEY = "e413fcd9056ba17edf8fb1905cdf4c4a"

COVER_FOLDER = os.path.join(MUSIC_FOLDER, "covers")
os.makedirs(COVER_FOLDER, exist_ok=True)


def clean_song_name(filename):
    name = os.path.splitext(filename)[0]

    name = re.sub(r'^\d+[_\- ]*', '', name)
    name = re.sub(r'SpotiDost', '', name, flags=re.I)

    name = name.replace("_", " ")
    name = re.sub(r'\s+', ' ', name)

    return name.strip()


# ---------- iTunes ----------
def cover_from_itunes(song):
    try:
        url = f"https://itunes.apple.com/search?term={quote(song)}&entity=song&limit=1"

        data = requests.get(url, timeout=15).json()

        if data["resultCount"] > 0:
            img = data["results"][0]["artworkUrl100"]
            return img.replace("100x100bb", "1000x1000bb")

    except:
        pass

    return None


# ---------- Deezer ----------
def cover_from_deezer(song):
    try:
        url = f"https://api.deezer.com/search?q={quote(song)}"

        data = requests.get(url, timeout=15).json()

        if data.get("data"):
            return data["data"][0]["album"]["cover_xl"]

    except:
        pass

    return None


# ---------- LastFM ----------
def cover_from_lastfm(song):
    try:
        url = (
            "http://ws.audioscrobbler.com/2.0/"
            f"?method=track.search"
            f"&track={quote(song)}"
            f"&api_key={LASTFM_API_KEY}"
            "&format=json"
            "&limit=1"
        )

        data = requests.get(url, timeout=15).json()

        matches = data["results"]["trackmatches"]["track"]

        if not matches:
            return None

        if isinstance(matches, list):
            artist = matches[0]["artist"]
            track = matches[0]["name"]
        else:
            artist = matches["artist"]
            track = matches["name"]

        url2 = (
            "http://ws.audioscrobbler.com/2.0/"
            f"?method=track.getInfo"
            f"&api_key={LASTFM_API_KEY}"
            f"&artist={quote(artist)}"
            f"&track={quote(track)}"
            "&format=json"
        )

        info = requests.get(url2, timeout=15).json()

        images = info.get("track", {}).get("album", {}).get("image", [])

        if images:
            return images[-1]["#text"]

    except:
        pass

    return None


def download_image(url, song):
    try:
        r = requests.get(url, timeout=20)

        safe = re.sub(r'[<>:"/\\|?*]', '', song)

        path = os.path.join(COVER_FOLDER, safe + ".jpg")

        with open(path, "wb") as f:
            f.write(r.content)

        return path

    except:
        return None


def embed_cover(mp3_path, cover_path):
    try:
        audio = MP3(mp3_path, ID3=ID3)

        try:
            audio.add_tags()
        except:
            pass

        with open(cover_path, "rb") as img:
            audio.tags.add(
                APIC(
                    encoding=3,
                    mime="image/jpeg",
                    type=3,
                    desc="Cover",
                    data=img.read(),
                )
            )

        audio.save()

        return True

    except Exception as e:
        print("Embed error:", e)
        return False


files = [
    f for f in os.listdir(MUSIC_FOLDER)
    if f.lower().endswith(".mp3")
]

total = len(files)

for i, file in enumerate(files, start=1):

    song = clean_song_name(file)

    print(f"\n[{i}/{total}] Searching: {song}")

    cover_url = (
        cover_from_itunes(song)
        or cover_from_deezer(song)
        or cover_from_lastfm(song)
    )

    if not cover_url:
        print("No cover found")
        continue

    cover_file = download_image(cover_url, song)

    if cover_file:
        embed_cover(
            os.path.join(MUSIC_FOLDER, file),
            cover_file
        )
        print("Done")

print("\nAll Finished!")