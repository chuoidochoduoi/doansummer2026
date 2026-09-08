# Bộ dữ liệu demo CareS

Nguồn duy nhất: [data.sql](../../src/main/resources/data.sql). Bộ này thay dữ liệu demo cũ, không phải migration dữ liệu thật. Danh tính, tình huống chuyên môn, mức giá, BHYT, giấy phép và lịch 24 giờ đều là **mô phỏng**, không dùng để điều trị hoặc chứng minh tuân thủ pháp lý.

## Phạm vi

- 24 hồ sơ người bệnh: 20 Customer, 2 người thân của Nguyễn Anh Đức, 2 Guest.
- 24 VIS lịch sử H01–H24 và 9 ca N01–N09 có điều dưỡng/CLS trong 30 ngày trước.
- 12 kịch bản hôm nay, gồm 3 lịch chưa check-in và 9 VIS; 14 lịch hẹn trong 14 ngày tới.
- 34 bệnh án hoàn thành, 10 kết quả CLS có revision SIGNED; các hồ sơ đang khám không có nội dung hoàn tất giả.
- 29 đánh giá chung: 20 lịch sử Customer và 9 ca điều dưỡng; có NEW và RESPONDED.
- 11 bác sĩ và 9 điều dưỡng có dấu vết tham gia ca có đánh giá. Không seed FeedbackTarget.
- Giữ mã dịch vụ, danh mục gói–chỉ số và các form hiện hành; không đổi Java, API, quyền hoặc schema.
- Không seed tệp/PDF giả, không tạo thông báo EMAIL/SMS. Bốn yêu cầu quay lại chỉ là Notification IN_APP của Lễ tân/Clinic Manager.

Các ca lịch sử chủ yếu là khám kiểm tra/tư vấn, không chỉ định thuốc khi không có chỉ định điều trị. Không tạo đơn thuốc hoặc kết quả bất thường ngẫu nhiên chỉ để đủ dữ liệu màn hình. Không thay danh mục thuốc hiện hành.

## Reset an toàn

**Không chạy trên `order_db` hoặc database chứa dữ liệu cần giữ.** Tên database không bị ràng buộc;
script yêu cầu xác nhận phá hủy bằng `SET cares.demo_reset = 'yes'` trong đúng session thực thi.

1. Dừng backend. Sao lưu database demo nếu muốn giữ lần trình diễn trước; mỗi lần chạy sẽ xóa toàn bộ dữ liệu trong các bảng được liệt kê ở đầu script, không chỉ ca hôm nay.
2. Tạo một database riêng, ví dụ `cares_demo`, trên PostgreSQL đang dùng. Tạo schema bằng chính phiên bản backend hiện tại: đặt `DATABASE_URL=jdbc:postgresql://localhost:5437/cares_demo`, `SPRING_SQL_INIT_MODE=never`; chạy backend một lần để Hibernate tạo các bảng rồi **dừng backend**. Không dùng một bản schema cũ không khớp source.
3. Mở kết nối SQL tới **cares_demo**. Xác nhận tên database bằng `SELECT current_database();` rồi chạy toàn bộ script, không chạy riêng từng khối.

Ví dụ PowerShell tại repository backend (điều chỉnh đường dẫn PostgreSQL; dùng mật khẩu riêng của môi trường demo):

```powershell
$env:PGPASSWORD = '<mật khẩu PostgreSQL demo>'
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -X -h localhost -p 5437 -U postgres -d cares_demo -v ON_ERROR_STOP=1 -c "SET cares.demo_reset='yes';" -f .\src\main\resources\data.sql
```

Trong trình quản lý SQL: chạy `SET cares.demo_reset='yes';` và **toàn bộ** nội dung data.sql trên cùng kết nối. Khi có lỗi, không tiếp tục chạy phần sau; thực hiện `ROLLBACK;` trước lần chạy lại. Chỉ `COMMIT` ở cuối mới là thành công.

4. Khởi động lại backend với `DATABASE_URL` vẫn trỏ tới **cares_demo** và `SPRING_SQL_INIT_MODE=never`. Frontend tiếp tục dùng API như cũ. Nếu mở terminal mới, nhớ đặt lại biến môi trường; không để backend tự quay về `order_db` mặc định.
5. Chạy [SQL đối soát](reconciliation.sql), rồi đăng nhập theo [bảng tài khoản](accounts-and-traceability.md).

Không cần sửa ngày trong file: mốc mặc định lấy lúc bắt đầu transaction, theo `Asia/Ho_Chi_Minh`. Ca T luôn ở ngày/ca hiện tại; các thời điểm đã xảy ra được sắp theo phần thời gian đã trôi qua của ca (tối đa hai giờ). Ngay đầu ca, thời gian mô phỏng được nén; đây không phải thời lượng làm xét nghiệm thực tế. Qua ngày vẫn dùng tác vụ chốt ngày hiện có, không tự hồi sinh VIS cũ.

## Kịch bản hôm nay

