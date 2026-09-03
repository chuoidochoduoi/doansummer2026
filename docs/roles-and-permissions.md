# Vai trò và phân quyền

| Vai trò | Phạm vi chính |
|---|---|
| CUSTOMER | Đặt lịch, gia đình, hành trình, lịch sử, thanh toán và thẻ CareS |
| RECEPTIONIST | Tiếp nhận, check-in, tạo phiếu, quản lý lượt và xác nhận khách quay lại |
| CASHIER | Hóa đơn, thanh toán và nạp thẻ |
| NURSE | Phòng được phân công, hàng chờ, dấu hiệu sinh tồn và hỗ trợ CLS |
| DOCTOR / GENERAL_DOCTOR / SPECIALIST_DOCTOR | Khám bệnh, chỉ định, bệnh án và kết quả liên quan |
| CLINIC_MANAGER | Điều phối, danh mục, lịch, nhân sự, báo cáo và giám sát |
| ADMIN | Tài khoản, phòng, danh mục, cấu hình và audit theo route được cấp |

## Quy tắc

- Quyền backend là quyết định cuối cùng; ẩn nút ở frontend không thay thế kiểm tra quyền.
- Customer chỉ truy cập hồ sơ của mình hoặc thành viên gia đình được quản lý.
- Guest chỉ được xem thông tin hành trình công khai sau khi xác minh, không xem nội dung bệnh án.
- Đánh giá liên quan chỉ dành cho Receptionist và Clinic Manager.
- Màn hình gọi công khai không hiển thị dữ liệu y tế nhạy cảm.

Ma trận route frontend được duy trì trong tài liệu frontend; hợp đồng endpoint và security xem trực tiếp tại Swagger và `SecurityConfig`.
