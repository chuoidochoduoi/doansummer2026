-- ===================================================================
-- data-minimal.sql
-- Du phong: chi du lieu toi thieu de web chay duoc (admin + phong kham)
-- Su dung thay the data.sql khi can khoi tao he thong trong.
--
-- Tai khoan seed:
--   admin         / 88888888  (BCrypt)
--   clinicmanager / 88888888  (BCrypt)
-- ===================================================================

TRUNCATE TABLE
    medical_service, department, specialization,
    staff_info, staff_capability, staff_attendance, staff_schedule,
    profile, account
RESTART IDENTITY CASCADE;

-- ===================================================================
-- Specialization (danh muc chuyen khoa - toi thieu de bo sung vao department)
-- ===================================================================
INSERT INTO specialization (specialization_id, created_at, updated_at, deleted, name, description) VALUES
('00000001-1111-1111-1111-111111111111', NOW(), NOW(), false, 'Nội khoa', 'Chẩn đoán và điều trị bệnh nội'),
('00000002-2222-2222-2222-222222222222', NOW(), NOW(), false, 'Nhi khoa', 'Khám và điều trị cho trẻ em'),
('00000003-3333-3333-3333-333333333333', NOW(), NOW(), false, 'Chẩn đoán hình ảnh', 'Siêu âm, X-quang, CT, MRI'),
('00000006-6666-6666-6666-666666666666', NOW(), NOW(), false, 'Khám tổng quát', 'Khám ban đầu và điều phối đa khoa');

-- ===================================================================
-- Department (phòng khám - toi thieu de he thong hoat dong)
-- ===================================================================
INSERT INTO department (department_id, created_at, updated_at, deleted, room_code, name, status, department_type, specialization_id, description, head_doctor_id) VALUES
('33333333-3333-3333-3333-333333333333', NOW(), NOW(), false, 'EX-101', 'Phòng Khám Bệnh', 'AVAILABLE', 'EXAMINATION', '00000006-6666-6666-6666-666666666666', 'Khám bệnh tổng quát', NULL),
('44444444-4444-4444-4444-444444444444', NOW(), NOW(), false, 'LAB-201', 'Phòng Xét nghiệm', 'AVAILABLE', 'PARACLINICAL', NULL, 'Thực hiện các xét nghiệm mẫu bệnh phẩm', NULL),
('55555555-5555-5555-5555-555555555555', NOW(), NOW(), false, 'IMG-301', 'Phòng Chẩn đoán hình ảnh', 'AVAILABLE', 'PARACLINICAL', '00000003-3333-3333-3333-333333333333', 'Siêu âm, X-quang, CT, MRI', NULL),
('66666666-6666-6666-6666-666666666666', NOW(), NOW(), false, 'PED-102', 'Phòng Nhi', 'AVAILABLE', 'EXAMINATION', '00000002-2222-2222-2222-222222222222', 'Khám chăm sóc cho trẩ em', NULL);

-- ===================================================================
-- Account (tai khoan dang nhap) - 2 tai khoan chuẩn: admin + clinic manager
-- ===================================================================
INSERT INTO account (account_id, created_at, is_active, password_hash, role, username) VALUES
('30000012-2222-2222-2222-222222222222', NOW(), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'admin'),
('30000013-3333-3333-3333-333333333333', NOW(), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'clinicmanager');

-- ===================================================================
-- Profile (thong tin ca nhan lien ket voi account)
-- ===================================================================
INSERT INTO profile (profile_id, created_at, updated_at, deleted, full_name, date_of_birth, gender, phone, email, address, blood_type) VALUES
('20000009-9999-9999-9999-999999999999', NOW(), NOW(), false, 'Quản trị viên hệ thống', '1985-01-01', 'MALE', '0999999999', 'admin@example.com', 'Hà Nội', NULL),
('20000010-0000-0000-0000-000000000001', NOW(), NOW(), false, 'Quản lý phòng khám', '1988-01-01', 'FEMALE', '0888888888', 'clinicmanager@example.com', 'TP.HCM', NULL);

