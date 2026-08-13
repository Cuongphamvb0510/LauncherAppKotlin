# LauncherAppKotlin — Giải thích code

Tài liệu mô tả **từng file / hàm** trong project, luồng hoạt động, và các lưu ý quan trọng.

---

## 1. Tổng quan

**LauncherAppKotlin** là Android Home Launcher (Kotlin + ViewBinding + MVVM).

| Tính năng | Cách hoạt động ngắn |
|-----------|---------------------|
| Là Home launcher | Manifest `HOME` + `DEFAULT`, chặn Back |
| Lưới app | `PackageManager` → Room → Flow → UI |
| Cài/gỡ realtime | `PackageChangeReceiver` → sync Room |
| Đổi wallpaper / icon | Copy ảnh vào `filesDir`, prefs / Room |
| Theme từ API | NanoHTTPD localhost + Retrofit |

### Kiến trúc

```
ui/          MainActivity, AppGridAdapter
viewmodel/   LauncherViewModel
data/
  local/     Room, SharedPreferences, ImageFileHelper
  model/     AppInfo, ThemeResponse
  repository/ AppRepository, ThemeRepository
  api/       ThemeApi (Retrofit)
  remote/    ThemeServer (NanoHTTPD)
receiver/    PackageChangeReceiver
LauncherApp  Application: DB, Retrofit, start server
```

### Luồng dữ liệu chính

```
PackageManager / ThemeServer
        ↓
   Repository
        ↓
   ViewModel (StateFlow)
        ↓
   MainActivity collect → Adapter / ImageView
```

---

## 2. Application & Manifest

### `LauncherApp.kt`

`Application` — khởi tạo dependency dùng chung cả app.

| Thành phần | Vai trò |
|------------|---------|
| `database` | Singleton Room `AppDatabase` |
| `preferences` | `LauncherPreferences` (wallpaper path, theme name) |
| `themeApi` | Retrofit client → `http://127.0.0.1:8080/` |
| `themeRepository` | Gọi API theme + apply |
| `appRepository` | App list, wallpaper gallery, icon override |
| `themeServer` | NanoHTTPD port 8080 |
| `onCreate()` | Start `ThemeServer` khi app mở |

**Lưu ý:**
- `android:name=".LauncherApp"` phải khai báo trong Manifest.
- Server chỉ sống khi process app còn chạy.
- `baseUrl` Retrofit **phải** kết thúc bằng `/`.

---

### `AndroidManifest.xml`

| Mục | Vai trò |
|-----|---------|
| `INTERNET` | Gọi HTTP (Retrofit + tải wallpaper) — **ngoài** `<application>` |
| `usesCleartextTraffic="true"` | Cho phép HTTP `127.0.0.1` (không HTTPS) |
| `<queries>` | Android 11+: cho phép `queryIntentActivities` thấy app launcher |
| `MainActivity` + `HOME`/`DEFAULT`/`LAUNCHER` | Đăng ký làm Home + hiện icon |
| `launchMode="singleTask"` | Tránh nhiều instance launcher |
| `PackageChangeReceiver` | Lắng nghe cài/gỡ/cập nhật package |
| `<data android:scheme="package" />` | **Bắt buộc** — thiếu thì không nhận broadcast package |

**Lưu ý:** `<uses-permission>` không được đặt trong `<application>`.

---

## 3. UI layer

### `ui/launcher/MainActivity.kt`

Màn hình Home chính.

| Thành phần / hàm | Vai trò |
|------------------|---------|
| `binding` | ViewBinding `ActivityMainBinding` |
| `adapter` | `AppGridAdapter` lưới app |
| `pendingIconApp` | Nhớ app đang đổi icon khi gallery trả Uri |
| `viewModel` | Tạo qua `LauncherViewModelFactory(appRepo, themeRepo)` |
| `pickWallpaper` | `GetContent()` chọn ảnh làm nền |
| `pickIcon` | `GetContent()` chọn ảnh làm icon custom |
| `onCreate` | Setup UI, FAB, collect Flow |
| `showIconOptions(app)` | Dialog long-press: đổi icon / reset / hủy |
| `openApp(app)` | `getLaunchIntentForPackage` → `startActivity` |
| `applyTheme(theme)` | Đổi màu chữ label theo `summer` / `winter` |

