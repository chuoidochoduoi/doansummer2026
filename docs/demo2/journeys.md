# Hành trình trình diễn T01–T12

| Mã | Trạng thái khởi đầu | Nội dung trình diễn |
|---|---|---|
| T01 | Lịch PENDING | Customer đặt trước, Lễ tân check-in |
| T02 | Lịch Guest PENDING | Tiếp nhận khách không có tài khoản |
| T03 | Lịch gia đình PENDING | Check-in cho thành viên gia đình |
| T04 | Invoice PENDING | Thu tiền dịch vụ ban đầu |
| T05 | Queue WAITING | Gọi bệnh nhân từ Phòng Ngoại 1 |
| T06 | Queue CALLED | TV tổng và TV phòng hiển thị tên đang gọi |
| T07 | IN_PROGRESS | Hai dịch vụ khám thực hiện tuần tự |
| T08 | WAITING_FOR_TEST | Bác sĩ chỉ định CLS và chờ thanh toán |
| T09 | Lab WAITING | Gọi, nhập, lưu nháp và ký kết quả |
| T10 | TEST_DONE | Quay lại đúng bác sĩ nguồn |
| T11 | SKIPPED | Lễ tân hỗ trợ khách trở lại hàng chờ |
| T12 | COMPLETED | Xem lịch sử, BHYT, CareS, in và đánh giá |

Không chạy T01–T12 ngoài giờ mở cửa. Khi test Nội hoặc Ngoại mới, dùng `doctor.int1` hoặc `doctor.sur1`; tải mẫu đã được chuẩn bị để hệ thống chọn Phòng 1.
