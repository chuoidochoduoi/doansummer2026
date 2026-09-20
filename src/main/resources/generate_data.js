const crypto = require('crypto');
const fs = require('fs');

function getUuid(seed) {
    const hash = crypto.createHash('md5').update(String(seed)).digest('hex');
    return `${hash.slice(0,8)}-${hash.slice(8,12)}-${hash.slice(12,16)}-${hash.slice(16,20)}-${hash.slice(20,32)}`;
}

const departments = [
    {id: getUuid('dept_noi1'), code: 'INT-101', name: 'Phòng khám Nội 1', type: 'EXAMINATION', spec: '00000001-1111-1111-1111-111111111111', desc: 'Nội khoa'},
    {id: getUuid('dept_noi2'), code: 'INT-102', name: 'Phòng khám Nội 2', type: 'EXAMINATION', spec: '00000001-1111-1111-1111-111111111111', desc: 'Nội khoa'},
    {id: getUuid('dept_ngoai1'), code: 'SUR-201', name: 'Phòng khám Ngoại 1', type: 'EXAMINATION', spec: '00000003-3333-3333-3333-333333333333', desc: 'Ngoại khoa'},
    {id: getUuid('dept_ngoai2'), code: 'SUR-202', name: 'Phòng khám Ngoại 2', type: 'EXAMINATION', spec: '00000003-3333-3333-3333-333333333333', desc: 'Ngoại khoa'},
    {id: getUuid('dept_nhi'), code: 'PED-301', name: 'Phòng khám Nhi', type: 'EXAMINATION', spec: '00000002-2222-2222-2222-222222222222', desc: 'Nhi khoa'},
    {id: getUuid('dept_dalieu'), code: 'DER-401', name: 'Phòng khám Da liễu', type: 'EXAMINATION', spec: '00000004-4444-4444-4444-444444444444', desc: 'Da liễu'},
    {id: getUuid('dept_san'), code: 'OBG-501', name: 'Phòng khám Sản phụ khoa', type: 'EXAMINATION', spec: '00000008-8888-8888-8888-888888888888', desc: 'Sản phụ khoa'},
    {id: getUuid('dept_xn'), code: 'LAB-601', name: 'Phòng xét nghiệm', type: 'PARACLINICAL', spec: 'NULL', desc: 'Xét nghiệm'},
    {id: getUuid('dept_sa'), code: 'IMG-701', name: 'Phòng siêu âm', type: 'PARACLINICAL', spec: 'NULL', desc: 'Siêu âm, điện tim'},
    {id: getUuid('dept_xq'), code: 'XR-702', name: 'Phòng X-quang', type: 'PARACLINICAL', spec: 'NULL', desc: 'X-quang'}
];

const capabilities = [
    {id: 'ca000001-0000-0000-0000-000000000001', code: 'HEMATOLOGY'},
    {id: 'ca000002-0000-0000-0000-000000000002', code: 'BIOCHEMISTRY'},
    {id: 'ca000003-0000-0000-0000-000000000003', code: 'ULTRASOUND'},
    {id: 'ca000004-0000-0000-0000-000000000004', code: 'XRAY'},
    {id: 'ca000005-0000-0000-0000-000000000005', code: 'URINALYSIS'},
    {id: 'ca000006-0000-0000-0000-000000000006', code: 'ECG'},
    {id: 'ca000007-0000-0000-0000-000000000007', code: 'MICROBIOLOGY'},
    {id: 'ca000008-0000-0000-0000-000000000008', code: 'RAPID_TEST'}
];

const deptCaps = {
    [getUuid('dept_xn')]: ['ca000001-0000-0000-0000-000000000001', 'ca000002-0000-0000-0000-000000000002', 'ca000005-0000-0000-0000-000000000005', 'ca000007-0000-0000-0000-000000000007', 'ca000008-0000-0000-0000-000000000008'],
    [getUuid('dept_sa')]: ['ca000003-0000-0000-0000-000000000003', 'ca000006-0000-0000-0000-000000000006'],
    [getUuid('dept_xq')]: ['ca000004-0000-0000-0000-000000000004']
};

