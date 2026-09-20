# CareS System Function List

This document lists the functions implemented or explicitly exposed by the current CareS frontend and backend. It is intended for the Functional Requirements and system-description sections of the project report. A function represents a user or system goal; individual HTTP endpoints and JavaScript/Java methods are traceability evidence, not separate functions.

The list uses the stable actor/use-case identifiers where a user goal already exists. DS identifiers describe diagnostic functions performed by the existing Doctor and Nurse actors; DS does not introduce a Diagnostic Service Staff actor. SYS identifiers describe automatic or scheduled behavior inside CareS.

## Summary

| Measure | Value |
| --- | --- |
| Total functions | 140 |
| User and integration functions | 125 |
| Internal automatic or scheduled functions | 15 |
| Functions with UI and backend | 115 |
| Backend-only user functions | 6 |
| Frontend-only function | 1 |
| External integrations | 3 |

## Module Summary

| Module | English name | Tên tiếng Việt | Function count |
| --- | --- | --- | --- |
| G | Public and Guest Services | Dịch vụ công khai và khách vãng lai | 8 |
| RU | Authentication and Shared Account Services | Xác thực và chức năng tài khoản dùng chung | 8 |
| CU | Customer Self Service | Chức năng tự phục vụ của Customer | 20 |
| RC | Reception and Patient Coordination | Tiếp nhận và điều phối bệnh nhân | 14 |
| CA | Billing and Payment | Hóa đơn và thanh toán | 12 |
| NU | Nursing and Clinical Preparation | Điều dưỡng và chuẩn bị khám | 7 |
| DR | Medical Examination | Khám bệnh | 16 |
| DS | Diagnostic and Laboratory Services | Cận lâm sàng và xét nghiệm | 14 |
| CM | Clinic Operations Management | Quản lý vận hành phòng khám | 12 |
| AD | System Administration | Quản trị hệ thống | 11 |
| EXT | External Integrations | Tích hợp bên ngoài | 3 |
| SYS | Internal System Functions | Chức năng nội bộ của hệ thống | 15 |

## Full Function List

### Public and Guest Services
Dịch vụ công khai và khách vãng lai.

| ID | Function name | Tên chức năng | Actor or owner | Description | Delivery |
| --- | --- | --- | --- | --- | --- |
| G-01 | View Public Clinic Information | Xem thông tin phòng khám | Guest | Browse clinic information, public announcements, services, doctors, contact details, terms and privacy information. Public doctor information does not disclose individual duty schedules. | UI + backend |
| G-02 | Register Account | Đăng ký tài khoản | Guest | Register a Customer account using the registration form and OTP verification. Staff accounts are created through authorized staff administration, not public registration. | UI + backend |
| G-03 | Create Guest Appointment | Đặt lịch không cần tài khoản | Guest | Select publicly bookable services, an available date and shift, enter patient details and confirm a guest booking. The guest selector does not expose individually priced laboratory analytes. | UI + backend |
| G-05 | View Public Department Schedule | Xem lịch hoạt động các khoa | Guest | Review public department availability by date and shift before booking, without viewing named staff duty assignments. | UI + backend |
| G-06 | Look Up Guest Journey | Tra cứu hành trình khách vãng lai | Guest | Use the visit code and matching phone number to view the guest journey, current queue and service statuses. This lookup does not provide diagnoses, prescriptions or clinical result content. | UI + backend |
| G-08 | Use Guest Support Chat | Chat hỗ trợ không cần tài khoản | Guest | Start a guest conversation, read replies and continue it using the guest session credentials. Automated replies are an internal support feature, not a separate external actor. | UI + backend |
| G-09 | Submit Contact Enquiry | Gửi yêu cầu liên hệ | Guest | Send contact information and an enquiry through the public form for delivery to the configured clinic mailbox. This is separate from the live receptionist chat. | UI + backend |

### Authentication and Shared Account Services
Xác thực và chức năng tài khoản dùng chung.

| ID | Function name | Tên chức năng | Actor or owner | Description | Delivery |
| --- | --- | --- | --- | --- | --- |
| RU-01 | Log In and Log Out | Đăng nhập và đăng xuất | Registered User | Authenticate an existing account and end the local application session. Logout clears browser authentication state; no server-side logout or token-revocation endpoint is exposed by AuthController. | UI + backend |
| RU-02 | Verify OTP | Xác thực OTP | Registered User | Provide OTP evidence during registration or password recovery. Registration has a verification step; recovery submits the OTP with the new password. These operations can occur before login. | UI + backend |
| RU-03 | Recover Password | Khôi phục mật khẩu | Registered User | Request an OTP and set a new password after successful recovery verification. The recovery interaction is reached from the login screen. | UI + backend |
| RU-04 | Change Password | Đổi mật khẩu | Registered User | Change the current account password through the personal-profile dialog, supplying the current password and the required new-password confirmation. | UI + backend |
| RU-05 | View and Update Profile | Xem và sửa hồ sơ cá nhân | Registered User | Read and update permitted personal information. Staff have a separate professional-information request; this does not grant self-assignment of system roles, duty rooms or technical capabilities. | UI + backend |
| RU-06 | Read In-app Notifications | Xem thông báo trong ứng dụng | Registered User | Read recent notifications, check the unread count and mark a displayed notification as read. In-app delivery is distinct from email or SMS delivery. | UI + backend |
| RU-07 | View Own Duty Schedule | Xem lịch trực cá nhân | Receptionist, Cashier, Nurse, Doctor, Clinic Manager | View personal weekly duty assignments as marked shifts and move between weeks. This shared staff function does not allow editing assignments. Administrator is not admitted by this frontend route. | UI + backend |
| RU-08 | Set Local Interface Preferences | Đặt tùy chọn giao diện | Receptionist, Cashier, Nurse, Doctor, Clinic Manager, Administrator | Save interface preferences available on the Settings page, including theme, language and local display or notification options. Settings are stored in the browser, not a backend configuration API. | Frontend only by design |

