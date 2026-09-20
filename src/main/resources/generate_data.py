import uuid
from datetime import datetime, timedelta
import random
import json

def get_uuid(seed):
    import hashlib
    m = hashlib.md5()
    m.update(str(seed).encode('utf-8'))
    return str(uuid.UUID(m.hexdigest()))

departments = [
    {"id": get_uuid("dept1"), "code": "INT-101", "name": "Ph?ng khám N?i 1", "type": "EXAMINATION", "spec_id": "00000001-1111-1111-1111-111111111111", "desc": "N?i khoa"},
    {"id": get_uuid("dept2"), "code": "INT-102", "name": "Ph?ng khám N?i 2", "type": "EXAMINATION", "spec_id": "00000001-1111-1111-1111-111111111111", "desc": "N?i khoa"},
    {"id": get_uuid("dept3"), "code": "SUR-201", "name": "Ph?ng khám Ngo?i 1", "type": "EXAMINATION", "spec_id": "00000003-3333-3333-3333-333333333333", "desc": "Ngo?i khoa"},
    {"id": get_uuid("dept4"), "code": "SUR-202", "name": "Ph?ng khám Ngo?i 2", "type": "EXAMINATION", "spec_id": "00000003-3333-3333-3333-333333333333", "desc": "Ngo?i khoa"},
    {"id": get_uuid("dept5"), "code": "PED-301", "name": "Ph?ng khám Nhi", "type": "EXAMINATION", "spec_id": "00000002-2222-2222-2222-222222222222", "desc": "Nhi khoa"},
    {"id": get_uuid("dept6"), "code": "DER-401", "name": "Ph?ng khám Da li?u", "type": "EXAMINATION", "spec_id": "00000004-4444-4444-4444-444444444444", "desc": "Da li?u"},
    {"id": get_uuid("dept7"), "code": "OBG-501", "name": "Ph?ng khám S?n ph? khoa", "type": "EXAMINATION", "spec_id": "00000008-8888-8888-8888-888888888888", "desc": "S?n ph? khoa"},
    {"id": get_uuid("dept8"), "code": "LAB-601", "name": "Ph?ng xét nghi?m", "type": "PARACLINICAL", "spec_id": "NULL", "desc": "Xét nghi?m huy?t h?c, sinh hóa, ný?c ti?u"},
    {"id": get_uuid("dept9"), "code": "IMG-701", "name": "Ph?ng siêu âm", "type": "PARACLINICAL", "spec_id": "NULL", "desc": "Siêu âm, ði?n tim"},
    {"id": get_uuid("dept10"), "code": "XR-702", "name": "Ph?ng X-quang", "type": "PARACLINICAL", "spec_id": "NULL", "desc": "X-quang"}
]

capabilities = [
    {"id": "ca000001-0000-0000-0000-000000000001", "code": "HEMATOLOGY"},
    {"id": "ca000002-0000-0000-0000-000000000002", "code": "BIOCHEMISTRY"},
    {"id": "ca000003-0000-0000-0000-000000000003", "code": "ULTRASOUND"},
    {"id": "ca000004-0000-0000-0000-000000000004", "code": "XRAY"},
    {"id": "ca000005-0000-0000-0000-000000000005", "code": "URINALYSIS"},
    {"id": "ca000006-0000-0000-0000-000000000006", "code": "ECG"},
    {"id": "ca000007-0000-0000-0000-000000000007", "code": "MICROBIOLOGY"},
    {"id": "ca000008-0000-0000-0000-000000000008", "code": "RAPID_TEST"}
]

dept_capabilities = {
    get_uuid("dept8"): ["ca000001-0000-0000-0000-000000000001", "ca000002-0000-0000-0000-000000000002", "ca000005-0000-0000-0000-000000000005", "ca000007-0000-0000-0000-000000000007", "ca000008-0000-0000-0000-000000000008"],
    get_uuid("dept9"): ["ca000003-0000-0000-0000-000000000003", "ca000006-0000-0000-0000-000000000006"],
    get_uuid("dept10"): ["ca000004-0000-0000-0000-000000000004"]
}

