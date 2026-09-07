# Lỗi chờ duyệt trong đợt nâng coverage

## QUEUE-OVERDUE-CALLED-START — gọi lại qua ngày nhưng không bắt đầu khám được

**Trạng thái: xác nhận trước khi triển khai Đa khoa; chưa sửa production, chờ duyệt riêng theo điều kiện kế hoạch.**

- Test `QueueTicketWorkflowTest.yesterdayCompletedTestsCanBeCalledThenStartedToCloseOriginalRecord` thực hiện liên tiếp `call` rồi `startExam` trên phiếu hôm trước ở TEST_DONE, có bệnh án IN_PROGRESS nguồn.
- `call` thành công và chuyển TEST_DONE thành CALLED. `startExam` lập tức bị `ensureCallableToday` từ chối vì ngoại lệ qua ngày chỉ nhận TEST_DONE, không nhận CALLED vừa được tạo bởi chính thao tác gọi hợp lệ.
- Kết quả chạy: 27 test hàng chờ, 26 đạt, 1 regression thất bại với BadRequestException “Phiếu hàng chờ đã qua ngày…”. Không gọi bệnh nhân hoặc sửa database thật.
- Giao diện dùng endpoint `/api/v1/queue-tickets/{id}/start-exam` qua `useQueueActions.js`; đây không chỉ là endpoint tương thích không dùng. Lỗi ảnh hưởng bước bắt đầu sau khi gọi lại hồ sơ tồn đọng, đồng thời là tiền đề của bước tổng kết Đa khoa qua ngày.
- Hướng sửa: nhận diện đúng hồ sơ khám nguồn đang xử lý và lần gọi lại hợp lệ trong ngày, không chỉ dựa vào trạng thái TEST_DONE tức thời. Không mở lại các dịch vụ chưa bắt đầu của ngày cũ; không nới mọi phiếu CALLED qua ngày. Test thêm gọi lại, bắt đầu, tải lại, chốt ngày và các phiếu cũ không đủ điều kiện.
- Chưa triển khai tính năng Đa khoa trước khi giải quyết điều kiện nền này; giữ nguyên thay đổi kiểm thử và các bản sửa đã duyệt trước đó.

## LAB-SIGN-STORED-SPECIMEN — ký bỏ qua trạng thái mẫu đã lưu

**Trạng thái: đã được duyệt và sửa; full unit/integration đạt.**

- `completeResult` kiểm tra `r.getSampleStatus()` sau khi áp dụng đầu vào, thay vì chỉ kiểm tra trạng thái gửi lên. Không đổi DTO/API/schema.
- Bảy biến thể: trạng thái lỗi đã lưu và không gửi lại; gửi mới REJECTED/RECOLLECT; chuyển mẫu lỗi sang ACCEPTED; giữ ACCEPTED đã lưu. Kiểm tra ký revision, hoàn thành queue và refresh hành trình ở nhánh hợp lệ.

- Regression: `TestRequestWorkflowTest.signingCannotIgnorePreviouslyRejectedSpecimen`, hai trường hợp REJECTED và RECOLLECT.
- Yêu cầu IN_PROGRESS, bệnh nhân đang trong phòng, đúng bác sĩ phụ trách; bản nháp có mẫu BLOOD, người/thời điểm lấy mẫu và trạng thái không đạt/cần lấy lại. Gửi ký với sampleStatus null để giữ thông tin mẫu đã lưu, không phải gửi ACCEPTED.
- `applySpecimenInformation` giữ trạng thái cũ đúng, nhưng `completeResult` chỉ kiểm tra `req.sampleStatus()` thay vì trạng thái hiệu lực của TestResult. DTO cho phép null nên controller validation không chặn trường hợp này.
- Test xác nhận ký vẫn thành công: yêu cầu chuyển COMPLETED, TestResult có verifiedAt và ghi result/revision mặc dù mẫu không đạt. Không ký hay ghi dữ liệu thật; chỉ dùng Mockito.
- Frontend `useLabDetail.save` gửi sampleStatus từ payload (hoặc null); màn Lab thông thường nạp trạng thái mẫu. Không kết luận thao tác giao diện hiện tại luôn gặp lỗi này; đây là thiếu kiểm tra ở API khi payload không gửi lại trạng thái.
- Đề xuất: kiểm tra trạng thái mẫu hiệu lực sau khi áp dụng dữ liệu đầu vào, chặn REJECTED/RECOLLECT đã lưu hoặc vừa gửi. Vẫn cho ký sau khi mẫu đã được cập nhật ACCEPTED hợp lệ. Không thay DTO/API/schema.
- Lượt nhóm: 77 test, 75 đạt, 2 regression mới thất bại. Baseline toàn bộ trước regression: 983/983 unit, 2/2 integration đạt; LINE 51,00%, BRANCH 40,32%.

