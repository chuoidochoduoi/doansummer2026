import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const source = String.raw`C:\Users\Administrator\Downloads\SU26_SEP490_G66_Report_5.4_Acceptance_Test.xlsx`;
const outputDir = String.raw`D:\gitlap\doAnSummer2026\outputs\019fc0c3-dee6-7a11-9b42-26e3805728ed`;
const outputPath = `${outputDir}\\CareS_Report_5.4_Acceptance_Test.xlsx`;

const tc = (criteria, objective, role, page, action, expected) => ({
  criteria,
  objective,
  steps: role === "Public user"
    ? `1. Open the CareS website without signing in.\n2. Open ${page}.\n3. ${action}\n4. Observe the displayed information and system response.`
    : `1. Sign in with a valid ${role} account.\n2. Open ${page}.\n3. ${action}\n4. Confirm the action when the system requests confirmation.\n5. Observe the updated data and notification.`,
  expected,
});

const groups = [
  ["Global UI, Public Pages & Usability", [
    tc("Public home page", "Verify visitors can view the clinic overview and reach primary actions", "Public user", 'the Home page', 'Review the clinic summary, featured services, doctors, and click "Đặt lịch khám".', 'The page loads without broken content; clinic information comes from the configured clinic data and the booking action opens the appointment flow.'),
    tc("Public service catalog", "Verify visitors can browse active medical services", "Public user", 'the "Dịch vụ" page', 'Search for a service and open its displayed information.', 'Only active public services are shown; search narrows the list and displayed name, category, duration, and price are consistent.'),
    tc("Public doctor directory", "Verify visitors can browse doctors and specialties", "Public user", 'the "Đội ngũ bác sĩ" page', 'Filter or review doctors by specialty.', 'The list shows available doctors with the correct name, specialty, and public profile information.'),
    tc("Public clinic schedule", "Verify visitors can view the clinic schedule", "Public user", 'the "Lịch làm việc" page', 'Select a date or department from the available filters.', 'The schedule updates to the selected criteria and does not expose internal staff-only information.'),
    tc("About and legal information", "Verify visitors can view the clinic identity and legal information", "Public user", 'the "Về chúng tôi" page', 'Review the clinic address, contact details, and operating-license information.', 'The page displays the current clinic information from the system and no placeholder or sample wording remains.'),
    tc("Contact request", "Verify a visitor can submit a support request", "Public user", 'the "Liên hệ" page', 'Enter full name "Nguyễn An", phone "0912345678", a valid email, subject and message, then click "Gửi yêu cầu".', 'A success notification is displayed and the request is available to authorized support staff.'),
    tc("Responsive navigation", "Verify navigation remains usable at common desktop and mobile widths", "Public user", 'the Home, Services, About, Login, and Appointment pages', 'Resize the browser and use the header or mobile menu to move between pages.', 'Menus, buttons, forms, and cards remain readable without overlap or clipped controls.'),
    tc("Validation presentation", "Verify invalid public forms show understandable validation", "Public user", 'the Contact or Registration page', 'Leave required fields blank and submit the form.', 'Required fields are identified near the relevant input; entered values are preserved and no invalid record is created.'),
  ]],
  ["Registration, Authentication & Password Recovery", [
    tc("Customer registration", "Verify a new customer can register with valid information", "Public user", 'the "Đăng ký" page', 'Enter a valid full name, date of birth, gender, phone, email and password, then click "Đăng ký".', 'The system sends an OTP and moves to the OTP verification step without creating a duplicate account.'),
    tc("Registration validation", "Verify invalid registration information is rejected", "Public user", 'the "Đăng ký" page', 'Enter an invalid phone or email, mismatched passwords, or omit a required field, then submit.', 'The correct field-level message is shown and registration is not completed.'),
    tc("Registration OTP", "Verify registration is completed with a valid OTP", "Public user", 'the registration OTP step', 'Enter the OTP delivered to the registered contact and click "Xác nhận".', 'The customer account and profile are activated and the user can continue to the sign-in page.'),
    tc("Invalid or expired OTP", "Verify invalid or expired registration OTP is rejected", "Public user", 'the registration OTP step', 'Enter an incorrect or expired OTP and click "Xác nhận".', 'The system reports that the OTP is invalid or expired and does not activate the account.'),
    tc("Customer login", "Verify a customer can sign in", "Public user", 'the "Đăng nhập" page', 'Enter valid customer credentials and click "Đăng nhập".', 'The customer is authenticated and redirected to the appropriate customer screen.'),
    tc("Staff login and role routing", "Verify staff are routed to role-appropriate screens", "Public user", 'the "Đăng nhập" page', 'Sign in successively with Receptionist, Cashier, Doctor, Laboratory Staff, Administrator, and Clinic Manager demo accounts.', 'Each account reaches its permitted landing page and cannot see navigation items outside its role.'),
    tc("Invalid login", "Verify incorrect credentials do not create a session", "Public user", 'the "Đăng nhập" page', 'Enter a valid account identifier with an incorrect password and click "Đăng nhập".', 'A Vietnamese authentication error is displayed and protected pages remain inaccessible.'),
    tc("Forgot password", "Verify a user can request a password-reset OTP", "Public user", 'the "Quên mật khẩu" flow', 'Enter the registered email or phone and click the button to send an OTP.', 'A reset OTP is sent and the UI does not disclose whether unrelated accounts exist.'),
    tc("Reset password", "Verify a user can set a new password with a valid OTP", "Public user", 'the password-reset confirmation step', 'Enter the valid OTP, a compliant new password and matching confirmation, then submit.', 'The password is changed, the OTP cannot be reused, and the new password works at sign-in.'),
    tc("Logout", "Verify an authenticated session can be terminated", "Customer", 'the account menu', 'Click "Đăng xuất" and then try to open a protected customer URL.', 'The user returns to the public or login page and protected content requires authentication again.'),
  ]],
  ["Customer Profile & Family Members", [
    tc("View own profile", "Verify a customer can view the stored personal profile", "Customer", '"Hồ sơ cá nhân"', 'Review identity, contact, address, and health-insurance information.', 'The page shows the signed-in customer’s current information and does not expose another customer’s data.'),
    tc("Update own profile", "Verify a customer can update editable profile fields", "Customer", '"Hồ sơ cá nhân"', 'Click "Chỉnh sửa", update phone/address information with valid values, and click "Lưu".', 'A success notification appears and the refreshed profile shows the saved values.'),
    tc("Profile validation", "Verify invalid profile values are rejected", "Customer", '"Hồ sơ cá nhân"', 'Enter an invalid phone, email, or date of birth and click "Lưu".', 'The invalid fields are identified and the previous stored profile remains unchanged.'),
    tc("View family members", "Verify a customer can view linked family members", "Customer", '"Thành viên gia đình"', 'Review the member list and open one member.', 'Only members linked to the signed-in customer are displayed with their correct relationship and basic details.'),
    tc("Add family member", "Verify a customer can add a family member", "Customer", '"Thành viên gia đình"', 'Click "Thêm thành viên", enter valid identity, relationship, gender, and date-of-birth information, then save.', 'The member is created and appears in the family-member selector used by appointment booking.'),
    tc("Family-member validation", "Verify duplicate or invalid family-member data is rejected", "Customer", '"Thành viên gia đình"', 'Submit a member with missing required information or invalid date/relationship data.', 'The system displays the relevant validation message and does not add an invalid member.'),
    tc("Update family member", "Verify a customer can edit an existing family member", "Customer", '"Thành viên gia đình"', 'Open a member, click "Chỉnh sửa", change an editable field, and save.', 'The updated information is shown in both the member detail and appointment patient selector.'),
    tc("Remove family member", "Verify a customer can remove an eligible family member", "Customer", '"Thành viên gia đình"', 'Choose a member without a blocking active visit, click "Xóa", and confirm.', 'The member is removed from the active list and can no longer be selected for a new appointment.'),
  ]],
  ["Appointments & Check-in", [
    tc("Book for self", "Verify a customer can book an available service for themself", "Customer", '"Đặt lịch khám"', 'Select "Tôi", an active service, an available date and time slot, enter the reason for visit, and click "Xác nhận đặt lịch".', 'A pending appointment is created with the selected patient, service, date, and slot.'),
    tc("Book for family member", "Verify a customer can book for a linked family member", "Customer", '"Đặt lịch khám"', 'Select a family member, an active service, an available date and slot, then confirm.', 'The appointment is linked to the selected family member and appears in the customer’s appointment list.'),
    tc("Guest appointment", "Verify a guest can request an appointment without an account", "Public user", 'the public appointment page', 'Enter guest full name, phone, email, age and gender; select a service and available slot; then submit.', 'A guest appointment is created with pending status and the provided contact information.'),
    tc("Unavailable slot", "Verify a slot cannot be booked twice", "Customer", '"Đặt lịch khám"', 'Select a slot that has become unavailable and submit the appointment.', 'The system reports the conflict, refreshes availability, and does not create a duplicate booking.'),
    tc("Online booking restriction", "Verify a non-bookable service cannot be selected online", "Customer", '"Đặt lịch khám"', 'Search for a service configured with customer booking disabled.', 'The service is hidden or disabled for online booking and cannot be submitted.'),
    tc("View appointment list", "Verify customers can search and filter their appointments", "Customer", '"Lịch hẹn của tôi"', 'Filter by patient and status, then clear the filters.', 'Matching appointments are displayed and clearing filters restores the full authorized list.'),
    tc("View appointment details", "Verify appointment details contain the confirmed booking data", "Customer", '"Lịch hẹn của tôi"', 'Open a pending appointment.', 'The detail page shows patient, service, scheduled time, status, visit reason, and available actions.'),
    tc("Reschedule appointment", "Verify an eligible pending appointment can be rescheduled", "Customer", 'an appointment detail page', 'Click "Đổi lịch", choose another available date and slot, then confirm.', 'The appointment keeps its identity and patient but displays the new scheduled time.'),
    tc("Cancel appointment", "Verify an eligible appointment can be cancelled", "Customer", 'an appointment detail page', 'Click "Hủy lịch", enter or select a cancellation reason, and confirm.', 'The status changes to Cancelled and the former slot becomes available according to current rules.'),
    tc("Receptionist check-in", "Verify a receptionist can check in a valid appointment", "Receptionist", '"Tiếp nhận / Check-in"', 'Search by appointment code or patient phone, open the appointment, verify services, and click "Check-in".', 'A customer visit is created or linked, the appointment becomes checked in, and initial payment/service steps are prepared.'),
    tc("Early or late check-in", "Verify check-in timing rules are communicated", "Receptionist", '"Tiếp nhận / Check-in"', 'Open an appointment outside its permitted check-in window and attempt check-in.', 'The system blocks or warns according to the configured rule and does not silently create an invalid visit.'),
  ]],
  ["Reception, Walk-in Visits & Patient Records", [
    tc("Search patient", "Verify receptionists can locate a patient", "Receptionist", '"Hồ sơ bệnh nhân"', 'Search by patient code, name, or phone number.', 'The list shows only matching patients and opening a row displays the correct patient profile.'),
    tc("View patient history", "Verify receptionists can view authorized visit history", "Receptionist", 'a patient detail page', 'Open the visit-history section and select a completed visit.', 'The visit, services, records, and payment summary are displayed without exposing unrelated patients.'),
    tc("Create walk-in visit", "Verify a receptionist can create a visit for a direct customer", "Receptionist", '"Tạo phiếu khám"', 'Select or create the patient, choose examination services or packages, mark the visit as walk-in, and save.', 'A new visit code is created and the selected services are ready for invoicing.'),
    tc("Walk-in required fields", "Verify a walk-in visit requires patient and service information", "Receptionist", '"Tạo phiếu khám"', 'Leave the patient or service selection empty and click the create button.', 'The missing selection is identified and no visit or invoice is created.'),
    tc("Select service package", "Verify a receptionist can add an active examination package", "Receptionist", '"Tạo phiếu khám"', 'Search for an active package, select it, review included services, and add it to the visit.', 'The package appears once, its included services are displayed correctly, and the estimated amount is updated.'),
    tc("Prevent duplicate service", "Verify the same service cannot be added repeatedly to one visit", "Receptionist", '"Tạo phiếu khám"', 'Add a service and attempt to add the identical service again.', 'The service remains a single selection or a clear duplicate warning is shown.'),
    tc("Manage active visits", "Verify receptionists can search and filter current visits", "Receptionist", '"Danh sách lượt khám"', 'Search by visit code or patient and filter by visit status.', 'The list updates to matching visits and shows the correct current status for each visit.'),
    tc("Reception support requests", "Verify authorized staff can review public contact requests", "Receptionist", '"Hỗ trợ khách hàng"', 'Open a pending request, review its contact information and message, and update its processing status.', 'The request status and assigned staff information are updated and retained after refresh.'),
    tc("View medical record", "Verify receptionists can open an existing medical record in read-only context", "Receptionist", 'a patient visit detail page', 'Open the medical-record section for a visit.', 'The correct record, doctor, department, diagnoses, and related results are displayed according to permission.'),
    tc("Unauthorized patient URL", "Verify staff cannot open a patient record outside authorization", "Receptionist", 'a patient detail URL belonging to an inaccessible record', 'Replace the record identifier in the browser URL and load the page.', 'The system returns an access/not-found response and does not display protected medical information.'),
  ]],
  ["Invoices, Payments, Insurance & CareS Card", [
    tc("Create initial invoice", "Verify selected visit services produce an invoice", "Cashier", '"Hóa đơn"', 'Open an unpaid visit and review its service lines, quantities, prices, discounts, and payable total.', 'The invoice contains the selected billable services and calculated totals without duplicate lines.'),
    tc("Apply health insurance", "Verify eligible health-insurance benefits reduce the payable amount", "Cashier", 'an invoice detail page', 'Verify the patient’s insurance information and apply the eligible benefit.', 'Covered and customer-payable amounts are recalculated and the benefit is recorded on the invoice.'),
    tc("Reject invalid insurance", "Verify invalid or ineligible insurance is not applied", "Cashier", 'an invoice detail page', 'Attempt to apply expired, mismatched, or ineligible insurance information.', 'A clear reason is displayed and the invoice total is not reduced incorrectly.'),
    tc("Cash payment", "Verify a cashier can complete a cash payment", "Cashier", 'an unpaid invoice detail page', 'Choose "Tiền mặt", enter the received amount, and click "Thanh toán".', 'The payment succeeds, the invoice becomes Paid, a transaction is recorded, and eligible queue tickets are created.'),
    tc("Online payment", "Verify a cashier or customer can initiate online payment", "Cashier", 'an unpaid invoice detail page', 'Choose the online payment method and create the payment request.', 'A valid payment link or QR is displayed and the invoice remains pending until confirmation.'),
    tc("Payment confirmation", "Verify a successful online callback is reflected in the UI", "Customer", '"Lịch sử thanh toán"', 'Open the invoice after the payment provider confirms success.', 'The invoice and transaction show successful payment exactly once and the next visit step is available.'),
    tc("Download invoice PDF", "Verify a user can download the finalized invoice", "Customer", 'a paid receipt detail page', 'Click "Tải PDF".', 'A PDF file is downloaded with clinic, patient, invoice-line, amount, payment, and issue-date information.'),
    tc("Filter payment history", "Verify customers can filter their payment records", "Customer", '"Lịch sử thanh toán"', 'Search by invoice code and apply date or status filters, then reset them.', 'Only matching authorized invoices are shown and reset restores the original list.'),
    tc("Register CareS card", "Verify an eligible customer can register a CareS prepaid card", "Customer", '"Thẻ trả trước CareS"', 'Click "Đăng ký thẻ", enter PIN "123456" twice, accept the terms, and confirm.', 'The card is created, the PIN is masked by default, and card actions become available.'),
    tc("CareS PIN validation", "Verify invalid or mismatched PIN values are rejected", "Customer", 'the CareS card registration or PIN form', 'Enter a PIN with the wrong length or mismatched confirmation and submit.', 'A field-level PIN message is displayed and no PIN/card change is saved.'),
    tc("Forgot CareS PIN", "Verify a customer can recover a forgotten CareS PIN securely", "Customer", '"Thẻ trả trước CareS"', 'Choose "Quên mã PIN", complete identity/OTP verification, set and confirm a new six-digit PIN.', 'The new PIN is accepted, the old PIN stops working, and the PIN remains masked.'),
    tc("Top up CareS card", "Verify a cashier can add funds to an active CareS card", "Cashier", '"Nạp tiền thẻ CareS"', 'Search for the customer/card, enter a valid amount, choose a payment method, and confirm.', 'The top-up transaction is recorded and the available card balance is updated once.'),
    tc("CareS transaction history", "Verify card top-up and spending history is traceable", "Customer", '"Thẻ trả trước CareS"', 'Open card history and review recent transactions.', 'The list shows date, type, amount, status, and resulting information for the signed-in customer’s card.'),
  ]],
  ["Queue Management & Patient Journey", [
    tc("Create queue tickets after payment", "Verify paid services enter the correct queues", "Receptionist", 'the active visit detail page', 'Open a newly paid visit and review its journey and queue assignments.', 'The first eligible service is Waiting and later dependent services are Blocked according to the journey order.'),
    tc("Appointment priority", "Verify an on-time appointment is ordered according to queue priority rules", "Receptionist", '"Hành trình bệnh nhân"', 'Compare an on-time scheduled patient and a walk-in patient waiting for the same room.', 'The displayed order follows the configured appointment priority without exposing patient names publicly.'),
    tc("Call next patient", "Verify medical staff can call the next waiting ticket", "Doctor", 'the assigned room queue', 'Click "Gọi tiếp theo".', 'The correct ticket is called, the room display updates, and the customer journey reflects the current room.'),
    tc("Recall patient", "Verify medical staff can recall the current ticket", "Doctor", 'the assigned room queue', 'Click "Gọi lại" for the currently called ticket.', 'The same ticket is announced again without creating another queue entry or changing its order.'),
    tc("Mark absent", "Verify an absent patient can be skipped", "Doctor", 'the assigned room queue', 'Mark the called patient as absent or skipped and confirm.', 'The ticket receives the correct absent/skipped status and the next eligible ticket can be called.'),
    tc("Start service", "Verify the called ticket can enter service", "Doctor", 'the assigned room queue', 'Select the called ticket and click the action to start service.', 'The ticket becomes In Progress, the room shows the active patient, and the journey highlights the current step.'),
    tc("Customer waiting room", "Verify a customer can view their current waiting position", "Customer", '"Hành trình của tôi"', 'Select the active visit and click "Cập nhật".', 'The current task, room, status, and permitted position information are displayed without other patients’ identities.'),
    tc("Guest journey lookup", "Verify a guest can view a visit journey using valid lookup information", "Public user", 'the guest journey page', 'Enter the visit code and required verification information, then submit.', 'The matching journey is displayed with privacy-safe queue information.'),
    tc("Same-specialty continuation", "Verify consecutive examination services in one specialty stay in the assigned room", "Doctor", 'the active examination and room queue', 'Complete the first examination service without pending laboratory work and continue the visit.', 'The next examination service becomes available in the same physical room and does not require the patient to queue in another room.'),
  ]],
  ["Medical Examination & Prescriptions", [
    tc("Open medical record", "Verify the assigned doctor can open the active patient record", "Doctor", 'the assigned room queue', 'Start the called ticket and open the examination screen.', 'The correct patient, visit, service, vital signs, and available clinical sections are displayed.'),
    tc("Save examination draft", "Verify a doctor can save incomplete examination work", "Doctor", 'the examination screen', 'Enter symptoms, findings, and notes, then click "Lưu nháp".', 'The record remains editable in Draft status and the entered information is restored after reopening.'),
    tc("Record ICD-10 diagnosis", "Verify a doctor can search and select ICD-10 diagnoses", "Doctor", 'the diagnosis section of the examination screen', 'Search by ICD-10 code or name, select a diagnosis, and save.', 'The selected code and description are attached once to the correct medical record.'),
    tc("Create prescription", "Verify a doctor can prescribe medicine with instructions", "Doctor", 'the prescription section', 'Add medicine name, dosage, frequency, duration and usage instructions, then save.', 'The prescription displays the entered items and calculates/presents the prescribed quantities correctly.'),
    tc("Prescription validation", "Verify incomplete prescription items are rejected", "Doctor", 'the prescription section', 'Add a medicine without required dosage or usage information and attempt to save.', 'The missing field is identified and the incomplete item is not finalized.'),
    tc("Order laboratory services", "Verify a doctor can add a laboratory package or individual test", "Doctor", 'the laboratory-order section', 'Select an active package and an allowed individual test, add clinical notes, and confirm the order.', 'The order is created without duplicating package components and the visit moves to the required payment/laboratory step.'),
    tc("Laboratory gate", "Verify an examination does not continue prematurely while laboratory work is pending", "Doctor", 'the examination screen for a record with pending laboratory requests', 'Attempt to complete or advance the record before the required results are ready.', 'The current business rule is enforced and the next examination service is not opened early.'),
    tc("Review laboratory results", "Verify a doctor can review signed results for the same visit", "Doctor", 'the examination screen after laboratory completion', 'Open the grouped result section and review the returned package and analytes.', 'Signed results are grouped correctly and available to the responsible doctor for clinical completion.'),
    tc("Complete medical record", "Verify a doctor can finalize a complete record", "Doctor", 'the examination screen', 'Enter required diagnosis and treatment information, resolve required laboratory work, and click "Hoàn tất".', 'The record becomes Completed, further clinical editing is restricted, and the queue advances according to the visit journey.'),
    tc("Create follow-up appointment", "Verify a doctor can schedule a follow-up visit", "Doctor", 'the examination-completion screen', 'Choose a future date, service/department and follow-up note, then confirm.', 'A follow-up appointment is created for the patient and appears in the customer appointment list.'),
    tc("Download medical documents", "Verify completed examination documents can be downloaded", "Customer", 'a completed visit detail page', 'Open the medical record or prescription and click "Tải PDF".', 'The downloaded PDF belongs to the selected service/record and contains no simulation or placeholder wording.'),
  ]],
  ["Laboratory & Paraclinical Workflows", [
    tc("View laboratory work queue", "Verify laboratory staff can view requests for an assigned department", "Laboratory Staff", 'the laboratory request list', 'Open an assigned room or department and filter the request list by status.', 'Only relevant requests are shown with patient, service/package, priority, and current state.'),
    tc("Call laboratory patient", "Verify laboratory staff can call the next eligible patient", "Laboratory Staff", 'the laboratory room queue', 'Click "Gọi tiếp theo" and start the selected request.', 'The request enters the active state and the queue/customer journey shows the laboratory room.'),
    tc("Record specimen collection", "Verify specimen collection information can be recorded", "Laboratory Staff", 'an active laboratory request', 'Enter collection time, specimen information and collector details, then save.', 'The sample is marked collected with traceable staff and time information.'),
    tc("Mark recollection", "Verify an unsuitable sample can be rejected for recollection", "Laboratory Staff", 'an active laboratory request', 'Select the recollection action, enter a reason, and confirm.', 'The sample/request receives the recollection status and the reason is visible to authorized staff.'),
    tc("Enter individual analyte results", "Verify laboratory staff can enter results for required analytes", "Laboratory Staff", 'the laboratory result form', 'Enter valid result values, units, flags, and notes for the displayed analytes.', 'Values are retained against the correct analytes and invalid required fields are identified.'),
    tc("Save laboratory draft", "Verify incomplete laboratory results can be saved as a draft", "Laboratory Staff", 'the laboratory result form', 'Enter part of the result set and click "Lưu nháp".', 'The result remains unsigned/editable and the saved values reappear after reopening.'),
    tc("Sign laboratory result", "Verify a complete result can be signed", "Laboratory Staff", 'the laboratory result form', 'Complete all required values and click the sign/complete action.', 'The result becomes signed/completed, records the responsible staff, and is available to the doctor and customer according to permissions.'),
    tc("Prevent premature signing", "Verify incomplete required analytes cannot be signed", "Laboratory Staff", 'the laboratory result form', 'Leave a required analyte blank and attempt to sign.', 'The system highlights missing results and keeps the form in an editable non-completed state.'),
    tc("Grouped package result", "Verify package results are presented as one logical group", "Customer", 'a completed visit detail page', 'Open a laboratory package result containing multiple analytes.', 'The package name appears once with its component results grouped beneath it instead of unrelated individual cards.'),
    tc("Partial cancellation", "Verify an authorized user can cancel an eligible test without cancelling unrelated tests", "Laboratory Staff", 'the laboratory request detail', 'Cancel one eligible requested test, enter a reason, and confirm.', 'Only the selected test is cancelled; other requests keep their valid states and the journey recalculates correctly.'),
  ]],
  ["Administration, Operations & Support", [
    tc("Manage accounts", "Verify an administrator can create and update a staff account", "Administrator", '"Quản lý tài khoản"', 'Create an account with valid identity, contact and role information, then update its active status.', 'The account appears in the list with the selected role/status and receives the corresponding access.'),
    tc("Account validation", "Verify duplicate or invalid account information is rejected", "Administrator", '"Quản lý tài khoản"', 'Submit an existing phone/email or omit a required account field.', 'A clear validation message is shown and no duplicate account is created.'),
    tc("Manage rooms", "Verify administrators can create or update a clinic room", "Administrator", '"Quản lý phòng"', 'Create or edit a room with code, name, type, department and status, then save.', 'The room is saved with the selected configuration and appears in eligible operational screens.'),
    tc("Room maintenance guard", "Verify an active room cannot be moved to maintenance when work remains", "Administrator", '"Quản lý phòng"', 'Choose a room with active/future assignments and change its status to Maintenance.', 'The change is blocked with the operational reason and active work is preserved.'),
    tc("Manage services", "Verify administrators can create and publish a medical service", "Administrator", '"Quản lý dịch vụ"', 'Enter service code, name, category, type, price, duration and booking settings; save and publish it.', 'The service changes from Draft to Active and appears only in the applicable customer/staff selectors.'),
    tc("Manage service packages", "Verify administrators can define a package with component services", "Administrator", '"Quản lý dịch vụ"', 'Create a package, add active component services, set price and save.', 'The package is stored once with the selected components and displays correctly during visit creation.'),
    tc("Manage capabilities", "Verify staff capabilities can be assigned for service delivery", "Administrator", 'the capability-management page', 'Select a staff member, service/department capability and effective settings, then save.', 'The capability is recorded and the staff member becomes eligible only for matching assignments.'),
    tc("Manage shifts", "Verify administrators can configure clinic shifts", "Administrator", '"Quản lý ca làm việc"', 'Create or update a shift with name, start time, end time and active status.', 'The shift is saved and available to schedule planning without overlapping invalid times.'),
    tc("Build staff schedule", "Verify a clinic manager can assign staff to valid shifts and rooms", "Clinic Manager", '"Lịch làm việc"', 'Select a date, staff member, shift and eligible department/room, then save the assignment.', 'The assignment appears on the schedule and conflicting or incapable assignments are rejected.'),
    tc("View own schedule", "Verify staff can view their assigned schedule", "Doctor", '"Lịch làm việc của tôi"', 'Select the current or following week.', 'Only the signed-in staff member’s assignments are displayed with correct dates, shifts, departments, and rooms.'),
    tc("Manage clinic information", "Verify an administrator can update public clinic information", "Administrator", '"Thông tin phòng khám"', 'Update address, phone, email, operating-license number and public description, then save.', 'The values persist and approved public fields appear on the public About or footer area.'),
    tc("Manage announcements", "Verify an administrator can publish a public announcement", "Administrator", '"Thông báo công khai"', 'Create an announcement with title, content, visibility period and active status, then publish.', 'The announcement appears during its configured period and is hidden when inactive or expired.'),
    tc("Configure CareS policy", "Verify an administrator can update membership-card policy", "Administrator", 'the CareS membership-policy page', 'Update allowed policy settings and save.', 'The policy is stored and subsequent card operations use the updated effective settings.'),
    tc("View manager patient list", "Verify a clinic manager can search patients without editing clinical data", "Clinic Manager", 'the patient-management page', 'Search by code, name or phone and open a patient summary.', 'Matching authorized patient summaries are shown and clinical information remains protected according to role.'),
    tc("Operational report", "Verify a clinic manager can view filtered operational reports", "Clinic Manager", '"Báo cáo"', 'Choose a valid date range and report filters, then apply them.', 'Report totals and charts update to the selected period and agree with the displayed detail data.'),
    tc("Audit log", "Verify administrators can trace significant system actions", "Administrator", '"Nhật ký hệ thống"', 'Filter by actor, action, date, or entity and open a log detail.', 'The log shows who performed the action, when it occurred, and the affected operation without allowing log editing.'),
    tc("Notifications", "Verify users receive and manage role-relevant notifications", "Receptionist", 'the notification bell', 'Open an unread notification and mark it as read.', 'The notification opens its relevant context when available and the unread counter decreases once.'),
    tc("Feedback management", "Verify authorized staff can review and process patient feedback", "Receptionist", '"Phản hồi"', 'Open a feedback item, review its details and update the processing state.', 'The updated state is retained and the patient’s protected information is shown only as permitted.'),
    tc("Support chat", "Verify a customer can exchange support messages", "Customer", 'the support chat widget', 'Send a valid question, wait for the automated or staff response, and reopen the conversation.', 'The message appears once, the response is associated with the same conversation, and history remains available.'),
    tc("Queue display privacy", "Verify the public queue display does not expose patient identity", "Public user", 'the room or all-room queue display', 'Open the queue display and review called/waiting information.', 'Only permitted ticket/position information is shown; patient full name, diagnosis, and contact data are not displayed.'),
  ]],
];

