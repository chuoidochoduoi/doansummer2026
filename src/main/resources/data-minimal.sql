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
    contact_request, public_announcement, department_capability, medical_service,
    medicine_catalog, icd_10_codes, department, specialization, service_capability,
    staff_info, staff_capability, staff_schedule,
    profile, account, shift_config
RESTART IDENTITY CASCADE;

-- ===================================================================
-- Specialization (danh muc chuyen khoa - toi thieu de bo sung vao department)
-- ===================================================================
INSERT INTO specialization (specialization_id, created_at, updated_at, deleted, active, name, description) VALUES
('00000001-1111-1111-1111-111111111111', NOW(), NOW(), false, true, 'Nội khoa', 'Chẩn đoán và điều trị bệnh nội'),
('00000002-2222-2222-2222-222222222222', NOW(), NOW(), false, true, 'Nhi khoa', 'Khám và điều trị cho trẻ em'),
('00000003-3333-3333-3333-333333333333', NOW(), NOW(), false, true, 'Ngoại khoa', 'Khám và xử trí các bệnh lý ngoại khoa'),
('00000004-4444-4444-4444-444444444444', NOW(), NOW(), false, true, 'Da liễu', 'Khám và điều trị bệnh da liễu'),
('00000008-8888-8888-8888-888888888888', NOW(), NOW(), false, true, 'Sản phụ khoa', 'Khám sức khỏe phụ nữ và thai kỳ');

-- ===================================================================
-- Department (phòng khám - toi thieu de he thong hoat dong)
-- ===================================================================
INSERT INTO department (department_id, created_at, updated_at, deleted, room_code, name, status, department_type, specialization_id, description, head_doctor_id) VALUES
('33333333-3333-3333-3333-333333333333', NOW(), NOW(), false, 'INT-101', 'Phòng khám Nội', 'AVAILABLE', 'EXAMINATION', '00000001-1111-1111-1111-111111111111', 'Khám bệnh nội khoa', NULL),
('44444444-4444-4444-4444-444444444444', NOW(), NOW(), false, 'LAB-201', 'Phòng Xét nghiệm', 'AVAILABLE', 'PARACLINICAL', NULL, 'Thực hiện các xét nghiệm mẫu bệnh phẩm', NULL),
('55555555-5555-5555-5555-555555555555', NOW(), NOW(), false, 'IMG-301', 'Phòng Chẩn đoán hình ảnh', 'AVAILABLE', 'PARACLINICAL', NULL, 'Siêu âm và X-quang', NULL),
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
('20000009-9999-9999-9999-999999999999', NOW(), NOW(), false, 'Phạm Đức Minh', '1985-03-18', 'MALE', '0903214567', 'ducminh.pham@cares.vn', 'Cầu Giấy, Hà Nội', NULL),
('20000010-0000-0000-0000-000000000001', NOW(), NOW(), false, 'Nguyễn Thu Hương', '1988-09-24', 'FEMALE', '0912864537', 'thuhuong.nguyen@cares.vn', 'Thanh Xuân, Hà Nội', NULL);

-- ===================================================================
-- ServiceCapability (nang luc thuc hien dich vu - dung de gan voi medical_service/lab)
-- ===================================================================
INSERT INTO service_capability (capability_id, created_at, updated_at, deleted, code, name, description, active) VALUES
('ca000001-0000-0000-0000-000000000001', NOW(), NOW(), false, 'HEMATOLOGY', 'Xét nghiệm huyết học', 'Công thức máu và các xét nghiệm liên quan', true),
('ca000002-0000-0000-0000-000000000002', NOW(), NOW(), false, 'BIOCHEMISTRY', 'Xét nghiệm sinh hóa', 'Sinh hóa máu và chức năng cơ quan', true),
('ca000003-0000-0000-0000-000000000003', NOW(), NOW(), false, 'ULTRASOUND', 'Siêu âm', 'Các dịch vụ siêu âm hình ảnh', true),
('ca000004-0000-0000-0000-000000000004', NOW(), NOW(), false, 'XRAY', 'X-quang', 'Các dịch vụ X-quang', true),
('ca000005-0000-0000-0000-000000000005', NOW(), NOW(), false, 'URINALYSIS', 'Xét nghiệm nước tiểu', 'Phân tích nước tiểu thường quy', true),
('ca000006-0000-0000-0000-000000000006', NOW(), NOW(), false, 'ECG', 'Điện tim', 'Ghi và đọc điện tâm đồ', true),
('ca000007-0000-0000-0000-000000000007', NOW(), NOW(), false, 'MICROBIOLOGY', 'Xét nghiệm vi sinh', 'Soi và nuôi cấy vi sinh', true),
('ca000008-0000-0000-0000-000000000008', NOW(), NOW(), false, 'RAPID_TEST', 'Test nhanh', 'Thực hiện các kỹ thuật test nhanh bệnh truyền nhiễm', true);

