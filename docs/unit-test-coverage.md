# JUnit coverage: baseline và cổng 85%

## Trạng thái

**Chưa đạt mục tiêu.** Không được coi việc cấu hình ngưỡng là đã đạt coverage.

Baseline chạy lại toàn bộ tại commit `2260d7a`, ngày 04/09/2026:

- 605 test: 518 đạt, 87 lỗi; không phải baseline của 34 test thống kê.
- LINE: 4.304 / 12.125 = **35,50%**.
- BRANCH: 2.392 / 8.845 = **27,04%**.
- Baseline này còn gộp hai test Spring Context. Sau tách, báo cáo `jacocoTestReport` chỉ nhận `build/jacoco/test.exec` của unit test; không cộng integration coverage.
- File `QueueTicketServiceTest.java` đã bị comment toàn bộ trước đợt này, nên không có test từ file đó được thực thi. Không thay đổi hoặc xóa file này trong đợt thiết lập baseline.

Sau khi tách và thêm test hồi quy (cùng ngày):

- Unit: **604 test, 516 đạt, 88 lỗi** (87 lỗi cũ + 1 hồi quy mới).
- Integration: **2/2 đạt**, H2 và mock biên ngoài.
- Coverage chỉ unit: **LINE 4.095/12.125 = 33,77%; BRANCH 2.382/8.845 = 26,93%**.
- Đã chạy verification trên kết quả đầy đủ này: thất bại đúng ở cả hai ngưỡng 0,85. Không hạ ngưỡng. Lệnh kiểm tra coverage riêng dùng `-x test` chỉ để đọc kết quả suite vừa chạy, không phải nghiệm thu đạt.
- Đây là mốc trước khi chủ dự án duyệt sửa lỗi ép kiểu; tiến triển tiếp theo được ghi riêng bên dưới.

## Lỗi nghiệp vụ đã được duyệt sửa: APPOINTMENT-SET-CAST

- Đường gọi: `POST /api/v1/appointments` → `AppointmentService.create`.
- DTO `AppointmentCreateRequest.serviceIds` có kiểu `Set<UUID>`, nhưng service ép `(List<UUID>) req.serviceIds()` tại dòng 113.
- `Set` thông thường không phải `List`, gây `ClassCastException` trước bước kiểm tra ca/dịch vụ; không phải lỗi Mockito.
- Test hồi quy: `AppointmentCreateCollectionRegressionTest.realRequestWithSetMustReachShiftValidationWithoutCollectionCastFailure` sử dụng DTO thật, account/profile hợp lệ, một service ID và ca không tồn tại. Kỳ vọng lỗi nghiệp vụ `ResourceNotFoundException("Ca khám không tồn tại")`, không phải lỗi ép kiểu.
- Test hồi quy giữ kỳ vọng đúng; không dùng `assertThrows(ClassCastException)` để hợp thức hóa lỗi.
- Chủ dự án đã duyệt sửa riêng lỗi này: tạo `ArrayList` từ `Set`, giữ null để validation hiện có xử lý; không đổi DTO/API hoặc nhánh Customer `createMy`/`createMyGroup`.
- Bổ sung ba ca null, rỗng và có phần tử null để bảo đảm vẫn trả lỗi nghiệp vụ, không truy cập repository ghi lịch hoặc phân giải ca trước validation.
- Kiểm tra ngay sau sửa: 4/4 ca hồi quy đạt; nhóm `AppointmentServiceTest` còn 16/46 lỗi (trước đó 22/46). Không còn `ClassCastException` trong nhóm này.

## Đợt tiếp theo: fixture đặt lịch và phân quyền

