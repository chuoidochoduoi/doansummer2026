$ErrorActionPreference = 'Stop'

$source = 'C:\Users\Administrator\Downloads\_Report5_IntegrationTest_Sample.xlsx'
$outputDir = Join-Path (Get-Location) 'outputs'
$output = Join-Path $outputDir 'Report5_IntegrationTest_Clinic_Sample.xlsx'
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
Copy-Item -LiteralPath $source -Destination $output -Force

function Set-Cell($sheet, $address, $value) {
    $sheet.Range($address).Value2 = $value
}

function Set-Formula($sheet, $address, $formula) {
    $sheet.Range($address).Formula = $formula
}

function Copy-RowStyle($sheet, $sourceRow, $targetRow, $lastColumn) {
    $sheet.Range("A$sourceRow`:$lastColumn$sourceRow").Copy()
    $sheet.Range("A$targetRow`:$lastColumn$targetRow").PasteSpecial(-4122)
    $sheet.Rows($targetRow).RowHeight = $sheet.Rows($sourceRow).RowHeight
}

function Prepare-ConditionSheet($sheet, $title, $rows) {
    $sheet.Name = $title
    $sheet.Range('A3:I31').UnMerge()
    $sheet.Range('A3:I31').ClearContents()
    for ($r = 3; $r -le 31; $r++) { Copy-RowStyle $sheet 10 $r 'I' }
    $sheet.Range('A1').Value2 = 'Test Condition Analysis'
    $sheet.Range('A2:I2').Value2 = @('Condition','Valid Partitions','Tag','Invalid Partitions','Tag','Valid Boundaries','Tag','Invalid Boundaries','Tag')
    $r = 3
    foreach ($item in $rows) {
        for ($c = 0; $c -lt 9; $c++) { $sheet.Cells.Item($r, $c + 1).Value2 = $item[$c] }
        $sheet.Range("A$r`:I$r").WrapText = $true
        $sheet.Range("A$r`:I$r").VerticalAlignment = -4160
        $sheet.Rows($r).RowHeight = 42
        $r++
    }
    $sheet.Range('A27').Value2 = '* Notes:'
    $sheet.Range('B27').Value2 = 'Các tag VP/IP/VB/IB được dùng để đối chiếu với Test Design.'
    $sheet.PageSetup.PrintArea = '$A$1:$I$27'
}

function Prepare-DesignSheet($sheet, $title, $cases) {
    $sheet.Name = $title
    $sheet.Range('A3:D21').ClearContents()
    for ($r = 3; $r -le 21; $r++) { Copy-RowStyle $sheet 5 $r 'D' }
    $sheet.Range('A1').Value2 = 'Test case design'
    $sheet.Range('A2:D2').Value2 = @('Test-case No','Description','Expected result','TAG')
    $r = 3
    foreach ($case in $cases) {
        $sheet.Cells.Item($r,1).Value2 = $case[0]
        $sheet.Cells.Item($r,2).Value2 = $case[1]
        $sheet.Cells.Item($r,3).Value2 = $case[2]
        $sheet.Cells.Item($r,4).Value2 = $case[3]
        $sheet.Range("A$r`:D$r").WrapText = $true
        $sheet.Range("A$r`:D$r").VerticalAlignment = -4160
        $sheet.Rows($r).RowHeight = 82
        $r++
    }
    $sheet.PageSetup.PrintArea = '$A$1:$D$21'
}

