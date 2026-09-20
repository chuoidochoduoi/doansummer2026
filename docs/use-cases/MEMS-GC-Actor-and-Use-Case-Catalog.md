# MEMS-GC Actor and Use Case Catalog

CareS application · Unified Doctor model · Source review dated 6 September 2026

Purpose: provide the English actor and use-case baseline for Report 3 and system use-case diagrams. The six sections follow the supplied previous catalog. Traceability, route/API classification and old-to-new reconciliation are companion documents, not separate feature specifications.

## 1 Modeling Decision

MEMS-GC uses one Doctor actor for CareS. Examination and diagnostic services are two groups of responsibilities within that actor, not separate General Doctor, Specialist Doctor, Lab Employee or Diagnostic Service Staff actors. The DS use-case identifiers are retained for traceability only; they do not name an actor.

A Doctor account can hold examination specialization and diagnostic capabilities, but access to a particular operation also depends on the assigned room, current duty, relevant capability or specialization, responsibility for the medical record and the clinical state. Nurse access covers queue support, preparation and permitted diagnostic drafts; it does not imply authority to sign results or complete examinations.

Registered User is an abstract modeling parent for common account interactions, not a new login role. Authentication and recovery can start before login. Concrete roles remain Customer, Receptionist, Cashier, Nurse, Doctor, Clinic Manager and Administrator. Guest is separate. Legacy doctor role values are normalized to DOCTOR; ROLE_STAFF is a technical authority, not another business actor.

PayOS, Mock BHXH Service, Email Service and SMS Gateway are supporting integration actors. SMS is configured as mock in the inspected configuration. Mock insurance verification is not a certified connection to the national insurance system. The TV displays, database, internal chatbot, WebSocket components and scheduled jobs are inside the CareS boundary.

This catalog describes the inspected working trees, including uncommitted changes, rather than a proposed feature set. It is a static source review, not a claim that every screen or integration was exercised. Endpoint declarations were reconciled with frontend routes and action code; important service-level restrictions are recorded below. Full payload contracts remain in OpenAPI and source.

The inventory contains 70 registered page routes and 312 application endpoint mappings across 41 controllers. The catalog has 124 current unique use-case IDs and 192 explicit actor-to-use-case associations. Shared IDs are counted once. Inherited account relations are not added again to that association total. Supporting endpoints, rejected operations and legacy APIs are individually classified in endpoint-inventory.md; removed ID RC-11 remains reserved in old-to-new.md.

## 2 Actor Summary

| Actor | Type | Responsibility |
| --- | --- | --- |
| Guest | External human | Use public information, booking, verified guest journey and support. |
| Registered User | Abstract parent | Model shared account functions; not an extra role or person. |
| Customer | Primary human | Manage own and permitted family bookings, journeys, published history, feedback and CareS card. |
| Receptionist | Primary staff | Receive patients, coordinate visits and manage support and feedback. |
| Cashier | Primary staff | Collect eligible payments, apply insurance, accept and top up CareS cards, and issue receipts. |
| Nurse | Clinical support staff | Operate assigned queue and preparation tasks and enter permitted diagnostic drafts; cannot sign diagnostic results or complete medical examinations. |
| Doctor | Primary clinical | Perform examination and/or diagnostic work according to assigned room, specialization/capability, duty and record responsibility. |
| Clinic Manager | Management | Manage operations, staff, resources, forms, feedback, card policy and reports within permitted screens and endpoints. |
| Administrator | Administration | Manage accounts and clinic configuration and inspect audits; not a universal bypass of clinical duty or record ownership. |
| PayOS | External system | Provide payment-link and callback integration. |
| Mock BHXH Service | External simulated system | Provide demonstration insurance verification, not official certification. |
| Email Service | External delivery service | Deliver OTP and public contact enquiry email. |
| SMS Gateway | Configurable external delivery | Represent provider-based OTP delivery; the inspected runtime configuration selects mock SMS. |

There are eight concrete human actors including Guest, one abstract parent and four integration actors. Email and SMS share EXT-03 but remain distinct integration participants.

## 3 Actor–Use Case Matrix

