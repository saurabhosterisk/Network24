# PROJECT_CONTEXT — Network24 (Read This First)

## 1) One‑line summary
Network24 is a Kotlin Android IPTV client that connects to an Xtream Codes–compatible server, fetches live categories and channels via `player_api.php`, and plays a selected stream using AndroidX Media3 (ExoPlayer).

Primary application package: `com.network24.player`

---

## 2) What problem this app solves (product intent)
The app provides a simple flow:
1) user enters server URL + credentials,
2) app validates login,
3) app shows live categories and channels,
4) app plays a live stream.

Scope currently covered:
- Live categories list
- Live streams list by category
- Playback (MPEG-TS `.ts` stream URL)

Non-goals / not implemented (as of this snapshot):
- VOD browsing
- Series browsing
- EPG UI
- Multi-profile
- Offline/download

---

## 3) Runtime user flow (screens)
**Launcher:** `SplashActivity` (declared `MAIN/LAUNCHER` in `AndroidManifest.xml`)

Flow:
`SplashActivity` (2s delay)
→ `LoginActivity`
→ `HomeActivity` (categories + channels)
→ `PlayerActivity` (stream playback)

Notes:
- `MainActivity` exists but is currently a template/placeholder and is not part of the normal navigation flow.

---

## 4) Architecture overview (pragmatic layering)
This is a **single-module** Android project (`:app`) using **package-based separation** (not strict Clean Architecture).

### 4.1 Packages and responsibilities
- `activities/` — UI screens + navigation logic
- `adapters/` — RecyclerView adapters for category/channel lists
- `api/` — Retrofit/OkHttp configuration + endpoint interface
- `repository/` — data-fetch orchestration (calls `ApiService`)
- `models/` — API DTOs
- `utils/` — SharedPreferences wrapper and shared helpers

### 4.2 Data flow (end-to-end)
Login:
`LoginActivity` → `LoginRepository.login()` → `ApiClient.create(baseUrl)` → `ApiService.login()` → `LoginResponse(UserInfo, ServerInfo)`

Browse:
`HomeActivity` → `LiveRepository.getCategories()` → `ApiService.getLiveCategories()` → `List<LiveCategory>`

Channels:
`HomeActivity` → `LiveRepository.getChannels(categoryId)` → `ApiService.getLiveStreams(categoryId)` → `List<LiveChannel>`

Playback:
`HomeActivity` (channel click) → `PlayerActivity(stream_id, stream_name)` → builds stream URL → Media3 ExoPlayer plays

---

## 5) Build & configuration
### 5.1 Modules
- Only `:app` is included (see `settings.gradle.kts`).

### 5.2 Android & Kotlin setup
- Kotlin + ViewBinding (Compose is not used)
- Java/Kotlin target: 17
- minSdk: 26
- targetSdk: 36
- compileSdk: 36
- Gradle flags:
    - `android.useAndroidX=true`
    - `android.nonTransitiveRClass=true`

### 5.3 Dependencies (what matters architecturally)
- Networking: Retrofit + OkHttp + Gson
- Async: Coroutines (Android)
- Player: AndroidX Media3 ExoPlayer + Media3 UI
- UI: Material Components, RecyclerView
- Images: Coil (Glide dependency also present)

### 5.4 Version management
Versions are managed in `gradle/libs.versions.toml` (version catalog). Keep this file updated whenever dependencies change.

---

## 6) Manifest & permissions
File: `app/src/main/AndroidManifest.xml`

Key points:
- `SplashActivity` is `MAIN/LAUNCHER` and `exported=true`
- `LoginActivity`, `HomeActivity`, `PlayerActivity` are `exported=false`
- `usesCleartextTraffic="true"` is enabled (HTTP server URLs allowed)
- Permission present: `android.permission.INTERNET`

Recommended if implementing robust connectivity monitoring:
- Add `android.permission.ACCESS_NETWORK_STATE`

---

## 7) Network/API contract (Xtream Codes style)
All API calls are GET requests to:
`{SERVER_BASE_URL}/player_api.php`

### 7.1 Base URL rules
- `SERVER_BASE_URL` comes from user input (`server` field).
- The code trims whitespace and ensures a trailing slash before Retrofit creation.

### 7.2 Endpoints used
1) **Login**

`player_api.php?username={u}&password={p}`

Response: `LoginResponse`
- success rule used by app: `user_info.auth == 1`

2) **Live categories**

`player_api.php?username={u}&password={p}&action=get_live_categories`

Response: `List<LiveCategory>`

3) **Live streams by category**

`player_api.php?username={u}&password={p}&action=get_live_streams&category_id={categoryId}`

Response: `List<LiveChannel>`

### 7.3 Models (DTO summary)
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

## 8) Playback (PlayerActivity) — stream URL + player policy
### 8.1 Stream URL format used
PlayerActivity constructs:
`{SERVER}/live/{username}/{password}/{streamId}.ts`