function Prepare-FeatureSheet($sheet, $name, $featureTitle, $requirement, $groups, $cases) {
    $sheet.Name = $name
    $sheet.Range('A11:O40').ClearContents()
    for ($r = 11; $r -le 40; $r++) { Copy-RowStyle $sheet 19 $r 'O' }
    $sheet.Range('A2').Value2 = 'Feature'
    $sheet.Range('B2').Value2 = $featureTitle
    $sheet.Range('A3').Value2 = 'Test requirement'
    $sheet.Range('B3').Value2 = $requirement
    $sheet.Range('A4').Value2 = 'Number of TCs'
    Set-Formula $sheet 'B4' '=COUNTIF(A12:A1000,"IT*")'
    $sheet.Range('A5:E8').ClearContents()
    $sheet.Range('A5:E8').Value2 = @(
        @('Testing Round','Passed','Failed','Pending','N/A'),
        @('Round 1',$null,$null,$null,$null),
        @('Round 2',$null,$null,$null,$null),
        @('Round 3',$null,$null,$null,$null)
    )
    foreach ($row in 6..8) {
        Set-Formula $sheet "B$row" "=COUNTIF(`$F`$12:`$F`$1000,B`$5)"
        Set-Formula $sheet "C$row" "=COUNTIF(`$F`$12:`$F`$1000,C`$5)"
        Set-Formula $sheet "D$row" "=COUNTIF(`$F`$12:`$F`$1000,D`$5)"
        Set-Formula $sheet "E$row" "=COUNTIF(`$F`$12:`$F`$1000,E`$5)"
    }
    $sheet.Range('A10:O10').Value2 = @('Test Case ID','Test Case Description','Test Case Procedure','Expected Results','Pre-conditions','Round 1','Test date','Tester','Round 2','Test date','Tester','Round 3','Test date','Tester','Note')
    $row = 11
    $caseIndex = 0
    foreach ($group in $groups) {
        Copy-RowStyle $sheet 11 $row 'O'
        $sheet.Cells.Item($row,1).Value2 = $group[0]
        $sheet.Range("A$row`:O$row").Interior.Color = 13434828
        $row++
        for ($i = 0; $i -lt $group[1]; $i++) {
            $case = $cases[$caseIndex]
            Copy-RowStyle $sheet 12 $row 'O'
            $sheet.Cells.Item($row,1).Value2 = $case[0]
            $sheet.Cells.Item($row,2).Value2 = $case[1]
            $sheet.Cells.Item($row,3).Value2 = $case[2]
            $sheet.Cells.Item($row,4).Value2 = $case[3]
            $sheet.Cells.Item($row,5).Value2 = $case[4]
            $sheet.Cells.Item($row,6).Value2 = 'Pending'
            $sheet.Cells.Item($row,9).Value2 = 'Pending'
            $sheet.Cells.Item($row,12).Value2 = 'Pending'
            $sheet.Cells.Item($row,15).Value2 = $case[5]
            $sheet.Range("A$row`:O$row").WrapText = $true
            $sheet.Range("A$row`:O$row").VerticalAlignment = -4160
            $sheet.Rows($row).RowHeight = 105
            $row++; $caseIndex++
        }
    }
    $sheet.Range("A$row`:O40").ClearContents()
    $sheet.Application.ActiveWindow.SplitRow = 10
    $sheet.Application.ActiveWindow.FreezePanes = $true
    $sheet.PageSetup.PrintArea = "`$A`$1:`$O`$$($row-1)"
}

