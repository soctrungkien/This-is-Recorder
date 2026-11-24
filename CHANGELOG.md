# Lịch sử thay đổi

## Unreleased (1.3.0) — 2026-11-24

Từ tag `1.2.0` (2026-11-19) đến commit `0822fbd`.

### Tính năng mới

- **Màu khóa Monet tùy chỉnh**: dãy swatch gồm hình nền, 15 màu có sẵn và ô "Tùy chỉnh" mở hộp chọn `ColorPalette` của Miuix kèm preview hex (port từ màn hình theme của ZeroUI). Áp dụng trực tiếp làm seed của `ThemeController`.
- **Liquid Slider**: `LiquidSlider` được đưa vào màn hình thử nghiệm và dùng thuộc tính `progress` theo đúng upstream ZeroUI cho cả độ dài track lẫn vị trí nút kéo.
- **Bộ icon bổ sung**: thêm **OneUI Icons** (MIT) và **Remix Icon** (Remix Icon License v1.0) vào danh sách chọn kiểu icon, thanh điều hướng và màn hình thử nghiệm.
- **Màn hình thử nghiệm mở rộng**: thêm điều khiển, text style, swatch theme, dialog, snackbar, markdown preview, component COUI (switch, slider, checkbox, radio, segmented) và các bộ icon.
- **Credit tập trung**: toàn bộ thông tin credit và văn bản giấy phép được gom trong một file duy nhất `app/src/main/assets/credit.json`. Danh sách và trang chi tiết trong ứng dụng đọc từ file này; thêm/xoá credit chỉ cần sửa một object JSON.

### Thay đổi hành vi

- **Monet luôn bật** trên Android 12+; bỏ công tắc bật/tắt màu động.
- **Chuẩn màu cố định ở Material 3 Expressive (2025)** và kiểu bảng màu `TonalSpot`; bỏ các tùy chọn tương ứng trong cài đặt.
- **COUI không còn là kiểu giao diện có thể chọn**: giá trị đã lưu được chuẩn hóa về Miuix. Component và icon COUI vẫn giữ lại trong màn hình thử nghiệm và danh sách icon.
- **Liền mạch Liquid Glass**: một tùy chọn Off/On duy nhất điều khiển công tắc, thanh kéo và thanh điều hướng; toggle style được áp dụng cho mọi công tắc.
- **minSdk hạ về 24 (Android 5.0)** với guard phiên bản cho các API mới; bỏ dependency `coui-material3` không còn dùng.

### Sửa lỗi

- `LiquidSlider` dùng `pressProgress` (trạng thái nhấn) thay vì `progress` (giá trị) → thanh kéo giờ phản ánh đúng giá trị.
- Tránh tự tham chiếu layer/backdrop gây tràn stack RenderThread (`SIGSEGV` trong `prepareTreeImpl`).
- Sửa màu chữ trong chế độ tối và giữ Monet khi dùng component COUI.
- Logo trong trang Giới thiệu: 5 lần chạm mở màn hình thử nghiệm, còn lại hiển thị chuỗi 10 chữ cái ngẫu nhiên mã Morse trong toast.
- Dọn dẹp file giấy phép trùng lặp trong `assets/licenses` và `res/raw`, thay bằng `credit.json`.

### Hạ tầng / ghi nhận

- Gradle 9.9.2, AGP 9.5.2, build-tools 37.0.0; targetSdk 37.
- giấy phép và credit cập nhật trong About (en/vi) và `README.md`: ZeroUI, Kyant, OneUI Icons (MIT © 2022 Yanndroid & BlackMesa123), Remix Icon (© 2017–2026 Remix Design, Remix Icon License v1.0). Không dùng Geist của Vercel do bộ icon UI không có license công khai rõ ràng.

---

## 1.2.0 (3) — 2026-11-19

Từ tag `1.1.0` (2026-10-28) đến bản `1.2.0`.

### Tính năng mới

- **Lịch sử thay đổi dạng Markdown**: dialog cập nhật dùng `multiplatform-markdown-renderer` để hiển thị đầy đủ heading, danh sách, in đậm/nghiêng, code và link.
- **Link tải trực tiếp**: link cập nhật trỏ thẳng tới GitHub; gh-proxy chỉ còn dùng để kiểm tra phiên bản mới.
- **Màn hình xem thành phần (component showcase)**: mở bằng cách bấm liên tục 5 lần vào icon app trong trang Giới thiệu. Xem các component, thử dialog (cập nhật, xác nhận, nhập liệu, chọn), hiện snackbar, buộc crash và buộc thoát app.
- **Kiểu Toggle**: tùy chọn giao diện công tắc gồm **Giao diện** và **Liquid glass** (mặc định Liquid glass). Khi chọn Liquid glass, mọi công tắc trong cài đặt và màn hình thử nghiệm đều dùng `LiquidToggle` (đồng bộ màu với theme).
- **Thanh điều hướng**: kích thước item theo component tham khảo (rộng tối thiểu 76 dp, nhãn 11 sp / line-height 14 sp, padding dưới `12 dp + navigation bar`), giữ nguyên hiệu ứng mờ nền khi bật "Blur effect" với kiểu Liquid.

### Sửa lỗi

- **Crash RenderThread (stack overflow)**: các công tắc liquid dùng chung một backdrop với source layer là *sibling*, không còn layer chứa chính surface lấy mẫu → hết lỗi `SIGSEGV` trong `prepareTreeImpl`.
- **Nền tự đổi màu khi bật Toggle liquid**: bỏ lớp vẽ đè lên nền của source layer, nền giữ nguyên giao diện cũ.
- **Màn hình crash**: dùng theme Miuix cố định (Monet + sáng/tối theo hệ thống) thay vì cài đặt đã lưu; sửa lỗi import `weight` gây lỗi biên dịch.
- Khôi phục hệ thống UI gốc (không thay toàn bộ bằng ZeroUI); chỉ giữ phần liquid glass cần thiết.

### Thư viện / ghi nhận

- Thêm `multiplatform-markdown-renderer`, `CrashHandler`, các module `miuix-glass*` và phần tử liquid lấy từ ZeroUI / Kyant.
- Cập nhật credit trong trang Giới thiệu và `README.md` (ZeroUI, Kyant, Bhuvaneshwaran) với giấy phép tương ứng.

---

## 1.1.0 (2) — 2026-10-28

- Giữ một helper nền chung cho ADB, root và Shizuku.
- Sửa lỗi bypass MediaProjection.
- Bỏ chế độ root, thêm chế độ screenrecord.
- Thanh công cụ hoạt ảnh, nền OS3 cho trang Giới thiệu, root mix, Monet và icon.
