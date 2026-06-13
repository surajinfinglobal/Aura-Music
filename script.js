'use strict';

/* ═══════════════════════════════
   PLAYLIST DATA — loaded from songs.json
═══════════════════════════════ */

// Accent color pool — assigned by song index when JSON has none
const ACCENT_POOL = [
  '#c084fc','#60a5fa','#f472b6','#fbbf24','#34d399',
  '#f87171','#a78bfa','#38bdf8','#fb923c','#4ade80',

  '#22d3ee','#818cf8','#e879f9','#fb7185','#facc15',
  '#2dd4bf','#a3e635','#f97316','#ef4444','#06b6d4',

  '#8b5cf6','#ec4899','#14b8a6','#eab308','#10b981',
  '#3b82f6','#d946ef','#f43f5e','#0ea5e9','#84cc16',

  '#7c3aed','#2563eb','#db2777','#ea580c','#16a34a',
  '#0891b2','#9333ea','#be123c','#ca8a04','#059669',

  '#6366f1','#c026d3','#dc2626','#0284c7','#65a30d',
  '#f59e0b','#0d9488','#7e22ce','#e11d48','#0284c7',

  '#4f46e5','#9333ea','#0f766e','#b45309','#15803d',
  '#0369a1','#a21caf','#991b1b','#854d0e','#047857',

  '#1d4ed8','#be185d'
];

// Fallback album art images (cycled by index)
const ART_POOL = [
  'https://images.unsplash.com/photo-1571330735066-03aaa9429d89?w=600&h=600&fit=crop',
  'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&h=600&fit=crop',
  'https://images.unsplash.com/photo-1510915361894-db8b60106cb1?w=600&h=600&fit=crop',
  'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&h=600&fit=crop',
  'https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=600&h=600&fit=crop',
  'https://images.unsplash.com/photo-1488459716781-31db52582fe9?w=600&h=600&fit=crop',
];

let PLAYLIST = [];

/**
 * Normalize a raw JSON song object into the internal PLAYLIST format.
 * Maps: file→src, durationText→duration, assigns img + accentColor if absent.
 */
function normalizeSong(raw, idx) {
  return {
    title:       raw.title       || 'Unknown Title',
    artist:      raw.artist      || 'Unknown Artist',
    album:       raw.album       || '',
    genre:       Array.isArray(raw.genre) ? raw.genre : (raw.genre ? [raw.genre] : []),
    year:        raw.year        || '',
    duration:    raw.durationText|| raw.duration || '0:00',
    bitrate:     raw.bitrate     || null,
    sampleRate:  raw.sampleRate  || null,
    src:         raw.file        || raw.src || '',
    img:         raw.img         || raw.cover || ART_POOL[idx % ART_POOL.length],
    accentColor: raw.accentColor || ACCENT_POOL[idx % ACCENT_POOL.length],
  };
}

/**
 * Load songs.json — works on file://, http://, and https://.
 * Uses XMLHttpRequest which handles file:// same-folder access without CORS errors.
 * Falls back to built-in playlist if JSON is missing or unreadable.
 */