| Ca | Người bệnh/tài khoản | Trạng thái đầu | Thao tác trình diễn |
|---|---|---|---|
| T01 | Nguyễn Anh Đức · 0909000013 | PENDING | Lễ tân check-in lịch Nội |
| T02 | Đỗ Quang Huy · Guest 0909000023 | PENDING | Tiếp nhận khách không tài khoản |
| T03 | Nguyễn Gia An · gia đình 0909000013 | PENDING | Check-in lịch Nhi của người thân |
| T04 | Phạm Thị Huyền · 0909000004 | Hóa đơn PENDING | Thu tiền ban đầu, chưa có queue/bệnh án |
| T05 | Hoàng Quốc Bảo · 0909000005 | WAITING · SUR-101 | Gọi bệnh nhân từ hàng chờ |
| T06 | Vũ Thị Lan · 0909000006 | CALLED · DER-104 | Xác nhận vào khám; TV hiển thị người đang gọi |
| T07 | Đặng Minh Tuấn · 0909000007 | IN_PROGRESS · INT-104 | Hoàn thành Tim mạch rồi khám Nội; bước sau BLOCKED, chưa có bệnh án |
| T08 | Nguyễn Ngọc Mai · 0909000008 | WAITING_FOR_TEST · INT-103 | Thanh toán hóa đơn CBC; khám kế tiếp còn BLOCKED |
| T09 | Phạm Đức Anh · 0909000011 | WAITING_FOR_TEST · SUR-102 | CBC đã trả tiền, Lab WAITING; bước chăm sóc sau còn BLOCKED |
| T10 | Bùi Thanh Thảo · 0909000012 | TEST_DONE · OBG-107 | Đường huyết đã ký, gọi quay lại bác sĩ; bệnh án nguồn chưa hoàn tất |
| T11 | Trần Gia Hân · 0909000009 | SKIPPED · PED-102 | Lễ tân xác nhận yêu cầu quay lại đang chờ |
| T12 | Lê Văn Khoa · 0909000003 | COMPLETED | Xem bệnh án, phiếu thu BHYT + ưu đãi CareS |

H01 là lịch sử hoàn thành một phần: có bệnh án công bố và dịch vụ đã trả nhưng bỏ lượt. Không tự hoàn tiền. Các ca H/N có mã, hóa đơn, bác sĩ và đánh giá tra được trong bảng đối soát; lịch sử không mất đi khi kịch bản T đang hoạt động.

## Kiểm thử đã thực hiện

Trên PostgreSQL 18 tách biệt ở cổng 5449, schema lấy từ phiên bản hệ thống đang có, không chứa dữ liệu người dùng:

- Chạy hai lần trong cùng session: qua, không trùng khóa hoặc hàm tạm.
- Session UTC: qua 00:00, 08:00, 16:00; 30/09 23:59:59.5; đầu năm; 29/02 năm nhuận.
- Không xác nhận reset: bị chặn và dữ liệu giữ nguyên.
- Cố tình đưa ngày về trước ngày sinh: assertion cuối thất bại **sau các INSERT**, dữ liệu cũ được rollback nguyên vẹn.
- Kiểm tra ngay trong transaction: Customer đủ lịch sử/đánh giá/giao dịch; nhân viên đúng phòng/ca; biểu mẫu đã công bố trước ký, đủ trường bắt buộc; tuổi/giới phù hợp; không tạo bệnh án cho dịch vụ chưa bắt đầu; không trùng VIS; đối soát tiền và ledger.
- Backend `compileJava` và 14 test thuộc `LaboratoryAnalyteCatalogTest`, `ClinicalFormEngineTest`, `VisitDetailResponsePublishedHistoryTest`, `MembershipCardRealtimeTest`: đạt. Không sửa test nền hoặc code nghiệp vụ.

Chạy lại kiểm thử phá hủy chỉ trên database kiểm thử riêng đã có schema:

```powershell
.\scripts\test-demo-seed.ps1 -Database cares_seed_validation -Port 5449 -Psql 'C:\Program Files\PostgreSQL\18\bin\psql.exe'
```

Hai thông báo lỗi ở kiểm thử bảo vệ/rollback là lỗi **được chủ động mong đợi**; script phải kết thúc bằng PASS. Script trả database kiểm thử về dữ liệu ngày thực tế sau các phép thử.

## Cần kiểm tra trên giao diện trước buổi bảo vệ

Chưa reset database đang chạy của người dùng, nên chưa xác nhận smoke test UI với bộ seed mới. Sau khi chuyển backend sang database demo, kiểm tra:

- Customer 0909000013: tên Nguyễn Anh Đức, lịch sử H13, hai thành viên gia đình, lịch T01.
- Customer 0909000001: H01 hiển thị hoàn thành một phần; không công bố bệnh án cho dịch vụ bỏ lượt.
- Customer 0909000003: phiếu thu T12 có BHYT 44.000đ, CareS 26.400đ, trừ thẻ 149.600đ (theo giá danh mục đang giữ nguyên).
- Lễ tân/Clinic Manager: đánh giá NEW/RESPONDED, nội dung nhắc đúng người tham gia; T11 có yêu cầu quay lại.
- Bác sĩ T07/T08/T09/T10: chỉ xử lý bệnh án hiện tại; không mở dịch vụ kế tiếp trước khi đủ điều kiện.
- Lab: CBC T09 chờ thực hiện; lịch sử N có kết quả đã ký, không có tệp giả.
- In bệnh án, xem kết quả CLS, tra cứu Guest và ba ca lịch trực.

Nếu cần bộ ca bệnh lý/đơn thuốc đa dạng hơn để trình bày chuyên môn, cần được người phụ trách chuyên môn duyệt riêng; bộ này không khẳng định là phác đồ chuẩn.