Important implication:
- Username/password are embedded in the stream URL; avoid logging this URL in production.

### 8.2 Media3 ExoPlayer configuration
- Uses Media3 `PlayerView`.
- Uses a customized `DefaultLoadControl` buffer policy:
    - minBufferMs: 3000
    - maxBufferMs: 10000
    - bufferForPlaybackMs: 1000
    - bufferForPlaybackAfterRebufferMs: 2000

### 8.3 Retry logic
- maxRetry: 2
- retry delay: `retryCount * 1000` ms
- On `STATE_READY`, retryCount is reset.

### 8.4 Network recovery (status)
- A `ConnectivityManager.NetworkCallback` exists to resume playback when the network becomes available.
- Ensure this callback is actually registered/unregistered in lifecycle (and add `ACCESS_NETWORK_STATE` if required).

### 8.5 Lifecycle policy
- `onStart()` resumes PlayerView
- `onStop()` pauses player
- `onDestroy()` stops, clears media items, releases player

---

## 9) Persistence (PreferenceManager)
Credentials and remember-me are stored using SharedPreferences.

- SharedPreferences name: `network24`
- Keys: `server`, `username`, `password`, `remember`

Behavior:
- If remember-me is checked: save server/username/password.
- Otherwise: clear saved prefs.

Security considerations:
- Credentials are stored in plain SharedPreferences.
- Consider migrating to EncryptedSharedPreferences for production.
- Backup rules are currently templates; decide whether to exclude credentials from backup.

---

## 10) UI & resources map (what IDs exist and where)
### 10.1 Layout files
- `activity_splash.xml` — centered title text
- `activity_login.xml` — server/username/password fields, remember checkbox, login button, progress bar
- `activity_home.xml` — category RecyclerView + channel RecyclerView + progress bar
- `activity_player.xml` — `PlayerView` + progress bar
- `activity_main.xml` — template “Hello World” layout
- `item_category.xml` — category row (TextView `txtCategory`)
- `item_channel.xml` — channel row (`imgLogo`, `txtChannel`)
- `tv_menu_item.xml` — TV tile (`icon`, `title`) used for focusable menu UI

### 10.2 Drawables
- `bg_login.xml` — background color
- `button_login.xml` — rounded red button
- `edit_text_bg.xml` — rounded card-like input background
- `tv_menu_background.xml` — selector: focused=red, normal=semi-transparent
- `app_logo.png` — branding
- `ic_launcher_background.xml`, `ic_launcher_foreground.xml` — launcher vectors

### 10.3 Colors/Theme
- Colors are defined in `res/values/colors.xml` (dark palette + red primary).
- Theme uses `Theme.Material3.DayNight.NoActionBar` with minimal customization.

---

## 11) Key source files (by role)
### UI
- `activities/SplashActivity.kt`
- `activities/LoginActivity.kt`
- `activities/HomeActivity.kt`
- `activities/PlayerActivity.kt`
- `activities/MainActivity.kt` (template)

### Adapters
- `adapters/CategoryAdapter.kt`
- `adapters/ChannelAdapter.kt`

### Network
- `api/ApiClient.kt`
- `api/ApiService.kt`

### Data
- `repository/LiveRepository.kt`
- `LoginRepository.kt` (declared in `com.network24.player.repository` but file location may differ)

### Storage
- `utils/PreferenceManager.kt`

---

## 12) How to run locally (developer quickstart)
1) Open project in Android Studio
2) Sync Gradle
3) Run `app`
4) Enter:
    - Server URL: e.g., `http://example.com:8080`
    - Username/password
5) Login → Home → select a channel → Player

---

## 13) Operational / security notes (IMPORTANT)
1) **Do not ship OkHttp BODY logging** in release builds.
    - It can log credentials and stream URLs.
2) **Cleartext HTTP is enabled** for compatibility; ensure users understand the risk.
3) **Credentials in SharedPreferences** are sensitive; consider encryption + backup exclusion.

---

## 14) Update policy (MANDATORY)
Whenever you change any of the following, update this file in the same PR/commit:
- Navigation flow / launcher activity
- API endpoints, query params, or response models
- Login success rule (e.g., `auth` semantics)
- Stream URL format or player configuration (buffers/retry)
- Permissions (INTERNET, NETWORK_STATE, etc.)
- Preference keys/storage/encryption
- Any layout IDs relied upon by code

---

## 15) Planned improvements (suggested roadmap)
(Keep this list current; remove items once implemented.)
- [ ] Disable/guard OkHttp logging in release.
- [ ] Add `ACCESS_NETWORK_STATE` and correctly register/unregister NetworkCallback.
- [ ] Move `LoginRepository.kt` into the `repository/` folder to match its package.
- [ ] Consider EncryptedSharedPreferences for credentials.
- [ ] Consider using Theme attributes instead of hard-coded layout colors.
- [ ] Add error UI states (empty/error views) for categories/channels.
