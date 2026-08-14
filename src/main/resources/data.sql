-- ===================================================================
-- data.sql - bo du lieu demo day du cho he thong CareS
-- Cac bang nghiep vu chinh co xap xi 20 ban ghi; danh muc dac thu
-- (chuyen khoa, phong, nang luc, ca lam viec) giu so luong sat thuc te.
--
-- Tai khoan seed:
--   admin         / 88888888  (BCrypt)
--   clinicmanager / 88888888  (BCrypt)
--   doctor1 / doctor_lab / doctor8 / doctor_xray / 88888888
-- ===================================================================

TRUNCATE TABLE
    audit_log,
    chat_messages, chat_sessions, feedback_target, icd_10_selections,
    insurance_rule, invoice_item, notification, payment_transaction,
    prescription_item, staff_schedule, staff_schedule_template,
    test_result, test_request, vital_signs, medical_record, queue_ticket,
    invoice, customer_visit, appointment_services, appointment,
    department_capability, staff_capability, staff_info, medical_service,
    service_category, service_capability, medicine_catalog, icd_10_codes,
    insurance, department, specialization, shift_config, profile, account
RESTART IDENTITY CASCADE;

-- ===================================================================
-- Specialization (danh muc chuyen khoa - toi thieu de bo sung vao department)
-- ===================================================================
INSERT INTO specialization (specialization_id, created_at, updated_at, deleted, active, name, description) VALUES
('00000001-1111-1111-1111-111111111111', NOW(), NOW(), false, true, 'Nội khoa', 'Chẩn đoán và điều trị bệnh nội'),
('00000002-2222-2222-2222-222222222222', NOW(), NOW(), false, true, 'Nhi khoa', 'Khám và điều trị cho trẻ em'),
('00000003-3333-3333-3333-333333333333', NOW(), NOW(), false, true, 'Chẩn đoán hình ảnh', 'Siêu âm, X-quang và các kỹ thuật hình ảnh'),
('00000004-4444-4444-4444-444444444444', NOW(), NOW(), false, true, 'Da liễu', 'Khám và điều trị bệnh da liễu'),
('00000005-5555-5555-5555-555555555555', NOW(), NOW(), false, true, 'Tai Mũi Họng', 'Khám và điều trị tai, mũi, họng'),
('00000006-6666-6666-6666-666666666666', NOW(), NOW(), false, true, 'Khám tổng quát', 'Khám ban đầu và điều phối đa khoa'),
('00000007-7777-7777-7777-777777777777', NOW(), NOW(), false, true, 'Tim mạch', 'Khám và theo dõi bệnh tim mạch'),
('00000008-8888-8888-8888-888888888888', NOW(), NOW(), false, true, 'Sản phụ khoa', 'Khám sức khỏe phụ nữ và thai kỳ');

-- ===================================================================
-- Department (phòng khám - toi thieu de he thong hoat dong)
-- ===================================================================
INSERT INTO department (department_id, created_at, updated_at, deleted, room_code, name, status, department_type, specialization_id, description, head_doctor_id) VALUES
('33333333-3333-3333-3333-333333333333', NOW(), NOW(), false, 'EX-101', 'Phòng khám tổng quát 1', 'AVAILABLE', 'EXAMINATION', '00000006-6666-6666-6666-666666666666', 'Khám tổng quát và sàng lọc ban đầu', NULL),
('44444444-4444-4444-4444-444444444444', NOW(), NOW(), false, 'LAB-201', 'Phòng xét nghiệm huyết học', 'AVAILABLE', 'PARACLINICAL', NULL, 'Tiếp nhận và phân tích mẫu máu', NULL),
('55555555-5555-5555-5555-555555555555', NOW(), NOW(), false, 'IMG-301', 'Phòng siêu âm', 'AVAILABLE', 'PARACLINICAL', '00000003-3333-3333-3333-333333333333', 'Thực hiện siêu âm chẩn đoán', NULL),
('66666666-6666-6666-6666-666666666666', NOW(), NOW(), false, 'PED-102', 'Phòng khám Nhi', 'AVAILABLE', 'EXAMINATION', '00000002-2222-2222-2222-222222222222', 'Khám và chăm sóc trẻ em', NULL),
('77777777-7777-7777-7777-777777777777', NOW(), NOW(), false, 'INT-103', 'Phòng khám Nội', 'AVAILABLE', 'EXAMINATION', '00000001-1111-1111-1111-111111111111', 'Khám nội khoa', NULL),
('88888888-8888-8888-8888-888888888888', NOW(), NOW(), false, 'DER-104', 'Phòng khám Da liễu', 'AVAILABLE', 'EXAMINATION', '00000004-4444-4444-4444-444444444444', 'Khám da liễu', NULL),
('99999999-9999-9999-9999-999999999999', NOW(), NOW(), false, 'ENT-105', 'Phòng khám Tai Mũi Họng', 'AVAILABLE', 'EXAMINATION', '00000005-5555-5555-5555-555555555555', 'Khám tai mũi họng', NULL),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', NOW(), NOW(), false, 'CAR-106', 'Phòng khám Tim mạch', 'AVAILABLE', 'EXAMINATION', '00000007-7777-7777-7777-777777777777', 'Khám tim mạch', NULL),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', NOW(), NOW(), false, 'XR-302', 'Phòng X-quang', 'AVAILABLE', 'PARACLINICAL', '00000003-3333-3333-3333-333333333333', 'Chụp X-quang kỹ thuật số', NULL),
('cccccccc-cccc-cccc-cccc-cccccccccccc', NOW(), NOW(), false, 'LAB-202', 'Phòng xét nghiệm sinh hóa', 'AVAILABLE', 'PARACLINICAL', NULL, 'Phân tích sinh hóa và nước tiểu', NULL),
('dddddddd-dddd-dddd-dddd-dddddddddddd', NOW(), NOW(), false, 'OBG-107', 'Phòng khám Sản phụ khoa', 'AVAILABLE', 'EXAMINATION', '00000008-8888-8888-8888-888888888888', 'Khám sức khỏe phụ nữ và thai kỳ', NULL);

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
('ca000002-0000-0000-0000-000000000002', NOW(), NOW(), false, 'BIOCHEMISTRY', 'Xét nghiệm sinh hóa', 'Sinh hóa máu và chức năng cơ quan', true),
('ca000003-0000-0000-0000-000000000003', NOW(), NOW(), false, 'ULTRASOUND', 'Siêu âm', 'Các dịch vụ siêu âm hình ảnh', true),
('ca000004-0000-0000-0000-000000000004', NOW(), NOW(), false, 'XRAY', 'X-quang', 'Các dịch vụ X-quang', true),
('ca000005-0000-0000-0000-000000000005', NOW(), NOW(), false, 'URINALYSIS', 'Xét nghiệm nước tiểu', 'Phân tích nước tiểu thường quy', true),
('ca000006-0000-0000-0000-000000000006', NOW(), NOW(), false, 'ECG', 'Điện tim', 'Ghi và đọc điện tâm đồ', true),
('ca000007-0000-0000-0000-000000000007', NOW(), NOW(), false, 'MICROBIOLOGY', 'Xét nghiệm vi sinh', 'Soi và nuôi cấy vi sinh', true);

-- Link department -> capability
INSERT INTO department_capability (department_id, capability_id) VALUES
('44444444-4444-4444-4444-444444444444', 'ca000001-0000-0000-0000-000000000001'),
('44444444-4444-4444-4444-444444444444', 'ca000007-0000-0000-0000-000000000007'),
('55555555-5555-5555-5555-555555555555', 'ca000003-0000-0000-0000-000000000003'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'ca000004-0000-0000-0000-000000000004'),
('cccccccc-cccc-cccc-cccc-cccccccccccc', 'ca000002-0000-0000-0000-000000000002'),
('cccccccc-cccc-cccc-cccc-cccccccccccc', 'ca000005-0000-0000-0000-000000000005'),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'ca000006-0000-0000-0000-000000000006');