- `AppointmentServiceTest`: cập nhật mock kiểm tra hồ sơ gia đình và đăng ký khám trùng ngày; không gọi repository của cơ chế chặn mọi VIS đang hoạt động cũ. Giữ kiểm tra bị từ chối không lập hóa đơn. Nhóm đặt lịch và hồi quy ép kiểu đạt **50/50**.
- Test trùng lịch được bổ sung service ID hợp lệ và `verify` kiểm tra xung đột: trước đây test dừng sớm ở validation thiếu dịch vụ nên chưa thực sự kiểm tra trùng lịch.
- `FamilyAccessServiceTest`: **20 ca** kiểm tra chủ tài khoản, hồ sơ ngoài gia đình, thành viên ngừng quản lý, danh sách lịch sử và người nhận thông báo. Kiểm tra policy thật, không chỉ mock kết quả kiểm tra quyền ở facade.
- `ServiceAvailabilityServiceTest`: gắn chuyên khoa phòng đúng với bác sĩ và dịch vụ trong fixture thành công; không đổi quy tắc phân công.
- `TestRequestAuthorizationServiceTest`: cập nhật fixture bác sĩ thuộc phòng và mock `StaffDutyService` theo cơ chế hiện tại. Hàm trực phòng trả `BadRequestException` khi không thuộc phòng; kiểm tra quyền xem vẫn trả `AccessDeniedException`. Bổ sung các ca đã hoàn tất/đã hủy, chưa vào phòng và bác sĩ không liên quan.
- `StaffDutyServiceTest`: kiểm tra trực tiếp quyền phòng/chuyên khoa/năng lực, ca qua đêm, đầu/cuối ca, dữ liệu ca thiếu và lọc tài khoản ngừng hoạt động; dùng mốc thời gian cố định 2030-06-10 cho phép kiểm tra thời gian.
- Các test facade dùng mock không chứng minh khóa database hoặc đồng thời; các test thời gian không sửa đồng hồ hoặc database demo.
- Không thay production ngoài lỗi ép kiểu đã được duyệt. Không commit/push trong đợt test.

### Kết quả toàn suite sau đợt này (04/09/2026)

- Unit: **653 test, 593 đạt, 60 lỗi**, không bỏ qua test. Các nhóm vừa sửa/thêm đạt **106/106**: đặt lịch 50, gia đình 20, năng lực phòng 4, quyền Lab 12, trực phòng 20.
- Integration: **2/2 đạt** trong lần chạy `test integrationTest --rerun-tasks --continue` cùng đợt. Các chỉnh sửa sau đó chỉ ở test unit.
- Coverage unit toàn backend: **LINE 4.408/12.125 = 36,35%; BRANCH 2.582/8.847 = 29,19%**. Không dùng báo cáo chạy lọc lớp làm số liệu toàn hệ thống.
- Verification vẫn thất bại ở cả hai ngưỡng 85%, đúng thiết kế. Chưa nghiệm thu.
- Còn 60 lỗi: CustomerVisit 10, Department 24, Invoice 11, MedicalRecord 13, PatientJourneyCycleTimeline 1, PatientQueue 1. Đây là lỗi test còn phải phân loại, không đồng nghĩa có 60 lỗi chức năng thật.

| Phạm vi | Test kiểm chứng |
|---|---|
| API tạo lịch nhận Set, validation lựa chọn | `AppointmentCreateCollectionRegressionTest` |
| Tạo/cập nhật/hủy/check-in lịch | `AppointmentServiceTest` |
| Chủ hồ sơ, thành viên, thông báo | `FamilyAccessServiceTest` |
| Chuyên khoa phòng, năng lực thực hiện | `ServiceAvailabilityServiceTest` |
| Xem/nhập/ký/hủy CLS và trạng thái phòng | `TestRequestAuthorizationServiceTest` |
| Quyền trực phòng, năng lực, biên ca và qua đêm | `StaffDutyServiceTest` |

## Phân loại lỗi baseline