staff = [
    # ADMIN & RECEPTION
    {"role": "ADMIN", "user": "admin", "name": "Ph?m Ð?c Minh", "dept": "NULL", "doctor": False},
    {"role": "CLINIC_MANAGER", "user": "clinicmanager", "name": "Nguy?n Thu Hýõng", "dept": "NULL", "doctor": False},
    {"role": "RECEPTIONIST", "user": "receptionist", "name": "Tr?nh Th? Ki?u Oanh", "dept": "NULL", "doctor": False},
    {"role": "CASHIER", "user": "cashier", "name": "Ðinh Vãn Quang", "dept": "NULL", "doctor": False},
    
    # DOCTORS
    {"role": "DOCTOR", "user": "doctor1", "name": "BS. Tr?n Minh Tu?n", "dept": get_uuid("dept1"), "doctor": True},
    {"role": "DOCTOR", "user": "doctor2", "name": "BS. Nguy?n Th? Lan Anh", "dept": get_uuid("dept2"), "doctor": True},
    {"role": "DOCTOR", "user": "doctor3", "name": "BS. Nguy?n Ð?c Khoa", "dept": get_uuid("dept3"), "doctor": True},
    {"role": "DOCTOR", "user": "doctor4", "name": "BS. Lê Hoàng Nam", "dept": get_uuid("dept4"), "doctor": True},
    {"role": "DOCTOR", "user": "doctor5", "name": "BS. Ph?m Qu?c B?o", "dept": get_uuid("dept5"), "doctor": True},
    {"role": "DOCTOR", "user": "doctor6", "name": "BS. Hoàng Th? Bích Ng?c", "dept": get_uuid("dept6"), "doctor": True},
    {"role": "DOCTOR", "user": "doctor7", "name": "BS. Ð? Th? Phýõng Th?o", "dept": get_uuid("dept7"), "doctor": True},
    {"role": "DOCTOR", "user": "doctor8", "name": "KTV. Bùi Vãn Thành", "dept": get_uuid("dept8"), "doctor": True}, # Ph? trách khoa xét nghi?m
    {"role": "DOCTOR", "user": "doctor9", "name": "BS. V? Ð?nh Phúc", "dept": get_uuid("dept9"), "doctor": True},
    {"role": "DOCTOR", "user": "doctor10", "name": "KTV. Ðoàn Minh Quân", "dept": get_uuid("dept10"), "doctor": True}, # Ph? trách XQuang
    
    # NURSES / KTV
    {"role": "NURSE", "user": "nurse1", "name": "ÐÐ. Lê Th? H?ng Nhung", "dept": get_uuid("dept1"), "doctor": False},
    {"role": "NURSE", "user": "nurse2", "name": "ÐÐ. Ph?m Vãn Ð?c", "dept": get_uuid("dept2"), "doctor": False},
    {"role": "NURSE", "user": "nurse3", "name": "ÐÐ. V? Th? Mai Linh", "dept": get_uuid("dept3"), "doctor": False},
    {"role": "NURSE", "user": "nurse4", "name": "ÐÐ. Ð?ng Th? Thùy Dung", "dept": get_uuid("dept4"), "doctor": False},
    {"role": "NURSE", "user": "nurse5", "name": "ÐÐ. Nguy?n Th? Thanh Hà", "dept": get_uuid("dept5"), "doctor": False},
    {"role": "NURSE", "user": "nurse6", "name": "ÐÐ. Tr?n Vãn Hùng", "dept": get_uuid("dept6"), "doctor": False},
    {"role": "NURSE", "user": "nurse7", "name": "ÐÐ. Lê Th? Ng?c Ánh", "dept": get_uuid("dept7"), "doctor": False},
    {"role": "NURSE", "user": "nurse8", "name": "KTV. Ngô Th? H?i Y?n", "dept": get_uuid("dept8"), "doctor": False},
    {"role": "NURSE", "user": "nurse9", "name": "KTV. Phan Th? M? Duyên", "dept": get_uuid("dept9"), "doctor": False},
    {"role": "NURSE", "user": "nurse10", "name": "KTV. Hoàng Th? Kim Chi", "dept": get_uuid("dept10"), "doctor": False},
]