### Customer Self Service
Chức năng tự phục vụ của Customer.

| ID | Function name | Tên chức năng | Actor or owner | Description | Delivery |
| --- | --- | --- | --- | --- | --- |
| CU-01 | Create Appointment | Đặt lịch khám | Customer | Book active, customer-bookable services for an authorized patient profile, selecting an available date and shift. Availability is service based; the form is not a named-doctor appointment selector. | UI + backend |
| CU-02 | View Appointment List and Details | Xem lịch hẹn | Customer | Review appointment lists, selected services and status for the account or permitted family profile. Present the displayed booking information to reception when attending. | UI + backend |
| CU-03 | Cancel Appointment | Hủy lịch hẹn | Customer | Cancel an owned or manageable appointment when its state permits cancellation. Reception check-in is not a Customer confirmation action. | UI + backend |
| CU-04 | View Follow-up Information | Xem yêu cầu và lịch tái khám | Customer | Review follow-up advice in the published medical record and view an appointment once it has been scheduled. A medical follow-up recommendation is not itself a confirmed future appointment. | UI + backend |
| CU-06 | Track Examination Journey | Theo dõi hành trình khám | Customer | Track the current step, queue position and completed or skipped services for an authorized visit. A skipped patient contacts the room staff directly; the Customer interface has no queue-return action. | UI + backend |
| CU-07 | View Completed Medical Records | Xem bệnh án đã hoàn thành | Customer | Browse visit-based history and separate completed examination records within each visit. A partially completed visit can retain published clinical history; unfinished medical-record content is not a Customer publication. | UI + backend |
| CU-08 | View Prescriptions and Published Diagnostic Results | Xem đơn thuốc và kết quả CLS | Customer | Read the prescription belonging to each completed medical record and authorized signed diagnostic results. Laboratory analytes are presented by panel where supported; files remain authorized resources, and referenced same-day results are distinguished from newly performed services. | UI + backend |
| CU-09 | View Invoice and Payment History | Xem lịch sử thanh toán và phiếu thu | Customer | Filter payment history by date, method and permitted patient profile, then inspect a receipt with invoice totals, insurance and CareS payment details where applicable. | UI + backend |
| CU-11 | Send Message to Receptionist | Nhắn tin với lễ tân | Customer | Start or resume the account support conversation and send messages. Clinic Manager can also operate the staff-side support interface. | UI + backend |
| CU-12 | View Conversation Messages | Xem nội dung hội thoại | Customer | Read messages and the status of the accessible customer conversation. The Customer widget is not a separate searchable archive of all closed conversations. | UI + backend |
| CU-13 | Submit and View Medical-record Feedback | Gửi và xem đánh giá bệnh án | Customer | Submit the supported overall rating and comment for an eligible completed record and view its response. Feedback concerns the record experience; it is not a separate rating score for every doctor or nurse. | UI + backend |
| CU-14 | Manage Family Profiles | Quản lý hồ sơ gia đình | Customer | Create and update linked family profiles, stop managing a member or restore an eligible archived relationship. Reading historical family data and initiating new actions use different access checks. | UI + backend |
| CU-15 | Create Family Group Booking | Đặt lịch nhóm gia đình | Customer | Book for multiple manageable patient profiles through the family-booking flow. Each patient retains a separate appointment and later visit, rather than sharing one medical record. | UI + backend |
| CU-16 | Change Appointment Details | Thay đổi lịch hẹn | Customer | Reopen an eligible appointment in the booking flow and save permitted service, date or shift changes after availability validation. | UI + backend |
| CU-18 | Register CareS Prepaid Card | Đăng ký thẻ trả trước CareS | Customer | Register the account CareS card using the card form and required PIN confirmation. Card registration does not itself top up the balance. | UI + backend |
| CU-19 | View CareS Card and Balance History | Xem thẻ CareS và lịch sử số dư | Customer | Review card status, benefit information, balance and the balance-ledger history exposed by the Customer card screen. Top-ups are handled at the counter. | UI + backend |
| CU-20 | Print an Individual Medical Record | In bệnh án theo dịch vụ | Customer, Doctor | Open and print the specific examination record selected by record ID. Customer printing reloads authorized completed data and supports a permitted family profile; clinical data from different examination services must remain separate. | UI + backend |
| CU-21 | Print Customer Receipt | In phiếu thu của Customer | Customer | Print the authorized receipt through the shared receipt document, excluding the application navigation. This operation does not initiate or confirm payment. | UI + backend |
| CU-22 | Pay with Own CareS Card through API | Thanh toán thẻ cá nhân qua API | Customer | The Customer API accepts a card payment against an authorized invoice with the required PIN and idempotency information. No caller for this self-payment endpoint is present in the current Customer frontend. | Backend only |

