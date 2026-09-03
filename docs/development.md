# Phát triển và kiểm thử

## Yêu cầu

- Java 17
- PostgreSQL và Redis
- Node.js/npm cho frontend repository
- Các biến môi trường theo `.env.example`

## Backend

```powershell
.\gradlew.bat bootRun
.\gradlew.bat compileJava
.\gradlew.bat test
.\gradlew.bat jacocoTestReport
.\gradlew.bat buildHealth
```

Swagger UI: `http://localhost:8080/swagger-ui.html`.

## Frontend

```powershell
npm install
npm run dev
npm run lint
npm run check:dead-code
npm run build
```

## Quy tắc refactor an toàn

1. Một commit chỉ chứa một nhóm thay đổi.
2. Không trộn sửa nghiệp vụ với di chuyển hoặc format code.
3. Giữ controller/service public và transaction boundary.
4. Chỉ xóa code sau khi kiểm tra import động, route, Spring metadata và test.
5. So sánh Swagger, route-role và smoke test trước/sau.

## Smoke test tối thiểu

- Đăng nhập theo từng vai trò.
- Đặt lịch và check-in.
- Thanh toán, tạo hàng chờ và gọi bệnh nhân.
- Khám nhiều dịch vụ, chỉ định CLS và quay lại bác sĩ.
- Nhập/ký kết quả CLS.
- Hóa đơn, thẻ CareS và hoàn tác giao dịch.
- Lịch trực, hành trình và chốt ngày.