| Nhóm test | Lỗi / tổng | Đánh giá ban đầu |
|---|---:|---|
| AppointmentServiceTest | 22/46 | Có lỗi ép kiểu thật; còn lỗi mock/dependency cần rà từng ca |
| CustomerVisitServiceTest | 10/31 | Cần rà fixture và dependency theo luồng hiện tại |
| DepartmentServiceTest | 24/53 | Có fixture phòng CLS thiếu danh mục kỹ thuật và lỗi mock |
| InvoiceServiceTest | 11/48 | Cần rà fixture và kỳ vọng theo quy tắc hiện hành |
| MedicalRecordServiceTest | 13/28 | Có stub không dùng và fixture/validation chưa khớp |
| PatientJourneyCycleTimelineTest | 1/2 | Kỳ vọng gộp khám cần đối chiếu quy tắc tách từng dịch vụ |
| PatientQueueServiceTest | 1/12 | Cần đối chiếu fixture hàng chờ quay lại |
| ServiceAvailabilityServiceTest | 1/4 | Fixture bác sĩ có chuyên khoa nhưng phòng chưa gắn chuyên khoa |
| TestRequestAuthorizationServiceTest | 4/6 | Cần rà quyền và dependency phòng CLS |

Đây không phải kết luận tất cả 87 lỗi đều là bug production; không sửa kỳ vọng hàng loạt.

## Chạy kiểm tra

```powershell
.\gradlew.bat test
.\gradlew.bat integrationTest
.\gradlew.bat check --continue
```

- `test`: JUnit Jupiter, không chạy tag `integration`.
- `integrationTest`: hai test Spring/H2, tag `integration`, nằm trong `check`. Mock email, SMS, PayOS, BHXH, Redis và tác vụ chốt ngày; database kiểm thử H2, `spring.sql.init.mode=never`.
- `CheckDBTest` kiểm tra lưu/tìm tài khoản giả lập, rollback transaction; không in hồ sơ từ database.
- `jacocoTestCoverageVerification`: LINE và BRANCH đều ≥0,85 trên toàn bộ class production, không thêm loại trừ. `check` phải thất bại khi test hoặc coverage chưa đạt.
- HTML coverage: `build/reports/jacoco/test/html/index.html`; XML cùng thư mục cha. Báo cáo test: `build/reports/tests/test` và `build/reports/tests/integrationTest`.
- Nếu cần đọc coverage dù test đang đỏ: `gradlew jacocoTestReport -x test` sau khi đã chạy toàn bộ unit test. Không dùng kết quả một nhóm `--tests` để tuyên bố coverage toàn bộ suite.
- Khi nghiệm thu cuối phải dùng lượt chạy không cache (`--rerun-tasks`) và chạy lại xác nhận ổn định. Chưa nghiệm thu 85% ở giai đoạn này.

Môi trường Windows hiện dùng Java 17 và `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=D:\gitlap\doAnSummer2026\tmp` để tránh lỗi loopback. Đây là cấu hình môi trường, không phải thay đổi source production.

## Phần còn lại

Sau khi được duyệt xử lý lỗi thật: sửa fixture/mock từng nhóm, bổ sung nhánh chưa phủ, khôi phục kiểm thử hàng chờ bằng ca phù hợp hành vi hiện tại; chạy lại toàn suite và xác nhận cả hai tỷ lệ. Không đổi nghiệp vụ để chạy qua test, không dùng test bỏ qua/coverage exclusion và không cộng integration vào unit coverage.

## Rà soát tiếp theo theo mục tiêu liên tục