-- ===================================================================
-- ServiceCapability (nang luc thuc hien dich vu - dung de gan voi medical_service/lab)
-- ===================================================================
INSERT INTO service_capability (capability_id, created_at, updated_at, deleted, code, name, description, active) VALUES
('ca000001-0000-0000-0000-000000000001', NOW(), NOW(), false, 'HEMATOLOGY', 'Xét nghiệm huyết học', 'Công thức máu và các xét nghiệm liên quan', true),
('ca000003-0000-0000-0000-000000000003', NOW(), NOW(), false, 'ULTRASOUND', 'Siêu âm', 'Các dịch vụ siêu âm hình ảnh', true),
('ca000004-0000-0000-0000-000000000004', NOW(), NOW(), false, 'XRAY', 'X-quang', 'Các dịch vụ X-quang', true);

-- Link department -> capability
INSERT INTO department_capability (department_id, capability_id) VALUES
('44444444-4444-4444-4444-444444444444', 'ca000001-0000-0000-0000-000000000001'),
('55555555-5555-5555-5555-555555555555', 'ca000003-0000-0000-0000-000000000003'),
('55555555-5555-5555-5555-555555555555', 'ca000004-0000-0000-0000-000000000004');

-- ===================================================================
-- MedicalService (dich vu y te - toi thieu: kham + xn + CDHA)
-- ===================================================================
INSERT INTO medical_service (service_id, service_code, created_at, updated_at, deleted, description, status, is_point_of_care, name, price, department_type, duration_minutes, allow_customer_booking, minimum_age, maximum_age, allowed_gender, department_id, required_specialization_id, required_capability_id) VALUES
('40000008-8888-8888-8888-888888888888', 'KHB001', NOW(), NOW(), false, 'Khám bệnh tổng quát', 'ACTIVE', false, 'Khám bệnh tổng quát', 200000, 'EXAMINATION', 30, true, 0, 120, NULL, '33333333-3333-3333-3333-333333333333', '00000006-6666-6666-6666-666666666666', NULL),
('40000001-1111-1111-1111-111111111111', 'XN001', NOW(), NOW(), false, 'Xét nghiệm công thức máu (CBC)', 'ACTIVE', false, 'Xét nghiệm công thức máu', 120000, 'PARACLINICAL', 20, true, 0, 120, NULL, '44444444-4444-4444-4444-444444444444', NULL, 'ca000001-0000-0000-0000-000000000001'),
('40000004-4444-4444-4444-444444444444', 'CDHA001', NOW(), NOW(), false, 'Siêu âm ổ bụng', 'ACTIVE', false, 'Siêu âm ổ bụng', 250000, 'PARACLINICAL', 20, true, 0, 120, NULL, '55555555-5555-5555-5555-555555555555', '00000003-3333-3333-3333-333333333333', 'ca000003-0000-0000-0000-000000000003');

-- Link profile -> account
UPDATE profile SET account_id = '30000012-2222-2222-2222-222222222222' WHERE profile_id = '20000009-9999-9999-9999-999999999999';
UPDATE profile SET account_id = '30000013-3333-3333-3333-333333333333' WHERE profile_id = '20000010-0000-0000-0000-000000000001';

-- ===================================================================
-- StaffInfo (thong tin nhan vien) - admin + clinic manager
-- ===================================================================
INSERT INTO staff_info (staff_id, created_at, updated_at, deleted, profile_id, staff_code, system_role, national_id, bank_account, highest_degree, university, license_number, specialization_id, department_id) VALUES
('90000008-1111-1111-1111-111111111111', NOW(), NOW(), false, '20000009-9999-9999-9999-999999999999', 'STF-ADM-001', 'ADMIN', '9123456789', NULL, NULL, NULL, NULL, NULL, NULL),
('90000009-2222-2222-2222-222222222222', NOW(), NOW(), false, '20000010-0000-0000-0000-000000000001', 'STF-CLM-001', 'CLINIC_MANAGER', '9223456789', NULL, NULL, NULL, NULL, NULL, '33333333-3333-3333-3333-333333333333');
