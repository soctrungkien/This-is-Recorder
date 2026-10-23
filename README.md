# Miuix Recorder

Ứng dụng quay màn hình Android viết lại hoàn toàn bằng Kotlin, dùng giao diện **Miuix** (Jetpack Compose). Ứng dụng hỗ trợ hai chế độ:

- **Chuẩn (MediaProjection)**: quay màn hình + âm thanh điện thoại / micrô / trộn cả hai.
- **Root (screenrecord)**: quay bằng `su -c screenrecord`, không cần hộp thoại cấp quyền MediaProjection.

> Đây là bản viết lại sạch từ mã JADX cũ. Thư mục `jadx/` chỉ được giữ làm tài liệu tham khảo, không được dùng làm mã nguồn của ứng dụng.

## Tính năng

- Giao diện Miuix, sáng/tối/theo hệ thống, có bản dịch tiếng Việt.
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
- Lưu video vào `Movies/MiuixRecorder`; hiển thị bản ghi gần nhất, ảnh thu nhỏ, nút Mở/Chia sẻ.
- Thông báo đang quay có nút Dừng.
- Chế độ Root tùy chọn, dùng `screenrecord` và tự ghép các đoạn 180 giây.

## Chế độ Root

Chế độ Root cần quyền `su` (Magisk, KernelSU, APatch...). Đặc điểm:

- Không cần hộp thoại MediaProjection.
- Chỉ ghi **micrô**. Không ghi được âm thanh nội bộ của điện thoại.
- `screenrecord` bị giới hạn 180 giây mỗi đoạn; ứng dụng tự quay nhiều đoạn và ghép lại khi dừng.
- Cửa sổ có `FLAG_SECURE` vẫn hiển thị màu đen.
- Nếu thiếu `su` hoặc thiết bị không có `screenrecord`, ứng dụng báo lỗi và không ảnh hưởng chế độ Chuẩn.

Khi chọn Root trong lúc đang đặt nguồn âm thanh là “Âm thanh của điện thoại”, ứng dụng sẽ hỏi xác nhận và chuyển sang micrô.

## Giới hạn âm thanh điện thoại

Android không cho ứng dụng ghi mọi âm thanh một cách vô điều kiện:

- Ứng dụng khác có thể chủ động loại trừ âm thanh của họ khỏi playback capture.
- Cuộc gọi, nội dung DRM và một số game thường bị im lặng.
- Một số thiết bị có lỗi âm thanh playback capture; ứng dụng sẽ tự chuyển sang micrô hoặc quay không tiếng và hiển thị cảnh báo.

## Cài đặt APK từ GitHub Actions

1. Mở tab **Actions** của kho GitHub.
2. Chọn workflow **Build Miuix Recorder APK**.
3. Nhấn **Run workflow** (hoặc push code).
4. Sau khi build xong, tải artifact **MiuixRecorder-debug-...**.
5. Giải nén và cài `app-debug.apk` trên điện thoại (cho phép cài đặt từ nguồn không xác định).

Nếu push một tag (ví dụ `v1.0.0`), workflow cũng đính kèm APK vào GitHub Release.

## Build trên máy

Yêu cầu:

- JDK 21
- Android SDK Platform 37.1
- Build Tools 36.0.0

```bash
./gradlew :app:assembleDebug
```

APK nằm ở:

```text
app/build/outputs/apk/debug/app-debug.apk
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

## Cấu trúc chính

```text
app/src/main/java/com/snowddo/recorder/
├── MainActivity.kt           # quyền, MediaProjection, mở đầu ghi
├── data/                     # cài đặt, trạng thái, MediaStore, thư viện video
├── media/                    # engine MediaProjection và root screenrecord
├── root/                     # su + kiểm tra root
├── service/                  # foreground service, notification, QS tile
└── ui/                       # giao diện Miuix
```

## Ghi chú kỹ thuật

- Âm thanh chuẩn: PCM 16-bit mono 48 kHz → AAC-LC 128 kbps.
- Video chuẩn: H.264 surface encoder, GOP 1 giây.
- File tạm được ghi vào cache, chỉ đưa vào MediaStore sau khi muxer hoàn tất.
- Nếu quá trình quay lỗi, file tạm bị xóa và thông báo lỗi được hiển thị.
- Chế độ Root dùng một script bọc `screenrecord`, lấy PID con, gửi `SIGINT` khi dừng và `chmod 666` file đầu ra trước khi ứng dụng đọc.
