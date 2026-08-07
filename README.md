# Network24

> IMPORTANT (Single Source of Truth)
>
> This README is the canonical project context.
> Any change to navigation, API, models, stream URL format, player logic, permissions,
> preferences, or layout IDs MUST be reflected here in the same PR/commit.

## Quick summary
Network24 is a Kotlin Android IPTV client that connects to an Xtream Codes–compatible server, fetches **live categories** and **live channels** via `player_api.php`, and plays a selected stream using **AndroidX Media3 (ExoPlayer)**.

Primary application package: `com.network24.player`

---

## Table of contents
1. [Product intent](#product-intent)
2. [User flow (screens)](#user-flow-screens)
3. [Architecture](#architecture)
4. [Build & configuration](#build--configuration)
5. [Manifest & permissions](#manifest--permissions)
6. [API contract (Xtream Codes)](#api-contract-xtream-codes)
7. [Playback (Media3 / ExoPlayer)](#playback-media3--exoplayer)
8. [Persistence (SharedPreferences)](#persistence-sharedpreferences)
9. [UI & resources map](#ui--resources-map)
10. [Key files (where to change what)](#key-files-where-to-change-what)
11. [How to run](#how-to-run)
12. [Security / operational notes](#security--operational-notes)
13. [Maintenance rules](#maintenance-rules)
14. [Roadmap / TODO](#roadmap--todo)

---

## Product intent
The app provides a simple flow:

1. User enters **server URL + credentials**
2. App validates **login**
3. App lists **live categories** and **channels**
4. App plays a **live stream**

Current scope:
- Live categories list
- Live streams list by category
- Playback (MPEG-TS `.ts` stream URL)

Out of scope (not implemented right now):
- VOD browsing
- Series browsing
- EPG UI
- Profiles / multi-user
- Offline/download

---

## User flow (screens)
**Launcher activity:** `SplashActivity` (declared `MAIN/LAUNCHER` in `AndroidManifest.xml`)

Flow:
`SplashActivity` (2s delay)  
→ `LoginActivity`  
→ `HomeActivity` (categories + channels)  
→ `PlayerActivity` (stream playback)

Note:
- `MainActivity` exists but is currently a template/placeholder and is not part of normal navigation.

---

## Architecture
This is a **single-module** Android project (`:app`) using **package-based separation** (not strict Clean Architecture).

### Package layout (responsibilities)
- `activities/` — UI screens + navigation logic
- `adapters/` — RecyclerView adapters for category/channel lists
- `api/` — Retrofit/OkHttp configuration + endpoint interface
- `repository/` — data-fetch orchestration (calls `ApiService`)
- `models/` — API DTOs
- `utils/` — SharedPreferences wrapper and shared helpers

### End-to-end data flow
Login:
`LoginActivity` → `LoginRepository.login()` → `ApiClient.create(baseUrl)` → `ApiService.login()` → `LoginResponse(UserInfo, ServerInfo)`

Browse:
`HomeActivity` → `LiveRepository.getCategories()` → `ApiService.getLiveCategories()` → `List<LiveCategory>`

Channels:
`HomeActivity` → `LiveRepository.getChannels(categoryId)` → `ApiService.getLiveStreams(categoryId)` → `List<LiveChannel>`

Playback:
`HomeActivity` (channel click) → `PlayerActivity(stream_id, stream_name)` → builds stream URL → Media3 ExoPlayer plays

Organization note:
- There is a known **folder/package mismatch**: `LoginRepository.kt` is physically under `utils/` but declares package `com.network24.player.repository`.
  Recommended cleanup: move it into the `repository/` folder to match its package.

---

## Build & configuration
### Modules
- Only `:app` is included (see `settings.gradle.kts`).

### Android & Kotlin
- Kotlin + ViewBinding (Compose is not used)
- Java/Kotlin target: 17
- minSdk: 26
- targetSdk: 36
- compileSdk: 36
- Gradle flags:
  - `android.useAndroidX=true`
  - `android.nonTransitiveRClass=true`

### Dependencies (architectural)
- Networking: Retrofit + OkHttp + Gson
- Async: Coroutines (Android)
- Player: AndroidX Media3 ExoPlayer + Media3 UI
- UI: Material Components, RecyclerView
- Images: Coil (Glide dependency also present)

### Version management
Versions are managed in `gradle/libs.versions.toml` (version catalog).

---

## Manifest & permissions
File: `app/src/main/AndroidManifest.xml`

Key points:
- `SplashActivity` is `MAIN/LAUNCHER` and `exported=true`
- `LoginActivity`, `HomeActivity`, `PlayerActivity` are `exported=false`
- `usesCleartextTraffic="true"` is enabled (HTTP server URLs allowed)
- Permission present: `android.permission.INTERNET`

Recommended (if implementing robust connectivity monitoring):
- Add `android.permission.ACCESS_NETWORK_STATE`

---

## API contract (Xtream Codes)
All API calls are **GET** requests to:
`{SERVER_BASE_URL}/player_api.php`

### Base URL rules
- `SERVER_BASE_URL` comes from user input (`server` field).
- The code trims whitespace and ensures a trailing slash before Retrofit creation.

### Endpoints used
1) **Login**
`player_api.php?username={u}&password={p}`

Response: `LoginResponse`  
Login success rule used by app: `user_info.auth == 1`

2) **Live categories**
`player_api.php?username={u}&password={p}&action=get_live_categories`

Response: `List<LiveCategory>`

3) **Live streams by category**
`player_api.php?username={u}&password={p}&action=get_live_streams&category_id={categoryId}`

Response: `List<LiveChannel>`

### Models (DTO summary)
- `LoginResponse(user_info: UserInfo?, server_info: ServerInfo?)`
- `UserInfo` includes:
  - `auth: Int?` (login success uses `1`)
  - account metadata: `status`, `exp_date`, `is_trial`, `active_cons`, `max_connections`, `allowed_output_formats`, etc.
- `ServerInfo` includes server metadata: `url`, `port`, `https_port`, `server_protocol`, `timezone`, `timestamp_now`, `time_now`, etc.
- `LiveCategory`:
  - `category_id: String`
  - `category_name: String`
  - `parent_id: Int?`
- `LiveChannel` key fields:
  - `stream_id: Int?`
  - `name: String?`
  - `stream_icon: String?`
  - `category_id: String?`
  - optional: `direct_source`, `tv_archive`, `tv_archive_duration`, etc.

---

## Playback (Media3 / ExoPlayer)
### Stream URL format used
PlayerActivity constructs:
`{SERVER}/live/{username}/{password}/{streamId}.ts`

Important implication:
- Username/password are embedded in the stream URL; **avoid logging** this URL in production.

### Media3 configuration
- Uses Media3 `PlayerView`.
- Uses a customized `DefaultLoadControl` buffer policy:
  - minBufferMs: 3000
  - maxBufferMs: 10000
  - bufferForPlaybackMs: 1000
  - bufferForPlaybackAfterRebufferMs: 2000

### Retry logic
- maxRetry: 2
- retry delay: `retryCount * 1000` ms
- On `STATE_READY`, retryCount is reset.

### Network recovery (status)
- A `ConnectivityManager.NetworkCallback` exists to resume playback when network becomes available.
- Ensure callback is actually registered/unregistered in lifecycle (and add `ACCESS_NETWORK_STATE` if required).

### Lifecycle policy
- `onStart()` resumes PlayerView
- `onStop()` pauses player
- `onDestroy()` stops, clears media items, releases player

---

## Persistence (SharedPreferences)
Credentials and remember-me are stored using SharedPreferences.

- SharedPreferences name: `network24`
- Keys: `server`, `username`, `password`, `remember`

Behavior:
- If remember-me is checked: save server/username/password.
- Otherwise: clear saved prefs.

Security considerations:
- Credentials are stored in plain SharedPreferences.
- Consider migrating to EncryptedSharedPreferences for production.
- Backup rules are currently templates (`res/xml/backup_rules.xml`, `data_extraction_rules.xml`); decide whether to exclude credentials from backup.

---

## UI & resources map
### Layout files
- `activity_splash.xml` — centered title text
- `activity_login.xml` — server/username/password fields, remember checkbox, login button, progress bar, version label
- `activity_home.xml` — category RecyclerView + channel RecyclerView + progress bar
- `activity_player.xml` — `PlayerView` + progress bar
- `activity_main.xml` — template “Hello World” layout
- `item_category.xml` — category row (TextView `txtCategory`)
- `item_channel.xml` — channel row (`imgLogo`, `txtChannel`)
- `tv_menu_item.xml` — TV tile (`icon`, `title`) focusable menu UI

### Drawables
- `bg_login.xml` — background color
- `button_login.xml` — rounded red button
- `edit_text_bg.xml` — rounded card-like input background
- `tv_menu_background.xml` — selector: focused=red, normal=semi-transparent
- `app_logo.png` — branding
- `ic_launcher_background.xml`, `ic_launcher_foreground.xml` — launcher vectors

### Colors / theme
- Colors are defined in `res/values/colors.xml` (dark palette + red primary).
- Theme uses `Theme.Material3.DayNight.NoActionBar` with minimal customization.

---

## Key files (where to change what)
UI:
- `app/src/main/java/com/network24/player/activities/*`

Lists:
- `app/src/main/java/com/network24/player/adapters/CategoryAdapter.kt`
- `app/src/main/java/com/network24/player/adapters/ChannelAdapter.kt`

Network:
- `app/src/main/java/com/network24/player/api/ApiClient.kt`
- `app/src/main/java/com/network24/player/api/ApiService.kt`

Repositories:
- `app/src/main/java/com/network24/player/repository/LiveRepository.kt`
- `LoginRepository.kt` (declared package: `com.network24.player.repository`)

Models:
- `app/src/main/java/com/network24/player/models/*`

Preferences:
- `app/src/main/java/com/network24/player/utils/PreferenceManager.kt`

---

## How to run
1. Open project in Android Studio
2. Sync Gradle
3. Run `app`
4. Enter:
   - Server URL: e.g., `http://example.com:8080`
   - Username/password
5. Login → Home → select a channel → Player

---

## Security / operational notes
1) Do not ship OkHttp BODY logging in release builds (may log credentials/stream URLs).  
2) Cleartext HTTP is enabled for compatibility; ensure users understand the risk.  
3) Credentials in SharedPreferences are sensitive; consider encryption + backup exclusion.

---

## Maintenance rules
Whenever you change any of the following, update this README in the same PR/commit:
- Navigation flow / launcher activity
- API endpoints, query params, or response models
- Login success rule (auth semantics)
- Stream URL format or player configuration (buffers/retry/recovery)
- Permissions (INTERNET, NETWORK_STATE, etc.)
- Preference keys/storage/encryption
- Any layout IDs relied upon by code

---

## Roadmap / TODO
(Keep this list current; remove items once implemented.)

- [ ] Disable/guard OkHttp logging in release.
- [ ] Add `ACCESS_NETWORK_STATE` and correctly register/unregister NetworkCallback.
- [ ] Move `LoginRepository.kt` into the `repository/` folder to match its package.
- [ ] Consider EncryptedSharedPreferences for credentials.
- [ ] Consider using Theme attributes instead of hard-coded layout colors.
- [ ] Add error UI states (empty/error views) for categories/channels.
