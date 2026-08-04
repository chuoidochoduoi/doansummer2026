-- Dữ liệu mẫu cho hệ thống quản lý khám bệnh
-- Phù hợp với model entity (UUID chuẩn, các trường required, enums đúng)

-- Xóa dữ liệu cũ
TRUNCATE TABLE
    test_result, test_request, vital_signs, icd_10_selections, prescription_item, medical_record,
    payment_transaction, invoice_item, invoice, queue_ticket, customer_visit,
    appointment_services, appointment, staff_schedule, staff_schedule_template,
    notification, audit_log, staff_info, profile, account,
    medical_service, service_category, department, specialization,
    insurance_rule, insurance, mock_bhyt_card, icd_10_codes
RESTART IDENTITY CASCADE;

-- ============================
-- Department (3 bản ghi lõi; phòng khám Nhi được bổ sung sau danh mục chuyên khoa)
-- ============================
INSERT INTO department (department_id, created_at, updated_at, deleted, room_code, name, status, department_type, description) VALUES
('33333333-3333-3333-3333-333333333333', NOW(), NOW(), false, 'EX-101', 'Khoa Khám Bệnh', 'AVAILABLE', 'EXAMINATION', 'Khám bệnh tổng quát'),
('44444444-4444-4444-4444-444444444444', NOW(), NOW(), false, 'LAB-201', 'Phòng Xét nghiệm 1', 'AVAILABLE', 'PARACLINICAL', 'Thực hiện các xét nghiệm mẫu bệnh phẩm'),
('55555555-5555-5555-5555-555555555555', NOW(), NOW(), false, 'IMG-301', 'Phòng Chẩn đoán hình ảnh 1', 'AVAILABLE', 'PARACLINICAL', 'Siêu âm, X-quang, CT, MRI');

-- ============================
-- Specialization - danh mục do Admin quản lý, dùng chung cho phòng/dịch vụ/nhân viên
-- ============================
INSERT INTO specialization (specialization_id, created_at, updated_at, deleted, name, description) VALUES
('00000001-1111-1111-1111-111111111111', NOW(), NOW(), false, 'Bác sĩ Nội tổng quát', 'Chẩn đoán và điều trị bệnh nội'),
('00000002-2222-2222-2222-222222222222', NOW(), NOW(), false, 'Bác sĩ Chẩn đoán hình ảnh', 'Siêu âm, X-quang, CT, MRI'),
('00000003-3333-3333-3333-333333333333', NOW(), NOW(), false, 'Bác sĩ Tim mạch', 'Chẩn đoán và điều trị bệnh tim mạch'),
('00000004-4444-4444-4444-444444444444', NOW(), NOW(), false, 'Bác sĩ Hô hấp', 'Chẩn đoán và điều trị bệnh hô hấp'),
('00000005-5555-5555-5555-555555555555', NOW(), NOW(), false, 'Bác sĩ Nội soi', 'Thăm khám nội soi');

INSERT INTO specialization (specialization_id, created_at, updated_at, deleted, name, description) VALUES
('00000006-6666-6666-6666-666666666666', NOW(), NOW(), false, 'Khám tổng quát', 'Khám ban đầu và điều phối đa khoa'),
('00000007-7777-7777-7777-777777777777', NOW(), NOW(), false, 'Nhi khoa', 'Khám và điều trị cho trẻ em'),
('00000008-8888-8888-8888-888888888888', NOW(), NOW(), false, 'Tai Mũi Họng', 'Khám tai, mũi và họng'),
('00000009-9999-9999-9999-999999999999', NOW(), NOW(), false, 'Nhãn khoa', 'Khám và điều trị các bệnh về mắt'),
('00000010-aaaa-aaaa-aaaa-aaaaaaaaaaaa', NOW(), NOW(), false, 'Da liễu', 'Khám và điều trị các bệnh da liễu'),
('00000011-bbbb-bbbb-bbbb-bbbbbbbbbbbb', NOW(), NOW(), false, 'Răng Hàm Mặt', 'Khám và điều trị răng hàm mặt'),
('00000012-cccc-cccc-cccc-cccccccccccc', NOW(), NOW(), false, 'Sản phụ khoa', 'Khám và chăm sóc sức khỏe nữ giới');

UPDATE specialization SET name = 'Nội khoa' WHERE specialization_id = '00000001-1111-1111-1111-111111111111';
UPDATE specialization SET name = 'Chẩn đoán hình ảnh' WHERE specialization_id = '00000002-2222-2222-2222-222222222222';
UPDATE specialization SET name = 'Tim mạch' WHERE specialization_id = '00000003-3333-3333-3333-333333333333';
UPDATE specialization SET name = 'Hô hấp' WHERE specialization_id = '00000004-4444-4444-4444-444444444444';
UPDATE specialization SET name = 'Nội soi' WHERE specialization_id = '00000005-5555-5555-5555-555555555555';

-- Phòng vật lý và chuyên khoa phục vụ là hai khái niệm độc lập.
UPDATE department
SET specialization_id = '00000001-1111-1111-1111-111111111111', name = 'Phòng Nội 1', description = 'Phòng khám Nội dành cho người lớn'
WHERE department_id = '33333333-3333-3333-3333-333333333333';

INSERT INTO department (department_id, created_at, updated_at, deleted, room_code, name, status, department_type, specialization_id, description) VALUES
('66666666-6666-6666-6666-666666666666', NOW(), NOW(), false, 'PED-102', 'Phòng Nhi 1', 'AVAILABLE', 'EXAMINATION', '00000007-7777-7777-7777-777777777777', 'Phòng khám dành cho trẻ em');

-- ============================
-- Profile - Bệnh nhân (3 bản ghi)
-- ============================
INSERT INTO profile (profile_id, created_at, updated_at, deleted, full_name, date_of_birth, gender, phone, email, address, blood_type) VALUES
('10000001-1111-1111-1111-111111111111', NOW(), NOW(), false, 'Nguyễn Thị Bệnh Nhân 1', '1990-05-15', 'FEMALE', '0912345678', 'patient1@example.com', 'Hà Nội', 'O_POSITIVE'),
('10000002-2222-2222-2222-222222222222', NOW(), NOW(), false, 'Trần Văn Bệnh Nhân 2', '1985-10-20', 'MALE', '0912345679', 'patient2@example.com', 'TP.HCM', 'A_POSITIVE'),
('10000003-3333-3333-3333-333333333333', NOW(), NOW(), false, 'Lê Thị Bệnh Nhân 3', '1995-03-25', 'FEMALE', '0912345680', 'patient3@example.com', 'Đà Nẵng', 'B_POSITIVE');

