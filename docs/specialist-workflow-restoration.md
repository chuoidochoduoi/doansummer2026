# Gỡ chức năng Khám Đa khoa — 04/09/2026

## Phạm vi và bảo toàn

- Gỡ điều phối Đa khoa, chỉ định khám chuyên khoa và tổng kết toàn VIS khỏi backend, đặt lịch Customer/Guest, tiếp nhận, màn khám, hành trình, bệnh án Customer và bản in.
- Giữ một bệnh án cho mỗi dịch vụ khám; CLS quay lại đúng bệnh án nguồn rồi mới mở dịch vụ khám tiếp theo. Giữ CLS đặt trước, nhiều vòng chỉ định, gom chỉ số theo gói, in riêng từng bệnh án, tái khám và kế thừa sinh hiệu từ bệnh án hoàn thành đầu tiên cùng VIS.
- Xóa các trường DTO chỉ phục vụ Đa khoa; frontend không gửi `specialistReferrals`. Giữ endpoint và hợp đồng trước tính năng, không thêm entity/enum/schema/migration.
- Giữ nhãn chính xác **Bệnh án trong cùng lượt** và tổng số bệnh án ở Customer, không đưa lại nhãn “bệnh án khác” có số đếm thiếu một.
- Không dùng reset/checkout hàng loạt, không viết lại lịch sử Git, không commit/push.

## Bản sao trước khi sửa

Bản sao cục bộ nằm tại `tmp/before-remove-gp-20260904/` (đã được ignore): mỗi repository có diff working tree/staged và bản sao các file đã sửa/chưa theo dõi; kèm XML kết quả test, JaCoCo và lint baseline.

Các commit backend `2260d7a`, frontend `b01474a` chỉ dùng đối chiếu từng hunk. Các bản sửa độc lập tạo lịch từ tập hợp ID, lịch trực quá khứ, kiểm tra mẫu hiệu lực, chặn ghi/ký CLS đã hủy và đính chính vẫn giữ. Cấu hình Gradle và các test không thuộc Đa khoa được so sánh với bản sao trước sửa.

Riêng hành trình giữ `includedTestIds` ở phạm vi VIS để không tạo bước quay lại giả khi dùng chung lượt Lab; bước khám ban đầu và tiến độ dịch vụ vẫn DONE khi đang quay lại sau CLS. Phiếu TEST_DONE qua ngày đã được gọi hôm nay vẫn có thể bắt đầu xử lý bệnh án nguồn.

## Dữ liệu

- Chỉ gỡ seed chuyên khoa Đa khoa, dịch vụ `EX-GP-001`, phòng `GP-101`, ba bác sĩ Đa khoa và lịch trực tương ứng. Các phần còn lại của `data.sql` giữ bản trước Đa khoa.
- Kiểm tra chỉ đọc database cấu hình mặc định `order_db` trên localhost:5437: 0 dịch vụ `EX-GP-001`, 0 QueueTicket, 0 liên kết lịch hẹn của dịch vụ này.
- Không chạy seed, reset, hoàn tiền hay thay đổi dữ liệu hiện tại. Backend chưa được khởi động lại vào database thật trong đợt kiểm tra.
- Nếu triển khai sang database khác, phải kiểm tra lại ca Đa khoa; có dữ liệu thực tế thì dừng và quyết định bảo toàn/chuyển đổi riêng, không tự xóa.

## Test được loại cùng tính năng

| File | Ca loại | Số lần thực thi |
| --- | --- | ---: |
| `GeneralPracticePolicyTest` | Toàn bộ 7 test riêng của policy Đa khoa; file đã có bản sao | 7 |
| `MedicalRecordServiceTest` | `specialistInheritsGpVitalsAfterInitialAssessmentWithoutPublishingGpDraft` | 1 |
| `PatientJourneyCycleTimelineTest` | `pendingGeneralPracticeOrderShowsClinicalTestsThenSpecialistThenFinalSummary` | 1 |
| `PatientJourneyCycleTimelineTest` | `prebookedGpServicesAppearBeforeSpecialistsAndInitialPhaseStaysDone` | 5 |
| `PatientJourneyCycleTimelineTest` | `gpActivationRespectsPaymentLabsSpecialistsAndUnfinishedResults` | 7 |
| `QueueTicketWorkflowTest` | `gpCannotCompleteSummaryWhileSpecialistOrResultIsUnfinished` | 2 |

Tổng 23 ca chỉ thuộc tính năng bị gỡ. Không xóa cả các file test hỗn hợp, không disable test hoặc loại package khỏi coverage.