### Reception and Patient Coordination
Tiếp nhận và điều phối bệnh nhân.

| ID | Function name | Tên chức năng | Actor or owner | Description | Delivery |
| --- | --- | --- | --- | --- | --- |
| RC-01 | Search Patient | Tìm hồ sơ bệnh nhân | Receptionist, Clinic Manager | Identify existing patients using the reception search functions before check-in or visit creation. Review same-day examination and signed-result information to avoid an inappropriate duplicate service. | UI + backend |
| RC-02 | Create or Complete Patient Profile | Tạo và cập nhật hồ sơ bệnh nhân | Receptionist, Clinic Manager | Enter or correct permitted identity, contact and patient-profile information during reception or in patient management. Clinical history is viewed separately from demographic editing. | UI + backend |
| RC-03 | Check In Scheduled Patient | Tiếp nhận khách có lịch hẹn | Receptionist, Clinic Manager | Review the appointment, verify patient information and confirm reception. Allowed changes can add examination services, laboratory panels or individual analytes before the visit and initial invoice are prepared. | UI + backend |
| RC-04 | Create Walk-in Visit | Tạo phiếu khám tại quầy | Receptionist, Clinic Manager | Select or enter the patient, choose examination services and diagnostic panels or analytes, review the selection and create a walk-in visit with its initial charges. Callable services depend on subsequent payment and sequencing. | UI + backend |
| RC-05 | View Visit and Ticket Information | Xem phiếu khám và lượt khám | Receptionist, Clinic Manager | Search reception visit records and inspect the selected visit, services, invoice and queue information using the visit-management interface. | UI + backend |
| RC-06 | Update Permitted Appointment Information | Sửa lịch hẹn tại lễ tân | Receptionist, Clinic Manager | Save permitted patient or appointment changes, including the service selection, or cancel an eligible booking. The reception selector supports both full panels and individual laboratory analytes. | UI + backend |
| RC-07 | Review and Coordinate Patient Journey | Theo dõi và điều phối hành trình | Receptionist, Clinic Manager | Inspect the visit timeline, current department, payment gates, blocked steps and completed or skipped services to direct the patient. Viewing a later step does not make it callable. | UI + backend |
| RC-08 | Manage Support Conversations | Quản lý hội thoại hỗ trợ | Receptionist, Clinic Manager | View active or closed conversations, open messages, reply to Customer or Guest and close an eligible session. This interface is not available to doctors, nurses or cashiers. | UI + backend |
| RC-12 | Review Patient Visit History | Xem lịch sử bệnh nhân tại lễ tân | Receptionist, Clinic Manager | Open the patient, visits and record-detail screens through reception history. Access uses the staff endpoints and is distinct from Customer family-profile authorization. | UI + backend |
| RC-13 | Recover an Eligible Blocked Journey through API | Khôi phục bước bị kẹt qua API | Clinic Manager, Administrator | Request recovery of an eligible blocked journey through the management API. The current staff journey screen is explicitly read-only and has no advance action. The backend checks dependencies; this is not an unrestricted override of clinical completion. | Backend only |
| RC-14 | Open Queue Display Screens | Mở màn hình gọi bệnh nhân | Receptionist, Clinic Manager | Use the launcher to open the overall calling screen or a selected room display in an authenticated browser tab. The launcher itself is a staff function, not an external actor. | UI + backend |
| RC-15 | View Overall or Room Calling Display | Xem màn gọi tổng hoặc từng phòng | Receptionist, Clinic Manager, Doctor, Nurse, Administrator | Display only CALLED patients on the overall screen. The room screen shows the called or in-progress patient and next waiting patients, using names and birth years where available rather than treating ticket numbers as names. | UI + backend |

### Billing and Payment
Hóa đơn và thanh toán.