async function loadSongsJSON() {
  // Try XHR — works on file:// for same-directory files (no CORS issue)
  const xhrLoaded = await new Promise((resolve) => {
    try {
      const xhr = new XMLHttpRequest();
      xhr.open('GET', 'songs.json', true);
      xhr.onload = () => {
        if (xhr.status === 0 || xhr.status === 200) { // 0 = file://, 200 = http://
          try {
            const data = JSON.parse(xhr.responseText);
            PLAYLIST = data.map(normalizeSong);
            console.log(`[Aura] Loaded ${PLAYLIST.length} songs from songs.json`);
            resolve(true);
          } catch (parseErr) {
            console.warn('[Aura] songs.json parse error:', parseErr);
            resolve(false);
          }
        } else { resolve(false); }
      };
      xhr.onerror = () => resolve(false);
      xhr.send();
    } catch (e) { resolve(false); }
  });

  if (xhrLoaded) { onPlaylistReady(); return; }

  // XHR failed — use built-in fallback
  console.warn('[Aura] songs.json not found, using built-in playlist.');
    // ── FALLBACK built-in playlist (used when songs.json is absent) ──
    PLAYLIST = [
  {
    title: 'Levitating', artist: 'Dua Lipa', album: 'Future Nostalgia',
    img: 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&h=600&fit=crop',
    src: 'music/02_Agar_Tum_Saath_Ho_From_Tamasha_SpotiDost.mp3',
    duration: '3:23', accentColor: '#60a5fa'
  },
  {
    title: 'As It Was', artist: 'Harry Styles', album: "Harry's House",
    img: 'https://images.unsplash.com/photo-1510915361894-db8b60106cb1?w=600&h=600&fit=crop',
    src: 'music/03_Enna_Sona_SpotiDost.mp3',
    duration: '2:37', accentColor: '#f472b6'
  },
  {
    title: 'Anti-Hero', artist: 'Taylor Swift', album: 'Midnights',
    img: 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&h=600&fit=crop',
    src: 'music/04_Hamari_Adhuri_Kahani_Title_Track_SpotiDost.mp3',
    duration: '3:20', accentColor: '#fbbf24'
  },
  {
    title: 'Happier Than Ever', artist: 'Billie Eilish', album: 'Happier Than Ever',
    img: 'https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=600&h=600&fit=crop',
    src: 'music/05_Heeriye_feat._Arijit_Singh_SpotiDost.mp3',
    duration: '4:58', accentColor: '#34d399'
  },
  {
    title: 'Good 4 U', artist: 'Olivia Rodrigo', album: 'SOUR',
    img: 'https://images.unsplash.com/photo-1488459716781-31db52582fe9?w=600&h=600&fit=crop',
    src: 'music/06_Mast_Magan_From_2_States_SpotiDost.mp3',
    duration: '2:58', accentColor: '#f87171'
  },
  {
    title: 'Good 4 U', artist: 'Olivia Rodrigo', album: 'SOUR',
    img: 'https://images.unsplash.com/photo-1488459716781-31db52582fe9?w=600&h=600&fit=crop',
    src: 'music/07_Sajni_From_Laapataa_Ladies_SpotiDost.mp3',
    duration: '2:58', accentColor: '#f87171'
  },
  {
    title: 'Good 4 U', artist: 'Olivia Rodrigo', album: 'SOUR',
    img: 'https://images.unsplash.com/photo-1488459716781-31db52582fe9?w=600&h=600&fit=crop',
    src: 'music/08_Chaleya_SpotiDost.mp3',
    duration: '2:58', accentColor: '#f87171'
  },
  {
    title: 'Good 4 U', artist: 'Olivia Rodrigo', album: 'SOUR',
    img: 'https://images.unsplash.com/photo-1488459716781-31db52582fe9?w=600&h=600&fit=crop',
    src: 'music/09_Ijazat_SpotiDost.mp3',
    duration: '2:58', accentColor: '#f87171'
  },
  {
    title: 'Good 4 U', artist: 'Olivia Rodrigo', album: 'SOUR',
    img: 'https://images.unsplash.com/photo-1488459716781-31db52582fe9?w=600&h=600&fit=crop',
    src: 'music/10_Khamoshiyan_SpotiDost.mp3',
    duration: '2:58', accentColor: '#f87171'
  },
  {
    title: 'Good 4 U', artist: 'Olivia Rodrigo', album: 'SOUR',
    img: 'https://images.unsplash.com/photo-1488459716781-31db52582fe9?w=600&h=600&fit=crop',
    src: 'music/11_Tera_Yaar_Hoon_Main_From_Sonu_Ke_Titu_Ki_Sweety_SpotiDost.mp3',
    duration: '2:58', accentColor: '#f87171'
  },
  {
    title: 'Good 4 U', artist: 'Olivia Rodrigo', album: 'SOUR',
    img: 'https://images.unsplash.com/photo-1488459716781-31db52582fe9?w=600&h=600&fit=crop',
    src: 'music/12_Bandeya_-_From_Dil_Juunglee_SpotiDost.mp3',
    duration: '2:58', accentColor: '#f87171'
  },
  {
    title: 'Good 4 U', artist: 'Olivia Rodrigo', album: 'SOUR',
    img: 'https://images.unsplash.com/photo-1488459716781-31db52582fe9?w=600&h=600&fit=crop',
    src: 'music/13_Oonchi_Oonchi_Deewarein_SpotiDost.mp3',
    duration: '2:58', accentColor: '#f87171'
  },
  {
    title: 'Good 4 U', artist: 'Olivia Rodrigo', album: 'SOUR',
    img: 'https://images.unsplash.com/photo-1488459716781-31db52582fe9?w=600&h=600&fit=crop',
    src: 'music/14_Tu_Har_Lamha_From_Khamoshiyan_SpotiDost.mp3',
    duration: '2:58', accentColor: '#f87171'
  },
  {
    title: 'Good 4 U', artist: 'Olivia Rodrigo', album: 'SOUR',
    img: 'https://images.unsplash.com/photo-1488459716781-31db52582fe9?w=600&h=600&fit=crop',
    src: 'music/15_Soulmate_SpotiDost.mp3',
    duration: '2:58', accentColor: '#f87171'
  },
  {
    title: 'Good 4 U', artist: 'Olivia Rodrigo', album: 'SOUR',
    img: 'https://images.unsplash.com/photo-1488459716781-31db52582fe9?w=600&h=600&fit=crop',
    src: 'music/16_Zaalima_SpotiDost.mp3',
    duration: '2:58', accentColor: '#f87171'
  },
  {
    title: 'Good 4 U', artist: 'Olivia Rodrigo', album: 'SOUR',
    img: 'https://images.unsplash.com/photo-1488459716781-31db52582fe9?w=600&h=600&fit=crop',
    src: 'music/17_Muskurane_-_Romantic_SpotiDost.mp3',
    duration: '2:58', accentColor: '#f87171'
  },
  {
    title: 'Good 4 U', artist: 'Olivia Rodrigo', album: 'SOUR',
    img: 'https://images.unsplash.com/photo-1488459716781-31db52582fe9?w=600&h=600&fit=crop',
    src: 'music/18_Mareez_-_E_-_Ishq_SpotiDost.mp3',
    duration: '2:58', accentColor: '#f87171'
  },
  {
    title: 'Good 4 U', artist: 'Olivia Rodrigo', album: 'SOUR',
    img: 'https://images.unsplash.com/photo-1488459716781-31db52582fe9?w=600&h=600&fit=crop',
    src: 'music/19_Tenu_Sang_Rakhna_SpotiDost.mp3',
    duration: '2:58', accentColor: '#f87171'
  },
  {
    title: 'Good 4 U', artist: 'Olivia Rodrigo', album: 'SOUR',
    img: 'https://images.unsplash.com/photo-1488459716781-31db52582fe9?w=600&h=600&fit=crop',
    src: 'music/20_Laal_Ishq_SpotiDost.mp3',
    duration: '2:58', accentColor: '#f87171'
  },
    ]; // end fallback PLAYLIST

  onPlaylistReady();
}

/* State */
let currentIdx   = 0;
let isPlaying    = false;
let shuffleOn    = false;
let repeatMode   = 0; // 0=off 1=all 2=one
let favorites    = JSON.parse(localStorage.getItem('aura_favs') || '[]');
let currentTheme = localStorage.getItem('aura_theme') || '';

const audio = new Audio();
audio.volume = 0.65;

/* ═══════════════════════════════
   UTILS
═══════════════════════════════ */
const $ = (id) => document.getElementById(id);
const fmt = (t) => isNaN(t) || !t ? '0:00' : `${Math.floor(t/60)}:${String(Math.floor(t%60)).padStart(2,'0')}`;

function isFav(idx) { return favorites.includes(idx); }

function saveFavs() {
  localStorage.setItem('aura_favs', JSON.stringify(favorites));
}