Thêm 10 ca: 6 tổ hợp bước khám ban đầu/quay lại theo tiến độ CLS; 2 tổ hợp hai dịch vụ cùng/khác phòng không hoàn thành cùng nhau; không kế thừa sinh hiệu từ bệnh án chưa hoàn tất; bản sao sinh hiệu khác phòng độc lập và lần ba vẫn lấy lần đầu.

## Kết quả kiểm tra

| Kiểm tra | Trước khi gỡ | Sau khi gỡ |
| --- | --- | --- |
| Unit test | 1.040 đạt, 0 lỗi/skip | 1.027 đạt, 0 lỗi/skip |
| Integration Spring/H2 | 2 đạt, 0 lỗi/skip | 2 đạt, 0 lỗi/skip |
| JaCoCo LINE | 6.772/12.523 — 54,08% | 6.435/12.156 — 52,94% |
| JaCoCo BRANCH | 4.005/9.291 — 43,11% | 3.735/8.875 — 42,08% |
| Frontend build | Đạt | Đạt |
| Lint 10 file frontend liên quan | 46 lỗi, 26 cảnh báo | 46 lỗi, 25 cảnh báo |

Chạy unit/integration bằng `gradlew.bat test integrationTest --rerun-tasks`; frontend `npm run build` và ESLint các file liên quan. Không sửa lỗi lint cũ để tránh mở rộng phạm vi. Dữ liệu unit và integration tách riêng; JaCoCo chỉ tính unit test.

Ngưỡng 85% LINE/BRANCH giữ nguyên; **chưa đạt mục tiêu coverage**. Mẫu số giảm vì bỏ chức năng, không phải cấu hình loại trừ. Không coi test xanh là cổng `check` đạt.

Đã chạy riêng `gradlew.bat check` sau lượt kiểm thử đầy đủ: thất bại đúng tại `jacocoTestCoverageVerification` do cả LINE và BRANCH dưới 0,85; không có lỗi compile hoặc test mới trong lần kiểm tra này.

Một fixture regression mới ban đầu đặt kết quả COMPLETED nhưng mong bước quay lại BLOCKED. Đã tách hai tình huống: kết quả IN_PROGRESS thì BLOCKED; COMPLETED thì có thể TEST_DONE. Không thay production để ép qua kỳ vọng sai.

## Giới hạn và kiểm tra bàn giao

- Trình duyệt không kết nối được `http://localhost:3000/appointment` (`ERR_CONNECTION_REFUSED`). Chưa nghiệm thu trực tiếp desktop/mobile/dark mode, modal và bản in trên giao diện đang chạy.
- Khi bật backend/frontend cùng bản mới: kiểm tra đặt Nội + Tim mạch, chọn/bỏ CLS và xác nhận; khám đầu → CLS → quay lại → khám tiếp; Customer xem bệnh án/kết quả theo gói và in từng bệnh án; kiểm tra các màn ở mobile/dark mode.
- Không tạo phiếu/thanh toán/bệnh án thật để thử. Mockito không chứng minh khóa đồng thời PostgreSQL; giữ yêu cầu kiểm tra concurrency riêng trên database thử nghiệm.

## Tái kiểm tra ngày 05/09/2026

- Tạo thêm bản sao an toàn tại `work/remove-general-practice-20260905-090450/`, gồm patch của backend/frontend, trạng thái Git và bản nén toàn bộ file backend chưa được theo dõi. Thư mục này được ignore và không dùng làm source chạy.
- Đối chiếu lại với backend `2260d7a` và frontend `b01474a`: không còn `GeneralPracticePolicy`, DTO/payload chuyển chuyên khoa, mã `EX-GP-001`, phòng `GP-101`, bác sĩ/lịch trực Đa khoa hoặc giao diện tổng kết Đa khoa.
- `GENERAL_DOCTOR` và `SPECIALIST_DOCTOR` vẫn được giữ trong enum/phân quyền tương thích đã có trước đợt tính năng; không dùng chúng để kích hoạt luồng Đa khoa. Cụm “Phòng khám đa khoa CareS” vẫn là tên thương hiệu.
- Các thay đổi production còn khác mốc đối chiếu đều là sửa lỗi độc lập đã duyệt: nhận tập hợp ID khi tạo lịch, hành trình không lặp CLS dùng chung lượt Lab, xử lý phiếu tồn đọng qua ngày, bảo vệ lịch trực quá khứ và chặn ghi/ký/đính chính kết quả CLS không hợp lệ.
- Baseline mới: 1.100 unit test đạt, 2 integration test đạt, frontend build đạt và ESLint ba màn từng liên quan Đa khoa đạt. JaCoCo unit-test là 7.153/12.156 dòng (58,84%) và 4.229/8.875 nhánh (47,65%); ngưỡng 85% vẫn giữ nguyên và chưa được tuyên bố đạt.
- Không chạy lại `data.sql`, không reset database, không commit và không push.
