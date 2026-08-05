# LauncherAppKotlin

Android Home Launcher (Kotlin + ViewBinding). Hiện tại đã xong **Bước 1–2**: đăng ký làm Home launcher và hiển thị lưới app từ `PackageManager`.

## Mục tiêu sản phẩm

- Đổi hình nền, icon app
- Observe cài/gỡ app realtime
- Cache bằng Room → mở lại không loading
- Lấy theme từ API + đổi theme

## Kiến trúc

MVVM (đang dựng dần):

```
ui/          → View (Activity, Adapter)
viewmodel/   → ViewModel (Bước sau)
data/        → Model (Room, API, Repository)
receiver/    → Package install/uninstall events
```

## Tiến độ

| Bước | Nội dung | Trạng thái |
|------|----------|------------|
| 1 | HOME intent-filter, không thoát bằng Back | Done |
| 2 | Lưới app + mở app (ViewBinding) | Done |
| 3 | Room cache | Tiếp theo |
| 4 | BroadcastReceiver observe app | Pending |
| 5 | Wallpaper + icon | Pending |
| 6 | Theme API | Pending |

## Điểm quan trọng cần nhớ

### 1. Biến app thành Home Launcher

Trong `AndroidManifest.xml`, `MainActivity` cần:

- `HOME` + `DEFAULT` → hiện trong “Chọn màn hình chính”
- `LAUNCHER` → hiện icon trên drawer/launcher khác
- `launchMode="singleTask"` → tránh nhiều instance

```xml
<category android:name="android.intent.category.HOME" />
<category android:name="android.intent.category.DEFAULT" />
<category android:name="android.intent.category.LAUNCHER" />
```

### 2. `<queries>` (Android 11+)

Không khai báo `<queries>`, `queryIntentActivities` có thể trả về list rỗng/thiếu app:

```xml
<queries>
    <intent>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent>
</queries>
```

### 3. ViewBinding

Bật trong `app/build.gradle.kts`:

```kotlin
buildFeatures {
    viewBinding = true
}
```

- `activity_main.xml` → tự sinh `ActivityMainBinding`
- `item_app.xml` → tự sinh `ItemAppBinding`
- **Không** sửa file trong `app/build/generated/`

### 4. Luồng load & mở app (Bước 2)

```
User mở launcher
    → MainActivity.onCreate
    → inflate ActivityMainBinding
    → loadInstalledApps()  // PackageManager
    → AppGridAdapter bind icon + tên
    → User tap item
    → openApp() → getLaunchIntentForPackage → startActivity
```

`loadInstalledApps()` hỏi hệ thống mọi Activity có:

- `ACTION_MAIN` + `CATEGORY_LAUNCHER`  
(= app hiện được trên launcher thông thường)

### 5. Không thoát bằng Back

```kotlin
onBackPressedDispatcher.addCallback(this) {
    // rỗng = nuốt Back, không finish Activity
}
```

Không dùng `onBackPressed()` cũ (deprecated, gesture Back không ổn định).

### 6. RecyclerView tái sử dụng View

| Method | Khi nào gọi |
|--------|-------------|
| `onCreateViewHolder` | Cần tạo ô mới (lần đầu / ít lần) |
| `onBindViewHolder` | Gắn data vào ô (mỗi lần scroll hiện item) |
| `getItemCount` | Biết list có bao nhiêu item |

## Cấu trúc file Bước 2

```
data/model/AppInfo.kt              # Model 1 app
ui/launcher/MainActivity.kt        # Màn launcher
ui/launcher/AppGridAdapter.kt      # Adapter lưới
res/layout/activity_main.xml       # RecyclerView
res/layout/item_app.xml            # 1 ô: icon + tên
AndroidManifest.xml                # HOME + queries
```

## Chạy thử

1. Sync Gradle + Run
2. Bấm Home → chọn **LauncherAppKotlin** làm mặc định
3. Thấy lưới app → tap để mở

## Roadmap tiếp

1. **Room**: cache app → mở lại UI ngay, sync nền
2. **PackageReceiver**: cài/gỡ app → cập nhật DB/UI
3. **Wallpaper / icon override**
4. **Theme API** + apply theme từ cache
