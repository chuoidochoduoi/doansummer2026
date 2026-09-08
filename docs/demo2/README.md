# Bộ dữ liệu trình diễn hội đồng CareS

`data2.sql` là bộ seed dành riêng cho buổi trình diễn. File gọi danh mục chuẩn trong `data.sql`, sau đó chuẩn hóa tài khoản, lịch khám hai ca, tài khoản quầy chính ba ca, dữ liệu hai tháng và các trạng thái nghiệp vụ. Vì dùng `\ir`, hãy chạy bằng `psql`, không chạy riêng từng đoạn trong trình soạn thảo SQL.

Database mới phải có schema hiện tại trước khi seed. Cách an toàn là tạo database `cares_demo2`, khởi động backend một lần với `ddl-auto=update` và tắt nạp SQL, sau khi schema được tạo thì dừng backend rồi mới chạy lệnh dưới đây.

## Cách chạy

Chỉ dùng database demo riêng và dừng backend trước khi reset. Tên database không bị ràng buộc;
chốt an toàn bắt buộc là `SET cares.demo2_reset = 'yes'` trong đúng session chạy script.

```powershell
$env:PGPASSWORD = '<mật khẩu PostgreSQL demo>'
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -X -h 127.0.0.1 -p 5432 -U postgres `
  -d cares_demo2 -v ON_ERROR_STOP=1 `
  -c "SET cares.demo2_reset='yes';" `
  -f .\src\main\resources\data2.sql
```

Khởi động backend với `DATABASE_URL` trỏ đúng `cares_demo2` và `SPRING_SQL_INIT_MODE=never`. Không để backend tự nạp lại `data.sql`.

## Giờ hoạt động

- Thứ Hai–Thứ Bảy: 07:30–11:30 và 13:30–17:30.
- Chủ nhật nghỉ.
- Mỗi phòng có một bác sĩ cố định, được xếp cả hai ca.
- Chạy seed ngoài giờ vẫn dựng lịch sử và lịch tương lai, nhưng các phiếu hôm nay được đóng để không giả lập phòng khám đang mở.

## Lưu ý an toàn

- Toàn bộ danh tính, địa chỉ, giấy phép, thông tin chuyên môn và giao dịch đều là giả lập.
- Mật khẩu chung chỉ dùng cho database demo: `Cares@2026`.
- Không dùng file này trên database thật hoặc database chứa dữ liệu cần giữ.
- Nếu script dừng vì assertion, giao dịch data2 bị rollback. Chạy lại sau khi xử lý nguyên nhân.

Xem thêm [tài khoản](accounts.md), [bản đồ phòng–bác sĩ](room-doctor-map.md), [hành trình](journeys.md) và [kịch bản báo cáo](report-scenarios.md).