const input = await FileBlob.load(source);
const workbook = await SpreadsheetFile.importXlsx(input);
const cover = workbook.worksheets.getItem("Cover");
const testCases = workbook.worksheets.getItem("Test Cases");
const stats = workbook.worksheets.getItem("Test Statistics");
const acceptance = workbook.worksheets.getItem("Acceptance Test");

cover.getRange("B4").values = [["CareS Clinic Management System"]];
cover.getRange("F4").values = [["CuongND"]];
cover.getRange("B5").values = [["CARES"]];
cover.getRange("F5").values = [[new Date(2026, 8, 15)]];
cover.getRange("F5").format.numberFormat = "dd/mm/yyyy";
cover.getRange("B6").formulas = [['=B5&"_AcceptanceTest_v"&TEXT(F6,"0.0")']];
cover.getRange("F6").values = [[1.0]];
cover.getRange("F6").format.numberFormat = "0.0";
cover.getRange("A11:F30").clear({ applyTo: "contents" });
cover.getRange("A11:F13").values = [
  [new Date(2026, 8, 15), "1.0", "Information", "A", "Initialize the CareS acceptance test document", ""],
  [new Date(2026, 8, 15), "1.0", "Acceptance test cases", "A", "Add user-facing acceptance scenarios for the CareS clinic workflow", ""],
  [new Date(2026, 8, 15), "1.0", "Test result structure", "M", "Replace separate Passed and Failed columns with one Boolean Test Result column", ""],
];
cover.getRange("A11:A13").format.numberFormat = "dd/mm/yyyy";
cover.getRange("A11:F13").format.wrapText = true;
cover.getRange("A11:F13").format.verticalAlignment = "center";
cover.getRange("A11:F13").format.rowHeight = 34;