const staff = [
    {role: 'ADMIN', user: 'admin', name: 'Phạm Đức Minh', dept: 'NULL', isDoc: false, degree: 'NULL', code: 'STF-0001', spec: 'NULL'},
    {role: 'CLINIC_MANAGER', user: 'clinicmanager', name: 'Nguyễn Thu Hương', dept: 'NULL', isDoc: false, degree: 'NULL', code: 'STF-0002', spec: 'NULL'},
    {role: 'RECEPTIONIST', user: 'receptionist', name: 'Trịnh Thị Kiều Oanh', dept: 'NULL', isDoc: false, degree: 'NULL', code: 'STF-0003', spec: 'NULL'},
    {role: 'CASHIER', user: 'cashier', name: 'Đinh Văn Quang', dept: 'NULL', isDoc: false, degree: 'NULL', code: 'STF-0004', spec: 'NULL'},

    {role: 'DOCTOR', user: 'doctor_noi1', name: 'BS. Trần Minh Tuấn', dept: getUuid('dept_noi1'), isDoc: true, degree: "'Bác sĩ CKI'", code: 'STF-D001', spec: "'00000001-1111-1111-1111-111111111111'"},
    {role: 'DOCTOR', user: 'doctor_noi2', name: 'BS. Nguyễn Thị Lan Anh', dept: getUuid('dept_noi2'), isDoc: true, degree: "'Bác sĩ CKI'", code: 'STF-D002', spec: "'00000001-1111-1111-1111-111111111111'"},
    {role: 'DOCTOR', user: 'doctor_ngoai1', name: 'BS. Nguyễn Đức Khoa', dept: getUuid('dept_ngoai1'), isDoc: true, degree: "'Bác sĩ CKII'", code: 'STF-D003', spec: "'00000003-3333-3333-3333-333333333333'"},
    {role: 'DOCTOR', user: 'doctor_ngoai2', name: 'BS. Lê Hoàng Nam', dept: getUuid('dept_ngoai2'), isDoc: true, degree: "'Bác sĩ CKII'", code: 'STF-D004', spec: "'00000003-3333-3333-3333-333333333333'"},
    {role: 'DOCTOR', user: 'doctor_nhi', name: 'BS. Phạm Quốc Bảo', dept: getUuid('dept_nhi'), isDoc: true, degree: "'Bác sĩ CKI'", code: 'STF-D005', spec: "'00000002-2222-2222-2222-222222222222'"},
    {role: 'DOCTOR', user: 'doctor_dalieu', name: 'BS. Hoàng Thị Bích Ngọc', dept: getUuid('dept_dalieu'), isDoc: true, degree: "'Bác sĩ CKI'", code: 'STF-D006', spec: "'00000004-4444-4444-4444-444444444444'"},
    {role: 'DOCTOR', user: 'doctor_san', name: 'BS. Đỗ Thị Phương Thảo', dept: getUuid('dept_san'), isDoc: true, degree: "'Bác sĩ CKI'", code: 'STF-D007', spec: "'00000008-8888-8888-8888-888888888888'"},
    {role: 'DOCTOR', user: 'doctor_xn', name: 'BS. Bùi Văn Thành', dept: getUuid('dept_xn'), isDoc: true, degree: "'Bác sĩ CKI'", code: 'STF-D008', spec: 'NULL'},
    {role: 'DOCTOR', user: 'doctor_sa', name: 'BS. Võ Đình Phúc', dept: getUuid('dept_sa'), isDoc: true, degree: "'Bác sĩ CKI'", code: 'STF-D009', spec: 'NULL'},
    {role: 'DOCTOR', user: 'doctor_xq', name: 'BS. Đoàn Minh Quân', dept: getUuid('dept_xq'), isDoc: true, degree: "'Bác sĩ CKI'", code: 'STF-D010', spec: 'NULL'},

    {role: 'NURSE', user: 'nurse_noi1', name: 'ĐD. Lê Thị Hồng Nhung', dept: getUuid('dept_noi1'), isDoc: false, degree: "'Cử nhân'", code: 'STF-N001', spec: 'NULL'},
    {role: 'NURSE', user: 'nurse_noi2', name: 'ĐD. Phạm Văn Đức', dept: getUuid('dept_noi2'), isDoc: false, degree: "'Cử nhân'", code: 'STF-N002', spec: 'NULL'},
    {role: 'NURSE', user: 'nurse_ngoai1', name: 'ĐD. Vũ Thị Mai Linh', dept: getUuid('dept_ngoai1'), isDoc: false, degree: "'Cử nhân'", code: 'STF-N003', spec: 'NULL'},
    {role: 'NURSE', user: 'nurse_ngoai2', name: 'ĐD. Đặng Thị Thùy Dung', dept: getUuid('dept_ngoai2'), isDoc: false, degree: "'Cử nhân'", code: 'STF-N004', spec: 'NULL'},
    {role: 'NURSE', user: 'nurse_nhi', name: 'ĐD. Nguyễn Thị Thanh Hà', dept: getUuid('dept_nhi'), isDoc: false, degree: "'Cử nhân'", code: 'STF-N005', spec: 'NULL'},
    {role: 'NURSE', user: 'nurse_dalieu', name: 'ĐD. Trần Văn Hùng', dept: getUuid('dept_dalieu'), isDoc: false, degree: "'Cử nhân'", code: 'STF-N006', spec: 'NULL'},
    {role: 'NURSE', user: 'nurse_san', name: 'ĐD. Lê Thị Ngọc Ánh', dept: getUuid('dept_san'), isDoc: false, degree: "'Cử nhân'", code: 'STF-N007', spec: 'NULL'},
    {role: 'NURSE', user: 'ktv_xn', name: 'KTV. Ngô Thị Hải Yến', dept: getUuid('dept_xn'), isDoc: false, degree: "'Cử nhân'", code: 'STF-N008', spec: 'NULL'},
    {role: 'NURSE', user: 'ktv_sa', name: 'KTV. Phan Thị Mỹ Duyên', dept: getUuid('dept_sa'), isDoc: false, degree: "'Cử nhân'", code: 'STF-N009', spec: 'NULL'},
    {role: 'NURSE', user: 'ktv_xq', name: 'KTV. Hoàng Thị Kim Chi', dept: getUuid('dept_xq'), isDoc: false, degree: "'Cử nhân'", code: 'STF-N010', spec: 'NULL'},
];