**Collect song song (quan trọng):**

```kotlin
launch { viewModel.apps.collect { ... } }
launch { viewModel.wallpaperPath.collect { ... } }
launch { viewModel.currentTheme.collect { ... } }
```

Không được `collect` nối tiếp trong cùng coroutine — `collect` treo mãi, collect sau không chạy.

**Lưu ý:**
- Back bị nuốt (`onBackPressedDispatcher`) — đúng hành vi Home launcher.
- `GetContent()` không cần xin permission đọc storage.
- `BitmapFactory.decodeFile` đang chạy trên main thread khi collect wallpaper — ảnh lớn có thể giật (cải thiện sau: decode IO).

---

### `ui/launcher/AppGridAdapter.kt`

`ListAdapter` hiển thị lưới app.

| Thành phần / hàm | Vai trò |
|------------------|---------|
| `onClick` | Tap → mở app |
| `onLongClick` | Long-press → dialog icon |
| `onCreateViewHolder` | Inflate `item_app.xml` |
| `onBindViewHolder` | Gắn icon, tên, click/long-click, màu chữ |
| `DiffCallback.areItemsTheSame` | Cùng package + activity |
| `DiffCallback.areContentsTheSame` | So `label` + `hasCustomIcon` |
| `setLabelColor(color)` | Đổi màu chữ toàn bộ lưới (theme) |
| `notifyDataSetChanged()` | Vẽ lại sau khi đổi màu |

**Lưu ý:** Nếu `areContentsTheSame` không so `hasCustomIcon`, đổi icon xong UI có thể không refresh.

---

### Layout & resources

| File | Vai trò |
|------|---------|
| `res/layout/activity_main.xml` | `ivWallpaper` + `rvApps` + `fabWallpaper` + `fabTheme` |
| `res/layout/item_app.xml` | 1 ô: `ivIcon` + `tvLabel` |
| `res/values/strings.xml` | Text UI (wallpaper, icon, theme…) |
| `res/values/colors.xml` | Nền launcher + màu label summer/winter |
| `res/values/themes.xml` | Theme Material app |

Thứ tự vẽ: ImageView (dưới) → RecyclerView → FAB (trên).

---

## 4. ViewModel

### `viewmodel/LauncherViewModel.kt`

Cầu nối UI ↔ Repository. Không gọi Retrofit / Room trực tiếp ngoài qua repository.

| Thành phần / hàm | Vai trò |
|------------------|---------|
| `apps` | `StateFlow` danh sách app từ Room |
| `_wallpaperPath` / `wallpaperPath` | Path file hình nền |
| `_currentTheme` / `currentTheme` | Tên theme (`summer` / `winter` / null) |
| `init` | Load wallpaper + theme đã lưu, sync app nền |
| `setWallpaper(uri)` | Copy ảnh gallery → cập nhật path |
| `setCustomIcon(componentKey, uri)` | Lưu icon custom |
| `clearCustomIcon(componentKey)` | Xóa icon custom |
| `fetchThemeFromServer()` | Gọi Theme API → cập nhật theme + wallpaper |

### `LauncherViewModelFactory`

Tạo ViewModel với **2** dependency: `AppRepository` + `ThemeRepository`.

**Lưu ý:** Thiếu `themeRepository` → lỗi `No value passed for parameter 'themeRepository'`.

---

## 5. Data — Model

### `data/model/AppInfo.kt`

Model 1 app trên UI (không phải Entity Room).

| Field | Ý nghĩa |
|-------|---------|
| `label` | Tên hiển thị |
| `packageName` | Package app |
| `activityName` | Activity launcher |
| `icon` | `Drawable` (hệ thống hoặc custom) |
| `componentKey` | `"package/activity"` — ID duy nhất |
| `hasCustomIcon` | Có đang dùng icon override không |

---

### `data/model/ThemeResponse.kt`

