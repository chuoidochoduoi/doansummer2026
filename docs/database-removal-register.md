# CareS database removal register

This register is the reference before removing a table or a database field from CareS.
It separates physical database cleanup from items that are merely hidden in the report ERD.

## Protected tables — do not remove

| Table | Current purpose | Evidence |
|---|---|---|
| `clinical_form_template` | Identifies a fixed clinical/laboratory result form. | `FixedClinicalFormService` reads it when opening a result form. |
| `clinical_form_template_version` | Stores the published JSON schema used to render the fields. | `LAB-CBC-V1` currently has 24 fields. |
| `medical_service_form_template` | Maps a medical service to its fixed form. | Current binding: `LAB-001` → `LAB-CBC-V1`; `service_id` is unique. |
| `test_request` | Stores laboratory/paraclinical requests. | Used by the laboratory workbench and queue workflow. |
| `test_result` | Stores the current result, structured values and one PDF URL. | `result_data`, `form_template_version_id`, and `image_url` are active. |

The first three tables form one feature and must be assessed together. Removing any one of
them breaks the clinical form displayed in the laboratory workbench.

## Tables physically removed on purpose

| Removed table | Reason | Current replacement |
|---|---|---|
| `test_result_revision` | Result amendment/version history is outside the simplified project scope. | One editable/current record in `test_result`. |
| `test_result_attachment` | Multiple attachments are outside the simplified project scope. | One PDF path in `test_result.image_url`; the protected download endpoint remains active. |
| `contact_request` | The contact form now sends email and uses rate limiting; it no longer stores requests locally. | `ContactRequestService.send()` and email delivery. |
| `feedback_target` | The former doctor/manager explanation-target workflow is not exposed by the current UI. | General feedback data retained only in the active workflow, where applicable. |
| `medical_service_relation` | Legacy service-relation catalogue is not used by the current fixed service selection flow. | Active `medical_service`, category and capability tables. |
| `mock_bhyt_card` | The in-process mock was moved outside the main application. | External `mock-bhxh-service` through `BHXH_SERVICE_URL`. |
| `staff_attendance` | QR attendance is outside the current clinic scope. | Staff schedules and duty validation. |
| `attendance_adjustment` | Belonged to the removed attendance workflow. | Not replaced in the current scope. |
| `attendance_qr_token` | Belonged to the removed QR attendance workflow. | Not replaced in the current scope. |

## Removed columns/relations

| Removed item | Reason | Canonical replacement |
|---|---|---|
| `medical_record.vital_signs_id` | Duplicated the relationship and produced two ERD lines. | `vital_signs.medical_record_id` is the single 1:1 FK. |
| `test_request.performed_at` | Duplicated execution timing already derived from the active workflow/result. | Current request, queue and result timestamps. |
| `notification.failure_reason` | No retry/failure-management UI or active business use. | Notification status and delivery flow. |
| `staff_info.bank_account` | Payroll/banking is outside the project scope. | None. |
| Legacy contact workflow fields such as `internal_note`, `accepted_at`, `completed_at` | The database-backed contact workflow was removed. | Email contact flow. |
| Legacy feedback target fields | The target/explanation workflow was removed from both backend and UI. | Current feedback scope only. |

## Tables hidden from the report ERD but not removed

`insurance` and `insurance_rule` may be hidden from a scope-focused report diagram when they
are presented as external BHYT/mock-support data. They still exist physically and must not be
dropped while invoice insurance calculation or the external BHYT adapter references them.

## Required check before future cleanup

1. Search backend, frontend, SQL seed, migrations and tests for both snake_case and Java/camelCase names.
2. Confirm whether the item is physically removed or only omitted from a scope-focused ERD.
3. Check foreign keys and actual rows in PostgreSQL.
4. Run `db/verify_required_clinical_tables.sql` after rebuilding the database.
5. Do not remove a protected table without replacing its read/write path first.

