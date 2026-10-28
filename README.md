# This is Recorder

Ứng dụng quay màn hình Android viết lại hoàn toàn bằng Kotlin, dùng giao diện **Miuix** (Jetpack Compose). Ứng dụng hỗ trợ hai chế độ:

- **Chuẩn (MediaProjection)**: quay màn hình + âm thanh điện thoại / micrô / trộn cả hai.
- **Screenrecord (ADB / Shizuku / root)**: quay bằng `screenrecord`, không cần hộp thoại cấp quyền MediaProjection cho video.

## Tính năng

- Giao diện Miuix, sáng/tối/theo hệ thống, có bản dịch tiếng Việt.
- Thanh tiêu đề kính mờ; thanh điều hướng theo UI Style tiêu chuẩn hoặc Liquid Glass (làm mờ trên Android 13+, có thể tắt trong cài đặt).
- Trang **Giới thiệu** với logo, phiên bản, liên kết mã nguồn, xem toàn văn giấy phép MIT/Apache-2.0 ngay trong ứng dụng, ghi công đầy đủ và các thẻ kính mờ.
- Quay màn hình bằng MediaProjection + MediaCodec + MediaMuxer.
- Nguồn âm thanh:
  - Không âm thanh
  - Micrô
  - Âm thanh của điện thoại (playback capture)
  - Micrô + âm thanh điện thoại (trộn)
- Độ phân giải 720p / 1080p / 1440p / gốc.
- 24 / 30 / 60 fps, bitrate tự động hoặc cố định.
- Đếm ngược 3 / 10 giây, tự dừng sau 5 / 15 / 30 / 60 phút.
- Rung khi bắt đầu/dừng, tự dừng khi tắt màn hình.
- Nút **Quay màn hình** trên Bảng điều khiển nhanh (Quick Settings).
- Lưu video vào `Movies/ThisIsRecorder`; hiển thị bản ghi gần nhất, ảnh thu nhỏ, nút Mở/Chia sẻ.
- Thông báo đang quay có nút Dừng.
- Chế độ Screenrecord tùy chọn qua ADB, Shizuku hoặc root, dùng `screenrecord` và tự ghép các đoạn 180 giây.

## Chế độ Screenrecord

Chế độ Screenrecord lấy quyền shell theo một trong ba cách:

- **ADB**: bật gỡ lỗi USB, kết nối máy tính và chạy lệnh kích hoạt một lần trong ứng dụng. Trợ lý chạy nền đến khi dừng hoặc khởi động lại máy.
- **Shizuku**: khởi động Shizuku rồi cấp quyền cho ứng dụng; `UserService` chạy nền với UID shell và được giữ sống giữa các phiên.
- **Root**: cấp quyền `su` một lần; ứng dụng khởi động daemon chạy nền giống ADB, nên các lệnh và đoạn quay sau không hỏi lại root.

Đặc điểm:

- Không cần hộp thoại MediaProjection cho video.
- Vẫn hỗ trợ micrô, âm thanh điện thoại và trộn cả hai. Âm thanh điện thoại dùng playback capture nên vẫn cần xác nhận MediaProjection một lần; ứng dụng tự cấp app-op `PROJECT_MEDIA` trước khi quay và khôi phục chế độ cũ khi dừng.
- `screenrecord` bị giới hạn 180 giây mỗi đoạn; ứng dụng tự quay nhiều đoạn và ghép lại khi dừng.
- Cửa sổ có `FLAG_SECURE` vẫn hiển thị màu đen.
- Nếu backend không khả dụng hoặc thiếu `screenrecord`, ứng dụng báo lỗi và không ảnh hưởng chế độ Chuẩn.

## Giới hạn âm thanh điện thoại

Android không cho ứng dụng ghi mọi âm thanh một cách vô điều kiện:

- Ứng dụng khác có thể chủ động loại trừ âm thanh của họ khỏi playback capture.
- Cuộc gọi, nội dung DRM và một số game thường bị im lặng.
- Một số thiết bị có lỗi âm thanh playback capture; ứng dụng sẽ tự chuyển sang micrô hoặc quay không tiếng và hiển thị cảnh báo.