Map JSON từ `/theme`:

```json
{ "theme": "summer", "wallpaper": "http://127.0.0.1:8080/wallpaper.jpg" }
```

| Field | Ý nghĩa |
|-------|---------|
| `theme` | Tên theme |
| `wallpaper` | URL tải ảnh nền |

---

## 6. Data — Local (Room, Prefs, File)

### `data/local/entity/AppEntity.kt`

Bảng `installed_apps` — cache danh sách app (không lưu icon blob).

| Field | Ý nghĩa |
|-------|---------|
| `componentKey` | Primary key |
| `packageName`, `activityName`, `label` | Metadata app |

Icon load lúc map sang `AppInfo` từ `PackageManager` hoặc file override.

---

### `data/local/dao/AppDao.kt`

| Hàm | Vai trò |
|-----|---------|
| `observeAll()` | Flow list app (UI tự cập nhật) |
| `insertAll` | Insert / replace |
| `deleteAll` | Xóa hết |
| `replaceAll` | Transaction: xóa + insert (full sync) |

---

### `data/local/entity/IconOverrideEntity.kt`

Bảng `icon_overrides` — app nào đang dùng icon custom.

| Field | Ý nghĩa |
|-------|---------|
| `componentKey` | App nào |
| `iconPath` | Đường dẫn file trong `filesDir/icons/` |

---

### `data/local/dao/IconOverrideDao.kt`

| Hàm | Vai trò |
|-----|---------|
| `observeAll()` | Flow mọi override |
| `upsert` | Lưu / ghi đè icon |
| `delete` | Xóa override → về icon hệ thống |

---

### `data/local/AppDatabase.kt`

Room DB `launcher.db`, version **2**.

| Thành phần | Vai trò |
|------------|---------|
| `appDao()` | Truy cập `installed_apps` |
| `iconOverrideDao()` | Truy cập `icon_overrides` |
| `MIGRATION_1_2` | Tạo bảng `icon_overrides` khi nâng version |
| `getInstance` | Singleton + `addMigrations` |

**Lưu ý:** Nâng version mà không migration → crash khi mở app cũ.

---

### `data/local/LauncherPreferences.kt`

SharedPreferences `launcher_prefs`.

| Hàm | Vai trò |
|-----|---------|
| `getWallpaperPath` / `setWallpaperPath` | Path `wallpaper.jpg` |
| `getThemeName` / `setThemeName` | Tên theme đã apply |

Wallpaper = 1 giá trị → Prefs đủ. Icon nhiều app → Room.

---

### `data/local/ImageFileHelper.kt`

Utility xử lý ảnh file.

| Hàm | Vai trò |
|-----|---------|
| `copyToInternal(context, uri, dest)` | Copy từ gallery Uri → file nội bộ |
| `loadDrawable(context, path)` | File → `Drawable` |
| `downloadFromUrl(url, dest)` | Tải HTTP URL → file (wallpaper từ server) |

**Lưu ý:** Không lưu Uri gallery lâu dài (dễ hết hạn). Luôn copy vào `filesDir`.

---

## 7. Data — Repository

### `data/repository/AppRepository.kt`

Logic app list + wallpaper gallery + icon custom.

| Hàm | Vai trò |
|-----|---------|
| `observeApps()` | `combine` Room apps + icon overrides → `List<AppInfo>` |
| `getWallpaperPath()` | Đọc prefs |
| `setWallpaper(uri)` | Copy → `filesDir/wallpaper.jpg` → lưu path |
| `setCustomIcon(componentKey, uri)` | Copy → `filesDir/icons/...` → upsert Room |
| `clearCustomIcon(componentKey)` | Xóa Room + xóa file |
| `syncInstalledApps()` | Query hệ thống → `replaceAll` Room |
| `queryLauncherApps()` | `ACTION_MAIN` + `CATEGORY_LAUNCHER` |
| `AppEntity.toAppInfo(override)` | Ưu tiên icon custom, không thì icon hệ thống |

**Lưu ý:**
- `combine` 2 Flow: đổi icon cũng làm UI refresh, không chỉ khi sync app.
- `componentKey` có `/` → đổi thành `_` khi đặt tên file.

