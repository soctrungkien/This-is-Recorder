# Lịch sử thay đổi

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
