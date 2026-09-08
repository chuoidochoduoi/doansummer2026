# Old to New Catalog Comparison

The supplied tab-delimited catalog is the reference. Removed or merged IDs remain reserved and are not reused. DS identifiers retain their meaning but no longer imply a Diagnostic Service Staff actor.

| Old or new ID | Old use-case name | Disposition | Current ID | Explanation |
| --- | --- | --- | --- | --- |
| G-01 | View Public Clinic Information | Retained | G-01 | Description and authorization constraints refreshed from current source. |
| G-02 | Register Account | Retained | G-02 | Description and authorization constraints refreshed from current source. |
| G-03 | Create Guest Appointment | Retained | G-03 | Description and authorization constraints refreshed from current source. |
| G-04 | Log In | Merged | RU-01 | Guest login entry is part of shared account authentication; no second login goal. |
| RU-01 | Log In and Log Out | Retained | RU-01 | Description and authorization constraints refreshed from current source. |
| RU-02 | Verify OTP | Retained | RU-02 | Description and authorization constraints refreshed from current source. |
| RU-03 | Recover Password | Retained | RU-03 | Description and authorization constraints refreshed from current source. |
| RU-04 | Change Password | Retained | RU-04 | Description and authorization constraints refreshed from current source. |
| RU-05 | View and Update Profile | Retained | RU-05 | Description and authorization constraints refreshed from current source. |
| RU-06 | View Notifications | Retained and clarified | RU-06 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| CU-01 | Create Appointment | Retained | CU-01 | Description and authorization constraints refreshed from current source. |
| CU-02 | View Appointment List and Details | Retained | CU-02 | Description and authorization constraints refreshed from current source. |
| CU-03 | Confirm or Cancel Appointment | Retained and clarified | CU-03 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| CU-04 | View Follow-up Appointment | Retained and clarified | CU-04 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| CU-05 | Check In or Present Appointment Information | Reassigned | RC-03,CU-02 | Reception performs check-in. Customer can view the booking; there is no Customer self-check-in action. |
| CU-06 | Track Examination Journey | Retained | CU-06 | Description and authorization constraints refreshed from current source. |
| CU-07 | View Medical Records | Retained and clarified | CU-07 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| CU-08 | View Prescriptions and Diagnostic Results | Retained and clarified | CU-08 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| CU-09 | View Invoice and Payment History | Retained | CU-09 | Description and authorization constraints refreshed from current source. |
| CU-10 | Initiate Online Payment | Reassigned | CA-12,EXT-01 | Current PayOS link creation is at the counter, not a Customer payment button. Customer card payment is separately recorded as backend-only CU-22. |
| CU-11 | Send Message to Receptionist | Retained | CU-11 | Description and authorization constraints refreshed from current source. |
| CU-12 | View Conversation History | Retained and clarified | CU-12 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| CU-13 | Submit and View Feedback | Retained and clarified | CU-13 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| RC-01 | Search Customer | Retained and clarified | RC-01 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| RC-02 | Create or Complete Customer Profile | Retained and clarified | RC-02 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| RC-03 | Check In Scheduled Customer | Retained and clarified | RC-03 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| RC-04 | Create Walk-in Examination Ticket | Retained and clarified | RC-04 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| RC-05 | View Examination Ticket List and Details | Retained and clarified | RC-05 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| RC-06 | Update Permitted Appointment Information | Retained | RC-06 | Description and authorization constraints refreshed from current source. |
| RC-07 | Support Queue and Customer Direction | Retained and clarified | RC-07 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| RC-08 | Manage Customer Conversations | Retained and clarified | RC-08 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| RC-09 | View Relevant Notifications | Merged | RU-06 | Shared notification inbox; no separate receptionist notification engine. |
| CA-01 | View Pending Invoices | Retained | CA-01 | Description and authorization constraints refreshed from current source. |
| CA-02 | View Invoice Details | Retained | CA-02 | Description and authorization constraints refreshed from current source. |
| CA-03 | Confirm Direct Payment | Retained and clarified | CA-03 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| CA-04 | Monitor Online Payment | Retained | CA-04 | Description and authorization constraints refreshed from current source. |
| CA-05 | View Payment History | Retained and clarified | CA-05 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| CA-06 | Print or Provide Invoice | Retained and clarified | CA-06 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| NU-01 | View Assigned Queue | Retained | NU-01 | Description and authorization constraints refreshed from current source. |
| NU-02 | Call or Mark Customer Absent | Retained and clarified | NU-02 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| NU-03 | Receive and Prepare Customer | Retained and clarified | NU-03 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| NU-04 | Record Vital Signs | Retained | NU-04 | Description and authorization constraints refreshed from current source. |
| NU-05 | View Relevant Customer Information | Retained and clarified | NU-05 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| NU-06 | Support Examination or Diagnostic Workflow | Retained and clarified | NU-06 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| NU-07 | Update Permitted Queue Status | Retained and clarified | NU-07 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| DR-01 | View Examination Queue | Retained | DR-01 | Description and authorization constraints refreshed from current source. |
| DR-02 | Call and Receive Customer | Retained and clarified | DR-02 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| DR-03 | View Medical Records and Examination History | Retained and clarified | DR-03 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| DR-04 | Record Medical Examination | Retained | DR-04 | Description and authorization constraints refreshed from current source. |
| DR-05 | Record Diagnosis and Treatment Plan | Retained | DR-05 | Description and authorization constraints refreshed from current source. |
| DR-06 | Create Prescription | Retained | DR-06 | Description and authorization constraints refreshed from current source. |
| DR-07 | Request Diagnostic Services | Retained | DR-07 | Description and authorization constraints refreshed from current source. |
| DR-08 | Request Another Examination Service | Removed | — | Specialist-referral/general-practice flow has been removed from the current product. ID is reserved. |
| DR-09 | View Diagnostic Results | Retained and clarified | DR-09 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| DR-10 | Save Medical Record as Draft | Retained | DR-10 | Description and authorization constraints refreshed from current source. |
| DR-11 | Resume Examination after Diagnostic Services | Retained | DR-11 | Description and authorization constraints refreshed from current source. |
| DR-12 | Complete Medical Record | Retained | DR-12 | Description and authorization constraints refreshed from current source. |
| DR-13 | Create Follow-up Request | Retained and clarified | DR-13 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| DR-14 | View Same-day Examination Information | Retained and clarified | DR-14 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| DS-01 | View Diagnostic Queue | Actor reassigned | DS-01 | Diagnostic Service Staff removed; current actors: Doctor, Nurse. Same ID retained. |
| DS-02 | Call and Receive Customer | Actor reassigned | DS-02 | Diagnostic Service Staff removed; current actors: Doctor, Nurse. Same ID retained. |
| DS-03 | View Diagnostic Request | Actor reassigned | DS-03 | Diagnostic Service Staff removed; current actors: Doctor, Nurse. Same ID retained. |
| DS-04 | Record Specimen Information | Actor reassigned | DS-04 | Diagnostic Service Staff removed; current actors: Doctor, Nurse. Same ID retained. |
| DS-05 | Perform Diagnostic Service | Actor reassigned | DS-05 | Diagnostic Service Staff removed; current actors: Doctor, Nurse. Same ID retained. |
| DS-06 | Enter Diagnostic Result | Actor reassigned | DS-06 | Diagnostic Service Staff removed; current actors: Doctor, Nurse. Same ID retained. |
| DS-07 | Upload Result File | Actor reassigned | DS-07 | Diagnostic Service Staff removed; current actors: Doctor, Nurse. Same ID retained. |
| DS-08 | Save Result as Draft | Actor reassigned | DS-08 | Diagnostic Service Staff removed; current actors: Doctor, Nurse. Same ID retained. |
| DS-09 | Sign or Publish Result | Actor reassigned | DS-09 | Diagnostic Service Staff removed; current actors: Doctor. Same ID retained. |
| DS-10 | Complete Diagnostic Request | Actor reassigned | DS-10 | Diagnostic Service Staff removed; current actors: Doctor. Same ID retained. |
| DS-11 | Mark Customer Absent or Update Queue | Actor reassigned | DS-11 | Diagnostic Service Staff removed; current actors: Doctor, Nurse. Same ID retained. |
| CM-01 | View Operational Dashboard and Reports | Retained and clarified | CM-01 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| CM-02 | Manage Staff Information | Retained | CM-02 | Description and authorization constraints refreshed from current source. |
| CM-03 | Manage Staff Schedules | Retained | CM-03 | Description and authorization constraints refreshed from current source. |
| CM-04 | Manage Services and Prices | Retained | CM-04 | Description and authorization constraints refreshed from current source. |
| CM-05 | Manage Departments and Rooms | Retained | CM-05 | Description and authorization constraints refreshed from current source. |
| CM-06 | Manage Feedback | Retained and clarified | CM-06 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| CM-07 | View Customer-flow Information | Merged | RC-07 | Manager shares the staff patient journey goal; do not duplicate it by actor. |
| CM-08 | Manage Public or Operational Announcements | Retained and clarified | CM-08 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| AD-01 | Manage User Accounts | Retained | AD-01 | Description and authorization constraints refreshed from current source. |
| AD-02 | Manage Roles and Permissions | Retained and clarified | AD-02 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| AD-03 | Manage Clinic Configuration | Retained and clarified | AD-03 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| AD-04 | Manage Clinical and Diagnostic Templates | Reassigned | CM-09 | Clinical form-template management belongs to Clinic Manager, not Administrator. |
| AD-05 | Manage ICD-10 Catalog | Retained and clarified | AD-05 | ID and related goal retained; name/scope corrected to source. Current delivery: Backend only. |
| AD-06 | View Audit Information | Retained and clarified | AD-06 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| AD-07 | Manage Shift Configuration | Retained and clarified | AD-07 | ID and related goal retained; name/scope corrected to source. Current delivery: UI + backend. |
| AD-08 | Monitor System Integrations | Not implemented as a screen | EXT-01,EXT-02,EXT-03 | External integrations exist, but no dedicated integration-health/monitoring dashboard is registered. |
| EXT-01 | Process Online Payment | Retained | EXT-01 | Description and authorization constraints refreshed from current source. |
| EXT-02 | Simulate Insurance Verification | Retained | EXT-02 | Description and authorization constraints refreshed from current source. |
| EXT-03 | Deliver OTP and Notification | Retained and clarified | EXT-03 | ID and related goal retained; name/scope corrected to source. Current delivery: External integration. |
| EXT-04 | Display Queue Updates | Reclassified inside system | RC-15 | Queue display is an authenticated system interface, not an external actor. |
| G-05 | — | Added | G-05 | View Public Department Schedule; UI + backend. |
| G-06 | — | Added | G-06 | Look Up Guest Journey; UI + backend. |
| G-07 | — | Added | G-07 | Request Same-day Return as Guest; UI + backend. |
| G-08 | — | Added | G-08 | Use Guest Support Chat; UI + backend. |
| G-09 | — | Added | G-09 | Submit Contact Enquiry; UI + backend. |
| RU-07 | — | Added | RU-07 | View Own Duty Schedule; UI + backend. |
| RU-08 | — | Added | RU-08 | Set Local Interface Preferences; Frontend only by design. |
| CU-14 | — | Added | CU-14 | Manage Family Profiles; UI + backend. |
| CU-15 | — | Added | CU-15 | Create Family Group Booking; UI + backend. |
| CU-16 | — | Added | CU-16 | Change Appointment Details; UI + backend. |
| CU-17 | — | Added | CU-17 | Request Same-day Return; UI + backend. |
| CU-18 | — | Added | CU-18 | Register CareS Prepaid Card; UI + backend. |
| CU-19 | — | Added | CU-19 | View CareS Card and Balance History; UI + backend. |
| CU-20 | — | Added | CU-20 | Print an Individual Medical Record; UI + backend. |
| CU-21 | — | Added | CU-21 | Print Customer Receipt; UI + backend. |
| CU-22 | — | Added | CU-22 | Pay with Own CareS Card through API; Backend only. |
| RC-10 | — | Added | RC-10 | Confirm Patient Return; UI + backend. |
| RC-11 | — | Removed and reserved | — | The separate Receptionist follow-up queue was removed. Doctors create follow-up appointments from the medical record; Receptionists support existing appointments through ordinary appointment management. |
| RC-12 | — | Added | RC-12 | Review Patient Visit History; UI + backend. |
| RC-13 | — | Added | RC-13 | Recover an Eligible Blocked Journey through API; Backend only. |
| RC-14 | — | Added | RC-14 | Open Queue Display Screens; UI + backend. |
| RC-15 | — | Added | RC-15 | View Overall or Room Calling Display; UI + backend. |
| CA-07 | — | Added | CA-07 | Verify and Apply Health Insurance; UI + backend. |
| CA-08 | — | Added | CA-08 | Accept CareS Card Payment; UI + backend. |
| CA-09 | — | Added | CA-09 | Top Up CareS Card; UI + backend. |
| CA-10 | — | Added | CA-10 | Review and Print Top-up History; UI + backend. |
| CA-11 | — | Added | CA-11 | Cancel an Eligible Invoice; UI + backend. |
| CA-12 | — | Added | CA-12 | Create PayOS Payment Link; UI + backend. |
| DR-15 | — | Added | DR-15 | Verify Patient Allergies; UI + backend. |
| DR-16 | — | Added | DR-16 | Continue Next Examination in the Same Room; UI + backend. |
| DR-17 | — | Added | DR-17 | Finish Carried-over Clinical Work; UI + backend. |
| DS-12 | — | Added | DS-12 | Cancel an Eligible Diagnostic Request; UI + backend. |
| DS-13 | — | Added | DS-13 | View Result Revision History; UI + backend. |
| DS-14 | — | Added | DS-14 | Amend a Signed Result through API; Backend only. |
| CM-09 | — | Added | CM-09 | Manage Diagnostic Form Templates; UI + backend. |
| CM-10 | — | Added | CM-10 | Print or Export Reports; UI + backend. |
| CM-11 | — | Added | CM-11 | View Patient Directory; UI + backend. |
| CM-12 | — | Added | CM-12 | Configure CareS Policy; UI + backend. |
| CM-13 | — | Added | CM-13 | Review CareS Ledger and Reverse Eligible Payment; UI + backend. |
| AD-09 | — | Added | AD-09 | Manage Operating Exceptions; UI + backend. |
| AD-10 | — | Added | AD-10 | Check Service Coverage; UI + backend. |
| AD-11 | — | Added | AD-11 | View Fixed Technical Catalog; UI + backend. |
| AD-12 | — | Added | AD-12 | Manage Schedule Templates through API; Backend only. |
| AD-13 | — | Added | AD-13 | Administer Notification Records through API; Backend only. |

Actor changes: remove Diagnostic Service Staff and Queue Display Device; retain one Doctor; separate the previous SMS/Email integration label into Email Service and SMS Gateway, sharing EXT-03. General/Specialist Doctor are not reintroduced.