---

### `data/repository/ThemeRepository.kt`

Logic theme từ API.

| Hàm | Vai trò |
|-----|---------|
| `getCurrentTheme()` | Đọc theme đã lưu |
| `fetchAndApplyTheme()` | GET `/theme` → lưu tên → download wallpaper → lưu path |

Trả `Result<ThemeResponse>` — ViewModel dùng `onSuccess` / (sau này) `onFailure`.

---

## 8. Theme API (server nội bộ + Retrofit)

### `data/api/ThemeApi.kt`

```kotlin
@GET("theme")
suspend fun getTheme(): ThemeResponse
```

Retrofit gọi `{baseUrl}theme` = `http://127.0.0.1:8080/theme`.

---

### `data/remote/ThemeServer.kt`

NanoHTTPD lắng nghe port **8080** trong máy (emulator/device).

| Endpoint | Hiện trả về |
|----------|-------------|
| `GET /theme` | JSON `{ theme: "summer", wallpaper: "..." }` (đang hardcode) |
| `GET /wallpaper.jpg` | JPEG demo (bitmap 1x1 màu cam) |
| Khác | 404 |

**Lưu ý hiện tại:**
- Theme luôn `"summer"` — bấm nhiều lần cũng không đổi theme khác trừ khi sửa server (xoay list / assets ảnh thật).
- Wallpaper demo là 1 pixel — muốn ảnh thật thì đọc từ `assets/` (cần truyền `Context` vào server).
- `127.0.0.1` = chính device/emulator, không phải máy PC host (trừ khi dùng cổng đặc biệt).

---

## 9. Receiver

### `receiver/PackageChangeReceiver.kt`

| Thành phần | Vai trò |
|------------|---------|
| `onReceive` | Nhận `PACKAGE_ADDED` / `REMOVED` / `REPLACED` |
| Coroutine IO | Gọi `appRepository.syncInstalledApps()` |

**Lưu ý:**
- `onReceive` chạy main thread → không sync nặng đồng bộ; dùng coroutine.
- `CoroutineScope(Dispatchers.IO).launch` không gắn lifecycle — chấp nhận được cho sync ngắn; có thể cải thiện bằng `goAsync()` nếu cần.
- Manifest: `exported="false"` + `scheme="package"`.

---

## 10. Build / dependency

### `gradle/libs.versions.toml` + `app/build.gradle.kts`

| Lib | Dùng cho |
|-----|----------|
| Room + KSP | Cache app / icon override |
| Lifecycle + Coroutines | ViewModel, Flow |
| ViewBinding | Binding layout |
| NanoHTTPD | Theme server nội bộ |
| Retrofit + Gson | Gọi `/theme` |

---

## 11. Follow chạy code (startup → từng tính năng)

Phần này trả lời: **app mở thì chạy file nào trước, đọc data ở đâu, UI lấy data từ đâu.**

### 11.1 App vừa mở (startup)

```
1. Android đọc AndroidManifest.xml
      → android:name=".LauncherApp"
      → Activity HOME = MainActivity

2. LauncherApp.onCreate()                         ← INIT ĐẦU TIÊN
      → ThemeServer().start(...)                  ← server :8080 chạy nền
      (database / preferences / repository = lazy → chưa tạo nếu chưa ai gọi)

3. MainActivity.onCreate()
      → inflate activity_main.xml (ViewBinding)
      → by viewModels { LauncherViewModelFactory(appRepository, themeRepository) }
           ↓ lần đầu dùng appRepository / themeRepository
           ↓ LauncherApp mới tạo Room + Prefs + Retrofit + ThemeRepository

4. LauncherViewModel.init { ... }
      → repository.getWallpaperPath()             ← SharedPreferences
      → themeRepository.getCurrentTheme()         ← SharedPreferences
      → repository.syncInstalledApps()            ← PackageManager → Room

5. MainActivity collect (3 launch song song)
      → apps            ← Room Flow (AppDao.observeAll + IconOverrideDao)
      → wallpaperPath   ← StateFlow ViewModel (path file)
      → currentTheme    ← StateFlow ViewModel (tên theme)
```