| ID | Function name | Tên chức năng | Actor or owner | Description | Delivery |
| --- | --- | --- | --- | --- | --- |
| CA-01 | View Pending Invoices | Xem hóa đơn chờ thanh toán | Cashier, Clinic Manager | Filter and inspect invoices requiring attention in the cashier invoice list. Listing an invoice does not acknowledge receipt of money. | UI + backend |
| CA-02 | View Invoice Details | Xem chi tiết hóa đơn | Cashier, Clinic Manager | Review service items, insurance, existing reductions, payments and the remaining amount. CareS payment metadata is read from receipt data when available. | UI + backend |
| CA-03 | Confirm Cash Payment | Xác nhận thanh toán tiền mặt | Cashier, Clinic Manager | Confirm the supported cashier cash-payment operation. Payment updates the invoice and downstream service eligibility; the UI action is not a general editor for arbitrary transaction states. | UI + backend |
| CA-04 | Monitor Online Payment | Theo dõi thanh toán PayOS | Cashier, Clinic Manager | Check invoice payment status after opening the PayOS link. Verified gateway callbacks are handled by the backend; opening a link alone is not proof of payment. | UI + backend |
| CA-05 | Review Invoice Payment History | Tra cứu lịch sử hóa đơn đã thu | Cashier, Clinic Manager | Search historical invoices and inspect their payment information through the cashier screens. There is no separate current frontend page for arbitrary Transaction CRUD. | UI + backend |
| CA-06 | Print Receipt | In phiếu thu tại quầy | Cashier, Clinic Manager | Open the web/A4 receipt with service charges, insurance, payment method and masked CareS information where applicable. Print the receipt document rather than the application sidebar. | UI + backend |
| CA-07 | Verify and Apply Health Insurance | Kiểm tra và áp dụng BHYT | Cashier, Clinic Manager | Submit the insurance information and apply permitted coverage to the invoice. Verification uses the configured mock BHXH integration; this is not an official insurance claim submission. | UI + backend |
| CA-08 | Accept CareS Card Payment | Thu tiền bằng thẻ CareS | Cashier, Clinic Manager | Validate the card and PIN, optionally apply eligible card benefits and pay the remaining invoice amount. Benefits apply to the eligible patient-payable amount, not a second deduction of insurer coverage. | UI + backend |
| CA-09 | Top Up CareS Card | Nạp tiền thẻ CareS | Cashier, Clinic Manager | Record an eligible card top-up at the counter, with the required amount and idempotency key, and provide the resulting top-up receipt. | UI + backend |
| CA-10 | Review and Print Top-up History | Xem và in lịch sử nạp thẻ | Cashier, Clinic Manager | Browse recorded CareS top-ups and open their receipt representation. The top-up receipt is distinct from an examination invoice receipt. | UI + backend |
| CA-11 | Cancel an Eligible Invoice | Hủy hóa đơn đủ điều kiện | Cashier, Clinic Manager | Cancel an invoice only when its payment and service state allow it. This is not a general refund action for already delivered services. | UI + backend |
| CA-12 | Create PayOS Payment Link | Tạo link thanh toán PayOS | Cashier, Clinic Manager | Create and open a payment link from the cashier invoice screen. The current Customer frontend does not initiate this endpoint. | UI + backend |

### Nursing and Clinical Preparation
Điều dưỡng và chuẩn bị khám.

| ID | Function name | Tên chức năng | Actor or owner | Description | Delivery |
| --- | --- | --- | --- | --- | --- |
| NU-01 | View Assigned Queue | Xem hàng chờ được phân công | Nurse | View the assigned room, current patient and waiting patients, including blocked and absent states. Clinical-room access remains subject to backend assignment checks. | UI + backend |
| NU-02 | Call or Mark Patient Absent | Gọi hoặc đánh dấu vắng | Nurse | Call or recall the permitted patient and mark absence through room queue actions. Calling order and patient availability are enforced by the backend. | UI + backend |
| NU-03 | Receive and Prepare Patient | Tiếp nhận và chuẩn bị bệnh nhân | Nurse | Confirm arrival in the permitted room workflow and prepare the patient. Starting an examination requires a suitable on-duty treating doctor; the nurse does not become the treating doctor. | UI + backend |
| NU-04 | Record Vital Signs | Ghi dấu hiệu sinh tồn | Nurse, Doctor | Record or update vital signs for the active examination. Later examination records in the same visit can receive independent copies of valid source measurements; editing one record does not edit every copy. | UI + backend |
| NU-05 | View Relevant Patient Information | Xem thông tin phục vụ chăm sóc | Nurse | Review patient details and relevant history available in the assigned clinical workflow. Customer and family authorization rules are separate from staff access rules. | UI + backend |
| NU-06 | Save Nursing Preparation Notes | Lưu ghi nhận hỗ trợ khám | Nurse | Save permitted complaint, clinical preparation notes and vital signs in a nursing draft. This does not authorize prescribing, final diagnosis, diagnostic ordering or medical-record completion. | UI + backend |

### Medical Examination
Khám bệnh.