-- Link department -> capability
INSERT INTO department_capability (department_id, capability_id) VALUES
('44444444-4444-4444-4444-444444444444', 'ca000001-0000-0000-0000-000000000001'),
('44444444-4444-4444-4444-444444444444', 'ca000002-0000-0000-0000-000000000002'),
('44444444-4444-4444-4444-444444444444', 'ca000005-0000-0000-0000-000000000005'),
('44444444-4444-4444-4444-444444444444', 'ca000007-0000-0000-0000-000000000007'),
('44444444-4444-4444-4444-444444444444', 'ca000008-0000-0000-0000-000000000008'),
('55555555-5555-5555-5555-555555555555', 'ca000003-0000-0000-0000-000000000003'),
('55555555-5555-5555-5555-555555555555', 'ca000004-0000-0000-0000-000000000004'),
('55555555-5555-5555-5555-555555555555', 'ca000006-0000-0000-0000-000000000006');

-- ===================================================================
-- MedicalService (dich vu y te - toi thieu: kham + xn + CDHA)
-- ===================================================================
INSERT INTO medical_service (
    service_id, service_code, created_at, updated_at, deleted,
    description, status, is_point_of_care, name, price, department_type,
    duration_minutes, workflow_priority, requires_doctor_order,
    requires_return_to_doctor, requires_specimen, result_wait_minutes,
    allow_customer_booking, minimum_age, maximum_age, allowed_gender,
    department_id, required_specialization_id, required_capability_id
) VALUES
-- Dịch vụ khám không gắn cứng department_id. Phòng được chọn theo chuyên khoa
-- phục vụ tại thời điểm điều phối.
('40000008-8888-8888-8888-888888888888', 'KHB001', NOW(), NOW(), false,
 'Khám và đánh giá tổng quát các bệnh lý nội khoa thường gặp ở người lớn.', 'ACTIVE', false, 'Khám Nội tổng quát', 220000, 'EXAMINATION',
 30, 1, false, false, false, 0, true, 0, 120, NULL,
 NULL, '00000001-1111-1111-1111-111111111111', NULL),
-- Dịch vụ cận lâm sàng được chọn phòng theo năng lực thực hiện.
('40000001-1111-1111-1111-111111111111', 'XN001', NOW(), NOW(), false,
 'Đánh giá các thành phần tế bào máu, hỗ trợ phát hiện thiếu máu và nhiễm trùng.', 'ACTIVE', false, 'Công thức máu', 120000, 'PARACLINICAL',
 20, 1, true, true, true, 30, false, 0, 120, NULL,
 NULL, NULL, 'ca000001-0000-0000-0000-000000000001'),
('40000004-4444-4444-4444-444444444444', 'CDHA001', NOW(), NOW(), false,
 'Siêu âm ổ bụng', 'ACTIVE', false, 'Siêu âm ổ bụng', 250000, 'PARACLINICAL',
 20, 1, true, true, false, 15, false, 0, 120, NULL,
 NULL, NULL, 'ca000003-0000-0000-0000-000000000003');

-- Tất cả dịch vụ ACTIVE đều được phép đặt từ phía khách hàng.
UPDATE medical_service
SET allow_customer_booking = true
WHERE status = 'ACTIVE' AND deleted = false;

-- Link profile -> account
UPDATE profile SET account_id = '30000012-2222-2222-2222-222222222222' WHERE profile_id = '20000009-9999-9999-9999-999999999999';
UPDATE profile SET account_id = '30000013-3333-3333-3333-333333333333' WHERE profile_id = '20000010-0000-0000-0000-000000000001';