/* ═══════════════════════════════
   TOAST NOTIFICATION SYSTEM
═══════════════════════════════ */
function showToast(type, title, subtitle) {
  const icons = { success: 'fa-check', info: 'fa-info', warning: 'fa-triangle-exclamation', error: 'fa-xmark' };
  const container = $('toast-container');
  const t = document.createElement('div');
  t.className = 'toast';
  t.innerHTML = `
    <div class="toast-icon ${type}"><i class="fa-solid ${icons[type] || 'fa-info'}"></i></div>
    <div><div class="toast-text">${title}</div>${subtitle ? `<div class="toast-sub">${subtitle}</div>` : ''}</div>
  `;
  container.appendChild(t);
  requestAnimationFrame(() => {
    requestAnimationFrame(() => t.classList.add('show'));
  });
  setTimeout(() => {
    t.classList.add('hide');
    setTimeout(() => t.remove(), 400);
  }, 3000);
}

/* ═══════════════════════════════
   PLAYER CORE
═══════════════════════════════ */
function loadTrack(idx, autoPlay) {
  currentIdx = ((idx % PLAYLIST.length) + PLAYLIST.length) % PLAYLIST.length;
  const t = PLAYLIST[currentIdx];
  audio.src = t.src;

  // Update bar
  $('bar-art').src = t.img;
  $('bar-title').textContent = t.title;
  $('bar-artist').textContent = t.artist;

  // Update NP screen
  $('np-art').src = t.img;
  $('np-title').textContent = t.title;
  $('np-artist').textContent = t.artist;
  $('np-album-badge').textContent = t.album;

  // Update FS
  $('fs-art').src = t.img;
  $('fs-title').textContent = t.title;
  $('fs-artist').textContent = t.artist;

  // Dynamic theme based on song accent
  document.documentElement.style.setProperty('--accent', t.accentColor);
  const hexToRgb = (hex) => {
    const r = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    return r ? `${parseInt(r[1],16)},${parseInt(r[2],16)},${parseInt(r[3],16)}` : '192,132,252';
  };
  document.documentElement.style.setProperty('--accent-rgb', hexToRgb(t.accentColor));
  document.documentElement.style.setProperty('--accent-glow', t.accentColor.replace('#','rgba(').replace(/(.*)\)/,'$1,0.45)') || 'rgba(192,132,252,0.45)');

  updateFavUI();
  renderQueue();

  if (autoPlay) { audio.play().then(() => setPlayUI(true)).catch(() => {}); }
}

function playSong(idx) {
  loadTrack(idx, true);
  openNowPlaying();
  showToast('info', 'Now Playing', PLAYLIST[currentIdx].title + ' — ' + PLAYLIST[currentIdx].artist);
}

function setPlayUI(playing) {
  isPlaying = playing;
  const icon = playing ? 'fa-pause' : 'fa-play';
  $('bar-play-icon').className = `fa-solid ${icon}`;
  $('fs-play-icon').className  = `fa-solid ${icon}`;
  const art = $('np-art');
  const fsArt = $('fs-art');
  if (playing) { art.classList.add('spinning'); fsArt.classList.add('spinning'); }
  else         { art.classList.remove('spinning'); fsArt.classList.remove('spinning'); }
  // Visualizer activity
  document.querySelectorAll('.vis-bar').forEach(b => {
    b.style.animationPlayState = playing ? 'running' : 'paused';
  });
}

function toggleMainPlay() {
  if (audio.paused) {
    audio.play().then(() => setPlayUI(true)).catch(() => showToast('warning', 'Playback blocked', 'Click play again'));
  } else {
    audio.pause(); setPlayUI(false);
  }
}

function nextTrack() {
  if (shuffleOn) {
    const r = Math.floor(Math.random() * PLAYLIST.length);
    loadTrack(r, isPlaying);
  } else {
    loadTrack(currentIdx + 1, isPlaying);
  }
}

function prevTrack() {
  if (audio.currentTime > 3) { audio.currentTime = 0; return; }
  loadTrack(currentIdx - 1, isPlaying);
}

function toggleShuffle() {
  shuffleOn = !shuffleOn;
  $('shuffle-btn').classList.toggle('active', shuffleOn);
  $('fs-shuffle').classList.toggle('active', shuffleOn);
  showToast('info', shuffleOn ? 'Shuffle On' : 'Shuffle Off', '');
}

function toggleRepeat() {
  repeatMode = (repeatMode + 1) % 3;
  const labels = ['', 'Repeat All', 'Repeat One'];
  const icons  = ['fa-repeat', 'fa-repeat', 'fa-rotate-right'];
  $('repeat-btn').classList.toggle('active', repeatMode > 0);
  $('repeat-btn').querySelector('i').className = `fa-solid ${icons[repeatMode]} fa-sm`;
  $('fs-repeat').classList.toggle('active', repeatMode > 0);
  $('fs-repeat').querySelector('i').className = `fa-solid ${icons[repeatMode]}`;
  if (repeatMode > 0) showToast('info', labels[repeatMode], '');
}

function toggleFavorite() {
  const idx = currentIdx;
  if (isFav(idx)) {
    favorites = favorites.filter(i => i !== idx);
    showToast('info', 'Removed from Favorites', PLAYLIST[idx].title);
  } else {
    favorites.push(idx);
    showToast('success', 'Added to Favorites', PLAYLIST[idx].title);
  }
  saveFavs();
  updateFavUI();
  renderFavorites();
}

function updateFavUI() {
  const on = isFav(currentIdx);
  const cls = on ? 'fa-solid fa-heart' : 'fa-regular fa-heart';
  const color = on ? 'color:var(--accent3)' : '';
  [$('fav-btn'), $('fav-btn-np'), $('fs-fav')].forEach(el => {
    if (!el) return;
    el.querySelector('i').className = cls;
    el.style.cssText = on ? 'color:var(--accent3)' : '';
  });
}

