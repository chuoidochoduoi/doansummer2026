# Tài liệu hệ thống CareS

Đây là điểm bắt đầu duy nhất cho tài liệu đồ án và tài liệu kỹ thuật. Swagger là nguồn chính xác cho hợp đồng API; source code và enum là nguồn chính xác cho trạng thái nghiệp vụ.

## Tổng quan

- [Kiến trúc hệ thống](architecture.md)
- [Mô hình domain và dữ liệu](domain-and-data.md)
- [Vai trò và phân quyền](roles-and-permissions.md)
- [Các luồng nghiệp vụ chính](workflows.md)
- [Bảng truy vết chức năng](traceability.md)
- [Actor và Use Case Catalog MEMS-GC — Word, đối chiếu source và mẫu cũ](use-cases/README.md)
- [Danh sách chức năng toàn hệ thống — System Function List](function-list/README.md)
- [Thống kê Clinic Manager và quy tắc tính số liệu](reports.md)

## Phát triển và vận hành

- [Cài đặt, cấu hình và kiểm thử](development.md)
- [JUnit coverage, cổng 85% và lỗi cần phê duyệt](unit-test-coverage.md)
- [Khôi phục luồng chuyên khoa và bảo toàn test](specialist-workflow-restoration.md)
- [Baseline hiện tại](baseline.md)
- [Dữ liệu demo, reset an toàn và tài khoản](demo/README.md)
- [ERD tổng quan](diagrams/ERD-Tong-Quan.drawio)
- [ERD chi tiết](diagrams/ERD-Chi-Tiet.drawio)
- [Schema tham khảo](../database-schema.sql)

## Quy tắc cập nhật tài liệu

1. Thay đổi route, quyền, API hoặc trạng thái phải cập nhật tài liệu tương ứng trong cùng commit.
2. Không chép toàn bộ DTO vào Markdown; dùng Swagger tại `/swagger-ui.html`.
3. Mermaid dùng cho sơ đồ đọc nhanh trong Git; Draw.io giữ bản ERD chỉnh sửa chi tiết.
4. Mỗi chức năng mới phải có dòng trong bảng truy vết và ít nhất một kiểm thử chấp nhận.
