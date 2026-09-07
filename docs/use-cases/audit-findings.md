# Audit Findings and Verification Scope

Review date: 6 September 2026. Only documentation is changed in this task.

## Source baseline

| Repository | HEAD at start | State |
| --- | --- | --- |
| Backend | 2260d7a0ab56f3082c995bd118b700b53bea0ffc | Existing uncommitted source/test changes preserved |
| Frontend | b01474a00a7a8cda23bc4c557b279600283bc0c5 | Existing uncommitted source changes preserved |

The recorded baseline hashes tracked and nonignored untracked files, including existing tests. Ignored build/runtime data was not modified. The current catalog targets these working trees, not only the HEAD commit.

## Findings that affect the catalog

| ID | Category | Observation | Evidence | Treatment |
| --- | --- | --- | --- | --- |
| F-01 | Route/API role mismatch | The appointment-detail route permits Doctor/Nurse, but AppointmentController get/update annotations permit reception/management, not those clinical roles. | src/App.jsx; AppointmentController.java | Documented as mismatch; no clinical reception-edit association added. |
| F-02 | Backend only recovery | PatientJourneyPage states that steps cannot be advanced from this screen. POST advance is restricted to Clinic Manager/Admin. | PatientJourneyPage.jsx; PatientJourneyController.advance | RC-13 is API-only and not a Receptionist action. |
| F-03 | Current direct follow-up booking | ExaminationPage confirmation invokes createFollowUpAppointment, sending a fixed 08:00 scheduled time and serviceIds without shiftId or the displayed advice. MedicalRecordService.scheduleFollowUp creates Appointment directly and links it to the record. | ExaminationPage.jsx around createFollowUpAppointment; MedicalRecordService.scheduleFollowUp | DR-13 describes direct booking, not recommendation-only. Payload/shift/advice and parity with reception scheduling need separate functional review; no code changed. |
| F-04 | Payment channel difference | PayOS links are created by the counter UI; the Customer screen displays history/receipts. Customer membership-card payment API exists without a current payment UI. | useInvoiceDetail.js; MembershipCardController; customer payment/card pages | Old CU-10 moved to CA-12. CU-22 explicitly backend-only. |
| F-05 | Fixed catalog | ServiceCapabilityController create/update/delete deliberately reject mutation. The current capability page is read-only. | ServiceCapabilityController; CapabilityManagementPage.jsx | AD-11 is view only; rejected mappings remain classified in inventory. |
| F-06 | Permission limits | Template management is Clinic Manager only; feedback management is Receptionist/Clinic Manager; CareS reversal is Administrator/Clinic Manager. | ClinicalFormTemplateController; FeedbackController; MembershipCardController; App.jsx | No extra Doctor/Nurse/Cashier management associations inferred. |
| F-07 | Integration claims | SMS provider is mock in current configuration. Mock BHXH includes a demonstration branch and configured verification service. Notification SENT flags are not proof of email/SMS delivery. | application configuration; SmsSender implementations; Bhxh service; NotificationService | External actors distinguished from deployment verification. No live messages or payment requests sent. |
| F-08 | Clinical capability granularity | Room diagnostic eligibility checks compatible active room capabilities rather than a general legal/professional certification. Actual operations still check duty, assignment and record/state. | StaffDutyService; MedicalRecordService; TestRequestService | Catalog describes software authorization, not certification that each Doctor may perform every medical technique. |
| F-09 | API and compatibility inventory | Generic transaction, invoice, queue, record, service-category and schedule endpoints remain alongside richer screen workflows. ICD editing and result amendment have no dedicated current screen. | endpoint-inventory.md | Kept as API-only/support/compatibility evidence; no invented navigation or CRUD screen. |

## Internal responsibilities not actors

- SystemCleanupService performs scheduled closing and expiry. QueuePriorityService ranks eligible tickets; workflow services coordinate blocked steps and source-record return. These are system behaviors, not external actors.
- WebSocket queue updates and polling synchronize system screens. The TV is another presentation of the same queue, not a patient-calling decision maker.
- Authentication filters, audit aspects, repositories, result revision signing/storage and chatbot rules are implementation mechanisms supporting the catalog goals.

## Verification performed

- Reconciled 125 unique IDs, 194 explicit associations, 90 old IDs, 71 registered routes and 314 application endpoint mappings.
- Validated referenced frontend files, controller methods, direct service type links and existing test filenames. Every route is assigned; every endpoint is assigned or explicitly classified.
- Related test files are evidence/navigation links only. Unit tests, integration tests, live Swagger and browser workflows were not run for this documentation-only task. No claim is made about current JUnit coverage or deployment behavior.
- Word rendering and final repository preservation results are recorded in verification.md after generation.