## Cài đặt APK từ GitHub Actions

1. Mở tab **Actions** của kho GitHub.
2. Chọn workflow **Build This is Recorder APK**.
3. Nhấn **Run workflow** (hoặc push code).
4. Sau khi build xong, tải artifact **ThisIsRecorder-release-...** (bản đã ký bằng keystore trong secrets) hoặc **ThisIsRecorder-debug-...** nếu cần gỡ lỗi.
5. Giải nén và cài `app-release.apk` (hoặc `app-debug.apk`) trên điện thoại (cho phép cài đặt từ nguồn không xác định).

Nếu push một tag (ví dụ `v1.0.0`), workflow cũng đính kèm cả APK debug và release vào GitHub Release.

### Ký APK release bằng GitHub Secrets

Thêm các secret sau trong **Settings → Secrets and variables → Actions** của repository:

| Secret | Nội dung |
| --- | --- |
| `SIGNING_KEYSTORE_BASE64` | File keystore (`.jks` / `.keystore`) được mã hóa Base64 |
| `SIGNING_STORE_PASSWORD` | Mật khẩu của keystore |
| `SIGNING_KEY_ALIAS` | Alias của key |
| `SIGNING_KEY_PASSWORD` | Mật khẩu của key |

Tạo chuỗi Base64 trên máy:

```bash
base64 -w 0 release.jks > keystore.b64
```