staff.forEach((s, idx) => {
    s.accountId = getUuid('acc_' + s.user);
    s.profileId = getUuid('prof_' + s.user);
    s.staffId = getUuid('staff_' + s.user);
});

const patients = [];
for (let i = 1; i <= 26; i++) {
    const isGuest = i > 21;
    const isDemo = i === 1;
    const hasAccount = !isGuest;
    const user = `patient${i}`;
    patients.push({
        accountId: hasAccount ? getUuid('acc_' + user) : 'NULL',
        profileId: getUuid('prof_' + user),
        name: `Bệnh Nhân ${i}`,
        phone: `0987654${i.toString().padStart(3, '0')}`,
        isGuest,
        isDemo
    });
}

const sql = [];
sql.push(`\\set ON_ERROR_STOP on
SET cares.demo_reset = 'yes';

BEGIN;
SET LOCAL TIME ZONE 'Asia/Ho_Chi_Minh';

CREATE TEMP TABLE demo2_clock ON COMMIT DROP AS
SELECT CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh' moment;
CREATE OR REPLACE FUNCTION pg_temp.demo_now() RETURNS timestamp LANGUAGE sql STABLE AS 'SELECT moment FROM demo2_clock';
CREATE OR REPLACE FUNCTION pg_temp.demo_date() RETURNS date LANGUAGE sql STABLE AS 'SELECT moment::date FROM demo2_clock';
CREATE OR REPLACE FUNCTION pg_temp.did(key text) RETURNS uuid LANGUAGE sql IMMUTABLE AS 'SELECT md5(''CareS-demo:''||key)::uuid';
CREATE OR REPLACE FUNCTION pg_temp.event(c record, rel_offset float) RETURNS timestamp LANGUAGE plpgsql STABLE AS $$
BEGIN
    RETURN c.started + (c.completed_at - c.started) * rel_offset;
END $$;

ALTER TABLE membership_card ADD COLUMN IF NOT EXISTS benefit_starts_at timestamp;

TRUNCATE TABLE
    clinic_information, membership_card_ledger, membership_card, membership_policy, family_member,
    clinic_schedule_exception, shift_version,
    audit_log,
    chat_messages, chat_sessions, icd_10_selections,
    insurance_rule, invoice_item, notification, public_announcement, payment_transaction,
    prescription_item, staff_schedule, staff_schedule_template,
    test_result, test_request, vital_signs, medical_record, queue_ticket,
    invoice, customer_visit, appointment_services, appointment,
    department_capability, staff_capability, staff_info, medical_service,
    service_category, service_capability, medicine_catalog, icd_10_codes,
    insurance, department, specialization, shift_config, profile, account
RESTART IDENTITY CASCADE;

INSERT INTO clinic_information (clinic_information_id, created_at, updated_at, deleted, clinic_name, legal_name, tax_code, operating_license, short_description, support_email, phone, address) VALUES ('00000000-0000-0000-0000-000000000100', pg_temp.demo_now(), pg_temp.demo_now(), false, 'Phòng khám CareS — mô phỏng', 'CareS Demo', 'DEMO-NOT-LEGAL', 'DEMO', 'Mô phỏng', 'test@test.com', '0123', 'Hà Nội');

INSERT INTO membership_policy (policy_id, created_at, updated_at, deleted, minimum_top_up, discount_percent, validity_months, active)
VALUES ('7c000001-0000-0000-0000-000000000001', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 1000000, 15, 12, true);

INSERT INTO specialization (specialization_id, created_at, updated_at, deleted, active, name, description) VALUES
('00000001-1111-1111-1111-111111111111', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, true, 'Nội khoa', 'Khám nội khoa'),
('00000002-2222-2222-2222-222222222222', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, true, 'Nhi khoa', 'Khám nhi khoa'),
('00000003-3333-3333-3333-333333333333', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, true, 'Ngoại khoa', 'Khám ngoại khoa'),
('00000004-4444-4444-4444-444444444444', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, true, 'Da liễu', 'Khám da liễu'),
('00000008-8888-8888-8888-888888888888', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, true, 'Sản phụ khoa', 'Sản phụ khoa');
`);