| Actor | Explicit associations | Use-case IDs |
| --- | --- | --- |
| Guest | 7 | G-01, G-02, G-03, G-05, G-06, G-08, G-09 |
| Registered User | 6 | RU-01, RU-02, RU-03, RU-04, RU-05, RU-06 |
| Customer | 19 | CU-01, CU-02, CU-03, CU-04, CU-06, CU-07, CU-08, CU-09, CU-11, CU-12, CU-13, CU-14, CU-15, CU-16, CU-18, CU-19, CU-20, CU-21, CU-22 |
| Receptionist | 14 | RU-07, RU-08, RC-01, RC-02, RC-03, RC-04, RC-05, RC-06, RC-07, RC-08, RC-12, RC-14, RC-15, CM-06 |
| Cashier | 14 | RU-07, RU-08, CA-01, CA-02, CA-03, CA-04, CA-05, CA-06, CA-07, CA-08, CA-09, CA-10, CA-11, CA-12 |
| Nurse | 20 | RU-07, RU-08, RC-15, NU-01, NU-02, NU-03, NU-04, NU-05, NU-06, DR-15, DS-01, DS-02, DS-03, DS-04, DS-05, DS-06, DS-07, DS-08, DS-11, DS-13 |
| Doctor | 35 | RU-07, RU-08, CU-20, RC-15, NU-04, DR-01, DR-02, DR-03, DR-04, DR-05, DR-06, DR-07, DR-09, DR-10, DR-11, DR-12, DR-13, DR-14, DR-15, DR-16, DR-17, DS-01, DS-02, DS-03, DS-04, DS-05, DS-06, DS-07, DS-08, DS-09, DS-10, DS-11, DS-12, DS-13, DS-14 |
| Clinic Manager | 46 | RU-07, RU-08, RC-01, RC-02, RC-03, RC-04, RC-05, RC-06, RC-07, RC-08, RC-12, RC-13, RC-14, RC-15, CA-01, CA-02, CA-03, CA-04, CA-05, CA-06, CA-07, CA-08, CA-09, CA-10, CA-11, CA-12, CM-01, CM-02, CM-03, CM-04, CM-05, CM-06, CM-08, CM-09, CM-10, CM-11, CM-12, CM-13, AD-01, AD-02, AD-03, AD-06, AD-07, AD-09, AD-10, AD-11 |
| Administrator | 21 | RU-08, RC-13, RC-15, CM-02, CM-03, CM-04, CM-05, CM-08, CM-12, CM-13, AD-01, AD-02, AD-03, AD-05, AD-06, AD-07, AD-09, AD-10, AD-11, AD-12, AD-13 |
| PayOS | 1 | EXT-01 |
| Mock BHXH Service | 1 | EXT-02 |
| Email Service | 1 | EXT-03 |
| SMS Gateway | 1 | EXT-03 |

RU-01 to RU-06 are inherited common functions. All other shared associations are listed explicitly. The unique use-case count is not the sum of actor counts. Backend-only rows are identified in the detailed list and are not represented as completed UI workflows.

## 4 Detailed Actor and Use Case List

### 4 1 Guest public services

| ID | Use Case | Description |
| --- | --- | --- |
| G-01 | View Public Clinic Information | Browse clinic information, public announcements, services, doctors, contact details, terms and privacy information. Public doctor information does not disclose individual duty schedules. Actors: Guest. Delivery: UI + backend. |
| G-02 | Register Account | Register a Customer account using the registration form and OTP verification. Staff accounts are created through authorized staff administration, not public registration. Actors: Guest. Delivery: UI + backend. |
| G-03 | Create Guest Appointment | Select publicly bookable services, an available date and shift, enter patient details and confirm a guest booking. The guest selector does not expose individually priced laboratory analytes. Actors: Guest. Delivery: UI + backend. |
| G-05 | View Public Department Schedule | Review public department availability by date and shift before booking, without viewing named staff duty assignments. Actors: Guest. Delivery: UI + backend. |
| G-06 | Look Up Guest Journey | Use the visit code and matching phone number to view the guest journey, current queue and service statuses. This lookup does not provide diagnoses, prescriptions or clinical result content. Actors: Guest. Delivery: UI + backend. |
| G-08 | Use Guest Support Chat | Start a guest conversation, read replies and continue it using the guest session credentials. Automated replies are an internal support feature, not a separate external actor. Actors: Guest. Delivery: UI + backend. |
| G-09 | Submit Contact Enquiry | Send contact information and an enquiry through the public form for delivery to the configured clinic mailbox. This is separate from the live receptionist chat. Actors: Guest. Delivery: UI + backend. |

### 4 2 Shared account functions

| ID | Use Case | Description |
| --- | --- | --- |
| RU-01 | Log In and Log Out | Authenticate an existing account and end the local application session. Logout clears browser authentication state; no server-side logout or token-revocation endpoint is exposed by AuthController. Actors: Registered User. Delivery: UI + backend. |
| RU-02 | Verify OTP | Provide OTP evidence during registration or password recovery. Registration has a verification step; recovery submits the OTP with the new password. These operations can occur before login. Actors: Registered User. Delivery: UI + backend. |
| RU-03 | Recover Password | Request an OTP and set a new password after successful recovery verification. The recovery interaction is reached from the login screen. Actors: Registered User. Delivery: UI + backend. |
| RU-04 | Change Password | Change the current account password through the personal-profile dialog, supplying the current password and the required new-password confirmation. Actors: Registered User. Delivery: UI + backend. |
| RU-05 | View and Update Profile | Read and update permitted personal information. Staff have a separate professional-information request; this does not grant self-assignment of system roles, duty rooms or technical capabilities. Actors: Registered User. Delivery: UI + backend. |
| RU-06 | Read In-app Notifications | Read recent notifications, check the unread count and mark a displayed notification as read. In-app delivery is distinct from email or SMS delivery. Actors: Registered User. Delivery: UI + backend. |
| RU-07 | View Own Duty Schedule | View personal weekly duty assignments as marked shifts and move between weeks. This shared staff function does not allow editing assignments. Administrator is not admitted by this frontend route. Actors: Receptionist, Cashier, Nurse, Doctor, Clinic Manager. Delivery: UI + backend. |
| RU-08 | Set Local Interface Preferences | Save interface preferences available on the Settings page, including theme, language and local display or notification options. Settings are stored in the browser, not a backend configuration API. Actors: Receptionist, Cashier, Nurse, Doctor, Clinic Manager, Administrator. Delivery: Frontend only by design. |

