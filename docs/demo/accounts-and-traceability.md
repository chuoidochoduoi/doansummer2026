# Tài khoản và bảng đối soát demo

Toàn bộ danh tính là giả lập. **Mật khẩu đăng nhập chung: `88888888`** (đã kiểm tra BCrypt). Không gửi SMS/email đến các số trong bộ demo.

## Customer

20 tài khoản, mỗi tài khoản có ít nhất một ca H đã kết thúc, bệnh án COMPLETED, hóa đơn PAID, giao dịch SUCCESS và đánh giá chung. Nguyễn Anh Đức đăng nhập **0909000013**.

| Tài khoản | Người bệnh | Ca | VIS cố định | Bác sĩ | Đã trả (đ) | Điểm | Phản hồi |
|---|---|---|---|---|---:|---:|---|
| 0909000001 | Nguyễn Thị Ánh | H01 | VIS-58C2FB59 | Nguyễn Hoàng Minh | 370.000 | 5 | NEW |
| 0909000002 | Trần Minh Anh | H02 | VIS-A36A4371 | Phạm Ngọc Lan | 220.000 | 5 | RESPONDED |
| 0909000003 | Lê Văn Khoa | H03 | VIS-5B407CCC | Đỗ Anh Tuấn | 224.000 | 5 | NEW |
| 0909000004 | Phạm Thị Huyền | H04 | VIS-2DB05D8D | Vũ Thanh Mai | 280.000 | 5 | RESPONDED |
| 0909000005 | Hoàng Quốc Bảo | H05 | VIS-861A354C | Lê Hoàng Nam | 230.000 | 4 | NEW |
| 0909000006 | Vũ Thị Lan | H06 | VIS-F48D26E6 | Nguyễn Hoàng Minh | 220.000 | 5 | RESPONDED |
| 0909000007 | Đặng Minh Tuấn | H07 | VIS-77347590 | Phạm Ngọc Lan | 220.000 | 5 | NEW |
| 0909000008 | Nguyễn Ngọc Mai | H08 | VIS-06BA9F4D | Đỗ Anh Tuấn | 280.000 | 5 | RESPONDED |
| 0909000009 | Trần Gia Hân | H09 | VIS-D18AAFFC | Nguyễn Thu Hà | 220.000 | 5 | NEW |
| 0909000010 | Lê Hoàng Phúc | H10 | VIS-7925357A | Nguyễn Thu Hà | 220.000 | 4 | RESPONDED |
| 0909000011 | Phạm Đức Anh | H11 | VIS-5B213F76 | Nguyễn Hoàng Minh | 220.000 | 5 | NEW |
| 0909000012 | Bùi Thanh Thảo | H12 | VIS-31868FE6 | Phạm Ngọc Lan | 220.000 | 5 | RESPONDED |
| 0909000013 | Nguyễn Anh Đức | H13 | VIS-C8EB241A | Đỗ Anh Tuấn | 280.000 | 5 | NEW |
| 0909000014 | Ngô Thu Trang | H14 | VIS-B551A2A8 | Trần Minh Quân | 220.000 | 5 | RESPONDED |
| 0909000015 | Dương Quốc Huy | H15 | VIS-7D00C690 | Lê Hoàng Nam | 230.000 | 4 | NEW |
| 0909000016 | Mai Phương Linh | H16 | VIS-65959AB2 | Nguyễn Hoàng Minh | 220.000 | 5 | RESPONDED |
| 0909000017 | Phan Anh Khoa | H17 | VIS-23515119 | Phạm Ngọc Lan | 220.000 | 5 | NEW |
| 0909000018 | Trịnh Mỹ Duyên | H18 | VIS-12C89223 | Đỗ Anh Tuấn | 280.000 | 5 | RESPONDED |
| 0909000019 | Lương Thành Đạt | H19 | VIS-42DC97D6 | Trần Minh Quân | 220.000 | 5 | NEW |
| 0909000020 | Tạ Bảo Ngọc | H20 | VIS-08324075 | Lê Hoàng Nam | 230.000 | 4 | RESPONDED |

Mã bệnh án: `MR-yyyyMMdd-Hxx`, ngày = ngày chạy trừ số H. Hóa đơn: `INV-yyyyMMdd-Hxx-initial`; giao dịch tương ứng được truy từ invoice_id bằng [SQL đối soát](reconciliation.sql). UUID và mã VIS ổn định giữa các lần reset; ngày và mã bệnh án/hóa đơn thay đổi theo mốc chạy. H01 có thêm dịch vụ đã trả nhưng bỏ lượt: không hoàn tiền tự động.

## Hồ sơ không có tài khoản riêng

| Mã hồ sơ | Họ tên | Quan hệ/loại | Số điện thoại giả lập | Lịch sử |
|---|---|---|---|---|
| BN-DEMO-021 | Nguyễn Gia An | Con của Nguyễn Anh Đức | 0909000021 | H21 |
| BN-DEMO-022 | Nguyễn Thu Hà | Vợ của Nguyễn Anh Đức | 0909000022 | H22 |
| BN-DEMO-023 | Đỗ Quang Huy | Guest | 0909000023 | H23 |
| BN-DEMO-024 | Trần Ngọc Diệp | Guest | 0909000024 | H24 |

Tài khoản 0909000013 quản lý hai hồ sơ gia đình. Guest chỉ dùng luồng tra cứu hiện có, không được cấp quyền đọc bệnh án chuyên môn.

## Nhân viên

