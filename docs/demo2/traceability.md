# Truy vết dữ liệu demo2

- `RyyyyMMdd-nn`: ca lịch sử dùng cho báo cáo.
- `T01`–`T12`: ca thao tác trong ngày từ seed chuẩn.
- `LOAD-INT-*`: số phiếu hợp lệ được bổ sung để INT-104 hơn INT-103 đúng bốn phiếu đang hoạt động.
- `LOAD-SUR-*`: số phiếu hợp lệ được bổ sung để SUR-102 hơn SUR-101 đúng bốn phiếu đang hoạt động.

UUID của dữ liệu bổ sung được tạo bằng `md5('CareS-board-demo-v1:' || key)::uuid`, nên chạy lại cho cùng ngày không làm thay đổi định danh. Ngày trong mã bệnh án, hóa đơn và mẫu xét nghiệm lấy từ ngày nghiệp vụ tương ứng.

Chuỗi lịch sử đầy đủ:

`Profile → Appointment → CustomerVisit → Invoice/InvoiceItem → PaymentTransaction → QueueTicket → MedicalRecord/VitalSigns → TestRequest/TestResult/SIGNED revision → Feedback`.

Các phiếu tạo tải cũng có đủ chuỗi tới thanh toán và hàng chờ; không tạo QueueTicket mồ côi chỉ để thay đổi phép đếm.

Mã bệnh án và hóa đơn chứa ngày chạy nên không ghi cứng trong tài liệu. Chạy `reconciliation.sql` để nhận bảng đối chiếu hiện hành cho từng Customer, gồm tài khoản, mã VIS, bệnh án, hóa đơn, giao dịch và điểm đánh giá.