sql.push('-- Departments');
sql.push('INSERT INTO department (department_id, created_at, updated_at, deleted, room_code, name, status, department_type, specialization_id, description, head_doctor_id) VALUES');
const deptVals = departments.map(d => {
    const spec = d.spec === 'NULL' ? 'NULL' : `'${d.spec}'`;
    let headDoc = 'NULL';
    const doc = staff.find(s => s.dept === d.id && s.isDoc);
    if (doc) headDoc = `'${doc.staffId}'`;
    return `('${d.id}', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '${d.code}', '${d.name}', 'AVAILABLE', '${d.type}', ${spec}, '${d.desc}', ${headDoc})`;
});
sql.push(deptVals.join(',\\n') + ';');

sql.push('-- Accounts');
sql.push('INSERT INTO account (account_id, created_at, is_active, password_hash, role, username) VALUES');
const accVals = [];
staff.forEach(s => {
    accVals.push(`('${s.accountId}', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', '${s.user}')`);
});
patients.forEach(p => {
    if (p.accountId !== 'NULL') {
        accVals.push(`('${p.accountId}', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'CUSTOMER', '${p.phone}')`);
    }
});
sql.push(accVals.join(',\\n') + ';');

sql.push('-- Profiles');
sql.push('INSERT INTO profile (profile_id, account_id, created_at, updated_at, deleted, full_name, date_of_birth, gender, phone, email, address, blood_type) VALUES');
const profVals = [];
staff.forEach((s, idx) => {
    profVals.push(`('${s.profileId}', '${s.accountId}', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '${s.name}', '1980-01-01', 'MALE', '098000${idx.toString().padStart(4,'0')}', '${s.user}@cares.vn', 'Hà Nội', NULL)`);
});
patients.forEach(p => {
    const acc = p.accountId === 'NULL' ? 'NULL' : `'${p.accountId}'`;
    profVals.push(`('${p.profileId}', ${acc}, (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '${p.name}', '1990-01-01', 'FEMALE', '${p.phone}', 'patient${p.phone}@example.com', 'Hà Nội', NULL)`);
});
sql.push(profVals.join(',\\n') + ';');