**Tóm tắt “data lấy từ đâu lúc mở app”:**

| UI cần | Nguồn đọc | File |
|--------|-----------|------|
| Lưới app | Room `installed_apps` (+ icon override) | `AppDao` + `IconOverrideDao` → `AppRepository.observeApps` |
| Sync list mới | `PackageManager` | `AppRepository.syncInstalledApps` |
| Hình nền | File `filesDir/wallpaper.jpg` + path prefs | `LauncherPreferences` + `ImageView` |
| Màu chữ theme | Prefs `theme_name` | `LauncherPreferences` → `applyTheme` |

---

### 11.2 Hiện lưới app + mở app

```
MainActivity
  → collect viewModel.apps
       → AppRepository.observeApps()
            → combine(
                 AppDao.observeAll(),           ← Room bảng installed_apps
                 IconOverrideDao.observeAll()   ← Room bảng icon_overrides
               )
            → mỗi AppEntity → AppInfo (icon từ PackageManager hoặc file custom)
  → adapter.submitList(apps)

User tap app
  → AppGridAdapter.onClick
  → MainActivity.openApp
  → packageManager.getLaunchIntentForPackage → startActivity
```

**File theo thứ tự:**  
`MainActivity` → `LauncherViewModel.apps` → `AppRepository.observeApps` → `AppDao` / `IconOverrideDao` → `AppInfo` → `AppGridAdapter`

---

### 11.3 Cài / gỡ app (realtime)

```
Hệ thống Android gửi Broadcast (PACKAGE_ADDED / REMOVED / REPLACED)
  → PackageChangeReceiver.onReceive
       → LauncherApp.appRepository.syncInstalledApps()
            → queryIntentActivities (PackageManager)
            → AppDao.replaceAll(...)
  → Room đổi → observeApps() emit
  → ViewModel.apps cập nhật
  → MainActivity collect → adapter.submitList
```

**File theo thứ tự:**  
`AndroidManifest` (đăng ký receiver) → `PackageChangeReceiver` → `AppRepository` → `AppDao` → Flow → `MainActivity`

---

### 11.4 Đổi hình nền (gallery)

```
User bấm fabWallpaper
  → pickWallpaper.launch("image/*")
  → Uri từ gallery
  → viewModel.setWallpaper(uri)
       → AppRepository.setWallpaper
            → ImageFileHelper.copyToInternal → filesDir/wallpaper.jpg
            → LauncherPreferences.setWallpaperPath
       → _wallpaperPath.value = path mới
  → MainActivity collect wallpaperPath
  → BitmapFactory.decodeFile → ivWallpaper
```

**File theo thứ tự:**  
`activity_main` (FAB) → `MainActivity` → `LauncherViewModel` → `AppRepository` → `ImageFileHelper` + `LauncherPreferences` → lại `MainActivity` (ImageView)

---

### 11.5 Đổi / reset icon app

```
User long-press ô app
  → AppGridAdapter.onLongClick
  → MainActivity.showIconOptions
       → "Đổi icon" → pickIcon → viewModel.setCustomIcon(componentKey, uri)
            → copy filesDir/icons/... → IconOverrideDao.upsert
       → "Dùng icon gốc" → viewModel.clearCustomIcon
            → IconOverrideDao.delete + xóa file
  → Room icon_overrides đổi
  → observeApps() emit lại (combine)
  → adapter hiện icon mới
```

**File theo thứ tự:**  
`AppGridAdapter` → `MainActivity` → `LauncherViewModel` → `AppRepository` → `ImageFileHelper` + `IconOverrideDao` → Flow → `AppGridAdapter`

---

### 11.6 Đổi theme từ server