- Tiếp nhận: bổ sung repository kiểm tra trùng dịch vụ cùng ngày, ca trực hợp lệ, tra cứu biến thể số điện thoại và cờ xác nhận cập nhật hồ sơ. Không dùng mock luật chặn toàn bộ VIS cũ. Đầu vào trùng ID được chuẩn hóa theo quy tắc chọn gói/chỉ số hiện hành; test xác minh chỉ tra cứu dịch vụ một lần.
- Phòng: fixture cập nhật một phần phải phân biệt danh sách `null` (giữ nguyên) và rỗng (gỡ hết). Thêm danh mục kỹ thuật, năng lực nhân viên và quan hệ phòng đúng. Dùng `StaffDutyService` thật với repository mock để không bỏ qua validation năng lực. Kiểm tra từ chối trước ghi phân công nhân viên; rollback transaction không được suy ra từ Mockito.
- Bệnh án: các ca kiểm tra nội dung chạy dưới bác sĩ điều trị thực sự. Thêm hai test riêng từ chối Admin sửa hoặc chốt bệnh án qua luồng bác sĩ. Không mở lại quyền Admin trong production.
- Hóa đơn: bổ sung mock ledger/thông tin kết quả CLS cùng ngày/điều phối; fixture bác sĩ phòng phải có tài khoản đang hoạt động. Giữ assertion tiền và phiếu hàng chờ.
- Hàng chờ quay lại: fixture có bệnh án nguồn, hóa đơn chỉ định và kết quả đã hoàn thành, thay vì chỉ tạo một phiếu `TEST_DONE` mồ côi không có đợt CLS.
- `FamilyMemberServiceTest`: tạo/cập nhật/ngừng quản lý/khôi phục, chống trùng, phân quyền, trạng thái dị ứng, lịch hẹn và VIS đang hoạt động; mọi repository cô lập.
- `AuthServiceTest`: đăng nhập bằng liên hệ hiện hành, chặn username Customer cũ, quyền staff, rate-limit và lỗi Redis, refresh, OTP đăng ký/reset, nối lịch sử Guest và định danh trong SecurityContext. Không gửi OTP hoặc dùng Redis thật.
- Lỗi nghiệp vụ cần duyệt được giữ test đỏ và ghi ở [danh sách chờ duyệt](unit-test-approval-queue.md). Không dừng rà soát các lớp khác vì lỗi này, theo yêu cầu chủ dự án.

## Mốc toàn bộ suite sau nhóm Auth và gia đình

- 730 unit test: 729 đạt, 1 lỗi hành trình `JOURNEY-SHARED-LAB-RETURN` đang chờ duyệt.
- JaCoCo unit: LINE 4.995/12.125 (41,20%); BRANCH 2.905/8.847 (32,84%). Đây là kết quả chạy toàn bộ suite, không phải riêng Auth.
- Verification vẫn thất bại đúng thiết kế vì chưa đạt 85% ở cả hai tiêu chí. Không giảm ngưỡng hoặc loại package.
- Nhóm tiếp theo `StaffServiceTest`: tạo nhân sự với vai trò chuẩn hóa, dữ liệu trùng, giới hạn sửa vai trò/chuyên khoa, khóa tài khoản có phân công, bảo vệ tài khoản quản trị, cấp kỹ thuật và chống trùng, danh sách hoạt động/công khai, phân trang. Kiểm tra tác động repository; không dùng mock làm bằng chứng rollback transaction thực tế.

## Mốc sau nhóm nhân sự

- `StaffServiceTest`: 61/61 đạt. Toàn bộ unit suite: 791 test, 790 đạt, vẫn chỉ lỗi hành trình đã ghi chờ duyệt.
- JaCoCo từ lượt chạy toàn bộ này: LINE 5.266/12.125 (43,43%); BRANCH 3.074/8.847 (34,75%). Chưa đạt mục tiêu 85%.
- Không có thay đổi production bổ sung; `AppointmentService.create` vẫn là sửa lỗi production duy nhất đã được duyệt trong đợt này.

## Biểu mẫu kết quả cận lâm sàng