sql.push('-- StaffInfo');
sql.push('INSERT INTO staff_info (staff_id, created_at, updated_at, deleted, profile_id, staff_code, system_role, national_id, highest_degree, university, license_number, specialization_id, department_id) VALUES');
const staffVals = [];
staff.forEach((s, idx) => {
    const dept = s.dept === 'NULL' ? 'NULL' : `'${s.dept}'`;
    staffVals.push(`('${s.staffId}', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '${s.profileId}', '${s.code}', '${s.role}', '001000${idx.toString().padStart(6,'0')}', ${s.degree}, NULL, NULL, ${s.spec}, ${dept})`);
});
sql.push(staffVals.join(',\\n') + ';');

sql.push(`
-- Capabilities
INSERT INTO service_capability (capability_id, created_at, updated_at, deleted, code, name, description, active) VALUES
('ca000001-0000-0000-0000-000000000001', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'HEMATOLOGY', 'Xét nghiệm huyết học', 'Công thức máu', true),
('ca000002-0000-0000-0000-000000000002', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'BIOCHEMISTRY', 'Xét nghiệm sinh hóa', 'Sinh hóa máu', true),
('ca000003-0000-0000-0000-000000000003', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'ULTRASOUND', 'Siêu âm', 'Siêu âm', true),
('ca000004-0000-0000-0000-000000000004', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'XRAY', 'X-quang', 'X-quang', true),
('ca000005-0000-0000-0000-000000000005', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'URINALYSIS', 'Xét nghiệm nước tiểu', 'Nước tiểu', true),
('ca000006-0000-0000-0000-000000000006', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'ECG', 'Điện tim', 'Điện tim', true),
('ca000007-0000-0000-0000-000000000007', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'MICROBIOLOGY', 'Xét nghiệm vi sinh', 'Vi sinh', true),
('ca000008-0000-0000-0000-000000000008', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'RAPID_TEST', 'Test nhanh', 'Test nhanh', true);
`);

sql.push('-- Department Capabilities');
sql.push('INSERT INTO department_capability (department_id, capability_id) VALUES');
const deptCapVals = [];
Object.entries(deptCaps).forEach(([d, caps]) => {
    caps.forEach(c => deptCapVals.push(`('${d}', '${c}')`));
});
sql.push(deptCapVals.join(',\\n') + ';');