```
LauncherApp.onCreate đã start ThemeServer :8080

User bấm fabTheme
  → viewModel.fetchThemeFromServer()
       → ThemeRepository.fetchAndApplyTheme()
            → ThemeApi.getTheme()                    ← Retrofit
                 → HTTP GET http://127.0.0.1:8080/theme
                 → ThemeServer.serve("/theme")       ← JSON theme + URL wallpaper
            → preferences.setThemeName
            → ImageFileHelper.downloadFromUrl(wallpaperUrl)
                 → HTTP GET .../wallpaper.jpg
                 → ThemeServer.serve("/wallpaper.jpg")
            → preferences.setWallpaperPath
       → _currentTheme + _wallpaperPath cập nhật
  → MainActivity
       → applyTheme(theme) → adapter.setLabelColor
       → decodeFile wallpaper → ivWallpaper
```

**File theo thứ tự:**  
`MainActivity` → `LauncherViewModel` → `ThemeRepository` → `ThemeApi` → `ThemeServer` → `ImageFileHelper` + `LauncherPreferences` → lại `MainActivity` / `AppGridAdapter`

---

### 11.7 Sơ đồ “ai gọi ai” (dependency)

```
AndroidManifest
    ├── LauncherApp (Application)
    │     ├── AppDatabase → AppDao, IconOverrideDao
    │     ├── LauncherPreferences
    │     ├── ThemeServer (NanoHTTPD)
    │     ├── ThemeApi (Retrofit)
    │     ├── AppRepository
    │     └── ThemeRepository
    ├── MainActivity
    │     ├── LauncherViewModel (qua Factory)
    │     │     ├── AppRepository
    │     │     └── ThemeRepository
    │     └── AppGridAdapter
    └── PackageChangeReceiver → AppRepository
```

---

## 12. Sau này làm tính năng mới — làm theo thứ tự nào?

Giữ **cùng pattern** project đang dùng (UI → ViewModel → Repository → Data). Đừng nhảy thẳng vào Activity gọi Room/Retrofit.

### 12.1 Checklist chung (làm lần lượt)

| Bước | Làm gì | Ví dụ file |
|------|--------|------------|
| **1. Model** | Data class mô tả data (API JSON hoặc UI model) | `data/model/Xxx.kt` |
| **2. Nguồn data** | Room Entity/Dao **hoặc** Prefs **hoặc** API interface **hoặc** file helper | `entity/`, `dao/`, `api/`, `ImageFileHelper` |
| **3. Database / Manifest** | Nếu Room: thêm entity, tăng version + Migration. Nếu network/broadcast: permission, receiver, cleartext… | `AppDatabase`, `AndroidManifest` |
| **4. Repository** | Hàm nghiệp vụ: đọc/ghi/sync. Trả Flow hoặc `Result`/`suspend` | `XxxRepository.kt` |
| **5. Wiring Application** | Tạo repo/api trong `LauncherApp`, inject đủ dependency | `LauncherApp.kt` |
| **6. ViewModel** | StateFlow + hàm public cho UI gọi. Sửa Factory nếu thêm dependency | `LauncherViewModel` + Factory |
| **7. UI** | Layout (nút/màn), Activity collect Flow + gọi ViewModel, Adapter nếu cần | `xml` + `MainActivity` + Adapter |
| **8. Strings/colors** | Text, màu | `strings.xml`, `colors.xml` |
| **9. Test** | Build → chạy đúng 1 scenario → kiểm tra persist (mở lại app còn không) | — |

**Không làm ngược:** đừng viết nút UI trước khi Repository/ViewModel chưa có đường data — dễ gắn logic lung tung trong Activity.

---

### 12.2 Gợi ý theo loại tính năng

#### A) Tính năng chỉ lưu local (vd: sắp xếp app, ẩn app)
 
```
1. Entity + Dao (hoặc thêm cột Entity cũ + Migration)
2. Repository: observe + update
3. ViewModel: StateFlow / hàm update
4. UI: gesture hoặc màn settings → gọi ViewModel
```

#### B) Tính năng cần API (vd: theme, remote config)

```
1. Model response (ThemeResponse)
2. Api interface (Retrofit)
3. (Optional) mock server / baseUrl
4. Repository: gọi API + cache prefs/Room
5. LauncherApp: Retrofit + ThemeRepository
6. ViewModel: fetchXxx()
7. UI: nút + collect state
8. Manifest: INTERNET + cleartext nếu HTTP
```