/* Audio events */
audio.addEventListener('timeupdate', () => {
  const pct = audio.duration ? (audio.currentTime / audio.duration) * 100 : 0;
  const fillCss = pct + '%';
  $('np-fill').style.width  = fillCss;
  $('fs-fill').style.width  = fillCss;
  $('np-cur').textContent   = fmt(audio.currentTime);
  $('np-dur').textContent   = fmt(audio.duration);
  $('fs-cur').textContent   = fmt(audio.currentTime);
  $('fs-dur').textContent   = fmt(audio.duration);
});

audio.addEventListener('ended', () => {
  if (repeatMode === 2) { audio.currentTime = 0; audio.play(); }
  else nextTrack();
});

/* ── SEEK ── */
function makeSeekable(barEl, fsBarEl) {
  let seeking = false;
  function seekTo(x, el) {
    const r = el.getBoundingClientRect();
    const p = Math.min(Math.max((x - r.left) / r.width, 0), 1);
    if (audio.duration) audio.currentTime = p * audio.duration;
  }
  [barEl, fsBarEl].forEach(el => {
    if (!el) return;
    el.addEventListener('pointerdown', e => { seeking = true; seekTo(e.clientX, el); e.stopPropagation(); });
  });
  window.addEventListener('pointermove', e => { if (seeking) seekTo(e.clientX, barEl); });
  window.addEventListener('pointerup',   e => { if (seeking) { seekTo(e.clientX, barEl); seeking = false; } });
}
makeSeekable($('np-bar'), $('fs-bar'));

/* ── VOLUME ── */
function makeVolumeSlider(wrapId, fillId, thumbId) {
  const wrap  = $(wrapId), fill = $(fillId), thumb = $(thumbId);
  if (!wrap) return;
  let dragging = false;
  function setVol(x) {
    const r = wrap.getBoundingClientRect();
    const p = Math.min(Math.max((x - r.left) / r.width, 0), 1);
    audio.volume = p;
    const pct = (p * 100) + '%';
    fill.style.width  = pct;
    thumb.style.left  = `calc(${pct} - 5px)`;
    // Sync all volume sliders
    ['vol-fill','fs-vol-fill'].forEach(id => { const el=$(id); if(el) el.style.width=pct; });
    ['vol-thumb','fs-vol-thumb'].forEach(id => { const el=$(id); if(el) el.style.left=`calc(${pct} - 5px)`; });
  }
  wrap.addEventListener('pointerdown', e => { dragging = true; setVol(e.clientX); e.stopPropagation(); });
  window.addEventListener('pointermove', e => { if (dragging) setVol(e.clientX); });
  window.addEventListener('pointerup',   e => { if (dragging) { setVol(e.clientX); dragging = false; } });
}
makeVolumeSlider('vol-slider',    'vol-fill',    'vol-thumb');
makeVolumeSlider('fs-vol-slider', 'fs-vol-fill', 'fs-vol-thumb');

/* ═══════════════════════════════
   ON PLAYLIST READY — runs after JSON load
═══════════════════════════════ */
function onPlaylistReady() {
  renderHomeSections();
  renderHomeSuggestions();
  renderAllSongsList();
  loadTrack(0, false);
  renderFavorites();
  const totalSongsEl = $('total-songs-count');
  if (totalSongsEl) {
    totalSongsEl.textContent = PLAYLIST.length;
    totalSongsEl.dataset.count = PLAYLIST.length;
  }
  initReveal($('screen-home'));
}

/* ═══════════════════════════════
   RENDER HOME SECTIONS FROM PLAYLIST
═══════════════════════════════ */
function renderHomeSections() {

  /* ── 1. TRENDING NOW — all songs as horizontal cards ── */
  const trendingEl = $('home-trending');
  if (trendingEl) {
    trendingEl.innerHTML = '';
    PLAYLIST.forEach((t, i) => {
      const card = document.createElement('div');
      card.className = 'trending-card' + (i < 5 ? ' anim-in' : '');
      card.innerHTML = `
        <img src="${t.img}" alt="${t.title}">
        <span class="trending-rank">#${i + 1}</span>
        <div class="trending-play"><i class="fa-solid fa-play"></i></div>
        <div class="trending-card-info">
          <div class="title">${t.title}</div>
          <div class="artist">${t.artist}</div>
        </div>
      `;
      card.onclick = () => playSong(i);
      // Re-apply 3D tilt
      card.addEventListener('mousemove', e => {
        const r = card.getBoundingClientRect();
        const x = (e.clientX - r.left) / r.width  - 0.5;
        const y = (e.clientY - r.top)  / r.height - 0.5;
        card.style.transform = `translateY(-4px) scale(1.02) rotateX(${-y*6}deg) rotateY(${x*6}deg)`;
      });
      card.addEventListener('mouseleave', () => { card.style.transform = ''; });
      trendingEl.appendChild(card);
    });
  }

  /* ── 2. RECENTLY PLAYED — all songs as rows ── */
  const recentEl = $('home-recent');
  if (recentEl) {
    recentEl.innerHTML = '';
    PLAYLIST.forEach((t, i) => {
      const row = document.createElement('div');
      row.className = 'recent-row';
      row.innerHTML = `
        <img src="${t.img}" alt="${t.title}">
        <div class="info">
          <div class="name">${t.title}</div>
          <div class="meta">${t.artist}${t.album ? ' · ' + t.album : ''}</div>
        </div>
        <span class="duration">${t.duration}</span>
        <i class="fa-solid fa-play play-icon"></i>
      `;
      row.onclick = () => playSong(i);
      recentEl.appendChild(row);
    });
  }

  /* ── 3. PLAYLISTS — group songs into albums/genres as playlist cards ── */
  const playlistEl = $('home-playlists');
  if (playlistEl) {
    playlistEl.innerHTML = '';
    // Group by album — each unique album becomes a "playlist card"
    const albumMap = {};
    PLAYLIST.forEach((t, i) => {
      const key = t.album || 'Singles';
      if (!albumMap[key]) albumMap[key] = { name: key, songs: [], img: t.img, firstIdx: i };
      albumMap[key].songs.push(i);
    });
    Object.values(albumMap).forEach(album => {
      const card = document.createElement('div');
      card.className = 'playlist-card';
      card.innerHTML = `
        <div class="playlist-cover">
          <img src="${album.img}" alt="${album.name}">
          <span class="glass-badge">${album.songs.length} track${album.songs.length !== 1 ? 's' : ''}</span>
        </div>
        <div class="playlist-info">
          <div class="title">${album.name}</div>
          <div class="count">${album.songs.length} songs</div>
        </div>
      `;
      card.onclick = () => playSong(album.firstIdx);
      // 3D tilt
      card.addEventListener('mousemove', e => {
        const r = card.getBoundingClientRect();
        const x = (e.clientX - r.left) / r.width  - 0.5;
        const y = (e.clientY - r.top)  / r.height - 0.5;
        card.style.transform = `translateY(-4px) scale(1.02) rotateX(${-y*6}deg) rotateY(${x*6}deg)`;
      });
      card.addEventListener('mouseleave', () => { card.style.transform = ''; });
      playlistEl.appendChild(card);
    });
  }

  /* ── 4. TOP ARTISTS — unique artists extracted from PLAYLIST ── */
  const artistsEl = $('home-artists');
  if (artistsEl) {
    artistsEl.innerHTML = '';
    const artistMap = {};
    PLAYLIST.forEach((t, i) => {
      if (!artistMap[t.artist]) {
        artistMap[t.artist] = {
          name: t.artist,
          img: t.img,
          genre: Array.isArray(t.genre) ? t.genre[0] : (t.genre || 'Music'),
          firstIdx: i
        };
      }
    });
    Object.values(artistMap).forEach(a => {
      const card = document.createElement('div');
      card.className = 'artist-card';
      card.innerHTML = `
        <img src="${a.img}" alt="${a.name}">
        <div class="name">${a.name}</div>
        <div class="genre">${a.genre}</div>
      `;
      card.onclick = () => {
        playSong(a.firstIdx);
        showToast('info', a.name, 'Now playing…');
      };
      artistsEl.appendChild(card);
    });
  }
}