sql.push(`-- Medical Services (Keeping the core list)
INSERT INTO medical_service (
    service_id, service_code, created_at, updated_at, deleted, description,
    status, is_point_of_care, name, price, department_type, duration_minutes,
    workflow_priority, requires_doctor_order, requires_return_to_doctor,
    requires_specimen, result_wait_minutes, allow_customer_booking,
    minimum_age, maximum_age, allowed_gender, department_id,
    required_specialization_id, required_capability_id
) VALUES
('40000001-0000-0000-0000-000000000001', 'EX-SU-001', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Khám ban đầu các bệnh lý.', 'ACTIVE', false, 'Khám Ngoại tổng quát', 220000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 0, 120, NULL, NULL, '00000003-3333-3333-3333-333333333333', NULL),
('40000002-0000-0000-0000-000000000002', 'EX-IN-001', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Khám và đánh giá tổng quát.', 'ACTIVE', false, 'Khám Nội tổng quát', 220000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 16, 120, NULL, NULL, '00000001-1111-1111-1111-111111111111', NULL),
('40000003-0000-0000-0000-000000000003', 'EX-PE-001', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Khám Nhi.', 'ACTIVE', false, 'Khám Nhi', 220000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 0, 15, NULL, NULL, '00000002-2222-2222-2222-222222222222', NULL),
('40000008-0000-0000-0000-000000000008', 'LAB-001', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Đánh giá các thành phần tế bào máu.', 'ACTIVE', false, 'Công thức máu', 120000, 'PARACLINICAL', 15, 1, true, true, true, 45, false, 0, 120, NULL, NULL, NULL, 'ca000001-0000-0000-0000-000000000001'),
('40000009-0000-0000-0000-000000000009', 'LAB-002', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Đo nồng độ glucose trong máu.', 'ACTIVE', false, 'Đường huyết', 70000, 'PARACLINICAL', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('40000015-0000-0000-0000-000000000015', 'IMG-003', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Khảo sát các cơ quan trong ổ bụng.', 'ACTIVE', false, 'Siêu âm ổ bụng tổng quát', 250000, 'PARACLINICAL', 20, 1, true, true, false, 10, false, 0, 120, NULL, NULL, NULL, 'ca000003-0000-0000-0000-000000000003'),
('40000017-0000-0000-0000-000000000017', 'IMG-001', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chụp hình vùng ngực.', 'ACTIVE', false, 'X-quang ngực', 180000, 'PARACLINICAL', 15, 1, true, true, false, 15, false, 6, 120, NULL, NULL, NULL, 'ca000004-0000-0000-0000-000000000004');
`);

sql.push(`
-- Medicine & ICD 10
INSERT INTO medicine_catalog (medicine_id, medicine_code, name, active_ingredient, default_unit, default_usage, default_frequency_per_day, active, deleted) VALUES
('b0000000-0000-0000-0000-000000000001', 'MED-001', 'Paracetamol 500mg', 'Paracetamol', 'Viên', 'Uống sau ăn', 2, true, false);

INSERT INTO icd_10_codes (code, name, description, category, deleted) VALUES
('J00', 'Viêm mũi họng cấp', 'Cảm lạnh thông thường', 'Bệnh hệ hô hấp', false),
('I10', 'Tăng huyết áp vô căn', 'Tăng huyết áp nguyên phát', 'Bệnh hệ tuần hoàn', false),
('Z00.0', 'Khám sức khỏe tổng quát', 'Khám định kỳ', 'Yếu tố sức khỏe', false);
`);

sql.push(`
-- Shift Config
INSERT INTO shift_config (shift_id, created_at, updated_at, deleted, name, start_time, end_time, is_active) VALUES
('70000001-1111-1111-1111-111111111111', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Ca Sáng', '07:30', '11:30', true),
('70000002-2222-2222-2222-222222222222', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Ca Chiều', '13:00', '17:00', true),
('70000003-3333-3333-3333-333333333333', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Ca Tối', '17:30', '21:30', true);

INSERT INTO shift_version (shift_version_id, shift_id, start_time, end_time, effective_from, effective_to, change_reason, created_at, updated_at, deleted) VALUES
('71000001-1111-1111-1111-111111111111', '70000001-1111-1111-1111-111111111111', '07:30', '11:30', pg_temp.demo_date() - 365, NULL, 'Khởi tạo', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false),
('71000002-2222-2222-2222-222222222222', '70000002-2222-2222-2222-222222222222', '13:00', '17:00', pg_temp.demo_date() - 365, NULL, 'Khởi tạo', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false),
('71000003-3333-3333-3333-333333333333', '70000003-3333-3333-3333-333333333333', '17:30', '21:30', pg_temp.demo_date() - 365, NULL, 'Khởi tạo', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false);
`);

sql.push('-- Staff Schedules (Full tuần 3 ca)');
sql.push(`
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
CROSS JOIN staff_info s
WHERE s.system_role IN ('DOCTOR', 'NURSE');
`);