### 4 3 Customer services

| ID | Use Case | Description |
| --- | --- | --- |
| CU-01 | Create Appointment | Book active, customer-bookable services for an authorized patient profile, selecting an available date and shift. Availability is service based; the form is not a named-doctor appointment selector. Actors: Customer. Delivery: UI + backend. |
| CU-02 | View Appointment List and Details | Review appointment lists, selected services and status for the account or permitted family profile. Present the displayed booking information to reception when attending. Actors: Customer. Delivery: UI + backend. |
| CU-03 | Cancel Appointment | Cancel an owned or manageable appointment when its state permits cancellation. Reception check-in is not a Customer confirmation action. Actors: Customer. Delivery: UI + backend. |
| CU-04 | View Follow-up Information | Review follow-up advice in the published medical record and view an appointment once it has been scheduled. A medical follow-up recommendation is not itself a confirmed future appointment. Actors: Customer. Delivery: UI + backend. |
| CU-06 | Track Examination Journey | Track the current step, queue position and completed or skipped services for an authorized visit. A skipped patient contacts the room staff directly; the Customer interface has no queue-return action. Actors: Customer. Delivery: UI + backend. |
| CU-07 | View Completed Medical Records | Browse visit-based history and separate completed examination records within each visit. A partially completed visit can retain published clinical history; unfinished medical-record content is not a Customer publication. Actors: Customer. Delivery: UI + backend. |
| CU-08 | View Prescriptions and Published Diagnostic Results | Read the prescription belonging to each completed medical record and authorized signed diagnostic results. Laboratory analytes are presented by panel where supported; files remain authorized resources, and referenced same-day results are distinguished from newly performed services. Actors: Customer. Delivery: UI + backend. |
| CU-09 | View Invoice and Payment History | Filter payment history by date, method and permitted patient profile, then inspect a receipt with invoice totals, insurance and CareS payment details where applicable. Actors: Customer. Delivery: UI + backend. |
| CU-11 | Send Message to Receptionist | Start or resume the account support conversation and send messages. Clinic Manager can also operate the staff-side support interface. Actors: Customer. Delivery: UI + backend. |
| CU-12 | View Conversation Messages | Read messages and the status of the accessible customer conversation. The Customer widget is not a separate searchable archive of all closed conversations. Actors: Customer. Delivery: UI + backend. |
| CU-13 | Submit and View Medical-record Feedback | Submit the supported overall rating and comment for an eligible completed record and view its response. Feedback concerns the record experience; it is not a separate rating score for every doctor or nurse. Actors: Customer. Delivery: UI + backend. |
| CU-14 | Manage Family Profiles | Create and update linked family profiles, stop managing a member or restore an eligible archived relationship. Reading historical family data and initiating new actions use different access checks. Actors: Customer. Delivery: UI + backend. |
| CU-15 | Create Family Group Booking | Book for multiple manageable patient profiles through the family-booking flow. Each patient retains a separate appointment and later visit, rather than sharing one medical record. Actors: Customer. Delivery: UI + backend. |
| CU-16 | Change Appointment Details | Reopen an eligible appointment in the booking flow and save permitted service, date or shift changes after availability validation. Actors: Customer. Delivery: UI + backend. |
| CU-18 | Register CareS Prepaid Card | Register the account CareS card using the card form and required PIN confirmation. Card registration does not itself top up the balance. Actors: Customer. Delivery: UI + backend. |
| CU-19 | View CareS Card and Balance History | Review card status, benefit information, balance and the balance-ledger history exposed by the Customer card screen. Top-ups are handled at the counter. Actors: Customer. Delivery: UI + backend. |
| CU-20 | Print an Individual Medical Record | Open and print the specific examination record selected by record ID. Customer printing reloads authorized completed data and supports a permitted family profile; clinical data from different examination services must remain separate. Actors: Customer, Doctor. Delivery: UI + backend. |
| CU-21 | Print Customer Receipt | Print the authorized receipt through the shared receipt document, excluding the application navigation. This operation does not initiate or confirm payment. Actors: Customer. Delivery: UI + backend. |
| CU-22 | Pay with Own CareS Card through API | The Customer API accepts a card payment against an authorized invoice with the required PIN and idempotency information. No caller for this self-payment endpoint is present in the current Customer frontend. Actors: Customer. Delivery: Backend only. |

### 4 4 Reception and shared coordination