/**
 * Populate the home search suggestions with first 5 songs from PLAYLIST.
 * Replaces the hardcoded suggestion-item elements inside #home-suggestions.
 */
function renderHomeSuggestions() {
  const container = $('home-suggestions');
  if (!container) return;
  // Remove old hardcoded suggestion-items only (keep glass-filter/overlay/specular)
  container.querySelectorAll('.suggestion-item').forEach(el => el.remove());
  const icons = ['fa-fire','fa-star','fa-music','fa-music','fa-music'];
  PLAYLIST.slice(0, 5).forEach((t, i) => {
    const si = document.createElement('div');
    si.className = 'suggestion-item';
    si.innerHTML = `<i class="fa-solid ${icons[i]||'fa-music'}"></i><span>${t.title} — ${t.artist}</span>`;
    si.onclick = () => { playSong(i); $('home-suggestions').classList.remove('visible'); };
    container.appendChild(si);
  });
}

/**
 * Render all songs as a song list inside #search-results (Search screen).
 * Also used as the "all songs" browse list.
 */
function renderAllSongsList(filterList) {
  const container = $('search-results');
  if (!container) return;
  const songs = filterList || PLAYLIST;
  if (songs.length === 0) {
    container.innerHTML = `<div style="text-align:center;padding:40px 0;color:var(--text-sub)"><i class="fa-solid fa-magnifying-glass" style="font-size:32px;margin-bottom:12px;display:block"></i><div style="font-size:15px;font-weight:600">No results found</div></div>`;
    return;
  }
  container.innerHTML = `<div class="section-head" style="margin-bottom:12px"><h2 style="font-size:16px;color:var(--text-sub)">${filterList ? `${songs.length} result${songs.length!==1?'s':''}` : `All Songs · ${songs.length} tracks`}</h2></div>`;
  songs.forEach((t) => {
    const idx = PLAYLIST.indexOf(t);
    const genreText = Array.isArray(t.genre) ? t.genre.join(', ') : (t.genre || '');
    const row = document.createElement('div');
    row.className = 'recent-row';
    row.style.cssText = 'cursor:pointer;transition:background 0.2s;margin-bottom:4px;border-radius:12px;';
    row.innerHTML = `
      <img src="${t.img}" alt="" style="width:44px;height:44px;border-radius:10px;object-fit:cover;flex-shrink:0">
      <div class="info" style="flex:1;min-width:0">
        <div class="name" style="font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${t.title}</div>
        <div class="meta" style="color:var(--text-sub);font-size:12px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">
          ${t.artist}${t.album ? ' · ' + t.album : ''}${genreText ? ' · ' + genreText : ''}${t.year ? ' · ' + t.year : ''}
        </div>
      </div>
      <div style="display:flex;flex-direction:column;align-items:flex-end;gap:3px;flex-shrink:0">
        <span class="duration" style="font-size:12px;color:var(--text-sub)">${t.duration}</span>
        ${t.bitrate ? `<span style="font-size:10px;color:var(--text-dim)">${Math.round(t.bitrate/1000)}kbps</span>` : ''}
      </div>
      <i class="fa-solid fa-play play-icon" style="margin-left:10px;color:var(--text-sub)"></i>
    `;
    row.onmouseenter = () => row.style.background = 'rgba(255,255,255,0.06)';
    row.onmouseleave = () => row.style.background = '';
    row.onclick = () => playSong(idx);
    container.appendChild(row);
  });
}

/* ═══════════════════════════════
   QUEUE
═══════════════════════════════ */
function renderQueue() {
  const list = $('queue-list');
  list.innerHTML = '';
  PLAYLIST.forEach((t, i) => {
    const d = document.createElement('div');
    d.className = 'queue-item' + (i === currentIdx ? ' playing' : '');
    d.innerHTML = `
      <img src="${t.img}" alt="">
      <div class="q-info"><div class="q-title">${t.title}</div><div class="q-artist">${t.artist}</div></div>
      ${i === currentIdx ? `<div class="q-playing-indicator"><div class="q-eq-bar" style="height:8px"></div><div class="q-eq-bar" style="height:12px"></div><div class="q-eq-bar" style="height:6px"></div></div>` : ''}
    `;
    d.onclick = () => { playSong(i); toggleQueue(); };
    list.appendChild(d);
  });
}