-- ===================================================================
-- MedicalService (dich vu y te - toi thieu: kham + xn + CDHA)
-- ===================================================================
INSERT INTO medical_service (
    service_id, service_code, created_at, updated_at, deleted, description,
    status, is_point_of_care, name, price, department_type, duration_minutes,
    workflow_priority, requires_doctor_order, requires_return_to_doctor,
    requires_specimen, result_wait_minutes, allow_customer_booking,
    minimum_age, maximum_age, allowed_gender, department_id,
    required_specialization_id, required_capability_id
) VALUES
('40000001-0000-0000-0000-000000000001', 'KHB001', NOW(), NOW(), false, 'Khám sức khỏe tổng quát', 'ACTIVE', false, 'Khám bệnh tổng quát', 200000, 'EXAMINATION', 30, 10, false, false, false, 0, true, 0, 120, NULL, NULL, '00000006-6666-6666-6666-666666666666', NULL),
('40000002-0000-0000-0000-000000000002', 'KHB002', NOW(), NOW(), false, 'Khám và tư vấn bệnh nội khoa', 'ACTIVE', false, 'Khám Nội khoa', 220000, 'EXAMINATION', 30, 20, false, false, false, 0, true, 16, 120, NULL, NULL, '00000001-1111-1111-1111-111111111111', NULL),
('40000003-0000-0000-0000-000000000003', 'KHB003', NOW(), NOW(), false, 'Khám trẻ em từ sơ sinh đến 15 tuổi', 'ACTIVE', false, 'Khám Nhi khoa', 220000, 'EXAMINATION', 30, 15, false, false, false, 0, true, 0, 15, NULL, NULL, '00000002-2222-2222-2222-222222222222', NULL),
('40000004-0000-0000-0000-000000000004', 'KHB004', NOW(), NOW(), false, 'Khám bệnh da và tư vấn chăm sóc da', 'ACTIVE', false, 'Khám Da liễu', 230000, 'EXAMINATION', 30, 25, false, false, false, 0, true, 0, 120, NULL, NULL, '00000004-4444-4444-4444-444444444444', NULL),
('40000005-0000-0000-0000-000000000005', 'KHB005', NOW(), NOW(), false, 'Khám tai, mũi và họng', 'ACTIVE', false, 'Khám Tai Mũi Họng', 230000, 'EXAMINATION', 30, 25, false, false, false, 0, true, 0, 120, NULL, NULL, '00000005-5555-5555-5555-555555555555', NULL),
('40000006-0000-0000-0000-000000000006', 'KHB006', NOW(), NOW(), false, 'Khám và đánh giá nguy cơ tim mạch', 'ACTIVE', false, 'Khám Tim mạch', 280000, 'EXAMINATION', 40, 30, false, false, false, 0, true, 16, 120, NULL, NULL, '00000007-7777-7777-7777-777777777777', NULL),
('40000007-0000-0000-0000-000000000007', 'KHB007', NOW(), NOW(), false, 'Khám sức khỏe phụ nữ', 'ACTIVE', false, 'Khám Sản phụ khoa', 280000, 'EXAMINATION', 40, 30, false, false, false, 0, true, 16, 60, 'FEMALE', NULL, '00000008-8888-8888-8888-888888888888', NULL),
('40000008-0000-0000-0000-000000000008', 'XN001', NOW(), NOW(), false, 'Tổng phân tích tế bào máu ngoại vi', 'ACTIVE', false, 'Xét nghiệm công thức máu', 120000, 'PARACLINICAL', 15, 40, false, false, true, 45, true, 0, 120, NULL, NULL, NULL, 'ca000001-0000-0000-0000-000000000001'),
('40000009-0000-0000-0000-000000000009', 'XN002', NOW(), NOW(), false, 'Định lượng glucose máu', 'ACTIVE', false, 'Xét nghiệm đường huyết', 70000, 'PARACLINICAL', 10, 45, false, false, true, 30, true, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('40000010-0000-0000-0000-000000000010', 'XN003', NOW(), NOW(), false, 'Đánh giá chức năng gan', 'ACTIVE', false, 'Xét nghiệm AST - ALT', 140000, 'PARACLINICAL', 15, 46, true, true, true, 60, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('40000011-0000-0000-0000-000000000011', 'XN004', NOW(), NOW(), false, 'Đánh giá chức năng thận', 'ACTIVE', false, 'Xét nghiệm Ure - Creatinin', 150000, 'PARACLINICAL', 15, 47, true, true, true, 60, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('40000012-0000-0000-0000-000000000012', 'XN005', NOW(), NOW(), false, 'Tổng phân tích nước tiểu', 'ACTIVE', false, 'Xét nghiệm nước tiểu', 90000, 'PARACLINICAL', 10, 48, false, false, true, 30, true, 0, 120, NULL, NULL, NULL, 'ca000005-0000-0000-0000-000000000005'),
('40000013-0000-0000-0000-000000000013', 'XN006', NOW(), NOW(), false, 'Đánh giá mỡ máu', 'ACTIVE', false, 'Xét nghiệm bộ mỡ máu', 190000, 'PARACLINICAL', 15, 49, true, true, true, 60, false, 16, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('40000014-0000-0000-0000-000000000014', 'XN007', NOW(), NOW(), false, 'Xét nghiệm CRP định lượng', 'ACTIVE', false, 'Xét nghiệm CRP', 130000, 'PARACLINICAL', 15, 50, true, true, true, 60, false, 0, 120, NULL, NULL, NULL, 'ca000001-0000-0000-0000-000000000001'),
('40000015-0000-0000-0000-000000000015', 'CDHA001', NOW(), NOW(), false, 'Khảo sát tổng quát ổ bụng', 'ACTIVE', false, 'Siêu âm ổ bụng', 250000, 'PARACLINICAL', 20, 55, false, false, false, 10, true, 0, 120, NULL, NULL, NULL, 'ca000003-0000-0000-0000-000000000003'),
('40000016-0000-0000-0000-000000000016', 'CDHA002', NOW(), NOW(), false, 'Siêu âm tuyến giáp', 'ACTIVE', false, 'Siêu âm tuyến giáp', 220000, 'PARACLINICAL', 20, 56, true, true, false, 10, false, 0, 120, NULL, NULL, NULL, 'ca000003-0000-0000-0000-000000000003'),
('40000017-0000-0000-0000-000000000017', 'CDHA003', NOW(), NOW(), false, 'Chụp ngực thẳng kỹ thuật số', 'ACTIVE', false, 'X-quang ngực thẳng', 180000, 'PARACLINICAL', 15, 57, true, true, false, 15, false, 6, 120, NULL, NULL, NULL, 'ca000004-0000-0000-0000-000000000004'),
('40000018-0000-0000-0000-000000000018', 'CDHA004', NOW(), NOW(), false, 'Chụp cột sống thắt lưng', 'ACTIVE', false, 'X-quang cột sống thắt lưng', 220000, 'PARACLINICAL', 20, 58, true, true, false, 15, false, 16, 120, NULL, NULL, NULL, 'ca000004-0000-0000-0000-000000000004'),
('40000019-0000-0000-0000-000000000019', 'TDCN001', NOW(), NOW(), false, 'Điện tâm đồ 12 chuyển đạo', 'ACTIVE', false, 'Điện tim thường', 150000, 'PARACLINICAL', 15, 52, true, true, false, 5, false, 16, 120, NULL, NULL, NULL, 'ca000006-0000-0000-0000-000000000006'),
('40000020-0000-0000-0000-000000000020', 'XN008', NOW(), NOW(), false, 'Soi tươi tìm nấm da', 'INACTIVE', false, 'Soi tươi vi nấm', 110000, 'PARACLINICAL', 15, 60, true, true, true, 45, false, 0, 120, NULL, NULL, NULL, 'ca000007-0000-0000-0000-000000000007');

-- Link profile -> account
UPDATE profile SET account_id = '30000012-2222-2222-2222-222222222222' WHERE profile_id = '20000009-9999-9999-9999-999999999999';
UPDATE profile SET account_id = '30000013-3333-3333-3333-333333333333' WHERE profile_id = '20000010-0000-0000-0000-000000000001';

-- ===================================================================
-- StaffInfo (thong tin nhan vien) - admin + clinic manager
-- ===================================================================
INSERT INTO staff_info (staff_id, created_at, updated_at, deleted, profile_id, staff_code, system_role, national_id, bank_account, highest_degree, university, license_number, specialization_id, department_id) VALUES
                                                                                                                                                                                                                     ('90000008-1111-1111-1111-111111111111', NOW(), NOW(), false, '20000009-9999-9999-9999-999999999999', 'STF-ADM-001', 'ADMIN', '9123456789', NULL, NULL, NULL, NULL, NULL, NULL),
                                                                                                                                                                                                                     ('90000009-2222-2222-2222-222222222222', NOW(), NOW(), false, '20000010-0000-0000-0000-000000000001', 'STF-CLM-001', 'CLINIC_MANAGER', '9223456789', NULL, NULL, NULL, NULL, NULL, '33333333-3333-3333-3333-333333333333');

-- ===================================================================
-- Medicine Catalog
-- ===================================================================
INSERT INTO medicine_catalog (medicine_id, medicine_code, name, active_ingredient, default_unit, default_usage, default_frequency_per_day, active, deleted) VALUES
('b0000000-0000-0000-0000-000000000001', 'MED-001', 'Paracetamol 500mg', 'Paracetamol', 'Viên', 'Uống sau ăn', 2, true, false),
('b0000000-0000-0000-0000-000000000002', 'MED-002', 'Amoxicillin 500mg', 'Amoxicillin', 'Viên', 'Uống sau ăn', 3, true, false),
('b0000000-0000-0000-0000-000000000003', 'MED-003', 'Oresol', 'Glucose, Natri, Kali', 'Gói', 'Pha đúng lượng nước hướng dẫn', 3, true, false),
('b0000000-0000-0000-0000-000000000004', 'MED-004', 'Cetirizine 10mg', 'Cetirizine', 'Viên', 'Uống buổi tối', 1, true, false),
('b0000000-0000-0000-0000-000000000005', 'MED-005', 'Loratadine 10mg', 'Loratadine', 'Viên', 'Uống sau ăn', 1, true, false),
('b0000000-0000-0000-0000-000000000006', 'MED-006', 'Omeprazole 20mg', 'Omeprazole', 'Viên', 'Uống trước ăn sáng 30 phút', 1, true, false),
('b0000000-0000-0000-0000-000000000007', 'MED-007', 'Pantoprazole 40mg', 'Pantoprazole', 'Viên', 'Uống trước ăn sáng', 1, true, false),
('b0000000-0000-0000-0000-000000000008', 'MED-008', 'Amlodipine 5mg', 'Amlodipine', 'Viên', 'Uống cùng một giờ mỗi ngày', 1, true, false),
('b0000000-0000-0000-0000-000000000009', 'MED-009', 'Losartan 50mg', 'Losartan', 'Viên', 'Uống theo chỉ định bác sĩ', 1, true, false),
('b0000000-0000-0000-0000-000000000010', 'MED-010', 'Metformin 500mg', 'Metformin', 'Viên', 'Uống trong hoặc sau bữa ăn', 2, true, false),
('b0000000-0000-0000-0000-000000000011', 'MED-011', 'Atorvastatin 20mg', 'Atorvastatin', 'Viên', 'Uống buổi tối', 1, true, false),
('b0000000-0000-0000-0000-000000000012', 'MED-012', 'Azithromycin 500mg', 'Azithromycin', 'Viên', 'Uống theo đơn', 1, true, false),
('b0000000-0000-0000-0000-000000000013', 'MED-013', 'Cefuroxime 500mg', 'Cefuroxime', 'Viên', 'Uống sau ăn', 2, true, false),
('b0000000-0000-0000-0000-000000000014', 'MED-014', 'Salbutamol 2mg', 'Salbutamol', 'Viên', 'Dùng theo chỉ định', 2, true, false),
('b0000000-0000-0000-0000-000000000015', 'MED-015', 'Acetylcysteine 200mg', 'Acetylcysteine', 'Gói', 'Hòa tan trong nước', 3, true, false),
('b0000000-0000-0000-0000-000000000016', 'MED-016', 'Diosmectite 3g', 'Diosmectite', 'Gói', 'Pha với nước, uống xa bữa ăn', 3, true, false),
('b0000000-0000-0000-0000-000000000017', 'MED-017', 'Ibuprofen 400mg', 'Ibuprofen', 'Viên', 'Uống sau ăn', 2, true, false),
('b0000000-0000-0000-0000-000000000018', 'MED-018', 'Vitamin C 500mg', 'Ascorbic acid', 'Viên', 'Uống sau ăn', 1, true, false),
('b0000000-0000-0000-0000-000000000019', 'MED-019', 'Natri clorid 0,9%', 'Sodium chloride', 'Chai', 'Rửa mũi theo hướng dẫn', 3, true, false),
('b0000000-0000-0000-0000-000000000020', 'MED-020', 'Hydrocortisone 1%', 'Hydrocortisone', 'Tuýp', 'Bôi lớp mỏng vùng tổn thương', 2, true, false);

-- ===================================================================
-- ICD-10 Codes
-- ===================================================================
INSERT INTO icd_10_codes (code, name, description, category, deleted) VALUES
('J00', 'Viêm mũi họng cấp', 'Cảm lạnh thông thường', 'Bệnh hệ hô hấp', false),
('J02.9', 'Viêm họng cấp, không chỉ định', 'Viêm họng cấp chưa rõ nguyên nhân', 'Bệnh hệ hô hấp', false),
('J03.9', 'Viêm amidan cấp', 'Viêm amidan cấp chưa xác định tác nhân', 'Bệnh hệ hô hấp', false),
('J06.9', 'Nhiễm khuẩn hô hấp trên cấp', 'Nhiễm khuẩn đường hô hấp trên', 'Bệnh hệ hô hấp', false),
('J20.9', 'Viêm phế quản cấp', 'Viêm phế quản cấp chưa xác định tác nhân', 'Bệnh hệ hô hấp', false),
('I10', 'Tăng huyết áp vô căn', 'Tăng huyết áp nguyên phát', 'Bệnh hệ tuần hoàn', false),
('I25.1', 'Bệnh tim do xơ vữa', 'Bệnh tim thiếu máu cục bộ mạn', 'Bệnh hệ tuần hoàn', false),
('E11.9', 'Đái tháo đường típ 2', 'Đái tháo đường không biến chứng', 'Bệnh nội tiết', false),
('E78.5', 'Rối loạn lipid máu', 'Tăng lipid máu chưa xác định', 'Bệnh nội tiết', false),
('K21.9', 'Trào ngược dạ dày thực quản', 'Không kèm viêm thực quản', 'Bệnh hệ tiêu hóa', false),
('K29.7', 'Viêm dạ dày', 'Viêm dạ dày chưa xác định', 'Bệnh hệ tiêu hóa', false),
('A09', 'Viêm dạ dày ruột do nhiễm khuẩn', 'Tiêu chảy cấp nghi nhiễm khuẩn', 'Bệnh nhiễm trùng', false),
('L20.9', 'Viêm da cơ địa', 'Viêm da cơ địa chưa xác định', 'Bệnh da liễu', false),
('L30.9', 'Viêm da không đặc hiệu', 'Tổn thương viêm da chưa phân loại', 'Bệnh da liễu', false),
('H10.9', 'Viêm kết mạc', 'Viêm kết mạc chưa xác định', 'Bệnh mắt', false),
('H66.9', 'Viêm tai giữa', 'Viêm tai giữa chưa xác định', 'Bệnh tai', false),
('M54.5', 'Đau thắt lưng', 'Đau vùng cột sống thắt lưng', 'Bệnh cơ xương khớp', false),
('R10.4', 'Đau bụng khác', 'Đau bụng chưa xác định nguyên nhân', 'Triệu chứng', false),
('R50.9', 'Sốt không rõ nguyên nhân', 'Sốt chưa xác định nguyên nhân', 'Triệu chứng', false),
('Z00.0', 'Khám sức khỏe tổng quát', 'Khám định kỳ người không có triệu chứng', 'Yếu tố sức khỏe', false);

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
                                                                                                                                                       ('20000011-1111-1111-1111-111111111111', '30000014-4444-4444-4444-444444444444', NOW(), NOW(), false, 'Bác sĩ Nguyễn Văn A', '1980-05-10', 'MALE', '0911111111', 'doctor1@example.com', 'Hà Nội', NULL),
                                                                                                                                                       ('20000012-2222-2222-2222-222222222222', '30000015-5555-5555-5555-555555555555', NOW(), NOW(), false, 'Y tá Trần Thị B', '1990-08-20', 'FEMALE', '0922222222', 'nurse1@example.com', 'Hà Nội', NULL),
                                                                                                                                                       ('20000013-3333-3333-3333-333333333333', '30000016-6666-6666-6666-666666666666', NOW(), NOW(), false, 'Lễ tân Lê Văn C', '1995-12-01', 'MALE', '0933333333', 'receptionist1@example.com', 'Hà Nội', NULL),
                                                                                                                                                       ('20000014-4444-4444-4444-444444444444', '30000017-7777-7777-7777-777777777777', NOW(), NOW(), false, 'Thu ngân Phạm Thị D', '1992-03-15', 'FEMALE', '0944444444', 'cashier1@example.com', 'Hà Nội', NULL);

-- ===================================================================
-- StaffInfo (Thêm staff_info cho các profile trên)
-- ===================================================================
INSERT INTO staff_info (staff_id, created_at, updated_at, deleted, profile_id, staff_code, system_role, national_id, bank_account, highest_degree, university, license_number, specialization_id, department_id) VALUES
                                                                                                                                                                                                                     ('90000010-3333-3333-3333-333333333333', NOW(), NOW(), false, '20000011-1111-1111-1111-111111111111', 'STF-DOC-001', 'DOCTOR', '001080123456', NULL, 'Bác sĩ chuyên khoa', 'Đại học Y Hà Nội', 'CCHN-12345', '00000006-6666-6666-6666-666666666666', '33333333-3333-3333-3333-333333333333'),
                                                                                                                                                                                                                     ('90000011-4444-4444-4444-444444444444', NOW(), NOW(), false, '20000012-2222-2222-2222-222222222222', 'STF-NUR-001', 'NURSE', '001090123456', NULL, 'Cử nhân điều dưỡng', 'Đại học Y Dược', 'CCHN-23456', NULL, '44444444-4444-4444-4444-444444444444'),
                                                                                                                                                                                                                     ('90000012-5555-5555-5555-555555555555', NOW(), NOW(), false, '20000013-3333-3333-3333-333333333333', 'STF-REC-001', 'RECEPTIONIST', '001095123456', NULL, NULL, NULL, NULL, NULL, NULL),
                                                                                                                                                                                                                     ('90000013-6666-6666-6666-666666666666', NOW(), NOW(), false, '20000014-4444-4444-4444-444444444444', 'STF-CAS-001', 'CASHIER', '001092123456', NULL, NULL, NULL, NULL, NULL, NULL);

-- Bo sung 14 nhan su; cung 2 bac si can lam sang o duoi tao thanh
-- 22 tai khoan nhan vien de phu day cac vai tro va phong demo.
-- Tat ca tai khoan demo dung mat khau: 88888888.
INSERT INTO account (account_id, created_at, is_active, password_hash, role, username)
SELECT format('31000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       NOW(), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF',
       CASE
           WHEN i <= 7 THEN 'doctor' || (i + 1)
           WHEN i <= 10 THEN 'nurse' || (i - 6)
           WHEN i <= 12 THEN 'receptionist' || (i - 9)
           ELSE 'cashier' || (i - 11)
       END
FROM generate_series(1, 14) AS g(i);

-- ===================================================================
-- Hai tai khoan bac si can lam sang con thieu trong bo du lieu goc.
-- Bon nhom demo: doctor1 (kham), doctor_lab (xet nghiem),
-- doctor_biochem (sinh hoa), doctor8 (sieu am), doctor_xray (X-quang).
-- Mat khau chung: 88888888.
-- ===================================================================
INSERT INTO account (account_id, created_at, is_active, password_hash, role, username) VALUES
('33000000-0000-0000-0000-000000000002', NOW(), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'doctor_lab'),
('33000000-0000-0000-0000-000000000003', NOW(), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'doctor_biochem'),
('33000000-0000-0000-0000-000000000004', NOW(), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'doctor_xray');

INSERT INTO profile (
    profile_id, account_id, created_at, updated_at, deleted,
    full_name, date_of_birth, gender, phone, email, address, blood_type
) VALUES
('23000000-0000-0000-0000-000000000002', '33000000-0000-0000-0000-000000000002', NOW(), NOW(), false, 'Bác sĩ Nguyễn Hải Yến', '1986-07-22', 'FEMALE', '0968000002', 'doctor.lab@cares.vn', 'Hà Nội', NULL),
('23000000-0000-0000-0000-000000000003', '33000000-0000-0000-0000-000000000003', NOW(), NOW(), false, 'Bác sĩ Trần Minh Sinh', '1985-09-12', 'MALE', '0968000003', 'doctor.biochem@cares.vn', 'Hà Nội', NULL),
('23000000-0000-0000-0000-000000000004', '33000000-0000-0000-0000-000000000004', NOW(), NOW(), false, 'Bác sĩ Lê Thu Phương', '1987-01-18', 'FEMALE', '0968000004', 'doctor.xray@cares.vn', 'Hà Nội', NULL);

INSERT INTO staff_info (
    staff_id, created_at, updated_at, deleted, profile_id, staff_code,
    system_role, national_id, bank_account, highest_degree, university,
    license_number, specialization_id, department_id
) VALUES
('93000000-0000-0000-0000-000000000002', NOW(), NOW(), false, '23000000-0000-0000-0000-000000000002', 'STF-DOC-LAB', 'DOCTOR', '001086000002', NULL, 'Bác sĩ chuyên khoa xét nghiệm', 'Đại học Y Hà Nội', 'CCHN-LAB-001', NULL, '44444444-4444-4444-4444-444444444444'),
('93000000-0000-0000-0000-000000000003', NOW(), NOW(), false, '23000000-0000-0000-0000-000000000003', 'STF-DOC-BIO', 'DOCTOR', '001085000003', NULL, 'Bác sĩ chuyên khoa xét nghiệm', 'Đại học Y Hà Nội', 'CCHN-BIO-001', NULL, 'cccccccc-cccc-cccc-cccc-cccccccccccc'),
('93000000-0000-0000-0000-000000000004', NOW(), NOW(), false, '23000000-0000-0000-0000-000000000004', 'STF-DOC-XRAY', 'DOCTOR', '001087000004', NULL, 'Bác sĩ chẩn đoán hình ảnh', 'Đại học Y Hà Nội', 'CCHN-XRAY-001', '00000003-3333-3333-3333-333333333333', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb');

INSERT INTO profile (
    profile_id, account_id, created_at, updated_at, deleted,
    full_name, date_of_birth, gender, phone, email, address, blood_type
)
SELECT format('21000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       format('31000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       NOW(), NOW(), false,
       (ARRAY[
           'Bác sĩ Trần Minh Quân', 'Bác sĩ Nguyễn Thu Hà', 'Bác sĩ Lê Hoàng Nam',
           'Bác sĩ Phạm Ngọc Lan', 'Bác sĩ Đỗ Anh Tuấn', 'Bác sĩ Vũ Thanh Mai',
           'Bác sĩ Bùi Đức Long', 'Điều dưỡng Nguyễn Thị Hương',
           'Điều dưỡng Trần Thị Mai', 'Điều dưỡng Lê Quốc Việt',
           'Lễ tân Nguyễn Minh Anh', 'Lễ tân Trần Thu Trang',
           'Thu ngân Lê Thị Hạnh', 'Thu ngân Phạm Quốc Khánh'
       ])[i],
       (DATE '1978-01-01' + (i * 420) * INTERVAL '1 day')::date,
       CASE WHEN i IN (2,4,6,8,9,11,12,13) THEN 'FEMALE' ELSE 'MALE' END,
       '0971' || lpad(i::text, 6, '0'),
       CASE
           WHEN i <= 7 THEN 'doctor' || (i + 1) || '@cares.vn'
           WHEN i <= 10 THEN 'nurse' || (i - 6) || '@cares.vn'
           WHEN i <= 12 THEN 'receptionist' || (i - 9) || '@cares.vn'
           ELSE 'cashier' || (i - 11) || '@cares.vn'
       END,
       (ARRAY['Hà Nội','Hà Nội','Bắc Ninh','Hà Nội','Hưng Yên','Hà Nội','Hải Dương',
              'Hà Nội','Bắc Ninh','Hà Nội','Hà Nội','Hưng Yên','Hà Nội','Bắc Ninh'])[i],
       NULL
FROM generate_series(1, 14) AS g(i);

INSERT INTO staff_info (
    staff_id, created_at, updated_at, deleted, profile_id, staff_code,
    system_role, national_id, bank_account, highest_degree, university,
    license_number, specialization_id, department_id
)
SELECT format('91000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       NOW(), NOW(), false,
       format('21000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       CASE
           WHEN i <= 7 THEN 'STF-DOC-' || lpad((i + 1)::text, 3, '0')
           WHEN i <= 10 THEN 'STF-NUR-' || lpad((i - 6)::text, 3, '0')
           WHEN i <= 12 THEN 'STF-REC-' || lpad((i - 9)::text, 3, '0')
           ELSE 'STF-CAS-' || lpad((i - 11)::text, 3, '0')
       END,
       CASE WHEN i <= 7 THEN 'DOCTOR' WHEN i <= 10 THEN 'NURSE'
            WHEN i <= 12 THEN 'RECEPTIONIST' ELSE 'CASHIER' END,
       '0012' || lpad(i::text, 8, '0'), NULL,
       CASE WHEN i <= 7 THEN 'Bác sĩ chuyên khoa I'
            WHEN i <= 10 THEN 'Cử nhân điều dưỡng' ELSE NULL END,
       CASE WHEN i <= 7 THEN 'Đại học Y Hà Nội'
            WHEN i <= 10 THEN 'Đại học Điều dưỡng Nam Định' ELSE NULL END,
       CASE WHEN i <= 7 THEN 'CCHN-DEMO-' || lpad((i + 1)::text, 3, '0') ELSE NULL END,
       CASE i
           WHEN 1 THEN '00000001-1111-1111-1111-111111111111'::uuid
           WHEN 2 THEN '00000002-2222-2222-2222-222222222222'::uuid
           WHEN 3 THEN '00000004-4444-4444-4444-444444444444'::uuid
           WHEN 4 THEN '00000005-5555-5555-5555-555555555555'::uuid
           WHEN 5 THEN '00000007-7777-7777-7777-777777777777'::uuid
           WHEN 6 THEN '00000008-8888-8888-8888-888888888888'::uuid
           WHEN 7 THEN '00000003-3333-3333-3333-333333333333'::uuid
           ELSE NULL
       END,
       CASE i
           WHEN 1 THEN '77777777-7777-7777-7777-777777777777'::uuid
           WHEN 2 THEN '66666666-6666-6666-6666-666666666666'::uuid
           WHEN 3 THEN '88888888-8888-8888-8888-888888888888'::uuid
           WHEN 4 THEN '99999999-9999-9999-9999-999999999999'::uuid
           WHEN 5 THEN 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'::uuid
           WHEN 6 THEN '33333333-3333-3333-3333-333333333333'::uuid
           WHEN 7 THEN '55555555-5555-5555-5555-555555555555'::uuid
           WHEN 8 THEN '44444444-4444-4444-4444-444444444444'::uuid
           WHEN 9 THEN 'cccccccc-cccc-cccc-cccc-cccccccccccc'::uuid
           WHEN 10 THEN '55555555-5555-5555-5555-555555555555'::uuid
           ELSE NULL
       END
FROM generate_series(1, 14) AS g(i);

-- Bac si phu trach cho cac phong kham.
UPDATE department SET head_doctor_id = '90000010-3333-3333-3333-333333333333' WHERE department_id = '33333333-3333-3333-3333-333333333333';
UPDATE department SET head_doctor_id = '91000000-0000-0000-0000-000000000001' WHERE department_id = '77777777-7777-7777-7777-777777777777';
UPDATE department SET head_doctor_id = '91000000-0000-0000-0000-000000000002' WHERE department_id = '66666666-6666-6666-6666-666666666666';
UPDATE department SET head_doctor_id = '91000000-0000-0000-0000-000000000003' WHERE department_id = '88888888-8888-8888-8888-888888888888';
UPDATE department SET head_doctor_id = '91000000-0000-0000-0000-000000000004' WHERE department_id = '99999999-9999-9999-9999-999999999999';
UPDATE department SET head_doctor_id = '91000000-0000-0000-0000-000000000005' WHERE department_id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';
UPDATE department SET head_doctor_id = '91000000-0000-0000-0000-000000000006' WHERE department_id = 'dddddddd-dddd-dddd-dddd-dddddddddddd';
UPDATE department SET head_doctor_id = '93000000-0000-0000-0000-000000000002' WHERE department_id = '44444444-4444-4444-4444-444444444444';
UPDATE department SET head_doctor_id = '93000000-0000-0000-0000-000000000003' WHERE department_id = 'cccccccc-cccc-cccc-cccc-cccccccccccc';
UPDATE department SET head_doctor_id = '91000000-0000-0000-0000-000000000007' WHERE department_id = '55555555-5555-5555-5555-555555555555';
UPDATE department SET head_doctor_id = '93000000-0000-0000-0000-000000000004' WHERE department_id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb';

INSERT INTO staff_capability (
    staff_capability_id, created_at, updated_at, deleted, staff_id,
    capability_id, certificate_number, issued_date, expiry_date,
    issuing_organization, status
) VALUES
('a1000000-0000-0000-0000-000000000001', NOW(), NOW(), false, '90000011-4444-4444-4444-444444444444', 'ca000001-0000-0000-0000-000000000001', NULL, NULL, NULL, NULL, 'ACTIVE'),
('a1000000-0000-0000-0000-000000000002', NOW(), NOW(), false, '91000000-0000-0000-0000-000000000008', 'ca000001-0000-0000-0000-000000000001', NULL, NULL, NULL, NULL, 'ACTIVE'),
('a1000000-0000-0000-0000-000000000003', NOW(), NOW(), false, '91000000-0000-0000-0000-000000000008', 'ca000007-0000-0000-0000-000000000007', NULL, NULL, NULL, NULL, 'ACTIVE'),
('a1000000-0000-0000-0000-000000000004', NOW(), NOW(), false, '91000000-0000-0000-0000-000000000009', 'ca000002-0000-0000-0000-000000000002', NULL, NULL, NULL, NULL, 'ACTIVE'),
('a1000000-0000-0000-0000-000000000005', NOW(), NOW(), false, '91000000-0000-0000-0000-000000000009', 'ca000005-0000-0000-0000-000000000005', NULL, NULL, NULL, NULL, 'ACTIVE'),
('a1000000-0000-0000-0000-000000000006', NOW(), NOW(), false, '91000000-0000-0000-0000-000000000010', 'ca000003-0000-0000-0000-000000000003', NULL, NULL, NULL, NULL, 'ACTIVE'),
('a1000000-0000-0000-0000-000000000007', NOW(), NOW(), false, '91000000-0000-0000-0000-000000000007', 'ca000003-0000-0000-0000-000000000003', NULL, NULL, NULL, NULL, 'ACTIVE'),
('a1000000-0000-0000-0000-000000000008', NOW(), NOW(), false, '91000000-0000-0000-0000-000000000005', 'ca000006-0000-0000-0000-000000000006', NULL, NULL, NULL, NULL, 'ACTIVE');

INSERT INTO staff_capability (
    staff_capability_id, created_at, updated_at, deleted, staff_id,
    capability_id, certificate_number, issued_date, expiry_date,
    issuing_organization, status
) VALUES
('a2000000-0000-0000-0000-000000000001', NOW(), NOW(), false, '93000000-0000-0000-0000-000000000002', 'ca000001-0000-0000-0000-000000000001', NULL, NULL, NULL, NULL, 'ACTIVE'),
('a2000000-0000-0000-0000-000000000002', NOW(), NOW(), false, '93000000-0000-0000-0000-000000000002', 'ca000002-0000-0000-0000-000000000002', NULL, NULL, NULL, NULL, 'ACTIVE'),
('a2000000-0000-0000-0000-000000000003', NOW(), NOW(), false, '93000000-0000-0000-0000-000000000002', 'ca000005-0000-0000-0000-000000000005', NULL, NULL, NULL, NULL, 'ACTIVE'),
('a2000000-0000-0000-0000-000000000004', NOW(), NOW(), false, '93000000-0000-0000-0000-000000000003', 'ca000002-0000-0000-0000-000000000002', NULL, NULL, NULL, NULL, 'ACTIVE'),
('a2000000-0000-0000-0000-000000000005', NOW(), NOW(), false, '93000000-0000-0000-0000-000000000004', 'ca000004-0000-0000-0000-000000000004', NULL, NULL, NULL, NULL, 'ACTIVE');

-- ===================================================================
-- Shift Config
-- ===================================================================
INSERT INTO shift_config (shift_id, created_at, updated_at, deleted, name, start_time, end_time, is_active) VALUES
                                                                                                                ('70000001-1111-1111-1111-111111111111', NOW(), NOW(), false, 'Ca Sáng', '07:30', '11:30', true),
                                                                                                                ('70000002-2222-2222-2222-222222222222', NOW(), NOW(), false, 'Ca Chiều', '13:30', '17:30', true),
                                                                                                                ('70000003-3333-3333-3333-333333333333', NOW(), NOW(), false, 'Ca Tối', '17:30', '20:30', true);

-- ===================================================================
-- 20 benh nhan co tai khoan
-- ===================================================================
INSERT INTO account (account_id, created_at, is_active, password_hash, role, username)
SELECT format('32000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       NOW(), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m',
       'CUSTOMER', 'patient' || i
FROM generate_series(1, 20) AS g(i);


-- Lich truc bo sung cho bac si LAB va X-quang; doctor1/doctor8 da co lich mau.
INSERT INTO staff_schedule (
    schedule_id, created_at, updated_at, deleted, is_custom, note,
    status, work_date, shift_id, staff_id, template_id
) VALUES
('61100000-0000-0000-0000-000000000002', NOW(), NOW(), false, false, 'Trực phòng xét nghiệm', 'SCHEDULED', CURRENT_DATE, '70000001-1111-1111-1111-111111111111', '93000000-0000-0000-0000-000000000002', NULL),
('61100000-0000-0000-0000-000000000003', NOW(), NOW(), false, false, 'Trực phòng sinh hóa', 'SCHEDULED', CURRENT_DATE, '70000001-1111-1111-1111-111111111111', '93000000-0000-0000-0000-000000000003', NULL),
('61100000-0000-0000-0000-000000000004', NOW(), NOW(), false, false, 'Trực phòng X-quang', 'SCHEDULED', CURRENT_DATE, '70000002-2222-2222-2222-222222222222', '93000000-0000-0000-0000-000000000004', NULL);

INSERT INTO profile (
    profile_id, account_id, created_at, updated_at, deleted, patient_code,
    full_name, date_of_birth, gender, phone, email, address, blood_type,
    insurance_id, allergies, height, weight
)
SELECT format('22000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       format('32000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       NOW(), NOW(), false, 'BN-' || to_char(CURRENT_DATE, 'YY') || '-' || lpad(i::text, 5, '0'),
       (ARRAY[
           'Nguyễn Đức Cường', 'Trần Minh Anh', 'Lê Văn Khoa', 'Phạm Thị Huyền',
           'Hoàng Quốc Bảo', 'Vũ Thị Lan', 'Đặng Minh Tuấn', 'Nguyễn Ngọc Mai',
           'Trần Gia Hân', 'Lê Hoàng Phúc', 'Phạm Đức Anh', 'Bùi Thanh Thảo',
           'Đỗ Minh Khôi', 'Ngô Thu Trang', 'Dương Quốc Huy', 'Mai Phương Linh',
           'Phan Anh Khoa', 'Trịnh Mỹ Duyên', 'Lương Thành Đạt', 'Tạ Bảo Ngọc'
       ])[i],
       (ARRAY[
           DATE '1994-04-12', DATE '1988-11-03', DATE '1976-07-21', DATE '1995-02-18',
           DATE '1982-09-30', DATE '1999-06-14', DATE '1968-01-25', DATE '2001-12-08',
           DATE '2018-03-19', DATE '2014-10-05', DATE '1991-05-27', DATE '1985-08-16',
           DATE '1979-12-02', DATE '1997-07-11', DATE '2006-04-23', DATE '1993-09-09',
           DATE '1987-02-28', DATE '2000-01-17', DATE '1972-06-06', DATE '2016-11-29'
       ])[i],
       CASE WHEN i IN (2,4,6,8,9,12,14,16,18,20) THEN 'FEMALE' ELSE 'MALE' END,
       '0988' || lpad(i::text, 6, '0'),
       'patient' || i || '@example.com',
       (ARRAY[
           'Cầu Giấy, Hà Nội', 'Thanh Xuân, Hà Nội', 'Long Biên, Hà Nội', 'Hai Bà Trưng, Hà Nội',
           'Gia Lâm, Hà Nội', 'Nam Từ Liêm, Hà Nội', 'Ba Đình, Hà Nội', 'Hoàng Mai, Hà Nội',
           'Bắc Ninh', 'Hưng Yên', 'Hải Dương', 'Đống Đa, Hà Nội', 'Hà Đông, Hà Nội',
           'Tây Hồ, Hà Nội', 'Sóc Sơn, Hà Nội', 'Đông Anh, Hà Nội', 'Bắc Từ Liêm, Hà Nội',
           'Hoàn Kiếm, Hà Nội', 'Thanh Trì, Hà Nội', 'Bắc Giang'
       ])[i],
       (ARRAY['O_POSITIVE','A_POSITIVE','B_POSITIVE','AB_POSITIVE',NULL,
              'O_POSITIVE','A_NEGATIVE',NULL,'B_POSITIVE',NULL,
              'O_NEGATIVE','A_POSITIVE',NULL,'AB_NEGATIVE','B_POSITIVE',
              NULL,'O_POSITIVE','A_POSITIVE',NULL,'B_NEGATIVE'])[i],
       CASE WHEN i % 3 = 0 THEN 'DN401' || lpad(i::text, 9, '0') ELSE NULL END,
       CASE WHEN i IN (4,12) THEN 'Dị ứng Penicillin'
            WHEN i = 8 THEN 'Dị ứng hải sản' ELSE NULL END,
       CASE WHEN i IN (9,10,20) THEN 120 + i ELSE 155 + (i % 20) END,
       CASE WHEN i IN (9,10,20) THEN 22 + i ELSE 48 + (i % 30) END
FROM generate_series(1, 20) AS g(i);

-- ===================================================================
-- Bao hiem va quy tac ap dung (so luong nho dung voi thuc te cau hinh)
-- ===================================================================
INSERT INTO insurance (insurance_id, created_at, updated_at, deleted, code, name, description) VALUES
('62000000-0000-0000-0000-000000000001', NOW(), NOW(), false, 'BHYT', 'Bảo hiểm y tế', 'Quyền lợi BHYT theo kết quả xác minh'),
('62000000-0000-0000-0000-000000000002', NOW(), NOW(), false, 'BAOVIET', 'Bảo Việt', 'Bảo hiểm sức khỏe tư nhân'),
('62000000-0000-0000-0000-000000000003', NOW(), NOW(), false, 'PVI', 'PVI Care', 'Bảo hiểm sức khỏe doanh nghiệp');

INSERT INTO insurance_rule (rule_id, created_at, updated_at, deleted, department_type, discount_percent, insurance_id) VALUES
('62100000-0000-0000-0000-000000000001', NOW(), NOW(), false, 'EXAMINATION', 20, '62000000-0000-0000-0000-000000000001'),
('62100000-0000-0000-0000-000000000002', NOW(), NOW(), false, 'PARACLINICAL', 20, '62000000-0000-0000-0000-000000000001'),
('62100000-0000-0000-0000-000000000003', NOW(), NOW(), false, 'EXAMINATION', 15, '62000000-0000-0000-0000-000000000002'),
('62100000-0000-0000-0000-000000000004', NOW(), NOW(), false, 'PARACLINICAL', 10, '62000000-0000-0000-0000-000000000002'),
('62100000-0000-0000-0000-000000000005', NOW(), NOW(), false, 'EXAMINATION', 10, '62000000-0000-0000-0000-000000000003'),
('62100000-0000-0000-0000-000000000006', NOW(), NOW(), false, 'PARACLINICAL', 10, '62000000-0000-0000-0000-000000000003');

-- ===================================================================
-- 20 lich lam viec gan ngay hien tai
-- ===================================================================
INSERT INTO staff_schedule (
    schedule_id, created_at, updated_at, deleted, is_custom, note,
    status, work_date, shift_id, staff_id, template_id
)
SELECT format('61000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       NOW(), NOW(), false, false, 'Lịch làm việc mẫu',
       CASE WHEN i <= 4 THEN 'COMPLETED' ELSE 'SCHEDULED' END,
       CURRENT_DATE + ((i - 5) / 8),
       CASE (i % 3)
           WHEN 1 THEN '70000001-1111-1111-1111-111111111111'::uuid
           WHEN 2 THEN '70000002-2222-2222-2222-222222222222'::uuid
           ELSE '70000003-3333-3333-3333-333333333333'::uuid
       END,
       CASE
           WHEN i <= 14 THEN format('91000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid
           WHEN i = 15 THEN '90000010-3333-3333-3333-333333333333'::uuid
           WHEN i = 16 THEN '90000011-4444-4444-4444-444444444444'::uuid
           WHEN i = 17 THEN '90000012-5555-5555-5555-555555555555'::uuid
           WHEN i = 18 THEN '90000013-6666-6666-6666-666666666666'::uuid
           WHEN i = 19 THEN '90000008-1111-1111-1111-111111111111'::uuid
           ELSE '90000009-2222-2222-2222-222222222222'::uuid
       END,
       NULL
FROM generate_series(1, 20) AS g(i);

-- ===================================================================
-- 20 lich hen: cho xac nhan/check-in/huy/doi lich deu co du lieu demo
-- ===================================================================
INSERT INTO appointment (
    appointment_id, created_at, updated_at, deleted, scheduled_at, status,
    is_guest, customer_id, guest_full_name, guest_phone, guest_email,
    guest_age, guest_gender, guest_address, shift_name, shift_time, cancel_reason
)
SELECT format('51000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       NOW() - (i || ' hours')::interval, NOW(), false,
       CASE
           WHEN i <= 8 THEN CURRENT_DATE + ((i + 3) / 4) + (TIME '08:00' + ((i - 1) % 4) * INTERVAL '45 minutes')
           WHEN i <= 14 THEN CURRENT_DATE + (TIME '08:00' + (i - 9) * INTERVAL '35 minutes')
           WHEN i <= 16 THEN CURRENT_DATE - 1 + (TIME '14:00' + (i - 15) * INTERVAL '45 minutes')
           ELSE CURRENT_DATE + 2 + (TIME '09:00' + (i - 17) * INTERVAL '40 minutes')
       END,
       CASE WHEN i <= 8 THEN 'PENDING' WHEN i <= 16 THEN 'CHECKED_IN'
            WHEN i <= 18 THEN 'CANCELLED' ELSE 'RESCHEDULED' END,
       false,
       format('22000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       NULL, NULL, NULL, NULL, NULL, NULL,
       CASE WHEN i % 2 = 0 THEN 'Ca Chiều' ELSE 'Ca Sáng' END,
       CASE WHEN i % 2 = 0 THEN '13:30 - 17:30' ELSE '07:30 - 11:30' END,
       CASE WHEN i IN (17,18) THEN 'Khách hàng bận việc cá nhân' ELSE NULL END
FROM generate_series(1, 20) AS g(i);

INSERT INTO appointment_services (appointment_id, service_id)
SELECT format('51000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       ('400000' || lpad(service_no::text, 2, '0') || '-0000-0000-0000-' || lpad(service_no::text, 12, '0'))::uuid
FROM (
    SELECT i, CASE WHEN i <= 10 THEN ((i - 1) % 7) + 1 ELSE i - 3 END AS service_no
    FROM generate_series(1, 20) AS g(i)
) s;

-- ===================================================================
-- 20 luot kham: 8 luot tu lich hen va 12 luot tiep nhan truc tiep
-- ===================================================================
INSERT INTO customer_visit (
    visit_id, created_at, updated_at, deleted, check_in_time, check_out_time,
    status, appointment_id, checked_in_by, customer_id
)
SELECT format('52000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       NOW() - ((i + 2) || ' hours')::interval, NOW(), false,
       CASE WHEN i <= 8 THEN CURRENT_DATE + TIME '08:00' + (i - 1) * INTERVAL '25 minutes'
            ELSE CURRENT_DATE - ((i - 8) / 5) + TIME '08:00' + ((i - 9) % 5) * INTERVAL '40 minutes' END,
       CASE WHEN i BETWEEN 9 AND 18 THEN
            CURRENT_DATE - ((i - 8) / 5) + TIME '10:30' + ((i - 9) % 5) * INTERVAL '40 minutes'
            ELSE NULL END,
       CASE WHEN i <= 4 THEN 'IN_PROGRESS' WHEN i <= 8 THEN 'CHECKED_IN'
            WHEN i <= 18 THEN 'COMPLETED' WHEN i = 19 THEN 'CANCELLED' ELSE 'IN_PROGRESS' END,
       CASE WHEN i <= 8 THEN format('51000000-0000-0000-0000-%s', lpad((i + 8)::text, 12, '0'))::uuid ELSE NULL END,
       '90000012-5555-5555-5555-555555555555',
       format('22000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid
FROM generate_series(1, 20) AS g(i);

-- Tao du lieu hoa don -> thanh toan -> hang cho -> ho so -> can lam sang
-- sau khi toan bo khoa ngoai nen da ton tai.
-- ===================================================================
-- 20 hoa don gan voi 20 luot kham.
-- 18 hoa don da thanh toan, 1 da huy va 1 dang cho thanh toan.
-- ===================================================================
INSERT INTO invoice (
    invoice_id, invoice_code, customer_id, visit_id, medical_record_id,
    issue_date, due_date, subtotal, discount, tax, total_amount, paid_amount,
    status, note, issued_by, created_at, updated_at, deleted
)
SELECT format('53000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       'INV-' || to_char(CURRENT_DATE, 'YYYYMMDD') || '-' || lpad(i::text, 4, '0'),
       format('22000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       format('52000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       NULL, NOW() - ((20 - i) || ' hours')::interval, CURRENT_DATE + 1,
       ms.price, 0, 0, ms.price,
       CASE WHEN i <= 18 THEN ms.price ELSE 0 END,
       CASE WHEN i <= 18 THEN 'PAID' WHEN i = 19 THEN 'CANCELLED' ELSE 'PENDING' END,
       CASE WHEN i <= 18 THEN 'Đã thu đủ chi phí dịch vụ' WHEN i = 19 THEN 'Phiếu khám đã hủy' ELSE 'Chờ khách hàng thanh toán' END,
       '90000013-6666-6666-6666-666666666666', NOW(), NOW(), false
FROM generate_series(1, 20) AS g(i)
JOIN medical_service ms
  ON ms.service_id = format('400000%s-0000-0000-0000-%s',
       lpad((((i - 1) % 7) + 1)::text, 2, '0'),
       lpad((((i - 1) % 7) + 1)::text, 12, '0'))::uuid;

-- Moi hoa don co mot dich vu kham benh.
INSERT INTO invoice_item (
    item_id, invoice_id, service_id, service_snapshot, service_code_snapshot,
    unit_price, quantity, discount_percent, discount_amount, final_price,
    line_total, note, bhyt_fund, created_at, updated_at, deleted
)
SELECT format('54000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       format('53000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       ms.service_id, ms.name, ms.service_code, ms.price, 1, 0, 0,
       ms.price, ms.price, 'Dịch vụ khám ban đầu', 0, NOW(), NOW(), false
FROM generate_series(1, 20) AS g(i)
JOIN medical_service ms
  ON ms.service_id = format('400000%s-0000-0000-0000-%s',
       lpad((((i - 1) % 7) + 1)::text, 2, '0'),
       lpad((((i - 1) % 7) + 1)::text, 12, '0'))::uuid;

-- 15 dich vu can lam sang de demo du cac phong LAB/sieu am/X-quang/dien tim.
INSERT INTO invoice_item (
    item_id, invoice_id, service_id, service_snapshot, service_code_snapshot,
    unit_price, quantity, discount_percent, discount_amount, final_price,
    line_total, note, bhyt_fund, created_at, updated_at, deleted
)
SELECT format('54100000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       format('53000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       ms.service_id, ms.name, ms.service_code, ms.price, 1, 0, 0,
       ms.price, ms.price, 'Dịch vụ cận lâm sàng', 0, NOW(), NOW(), false
FROM generate_series(1, 15) AS g(i)
JOIN medical_service ms
  ON ms.service_id = format('400000%s-0000-0000-0000-%s',
       lpad((8 + ((i - 1) % 12))::text, 2, '0'),
       lpad((8 + ((i - 1) % 12))::text, 12, '0'))::uuid;

-- Cap nhat tong hoa don co them dich vu can lam sang.
UPDATE invoice inv
SET subtotal = inv.subtotal + ii.line_total,
    total_amount = inv.total_amount + ii.line_total,
    paid_amount = CASE WHEN inv.status = 'PAID' THEN inv.paid_amount + ii.line_total ELSE inv.paid_amount END,
    updated_at = NOW()
FROM invoice_item ii
WHERE ii.invoice_id = inv.invoice_id
  AND ii.item_id::text LIKE '54100000-%';

-- 18 giao dich thanh toan thanh cong.
INSERT INTO payment_transaction (
    transaction_id, invoice_id, transaction_code, amount, payment_method,
    status, paid_at, gateway_reference, note, received_by,
    created_at, updated_at, deleted
)
SELECT format('54200000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       inv.invoice_id, 'PAY-' || to_char(CURRENT_DATE, 'YYYYMMDD') || '-' || lpad(i::text, 4, '0'),
       inv.total_amount,
       CASE WHEN i % 3 = 0 THEN 'BANK_TRANSFER' WHEN i % 3 = 1 THEN 'CASH' ELSE 'CARD' END,
       'SUCCESS', inv.issue_date + INTERVAL '5 minutes',
       CASE WHEN i % 3 = 0 THEN 'BANK-DEMO-' || lpad(i::text, 4, '0') ELSE NULL END,
       'Giao dịch thanh toán dữ liệu mẫu',
       '90000013-6666-6666-6666-666666666666', NOW(), NOW(), false
FROM generate_series(1, 18) AS g(i)
JOIN invoice inv ON inv.invoice_id = format('53000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid;

-- ===================================================================
-- Hang cho kham benh: uu tien kham truoc, sau do moi den can lam sang.
-- ===================================================================
INSERT INTO queue_ticket (
    ticket_id, visit_id, department_id, work_date, queue_number, status,
    called_at, completed_at, service_id, created_at, updated_at, deleted
)
SELECT format('55000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       v.visit_id,
       (ARRAY[
           '33333333-3333-3333-3333-333333333333'::uuid,
           '77777777-7777-7777-7777-777777777777'::uuid,
           '66666666-6666-6666-6666-666666666666'::uuid,
           '88888888-8888-8888-8888-888888888888'::uuid,
           '99999999-9999-9999-9999-999999999999'::uuid,
           'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'::uuid,
           'dddddddd-dddd-dddd-dddd-dddddddddddd'::uuid
       ])[((i - 1) % 7) + 1],
       v.check_in_time::date, i,
       CASE WHEN i <= 4 THEN 'IN_PROGRESS' WHEN i <= 8 THEN 'WAITING' ELSE 'DONE' END,
       CASE WHEN i <= 4 OR i >= 9 THEN v.check_in_time + INTERVAL '10 minutes' ELSE NULL END,
       CASE WHEN i >= 9 THEN v.check_in_time + INTERVAL '50 minutes' ELSE NULL END,
       format('400000%s-0000-0000-0000-%s',
           lpad((((i - 1) % 7) + 1)::text, 2, '0'),
           lpad((((i - 1) % 7) + 1)::text, 12, '0'))::uuid,
       NOW(), NOW(), false
FROM generate_series(1, 18) AS g(i)
JOIN customer_visit v ON v.visit_id = format('52000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid;

-- 18 ho so benh an tuong ung hang cho kham.
INSERT INTO medical_record (
    record_id, record_version, record_code, visit_id, queue_ticket_id, doctor_id,
    chief_complaint, clinical_findings, diagnosis, prescription_note, conclusion,
    patient_instruction, status, completed_at, contact_requested,
    created_at, updated_at, deleted
)
SELECT format('56000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       0, 'MR-' || to_char(CURRENT_DATE, 'YYYY') || '-' || lpad(i::text, 5, '0'),
       q.visit_id, q.ticket_id, d.head_doctor_id,
       (ARRAY['Đau đầu và mệt mỏi','Ho và đau họng','Đau bụng','Nổi ban ngứa','Ù tai','Đau ngực khi gắng sức','Khám sức khỏe định kỳ'])[((i - 1) % 7) + 1],
       CASE WHEN i >= 9 THEN 'Khám lâm sàng ghi nhận dấu hiệu ổn định, chưa có biểu hiện cấp cứu.' ELSE 'Đang cập nhật kết quả khám.' END,
       CASE WHEN i >= 9 THEN 'Chẩn đoán ban đầu theo triệu chứng và kết quả khám.' ELSE NULL END,
       CASE WHEN i >= 9 THEN 'Dùng thuốc theo đơn và tái khám khi có dấu hiệu bất thường.' ELSE NULL END,
       CASE WHEN i >= 9 THEN 'Tình trạng ổn định, điều trị ngoại trú.' ELSE NULL END,
       CASE WHEN i >= 9 THEN 'Nghỉ ngơi, uống đủ nước và tuân thủ hướng dẫn của bác sĩ.' ELSE NULL END,
       CASE WHEN i <= 4 THEN 'IN_PROGRESS' WHEN i <= 8 THEN 'DRAFT' ELSE 'COMPLETED' END,
       CASE WHEN i >= 9 THEN q.completed_at ELSE NULL END,
       false, NOW(), NOW(), false
FROM generate_series(1, 18) AS g(i)
JOIN queue_ticket q ON q.ticket_id = format('55000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid
JOIN department d ON d.department_id = q.department_id;

UPDATE invoice inv
SET medical_record_id = format('56000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
    updated_at = NOW()
FROM generate_series(1, 18) AS g(i)
WHERE inv.invoice_id = format('53000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid;

-- 18 bo chi so sinh hieu.
INSERT INTO vital_signs (
    vital_id, medical_record_id, blood_pressure, heart_rate, temperature,
    weight, height, recorded_at, recorded_by, created_at, updated_at, deleted
)
SELECT format('56100000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       format('56000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       (110 + (i % 20)) || '/' || (70 + (i % 10)),
       68 + (i % 18), 36.4 + ((i % 5) * 0.1),
       48 + (i % 28), 155 + (i % 20), NOW() - (i || ' hours')::interval,
       '90000011-4444-4444-4444-444444444444', NOW(), NOW(), false
FROM generate_series(1, 18) AS g(i);

UPDATE medical_record mr
SET vital_signs_id = format('56100000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
    updated_at = NOW()
FROM generate_series(1, 18) AS g(i)
WHERE mr.record_id = format('56000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid;

-- ===================================================================
-- 15 hang cho va yeu cau can lam sang.
-- Cac luot chua kham xong bi BLOCKED; luot da kham se duoc xu ly tiep.
-- ===================================================================
INSERT INTO queue_ticket (
    ticket_id, visit_id, department_id, work_date, queue_number, status,
    called_at, completed_at, service_id, created_at, updated_at, deleted
)
SELECT format('55100000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       v.visit_id,
       CASE service_no
           WHEN 8 THEN '44444444-4444-4444-4444-444444444444'::uuid
           WHEN 9 THEN 'cccccccc-cccc-cccc-cccc-cccccccccccc'::uuid
           WHEN 10 THEN 'cccccccc-cccc-cccc-cccc-cccccccccccc'::uuid
           WHEN 11 THEN 'cccccccc-cccc-cccc-cccc-cccccccccccc'::uuid
           WHEN 12 THEN 'cccccccc-cccc-cccc-cccc-cccccccccccc'::uuid
           WHEN 13 THEN 'cccccccc-cccc-cccc-cccc-cccccccccccc'::uuid
           WHEN 14 THEN '44444444-4444-4444-4444-444444444444'::uuid
           WHEN 15 THEN '55555555-5555-5555-5555-555555555555'::uuid
           WHEN 16 THEN '55555555-5555-5555-5555-555555555555'::uuid
           WHEN 17 THEN 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'::uuid
           WHEN 18 THEN 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'::uuid
           ELSE 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'::uuid
       END,
       v.check_in_time::date, i,
       CASE WHEN i <= 3 OR i BETWEEN 5 AND 8 THEN 'BLOCKED'
            WHEN i = 4 THEN 'IN_PROGRESS' ELSE 'DONE' END,
       CASE WHEN i = 4 OR i >= 9 THEN v.check_in_time + INTERVAL '60 minutes' ELSE NULL END,
       CASE WHEN i >= 9 THEN v.check_in_time + INTERVAL '85 minutes' ELSE NULL END,
       format('400000%s-0000-0000-0000-%s', lpad(service_no::text, 2, '0'), lpad(service_no::text, 12, '0'))::uuid,
       NOW(), NOW(), false
FROM (
    SELECT i, 8 + ((i - 1) % 12) AS service_no
    FROM generate_series(1, 15) AS g(i)
) s
JOIN customer_visit v ON v.visit_id = format('52000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid;

INSERT INTO test_request (
    test_request_id, medical_record_id, service_id, performing_department,
    queue_ticket_id, description, status, requested_by, completed_at,
    performed_at, cancel_reason, invoice_item_id, created_at, updated_at, deleted
)
SELECT format('57000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       mr.record_id, q.service_id, q.department_id, q.ticket_id,
       'Yêu cầu cận lâm sàng dữ liệu mẫu',
       CASE WHEN i <= 3 OR i BETWEEN 5 AND 8 THEN 'BLOCKED'
            WHEN i = 4 THEN 'IN_PROGRESS' ELSE 'COMPLETED' END,
       mr.doctor_id,
       CASE WHEN i >= 9 THEN q.completed_at ELSE NULL END,
       CASE WHEN i = 4 OR i >= 9 THEN COALESCE(q.called_at, NOW()) ELSE NULL END,
       NULL,
       format('54100000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       NOW(), NOW(), false
FROM generate_series(1, 15) AS g(i)
JOIN medical_record mr ON mr.record_id = format('56000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid
JOIN queue_ticket q ON q.ticket_id = format('55100000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid;

-- Ket qua cho cac yeu cau da hoan thanh (7 ban ghi).
INSERT INTO test_result (
    result_id, test_request_id, image_url, conclusion, sample_id,
    sample_type, sample_status, collected_at, collected_by,
    performed_by, performed_at, verified_by, verified_at,
    created_at, updated_at, deleted
)
SELECT format('58000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       tr.test_request_id,
       CASE WHEN q.department_id IN ('55555555-5555-5555-5555-555555555555'::uuid, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'::uuid)
            THEN '/uploads/demo/ket-qua-' || i || '.pdf' ELSE NULL END,
       CASE WHEN q.department_id IN ('55555555-5555-5555-5555-555555555555'::uuid, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'::uuid)
            THEN 'Hình ảnh chưa ghi nhận bất thường đáng kể.'
            ELSE 'Các chỉ số trong giới hạn tham chiếu tại thời điểm thực hiện.' END,
       CASE WHEN q.department_id IN ('44444444-4444-4444-4444-444444444444'::uuid, 'cccccccc-cccc-cccc-cccc-cccccccccccc'::uuid)
            THEN 'SMP-' || to_char(CURRENT_DATE, 'YYMMDD') || '-' || lpad(i::text, 3, '0') ELSE NULL END,
       CASE WHEN q.department_id IN ('44444444-4444-4444-4444-444444444444'::uuid, 'cccccccc-cccc-cccc-cccc-cccccccccccc'::uuid)
            THEN 'BLOOD' ELSE NULL END,
       CASE WHEN q.department_id IN ('44444444-4444-4444-4444-444444444444'::uuid, 'cccccccc-cccc-cccc-cccc-cccccccccccc'::uuid)
            THEN 'ACCEPTED' ELSE NULL END,
       CASE WHEN q.department_id IN ('44444444-4444-4444-4444-444444444444'::uuid, 'cccccccc-cccc-cccc-cccc-cccccccccccc'::uuid)
            THEN q.called_at ELSE NULL END,
       CASE WHEN q.department_id IN ('44444444-4444-4444-4444-444444444444'::uuid, 'cccccccc-cccc-cccc-cccc-cccccccccccc'::uuid)
            THEN '90000011-4444-4444-4444-444444444444'::uuid ELSE NULL END,
       CASE
           WHEN q.department_id = '44444444-4444-4444-4444-444444444444'::uuid THEN '93000000-0000-0000-0000-000000000002'::uuid
           WHEN q.department_id = 'cccccccc-cccc-cccc-cccc-cccccccccccc'::uuid THEN '93000000-0000-0000-0000-000000000003'::uuid
           WHEN q.department_id = '55555555-5555-5555-5555-555555555555'::uuid THEN '91000000-0000-0000-0000-000000000007'::uuid
           WHEN q.department_id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'::uuid THEN '93000000-0000-0000-0000-000000000004'::uuid
           ELSE '91000000-0000-0000-0000-000000000005'::uuid
       END,
       tr.performed_at,
       CASE
           WHEN q.department_id = '44444444-4444-4444-4444-444444444444'::uuid THEN '93000000-0000-0000-0000-000000000002'::uuid
           WHEN q.department_id = 'cccccccc-cccc-cccc-cccc-cccccccccccc'::uuid THEN '93000000-0000-0000-0000-000000000003'::uuid
           WHEN q.department_id = '55555555-5555-5555-5555-555555555555'::uuid THEN '91000000-0000-0000-0000-000000000007'::uuid
           WHEN q.department_id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'::uuid THEN '93000000-0000-0000-0000-000000000004'::uuid
           ELSE '91000000-0000-0000-0000-000000000005'::uuid
       END,
       tr.completed_at, NOW(), NOW(), false
FROM generate_series(9, 15) AS g(i)
JOIN test_request tr ON tr.test_request_id = format('57000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid
JOIN queue_ticket q ON q.ticket_id = tr.queue_ticket_id;

-- 20 dong thuoc cho 10 ho so da hoan thanh.
INSERT INTO prescription_item (
    prescription_item_id, record_id, medicine_name, quantity, unit,
    note, frequency_per_day, created_at, updated_at, deleted
)
SELECT format('59000000-0000-0000-0000-%s', lpad(n::text, 12, '0'))::uuid,
       format('56000000-0000-0000-0000-%s', lpad((9 + ((n - 1) / 2))::text, 12, '0'))::uuid,
       CASE WHEN n % 2 = 1 THEN 'Paracetamol 500mg' ELSE 'Vitamin C 500mg' END,
       CASE WHEN n % 2 = 1 THEN 10 ELSE 14 END,
       'Viên',
       CASE WHEN n % 2 = 1 THEN 'Uống sau ăn khi đau hoặc sốt' ELSE 'Uống sau bữa sáng' END,
       CASE WHEN n % 2 = 1 THEN 2 ELSE 1 END,
       NOW(), NOW(), false
FROM generate_series(1, 20) AS g(n);

-- 20 thong bao trong ung dung cho benh nhan.
INSERT INTO notification (
    notification_id, recipient_id, notification_type, channel, title, content,
    related_entity, related_entity_id, status, sent_at, read_at,
    failure_reason, created_at, updated_at, deleted
)
SELECT format('5a000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       format('22000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       CASE WHEN i <= 8 THEN 'APPOINTMENT_CONFIRMED'
            WHEN i <= 18 THEN 'PAYMENT_SUCCESS' ELSE 'GENERAL' END,
       'IN_APP',
       CASE WHEN i <= 8 THEN 'Lịch hẹn đã được xác nhận'
            WHEN i <= 18 THEN 'Thanh toán thành công' ELSE 'Thông báo từ phòng khám' END,
       CASE WHEN i <= 8 THEN 'Vui lòng đến trước giờ hẹn 15 phút để làm thủ tục.'
            WHEN i <= 18 THEN 'Hóa đơn dịch vụ của bạn đã được thanh toán.'
            ELSE 'Vui lòng kiểm tra thông tin lượt khám.' END,
       CASE WHEN i <= 8 THEN 'Appointment' ELSE 'Invoice' END,
       CASE WHEN i <= 8
            THEN format('51000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid
            ELSE format('53000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid END,
       CASE WHEN i % 3 = 0 THEN 'READ' ELSE 'SENT' END,
       NOW() - (i || ' hours')::interval,
       CASE WHEN i % 3 = 0 THEN NOW() - ((i - 1) || ' hours')::interval ELSE NULL END,
       NULL, NOW(), NOW(), false
FROM generate_series(1, 20) AS g(i);

-- 20 audit log mau cho cac moc quan trong cua luong kham.
INSERT INTO audit_log (
    audit_id, action, entity_name, entity_id, actor_account_id,
    ip_address, user_agent, old_value, new_value, description,
    created_at, deleted
)
SELECT format('5b000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       CASE WHEN i <= 6 THEN 'PAYMENT_CONFIRMED'
            WHEN i <= 12 THEN 'PATIENT_CALLED'
            WHEN i <= 16 THEN 'EXAM_STARTED' ELSE 'RECORD_COMPLETED' END,
       CASE WHEN i <= 6 THEN 'Invoice' WHEN i <= 12 THEN 'QueueTicket'
            WHEN i <= 16 THEN 'MedicalRecord' ELSE 'MedicalRecord' END,
       CASE WHEN i <= 6 THEN format('53000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))
            WHEN i <= 12 THEN format('55000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))
            ELSE format('56000000-0000-0000-0000-%s', lpad(LEAST(i, 18)::text, 12, '0')) END,
       CASE WHEN i <= 6 THEN '30000017-7777-7777-7777-777777777777'::uuid
            ELSE '30000014-4444-4444-4444-444444444444'::uuid END,
       '127.0.0.1', 'CareS demo data', NULL, NULL,
       'Dữ liệu audit mẫu cho thao tác nghiệp vụ quan trọng',
       NOW() - ((20 - i) || ' minutes')::interval, false
FROM generate_series(1, 20) AS g(i);