| ID | Use Case | Description |
| --- | --- | --- |
| RC-01 | Search Patient | Identify existing patients using the reception search functions before check-in or visit creation. Review same-day examination and signed-result information to avoid an inappropriate duplicate service. Actors: Receptionist, Clinic Manager. Delivery: UI + backend. |
| RC-02 | Create or Complete Patient Profile | Enter or correct permitted identity, contact and patient-profile information during reception or in patient management. Clinical history is viewed separately from demographic editing. Actors: Receptionist, Clinic Manager. Delivery: UI + backend. |
| RC-03 | Check In Scheduled Patient | Review the appointment, verify patient information and confirm reception. Allowed changes can add examination services, laboratory panels or individual analytes before the visit and initial invoice are prepared. Actors: Receptionist, Clinic Manager. Delivery: UI + backend. |
| RC-04 | Create Walk-in Visit | Select or enter the patient, choose examination services and diagnostic panels or analytes, review the selection and create a walk-in visit with its initial charges. Callable services depend on subsequent payment and sequencing. Actors: Receptionist, Clinic Manager. Delivery: UI + backend. |
| RC-05 | View Visit and Ticket Information | Search reception visit records and inspect the selected visit, services, invoice and queue information using the visit-management interface. Actors: Receptionist, Clinic Manager. Delivery: UI + backend. |
| RC-06 | Update Permitted Appointment Information | Save permitted patient or appointment changes, including the service selection, or cancel an eligible booking. The reception selector supports both full panels and individual laboratory analytes. Actors: Receptionist, Clinic Manager. Delivery: UI + backend. |
| RC-07 | Review and Coordinate Patient Journey | Inspect the visit timeline, current department, payment gates, blocked steps and completed or skipped services to direct the patient. Viewing a later step does not make it callable. Actors: Receptionist, Clinic Manager. Delivery: UI + backend. |
| RC-08 | Manage Support Conversations | View active or closed conversations, open messages, reply to Customer or Guest and close an eligible session. This interface is not available to doctors, nurses or cashiers. Actors: Receptionist, Clinic Manager. Delivery: UI + backend. |
| RC-12 | Review Patient Visit History | Open the patient, visits and record-detail screens through reception history. Access uses the staff endpoints and is distinct from Customer family-profile authorization. Actors: Receptionist, Clinic Manager. Delivery: UI + backend. |
| RC-13 | Recover an Eligible Blocked Journey through API | Request recovery of an eligible blocked journey through the management API. The current staff journey screen is explicitly read-only and has no advance action. The backend checks dependencies; this is not an unrestricted override of clinical completion. Actors: Clinic Manager, Administrator. Delivery: Backend only. |
| RC-14 | Open Queue Display Screens | Use the launcher to open the overall calling screen or a selected room display in an authenticated browser tab. The launcher itself is a staff function, not an external actor. Actors: Receptionist, Clinic Manager. Delivery: UI + backend. |
| RC-15 | View Overall or Room Calling Display | Display only CALLED patients on the overall screen. The room screen shows the called or in-progress patient and next waiting patients, using names and birth years where available rather than treating ticket numbers as names. Actors: Receptionist, Clinic Manager, Doctor, Nurse, Administrator. Delivery: UI + backend. |

### 4 5 Cashier and shared payment services

| ID | Use Case | Description |
| --- | --- | --- |
| CA-01 | View Pending Invoices | Filter and inspect invoices requiring attention in the cashier invoice list. Listing an invoice does not acknowledge receipt of money. Actors: Cashier, Clinic Manager. Delivery: UI + backend. |
| CA-02 | View Invoice Details | Review service items, insurance, existing reductions, payments and the remaining amount. CareS payment metadata is read from receipt data when available. Actors: Cashier, Clinic Manager. Delivery: UI + backend. |
| CA-03 | Confirm Cash Payment | Confirm the supported cashier cash-payment operation. Payment updates the invoice and downstream service eligibility; the UI action is not a general editor for arbitrary transaction states. Actors: Cashier, Clinic Manager. Delivery: UI + backend. |
| CA-04 | Monitor Online Payment | Check invoice payment status after opening the PayOS link. Verified gateway callbacks are handled by the backend; opening a link alone is not proof of payment. Actors: Cashier, Clinic Manager. Delivery: UI + backend. |
| CA-05 | Review Invoice Payment History | Search historical invoices and inspect their payment information through the cashier screens. There is no separate current frontend page for arbitrary Transaction CRUD. Actors: Cashier, Clinic Manager. Delivery: UI + backend. |
| CA-06 | Print Receipt | Open the web/A4 receipt with service charges, insurance, payment method and masked CareS information where applicable. Print the receipt document rather than the application sidebar. Actors: Cashier, Clinic Manager. Delivery: UI + backend. |
| CA-07 | Verify and Apply Health Insurance | Submit the insurance information and apply permitted coverage to the invoice. Verification uses the configured mock BHXH integration; this is not an official insurance claim submission. Actors: Cashier, Clinic Manager. Delivery: UI + backend. |
| CA-08 | Accept CareS Card Payment | Validate the card and PIN, optionally apply eligible card benefits and pay the remaining invoice amount. Benefits apply to the eligible patient-payable amount, not a second deduction of insurer coverage. Actors: Cashier, Clinic Manager. Delivery: UI + backend. |
| CA-09 | Top Up CareS Card | Record an eligible card top-up at the counter, with the required amount and idempotency key, and provide the resulting top-up receipt. Actors: Cashier, Clinic Manager. Delivery: UI + backend. |
| CA-10 | Review and Print Top-up History | Browse recorded CareS top-ups and open their receipt representation. The top-up receipt is distinct from an examination invoice receipt. Actors: Cashier, Clinic Manager. Delivery: UI + backend. |
| CA-11 | Cancel an Eligible Invoice | Cancel an invoice only when its payment and service state allow it. This is not a general refund action for already delivered services. Actors: Cashier, Clinic Manager. Delivery: UI + backend. |
| CA-12 | Create PayOS Payment Link | Create and open a payment link from the cashier invoice screen. The current Customer frontend does not initiate this endpoint. Actors: Cashier, Clinic Manager. Delivery: UI + backend. |