## LAB-AMEND-INITIAL-DRAFT — ký nháp ban đầu qua luồng đính chính

**Trạng thái: đã được duyệt và đã sửa.**

- Guard dùng chung trong `findDraftRevision`: chỉ nhận yêu cầu COMPLETED và revision DRAFT có lý do đính chính. Giữ nguyên quyền, kiểm tra revision thuộc yêu cầu và luồng hoàn thành ban đầu.
- Bổ sung test các trạng thái chưa hoàn thành, sửa/ký đính chính hợp lệ, revision không tồn tại/khác yêu cầu/đã ký/không có lý do và thiếu kết luận.

- Test: `TestRequestWorkflowTest.initialDraftCannotBeSignedThroughAmendmentEndpoint`.
- Tình huống hợp lệ: yêu cầu IN_PROGRESS, queue IN_PROGRESS, đã lưu kết quả và revision DRAFT lần 1; bác sĩ đúng phòng/ca. Đây là bản nháp ban đầu, chưa có kết quả ký để đính chính.
- `amendResult` yêu cầu TestRequest COMPLETED, nhưng `signAmendment` chỉ kiểm tra revision thuộc yêu cầu và còn DRAFT. Vì thế gọi endpoint ký đính chính trực tiếp với revision đầu tiên vẫn ký thành công.
- Test xác nhận không ném ConflictException; revision thành SIGNED, TestResult có verifiedAt, cả hai repository bị ghi. TestRequest/queue vẫn IN_PROGRESS vì đường ký đính chính không thực hiện điều phối hoàn thành lần đầu. Không tác động dữ liệu thật.
- Endpoint đang đăng ký: POST `/api/v1/test-requests/{id}/result/amend/{revisionId}/sign`, quyền DOCTOR. Chưa thấy tham chiếu frontend qua tìm kiếm `amend/|signAmendment`; không coi đó là bằng chứng API vô hại.
- Đề xuất: chỉ sửa/ký đính chính trên yêu cầu COMPLETED và bản đính chính hợp lệ; bản nháp ban đầu phải qua luồng hoàn thành kết quả hiện có để cập nhật hàng chờ/hành trình. Giữ đọc lịch sử, kiểm tra sở hữu revision và quyền bác sĩ.
- Lượt nhóm sau thêm regression: 59 test, 58 đạt, 1 lỗi nghiệp vụ mới. Lượt toàn bộ trước regression: 966/966 unit và 2/2 integration đạt; coverage 50,58% dòng, 40,04% nhánh.

## LAB-CANCELLED-RESULT — yêu cầu đã hủy vẫn ghi và ký kết quả

**Trạng thái: người dùng đã duyệt; đã bổ sung chốt chặn cho yêu cầu CANCELLED.**

- Chặn tạo/cập nhật/ký kết quả, tải PDF/tệp đính kèm và sửa/ký đính chính trước thao tác ghi. Luồng tạo đính chính đã có điều kiện chỉ nhận COMPLETED nên giữ nguyên.
- Lưu phiếu nhóm kiểm tra đại diện và toàn bộ yêu cầu được mua trước khi ghi kết quả đầu tiên. API đọc và lịch sử không thay đổi.
- Mở rộng regression cho bảy đường ghi, hai biến thể lưu nhóm và kiểm tra lưu nháp hợp lệ vẫn tạo revision DRAFT.