testCases.getRange("D5:F5").values = [["1. Frontend: CareS React application running at the configured web address.\n2. Backend: CareS Spring Boot API is available.\n3. Database: PostgreSQL contains the approved demonstration dataset.\n4. Browser: current Google Chrome or Microsoft Edge.\n5. Valid demo accounts exist for Customer, Receptionist, Cashier, Doctor, Laboratory Staff, Administrator, and Clinic Manager."]];
testCases.getRange("B9:F200").clear({ applyTo: "contents" });
testCases.getRange("B9:F9").values = [[1, "CareS User Acceptance", "Acceptance Test", "Verify complete user-facing CareS workflows across public, customer, clinical, operational, and administration screens.", "The CareS frontend, backend, PostgreSQL database, demo accounts, and representative visit data are available."]];
testCases.getRange("A1:G1").format.rowHeight = 38;

acceptance.getRange("F4:H4").unmerge();
acceptance.getRange("G4:H4").unmerge();
acceptance.getRange("F5:H5").unmerge();
acceptance.getRange("G5:H5").unmerge();
acceptance.getRange("G8:H8").unmerge();
acceptance.getRange("I8:I9").unmerge();
for (const range of ["B10:I10","B14:I14","B20:I20","B36:I36","B59:I59","B82:I82","B98:I98","B111:I111","B121:I121","B128:I128"]) {
  try { acceptance.getRange(range).unmerge(); } catch {}
}
acceptance.getRange("F3:H3").values = [["CuongND"]];
acceptance.getRange("F3:H3").merge();
acceptance.getRange("F4:H4").values = [["SangND"]];
acceptance.getRange("F4:H4").merge();
acceptance.getRange("F5:H5").formulas = [["=Cover!F5"]];
acceptance.getRange("F5:H5").merge();
acceptance.getRange("F5:H5").format.numberFormat = "dd/mm/yyyy";
acceptance.getRange("B7:I500").clear({ applyTo: "contents" });
acceptance.getRange("B7").formulas = [["=COUNTA(G11:G129)"]];
acceptance.getRange("G7").formulas = [["=COUNTA(G11:G129)"]];
acceptance.getRange("H7").formulas = [["=B7-G7"]];
acceptance.getRange("B8:B9").values = [["ID"]];
acceptance.getRange("B8:B9").merge();
acceptance.getRange("C8:F8").values = [["Checklist"]];
acceptance.getRange("C8:F8").merge();
acceptance.getRange("G8:G9").values = [["Test Result"]];
acceptance.getRange("G8:G9").merge();
acceptance.getRange("H8:H9").values = [["Note"]];
acceptance.getRange("H8:H9").merge();
acceptance.getRange("H8:H9").format.fill = "#2F751D";
acceptance.getRange("H8:H9").format.font = { bold: true, color: "#FFFFFF" };
acceptance.getRange("H8:H9").format.horizontalAlignment = "center";
acceptance.getRange("H8:H9").format.verticalAlignment = "center";
acceptance.getRange("C9:F9").values = [["Criteria", "Objective", "Test Steps", "Expected Results"]];