### 4 6 Nursing and shared clinical preparation

| ID | Use Case | Description |
| --- | --- | --- |
| NU-01 | View Assigned Queue | View the assigned room, current patient and waiting patients, including blocked and absent states. Clinical-room access remains subject to backend assignment checks. Actors: Nurse. Delivery: UI + backend. |
| NU-02 | Call or Mark Patient Absent | Call or recall the permitted patient and mark absence through room queue actions. Calling order and patient availability are enforced by the backend. Actors: Nurse. Delivery: UI + backend. |
| NU-03 | Receive and Prepare Patient | Confirm arrival in the permitted room workflow and prepare the patient. Starting an examination requires a suitable on-duty treating doctor; the nurse does not become the treating doctor. Actors: Nurse. Delivery: UI + backend. |
| NU-04 | Record Vital Signs | Record or update vital signs for the active examination. Later examination records in the same visit can receive independent copies of valid source measurements; editing one record does not edit every copy. Actors: Nurse, Doctor. Delivery: UI + backend. |
| NU-05 | View Relevant Patient Information | Review patient details and relevant history available in the assigned clinical workflow. Customer and family authorization rules are separate from staff access rules. Actors: Nurse. Delivery: UI + backend. |
| NU-06 | Save Nursing Preparation Notes | Save permitted complaint, clinical preparation notes and vital signs in a nursing draft. This does not authorize prescribing, final diagnosis, diagnostic ordering or medical-record completion. Actors: Nurse. Delivery: UI + backend. |

### 4 7 Doctor examination services

| ID | Use Case | Description |
| --- | --- | --- |
| DR-01 | View Examination Queue | Review assigned examination-room patients, service order and current work. Display position is calculated separately from the fixed ticket number. Actors: Doctor. Delivery: UI + backend. |
| DR-02 | Call and Receive Patient | Call or recall the permitted patient, mark absence, restore an eligible same-day skipped patient after direct confirmation, or begin service. Duty, room and visit checks prevent cross-room handling. Actors: Doctor. Delivery: UI + backend. |
| DR-03 | View Medical Records and History | Read the current patient history, allergies and available previous examination records before making clinical decisions. Access and publication differ from the Customer view. Actors: Doctor. Delivery: UI + backend. |
| DR-04 | Record Medical Examination | Record symptoms and clinical findings for the current examination service. Each service has its own medical record, rather than completing every service in the visit together. Actors: Doctor. Delivery: UI + backend. |
| DR-05 | Record Diagnosis and Treatment Plan | Select ICD-10 reference codes and record diagnosis, conclusion, treatment direction and patient instructions in the responsible doctor medical record. Actors: Doctor. Delivery: UI + backend. |
| DR-06 | Create Prescription | Enter medication, quantity and usage instructions for the current record, using the available medicine lookup. Prescription validation includes the required allergy-verification state. Actors: Doctor. Delivery: UI + backend. |
| DR-07 | Request Diagnostic Services | Choose diagnostic services, complete panels or individual laboratory analytes and confirm the order. The backend normalizes selection and billing; this use case does not include referral to another examination specialty. Actors: Doctor. Delivery: UI + backend. |
| DR-09 | Review Diagnostic Results | Open read-only result or status dialogs from the examination screen without navigating to the Lab workbench. Completed results expose the supported values, conclusions and authorized files; unfinished requests show progress rather than editable result fields. Actors: Doctor. Delivery: UI + backend. |
| DR-10 | Save Medical Record as Draft | Save incomplete examination content in the responsible doctor record. Version checks reject stale updates, and completed records cannot be rewritten through the normal draft flow. Actors: Doctor. Delivery: UI + backend. |
| DR-11 | Resume Examination after Diagnostic Services | Resume the source examination when its required diagnostic work is complete, review results and continue the same record. Later examination services remain dependent on completion of this record. Actors: Doctor. Delivery: UI + backend. |
| DR-12 | Complete Medical Record | Confirm completion of the current examination record. When diagnostic work is still required, the order workflow waits for results instead of closing the record; otherwise it can release the next eligible service. Actors: Doctor. Delivery: UI + backend. |
| DR-13 | Arrange Follow-up from Examination | Enter the follow-up date, service and advice, review the confirmation dialog and create the linked follow-up appointment through the examination screen. Follow-up fields also travel with examination draft/completion. This direct booking action coexists with the separate reception follow-up workflow; it is not a specialist referral. Actors: Doctor. Delivery: UI + backend. |
| DR-14 | View Same-day Clinical Information | Review other completed examination information and signed same-day diagnostic results where authorized. Reused reference results are not newly billed or performed results in the current visit. Actors: Doctor. Delivery: UI + backend. |
| DR-15 | Verify Patient Allergies | Review and update the supported allergy assessment for the patient in the clinical context, including verified absence of known allergies or a recorded allergy history. Actors: Doctor, Nurse. Delivery: UI + backend. |
| DR-16 | Continue Next Examination in the Same Room | After confirming one service, continue the next eligible same-room examination using a separate record. A pending diagnostic cycle prevents premature completion of the chain. Actors: Doctor. Delivery: UI + backend. |
| DR-17 | Finish Carried-over Clinical Work | Resume eligible clinical work already started before the current day and finish it under current operational checks. This does not reactivate old unstarted services cancelled or skipped at day close. Actors: Doctor. Delivery: UI + backend. |