- Regression: `TestRequestWorkflowTest.cancelledRequestCannotWriteOrSignResults`, ba biến thể `create`, `update`, `complete`.
- Dữ liệu có thể phát sinh từ `cancel`: TestRequest `CANCELLED`; QueueTicket chuyển `DONE` khi mọi kỹ thuật đã hủy. Nhân viên đúng phòng, đúng quyền/trực; đã có nháp ở ca update/complete.
- Mong đợi: từ chối thao tác ghi/ký trên yêu cầu đã hủy, không ghi TestResult hoặc revision, giữ `CANCELLED`.
- Thực tế test: cả ba thao tác không ném lỗi và đều gọi save result/revision; `completeResult` còn đặt `CANCELLED` thành `COMPLETED` và ký revision. Không có database thật hoặc ký kết quả thực tế trong đợt test này.
- Nguyên nhân: ba phương thức chỉ chặn `COMPLETED`; guard queue chấp nhận `DONE`, trong khi đây cũng là trạng thái sau hủy. `actionPermissions` đã trả không cho sửa nhưng API mutation không chặn `CANCELLED` tương ứng.
- Endpoint: POST/PUT `/api/v1/test-requests/{id}/result` (Doctor/Nurse) và POST `/api/v1/test-requests/{id}/result/complete` (Doctor). Controller kiểm tra role/DTO rồi gọi service; không tự chặn trạng thái hủy.
- Hướng sửa sau duyệt: chặn yêu cầu đã hủy tại các đường ghi/ký kết quả trước khi sửa entity/save; rà soát upload và thao tác nhóm để không còn đường vòng. Giữ thao tác đọc, không tự sửa dữ liệu lịch sử; thêm test trạng thái đang làm vẫn chạy bình thường.
- Test riêng nhóm: 50 test, 47 đạt, 3 regression thất bại. Lượt toàn bộ trước regression: 955/955 đạt; không lấy kết quả nhóm để công bố coverage toàn hệ thống.

## SCHEDULE-GENERATE-HISTORY — sinh lịch từ mẫu ghi đè lịch đã hoàn thành

**Trạng thái: đã được chủ dự án duyệt sửa; đã triển khai và test nhóm lịch trực đạt.**

- Test: `StaffScheduleWorkflowTest.templateOverrideMustNotReplaceCompletedHistoricalShift`.
- Ngày kiểm thử cố định 04/09/2026 (Việt Nam), chọn tuần trước đó, mẫu thứ Hai và lịch đã `COMPLETED`, `overrideExisting=true`.
- Mong đợi: từ chối trước khi xóa/ghi lịch. Quy tắc tạo/gán/xóa/sao chép lịch hiện hành đều bảo vệ lịch quá khứ hoặc đã diễn ra.
- Thực tế unit test: `generateFromTemplates` không ném lỗi, gọi `deleteAll` cho lịch hoàn thành rồi `saveAll` lịch thay thế `SCHEDULED`. Test giữ ba assertion: phải từ chối, không xóa, không tạo lại. Cả sai lệch hành vi và lệnh ghi repository được kiểm tra, chưa thực thi trên database thật.
- Entry point vẫn đăng ký: `POST /api/v1/schedules/generate`, quyền Admin/Clinic Manager; controller chuyển thẳng dữ liệu vào service, không kiểm tra ngày.
- Tìm trong frontend `src` chưa thấy tham chiếu đường dẫn này; không kết luận không có client bên ngoài sử dụng. Không ảnh hưởng mọi lần tick lịch thông thường, nhưng endpoint còn hoạt động có nguy cơ làm sai lịch sử nếu được gọi với tham số trên.
- Hướng sửa đề xuất: kiểm tra ngày/giờ và trạng thái lịch trước mọi thao tác batch xóa; không ghi đè lịch đã bắt đầu/hoàn thành; giữ sinh lịch tương lai. Kiểm tra toàn bộ batch trước khi ghi để tránh ảnh hưởng một phần và bổ sung test biên tuần hiện tại.
- Không sửa production trước duyệt. Lượt test riêng nhóm: 42 test, 41 đạt, 1 regression mới thất bại như kỳ vọng. Coverage toàn backend gần nhất vẫn là lượt 877/877 trước regression, không lấy báo cáo nhóm làm baseline toàn backend.

### Bản sửa đã duyệt

