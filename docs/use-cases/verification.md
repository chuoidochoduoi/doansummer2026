# Kiểm tra bộ Actor và Use Case Catalog

Ngày kiểm tra: 06/09/2026.

## Phạm vi đối chiếu

- 125 use case duy nhất và 194 quan hệ actor–use case được ghi rõ.
- 90 ID từ mẫu cũ đã được đối chiếu: 81 ID tiếp tục sử dụng, 9 ID được gộp, chuyển actor, loại bỏ hoặc phân loại lại; 44 ID mới được bổ sung.
- 71 route frontend đã đăng ký đều được gắn với use case và vai trò khai báo.
- 314 endpoint backend thuộc 41 controller đều được gắn use case hoặc phân loại rõ là API hỗ trợ, API tương thích, backend-only hay thao tác bị từ chối có chủ đích.
- 1.553 liên kết nội bộ và liên kết source trong bộ Markdown đã được kiểm tra tồn tại.

## Kiểm tra bản Word

- Bản Word có 18 trang Letter dọc, sử dụng Title và Heading chuẩn, chữ Arial, tiêu đề màu đen và không có đường trang trí dưới tiêu đề.
- Cả 18 trang đã được xuất thành ảnh và xem trực quan ở mức 100%; không phát hiện chữ bị cắt, bảng tràn lề hoặc hàng bị tách sai.
- Tiêu đề bảng được lặp lại khi bảng sang trang; tiêu đề cột không đứng một mình ở cuối trang.
- 125 ID và tên use case trong Word đã được đối chiếu tự động với nguồn Markdown dùng để dựng tài liệu.
- Công cụ dựng ảnh chuẩn đã được chạy trước nhưng máy Windows không có LibreOffice trong PATH. Bản kiểm tra cuối dùng Microsoft Word 2021 để xuất PDF, sau đó dùng chính bộ rasterizer của công cụ tài liệu để tạo và kiểm tra 18 ảnh trang. Không sửa công cụ được quản lý và không đưa PDF/ảnh QA vào repository.

## Bảo toàn dự án

- Backend: 595 file có sẵn trong baseline đã được kiểm tra bằng SHA-256; ngoài `docs/README.md`, chỉ có các file mới trong `docs/use-cases` thuộc tác vụ này.
- Frontend: 232 file có sẵn trong baseline đã được kiểm tra. Trong lúc tạo tài liệu, bốn trang công khai `AboutPage.jsx`, `ContactPage.jsx`, `PrivacyPage.jsx` và `TermsPage.jsx` xuất hiện thay đổi CSS bên ngoài tác vụ; chúng được giữ nguyên và không ảnh hưởng tới route hay catalog use case.
- HEAD của cả hai repository không thay đổi. Không commit, push, chạy database, gửi OTP, tạo thanh toán hoặc gọi tích hợp ngoài.

## Giới hạn kết luận

Đây là kiểm tra tĩnh từ source hiện tại, không phải kết quả chạy giao diện, Swagger, JUnit hay integration test. Các file test trong bảng truy vết chỉ là test liên quan được tìm thấy trong source, không chứng minh mọi nhánh use case đã được chạy hoặc đạt coverage.