### 4 8 Doctor and Nurse diagnostic services

| ID | Use Case | Description |
| --- | --- | --- |
| DS-01 | View Diagnostic Queue and Panels | Browse requests for the assigned diagnostic room. Individually purchased analytes are grouped by their source panel and execution context; distinct panels remain separate. Actors: Doctor, Nurse. Delivery: UI + backend. |
| DS-02 | Call and Receive Diagnostic Patient | Call the next eligible patient and confirm the start of diagnostic work in the assigned room. Payment and preceding journey steps determine whether a request is ready. Actors: Doctor, Nurse. Delivery: UI + backend. |
| DS-03 | View Diagnostic Request | Inspect the diagnostic request or panel, source record, service and current action permissions. A panel workbench shows which analytes were purchased and which are locked. Actors: Doctor, Nurse. Delivery: UI + backend. |
| DS-04 | Record Specimen Information | Review generated specimen information and edit the permitted specimen fields where required. Related purchased requests in a panel use consistent specimen details. Actors: Doctor, Nurse. Delivery: UI + backend. |
| DS-05 | Perform Diagnostic Service | Work on the diagnostic service already started in the room, using its configured form. Unpurchased analytes remain non-editable; a physical laboratory act alone does not mark a result signed in the system. Actors: Doctor, Nurse. Delivery: UI + backend. |
| DS-06 | Enter Diagnostic Result | Enter values and conclusion for permitted purchased services. Required fields need a valid result or a supported omission reason; partial completion does not automatically refund the invoice. Actors: Doctor, Nurse. Delivery: UI + backend. |
| DS-07 | Attach Result Files | Upload supported result files or revision attachments after execution has begun, under file validation and room permissions. Files remain linked to the request or result revision. Actors: Doctor, Nurse. Delivery: UI + backend. |
| DS-08 | Save Diagnostic Draft | Persist unfinished result data and omission reasons for a request or purchased panel members. Draft saving does not publish the result to the Customer. Actors: Doctor, Nurse. Delivery: UI + backend. |
| DS-09 | Sign and Publish Diagnostic Result | Confirm the diagnostic result in the performing room as an eligible on-duty doctor. A Nurse may prepare the draft but cannot sign it. Cancelled requests cannot receive a new signed result. Actors: Doctor. Delivery: UI + backend. |
| DS-10 | Complete Diagnostic Request | Complete purchased diagnostic requests through the result-confirmation operation. Shared queue completion and return-to-doctor readiness depend on all relevant requests; this is an outcome of signing, not an additional independent completion button. Actors: Doctor. Delivery: UI + backend. |
| DS-11 | Handle Diagnostic Absence and Return | Doctor or Nurse may mark an eligible called patient absent; only the on-duty Doctor may restore the patient to that room's queue on the same day. Actors: Doctor, Nurse. Delivery: UI + backend. |
| DS-12 | Cancel an Eligible Diagnostic Request | Cancel a request when the performing-room doctor, current duty and request state permit it. This is distinct from refunding a paid service. Actors: Doctor. Delivery: UI + backend. |
| DS-13 | View Result Revision History | Read result revision history and authorized revision attachments in the diagnostic detail flow. Viewing an old revision does not edit a published result. Actors: Doctor, Nurse. Delivery: UI + backend. |
| DS-14 | Amend a Signed Result through API | Create an amendment, update its draft and sign it under performing-room doctor checks. These endpoints exist, but the current LabDetailPage does not expose an amendment-authoring action. Actors: Doctor. Delivery: Backend only. |

### 4 9 Clinic management and shared operations

