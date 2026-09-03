# Baseline refactor

Ngày ghi nhận: 03/09/2026, múi giờ `Asia/Ho_Chi_Minh`.

| Thành phần | Giá trị |
|---|---|
| Backend commit | `a44bd7f` |
| Frontend baseline chức năng | `2722e66` |
| Java | Oracle JDK 17.0.9 |
| Gradle | 9.5.1 |
| Spring Boot | 4.1.0 |
| Node.js | 24.18.0 |
| npm | 11.16.0 |
| Frontend production build | Đạt, 2.633 modules |
| Backend compile/test | Chưa chạy được do lỗi Java NIO loopback trước bước compile |

## Blocker môi trường backend

Gradle dừng tại `java.nio.channels.Selector.open` với `Unable to establish loopback connection` / `Invalid argument: connect`. Đây là lỗi tạo kết nối nội bộ giữa Gradle client và daemon, chưa phải lỗi biên dịch source.

Không thực hiện di chuyển package hoặc tách service backend cho tới khi `compileJava` và test liên quan chạy thành công trên môi trường hợp lệ.