$excel = New-Object -ComObject Excel.Application
$excel.Visible = $false
$excel.DisplayAlerts = $false
try {
    $wb = $excel.Workbooks.Open($output)

    $cover = $wb.Worksheets.Item('Cover')
    Set-Cell $cover 'B4' 'Clinic Management System'
    Set-Cell $cover 'B5' 'CMS'
    Set-Formula $cover 'B6' '=B5&"_IntegrationTest_"&"v1.0"'
    Set-Cell $cover 'F5' '2026-08-12'
    Set-Cell $cover 'F6' 'v1.0'
    Set-Cell $cover 'A11' '2026-08-12'
    Set-Cell $cover 'B11' 'v1.0'
    Set-Cell $cover 'C11' 'Initial sample'
    Set-Cell $cover 'D11' 'A'
    Set-Cell $cover 'E11' 'Tạo mẫu Integration Test cho 3 luồng nghiệp vụ.'
    Set-Cell $cover 'F11' 'Source code repository'

    $condition1 = $wb.Worksheets.Item('ConditionAnalysis-UpdateProfile')
    $design1 = $wb.Worksheets.Item('TestDesign-UpdateProfile')
    $feature1 = $wb.Worksheets.Item('Feature 1')
    $feature2 = $wb.Worksheets.Item('Feature 2')

    $condition1.Copy($null, $condition1)
    $condition2 = $wb.ActiveSheet
    $condition1.Copy($null, $condition2)
    $condition3 = $wb.ActiveSheet
    $design1.Copy($null, $design1)
    $design2 = $wb.ActiveSheet
    $design1.Copy($null, $design2)
    $design3 = $wb.ActiveSheet
    $feature2.Copy($null, $feature2)
    $feature3 = $wb.ActiveSheet

    $ca1 = @(
        @('identifier','Email hợp lệ hoặc SĐT VN bắt đầu 0/+84','VP1','Rỗng hoặc sai định dạng','IP1','Email/SĐT ngắn nhất hợp lệ','VB1','Thiếu 1 chữ số / email thiếu domain','IB1'),
        @('OTP','Chuỗi đúng 6 chữ số, còn hạn','VP2','Sai mã hoặc đã hết hạn','IP2','6 chữ số','VB2','5 hoặc 7 chữ số','IB2'),
        @('OTP verified','Đã verify trong 15 phút','VP3','Chưa verify hoặc quá 15 phút','IP3','Phút thứ 15 còn hợp lệ theo TTL','VB3','Sau TTL 15 phút','IB3'),
        @('password','8-64 ký tự','VP4','Rỗng, <8 hoặc >64','IP4','8 và 64 ký tự','VB4','7 và 65 ký tự','IB4'),
        @('fullName','Không rỗng','VP5','Null/rỗng/chỉ khoảng trắng','IP5','1 ký tự không trắng','VB5','0 ký tự','IB5'),
        @('dob','Không ở tương lai, tuổi <=150','VP6','Tương lai hoặc tuổi >150','IP6','Hôm nay hoặc đúng 150 tuổi','VB6','Ngày mai hoặc 151 tuổi','IB6'),
        @('identifier uniqueness','Chưa liên kết account hoặc profile guest chưa có account','VP7','Đã liên kết account khác','IP7','Không áp dụng','VB7','Không áp dụng','IB7'),
        @('OTP send cooldown','Gửi lại sau cooldown, chưa vượt giới hạn giờ','VP8','Gửi trong cooldown hoặc vượt max/hour','IP8','Ngay sau khi cooldown hết','VB8','Trước khi cooldown hết 1 giây','IB8')
    )
    $td1 = @(
        @(1,'Đăng ký bằng số điện thoại sau khi gửi và xác thực OTP hợp lệ. Password 8 ký tự, ngày sinh hợp lệ.','200 OK; tạo Account CUSTOMER và Profile; OTP verified bị consume; trả access/refresh token.','VP1,VP2,VP3,VB4,VP5,VP6,VP7'),
        @(2,'Xác thực OTP chỉ có 5 chữ số.','400 Bad Request do OTP không đúng định dạng 6 chữ số; không đánh dấu verified.','IB2'),
        @(3,'Gọi đăng ký khi identifier chưa được xác thực OTP.','400 Bad Request; không tạo Account/Profile.','IP3'),
        @(4,'Đăng ký với password 7 ký tự.','400 Bad Request tại validation; không ghi dữ liệu.','IB4'),
        @(5,'Đăng ký lại bằng số điện thoại đã liên kết với account khác.','400 Bad Request; không tạo account trùng, giữ nguyên profile hiện có.','IP7'),
        @(6,'Đăng ký với ngày sinh ở ngày mai.','400 Bad Request; thông báo ngày sinh không thể ở tương lai.','IB6')
    )
    Prepare-ConditionSheet $condition1 'CA-IT01-Register' $ca1
    Prepare-DesignSheet $design1 'TD-IT01-Register' $td1

    $ca2 = @(
        @('customerId','Account CUSTOMER/STAFF tồn tại và có Profile','VP1','UUID không tồn tại hoặc không có Profile','IP1','Không áp dụng','VB1','Null','IB1'),
        @('scheduledAt','Hiện tại hoặc tương lai','VP2','Thời gian quá khứ','IP2','Thời điểm hiện tại','VB2','Trước hiện tại','IB2'),
        @('appointment conflict','Không có lịch PENDING trong khoảng xung đột','VP3','Có lịch PENDING trong ±30 phút','IP3','Ngoài khoảng 30 phút','VB3','Trong/đúng khoảng xung đột','IB3'),
        @('serviceIds','Dịch vụ tồn tại và phù hợp tuổi/giới tính','VP4','Dịch vụ không tồn tại hoặc không đủ điều kiện','IP4','Tuổi đúng min/max dịch vụ','VB4','Tuổi min-1/max+1','IB4'),
        @('appointment status khi check-in','PENDING','VP5','CHECKED_IN/CANCELLED/RESCHEDULED','IP5','Không áp dụng','VB5','Không áp dụng','IB5'),
        @('check-in date','scheduledAt đúng ngày hôm nay','VP6','Ngày khác hôm nay','IP6','00:00 và 23:59 hôm nay','VB6','Ngày trước/sau','IB6'),
        @('issuedById','Nhân viên thực hiện tồn tại','VP7','Null hoặc nhân viên không tồn tại','IP7','Không áp dụng','VB7','Null','IB7'),
        @('active visit','Không có Visit CHECKED_IN/IN_PROGRESS','VP8','Đã có Visit đang hoạt động','IP8','Không áp dụng','VB8','Không áp dụng','IB8')
    )
    $td2 = @(
        @(1,'Tạo lịch cho customer hợp lệ, dịch vụ tồn tại, thời gian tương lai và không xung đột.','201 Created; Appointment PENDING; liên kết Profile/Service; gửi notification cho lễ tân.','VP1,VP2,VP3,VP4'),
        @(2,'Tạo lịch với scheduledAt ở quá khứ.','400 Bad Request tại @FutureOrPresent; không tạo Appointment.','IP2,IB2'),
        @(3,'Tạo lịch thứ hai trong khoảng ±30 phút của lịch PENDING hiện có.','400 Bad Request; chỉ giữ lịch ban đầu.','IP3,IB3'),
        @(4,'Check-in lịch PENDING đúng ngày, nhân viên và dịch vụ hợp lệ.','200 OK; Appointment -> CHECKED_IN; tạo CustomerVisit CHECKED_IN và Invoice PENDING. QueueTicket chỉ sinh sau thanh toán.','VP5,VP6,VP7,VP8'),
        @(5,'Check-in lại Appointment đã CHECKED_IN.','409 Conflict; không tạo thêm Visit/Invoice.','IP5'),
        @(6,'Check-in khi bệnh nhân đang có Visit CHECKED_IN hoặc IN_PROGRESS.','409 Conflict; giữ nguyên Appointment và Visit đang hoạt động.','IP8')
    )
    Prepare-ConditionSheet $condition2 'CA-IT04-Appointment' $ca2
    Prepare-DesignSheet $design2 'TD-IT04-Appointment' $td2

    $ca3 = @(
        @('medicalRecordId','MedicalRecord tồn tại và gắn Visit','VP1','Không tồn tại hoặc không có Visit','IP1','Không áp dụng','VB1','Null','IB1'),
        @('serviceId/serviceIds','Dịch vụ tồn tại, có khoa/capability phù hợp','VP2','Không tồn tại hoặc không có khoa hoạt động','IP2','Danh sách có ít nhất 1 phần tử','VB2','Danh sách rỗng','IB2'),
        @('duplicate request','Chưa có chỉ định cùng service trong Visit','VP3','Đã tồn tại request chưa CANCELLED','IP3','Không áp dụng','VB3','Không áp dụng','IB3'),
        @('TestRequest status','PENDING/IN_PROGRESS trước hoàn tất','VP4','COMPLETED hoặc CANCELLED','IP4','Không áp dụng','VB4','Không áp dụng','IB4'),
        @('execution queue','IN_PROGRESS hoặc DONE','VP5','Null/WAITING/BLOCKED','IP5','Không áp dụng','VB5','Không áp dụng','IB5'),
        @('result PDF','URL kết thúc .pdf; file upload hợp lệ <=10MB','VP6','Rỗng, sai MIME/chữ ký hoặc >10MB','IP6','10MB','VB6','10MB + 1 byte','IB6'),
        @('conclusion','Không rỗng','VP7','Null/rỗng','IP7','1 ký tự','VB7','0 ký tự','IB7'),
        @('verifiedBy','Bác sĩ trưởng/phụ trách khoa thực hiện','VP8','Không phải bác sĩ hoặc không phải head doctor','IP8','Không áp dụng','VB8','Null','IB8'),
        @('sampleStatus','Không REJECTED/RECOLLECT khi complete','VP9','REJECTED hoặc RECOLLECT','IP9','Không áp dụng','VB9','Không áp dụng','IB9')
    )
    $td3 = @(
        @(1,'Bác sĩ tạo chỉ định CLS cho MedicalRecord, dịch vụ và khoa thực hiện hợp lệ.','201 Created; TestRequest PENDING; tạo/dùng chung QueueTicket theo khoa; liên kết MedicalRecord/Service/InvoiceItem.','VP1,VP2,VP3'),
        @(2,'Tạo lại cùng dịch vụ trong cùng lượt khám khi request cũ chưa hủy.','409 Conflict; không tạo TestRequest hoặc QueueTicket trùng.','IP3'),
        @(3,'Nhân viên lưu kết quả nháp khi request đang PENDING.','Tạo/cập nhật TestResult; TestRequest chuyển IN_PROGRESS; chưa đánh dấu hoàn tất.','VP4'),
        @(4,'Hoàn tất kết quả khi queue IN_PROGRESS, có PDF, conclusion và head doctor xác nhận.','200 OK; TestRequest COMPLETED; lưu verifiedAt/verifiedBy; cập nhật lab queue DONE và queue khám TEST_DONE khi mọi chỉ định đã xong.','VP4,VP5,VP6,VP7,VP8,VP9'),
        @(5,'Hoàn tất kết quả nhưng imageUrl rỗng hoặc không kết thúc .pdf.','400 Bad Request; TestRequest chưa COMPLETED.','IP6'),
        @(6,'Hoàn tất kết quả bằng bác sĩ không phải người phụ trách khoa.','400 Bad Request; không ký xác nhận và không chuyển trạng thái hoàn tất.','IP8')
    )
    Prepare-ConditionSheet $condition3 'CA-IT09-LabResult' $ca3
    Prepare-DesignSheet $design3 'TD-IT09-LabResult' $td3

    $f1 = @(
        @('IT01-001','Đăng ký thành công bằng SĐT đã verify OTP','1. POST /api/auth/send-otp với 0912345678.`n2. POST /api/auth/verify-register-otp với OTP 6 số.`n3. POST /api/auth/register: password=Abc12345, fullName=Nguyễn Văn A, dob=1995-05-20.','200 OK; trả accessToken và refreshToken.`nDB: tạo 1 Account role CUSTOMER và 1 Profile liên kết; OTP verified bị xóa.','Redis hoạt động; SĐT chưa liên kết account; app.otp.expose-code=true ở môi trường test.','Tags: VP1,VP2,VP3,VB4,VP5,VP6,VP7'),
        @('IT01-002','Từ chối OTP chỉ có 5 chữ số','POST /api/auth/verify-register-otp với otp=12345.','400 Bad Request; không tạo key otp_verified.','Đã gửi OTP cho identifier.','Tags: IB2'),
        @('IT01-003','Từ chối đăng ký khi chưa verify OTP','POST /api/auth/register trực tiếp với dữ liệu hợp lệ.','400 Bad Request; không tạo Account/Profile.','Identifier chưa có key otp_verified.','Tags: IP3'),
        @('IT01-004','Từ chối mật khẩu 7 ký tự','Verify OTP, sau đó POST /api/auth/register với password=Abc1234.','400 Bad Request do @Size(min=8); OTP/Profile/Account không bị tạo sai.','OTP đã verify.','Tags: IB4'),
        @('IT01-005','Từ chối SĐT đã liên kết tài khoản khác','Verify OTP cho 0912345678; đăng ký lại cùng identifier.','400 Bad Request; chỉ tồn tại 1 Account liên kết Profile.','SĐT đã thuộc một Profile có Account.','Tags: IP7'),
        @('IT01-006','Từ chối ngày sinh ở tương lai','Verify OTP; POST /api/auth/register với dob=ngày mai.','400 Bad Request; thông báo ngày sinh không thể ở tương lai; không tạo dữ liệu.','Identifier chưa đăng ký.','Tags: IB6')
    )
    Prepare-FeatureSheet $feature1 'IT01-Register' 'IT01 - Đăng ký và xác thực OTP' 'Kiểm tra tích hợp Redis OTP -> Account -> Profile -> JWT.' @(@('OTP và đăng ký hợp lệ',1),@('Validation và trạng thái OTP',3),@('Ràng buộc dữ liệu tài khoản',2)) $f1

    $f2 = @(
        @('IT04-001','Tạo lịch khám hợp lệ','Đăng nhập CUSTOMER; POST /api/v1/appointments với customerId hợp lệ, scheduledAt ngày mai, serviceIds=[SERVICE_GENERAL].','201 Created; Appointment PENDING; liên kết Profile/Service; lễ tân nhận notification.','Customer có Profile; dịch vụ active và phù hợp tuổi/giới tính.','Tags: VP1,VP2,VP3,VP4'),
        @('IT04-002','Từ chối lịch ở quá khứ','POST /api/v1/appointments với scheduledAt=hiện tại-1 phút.','400 Bad Request; không tạo Appointment.','Customer hợp lệ.','Tags: IP2,IB2'),
        @('IT04-003','Từ chối lịch xung đột trong 30 phút','Tạo lịch thứ hai cách lịch PENDING hiện có 20 phút.','400 Bad Request; DB chỉ giữ lịch ban đầu.','Có Appointment PENDING của cùng Profile.','Tags: IP3,IB3'),
        @('IT04-004','Check-in lịch hẹn thành công','RECEPTIONIST POST /api/v1/appointments/{id}/check-in với appointmentId, issuedById và serviceIds hợp lệ.','200 OK; Appointment CHECKED_IN; tạo một CustomerVisit CHECKED_IN và Invoice PENDING có InvoiceItem. Chưa tạo QueueTicket trước thanh toán.','Appointment PENDING đúng ngày; bệnh nhân không có Visit đang hoạt động.','Tags: VP5,VP6,VP7,VP8'),
        @('IT04-005','Chặn check-in trùng','Gọi lại API check-in cho cùng appointmentId.','409 Conflict; không tạo thêm CustomerVisit hoặc Invoice.','Appointment đã CHECKED_IN.','Tags: IP5'),
        @('IT04-006','Chặn check-in khi còn lượt khám hoạt động','Check-in lịch mới khi Profile đã có Visit CHECKED_IN.','409 Conflict; giữ Visit hiện tại; lịch mới chưa chuyển CHECKED_IN.','Profile có Visit CHECKED_IN/IN_PROGRESS.','Tags: IP8')
    )
    Prepare-FeatureSheet $feature2 'IT04-Appointment' 'IT04 - Đặt lịch và check-in' 'Kiểm tra Appointment -> CustomerVisit -> Invoice; QueueTicket sinh sau thanh toán.' @(@('Đặt lịch khám',3),@('Check-in thành công',1),@('Chặn check-in không hợp lệ',2)) $f2

    $f3 = @(
        @('IT09-001','Tạo chỉ định cận lâm sàng hợp lệ','DOCTOR POST /api/v1/test-requests với medicalRecordId, serviceId, requestedById và invoiceItemId hợp lệ.','201 Created; TestRequest PENDING; chọn khoa phù hợp; tạo/dùng chung QueueTicket theo Visit+khoa.','MedicalRecord IN_PROGRESS; dịch vụ có khoa/capability hoạt động.','Tags: VP1,VP2,VP3'),
        @('IT09-002','Chặn chỉ định trùng dịch vụ trong cùng Visit','POST chỉ định cùng serviceId lần hai khi request cũ chưa CANCELLED.','409 Conflict; không tạo request/ticket trùng.','Đã có TestRequest cùng service trong Visit.','Tags: IP3'),
        @('IT09-003','Lưu kết quả nháp','POST /api/v1/test-requests/{id}/result với conclusion nháp, imageUrl PDF và performedById.','Tạo TestResult; TestRequest PENDING -> IN_PROGRESS; chưa có verifiedBy/verifiedAt.','TestRequest PENDING; nhân viên thực hiện tồn tại.','Tags: VP4'),
        @('IT09-004','Hoàn tất kết quả và trả bệnh nhân về luồng khám','POST /api/v1/test-requests/{id}/result/complete với PDF, conclusion, sample hợp lệ; token của head doctor khoa thực hiện.','200 OK; TestRequest COMPLETED; TestResult có verifiedBy/At; lab Queue DONE; queue khám TEST_DONE nếu mọi chỉ định hoàn tất.','Execution Queue IN_PROGRESS; head doctor đúng khoa; mọi file/dữ liệu hợp lệ.','Tags: VP4,VP5,VP6,VP7,VP8,VP9'),
        @('IT09-005','Từ chối hoàn tất khi thiếu PDF','Gọi complete với imageUrl rỗng hoặc file .jpg.','400 Bad Request; TestRequest không chuyển COMPLETED.','Queue IN_PROGRESS; signer hợp lệ.','Tags: IP6'),
        @('IT09-006','Từ chối bác sĩ không phụ trách khoa ký kết quả','Gọi complete bằng token bác sĩ khác khoa/không phải head doctor.','400 Bad Request; không ghi verifiedBy và không hoàn tất request.','Kết quả, PDF và queue hợp lệ.','Tags: IP8')
    )
    Prepare-FeatureSheet $feature3 'IT09-LabResult' 'IT09 - Chỉ định và trả kết quả xét nghiệm' 'Kiểm tra MedicalRecord -> TestRequest -> TestResult -> QueueTicket/PatientJourney.' @(@('Tạo chỉ định',2),@('Nhập và hoàn tất kết quả',2),@('Validation khi hoàn tất',2)) $f3

    $list = $wb.Worksheets.Item('Test Cases')
    Set-Cell $list 'D3' 'Clinic Management System'
    Set-Cell $list 'D4' 'CMS'
    Set-Cell $list 'D5' "1. Spring Boot API`n2. PostgreSQL`n3. Redis`n4. Mock SMS/Email`n5. Postman hoặc Swagger"
    $list.Range('B9:F21').ClearContents()
    $items = @(
        @(1,'IT01 - Đăng ký và OTP','IT01-Register','Account, Redis OTP, Profile, JWT','Redis/PostgreSQL hoạt động'),
        @(2,'IT04 - Đặt lịch và check-in','IT04-Appointment','Appointment, Visit, Invoice','Có customer, service và receptionist'),
        @(3,'IT09 - Xét nghiệm','IT09-LabResult','MedicalRecord, TestRequest, Result, Queue','Có visit, record, service và head doctor')
    )
    $r=9
    foreach($item in $items){ for($c=0;$c -lt 5;$c++){ $list.Cells.Item($r,$c+2).Value2=$item[$c] }; $r++ }
    foreach($r in 9..11){
        $target=$items[$r-9][2]
        $list.Hyperlinks.Add($list.Cells.Item($r,4),'',"'$target'!A1",'Mở sheet test case',$target) | Out-Null
    }

    $stats = $wb.Worksheets.Item('Test Statistics')
    Set-Cell $stats 'C3' 'Clinic Management System'
    Set-Cell $stats 'C4' 'CMS'
    Set-Formula $stats 'C5' '=C4&"_Integration Test Report_"&"v1.0"'
    Set-Cell $stats 'C6' 'Sample review: IT01, IT04 và IT09'
    Set-Cell $stats 'H5' '2026-08-12'
    $modules=@('IT01-Register','IT04-Appointment','IT09-LabResult')
    foreach($i in 0..2){
        $r=11+$i; Set-Cell $stats "B$r" ($i+1)
        Set-Formula $stats "C$r" "='$($modules[$i])'!B2"
        Set-Formula $stats "D$r" "='$($modules[$i])'!B6"
        Set-Formula $stats "E$r" "='$($modules[$i])'!C6"
        Set-Formula $stats "F$r" "='$($modules[$i])'!D6"
        Set-Formula $stats "G$r" "='$($modules[$i])'!E6"
        Set-Formula $stats "H$r" "='$($modules[$i])'!B4"
    }
    foreach($col in 'D','E','F','G','H'){ Set-Formula $stats "$col`14" "=SUM($col`11:$col`13)" }
    Set-Formula $stats 'E16' '=IFERROR((D14+E14)*100/(H14-G14),0)'
    Set-Formula $stats 'E17' '=IFERROR(D14*100/(H14-G14),0)'

    $messages = $wb.Worksheets.Item('MessageList')
    $messages.Range('A2:C88').ClearContents()
    $messageRows = @(
        @('AUTH-OTP-INVALID','OTP không hợp lệ hoặc đã hết hạn','IT01'),
        @('AUTH-OTP-REQUIRED','Vui lòng xác thực OTP trước khi đăng ký','IT01'),
        @('APPT-CONFLICT','Đã có lịch hẹn trùng hoặc quá gần thời gian này','IT04'),
        @('APPT-CHECKED-IN','Lịch hẹn đã được nhân viên khác check-in','IT04'),
        @('VISIT-ACTIVE','Bệnh nhân đang có lượt khám chưa hoàn thành','IT04'),
        @('TEST-DUPLICATE','Dịch vụ đã được chỉ định trong lượt khám hiện tại','IT09'),
        @('RESULT-PDF-REQUIRED','Vui lòng tải phiếu kết quả định dạng PDF','IT09'),
        @('RESULT-VERIFIER','Chỉ bác sĩ phụ trách phòng được ký kết quả','IT09')
    )
    $r=2; foreach($m in $messageRows){for($c=0;$c -lt 3;$c++){$messages.Cells.Item($r,$c+1).Value2=$m[$c]};$r++}

    $wb.CalculateFull()
    $wb.Save()
    $wb.Close($true)
} finally {
    $excel.Quit()
    [System.Runtime.InteropServices.Marshal]::ReleaseComObject($excel) | Out-Null
}

Get-Item -LiteralPath $output | Select-Object FullName,Length,LastWriteTime