| ID | Use Case | Description |
| --- | --- | --- |
| CM-01 | View Operational Reports | Select the reporting period and inspect overview, collections/invoices and room activity. Service and room detail tables use their own search filters and fixed pagination without changing period totals. Actors: Clinic Manager. Delivery: UI + backend. |
| CM-02 | Manage Staff Information | Create staff accounts or update permitted staff profile, photo, specialization, room assignment and activity details through account management. The Manager Staff page itself is a directory and detail view. Actors: Clinic Manager, Administrator. Delivery: UI + backend. |
| CM-03 | Manage Staff Schedules | View weekly staffing, assign staff to shifts and copy an eligible previous-week schedule. Validation checks conflicts and prevents unsupported changes to past assignments; no separate publish-schedule step is assumed. Actors: Clinic Manager, Administrator. Delivery: UI + backend. |
| CM-04 | Manage Services and Prices | Create or update allowed service details, price and booking settings; publish, deactivate or delete only where permitted. Fixed laboratory structures and linked-service constraints are not arbitrary panel-design CRUD. Actors: Clinic Manager, Administrator. Delivery: UI + backend. |
| CM-05 | Manage Departments and Rooms | Maintain rooms, descriptions, allowed service types, capabilities and staff associations through room management. Selecting an existing specialty is not editing the specialty reference catalog. Actors: Clinic Manager, Administrator. Delivery: UI + backend. |
| CM-06 | Manage Medical-record Feedback | Review overall record feedback, filter unanswered items and submit responses. Doctor, Nurse, Cashier and Administrator do not receive this feedback-management route or API permission. Actors: Clinic Manager, Receptionist. Delivery: UI + backend. |
| CM-08 | Manage Public Announcements | Create, edit, publish or withdraw and delete supported public announcements. Public visibility and local dismissal are separate from staff in-app notifications. Actors: Clinic Manager, Administrator. Delivery: UI + backend. |
| CM-09 | Manage Diagnostic Form Templates | Create or version permitted diagnostic forms, edit drafts, bind services, publish and retire forms. System laboratory forms are protected; this UI does not allow rewriting their fixed analytes and reference ranges. Actors: Clinic Manager. Delivery: UI + backend. |
| CM-10 | Print or Export Reports | Choose an available report type, preview and print it or export CSV. Exports use all matching loaded rows, not only the current page, and preserve the selected period and relevant detail filter. Actors: Clinic Manager. Delivery: UI + backend. |
| CM-11 | View Patient Directory | Search the manager patient directory, inspect patient details and open permitted visit history. The directory is not an independent clinical-editing workbench. Actors: Clinic Manager. Delivery: UI + backend. |
| CM-12 | Configure CareS Policy | Review and update supported prepaid-card policy values such as minimum top-up, benefit percentage and validity period. Existing card/payment rules remain enforced by the service. Actors: Clinic Manager, Administrator. Delivery: UI + backend. |
| CM-13 | Review CareS Ledger and Reverse Eligible Payment | Review card ledger entries and reverse an eligible erroneous card payment with a reason and idempotency key. This is not a Cashier action or unrestricted refund; service-start and existing reversal checks apply. Actors: Clinic Manager, Administrator. Delivery: UI + backend. |

### 4 10 Administration and shared configuration

| ID | Use Case | Description |
| --- | --- | --- |
| AD-01 | Manage User Accounts | Search staff or customer accounts, edit permitted fields, lock/unlock accounts and reset a password through account management. Account constraints can reject self-locking or unsupported removal. Actors: Administrator, Clinic Manager. Delivery: UI + backend. |
| AD-02 | Assign Supported Staff Roles and Capabilities | Assign supported fixed staff roles and permitted professional/room/capability information. The system has no arbitrary permission-matrix or new-role editor; Doctor eligibility is checked again during clinical operations. Actors: Administrator, Clinic Manager. Delivery: UI + backend. |
| AD-03 | Manage Clinic Information | Edit the clinic information exposed by the configuration screen and public site. Room, service, shift and announcement management use their own shared use cases. Actors: Administrator, Clinic Manager. Delivery: UI + backend. |
| AD-05 | Manage ICD-10 Catalog through API | Create, update or delete ICD-10 reference entries through the administrator API. The current frontend provides diagnosis lookup, but no registered ICD catalog-management page. Actors: Administrator. Delivery: Backend only. |
| AD-06 | Review Audit Information | Search the available audit events and inspect entity-related changes through the audit screen. This does not provide a general server-monitoring or integration-health console. Actors: Administrator, Clinic Manager. Delivery: UI + backend. |
| AD-07 | Manage Shift Versions | Review fixed shifts, preview the impact of changed hours and create a future-effective shift version with a reason. Existing historical versions are retained. Actors: Administrator, Clinic Manager. Delivery: UI + backend. |
| AD-09 | Manage Operating Exceptions | Preview and configure future clinic closures, shift-off dates or special hours, and reopen an eligible exception. Impact checks can block changes affecting existing operations. Actors: Administrator, Clinic Manager. Delivery: UI + backend. |
| AD-10 | Check Service Coverage | Check service availability for a date and shift to identify missing room or staffing coverage before changing schedules or accepting bookings. Actors: Administrator, Clinic Manager. Delivery: UI + backend. |
| AD-11 | View Fixed Technical Catalog | Search and read the fixed technical-capability catalog. Both the interface and controller reject catalog creation, editing and deletion. Assigning an existing capability to staff is a separate operation. Actors: Administrator, Clinic Manager. Delivery: UI + backend. |
| AD-12 | Manage Schedule Templates through API | Maintain recurring staff schedule templates through the backend endpoints. This is distinct from weekly assignment and copying in SchedulePage; no dedicated template-management route is registered. Actors: Administrator. Delivery: Backend only. |
| AD-13 | Administer Notification Records through API | Use administrator endpoints to inspect and maintain notification records and their delivery flags. Setting SENT is not evidence that an email or SMS was actually delivered; no notification-administration page is registered. Actors: Administrator. Delivery: Backend only. |

### 4 11 External integrations

| ID | Use Case | Description |
| --- | --- | --- |
| EXT-01 | Process Online Payment | Receive a supported payment-link request and return gateway payment information or a verified callback. The backend applies the payment outcome; a browser redirect alone does not confirm settlement. Actors: PayOS. Delivery: External integration. |
| EXT-02 | Simulate Insurance Verification | Return simulated insurance verification through the configured service. The built-in demo-card branch also simulates success locally; neither path is represented as an official BHXH claim or certification. Actors: Mock BHXH Service. Delivery: External integration. |
| EXT-03 | Deliver Verification and Contact Messages | Support outbound verification messages. Email also sends the public contact enquiry to the clinic mailbox. SMS is configured as mock in the inspected application configuration; provider implementations are not proof of live delivery. Actors: Email Service, SMS Gateway. Delivery: External integration. |