let row = 10;
let id = 1;
for (const [groupName, cases] of groups) {
  acceptance.getRange(`B${row}:H${row}`).copyFrom(acceptance.getRange("B10:H10"), "all");
  acceptance.getRange(`B${row}:H${row}`).clear({ applyTo: "contents" });
  acceptance.getRange(`B${row}:H${row}`).formulas = [[null, null, null, null, null, null, null]];
  acceptance.getRange(`B${row}:H${row}`).merge();
  acceptance.getRange(`B${row}`).values = [[groupName]];
  acceptance.getRange(`B${row}`).formulas = [[`="${groupName.replaceAll('"', '""')}"`]];
  row += 1;
  for (const item of cases) {
    acceptance.getRange(`B${row}:H${row}`).copyFrom(acceptance.getRange("B11:H11"), "all");
    acceptance.getRange(`B${row}:H${row}`).clear({ applyTo: "contents" });
    acceptance.getRange(`B${row}:H${row}`).formulas = [[null, null, null, null, null, null, null]];
    acceptance.getRange(`B${row}:H${row}`).values = [[id, item.criteria, item.objective, item.steps, item.expected, true, ""]];
    acceptance.getRange(`B${row}`).formulas = [[`=${id}`]];
    acceptance.getRange(`B${row}:H${row}`).format.fill = "#FFFFFF";
    acceptance.getRange(`B${row}:H${row}`).format.font = { color: "#000000", size: 10 };
    acceptance.getRange(`B${row}:H${row}`).format.wrapText = true;
    acceptance.getRange(`B${row}:H${row}`).format.verticalAlignment = "top";
    acceptance.getRange(`B${row}`).format.verticalAlignment = "center";
    acceptance.getRange(`G${row}`).format.verticalAlignment = "center";
    acceptance.getRange(`G${row}`).format.horizontalAlignment = "center";
    acceptance.getRange(`B${row}:H${row}`).format.rowHeight = 126;
    id += 1;
    row += 1;
  }
}
acceptance.getRange(`B${row}:I500`).clear({ applyTo: "contents" });
acceptance.getRange("I:I").format.columnWidth = 2;
acceptance.getRange("G:G").format.columnWidth = 12;
acceptance.getRange("H:H").format.columnWidth = 20;
acceptance.freezePanes.freezeRows(9);
acceptance.getRange("A1:H1").format.rowHeight = 38;