-- ===================================================================
-- StaffInfo (thong tin nhan vien) - admin + clinic manager
-- ===================================================================
INSERT INTO staff_info (staff_id, created_at, updated_at, deleted, profile_id, staff_code, system_role, national_id, highest_degree, university, license_number, specialization_id, department_id) VALUES
('90000008-1111-1111-1111-111111111111', NOW(), NOW(), false, '20000009-9999-9999-9999-999999999999', 'STF-ADM-001', 'ADMIN', '9123456789', NULL, NULL, NULL, NULL, NULL),
('90000009-2222-2222-2222-222222222222', NOW(), NOW(), false, '20000010-0000-0000-0000-000000000001', 'STF-CLM-001', 'CLINIC_MANAGER', '9223456789', NULL, NULL, NULL, NULL, '33333333-3333-3333-3333-333333333333');

-- ===================================================================
-- Medicine Catalog
-- ===================================================================
INSERT INTO medicine_catalog (medicine_id, medicine_code, name, active_ingredient, default_unit, default_usage, default_frequency_per_day, active, deleted) VALUES
('b0000001-1111-1111-1111-111111111111', 'MED-001', 'Paracetamol 500mg', 'Paracetamol', 'Viên', 'Uống sau ăn', 2, true, false),
('b0000002-2222-2222-2222-222222222222', 'MED-002', 'Amoxicillin 500mg', 'Amoxicillin', 'Viên', 'Uống sau ăn', 3, true, false),
('b0000003-3333-3333-3333-333333333333', 'MED-003', 'Oresol', 'Glucose, Natri, Kali', 'Gói', 'Pha với 200ml nước', 3, true, false);

-- ===================================================================
-- ICD-10 Codes
-- ===================================================================
INSERT INTO icd_10_codes (code, name, description, category, deleted) VALUES
('J00', 'Viêm mũi họng cấp [cảm lạnh thông thường]', 'Viêm mũi họng cấp tính do virus', 'Bệnh hệ hô hấp', false),
('J02.9', 'Viêm họng cấp, không chỉ định', 'Viêm họng cấp tính chưa rõ nguyên nhân', 'Bệnh hệ hô hấp', false),
('A09', 'Tiêu chảy và viêm dạ dày ruột do nhiễm khuẩn', 'Tiêu chảy cấp', 'Bệnh nhiễm trùng', false);

-- ===================================================================
-- Account (Thêm Doctor, Nurse, Receptionist, Cashier)
-- ===================================================================
INSERT INTO account (account_id, created_at, is_active, password_hash, role, username) VALUES
('30000014-4444-4444-4444-444444444444', NOW(), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'doctor1'),
('30000015-5555-5555-5555-555555555555', NOW(), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'nurse1'),
('30000016-6666-6666-6666-666666666666', NOW(), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'receptionist1'),
('30000017-7777-7777-7777-777777777777', NOW(), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'cashier1');

-- ===================================================================
-- Profile (Thêm profile cho các account trên)
-- ===================================================================
INSERT INTO profile (profile_id, account_id, created_at, updated_at, deleted, full_name, date_of_birth, gender, phone, email, address, blood_type) VALUES
('20000011-1111-1111-1111-111111111111', '30000014-4444-4444-4444-444444444444', NOW(), NOW(), false, 'Nguyễn Hoàng Minh', '1980-05-10', 'MALE', '0913517624', 'hoangminh.nguyen@cares.vn', 'Ba Đình, Hà Nội', NULL),
('20000012-2222-2222-2222-222222222222', '30000015-5555-5555-5555-555555555555', NOW(), NOW(), false, 'Trần Ngọc Hân', '1990-08-20', 'FEMALE', '0924186357', 'ngochan.tran@cares.vn', 'Đống Đa, Hà Nội', NULL),
('20000013-3333-3333-3333-333333333333', '30000016-6666-6666-6666-666666666666', NOW(), NOW(), false, 'Lê Quốc Bảo', '1995-12-01', 'MALE', '0935271468', 'quocbao.le@cares.vn', 'Hai Bà Trưng, Hà Nội', NULL),
('20000014-4444-4444-4444-444444444444', '30000017-7777-7777-7777-777777777777', NOW(), NOW(), false, 'Phạm Thùy Dương', '1992-03-15', 'FEMALE', '0946382517', 'thuyduong.pham@cares.vn', 'Hoàng Mai, Hà Nội', NULL);

