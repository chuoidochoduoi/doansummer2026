# Bilingual Use Case Traceability

Scope: current source, including uncommitted files. Related test files below are selected by the exercised type named in the test filename; they are navigation evidence, not a claim that every use-case branch is covered or that tests were run during this documentation task. Endpoint-specific authority and delivery exceptions are in endpoint-inventory.md.

| ID | English | Tiếng Việt | Actors | Delivery |
| --- | --- | --- | --- | --- |
| G-01 | View Public Clinic Information | Xem thông tin phòng khám | Guest | UI + backend |
| G-02 | Register Account | Đăng ký tài khoản | Guest | UI + backend |
| G-03 | Create Guest Appointment | Đặt lịch không cần tài khoản | Guest | UI + backend |
| G-05 | View Public Department Schedule | Xem lịch hoạt động các khoa | Guest | UI + backend |
| G-06 | Look Up Guest Journey | Tra cứu hành trình khách vãng lai | Guest | UI + backend |
| G-07 | Request Same-day Return as Guest | Báo quay lại trong ngày cho Guest | Guest | UI + backend |
| G-08 | Use Guest Support Chat | Chat hỗ trợ không cần tài khoản | Guest | UI + backend |
| G-09 | Submit Contact Enquiry | Gửi yêu cầu liên hệ | Guest | UI + backend |
| RU-01 | Log In and Log Out | Đăng nhập và đăng xuất | Registered User | UI + backend |
| RU-02 | Verify OTP | Xác thực OTP | Registered User | UI + backend |
| RU-03 | Recover Password | Khôi phục mật khẩu | Registered User | UI + backend |
| RU-04 | Change Password | Đổi mật khẩu | Registered User | UI + backend |
| RU-05 | View and Update Profile | Xem và sửa hồ sơ cá nhân | Registered User | UI + backend |
| RU-06 | Read In-app Notifications | Xem thông báo trong ứng dụng | Registered User | UI + backend |
| RU-07 | View Own Duty Schedule | Xem lịch trực cá nhân | Receptionist, Cashier, Nurse, Doctor, Clinic Manager | UI + backend |
| RU-08 | Set Local Interface Preferences | Đặt tùy chọn giao diện | Receptionist, Cashier, Nurse, Doctor, Clinic Manager, Administrator | Frontend only by design |
| CU-01 | Create Appointment | Đặt lịch khám | Customer | UI + backend |
| CU-02 | View Appointment List and Details | Xem lịch hẹn | Customer | UI + backend |
| CU-03 | Cancel Appointment | Hủy lịch hẹn | Customer | UI + backend |
| CU-04 | View Follow-up Information | Xem yêu cầu và lịch tái khám | Customer | UI + backend |
| CU-06 | Track Examination Journey | Theo dõi hành trình khám | Customer | UI + backend |
| CU-07 | View Completed Medical Records | Xem bệnh án đã hoàn thành | Customer | UI + backend |
| CU-08 | View Prescriptions and Published Diagnostic Results | Xem đơn thuốc và kết quả CLS | Customer | UI + backend |
| CU-09 | View Invoice and Payment History | Xem lịch sử thanh toán và phiếu thu | Customer | UI + backend |
| CU-11 | Send Message to Receptionist | Nhắn tin với lễ tân | Customer | UI + backend |
| CU-12 | View Conversation Messages | Xem nội dung hội thoại | Customer | UI + backend |
| CU-13 | Submit and View Medical-record Feedback | Gửi và xem đánh giá bệnh án | Customer | UI + backend |
| CU-14 | Manage Family Profiles | Quản lý hồ sơ gia đình | Customer | UI + backend |
| CU-15 | Create Family Group Booking | Đặt lịch nhóm gia đình | Customer | UI + backend |
| CU-16 | Change Appointment Details | Thay đổi lịch hẹn | Customer | UI + backend |
| CU-17 | Request Same-day Return | Báo đã quay lại trong ngày | Customer | UI + backend |
| CU-18 | Register CareS Prepaid Card | Đăng ký thẻ trả trước CareS | Customer | UI + backend |
| CU-19 | View CareS Card and Balance History | Xem thẻ CareS và lịch sử số dư | Customer | UI + backend |
| CU-20 | Print an Individual Medical Record | In bệnh án theo dịch vụ | Customer, Doctor | UI + backend |
| CU-21 | Print Customer Receipt | In phiếu thu của Customer | Customer | UI + backend |
| CU-22 | Pay with Own CareS Card through API | Thanh toán thẻ cá nhân qua API | Customer | Backend only |
| RC-01 | Search Patient | Tìm hồ sơ bệnh nhân | Receptionist, Clinic Manager | UI + backend |
| RC-02 | Create or Complete Patient Profile | Tạo và cập nhật hồ sơ bệnh nhân | Receptionist, Clinic Manager | UI + backend |
| RC-03 | Check In Scheduled Patient | Tiếp nhận khách có lịch hẹn | Receptionist, Clinic Manager | UI + backend |
| RC-04 | Create Walk-in Visit | Tạo phiếu khám tại quầy | Receptionist, Clinic Manager | UI + backend |
| RC-05 | View Visit and Ticket Information | Xem phiếu khám và lượt khám | Receptionist, Clinic Manager | UI + backend |
| RC-06 | Update Permitted Appointment Information | Sửa lịch hẹn tại lễ tân | Receptionist, Clinic Manager | UI + backend |
| RC-07 | Review and Coordinate Patient Journey | Theo dõi và điều phối hành trình | Receptionist, Clinic Manager | UI + backend |
| RC-08 | Manage Support Conversations | Quản lý hội thoại hỗ trợ | Receptionist, Clinic Manager | UI + backend |
| RC-10 | Confirm Patient Return | Xác nhận khách đã quay lại | Receptionist, Clinic Manager | UI + backend |
| RC-11 | Schedule Requested Follow-up | Đặt lịch từ yêu cầu tái khám | Receptionist, Clinic Manager | UI + backend |
| RC-12 | Review Patient Visit History | Xem lịch sử bệnh nhân tại lễ tân | Receptionist, Clinic Manager | UI + backend |
| RC-13 | Recover an Eligible Blocked Journey through API | Khôi phục bước bị kẹt qua API | Clinic Manager, Administrator | Backend only |
| RC-14 | Open Queue Display Screens | Mở màn hình gọi bệnh nhân | Receptionist, Clinic Manager | UI + backend |
| RC-15 | View Overall or Room Calling Display | Xem màn gọi tổng hoặc từng phòng | Receptionist, Clinic Manager, Doctor, Nurse, Administrator | UI + backend |
| CA-01 | View Pending Invoices | Xem hóa đơn chờ thanh toán | Cashier, Clinic Manager | UI + backend |
| CA-02 | View Invoice Details | Xem chi tiết hóa đơn | Cashier, Clinic Manager | UI + backend |
| CA-03 | Confirm Cash Payment | Xác nhận thanh toán tiền mặt | Cashier, Clinic Manager | UI + backend |
| CA-04 | Monitor Online Payment | Theo dõi thanh toán PayOS | Cashier, Clinic Manager | UI + backend |
| CA-05 | Review Invoice Payment History | Tra cứu lịch sử hóa đơn đã thu | Cashier, Clinic Manager | UI + backend |
| CA-06 | Print Receipt | In phiếu thu tại quầy | Cashier, Clinic Manager | UI + backend |
| CA-07 | Verify and Apply Health Insurance | Kiểm tra và áp dụng BHYT | Cashier, Clinic Manager | UI + backend |
| CA-08 | Accept CareS Card Payment | Thu tiền bằng thẻ CareS | Cashier, Clinic Manager | UI + backend |
| CA-09 | Top Up CareS Card | Nạp tiền thẻ CareS | Cashier, Clinic Manager | UI + backend |
| CA-10 | Review and Print Top-up History | Xem và in lịch sử nạp thẻ | Cashier, Clinic Manager | UI + backend |
| CA-11 | Cancel an Eligible Invoice | Hủy hóa đơn đủ điều kiện | Cashier, Clinic Manager | UI + backend |
| CA-12 | Create PayOS Payment Link | Tạo link thanh toán PayOS | Cashier, Clinic Manager | UI + backend |
| NU-01 | View Assigned Queue | Xem hàng chờ được phân công | Nurse | UI + backend |
| NU-02 | Call or Mark Patient Absent | Gọi hoặc đánh dấu vắng | Nurse | UI + backend |
| NU-03 | Receive and Prepare Patient | Tiếp nhận và chuẩn bị bệnh nhân | Nurse | UI + backend |
| NU-04 | Record Vital Signs | Ghi dấu hiệu sinh tồn | Nurse, Doctor | UI + backend |
| NU-05 | View Relevant Patient Information | Xem thông tin phục vụ chăm sóc | Nurse | UI + backend |
| NU-06 | Save Nursing Preparation Notes | Lưu ghi nhận hỗ trợ khám | Nurse | UI + backend |
| NU-07 | Restore Eligible Absent Patient at Room | Đưa khách vắng trở lại hàng chờ phòng | Nurse, Doctor | UI + backend |
| DR-01 | View Examination Queue | Xem hàng chờ khám bệnh | Doctor | UI + backend |
| DR-02 | Call and Receive Patient | Gọi và tiếp nhận bệnh nhân | Doctor | UI + backend |
| DR-03 | View Medical Records and History | Xem bệnh án và tiền sử | Doctor | UI + backend |
| DR-04 | Record Medical Examination | Ghi nhận khám lâm sàng | Doctor | UI + backend |
| DR-05 | Record Diagnosis and Treatment Plan | Ghi chẩn đoán và hướng điều trị | Doctor | UI + backend |
| DR-06 | Create Prescription | Kê đơn thuốc | Doctor | UI + backend |
| DR-07 | Request Diagnostic Services | Chỉ định cận lâm sàng | Doctor | UI + backend |
| DR-09 | Review Diagnostic Results | Xem kết quả cận lâm sàng | Doctor | UI + backend |
| DR-10 | Save Medical Record as Draft | Lưu nháp bệnh án | Doctor | UI + backend |
| DR-11 | Resume Examination after Diagnostic Services | Khám lại sau cận lâm sàng | Doctor | UI + backend |
| DR-12 | Complete Medical Record | Hoàn thành bệnh án | Doctor | UI + backend |
| DR-13 | Arrange Follow-up from Examination | Thiết lập tái khám từ màn khám | Doctor | UI + backend |
| DR-14 | View Same-day Clinical Information | Xem thông tin chuyên môn cùng ngày | Doctor | UI + backend |
| DR-15 | Verify Patient Allergies | Xác minh dị ứng | Doctor, Nurse | UI + backend |
| DR-16 | Continue Next Examination in the Same Room | Tiếp tục dịch vụ khám cùng phòng | Doctor | UI + backend |
| DR-17 | Finish Carried-over Clinical Work | Xử lý hồ sơ chuyên môn tồn đọng | Doctor | UI + backend |
| DS-01 | View Diagnostic Queue and Panels | Xem hàng chờ và phiếu CLS | Doctor, Nurse | UI + backend |
| DS-02 | Call and Receive Diagnostic Patient | Gọi và tiếp nhận tại phòng CLS | Doctor, Nurse | UI + backend |
| DS-03 | View Diagnostic Request | Xem chỉ định CLS | Doctor, Nurse | UI + backend |
| DS-04 | Record Specimen Information | Ghi thông tin mẫu bệnh phẩm | Doctor, Nurse | UI + backend |
| DS-05 | Perform Diagnostic Service | Thực hiện dịch vụ CLS | Doctor, Nurse | UI + backend |
| DS-06 | Enter Diagnostic Result | Nhập kết quả CLS | Doctor, Nurse | UI + backend |
| DS-07 | Attach Result Files | Đính kèm tệp kết quả | Doctor, Nurse | UI + backend |
| DS-08 | Save Diagnostic Draft | Lưu nháp kết quả CLS | Doctor, Nurse | UI + backend |
| DS-09 | Sign and Publish Diagnostic Result | Ký và công bố kết quả CLS | Doctor | UI + backend |
| DS-10 | Complete Diagnostic Request | Hoàn tất yêu cầu CLS | Doctor | UI + backend |
| DS-11 | Handle Diagnostic Absence and Return | Xử lý vắng và quay lại phòng CLS | Doctor, Nurse | UI + backend |
| DS-12 | Cancel an Eligible Diagnostic Request | Hủy yêu cầu CLS đủ điều kiện | Doctor | UI + backend |
| DS-13 | View Result Revision History | Xem lịch sử phiên bản kết quả | Doctor, Nurse | UI + backend |
| DS-14 | Amend a Signed Result through API | Đính chính kết quả qua API | Doctor | Backend only |
| CM-01 | View Operational Reports | Xem thống kê vận hành | Clinic Manager | UI + backend |
| CM-02 | Manage Staff Information | Quản lý thông tin nhân sự | Clinic Manager, Administrator | UI + backend |
| CM-03 | Manage Staff Schedules | Phân công lịch trực | Clinic Manager, Administrator | UI + backend |
| CM-04 | Manage Services and Prices | Quản lý dịch vụ và giá | Clinic Manager, Administrator | UI + backend |
| CM-05 | Manage Departments and Rooms | Quản lý phòng | Clinic Manager, Administrator | UI + backend |
| CM-06 | Manage Medical-record Feedback | Quản lý đánh giá bệnh án | Clinic Manager, Receptionist | UI + backend |
| CM-08 | Manage Public Announcements | Quản lý thông báo công khai | Clinic Manager, Administrator | UI + backend |
| CM-09 | Manage Diagnostic Form Templates | Quản lý biểu mẫu CLS | Clinic Manager | UI + backend |
| CM-10 | Print or Export Reports | In và xuất báo cáo | Clinic Manager | UI + backend |
| CM-11 | View Patient Directory | Xem danh sách bệnh nhân | Clinic Manager | UI + backend |
| CM-12 | Configure CareS Policy | Cấu hình chính sách thẻ CareS | Clinic Manager, Administrator | UI + backend |
| CM-13 | Review CareS Ledger and Reverse Eligible Payment | Xem sổ thẻ và hoàn tác đủ điều kiện | Clinic Manager, Administrator | UI + backend |
| AD-01 | Manage User Accounts | Quản lý tài khoản | Administrator, Clinic Manager | UI + backend |
| AD-02 | Assign Supported Staff Roles and Capabilities | Gán vai trò và năng lực nhân sự | Administrator, Clinic Manager | UI + backend |
| AD-03 | Manage Clinic Information | Cập nhật thông tin phòng khám | Administrator, Clinic Manager | UI + backend |
| AD-05 | Manage ICD-10 Catalog through API | Quản lý ICD qua API | Administrator | Backend only |
| AD-06 | Review Audit Information | Xem nhật ký hệ thống | Administrator, Clinic Manager | UI + backend |
| AD-07 | Manage Shift Versions | Quản lý phiên bản ca | Administrator, Clinic Manager | UI + backend |
| AD-09 | Manage Operating Exceptions | Quản lý lịch hoạt động ngoại lệ | Administrator, Clinic Manager | UI + backend |
| AD-10 | Check Service Coverage | Kiểm tra khả năng phục vụ theo ca | Administrator, Clinic Manager | UI + backend |
| AD-11 | View Fixed Technical Catalog | Xem danh mục kỹ thuật cố định | Administrator, Clinic Manager | UI + backend |
| AD-12 | Manage Schedule Templates through API | Quản lý mẫu lịch qua API | Administrator | Backend only |
| AD-13 | Administer Notification Records through API | Quản trị bản ghi thông báo qua API | Administrator | Backend only |
| EXT-01 | Process Online Payment | Xử lý thanh toán trực tuyến | PayOS | External integration |
| EXT-02 | Simulate Insurance Verification | Mô phỏng xác minh BHYT | Mock BHXH Service | External integration |
| EXT-03 | Deliver Verification and Contact Messages | Gửi thông điệp xác thực và liên hệ | Email Service, SMS Gateway | External integration |

## G-01 View Public Clinic Information

Vietnamese: Xem thông tin phòng khám. Actors: Guest. Delivery: UI + backend.

Browse clinic information, public announcements, services, doctors, contact details, terms and privacy information. Public doctor information does not disclose individual duty schedules.

**Registered routes:** `/`; `/about`; `/terms`; `/privacy`; `/services`; `/doctors`