stats.getRange("G3:H3").values = [["CuongND"]];
stats.getRange("G4:H4").values = [["SangND"]];
stats.getRange("G5:H5").formulas = [["=Cover!F5"]];
stats.getRange("G5:H5").format.numberFormat = "dd/mm/yyyy";
stats.getRange("B11:H30").clear({ applyTo: "contents" });
stats.getRange("B11:H11").values = [[1, "Acceptance Test", null, null, null, null, null]];
stats.getRange("D11").formulas = [["=COUNTA('Acceptance Test'!G11:G129)"]];
stats.getRange("E11").formulas = [["=F11-D11"]];
stats.getRange("F11:H11").merge();
stats.getRange("F11").formulas = [["=COUNTA('Acceptance Test'!G11:G129)"]];
stats.getRange("C13").values = [["Sub total"]];
stats.getRange("D13").formulas = [["=D11"]];
stats.getRange("E13").formulas = [["=E11"]];
stats.getRange("F13:H13").merge();
stats.getRange("F13").formulas = [["=F11"]];
stats.getRange("E15").formulas = [["=IFERROR((D13+E13)*100/F13,0)"]];
stats.getRange("E16").formulas = [["=IFERROR(D13*100/F13,0)"]];
stats.getRange("E15:E16").format.numberFormat = "0";
stats.getRange("F15:F16").values = [["%"], ["%"]];
stats.getRange("A1:I1").format.rowHeight = 38;