# Generate UUIDs for staff
for idx, s in enumerate(staff):
    s['account_id'] = get_uuid(f"acc_{s['user']}")
    s['profile_id'] = get_uuid(f"prof_{s['user']}")
    s['staff_id'] = get_uuid(f"staff_{s['user']}")

patients = []
for i in range(1, 27):
    is_guest = True if i > 21 else False
    has_account = not is_guest
    user = f"patient{i}"
    patients.append({
        "account_id": get_uuid(f"acc_{user}") if has_account else "NULL",
        "profile_id": get_uuid(f"prof_{user}"),
        "name": f"B?nh Nhân {i}",
        "phone": f"0987654{i:03d}",
        "is_guest": is_guest,
        "is_demo": i == 1
    })

sql = []
sql.append(\"\"\"
\\set ON_ERROR_STOP on
SET cares.demo_reset = 'yes';

BEGIN;
SET LOCAL TIME ZONE 'Asia/Ho_Chi_Minh';

CREATE TEMP TABLE demo2_clock ON COMMIT DROP AS
SELECT CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh' moment;
CREATE OR REPLACE FUNCTION pg_temp.demo_now() RETURNS timestamp LANGUAGE sql STABLE AS 'SELECT moment FROM demo2_clock';
CREATE OR REPLACE FUNCTION pg_temp.demo_date() RETURNS date LANGUAGE sql STABLE AS 'SELECT moment::date FROM demo2_clock';
CREATE OR REPLACE FUNCTION pg_temp.did(key text) RETURNS uuid LANGUAGE sql IMMUTABLE AS 'SELECT md5(''CareS-demo:''||key)::uuid';
CREATE OR REPLACE FUNCTION pg_temp.event(c record, rel_offset float) RETURNS timestamp LANGUAGE plpgsql STABLE AS \\$\\$
BEGIN
    RETURN c.started + (c.completed_at - c.started) * rel_offset;
END \\$\\$;

ALTER TABLE membership_card ADD COLUMN IF NOT EXISTS benefit_starts_at timestamp;

TRUNCATE TABLE
    clinic_information, membership_card_ledger, membership_card, membership_policy, family_member,
    clinic_schedule_exception, shift_version,
    audit_log, contact_request,
    chat_messages, chat_sessions, feedback_target, icd_10_selections,
    insurance_rule, invoice_item, notification, public_announcement, payment_transaction,
    prescription_item, staff_schedule, staff_schedule_template,
    test_result, test_request, vital_signs, medical_record, queue_ticket,
    invoice, customer_visit, appointment_services, appointment,
    department_capability, staff_capability, staff_info, medical_service,
    service_category, service_capability, medicine_catalog, icd_10_codes,
    insurance, department, specialization, shift_config, profile, account
RESTART IDENTITY CASCADE;

INSERT INTO membership_policy (policy_id, created_at, updated_at, deleted, minimum_top_up, discount_percent, validity_months, active)
VALUES ('7c000001-0000-0000-0000-000000000001', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 1000000, 15, 12, true);

INSERT INTO specialization (specialization_id, created_at, updated_at, deleted, active, name, description) VALUES
('00000001-1111-1111-1111-111111111111', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, true, 'N?i khoa', 'Khám n?i khoa'),
('00000002-2222-2222-2222-222222222222', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, true, 'Nhi khoa', 'Khám nhi khoa'),
('00000003-3333-3333-3333-333333333333', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, true, 'Ngo?i khoa', 'Khám ngo?i khoa'),
('00000004-4444-4444-4444-444444444444', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, true, 'Da li?u', 'Khám da li?u'),
('00000008-8888-8888-8888-888888888888', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, true, 'S?n ph? khoa', 'S?n ph? khoa');
\"\"\")

sql.append("-- Departments")
sql.append("INSERT INTO department (department_id, created_at, updated_at, deleted, room_code, name, status, department_type, specialization_id, description, head_doctor_id) VALUES")
dept_vals = []
for d in departments:
    spec = f"'{d['spec_id']}'" if d['spec_id'] != "NULL" else "NULL"
    head_doc = "NULL"
    for s in staff:
        if s['dept'] == d['id'] and s['doctor']:
            head_doc = f"'{s['staff_id']}'"
            break
    dept_vals.append(f"('{d['id']}', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '{d['code']}', '{d['name']}', 'AVAILABLE', '{d['type']}', {spec}, '{d['desc']}', {head_doc})")
sql.append(",\n".join(dept_vals) + ";")

sql.append("-- Accounts")
sql.append("INSERT INTO account (account_id, created_at, is_active, password_hash, role, username) VALUES")
acc_vals = []
for s in staff:
    acc_vals.append(f"('{s['account_id']}', (pg_temp.demo_now()-interval '60 days'), true, '/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', '{s['user']}')")
for p in patients:
    if p['account_id'] != "NULL":
        acc_vals.append(f"('{p['account_id']}', (pg_temp.demo_now()-interval '60 days'), true, '/y1TOJXb2oI0yAF/86Qyy4T9m', 'CUSTOMER', '{p['phone']}')")
sql.append(",\n".join(acc_vals) + ";")

sql.append("-- Profiles")
sql.append("INSERT INTO profile (profile_id, account_id, created_at, updated_at, deleted, full_name, date_of_birth, gender, phone, email, address, blood_type) VALUES")
prof_vals = []
for idx, s in enumerate(staff):
    prof_vals.append(f"('{s['profile_id']}', '{s['account_id']}', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '{s['name']}', '1980-01-01', 'MALE', '098000{idx:04d}', '{s['user']}@cares.vn', 'Hà N?i', NULL)")
for p in patients:
    acc = f"'{p['account_id']}'" if p['account_id'] != "NULL" else "NULL"
    prof_vals.append(f"('{p['profile_id']}', {acc}, (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '{p['name']}', '1990-01-01', 'FEMALE', '{p['phone']}', 'patient{p['phone']}@example.com', 'Hà N?i', NULL)")
sql.append(",\n".join(prof_vals) + ";")


sql.append("-- StaffInfo")
sql.append("INSERT INTO staff_info (staff_id, created_at, updated_at, deleted, profile_id, staff_code, system_role, national_id, highest_degree, university, license_number, specialization_id, department_id) VALUES")
staff_vals = []
for idx, s in enumerate(staff):
    dept = f"'{s['dept']}'" if s['dept'] != "NULL" else "NULL"
    staff_vals.append(f"('{s['staff_id']}', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '{s['profile_id']}', 'STF-{idx:04d}', '{s['role']}', '001000{idx:06d}', NULL, NULL, NULL, NULL, {dept})")
sql.append(",\n".join(staff_vals) + ";")


sql.append(\"\"\"
-- Capabilities
INSERT INTO service_capability (capability_id, created_at, updated_at, deleted, code, name, description, active) VALUES
('ca000001-0000-0000-0000-000000000001', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'HEMATOLOGY', 'Xét nghi?m huy?t h?c', 'Công th?c máu', true),
('ca000002-0000-0000-0000-000000000002', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'BIOCHEMISTRY', 'Xét nghi?m sinh hóa', 'Sinh hóa máu', true),
('ca000003-0000-0000-0000-000000000003', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'ULTRASOUND', 'Siêu âm', 'Siêu âm', true),
('ca000004-0000-0000-0000-000000000004', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'XRAY', 'X-quang', 'X-quang', true),
('ca000005-0000-0000-0000-000000000005', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'URINALYSIS', 'Xét nghi?m ný?c ti?u', 'Ný?c ti?u', true),
('ca000006-0000-0000-0000-000000000006', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'ECG', 'Ði?n tim', 'Ði?n tim', true),
('ca000007-0000-0000-0000-000000000007', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'MICROBIOLOGY', 'Xét nghi?m vi sinh', 'Vi sinh', true),
('ca000008-0000-0000-0000-000000000008', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'RAPID_TEST', 'Test nhanh', 'Test nhanh', true);
\"\"\")

sql.append("-- Department Capabilities")
sql.append("INSERT INTO department_capability (department_id, capability_id) VALUES")
dept_cap_vals = []
for d, caps in dept_capabilities.items():
    for c in caps:
        dept_cap_vals.append(f"('{d}', '{c}')")
sql.append(",\n".join(dept_cap_vals) + ";")

sql.append("-- Medical Services (Keeping the core list)")
sql.append(\"\"\"
INSERT INTO medical_service (
    service_id, service_code, created_at, updated_at, deleted, description,
    status, is_point_of_care, name, price, department_type, duration_minutes,
    workflow_priority, requires_doctor_order, requires_return_to_doctor,
    requires_specimen, result_wait_minutes, allow_customer_booking,
    minimum_age, maximum_age, allowed_gender, department_id,
    required_specialization_id, required_capability_id
) VALUES
('40000001-0000-0000-0000-000000000001', 'EX-SU-001', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Khám ban ð?u các b?nh l?.', 'ACTIVE', false, 'Khám Ngo?i t?ng quát', 220000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 0, 120, NULL, NULL, '00000003-3333-3333-3333-333333333333', NULL),
('40000002-0000-0000-0000-000000000002', 'EX-IN-001', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Khám và ðánh giá t?ng quát.', 'ACTIVE', false, 'Khám N?i t?ng quát', 220000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 16, 120, NULL, NULL, '00000001-1111-1111-1111-111111111111', NULL),
('40000008-0000-0000-0000-000000000008', 'LAB-001', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Ðánh giá các thành ph?n t? bào máu.', 'ACTIVE', false, 'Công th?c máu', 120000, 'PARACLINICAL', 15, 1, true, true, true, 45, false, 0, 120, NULL, NULL, NULL, 'ca000001-0000-0000-0000-000000000001'),
('40000009-0000-0000-0000-000000000009', 'LAB-002', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Ðo n?ng ð? glucose trong máu.', 'ACTIVE', false, 'Ðý?ng huy?t', 70000, 'PARACLINICAL', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('40000015-0000-0000-0000-000000000015', 'IMG-003', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Kh?o sát các cõ quan trong ? b?ng.', 'ACTIVE', false, 'Siêu âm ? b?ng t?ng quát', 250000, 'PARACLINICAL', 20, 1, true, true, false, 10, false, 0, 120, NULL, NULL, NULL, 'ca000003-0000-0000-0000-000000000003'),
('40000017-0000-0000-0000-000000000017', 'IMG-001', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Ch?p h?nh vùng ng?c.', 'ACTIVE', false, 'X-quang ng?c', 180000, 'PARACLINICAL', 15, 1, true, true, false, 15, false, 6, 120, NULL, NULL, NULL, 'ca000004-0000-0000-0000-000000000004');
\"\"\")

sql.append(\"\"\"
-- Medicine & ICD 10
INSERT INTO medicine_catalog (medicine_id, medicine_code, name, active_ingredient, default_unit, default_usage, default_frequency_per_day, active, deleted) VALUES
('b0000000-0000-0000-0000-000000000001', 'MED-001', 'Paracetamol 500mg', 'Paracetamol', 'Viên', 'U?ng sau ãn', 2, true, false);

INSERT INTO icd_10_codes (code, name, description, category, deleted) VALUES
('J00', 'Viêm m?i h?ng c?p', 'C?m l?nh thông thý?ng', 'B?nh h? hô h?p', false),
('I10', 'Tãng huy?t áp vô cãn', 'Tãng huy?t áp nguyên phát', 'B?nh h? tu?n hoàn', false),
('Z00.0', 'Khám s?c kh?e t?ng quát', 'Khám ð?nh k?', 'Y?u t? s?c kh?e', false);
\"\"\")

sql.append(\"\"\"
-- Shift Config
INSERT INTO shift_config (shift_id, created_at, updated_at, deleted, name, start_time, end_time, is_active) VALUES
('70000001-1111-1111-1111-111111111111', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Ca Sáng', '07:30', '11:30', true),
('70000002-2222-2222-2222-222222222222', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Ca Chi?u', '13:00', '17:00', true),
('70000003-3333-3333-3333-333333333333', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Ca T?i', '17:30', '21:30', true);

INSERT INTO shift_version (shift_version_id, shift_id, start_time, end_time, effective_from, effective_to, change_reason, created_by, created_at, updated_at, deleted) VALUES
('71000001-1111-1111-1111-111111111111', '70000001-1111-1111-1111-111111111111', '07:30', '11:30', pg_temp.demo_date() - 365, NULL, 'Kh?i t?o', NULL, (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false),
('71000002-2222-2222-2222-222222222222', '70000002-2222-2222-2222-222222222222', '13:00', '17:00', pg_temp.demo_date() - 365, NULL, 'Kh?i t?o', NULL, (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false),
('71000003-3333-3333-3333-333333333333', '70000003-3333-3333-3333-333333333333', '17:30', '21:30', pg_temp.demo_date() - 365, NULL, 'Kh?i t?o', NULL, (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false);
\"\"\")

sql.append("-- Staff Schedules (Full tu?n 3 ca)")
sql.append(\"\"\"
WITH calendar AS (
    SELECT d::date AS work_date
    FROM generate_series((pg_temp.demo_date() - 30), pg_temp.demo_date() + 14, INTERVAL '1 day') d
)
INSERT INTO staff_schedule (
    schedule_id, created_at, updated_at, deleted, is_custom, note,
    status, work_date, shift_id, shift_version_id, actual_start_time, actual_end_time, staff_id, template_id
)
SELECT gen_random_uuid(), (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, false, 'Full week', 'SCHEDULED', c.work_date,
       v.shift_id, v.shift_version_id, v.start_time, v.end_time, s.staff_id, NULL
FROM calendar c
CROSS JOIN shift_version v
CROSS JOIN staff_info s;
\"\"\")

# Appointments and Visits (Histories)
sql.append("-- History Visits (25 patients)")
hist_appts = []
hist_visits = []
hist_tickets = []
hist_records = []
hist_invoices = []
for i in range(1, 26): # 25 completed
    p = patients[i]
    appt_id = get_uuid(f"appt_{i}")
    visit_id = get_uuid(f"visit_{i}")
    ticket_id = get_uuid(f"ticket_{i}")
    record_id = get_uuid(f"record_{i}")
    inv_id = get_uuid(f"inv_{i}")
    
    hist_appts.append(f"('{appt_id}', pg_temp.demo_now()-interval '{30-i} days', pg_temp.demo_now()-interval '{30-i} days', false, pg_temp.demo_date()-interval '{30-i} days' + time '08:00', 'COMPLETED', '{str(p['is_guest']).lower()}', '{p['profile_id']}', 'Ca Sáng', '07:30-11:30', '71000001-1111-1111-1111-111111111111')")
    hist_visits.append(f"('{visit_id}', '{p['profile_id']}', '{str(p['is_guest']).lower()}', 'VIS-{i:04d}', 'COMPLETED', pg_temp.demo_date()-interval '{30-i} days' + time '08:00', pg_temp.demo_date()-interval '{30-i} days' + time '09:00', pg_temp.demo_now(), pg_temp.demo_now(), false, 'walkin')")
    hist_tickets.append(f"('{ticket_id}', '{visit_id}', '{get_uuid('dept1')}', 'DONE', 1, pg_temp.demo_now()-interval '{30-i} days', pg_temp.demo_now()-interval '{30-i} days', false, 'EXAMINATION', pg_temp.demo_date()-interval '{30-i} days', '{str(p['is_guest']).lower()}', '{p['profile_id']}')")
    hist_records.append(f"('{record_id}', '{visit_id}', '{ticket_id}', 'COMPLETED', '{get_uuid('staff_doctor1')}', pg_temp.demo_now()-interval '{30-i} days', pg_temp.demo_now()-interval '{30-i} days', false)")
    hist_invoices.append(f"('{inv_id}', '{visit_id}', 'INV-{i:04d}', 220000, 0, 0, 220000, 'PAID', 220000, pg_temp.demo_now()-interval '{30-i} days', pg_temp.demo_now()-interval '{30-i} days', false)")

sql.append("INSERT INTO appointment (appointment_id, created_at, updated_at, deleted, scheduled_at, status, is_guest, customer_id, shift_name, shift_time, shift_version_id) VALUES")
sql.append(",\n".join(hist_appts) + ";")

sql.append("INSERT INTO customer_visit (visit_id, customer_id, is_guest, visit_code, status, check_in_time, check_out_time, created_at, updated_at, deleted, source) VALUES")
sql.append(",\n".join(hist_visits) + ";")

sql.append("INSERT INTO queue_ticket (ticket_id, visit_id, department_id, status, ticket_number, created_at, updated_at, deleted, type, work_date, is_guest, customer_id) VALUES")
sql.append(",\n".join(hist_tickets) + ";")

sql.append("INSERT INTO medical_record (record_id, visit_id, queue_ticket_id, status, doctor_id, created_at, updated_at, deleted) VALUES")
sql.append(",\n".join(hist_records) + ";")

sql.append("INSERT INTO invoice (invoice_id, visit_id, invoice_code, subtotal, discount, tax, total_amount, status, paid_amount, created_at, updated_at, deleted) VALUES")
sql.append(",\n".join(hist_invoices) + ";")

# Today pending appointment for demo patient (index 0)
p0 = patients[0]
a0 = get_uuid("appt_0")
sql.append(f"INSERT INTO appointment (appointment_id, created_at, updated_at, deleted, scheduled_at, status, is_guest, customer_id, shift_name, shift_time, shift_version_id) VALUES ('{a0}', pg_temp.demo_now(), pg_temp.demo_now(), false, pg_temp.demo_date() + time '14:00', 'PENDING', false, '{p0['profile_id']}', 'Ca Chi?u', '13:00-17:00', '71000002-2222-2222-2222-222222222222');")

sql.append("INSERT INTO clinic_information (clinic_information_id, created_at, updated_at, deleted, clinic_name, legal_name, tax_code, operating_license, short_description, support_email, phone, address) VALUES ('00000000-0000-0000-0000-000000000100', pg_temp.demo_now(), pg_temp.demo_now(), false, 'Ph?ng khám CareS — mô ph?ng', 'CareS Demo', 'DEMO-NOT-LEGAL', 'DEMO', 'Mô ph?ng', 'test@test.com', '0123', 'Hà N?i');")

sql.append("COMMIT;")

with open(r"d:\gitlap\doAnSummer2026\src\main\resources\data.sql", "w", encoding="utf-8") as f:
    f.write("\\n".join(sql))

print("data.sql generated successfully")