-- ============================
-- Profile - Nhân viên (7 bản ghi)
-- ============================
INSERT INTO profile (profile_id, created_at, updated_at, deleted, full_name, date_of_birth, gender, phone, email, address, blood_type) VALUES
('20000001-1111-1111-1111-111111111111', NOW(), NOW(), false, 'Trần Thị Lễ Tân', '1995-01-10', 'FEMALE', '0987654321', 'reception@example.com', 'Hà Nội', NULL),
('20000002-2222-2222-2222-222222222222', NOW(), NOW(), false, 'Phạm Văn Thu Ngân', '1992-03-15', 'MALE', '0987654322', 'cashier@example.com', 'TP.HCM', NULL),
('20000003-3333-3333-3333-333333333333', NOW(), NOW(), false, 'Bác sĩ Nguyễn Khám Bệnh', '1985-06-20', 'MALE', '0987654323', 'doctor@example.com', 'Hà Nội', NULL),
('20000004-4444-4444-4444-444444444444', NOW(), NOW(), false, 'Y tá Trần Chăm Sóc', '1990-09-25', 'FEMALE', '0987654324', 'nurse@example.com', 'Đà Nẵng', NULL),
('20000005-5555-5555-5555-555555555555', NOW(), NOW(), false, 'Bác sĩ Lê Xét Nghiệm', '1980-05-15', 'MALE', '0987654325', 'labdoctor@example.com', 'Hà Nội', NULL),
('20000006-6666-6666-6666-666666666666', NOW(), NOW(), false, 'Bác sĩ Phạm Hình Ảnh', '1982-08-20', 'FEMALE', '0987654326', 'imagingdoctor@example.com', 'TP.HCM', NULL),
('20000007-7777-7777-7777-777777777777', NOW(), NOW(), false, 'Bác sĩ Tim Mạch', '1978-03-10', 'MALE', '0987654327', 'cardiologydoctor@example.com', 'Hà Nội', NULL),
('20000008-8888-8888-8888-888888888888', NOW(), NOW(), false, 'Khách vãng lai', '1996-01-01', 'MALE', '0909090909', 'guest@example.com', 'Hà Nội', NULL);