let queueOpen = false;
function toggleQueue() {
  queueOpen = !queueOpen;
  $('queue-panel').classList.toggle('open', queueOpen);
  if (queueOpen) renderQueue();
}

/* ═══════════════════════════════
   FULLSCREEN PLAYER
═══════════════════════════════ */
function openFullscreen() {
  $('fullscreen-player').classList.add('open');
  document.body.style.overflow = 'hidden';
}
function closeFullscreen() {
  $('fullscreen-player').classList.remove('open');
  document.body.style.overflow = '';
}

/* ═══════════════════════════════
   LYRICS MODAL
═══════════════════════════════ */
function openLyricsModal() {
  $('lyrics-modal').classList.add('open');
}
function closeLyricsModal(e) {
  if (!e || e.target === $('lyrics-modal')) $('lyrics-modal').classList.remove('open');
}

/* ═══════════════════════════════
   NAVIGATION
═══════════════════════════════ */
function switchScreen(target) {
  document.querySelectorAll('.nav-item[data-screen]').forEach(n => n.classList.remove('active'));
  document.querySelectorAll('.mobile-nav-item[data-screen]').forEach(n => n.classList.remove('active'));
  document.querySelectorAll(`[data-screen="${target}"]`).forEach(n => n.classList.add('active'));
  document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
  const target_screen = $('screen-' + target);
  if (target_screen) {
    target_screen.classList.add('active');
    initReveal(target_screen);
  }
  const nowBar = $('now-bar');
  if (nowBar) {
    nowBar.style.display = target === 'profile' ? 'none' : '';
  }
}

function openNowPlaying() { switchScreen('nowplaying'); }

// Sidebar nav
document.querySelectorAll('.nav-item[data-screen]').forEach(item => {
  item.addEventListener('click', () => switchScreen(item.dataset.screen));
});

// Mobile nav
document.querySelectorAll('.mobile-nav-item[data-screen]').forEach(item => {
  item.addEventListener('click', () => switchScreen(item.dataset.screen));
});

/* ═══════════════════════════════
   SIDEBAR EXPAND
═══════════════════════════════ */
function toggleSidebar() {
  const sb = $('sidebar');
  sb.classList.toggle('expanded');
  document.documentElement.style.setProperty('--sidebar-w', sb.classList.contains('expanded') ? '220px' : '72px');
  // Reposition bar and visualizer
  const w = sb.classList.contains('expanded') ? '220px' : '72px';
  $('now-bar').style.left = `calc(${w} + 16px)`;
  $('vis-container').style.left = `calc(${w} + 16px)`;
}

/* ═══════════════════════════════
   TABS
═══════════════════════════════ */
document.querySelectorAll('.tabs').forEach(group => {
  group.querySelectorAll('.tab').forEach(tab => {
    tab.addEventListener('click', () => {
      group.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
    });
  });
});
document.querySelectorAll('.genre-chip').forEach(chip => {
  chip.addEventListener('click', () => chip.classList.toggle('active'));
});

/* ═══════════════════════════════
   VISUALIZER
═══════════════════════════════ */
const vis = $('visualizer');
const BAR_COUNT = 50;
for (let i = 0; i < BAR_COUNT; i++) {
  const b = document.createElement('div');
  b.className = 'vis-bar';
  b.style.setProperty('--h', (80 + Math.random() * 28) + 'px');
  b.style.setProperty('--d', (0.35 + Math.random() * 0.75) + 's');
  b.style.animationDelay = (Math.random() * 0.5) + 's';
  b.style.animationPlayState = 'paused';
  vis.appendChild(b);
}

/* ═══════════════════════════════
   RENDER FAVORITES LIST
═══════════════════════════════ */
function renderFavorites() {
  const list = $('fav-list');
  if (!list) return;
  if (favorites.length === 0) {
    list.innerHTML = `<div style="text-align:center;padding:48px 0;color:var(--text-sub)"><i class="fa-regular fa-heart" style="font-size:36px;margin-bottom:12px;display:block;color:var(--accent3)"></i><div style="font-size:15px;font-weight:600">No favorites yet</div><div style="font-size:13px;margin-top:6px">Tap the heart on any song to save it</div></div>`;
    return;
  }
  list.innerHTML = '';
  favorites.forEach((idx, num) => {
    const t = PLAYLIST[idx];
    const row = document.createElement('div');
    row.className = 'fav-row glass';
    row.innerHTML = `
      <div class="glass-filter"></div><div class="glass-overlay" style="background:rgba(255,255,255,0.05)"></div><div class="glass-specular" style="box-shadow:none"></div>
      <span class="num" style="position:relative;z-index:3">${num+1}</span>
      <img src="${t.img}" style="position:relative;z-index:3;width:44px;height:44px;border-radius:10px;object-fit:cover" alt="">
      <div class="info" style="position:relative;z-index:3"><div class="name">${t.title}</div><div class="meta">${t.artist}</div></div>
      <span class="dur" style="position:relative;z-index:3">${t.duration}</span>
      <i class="fa-solid fa-heart heart" style="position:relative;z-index:3"></i>
    `;
    row.onclick = () => playSong(idx);
    list.appendChild(row);
  });
  // Update stat counter
  const stat = $('fav-count-stat');
  if (stat) stat.textContent = favorites.length;
}

/* ═══════════════════════════════
   THEME SWITCHER
═══════════════════════════════ */
function setTheme(btn, cls) {
  document.body.className = cls;
  document.querySelectorAll('.theme-btn').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');
  localStorage.setItem('aura_theme', cls);
  showToast('info', 'Theme Changed', cls ? cls.replace('theme-','').charAt(0).toUpperCase()+cls.replace('theme-','').slice(1) : 'Dark');
}

function toggleThemeCycler() {
  const themes = ['', 'theme-light', 'theme-neon'];
  const cur = themes.indexOf(document.body.className);
  const next = themes[(cur + 1) % themes.length];
  const btn = document.querySelector(`[data-theme="${next}"]`);
  if (btn) setTheme(btn, next);
}