| Tài khoản | Họ tên giả lập | Vai trò | Phòng |
|---|---|---|---|
| admin | Phạm Đức Minh | ADMIN | — |
| cashier1 | Phạm Thùy Dương | CASHIER | — |
| cashier2 | Lê Thị Hạnh | CASHIER | — |
| cashier3 | Phạm Quốc Khánh | CASHIER | — |
| clinicmanager | Nguyễn Thu Hương | CLINIC_MANAGER | — |
| doctor1 | Nguyễn Hoàng Minh | DOCTOR | SUR-101 |
| doctor2 | Trần Minh Quân | DOCTOR | INT-103 |
| doctor3 | Nguyễn Thu Hà | DOCTOR | PED-102 |
| doctor4 | Lê Hoàng Nam | DOCTOR | DER-104 |
| doctor5 | Phạm Ngọc Lan | DOCTOR | SUR-102 |
| doctor6 | Đỗ Anh Tuấn | DOCTOR | INT-104 |
| doctor7 | Vũ Thanh Mai | DOCTOR | OBG-107 |
| doctor8 | Bùi Đức Long | DOCTOR | IMG-301 |
| doctor_biochem | Trần Minh Sinh | DOCTOR | LAB-202 |
| doctor_lab | Nguyễn Hải Yến | DOCTOR | LAB-201 |
| doctor_xray | Lê Thu Phương | DOCTOR | XR-302 |
| lab_evening | Phạm Ngọc Diệp | NURSE | LAB-201 |
| nurse1 | Trần Ngọc Hân | NURSE | LAB-201 |
| nurse2 | Nguyễn Thị Hương | NURSE | LAB-201 |
| nurse3 | Trần Thị Mai | NURSE | LAB-202 |
| nurse4 | Lê Quốc Việt | NURSE | IMG-301 |
| technician_biochem_evening | Bùi Đức Anh | NURSE | LAB-202 |
| technician_ultrasound_evening | Nguyễn Thảo Vy | NURSE | IMG-301 |
| technician_xray_evening | Phạm Mai Chi | NURSE | XR-302 |
| technician_xray_pm | Trần Hoàng Long | NURSE | XR-302 |
| receptionist1 | Lê Quốc Bảo | RECEPTIONIST | — |
| receptionist2 | Nguyễn Minh Anh | RECEPTIONIST | — |
| receptionist3 | Trần Thu Trang | RECEPTIONIST | — |

Bác sĩ phủ ba ca: 00:00–08:00, 08:00–16:00, 16:00–hết ngày. Đây là **lịch mô phỏng**, không phải lịch lao động thực tế. Mỗi điều dưỡng trực một ca/ngày; nhân viên không bị xếp vào nhiều phòng cùng ca. Lịch có từ D−30 đến D+14.

## Ca có điều dưỡng tham gia và đánh giá liên quan

| Ca lịch sử | Người bệnh | Điều dưỡng/KTV | Phòng | Bác sĩ ký kết quả |
|---|---|---|---|---|
| N01 | Nguyễn Thị Ánh | Trần Ngọc Hân | LAB-201 | Nguyễn Hải Yến |
| N02 | Trần Minh Anh | Nguyễn Thị Hương | LAB-201 | Nguyễn Hải Yến |
| N03 | Lê Văn Khoa | Trần Thị Mai | LAB-202 | Trần Minh Sinh |
| N04 | Phạm Thị Huyền | Lê Quốc Việt | IMG-301 | Bùi Đức Long |
| N05 | Hoàng Quốc Bảo | Phạm Ngọc Diệp | LAB-201 | Nguyễn Hải Yến |
| N06 | Vũ Thị Lan | Bùi Đức Anh | LAB-202 | Trần Minh Sinh |
| N07 | Đặng Minh Tuấn | Nguyễn Thảo Vy | IMG-301 | Bùi Đức Long |
| N08 | Nguyễn Ngọc Mai | Trần Hoàng Long | XR-302 | Lê Thu Phương |
| N09 | Nguyễn Thị Ánh | Phạm Mai Chi | XR-302 | Lê Thu Phương |

Các ca N01–N09 do Trần Minh Quân khám tại INT-103; chỉ định sang phòng CLS như bảng. Ngày = D−(20+số N). Giờ bắt đầu nằm trong ca của điều dưỡng; mọi sự kiện kết thúc trước khi ca đó kết thúc.

Ở Lab, `collected_by` ghi người lấy mẫu; tại phòng hình ảnh, `performed_by` ghi người thực hiện hỗ trợ, bác sĩ phòng ký kết quả. Không gán mẫu máu giả cho siêu âm/X-quang.

Đánh giá vẫn là **điểm chung của bệnh án**, không phải điểm riêng của bác sĩ/y tá. Bảng này truy vết người tham gia qua bệnh án, kết quả và lịch trực. Bộ seed không thêm FeedbackTarget và không mở quyền quản lý đánh giá cho các vai trò khác. Bộ lọc nhân viên cũ phụ thuộc FeedbackTarget không được thay đổi trong đợt dữ liệu này.

## Thẻ CareS

- Chủ thẻ: Lê Văn Khoa, 0909000003.
- Mã demo: `CS-DEMO-0003`; PIN: `888888`.
- Nạp 1.000.000đ; ca T12: dịch vụ 220.000đ, BHYT mô phỏng 44.000đ.
- Người bệnh trả sau BHYT: 176.000đ; CareS 15%: 26.400đ.
- Trừ thẻ: 149.600đ; số dư: 850.400đ.
- Hai ledger TOP_UP/PAYMENT; ưu đãi không ghi đè các trường BHYT của InvoiceItem.