| ID | Function name | Tên chức năng | Actor or owner | Description | Delivery |
| --- | --- | --- | --- | --- | --- |
| DR-01 | View Examination Queue | Xem hàng chờ khám bệnh | Doctor | Review assigned examination-room patients, service order and current work. Display position is calculated separately from the fixed ticket number. | UI + backend |
| DR-02 | Call and Receive Patient | Gọi và tiếp nhận bệnh nhân | Doctor | Call or recall the permitted patient, mark absence, restore an eligible same-day skipped patient after direct confirmation, or begin service. Duty, room and visit checks prevent cross-room handling. | UI + backend |
| DR-03 | View Medical Records and History | Xem bệnh án và tiền sử | Doctor | Read the current patient history, allergies and available previous examination records before making clinical decisions. Access and publication differ from the Customer view. | UI + backend |
| DR-04 | Record Medical Examination | Ghi nhận khám lâm sàng | Doctor | Record symptoms and clinical findings for the current examination service. Each service has its own medical record, rather than completing every service in the visit together. | UI + backend |
| DR-05 | Record Diagnosis and Treatment Plan | Ghi chẩn đoán và hướng điều trị | Doctor | Select ICD-10 reference codes and record diagnosis, conclusion, treatment direction and patient instructions in the responsible doctor medical record. | UI + backend |
| DR-06 | Create Prescription | Kê đơn thuốc | Doctor | Enter medication, quantity and usage instructions for the current record, using the available medicine lookup. Prescription validation includes the required allergy-verification state. | UI + backend |
| DR-07 | Request Diagnostic Services | Chỉ định cận lâm sàng | Doctor | Choose diagnostic services, complete panels or individual laboratory analytes and confirm the order. The backend normalizes selection and billing; this use case does not include referral to another examination specialty. | UI + backend |
| DR-09 | Review Diagnostic Results | Xem kết quả cận lâm sàng | Doctor | Open read-only result or status dialogs from the examination screen without navigating to the Lab workbench. Completed results expose the supported values, conclusions and authorized files; unfinished requests show progress rather than editable result fields. | UI + backend |
| DR-10 | Save Medical Record as Draft | Lưu nháp bệnh án | Doctor | Save incomplete examination content in the responsible doctor record. Version checks reject stale updates, and completed records cannot be rewritten through the normal draft flow. | UI + backend |
| DR-11 | Resume Examination after Diagnostic Services | Khám lại sau cận lâm sàng | Doctor | Resume the source examination when its required diagnostic work is complete, review results and continue the same record. Later examination services remain dependent on completion of this record. | UI + backend |
| DR-12 | Complete Medical Record | Hoàn thành bệnh án | Doctor | Confirm completion of the current examination record. When diagnostic work is still required, the order workflow waits for results instead of closing the record; otherwise it can release the next eligible service. | UI + backend |
| DR-13 | Arrange Follow-up from Examination | Thiết lập tái khám từ màn khám | Doctor | Enter the follow-up date, service and advice, review the confirmation dialog and create the linked follow-up appointment through the examination screen. Follow-up fields also travel with examination draft/completion. This direct booking action coexists with the separate reception follow-up workflow; it is not a specialist referral. | UI + backend |
| DR-14 | View Same-day Clinical Information | Xem thông tin chuyên môn cùng ngày | Doctor | Review other completed examination information and signed same-day diagnostic results where authorized. Reused reference results are not newly billed or performed results in the current visit. | UI + backend |
| DR-15 | Verify Patient Allergies | Xác minh dị ứng | Doctor, Nurse | Review and update the supported allergy assessment for the patient in the clinical context, including verified absence of known allergies or a recorded allergy history. | UI + backend |
| DR-16 | Continue Next Examination in the Same Room | Tiếp tục dịch vụ khám cùng phòng | Doctor | After confirming one service, continue the next eligible same-room examination using a separate record. A pending diagnostic cycle prevents premature completion of the chain. | UI + backend |
| DR-17 | Finish Carried-over Clinical Work | Xử lý hồ sơ chuyên môn tồn đọng | Doctor | Resume eligible clinical work already started before the current day and finish it under current operational checks. This does not reactivate old unstarted services cancelled or skipped at day close. | UI + backend |

### Diagnostic and Laboratory Services
Cận lâm sàng và xét nghiệm.