workbook.recalculate();
const inspect = await workbook.inspect({ kind: "table", range: `Acceptance Test!B1:H${Math.min(row, 25)}`, include: "values,formulas", tableMaxRows: 25, tableMaxCols: 8, maxChars: 8000 });
console.log(inspect.ndjson);
const errors = await workbook.inspect({ kind: "match", searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A|#NUM!|#NULL!|#SPILL!|#CALC!", options: { useRegex: true, maxResults: 100 }, summary: "final formula error scan" });
console.log(errors.ndjson);
await fs.mkdir(outputDir, { recursive: true });
for (const [sheetName, range, fileName] of [["Cover","A1:G16","Cover"],["Test Cases","A1:G12","Test-Cases"],["Test Statistics","A1:I18","Test-Statistics"],["Acceptance Test","A1:H28","Acceptance-Test"],["Acceptance Test","A112:H129","Acceptance-Test-Bottom"]]) {
  const preview = await workbook.render({ sheetName, range, scale: 1.3, format: "png" });
  await fs.writeFile(`${outputDir}\\preview-${fileName}.png`, new Uint8Array(await preview.arrayBuffer()));
}
const out = await SpreadsheetFile.exportXlsx(workbook);
await out.save(outputPath);
console.log(JSON.stringify({ outputPath, testCases: id - 1, lastRow: row - 1, groups: groups.length }));