#### C) Tính năng nghe hệ thống (vd: package change, boot)

```
1. BroadcastReceiver (hoặc API listener)
2. Manifest đăng ký (đúng chỗ, exported, data scheme…)
3. Receiver gọi Repository.sync...
4. UI không cần sửa nếu đã collect Flow từ Room
```

#### D) Tính năng chọn ảnh / file

```
1. ImageFileHelper (copy / load)
2. Prefs hoặc Room lưu path
3. Repository set/clear
4. ViewModel
5. ActivityResultContracts.GetContent + UI
```

---

### 12.3 Ví dụ đã làm trong project (đối chiếu)

| Tính năng đã có | Bước 1 | … → cuối |
|-----------------|--------|----------|
| Room cache app | `AppEntity` + `AppDao` | `AppDatabase` → `AppRepository` → ViewModel → MainActivity collect |
| Observe cài/gỡ | `PackageChangeReceiver` | Manifest → sync Repository (UI tự cập nhật qua Flow) |
| Wallpaper gallery | `LauncherPreferences` + `ImageFileHelper` | Repository → ViewModel → FAB + collect ImageView |
| Icon custom | `IconOverrideEntity` + Dao + Migration | Repository combine → long-press UI |
| Theme API | `ThemeResponse` + `ThemeApi` + `ThemeServer` | `ThemeRepository` → LauncherApp → ViewModel → FAB theme |

---

### 12.4 Câu hỏi tự hỏi trước khi code

1. Data **lấy từ đâu**? (PackageManager / Room / Prefs / HTTP / file)
2. Data **lưu ở đâu** để mở lại còn? (Room vs Prefs vs chỉ RAM)
3. UI **lắng nghe thế nào**? (thêm StateFlow mới hay gắn vào Flow cũ)
4. Có cần **Manifest** không? (permission, receiver, cleartext)
5. Có phá **Factory / LauncherApp** không? (thêm dependency → sửa chỗ tạo)

Trả lời xong 5 câu → làm đúng thứ tự mục 12.1.

---

## 13. Checklist lưu ý quan trọng

1. **Home launcher:** `HOME` + `DEFAULT` + `LAUNCHER` + `singleTask` + nuốt Back.
2. **Android 11+:** thiếu `<queries>` → list app thiếu/rỗng.
3. **Room migration:** version tăng phải có `Migration` hoặc mất data / crash.
4. **Import class mới:** quên import → `Unresolved reference` (vd. `IconOverrideEntity`, `ThemeRepository`).
5. **Factory ViewModel:** số tham số phải khớp constructor ViewModel.
6. **2+ Flow collect:** phải `launch` song song trong `repeatOnLifecycle`.
7. **Permission INTERNET:** ngoài `<application>`; cleartext cho HTTP localhost.
8. **Ảnh:** copy vào `filesDir`, không phụ thuộc Uri gallery.
9. **DiffUtil:** nhớ `hasCustomIcon` khi so nội dung item.
10. **ThemeServer:** đang demo hardcode; muốn đổi theme / ảnh thật phải sửa server (và có thể assets).
11. **INIT order:** `LauncherApp.onCreate` → `MainActivity` → tạo ViewModel → `init` sync → UI collect.
12. **Tính năng mới:** Model → Data nguồn → Repository → LauncherApp → ViewModel → UI (không nhảy cóc).

---

## 14. Cách test nhanh

| # | Thao tác | Kỳ vọng |
|---|----------|---------|
| 1 | Set làm Home mặc định | Hiện lưới app |
| 2 | Cài / gỡ app | List tự cập nhật |
| 3 | FAB gallery | Đổi hình nền |
| 4 | Long-press app | Đổi / reset icon |
| 5 | FAB theme | Gọi API → nền + màu chữ summer |
| 6 | Chrome emulator: `http://127.0.0.1:8080/theme` | Thấy JSON |

---

*File này phản ánh codebase tại thời điểm viết. Khi thêm feature mới, cập nhật mục tương ứng.*