| ID | Function name | Tên chức năng | Actor or owner | Description | Delivery |
| --- | --- | --- | --- | --- | --- |
| DS-01 | View Diagnostic Queue and Panels | Xem hàng chờ và phiếu CLS | Doctor, Nurse | Browse requests for the assigned diagnostic room. Individually purchased analytes are grouped by their source panel and execution context; distinct panels remain separate. | UI + backend |
| DS-02 | Call and Receive Diagnostic Patient | Gọi và tiếp nhận tại phòng CLS | Doctor, Nurse | Call the next eligible patient and confirm the start of diagnostic work in the assigned room. Payment and preceding journey steps determine whether a request is ready. | UI + backend |
| DS-03 | View Diagnostic Request | Xem chỉ định CLS | Doctor, Nurse | Inspect the diagnostic request or panel, source record, service and current action permissions. A panel workbench shows which analytes were purchased and which are locked. | UI + backend |
| DS-04 | Record Specimen Information | Ghi thông tin mẫu bệnh phẩm | Doctor, Nurse | Review generated specimen information and edit the permitted specimen fields where required. Related purchased requests in a panel use consistent specimen details. | UI + backend |
| DS-05 | Perform Diagnostic Service | Thực hiện dịch vụ CLS | Doctor, Nurse | Work on the diagnostic service already started in the room, using its configured form. Unpurchased analytes remain non-editable; a physical laboratory act alone does not mark a result signed in the system. | UI + backend |
| DS-06 | Enter Diagnostic Result | Nhập kết quả CLS | Doctor, Nurse | Enter values and conclusion for permitted purchased services. Required fields need a valid result or a supported omission reason; partial completion does not automatically refund the invoice. | UI + backend |
| DS-07 | Attach Result Files | Đính kèm tệp kết quả | Doctor, Nurse | Upload supported result files or revision attachments after execution has begun, under file validation and room permissions. Files remain linked to the request or result revision. | UI + backend |
| DS-08 | Save Diagnostic Draft | Lưu nháp kết quả CLS | Doctor, Nurse | Persist unfinished result data and omission reasons for a request or purchased panel members. Draft saving does not publish the result to the Customer. | UI + backend |
| DS-09 | Sign and Publish Diagnostic Result | Ký và công bố kết quả CLS | Doctor | Confirm the diagnostic result in the performing room as an eligible on-duty doctor. A Nurse may prepare the draft but cannot sign it. Cancelled requests cannot receive a new signed result. | UI + backend |
| DS-10 | Complete Diagnostic Request | Hoàn tất yêu cầu CLS | Doctor | Complete purchased diagnostic requests through the result-confirmation operation. Shared queue completion and return-to-doctor readiness depend on all relevant requests; this is an outcome of signing, not an additional independent completion button. | UI + backend |
| DS-11 | Handle Diagnostic Absence and Return | Xử lý vắng và quay lại phòng CLS | Doctor, Nurse | Doctor or Nurse may mark an eligible called patient absent; only the on-duty Doctor may restore the patient to that room's queue on the same day. | UI + backend |
| DS-12 | Cancel an Eligible Diagnostic Request | Hủy yêu cầu CLS đủ điều kiện | Doctor | Cancel a request when the performing-room doctor, current duty and request state permit it. This is distinct from refunding a paid service. | UI + backend |
| DS-13 | View Result Revision History | Xem lịch sử phiên bản kết quả | Doctor, Nurse | Read result revision history and authorized revision attachments in the diagnostic detail flow. Viewing an old revision does not edit a published result. | UI + backend |
| DS-14 | Amend a Signed Result through API | Đính chính kết quả qua API | Doctor | Create an amendment, update its draft and sign it under performing-room doctor checks. These endpoints exist, but the current LabDetailPage does not expose an amendment-authoring action. | Backend only |

### Clinic Operations Management
Quản lý vận hành phòng khám.

| ID | Function name | Tên chức năng | Actor or owner | Description | Delivery |
| --- | --- | --- | --- | --- | --- |
| CM-01 | View Operational Reports | Xem thống kê vận hành | Clinic Manager | Select the reporting period and inspect overview, collections/invoices and room activity. Service and room detail tables use their own search filters and fixed pagination without changing period totals. | UI + backend |
| CM-02 | Manage Staff Information | Quản lý thông tin nhân sự | Clinic Manager, Administrator | Create staff accounts or update permitted staff profile, photo, specialization, room assignment and activity details through account management. The Manager Staff page itself is a directory and detail view. | UI + backend |
| CM-03 | Manage Staff Schedules | Phân công lịch trực | Clinic Manager, Administrator | View weekly staffing, assign staff to shifts and copy an eligible previous-week schedule. Validation checks conflicts and prevents unsupported changes to past assignments; no separate publish-schedule step is assumed. | UI + backend |
| CM-04 | Manage Services and Prices | Quản lý dịch vụ và giá | Clinic Manager, Administrator | Create or update allowed service details, price and booking settings; publish, deactivate or delete only where permitted. Fixed laboratory structures and linked-service constraints are not arbitrary panel-design CRUD. | UI + backend |
| CM-05 | Manage Departments and Rooms | Quản lý phòng | Clinic Manager, Administrator | Maintain rooms, descriptions, allowed service types, capabilities and staff associations through room management. Selecting an existing specialty is not editing the specialty reference catalog. | UI + backend |
| CM-06 | Manage Medical-record Feedback | Quản lý đánh giá bệnh án | Clinic Manager, Receptionist | Review overall record feedback, filter unanswered items and submit responses. Doctor, Nurse, Cashier and Administrator do not receive this feedback-management route or API permission. | UI + backend |
| CM-08 | Manage Public Announcements | Quản lý thông báo công khai | Clinic Manager, Administrator | Create, edit, publish or withdraw and delete supported public announcements. Public visibility and local dismissal are separate from staff in-app notifications. | UI + backend |
| CM-09 | Manage Diagnostic Form Templates | Quản lý biểu mẫu CLS | Clinic Manager | Create or version permitted diagnostic forms, edit drafts, bind services, publish and retire forms. System laboratory forms are protected; this UI does not allow rewriting their fixed analytes and reference ranges. | UI + backend |
| CM-10 | Print or Export Reports | In và xuất báo cáo | Clinic Manager | Choose an available report type, preview and print it or export CSV. Exports use all matching loaded rows, not only the current page, and preserve the selected period and relevant detail filter. | UI + backend |
| CM-11 | View Patient Directory | Xem danh sách bệnh nhân | Clinic Manager | Search the manager patient directory, inspect patient details and open permitted visit history. The directory is not an independent clinical-editing workbench. | UI + backend |
| CM-12 | Configure CareS Policy | Cấu hình chính sách thẻ CareS | Clinic Manager, Administrator | Review and update supported prepaid-card policy values such as minimum top-up, benefit percentage and validity period. Existing card/payment rules remain enforced by the service. | UI + backend |
| CM-13 | Review CareS Ledger and Reverse Eligible Payment | Xem sổ thẻ và hoàn tác đủ điều kiện | Clinic Manager, Administrator | Review card ledger entries and reverse an eligible erroneous card payment with a reason and idempotency key. This is not a Cashier action or unrestricted refund; service-start and existing reversal checks apply. | UI + backend |