**Frontend evidence:** [src/pages/public/PublicHomePage.jsx](../../../untitled/src/pages/public/PublicHomePage.jsx); [src/pages/public/PublicServicesPage.jsx](../../../untitled/src/pages/public/PublicServicesPage.jsx); [src/pages/public/PublicDoctorsPage.jsx](../../../untitled/src/pages/public/PublicDoctorsPage.jsx); [src/pages/public/AboutPage.jsx](../../../untitled/src/pages/public/AboutPage.jsx); [src/pages/public/TermsPage.jsx](../../../untitled/src/pages/public/TermsPage.jsx); [src/pages/public/PrivacyPage.jsx](../../../untitled/src/pages/public/PrivacyPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/public/clinic-information | [ClinicInformationController.publicInformation](../../src/main/java/org/example/doansummer2026/controller/ClinicInformationController.java) line 23 | [ClinicInformationService.get](../../src/main/java/org/example/doansummer2026/service/ClinicInformationService.java) |
| GET /api/public/announcements | [PublicAnnouncementController.visible](../../src/main/java/org/example/doansummer2026/controller/PublicAnnouncementController.java) line 24 | [PublicAnnouncementService.listVisible](../../src/main/java/org/example/doansummer2026/service/PublicAnnouncementService.java) |
| GET /api/v1/medical-services/available | [MedicalServiceController.listAvailable](../../src/main/java/org/example/doansummer2026/controller/MedicalServiceController.java) line 57 | [MedicalServiceService.listAvailable](../../src/main/java/org/example/doansummer2026/service/MedicalServiceService.java) |
| GET /api/v1/staff/public/doctors | [StaffController.getPublicDoctors](../../src/main/java/org/example/doansummer2026/controller/StaffController.java) line 54 | [StaffService.getPublicActiveDoctors](../../src/main/java/org/example/doansummer2026/service/StaffService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/StaffControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/StaffControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/ClinicInformationServiceTest.java](../../src/test/java/org/example/doansummer2026/service/ClinicInformationServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/MedicalServiceServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MedicalServiceServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/PublicAnnouncementServiceTest.java](../../src/test/java/org/example/doansummer2026/service/PublicAnnouncementServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/StaffServiceTest.java](../../src/test/java/org/example/doansummer2026/service/StaffServiceTest.java)

## G-02 Register Account

Vietnamese: Đăng ký tài khoản. Actors: Guest. Delivery: UI + backend.

Register a Customer account using the registration form and OTP verification. Staff accounts are created through authorized staff administration, not public registration.

**Registered routes:** `/register`

**Frontend evidence:** [src/pages/auth/RegisterPage.jsx](../../../untitled/src/pages/auth/RegisterPage.jsx); [src/hooks/useRegister.js](../../../untitled/src/hooks/useRegister.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/auth/register | [AuthController.register](../../src/main/java/org/example/doansummer2026/controller/AuthController.java) line 41 | [AuthServiceInterface.register](../../src/main/java/org/example/doansummer2026/service/interfaces/AuthServiceInterface.java) |
| POST /api/auth/send-register-otp | [AuthController.sendRegisterOtp](../../src/main/java/org/example/doansummer2026/controller/AuthController.java) line 61 | [AuthServiceInterface.ensureRegistrationIdentifierAvailable](../../src/main/java/org/example/doansummer2026/service/interfaces/AuthServiceInterface.java); [OtpService.sendOtp](../../src/main/java/org/example/doansummer2026/service/OtpService.java) |
| POST /api/auth/verify-register-otp | [AuthController.verifyRegisterOtp](../../src/main/java/org/example/doansummer2026/controller/AuthController.java) line 69 | [OtpService.markOtpAsVerified](../../src/main/java/org/example/doansummer2026/service/OtpService.java) |

**Additional classified endpoints:** `AuthController.registrationAvailability`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/OtpServiceTest.java](../../src/test/java/org/example/doansummer2026/service/OtpServiceTest.java)

## G-03 Create Guest Appointment

Vietnamese: Đặt lịch không cần tài khoản. Actors: Guest. Delivery: UI + backend.

Select publicly bookable services, an available date and shift, enter patient details and confirm a guest booking. The guest selector does not expose individually priced laboratory analytes.

**Registered routes:** `/appointment`

**Frontend evidence:** [src/pages/appointment/AppointmentPage.jsx](../../../untitled/src/pages/appointment/AppointmentPage.jsx); [src/hooks/useAppointment.js](../../../untitled/src/hooks/useAppointment.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/appointments/guest | [AppointmentController.createForGuest](../../src/main/java/org/example/doansummer2026/controller/AppointmentController.java) line 81 | [AppointmentService.createForGuest](../../src/main/java/org/example/doansummer2026/service/AppointmentService.java) |
| GET /api/v1/medical-services/available | [MedicalServiceController.listAvailable](../../src/main/java/org/example/doansummer2026/controller/MedicalServiceController.java) line 57 | [MedicalServiceService.listAvailable](../../src/main/java/org/example/doansummer2026/service/MedicalServiceService.java) |
| POST /api/v1/medical-services/resolve-selection | [MedicalServiceController.resolveSelection](../../src/main/java/org/example/doansummer2026/controller/MedicalServiceController.java) line 66 | [MedicalServiceSelectionPolicyService.resolveResponse](../../src/main/java/org/example/doansummer2026/service/MedicalServiceSelectionPolicyService.java) |
| GET /api/v1/shifts/available | [ShiftConfigController.getAvailableShifts](../../src/main/java/org/example/doansummer2026/controller/ShiftConfigController.java) line 38 | [ShiftAvailabilityService.available](../../src/main/java/org/example/doansummer2026/service/ShiftAvailabilityService.java) |

**Additional classified endpoints:** `ShiftConfigController.getActiveShifts`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/AppointmentControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/AppointmentControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/AppointmentServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AppointmentServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/MedicalServiceSelectionPolicyServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MedicalServiceSelectionPolicyServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/MedicalServiceServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MedicalServiceServiceTest.java)

## G-05 View Public Department Schedule

Vietnamese: Xem lịch hoạt động các khoa. Actors: Guest. Delivery: UI + backend.

Review public department availability by date and shift before booking, without viewing named staff duty assignments.

**Registered routes:** `/schedule`

**Frontend evidence:** [src/pages/public/PublicSchedulePage.jsx](../../../untitled/src/pages/public/PublicSchedulePage.jsx); [src/services/publicDepartmentScheduleService.js](../../../untitled/src/services/publicDepartmentScheduleService.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/public/department-schedules | [PublicDepartmentScheduleController.getDepartmentSchedules](../../src/main/java/org/example/doansummer2026/controller/PublicDepartmentScheduleController.java) line 21 | [PublicDepartmentScheduleService.getWeek](../../src/main/java/org/example/doansummer2026/service/PublicDepartmentScheduleService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/PublicDepartmentScheduleServiceTest.java](../../src/test/java/org/example/doansummer2026/service/PublicDepartmentScheduleServiceTest.java)

## G-06 Look Up Guest Journey

Vietnamese: Tra cứu hành trình khách vãng lai. Actors: Guest. Delivery: UI + backend.

Use the visit code and matching phone number to view the guest journey, current queue and service statuses. This lookup does not provide diagnoses, prescriptions or clinical result content.

**Registered routes:** `/guest/journey`

**Frontend evidence:** [src/pages/guest/GuestJourneyPage.jsx](../../../untitled/src/pages/guest/GuestJourneyPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/public/patient-journeys/lookup | [PatientJourneyController.lookupGuest](../../src/main/java/org/example/doansummer2026/controller/PatientJourneyController.java) line 79 | [PatientJourneyService.lookupGuest](../../src/main/java/org/example/doansummer2026/service/PatientJourneyService.java) |
| GET /api/public/patient-journeys/lookup/queue | [PatientJourneyController.lookupGuestQueue](../../src/main/java/org/example/doansummer2026/controller/PatientJourneyController.java) line 86 | [PatientJourneyService.lookupGuestQueue](../../src/main/java/org/example/doansummer2026/service/PatientJourneyService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/PatientJourneyServiceTest.java](../../src/test/java/org/example/doansummer2026/service/PatientJourneyServiceTest.java)

## G-07 Request Same-day Return as Guest

Vietnamese: Báo quay lại trong ngày cho Guest. Actors: Guest. Delivery: UI + backend.

Submit a return request for an eligible skipped ticket using the validated guest visit and phone number. Wait for reception confirmation; an expired ticket cannot be restored into a new day.

**Registered routes:** `/guest/journey`

**Frontend evidence:** [src/pages/guest/GuestJourneyPage.jsx](../../../untitled/src/pages/guest/GuestJourneyPage.jsx); [src/services/queueReturnRequestService.js](../../../untitled/src/services/queueReturnRequestService.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/public/patient-journeys/return-request | [PatientJourneyController.requestGuestReturn](../../src/main/java/org/example/doansummer2026/controller/PatientJourneyController.java) line 93 | [QueueReturnRequestService.requestForGuest](../../src/main/java/org/example/doansummer2026/service/QueueReturnRequestService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/QueueReturnRequestServiceTest.java](../../src/test/java/org/example/doansummer2026/service/QueueReturnRequestServiceTest.java)

## G-08 Use Guest Support Chat

Vietnamese: Chat hỗ trợ không cần tài khoản. Actors: Guest. Delivery: UI + backend.

Start a guest conversation, read replies and continue it using the guest session credentials. Automated replies are an internal support feature, not a separate external actor.

**Registered routes:** No standalone route; shared component/hook, API-only or integration as indicated.

**Frontend evidence:** [src/components/ui/ChatWidget.jsx](../../../untitled/src/components/ui/ChatWidget.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/chat/guest/session | [ChatController.startOrGetGuestSession](../../src/main/java/org/example/doansummer2026/controller/ChatController.java) line 57 | [ChatService.startOrGetGuestSession](../../src/main/java/org/example/doansummer2026/service/ChatService.java) |
| GET /api/v1/chat/guest/{sessionId}/messages | [ChatController.getGuestMessages](../../src/main/java/org/example/doansummer2026/controller/ChatController.java) line 111 | Controller/local handling; inspect linked method. |
| GET /api/v1/chat/guest/{sessionId}/status | [ChatController.getGuestSessionStatus](../../src/main/java/org/example/doansummer2026/controller/ChatController.java) line 138 | [ChatService.getSession](../../src/main/java/org/example/doansummer2026/service/ChatService.java) |
| POST /api/v1/chat/guest/{sessionId}/messages | [ChatController.sendGuestMessage](../../src/main/java/org/example/doansummer2026/controller/ChatController.java) line 189 | [ChatService.processCustomerMessage](../../src/main/java/org/example/doansummer2026/service/ChatService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/ChatControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/ChatControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/ChatServiceTest.java](../../src/test/java/org/example/doansummer2026/service/ChatServiceTest.java)

## G-09 Submit Contact Enquiry

Vietnamese: Gửi yêu cầu liên hệ. Actors: Guest. Delivery: UI + backend.

Send contact information and an enquiry through the public form for delivery to the configured clinic mailbox. This is separate from the live receptionist chat.

**Registered routes:** `/contact`

**Frontend evidence:** [src/pages/public/ContactPage.jsx](../../../untitled/src/pages/public/ContactPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/public/contact-requests | [ContactRequestController.send](../../src/main/java/org/example/doansummer2026/controller/ContactRequestController.java) line 20 | [ContactRequestService.send](../../src/main/java/org/example/doansummer2026/service/ContactRequestService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/ContactRequestServiceTest.java](../../src/test/java/org/example/doansummer2026/service/ContactRequestServiceTest.java)

## RU-01 Log In and Log Out

Vietnamese: Đăng nhập và đăng xuất. Actors: Registered User. Delivery: UI + backend.

Authenticate an existing account and end the local application session. Logout clears browser authentication state; no server-side logout or token-revocation endpoint is exposed by AuthController.

**Registered routes:** `/login`

**Frontend evidence:** [src/pages/auth/LoginPage.jsx](../../../untitled/src/pages/auth/LoginPage.jsx); [src/hooks/useLogin.js](../../../untitled/src/hooks/useLogin.js); [src/components/layout/CustomerLayout.jsx](../../../untitled/src/components/layout/CustomerLayout.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/auth/login | [AuthController.login](../../src/main/java/org/example/doansummer2026/controller/AuthController.java) line 83 | [AuthServiceInterface.login](../../src/main/java/org/example/doansummer2026/service/interfaces/AuthServiceInterface.java) |
| GET /api/auth/me | [AuthController.me](../../src/main/java/org/example/doansummer2026/controller/AuthController.java) line 95 | [AuthServiceInterface.currentAccount](../../src/main/java/org/example/doansummer2026/service/interfaces/AuthServiceInterface.java); [AuthServiceInterface.getCurrentSystemRole](../../src/main/java/org/example/doansummer2026/service/interfaces/AuthServiceInterface.java) |
| POST /api/auth/refresh | [AuthController.refresh](../../src/main/java/org/example/doansummer2026/controller/AuthController.java) line 89 | [AuthServiceInterface.refresh](../../src/main/java/org/example/doansummer2026/service/interfaces/AuthServiceInterface.java) |

**Related backend tests (not run here):**

No filename-matched test reference found; this is not proof of missing test coverage.

## RU-02 Verify OTP

Vietnamese: Xác thực OTP. Actors: Registered User. Delivery: UI + backend.

Provide OTP evidence during registration or password recovery. Registration has a verification step; recovery submits the OTP with the new password. These operations can occur before login.

**Registered routes:** `/login`

**Frontend evidence:** [src/hooks/useRegister.js](../../../untitled/src/hooks/useRegister.js); [src/pages/auth/LoginPage.jsx](../../../untitled/src/pages/auth/LoginPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/auth/send-otp | [AuthController.sendOtp](../../src/main/java/org/example/doansummer2026/controller/AuthController.java) line 47 | [OtpService.sendOtp](../../src/main/java/org/example/doansummer2026/service/OtpService.java) |
| POST /api/auth/send-register-otp | [AuthController.sendRegisterOtp](../../src/main/java/org/example/doansummer2026/controller/AuthController.java) line 61 | [AuthServiceInterface.ensureRegistrationIdentifierAvailable](../../src/main/java/org/example/doansummer2026/service/interfaces/AuthServiceInterface.java); [OtpService.sendOtp](../../src/main/java/org/example/doansummer2026/service/OtpService.java) |
| POST /api/auth/verify-register-otp | [AuthController.verifyRegisterOtp](../../src/main/java/org/example/doansummer2026/controller/AuthController.java) line 69 | [OtpService.markOtpAsVerified](../../src/main/java/org/example/doansummer2026/service/OtpService.java) |
| POST /api/auth/reset-password | [AuthController.resetPassword](../../src/main/java/org/example/doansummer2026/controller/AuthController.java) line 76 | [AuthServiceInterface.resetPassword](../../src/main/java/org/example/doansummer2026/service/interfaces/AuthServiceInterface.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/OtpServiceTest.java](../../src/test/java/org/example/doansummer2026/service/OtpServiceTest.java)

## RU-03 Recover Password

Vietnamese: Khôi phục mật khẩu. Actors: Registered User. Delivery: UI + backend.

Request an OTP and set a new password after successful recovery verification. The recovery interaction is reached from the login screen.

**Registered routes:** `/login`

**Frontend evidence:** [src/pages/auth/LoginPage.jsx](../../../untitled/src/pages/auth/LoginPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/auth/send-otp | [AuthController.sendOtp](../../src/main/java/org/example/doansummer2026/controller/AuthController.java) line 47 | [OtpService.sendOtp](../../src/main/java/org/example/doansummer2026/service/OtpService.java) |
| POST /api/auth/reset-password | [AuthController.resetPassword](../../src/main/java/org/example/doansummer2026/controller/AuthController.java) line 76 | [AuthServiceInterface.resetPassword](../../src/main/java/org/example/doansummer2026/service/interfaces/AuthServiceInterface.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/OtpServiceTest.java](../../src/test/java/org/example/doansummer2026/service/OtpServiceTest.java)

## RU-04 Change Password

Vietnamese: Đổi mật khẩu. Actors: Registered User. Delivery: UI + backend.

Change the current account password through the personal-profile dialog, supplying the current password and the required new-password confirmation.

**Registered routes:** `/customer/profile`; `/staff/profile`

**Frontend evidence:** [src/pages/customer/ProfilePage.jsx](../../../untitled/src/pages/customer/ProfilePage.jsx); [src/pages/staff/StaffProfilePage.jsx](../../../untitled/src/pages/staff/StaffProfilePage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| PUT /api/auth/me/password | [AuthController.changePassword](../../src/main/java/org/example/doansummer2026/controller/AuthController.java) line 104 | [AuthServiceInterface.changeMyPassword](../../src/main/java/org/example/doansummer2026/service/interfaces/AuthServiceInterface.java) |

**Related backend tests (not run here):**

No filename-matched test reference found; this is not proof of missing test coverage.

## RU-05 View and Update Profile

Vietnamese: Xem và sửa hồ sơ cá nhân. Actors: Registered User. Delivery: UI + backend.

Read and update permitted personal information. Staff have a separate professional-information request; this does not grant self-assignment of system roles, duty rooms or technical capabilities.

**Registered routes:** `/customer/profile`; `/staff/profile`

**Frontend evidence:** [src/pages/customer/ProfilePage.jsx](../../../untitled/src/pages/customer/ProfilePage.jsx); [src/hooks/useProfile.js](../../../untitled/src/hooks/useProfile.js); [src/pages/staff/StaffProfilePage.jsx](../../../untitled/src/pages/staff/StaffProfilePage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/profiles/me | [ProfileController.me](../../src/main/java/org/example/doansummer2026/controller/ProfileController.java) line 58 | [AuthService.currentAccount](../../src/main/java/org/example/doansummer2026/service/AuthService.java); [ProfileService.getMyProfile](../../src/main/java/org/example/doansummer2026/service/ProfileService.java) |
| PUT /api/v1/profiles/me | [ProfileController.updateMe](../../src/main/java/org/example/doansummer2026/controller/ProfileController.java) line 67 | [AuthService.currentAccount](../../src/main/java/org/example/doansummer2026/service/AuthService.java); [ProfileService.getByAccount](../../src/main/java/org/example/doansummer2026/service/ProfileService.java); [ProfileService.updateSelf](../../src/main/java/org/example/doansummer2026/service/ProfileService.java) |
| GET /api/v1/staff/account/{accountId} | [StaffController.getByAccount](../../src/main/java/org/example/doansummer2026/controller/StaffController.java) line 152 | [AuthService.currentAccount](../../src/main/java/org/example/doansummer2026/service/AuthService.java); [AuthService.getCurrentSystemRole](../../src/main/java/org/example/doansummer2026/service/AuthService.java); [StaffService.getByAccountId](../../src/main/java/org/example/doansummer2026/service/StaffService.java) |
| PUT /api/v1/staff/me/professional | [StaffController.updateOwnProfessionalInfo](../../src/main/java/org/example/doansummer2026/controller/StaffController.java) line 166 | [StaffService.updateOwnProfessionalInfo](../../src/main/java/org/example/doansummer2026/service/StaffService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/StaffControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/StaffControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/AuthServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AuthServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/ProfileServiceTest.java](../../src/test/java/org/example/doansummer2026/service/ProfileServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/StaffServiceTest.java](../../src/test/java/org/example/doansummer2026/service/StaffServiceTest.java)

## RU-06 Read In-app Notifications

Vietnamese: Xem thông báo trong ứng dụng. Actors: Registered User. Delivery: UI + backend.

Read recent notifications, check the unread count and mark a displayed notification as read. In-app delivery is distinct from email or SMS delivery.

**Registered routes:** No standalone route; shared component/hook, API-only or integration as indicated.

**Frontend evidence:** [src/components/ui/NotificationBell.jsx](../../../untitled/src/components/ui/NotificationBell.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/notifications/me | [NotificationController.myNotifications](../../src/main/java/org/example/doansummer2026/controller/NotificationController.java) line 50 | [ProfileService.findById](../../src/main/java/org/example/doansummer2026/service/ProfileService.java); [ProfileService.getByAccount](../../src/main/java/org/example/doansummer2026/service/ProfileService.java); [AuthService.currentAccount](../../src/main/java/org/example/doansummer2026/service/AuthService.java); [NotificationService.search](../../src/main/java/org/example/doansummer2026/service/NotificationService.java) |
| GET /api/v1/notifications/me/unread-count | [NotificationController.myUnreadCount](../../src/main/java/org/example/doansummer2026/controller/NotificationController.java) line 59 | [ProfileService.findById](../../src/main/java/org/example/doansummer2026/service/ProfileService.java); [ProfileService.getByAccount](../../src/main/java/org/example/doansummer2026/service/ProfileService.java); [AuthService.currentAccount](../../src/main/java/org/example/doansummer2026/service/AuthService.java); [NotificationService.unreadCount](../../src/main/java/org/example/doansummer2026/service/NotificationService.java) |
| POST /api/v1/notifications/{id}/mark-read | [NotificationController.markRead](../../src/main/java/org/example/doansummer2026/controller/NotificationController.java) line 99 | [NotificationService.findById](../../src/main/java/org/example/doansummer2026/service/NotificationService.java); [NotificationService.markRead](../../src/main/java/org/example/doansummer2026/service/NotificationService.java) |

**Additional classified endpoints:** `NotificationController.unreadCount`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/AuthServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AuthServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/NotificationServiceTest.java](../../src/test/java/org/example/doansummer2026/service/NotificationServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/ProfileServiceTest.java](../../src/test/java/org/example/doansummer2026/service/ProfileServiceTest.java)

## RU-07 View Own Duty Schedule

Vietnamese: Xem lịch trực cá nhân. Actors: Receptionist, Cashier, Nurse, Doctor, Clinic Manager. Delivery: UI + backend.

View personal weekly duty assignments as marked shifts and move between weeks. This shared staff function does not allow editing assignments. Administrator is not admitted by this frontend route.

**Registered routes:** `/staff/schedule`

**Frontend evidence:** [src/pages/staff/MySchedulePage.jsx](../../../untitled/src/pages/staff/MySchedulePage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/staff/my-schedule | [StaffController.mySchedule](../../src/main/java/org/example/doansummer2026/controller/StaffController.java) line 111 | [StaffScheduleService.findByStaffAndWeek](../../src/main/java/org/example/doansummer2026/service/StaffScheduleService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/StaffControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/StaffControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/StaffScheduleServiceTest.java](../../src/test/java/org/example/doansummer2026/service/StaffScheduleServiceTest.java)

## RU-08 Set Local Interface Preferences

Vietnamese: Đặt tùy chọn giao diện. Actors: Receptionist, Cashier, Nurse, Doctor, Clinic Manager, Administrator. Delivery: Frontend only by design.

Save interface preferences available on the Settings page, including theme, language and local display or notification options. Settings are stored in the browser, not a backend configuration API.

**Registered routes:** `/settings`

**Frontend evidence:** [src/pages/staff/SettingsPage.jsx](../../../untitled/src/pages/staff/SettingsPage.jsx); [src/utils/appPreferences.js](../../../untitled/src/utils/appPreferences.js)

**Primary API and service evidence:**

No dedicated backend endpoint for this presentation-only goal.

**Related backend tests (not run here):**

No filename-matched test reference found; this is not proof of missing test coverage.

## CU-01 Create Appointment

Vietnamese: Đặt lịch khám. Actors: Customer. Delivery: UI + backend.

Book active, customer-bookable services for an authorized patient profile, selecting an available date and shift. Availability is service based; the form is not a named-doctor appointment selector.

**Registered routes:** `/customer/appointment`

**Frontend evidence:** [src/pages/customer/CustomerAppointmentPage.jsx](../../../untitled/src/pages/customer/CustomerAppointmentPage.jsx); [src/hooks/useAppointment.js](../../../untitled/src/hooks/useAppointment.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/appointments/my | [AppointmentController.createMy](../../src/main/java/org/example/doansummer2026/controller/AppointmentController.java) line 164 | [AppointmentService.createMy](../../src/main/java/org/example/doansummer2026/service/AppointmentService.java); [AuthService.currentAccount](../../src/main/java/org/example/doansummer2026/service/AuthService.java) |
| GET /api/v1/medical-services/available | [MedicalServiceController.listAvailable](../../src/main/java/org/example/doansummer2026/controller/MedicalServiceController.java) line 57 | [MedicalServiceService.listAvailable](../../src/main/java/org/example/doansummer2026/service/MedicalServiceService.java) |
| POST /api/v1/medical-services/resolve-selection | [MedicalServiceController.resolveSelection](../../src/main/java/org/example/doansummer2026/controller/MedicalServiceController.java) line 66 | [MedicalServiceSelectionPolicyService.resolveResponse](../../src/main/java/org/example/doansummer2026/service/MedicalServiceSelectionPolicyService.java) |
| GET /api/v1/shifts/available | [ShiftConfigController.getAvailableShifts](../../src/main/java/org/example/doansummer2026/controller/ShiftConfigController.java) line 38 | [ShiftAvailabilityService.available](../../src/main/java/org/example/doansummer2026/service/ShiftAvailabilityService.java) |

**Additional classified endpoints:** `AppointmentController.create`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/AppointmentControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/AppointmentControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/AppointmentServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AppointmentServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/AuthServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AuthServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/MedicalServiceSelectionPolicyServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MedicalServiceSelectionPolicyServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/MedicalServiceServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MedicalServiceServiceTest.java)

## CU-02 View Appointment List and Details

Vietnamese: Xem lịch hẹn. Actors: Customer. Delivery: UI + backend.

Review appointment lists, selected services and status for the account or permitted family profile. Present the displayed booking information to reception when attending.

**Registered routes:** `/my-appointments`; `/my-appointments/:id`

**Frontend evidence:** [src/pages/customer/MyAppointmentsPage.jsx](../../../untitled/src/pages/customer/MyAppointmentsPage.jsx); [src/pages/customer/AppointmentDetailPage.jsx](../../../untitled/src/pages/customer/AppointmentDetailPage.jsx); [src/hooks/useAppointmentsCustomer.js](../../../untitled/src/hooks/useAppointmentsCustomer.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/appointments/my | [AppointmentController.getMyAppointments](../../src/main/java/org/example/doansummer2026/controller/AppointmentController.java) line 149 | [AppointmentService.getMyAppointments](../../src/main/java/org/example/doansummer2026/service/AppointmentService.java); [AuthService.currentAccount](../../src/main/java/org/example/doansummer2026/service/AuthService.java) |
| GET /api/v1/appointments/my/{id} | [AppointmentController.getMyAppointmentDetail](../../src/main/java/org/example/doansummer2026/controller/AppointmentController.java) line 181 | [AppointmentService.getMyAppointmentDetail](../../src/main/java/org/example/doansummer2026/service/AppointmentService.java); [AuthService.currentAccount](../../src/main/java/org/example/doansummer2026/service/AuthService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/AppointmentControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/AppointmentControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/AppointmentServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AppointmentServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/AuthServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AuthServiceTest.java)

## CU-03 Cancel Appointment

Vietnamese: Hủy lịch hẹn. Actors: Customer. Delivery: UI + backend.

Cancel an owned or manageable appointment when its state permits cancellation. Reception check-in is not a Customer confirmation action.

**Registered routes:** No standalone route; shared component/hook, API-only or integration as indicated.

**Frontend evidence:** [src/hooks/useAppointmentsCustomer.js](../../../untitled/src/hooks/useAppointmentsCustomer.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/appointments/my/{id}/cancel | [AppointmentController.cancelMyAppointment](../../src/main/java/org/example/doansummer2026/controller/AppointmentController.java) line 195 | [AppointmentService.cancelMyAppointment](../../src/main/java/org/example/doansummer2026/service/AppointmentService.java); [AuthService.currentAccount](../../src/main/java/org/example/doansummer2026/service/AuthService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/AppointmentControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/AppointmentControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/AppointmentServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AppointmentServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/AuthServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AuthServiceTest.java)

## CU-04 View Follow-up Information

Vietnamese: Xem yêu cầu và lịch tái khám. Actors: Customer. Delivery: UI + backend.

Review follow-up advice in the published medical record and view an appointment once it has been scheduled. A medical follow-up recommendation is not itself a confirmed future appointment.

**Registered routes:** `/my-appointments/:id`; `/customer/visits/:id`

**Frontend evidence:** [src/pages/customer/VisitDetailPage.jsx](../../../untitled/src/pages/customer/VisitDetailPage.jsx); [src/pages/customer/AppointmentDetailPage.jsx](../../../untitled/src/pages/customer/AppointmentDetailPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/patient/medical-history/visits/{visitId} | [MedicalRecordController.getPatientVisitDetail](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 209 | [MedicalRecordService.getPatientVisitDetail](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |
| GET /api/v1/appointments/my/{id} | [AppointmentController.getMyAppointmentDetail](../../src/main/java/org/example/doansummer2026/controller/AppointmentController.java) line 181 | [AppointmentService.getMyAppointmentDetail](../../src/main/java/org/example/doansummer2026/service/AppointmentService.java); [AuthService.currentAccount](../../src/main/java/org/example/doansummer2026/service/AuthService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/AppointmentControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/AppointmentControllerTest.java)
- [src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/AppointmentServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AppointmentServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/AuthServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AuthServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java)

## CU-06 Track Examination Journey

Vietnamese: Theo dõi hành trình khám. Actors: Customer. Delivery: UI + backend.

Track the current step, queue position, return requests and completed or skipped services for an authorized visit. The interface distinguishes ticket number from current waiting position and offers a new booking after the day has ended.

**Registered routes:** `/customer/waiting-room`

**Frontend evidence:** [src/pages/customer/WaitingRoomPage.jsx](../../../untitled/src/pages/customer/WaitingRoomPage.jsx); [src/components/journey/QueuePanel.jsx](../../../untitled/src/components/journey/QueuePanel.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/patient/my-journeys | [PatientJourneyController.mine](../../src/main/java/org/example/doansummer2026/controller/PatientJourneyController.java) line 44 | [AuthService.currentAccount](../../src/main/java/org/example/doansummer2026/service/AuthService.java); [FamilyAccessService.resolveReadableProfile](../../src/main/java/org/example/doansummer2026/service/FamilyAccessService.java); [PatientJourneyService.listForCustomer](../../src/main/java/org/example/doansummer2026/service/PatientJourneyService.java); [FamilyAccessService.readableProfiles](../../src/main/java/org/example/doansummer2026/service/FamilyAccessService.java); [FamilyAccessService.ownerProfile](../../src/main/java/org/example/doansummer2026/service/FamilyAccessService.java) |
| GET /api/patient/my-journeys/{visitId}/queue | [PatientJourneyController.myQueue](../../src/main/java/org/example/doansummer2026/controller/PatientJourneyController.java) line 63 | [AuthService.currentAccount](../../src/main/java/org/example/doansummer2026/service/AuthService.java); [FamilyAccessService.readableProfiles](../../src/main/java/org/example/doansummer2026/service/FamilyAccessService.java); [PatientJourneyService.queueForCustomer](../../src/main/java/org/example/doansummer2026/service/PatientJourneyService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/AuthServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AuthServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/FamilyAccessServiceTest.java](../../src/test/java/org/example/doansummer2026/service/FamilyAccessServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/PatientJourneyServiceTest.java](../../src/test/java/org/example/doansummer2026/service/PatientJourneyServiceTest.java)

## CU-07 View Completed Medical Records

Vietnamese: Xem bệnh án đã hoàn thành. Actors: Customer. Delivery: UI + backend.

Browse visit-based history and separate completed examination records within each visit. A partially completed visit can retain published clinical history; unfinished medical-record content is not a Customer publication.

**Registered routes:** `/customer/visits`; `/customer/visits/:id`

**Frontend evidence:** [src/pages/customer/MedicalHistoryPage.jsx](../../../untitled/src/pages/customer/MedicalHistoryPage.jsx); [src/pages/customer/VisitDetailPage.jsx](../../../untitled/src/pages/customer/VisitDetailPage.jsx); [src/hooks/useMedicalHistory.js](../../../untitled/src/hooks/useMedicalHistory.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/patient/medical-history/visits | [MedicalRecordController.getVisitHistory](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 196 | [MedicalRecordService.getVisitHistoryForPatient](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |
| GET /api/patient/medical-history/visits/{visitId} | [MedicalRecordController.getPatientVisitDetail](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 209 | [MedicalRecordService.getPatientVisitDetail](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |
| GET /api/patient/medical-history/{recordId} | [MedicalRecordController.getVisitDetail](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 184 | [MedicalRecordService.getVisitDetailByRecordId](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |

**Additional classified endpoints:** `MedicalRecordController.getMedicalHistory`, `MedicalRecordController.rateVisit`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java)

## CU-08 View Prescriptions and Published Diagnostic Results

Vietnamese: Xem đơn thuốc và kết quả CLS. Actors: Customer. Delivery: UI + backend.

Read the prescription belonging to each completed medical record and authorized signed diagnostic results. Laboratory analytes are presented by panel where supported; files remain authorized resources, and referenced same-day results are distinguished from newly performed services.

**Registered routes:** `/customer/visits/:id`

**Frontend evidence:** [src/pages/customer/VisitDetailPage.jsx](../../../untitled/src/pages/customer/VisitDetailPage.jsx); [src/features/medical-history/visitDetailUtils.js](../../../untitled/src/features/medical-history/visitDetailUtils.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/patient/medical-history/visits/{visitId} | [MedicalRecordController.getPatientVisitDetail](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 209 | [MedicalRecordService.getPatientVisitDetail](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |
| GET /api/patient/medical-history/{recordId}/clinical-form | [MedicalRecordController.getPatientClinicalForm](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 221 | [MedicalRecordService.getClinicalFormForPatient](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |
| GET /api/v1/test-results/{resultId}/file | [TestResultFileController.viewFile](../../src/main/java/org/example/doansummer2026/controller/TestResultFileController.java) line 45 | [AuthService.currentAccount](../../src/main/java/org/example/doansummer2026/service/AuthService.java) |
| GET /api/v1/test-results/attachments/{attachmentId}/file | [TestResultFileController.viewAttachment](../../src/main/java/org/example/doansummer2026/controller/TestResultFileController.java) line 81 | [AuthService.currentAccount](../../src/main/java/org/example/doansummer2026/service/AuthService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java)
- [src/test/java/org/example/doansummer2026/controller/TestResultFileControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/TestResultFileControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/AuthServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AuthServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java)

## CU-09 View Invoice and Payment History

Vietnamese: Xem lịch sử thanh toán và phiếu thu. Actors: Customer. Delivery: UI + backend.

Filter payment history by date, method and permitted patient profile, then inspect a receipt with invoice totals, insurance and CareS payment details where applicable.

**Registered routes:** `/customer/payments`; `/customer/payments/:id`

**Frontend evidence:** [src/pages/customer/PaymentHistoryPage.jsx](../../../untitled/src/pages/customer/PaymentHistoryPage.jsx); [src/pages/customer/ReceiptDetailPage.jsx](../../../untitled/src/pages/customer/ReceiptDetailPage.jsx); [src/hooks/usePaymentHistory.js](../../../untitled/src/hooks/usePaymentHistory.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/patient/payments | [InvoiceController.getPaymentHistory](../../src/main/java/org/example/doansummer2026/controller/InvoiceController.java) line 147 | [FamilyAccessService.resolveReadableProfile](../../src/main/java/org/example/doansummer2026/service/FamilyAccessService.java); [AuthService.currentAccount](../../src/main/java/org/example/doansummer2026/service/AuthService.java); [InvoiceService.getPaymentHistoryForPatient](../../src/main/java/org/example/doansummer2026/service/InvoiceService.java) |
| GET /api/patient/payments/{invoiceId} | [InvoiceController.getReceiptDetail](../../src/main/java/org/example/doansummer2026/controller/InvoiceController.java) line 174 | [FamilyAccessService.resolveReadableProfile](../../src/main/java/org/example/doansummer2026/service/FamilyAccessService.java); [AuthService.currentAccount](../../src/main/java/org/example/doansummer2026/service/AuthService.java); [InvoiceService.getReceiptDetail](../../src/main/java/org/example/doansummer2026/service/InvoiceService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/InvoiceControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/InvoiceControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/AuthServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AuthServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/FamilyAccessServiceTest.java](../../src/test/java/org/example/doansummer2026/service/FamilyAccessServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/InvoiceServiceTest.java](../../src/test/java/org/example/doansummer2026/service/InvoiceServiceTest.java)

## CU-11 Send Message to Receptionist

Vietnamese: Nhắn tin với lễ tân. Actors: Customer. Delivery: UI + backend.

Start or resume the account support conversation and send messages. Clinic Manager can also operate the staff-side support interface.

**Registered routes:** No standalone route; shared component/hook, API-only or integration as indicated.

**Frontend evidence:** [src/components/ui/ChatWidget.jsx](../../../untitled/src/components/ui/ChatWidget.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/chat/session | [ChatController.startOrGetSession](../../src/main/java/org/example/doansummer2026/controller/ChatController.java) line 42 | [ChatService.startOrGetActiveSession](../../src/main/java/org/example/doansummer2026/service/ChatService.java) |
| POST /api/v1/chat/{sessionId}/messages/customer | [ChatController.sendCustomerMessage](../../src/main/java/org/example/doansummer2026/controller/ChatController.java) line 171 | [ChatService.processCustomerMessage](../../src/main/java/org/example/doansummer2026/service/ChatService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/ChatControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/ChatControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/ChatServiceTest.java](../../src/test/java/org/example/doansummer2026/service/ChatServiceTest.java)

## CU-12 View Conversation Messages

Vietnamese: Xem nội dung hội thoại. Actors: Customer. Delivery: UI + backend.

Read messages and the status of the accessible customer conversation. The Customer widget is not a separate searchable archive of all closed conversations.

**Registered routes:** No standalone route; shared component/hook, API-only or integration as indicated.

**Frontend evidence:** [src/components/ui/ChatWidget.jsx](../../../untitled/src/components/ui/ChatWidget.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/chat/{sessionId}/messages | [ChatController.getMessages](../../src/main/java/org/example/doansummer2026/controller/ChatController.java) line 103 | Controller/local handling; inspect linked method. |
| GET /api/v1/chat/{sessionId}/status | [ChatController.getSessionStatus](../../src/main/java/org/example/doansummer2026/controller/ChatController.java) line 129 | [ChatService.getSession](../../src/main/java/org/example/doansummer2026/service/ChatService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/ChatControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/ChatControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/ChatServiceTest.java](../../src/test/java/org/example/doansummer2026/service/ChatServiceTest.java)

## CU-13 Submit and View Medical-record Feedback

Vietnamese: Gửi và xem đánh giá bệnh án. Actors: Customer. Delivery: UI + backend.

Submit the supported overall rating and comment for an eligible completed record and view its response. Feedback concerns the record experience; it is not a separate rating score for every doctor or nurse.

**Registered routes:** `/customer/visits/:id`

**Frontend evidence:** [src/pages/customer/VisitDetailPage.jsx](../../../untitled/src/pages/customer/VisitDetailPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/patient/medical-history/{recordId}/feedback | [MedicalRecordController.submitFeedback](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 245 | [MedicalRecordService.submitFeedback](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |
| GET /api/patient/medical-history/visits/{visitId} | [MedicalRecordController.getPatientVisitDetail](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 209 | [MedicalRecordService.getPatientVisitDetail](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |

**Additional classified endpoints:** `MedicalRecordController.getMedicalHistory`, `MedicalRecordController.rateVisit`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java)

## CU-14 Manage Family Profiles

Vietnamese: Quản lý hồ sơ gia đình. Actors: Customer. Delivery: UI + backend.

Create and update linked family profiles, stop managing a member or restore an eligible archived relationship. Reading historical family data and initiating new actions use different access checks.

**Registered routes:** `/customer/family-members`

**Frontend evidence:** [src/pages/customer/FamilyMembersPage.jsx](../../../untitled/src/pages/customer/FamilyMembersPage.jsx); [src/hooks/useFamilyMembers.js](../../../untitled/src/hooks/useFamilyMembers.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/customer/family-members | [FamilyMemberController.list](../../src/main/java/org/example/doansummer2026/controller/FamilyMemberController.java) line 36 | [FamilyMemberService.list](../../src/main/java/org/example/doansummer2026/service/FamilyMemberService.java) |
| POST /api/v1/customer/family-members | [FamilyMemberController.create](../../src/main/java/org/example/doansummer2026/controller/FamilyMemberController.java) line 42 | [FamilyMemberService.create](../../src/main/java/org/example/doansummer2026/service/FamilyMemberService.java) |
| PUT /api/v1/customer/family-members/{id} | [FamilyMemberController.update](../../src/main/java/org/example/doansummer2026/controller/FamilyMemberController.java) line 49 | [FamilyMemberService.update](../../src/main/java/org/example/doansummer2026/service/FamilyMemberService.java) |
| DELETE /api/v1/customer/family-members/{id} | [FamilyMemberController.archive](../../src/main/java/org/example/doansummer2026/controller/FamilyMemberController.java) line 56 | [FamilyMemberService.archive](../../src/main/java/org/example/doansummer2026/service/FamilyMemberService.java) |
| POST /api/v1/customer/family-members/{id}/restore | [FamilyMemberController.restore](../../src/main/java/org/example/doansummer2026/controller/FamilyMemberController.java) line 63 | [FamilyMemberService.restore](../../src/main/java/org/example/doansummer2026/service/FamilyMemberService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/FamilyMemberServiceTest.java](../../src/test/java/org/example/doansummer2026/service/FamilyMemberServiceTest.java)

## CU-15 Create Family Group Booking

Vietnamese: Đặt lịch nhóm gia đình. Actors: Customer. Delivery: UI + backend.

Book for multiple manageable patient profiles through the family-booking flow. Each patient retains a separate appointment and later visit, rather than sharing one medical record.

**Registered routes:** `/customer/appointment`

**Frontend evidence:** [src/pages/customer/CustomerAppointmentPage.jsx](../../../untitled/src/pages/customer/CustomerAppointmentPage.jsx); [src/hooks/useAppointment.js](../../../untitled/src/hooks/useAppointment.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/appointments/my/group | [AppointmentController.createMyGroup](../../src/main/java/org/example/doansummer2026/controller/AppointmentController.java) line 173 | [AppointmentService.createMyGroup](../../src/main/java/org/example/doansummer2026/service/AppointmentService.java); [AuthService.currentAccount](../../src/main/java/org/example/doansummer2026/service/AuthService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/AppointmentControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/AppointmentControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/AppointmentServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AppointmentServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/AuthServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AuthServiceTest.java)

## CU-16 Change Appointment Details

Vietnamese: Thay đổi lịch hẹn. Actors: Customer. Delivery: UI + backend.

Reopen an eligible appointment in the booking flow and save permitted service, date or shift changes after availability validation.

**Registered routes:** `/my-appointments/:id`

**Frontend evidence:** [src/pages/customer/AppointmentDetailPage.jsx](../../../untitled/src/pages/customer/AppointmentDetailPage.jsx); [src/hooks/useAppointment.js](../../../untitled/src/hooks/useAppointment.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| PUT /api/v1/appointments/my/{id} | [AppointmentController.updateMyAppointment](../../src/main/java/org/example/doansummer2026/controller/AppointmentController.java) line 187 | [AppointmentService.updateMyAppointment](../../src/main/java/org/example/doansummer2026/service/AppointmentService.java); [AuthService.currentAccount](../../src/main/java/org/example/doansummer2026/service/AuthService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/AppointmentControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/AppointmentControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/AppointmentServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AppointmentServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/AuthServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AuthServiceTest.java)

## CU-17 Request Same-day Return

Vietnamese: Báo đã quay lại trong ngày. Actors: Customer. Delivery: UI + backend.

Request reception verification for an eligible skipped ticket of the account or actively managed family profile. The request alone does not place the patient back into the callable queue.

**Registered routes:** `/customer/waiting-room`

**Frontend evidence:** [src/pages/customer/WaitingRoomPage.jsx](../../../untitled/src/pages/customer/WaitingRoomPage.jsx); [src/services/queueReturnRequestService.js](../../../untitled/src/services/queueReturnRequestService.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/patient/my-journeys/{visitId}/return-request | [PatientJourneyController.requestReturn](../../src/main/java/org/example/doansummer2026/controller/PatientJourneyController.java) line 72 | [QueueReturnRequestService.requestForCustomer](../../src/main/java/org/example/doansummer2026/service/QueueReturnRequestService.java); [AuthService.currentAccount](../../src/main/java/org/example/doansummer2026/service/AuthService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/AuthServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AuthServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/QueueReturnRequestServiceTest.java](../../src/test/java/org/example/doansummer2026/service/QueueReturnRequestServiceTest.java)

## CU-18 Register CareS Prepaid Card

Vietnamese: Đăng ký thẻ trả trước CareS. Actors: Customer. Delivery: UI + backend.

Register the account CareS card using the card form and required PIN confirmation. Card registration does not itself top up the balance.

**Registered routes:** `/customer/membership-card`

**Frontend evidence:** [src/pages/customer/MembershipCardPage.jsx](../../../untitled/src/pages/customer/MembershipCardPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/membership-cards/my/register | [MembershipCardController.register](../../src/main/java/org/example/doansummer2026/controller/MembershipCardController.java) line 24 | [MembershipCardService.register](../../src/main/java/org/example/doansummer2026/service/MembershipCardService.java); [AuthService.currentAccount](../../src/main/java/org/example/doansummer2026/service/AuthService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/AuthServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AuthServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/MembershipCardServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MembershipCardServiceTest.java)

## CU-19 View CareS Card and Balance History

Vietnamese: Xem thẻ CareS và lịch sử số dư. Actors: Customer. Delivery: UI + backend.

Review card status, benefit information, balance and the balance-ledger history exposed by the Customer card screen. Top-ups are handled at the counter.

**Registered routes:** `/customer/membership-card`

**Frontend evidence:** [src/pages/customer/MembershipCardPage.jsx](../../../untitled/src/pages/customer/MembershipCardPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/membership-cards/my | [MembershipCardController.myCard](../../src/main/java/org/example/doansummer2026/controller/MembershipCardController.java) line 31 | [MembershipCardService.myCard](../../src/main/java/org/example/doansummer2026/service/MembershipCardService.java); [AuthService.currentAccount](../../src/main/java/org/example/doansummer2026/service/AuthService.java) |
| GET /api/v1/membership-cards/my/history | [MembershipCardController.history](../../src/main/java/org/example/doansummer2026/controller/MembershipCardController.java) line 37 | [MembershipCardService.history](../../src/main/java/org/example/doansummer2026/service/MembershipCardService.java); [AuthService.currentAccount](../../src/main/java/org/example/doansummer2026/service/AuthService.java) |
| GET /api/v1/membership-cards/policy | [MembershipCardController.policy](../../src/main/java/org/example/doansummer2026/controller/MembershipCardController.java) line 65 | [MembershipCardService.getPolicy](../../src/main/java/org/example/doansummer2026/service/MembershipCardService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/AuthServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AuthServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/MembershipCardServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MembershipCardServiceTest.java)

## CU-20 Print an Individual Medical Record

Vietnamese: In bệnh án theo dịch vụ. Actors: Customer, Doctor. Delivery: UI + backend.

Open and print the specific examination record selected by record ID. Customer printing reloads authorized completed data and supports a permitted family profile; clinical data from different examination services must remain separate.

**Registered routes:** `/customer/visits/:id`; `/customer/medical-records/:recordId/print`; `/doctor/medical-records/:recordId/print`

**Frontend evidence:** [src/pages/doctor/MedicalRecordPrintPage.jsx](../../../untitled/src/pages/doctor/MedicalRecordPrintPage.jsx); [src/pages/customer/VisitDetailPage.jsx](../../../untitled/src/pages/customer/VisitDetailPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/patient/medical-history/{recordId} | [MedicalRecordController.getVisitDetail](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 184 | [MedicalRecordService.getVisitDetailByRecordId](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |
| GET /api/v1/medical-records/{id} | [MedicalRecordController.get](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 63 | [MedicalRecordService.get](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |
| GET /api/v1/medical-records/{id}/visit-detail | [MedicalRecordController.staffVisitDetail](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 83 | [MedicalRecordService.getVisitDetailForStaff](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java)

## CU-21 Print Customer Receipt

Vietnamese: In phiếu thu của Customer. Actors: Customer. Delivery: UI + backend.

Print the authorized receipt through the shared receipt document, excluding the application navigation. This operation does not initiate or confirm payment.

**Registered routes:** `/customer/payments/:id`

**Frontend evidence:** [src/pages/customer/ReceiptDetailPage.jsx](../../../untitled/src/pages/customer/ReceiptDetailPage.jsx); [src/components/receipts/ReceiptDocument.jsx](../../../untitled/src/components/receipts/ReceiptDocument.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/patient/payments/{invoiceId} | [InvoiceController.getReceiptDetail](../../src/main/java/org/example/doansummer2026/controller/InvoiceController.java) line 174 | [FamilyAccessService.resolveReadableProfile](../../src/main/java/org/example/doansummer2026/service/FamilyAccessService.java); [AuthService.currentAccount](../../src/main/java/org/example/doansummer2026/service/AuthService.java); [InvoiceService.getReceiptDetail](../../src/main/java/org/example/doansummer2026/service/InvoiceService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/InvoiceControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/InvoiceControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/AuthServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AuthServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/FamilyAccessServiceTest.java](../../src/test/java/org/example/doansummer2026/service/FamilyAccessServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/InvoiceServiceTest.java](../../src/test/java/org/example/doansummer2026/service/InvoiceServiceTest.java)

## CU-22 Pay with Own CareS Card through API

Vietnamese: Thanh toán thẻ cá nhân qua API. Actors: Customer. Delivery: Backend only.

The Customer API accepts a card payment against an authorized invoice with the required PIN and idempotency information. No caller for this self-payment endpoint is present in the current Customer frontend.

**Registered routes:** No standalone route; shared component/hook, API-only or integration as indicated.

**Frontend evidence:** No frontend evidence claimed.

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/membership-cards/my/pay | [MembershipCardController.pay](../../src/main/java/org/example/doansummer2026/controller/MembershipCardController.java) line 43 | [MembershipCardService.pay](../../src/main/java/org/example/doansummer2026/service/MembershipCardService.java); [AuthService.currentAccount](../../src/main/java/org/example/doansummer2026/service/AuthService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/AuthServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AuthServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/MembershipCardServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MembershipCardServiceTest.java)

## RC-01 Search Patient

Vietnamese: Tìm hồ sơ bệnh nhân. Actors: Receptionist, Clinic Manager. Delivery: UI + backend.

Identify existing patients using the reception search functions before check-in or visit creation. Review same-day examination and signed-result information to avoid an inappropriate duplicate service.

**Registered routes:** `/receptionist/create-ticket`; `/receptionist/records`

**Frontend evidence:** [src/pages/receptionist/CreateTicketPage.jsx](../../../untitled/src/pages/receptionist/CreateTicketPage.jsx); [src/pages/receptionist/RecordsManagementPage.jsx](../../../untitled/src/pages/receptionist/RecordsManagementPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/receptionist/records/search-by-phone | [MedicalRecordController.searchByPhoneForReceptionist](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 356 | [MedicalRecordService.searchByPhone](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |
| GET /api/receptionist/records/customers | [MedicalRecordController.listCustomersForReceptionist](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 298 | [MedicalRecordService.searchUniqueCustomers](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |
| GET /api/v1/customer-visits/customers/{customerId}/same-day-paraclinical-results | [CustomerVisitController.sameDayResultsForReception](../../src/main/java/org/example/doansummer2026/controller/CustomerVisitController.java) line 55 | [SameDayParaclinicalResultService.findForCustomerToday](../../src/main/java/org/example/doansummer2026/service/SameDayParaclinicalResultService.java) |
| GET /api/v1/customer-visits/customers/{customerId}/same-day-examination-services | [CustomerVisitController.sameDayExaminationServices](../../src/main/java/org/example/doansummer2026/controller/CustomerVisitController.java) line 62 | [CustomerVisitService.getSameDayExaminationServices](../../src/main/java/org/example/doansummer2026/service/CustomerVisitService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/CustomerVisitServiceTest.java](../../src/test/java/org/example/doansummer2026/service/CustomerVisitServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/SameDayParaclinicalResultServiceTest.java](../../src/test/java/org/example/doansummer2026/service/SameDayParaclinicalResultServiceTest.java)

## RC-02 Create or Complete Patient Profile

Vietnamese: Tạo và cập nhật hồ sơ bệnh nhân. Actors: Receptionist, Clinic Manager. Delivery: UI + backend.

Enter or correct permitted identity, contact and patient-profile information during reception or in patient management. Clinical history is viewed separately from demographic editing.

**Registered routes:** `/receptionist/create-ticket`; `/receptionist/patients/:id`

**Frontend evidence:** [src/pages/receptionist/PatientDetailPage.jsx](../../../untitled/src/pages/receptionist/PatientDetailPage.jsx); [src/pages/receptionist/PatientUpdateModal.jsx](../../../untitled/src/pages/receptionist/PatientUpdateModal.jsx); [src/pages/receptionist/CreateTicketPage.jsx](../../../untitled/src/pages/receptionist/CreateTicketPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/profiles | [ProfileController.create](../../src/main/java/org/example/doansummer2026/controller/ProfileController.java) line 92 | [ProfileService.create](../../src/main/java/org/example/doansummer2026/service/ProfileService.java) |
| PUT /api/v1/profiles/{id} | [ProfileController.update](../../src/main/java/org/example/doansummer2026/controller/ProfileController.java) line 103 | [ProfileService.update](../../src/main/java/org/example/doansummer2026/service/ProfileService.java) |
| PUT /api/receptionist/records/customers/{customerId} | [MedicalRecordController.updateCustomerForReceptionist](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 330 | [ProfileService.update](../../src/main/java/org/example/doansummer2026/service/ProfileService.java) |

**Additional classified endpoints:** `ProfileController.delete`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/ProfileServiceTest.java](../../src/test/java/org/example/doansummer2026/service/ProfileServiceTest.java)

## RC-03 Check In Scheduled Patient

Vietnamese: Tiếp nhận khách có lịch hẹn. Actors: Receptionist, Clinic Manager. Delivery: UI + backend.

Review the appointment, verify patient information and confirm reception. Allowed changes can add examination services, laboratory panels or individual analytes before the visit and initial invoice are prepared.

**Registered routes:** `/receptionist/check-in`; `/receptionist/appointments/:id`

**Frontend evidence:** [src/pages/receptionist/CheckInPage.jsx](../../../untitled/src/pages/receptionist/CheckInPage.jsx); [src/pages/receptionist/AppointmentDetailPage.jsx](../../../untitled/src/pages/receptionist/AppointmentDetailPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/appointments | [AppointmentController.list](../../src/main/java/org/example/doansummer2026/controller/AppointmentController.java) line 50 | [AppointmentService.search](../../src/main/java/org/example/doansummer2026/service/AppointmentService.java) |
| GET /api/v1/appointments/{id} | [AppointmentController.get](../../src/main/java/org/example/doansummer2026/controller/AppointmentController.java) line 61 | [AppointmentService.get](../../src/main/java/org/example/doansummer2026/service/AppointmentService.java) |
| POST /api/v1/appointments/{id}/check-in | [AppointmentController.checkIn](../../src/main/java/org/example/doansummer2026/controller/AppointmentController.java) line 109 | [AuthService.currentStaffId](../../src/main/java/org/example/doansummer2026/service/AuthService.java); [AppointmentService.checkIn](../../src/main/java/org/example/doansummer2026/service/AppointmentService.java) |

**Additional classified endpoints:** `AppointmentController.guestCheckIn`, `AppointmentController.getGuestHistory`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/AppointmentControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/AppointmentControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/AppointmentServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AppointmentServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/AuthServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AuthServiceTest.java)

## RC-04 Create Walk-in Visit

Vietnamese: Tạo phiếu khám tại quầy. Actors: Receptionist, Clinic Manager. Delivery: UI + backend.

Select or enter the patient, choose examination services and diagnostic panels or analytes, review the selection and create a walk-in visit with its initial charges. Callable services depend on subsequent payment and sequencing.

**Registered routes:** `/receptionist/create-ticket`

**Frontend evidence:** [src/pages/receptionist/CreateTicketPage.jsx](../../../untitled/src/pages/receptionist/CreateTicketPage.jsx); [src/hooks/useCreateTicket.js](../../../untitled/src/hooks/useCreateTicket.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/customer-visits | [CustomerVisitController.create](../../src/main/java/org/example/doansummer2026/controller/CustomerVisitController.java) line 69 | [AuthService.currentStaffId](../../src/main/java/org/example/doansummer2026/service/AuthService.java); [CustomerVisitService.create](../../src/main/java/org/example/doansummer2026/service/CustomerVisitService.java) |
| POST /api/v1/medical-services/resolve-selection | [MedicalServiceController.resolveSelection](../../src/main/java/org/example/doansummer2026/controller/MedicalServiceController.java) line 66 | [MedicalServiceSelectionPolicyService.resolveResponse](../../src/main/java/org/example/doansummer2026/service/MedicalServiceSelectionPolicyService.java) |

**Additional classified endpoints:** `AppointmentController.create`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/AuthServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AuthServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/CustomerVisitServiceTest.java](../../src/test/java/org/example/doansummer2026/service/CustomerVisitServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/MedicalServiceSelectionPolicyServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MedicalServiceSelectionPolicyServiceTest.java)

## RC-05 View Visit and Ticket Information

Vietnamese: Xem phiếu khám và lượt khám. Actors: Receptionist, Clinic Manager. Delivery: UI + backend.

Search reception visit records and inspect the selected visit, services, invoice and queue information using the visit-management interface.

**Registered routes:** `/receptionist/visits`

**Frontend evidence:** [src/pages/receptionist/VisitManagementPage.jsx](../../../untitled/src/pages/receptionist/VisitManagementPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/customer-visits | [CustomerVisitController.list](../../src/main/java/org/example/doansummer2026/controller/CustomerVisitController.java) line 38 | [CustomerVisitService.search](../../src/main/java/org/example/doansummer2026/service/CustomerVisitService.java) |
| GET /api/v1/customer-visits/{id} | [CustomerVisitController.get](../../src/main/java/org/example/doansummer2026/controller/CustomerVisitController.java) line 49 | [CustomerVisitService.get](../../src/main/java/org/example/doansummer2026/service/CustomerVisitService.java) |

**Additional classified endpoints:** `CustomerVisitController.update`, `CustomerVisitController.delete`, `QueueTicketController.create`, `QueueTicketController.update`, `QueueTicketController.delete`, `QueueTicketController.updateQueue`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/CustomerVisitServiceTest.java](../../src/test/java/org/example/doansummer2026/service/CustomerVisitServiceTest.java)

## RC-06 Update Permitted Appointment Information

Vietnamese: Sửa lịch hẹn tại lễ tân. Actors: Receptionist, Clinic Manager. Delivery: UI + backend.

Save permitted patient or appointment changes, including the service selection, or cancel an eligible booking. The reception selector supports both full panels and individual laboratory analytes.

**Registered routes:** `/receptionist/appointments/:id`

**Frontend evidence:** [src/pages/receptionist/AppointmentDetailPage.jsx](../../../untitled/src/pages/receptionist/AppointmentDetailPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| PUT /api/v1/appointments/{id} | [AppointmentController.update](../../src/main/java/org/example/doansummer2026/controller/AppointmentController.java) line 88 | [AppointmentService.update](../../src/main/java/org/example/doansummer2026/service/AppointmentService.java) |
| POST /api/v1/medical-services/resolve-selection | [MedicalServiceController.resolveSelection](../../src/main/java/org/example/doansummer2026/controller/MedicalServiceController.java) line 66 | [MedicalServiceSelectionPolicyService.resolveResponse](../../src/main/java/org/example/doansummer2026/service/MedicalServiceSelectionPolicyService.java) |

**Additional classified endpoints:** `AppointmentController.delete`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/AppointmentControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/AppointmentControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/AppointmentServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AppointmentServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/MedicalServiceSelectionPolicyServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MedicalServiceSelectionPolicyServiceTest.java)

## RC-07 Review and Coordinate Patient Journey

Vietnamese: Theo dõi và điều phối hành trình. Actors: Receptionist, Clinic Manager. Delivery: UI + backend.

Inspect the visit timeline, current department, payment gates, blocked steps and completed or skipped services to direct the patient. Viewing a later step does not make it callable.

**Registered routes:** `/staff/patient-journeys`

**Frontend evidence:** [src/pages/staff/PatientJourneyPage.jsx](../../../untitled/src/pages/staff/PatientJourneyPage.jsx); [src/services/patientJourneyService.js](../../../untitled/src/services/patientJourneyService.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/patient-journeys | [PatientJourneyController.list](../../src/main/java/org/example/doansummer2026/controller/PatientJourneyController.java) line 26 | [PatientJourneyService.list](../../src/main/java/org/example/doansummer2026/service/PatientJourneyService.java) |
| GET /api/v1/patient-journeys/{visitId} | [PatientJourneyController.get](../../src/main/java/org/example/doansummer2026/controller/PatientJourneyController.java) line 34 | [PatientJourneyService.get](../../src/main/java/org/example/doansummer2026/service/PatientJourneyService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/PatientJourneyServiceTest.java](../../src/test/java/org/example/doansummer2026/service/PatientJourneyServiceTest.java)

## RC-08 Manage Support Conversations

Vietnamese: Quản lý hội thoại hỗ trợ. Actors: Receptionist, Clinic Manager. Delivery: UI + backend.

View active or closed conversations, open messages, reply to Customer or Guest and close an eligible session. This interface is not available to doctors, nurses or cashiers.

**Registered routes:** `/receptionist/support`

**Frontend evidence:** [src/pages/receptionist/ReceptionistSupportPage.jsx](../../../untitled/src/pages/receptionist/ReceptionistSupportPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/chat/sessions/active | [ChatController.getActiveSessionsForReceptionist](../../src/main/java/org/example/doansummer2026/controller/ChatController.java) line 73 | [ChatService.getActiveSessionsForReceptionist](../../src/main/java/org/example/doansummer2026/service/ChatService.java) |
| GET /api/v1/chat/sessions/history | [ChatController.getClosedSessionsForReceptionist](../../src/main/java/org/example/doansummer2026/controller/ChatController.java) line 88 | [ChatService.getClosedSessionsForReceptionist](../../src/main/java/org/example/doansummer2026/service/ChatService.java) |
| GET /api/v1/chat/{sessionId}/messages | [ChatController.getMessages](../../src/main/java/org/example/doansummer2026/controller/ChatController.java) line 103 | Controller/local handling; inspect linked method. |
| POST /api/v1/chat/{sessionId}/messages/receptionist | [ChatController.sendReceptionistMessage](../../src/main/java/org/example/doansummer2026/controller/ChatController.java) line 206 | [ChatService.processReceptionistMessage](../../src/main/java/org/example/doansummer2026/service/ChatService.java) |
| POST /api/v1/chat/{sessionId}/close | [ChatController.closeSession](../../src/main/java/org/example/doansummer2026/controller/ChatController.java) line 226 | [ChatService.closeSession](../../src/main/java/org/example/doansummer2026/service/ChatService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/ChatControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/ChatControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/ChatServiceTest.java](../../src/test/java/org/example/doansummer2026/service/ChatServiceTest.java)

## RC-10 Confirm Patient Return

Vietnamese: Xác nhận khách đã quay lại. Actors: Receptionist, Clinic Manager. Delivery: UI + backend.

Review pending same-day return requests and confirm the patient is present. The backend rechecks the ticket and restores WAITING or BLOCKED as the workflow permits; expired requests cannot be confirmed.

**Registered routes:** `/receptionist/check-in`

**Frontend evidence:** [src/pages/receptionist/CheckInPage.jsx](../../../untitled/src/pages/receptionist/CheckInPage.jsx); [src/services/queueReturnRequestService.js](../../../untitled/src/services/queueReturnRequestService.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/queue-return-requests | [QueueReturnRequestController.pending](../../src/main/java/org/example/doansummer2026/controller/QueueReturnRequestController.java) line 28 | [QueueReturnRequestService.pendingRequests](../../src/main/java/org/example/doansummer2026/service/QueueReturnRequestService.java) |
| POST /api/v1/queue-return-requests/{queueTicketId}/confirm | [QueueReturnRequestController.confirm](../../src/main/java/org/example/doansummer2026/controller/QueueReturnRequestController.java) line 33 | [QueueReturnRequestService.confirm](../../src/main/java/org/example/doansummer2026/service/QueueReturnRequestService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/QueueReturnRequestServiceTest.java](../../src/test/java/org/example/doansummer2026/service/QueueReturnRequestServiceTest.java)

## RC-11 Schedule Requested Follow-up

Vietnamese: Đặt lịch từ yêu cầu tái khám. Actors: Receptionist, Clinic Manager. Delivery: UI + backend.

Find a pending follow-up recommendation, select the appointment date and shift, and create the follow-up booking. Existing scheduled follow-ups are not treated as unset requests.

**Registered routes:** `/receptionist/follow-ups`

**Frontend evidence:** [src/pages/receptionist/FollowUpListPage.jsx](../../../untitled/src/pages/receptionist/FollowUpListPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/receptionist/follow-ups | [MedicalRecordController.getPendingFollowUps](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 364 | [MedicalRecordService.getPendingFollowUps](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |
| POST /api/receptionist/follow-ups/{recordId}/schedule | [MedicalRecordController.scheduleFollowUp](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 372 | [MedicalRecordService.scheduleFollowUp](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java)

## RC-12 Review Patient Visit History

Vietnamese: Xem lịch sử bệnh nhân tại lễ tân. Actors: Receptionist, Clinic Manager. Delivery: UI + backend.

Open the patient, visits and record-detail screens through reception history. Access uses the staff endpoints and is distinct from Customer family-profile authorization.

**Registered routes:** `/receptionist/patients/:id`; `/receptionist/patients/:id/visits/:visitId`; `/receptionist/records/:id`

**Frontend evidence:** [src/pages/receptionist/PatientDetailPage.jsx](../../../untitled/src/pages/receptionist/PatientDetailPage.jsx); [src/pages/receptionist/PatientVisitDetailPage.jsx](../../../untitled/src/pages/receptionist/PatientVisitDetailPage.jsx); [src/pages/receptionist/RecordDetailPage.jsx](../../../untitled/src/pages/receptionist/RecordDetailPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/receptionist/records/customers/{customerId} | [MedicalRecordController.getCustomerForReceptionist](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 323 | [MedicalRecordService.getCustomerForReceptionist](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |
| GET /api/receptionist/records/customers/{customerId}/visits | [MedicalRecordController.getCustomerVisitsForReceptionist](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 339 | [MedicalRecordService.getMedicalHistoryForPatient](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |
| GET /api/receptionist/records/customers/{customerId}/visits/{visitId} | [MedicalRecordController.getCustomerVisitForReceptionist](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 348 | [MedicalRecordService.getVisitDetail](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |
| GET /api/v1/medical-records/{id} | [MedicalRecordController.get](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 63 | [MedicalRecordService.get](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |

**Additional classified endpoints:** `AppointmentController.guestCheckIn`, `AppointmentController.getGuestHistory`, `MedicalRecordController.listRecordsForReceptionist`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java)

## RC-13 Recover an Eligible Blocked Journey through API

Vietnamese: Khôi phục bước bị kẹt qua API. Actors: Clinic Manager, Administrator. Delivery: Backend only.

Request recovery of an eligible blocked journey through the management API. The current staff journey screen is explicitly read-only and has no advance action. The backend checks dependencies; this is not an unrestricted override of clinical completion.

**Registered routes:** No standalone route; shared component/hook, API-only or integration as indicated.

**Frontend evidence:** No frontend evidence claimed.

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/patient-journeys/{visitId}/advance | [PatientJourneyController.advance](../../src/main/java/org/example/doansummer2026/controller/PatientJourneyController.java) line 38 | [PatientJourneyService.advanceBlockedStep](../../src/main/java/org/example/doansummer2026/service/PatientJourneyService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/PatientJourneyServiceTest.java](../../src/test/java/org/example/doansummer2026/service/PatientJourneyServiceTest.java)

## RC-14 Open Queue Display Screens

Vietnamese: Mở màn hình gọi bệnh nhân. Actors: Receptionist, Clinic Manager. Delivery: UI + backend.

Use the launcher to open the overall calling screen or a selected room display in an authenticated browser tab. The launcher itself is a staff function, not an external actor.

**Registered routes:** `/staff/queue-displays`

**Frontend evidence:** [src/pages/display/QueueDisplayLauncherPage.jsx](../../../untitled/src/pages/display/QueueDisplayLauncherPage.jsx); [src/utils/openAuthenticatedTab.js](../../../untitled/src/utils/openAuthenticatedTab.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/departments/clinical | [DepartmentController.listClinical](../../src/main/java/org/example/doansummer2026/controller/DepartmentController.java) line 66 | [DepartmentService.listMultiple](../../src/main/java/org/example/doansummer2026/service/DepartmentService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/DepartmentServiceTest.java](../../src/test/java/org/example/doansummer2026/service/DepartmentServiceTest.java)

## RC-15 View Overall or Room Calling Display

Vietnamese: Xem màn gọi tổng hoặc từng phòng. Actors: Receptionist, Clinic Manager, Doctor, Nurse, Administrator. Delivery: UI + backend.

Display only CALLED patients on the overall screen. The room screen shows the called or in-progress patient and next waiting patients, using names and birth years where available rather than treating ticket numbers as names.

**Registered routes:** `/display/room/:departmentId`; `/display/queues`

**Frontend evidence:** [src/pages/display/RoomQueueDisplayPage.jsx](../../../untitled/src/pages/display/RoomQueueDisplayPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/queue-tickets | [QueueTicketController.list](../../src/main/java/org/example/doansummer2026/controller/QueueTicketController.java) line 44 | [QueueTicketService.search](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |
| GET /api/v1/queue-tickets/waiting/{departmentId} | [QueueTicketController.getWaiting](../../src/main/java/org/example/doansummer2026/controller/QueueTicketController.java) line 175 | [QueueTicketService.getWaitingByDepartment](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |
| GET /api/v1/queue-tickets/in-progress/{departmentId} | [QueueTicketController.getInprogress](../../src/main/java/org/example/doansummer2026/controller/QueueTicketController.java) line 148 | [QueueTicketService.getInprogressByDepartment](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |
| GET /api/v1/departments/clinical | [DepartmentController.listClinical](../../src/main/java/org/example/doansummer2026/controller/DepartmentController.java) line 66 | [DepartmentService.listMultiple](../../src/main/java/org/example/doansummer2026/service/DepartmentService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/QueueTicketControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/QueueTicketControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/DepartmentServiceTest.java](../../src/test/java/org/example/doansummer2026/service/DepartmentServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java](../../src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java)

## CA-01 View Pending Invoices

Vietnamese: Xem hóa đơn chờ thanh toán. Actors: Cashier, Clinic Manager. Delivery: UI + backend.

Filter and inspect invoices requiring attention in the cashier invoice list. Listing an invoice does not acknowledge receipt of money.

**Registered routes:** `/cashier/invoices`

**Frontend evidence:** [src/pages/cashier/InvoiceListPage.jsx](../../../untitled/src/pages/cashier/InvoiceListPage.jsx); [src/hooks/useInvoiceList.js](../../../untitled/src/hooks/useInvoiceList.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/invoices | [InvoiceController.list](../../src/main/java/org/example/doansummer2026/controller/InvoiceController.java) line 50 | [InvoiceService.search](../../src/main/java/org/example/doansummer2026/service/InvoiceService.java) |

**Additional classified endpoints:** `InvoiceController.create`, `InvoiceController.update`, `InvoiceController.issue`, `InvoiceController.delete`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/InvoiceControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/InvoiceControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/InvoiceServiceTest.java](../../src/test/java/org/example/doansummer2026/service/InvoiceServiceTest.java)

## CA-02 View Invoice Details

Vietnamese: Xem chi tiết hóa đơn. Actors: Cashier, Clinic Manager. Delivery: UI + backend.

Review service items, insurance, existing reductions, payments and the remaining amount. CareS payment metadata is read from receipt data when available.

**Registered routes:** `/cashier/invoices/:id`

**Frontend evidence:** [src/pages/cashier/InvoiceDetailPage.jsx](../../../untitled/src/pages/cashier/InvoiceDetailPage.jsx); [src/hooks/useInvoiceDetail.js](../../../untitled/src/hooks/useInvoiceDetail.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/invoices/{id} | [InvoiceController.get](../../src/main/java/org/example/doansummer2026/controller/InvoiceController.java) line 63 | [InvoiceService.get](../../src/main/java/org/example/doansummer2026/service/InvoiceService.java) |
| GET /api/v1/invoices/{id}/print | [InvoiceController.getPrintData](../../src/main/java/org/example/doansummer2026/controller/InvoiceController.java) line 128 | [InvoiceService.getReceiptPrintData](../../src/main/java/org/example/doansummer2026/service/InvoiceService.java) |

**Additional classified endpoints:** `TestRequestController.findByInvoiceItem`, `TestRequestController.findByInvoice`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/InvoiceControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/InvoiceControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/InvoiceServiceTest.java](../../src/test/java/org/example/doansummer2026/service/InvoiceServiceTest.java)

## CA-03 Confirm Cash Payment

Vietnamese: Xác nhận thanh toán tiền mặt. Actors: Cashier, Clinic Manager. Delivery: UI + backend.

Confirm the supported cashier cash-payment operation. Payment updates the invoice and downstream service eligibility; the UI action is not a general editor for arbitrary transaction states.

**Registered routes:** `/cashier/invoices/:id`

**Frontend evidence:** [src/pages/cashier/InvoiceDetailPage.jsx](../../../untitled/src/pages/cashier/InvoiceDetailPage.jsx); [src/hooks/useInvoiceDetail.js](../../../untitled/src/hooks/useInvoiceDetail.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/invoices/{id}/pay | [InvoiceController.pay](../../src/main/java/org/example/doansummer2026/controller/InvoiceController.java) line 111 | [InvoiceService.pay](../../src/main/java/org/example/doansummer2026/service/InvoiceService.java); [AuthService.currentStaffId](../../src/main/java/org/example/doansummer2026/service/AuthService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/InvoiceControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/InvoiceControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/AuthServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AuthServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/InvoiceServiceTest.java](../../src/test/java/org/example/doansummer2026/service/InvoiceServiceTest.java)

## CA-04 Monitor Online Payment

Vietnamese: Theo dõi thanh toán PayOS. Actors: Cashier, Clinic Manager. Delivery: UI + backend.

Check invoice payment status after opening the PayOS link. Verified gateway callbacks are handled by the backend; opening a link alone is not proof of payment.

**Registered routes:** `/cashier/invoices/:id`

**Frontend evidence:** [src/pages/cashier/InvoiceDetailPage.jsx](../../../untitled/src/pages/cashier/InvoiceDetailPage.jsx); [src/hooks/useInvoiceDetail.js](../../../untitled/src/hooks/useInvoiceDetail.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/invoices/{id} | [InvoiceController.get](../../src/main/java/org/example/doansummer2026/controller/InvoiceController.java) line 63 | [InvoiceService.get](../../src/main/java/org/example/doansummer2026/service/InvoiceService.java) |
| POST /api/v1/payos/webhook | [PayOSWebhookController.handleWebhook](../../src/main/java/org/example/doansummer2026/controller/PayOSWebhookController.java) line 28 | [PayOSService.verifyWebhook](../../src/main/java/org/example/doansummer2026/service/PayOSService.java); [PayOSService.getInvoiceIdByOrderCode](../../src/main/java/org/example/doansummer2026/service/PayOSService.java); [InvoiceService.pay](../../src/main/java/org/example/doansummer2026/service/InvoiceService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/InvoiceControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/InvoiceControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/InvoiceServiceTest.java](../../src/test/java/org/example/doansummer2026/service/InvoiceServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/PayOSServiceTest.java](../../src/test/java/org/example/doansummer2026/service/PayOSServiceTest.java)

## CA-05 Review Invoice Payment History

Vietnamese: Tra cứu lịch sử hóa đơn đã thu. Actors: Cashier, Clinic Manager. Delivery: UI + backend.

Search historical invoices and inspect their payment information through the cashier screens. There is no separate current frontend page for arbitrary Transaction CRUD.

**Registered routes:** `/cashier/invoices`

**Frontend evidence:** [src/pages/cashier/InvoiceListPage.jsx](../../../untitled/src/pages/cashier/InvoiceListPage.jsx); [src/hooks/useInvoiceList.js](../../../untitled/src/hooks/useInvoiceList.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/invoices | [InvoiceController.list](../../src/main/java/org/example/doansummer2026/controller/InvoiceController.java) line 50 | [InvoiceService.search](../../src/main/java/org/example/doansummer2026/service/InvoiceService.java) |
| GET /api/v1/invoices/{id} | [InvoiceController.get](../../src/main/java/org/example/doansummer2026/controller/InvoiceController.java) line 63 | [InvoiceService.get](../../src/main/java/org/example/doansummer2026/service/InvoiceService.java) |

**Additional classified endpoints:** `TransactionController.list`, `TransactionController.get`, `TransactionController.create`, `TransactionController.update`, `TransactionController.confirm`, `TransactionController.fail`, `TransactionController.delete`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/InvoiceControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/InvoiceControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/InvoiceServiceTest.java](../../src/test/java/org/example/doansummer2026/service/InvoiceServiceTest.java)

## CA-06 Print Receipt

Vietnamese: In phiếu thu tại quầy. Actors: Cashier, Clinic Manager. Delivery: UI + backend.

Open the web/A4 receipt with service charges, insurance, payment method and masked CareS information where applicable. Print the receipt document rather than the application sidebar.

**Registered routes:** `/cashier/invoices/:id/print`

**Frontend evidence:** [src/pages/cashier/InvoicePrintPage.jsx](../../../untitled/src/pages/cashier/InvoicePrintPage.jsx); [src/hooks/useInvoicePrint.js](../../../untitled/src/hooks/useInvoicePrint.js); [src/components/receipts/ReceiptDocument.jsx](../../../untitled/src/components/receipts/ReceiptDocument.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/invoices/{id}/print | [InvoiceController.getPrintData](../../src/main/java/org/example/doansummer2026/controller/InvoiceController.java) line 128 | [InvoiceService.getReceiptPrintData](../../src/main/java/org/example/doansummer2026/service/InvoiceService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/InvoiceControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/InvoiceControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/InvoiceServiceTest.java](../../src/test/java/org/example/doansummer2026/service/InvoiceServiceTest.java)

## CA-07 Verify and Apply Health Insurance

Vietnamese: Kiểm tra và áp dụng BHYT. Actors: Cashier, Clinic Manager. Delivery: UI + backend.

Submit the insurance information and apply permitted coverage to the invoice. Verification uses the configured mock BHXH integration; this is not an official insurance claim submission.

**Registered routes:** `/cashier/invoices/:id`

**Frontend evidence:** [src/pages/cashier/InvoiceDetailPage.jsx](../../../untitled/src/pages/cashier/InvoiceDetailPage.jsx); [src/hooks/useInvoiceDetail.js](../../../untitled/src/hooks/useInvoiceDetail.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/invoices/{id}/insurance | [InvoiceController.applyInsurance](../../src/main/java/org/example/doansummer2026/controller/InvoiceController.java) line 88 | [InvoiceService.applyInsurance](../../src/main/java/org/example/doansummer2026/service/InvoiceService.java) |
| GET /api/v1/insurances | [InsuranceController.getAll](../../src/main/java/org/example/doansummer2026/controller/InsuranceController.java) line 23 | [InsuranceService.getAllInsurances](../../src/main/java/org/example/doansummer2026/service/InsuranceService.java) |
| GET /api/v1/bhxh/check | [BhxhController.checkCard](../../src/main/java/org/example/doansummer2026/controller/BhxhController.java) line 22 | [BhxhIntegrationService.checkBhytCard](../../src/main/java/org/example/doansummer2026/service/BhxhIntegrationService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/InvoiceControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/InvoiceControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/InvoiceServiceTest.java](../../src/test/java/org/example/doansummer2026/service/InvoiceServiceTest.java)

## CA-08 Accept CareS Card Payment

Vietnamese: Thu tiền bằng thẻ CareS. Actors: Cashier, Clinic Manager. Delivery: UI + backend.

Validate the card and PIN, optionally apply eligible card benefits and pay the remaining invoice amount. Benefits apply to the eligible patient-payable amount, not a second deduction of insurer coverage.

**Registered routes:** `/cashier/invoices/:id`

**Frontend evidence:** [src/pages/cashier/InvoiceDetailPage.jsx](../../../untitled/src/pages/cashier/InvoiceDetailPage.jsx); [src/hooks/useInvoiceDetail.js](../../../untitled/src/hooks/useInvoiceDetail.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/membership-cards/pay-at-counter | [MembershipCardController.payAtCounter](../../src/main/java/org/example/doansummer2026/controller/MembershipCardController.java) line 58 | [MembershipCardService.payAtCounter](../../src/main/java/org/example/doansummer2026/service/MembershipCardService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/MembershipCardServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MembershipCardServiceTest.java)

## CA-09 Top Up CareS Card

Vietnamese: Nạp tiền thẻ CareS. Actors: Cashier, Clinic Manager. Delivery: UI + backend.

Record an eligible card top-up at the counter, with the required amount and idempotency key, and provide the resulting top-up receipt.

**Registered routes:** `/cashier/membership-top-up`

**Frontend evidence:** [src/pages/cashier/MembershipTopUpPage.jsx](../../../untitled/src/pages/cashier/MembershipTopUpPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/membership-cards/{cardCode}/top-up | [MembershipCardController.topUp](../../src/main/java/org/example/doansummer2026/controller/MembershipCardController.java) line 50 | [MembershipCardService.topUp](../../src/main/java/org/example/doansummer2026/service/MembershipCardService.java); [AuthService.currentStaffId](../../src/main/java/org/example/doansummer2026/service/AuthService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/AuthServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AuthServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/MembershipCardServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MembershipCardServiceTest.java)

## CA-10 Review and Print Top-up History

Vietnamese: Xem và in lịch sử nạp thẻ. Actors: Cashier, Clinic Manager. Delivery: UI + backend.

Browse recorded CareS top-ups and open their receipt representation. The top-up receipt is distinct from an examination invoice receipt.

**Registered routes:** `/cashier/membership-top-up/history`

**Frontend evidence:** [src/pages/cashier/MembershipTopUpHistoryPage.jsx](../../../untitled/src/pages/cashier/MembershipTopUpHistoryPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/membership-cards/top-ups | [MembershipCardController.topUpHistory](../../src/main/java/org/example/doansummer2026/controller/MembershipCardController.java) line 82 | [MembershipCardService.topUpHistory](../../src/main/java/org/example/doansummer2026/service/MembershipCardService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/MembershipCardServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MembershipCardServiceTest.java)

## CA-11 Cancel an Eligible Invoice

Vietnamese: Hủy hóa đơn đủ điều kiện. Actors: Cashier, Clinic Manager. Delivery: UI + backend.

Cancel an invoice only when its payment and service state allow it. This is not a general refund action for already delivered services.

**Registered routes:** `/cashier/invoices/:id`

**Frontend evidence:** [src/pages/cashier/InvoiceDetailPage.jsx](../../../untitled/src/pages/cashier/InvoiceDetailPage.jsx); [src/hooks/useInvoiceDetail.js](../../../untitled/src/hooks/useInvoiceDetail.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/invoices/{id}/cancel | [InvoiceController.cancel](../../src/main/java/org/example/doansummer2026/controller/InvoiceController.java) line 104 | [InvoiceService.cancel](../../src/main/java/org/example/doansummer2026/service/InvoiceService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/InvoiceControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/InvoiceControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/InvoiceServiceTest.java](../../src/test/java/org/example/doansummer2026/service/InvoiceServiceTest.java)

## CA-12 Create PayOS Payment Link

Vietnamese: Tạo link thanh toán PayOS. Actors: Cashier, Clinic Manager. Delivery: UI + backend.

Create and open a payment link from the cashier invoice screen. The current Customer frontend does not initiate this endpoint.

**Registered routes:** `/cashier/invoices/:id`

**Frontend evidence:** [src/pages/cashier/InvoiceDetailPage.jsx](../../../untitled/src/pages/cashier/InvoiceDetailPage.jsx); [src/hooks/useInvoiceDetail.js](../../../untitled/src/hooks/useInvoiceDetail.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/invoices/{id}/payos | [InvoiceController.payosMock](../../src/main/java/org/example/doansummer2026/controller/InvoiceController.java) line 121 | [PayOSService.createPaymentLink](../../src/main/java/org/example/doansummer2026/service/PayOSService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/InvoiceControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/InvoiceControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/PayOSServiceTest.java](../../src/test/java/org/example/doansummer2026/service/PayOSServiceTest.java)

## NU-01 View Assigned Queue

Vietnamese: Xem hàng chờ được phân công. Actors: Nurse. Delivery: UI + backend.

View the assigned room, current patient and waiting patients, including blocked and absent states. Clinical-room access remains subject to backend assignment checks.

**Registered routes:** `/doctor/rooms`; `/doctor/departments/:departmentId`

**Frontend evidence:** [src/pages/doctor/DoctorDepartmentPage.jsx](../../../untitled/src/pages/doctor/DoctorDepartmentPage.jsx); [src/pages/doctor/RoomListPage.jsx](../../../untitled/src/pages/doctor/RoomListPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/queue-tickets/waiting/{departmentId} | [QueueTicketController.getWaiting](../../src/main/java/org/example/doansummer2026/controller/QueueTicketController.java) line 175 | [QueueTicketService.getWaitingByDepartment](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |
| GET /api/v1/queue-tickets/in-progress/{departmentId} | [QueueTicketController.getInprogress](../../src/main/java/org/example/doansummer2026/controller/QueueTicketController.java) line 148 | [QueueTicketService.getInprogressByDepartment](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |
| GET /api/v1/departments/my-department | [DepartmentController.getMyDepartment](../../src/main/java/org/example/doansummer2026/controller/DepartmentController.java) line 139 | [DepartmentService.getMyDepartment](../../src/main/java/org/example/doansummer2026/service/DepartmentService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/QueueTicketControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/QueueTicketControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/DepartmentServiceTest.java](../../src/test/java/org/example/doansummer2026/service/DepartmentServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java](../../src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java)

## NU-02 Call or Mark Patient Absent

Vietnamese: Gọi hoặc đánh dấu vắng. Actors: Nurse. Delivery: UI + backend.

Call or recall the permitted patient and mark absence through room queue actions. Calling order and patient availability are enforced by the backend.

**Registered routes:** `/doctor/departments/:departmentId`

**Frontend evidence:** [src/pages/doctor/DoctorDepartmentPage.jsx](../../../untitled/src/pages/doctor/DoctorDepartmentPage.jsx); [src/hooks/useQueueActions.js](../../../untitled/src/hooks/useQueueActions.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/queue-tickets/{id}/call | [QueueTicketController.call](../../src/main/java/org/example/doansummer2026/controller/QueueTicketController.java) line 76 | [QueueTicketService.call](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |
| POST /api/v1/queue-tickets/{id}/skip | [QueueTicketController.skip](../../src/main/java/org/example/doansummer2026/controller/QueueTicketController.java) line 121 | [QueueTicketService.skip](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/QueueTicketControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/QueueTicketControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java](../../src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java)

## NU-03 Receive and Prepare Patient

Vietnamese: Tiếp nhận và chuẩn bị bệnh nhân. Actors: Nurse. Delivery: UI + backend.

Confirm arrival in the permitted room workflow and prepare the patient. Starting an examination requires a suitable on-duty treating doctor; the nurse does not become the treating doctor.

**Registered routes:** `/doctor/departments/:departmentId`

**Frontend evidence:** [src/pages/doctor/DoctorDepartmentPage.jsx](../../../untitled/src/pages/doctor/DoctorDepartmentPage.jsx); [src/hooks/useQueueActions.js](../../../untitled/src/hooks/useQueueActions.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/queue-tickets/{id}/start-exam | [QueueTicketController.startExam](../../src/main/java/org/example/doansummer2026/controller/QueueTicketController.java) line 83 | [QueueTicketService.startExam](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/QueueTicketControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/QueueTicketControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java](../../src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java)

## NU-04 Record Vital Signs

Vietnamese: Ghi dấu hiệu sinh tồn. Actors: Nurse, Doctor. Delivery: UI + backend.

Record or update vital signs for the active examination. Later examination records in the same visit can receive independent copies of valid source measurements; editing one record does not edit every copy.

**Registered routes:** `/doctor/examinations/department/:departmentId`

**Frontend evidence:** [src/pages/doctor/ExaminationPage.jsx](../../../untitled/src/pages/doctor/ExaminationPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| PUT /api/doctor/examinations/{id} | [DoctorExaminationController.draft](../../src/main/java/org/example/doansummer2026/controller/DoctorExaminationController.java) line 59 | [QueueTicketService.saveExaminationDraft](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |
| POST /api/v1/medical-records/{id}/draft | [MedicalRecordController.saveDraft](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 126 | [MedicalRecordService.saveDraft](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |

**Additional classified endpoints:** `MedicalRecordController.update`, `VitalSignsController.get`, `VitalSignsController.create`, `VitalSignsController.update`, `VitalSignsController.delete`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java](../../src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java)

## NU-05 View Relevant Patient Information

Vietnamese: Xem thông tin phục vụ chăm sóc. Actors: Nurse. Delivery: UI + backend.

Review patient details and relevant history available in the assigned clinical workflow. Customer and family authorization rules are separate from staff access rules.

**Registered routes:** `/doctor/examinations/department/:departmentId`

**Frontend evidence:** [src/pages/doctor/ExaminationPage.jsx](../../../untitled/src/pages/doctor/ExaminationPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/doctor/examinations/{id} | [DoctorExaminationController.load](../../src/main/java/org/example/doansummer2026/controller/DoctorExaminationController.java) line 40 | [QueueTicketService.loadExamination](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |
| GET /api/v1/medical-records/{id}/previous-history | [MedicalRecordController.previousHistory](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 99 | [MedicalRecordService.getPreviousHistoryForDoctor](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |
| GET /api/v1/medical-records/{id}/patient-allergies | [MedicalRecordController.patientAllergies](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 76 | [MedicalRecordService.getPatientAllergies](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |

**Additional classified endpoints:** `ProfileController.get`, `ProfileController.search`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java](../../src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java)

## NU-06 Save Nursing Preparation Notes

Vietnamese: Lưu ghi nhận hỗ trợ khám. Actors: Nurse. Delivery: UI + backend.

Save permitted complaint, clinical preparation notes and vital signs in a nursing draft. This does not authorize prescribing, final diagnosis, diagnostic ordering or medical-record completion.

**Registered routes:** `/doctor/examinations/department/:departmentId`

**Frontend evidence:** [src/pages/doctor/ExaminationPage.jsx](../../../untitled/src/pages/doctor/ExaminationPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| PUT /api/doctor/examinations/{id} | [DoctorExaminationController.draft](../../src/main/java/org/example/doansummer2026/controller/DoctorExaminationController.java) line 59 | [QueueTicketService.saveExaminationDraft](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |
| POST /api/v1/medical-records/{id}/draft | [MedicalRecordController.saveDraft](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 126 | [MedicalRecordService.saveDraft](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java](../../src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java)

## NU-07 Restore Eligible Absent Patient at Room

Vietnamese: Đưa khách vắng trở lại hàng chờ phòng. Actors: Nurse, Doctor. Delivery: UI + backend.

Use the existing room return action for an eligible same-day absent ticket. This direct room action is separate from the Customer or Guest request that reception confirms. It cannot restore an expired old-day ticket.

**Registered routes:** `/doctor/lab/:departmentId/call`

**Frontend evidence:** [src/hooks/useQueueActions.js](../../../untitled/src/hooks/useQueueActions.js); [src/pages/lab/LabCallQueuePage.jsx](../../../untitled/src/pages/lab/LabCallQueuePage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/queue-tickets/{id}/return | [QueueTicketController.returnToQueue](../../src/main/java/org/example/doansummer2026/controller/QueueTicketController.java) line 128 | [QueueTicketService.returnToQueue](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/QueueTicketControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/QueueTicketControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java](../../src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java)

## DR-01 View Examination Queue

Vietnamese: Xem hàng chờ khám bệnh. Actors: Doctor. Delivery: UI + backend.

Review assigned examination-room patients, service order and current work. Display position is calculated separately from the fixed ticket number.

**Registered routes:** `/doctor/rooms`; `/doctor/departments/:departmentId`

**Frontend evidence:** [src/pages/doctor/DoctorDepartmentPage.jsx](../../../untitled/src/pages/doctor/DoctorDepartmentPage.jsx); [src/pages/doctor/RoomListPage.jsx](../../../untitled/src/pages/doctor/RoomListPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/queue-tickets/waiting/{departmentId} | [QueueTicketController.getWaiting](../../src/main/java/org/example/doansummer2026/controller/QueueTicketController.java) line 175 | [QueueTicketService.getWaitingByDepartment](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |
| GET /api/v1/queue-tickets/in-progress/{departmentId} | [QueueTicketController.getInprogress](../../src/main/java/org/example/doansummer2026/controller/QueueTicketController.java) line 148 | [QueueTicketService.getInprogressByDepartment](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |
| GET /api/v1/queue-tickets/{id}/same-room-chain | [QueueTicketController.sameRoomChain](../../src/main/java/org/example/doansummer2026/controller/QueueTicketController.java) line 108 | [QueueTicketService.sameRoomChain](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |
| GET /api/v1/departments/my-department | [DepartmentController.getMyDepartment](../../src/main/java/org/example/doansummer2026/controller/DepartmentController.java) line 139 | [DepartmentService.getMyDepartment](../../src/main/java/org/example/doansummer2026/service/DepartmentService.java) |

**Additional classified endpoints:** `DepartmentController.list`, `DepartmentController.get`, `QueueTicketController.get`, `QueueTicketController.getAllInprogress`, `QueueTicketController.getWaitingForTest`, `QueueTicketController.getTestDone`, `QueueTicketController.getQueue`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/QueueTicketControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/QueueTicketControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/DepartmentServiceTest.java](../../src/test/java/org/example/doansummer2026/service/DepartmentServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java](../../src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java)

## DR-02 Call and Receive Patient

Vietnamese: Gọi và tiếp nhận bệnh nhân. Actors: Doctor. Delivery: UI + backend.

Call or recall the permitted patient, mark absence or begin the service with an eligible on-duty doctor. Room and visit checks prevent bypassing active work.

**Registered routes:** `/doctor/departments/:departmentId`

**Frontend evidence:** [src/pages/doctor/DoctorDepartmentPage.jsx](../../../untitled/src/pages/doctor/DoctorDepartmentPage.jsx); [src/hooks/useQueueActions.js](../../../untitled/src/hooks/useQueueActions.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/queue-tickets/{id}/call | [QueueTicketController.call](../../src/main/java/org/example/doansummer2026/controller/QueueTicketController.java) line 76 | [QueueTicketService.call](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |
| POST /api/v1/queue-tickets/{id}/skip | [QueueTicketController.skip](../../src/main/java/org/example/doansummer2026/controller/QueueTicketController.java) line 121 | [QueueTicketService.skip](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |
| POST /api/v1/queue-tickets/{id}/start-exam | [QueueTicketController.startExam](../../src/main/java/org/example/doansummer2026/controller/QueueTicketController.java) line 83 | [QueueTicketService.startExam](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/QueueTicketControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/QueueTicketControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java](../../src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java)

## DR-03 View Medical Records and History

Vietnamese: Xem bệnh án và tiền sử. Actors: Doctor. Delivery: UI + backend.

Read the current patient history, allergies and available previous examination records before making clinical decisions. Access and publication differ from the Customer view.

**Registered routes:** `/doctor/examinations/department/:departmentId`

**Frontend evidence:** [src/pages/doctor/ExaminationPage.jsx](../../../untitled/src/pages/doctor/ExaminationPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/doctor/examinations/{id} | [DoctorExaminationController.load](../../src/main/java/org/example/doansummer2026/controller/DoctorExaminationController.java) line 40 | [QueueTicketService.loadExamination](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |
| GET /api/v1/medical-records/{id}/previous-history | [MedicalRecordController.previousHistory](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 99 | [MedicalRecordService.getPreviousHistoryForDoctor](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |
| GET /api/v1/medical-records/{id}/visit-detail | [MedicalRecordController.staffVisitDetail](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 83 | [MedicalRecordService.getVisitDetailForStaff](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |

**Additional classified endpoints:** `MedicalRecordController.list`, `MedicalRecordController.create`, `MedicalRecordController.delete`, `MedicalRecordController.rate`, `ProfileController.get`, `ProfileController.search`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java](../../src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java)

## DR-04 Record Medical Examination

Vietnamese: Ghi nhận khám lâm sàng. Actors: Doctor. Delivery: UI + backend.

Record symptoms and clinical findings for the current examination service. Each service has its own medical record, rather than completing every service in the visit together.

**Registered routes:** `/doctor/examinations/department/:departmentId`

**Frontend evidence:** [src/pages/doctor/ExaminationPage.jsx](../../../untitled/src/pages/doctor/ExaminationPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| PUT /api/doctor/examinations/{id} | [DoctorExaminationController.draft](../../src/main/java/org/example/doansummer2026/controller/DoctorExaminationController.java) line 59 | [QueueTicketService.saveExaminationDraft](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |

**Additional classified endpoints:** `MedicalRecordController.clinicalForm`, `MedicalRecordController.update`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java](../../src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java)

## DR-05 Record Diagnosis and Treatment Plan

Vietnamese: Ghi chẩn đoán và hướng điều trị. Actors: Doctor. Delivery: UI + backend.

Select ICD-10 reference codes and record diagnosis, conclusion, treatment direction and patient instructions in the responsible doctor medical record.

**Registered routes:** `/doctor/examinations/department/:departmentId`

**Frontend evidence:** [src/pages/doctor/ExaminationPage.jsx](../../../untitled/src/pages/doctor/ExaminationPage.jsx); [src/hooks/useDiagnosis.js](../../../untitled/src/hooks/useDiagnosis.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| PUT /api/doctor/examinations/{id} | [DoctorExaminationController.draft](../../src/main/java/org/example/doansummer2026/controller/DoctorExaminationController.java) line 59 | [QueueTicketService.saveExaminationDraft](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |
| GET /api/v1/icd10-codes | [Icd10CodeController.list](../../src/main/java/org/example/doansummer2026/controller/Icd10CodeController.java) line 35 | [Icd10CodeService.search](../../src/main/java/org/example/doansummer2026/service/Icd10CodeService.java) |

**Additional classified endpoints:** `Icd10CodeController.get`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java](../../src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java)

## DR-06 Create Prescription

Vietnamese: Kê đơn thuốc. Actors: Doctor. Delivery: UI + backend.

Enter medication, quantity and usage instructions for the current record, using the available medicine lookup. Prescription validation includes the required allergy-verification state.

**Registered routes:** `/doctor/examinations/department/:departmentId`; `/doctor/prescriptions/:recordId/preview`

**Frontend evidence:** [src/pages/doctor/ExaminationPage.jsx](../../../untitled/src/pages/doctor/ExaminationPage.jsx); [src/pages/doctor/PrescriptionPreviewPage.jsx](../../../untitled/src/pages/doctor/PrescriptionPreviewPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| PUT /api/doctor/examinations/{id} | [DoctorExaminationController.draft](../../src/main/java/org/example/doansummer2026/controller/DoctorExaminationController.java) line 59 | [QueueTicketService.saveExaminationDraft](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |
| GET /api/v1/medicines | [MedicineCatalogController.search](../../src/main/java/org/example/doansummer2026/controller/MedicineCatalogController.java) line 17 | [MedicineCatalogRepository.searchActive](../../src/main/java/org/example/doansummer2026/repository/MedicineCatalogRepository.java) |
| GET /api/v1/medical-records/{id} | [MedicalRecordController.get](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 63 | [MedicalRecordService.get](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java](../../src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java)

## DR-07 Request Diagnostic Services

Vietnamese: Chỉ định cận lâm sàng. Actors: Doctor. Delivery: UI + backend.

Choose diagnostic services, complete panels or individual laboratory analytes and confirm the order. The backend normalizes selection and billing; this use case does not include referral to another examination specialty.

**Registered routes:** `/doctor/examinations/department/:departmentId`

**Frontend evidence:** [src/pages/doctor/ExaminationPage.jsx](../../../untitled/src/pages/doctor/ExaminationPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/medical-services/resolve-selection | [MedicalServiceController.resolveSelection](../../src/main/java/org/example/doansummer2026/controller/MedicalServiceController.java) line 66 | [MedicalServiceSelectionPolicyService.resolveResponse](../../src/main/java/org/example/doansummer2026/service/MedicalServiceSelectionPolicyService.java) |
| POST /api/doctor/examinations/{id}/complete | [DoctorExaminationController.complete](../../src/main/java/org/example/doansummer2026/controller/DoctorExaminationController.java) line 75 | [QueueTicketService.completeExamination](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |
| POST /api/v1/queue-tickets/{id}/complete-transition | [QueueTicketController.completeTransition](../../src/main/java/org/example/doansummer2026/controller/QueueTicketController.java) line 98 | [QueueTicketService.completeAndTransition](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |

**Additional classified endpoints:** `TestRequestController.listByVisit`, `TestRequestController.create`, `TestRequestController.createBatch`, `TestRequestController.update`, `TestRequestController.delete`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/QueueTicketControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/QueueTicketControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/MedicalServiceSelectionPolicyServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MedicalServiceSelectionPolicyServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java](../../src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java)

## DR-09 Review Diagnostic Results

Vietnamese: Xem kết quả cận lâm sàng. Actors: Doctor. Delivery: UI + backend.

Open read-only result or status dialogs from the examination screen without navigating to the Lab workbench. Completed results expose the supported values, conclusions and authorized files; unfinished requests show progress rather than editable result fields.

**Registered routes:** `/doctor/examinations/department/:departmentId`

**Frontend evidence:** [src/pages/doctor/ExaminationPage.jsx](../../../untitled/src/pages/doctor/ExaminationPage.jsx); [src/components/clinical/LabResultReviewModal.jsx](../../../untitled/src/components/clinical/LabResultReviewModal.jsx); [src/components/clinical/LabPanelResultReviewModal.jsx](../../../untitled/src/components/clinical/LabPanelResultReviewModal.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/test-requests/{id:[0-9a-fA-F-]+} | [TestRequestController.get](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 76 | [TestRequestService.get](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |
| GET /api/v1/test-requests/{id}/panel-workbench | [TestRequestController.panelWorkbench](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 82 | [TestRequestService.getPanelWorkbench](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |
| GET /api/v1/test-requests/{id}/result | [TestRequestController.getResult](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 201 | [TestRequestService.getResult](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |
| GET /api/v1/test-requests/{id}/clinical-form | [TestRequestController.getClinicalForm](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 207 | [TestRequestService.getClinicalForm](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |
| GET /api/v1/test-results/{resultId}/file | [TestResultFileController.viewFile](../../src/main/java/org/example/doansummer2026/controller/TestResultFileController.java) line 45 | [AuthService.currentAccount](../../src/main/java/org/example/doansummer2026/service/AuthService.java) |
| GET /api/v1/test-results/attachments/{attachmentId}/file | [TestResultFileController.viewAttachment](../../src/main/java/org/example/doansummer2026/controller/TestResultFileController.java) line 81 | [AuthService.currentAccount](../../src/main/java/org/example/doansummer2026/service/AuthService.java) |

**Additional classified endpoints:** `TestRequestController.listByVisit`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/TestRequestControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/TestRequestControllerTest.java)
- [src/test/java/org/example/doansummer2026/controller/TestResultFileControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/TestResultFileControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/AuthServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AuthServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/TestRequestServiceTest.java](../../src/test/java/org/example/doansummer2026/service/TestRequestServiceTest.java)

## DR-10 Save Medical Record as Draft

Vietnamese: Lưu nháp bệnh án. Actors: Doctor. Delivery: UI + backend.

Save incomplete examination content in the responsible doctor record. Version checks reject stale updates, and completed records cannot be rewritten through the normal draft flow.

**Registered routes:** `/doctor/examinations/department/:departmentId`

**Frontend evidence:** [src/pages/doctor/ExaminationPage.jsx](../../../untitled/src/pages/doctor/ExaminationPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| PUT /api/doctor/examinations/{id} | [DoctorExaminationController.draft](../../src/main/java/org/example/doansummer2026/controller/DoctorExaminationController.java) line 59 | [QueueTicketService.saveExaminationDraft](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |

**Additional classified endpoints:** `MedicalRecordController.list`, `MedicalRecordController.create`, `MedicalRecordController.delete`, `MedicalRecordController.rate`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java](../../src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java)

## DR-11 Resume Examination after Diagnostic Services

Vietnamese: Khám lại sau cận lâm sàng. Actors: Doctor. Delivery: UI + backend.

Resume the source examination when its required diagnostic work is complete, review results and continue the same record. Later examination services remain dependent on completion of this record.

**Registered routes:** `/doctor/departments/:departmentId`; `/doctor/examinations/department/:departmentId`

**Frontend evidence:** [src/pages/doctor/DoctorDepartmentPage.jsx](../../../untitled/src/pages/doctor/DoctorDepartmentPage.jsx); [src/pages/doctor/ExaminationPage.jsx](../../../untitled/src/pages/doctor/ExaminationPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/queue-tickets/{id}/call | [QueueTicketController.call](../../src/main/java/org/example/doansummer2026/controller/QueueTicketController.java) line 76 | [QueueTicketService.call](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |
| POST /api/v1/queue-tickets/{id}/start-exam | [QueueTicketController.startExam](../../src/main/java/org/example/doansummer2026/controller/QueueTicketController.java) line 83 | [QueueTicketService.startExam](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |
| GET /api/doctor/examinations/{id} | [DoctorExaminationController.load](../../src/main/java/org/example/doansummer2026/controller/DoctorExaminationController.java) line 40 | [QueueTicketService.loadExamination](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |

**Additional classified endpoints:** `QueueTicketController.markTestDone`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/QueueTicketControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/QueueTicketControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java](../../src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java)

## DR-12 Complete Medical Record

Vietnamese: Hoàn thành bệnh án. Actors: Doctor. Delivery: UI + backend.

Confirm completion of the current examination record. When diagnostic work is still required, the order workflow waits for results instead of closing the record; otherwise it can release the next eligible service.

**Registered routes:** `/doctor/examinations/department/:departmentId`; `/doctor/examinations/:recordId/completed`

**Frontend evidence:** [src/pages/doctor/ExaminationPage.jsx](../../../untitled/src/pages/doctor/ExaminationPage.jsx); [src/pages/doctor/ExamCompletionPage.jsx](../../../untitled/src/pages/doctor/ExamCompletionPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/doctor/examinations/{id}/complete | [DoctorExaminationController.complete](../../src/main/java/org/example/doansummer2026/controller/DoctorExaminationController.java) line 75 | [QueueTicketService.completeExamination](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |
| POST /api/v1/queue-tickets/{id}/complete-transition | [QueueTicketController.completeTransition](../../src/main/java/org/example/doansummer2026/controller/QueueTicketController.java) line 98 | [QueueTicketService.completeAndTransition](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |

**Additional classified endpoints:** `MedicalRecordController.complete`, `QueueTicketController.complete`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/QueueTicketControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/QueueTicketControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java](../../src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java)

## DR-13 Arrange Follow-up from Examination

Vietnamese: Thiết lập tái khám từ màn khám. Actors: Doctor. Delivery: UI + backend.

Enter the follow-up date, service and advice, review the confirmation dialog and create the linked follow-up appointment through the examination screen. Follow-up fields also travel with examination draft/completion. This direct booking action coexists with the separate reception follow-up workflow; it is not a specialist referral.

**Registered routes:** `/doctor/examinations/department/:departmentId`

**Frontend evidence:** [src/pages/doctor/ExaminationPage.jsx](../../../untitled/src/pages/doctor/ExaminationPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/medical-records/{id}/follow-up-appointment | [MedicalRecordController.createFollowUpAppointment](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 143 | [MedicalRecordService.scheduleFollowUp](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |
| PUT /api/doctor/examinations/{id} | [DoctorExaminationController.draft](../../src/main/java/org/example/doansummer2026/controller/DoctorExaminationController.java) line 59 | [QueueTicketService.saveExaminationDraft](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |
| POST /api/doctor/examinations/{id}/complete | [DoctorExaminationController.complete](../../src/main/java/org/example/doansummer2026/controller/DoctorExaminationController.java) line 75 | [QueueTicketService.completeExamination](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java](../../src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java)

## DR-14 View Same-day Clinical Information

Vietnamese: Xem thông tin chuyên môn cùng ngày. Actors: Doctor. Delivery: UI + backend.

Review other completed examination information and signed same-day diagnostic results where authorized. Reused reference results are not newly billed or performed results in the current visit.

**Registered routes:** `/doctor/examinations/department/:departmentId`

**Frontend evidence:** [src/pages/doctor/ExaminationPage.jsx](../../../untitled/src/pages/doctor/ExaminationPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/doctor/examinations/{id}/same-day-paraclinical-results | [DoctorExaminationController.sameDayResults](../../src/main/java/org/example/doansummer2026/controller/DoctorExaminationController.java) line 47 | [QueueTicketService.loadExamination](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java); [SameDayParaclinicalResultService.findForRecord](../../src/main/java/org/example/doansummer2026/service/SameDayParaclinicalResultService.java) |
| GET /api/v1/medical-records/{id}/visit-detail | [MedicalRecordController.staffVisitDetail](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 83 | [MedicalRecordService.getVisitDetailForStaff](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java](../../src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/SameDayParaclinicalResultServiceTest.java](../../src/test/java/org/example/doansummer2026/service/SameDayParaclinicalResultServiceTest.java)

## DR-15 Verify Patient Allergies

Vietnamese: Xác minh dị ứng. Actors: Doctor, Nurse. Delivery: UI + backend.

Review and update the supported allergy assessment for the patient in the clinical context, including verified absence of known allergies or a recorded allergy history.

**Registered routes:** `/doctor/examinations/department/:departmentId`

**Frontend evidence:** [src/pages/doctor/ExaminationPage.jsx](../../../untitled/src/pages/doctor/ExaminationPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/medical-records/{id}/patient-allergies | [MedicalRecordController.patientAllergies](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 76 | [MedicalRecordService.getPatientAllergies](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |
| PUT /api/v1/medical-records/{id}/patient-allergies | [MedicalRecordController.updatePatientAllergies](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 89 | [MedicalRecordService.updatePatientAllergies](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java)

## DR-16 Continue Next Examination in the Same Room

Vietnamese: Tiếp tục dịch vụ khám cùng phòng. Actors: Doctor. Delivery: UI + backend.

After confirming one service, continue the next eligible same-room examination using a separate record. A pending diagnostic cycle prevents premature completion of the chain.

**Registered routes:** `/doctor/examinations/department/:departmentId`

**Frontend evidence:** [src/pages/doctor/ExaminationPage.jsx](../../../untitled/src/pages/doctor/ExaminationPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/queue-tickets/{id}/complete-transition | [QueueTicketController.completeTransition](../../src/main/java/org/example/doansummer2026/controller/QueueTicketController.java) line 98 | [QueueTicketService.completeAndTransition](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |
| GET /api/v1/queue-tickets/{id}/same-room-chain | [QueueTicketController.sameRoomChain](../../src/main/java/org/example/doansummer2026/controller/QueueTicketController.java) line 108 | [QueueTicketService.sameRoomChain](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/QueueTicketControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/QueueTicketControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java](../../src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java)

## DR-17 Finish Carried-over Clinical Work

Vietnamese: Xử lý hồ sơ chuyên môn tồn đọng. Actors: Doctor. Delivery: UI + backend.

Resume eligible clinical work already started before the current day and finish it under current operational checks. This does not reactivate old unstarted services cancelled or skipped at day close.

**Registered routes:** `/doctor/departments/:departmentId`; `/doctor/examinations/department/:departmentId`

**Frontend evidence:** [src/pages/doctor/DoctorDepartmentPage.jsx](../../../untitled/src/pages/doctor/DoctorDepartmentPage.jsx); [src/pages/doctor/ExaminationPage.jsx](../../../untitled/src/pages/doctor/ExaminationPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/queue-tickets | [QueueTicketController.list](../../src/main/java/org/example/doansummer2026/controller/QueueTicketController.java) line 44 | [QueueTicketService.search](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |
| POST /api/v1/queue-tickets/{id}/call | [QueueTicketController.call](../../src/main/java/org/example/doansummer2026/controller/QueueTicketController.java) line 76 | [QueueTicketService.call](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |
| POST /api/doctor/examinations/{id}/complete | [DoctorExaminationController.complete](../../src/main/java/org/example/doansummer2026/controller/DoctorExaminationController.java) line 75 | [QueueTicketService.completeExamination](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/QueueTicketControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/QueueTicketControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java](../../src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java)

## DS-01 View Diagnostic Queue and Panels

Vietnamese: Xem hàng chờ và phiếu CLS. Actors: Doctor, Nurse. Delivery: UI + backend.

Browse requests for the assigned diagnostic room. Individually purchased analytes are grouped by their source panel and execution context; distinct panels remain separate.

**Registered routes:** `/doctor/lab/:departmentId`; `/doctor/lab/:departmentId/call`

**Frontend evidence:** [src/pages/lab/LabRequestListPage.jsx](../../../untitled/src/pages/lab/LabRequestListPage.jsx); [src/pages/lab/LabCallQueuePage.jsx](../../../untitled/src/pages/lab/LabCallQueuePage.jsx); [src/hooks/useLabQueue.js](../../../untitled/src/hooks/useLabQueue.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/test-requests/panels | [TestRequestController.panels](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 62 | [TestRequestService.searchPanels](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |
| GET /api/v1/queue-tickets/waiting/{departmentId} | [QueueTicketController.getWaiting](../../src/main/java/org/example/doansummer2026/controller/QueueTicketController.java) line 175 | [QueueTicketService.getWaitingByDepartment](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |

**Additional classified endpoints:** `QueueTicketController.get`, `QueueTicketController.getAllInprogress`, `QueueTicketController.getWaitingForTest`, `QueueTicketController.getTestDone`, `QueueTicketController.getQueue`, `TestRequestController.list`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/QueueTicketControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/QueueTicketControllerTest.java)
- [src/test/java/org/example/doansummer2026/controller/TestRequestControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/TestRequestControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java](../../src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/TestRequestServiceTest.java](../../src/test/java/org/example/doansummer2026/service/TestRequestServiceTest.java)

## DS-02 Call and Receive Diagnostic Patient

Vietnamese: Gọi và tiếp nhận tại phòng CLS. Actors: Doctor, Nurse. Delivery: UI + backend.

Call the next eligible patient and confirm the start of diagnostic work in the assigned room. Payment and preceding journey steps determine whether a request is ready.

**Registered routes:** `/doctor/lab/:departmentId/call`

**Frontend evidence:** [src/pages/lab/LabCallQueuePage.jsx](../../../untitled/src/pages/lab/LabCallQueuePage.jsx); [src/hooks/useQueueActions.js](../../../untitled/src/hooks/useQueueActions.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/queue-tickets/{id}/call | [QueueTicketController.call](../../src/main/java/org/example/doansummer2026/controller/QueueTicketController.java) line 76 | [QueueTicketService.call](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |
| POST /api/v1/queue-tickets/{id}/start-exam | [QueueTicketController.startExam](../../src/main/java/org/example/doansummer2026/controller/QueueTicketController.java) line 83 | [QueueTicketService.startExam](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/QueueTicketControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/QueueTicketControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java](../../src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java)

## DS-03 View Diagnostic Request

Vietnamese: Xem chỉ định CLS. Actors: Doctor, Nurse. Delivery: UI + backend.

Inspect the diagnostic request or panel, source record, service and current action permissions. A panel workbench shows which analytes were purchased and which are locked.

**Registered routes:** `/lab/:id`

**Frontend evidence:** [src/pages/lab/LabDetailPage.jsx](../../../untitled/src/pages/lab/LabDetailPage.jsx); [src/hooks/useLabDetail.js](../../../untitled/src/hooks/useLabDetail.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/test-requests/{id:[0-9a-fA-F-]+} | [TestRequestController.get](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 76 | [TestRequestService.get](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |
| GET /api/v1/test-requests/{id}/panel-workbench | [TestRequestController.panelWorkbench](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 82 | [TestRequestService.getPanelWorkbench](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |
| GET /api/v1/test-requests/{id}/action-permissions | [TestRequestController.actionPermissions](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 105 | [TestRequestService.actionPermissions](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |
| GET /api/v1/test-requests/{id}/clinical-form | [TestRequestController.getClinicalForm](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 207 | [TestRequestService.getClinicalForm](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |

**Additional classified endpoints:** `TestRequestController.listByQueue`, `TestRequestController.create`, `TestRequestController.createBatch`, `TestRequestController.update`, `TestRequestController.delete`, `TestRequestController.findByInvoiceItem`, `TestRequestController.findByInvoice`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/TestRequestControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/TestRequestControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/TestRequestServiceTest.java](../../src/test/java/org/example/doansummer2026/service/TestRequestServiceTest.java)

## DS-04 Record Specimen Information

Vietnamese: Ghi thông tin mẫu bệnh phẩm. Actors: Doctor, Nurse. Delivery: UI + backend.

Review generated specimen information and edit the permitted specimen fields where required. Related purchased requests in a panel use consistent specimen details.

**Registered routes:** `/lab/:id`

**Frontend evidence:** [src/pages/lab/LabDetailPage.jsx](../../../untitled/src/pages/lab/LabDetailPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/test-requests/{id}/result | [TestRequestController.createResult](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 245 | [TestRequestService.createResult](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |
| PUT /api/v1/test-requests/{id}/result | [TestRequestController.updateResult](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 254 | [TestRequestService.updateResult](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |
| PUT /api/v1/test-requests/{id}/panel-workbench/result | [TestRequestController.savePanelResult](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 89 | [TestRequestService.savePanelResult](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/TestRequestControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/TestRequestControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/TestRequestServiceTest.java](../../src/test/java/org/example/doansummer2026/service/TestRequestServiceTest.java)

## DS-05 Perform Diagnostic Service

Vietnamese: Thực hiện dịch vụ CLS. Actors: Doctor, Nurse. Delivery: UI + backend.

Work on the diagnostic service already started in the room, using its configured form. Unpurchased analytes remain non-editable; a physical laboratory act alone does not mark a result signed in the system.

**Registered routes:** `/lab/:id`

**Frontend evidence:** [src/pages/lab/LabDetailPage.jsx](../../../untitled/src/pages/lab/LabDetailPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/test-requests/{id}/panel-workbench | [TestRequestController.panelWorkbench](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 82 | [TestRequestService.getPanelWorkbench](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |
| GET /api/v1/test-requests/{id}/action-permissions | [TestRequestController.actionPermissions](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 105 | [TestRequestService.actionPermissions](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |
| GET /api/v1/test-requests/{id}/clinical-form | [TestRequestController.getClinicalForm](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 207 | [TestRequestService.getClinicalForm](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/TestRequestControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/TestRequestControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/TestRequestServiceTest.java](../../src/test/java/org/example/doansummer2026/service/TestRequestServiceTest.java)

## DS-06 Enter Diagnostic Result

Vietnamese: Nhập kết quả CLS. Actors: Doctor, Nurse. Delivery: UI + backend.

Enter values and conclusion for permitted purchased services. Required fields need a valid result or a supported omission reason; partial completion does not automatically refund the invoice.

**Registered routes:** `/lab/:id`

**Frontend evidence:** [src/pages/lab/LabDetailPage.jsx](../../../untitled/src/pages/lab/LabDetailPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/test-requests/{id}/result | [TestRequestController.createResult](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 245 | [TestRequestService.createResult](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |
| PUT /api/v1/test-requests/{id}/result | [TestRequestController.updateResult](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 254 | [TestRequestService.updateResult](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |
| PUT /api/v1/test-requests/{id}/panel-workbench/result | [TestRequestController.savePanelResult](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 89 | [TestRequestService.savePanelResult](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/TestRequestControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/TestRequestControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/TestRequestServiceTest.java](../../src/test/java/org/example/doansummer2026/service/TestRequestServiceTest.java)

## DS-07 Attach Result Files

Vietnamese: Đính kèm tệp kết quả. Actors: Doctor, Nurse. Delivery: UI + backend.

Upload supported result files or revision attachments after execution has begun, under file validation and room permissions. Files remain linked to the request or result revision.

**Registered routes:** `/lab/:id`

**Frontend evidence:** [src/pages/lab/LabDetailPage.jsx](../../../untitled/src/pages/lab/LabDetailPage.jsx); [src/hooks/useLabDetail.js](../../../untitled/src/hooks/useLabDetail.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/test-requests/{id}/upload | [TestRequestController.uploadResult](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 280 | [TestRequestService.uploadResultFile](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |
| POST /api/v1/test-requests/{id}/result/revisions/{revisionId}/attachments | [TestRequestController.uploadAttachments](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 291 | [TestRequestService.uploadAttachments](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |
| GET /api/v1/test-requests/{id}/result/revisions/{revisionId}/attachments | [TestRequestController.listAttachments](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 300 | [TestRequestService.listAttachments](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/TestRequestControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/TestRequestControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/TestRequestServiceTest.java](../../src/test/java/org/example/doansummer2026/service/TestRequestServiceTest.java)

## DS-08 Save Diagnostic Draft

Vietnamese: Lưu nháp kết quả CLS. Actors: Doctor, Nurse. Delivery: UI + backend.

Persist unfinished result data and omission reasons for a request or purchased panel members. Draft saving does not publish the result to the Customer.

**Registered routes:** `/lab/:id`

**Frontend evidence:** [src/pages/lab/LabDetailPage.jsx](../../../untitled/src/pages/lab/LabDetailPage.jsx); [src/hooks/useLabDetail.js](../../../untitled/src/hooks/useLabDetail.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/test-requests/{id}/result | [TestRequestController.createResult](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 245 | [TestRequestService.createResult](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |
| PUT /api/v1/test-requests/{id}/result | [TestRequestController.updateResult](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 254 | [TestRequestService.updateResult](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |
| PUT /api/v1/test-requests/{id}/panel-workbench/result | [TestRequestController.savePanelResult](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 89 | [TestRequestService.savePanelResult](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/TestRequestControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/TestRequestControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/TestRequestServiceTest.java](../../src/test/java/org/example/doansummer2026/service/TestRequestServiceTest.java)

## DS-09 Sign and Publish Diagnostic Result

Vietnamese: Ký và công bố kết quả CLS. Actors: Doctor. Delivery: UI + backend.

Confirm the diagnostic result in the performing room as an eligible on-duty doctor. A Nurse may prepare the draft but cannot sign it. Cancelled requests cannot receive a new signed result.

**Registered routes:** `/lab/:id`

**Frontend evidence:** [src/pages/lab/LabDetailPage.jsx](../../../untitled/src/pages/lab/LabDetailPage.jsx); [src/hooks/useLabDetail.js](../../../untitled/src/hooks/useLabDetail.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/test-requests/{id}/result/complete | [TestRequestController.completeResult](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 266 | [TestRequestService.completeResult](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |
| POST /api/v1/test-requests/{id}/panel-workbench/complete | [TestRequestController.completePanelResult](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 97 | [TestRequestService.savePanelResult](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/TestRequestControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/TestRequestControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/TestRequestServiceTest.java](../../src/test/java/org/example/doansummer2026/service/TestRequestServiceTest.java)

## DS-10 Complete Diagnostic Request

Vietnamese: Hoàn tất yêu cầu CLS. Actors: Doctor. Delivery: UI + backend.

Complete purchased diagnostic requests through the result-confirmation operation. Shared queue completion and return-to-doctor readiness depend on all relevant requests; this is an outcome of signing, not an additional independent completion button.

**Registered routes:** `/lab/:id`

**Frontend evidence:** [src/pages/lab/LabDetailPage.jsx](../../../untitled/src/pages/lab/LabDetailPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/test-requests/{id}/result/complete | [TestRequestController.completeResult](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 266 | [TestRequestService.completeResult](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |
| POST /api/v1/test-requests/{id}/panel-workbench/complete | [TestRequestController.completePanelResult](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 97 | [TestRequestService.savePanelResult](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |

**Additional classified endpoints:** `QueueTicketController.finishParaclinicalService`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/TestRequestControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/TestRequestControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/TestRequestServiceTest.java](../../src/test/java/org/example/doansummer2026/service/TestRequestServiceTest.java)

## DS-11 Handle Diagnostic Absence and Return

Vietnamese: Xử lý vắng và quay lại phòng CLS. Actors: Doctor, Nurse. Delivery: UI + backend.

Mark an eligible called patient absent or use the existing same-day room-return action. Old skipped tickets cannot be restored across days.

**Registered routes:** `/doctor/lab/:departmentId/call`

**Frontend evidence:** [src/pages/lab/LabCallQueuePage.jsx](../../../untitled/src/pages/lab/LabCallQueuePage.jsx); [src/hooks/useQueueActions.js](../../../untitled/src/hooks/useQueueActions.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/queue-tickets/{id}/skip | [QueueTicketController.skip](../../src/main/java/org/example/doansummer2026/controller/QueueTicketController.java) line 121 | [QueueTicketService.skip](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |
| POST /api/v1/queue-tickets/{id}/return | [QueueTicketController.returnToQueue](../../src/main/java/org/example/doansummer2026/controller/QueueTicketController.java) line 128 | [QueueTicketService.returnToQueue](../../src/main/java/org/example/doansummer2026/service/QueueTicketService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/QueueTicketControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/QueueTicketControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java](../../src/test/java/org/example/doansummer2026/service/QueueTicketServiceTest.java)

## DS-12 Cancel an Eligible Diagnostic Request

Vietnamese: Hủy yêu cầu CLS đủ điều kiện. Actors: Doctor. Delivery: UI + backend.

Cancel a request when the performing-room doctor, current duty and request state permit it. This is distinct from refunding a paid service.

**Registered routes:** `/lab/:id`

**Frontend evidence:** [src/pages/lab/LabDetailPage.jsx](../../../untitled/src/pages/lab/LabDetailPage.jsx); [src/hooks/useLabDetail.js](../../../untitled/src/hooks/useLabDetail.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/test-requests/{id}/cancel | [TestRequestController.cancel](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 173 | [TestRequestService.cancel](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/TestRequestControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/TestRequestControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/TestRequestServiceTest.java](../../src/test/java/org/example/doansummer2026/service/TestRequestServiceTest.java)

## DS-13 View Result Revision History

Vietnamese: Xem lịch sử phiên bản kết quả. Actors: Doctor, Nurse. Delivery: UI + backend.

Read result revision history and authorized revision attachments in the diagnostic detail flow. Viewing an old revision does not edit a published result.

**Registered routes:** `/lab/:id`

**Frontend evidence:** [src/pages/lab/LabDetailPage.jsx](../../../untitled/src/pages/lab/LabDetailPage.jsx); [src/hooks/useLabDetail.js](../../../untitled/src/hooks/useLabDetail.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/test-requests/{id}/result/history | [TestRequestController.resultHistory](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 214 | [TestRequestService.resultHistory](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |
| GET /api/v1/test-requests/{id}/result/revisions/{revisionId}/attachments | [TestRequestController.listAttachments](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 300 | [TestRequestService.listAttachments](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |
| GET /api/v1/test-results/attachments/{attachmentId}/file | [TestResultFileController.viewAttachment](../../src/main/java/org/example/doansummer2026/controller/TestResultFileController.java) line 81 | [AuthService.currentAccount](../../src/main/java/org/example/doansummer2026/service/AuthService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/TestRequestControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/TestRequestControllerTest.java)
- [src/test/java/org/example/doansummer2026/controller/TestResultFileControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/TestResultFileControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/AuthServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AuthServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/TestRequestServiceTest.java](../../src/test/java/org/example/doansummer2026/service/TestRequestServiceTest.java)

## DS-14 Amend a Signed Result through API

Vietnamese: Đính chính kết quả qua API. Actors: Doctor. Delivery: Backend only.

Create an amendment, update its draft and sign it under performing-room doctor checks. These endpoints exist, but the current LabDetailPage does not expose an amendment-authoring action.

**Registered routes:** No standalone route; shared component/hook, API-only or integration as indicated.

**Frontend evidence:** No frontend evidence claimed.

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/test-requests/{id}/result/amend | [TestRequestController.amendResult](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 220 | [TestRequestService.amendResult](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |
| PUT /api/v1/test-requests/{id}/result/amend/{revisionId} | [TestRequestController.updateAmendment](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 228 | [TestRequestService.updateAmendment](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |
| POST /api/v1/test-requests/{id}/result/amend/{revisionId}/sign | [TestRequestController.signAmendment](../../src/main/java/org/example/doansummer2026/controller/TestRequestController.java) line 237 | [TestRequestService.signAmendment](../../src/main/java/org/example/doansummer2026/service/TestRequestService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/TestRequestControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/TestRequestControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/TestRequestServiceTest.java](../../src/test/java/org/example/doansummer2026/service/TestRequestServiceTest.java)

## CM-01 View Operational Reports

Vietnamese: Xem thống kê vận hành. Actors: Clinic Manager. Delivery: UI + backend.

Select the reporting period and inspect overview, collections/invoices and room activity. Service and room detail tables use their own search filters and fixed pagination without changing period totals.

**Registered routes:** `/owner/report`

**Frontend evidence:** [src/pages/owner/ReportPage.jsx](../../../untitled/src/pages/owner/ReportPage.jsx); [src/hooks/useReport.js](../../../untitled/src/hooks/useReport.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/reports/dashboard | [ReportController.getDashboard](../../src/main/java/org/example/doansummer2026/controller/ReportController.java) line 37 | [ReportService.getDashboardReport](../../src/main/java/org/example/doansummer2026/service/ReportService.java) |
| GET /api/v1/reports/services | [ReportController.getServices](../../src/main/java/org/example/doansummer2026/controller/ReportController.java) line 49 | [ReportService.getServiceReport](../../src/main/java/org/example/doansummer2026/service/ReportService.java) |

**Additional classified endpoints:** `ReportController.getOverview`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/ReportServiceTest.java](../../src/test/java/org/example/doansummer2026/service/ReportServiceTest.java)

## CM-02 Manage Staff Information

Vietnamese: Quản lý thông tin nhân sự. Actors: Clinic Manager, Administrator. Delivery: UI + backend.

Create staff accounts or update permitted staff profile, photo, specialization, room assignment and activity details through account management. The Manager Staff page itself is a directory and detail view.

**Registered routes:** `/admin/accounts`; `/manager/staff`

**Frontend evidence:** [src/pages/admin/AccountManagementPage.jsx](../../../untitled/src/pages/admin/AccountManagementPage.jsx); [src/hooks/useAccountManagement.js](../../../untitled/src/hooks/useAccountManagement.js); [src/pages/owner/ManagerStaffPage.jsx](../../../untitled/src/pages/owner/ManagerStaffPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/staff | [StaffController.create](../../src/main/java/org/example/doansummer2026/controller/StaffController.java) line 179 | [StaffService.create](../../src/main/java/org/example/doansummer2026/service/StaffService.java) |
| PUT /api/v1/staff/{id} | [StaffController.update](../../src/main/java/org/example/doansummer2026/controller/StaffController.java) line 188 | [StaffService.get](../../src/main/java/org/example/doansummer2026/service/StaffService.java); [StaffService.update](../../src/main/java/org/example/doansummer2026/service/StaffService.java) |
| GET /api/v1/staff/clinic-manager | [StaffController.searchForClinicManager](../../src/main/java/org/example/doansummer2026/controller/StaffController.java) line 59 | [StaffService.searchForClinicManager](../../src/main/java/org/example/doansummer2026/service/StaffService.java) |
| GET /api/v1/staff/clinic-manager/{id} | [StaffController.getForClinicManager](../../src/main/java/org/example/doansummer2026/controller/StaffController.java) line 66 | [StaffService.getForClinicManager](../../src/main/java/org/example/doansummer2026/service/StaffService.java) |

**Additional classified endpoints:** `SpecializationController.list`, `SpecializationController.get`, `StaffController.search`, `StaffController.get`, `StaffController.delete`, `StaffController.lock`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/StaffControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/StaffControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/StaffServiceTest.java](../../src/test/java/org/example/doansummer2026/service/StaffServiceTest.java)

## CM-03 Manage Staff Schedules

Vietnamese: Phân công lịch trực. Actors: Clinic Manager, Administrator. Delivery: UI + backend.

View weekly staffing, assign staff to shifts and copy an eligible previous-week schedule. Validation checks conflicts and prevents unsupported changes to past assignments; no separate publish-schedule step is assumed.

**Registered routes:** `/owner/schedule`; `/admin/schedule`

**Frontend evidence:** [src/pages/owner/SchedulePage.jsx](../../../untitled/src/pages/owner/SchedulePage.jsx); [src/hooks/useSchedule.js](../../../untitled/src/hooks/useSchedule.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/clinic-manager/schedules | [StaffScheduleController.getSchedules](../../src/main/java/org/example/doansummer2026/controller/StaffScheduleController.java) line 122 | [StaffScheduleService.findByWeek](../../src/main/java/org/example/doansummer2026/service/StaffScheduleService.java) |
| POST /api/v1/clinic-manager/schedules/assign | [StaffScheduleController.assign](../../src/main/java/org/example/doansummer2026/controller/StaffScheduleController.java) line 169 | [StaffScheduleService.assignStaff](../../src/main/java/org/example/doansummer2026/service/StaffScheduleService.java) |
| POST /api/v1/clinic-manager/schedules/copy | [StaffScheduleController.copy](../../src/main/java/org/example/doansummer2026/controller/StaffScheduleController.java) line 180 | [StaffScheduleService.copyWeek](../../src/main/java/org/example/doansummer2026/service/StaffScheduleService.java) |

**Additional classified endpoints:** `ShiftConfigController.getActiveShifts`, `StaffController.list`, `StaffScheduleController.search`, `StaffScheduleController.get`, `StaffScheduleController.create`, `StaffScheduleController.update`, `StaffScheduleController.delete`, `StaffScheduleController.generate`, `StaffScheduleController.updateShifts`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/StaffScheduleControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/StaffScheduleControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/StaffScheduleServiceTest.java](../../src/test/java/org/example/doansummer2026/service/StaffScheduleServiceTest.java)

## CM-04 Manage Services and Prices

Vietnamese: Quản lý dịch vụ và giá. Actors: Clinic Manager, Administrator. Delivery: UI + backend.

Create or update allowed service details, price and booking settings; publish, deactivate or delete only where permitted. Fixed laboratory structures and linked-service constraints are not arbitrary panel-design CRUD.

**Registered routes:** `/admin/services`

**Frontend evidence:** [src/pages/admin/ServiceManagementPage.jsx](../../../untitled/src/pages/admin/ServiceManagementPage.jsx); [src/hooks/useServiceManagement.js](../../../untitled/src/hooks/useServiceManagement.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/medical-services | [MedicalServiceController.list](../../src/main/java/org/example/doansummer2026/controller/MedicalServiceController.java) line 42 | [MedicalServiceService.search](../../src/main/java/org/example/doansummer2026/service/MedicalServiceService.java) |
| GET /api/v1/medical-services/stats | [MedicalServiceController.getStats](../../src/main/java/org/example/doansummer2026/controller/MedicalServiceController.java) line 78 | [MedicalServiceService.getStats](../../src/main/java/org/example/doansummer2026/service/MedicalServiceService.java) |
| POST /api/v1/medical-services | [MedicalServiceController.create](../../src/main/java/org/example/doansummer2026/controller/MedicalServiceController.java) line 84 | [MedicalServiceService.create](../../src/main/java/org/example/doansummer2026/service/MedicalServiceService.java) |
| PUT /api/v1/medical-services/{id} | [MedicalServiceController.update](../../src/main/java/org/example/doansummer2026/controller/MedicalServiceController.java) line 92 | [MedicalServiceService.update](../../src/main/java/org/example/doansummer2026/service/MedicalServiceService.java) |
| PATCH /api/v1/medical-services/{id}/publish | [MedicalServiceController.publish](../../src/main/java/org/example/doansummer2026/controller/MedicalServiceController.java) line 117 | [MedicalServiceService.publish](../../src/main/java/org/example/doansummer2026/service/MedicalServiceService.java) |
| PATCH /api/v1/medical-services/{id}/deactivate | [MedicalServiceController.deactivate](../../src/main/java/org/example/doansummer2026/controller/MedicalServiceController.java) line 109 | [MedicalServiceService.deactivate](../../src/main/java/org/example/doansummer2026/service/MedicalServiceService.java) |
| DELETE /api/v1/medical-services/{id} | [MedicalServiceController.delete](../../src/main/java/org/example/doansummer2026/controller/MedicalServiceController.java) line 100 | [MedicalServiceService.delete](../../src/main/java/org/example/doansummer2026/service/MedicalServiceService.java) |

**Additional classified endpoints:** `MedicalServiceController.get`, `ServiceCategoryController.list`, `ServiceCategoryController.get`, `ServiceCategoryController.create`, `ServiceCategoryController.update`, `ServiceCategoryController.delete`, `SpecializationController.list`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/MedicalServiceServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MedicalServiceServiceTest.java)

## CM-05 Manage Departments and Rooms

Vietnamese: Quản lý phòng. Actors: Clinic Manager, Administrator. Delivery: UI + backend.

Maintain rooms, descriptions, allowed service types, capabilities and staff associations through room management. Selecting an existing specialty is not editing the specialty reference catalog.

**Registered routes:** `/admin/rooms`

**Frontend evidence:** [src/pages/admin/RoomManagementPage.jsx](../../../untitled/src/pages/admin/RoomManagementPage.jsx); [src/hooks/useRoomManagement.js](../../../untitled/src/hooks/useRoomManagement.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/departments/admin | [DepartmentController.listForAdmin](../../src/main/java/org/example/doansummer2026/controller/DepartmentController.java) line 43 | [DepartmentService.listAll](../../src/main/java/org/example/doansummer2026/service/DepartmentService.java); [DepartmentService.listMultiple](../../src/main/java/org/example/doansummer2026/service/DepartmentService.java) |
| POST /api/v1/departments | [DepartmentController.create](../../src/main/java/org/example/doansummer2026/controller/DepartmentController.java) line 84 | [DepartmentService.create](../../src/main/java/org/example/doansummer2026/service/DepartmentService.java) |
| PUT /api/v1/departments/{id} | [DepartmentController.update](../../src/main/java/org/example/doansummer2026/controller/DepartmentController.java) line 92 | [DepartmentService.update](../../src/main/java/org/example/doansummer2026/service/DepartmentService.java) |
| DELETE /api/v1/departments/{id} | [DepartmentController.delete](../../src/main/java/org/example/doansummer2026/controller/DepartmentController.java) line 100 | [DepartmentService.delete](../../src/main/java/org/example/doansummer2026/service/DepartmentService.java) |
| PATCH /api/v1/departments/{id}/status | [DepartmentController.updateStatus](../../src/main/java/org/example/doansummer2026/controller/DepartmentController.java) line 108 | [DepartmentService.updateStatus](../../src/main/java/org/example/doansummer2026/service/DepartmentService.java); [DepartmentService.get](../../src/main/java/org/example/doansummer2026/service/DepartmentService.java) |

**Additional classified endpoints:** `DepartmentController.list`, `DepartmentController.get`, `DepartmentController.listDoctors`, `DepartmentController.listNurses`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/DepartmentServiceTest.java](../../src/test/java/org/example/doansummer2026/service/DepartmentServiceTest.java)

## CM-06 Manage Medical-record Feedback

Vietnamese: Quản lý đánh giá bệnh án. Actors: Clinic Manager, Receptionist. Delivery: UI + backend.

Review overall record feedback, filter unanswered items and submit responses. Doctor, Nurse, Cashier and Administrator do not receive this feedback-management route or API permission.

**Registered routes:** `/receptionist/feedbacks`

**Frontend evidence:** [src/pages/staff/FeedbackPage.jsx](../../../untitled/src/pages/staff/FeedbackPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/feedbacks | [MedicalRecordController.feedbacks](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 262 | [MedicalRecordService.listFeedbacks](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |
| GET /api/v1/feedbacks/stats/unanswered-count | [MedicalRecordController.countUnansweredFeedbacks](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 269 | [MedicalRecordService.countUnansweredFeedbacks](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |
| PUT /api/v1/feedbacks/{id}/respond | [MedicalRecordController.respond](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 275 | [MedicalRecordService.respondFeedback](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java); [AuthService.currentStaffId](../../src/main/java/org/example/doansummer2026/service/AuthService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/AuthServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AuthServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java)

## CM-08 Manage Public Announcements

Vietnamese: Quản lý thông báo công khai. Actors: Clinic Manager, Administrator. Delivery: UI + backend.

Create, edit, publish or withdraw and delete supported public announcements. Public visibility and local dismissal are separate from staff in-app notifications.

**Registered routes:** `/admin/public-announcements`

**Frontend evidence:** [src/pages/admin/PublicAnnouncementManagementPage.jsx](../../../untitled/src/pages/admin/PublicAnnouncementManagementPage.jsx); [src/services/publicAnnouncementService.js](../../../untitled/src/services/publicAnnouncementService.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/public-announcements | [PublicAnnouncementController.listAll](../../src/main/java/org/example/doansummer2026/controller/PublicAnnouncementController.java) line 29 | [PublicAnnouncementService.listAll](../../src/main/java/org/example/doansummer2026/service/PublicAnnouncementService.java) |
| POST /api/v1/public-announcements | [PublicAnnouncementController.create](../../src/main/java/org/example/doansummer2026/controller/PublicAnnouncementController.java) line 35 | [PublicAnnouncementService.create](../../src/main/java/org/example/doansummer2026/service/PublicAnnouncementService.java); [AuthService.currentAccount](../../src/main/java/org/example/doansummer2026/service/AuthService.java) |
| PUT /api/v1/public-announcements/{id} | [PublicAnnouncementController.update](../../src/main/java/org/example/doansummer2026/controller/PublicAnnouncementController.java) line 42 | [PublicAnnouncementService.update](../../src/main/java/org/example/doansummer2026/service/PublicAnnouncementService.java) |
| PATCH /api/v1/public-announcements/{id}/publication | [PublicAnnouncementController.setPublication](../../src/main/java/org/example/doansummer2026/controller/PublicAnnouncementController.java) line 50 | [PublicAnnouncementService.setPublished](../../src/main/java/org/example/doansummer2026/service/PublicAnnouncementService.java) |
| DELETE /api/v1/public-announcements/{id} | [PublicAnnouncementController.delete](../../src/main/java/org/example/doansummer2026/controller/PublicAnnouncementController.java) line 58 | [PublicAnnouncementService.delete](../../src/main/java/org/example/doansummer2026/service/PublicAnnouncementService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/AuthServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AuthServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/PublicAnnouncementServiceTest.java](../../src/test/java/org/example/doansummer2026/service/PublicAnnouncementServiceTest.java)

## CM-09 Manage Diagnostic Form Templates

Vietnamese: Quản lý biểu mẫu CLS. Actors: Clinic Manager. Delivery: UI + backend.

Create or version permitted diagnostic forms, edit drafts, bind services, publish and retire forms. System laboratory forms are protected; this UI does not allow rewriting their fixed analytes and reference ranges.

**Registered routes:** `/manager/clinical-form-templates`

**Frontend evidence:** [src/pages/owner/ClinicalFormTemplatePage.jsx](../../../untitled/src/pages/owner/ClinicalFormTemplatePage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/clinical-form-templates | [ClinicalFormTemplateController.list](../../src/main/java/org/example/doansummer2026/controller/ClinicalFormTemplateController.java) line 24 | [ClinicalFormTemplateService.list](../../src/main/java/org/example/doansummer2026/service/ClinicalFormTemplateService.java) |
| POST /api/v1/clinical-form-templates | [ClinicalFormTemplateController.create](../../src/main/java/org/example/doansummer2026/controller/ClinicalFormTemplateController.java) line 27 | [ClinicalFormTemplateService.create](../../src/main/java/org/example/doansummer2026/service/ClinicalFormTemplateService.java) |
| PUT /api/v1/clinical-form-templates/{id}/draft | [ClinicalFormTemplateController.draft](../../src/main/java/org/example/doansummer2026/controller/ClinicalFormTemplateController.java) line 34 | [ClinicalFormTemplateService.saveDraft](../../src/main/java/org/example/doansummer2026/service/ClinicalFormTemplateService.java) |
| PUT /api/v1/clinical-form-templates/{id}/services | [ClinicalFormTemplateController.bind](../../src/main/java/org/example/doansummer2026/controller/ClinicalFormTemplateController.java) line 48 | [ClinicalFormTemplateService.bindServices](../../src/main/java/org/example/doansummer2026/service/ClinicalFormTemplateService.java) |
| POST /api/v1/clinical-form-templates/{id}/publish | [ClinicalFormTemplateController.publish](../../src/main/java/org/example/doansummer2026/controller/ClinicalFormTemplateController.java) line 40 | [ClinicalFormTemplateService.publish](../../src/main/java/org/example/doansummer2026/service/ClinicalFormTemplateService.java) |
| POST /api/v1/clinical-form-templates/{id}/retire | [ClinicalFormTemplateController.retire](../../src/main/java/org/example/doansummer2026/controller/ClinicalFormTemplateController.java) line 44 | [ClinicalFormTemplateService.retire](../../src/main/java/org/example/doansummer2026/service/ClinicalFormTemplateService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/ClinicalFormTemplateServiceTest.java](../../src/test/java/org/example/doansummer2026/service/ClinicalFormTemplateServiceTest.java)

## CM-10 Print or Export Reports

Vietnamese: In và xuất báo cáo. Actors: Clinic Manager. Delivery: UI + backend.

Choose an available report type, preview and print it or export CSV. Exports use all matching loaded rows, not only the current page, and preserve the selected period and relevant detail filter.

**Registered routes:** `/owner/report`

**Frontend evidence:** [src/pages/owner/ReportPage.jsx](../../../untitled/src/pages/owner/ReportPage.jsx); [src/features/reports/reportExport.js](../../../untitled/src/features/reports/reportExport.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/reports/dashboard | [ReportController.getDashboard](../../src/main/java/org/example/doansummer2026/controller/ReportController.java) line 37 | [ReportService.getDashboardReport](../../src/main/java/org/example/doansummer2026/service/ReportService.java) |
| GET /api/v1/reports/services | [ReportController.getServices](../../src/main/java/org/example/doansummer2026/controller/ReportController.java) line 49 | [ReportService.getServiceReport](../../src/main/java/org/example/doansummer2026/service/ReportService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/ReportServiceTest.java](../../src/test/java/org/example/doansummer2026/service/ReportServiceTest.java)

## CM-11 View Patient Directory

Vietnamese: Xem danh sách bệnh nhân. Actors: Clinic Manager. Delivery: UI + backend.

Search the manager patient directory, inspect patient details and open permitted visit history. The directory is not an independent clinical-editing workbench.

**Registered routes:** `/manager/patients`

**Frontend evidence:** [src/pages/owner/ManagerPatientsPage.jsx](../../../untitled/src/pages/owner/ManagerPatientsPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/clinic-manager/patients | [MedicalRecordController.listPatientsForClinicManager](../../src/main/java/org/example/doansummer2026/controller/MedicalRecordController.java) line 312 | [MedicalRecordService.searchUniqueCustomers](../../src/main/java/org/example/doansummer2026/service/MedicalRecordService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/MedicalRecordControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MedicalRecordServiceTest.java)

## CM-12 Configure CareS Policy

Vietnamese: Cấu hình chính sách thẻ CareS. Actors: Clinic Manager, Administrator. Delivery: UI + backend.

Review and update supported prepaid-card policy values such as minimum top-up, benefit percentage and validity period. Existing card/payment rules remain enforced by the service.

**Registered routes:** `/admin/membership-policy`

**Frontend evidence:** [src/pages/admin/MembershipPolicyPage.jsx](../../../untitled/src/pages/admin/MembershipPolicyPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/membership-cards/policy | [MembershipCardController.policy](../../src/main/java/org/example/doansummer2026/controller/MembershipCardController.java) line 65 | [MembershipCardService.getPolicy](../../src/main/java/org/example/doansummer2026/service/MembershipCardService.java) |
| PUT /api/v1/membership-cards/policy | [MembershipCardController.updatePolicy](../../src/main/java/org/example/doansummer2026/controller/MembershipCardController.java) line 69 | [MembershipCardService.updatePolicy](../../src/main/java/org/example/doansummer2026/service/MembershipCardService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/MembershipCardServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MembershipCardServiceTest.java)

## CM-13 Review CareS Ledger and Reverse Eligible Payment

Vietnamese: Xem sổ thẻ và hoàn tác đủ điều kiện. Actors: Clinic Manager, Administrator. Delivery: UI + backend.

Review card ledger entries and reverse an eligible erroneous card payment with a reason and idempotency key. This is not a Cashier action or unrestricted refund; service-start and existing reversal checks apply.

**Registered routes:** `/admin/membership-policy`

**Frontend evidence:** [src/pages/admin/MembershipPolicyPage.jsx](../../../untitled/src/pages/admin/MembershipPolicyPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/membership-cards/ledger | [MembershipCardController.allLedger](../../src/main/java/org/example/doansummer2026/controller/MembershipCardController.java) line 76 | [MembershipCardService.allHistory](../../src/main/java/org/example/doansummer2026/service/MembershipCardService.java) |
| POST /api/v1/membership-cards/ledger/{ledgerId}/reverse | [MembershipCardController.reverse](../../src/main/java/org/example/doansummer2026/controller/MembershipCardController.java) line 91 | [MembershipCardService.reverse](../../src/main/java/org/example/doansummer2026/service/MembershipCardService.java); [AuthService.currentStaffId](../../src/main/java/org/example/doansummer2026/service/AuthService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/AuthServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AuthServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/MembershipCardServiceTest.java](../../src/test/java/org/example/doansummer2026/service/MembershipCardServiceTest.java)

## AD-01 Manage User Accounts

Vietnamese: Quản lý tài khoản. Actors: Administrator, Clinic Manager. Delivery: UI + backend.

Search staff or customer accounts, edit permitted fields, lock/unlock accounts and reset a password through account management. Account constraints can reject self-locking or unsupported removal.

**Registered routes:** `/admin/accounts`

**Frontend evidence:** [src/pages/admin/AccountManagementPage.jsx](../../../untitled/src/pages/admin/AccountManagementPage.jsx); [src/hooks/useAccountManagement.js](../../../untitled/src/hooks/useAccountManagement.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/accounts/staff | [AccountController.listStaff](../../src/main/java/org/example/doansummer2026/controller/AccountController.java) line 72 | [AccountService.listStaff](../../src/main/java/org/example/doansummer2026/service/AccountService.java) |
| GET /api/v1/accounts/customers | [AccountController.listCustomers](../../src/main/java/org/example/doansummer2026/controller/AccountController.java) line 84 | [AccountService.listCustomers](../../src/main/java/org/example/doansummer2026/service/AccountService.java) |
| GET /api/v1/accounts/{id} | [AccountController.get](../../src/main/java/org/example/doansummer2026/controller/AccountController.java) line 93 | [AccountService.findById](../../src/main/java/org/example/doansummer2026/service/AccountService.java) |
| PUT /api/v1/accounts/{id} | [AccountController.update](../../src/main/java/org/example/doansummer2026/controller/AccountController.java) line 104 | [AccountService.findById](../../src/main/java/org/example/doansummer2026/service/AccountService.java); [AccountService.update](../../src/main/java/org/example/doansummer2026/service/AccountService.java) |
| PATCH /api/v1/accounts/{id}/lock | [AccountController.lock](../../src/main/java/org/example/doansummer2026/controller/AccountController.java) line 139 | [AccountService.lock](../../src/main/java/org/example/doansummer2026/service/AccountService.java) |
| PUT /api/v1/accounts/{id}/password-reset | [AccountController.adminResetPassword](../../src/main/java/org/example/doansummer2026/controller/AccountController.java) line 119 | [AccountService.findById](../../src/main/java/org/example/doansummer2026/service/AccountService.java); [AccountService.adminResetPassword](../../src/main/java/org/example/doansummer2026/service/AccountService.java) |

**Additional classified endpoints:** `AccountController.list`, `AccountController.delete`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/AccountControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/AccountControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/AccountServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AccountServiceTest.java)

## AD-02 Assign Supported Staff Roles and Capabilities

Vietnamese: Gán vai trò và năng lực nhân sự. Actors: Administrator, Clinic Manager. Delivery: UI + backend.

Assign supported fixed staff roles and permitted professional/room/capability information. The system has no arbitrary permission-matrix or new-role editor; Doctor eligibility is checked again during clinical operations.

**Registered routes:** `/admin/accounts`

**Frontend evidence:** [src/pages/admin/AccountManagementPage.jsx](../../../untitled/src/pages/admin/AccountManagementPage.jsx); [src/hooks/useAccountManagement.js](../../../untitled/src/hooks/useAccountManagement.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/staff | [StaffController.create](../../src/main/java/org/example/doansummer2026/controller/StaffController.java) line 179 | [StaffService.create](../../src/main/java/org/example/doansummer2026/service/StaffService.java) |
| PUT /api/v1/staff/{id} | [StaffController.update](../../src/main/java/org/example/doansummer2026/controller/StaffController.java) line 188 | [StaffService.get](../../src/main/java/org/example/doansummer2026/service/StaffService.java); [StaffService.update](../../src/main/java/org/example/doansummer2026/service/StaffService.java) |
| GET /api/v1/staff/{staffId}/capabilities | [StaffController.listCapabilities](../../src/main/java/org/example/doansummer2026/controller/StaffController.java) line 72 | [AuthService.getCurrentSystemRole](../../src/main/java/org/example/doansummer2026/service/AuthService.java); [AuthService.currentStaffId](../../src/main/java/org/example/doansummer2026/service/AuthService.java); [StaffService.listCapabilities](../../src/main/java/org/example/doansummer2026/service/StaffService.java) |
| PUT /api/v1/staff/{staffId}/capabilities | [StaffController.replaceCapabilities](../../src/main/java/org/example/doansummer2026/controller/StaffController.java) line 84 | [StaffService.replaceCapabilities](../../src/main/java/org/example/doansummer2026/service/StaffService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/StaffControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/StaffControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/AuthServiceTest.java](../../src/test/java/org/example/doansummer2026/service/AuthServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/StaffServiceTest.java](../../src/test/java/org/example/doansummer2026/service/StaffServiceTest.java)

## AD-03 Manage Clinic Information

Vietnamese: Cập nhật thông tin phòng khám. Actors: Administrator, Clinic Manager. Delivery: UI + backend.

Edit the clinic information exposed by the configuration screen and public site. Room, service, shift and announcement management use their own shared use cases.

**Registered routes:** `/admin/clinic-information`

**Frontend evidence:** [src/pages/admin/ClinicInformationPage.jsx](../../../untitled/src/pages/admin/ClinicInformationPage.jsx); [src/services/clinicInformationService.js](../../../untitled/src/services/clinicInformationService.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/clinic-information | [ClinicInformationController.information](../../src/main/java/org/example/doansummer2026/controller/ClinicInformationController.java) line 28 | [ClinicInformationService.get](../../src/main/java/org/example/doansummer2026/service/ClinicInformationService.java) |
| PUT /api/v1/clinic-information | [ClinicInformationController.update](../../src/main/java/org/example/doansummer2026/controller/ClinicInformationController.java) line 34 | [ClinicInformationService.update](../../src/main/java/org/example/doansummer2026/service/ClinicInformationService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/ClinicInformationServiceTest.java](../../src/test/java/org/example/doansummer2026/service/ClinicInformationServiceTest.java)

## AD-05 Manage ICD-10 Catalog through API

Vietnamese: Quản lý ICD qua API. Actors: Administrator. Delivery: Backend only.

Create, update or delete ICD-10 reference entries through the administrator API. The current frontend provides diagnosis lookup, but no registered ICD catalog-management page.

**Registered routes:** No standalone route; shared component/hook, API-only or integration as indicated.

**Frontend evidence:** No frontend evidence claimed.

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/icd10-codes | [Icd10CodeController.create](../../src/main/java/org/example/doansummer2026/controller/Icd10CodeController.java) line 50 | [Icd10CodeService.create](../../src/main/java/org/example/doansummer2026/service/Icd10CodeService.java) |
| PUT /api/v1/icd10-codes/{code} | [Icd10CodeController.update](../../src/main/java/org/example/doansummer2026/controller/Icd10CodeController.java) line 57 | [Icd10CodeService.update](../../src/main/java/org/example/doansummer2026/service/Icd10CodeService.java) |
| DELETE /api/v1/icd10-codes/{code} | [Icd10CodeController.delete](../../src/main/java/org/example/doansummer2026/controller/Icd10CodeController.java) line 64 | [Icd10CodeService.delete](../../src/main/java/org/example/doansummer2026/service/Icd10CodeService.java) |

**Related backend tests (not run here):**

No filename-matched test reference found; this is not proof of missing test coverage.

## AD-06 Review Audit Information

Vietnamese: Xem nhật ký hệ thống. Actors: Administrator, Clinic Manager. Delivery: UI + backend.

Search the available audit events and inspect entity-related changes through the audit screen. This does not provide a general server-monitoring or integration-health console.

**Registered routes:** `/admin/audit-logs`

**Frontend evidence:** [src/pages/admin/AuditLogManagementPage.jsx](../../../untitled/src/pages/admin/AuditLogManagementPage.jsx); [src/hooks/useAuditLog.js](../../../untitled/src/hooks/useAuditLog.js)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/audit-logs | [AuditLogController.list](../../src/main/java/org/example/doansummer2026/controller/AuditLogController.java) line 33 | [AuditLogService.search](../../src/main/java/org/example/doansummer2026/service/AuditLogService.java) |
| GET /api/v1/audit-logs/by-entity | [AuditLogController.byEntity](../../src/main/java/org/example/doansummer2026/controller/AuditLogController.java) line 45 | [AuditLogService.findByEntity](../../src/main/java/org/example/doansummer2026/service/AuditLogService.java) |

**Related backend tests (not run here):**

No filename-matched test reference found; this is not proof of missing test coverage.

## AD-07 Manage Shift Versions

Vietnamese: Quản lý phiên bản ca. Actors: Administrator, Clinic Manager. Delivery: UI + backend.

Review fixed shifts, preview the impact of changed hours and create a future-effective shift version with a reason. Existing historical versions are retained.

**Registered routes:** `/admin/shifts`

**Frontend evidence:** [src/pages/admin/ShiftManagementPage.jsx](../../../untitled/src/pages/admin/ShiftManagementPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/shifts | [ShiftConfigController.getAllShifts](../../src/main/java/org/example/doansummer2026/controller/ShiftConfigController.java) line 46 | [ShiftConfigService.getAllShifts](../../src/main/java/org/example/doansummer2026/service/ShiftConfigService.java) |
| GET /api/v1/shifts/{id}/versions | [ShiftConfigController.versions](../../src/main/java/org/example/doansummer2026/controller/ShiftConfigController.java) line 52 | [ClinicScheduleManagementService.history](../../src/main/java/org/example/doansummer2026/service/ClinicScheduleManagementService.java) |
| GET /api/v1/shifts/{id}/versions/impact | [ShiftConfigController.versionImpact](../../src/main/java/org/example/doansummer2026/controller/ShiftConfigController.java) line 58 | [ClinicScheduleManagementService.previewVersionImpact](../../src/main/java/org/example/doansummer2026/service/ClinicScheduleManagementService.java) |
| POST /api/v1/shifts/{id}/versions | [ShiftConfigController.createVersion](../../src/main/java/org/example/doansummer2026/controller/ShiftConfigController.java) line 65 | [ClinicScheduleManagementService.createVersion](../../src/main/java/org/example/doansummer2026/service/ClinicScheduleManagementService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/ClinicScheduleManagementServiceTest.java](../../src/test/java/org/example/doansummer2026/service/ClinicScheduleManagementServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/ShiftConfigServiceTest.java](../../src/test/java/org/example/doansummer2026/service/ShiftConfigServiceTest.java)

## AD-09 Manage Operating Exceptions

Vietnamese: Quản lý lịch hoạt động ngoại lệ. Actors: Administrator, Clinic Manager. Delivery: UI + backend.

Preview and configure future clinic closures, shift-off dates or special hours, and reopen an eligible exception. Impact checks can block changes affecting existing operations.

**Registered routes:** `/admin/shifts`

**Frontend evidence:** [src/pages/admin/ShiftManagementPage.jsx](../../../untitled/src/pages/admin/ShiftManagementPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/clinic-schedule/exceptions | [ClinicScheduleController.exceptions](../../src/main/java/org/example/doansummer2026/controller/ClinicScheduleController.java) line 26 | [ClinicScheduleManagementService.exceptions](../../src/main/java/org/example/doansummer2026/service/ClinicScheduleManagementService.java) |
| POST /api/v1/clinic-schedule/exceptions/impact | [ClinicScheduleController.previewException](../../src/main/java/org/example/doansummer2026/controller/ClinicScheduleController.java) line 33 | [ClinicScheduleManagementService.previewExceptionImpact](../../src/main/java/org/example/doansummer2026/service/ClinicScheduleManagementService.java) |
| POST /api/v1/clinic-schedule/exceptions | [ClinicScheduleController.createException](../../src/main/java/org/example/doansummer2026/controller/ClinicScheduleController.java) line 39 | [ClinicScheduleManagementService.createException](../../src/main/java/org/example/doansummer2026/service/ClinicScheduleManagementService.java) |
| DELETE /api/v1/clinic-schedule/exceptions/{id} | [ClinicScheduleController.reopen](../../src/main/java/org/example/doansummer2026/controller/ClinicScheduleController.java) line 46 | [ClinicScheduleManagementService.reopen](../../src/main/java/org/example/doansummer2026/service/ClinicScheduleManagementService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/ClinicScheduleManagementServiceTest.java](../../src/test/java/org/example/doansummer2026/service/ClinicScheduleManagementServiceTest.java)

## AD-10 Check Service Coverage

Vietnamese: Kiểm tra khả năng phục vụ theo ca. Actors: Administrator, Clinic Manager. Delivery: UI + backend.

Check service availability for a date and shift to identify missing room or staffing coverage before changing schedules or accepting bookings.

**Registered routes:** `/admin/shifts`

**Frontend evidence:** [src/pages/admin/ShiftManagementPage.jsx](../../../untitled/src/pages/admin/ShiftManagementPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/clinic-schedule/coverage | [ClinicScheduleController.coverage](../../src/main/java/org/example/doansummer2026/controller/ClinicScheduleController.java) line 53 | [ServiceAvailabilityService.coverage](../../src/main/java/org/example/doansummer2026/service/ServiceAvailabilityService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/ServiceAvailabilityServiceTest.java](../../src/test/java/org/example/doansummer2026/service/ServiceAvailabilityServiceTest.java)

## AD-11 View Fixed Technical Catalog

Vietnamese: Xem danh mục kỹ thuật cố định. Actors: Administrator, Clinic Manager. Delivery: UI + backend.

Search and read the fixed technical-capability catalog. Both the interface and controller reject catalog creation, editing and deletion. Assigning an existing capability to staff is a separate operation.

**Registered routes:** `/admin/capabilities`

**Frontend evidence:** [src/pages/admin/CapabilityManagementPage.jsx](../../../untitled/src/pages/admin/CapabilityManagementPage.jsx)

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/service-capabilities | [ServiceCapabilityController.list](../../src/main/java/org/example/doansummer2026/controller/ServiceCapabilityController.java) line 19 | [ServiceCapabilityService.list](../../src/main/java/org/example/doansummer2026/service/ServiceCapabilityService.java) |

**Additional classified endpoints:** `ServiceCapabilityController.create`, `ServiceCapabilityController.update`, `ServiceCapabilityController.delete`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/ServiceCapabilityServiceTest.java](../../src/test/java/org/example/doansummer2026/service/ServiceCapabilityServiceTest.java)

## AD-12 Manage Schedule Templates through API

Vietnamese: Quản lý mẫu lịch qua API. Actors: Administrator. Delivery: Backend only.

Maintain recurring staff schedule templates through the backend endpoints. This is distinct from weekly assignment and copying in SchedulePage; no dedicated template-management route is registered.

**Registered routes:** No standalone route; shared component/hook, API-only or integration as indicated.

**Frontend evidence:** No frontend evidence claimed.

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/schedule-templates | [StaffScheduleTemplateController.create](../../src/main/java/org/example/doansummer2026/controller/StaffScheduleTemplateController.java) line 33 | [StaffScheduleTemplateService.create](../../src/main/java/org/example/doansummer2026/service/StaffScheduleTemplateService.java) |
| PUT /api/v1/schedule-templates/{id} | [StaffScheduleTemplateController.update](../../src/main/java/org/example/doansummer2026/controller/StaffScheduleTemplateController.java) line 41 | [StaffScheduleTemplateService.update](../../src/main/java/org/example/doansummer2026/service/StaffScheduleTemplateService.java) |
| DELETE /api/v1/schedule-templates/{id} | [StaffScheduleTemplateController.delete](../../src/main/java/org/example/doansummer2026/controller/StaffScheduleTemplateController.java) line 49 | [StaffScheduleTemplateService.delete](../../src/main/java/org/example/doansummer2026/service/StaffScheduleTemplateService.java) |
| GET /api/v1/schedule-templates | [StaffScheduleTemplateController.listByStaff](../../src/main/java/org/example/doansummer2026/controller/StaffScheduleTemplateController.java) line 57 | [StaffScheduleTemplateService.listByStaff](../../src/main/java/org/example/doansummer2026/service/StaffScheduleTemplateService.java) |
| GET /api/v1/schedule-templates/{id} | [StaffScheduleTemplateController.get](../../src/main/java/org/example/doansummer2026/controller/StaffScheduleTemplateController.java) line 63 | [StaffScheduleTemplateService.get](../../src/main/java/org/example/doansummer2026/service/StaffScheduleTemplateService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/StaffScheduleTemplateServiceTest.java](../../src/test/java/org/example/doansummer2026/service/StaffScheduleTemplateServiceTest.java)

## AD-13 Administer Notification Records through API

Vietnamese: Quản trị bản ghi thông báo qua API. Actors: Administrator. Delivery: Backend only.

Use administrator endpoints to inspect and maintain notification records and their delivery flags. Setting SENT is not evidence that an email or SMS was actually delivered; no notification-administration page is registered.

**Registered routes:** No standalone route; shared component/hook, API-only or integration as indicated.

**Frontend evidence:** No frontend evidence claimed.

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/notifications | [NotificationController.list](../../src/main/java/org/example/doansummer2026/controller/NotificationController.java) line 41 | [NotificationService.search](../../src/main/java/org/example/doansummer2026/service/NotificationService.java) |
| POST /api/v1/notifications | [NotificationController.create](../../src/main/java/org/example/doansummer2026/controller/NotificationController.java) line 79 | [NotificationService.create](../../src/main/java/org/example/doansummer2026/service/NotificationService.java) |
| PUT /api/v1/notifications/{id} | [NotificationController.update](../../src/main/java/org/example/doansummer2026/controller/NotificationController.java) line 86 | [NotificationService.update](../../src/main/java/org/example/doansummer2026/service/NotificationService.java) |
| POST /api/v1/notifications/{id}/send | [NotificationController.send](../../src/main/java/org/example/doansummer2026/controller/NotificationController.java) line 93 | [NotificationService.send](../../src/main/java/org/example/doansummer2026/service/NotificationService.java) |
| POST /api/v1/notifications/{id}/mark-failed | [NotificationController.markFailed](../../src/main/java/org/example/doansummer2026/controller/NotificationController.java) line 106 | [NotificationService.markFailed](../../src/main/java/org/example/doansummer2026/service/NotificationService.java) |
| DELETE /api/v1/notifications/{id} | [NotificationController.delete](../../src/main/java/org/example/doansummer2026/controller/NotificationController.java) line 113 | [NotificationService.delete](../../src/main/java/org/example/doansummer2026/service/NotificationService.java) |

**Additional classified endpoints:** `NotificationController.get`. See endpoint inventory; these do not imply a corresponding screen action.

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/NotificationServiceTest.java](../../src/test/java/org/example/doansummer2026/service/NotificationServiceTest.java)

## EXT-01 Process Online Payment

Vietnamese: Xử lý thanh toán trực tuyến. Actors: PayOS. Delivery: External integration.

Receive a supported payment-link request and return gateway payment information or a verified callback. The backend applies the payment outcome; a browser redirect alone does not confirm settlement.

**Registered routes:** No standalone route; shared component/hook, API-only or integration as indicated.

**Frontend evidence:** No frontend evidence claimed.

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/v1/invoices/{id}/payos | [InvoiceController.payosMock](../../src/main/java/org/example/doansummer2026/controller/InvoiceController.java) line 121 | [PayOSService.createPaymentLink](../../src/main/java/org/example/doansummer2026/service/PayOSService.java) |
| POST /api/v1/payos/webhook | [PayOSWebhookController.handleWebhook](../../src/main/java/org/example/doansummer2026/controller/PayOSWebhookController.java) line 28 | [PayOSService.verifyWebhook](../../src/main/java/org/example/doansummer2026/service/PayOSService.java); [PayOSService.getInvoiceIdByOrderCode](../../src/main/java/org/example/doansummer2026/service/PayOSService.java); [InvoiceService.pay](../../src/main/java/org/example/doansummer2026/service/InvoiceService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/InvoiceControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/InvoiceControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/InvoiceServiceTest.java](../../src/test/java/org/example/doansummer2026/service/InvoiceServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/PayOSServiceTest.java](../../src/test/java/org/example/doansummer2026/service/PayOSServiceTest.java)

## EXT-02 Simulate Insurance Verification

Vietnamese: Mô phỏng xác minh BHYT. Actors: Mock BHXH Service. Delivery: External integration.

Return simulated insurance verification through the configured service. The built-in demo-card branch also simulates success locally; neither path is represented as an official BHXH claim or certification.

**Registered routes:** No standalone route; shared component/hook, API-only or integration as indicated.

**Frontend evidence:** No frontend evidence claimed.

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| GET /api/v1/bhxh/check | [BhxhController.checkCard](../../src/main/java/org/example/doansummer2026/controller/BhxhController.java) line 22 | [BhxhIntegrationService.checkBhytCard](../../src/main/java/org/example/doansummer2026/service/BhxhIntegrationService.java) |
| POST /api/v1/invoices/{id}/insurance | [InvoiceController.applyInsurance](../../src/main/java/org/example/doansummer2026/controller/InvoiceController.java) line 88 | [InvoiceService.applyInsurance](../../src/main/java/org/example/doansummer2026/service/InvoiceService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/controller/InvoiceControllerTest.java](../../src/test/java/org/example/doansummer2026/controller/InvoiceControllerTest.java)
- [src/test/java/org/example/doansummer2026/service/InvoiceServiceTest.java](../../src/test/java/org/example/doansummer2026/service/InvoiceServiceTest.java)

## EXT-03 Deliver Verification and Contact Messages

Vietnamese: Gửi thông điệp xác thực và liên hệ. Actors: Email Service, SMS Gateway. Delivery: External integration.

Support outbound verification messages. Email also sends the public contact enquiry to the clinic mailbox. SMS is configured as mock in the inspected application configuration; provider implementations are not proof of live delivery.

**Registered routes:** No standalone route; shared component/hook, API-only or integration as indicated.

**Frontend evidence:** No frontend evidence claimed.

**Primary API and service evidence:**

| Endpoint | Controller | Direct service calls |
| --- | --- | --- |
| POST /api/auth/send-otp | [AuthController.sendOtp](../../src/main/java/org/example/doansummer2026/controller/AuthController.java) line 47 | [OtpService.sendOtp](../../src/main/java/org/example/doansummer2026/service/OtpService.java) |
| POST /api/auth/send-register-otp | [AuthController.sendRegisterOtp](../../src/main/java/org/example/doansummer2026/controller/AuthController.java) line 61 | [AuthServiceInterface.ensureRegistrationIdentifierAvailable](../../src/main/java/org/example/doansummer2026/service/interfaces/AuthServiceInterface.java); [OtpService.sendOtp](../../src/main/java/org/example/doansummer2026/service/OtpService.java) |
| POST /api/public/contact-requests | [ContactRequestController.send](../../src/main/java/org/example/doansummer2026/controller/ContactRequestController.java) line 20 | [ContactRequestService.send](../../src/main/java/org/example/doansummer2026/service/ContactRequestService.java) |

**Related backend tests (not run here):**

- [src/test/java/org/example/doansummer2026/service/ContactRequestServiceTest.java](../../src/test/java/org/example/doansummer2026/service/ContactRequestServiceTest.java)
- [src/test/java/org/example/doansummer2026/service/OtpServiceTest.java](../../src/test/java/org/example/doansummer2026/service/OtpServiceTest.java)
