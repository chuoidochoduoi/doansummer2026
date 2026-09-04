# Thống kê Clinic Manager

Màn thống kê dùng `GET /api/v1/reports/overview`, quyền `ADMIN` hoặc `CLINIC_MANAGER`. Hai endpoint báo cáo cũ được giữ để tương thích; màn mới không dùng các chỉ số cũ như công suất mặc định 0.

## Phạm vi và đơn vị

- Ngày bắt đầu/kết thúc tính gồm hai đầu, tối đa 367 ngày; mặc định hôm nay theo `Asia/Ho_Chi_Minh`. Thời điểm nghiệp vụ dùng giờ địa phương được lưu trong hệ thống.
- Lượt đến khám: số VIS có `checkInTime` trong kỳ, không phải số người duy nhất.
- Lượt kết thúc: VIS `COMPLETED` theo `checkOutTime`; lượt một phần là tập con có phiếu `SKIPPED`. VIS `CANCELLED` được đếm riêng.
- Bệnh án hoàn thành: `MedicalRecord.COMPLETED` của dịch vụ `EXAMINATION`, theo `completedAt`; không đếm phiếu kỹ thuật CLS.
- CLS hoàn thành: từng `TestRequest.COMPLETED` có revision `SIGNED`, người ký và thời điểm ký, theo `completedAt`. Đây là số yêu cầu, không phải số gói hoặc số lượt gọi phòng. Nhiều revision không làm tăng số đếm.
- Hoạt động phòng: hoàn thành theo ngày hoàn tất; chờ/đang xử lý/bỏ lượt là trạng thái hiện tại của phiếu có `workDate` trong kỳ. Không phải ảnh chụp trạng thái tại cuối kỳ.
- Điểm phòng: điểm bệnh án theo `ratedAt`, gắn phòng khám nguồn; hiển thị số phản hồi và thang 5. Không phải điểm riêng nhân viên, không tự quy thành phần trăm hài lòng. Không có đánh giá thì để trống.

## Tiền thanh toán và đối soát

Hai phạm vi không được đánh đồng:

1. **Thanh toán trong kỳ**: giao dịch dịch vụ đang `SUCCESS` theo `paidAt`, kể cả thanh toán một phần hoặc hóa đơn lập ngoài kỳ. Không tính `INSURANCE`, giao dịch hủy/thất bại hoặc tiền nạp thẻ. Thanh toán bằng CareS vẫn được tính một lần. Đây không phải báo cáo dòng tiền ròng; hủy giao dịch sau này có thể thay đổi số kỳ cũ.
2. **Hóa đơn lập trong kỳ**: hóa đơn theo `issueDate`, loại `CANCELLED`. BHYT từ dòng hóa đơn; CareS từ ledger `PAYMENT` gắn giao dịch còn thành công; giảm khác là tổng giảm trừ còn lại. Số đã trả/còn phải thu phản ánh hiện tại, không phải công nợ chốt cuối kỳ.

Đối soát: `Phải trả = Subtotal − BHYT − CareS − Giảm khác + Thuế`.

Bảng dịch vụ dùng tên/mã/giá chụp trên hóa đơn. `InvoiceItem.finalPrice` đã bao gồm số lượng nên không nhân số lượng lần nữa. Bảng này chưa phân bổ giảm CareS cấp hóa đơn, vì vậy không đặt tên là thực thu theo dịch vụ.

## Kiểm chứng

- `ClinicOverviewReportTest`: ngày thanh toán khác ngày hóa đơn, thanh toán một phần, CareS/BHYT, hoàn tác, phân biệt VIS/bệnh án/CLS, revision ký, ngày hoàn tất và giới hạn khoảng ngày.
- `ReportServiceTest`: giữ tương thích báo cáo cũ.
- Frontend: `src/pages/owner/ReportPage.jsx`, `src/hooks/useReport.js`; ba tab, lọc thời gian, tải lại, CSV theo tab và in tab đang xem.
- Không thay entity, schema, dữ liệu hay luồng thu tiền. API mới chỉ đọc dữ liệu.

Hiện API tổng hợp dữ liệu từ các repository trong một transaction chỉ đọc. Trước khi dùng với dữ liệu lớn cần đo hiệu năng và cân nhắc truy vấn tổng hợp có giới hạn ngày; khoảng ngày lọc chưa giới hạn lượng bản ghi đọc từ database.