## 5 Actor Relationships and Constraints

### Actor generalization

Customer and the six staff actors specialize the abstract Registered User for RU-01 to RU-06. RU-07 and RU-08 have explicit actor lists because their routes are not available to every registered role. Guest registration/recovery is not conditioned on already being authenticated.

### Authorization is layered

Route guards and controller annotations are entry checks, not complete permission proofs. JwtService grants STAFF to staff roles. StaffDutyService checks assignment and duty, examination specialization or a compatible room capability. Record responsibility and result state are enforced in clinical services. Do not infer that an Administrator annotation bypasses those checks.

### Family and guest boundaries

Customer booking and mutation use active permitted family relationships; historical reading can retain linked historical family access according to FamilyAccessService. Guest journey lookup uses VIS and phone evidence and publishes service status, not diagnoses, prescriptions or signed clinical content.

### Sequential examination

Each examination service retains its own record. If an examination needs CLS, paid eligible tests are completed and the patient returns to the source examination before the next examination opens. Eligible prepaid tests join the first ordered CLS cycle. There is no general-practice summary or specialist-referral use case.

### Nurse and Doctor boundaries

Nursing record preparation is limited to permitted complaint/findings and vital signs. Assigned diagnostic work can include specimen data, uploads and drafts, but signing, amendment and cancellation require the authorized on-duty Doctor. A shared page is not proof of equal button permissions.

### Published history and print

Customer history exposes completed records and signed results, including partially finished visits with skipped services. Laboratory analytes are grouped for presentation. Each examination record has a separate print view; guest lookup does not expose that content.

### Return and day closing

A skipped patient contacts the doctor directly. Only the on-duty Doctor in the assigned room can restore an eligible ticket on the same day; Customer, Guest and Receptionist screens do not provide this action. Old unstarted tickets expire; carried-over clinical work is handled by the existing clinical workflow. The 00:05 cleanup is an internal scheduled responsibility, not an actor.

### Payment and card ownership

Counter actors create PayOS links; the Customer UI currently reads payment history and receipts. Customer card payment exists as a backend-only endpoint. Card reversal is a Manager/Administrator operation, not a Cashier permission. Invoice and item totals, insurance and CareS benefits remain governed by the payment services.

### Feedback and templates

Receptionist and Clinic Manager manage feedback. Doctor/Nurse participation can be mentioned in patient feedback but does not grant a feedback-management screen. Clinical form-template management belongs to Clinic Manager. Protected system laboratory templates and the fixed technical catalog are not unrestricted editable catalogs.

### Follow-up is not referral

The Doctor creates the follow-up appointment directly from the examination record. Receptionist support begins only after an appointment exists and uses the ordinary appointment-management workflow; there is no separate reception queue for unscheduled follow-up recommendations. This is not cross-specialty referral.

### Exceptions to UI parity

The receptionist appointment-detail route admits Doctor/Nurse, while its appointment API does not. The staff journey screen explicitly has no advance action; recovery is Manager/Admin API-only. Do not draw completed screen associations from either mismatch.

### Use-case relationships

Sequential screens do not establish UML include or extend. Keep the actors, goals and constraints as associations unless a separate scenario analysis proves a mandatory reused subflow or a conditional extension. Internal recalculation, idempotency, locks, notification dispatch and cleanup are implementation responsibilities.

## 6 Recommended Diagram Simplification

| Diagram area | Recommendation |
| --- | --- |
| Public and account | Guest and Registered User; public information, booking, guest lookup/support and authentication. Reference RU IDs rather than redrawing login for each actor. |
| Patient self service | Customer; family, appointments, journey, published records, CareS, receipts and feedback. Mark CU-22 as API-only or omit it from the interface diagram with an explicit appendix reference. |
| Reception and payment | Receptionist, Cashier and shared Clinic Manager goals; reception, return confirmation, support, invoices, insurance and card operations. Add PayOS and Mock BHXH only at their integration boundary. |
| Clinical work | One Doctor and Nurse. Group examination UCs separately from diagnostic DS UCs inside the same system boundary. Connect both actors only to shared preparation/draft goals; connect signing and examination completion to Doctor. |
| Management and administration | Clinic Manager and Administrator; schedules, resources, accounts, reports, audits and configuration. Keep forms with Manager and show shared use cases once. |
| Display and technical appendix | Draw TV screens within the system or reference RC-15, not a Queue Display Device actor. Put backend-only, compatibility and rejected operations in the technical appendix rather than inflating the main business diagram. |

Use the bilingual traceability table for source links and the old-to-new comparison for reserved IDs. Supporting CRUD endpoints are not automatically extra business use cases. No new include/extend relations are asserted by this catalog.

Companions: [Traceability](traceability.md), [Route inventory](route-inventory.md), [Endpoint inventory](endpoint-inventory.md), [Old-to-new comparison](old-to-new.md), [Audit findings](audit-findings.md).