/* Apply saved theme */
if (currentTheme) {
  document.body.className = currentTheme;
  const btn = document.querySelector(`[data-theme="${currentTheme}"]`);
  if (btn) { document.querySelectorAll('.theme-btn').forEach(b => b.classList.remove('active')); btn.classList.add('active'); }
}

/* ═══════════════════════════════
   SEARCH
═══════════════════════════════ */
function setupSearch(inputId, suggestionsId) {
  const input = $(inputId), suggestions = $(suggestionsId);
  if (!input || !suggestions) return;

  function matchSong(t, q) {
    if (!q) return false;
    const genreStr = Array.isArray(t.genre) ? t.genre.join(' ').toLowerCase() : String(t.genre||'').toLowerCase();
    return (
      t.title.toLowerCase().includes(q)  ||
      t.artist.toLowerCase().includes(q) ||
      (t.album  || '').toLowerCase().includes(q) ||
      genreStr.includes(q)
    );
  }

  input.addEventListener('input', () => {
    const q = input.value.trim().toLowerCase();

    if (inputId === 'search-input') {
      // Search screen: filter full song list in #search-results
      if (q.length < 1) {
        suggestions.classList.remove('visible');
        renderAllSongsList(); // show all songs when query cleared
        return;
      }
      const matches = PLAYLIST.filter(t => matchSong(t, q));
      renderAllSongsList(matches);

      // Also show autocomplete dropdown
      if (matches.length > 0) {
        suggestions.classList.add('visible');
        suggestions.querySelectorAll('.suggestion-item').forEach(el => el.remove());
        matches.slice(0, 6).forEach(t => {
          const idx = PLAYLIST.indexOf(t);
          const si = document.createElement('div');
          si.className = 'suggestion-item';
          si.innerHTML = `<i class="fa-solid fa-music"></i><span>${t.title} — ${t.artist}</span>`;
          si.onclick = () => { playSong(idx); suggestions.classList.remove('visible'); input.value=''; renderAllSongsList(); };
          suggestions.appendChild(si);
        });
      } else {
        suggestions.classList.remove('visible');
      }
      return;
    }

    // Home search — suggestions dropdown only
    if (q.length < 1) { suggestions.classList.remove('visible'); return; }
    const matches = PLAYLIST.filter(t => matchSong(t, q));
    if (matches.length > 0) {
      suggestions.classList.add('visible');
      suggestions.querySelectorAll('.suggestion-item').forEach(el => el.remove());
      matches.slice(0, 5).forEach(t => {
        const idx = PLAYLIST.indexOf(t);
        const si = document.createElement('div');
        si.className = 'suggestion-item';
        si.innerHTML = `<i class="fa-solid fa-music"></i><span>${t.title} — ${t.artist}</span>`;
        si.onclick = () => { playSong(idx); suggestions.classList.remove('visible'); input.value=''; };
        suggestions.appendChild(si);
      });
    } else {
      suggestions.classList.remove('visible');
    }
  });
  input.addEventListener('blur', () => setTimeout(() => suggestions.classList.remove('visible'), 200));
}
setupSearch('home-search', 'home-suggestions');
setupSearch('search-input', 'search-suggestions');

function clearSearch() { $('search-input').value = ''; $('search-suggestions').classList.remove('visible'); }

function filterByGenre(genre) {
  const q = genre.toLowerCase();
  const matches = PLAYLIST.filter(t => {
    const genreStr = Array.isArray(t.genre) ? t.genre.join(' ').toLowerCase() : String(t.genre||'').toLowerCase();
    return genreStr.includes(q) || t.title.toLowerCase().includes(q) || t.artist.toLowerCase().includes(q);
  });
  renderAllSongsList(matches.length ? matches : null);
  switchScreen('search');
  showToast('info', genre, `${matches.length} track${matches.length!==1?'s':''} found`);
}

/* ═══════════════════════════════
   CUSTOM CURSOR
═══════════════════════════════ */
// const cursor = $('cursor'), cursorGlow = $('cursor-glow'), mouseGlow = $('mouse-glow');
// let mx = 0, my = 0, cgx = 0, cgy = 0;

// document.addEventListener('mousemove', e => {
//   mx = e.clientX; my = e.clientY;
//   cursor.style.left = mx + 'px';
//   cursor.style.top  = my + 'px';
//   if (mouseGlow) { mouseGlow.style.left = mx + 'px'; mouseGlow.style.top = my + 'px'; }
// });

// function lerpCursor() {
//   cgx += (mx - cgx) * 0.12;
//   cgy += (my - cgy) * 0.12;
//   if (cursorGlow) { cursorGlow.style.left = cgx + 'px'; cursorGlow.style.top = cgy + 'px'; }
//   requestAnimationFrame(lerpCursor);
// }
// lerpCursor();

// document.addEventListener('mousedown', () => { cursor.style.width='8px'; cursor.style.height='8px'; });
// document.addEventListener('mouseup',   () => { cursor.style.width='12px'; cursor.style.height='12px'; });

// // Hover state
// document.querySelectorAll('button, .ctrl-btn, .nav-item, .trending-card, .playlist-card, .lib-card, .fav-row, .recent-row, .artist-card').forEach(el => {
//   el.addEventListener('mouseenter', () => { cursor.style.width='20px'; cursor.style.height='20px'; cursor.style.opacity='0.6'; });
//   el.addEventListener('mouseleave', () => { cursor.style.width='12px'; cursor.style.height='12px'; cursor.style.opacity='1'; });
// });

// /* ═══════════════════════════════
//    PARTICLES CANVAS
// ═══════════════════════════════ */
// (function initParticles() {
//   const canvas = $('particles-canvas');
//   const ctx = canvas.getContext('2d');
//   let W, H, particles = [];

//   function resize() {
//     W = canvas.width  = window.innerWidth;
//     H = canvas.height = window.innerHeight;
//   }
//   resize();
//   window.addEventListener('resize', resize);

