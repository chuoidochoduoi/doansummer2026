# Kịch bản kiểm tra báo cáo

Đăng nhập `clinicmanager`, mở màn Thống kê và chọn khoảng từ ngày đầu tháng cách hiện tại hai tháng đến hôm nay.

## Ba tab

1. **Tổng quan:** kiểm tra lượt đến, lượt kết thúc, bệnh án, CLS, hủy/bỏ lượt, thực thu, BHYT, CareS và còn phải thu.
2. **Thu tiền & hóa đơn:** kiểm tra biểu đồ nhiều ngày/tháng, phương thức thanh toán và khối đối soát.
3. **Hoạt động phòng:** tất cả 11 phòng phải có dữ liệu lịch sử; hàng chờ hiện tại chỉ xuất hiện khi đang trong ca.

## Danh sách chi tiết

- Dịch vụ có tối thiểu 15 dòng, đủ để mở trang 2 với giới hạn 10 dòng/trang.
- Tìm theo mã, tên có dấu và tên không dấu.
- Phòng có 11 dòng, đủ hai trang.
- Tổng kết toàn kỳ không đổi khi tìm kiếm bảng chi tiết.

## In và CSV

Kiểm tra đủ: Tổng quan, Thanh toán theo thời gian, Phương thức thanh toán, Đối soát hóa đơn, Chi tiết dịch vụ và Hoạt động phòng.

- Đang ở trang 2 vẫn phải xuất toàn bộ dữ liệu phù hợp, không chỉ 10 dòng.
- CSV mở bằng Excel phải giữ tiếng Việt và cột tiền là số thuần.
- Bản in không có sidebar/header ứng dụng và dùng đúng A4 dọc/ngang.
- Báo cáo thanh toán dựa trên `paidAt`; đối soát hóa đơn dựa trên ngày phát hành. Một số ca cuối tháng được trả đầu tháng kế tiếp để kiểm chứng khác biệt này.