-- ===================================================================
-- StaffInfo (Thêm staff_info cho các profile trên)
-- ===================================================================
INSERT INTO staff_info (staff_id, created_at, updated_at, deleted, profile_id, staff_code, system_role, national_id, highest_degree, university, license_number, specialization_id, department_id) VALUES
('90000010-3333-3333-3333-333333333333', NOW(), NOW(), false, '20000011-1111-1111-1111-111111111111', 'STF-DOC-001', 'DOCTOR', '001080123456', 'Bác sĩ chuyên khoa', 'Đại học Y Hà Nội', 'CCHN-12345', '00000001-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333'),
('90000011-4444-4444-4444-444444444444', NOW(), NOW(), false, '20000012-2222-2222-2222-222222222222', 'STF-NUR-001', 'NURSE', '001090123456', 'Cử nhân điều dưỡng', 'Đại học Y Dược', 'CCHN-23456', NULL, '44444444-4444-4444-4444-444444444444'),
('90000012-5555-5555-5555-555555555555', NOW(), NOW(), false, '20000013-3333-3333-3333-333333333333', 'STF-REC-001', 'RECEPTIONIST', '001095123456', NULL, NULL, NULL, NULL, NULL),
('90000013-6666-6666-6666-666666666666', NOW(), NOW(), false, '20000014-4444-4444-4444-444444444444', 'STF-CAS-001', 'CASHIER', '001092123456', NULL, NULL, NULL, NULL, NULL);

-- Phòng khám cần bác sĩ phụ trách để hệ thống tạo được hàng chờ khám.
UPDATE department
SET head_doctor_id = '90000010-3333-3333-3333-333333333333'
WHERE department_id = '33333333-3333-3333-3333-333333333333';

-- Năng lực nhân sự; các thông tin chứng chỉ được để trống theo nghiệp vụ.
INSERT INTO staff_capability (
    staff_capability_id, created_at, updated_at, deleted,
    staff_id, capability_id, certificate_number, issued_date, expiry_date,
    issuing_organization, status
) VALUES
('a0000001-1111-1111-1111-111111111111', NOW(), NOW(), false,
 '90000011-4444-4444-4444-444444444444', 'ca000001-0000-0000-0000-000000000001',
 NULL, NULL, NULL, NULL, 'ACTIVE');

-- ===================================================================
-- Shift Config
-- ===================================================================
INSERT INTO shift_config (shift_id, created_at, updated_at, deleted, name, start_time, end_time, is_active) VALUES
('70000001-1111-1111-1111-111111111111', NOW(), NOW(), false, 'Ca Sáng', '00:00', '08:00', true),
('70000002-2222-2222-2222-222222222222', NOW(), NOW(), false, 'Ca Chiều', '08:00', '16:00', true),
('70000003-3333-3333-3333-333333333333', NOW(), NOW(), false, 'Ca Tối', '16:00', '23:59:59', true);

-- Thông tin công khai và pháp lý của phòng khám (một bản ghi duy nhất).
INSERT INTO clinic_information (
    clinic_information_id, created_at, updated_at, deleted, clinic_name, legal_name,
    tax_code, operating_license, short_description, support_email, phone, address,
    website_url, facebook_url, youtube_url, zalo_url, latitude, longitude
) VALUES (
    '00000000-0000-0000-0000-000000000100', NOW(), NOW(), false,
    'Phòng khám CareS', 'Công ty TNHH Phòng khám CareS', '0101234567', '000123/HNO-GPHD',
    'Phòng khám đa khoa cung cấp dịch vụ chăm sóc sức khỏe chất lượng và thuận tiện.',
    'phongkhamcares@gmail.com', '0968161266',
    'Thôn 1, Canh Nậu, Thạch Thất, Hà Nội', NULL,
    'https://www.facebook.com/profile.php?id=61593125259676', NULL, NULL,
    21.0128000, 105.5259000
)
ON CONFLICT (clinic_information_id) DO NOTHING;