- `ClinicalFormTemplateServiceTest`: 42/42 test đạt trong lượt chạy riêng nhóm.
- Phủ tạo mẫu đúng context, mã trùng, tác giả hợp lệ; lưu lại draft hoặc tạo phiên bản mới mà không sửa bản published; phát hành hôm nay/tương lai, từ chối ngày quá khứ; ngừng dùng và tác vụ loại phiên bản cũ đã có bản thay thế hiệu lực.
- Bảo vệ tám mẫu Lab hệ thống khỏi thao tác sửa/phát hành/ngừng/liên kết của manager; không khôi phục mẫu khám chuyên khoa.
- Liên kết dịch vụ khử ID trùng, kiểm tra tồn tại và loại dịch vụ trước khi gỡ liên kết; chọn đúng phiên bản hiện hành hoặc lịch sử của chính mẫu đang liên kết. Từ chối phiên bản của mẫu khác.
- Ngày nghiệp vụ được cố định trong test bằng Mockito static scope; không sửa đồng hồ production, không kết nối database hoặc gọi tác vụ thật.
- Kết quả chạy riêng nhóm không được dùng làm coverage toàn backend; cần đo lại bằng toàn bộ suite.
- Đã chạy lại toàn bộ sau nhóm này: 833 unit test, 832 đạt, chỉ lỗi `JOURNEY-SHARED-LAB-RETURN`. JaCoCo unit LINE 5.407/12.125 (44,59%), BRANCH 3.137/8.847 (35,46%); mục tiêu 85% chưa đạt.

## Sau sửa lỗi hành trình đã được duyệt

- `JOURNEY-SHARED-LAB-RETURN`: chuyển phạm vi ghi nhận CLS đã hiển thị sang toàn VIS, tránh bước quay lại giả từ dữ liệu fallback bệnh án tiếp theo. Giữ hợp đồng API và mọi dữ liệu/điều phối gốc.
- Test tái hiện được mở rộng bốn tổ hợp cùng/khác phòng khám và cùng/khác lượt Lab; không gộp nhầm lượt Lab độc lập. Test hai đợt chỉ định thực sự vẫn đạt.
- Chạy `test integrationTest --continue`: BUILD SUCCESSFUL; 836/836 unit test đạt. Integration test cũng hoàn tất thành công.
- JaCoCo chỉ từ unit: LINE 5.408/12.125 (44,60%); BRANCH 3.139/8.847 (35,48%). Chưa đạt 85%, không coi lần chạy test xanh là hoàn thành mục tiêu coverage.

## Lịch trực — test thực thi mới

- File `StaffScheduleServiceTest` cũ đang được comment toàn bộ từ trước đợt này; không tính là test đang thực thi. Bổ sung `StaffScheduleWorkflowTest` riêng, không xóa test cũ.
- 41 test mới: phân ca đúng ngày/khung giờ, dữ liệu thiếu, ca đóng, trùng ca/trùng giờ, ca tiếp giáp không chồng lấn, kiểm tra phòng chuyên môn, một bác sĩ mỗi phòng/ca, gán lặp idempotent, tài khoản ngừng hoạt động, quyền xem lịch, xóa lịch tương lai, cập nhật và sao chép/sinh lịch từ mẫu.
- Kiểm tra không gỡ người đủ điều kiện cuối cùng khi dịch vụ đã được đặt sẽ mất khả năng thực hiện. Notification lỗi không làm mất phân công. Mockito kiểm tra lệnh repository, không thay bằng chứng khóa hoặc rollback database.
- Cố định ngày Việt Nam trong test; không gọi database, thông báo hay bộ điều phối thật. Không sửa production trong nhóm này.
- Chạy toàn bộ unit: 877/877 đạt; JaCoCo LINE 5.725/12.125 (47,22%), BRANCH 3.301/8.847 (37,31%). Chưa đạt cổng 85%.

## Sau bản sửa sinh lịch và mở rộng CLS