### System Administration
Quản trị hệ thống.

| ID | Function name | Tên chức năng | Actor or owner | Description | Delivery |
| --- | --- | --- | --- | --- | --- |
| AD-01 | Manage User Accounts | Quản lý tài khoản | Administrator, Clinic Manager | Search staff or customer accounts, edit permitted fields, lock/unlock accounts and reset a password through account management. Account constraints can reject self-locking or unsupported removal. | UI + backend |
| AD-02 | Assign Supported Staff Roles and Capabilities | Gán vai trò và năng lực nhân sự | Administrator, Clinic Manager | Assign supported fixed staff roles and permitted professional/room/capability information. The system has no arbitrary permission-matrix or new-role editor; Doctor eligibility is checked again during clinical operations. | UI + backend |
| AD-03 | Manage Clinic Information | Cập nhật thông tin phòng khám | Administrator, Clinic Manager | Edit the clinic information exposed by the configuration screen and public site. Room, service, shift and announcement management use their own shared use cases. | UI + backend |
| AD-05 | Manage ICD-10 Catalog through API | Quản lý ICD qua API | Administrator | Create, update or delete ICD-10 reference entries through the administrator API. The current frontend provides diagnosis lookup, but no registered ICD catalog-management page. | Backend only |
| AD-06 | Review Audit Information | Xem nhật ký hệ thống | Administrator, Clinic Manager | Search the available audit events and inspect entity-related changes through the audit screen. This does not provide a general server-monitoring or integration-health console. | UI + backend |
| AD-07 | Manage Shift Versions | Quản lý phiên bản ca | Administrator, Clinic Manager | Review fixed shifts, preview the impact of changed hours and create a future-effective shift version with a reason. Existing historical versions are retained. | UI + backend |
| AD-09 | Manage Operating Exceptions | Quản lý lịch hoạt động ngoại lệ | Administrator, Clinic Manager | Preview and configure future clinic closures, shift-off dates or special hours, and reopen an eligible exception. Impact checks can block changes affecting existing operations. | UI + backend |
| AD-10 | Check Service Coverage | Kiểm tra khả năng phục vụ theo ca | Administrator, Clinic Manager | Check service availability for a date and shift to identify missing room or staffing coverage before changing schedules or accepting bookings. | UI + backend |
| AD-11 | View Fixed Technical Catalog | Xem danh mục kỹ thuật cố định | Administrator, Clinic Manager | Search and read the fixed technical-capability catalog. Both the interface and controller reject catalog creation, editing and deletion. Assigning an existing capability to staff is a separate operation. | UI + backend |
| AD-12 | Manage Schedule Templates through API | Quản lý mẫu lịch qua API | Administrator | Maintain recurring staff schedule templates through the backend endpoints. This is distinct from weekly assignment and copying in SchedulePage; no dedicated template-management route is registered. | Backend only |
| AD-13 | Administer Notification Records through API | Quản trị bản ghi thông báo qua API | Administrator | Use administrator endpoints to inspect and maintain notification records and their delivery flags. Setting SENT is not evidence that an email or SMS was actually delivered; no notification-administration page is registered. | Backend only |

### External Integrations
Tích hợp bên ngoài.

| ID | Function name | Tên chức năng | Actor or owner | Description | Delivery |
| --- | --- | --- | --- | --- | --- |
| EXT-01 | Process Online Payment | Xử lý thanh toán trực tuyến | PayOS | Receive a supported payment-link request and return gateway payment information or a verified callback. The backend applies the payment outcome; a browser redirect alone does not confirm settlement. | External integration |
| EXT-02 | Simulate Insurance Verification | Mô phỏng xác minh BHYT | Mock BHXH Service | Return simulated insurance verification through the configured service. The built-in demo-card branch also simulates success locally; neither path is represented as an official BHXH claim or certification. | External integration |
| EXT-03 | Deliver Verification and Contact Messages | Gửi thông điệp xác thực và liên hệ | Email Service, SMS Gateway | Support outbound verification messages. Email also sends the public contact enquiry to the clinic mailbox. SMS is configured as mock in the inspected application configuration; provider implementations are not proof of live delivery. | External integration |

### Internal System Functions
Chức năng nội bộ của hệ thống.