- Kiểm tra lịch cũ phải còn `SCHEDULED` và chưa bắt đầu; lịch mới cũng phải chưa bắt đầu theo giờ đã resolve. Không ghi đè `COMPLETED`/`ABSENT`, không sinh lịch quá khứ hoặc ca hiện tại đã bắt đầu.
- Gom danh sách thay thế trong bộ nhớ, chỉ xóa/flush/sinh mới sau khi mọi mẫu của batch qua kiểm tra. Giữ transaction và khóa tuần hiện có.
- Kiểm tra trùng giờ bỏ qua đúng các ID sắp được thay thế, nhưng vẫn chặn ca khác chồng giờ. Không phải xóa trước mới kiểm tra được.
- Không bật override thì các lịch đã tồn tại vẫn được bỏ qua như cũ; tạo tuần tương lai vẫn giữ hành vi hiện tại.
- Test bổ sung: trước giờ/đúng giờ/sau giờ bắt đầu hôm nay; trạng thái hoàn thành/vắng không bị đặt lại; mẫu quá khứ nằm sau mẫu hợp lệ không gây xóa một phần; ca khác chồng giờ vẫn bị chặn.

Không sửa production ở các mục dưới đây trước khi chủ dự án duyệt. Test tái hiện giữ nguyên kỳ vọng nghiệp vụ và tiếp tục chạy, không bỏ qua hoặc đổi kỳ vọng thành hành vi lỗi.

## JOURNEY-SHARED-LAB-RETURN — bước quay lại bị lặp

**Trạng thái: đã được chủ dự án duyệt sửa và đã triển khai.** Danh sách dưới đây giữ lại nguyên nhân/lịch sử để truy vết, không còn là yêu cầu đang chờ duyệt.

- Nguồn: `PatientJourneyCycleTimelineTest.sharedLabQueueAcrossSameRoomRecordsCreatesOneCycleAndPointsToLabStep`.
- Dữ liệu: hai bệnh án khám riêng cùng phòng; hai CLS dùng chung một QueueTicket Lab. Một CLS liên kết hóa đơn của bệnh án đầu, CLS còn lại là dữ liệu cũ không liên kết hóa đơn của bệnh án thứ hai.
- Đúng: hai bước khám riêng, một lượt Lab với cả hai dịch vụ, một bước quay lại nguồn. Không tạo thêm bước quay lại chỉ vì xét nghiệm đã được gom có bệnh án nguồn khác.
- Thực tế: sinh thêm `RETURN_EXAMINATION` sau bước khám thứ hai.
- Bằng chứng code: `PatientJourneyService.build` tạo `includedTestIds` mới bên trong vòng lặp từng bệnh án. Các CLS cùng QueueTicket được đánh dấu đã hiển thị ở bệnh án đầu nhưng mất dấu khi xử lý bệnh án thứ hai; nhánh `legacyTests` tạo lại bước quay lại.
- Test cũ còn gộp hai lần khám thành một; đã chỉnh riêng kỳ vọng để giữ hai bước khám đúng quy tắc hiện tại. Kỳ vọng không lặp bước quay lại vẫn giữ và vẫn phải thất bại cho tới khi lỗi được sửa.
- Hướng xử lý sau duyệt: theo dõi các yêu cầu đã đưa vào chu kỳ xuyên suốt việc dựng timeline, bảo toàn bước khám riêng và những chu kỳ thực sự mới; thêm ca không dùng chung QueueTicket để chống gộp nhầm.
- Không thay entity, dữ liệu, API hoặc điều phối thật trong đợt rà soát này.

### Cách sửa và kiểm tra

- Đưa `includedTestIds` ra phạm vi toàn VIS trong `build`; CLS đã gom vào lượt Lab của chu kỳ trước không bị nhánh fallback bệnh án sau dựng thêm bước quay lại.
- Không bỏ bước khám riêng, không bỏ hóa đơn hay chu kỳ chỉ định mới; chỉ chặn việc dùng lại CLS đã hiển thị để tạo chu kỳ fallback giả.
- Test tái hiện mở rộng bốn tổ hợp: cùng/khác lượt Lab và cùng/khác phòng khám. Lượt Lab độc lập vẫn giữ hai bước Lab và hai bước quay lại đúng phiếu nguồn; lượt dùng chung chỉ có một bước quay lại, CLS chưa xong vẫn `BLOCKED`.
- Nhóm test hành trình/hàng chờ đã chạy đạt; kiểm tra hai chu kỳ chỉ định thực sự vẫn giữ nguyên thứ tự và bước hiện tại.
- Không cần thay ERD, hợp đồng API hoặc hướng dẫn nghiệp vụ; nội dung này là ghi nhận sửa lỗi triển khai để khớp quy tắc đã có.