sql.push('-- History Visits (25 patients)');
const histAppts = [];
const histVisits = [];
const histTickets = [];
const histRecords = [];
const histInvoices = [];
for (let i = 1; i <= 25; i++) {
    const p = patients[i];
    const apptId = getUuid('appt_' + i);
    const visitId = getUuid('visit_' + i);
    const ticketId = getUuid('ticket_' + i);
    const recordId = getUuid('record_' + i);
    const invId = getUuid('inv_' + i);
    const isGuest = p.isGuest ? 'true' : 'false';
    const docId = staff.find(s => s.user === 'doctor_noi1').staffId;
    const deptId = departments[0].id; // Nội 1

    histAppts.push(`('${apptId}', pg_temp.demo_now()-interval '${30-i} days', pg_temp.demo_now()-interval '${30-i} days', false, pg_temp.demo_date()-interval '${30-i} days' + time '08:00', 'COMPLETED', ${isGuest}, '${p.profileId}', 'Ca Sáng', '07:30-11:30', '71000001-1111-1111-1111-111111111111')`);
    histVisits.push(`('${visitId}', '${p.profileId}', ${isGuest}, 'VIS-${i.toString().padStart(4,'0')}', 'COMPLETED', pg_temp.demo_date()-interval '${30-i} days' + time '08:00', pg_temp.demo_date()-interval '${30-i} days' + time '09:00', pg_temp.demo_now(), pg_temp.demo_now(), false, 'walkin')`);
    histTickets.push(`('${ticketId}', '${visitId}', '${deptId}', 'DONE', 1, pg_temp.demo_now()-interval '${30-i} days', pg_temp.demo_now()-interval '${30-i} days', false, 'EXAMINATION', pg_temp.demo_date()-interval '${30-i} days', ${isGuest}, '${p.profileId}')`);
    histRecords.push(`('${recordId}', '${visitId}', '${ticketId}', 'COMPLETED', '${docId}', pg_temp.demo_now()-interval '${30-i} days', pg_temp.demo_now()-interval '${30-i} days', false)`);
    histInvoices.push(`('${invId}', '${visitId}', 'INV-${i.toString().padStart(4,'0')}', 220000, 0, 0, 220000, 'PAID', 220000, pg_temp.demo_now()-interval '${30-i} days', pg_temp.demo_now()-interval '${30-i} days', false)`);
}

sql.push('INSERT INTO appointment (appointment_id, created_at, updated_at, deleted, scheduled_at, status, is_guest, customer_id, shift_name, shift_time, shift_version_id) VALUES');
sql.push(histAppts.join(',\\n') + ';');
sql.push('INSERT INTO customer_visit (visit_id, customer_id, is_guest, visit_code, status, check_in_time, check_out_time, created_at, updated_at, deleted, source) VALUES');
sql.push(histVisits.join(',\\n') + ';');
sql.push('INSERT INTO queue_ticket (ticket_id, visit_id, department_id, status, ticket_number, created_at, updated_at, deleted, type, work_date, is_guest, customer_id) VALUES');
sql.push(histTickets.join(',\\n') + ';');
sql.push('INSERT INTO medical_record (record_id, visit_id, queue_ticket_id, status, doctor_id, created_at, updated_at, deleted) VALUES');
sql.push(histRecords.join(',\\n') + ';');
sql.push('INSERT INTO invoice (invoice_id, visit_id, invoice_code, subtotal, discount, tax, total_amount, status, paid_amount, created_at, updated_at, deleted) VALUES');
sql.push(histInvoices.join(',\\n') + ';');

// Today pending appointment for demo patient (index 0)
const p0 = patients[0];
const a0 = getUuid('appt_0');
sql.push(`INSERT INTO appointment (appointment_id, created_at, updated_at, deleted, scheduled_at, status, is_guest, customer_id, shift_name, shift_time, shift_version_id) VALUES ('${a0}', pg_temp.demo_now(), pg_temp.demo_now(), false, pg_temp.demo_date() + time '14:00', 'PENDING', false, '${p0.profileId}', 'Ca Chiều', '13:00-17:00', '71000002-2222-2222-2222-222222222222');`);

sql.push('COMMIT;');

fs.writeFileSync('d:/gitlap/doAnSummer2026/src/main/resources/data.sql', sql.join('\\n'), 'utf8');
console.log('done!');