| ID | Function name | Tên chức năng | Actor or owner | Description | Delivery |
| --- | --- | --- | --- | --- | --- |
| SYS-01 | Enforce Authentication and Role Access | Kiểm soát xác thực và quyền theo vai trò | CareS System | Validate access tokens, derive current authorities and apply route, controller and service-level access checks. A controller role alone does not bypass ownership, duty or clinical-state rules. | Internal automatic |
| SYS-02 | Enforce Patient and Family Data Access | Kiểm soát quyền dữ liệu bệnh nhân và gia đình | CareS System | Resolve the owner profile, active managed-family permissions and permitted historical-family reading before returning or changing patient data. | Internal automatic |
| SYS-03 | Normalize Medical Service Selection | Chuẩn hóa lựa chọn dịch vụ y tế | CareS System | Validate selected examinations, diagnostic panels and individual analytes, remove duplicates and resolve the billable service set before booking, reception or ordering. | Internal automatic |
| SYS-04 | Resolve Shift and Service Availability | Xác định ca và khả năng cung cấp dịch vụ | CareS System | Resolve the effective shift version and check clinic exceptions, room coverage and available service capacity for the selected date. | Internal automatic |
| SYS-05 | Validate Staff Duty and Clinical Eligibility | Kiểm tra lịch trực và điều kiện nghiệp vụ của nhân viên | CareS System | Confirm that the current staff member is assigned to the department and shift and meets the examination-specialization or diagnostic-capability rule required by the requested action. | Internal automatic |
| SYS-06 | Rank the Queue and Validate the Next Call | Xếp hạng hàng chờ và kiểm tra lượt gọi tiếp theo | CareS System | Calculate waiting positions and priority categories, preserve the active head of the queue and reject a call that does not target the next eligible ticket. | Internal automatic |
| SYS-07 | Coordinate Sequential Patient Journey | Điều phối hành trình bệnh nhân tuần tự | CareS System | Keep examination, payment, diagnostic rooms, return-to-doctor and subsequent examination steps in valid order while maintaining the current journey step. | Internal automatic |
| SYS-08 | Group Laboratory Analytes into Panels | Gom chỉ số xét nghiệm theo gói | CareS System | Present individually purchased analytes as their source panel, open only purchased inputs and preserve the underlying request and invoice-item traceability. | Internal automatic |
| SYS-09 | Manage Diagnostic Result Revisions | Quản lý phiên bản kết quả cận lâm sàng | CareS System | Persist draft and signed result revisions, omission metadata, specimen details and permitted attachments without publishing unfinished content to Customer history. | Internal automatic |
| SYS-10 | Synchronize Queue and Operational Screens | Đồng bộ hàng chờ và màn hình vận hành | CareS System | Publish queue, Lab, return-request, chat, notification, card and journey update events so authenticated screens can refresh without changing the underlying transaction outcome. | Internal automatic |
| SYS-11 | Record Audit Events | Ghi nhận nhật ký kiểm toán | CareS System | Capture configured create, update, delete and status-change events with relevant before/after snapshots for authorized audit review. | Internal automatic |
| SYS-12 | Close Expired Daily Work | Chốt các công việc hết ngày | CareS Scheduler | At 00:05 Asia Ho Chi Minh time, cancel or skip eligible old unstarted work and retain clinical work that must finish through the clinical workflow. | Internal scheduled |
| SYS-13 | Protect Transaction and Queue Idempotency | Bảo vệ tính idempotent của thanh toán và hàng chờ | CareS System | Use idempotency keys, state rechecks and database locking where implemented to avoid duplicate card operations, duplicate return confirmations or inconsistent concurrent queue actions. | Internal automatic |
| SYS-14 | Calculate Operational Reports | Tính toán báo cáo vận hành | CareS System | Aggregate the requested reporting period for overview, collection, invoice, service and room-activity views while keeping report definitions consistent across screen, print and CSV output. | Internal automatic |
| SYS-15 | Create and Dispatch Operational Notifications | Tạo và phát thông báo vận hành | CareS System | Create in-app notifications and publish real-time notification events. Email and configured SMS delivery remain separate integration paths; a stored SENT flag alone is not proof of external delivery. | Internal automatic |

## Reading and Modeling Notes

- Registered User is an abstract parent for shared account behavior, not a concrete system role.
- CareS uses one Doctor actor. Examination specialization, diagnostic capability, department assignment, duty and record responsibility determine which clinical function the account may perform.
- Nurse can support queues, vital signs, preparation and permitted diagnostic drafts. Nurse does not sign results or complete a medical examination.
- Clinic Manager and Receptionist manage feedback. The fact that a Doctor or Nurse participated in a rated record does not create a feedback-management function for that actor.
- TV queue displays are CareS interfaces. They are not external actors and do not decide the next patient.
- PayOS, Mock BHXH Service, Email Service and SMS Gateway are integrations. Current SMS configuration is mock, and Mock BHXH is not an official certified insurance connection.
- Backend-only functions must be labeled as such in diagrams and reports. They should not be presented as completed screen workflows.
- For route, endpoint, service and test evidence, use [Bilingual Use Case Traceability](../use-cases/traceability.md), [Route Inventory](../use-cases/route-inventory.md), [Endpoint Inventory](../use-cases/endpoint-inventory.md) and [Audit Findings](../use-cases/audit-findings.md).
