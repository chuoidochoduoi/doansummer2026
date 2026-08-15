$ErrorActionPreference = 'Stop'

$source = 'C:\Users\Administrator\Downloads\_Report5_IntegrationTest_Sample.xlsx'
$output = 'D:\gitlap\doAnSummer2026\outputs\INTEGRATION_TEST_FULL_FLOW_TEMPLATE.xlsx'

function Case($description, $procedure, $expected, $precondition) {
    [pscustomobject]@{ Description=$description; Procedure=$procedure; Expected=$expected; Precondition=$precondition }
}
function Flow($id, $sheet, $title, $requirement, $groups, $cases) {
    [pscustomobject]@{
        Id=$id; Sheet=$sheet; Title=$title; Requirement=$requirement
        Groups=@($groups | ForEach-Object { $_ } | Where-Object { $_ -is [string] })
        Cases=@($cases | ForEach-Object { $_ } | Where-Object { $_ -is [pscustomobject] -and $_.PSObject.Properties.Name -contains 'Description' })
    }
}

$flows = @(
Flow 1 'IT01-RegisterOTP' 'Registration and OTP Verification' 'OTP service -> Account -> Profile -> JWT tokens.' @('Successful registration','Input validation','Duplicate and expired state') @(
    (Case 'Register a new customer after OTP verification' "1. Send OTP by POST /api/auth/send-otp.`n2. Verify OTP by POST /api/auth/verify-register-otp.`n3. Submit customer data to POST /api/auth/register." "1. HTTP 200/201 is returned.`n2. One CUSTOMER account and one profile are created.`n3. Access and refresh tokens are returned." 'Redis and PostgreSQL are available; phone number is not registered.'),
    (Case 'Register with a verified email identifier' "1. Send OTP to the email identifier.`n2. Verify the received OTP.`n3. Submit the registration request." "1. Registration succeeds.`n2. Account and profile reference the same identifier.`n3. The OTP is consumed." 'Email is not assigned to another account.'),
    (Case 'Reject an incorrect OTP' "1. Send an OTP.`n2. Submit a different OTP to /verify-register-otp.`n3. Attempt registration." "1. OTP verification is rejected.`n2. Registration is not completed.`n3. No account or profile is created." 'A valid OTP exists for the identifier.'),
    (Case 'Reject an expired OTP' "1. Send an OTP.`n2. Wait until the OTP expires.`n3. Submit the expired OTP." "1. HTTP 400 is returned.`n2. The identifier is not marked as verified.`n3. No account is created." 'OTP expiration is configured in Redis.'),
    (Case 'Reject a password shorter than the minimum length' "1. Verify the registration OTP.`n2. Submit /api/auth/register with a short password.`n3. Query the account repository." "1. Validation error is returned.`n2. Account count remains unchanged.`n3. OTP verification cannot bypass password validation." 'Identifier is verified and not yet registered.'),
    (Case 'Reject an identifier already assigned to another account' "1. Verify OTP for an existing phone number.`n2. Submit the registration request.`n3. Query account and profile data." "1. Duplicate identifier error is returned.`n2. No second account or profile is created.`n3. Existing data remains unchanged." 'An account already exists for the phone number.')
);
Flow 2 'IT02-LoginToken' 'Login and Token Refresh' 'Account -> Authentication -> JWT access and refresh tokens.' @('Successful authentication','Credential validation','Token state handling') @(
    (Case 'Login with an active customer account' "1. Submit phone and password to POST /api/auth/login.`n2. Read the response tokens.`n3. Call GET /api/auth/me with the access token." "1. Login returns HTTP 200.`n2. Access and refresh tokens are returned.`n3. /me returns the authenticated CUSTOMER account." 'Customer account exists, is active, and has a profile.'),
    (Case 'Refresh an authenticated session' "1. Login successfully.`n2. Submit the refresh token to POST /api/auth/refresh.`n3. Call /api/auth/me with the new access token." "1. Refresh returns HTTP 200.`n2. A new valid token pair is returned.`n3. The new access token authenticates the same account." 'A non-expired refresh token exists.'),
    (Case 'Reject an incorrect password' "1. Submit an existing phone with an incorrect password.`n2. Inspect the response.`n3. Check that no token is issued." "1. HTTP 400/401 is returned.`n2. Access and refresh tokens are absent.`n3. Account data is unchanged." 'Active account exists for the submitted phone.'),
    (Case 'Reject a locked account' "1. Lock the account by the account-management API.`n2. Submit valid login credentials.`n3. Inspect authentication data." "1. Login is rejected.`n2. No JWT token is issued.`n3. Locked state remains unchanged." 'Account exists with isActive=false or locked=true.'),
    (Case 'Reject an access token on the refresh endpoint' "1. Login successfully.`n2. Submit the access token as refreshToken.`n3. Inspect the response." "1. HTTP 400 is returned.`n2. Invalid token type is reported.`n3. No new token is issued." 'A valid access token is available.'),
    (Case 'Reject a request without bearer authorization' "1. Remove the Authorization header.`n2. Call GET /api/auth/me.`n3. Inspect the response body." "1. HTTP 401/403 is returned.`n2. Account information is not disclosed.`n3. Security context remains unauthenticated." 'No authenticated session is present.')
);
Flow 3 'IT03-Profile' 'Customer Profile Management' 'Account -> Profile -> Insurance lookup.' @('Profile retrieval and update','Validation and ownership','Insurance integration') @(
    (Case 'Retrieve the authenticated customer profile' "1. Login as CUSTOMER.`n2. Call GET /api/v1/profiles/me.`n3. Compare account and profile identifiers." "1. HTTP 200 is returned.`n2. The profile belongs to the authenticated account.`n3. Contact information is returned." 'Customer account has an associated profile.'),
    (Case 'Update profile contact information' "1. Login as CUSTOMER.`n2. Call PUT /api/v1/profiles/me with new name, phone, and email.`n3. Reload the profile." "1. HTTP 200 is returned.`n2. Profile fields are updated once.`n3. Account identity is not replaced." 'Profile exists and the new phone/email are unused.'),
    (Case 'Reject an invalid email format' "1. Submit PUT /profiles/me with an invalid email.`n2. Inspect the response.`n3. Reload the profile." "1. Validation error is returned.`n2. Invalid email is not saved.`n3. Previous profile data remains unchanged." 'Authenticated customer profile exists.'),
    (Case 'Reject a future date of birth' "1. Submit a future date in PUT /profiles/me.`n2. Inspect the response.`n3. Query the database profile." "1. HTTP 400 is returned.`n2. Date of birth remains unchanged.`n3. No audit entry records a successful update." 'Authenticated customer profile exists.'),
    (Case 'Prevent a customer from reading another profile' "1. Login as customer A.`n2. Call GET /api/v1/profiles/{id} for customer B.`n3. Inspect the response." "1. HTTP 403/404 is returned.`n2. Customer B data is not disclosed.`n3. Neither profile is modified." 'Two different customer profiles exist.'),
    (Case 'Look up an active social-insurance record' "1. Login as an authorized user.`n2. Call GET /api/v1/bhxh/check with an active insurance code.`n3. Compare the mapped response." "1. HTTP 200 is returned.`n2. Insurance identity and coverage are mapped correctly.`n3. Profile data is not overwritten unexpectedly." 'The BHXH integration service is available.')
);
Flow 4 'IT04-Appointment' 'Appointment Booking and Check-in' 'Profile -> Medical Service -> Appointment -> Customer Visit -> Invoice.' @('Appointment booking','Booking validation','Reception check-in') @(
    (Case 'Create a future appointment' "1. Select an available medical service.`n2. Submit POST /api/v1/appointments for a future time.`n3. Retrieve the new appointment." "1. HTTP 201 is returned.`n2. Appointment status is PENDING.`n3. Customer, service, and schedule are linked correctly." 'Customer profile and published medical service exist.'),
    (Case 'Create a guest appointment' "1. Submit guest identity and service data to POST /api/v1/appointments/guest.`n2. Save the guest token.`n3. Retrieve guest history." "1. Guest appointment is created.`n2. Guest identity is linked to the appointment.`n3. History returns the new booking." 'Guest identifier is valid and service is available.'),
    (Case 'Reject an appointment in the past' "1. Submit POST /appointments with a past scheduled time.`n2. Inspect the response.`n3. Search the appointment list." "1. HTTP 400 is returned.`n2. No appointment is created.`n3. Existing appointments remain unchanged." 'Customer and service exist.'),
    (Case 'Reject an overlapping pending appointment' "1. Create a pending appointment.`n2. Submit another booking in the conflict window.`n3. Query the customer appointments." "1. Conflict/validation error is returned.`n2. Only the original appointment exists.`n3. No duplicate notification is sent." 'Customer already has a PENDING appointment in the conflict window.'),
    (Case 'Check in a pending appointment' "1. Login as RECEPTIONIST.`n2. Call POST /api/v1/appointments/{id}/check-in.`n3. Inspect appointment, visit, and invoice." "1. Appointment becomes CHECKED_IN.`n2. One customer visit is created.`n3. One pending invoice is created for selected services." 'Appointment is PENDING for the current date; no active visit exists.'),
    (Case 'Reject duplicate check-in' "1. Check in an appointment successfully.`n2. Call the check-in endpoint again.`n3. Count visits and invoices." "1. Second request returns conflict/error.`n2. Only one visit exists.`n3. Only one invoice exists." 'Appointment is already CHECKED_IN.')
);
Flow 5 'IT05-PaymentQueue' 'Invoice Payment and Queue Creation' 'Invoice -> Insurance -> Transaction -> Queue Ticket.' @('Invoice payment','Payment validation','Queue side effects') @(
    (Case 'Pay a pending invoice in cash' "1. Open a pending invoice.`n2. Call POST /api/v1/invoices/{id}/pay with CASH.`n3. Inspect transaction and queue data." "1. Invoice becomes PAID.`n2. A successful transaction is stored.`n3. Required queue tickets are created once." 'Invoice is PENDING and contains valid service items.'),
    (Case 'Apply insurance before payment' "1. Call POST /api/v1/invoices/{id}/insurance.`n2. Review covered and patient-payable amounts.`n3. Pay the remaining amount." "1. Insurance coverage is calculated.`n2. Invoice totals are consistent.`n3. Payment completes the invoice." 'Invoice belongs to a customer with active insurance.'),
    (Case 'Reject an amount lower than the invoice total' "1. Submit payment below patient-payable amount.`n2. Inspect invoice and transaction status.`n3. Count queue tickets." "1. Payment is rejected.`n2. Invoice remains PENDING.`n3. No queue ticket is created." 'Invoice is PENDING.'),
    (Case 'Reject payment for a cancelled invoice' "1. Cancel a pending invoice.`n2. Submit the payment request.`n3. Inspect related records." "1. Payment is rejected.`n2. Invoice remains CANCELLED.`n3. No successful transaction or queue ticket is created." 'Invoice status is CANCELLED.'),
    (Case 'Prevent duplicate payment side effects' "1. Pay an invoice successfully.`n2. Submit the same payment again.`n3. Count transactions and queue tickets." "1. Second payment returns conflict/error.`n2. No duplicate successful transaction is created.`n3. Queue-ticket count does not increase." 'Invoice status is PAID.'),
    (Case 'Record an online payment failure' "1. Create a PayOS payment request.`n2. Mark the transaction as failed through /transactions/{id}/fail.`n3. Reload the invoice." "1. Transaction becomes FAILED.`n2. Invoice is not marked PAID.`n3. No service queue is activated." 'PayOS transaction exists in PENDING state.')
);
Flow 6 'IT06-QueueExam' 'Queue Handling and Examination Start' 'Queue Ticket -> Customer Visit -> Vital Signs -> Medical Record.' @('Queue operations','Examination start','Vital-sign validation') @(
    (Case 'Call the next waiting patient' "1. Retrieve waiting tickets for a department.`n2. Call POST /api/v1/queue-tickets/{id}/call.`n3. Reload the ticket." "1. Ticket changes from WAITING to CALLED.`n2. Called time is recorded.`n3. Department association is unchanged." 'Queue ticket is WAITING in the staff department.'),
    (Case 'Start examination for a called ticket' "1. Call a waiting ticket.`n2. Submit POST /queue-tickets/{id}/start-exam.`n3. Inspect visit and medical record." "1. Ticket and visit become IN_PROGRESS.`n2. Examination data is available to the doctor.`n3. No duplicate medical record is created." 'Ticket is CALLED and customer visit is active.'),
    (Case 'Skip a waiting ticket' "1. Select a WAITING ticket.`n2. Call POST /queue-tickets/{id}/skip.`n3. Reload the department queue." "1. Ticket becomes SKIPPED or moves according to queue rules.`n2. Other ticket order remains consistent.`n3. Visit is not completed." 'Ticket belongs to the current department queue.'),
    (Case 'Reject calling a completed ticket' "1. Select a DONE ticket.`n2. Call the /call endpoint.`n3. Reload the ticket." "1. HTTP 400/409 is returned.`n2. Ticket remains DONE.`n3. Called time and visit data are unchanged." 'Queue ticket status is DONE.'),
    (Case 'Record valid vital signs for an active visit' "1. Start an examination.`n2. POST valid measurements to /api/v1/vital-signs.`n3. Retrieve the saved vital signs." "1. Vital-sign record is created.`n2. It references the correct visit and patient.`n3. Numeric measurements are preserved." 'Customer visit is IN_PROGRESS.'),
    (Case 'Reject invalid vital-sign measurements' "1. Submit negative or impossible measurements.`n2. Inspect the response.`n3. Query vital-sign records for the visit." "1. Validation error is returned.`n2. Invalid values are not stored.`n3. Existing vital signs remain unchanged." 'Customer visit is active.')
);
Flow 7 'IT07-Examination' 'Diagnosis and Prescription' 'Customer Visit -> Medical Record -> ICD-10 -> Prescription.' @('Medical-record update','Diagnosis validation','Prescription handling') @(
    (Case 'Record symptoms and diagnosis' "1. Open an IN_PROGRESS examination.`n2. PUT symptoms, diagnosis, and ICD-10 code to /api/doctor/examinations/{id}.`n3. Reload the medical record." "1. Examination update succeeds.`n2. Diagnosis and ICD-10 relationship are stored.`n3. Record remains editable until completion." 'Doctor is assigned to the active visit.'),
    (Case 'Save a medical record as draft' "1. Enter partial examination data.`n2. POST /api/v1/medical-records/{id}/draft.`n3. Reopen the record." "1. Record status becomes DRAFT.`n2. Entered data is preserved.`n3. Doctor can continue editing." 'Medical record is IN_PROGRESS.'),
    (Case 'Add a valid prescription item' "1. Select an active medicine.`n2. Update the examination with dose and quantity.`n3. Retrieve prescription information." "1. Prescription item is created once.`n2. Medicine, quantity, and instructions are correct.`n3. It belongs to the current medical record." 'Medicine catalog entry is active; record is editable.'),
    (Case 'Reject an unknown ICD-10 code' "1. Submit a diagnosis with an unknown ICD-10 code.`n2. Inspect the response.`n3. Reload the record." "1. HTTP 400/404 is returned.`n2. Unknown code is not linked.`n3. Previous diagnosis data remains unchanged." 'Medical record is editable.'),
    (Case 'Reject a prescription quantity of zero' "1. Select an active medicine.`n2. Submit quantity=0.`n3. Query prescription items." "1. Validation error is returned.`n2. No invalid prescription item is created.`n3. Existing items are unchanged." 'Medical record is editable.'),
    (Case 'Prevent editing a completed medical record' "1. Complete a medical record.`n2. Submit another examination update.`n3. Reload the record." "1. Update is rejected.`n2. Completed status remains unchanged.`n3. Diagnosis and prescription are not overwritten." 'Medical record status is COMPLETED.')
);
Flow 8 'IT08-TestOrder' 'Clinical Test Ordering' 'Medical Record -> Test Request -> Department -> Queue Ticket.' @('Create test requests','Request validation','Multi-department sequence') @(
    (Case 'Create a single clinical test request' "1. Open an active medical record.`n2. POST a supported service to /api/v1/test-requests.`n3. Inspect request and queue records." "1. Test request is created in PENDING state.`n2. A capable active department is selected.`n3. A queue ticket is created for that department." 'Medical record, service, and department capability exist.'),
    (Case 'Create a batch of distinct test requests' "1. Submit several service IDs to /api/v1/test-requests/batch.`n2. Retrieve requests by invoice.`n3. Inspect department queues." "1. One request is created per distinct service.`n2. Requests are linked to the same visit/invoice.`n3. Queue grouping follows department rules." 'Medical record is active and all services are published.'),
    (Case 'Reject an empty batch request' "1. Submit an empty serviceIds list.`n2. Inspect the response.`n3. Count requests for the record." "1. HTTP 400 is returned.`n2. No test request is created.`n3. Queue data remains unchanged." 'Medical record exists.'),
    (Case 'Reject an unsupported service' "1. Select a service without an active capable department.`n2. Submit the test request.`n3. Query test requests and queues." "1. HTTP 400/404 is returned.`n2. No request is persisted.`n3. No queue ticket is created." 'Service exists but has no active capability mapping.'),
    (Case 'Prevent duplicate test requests in one visit' "1. Create a test request.`n2. Submit the same service again for the same visit.`n3. Count requests and tickets." "1. Duplicate request is rejected.`n2. Request count remains one.`n3. Queue-ticket count remains one." 'Original request is not CANCELLED.'),
    (Case 'Sequence requests across multiple departments' "1. Create requests for two departments.`n2. Inspect patient journey and queue tickets.`n3. Complete the first service step." "1. First required step is active.`n2. Later step remains BLOCKED until allowed.`n3. Completing the first step activates the next step." 'Patient journey is active with no conflicting step.')
);
Flow 9 'IT09-LabResult' 'Laboratory Result Completion' 'Test Request -> Test Result -> Queue Ticket -> Patient Journey.' @('Draft and complete results','Result validation','Return to doctor queue') @(
    (Case 'Save a draft laboratory result' "1. Open a PENDING test request.`n2. POST result data to /api/v1/test-requests/{id}/result.`n3. Reload the request and result." "1. A test result is created.`n2. Request moves to IN_PROGRESS.`n3. Result is not yet marked verified." 'Execution queue is active for the laboratory department.'),
    (Case 'Complete a valid laboratory result' "1. Upload a valid result PDF.`n2. Submit conclusion and authorized verifier to /result/complete.`n3. Inspect request and queue states." "1. Request becomes COMPLETED.`n2. Verification information is stored.`n3. Laboratory queue step becomes DONE." 'Test request is IN_PROGRESS and result data exists.'),
    (Case 'Retrieve a completed result file' "1. Complete a test result.`n2. Call GET /api/v1/test-results/{resultId}/file.`n3. Validate the returned file." "1. HTTP 200 is returned.`n2. PDF content and filename match the stored result.`n3. Request status remains COMPLETED." 'Completed result has an attached PDF.'),
    (Case 'Reject completion without a result file' "1. Save a draft result without a file.`n2. Call /result/complete.`n3. Reload the request." "1. Validation error is returned.`n2. Request remains IN_PROGRESS.`n3. Verification fields remain empty." 'Test request is IN_PROGRESS.'),
    (Case 'Reject an unauthorized verifier' "1. Save a complete draft result.`n2. Submit completion using a doctor outside the responsible department.`n3. Inspect verification data." "1. HTTP 400/403 is returned.`n2. Result is not verified.`n3. Request is not completed." 'Verifier lacks the required department role/capability.'),
    (Case 'Return the patient to the doctor after all tests finish' "1. Complete every test request in the visit.`n2. Inspect the laboratory queue.`n3. Query doctor queue tickets with TEST_DONE." "1. All laboratory steps are DON…716 tokens truncated… 200 is returned.`n2. Insurance identity and coverage are mapped correctly.`n3. No unrelated profile is updated." 'BHXH integration service is available.'),
    (Case 'Apply insurance coverage to an invoice' "1. Open a pending customer invoice.`n2. POST /api/v1/invoices/{id}/insurance.`n3. Review invoice totals." "1. Covered amount is applied according to rules.`n2. Patient-payable amount is recalculated.`n3. Invoice remains payable and internally consistent." 'Invoice and active insurance belong to the same customer.'),
    (Case 'Reject an inactive or unknown insurance code' "1. Submit an inactive/unknown insurance code.`n2. Inspect the response.`n3. Reload the invoice." "1. HTTP 400/404 is returned.`n2. Insurance coverage is not applied.`n3. Original invoice totals remain unchanged." 'Invoice is PENDING.'),
    (Case 'Confirm a pending transaction' "1. Create a transaction for a pending invoice.`n2. POST /api/v1/transactions/{id}/confirm.`n3. Reload transaction and invoice." "1. Transaction becomes SUCCESS.`n2. Invoice becomes PAID when payment is sufficient.`n3. Confirmation time is recorded." 'Transaction status is PENDING.'),
    (Case 'Fail a pending transaction' "1. Create a pending transaction.`n2. POST /api/v1/transactions/{id}/fail.`n3. Reload invoice and transaction." "1. Transaction becomes FAILED.`n2. Invoice is not marked PAID.`n3. Failure does not create queue side effects." 'Transaction status is PENDING.'),
    (Case 'Prevent a customer from reading another invoice' "1. Login as customer A.`n2. Call GET /api/patient/payments/{invoiceId} owned by customer B.`n3. Inspect the response." "1. HTTP 403/404 is returned.`n2. Invoice and transaction details are not disclosed.`n3. Payment data is unchanged." 'Two customers and an invoice owned by customer B exist.')
);
Flow 12 'IT12-StaffSchedule' 'Staff Scheduling and Attendance' 'Staff -> Capability -> Schedule -> Attendance -> Adjustment.' @('Schedule management','Schedule validation','Attendance adjustment') @(
    (Case 'Assign a valid staff schedule' "1. Login as CLINIC_MANAGER.`n2. POST /api/v1/clinic-manager/schedules/assign.`n3. Retrieve staff schedule." "1. Schedule is created once.`n2. Staff, department, shift, and date are linked.`n3. Schedule appears in the staff view." 'Staff is active and has the required department capability.'),
    (Case 'Generate schedules from configured shifts' "1. Configure active shifts and templates.`n2. POST /api/v1/schedules/generate.`n3. Review generated schedules." "1. Schedules are generated for eligible staff.`n2. No duplicate overlapping assignment is created.`n3. Generated rows reference active shifts." 'Shift configuration and staff availability exist.'),
    (Case 'Reject an overlapping staff schedule' "1. Create an initial schedule.`n2. Submit another schedule that overlaps it.`n3. Retrieve the staff schedule." "1. Conflict/validation error is returned.`n2. Only the original schedule remains.`n3. Other staff schedules are unaffected." 'Staff already has a schedule in the requested interval.'),
    (Case 'Reject assignment without required capability' "1. Select a staff member without the required capability.`n2. Submit the assignment.`n3. Query schedules." "1. Assignment is rejected.`n2. No schedule is created.`n3. Capability data is not modified." 'Staff is active but lacks service/department capability.'),
    (Case 'Scan a valid attendance token' "1. Obtain the kiosk token.`n2. POST /api/v1/attendance/scan as STAFF.`n3. Retrieve /attendance/me/today." "1. Attendance check-in/out is recorded.`n2. Record belongs to authenticated staff.`n3. Duplicate scan rules are respected." 'Staff has a schedule and token is valid.'),
    (Case 'Review an attendance adjustment' "1. Staff submits POST /attendance/adjustments.`n2. Manager opens pending adjustments.`n3. PUT /adjustments/{id}/review." "1. Adjustment becomes APPROVED or REJECTED.`n2. Reviewer and review time are stored.`n3. Attendance changes only when approved." 'A PENDING adjustment exists and reviewer is CLINIC_MANAGER.')
);
Flow 13 'IT13-AdminCatalog' 'Administrative Catalog Management' 'Department, service, category, specialization, staff, and capability catalogs.' @('Catalog creation','Catalog validation','Status and dependency handling') @(
    (Case 'Create a medical department' "1. Login as ADMIN.`n2. POST /api/v1/departments with a unique code and name.`n3. Retrieve the department." "1. Department is created.`n2. Code and status are stored correctly.`n3. It appears in the appropriate department list." 'No department uses the submitted code.'),
    (Case 'Create and publish a medical service' "1. Create a service category.`n2. POST /api/v1/medical-services.`n3. PATCH /medical-services/{id}/publish." "1. Service and category relationship are stored.`n2. Service becomes published/active.`n3. It appears in /medical-services/available." 'ADMIN is authenticated; department/category exist.'),
    (Case 'Reject a duplicate catalog code' "1. Create a catalog entity with a code.`n2. Submit another entity with the same code.`n3. Query the catalog." "1. Duplicate request is rejected.`n2. Only one entity uses the code.`n3. Existing entity is unchanged." 'Catalog entity with the code already exists.'),
    (Case 'Reject a negative service price' "1. Submit a medical service with a negative price.`n2. Inspect the response.`n3. Query medical services." "1. Validation error is returned.`n2. Invalid service is not stored.`n3. Catalog totals remain unchanged." 'ADMIN is authenticated; category exists.'),
    (Case 'Prevent deletion of a catalog entity in use' "1. Select a department/service referenced by business data.`n2. Call its DELETE endpoint.`n3. Reload references." "1. Delete is rejected or handled safely.`n2. Referenced entity remains available.`n3. Appointments/requests are not broken." 'At least one business record references the entity.'),
    (Case 'Assign a service capability' "1. Create or select a service and department/staff.`n2. POST /api/v1/service-capabilities.`n3. Create a test request that needs the capability." "1. Capability mapping is stored.`n2. It is returned by capability APIs.`n3. Department/staff can be selected for the service." 'ADMIN is authenticated and all mapped entities exist.')
);
Flow 14 'IT14-NotifyChat' 'Notifications and Support Chat' 'Account/Profile -> Notification -> Chat Session -> WebSocket messages.' @('Notification delivery','Chat session and message','Authorization and closed state') @(
    (Case 'Send a notification to a customer' "1. POST /api/v1/notifications for a customer.`n2. POST /notifications/{id}/send.`n3. Retrieve customer notifications." "1. Notification becomes SENT.`n2. Recipient and channel are correct.`n3. It appears in the customer notification list." 'Recipient account/profile exists.'),
    (Case 'Mark a notification as read' "1. Login as the recipient.`n2. POST /notifications/{id}/mark-read.`n3. Query unread count." "1. Notification becomes READ.`n2. Unread count decreases once.`n3. Repeating the request does not double-decrement." 'A SENT unread notification belongs to the customer.'),
    (Case 'Open an authenticated chat session' "1. Login as CUSTOMER.`n2. POST /api/v1/chat/session.`n3. Query session status." "1. ACTIVE chat session is returned.`n2. Session is linked to the customer.`n3. Receptionist can see it in active sessions." 'Customer account and profile exist.'),
    (Case 'Exchange messages in an active session' "1. Customer sends /messages/customer.`n2. Receptionist sends /messages/receptionist.`n3. GET all session messages." "1. Both messages are stored in order.`n2. Sender roles are correct.`n3. WebSocket subscribers receive the messages." 'Chat session is ACTIVE; customer and receptionist are authenticated.'),
    (Case 'Reject an empty chat message' "1. Submit an empty message to an active session.`n2. Inspect the response.`n3. Retrieve session messages." "1. Validation error is returned.`n2. Empty message is not stored.`n3. Message count remains unchanged." 'Chat session is ACTIVE.'),
    (Case 'Reject messages after the session is closed' "1. Close the session using /chat/{sessionId}/close.`n2. Submit another message.`n3. Retrieve status and messages." "1. New message is rejected.`n2. Session remains CLOSED.`n3. Existing conversation history is preserved." 'Chat session has been closed by an authorized receptionist.')
);
Flow 15 'IT15-JourneyReport' 'Patient Journey, Reports, and Feedback' 'Visit/Queue/Record/Invoice -> Journey -> Reports -> Feedback -> Audit.' @('Patient journey','Management reports','Feedback and audit') @(
    (Case 'Retrieve a patient journey' "1. Create/check in a customer visit.`n2. Call GET /api/v1/patient-journeys/{visitId}.`n3. Compare queue and service steps." "1. Journey is returned in execution order.`n2. Current and completed steps match queue records.`n3. Visit and customer identifiers are correct." 'Customer visit and journey steps exist.'),
    (Case 'Advance to the next journey step' "1. Complete the current active step.`n2. POST /patient-journeys/{visitId}/advance.`n3. Reload the journey and queues." "1. Next valid step becomes active/waiting.`n2. Completed step remains DONE.`n3. Later steps remain blocked as required." 'Current journey step is DONE and another step exists.'),
    (Case 'Reject journey advancement out of order' "1. Leave the current step active.`n2. Submit the advance request.`n3. Reload the journey." "1. HTTP 400/409 is returned.`n2. No later step is activated.`n3. Current state remains unchanged." 'Current journey step is not complete.'),
    (Case 'Retrieve the management dashboard' "1. Login as CLINIC_MANAGER.`n2. GET /api/v1/reports/dashboard for a date range.`n3. Reconcile counts with visits and invoices." "1. Dashboard returns HTTP 200.`n2. Visit, revenue, and service totals match source data.`n3. Date filters are applied correctly." 'Manager is authorized; report-period data exists.'),
    (Case 'Retrieve service performance statistics' "1. Login as CLINIC_MANAGER.`n2. GET /api/v1/reports/services for a date range.`n3. Compare results by medical service." "1. Service statistics are returned.`n2. Counts and revenue reconcile with invoices/visits.`n3. Inactive unrelated services do not distort results." 'Medical services and completed business data exist.'),
    (Case 'Trace a business change in the audit log' "1. Perform an auditable update.`n2. GET /api/v1/audit-logs/by-entity.`n3. Compare actor, action, and entity snapshot." "1. Audit entry is returned.`n2. Actor, entity ID, action, and time are correct.`n3. Audit query does not alter business data." 'ADMIN or authorized manager is authenticated; auditable change exists.')
)
)