- Bản sửa `SCHEDULE-GENERATE-HISTORY` đã được duyệt: kiểm tra lịch cũ/mới chưa bắt đầu, không ghi đè trạng thái đã xử lý; chỉ xóa sau khi toàn bộ batch hợp lệ. Test biên cùng ngày và batch có mẫu sai đạt. Full unit + integration sau sửa đã BUILD SUCCESSFUL (integration 2 test).
- Thêm `ShiftConfigServiceTest`: giờ ba ca mặc định, phiên bản nền, bảo toàn snapshot giờ cũ, không ghi đè lịch sử đã có nhiều phiên bản, chọn phiên bản hiệu lực khi đọc.
- Mở rộng `ServiceAvailabilityServiceTest`: cấu hình phòng/năng lực, nhân viên thiếu hoặc ngừng hoạt động, sai phòng/chuyên khoa/role, loại nhân viên đang định gỡ, phòng đóng và chống nhân bản người đủ điều kiện.
- Thêm `TestRequestWorkflowTest`: tạo yêu cầu và từ chối dữ liệu sai, chống thu/tạo trùng, gắn CLS đặt trước đúng VIS, chuyển trạng thái theo queue; thanh toán lại sửa queue thiếu mà giữ bệnh án nguồn và không làm sống lại yêu cầu hoàn thành.
- Lượt full suite trước regression CLS mới: 955/955 đạt. Coverage unit LINE 6.024/12.139 (49,63%), BRANCH 3.499/8.855 (39,51%). Mẫu số tăng do bản sửa nghiệp vụ lịch trực đã duyệt, không có loại trừ coverage.

## Sau bản sửa chặn ghi kết quả đã hủy

- Sửa `LAB-CANCELLED-RESULT` theo phê duyệt: chặn CANCELLED tại các đường ghi/ký/tải tệp; kiểm tra toàn bộ nhóm trước khi lưu. Không thay API, entity, schema hoặc API đọc lịch sử.
- Chạy đầy đủ `test integrationTest --continue`: BUILD SUCCESSFUL, **966/966 unit**, **2/2 integration**, không có lỗi.
- JaCoCo unit: **LINE 6.145/12.148 (50,58%)**, **BRANCH 3.546/8.857 (40,04%)**. Chưa đạt mục tiêu 85%; chưa chạy thành công cổng `check`.
- Số liệu trên là lượt toàn bộ trước test tái hiện nghi vấn ký nháp qua endpoint đính chính. Không dùng kết quả chạy riêng một nhóm để thay baseline toàn backend.

## Sau bản sửa ký đính chính

- `LAB-AMEND-INITIAL-DRAFT` đã được duyệt: chặn sửa/ký đính chính nếu yêu cầu chưa COMPLETED hoặc bản nháp không có lý do đính chính. Luồng hoàn thành ban đầu không thay đổi.
- Bổ sung test trạng thái chưa hoàn thành, sửa/ký hợp lệ, revision khác yêu cầu/không tồn tại/đã ký, lý do trống và thiếu kết luận. Kiểm tra revision cũ SUPERSEDED, revision mới SIGNED và kết quả cập nhật, không điều phối lại queue khi đính chính.
- Full `test integrationTest --continue`: BUILD SUCCESSFUL, **983/983 unit**; integration hoàn tất thành công.
- JaCoCo unit: **LINE 6.197/12.152 (51,00%)**, **BRANCH 3.574/8.863 (40,32%)**. Chưa đạt 85%.
- Lượt này trước regression tiếp theo về trạng thái mẫu đã lưu khi ký; không cộng coverage integration vào số liệu unit.

## Sau bản sửa kiểm tra mẫu hiệu lực

- `LAB-SIGN-STORED-SPECIMEN` đã sửa theo duyệt; trạng thái mẫu hiệu lực được kiểm tra trước ký, không chỉ payload. Có test từ chối mẫu lỗi và ký được khi đã cập nhật ACCEPTED.
- Full `test integrationTest --continue`: **990/990 unit, 2/2 integration**, BUILD SUCCESSFUL.
- Unit JaCoCo **LINE 6.283/12.152 (51,70%)**, **BRANCH 3.637/8.863 (41,04%)**. Chưa đạt cổng 85%.
- Đây là lượt toàn bộ trước khi bổ sung `QueueTicketWorkflowTest`; các lượt chạy riêng hàng chờ không thay thế baseline này.
