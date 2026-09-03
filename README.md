# CareS Backend

Backend của hệ thống quản lý quy trình khám bệnh CareS, xây dựng bằng Spring Boot, Java 17 và PostgreSQL.

## Chạy dự án

```powershell
.\gradlew.bat bootRun
```

Mặc định API chạy tại `http://localhost:8080`; Swagger UI tại `http://localhost:8080/swagger-ui.html`.
Các biến môi trường cần thiết được mô tả trong `.env.example` và [tài liệu phát triển](docs/development.md).

## Kiểm tra trước khi hợp nhất

```powershell
.\gradlew.bat compileJava
.\gradlew.bat test
.\gradlew.bat buildHealth
```

`buildHealth` chỉ tạo danh sách dependency cần xem xét. Không xóa lớp Spring/JPA nếu chưa kiểm tra annotation, reflection và test.

## Tài liệu

- [Mục lục tài liệu](docs/README.md)
- [Kiến trúc hệ thống](docs/architecture.md)
- [Mô hình dữ liệu](docs/domain-and-data.md)
- [Luồng nghiệp vụ](docs/workflows.md)
- [Phân quyền](docs/roles-and-permissions.md)
- [Bảng truy vết chức năng](docs/traceability.md)
- [Baseline kiểm thử](docs/baseline.md)