-- ============================
-- Account (10 bản ghi) - 3 bệnh nhân + 7 nhân viên
-- ============================
INSERT INTO account (account_id, created_at, is_active, password_hash, role, username) VALUES
('30000001-1111-1111-1111-111111111111', NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CUSTOMER', 'patient1'),
('30000002-2222-2222-2222-222222222222', NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CUSTOMER', 'patient2'),
('30000003-3333-3333-3333-333333333333', NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CUSTOMER', 'patient3'),
('30000004-4444-4444-4444-444444444444', NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'STAFF', 'receptionist'),
('30000005-5555-5555-5555-555555555555', NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'STAFF', 'cashier'),
('30000006-6666-6666-6666-666666666666', NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'STAFF', 'doctor'),
('30000007-7777-7777-7777-777777777777', NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'STAFF', 'nurse'),
('30000008-8888-8888-8888-888888888888', NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'STAFF', 'labdoctor'),
('30000009-9999-9999-9999-999999999999', NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'STAFF', 'imagingdoctor'),
('30000010-0000-0000-0000-000000000001', NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'STAFF', 'cardiologydoctor'),
('30000011-1111-1111-1111-111111111111', NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CUSTOMER', 'guest');

-- Link profile - account
UPDATE profile SET account_id = '30000001-1111-1111-1111-111111111111' WHERE profile_id = '10000001-1111-1111-1111-111111111111';
UPDATE profile SET account_id = '30000002-2222-2222-2222-222222222222' WHERE profile_id = '10000002-2222-2222-2222-222222222222';
UPDATE profile SET account_id = '30000003-3333-3333-3333-333333333333' WHERE profile_id = '10000003-3333-3333-3333-333333333333';
UPDATE profile SET account_id = '30000004-4444-4444-4444-444444444444' WHERE profile_id = '20000001-1111-1111-1111-111111111111';
UPDATE profile SET account_id = '30000005-5555-5555-5555-555555555555' WHERE profile_id = '20000002-2222-2222-2222-222222222222';
UPDATE profile SET account_id = '30000006-6666-6666-6666-666666666666' WHERE profile_id = '20000003-3333-3333-3333-333333333333';
UPDATE profile SET account_id = '30000007-7777-7777-7777-777777777777' WHERE profile_id = '20000004-4444-4444-4444-444444444444';
UPDATE profile SET account_id = '30000008-8888-8888-8888-888888888888' WHERE profile_id = '20000005-5555-5555-5555-555555555555';
UPDATE profile SET account_id = '30000009-9999-9999-9999-999999999999' WHERE profile_id = '20000006-6666-6666-6666-666666666666';
UPDATE profile SET account_id = '30000010-0000-0000-0000-000000000001' WHERE profile_id = '20000007-7777-7777-7777-777777777777';
UPDATE profile SET account_id = '30000011-1111-1111-1111-111111111111' WHERE profile_id = '20000008-8888-8888-8888-888888888888';

-- PostgreSQL/Hibernate khong tu cap nhat CHECK constraint khi enum co them DOCTOR.
-- Database moi chi seed vai tro hien hanh; khong can migration du lieu cu.
-- Danh muc nang luc thuc hien do admin mo rong.
INSERT INTO service_capability (capability_id, created_at, updated_at, deleted, code, name, description, active) VALUES
('ca000001-0000-0000-0000-000000000001', NOW(), NOW(), false, 'HEMATOLOGY', 'Xét nghiệm huyết học', 'Công thức máu và đông máu', true),
('ca000002-0000-0000-0000-000000000002', NOW(), NOW(), false, 'BIOCHEMISTRY', 'Xét nghiệm sinh hóa', 'Sinh hóa máu, gan, thận, đường huyết', true),
('ca000003-0000-0000-0000-000000000003', NOW(), NOW(), false, 'ULTRASOUND', 'Siêu âm', 'Các dịch vụ siêu âm', true),
('ca000004-0000-0000-0000-000000000004', NOW(), NOW(), false, 'XRAY', 'X-quang', 'Các dịch vụ X-quang', true),
('ca000005-0000-0000-0000-000000000005', NOW(), NOW(), false, 'CT_MRI', 'CT và MRI', 'Chụp cắt lớp và cộng hưởng từ', true);

-- ============================
-- MedicalService (39 bản ghi)
-- ============================
-- Lab tests
INSERT INTO medical_service (service_id, service_code, created_at, updated_at, deleted, description, status, is_point_of_care, name, price, department_type, department_id, required_specialization_id) VALUES
('40000001-1111-1111-1111-111111111111', 'XN001', NOW(), NOW(), false, 'Xét nghiệm công thức máu (CBC)', 'ACTIVE', false, 'Xét nghiệm công thức máu', 120000, 'LABORATORY', '44444444-4444-4444-4444-444444444444', NULL),
('40000002-2222-2222-2222-222222222222', 'XN002', NOW(), NOW(), false, 'Xét nghiệm sinh hóa máu', 'ACTIVE', false, 'Xét nghiệm sinh hóa máu', 150000, 'LABORATORY', '44444444-4444-4444-4444-444444444444', NULL),
('40000003-3333-3333-3333-333333333333', 'XN003', NOW(), NOW(), false, 'Xét nghiệm chức năng gan', 'ACTIVE', false, 'Xét nghiệm gan', 130000, 'LABORATORY', '44444444-4444-4444-4444-444444444444', NULL),
('40000011-1111-1111-1111-111111111111', 'XN004', NOW(), NOW(), false, 'Xét nghiệm đường huyết', 'ACTIVE', false, 'Xét nghiệm đường huyết', 80000, 'LABORATORY', '44444444-4444-4444-4444-444444444444', NULL),
('40000012-1111-1111-1111-111111111111', 'XN005', NOW(), NOW(), false, 'Xét nghiệm mỡ máu', 'ACTIVE', false, 'Xét nghiệm mỡ máu', 100000, 'LABORATORY', '44444444-4444-4444-4444-444444444444', NULL),
('40000013-1111-1111-1111-111111111111', 'XN006', NOW(), NOW(), false, 'Xét nghiệm chức năng thận', 'ACTIVE', false, 'Xét nghiệm thận', 110000, 'LABORATORY', '44444444-4444-4444-4444-444444444444', NULL),
('40000014-1111-1111-1111-111111111111', 'XN007', NOW(), NOW(), false, 'Xét nghiệm Hba1c', 'ACTIVE', false, 'Xét nghiệm Hba1c', 90000, 'LABORATORY', '44444444-4444-4444-4444-444444444444', NULL),
('40000015-1111-1111-1111-111111111111', 'XN008', NOW(), NOW(), false, 'Xét nghiệm nước tiểu', 'ACTIVE', false, 'Xét nghiệm nước tiểu', 50000, 'LABORATORY', '44444444-4444-4444-4444-444444444444', NULL),
('40000016-1111-1111-1111-111111111111', 'XN009', NOW(), NOW(), false, 'Xét nghiệm đông máu', 'ACTIVE', false, 'Xét nghiệm đông máu', 120000, 'LABORATORY', '44444444-4444-4444-4444-444444444444', NULL),
('40000017-1111-1111-1111-111111111111', 'XN010', NOW(), NOW(), false, 'Xét nghiệm viêm gan B', 'ACTIVE', false, 'Xét nghiệm viêm gan B', 150000, 'LABORATORY', '44444444-4444-4444-4444-444444444444', NULL),
('40000018-1111-1111-1111-111111111111', 'XN011', NOW(), NOW(), false, 'Xét nghiệm viêm gan C', 'ACTIVE', false, 'Xét nghiệm viêm gan C', 150000, 'LABORATORY', '44444444-4444-4444-4444-444444444444', NULL),
('40000019-1111-1111-1111-111111111111', 'XN012', NOW(), NOW(), false, 'Xét nghiệm HIV', 'ACTIVE', false, 'Xét nghiệm HIV', 200000, 'LABORATORY', '44444444-4444-4444-4444-444444444444', NULL),
('40000020-1111-1111-1111-111111111111', 'XN013', NOW(), NOW(), false, 'Xét nghiệm sốt xuất huyết', 'ACTIVE', false, 'Xét nghiệm sốt xuất huyết', 180000, 'LABORATORY', '44444444-4444-4444-4444-444444444444', NULL);

-- Imaging services
INSERT INTO medical_service (service_id, service_code, created_at, updated_at, deleted, description, status, is_point_of_care, name, price, department_type, department_id, required_specialization_id) VALUES
('40000004-4444-4444-4444-444444444444', 'CDHA001', NOW(), NOW(), false, 'Siêu âm ổ bụng', 'ACTIVE', false, 'Siêu âm ổ bụng', 250000, 'IMAGING', '55555555-5555-5555-5555-555555555555', NULL),
('40000005-5555-5555-5555-555555555555', 'CDHA002', NOW(), NOW(), false, 'Siêu âm tim', 'ACTIVE', false, 'Siêu âm tim', 300000, 'IMAGING', '55555555-5555-5555-5555-555555555555', '00000001-1111-1111-1111-111111111111'),
('40000006-6666-6666-6666-666666666666', 'XQ001', NOW(), NOW(), false, 'X-quang ngực', 'ACTIVE', false, 'X-quang ngực', 180000, 'IMAGING', '33333333-3333-3333-3333-333333333333', NULL),
('40000007-7777-7777-7777-777777777777', 'XQ002', NOW(), NOW(), false, 'X-quang cột sống', 'ACTIVE', false, 'X-quang cột sống', 250000, 'IMAGING', '33333333-3333-3333-3333-333333333333', NULL),
('40000021-1111-1111-1111-111111111111', 'CDHA003', NOW(), NOW(), false, 'Siêu âm tuyến giáp', 'ACTIVE', false, 'Siêu âm tuyến giáp', 200000, 'IMAGING', '55555555-5555-5555-5555-555555555555', NULL),
('40000022-1111-1111-1111-111111111111', 'CDHA004', NOW(), NOW(), false, 'Siêu âm phần mềm', 'ACTIVE', false, 'Siêu âm phần mềm', 200000, 'IMAGING', '55555555-5555-5555-5555-555555555555', NULL),
('40000023-1111-1111-1111-111111111111', 'XQ003', NOW(), NOW(), false, 'X-quang sọ não', 'ACTIVE', false, 'X-quang sọ não', 220000, 'IMAGING', '55555555-5555-5555-5555-555555555555', NULL),
('40000024-1111-1111-1111-111111111111', 'XQ004', NOW(), NOW(), false, 'X-quang xương khớp', 'ACTIVE', false, 'X-quang xương khớp', 200000, 'IMAGING', '55555555-5555-5555-5555-555555555555', NULL),
('40000025-1111-1111-1111-111111111111', 'MRI001', NOW(), NOW(), false, 'Chụp MRI sọ não', 'ACTIVE', false, 'Chụp MRI sọ não', 1500000, 'IMAGING', '55555555-5555-5555-5555-555555555555', NULL),
('40000026-1111-1111-1111-111111111111', 'MRI002', NOW(), NOW(), false, 'Chụp MRI cột sống', 'ACTIVE', false, 'Chụp MRI cột sống', 1800000, 'IMAGING', '55555555-5555-5555-5555-555555555555', NULL),
('40000027-1111-1111-1111-111111111111', 'CT001', NOW(), NOW(), false, 'Chụp CT scan đầu', 'ACTIVE', false, 'Chụp CT scan đầu', 1000000, 'IMAGING', '55555555-5555-5555-5555-555555555555', NULL),
('40000028-1111-1111-1111-111111111111', 'CT002', NOW(), NOW(), false, 'Chụp CT scan bụng', 'ACTIVE', false, 'Chụp CT scan bụng', 1200000, 'IMAGING', '55555555-5555-5555-5555-555555555555', NULL),
('40000029-1111-1111-1111-111111111111', 'CDHA005', NOW(), NOW(), false, 'Siêu âm thai', 'ACTIVE', false, 'Siêu âm thai', 350000, 'IMAGING', '55555555-5555-5555-5555-555555555555', NULL);

-- Clinical exam services
INSERT INTO medical_service (service_id, service_code, created_at, updated_at, deleted, description, status, is_point_of_care, name, price, department_type, department_id, required_specialization_id) VALUES
('40000008-8888-8888-8888-888888888888', 'KHB001', NOW(), NOW(), false, 'Khám bệnh tổng quát', 'ACTIVE', false, 'Khám bệnh tổng quát', 200000, 'EXAMINATION', '33333333-3333-3333-3333-333333333333', NULL),
('40000009-9999-9999-9999-999999999999', 'ECG001', NOW(), NOW(), false, 'Điện tâm đồ (ECG)', 'ACTIVE', false, 'Điện tâm đồ', 150000, 'EXAMINATION', '33333333-3333-3333-3333-333333333333', '00000004-4444-4444-4444-444444444444'),
('40000010-0000-0000-0000-000000000001', 'HH001', NOW(), NOW(), false, 'Đo chức năng hô hấp', 'ACTIVE', false, 'Đo chức năng hô hấp', 400000, 'EXAMINATION', '33333333-3333-3333-3333-333333333333', '00000004-4444-4444-4444-444444444444'),
('40000030-1111-1111-1111-111111111111', 'KHB002', NOW(), NOW(), false, 'Khám chuyên khoa nội', 'ACTIVE', false, 'Khám nội', 250000, 'EXAMINATION', '33333333-3333-3333-3333-333333333333', '00000001-1111-1111-1111-111111111111'),
('40000031-1111-1111-1111-111111111111', 'KHB003', NOW(), NOW(), false, 'Khám tim mạch', 'ACTIVE', false, 'Khám tim mạch', 300000, 'EXAMINATION', '33333333-3333-3333-3333-333333333333', '00000003-3333-3333-3333-333333333333'),
('40000032-1111-1111-1111-111111111111', 'KHB004', NOW(), NOW(), false, 'Khám hô hấp', 'ACTIVE', false, 'Khám hô hấp', 300000, 'EXAMINATION', '33333333-3333-3333-3333-333333333333', '00000004-4444-4444-4444-444444444444'),
('40000033-1111-1111-1111-111111111111', 'KHB005', NOW(), NOW(), false, 'Khám nội soi dạ dày', 'ACTIVE', false, 'Khám nội soi', 400000, 'EXAMINATION', '33333333-3333-3333-3333-333333333333', '00000005-5555-5555-5555-555555555555'),
('40000034-1111-1111-1111-111111111111', 'KHB006', NOW(), NOW(), false, 'Khám nhi khoa', 'ACTIVE', false, 'Khám nhi', 250000, 'EXAMINATION', '33333333-3333-3333-3333-333333333333', NULL),
('40000035-1111-1111-1111-111111111111', 'KHB007', NOW(), NOW(), false, 'Khám tai mũi họng', 'ACTIVE', false, 'Khám tai mũi họng', 250000, 'EXAMINATION', '33333333-3333-3333-3333-333333333333', NULL),
('40000036-1111-1111-1111-111111111111', 'KHB008', NOW(), NOW(), false, 'Khám mắt', 'ACTIVE', false, 'Khám mắt', 250000, 'EXAMINATION', '33333333-3333-3333-3333-333333333333', NULL),
('40000037-1111-1111-1111-111111111111', 'KHB009', NOW(), NOW(), false, 'Khám da liễu', 'ACTIVE', false, 'Khám da liễu', 250000, 'EXAMINATION', '33333333-3333-3333-3333-333333333333', NULL),
('40000038-1111-1111-1111-111111111111', 'KHB010', NOW(), NOW(), false, 'Khám răng hàm mặt', 'ACTIVE', false, 'Khám răng', 250000, 'EXAMINATION', '33333333-3333-3333-3333-333333333333', NULL),
('40000039-1111-1111-1111-111111111111', 'KHB011', NOW(), NOW(), false, 'Khám sức khỏe tổng quát', 'ACTIVE', false, 'Khám sức khỏe', 500000, 'EXAMINATION', '33333333-3333-3333-3333-333333333333', NULL);

-- Chuan hoa truc tiep ve hai nhom chuc nang hien tai tren database moi.
UPDATE department SET department_type = 'PARACLINICAL'
WHERE department_type IN ('LABORATORY', 'IMAGING');
UPDATE medical_service SET department_type = 'PARACLINICAL'
WHERE department_type IN ('LABORATORY', 'IMAGING');

-- Chuẩn hóa MedicalService theo mô hình điều phối mới:
-- dịch vụ chỉ khai báo yêu cầu; phòng cụ thể được chọn tại thời điểm check-in.
UPDATE medical_service
SET department_id = NULL,
    duration_minutes = CASE department_type
        WHEN 'EXAMINATION' THEN 30
        WHEN 'PARACLINICAL' THEN 20
        ELSE 15
    END,
    allow_customer_booking = true,
    minimum_age = 0,
    maximum_age = 120,
    allowed_gender = NULL;

-- Mỗi dịch vụ khám bắt buộc thuộc một chuyên khoa phục vụ.
UPDATE medical_service SET required_specialization_id = '00000006-6666-6666-6666-666666666666' WHERE service_code IN ('KHB001', 'KHB011');
UPDATE medical_service SET required_specialization_id = '00000001-1111-1111-1111-111111111111', minimum_age = 16 WHERE service_code = 'KHB002';
UPDATE medical_service SET required_specialization_id = '00000003-3333-3333-3333-333333333333' WHERE service_code IN ('KHB003', 'ECG001');
UPDATE medical_service SET required_specialization_id = '00000004-4444-4444-4444-444444444444' WHERE service_code IN ('KHB004', 'HH001');
UPDATE medical_service SET required_specialization_id = '00000005-5555-5555-5555-555555555555' WHERE service_code = 'KHB005';
UPDATE medical_service SET required_specialization_id = '00000007-7777-7777-7777-777777777777', maximum_age = 15 WHERE service_code = 'KHB006';
UPDATE medical_service SET required_specialization_id = '00000008-8888-8888-8888-888888888888' WHERE service_code = 'KHB007';
UPDATE medical_service SET required_specialization_id = '00000009-9999-9999-9999-999999999999' WHERE service_code = 'KHB008';
UPDATE medical_service SET required_specialization_id = '00000010-aaaa-aaaa-aaaa-aaaaaaaaaaaa' WHERE service_code = 'KHB009';
UPDATE medical_service SET required_specialization_id = '00000011-bbbb-bbbb-bbbb-bbbbbbbbbbbb' WHERE service_code = 'KHB010';

-- Xét nghiệm và chẩn đoán hình ảnh không phụ thuộc chuyên khoa phòng khám.
UPDATE medical_service SET required_specialization_id = NULL WHERE department_type = 'PARACLINICAL';
INSERT INTO department_capability (department_id, capability_id) VALUES
('44444444-4444-4444-4444-444444444444', 'ca000001-0000-0000-0000-000000000001'),
('44444444-4444-4444-4444-444444444444', 'ca000002-0000-0000-0000-000000000002'),
('55555555-5555-5555-5555-555555555555', 'ca000003-0000-0000-0000-000000000003'),
('55555555-5555-5555-5555-555555555555', 'ca000004-0000-0000-0000-000000000004'),
('55555555-5555-5555-5555-555555555555', 'ca000005-0000-0000-0000-000000000005');
UPDATE medical_service SET required_capability_id = 'ca000001-0000-0000-0000-000000000001', department_id = NULL WHERE service_code IN ('XN001', 'XN009');
UPDATE medical_service SET required_capability_id = 'ca000002-0000-0000-0000-000000000002', department_id = NULL WHERE department_type = 'PARACLINICAL' AND required_capability_id IS NULL;
UPDATE medical_service SET required_capability_id = 'ca000003-0000-0000-0000-000000000003', department_id = NULL WHERE service_code LIKE 'CDHA%';
UPDATE medical_service SET required_capability_id = 'ca000004-0000-0000-0000-000000000004', department_id = NULL WHERE service_code LIKE 'XQ%';
UPDATE medical_service SET required_capability_id = 'ca000005-0000-0000-0000-000000000005', department_id = NULL WHERE service_code LIKE 'CT%' OR service_code LIKE 'MRI%';

-- Ví dụ điều kiện nhân khẩu học: siêu âm thai chỉ cho nữ trong độ tuổi cấu hình.
UPDATE medical_service SET minimum_age = 15, maximum_age = 55, allowed_gender = 'FEMALE' WHERE service_code = 'CDHA005';

-- ============================
-- Appointment (5 bản ghi)
-- ============================
INSERT INTO appointment (appointment_id, created_at, updated_at, deleted, scheduled_at, status, is_guest, customer_id, time_slot) VALUES
('50000001-1111-1111-1111-111111111111', NOW(), NOW(), false, '2026-07-15 08:00:00', 'CHECKED_IN', false, '10000001-1111-1111-1111-111111111111', 'MORNING'),
('50000002-2222-2222-2222-222222222222', NOW(), NOW(), false, '2026-07-16 09:30:00', 'CHECKED_IN', false, '10000001-1111-1111-1111-111111111111', 'MORNING'),
('50000003-3333-3333-3333-333333333333', NOW(), NOW(), false, '2026-07-17 10:00:00', 'CHECKED_IN', false, '10000002-2222-2222-2222-222222222222', 'MORNING'),
('50000004-4444-4444-4444-444444444444', NOW(), NOW(), false, '2026-07-18 14:00:00', 'CHECKED_IN', false, '10000002-2222-2222-2222-222222222222', 'AFTERNOON'),
('50000005-5555-5555-5555-555555555555', NOW(), NOW(), false, '2026-07-19 15:30:00', 'CANCELLED', true, '20000008-8888-8888-8888-888888888888', 'AFTERNOON');

-- Thêm dịch vụ vào appointment
INSERT INTO appointment_services (appointment_id, service_id) VALUES
('50000001-1111-1111-1111-111111111111', '40000001-1111-1111-1111-111111111111'),
('50000001-1111-1111-1111-111111111111', '40000002-2222-2222-2222-222222222222'),
('50000002-2222-2222-2222-222222222222', '40000006-6666-6666-6666-666666666666'),
('50000003-3333-3333-3333-333333333333', '40000001-1111-1111-1111-111111111111'),
('50000004-4444-4444-4444-444444444444', '40000002-2222-2222-2222-222222222222');

-- ============================
-- CustomerVisit (5 bản ghi)
-- ============================
INSERT INTO customer_visit (visit_id, created_at, updated_at, deleted, check_in_time, check_out_time, status, customer_id, appointment_id) VALUES
('60000001-1111-1111-1111-111111111111', NOW(), NOW(), false, NOW() - INTERVAL '4 hour', NOW() - INTERVAL '1 hour', 'COMPLETED', '10000001-1111-1111-1111-111111111111', '50000001-1111-1111-1111-111111111111'),
('60000002-2222-2222-2222-222222222222', NOW(), NOW(), false, NOW() - INTERVAL '2 day', NOW() - INTERVAL '2 day' + INTERVAL '2 hour', 'COMPLETED', '10000001-1111-1111-1111-111111111111', '50000002-2222-2222-2222-222222222222'),
('60000003-3333-3333-3333-333333333333', NOW(), NOW(), false, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day' + INTERVAL '1 hour', 'COMPLETED', '10000002-2222-2222-2222-222222222222', '50000003-3333-3333-3333-333333333333'),
('60000004-4444-4444-4444-444444444444', NOW(), NOW(), false, NOW(), NULL, 'CHECKED_IN', '10000002-2222-2222-2222-222222222222', '50000004-4444-4444-4444-444444444444'),
('60000005-5555-5555-5555-555555555555', NOW(), NOW(), false, NOW() - INTERVAL '3 day', NOW() - INTERVAL '3 day', 'CANCELLED', '20000008-8888-8888-8888-888888888888', '50000005-5555-5555-5555-555555555555');

-- ============================
-- Invoice (5 bản ghi)
-- ============================
INSERT INTO invoice (invoice_id, created_at, updated_at, deleted, invoice_code, customer_id, visit_id, discount, due_date, issue_date, note, paid_amount, status, subtotal, tax, total_amount) VALUES
('70000001-1111-1111-1111-111111111111', NOW(), NOW(), false, 'INV-20260711-001', '10000001-1111-1111-1111-111111111111', '60000001-1111-1111-1111-111111111111', 0, NULL, NOW()::date, 'Khám + Xét nghiệm', 0, 'PENDING', 350000, 0, 350000),
('70000002-2222-2222-2222-222222222222', NOW(), NOW(), false, 'INV-20260711-002', '10000001-1111-1111-1111-111111111111', '60000002-2222-2222-2222-222222222222', 18000, NULL, NOW()::date, 'X-quang ngực - có giảm', 0, 'PENDING', 180000, 0, 162000),
('70000003-3333-3333-3333-333333333333', NOW(), NOW(), false, 'INV-20260711-003', '10000002-2222-2222-2222-222222222222', '60000003-3333-3333-3333-333333333333', 0, NULL, NOW()::date, 'Khám bệnh', 0, 'PENDING', 200000, 0, 200000),
('70000004-4444-4444-4444-444444444444', NOW(), NOW(), false, 'INV-20260711-004', '10000002-2222-2222-2222-222222222222', '60000004-4444-4444-4444-444444444444', 0, NULL, NOW()::date, 'Xét nghiệm - đã thanh toán', 150000, 'PAID', 150000, 0, 150000),
('70000005-5555-5555-5555-555555555555', NOW(), NOW(), false, 'INV-20260711-005', '20000008-8888-8888-8888-888888888888', '60000005-5555-5555-5555-555555555555', 0, NULL, NOW()::date, 'Khám bệnh - đã hủy', 0, 'CANCELLED', 200000, 0, 200000);

-- ============================
-- InvoiceItem (5 bản ghi) - có bhytFund
-- ============================
INSERT INTO invoice_item (item_id, created_at, updated_at, deleted, line_total, service_snapshot, service_code_snapshot, unit_price, quantity, invoice_id, service_id, bhyt_fund) VALUES
('80000001-1111-1111-1111-111111111111', NOW(), NOW(), false, 200000, 'Khám bệnh tổng quát', 'KHB001', 200000, 1, '70000001-1111-1111-1111-111111111111', '40000008-8888-8888-8888-888888888888', 160000),
('80000002-2222-2222-2222-222222222222', NOW(), NOW(), false, 150000, 'Xét nghiệm sinh hóa máu', 'XN001', 150000, 1, '70000001-1111-1111-1111-111111111111', '40000002-2222-2222-2222-222222222222', 120000),
('80000003-3333-3333-3333-333333333333', NOW(), NOW(), false, 180000, 'X-quang ngực', 'XQ001', 180000, 1, '70000002-2222-2222-2222-222222222222', '40000006-6666-6666-6666-666666666666', 0),
('80000004-4444-4444-4444-444444444444', NOW(), NOW(), false, 150000, 'Xét nghiệm sinh hóa máu', 'XN001', 150000, 1, '70000004-4444-4444-4444-444444444444', '40000002-2222-2222-2222-222222222222', 135000),
('80000005-5555-5555-5555-555555555555', NOW(), NOW(), false, 200000, 'Khám bệnh tổng quát', 'KHB001', 200000, 1, '70000003-3333-3333-3333-333333333333', '40000008-8888-8888-8888-888888888888', 0);

-- ============================
-- StaffInfo (7 bản ghi) - NOTE: đã xóa department_id
-- ============================
INSERT INTO staff_info (staff_id, created_at, updated_at, deleted, profile_id, staff_code, system_role, national_id, bank_account, highest_degree, university, license_number, specialization_id) VALUES
('90000001-1111-1111-1111-111111111111', NOW(), NOW(), false, '20000001-1111-1111-1111-111111111111', 'STF-20260711-LT01', 'RECEPTIONIST', '0123456789', NULL, NULL, NULL, NULL, NULL),
('90000002-2222-2222-2222-222222222222', NOW(), NOW(), false, '20000002-2222-2222-2222-222222222222', 'STF-20260711-TN01', 'CASHIER', '1123456789', NULL, NULL, NULL, NULL, NULL),
('90000003-3333-3333-3333-333333333333', NOW(), NOW(), false, '20000003-3333-3333-3333-333333333333', 'STF-20260711-BS01', 'DOCTOR', '2123456789', NULL, NULL, NULL, NULL, '00000001-1111-1111-1111-111111111111'),
('90000004-4444-4444-4444-444444444444', NOW(), NOW(), false, '20000004-4444-4444-4444-444444444444', 'STF-20260711-YT01', 'NURSE', '3123456789', NULL, NULL, NULL, NULL, NULL),
('90000005-5555-5555-5555-555555555555', NOW(), NOW(), false, '20000005-5555-5555-5555-555555555555', 'STF-20260711-XN01', 'DOCTOR', '4123456789', NULL, NULL, NULL, NULL, '00000002-2222-2222-2222-222222222222'),
('90000006-6666-6666-6666-666666666666', NOW(), NOW(), false, '20000006-6666-6666-6666-666666666666', 'STF-20260711-IMG01', 'DOCTOR', '5123456789', NULL, NULL, NULL, NULL, '00000003-3333-3333-3333-333333333333'),
('90000007-7777-7777-7777-777777777777', NOW(), NOW(), false, '20000007-7777-7777-7777-777777777777', 'STF-20260711-TM01', 'DOCTOR', '6123456789', NULL, NULL, NULL, NULL, '00000004-4444-4444-4444-444444444444');

-- Năng lực bác sĩ; toàn bộ thông tin chứng nhận chi tiết được phép NULL.
INSERT INTO staff_capability (staff_capability_id, created_at, updated_at, deleted, staff_id, capability_id, certificate_number, issued_date, expiry_date, issuing_organization, status) VALUES
('ac000001-0000-0000-0000-000000000001', NOW(), NOW(), false, '90000005-5555-5555-5555-555555555555', 'ca000001-0000-0000-0000-000000000001', NULL, NULL, NULL, NULL, 'ACTIVE'),
('ac000002-0000-0000-0000-000000000002', NOW(), NOW(), false, '90000005-5555-5555-5555-555555555555', 'ca000002-0000-0000-0000-000000000002', NULL, NULL, NULL, NULL, 'ACTIVE'),
('ac000003-0000-0000-0000-000000000003', NOW(), NOW(), false, '90000006-6666-6666-6666-666666666666', 'ca000003-0000-0000-0000-000000000003', NULL, NULL, NULL, NULL, 'ACTIVE'),
('ac000004-0000-0000-0000-000000000004', NOW(), NOW(), false, '90000006-6666-6666-6666-666666666666', 'ca000004-0000-0000-0000-000000000004', NULL, NULL, NULL, NULL, 'ACTIVE');

-- ============================
-- QueueTicket (5 bản ghi)
-- ============================
INSERT INTO queue_ticket (ticket_id, created_at, updated_at, deleted, work_date, queue_number, status, called_at, completed_at, visit_id, department_id, service_id) VALUES
('a0000001-a000-a000-a000-a00000000001', NOW(), NOW(), false, NOW()::date, 1, 'DONE', NOW() - INTERVAL '2 hour', NOW() - INTERVAL '1 hour', '60000001-1111-1111-1111-111111111111', '44444444-4444-4444-4444-444444444444', '40000001-1111-1111-1111-111111111111'),
('a0000002-a000-a000-a000-a00000000002', NOW(), NOW(), false, NOW()::date, 2, 'DONE', NOW(), NOW(), '60000002-2222-2222-2222-222222222222', '44444444-4444-4444-4444-444444444444', '40000002-2222-2222-2222-222222222222'),
('a0000003-a000-a000-a000-a00000000003', NOW(), NOW(), false, NOW()::date, 10, 'DONE', NOW(), NOW(), '60000003-3333-3333-3333-333333333333', '33333333-3333-3333-3333-333333333333', '40000008-8888-8888-8888-888888888888'),
('a0000004-a000-a000-a000-a00000000004', NOW(), NOW(), false, NOW()::date, 5, 'WAITING', NULL, NULL, '60000004-4444-4444-4444-444444444444', '33333333-3333-3333-3333-333333333333', '40000008-8888-8888-8888-888888888888'),
('a0000005-a000-a000-a000-a00000000005', NOW(), NOW(), false, NOW()::date, 6, 'SKIPPED', NULL, NOW(), '60000005-5555-5555-5555-555555555555', '33333333-3333-3333-3333-333333333333', '40000008-8888-8888-8888-888888888888');

-- ============================
-- MedicalRecord (3 bản ghi) - có rating mẫu
-- ============================
INSERT INTO medical_record (record_id, created_at, updated_at, deleted, chief_complaint, clinical_findings, diagnosis, prescription_note, conclusion, patient_instruction, status, completed_at, doctor_id, visit_id, vital_signs_id, rating_score, rated_at) VALUES
('c0000001-c000-c000-c000-c00000000001', NOW(), NOW(), false, 'Sốt hô hấp', 'Đã khám và đánh giá kết quả', 'J18.9', NULL, 'Hoàn thành đợt khám', 'Theo dõi tại nhà', 'COMPLETED', NOW() - INTERVAL '1 hour', '90000007-7777-7777-7777-777777777777', '60000001-1111-1111-1111-111111111111', NULL, NULL, NULL),
('c0000002-c000-c000-c000-c00000000002', NOW(), NOW(), false, 'Sốt ban đêm', 'Huyết áp: 120/80, Nhiệt độ: 38.5°C', 'A01', 'Paracetamol 500mg - 3 lần/ngày', 'Theo dõi tại nhà', 'Tái khám sau 1 tuần', 'COMPLETED', NOW(), '90000007-7777-7777-7777-777777777777', '60000002-2222-2222-2222-222222222222', NULL, 4, NOW()),
('c0000003-c000-c000-c000-c00000000003', NOW(), NOW(), false, 'Khó thở', NULL, 'R05', NULL, 'X-quang ngực - có giảm', 'Ghi nhận kết quả', 'COMPLETED', NOW(), '90000003-3333-3333-3333-333333333333', '60000003-3333-3333-3333-333333333333', NULL, 5, NOW()),
('c0000003-c000-c000-c000-c00000000004', NOW(), NOW(), false, NULL, NULL, NULL, NULL, NULL, NULL, 'IN_PROGRESS', NULL, '90000003-3333-3333-3333-333333333333', '60000004-4444-4444-4444-444444444444', NULL, NULL, NULL);

-- ============================
-- VitalSigns (1 bản ghi - cho medical_record c0000002)
-- ============================
INSERT INTO vital_signs (vital_id, created_at, updated_at, deleted, blood_pressure, heart_rate, temperature, weight, height, medical_record_id, recorded_by) VALUES
('80000001-1111-1111-1111-111111111111', NOW(), NOW(), false, '120/80', 80, 38.5::numeric(4,1), 65::numeric(5,2), 165::numeric(5,2), 'c0000002-c000-c000-c000-c00000000002', '90000007-7777-7777-7777-777777777777');

-- Cap nhat vital_signs_id cho record c0000002
UPDATE medical_record SET vital_signs_id = '80000001-1111-1111-1111-111111111111' WHERE record_id = 'c0000002-c000-c000-c000-c00000000002';

-- ============================
-- ICD-10 Selections cho record c0000002
-- ============================
INSERT INTO icd_10_selections (selection_id, created_at, updated_at, deleted, record_id, code, code_name, note) VALUES
('a1000001-a000-a000-a000-a00000000001', NOW(), NOW(), false, 'c0000002-c000-c000-c000-c00000000002', 'A01', 'Thương hàn và phó thương hàn', 'Bệnh nhiễm khuẩn đường tiêu hóa'),
('a1000002-a000-a000-a000-a00000000002', NOW(), NOW(), false, 'c0000002-c000-c000-c000-c00000000002', 'R50.9', 'Sốt chưa rõ nguyên nhân', NULL);

-- ============================
-- Prescription Items cho record c0000002
-- ============================
INSERT INTO prescription_item (prescription_item_id, created_at, updated_at, deleted, record_id, medicine_name, quantity, unit, note, frequency_per_day) VALUES
('b1000001-b000-b000-b000-b00000000001', NOW(), NOW(), false, 'c0000002-c000-c000-c000-c00000000002', 'Paracetamol 500mg', 12, 'viên', 'Uống khi sốt, sau ăn', 3),
('b1000002-b000-b000-b000-b00000000002', NOW(), NOW(), false, 'c0000002-c000-c000-c000-c00000000002', 'Vitamin C', 10, 'viên', 'Uống hàng ngày', 1),
('b1000003-b000-b000-b000-b00000000003', NOW(), NOW(), false, 'c0000002-c000-c000-c000-c00000000002', 'Thuốc ho tan mật', 6, 'ml', 'Uống 2 lần/ngày, 10ml/lần', 2);

-- Danh mục thuốc dùng để bác sĩ tìm/chọn nhanh khi kê đơn.
-- Bác sĩ vẫn được phép nhập tên thuốc ngoài danh mục; đơn thuốc lưu snapshot bằng medicine_name.
INSERT INTO medicine_catalog (medicine_id, medicine_code, name, active_ingredient, default_unit, default_usage, default_frequency_per_day, active, deleted) VALUES
('d1000001-d000-d000-d000-d00000000001', 'MED-PARA-500', 'Paracetamol 500mg', 'Paracetamol 500mg', 'viên', 'Uống sau ăn khi đau hoặc sốt', 2, true, false),
('d1000002-d000-d000-d000-d00000000002', 'MED-AMOX-500', 'Amoxicillin 500mg', 'Amoxicillin 500mg', 'viên', 'Uống sau ăn theo chỉ định của bác sĩ', 3, true, false),
('d1000003-d000-d000-d000-d00000000003', 'MED-OME-20', 'Omeprazole 20mg', 'Omeprazole 20mg', 'viên', 'Uống trước ăn sáng 30 phút', 1, true, false),
('d1000004-d000-d000-d000-d00000000004', 'MED-CET-10', 'Cetirizine 10mg', 'Cetirizine 10mg', 'viên', 'Uống vào buổi tối', 1, true, false),
('d1000005-d000-d000-d000-d00000000005', 'MED-ORS', 'Oresol', 'Glucose và điện giải', 'gói', 'Pha đúng lượng nước ghi trên bao bì', 3, true, false),
('d1000006-d000-d000-d000-d00000000006', 'MED-SAL-100', 'Salbutamol 100mcg/liều', 'Salbutamol', 'lọ', 'Sử dụng theo hướng dẫn của bác sĩ', 2, true, false)
ON CONFLICT (medicine_id) DO NOTHING;

-- ============================
-- PaymentTransaction (5 bản ghi)
-- ============================
INSERT INTO payment_transaction (transaction_id, created_at, updated_at, deleted, transaction_code, amount, payment_method, status, paid_at, gateway_reference, note, invoice_id, received_by) VALUES
('b0000001-b000-b000-b000-b00000000001', NOW(), NOW(), false, 'TXN-20260711-001', 100000, 'CASH', 'SUCCESS', NOW(), NULL, 'Thanh toán góp 1', '70000001-1111-1111-1111-111111111111', NULL),
('b0000002-b000-b000-b000-b00000000002', NOW(), NOW(), false, 'TXN-20260711-002', 250000, 'BANK_TRANSFER', 'SUCCESS', NOW(), NULL, 'Thanh toán góp 2 - hoàn thành', '70000001-1111-1111-1111-111111111111', NULL),
('b0000003-b000-b000-b000-b00000000003', NOW(), NOW(), false, 'TXN-20260711-003', 162000, 'CASH', 'SUCCESS', NOW(), NULL, 'Thanh toán hoàn', '70000002-2222-2222-2222-222222222222', NULL),
('b0000004-b000-b000-b000-b00000000004', NOW(), NOW(), false, 'TXN-20260711-004', 150000, 'CARD', 'SUCCESS', NOW(), NULL, 'Thanh toán thẻ', '70000004-4444-4444-4444-444444444444', NULL),
('b0000005-b000-b000-b000-b00000000005', NOW(), NOW(), false, 'TXN-20260711-005', 50000, 'CASH', 'FAILED', NULL, NULL, 'Thanh toán thất bại - không đủ tiền', '70000005-5555-5555-5555-555555555555', NULL);

INSERT INTO icd_10_codes (code, name, description, category, deleted)
VALUES
    ('A00', 'Bệnh tả', 'Bệnh truyền nhiễm cấp tính do vi khuẩn Vibrio cholerae gây ra.', 'Bệnh truyền nhiễm', false),

    ('A01', 'Thương hàn và phó thương hàn', 'Nhiễm khuẩn đường tiêu hóa do Salmonella.', 'Bệnh truyền nhiễm', false),

    ('B20', 'Nhiễm HIV', 'Bệnh do virus HIV gây suy giảm miễn dịch mắc phải.', 'Bệnh truyền nhiễm', false),

    ('C34', 'Ung thư phổi', 'Khối u ác tính xuất phát từ nhu mô phổi.', 'Ung thư', false),

    ('D50', 'Thiếu máu do thiếu sắt', 'Thiếu máu do thiếu hụt sắt kéo dài.', 'Huyết học', false),

    ('E11', 'Đái tháo đường týp 2', 'Bệnh rối loạn chuyển hóa glucose do đề kháng insulin.', 'Nội tiết', false),

    ('I10', 'Tăng huyết áp vô căn', 'Tăng huyết áp không xác định nguyên nhân.', 'Tim mạch', false),

    ('J18.9', 'Viêm phổi không xác định', 'Viêm phổi chưa xác định tác nhân gây bệnh.', 'Hô hấp', false),

    ('K35', 'Viêm ruột thừa cấp', 'Tình trạng viêm cấp tính của ruột thừa.', 'Tiêu hóa', false),

    ('N20.0', 'Sỏi thận', 'Sỏi nằm trong bể thận hoặc nhu mô thận.', 'Tiết niệu', false),

    ('M54.5', 'Đau thắt lưng', 'Đau vùng cột sống thắt lưng.', 'Cơ xương khớp', false),

    ('R50.9', 'Sốt chưa rõ nguyên nhân', 'Sốt nhưng chưa xác định được nguyên nhân.', 'Triệu chứng', false);

-- ============================
-- Admin & Clinic Manager Accounts
-- ============================
-- Tai khoan seed co the dang nhap:
--   admin         / 88888888
--   clinicmanager / 88888888

-- Profile - Admin (1 bản ghi)
INSERT INTO profile (profile_id, created_at, updated_at, deleted, full_name, date_of_birth, gender, phone, email, address, blood_type) VALUES
('20000009-9999-9999-9999-999999999999', NOW(), NOW(), false, 'Quản trị viên hệ thống', '1985-01-01', 'MALE', '0999999999', 'admin@example.com', 'Hà Nội', NULL);

-- Profile - Clinic Manager (1 bản ghi)
INSERT INTO profile (profile_id, created_at, updated_at, deleted, full_name, date_of_birth, gender, phone, email, address, blood_type) VALUES
('20000010-0000-0000-0000-000000000001', NOW(), NOW(), false, 'Quản lý phòng khám', '1988-01-01', 'FEMALE', '0888888888', 'clinicmanager@example.com', 'TP.HCM', NULL);

-- Account - Admin (1 bản ghi)
INSERT INTO account (account_id, created_at, is_active, password_hash, role, username) VALUES
('30000012-2222-2222-2222-222222222222', NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'STAFF', 'admin');

-- Account - Clinic Manager (1 bản ghi)
INSERT INTO account (account_id, created_at, is_active, password_hash, role, username) VALUES
('30000013-3333-3333-3333-333333333333', NOW(), true, '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'STAFF', 'clinicmanager');

-- Link profile - account
UPDATE profile SET account_id = '30000012-2222-2222-2222-222222222222' WHERE profile_id = '20000009-9999-9999-9999-999999999999';
UPDATE profile SET account_id = '30000013-3333-3333-3333-333333333333' WHERE profile_id = '20000010-0000-0000-0000-000000000001';

-- StaffInfo - Admin (khong co department)
INSERT INTO staff_info (staff_id, created_at, updated_at, deleted, profile_id, staff_code, system_role, national_id, bank_account, highest_degree, university, license_number, specialization_id) VALUES
('90000008-1111-1111-1111-111111111111', NOW(), NOW(), false, '20000009-9999-9999-9999-999999999999', 'STF-20260722-ADM01', 'ADMIN', '9123456789', NULL, NULL, NULL, NULL, NULL);

-- StaffInfo - Clinic Manager (khong co department - quan ly phong khong phai lam head doctor)
INSERT INTO staff_info (staff_id, created_at, updated_at, deleted, profile_id, staff_code, system_role, national_id, bank_account, highest_degree, university, license_number, specialization_id) VALUES
('90000009-2222-2222-2222-222222222222', NOW(), NOW(), false, '20000010-0000-0000-0000-000000000001', 'STF-20260722-QL01', 'CLINIC_MANAGER', '9223456789', NULL, NULL, NULL, NULL, NULL);

-- TestRequest cho patient1 (record c0000002)
INSERT INTO test_request (test_request_id, created_at, updated_at, deleted, description, status, completed_at, medical_record_id, service_id, performing_department, requested_by) VALUES
('d0000001-d000-d000-d000-d00000000001', NOW(), NOW(), false, NULL, 'COMPLETED', NOW(), 'c0000002-c000-c000-c000-c00000000002', '40000002-2222-2222-2222-222222222222', '44444444-4444-4444-4444-444444444444', '90000003-3333-3333-3333-333333333333');

-- TestResult cho test_request trên
INSERT INTO test_result (result_id, created_at, updated_at, deleted, conclusion, image_url, sample_id, performed_at, performed_by, verified_by, verified_at, test_request_id) VALUES
('e0000001-e000-e000-e000-e00000000001', NOW(), NOW(), false, 'Ket qua binh thuong', '/uploads/test-results/blood-test-001.pdf', 'SAMPLE-001', NOW(), '90000004-4444-4444-4444-444444444444', '90000005-5555-5555-5555-555555555555', NOW(), 'd0000001-d000-d000-d000-d00000000001');

-- Tat ca tai khoan seed dung chung mat khau: 88888888 (BCrypt).
UPDATE account SET password_hash = '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m';

-- Phong thu cuoi cung o tang database: moi benh nhan chi co mot luot dang hoat dong.
CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_visit_one_active
ON customer_visit (customer_id)
WHERE deleted = false
  AND customer_id IS NOT NULL
  AND status IN ('CHECKED_IN', 'IN_PROGRESS');