foreach($flowCheck in $flows){
    if(@($flowCheck.Groups).Count -ne 3){throw "Flow $($flowCheck.Id) has invalid group count."}
    if(@($flowCheck.Cases).Count -ne 6){throw "Flow $($flowCheck.Id) has invalid test-case count: $(@($flowCheck.Cases).Count)"}
    foreach($caseCheck in @($flowCheck.Cases)){
        if($caseCheck -isnot [pscustomobject] -or $caseCheck.Description -isnot [string]){throw "Flow $($flowCheck.Id) contains an invalid case object."}
    }
}

$excel = $null; $workbook = $null; $templateWorkbook=$null
try {
    if (Test-Path -LiteralPath $output) { Remove-Item -LiteralPath $output -Force }
    $excel = New-Object -ComObject Excel.Application
    $excel.Visible = $false
    $excel.DisplayAlerts = $false
    $excel.AskToUpdateLinks = $false
    $templateWorkbook = $excel.Workbooks.Open($source, 0, $true)
    $oldSheetCount=$excel.SheetsInNewWorkbook
    $excel.SheetsInNewWorkbook=19
    $workbook=$excel.Workbooks.Add()
    $excel.SheetsInNewWorkbook=$oldSheetCount

    $targetNames=New-Object System.Collections.ArrayList
    [void]$targetNames.Add('Cover');[void]$targetNames.Add('Test Cases');[void]$targetNames.Add('Test Statistics')
    foreach($flowName in $flows){[void]$targetNames.Add([string]$flowName.Sheet)}
    [void]$targetNames.Add('MessageList')
    if($workbook.Worksheets.Count -ne 19){throw "New workbook contains $($workbook.Worksheets.Count) sheets instead of 19."}
    for($i=1;$i -le 19;$i++){
        try{$workbook.Worksheets.Item($i).Name=[string]$targetNames[$i-1]}
        catch{throw "Could not rename sheet $i to $($targetNames[$i-1]): $($_.Exception.Message)"}
    }

    function Copy-TemplateLayout($sourceSheet,$targetSheet,$maxCol,$maxRow){
        $sourceSheet.Range($sourceSheet.Cells.Item(1,1),$sourceSheet.Cells.Item($maxRow,$maxCol)).Copy()
        $targetSheet.Range('A1').PasteSpecial(-4104)
        for($cc=1;$cc -le $maxCol;$cc++){$targetSheet.Columns.Item($cc).ColumnWidth=$sourceSheet.Columns.Item($cc).ColumnWidth}
        for($rr=1;$rr -le $maxRow;$rr++){$targetSheet.Rows.Item($rr).RowHeight=$sourceSheet.Rows.Item($rr).RowHeight}
        $targetSheet.PageSetup.Orientation=$sourceSheet.PageSetup.Orientation
        $targetSheet.PageSetup.PaperSize=$sourceSheet.PageSetup.PaperSize
        $targetSheet.PageSetup.Zoom=$sourceSheet.PageSetup.Zoom
    }
    Copy-TemplateLayout $templateWorkbook.Worksheets.Item('Cover') $workbook.Worksheets.Item('Cover') 6 11
    Copy-TemplateLayout $templateWorkbook.Worksheets.Item('Test Cases') $workbook.Worksheets.Item('Test Cases') 6 40
    Copy-TemplateLayout $templateWorkbook.Worksheets.Item('Test Statistics') $workbook.Worksheets.Item('Test Statistics') 8 40
    $featureTemplate=$templateWorkbook.Worksheets.Item('Feature 1')
    $featureSheets=@()
    foreach($f in $flows){$s=$workbook.Worksheets.Item($f.Sheet);Copy-TemplateLayout $featureTemplate $s 15 19;$featureSheets+=$s}
    Copy-TemplateLayout $templateWorkbook.Worksheets.Item('MessageList') $workbook.Worksheets.Item('MessageList') 3 100
    if($workbook.Worksheets.Count -ne 19){throw "Workbook has $($workbook.Worksheets.Count) sheets after clean creation."}

    for ($i=0; $i -lt $flows.Count; $i++) {
        $flow = $flows[$i]; $sheet = $featureSheets[$i]
        $sheet.Name = $flow.Sheet
        $sheet.Range('B2').Value2 = ('IT{0:D2} - {1}' -f $flow.Id, $flow.Title)
        $sheet.Range('B3').Value2 = $flow.Requirement
        $sheet.Range('B4').Value2 = 6
        $roundCols = @('F','I','L')
        for ($ri=0; $ri -lt 3; $ri++) {
            $row = 6 + $ri; $rc = $roundCols[$ri]
            $sheet.Range("B$row").Formula = "=COUNTIF(`$$rc`$12:`$$rc`$19,B`$5)"
            $sheet.Range("C$row").Formula = "=COUNTIF(`$$rc`$12:`$$rc`$19,C`$5)"
            $sheet.Range("D$row").Formula = "=COUNTIF(`$$rc`$12:`$$rc`$19,D`$5)"
            $sheet.Range("E$row").Formula = "=COUNTIF(`$$rc`$12:`$$rc`$19,E`$5)"
        }
        $sheet.Range('A11:O19').ClearContents()
        $groupRows = @(11,14,17); $caseRows = @(12,13,15,16,18,19)
        for ($g=0; $g -lt 3; $g++) { $sheet.Cells.Item($groupRows[$g],1).Value2 = $flow.Groups[$g] }
        for ($c=0; $c -lt 6; $c++) {
            $case = $flow.Cases[$c]; $row = $caseRows[$c]
            $sheet.Cells.Item($row,1).Value2 = ('IT{0:D2}-{1:D3}' -f $flow.Id, ($c+1))
            $sheet.Cells.Item($row,2).Value2 = $case.Description
            $sheet.Cells.Item($row,3).Value2 = $case.Procedure
            $sheet.Cells.Item($row,4).Value2 = $case.Expected
            $sheet.Cells.Item($row,5).Value2 = $case.Precondition
            $sheet.Cells.Item($row,6).Value2 = 'Pending'
            $sheet.Cells.Item($row,9).Value2 = 'Pending'
            $sheet.Cells.Item($row,12).Value2 = 'Pending'
            $sheet.Cells.Item($row,15).Value2 = ('Flow IT{0:D2}' -f $flow.Id)
        }
        $sheet.Range('A10:O19').WrapText = $true
    }
    if($workbook.Worksheets.Count -ne 19){throw "Workbook has $($workbook.Worksheets.Count) sheets after population loop."}

    $list = $workbook.Worksheets.Item('Test Cases')
    $list.Range('B9:F40').ClearContents()
    $list.Range('B9:F9').Copy()
    $list.Range('B9:F23').PasteSpecial(-4122)
    for ($i=0; $i -lt $flows.Count; $i++) {
        $r=9+$i; $f=$flows[$i]
        $list.Cells.Item($r,2).Value2=[string]($i+1)
        $list.Cells.Item($r,3).Value2=('IT{0:D2} - {1}' -f $f.Id,$f.Title)
        $list.Cells.Item($r,4).Value2=$f.Sheet
        $list.Hyperlinks.Add($list.Cells.Item($r,4), '', ("'{0}'!A1" -f $f.Sheet), '', $f.Sheet) | Out-Null
        $list.Cells.Item($r,5).Value2=$f.Requirement
        $list.Cells.Item($r,6).Value2='See the Feature sheet for flow-specific pre-conditions.'
    }

    $stats = $workbook.Worksheets.Item('Test Statistics')
    $stats.Range('B11:H40').ClearContents()
    $stats.Range('B11:H11').Copy(); $stats.Range('B11:H25').PasteSpecial(-4122)
    for ($i=0; $i -lt $flows.Count; $i++) {
        $r=11+$i; $f=$flows[$i]
        $stats.Cells.Item($r,2).Value2=[string]($i+1)
        $stats.Cells.Item($r,3).Formula=("='{0}'!B2" -f $f.Sheet)
        $stats.Cells.Item($r,4).Formula=("='{0}'!B6" -f $f.Sheet)
        $stats.Cells.Item($r,5).Formula=("='{0}'!C6" -f $f.Sheet)
        $stats.Cells.Item($r,6).Formula=("='{0}'!D6" -f $f.Sheet)
        $stats.Cells.Item($r,7).Formula=("='{0}'!E6" -f $f.Sheet)
        $stats.Cells.Item($r,8).Formula=("='{0}'!B4" -f $f.Sheet)
    }

    $cover=$workbook.Worksheets.Item('Cover')
    $cover.Range('B4').Value2='Clinic Management System'
    $cover.Range('B5').Value2='CMS'
    $cover.Range('B6').Value2='CMS_IntegrationTest_v1.0'
    $cover.Range('E5').Value2=(Get-Date -Format 'yyyy-MM-dd')
    $cover.Range('E6').Value2='v1.0'

    $messages=$workbook.Worksheets.Item('MessageList')
    $messages.Range('A2:C100').ClearContents()
    $mr=2
    foreach($f in $flows){
        $messages.Cells.Item($mr,1).Value2=('IT{0:D2}-VALIDATION' -f $f.Id)
        $messages.Cells.Item($mr,2).Value2=('Validation failed in {0}.' -f $f.Title)
        $messages.Cells.Item($mr,3).Value2=('IT{0:D2}' -f $f.Id); $mr++
        $messages.Cells.Item($mr,1).Value2=('IT{0:D2}-STATE' -f $f.Id)
        $messages.Cells.Item($mr,2).Value2=('The current business state does not allow {0}.' -f $f.Title)
        $messages.Cells.Item($mr,3).Value2=('IT{0:D2}' -f $f.Id); $mr++
    }

    foreach($n in @($workbook.Names)) { try { if ($n.RefersTo -like '*#REF!*') { $n.Delete() } } catch {} }
    $excel.CalculateFullRebuild()
    if($workbook.Worksheets.Count -ne 19){throw "Workbook has $($workbook.Worksheets.Count) sheets before SaveAs."}
    $workbook.SaveAs($output, 51)
    $workbook.Close($false)
    [Runtime.InteropServices.Marshal]::FinalReleaseComObject($workbook) | Out-Null; $workbook=$null

    # Reopen without repair mode; failure here means the deliverable is rejected.
    $verify = $excel.Workbooks.Open($output, 0, $true, 5, '', '', $true, 1, '', $false, $false, 0, $false, $true, 0)
    $sheetCount=$verify.Worksheets.Count
    $pending=0; $unitFound=$false
    foreach($f in $flows){
        $s=$verify.Worksheets.Item($f.Sheet)
        foreach($address in @('F12:F19','I12:I19','L12:L19')){
            foreach($cell in $s.Range($address).Cells){ if($cell.Value2 -eq 'Pending'){$pending++} }
        }
        if($s.UsedRange.Find('Unit Test')){$unitFound=$true}
    }
    $verify.Close($false)
    [Runtime.InteropServices.Marshal]::FinalReleaseComObject($verify) | Out-Null
    if($sheetCount -ne 19){throw "Unexpected sheet count: $sheetCount"}
    if($pending -ne 270){throw "Unexpected Pending count: $pending"}
    if($unitFound){throw 'Unit Test text was found in a Feature sheet.'}
    [pscustomobject]@{Output=$output;Sheets=$sheetCount;Flows=$flows.Count;TestCases=90;PendingCells=$pending;ExcelReopen='PASS'} | ConvertTo-Json
}
finally {
    if($workbook){try{$workbook.Close($false)}catch{};[Runtime.InteropServices.Marshal]::FinalReleaseComObject($workbook)|Out-Null}
    if($templateWorkbook){try{$templateWorkbook.Close($false)}catch{};[Runtime.InteropServices.Marshal]::FinalReleaseComObject($templateWorkbook)|Out-Null}
    if($excel){$excel.Quit();[Runtime.InteropServices.Marshal]::FinalReleaseComObject($excel)|Out-Null}
    [GC]::Collect();[GC]::WaitForPendingFinalizers()
}


