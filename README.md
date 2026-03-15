# 📸 GalleryApp — Play-Store Level Android Gallery

A production-ready Android Gallery application built with **Jetpack Compose + Kotlin**, targeting **Android SDK 35 (Android 15)**.

---

## ✨ Features

| Category | Features |
|---|---|
| **Core Gallery** | MediaStore-based loading, LazyVerticalGrid, pinch-to-zoom, dynamic columns, thumbnail caching |
| **Albums** | Auto-grouped by folder, animated cards with cover photo |
| **Photo Viewer** | Full-screen swipe, pinch/double-tap zoom, drag-to-dismiss, shared element transitions |
| **Video Player** | ExoPlayer, seek bar, rewind/forward 10s, playback speed, PiP mode, volume/brightness gestures |
| **Smart Search** | Search by filename/album, filter by type (photos/videos/favorites), instant results |
| **Hidden Vault** | Biometric + PIN lock, hide/unhide media |
| **Recycle Bin** | 30-day auto-delete, restore, permanent delete |
| **Media Editor** | Brightness, contrast, saturation sliders + rotate + flip |
| **Media Info** | Full EXIF metadata (camera model, GPS, aperture, ISO, etc.) |
| **Favorites** | Toggle favorites, dedicated screen |
| **Settings** | Dark mode, dynamic color, grid size, sort order, slideshow speed |
| **Bulk Operations** | Multi-select, bulk delete/favorite/hide/share |

---

## 🏗️ Architecture

```
Clean Architecture + MVVM

app/
├── data/
│   ├── datasource/      MediaStoreDataSource, PreferencesDataSource
│   ├── local/           Room database (favorites, vault, trash, cache)
│   ├── models/          MediaItem, Album, AppPreferences, etc.
│   ├── repository/      MediaRepositoryImpl
│   └── service/         MediaPlaybackService (ExoPlayer background)
├── domain/
│   ├── repository/      MediaRepository (interface)
│   └── usecases/        Fine-grained use cases per feature
├── ui/
│   ├── components/      MediaGridItem, AlbumCard, TopBar, etc.
│   ├── navigation/      Type-safe NavGraph with Screen sealed class
│   ├── screens/         HomeScreen, AlbumsScreen, PhotoViewerScreen...
│   └── theme/           Material 3, dynamic color, typography
├── viewmodel/           GalleryViewModel + per-screen VMs
├── workers/             TrashPurgeWorker (WorkManager)
└── di/                  Hilt modules
```

---

## 🛠️ Tech Stack

- **Language:** Kotlin 2.0
- **UI:** Jetpack Compose + Material 3
- **DI:** Hilt
- **Navigation:** Navigation Compose
- **Database:** Room
- **Preferences:** DataStore
- **Image Loading:** Coil (with GIF + video support)
- **Video:** Media3 ExoPlayer
- **Background Jobs:** WorkManager
- **Permissions:** Accompanist Permissions
- **Biometric:** AndroidX Biometric API
- **EXIF:** AndroidX ExifInterface
- **Concurrency:** Kotlin Coroutines + Flow

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 35

### Steps
1. Clone the repo
2. Open in Android Studio
3. Sync Gradle
4. Run on a device or emulator with Android 8.0+ (API 26+)

### Permissions
The app requests the following at runtime:
- `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` (Android 13+)
- `READ_EXTERNAL_STORAGE` (Android 12 and below)
- `USE_BIOMETRIC` (vault feature)

---

## 📁 Key Files

| File | Purpose |
|---|---|
| `MediaStoreDataSource.kt` | Queries device MediaStore for photos/videos |
| `MediaRepositoryImpl.kt` | Combines MediaStore + Room + DataStore |
| `GalleryViewModel.kt` | Main shared state + selection mode |
| `HomeScreen.kt` | Grid view with date grouping |
| `PhotoViewerScreen.kt` | Swipeable full-screen viewer with zoom |
| `VideoPlayerScreen.kt` | ExoPlayer with custom controls + PiP |
| `VaultScreen.kt` | PIN + biometric protected hidden media |
| `EditorScreen.kt` | Real-time image adjustments |
| `GalleryNavGraph.kt` | Full navigation graph |

---

## 🎨 Design

- **Material You** dynamic color (Android 12+)
- **Dark / Light** theme with smooth transitions
- **Rounded cards** with gradient overlays
- **Animated** selection overlays, shared element transitions
- **Edge-to-edge** with proper inset handling

---

## 📝 Notes

- The `EditorScreen` applies adjustments in-memory using Compose `ColorMatrix`. To save to disk, integrate a library like **Glide Transformations** or **CameraX Image Capture** with `BitmapFactory` + `Canvas` post-processing.
- Duplicate detection uses a size-first pre-filter then MD5 of the first 8KB, which is fast but not cryptographically perfect — good enough for gallery deduplication.
- The `MediaPlaybackService` handles background video playback. For full MediaSession integration with notification controls, extend the `MediaSession.Callback`.