//   for (let i = 0; i < 60; i++) {
//     particles.push({
//       x: Math.random() * 2000, y: Math.random() * 1200,
//       r: 0.5 + Math.random() * 1.5,
//       vx: (Math.random() - 0.5) * 0.3,
//       vy: -0.1 - Math.random() * 0.3,
//       o: 0.2 + Math.random() * 0.5
//     });
//   }

//   function draw() {
//     ctx.clearRect(0, 0, W, H);
//     particles.forEach(p => {
//       p.x += p.vx; p.y += p.vy;
//       if (p.y < -5) { p.y = H + 5; p.x = Math.random() * W; }
//       if (p.x < 0 || p.x > W) p.vx *= -1;
//       ctx.beginPath();
//       ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
//       ctx.fillStyle = `rgba(var(--accent-rgb,192,132,252),${p.o})`;
//       ctx.fill();
//     });
//     requestAnimationFrame(draw);
//   }
//   draw();
// })();

/* ═══════════════════════════════
   SCROLL REVEAL
═══════════════════════════════ */
const revealObserver = new IntersectionObserver((entries) => {
  entries.forEach(e => { if (e.isIntersecting) { e.target.classList.add('visible'); } });
}, { threshold: 0.1 });

function initReveal(container) {
  container.querySelectorAll('.reveal').forEach(el => revealObserver.observe(el));
}
document.querySelectorAll('.reveal').forEach(el => revealObserver.observe(el));

/* ═══════════════════════════════
   ANIMATED COUNTERS
═══════════════════════════════ */
function animateCounter(el) {
  const target = parseInt(el.dataset.count);
  if (!target) return;
  let current = 0;
  const step = target / 50;
  const timer = setInterval(() => {
    current = Math.min(current + step, target);
    el.textContent = Math.round(current).toLocaleString();
    if (current >= target) clearInterval(timer);
  }, 20);
}

const counterObserver = new IntersectionObserver((entries) => {
  entries.forEach(e => {
    if (e.isIntersecting) {
      animateCounter(e.target);
      counterObserver.unobserve(e.target);
    }
  });
}, { threshold: 0.5 });
document.querySelectorAll('[data-count]').forEach(el => counterObserver.observe(el));

/* ═══════════════════════════════
   KEYBOARD SHORTCUTS
═══════════════════════════════ */
document.addEventListener('keydown', e => {
  if (e.target.tagName === 'INPUT') return;
  switch (e.code) {
    case 'Space':       e.preventDefault(); toggleMainPlay(); break;
    case 'ArrowRight':  audio.currentTime = Math.min(audio.currentTime + 10, audio.duration||0); break;
    case 'ArrowLeft':   audio.currentTime = Math.max(audio.currentTime - 10, 0); break;
    case 'ArrowUp':     audio.volume = Math.min(audio.volume + 0.1, 1); showToast('info','Volume', `${Math.round(audio.volume*100)}%`); break;
    case 'ArrowDown':   audio.volume = Math.max(audio.volume - 0.1, 0); showToast('info','Volume', `${Math.round(audio.volume*100)}%`); break;
    case 'KeyN':        nextTrack(); break;
    case 'KeyP':        prevTrack(); break;
    case 'KeyS':        toggleShuffle(); break;
    case 'KeyR':        toggleRepeat(); break;
    case 'KeyF':        toggleFavorite(); break;
    case 'KeyL':        openLyricsModal(); break;
    case 'Escape':      closeFullscreen(); closeLyricsModal(); break;
  }
});

/* ═══════════════════════════════
   3D TILT CARDS
═══════════════════════════════ */
document.querySelectorAll('.trending-card, .playlist-card, .lib-card').forEach(card => {
  card.addEventListener('mousemove', e => {
    const r = card.getBoundingClientRect();
    const x = (e.clientX - r.left) / r.width  - 0.5;
    const y = (e.clientY - r.top)  / r.height - 0.5;
    card.style.transform = `translateY(-4px) scale(1.02) rotateX(${-y*6}deg) rotateY(${x*6}deg)`;
  });
  card.addEventListener('mouseleave', () => { card.style.transform = ''; });
});

/* ═══════════════════════════════
   MAGNETIC BUTTONS
═══════════════════════════════ */
document.querySelectorAll('.magnetic').forEach(btn => {
  btn.addEventListener('mousemove', e => {
    const r = btn.getBoundingClientRect();
    const x = (e.clientX - r.left - r.width/2)  * 0.3;
    const y = (e.clientY - r.top  - r.height/2) * 0.3;
    btn.style.transform = `translate(${x}px, ${y}px)`;
  });
  btn.addEventListener('mouseleave', () => { btn.style.transform = ''; });
});

/* ═══════════════════════════════
   DYNAMIC GREETING
═══════════════════════════════ */
function updateGreeting() {
  const h = new Date().getHours();
  const greet = h < 5 ? 'Good night 🌙' : h < 12 ? 'Good morning ☀️' : h < 17 ? 'Good afternoon 🌤' : h < 21 ? 'Good evening ✨' : 'Good night 🌙';
  const el = $('greeting-text');
  if (el) el.textContent = greet;
}
updateGreeting();

/* ═══════════════════════════════
   CLOSE DROPDOWNS ON OUTSIDE CLICK
═══════════════════════════════ */
document.addEventListener('click', e => {
  if (!e.target.closest('#queue-panel') && !e.target.closest('[onclick*="toggleQueue"]')) {
    if (queueOpen) { queueOpen = false; $('queue-panel').classList.remove('open'); }
  }
});

/* ═══════════════════════════════
   LOADING SCREEN
═══════════════════════════════ */
window.addEventListener('load', () => {
  setTimeout(async () => {
    $('loading-screen').classList.add('hidden');
    setTimeout(() => $('loading-screen').remove(), 700);
    // Load songs.json → boots player via onPlaylistReady()
    await loadSongsJSON();
    // Keyboard shortcut hint
    setTimeout(() => showToast('info', 'Keyboard Shortcuts', 'Space to play/pause · ← → to seek'), 1500);
  }, 2000);
});