Trên Windows PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.jks")) | Set-Content keystore.b64
```

Gradle giải mã keystore vào `.signing/release.jks` (đã nằm trong `.gitignore`) và dùng nó để ký APK release. Nếu các secret chưa được cấu hình, bản release sẽ tạm ký bằng debug keystore để build vẫn thành công.

## Build trên máy

Yêu cầu:

- JDK 21
- Android SDK Platform 37.1
- Build Tools 36.0.0

```bash
./gradlew :app:assembleDebug :app:assembleRelease
```

APK nằm ở:

```text
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release.apk
```

Workflow dùng Temurin JDK 21 và tự cài SDK cần thiết, nên không cần build cục bộ nếu bạn chỉ muốn lấy APK.

## Thêm nút vào Bảng điều khiển nhanh

1. Kéo thanh thông báo xuống, nhấn biểu tượng chỉnh sửa.
2. Tìm **Quay màn hình** và kéo vào bảng.
3. Khi chưa quay, nhấn nút sẽ mở ứng dụng để xin quyền (MediaProjection).
4. Khi đang quay, nhấn nút sẽ dừng ngay, không mở ứng dụng.

Android 14+ không cho Quick Settings tự lấy quyền MediaProjection, nên lần bắt đầu vẫn phải mở Activity để hiện hộp thoại đồng ý.

## Quyền

- `RECORD_AUDIO`: micrô / playback capture.
- `POST_NOTIFICATIONS`: thông báo điều khiển khi đang quay.
- `FOREGROUND_SERVICE` + các loại `mediaProjection`, `microphone`, `specialUse`.
- Không xin quyền truy cập toàn bộ thư viện ảnh; video do ứng dụng tạo được ghi trực tiếp qua MediaStore.
- Chế độ Screenrecord không cần quyền Android đặc biệt; quyền shell do ADB, Shizuku hoặc `su` cấp. Với âm thanh điện thoại, ứng dụng chạy `cmd appops set --user <id> <package> PROJECT_MEDIA allow` và khôi phục trạng thái trước đó khi dừng.

## Cấu trúc chính

```text
app/src/main/java/com/hihi/recorder/
├── MainActivity.kt           # quyền, MediaProjection, mở đầu ghi
├── data/                     # cài đặt, trạng thái, MediaStore, thư viện video
├── media/                    # engine MediaProjection và shell screenrecord
├── shell/                    # ADB/root daemon, Shizuku UserService, app-op
├── root/                     # khởi động daemon qua su
├── service/                  # foreground service, notification, QS tile
└── ui/                       # giao diện Miuix
```

## Ghi công

### Tác giả

- **Hihi** ([@soctrungkien](https://github.com/soctrungkien)) — phát triển ứng dụng.
- **Thiết kế và biểu tượng ứng dụng**: Hihi.

### Thư viện và nền tảng mã nguồn mở

- **[Miuix for Compose](https://github.com/compose-miuix-ui/miuix)** 0.9.4 — © compose-miuix-ui contributors, giấy phép Apache-2.0. Dùng các module `miuix-ui`, `miuix-preference`, `miuix-icons`, `miuix-blur`, `miuix-squircle` và `miuix-nav`.
- **[AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)** — © 2025 Kyant, giấy phép Apache-2.0. Các thành phần Liquid Glass được phỏng theo và chỉnh sửa bởi compose-miuix-ui contributors.
- **[Jetpack Compose](https://developer.android.com/compose)** 1.12.0 (`foundation`, `ui`, `animation`) — © The Android Open Source Project, giấy phép Apache-2.0.
- **[Material 3](https://developer.android.com/jetpack/compose/designsystems/material3)** 1.4.0 — © The Android Open Source Project, giấy phép Apache-2.0.
- **[AndroidX Core KTX & Activity Compose](https://developer.android.com/jetpack/androidx)** 1.19.1 / 1.13.0 — © The Android Open Source Project, giấy phép Apache-2.0.
- **[Shizuku API & Provider](https://github.com/RikkaApps/Shizuku-API)** 13.1.5 — © 2021 RikkaW, giấy phép MIT. Cung cấp `UserService` chạy với UID shell để thực hiện lệnh `screenrecord` và app-op mà không cần root.
- **[Kotlin](https://kotlinlang.org)** 2.4.21 — © JetBrains s.r.o. và cộng tác viên, giấy phép Apache-2.0.
- **[kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines)** 1.11.0 — © JetBrains s.r.o., giấy phép Apache-2.0.
- **[kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)** 1.11.0 — © JetBrains s.r.o., giấy phép Apache-2.0.

### Giấy phép

- Mã nguồn ứng dụng: [MIT](LICENSE), bản quyền © 2026 Hihi.
- Miuix, Jetpack Compose, Material 3, AndroidX, Kotlin và kotlinx: Apache-2.0. Tác giả của từng thư viện được ghi ở trên.
- Shizuku API: MIT, bản quyền © 2021 RikkaW.
- Toàn văn MIT của ứng dụng, MIT của Shizuku và Apache-2.0 có thể xem ngay trong ứng dụng tại **Giới thiệu → Giấy phép**.

## Ghi chú kỹ thuật

- Âm thanh chuẩn: PCM 16-bit mono 48 kHz → AAC-LC 128 kbps.
- Video chuẩn: H.264 surface encoder, GOP 1 giây.
- File tạm được ghi vào cache, chỉ đưa vào MediaStore sau khi muxer hoàn tất.
- Nếu quá trình quay lỗi, file tạm bị xóa và thông báo lỗi được hiển thị.
- Chế độ Screenrecord dùng một script bọc `screenrecord`, lấy PID con, gửi `SIGINT` khi dừng và `chmod 666` file đầu ra trước khi ứng dụng đọc. Đoạn video và file điều khiển nằm trong thư mục external files của ứng dụng để tiến trình shell đọc/ghi được.
- ADB và root dùng chung một daemon script chạy nền, trao đổi lệnh qua file và gửi heartbeat mỗi giây. ADB khởi động daemon bằng lệnh từ máy tính; root khởi động cùng daemon đó qua `su` một lần. Thư mục điều khiển của root là `0700` để ứng dụng khác không ghi được lệnh. Shizuku dùng `UserService` ở daemon mode, chạy với UID shell và được giữ sống giữa các phiên.
- App-op `PROJECT_MEDIA` được lưu tạm vào SharedPreferences trước khi cấp, để lần mở ứng dụng sau vẫn khôi phục được nếu tiến trình bị dừng đột ngột.
