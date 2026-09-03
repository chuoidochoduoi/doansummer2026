-- ===================================================================
-- data.sql - bo du lieu trinh dien nhat quan cho he thong CareS
-- Danh muc MedicalService ben duoi la danh muc da chot va khong duoc thay doi.
-- Du lieu nghiep vu dung 12 benh nhan, co lich nhan su va cac trang thai de review.
--
-- Tai khoan seed:
--   admin         / 88888888  (BCrypt)
--   clinicmanager / 88888888  (BCrypt)
--   doctor1 / doctor_lab / doctor8 / doctor_xray / 88888888
--   customer: username la so dien thoai trong Profile / 88888888
--   chuoidochoduoi7e@gmail.com / 88888888 (tai khoan Customer moi, chua co nghiep vu)
-- ===================================================================

TRUNCATE TABLE
    membership_card_ledger, membership_card, membership_policy, family_member,
    test_result_attachment, test_result_revision,
    medical_service_form_template, clinical_form_template_version, clinical_form_template,
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

-- ===================================================================
-- Chinh sach the tra truoc CareS (khong tao the cho tai khoan moi)
-- ===================================================================
INSERT INTO membership_policy (
    policy_id, created_at, updated_at, deleted,
    minimum_top_up, discount_percent, validity_months, active
) VALUES (
    '7c000001-0000-0000-0000-000000000001', NOW(), NOW(), false,
    1000000, 15, 12, true
);

-- ===================================================================
-- Specialization (danh muc chuyen khoa - toi thieu de bo sung vao department)
-- ===================================================================
INSERT INTO specialization (specialization_id, created_at, updated_at, deleted, active, name, description) VALUES
('00000001-1111-1111-1111-111111111111', NOW(), NOW(), false, true, 'Nội khoa', 'Khám, chẩn đoán và điều trị các bệnh lý nội khoa thường gặp'),
('00000002-2222-2222-2222-222222222222', NOW(), NOW(), false, true, 'Nhi khoa', 'Khám và chăm sóc sức khỏe cho trẻ em'),
('00000003-3333-3333-3333-333333333333', NOW(), NOW(), false, true, 'Ngoại khoa', 'Khám và xử trí ban đầu các bệnh lý, chấn thương ngoại khoa'),
('00000004-4444-4444-4444-444444444444', NOW(), NOW(), false, true, 'Da liễu', 'Khám và điều trị các bệnh lý về da, tóc và móng'),
('00000008-8888-8888-8888-888888888888', NOW(), NOW(), false, true, 'Sản phụ khoa', 'Khám sức khỏe phụ nữ và thai kỳ');

-- ===================================================================
-- Department (phòng khám - toi thieu de he thong hoat dong)
-- ===================================================================
INSERT INTO department (department_id, created_at, updated_at, deleted, room_code, name, status, department_type, specialization_id, description, head_doctor_id) VALUES
('33333333-3333-3333-3333-333333333333', NOW(), NOW(), false, 'SUR-101', 'Phòng khám Ngoại 1', 'AVAILABLE', 'EXAMINATION', '00000003-3333-3333-3333-333333333333', 'Khám ngoại tổng quát và xử trí ban đầu', NULL),
('44444444-4444-4444-4444-444444444444', NOW(), NOW(), false, 'LAB-201', 'Phòng xét nghiệm huyết học', 'AVAILABLE', 'PARACLINICAL', NULL, 'Tiếp nhận và phân tích mẫu máu', NULL),
('55555555-5555-5555-5555-555555555555', NOW(), NOW(), false, 'IMG-301', 'Phòng siêu âm', 'AVAILABLE', 'PARACLINICAL', NULL, 'Thực hiện siêu âm chẩn đoán', NULL),
('66666666-6666-6666-6666-666666666666', NOW(), NOW(), false, 'PED-102', 'Phòng khám Nhi', 'AVAILABLE', 'EXAMINATION', '00000002-2222-2222-2222-222222222222', 'Khám và chăm sóc trẻ em', NULL),
('77777777-7777-7777-7777-777777777777', NOW(), NOW(), false, 'INT-103', 'Phòng khám Nội', 'AVAILABLE', 'EXAMINATION', '00000001-1111-1111-1111-111111111111', 'Khám nội khoa', NULL),
('88888888-8888-8888-8888-888888888888', NOW(), NOW(), false, 'DER-104', 'Phòng khám Da liễu', 'AVAILABLE', 'EXAMINATION', '00000004-4444-4444-4444-444444444444', 'Khám da liễu', NULL),
('99999999-9999-9999-9999-999999999999', NOW(), NOW(), false, 'SUR-102', 'Phòng thủ thuật Ngoại', 'AVAILABLE', 'EXAMINATION', '00000003-3333-3333-3333-333333333333', 'Khám vết thương, thay băng và chăm sóc ngoại khoa', NULL),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', NOW(), NOW(), false, 'INT-104', 'Phòng khám Nội 2', 'AVAILABLE', 'EXAMINATION', '00000001-1111-1111-1111-111111111111', 'Khám tim mạch cơ bản và các bệnh nội khoa', NULL),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', NOW(), NOW(), false, 'XR-302', 'Phòng X-quang', 'AVAILABLE', 'PARACLINICAL', NULL, 'Chụp X-quang kỹ thuật số', NULL),
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
('44444444-4444-4444-4444-444444444444', 'ca000007-0000-0000-0000-000000000007'),
('44444444-4444-4444-4444-444444444444', 'ca000008-0000-0000-0000-000000000008'),
('55555555-5555-5555-5555-555555555555', 'ca000003-0000-0000-0000-000000000003'),
('55555555-5555-5555-5555-555555555555', 'ca000006-0000-0000-0000-000000000006'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'ca000004-0000-0000-0000-000000000004'),
('cccccccc-cccc-cccc-cccc-cccccccccccc', 'ca000002-0000-0000-0000-000000000002'),
('cccccccc-cccc-cccc-cccc-cccccccccccc', 'ca000005-0000-0000-0000-000000000005');

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
('40000001-0000-0000-0000-000000000001', 'EX-SU-001', NOW(), NOW(), false, 'Khám ban đầu các bệnh lý và tổn thương có khả năng cần can thiệp ngoại khoa.', 'ACTIVE', false, 'Khám Ngoại tổng quát', 220000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 0, 120, NULL, NULL, '00000003-3333-3333-3333-333333333333', NULL),
('40000002-0000-0000-0000-000000000002', 'EX-IN-001', NOW(), NOW(), false, 'Khám và đánh giá tổng quát các bệnh lý nội khoa thường gặp ở người lớn.', 'ACTIVE', false, 'Khám Nội tổng quát', 220000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 16, 120, NULL, NULL, '00000001-1111-1111-1111-111111111111', NULL),
('40000003-0000-0000-0000-000000000003', 'EX-PE-001', NOW(), NOW(), false, 'Khám tổng quát và đánh giá sức khỏe cho trẻ em.', 'ACTIVE', false, 'Khám Nhi tổng quát', 220000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 0, 15, NULL, NULL, '00000002-2222-2222-2222-222222222222', NULL),
('40000004-0000-0000-0000-000000000004', 'EX-DE-001', NOW(), NOW(), false, 'Khám tổng quát các vấn đề về da, tóc và móng.', 'ACTIVE', false, 'Khám Da liễu', 230000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 0, 120, NULL, NULL, '00000004-4444-4444-4444-444444444444', NULL),
('40000005-0000-0000-0000-000000000005', 'EX-SU-003', NOW(), NOW(), false, 'Kiểm tra mức độ tổn thương, nguy cơ nhiễm trùng và hướng xử trí vết thương.', 'ACTIVE', false, 'Khám vết thương', 180000, 'EXAMINATION', 25, 1, false, false, false, 0, true, 0, 120, NULL, NULL, '00000003-3333-3333-3333-333333333333', NULL),
('40000006-0000-0000-0000-000000000006', 'EX-IN-002', NOW(), NOW(), false, 'Kiểm tra các triệu chứng tim mạch như đau ngực, hồi hộp, khó thở hoặc tăng huyết áp.', 'ACTIVE', false, 'Khám Tim mạch cơ bản', 280000, 'EXAMINATION', 35, 1, false, false, false, 0, true, 16, 120, NULL, NULL, '00000001-1111-1111-1111-111111111111', NULL),
('40000007-0000-0000-0000-000000000007', 'EX-OB-001', NOW(), NOW(), false, 'Khám và tư vấn các vấn đề sức khỏe phụ khoa thường gặp.', 'ACTIVE', false, 'Khám Phụ khoa', 280000, 'EXAMINATION', 35, 1, false, false, false, 0, true, 16, 60, 'FEMALE', NULL, '00000008-8888-8888-8888-888888888888', NULL),
('40000008-0000-0000-0000-000000000008', 'LAB-001', NOW(), NOW(), false, 'Đánh giá các thành phần tế bào máu, hỗ trợ phát hiện thiếu máu và nhiễm trùng.', 'ACTIVE', false, 'Công thức máu', 120000, 'PARACLINICAL', 15, 1, true, true, true, 45, false, 0, 120, NULL, NULL, NULL, 'ca000001-0000-0000-0000-000000000001'),
('40000009-0000-0000-0000-000000000009', 'LAB-002', NOW(), NOW(), false, 'Đo nồng độ glucose trong máu, hỗ trợ sàng lọc và theo dõi đái tháo đường.', 'ACTIVE', false, 'Đường huyết', 70000, 'PARACLINICAL', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('40000010-0000-0000-0000-000000000010', 'LAB-003', NOW(), NOW(), false, 'Đánh giá một số chỉ số sinh hóa quan trọng trong máu.', 'ACTIVE', false, 'Sinh hóa máu cơ bản', 190000, 'PARACLINICAL', 15, 1, true, true, true, 60, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('40000011-0000-0000-0000-000000000011', 'LAB-004', NOW(), NOW(), false, 'Kiểm tra các chỉ số hỗ trợ đánh giá hoạt động và tổn thương gan.', 'ACTIVE', false, 'Chức năng gan', 140000, 'PARACLINICAL', 15, 1, true, true, true, 60, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('40000012-0000-0000-0000-000000000012', 'LAB-005', NOW(), NOW(), false, 'Kiểm tra các chỉ số hỗ trợ đánh giá khả năng hoạt động của thận.', 'ACTIVE', false, 'Chức năng thận', 150000, 'PARACLINICAL', 15, 1, true, true, true, 60, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('40000013-0000-0000-0000-000000000013', 'LAB-006', NOW(), NOW(), false, 'Phân tích các chỉ số nước tiểu, hỗ trợ phát hiện bệnh tiết niệu và chuyển hóa.', 'ACTIVE', false, 'Tổng phân tích nước tiểu', 90000, 'PARACLINICAL', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000005-0000-0000-0000-000000000005'),
('40000014-0000-0000-0000-000000000014', 'LAB-007', NOW(), NOW(), false, 'Định lượng CRP, hỗ trợ đánh giá tình trạng viêm hoặc nhiễm trùng.', 'ACTIVE', false, 'Xét nghiệm CRP', 130000, 'PARACLINICAL', 15, 1, true, true, true, 60, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('40000015-0000-0000-0000-000000000015', 'IMG-003', NOW(), NOW(), false, 'Khảo sát các cơ quan trong ổ bụng bằng phương pháp siêu âm.', 'ACTIVE', false, 'Siêu âm ổ bụng tổng quát', 250000, 'PARACLINICAL', 20, 1, true, true, false, 10, false, 0, 120, NULL, NULL, NULL, 'ca000003-0000-0000-0000-000000000003'),
('40000016-0000-0000-0000-000000000016', 'IMG-004', NOW(), NOW(), false, 'Đánh giá kích thước, cấu trúc và bất thường của tuyến giáp.', 'ACTIVE', false, 'Siêu âm tuyến giáp', 220000, 'PARACLINICAL', 20, 1, true, true, false, 10, false, 0, 120, NULL, NULL, NULL, 'ca000003-0000-0000-0000-000000000003'),
('40000017-0000-0000-0000-000000000017', 'IMG-001', NOW(), NOW(), false, 'Chụp hình vùng ngực, hỗ trợ đánh giá phổi, tim và lồng ngực.', 'ACTIVE', false, 'X-quang ngực', 180000, 'PARACLINICAL', 15, 1, true, true, false, 15, false, 6, 120, NULL, NULL, NULL, 'ca000004-0000-0000-0000-000000000004'),
('40000018-0000-0000-0000-000000000018', 'IMG-002', NOW(), NOW(), false, 'Chụp hình xương khớp, hỗ trợ phát hiện tổn thương hoặc bất thường.', 'ACTIVE', false, 'X-quang xương khớp', 220000, 'PARACLINICAL', 20, 1, true, true, false, 15, false, 0, 120, NULL, NULL, NULL, 'ca000004-0000-0000-0000-000000000004'),
('40000019-0000-0000-0000-000000000019', 'IMG-006', NOW(), NOW(), false, 'Ghi lại hoạt động điện của tim, hỗ trợ phát hiện rối loạn nhịp và bất thường tim mạch.', 'ACTIVE', false, 'Điện tim ECG', 150000, 'PARACLINICAL', 15, 1, true, true, false, 5, false, 16, 120, NULL, NULL, NULL, 'ca000006-0000-0000-0000-000000000006'),
('40000020-0000-0000-0000-000000000020', 'LAB-008', NOW(), NOW(), false, 'Thực hiện test nhanh hỗ trợ sàng lọc một số bệnh truyền nhiễm.', 'ACTIVE', true, 'Test nhanh một số bệnh truyền nhiễm', 110000, 'PARACLINICAL', 15, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000008-0000-0000-0000-000000000008'),
('40000021-0000-0000-0000-000000000021', 'EX-IN-003', NOW(), NOW(), false, 'Thăm khám các vấn đề về dạ dày, đường ruột, gan mật và rối loạn tiêu hóa.', 'ACTIVE', false, 'Khám Tiêu hóa', 230000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 16, 120, NULL, NULL, '00000001-1111-1111-1111-111111111111', NULL),
('40000022-0000-0000-0000-000000000022', 'EX-IN-004', NOW(), NOW(), false, 'Đánh giá ho, khó thở, đau ngực và các bệnh lý đường hô hấp thường gặp.', 'ACTIVE', false, 'Khám Hô hấp', 230000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 16, 120, NULL, NULL, '00000001-1111-1111-1111-111111111111', NULL),
('40000023-0000-0000-0000-000000000023', 'EX-IN-005', NOW(), NOW(), false, 'Khám đau khớp, đau lưng, hạn chế vận động và các vấn đề cơ xương khớp.', 'ACTIVE', false, 'Khám Cơ xương khớp', 240000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 16, 120, NULL, NULL, '00000001-1111-1111-1111-111111111111', NULL),
('40000024-0000-0000-0000-000000000024', 'EX-SU-002', NOW(), NOW(), false, 'Đánh giá sưng, đau, bầm tím và tổn thương cơ, gân hoặc phần mềm.', 'ACTIVE', false, 'Khám chấn thương phần mềm', 220000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 0, 120, NULL, NULL, '00000003-3333-3333-3333-333333333333', NULL),
('40000025-0000-0000-0000-000000000025', 'EX-SU-004', NOW(), NOW(), false, 'Làm sạch, thay băng và theo dõi quá trình hồi phục của vết thương.', 'ACTIVE', false, 'Thay băng, chăm sóc vết thương', 150000, 'EXAMINATION', 20, 1, false, false, false, 0, true, 0, 120, NULL, NULL, '00000003-3333-3333-3333-333333333333', NULL),
('40000026-0000-0000-0000-000000000026', 'EX-PE-002', NOW(), NOW(), false, 'Khám ho, sổ mũi, khó thở và các bệnh đường hô hấp ở trẻ.', 'ACTIVE', false, 'Khám bệnh hô hấp trẻ em', 220000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 0, 15, NULL, NULL, '00000002-2222-2222-2222-222222222222', NULL),
('40000027-0000-0000-0000-000000000027', 'EX-PE-003', NOW(), NOW(), false, 'Khám đau bụng, tiêu chảy, táo bón, nôn và rối loạn tiêu hóa ở trẻ.', 'ACTIVE', false, 'Khám tiêu hóa trẻ em', 220000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 0, 15, NULL, NULL, '00000002-2222-2222-2222-222222222222', NULL),
('40000028-0000-0000-0000-000000000028', 'EX-PE-004', NOW(), NOW(), false, 'Đánh giá nguyên nhân sốt và các dấu hiệu nhiễm khuẩn thường gặp ở trẻ.', 'ACTIVE', false, 'Khám sốt và bệnh nhiễm khuẩn thông thường', 230000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 0, 15, NULL, NULL, '00000002-2222-2222-2222-222222222222', NULL),
('40000029-0000-0000-0000-000000000029', 'EX-OB-002', NOW(), NOW(), false, 'Theo dõi tình trạng thai kỳ và sức khỏe của thai phụ.', 'ACTIVE', false, 'Khám Thai', 300000, 'EXAMINATION', 35, 1, false, false, false, 0, true, 16, 60, 'FEMALE', NULL, '00000008-8888-8888-8888-888888888888', NULL),
('40000030-0000-0000-0000-000000000030', 'EX-OB-003', NOW(), NOW(), false, 'Tư vấn biện pháp tránh thai và chăm sóc sức khỏe sinh sản.', 'ACTIVE', false, 'Khám và tư vấn kế hoạch hóa gia đình', 250000, 'EXAMINATION', 35, 1, false, false, false, 0, true, 16, 60, 'FEMALE', NULL, '00000008-8888-8888-8888-888888888888', NULL),
('40000031-0000-0000-0000-000000000031', 'EX-OB-004', NOW(), NOW(), false, 'Khám các dấu hiệu ngứa, đau, khí hư bất thường và viêm nhiễm phụ khoa.', 'ACTIVE', false, 'Khám viêm nhiễm phụ khoa', 280000, 'EXAMINATION', 35, 1, false, false, false, 0, true, 16, 60, 'FEMALE', NULL, '00000008-8888-8888-8888-888888888888', NULL),
('40000032-0000-0000-0000-000000000032', 'EX-DE-002', NOW(), NOW(), false, 'Đánh giá mức độ mụn và tư vấn phương pháp chăm sóc, điều trị phù hợp.', 'ACTIVE', false, 'Khám mụn trứng cá', 220000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 10, 120, NULL, NULL, '00000004-4444-4444-4444-444444444444', NULL),
('40000033-0000-0000-0000-000000000033', 'EX-DE-003', NOW(), NOW(), false, 'Khám ngứa, phát ban, mẩn đỏ và các biểu hiện dị ứng ngoài da.', 'ACTIVE', false, 'Khám viêm da, dị ứng', 230000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 0, 120, NULL, NULL, '00000004-4444-4444-4444-444444444444', NULL),
('40000034-0000-0000-0000-000000000034', 'EX-DE-004', NOW(), NOW(), false, 'Kiểm tra các tổn thương nghi ngờ do nấm ở da, tóc hoặc móng.', 'ACTIVE', false, 'Khám nấm da', 230000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 0, 120, NULL, NULL, '00000004-4444-4444-4444-444444444444', NULL),
('40000035-0000-0000-0000-000000000035', 'IMG-005', NOW(), NOW(), false, 'Khảo sát tình trạng thai và một số chỉ số phát triển của thai nhi.', 'ACTIVE', false, 'Siêu âm thai', 300000, 'PARACLINICAL', 25, 1, true, true, false, 10, false, 16, 60, 'FEMALE', NULL, NULL, 'ca000003-0000-0000-0000-000000000003');

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
INSERT INTO staff_info (staff_id, created_at, updated_at, deleted, profile_id, staff_code, system_role, national_id, bank_account, highest_degree, university, license_number, specialization_id, department_id) VALUES
                                                                                                                                                                                                                     ('90000008-1111-1111-1111-111111111111', NOW(), NOW(), false, '20000009-9999-9999-9999-999999999999', 'STF-ADM-001', 'ADMIN', '9123456789', NULL, NULL, NULL, NULL, NULL, NULL),
('90000009-2222-2222-2222-222222222222', NOW(), NOW(), false, '20000010-0000-0000-0000-000000000001', 'STF-CLM-001', 'CLINIC_MANAGER', '9223456789', NULL, NULL, NULL, NULL, NULL, NULL);

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
                                                                                                                                                       ('20000011-1111-1111-1111-111111111111', '30000014-4444-4444-4444-444444444444', NOW(), NOW(), false, 'Nguyễn Hoàng Minh', '1980-05-10', 'MALE', '0913517624', 'hoangminh.nguyen@cares.vn', 'Ba Đình, Hà Nội', NULL),
                                                                                                                                                       ('20000012-2222-2222-2222-222222222222', '30000015-5555-5555-5555-555555555555', NOW(), NOW(), false, 'Trần Ngọc Hân', '1990-08-20', 'FEMALE', '0924186357', 'ngochan.tran@cares.vn', 'Đống Đa, Hà Nội', NULL),
                                                                                                                                                       ('20000013-3333-3333-3333-333333333333', '30000016-6666-6666-6666-666666666666', NOW(), NOW(), false, 'Lê Quốc Bảo', '1995-12-01', 'MALE', '0935271468', 'quocbao.le@cares.vn', 'Hai Bà Trưng, Hà Nội', NULL),
                                                                                                                                                       ('20000014-4444-4444-4444-444444444444', '30000017-7777-7777-7777-777777777777', NOW(), NOW(), false, 'Phạm Thùy Dương', '1992-03-15', 'FEMALE', '0946382517', 'thuyduong.pham@cares.vn', 'Hoàng Mai, Hà Nội', NULL);

-- ===================================================================
-- StaffInfo (Thêm staff_info cho các profile trên)
-- ===================================================================
INSERT INTO staff_info (staff_id, created_at, updated_at, deleted, profile_id, staff_code, system_role, national_id, bank_account, highest_degree, university, license_number, specialization_id, department_id) VALUES
                                                                                                                                                                                                                     ('90000010-3333-3333-3333-333333333333', NOW(), NOW(), false, '20000011-1111-1111-1111-111111111111', 'STF-DOC-001', 'DOCTOR', '001080123456', NULL, 'Bác sĩ chuyên khoa', 'Đại học Y Hà Nội', 'CCHN-12345', '00000003-3333-3333-3333-333333333333', '33333333-3333-3333-3333-333333333333'),
                                                                                                                                                                                                                     ('90000011-4444-4444-4444-444444444444', NOW(), NOW(), false, '20000012-2222-2222-2222-222222222222', 'STF-NUR-001', 'NURSE', '001090123456', NULL, 'Cử nhân điều dưỡng', 'Đại học Y Dược', 'CCHN-23456', NULL, '44444444-4444-4444-4444-444444444444'),
                                                                                                                                                                                                                     ('90000012-5555-5555-5555-555555555555', NOW(), NOW(), false, '20000013-3333-3333-3333-333333333333', 'STF-REC-001', 'RECEPTIONIST', '001095123456', NULL, NULL, NULL, NULL, NULL, NULL),
                                                                                                                                                                                                                     ('90000013-6666-6666-6666-666666666666', NOW(), NOW(), false, '20000014-4444-4444-4444-444444444444', 'STF-CAS-001', 'CASHIER', '001092123456', NULL, NULL, NULL, NULL, NULL, NULL);

-- Bo sung nhan su nen cho cac phong kham, CLS va vai tro van hanh.
-- Tat ca tai khoan trinh dien dung mat khau: 88888888.
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
-- Cac tai khoan bac si can lam sang con thieu trong bo du lieu goc.
-- Bon nhom nghiep vu: doctor1 (kham), doctor_lab (xet nghiem),
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
('93000000-0000-0000-0000-000000000004', NOW(), NOW(), false, '23000000-0000-0000-0000-000000000004', 'STF-DOC-XRAY', 'DOCTOR', '001087000004', NULL, 'Bác sĩ chẩn đoán hình ảnh', 'Đại học Y Hà Nội', 'CCHN-XRAY-001', NULL, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb');

-- Ky thuat vien xet nghiem ca toi. Le tan 3, thu ngan 3 va dieu duong 3
-- da co trong nhom tai khoan sinh tu dong ben duoi.
INSERT INTO account (account_id, created_at, is_active, password_hash, role, username) VALUES
('34000000-0000-0000-0000-000000000002', NOW(), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'lab_evening');

INSERT INTO profile (
    profile_id, account_id, created_at, updated_at, deleted,
    full_name, date_of_birth, gender, phone, email, address, blood_type
) VALUES
('24000000-0000-0000-0000-000000000002', '34000000-0000-0000-0000-000000000002', NOW(), NOW(), false,
 'Kỹ thuật viên Phạm Ngọc Diệp', DATE '1993-03-18', 'FEMALE', '0827364159', 'ngocdiep.pham@cares.vn', 'Nam Từ Liêm, Hà Nội', NULL);

INSERT INTO staff_info (
    staff_id, created_at, updated_at, deleted, profile_id, staff_code,
    system_role, national_id, bank_account, highest_degree, university,
    license_number, specialization_id, department_id
) VALUES
('94000000-0000-0000-0000-000000000002', NOW(), NOW(), false, '24000000-0000-0000-0000-000000000002',
 'STF-LAB-EVE', 'NURSE', '001093527184', NULL, 'Cử nhân kỹ thuật xét nghiệm y học', 'Đại học Y Hà Nội',
 NULL, NULL, '44444444-4444-4444-4444-444444444444');

INSERT INTO profile (
    profile_id, account_id, created_at, updated_at, deleted,
    full_name, date_of_birth, gender, phone, email, address, blood_type
)
SELECT format('21000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       format('31000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       NOW(), NOW(), false,
       (ARRAY[
            'Trần Minh Quân', 'Nguyễn Thu Hà', 'Lê Hoàng Nam',
            'Phạm Ngọc Lan', 'Đỗ Anh Tuấn', 'Vũ Thanh Mai',
            'Bùi Đức Long', 'Nguyễn Thị Hương',
            'Trần Thị Mai', 'Lê Quốc Việt',
            'Nguyễn Minh Anh', 'Trần Thu Trang',
            'Lê Thị Hạnh', 'Phạm Quốc Khánh'
       ])[i],
       (DATE '1978-01-01' + (i * 420) * INTERVAL '1 day')::date,
       CASE WHEN i IN (2,4,6,8,9,11,12,13) THEN 'FEMALE' ELSE 'MALE' END,
       (ARRAY['0971358246','0972468135','0973516824','0974682513','0975824136','0976143852','0977285143',
              '0961357824','0962485137','0963514278','0964728153','0965843217','0966137584','0967251438'])[i],
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
       CASE WHEN i <= 7 THEN 'CCHN-HN-' || to_char(2018 + i, 'FM0000') || '-' || lpad((i + 1)::text, 4, '0') ELSE NULL END,
       CASE i
           WHEN 1 THEN '00000001-1111-1111-1111-111111111111'::uuid
           WHEN 2 THEN '00000002-2222-2222-2222-222222222222'::uuid
           WHEN 3 THEN '00000004-4444-4444-4444-444444444444'::uuid
           WHEN 4 THEN '00000003-3333-3333-3333-333333333333'::uuid
           WHEN 5 THEN '00000001-1111-1111-1111-111111111111'::uuid
           WHEN 6 THEN '00000008-8888-8888-8888-888888888888'::uuid
           ELSE NULL
       END,
       CASE i
           WHEN 1 THEN '77777777-7777-7777-7777-777777777777'::uuid
           WHEN 2 THEN '66666666-6666-6666-6666-666666666666'::uuid
           WHEN 3 THEN '88888888-8888-8888-8888-888888888888'::uuid
           WHEN 4 THEN '99999999-9999-9999-9999-999999999999'::uuid
           WHEN 5 THEN 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'::uuid
           WHEN 6 THEN 'dddddddd-dddd-dddd-dddd-dddddddddddd'::uuid
           WHEN 7 THEN '55555555-5555-5555-5555-555555555555'::uuid
           WHEN 8 THEN '44444444-4444-4444-4444-444444444444'::uuid
           WHEN 9 THEN 'cccccccc-cccc-cccc-cccc-cccccccccccc'::uuid
           WHEN 10 THEN '55555555-5555-5555-5555-555555555555'::uuid
           ELSE NULL
       END
FROM generate_series(1, 14) AS g(i);

-- Ky thuat vien bo sung cho cac phong can lam sang; mat khau trinh dien
-- chung la 88888888.
INSERT INTO account (account_id, created_at, is_active, password_hash, role, username) VALUES
('35000000-0000-0000-0000-000000000007', NOW(), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'technician_biochem_evening'),
('35000000-0000-0000-0000-000000000008', NOW(), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'technician_ultrasound_evening'),
('35000000-0000-0000-0000-000000000009', NOW(), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'technician_xray_pm'),
('35000000-0000-0000-0000-000000000010', NOW(), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'technician_xray_evening');

INSERT INTO profile (
    profile_id, account_id, created_at, updated_at, deleted,
    full_name, date_of_birth, gender, phone, email, address, blood_type
) VALUES
('25000000-0000-0000-0000-000000000007','35000000-0000-0000-0000-000000000007',NOW(),NOW(),false,'Kỹ thuật viên Bùi Đức Anh',DATE '1991-01-15','MALE','0837000007','duc.anh@cares.vn','Hà Nội',NULL),
('25000000-0000-0000-0000-000000000008','35000000-0000-0000-0000-000000000008',NOW(),NOW(),false,'Kỹ thuật viên Nguyễn Thảo Vy',DATE '1992-04-08','FEMALE','0837000008','thao.vy@cares.vn','Hà Nội',NULL),
('25000000-0000-0000-0000-000000000009','35000000-0000-0000-0000-000000000009',NOW(),NOW(),false,'Kỹ thuật viên Trần Hoàng Long',DATE '1990-09-23','MALE','0837000009','hoang.long@cares.vn','Hà Nội',NULL),
('25000000-0000-0000-0000-000000000010','35000000-0000-0000-0000-000000000010',NOW(),NOW(),false,'Kỹ thuật viên Phạm Mai Chi',DATE '1993-12-02','FEMALE','0837000010','mai.chi@cares.vn','Hà Nội',NULL);

INSERT INTO staff_info (
    staff_id, created_at, updated_at, deleted, profile_id, staff_code,
    system_role, national_id, bank_account, highest_degree, university,
    license_number, specialization_id, department_id
) VALUES
('95000000-0000-0000-0000-000000000007',NOW(),NOW(),false,'25000000-0000-0000-0000-000000000007','STF-TEC-BIO-EVE','NURSE','001091700007',NULL,'Cử nhân kỹ thuật xét nghiệm','Đại học Y Hà Nội',NULL,NULL,'cccccccc-cccc-cccc-cccc-cccccccccccc'),
('95000000-0000-0000-0000-000000000008',NOW(),NOW(),false,'25000000-0000-0000-0000-000000000008','STF-TEC-US-EVE','NURSE','001092700008',NULL,'Cử nhân kỹ thuật hình ảnh','Đại học Y Hà Nội',NULL,NULL,'55555555-5555-5555-5555-555555555555'),
('95000000-0000-0000-0000-000000000009',NOW(),NOW(),false,'25000000-0000-0000-0000-000000000009','STF-TEC-XR-PM','NURSE','001090700009',NULL,'Cử nhân kỹ thuật hình ảnh','Đại học Y Hà Nội',NULL,NULL,'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
('95000000-0000-0000-0000-000000000010',NOW(),NOW(),false,'25000000-0000-0000-0000-000000000010','STF-TEC-XR-EVE','NURSE','001093700010',NULL,'Cử nhân kỹ thuật hình ảnh','Đại học Y Hà Nội',NULL,NULL,'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb');

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
('a1000000-0000-0000-0000-000000000008', NOW(), NOW(), false, '91000000-0000-0000-0000-000000000005', 'ca000006-0000-0000-0000-000000000006', NULL, NULL, NULL, NULL, 'ACTIVE'),
('a1000000-0000-0000-0000-000000000009', NOW(), NOW(), false, '91000000-0000-0000-0000-000000000008', 'ca000008-0000-0000-0000-000000000008', NULL, NULL, NULL, NULL, 'ACTIVE');

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

-- Bo sung nang luc de cac ca can lam sang co nhan su du dieu kien.
INSERT INTO staff_capability (
    staff_capability_id, created_at, updated_at, deleted, staff_id,
    capability_id, certificate_number, issued_date, expiry_date,
    issuing_organization, status
) VALUES
('a2000000-0000-0000-0000-000000000006', NOW(), NOW(), false, '93000000-0000-0000-0000-000000000002', 'ca000008-0000-0000-0000-000000000008', 'NL-TN-2024-006', DATE '2024-03-15', DATE '2030-03-14', 'Sở Y tế Hà Nội', 'ACTIVE'),
('a2000000-0000-0000-0000-000000000007', NOW(), NOW(), false, '91000000-0000-0000-0000-000000000010', 'ca000006-0000-0000-0000-000000000006', 'NL-ECG-2024-007', DATE '2024-05-20', DATE '2030-05-19', 'Sở Y tế Hà Nội', 'ACTIVE'),
('a2000000-0000-0000-0000-000000000008', NOW(), NOW(), false, '94000000-0000-0000-0000-000000000002', 'ca000001-0000-0000-0000-000000000001', 'NL-HH-2025-008', DATE '2025-02-10', DATE '2031-02-09', 'Sở Y tế Hà Nội', 'ACTIVE'),
('a2000000-0000-0000-0000-000000000009', NOW(), NOW(), false, '94000000-0000-0000-0000-000000000002', 'ca000008-0000-0000-0000-000000000008', 'NL-TN-2025-009', DATE '2025-02-10', DATE '2031-02-09', 'Sở Y tế Hà Nội', 'ACTIVE');

-- Bo sung nang luc con thieu de tung phong CLS co nguoi phu trach hop le
-- trong ca sang, chieu va toi.
INSERT INTO staff_capability (
    staff_capability_id, created_at, updated_at, deleted, staff_id,
    capability_id, certificate_number, issued_date, expiry_date,
    issuing_organization, status
) VALUES
('a3000000-0000-0000-0000-000000000001',NOW(),NOW(),false,'93000000-0000-0000-0000-000000000002','ca000007-0000-0000-0000-000000000007',NULL,NULL,NULL,NULL,'ACTIVE'),
('a3000000-0000-0000-0000-000000000002',NOW(),NOW(),false,'94000000-0000-0000-0000-000000000002','ca000007-0000-0000-0000-000000000007',NULL,NULL,NULL,NULL,'ACTIVE'),
('a3000000-0000-0000-0000-000000000003',NOW(),NOW(),false,'93000000-0000-0000-0000-000000000003','ca000005-0000-0000-0000-000000000005',NULL,NULL,NULL,NULL,'ACTIVE'),
('a3000000-0000-0000-0000-000000000004',NOW(),NOW(),false,'91000000-0000-0000-0000-000000000007','ca000006-0000-0000-0000-000000000006',NULL,NULL,NULL,NULL,'ACTIVE'),
('a3000000-0000-0000-0000-000000000005',NOW(),NOW(),false,'95000000-0000-0000-0000-000000000007','ca000002-0000-0000-0000-000000000002',NULL,NULL,NULL,NULL,'ACTIVE'),
('a3000000-0000-0000-0000-000000000006',NOW(),NOW(),false,'95000000-0000-0000-0000-000000000007','ca000005-0000-0000-0000-000000000005',NULL,NULL,NULL,NULL,'ACTIVE'),
('a3000000-0000-0000-0000-000000000007',NOW(),NOW(),false,'95000000-0000-0000-0000-000000000008','ca000003-0000-0000-0000-000000000003',NULL,NULL,NULL,NULL,'ACTIVE'),
('a3000000-0000-0000-0000-000000000008',NOW(),NOW(),false,'95000000-0000-0000-0000-000000000008','ca000006-0000-0000-0000-000000000006',NULL,NULL,NULL,NULL,'ACTIVE'),
('a3000000-0000-0000-0000-000000000009',NOW(),NOW(),false,'95000000-0000-0000-0000-000000000009','ca000004-0000-0000-0000-000000000004',NULL,NULL,NULL,NULL,'ACTIVE'),
('a3000000-0000-0000-0000-000000000010',NOW(),NOW(),false,'95000000-0000-0000-0000-000000000010','ca000004-0000-0000-0000-000000000004',NULL,NULL,NULL,NULL,'ACTIVE');

UPDATE staff_capability
SET certificate_number = COALESCE(certificate_number, 'NL-' || upper(substr(replace(staff_capability_id::text, '-', ''), 1, 12))),
    issued_date = COALESCE(issued_date, DATE '2024-01-15'),
    expiry_date = COALESCE(expiry_date, DATE '2030-01-14'),
    issuing_organization = COALESCE(issuing_organization, 'Sở Y tế Hà Nội'),
    updated_at = NOW();

-- ===================================================================
-- Shift Config
-- ===================================================================
INSERT INTO shift_config (shift_id, created_at, updated_at, deleted, name, start_time, end_time, is_active) VALUES
                                                                                                                ('70000001-1111-1111-1111-111111111111', NOW(), NOW(), false, 'Ca Sáng', '00:00', '08:00', true),
                                                                                                                ('70000002-2222-2222-2222-222222222222', NOW(), NOW(), false, 'Ca Chiều', '08:00', '16:00', true),
                                                                                                                ('70000003-3333-3333-3333-333333333333', NOW(), NOW(), false, 'Ca Tối', '16:00', '23:59:59', true);

-- Phien ban gio ca ban dau. Moi lich hen va lich nhan vien moi deu tham chieu
-- phien ban nay, trong khi shift_name/shift_time van duoc giu lam snapshot.
INSERT INTO shift_version (
    shift_version_id, shift_id, start_time, end_time, effective_from, effective_to,
    change_reason, created_by, created_at, updated_at, deleted
) VALUES
('71000001-1111-1111-1111-111111111111', '70000001-1111-1111-1111-111111111111', '00:00', '08:00', DATE '2026-01-01', NULL,
 'Khởi tạo giờ làm việc ca sáng', '30000013-3333-3333-3333-333333333333', NOW(), NOW(), false),
('71000002-2222-2222-2222-222222222222', '70000002-2222-2222-2222-222222222222', '08:00', '16:00', DATE '2026-01-01', NULL,
 'Khởi tạo giờ làm việc ca chiều', '30000013-3333-3333-3333-333333333333', NOW(), NOW(), false),
('71000003-3333-3333-3333-333333333333', '70000003-3333-3333-3333-333333333333', '16:00', '23:59:59', DATE '2026-01-01', NULL,
 'Khởi tạo giờ làm việc ca tối', '30000013-3333-3333-3333-333333333333', NOW(), NOW(), false);

-- Ngoai le gio dac biet van giu ca hoat dong; bo du lieu nghi ca/nghi ngay
-- de 14 ngay trinh dien bao phu du ba ca va ca Chu nhat.
INSERT INTO clinic_schedule_exception (
    exception_id, work_date, shift_id, exception_type,
    special_start_time, special_end_time, reason, created_by,
    created_at, updated_at, deleted
) VALUES
('72000003-3333-3333-3333-333333333333', date_trunc('week', CURRENT_DATE)::date + 11, '70000001-1111-1111-1111-111111111111', 'SPECIAL_HOURS',
 '01:00', '07:00', 'Điều chỉnh giờ ca sáng trong ngày đào tạo nội bộ', '30000013-3333-3333-3333-333333333333', NOW(), NOW(), false);

-- ===================================================================
-- 12 benh nhan co tai khoan
-- ===================================================================
INSERT INTO account (account_id, created_at, is_active, password_hash, role, username)
SELECT format('32000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       NOW(), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m',
       'CUSTOMER',
       (ARRAY[
           '0944433222','0982143657','0975318246','0964182735','0936251748',
           '0927314856','0918462753','0909573146','0862147539','0853264718',
           '0845372619','0836481725'
       ])[i]
FROM generate_series(1, 12) AS g(i);

-- ===================================================================
-- Tai khoan Customer sach de kiem thu luong cua nguoi dung moi.
-- Chi co Account + Profile; khong co lich hen, luot kham, hoa don,
-- thanh vien gia dinh hoac the tra truoc CareS.
-- Mat khau: 88888888.
-- ===================================================================
INSERT INTO account (account_id, created_at, is_active, password_hash, role, username) VALUES
('32f00000-0000-0000-0000-000000000001', NOW(), true,
 '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m',
 'CUSTOMER', 'chuoidochoduoi7e@gmail.com');

-- ===================================================================
-- Lich mau va lich thuc te cua nhan su
-- De de test, moi phong kham va phong can lam sang dung mot tai khoan bac si
-- dai dien cho ca sang, ca chieu va ca toi. Dieu duong/ky thuat vien la nhan
-- su bo sung, khong duoc dung de thay the dieu kien bat buoc co bac si cua phong.
-- Lich duoc sinh cho tuan hien tai va tuan ke tiep, bao gom ca Chu nhat.
-- ===================================================================
WITH roster(staff_id, pattern) AS (
    VALUES
    ('90000009-2222-2222-2222-222222222222'::uuid, 'AM'), -- Clinic Manager
    ('90000012-5555-5555-5555-555555555555'::uuid, 'AM'), -- Receptionist 1
    ('91000000-0000-0000-0000-000000000011'::uuid, 'PM'), -- Receptionist 2
    ('91000000-0000-0000-0000-000000000012'::uuid, 'EVENING'), -- Receptionist 3
    ('90000013-6666-6666-6666-666666666666'::uuid, 'AM'), -- Cashier 1
    ('91000000-0000-0000-0000-000000000013'::uuid, 'PM'), -- Cashier 2
    ('91000000-0000-0000-0000-000000000014'::uuid, 'EVENING'), -- Cashier 3
    ('90000011-4444-4444-4444-444444444444'::uuid, 'AM'), -- Nurse 1
    ('91000000-0000-0000-0000-000000000008'::uuid, 'PM'), -- Nurse 2
    ('91000000-0000-0000-0000-000000000009'::uuid, 'PM'), -- Nurse 3 / biochemistry
    ('91000000-0000-0000-0000-000000000001'::uuid, 'AM'), -- Internal room 1
    ('91000000-0000-0000-0000-000000000005'::uuid, 'AM'), -- Internal room 2
    ('90000010-3333-3333-3333-333333333333'::uuid, 'AM'), -- Surgery room 1
    ('91000000-0000-0000-0000-000000000004'::uuid, 'AM'), -- Procedure room
    ('91000000-0000-0000-0000-000000000002'::uuid, 'AM'), -- Pediatrics
    ('91000000-0000-0000-0000-000000000006'::uuid, 'AM'), -- Obstetrics
    ('91000000-0000-0000-0000-000000000003'::uuid, 'AM'), -- Dermatology
    ('93000000-0000-0000-0000-000000000002'::uuid, 'AM'), -- Hematology / rapid test
    ('94000000-0000-0000-0000-000000000002'::uuid, 'EVENING'), -- Hematology / rapid test evening
    ('93000000-0000-0000-0000-000000000003'::uuid, 'AM'), -- Biochemistry / urinalysis
    ('95000000-0000-0000-0000-000000000007'::uuid, 'EVENING'), -- Biochemistry / urinalysis
    ('91000000-0000-0000-0000-000000000007'::uuid, 'AM'), -- Ultrasound 1
    ('91000000-0000-0000-0000-000000000010'::uuid, 'PM'), -- Ultrasound / ECG
    ('95000000-0000-0000-0000-000000000008'::uuid, 'EVENING'), -- Ultrasound / ECG
    ('93000000-0000-0000-0000-000000000004'::uuid, 'AM'), -- X-ray 1
    ('95000000-0000-0000-0000-000000000009'::uuid, 'PM'), -- X-ray 2
    ('95000000-0000-0000-0000-000000000010'::uuid, 'EVENING') -- X-ray 3
), weekdays(day_no, day_name) AS (
    VALUES (1, 'MONDAY'), (2, 'TUESDAY'), (3, 'WEDNESDAY'),
           (4, 'THURSDAY'), (5, 'FRIDAY'), (6, 'SATURDAY'), (7, 'SUNDAY')
)
INSERT INTO staff_schedule_template (
    template_id, created_at, updated_at, deleted,
    staff_id, day_of_week, shift_id, is_active
)
SELECT DISTINCT ON (r.staff_id, w.day_name)
       gen_random_uuid(), NOW(), NOW(), false, r.staff_id, w.day_name,
       CASE
           WHEN r.pattern = 'AM' THEN '70000001-1111-1111-1111-111111111111'::uuid
           WHEN r.pattern = 'PM' THEN '70000002-2222-2222-2222-222222222222'::uuid
           WHEN r.pattern = 'EVENING' THEN '70000003-3333-3333-3333-333333333333'::uuid
           ELSE '70000002-2222-2222-2222-222222222222'::uuid
       END,
       true
FROM roster r CROSS JOIN weekdays w
ORDER BY r.staff_id, w.day_name,
         CASE r.pattern WHEN 'AM' THEN 1 WHEN 'PM' THEN 2 WHEN 'EVENING' THEN 3 ELSE 4 END;

WITH calendar AS (
    SELECT d::date AS work_date,
           (ARRAY['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'])
               [EXTRACT(ISODOW FROM d)::int] AS day_name
    FROM generate_series(
        date_trunc('week', CURRENT_DATE)::date,
        date_trunc('week', CURRENT_DATE)::date + 13,
        INTERVAL '1 day'
    ) d
)
INSERT INTO staff_schedule (
    schedule_id, created_at, updated_at, deleted, is_custom, note,
    status, work_date, shift_id, shift_version_id,
    actual_start_time, actual_end_time, staff_id, template_id
)
SELECT gen_random_uuid(), NOW(), NOW(), false, false,
       'Lịch làm việc sinh từ lịch tuần', 'SCHEDULED', c.work_date,
       t.shift_id, v.shift_version_id,
       COALESCE(special.special_start_time, v.start_time),
       COALESCE(special.special_end_time, v.end_time),
       t.staff_id, t.template_id
FROM calendar c
JOIN staff_schedule_template t ON t.day_of_week::text = c.day_name AND t.is_active = true
JOIN shift_version v ON v.shift_id = t.shift_id
    AND v.effective_from <= c.work_date
    AND (v.effective_to IS NULL OR v.effective_to >= c.work_date)
LEFT JOIN clinic_schedule_exception special
    ON special.work_date = c.work_date
   AND special.shift_id = t.shift_id
   AND special.exception_type = 'SPECIAL_HOURS'
   AND special.deleted = false
WHERE NOT EXISTS (
      SELECT 1 FROM clinic_schedule_exception e
      WHERE e.work_date = c.work_date AND e.deleted = false
        AND (e.exception_type = 'CLOSED_DAY'
             OR (e.exception_type = 'SHIFT_OFF' AND e.shift_id = t.shift_id))
  );

-- Cac bac si dai dien cua TAT CA phong kham va CLS duoc xep them ca chieu va
-- ca toi de de kiem thu toan bo luong. Hai ca bo sung la lich thuc te (khong
-- phai template), vi template chi cho phep mot ca cho moi nhan vien trong mot
-- ngay trong tuan.
WITH representative_doctors(staff_id) AS (
    VALUES
    ('91000000-0000-0000-0000-000000000001'::uuid), -- Internal room 1
    ('91000000-0000-0000-0000-000000000005'::uuid), -- Internal room 2
    ('90000010-3333-3333-3333-333333333333'::uuid), -- Surgery room 1
    ('91000000-0000-0000-0000-000000000004'::uuid), -- Procedure room
    ('91000000-0000-0000-0000-000000000002'::uuid), -- Pediatrics
    ('91000000-0000-0000-0000-000000000006'::uuid), -- Obstetrics
    ('91000000-0000-0000-0000-000000000003'::uuid), -- Dermatology
    ('93000000-0000-0000-0000-000000000002'::uuid), -- Hematology / microbiology / rapid test
    ('93000000-0000-0000-0000-000000000003'::uuid), -- Biochemistry / urinalysis
    ('91000000-0000-0000-0000-000000000007'::uuid), -- Ultrasound / ECG
    ('93000000-0000-0000-0000-000000000004'::uuid)  -- X-ray
), extra_shifts(shift_id) AS (
    VALUES
    ('70000002-2222-2222-2222-222222222222'::uuid),
    ('70000003-3333-3333-3333-333333333333'::uuid)
), calendar AS (
    SELECT d::date AS work_date
    FROM generate_series(
        date_trunc('week', CURRENT_DATE)::date,
        date_trunc('week', CURRENT_DATE)::date + 13,
        INTERVAL '1 day'
    ) d
)
INSERT INTO staff_schedule (
    schedule_id, created_at, updated_at, deleted, is_custom, note,
    status, work_date, shift_id, shift_version_id,
    actual_start_time, actual_end_time, staff_id, template_id
)
SELECT gen_random_uuid(), NOW(), NOW(), false, true,
       'Lich thuc te bo sung de kiem thu du ba ca', 'SCHEDULED', c.work_date,
       s.shift_id, v.shift_version_id,
       COALESCE(special.special_start_time, v.start_time),
       COALESCE(special.special_end_time, v.end_time),
       d.staff_id, NULL
FROM representative_doctors d
CROSS JOIN extra_shifts s
CROSS JOIN calendar c
JOIN shift_version v ON v.shift_id = s.shift_id
    AND v.effective_from <= c.work_date
    AND (v.effective_to IS NULL OR v.effective_to >= c.work_date)
LEFT JOIN clinic_schedule_exception special
    ON special.work_date = c.work_date
   AND special.shift_id = s.shift_id
   AND special.exception_type = 'SPECIAL_HOURS'
   AND special.deleted = false
WHERE NOT EXISTS (
      SELECT 1 FROM clinic_schedule_exception e
      WHERE e.work_date = c.work_date AND e.deleted = false
        AND (e.exception_type = 'CLOSED_DAY'
             OR (e.exception_type = 'SHIFT_OFF' AND e.shift_id = s.shift_id))
  )
  AND NOT EXISTS (
      SELECT 1 FROM staff_schedule existing
      WHERE existing.staff_id = d.staff_id
        AND existing.work_date = c.work_date
        AND existing.shift_id = s.shift_id
        AND existing.deleted = false
  );

-- Bao ve du lieu lich: mot nhan vien chi co mot ban ghi hoat dong cho cung ngay va ca.
-- Khoi nay idempotent, co the chay lai sau khi du lieu cu da tung bi lap.
WITH ranked_schedule AS (
    SELECT schedule_id,
           ROW_NUMBER() OVER (
               PARTITION BY staff_id, work_date, shift_id
               ORDER BY is_custom DESC, updated_at DESC, created_at DESC, schedule_id DESC
           ) AS duplicate_order
    FROM staff_schedule
    WHERE deleted = false AND shift_id IS NOT NULL
)
UPDATE staff_schedule schedule
SET deleted = true,
    updated_at = NOW()
FROM ranked_schedule ranked
WHERE schedule.schedule_id = ranked.schedule_id
  AND ranked.duplicate_order > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uk_staff_schedule_active_slot
    ON staff_schedule (staff_id, work_date, shift_id)
    WHERE deleted = false AND shift_id IS NOT NULL;

-- Kiem tra du lieu trinh dien: moi dich vu ACTIVE phai co it nhat mot nhan su
-- dung phong, dung chuyen khoa/nang luc trong tung ca cua 14 ngay mau.
DO $coverage_check$
DECLARE
    missing_coverage text;
BEGIN
    WITH calendar AS (
        SELECT d::date AS work_date
        FROM generate_series(
            date_trunc('week', CURRENT_DATE)::date,
            date_trunc('week', CURRENT_DATE)::date + 13,
            INTERVAL '1 day'
        ) d
    ), missing AS (
        SELECT ms.name AS service_name, c.work_date, sc.name AS shift_name
        FROM medical_service ms
        CROSS JOIN calendar c
        CROSS JOIN shift_config sc
        WHERE ms.deleted = false
          AND ms.status = 'ACTIVE'
          AND sc.deleted = false
          AND sc.is_active = true
          AND NOT EXISTS (
              SELECT 1
              FROM staff_schedule ss
              JOIN staff_info si ON si.staff_id = ss.staff_id AND si.deleted = false
              JOIN profile p ON p.profile_id = si.profile_id AND p.deleted = false
              JOIN account a ON a.account_id = p.account_id AND a.is_active = true
              WHERE ss.deleted = false
                AND ss.status = 'SCHEDULED'
                AND ss.work_date = c.work_date
                AND ss.shift_id = sc.shift_id
                AND (
                    (
                        ms.department_type = 'EXAMINATION'
                        AND si.system_role = 'DOCTOR'
                        AND si.specialization_id = ms.required_specialization_id
                        AND EXISTS (
                            SELECT 1 FROM department d
                            WHERE d.department_id = si.department_id
                              AND d.deleted = false AND d.status = 'AVAILABLE'
                              AND d.department_type = 'EXAMINATION'
                              AND d.specialization_id = ms.required_specialization_id
                        )
                    )
                    OR
                    (
                        ms.department_type = 'PARACLINICAL'
                        AND EXISTS (
                            SELECT 1
                            FROM staff_capability cap
                            JOIN department_capability dc
                              ON dc.department_id = si.department_id
                             AND dc.capability_id = cap.capability_id
                            JOIN department d ON d.department_id = dc.department_id
                            WHERE cap.staff_id = si.staff_id
                              AND cap.deleted = false
                              AND cap.status = 'ACTIVE'
                              AND (cap.expiry_date IS NULL OR cap.expiry_date >= c.work_date)
                              AND cap.capability_id = ms.required_capability_id
                              AND d.deleted = false AND d.status = 'AVAILABLE'
                        )
                    )
                )
          )
    )
    SELECT string_agg(service_name || ' - ' || to_char(work_date, 'DD/MM/YYYY') || ' - ' || shift_name, '; ')
    INTO missing_coverage
    FROM missing;

    IF missing_coverage IS NOT NULL THEN
        RAISE EXCEPTION 'Dữ liệu lịch chưa phủ đủ dịch vụ: %', missing_coverage;
    END IF;
END
$coverage_check$;

-- Kiem tra theo tung phong de man Admin/Clinic Manager khong con hien ca trong.
-- Moi phong kham va phong CLS deu bat buoc co bac si. Bac si phong CLS phai co
-- it nhat mot nang luc ACTIVE thuoc danh muc ky thuat cua phong; dieu duong va
-- ky thuat vien khong duoc tinh thay cho bac si trong phep kiem tra nay.
DO $room_coverage_check$
DECLARE
    missing_rooms text;
    multiple_room_doctors text;
    missing_operations text;
    duplicate_daily_shifts text;
BEGIN
    WITH calendar AS (
        SELECT d::date AS work_date
        FROM generate_series(
            date_trunc('week', CURRENT_DATE)::date,
            date_trunc('week', CURRENT_DATE)::date + 13,
            INTERVAL '1 day'
        ) d
    ), missing AS (
        SELECT d.room_code, d.name AS room_name, c.work_date, sc.name AS shift_name
        FROM department d
        CROSS JOIN calendar c
        CROSS JOIN shift_config sc
        WHERE d.deleted = false
          AND d.status = 'AVAILABLE'
          AND d.department_type IN ('EXAMINATION', 'PARACLINICAL')
          AND sc.deleted = false
          AND sc.is_active = true
          AND NOT EXISTS (
              SELECT 1
              FROM staff_schedule ss
              JOIN staff_info si ON si.staff_id = ss.staff_id AND si.deleted = false
              JOIN profile p ON p.profile_id = si.profile_id AND p.deleted = false
              JOIN account a ON a.account_id = p.account_id AND a.is_active = true
              WHERE ss.deleted = false
                AND ss.status = 'SCHEDULED'
                AND ss.work_date = c.work_date
                AND ss.shift_id = sc.shift_id
                AND si.department_id = d.department_id
                AND si.system_role = 'DOCTOR'
                AND (
                    d.department_type = 'EXAMINATION'
                    OR
                    (d.department_type = 'PARACLINICAL' AND EXISTS (
                        SELECT 1
                        FROM staff_capability cap
                        JOIN department_capability dc
                          ON dc.department_id = d.department_id
                         AND dc.capability_id = cap.capability_id
                        WHERE cap.staff_id = si.staff_id
                          AND cap.deleted = false
                          AND cap.status = 'ACTIVE'
                          AND (cap.expiry_date IS NULL OR cap.expiry_date >= c.work_date)
                    ))
                )
          )
    )
    SELECT string_agg(room_code || ' ' || room_name || ' - '
                      || to_char(work_date, 'DD/MM/YYYY') || ' - ' || shift_name, '; ')
    INTO missing_rooms
    FROM missing;

    IF missing_rooms IS NOT NULL THEN
        RAISE EXCEPTION 'Dữ liệu lịch chưa phủ đủ từng phòng: %', missing_rooms;
    END IF;

    SELECT string_agg(room_code || ' ' || room_name || ' (' || doctor_count || ' bác sĩ)', '; ')
    INTO multiple_room_doctors
    FROM (
        SELECT department.room_code,
               department.name AS room_name,
               COUNT(DISTINCT schedule.staff_id) AS doctor_count
        FROM department
        JOIN staff_info doctor
          ON doctor.department_id = department.department_id
         AND doctor.system_role = 'DOCTOR'
         AND doctor.deleted = false
        JOIN staff_schedule schedule
          ON schedule.staff_id = doctor.staff_id
         AND schedule.deleted = false
         AND schedule.status = 'SCHEDULED'
         AND schedule.work_date BETWEEN date_trunc('week', CURRENT_DATE)::date
                                    AND date_trunc('week', CURRENT_DATE)::date + 13
        WHERE department.deleted = false
          AND department.status = 'AVAILABLE'
          AND department.department_type IN ('EXAMINATION', 'PARACLINICAL')
        GROUP BY department.department_id, department.room_code, department.name
        HAVING COUNT(DISTINCT schedule.staff_id) <> 1
    ) room_doctor_counts;

    IF multiple_room_doctors IS NOT NULL THEN
        RAISE EXCEPTION 'Dữ liệu demo phải dùng đúng một bác sĩ cho mỗi phòng khám/CLS: %', multiple_room_doctors;
    END IF;

    WITH calendar AS (
        SELECT d::date AS work_date
        FROM generate_series(
            date_trunc('week', CURRENT_DATE)::date,
            date_trunc('week', CURRENT_DATE)::date + 13,
            INTERVAL '1 day'
        ) d
    ), required_roles(system_role, role_name) AS (
        VALUES ('RECEPTIONIST', 'Lễ tân'), ('CASHIER', 'Thu ngân')
    ), missing AS (
        SELECT required_roles.role_name, calendar.work_date, shift_config.name AS shift_name
        FROM calendar
        CROSS JOIN shift_config
        CROSS JOIN required_roles
        WHERE shift_config.deleted = false
          AND shift_config.is_active = true
          AND NOT EXISTS (
              SELECT 1
              FROM staff_schedule schedule
              JOIN staff_info staff
                ON staff.staff_id = schedule.staff_id
               AND staff.deleted = false
              JOIN profile staff_profile
                ON staff_profile.profile_id = staff.profile_id
               AND staff_profile.deleted = false
              JOIN account staff_account
                ON staff_account.account_id = staff_profile.account_id
               AND staff_account.is_active = true
              WHERE schedule.deleted = false
                AND schedule.status = 'SCHEDULED'
                AND schedule.work_date = calendar.work_date
                AND schedule.shift_id = shift_config.shift_id
                AND staff.system_role::text = required_roles.system_role
          )
    )
    SELECT string_agg(role_name || ' - ' || to_char(work_date, 'DD/MM/YYYY') || ' - ' || shift_name, '; ')
    INTO missing_operations
    FROM missing;

    IF missing_operations IS NOT NULL THEN
        RAISE EXCEPTION 'Dữ liệu lịch vận hành chưa phủ đủ 7 ngày: %', missing_operations;
    END IF;

    SELECT string_agg(staff_code || ' - ' || to_char(work_date, 'DD/MM/YYYY'), '; ')
    INTO duplicate_daily_shifts
    FROM (
        SELECT si.staff_code, ss.work_date
        FROM staff_schedule ss
        JOIN staff_info si ON si.staff_id = ss.staff_id
        WHERE ss.deleted = false AND ss.status = 'SCHEDULED'
          AND si.system_role <> 'DOCTOR'
          AND ss.work_date BETWEEN date_trunc('week', CURRENT_DATE)::date
                               AND date_trunc('week', CURRENT_DATE)::date + 13
        GROUP BY ss.staff_id, si.staff_code, ss.work_date
        HAVING COUNT(DISTINCT ss.shift_id) > 1
    ) duplicate_staff_days;

    IF duplicate_daily_shifts IS NOT NULL THEN
        RAISE EXCEPTION 'Nhân sự vận hành/CLS bị xếp nhiều ca trong cùng ngày: %', duplicate_daily_shifts;
    END IF;
END
$room_coverage_check$;

INSERT INTO profile (
    profile_id, account_id, created_at, updated_at, deleted, patient_code,
    full_name, date_of_birth, gender, phone, email, address, blood_type,
    insurance_id, allergies, height, weight
)
SELECT format('22000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       format('32000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       NOW(), NOW(), false, 'BN-' || to_char(CURRENT_DATE, 'YY') || '-' || lpad(i::text, 5, '0'),
       (ARRAY[
            'Nguyễn Thị Ánh', 'Trần Minh Anh', 'Lê Văn Khoa', 'Phạm Thị Huyền',
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
        CASE WHEN i IN (1,2,4,6,8,9,12,14,16,18,20) THEN 'FEMALE' ELSE 'MALE' END,
        (ARRAY[
            '0944433222','0982143657','0975318246','0964182735','0936251748',
            '0927314856','0918462753','0909573146','0862147539','0853264718',
            '0845372619','0836481725','0827591634','0812635479','0793146285',
            '0784257391','0775368412','0766479523','0752184369','0743295178'
        ])[i],
        (ARRAY[
            'nguyenthianh94@gmail.com','tranminhanh88@gmail.com','levankhoa76@gmail.com','phamthihuyen95@gmail.com',
            'hoangquocbao82@gmail.com','vuthilan99@gmail.com','dangminhtuan68@gmail.com','nguyenngocmai01@gmail.com',
            'trangiahan18@gmail.com','lehoangphuc14@gmail.com','phamducanh91@gmail.com','buithanhthao85@gmail.com',
            'dominhkhoi79@gmail.com','ngothutrang97@gmail.com','duongquochuy06@gmail.com','maiphuonglinh93@gmail.com',
            'phananhkhoa87@gmail.com','trinhmyduyen00@gmail.com','luongthanhdat72@gmail.com','tabaongoc16@gmail.com'
        ])[i],
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
       CASE
            WHEN i IN (1,2,5,7) THEN NULL
            WHEN i = 4 THEN 'Penicillin'
            WHEN i = 8 THEN 'Hải sản'
            WHEN i = 12 THEN 'Ibuprofen'
            ELSE ''
       END,
       CASE WHEN i IN (9,10,20) THEN 120 + i ELSE 155 + (i % 20) END,
       CASE WHEN i IN (9,10,20) THEN 22 + i ELSE 48 + (i % 30) END
FROM generate_series(1, 12) AS g(i);

INSERT INTO profile (
    profile_id, account_id, created_at, updated_at, deleted, patient_code,
    full_name, date_of_birth, gender, phone, email, address, blood_type,
    insurance_id, allergies, height, weight
) VALUES (
    '22f00000-0000-0000-0000-000000000001',
    '32f00000-0000-0000-0000-000000000001',
    NOW(), NOW(), false, 'BN-NEW-00001',
    'Người dùng mới', DATE '2000-01-01', 'MALE', NULL,
    'chuoidochoduoi7e@gmail.com', NULL, NULL,
    NULL, NULL, NULL, NULL
);

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
-- 12 lich hen chinh: cho dat truoc, check-in va lich su kham
-- ===================================================================
INSERT INTO appointment (
    appointment_id, created_at, updated_at, deleted, scheduled_at, status,
    is_guest, customer_id, guest_full_name, guest_phone, guest_email,
    guest_age, guest_gender, guest_address, shift_name, shift_time,
    shift_version_id, cancel_reason
)
SELECT format('51000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       NOW() - (i || ' hours')::interval, NOW(), false,
       CASE
           WHEN i <= 8 THEN date_trunc('week', CURRENT_DATE)::date + 7 + ((i - 1) / 4)
                + CASE WHEN i = 5 OR i % 2 = 0 THEN TIME '10:30' ELSE TIME '06:30' END
           ELSE date_trunc('week', CURRENT_DATE)::date
                + CASE WHEN i % 2 = 1 THEN TIME '06:15' ELSE TIME '14:15' END
                + ((i - 9) / 2) * INTERVAL '30 minutes'
       END,
       CASE WHEN i <= 8 THEN 'PENDING' ELSE 'CHECKED_IN' END,
       false,
       format('22000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       NULL, NULL, NULL, NULL, NULL, NULL,
       CASE WHEN i = 5 OR i % 2 = 0 THEN 'Ca Chiều' ELSE 'Ca Sáng' END,
       CASE WHEN i = 5 OR i % 2 = 0 THEN '08:00 - 16:00' ELSE '00:00 - 08:00' END,
       CASE WHEN i = 5 OR i % 2 = 0 THEN '71000002-2222-2222-2222-222222222222'::uuid
            ELSE '71000001-1111-1111-1111-111111111111'::uuid END,
       NULL
FROM generate_series(1, 12) AS g(i);

-- Hai lich hen ca toi: kham Noi khoa va xet nghiem co nhan su phu trach.
INSERT INTO appointment (
    appointment_id, created_at, updated_at, deleted, scheduled_at, status,
    is_guest, customer_id, shift_name, shift_time, shift_version_id, cancel_reason
) VALUES
('51000000-0000-0000-0000-000000000081', NOW(), NOW(), false,
 date_trunc('week', CURRENT_DATE)::date + 7 + TIME '18:15', 'PENDING', false,
 '22000000-0000-0000-0000-000000000001', 'Ca Tối', '16:00 - 23:59',
 '71000003-3333-3333-3333-333333333333', NULL),
('51000000-0000-0000-0000-000000000082', NOW(), NOW(), false,
 date_trunc('week', CURRENT_DATE)::date + 8 + TIME '18:30', 'PENDING', false,
 '22000000-0000-0000-0000-000000000002', 'Ca Tối', '16:00 - 23:59',
 '71000003-3333-3333-3333-333333333333', NULL);

INSERT INTO appointment_services (appointment_id, service_id)
SELECT format('51000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       ('400000' || lpad(service_no::text, 2, '0') || '-0000-0000-0000-' || lpad(service_no::text, 12, '0'))::uuid
FROM (
    SELECT i, (ARRAY[2,2,1,4,6,7,2,2,3,3,2,4])[i] AS service_no
    FROM generate_series(1, 12) AS g(i)
) s;

INSERT INTO appointment_services (appointment_id, service_id) VALUES
('51000000-0000-0000-0000-000000000081', '40000002-0000-0000-0000-000000000002'),
('51000000-0000-0000-0000-000000000082', '40000008-0000-0000-0000-000000000008');

-- Hai lich hen lich su de review thao tac huy va doi lich.
INSERT INTO appointment (
    appointment_id, created_at, updated_at, deleted, scheduled_at, status,
    is_guest, customer_id, shift_name, shift_time, shift_version_id, cancel_reason
) VALUES
('51000000-0000-0000-0000-000000000091', NOW() - INTERVAL '4 days', NOW() - INTERVAL '3 days', false,
 date_trunc('week', CURRENT_DATE)::date - 3 + TIME '06:30', 'CANCELLED', false,
 '22000000-0000-0000-0000-000000000011', 'Ca Sáng', '00:00 - 08:00',
 '71000001-1111-1111-1111-111111111111', 'Khách hàng bận việc gia đình'),
('51000000-0000-0000-0000-000000000092', NOW() - INTERVAL '3 days', NOW() - INTERVAL '2 days', false,
 date_trunc('week', CURRENT_DATE)::date + 8 + TIME '14:30', 'RESCHEDULED', false,
 '22000000-0000-0000-0000-000000000012', 'Ca Chiều', '08:00 - 16:00',
 '71000002-2222-2222-2222-222222222222', NULL);

INSERT INTO appointment_services (appointment_id, service_id) VALUES
('51000000-0000-0000-0000-000000000091', '40000002-0000-0000-0000-000000000002'),
('51000000-0000-0000-0000-000000000092', '40000004-0000-0000-0000-000000000004');

-- ===================================================================
-- 12 luot kham cho dung 12 benh nhan: moi benh nhan co san dich vu,
-- hoa don, hang doi va benh an. Bon luot da hoan thanh lien ket lich hen;
-- cac luot con lai la tiep nhan truc tiep de bac si co the mo kham ngay.
-- ===================================================================
INSERT INTO customer_visit (
    visit_id, created_at, updated_at, deleted, check_in_time, check_out_time,
    status, appointment_id, checked_in_by, customer_id
)
SELECT format('52000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       NOW() - ((i + 2) || ' hours')::interval, NOW(), false,
       CASE WHEN i <= 8 THEN GREATEST(
                    date_trunc('day', NOW()),
                    NOW() - (i * INTERVAL '3 minutes')
                )
            ELSE date_trunc('week', CURRENT_DATE)::date - ((i - 4) / 4) + TIME '08:00' + ((i - 5) % 4) * INTERVAL '40 minutes' END,
       CASE WHEN i BETWEEN 9 AND 12 THEN
            date_trunc('week', CURRENT_DATE)::date - ((i - 4) / 4) + TIME '10:30' + ((i - 9) % 4) * INTERVAL '40 minutes'
            ELSE NULL END,
       CASE WHEN i <= 4 THEN 'IN_PROGRESS' WHEN i <= 8 THEN 'CHECKED_IN'
            ELSE 'COMPLETED' END,
       CASE WHEN i >= 9 THEN format('51000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid ELSE NULL END,
       '90000012-5555-5555-5555-555555555555',
       format('22000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid
FROM generate_series(1, 12) AS g(i);

-- Tao du lieu hoa don -> thanh toan -> hang cho -> ho so -> can lam sang
-- sau khi toan bo khoa ngoai nen da ton tai.
-- ===================================================================
-- 12 hoa don: 10 da thanh toan, 1 da huy va 1 dang cho thanh toan.
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
       NULL, NOW() - ((12 - i) || ' hours')::interval, CURRENT_DATE + 1,
       ms.price, 0, 0, ms.price,
       CASE WHEN i <= 10 THEN ms.price ELSE 0 END,
       CASE WHEN i <= 10 THEN 'PAID' WHEN i = 11 THEN 'CANCELLED' ELSE 'PENDING' END,
       CASE WHEN i <= 10 THEN 'Đã thu đủ chi phí dịch vụ' WHEN i = 11 THEN 'Phiếu khám đã hủy' ELSE 'Chờ khách hàng thanh toán' END,
       '90000013-6666-6666-6666-666666666666', NOW(), NOW(), false
FROM generate_series(1, 12) AS g(i)
JOIN medical_service ms
  ON ms.service_id = format('400000%s-0000-0000-0000-%s',
       lpad((ARRAY[2,2,1,4,5,6,1,2,3,3,2,4])[i]::text, 2, '0'),
       lpad((ARRAY[2,2,1,4,5,6,1,2,3,3,2,4])[i]::text, 12, '0'))::uuid;

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
FROM generate_series(1, 12) AS g(i)
JOIN medical_service ms
  ON ms.service_id = format('400000%s-0000-0000-0000-%s',
       lpad((ARRAY[2,2,1,4,5,6,1,2,3,3,2,4])[i]::text, 2, '0'),
       lpad((ARRAY[2,2,1,4,5,6,1,2,3,3,2,4])[i]::text, 12, '0'))::uuid;

-- Dich vu can lam sang di kem cac luot kham mau.
INSERT INTO invoice_item (
    item_id, invoice_id, service_id, service_snapshot, service_code_snapshot,
    unit_price, quantity, discount_percent, discount_amount, final_price,
    line_total, note, bhyt_fund, created_at, updated_at, deleted
)
SELECT format('54100000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       format('53000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       ms.service_id, ms.name, ms.service_code, ms.price, 1, 0, 0,
       ms.price, ms.price, 'Dịch vụ cận lâm sàng', 0, NOW(), NOW(), false
FROM generate_series(1, 12) AS g(i)
JOIN medical_service ms
  ON ms.service_id = format('400000%s-0000-0000-0000-%s',
       lpad((ARRAY[8,12,15,17,19,8,10,12,15,17,19,20])[i]::text, 2, '0'),
       lpad((ARRAY[8,12,15,17,19,8,10,12,15,17,19,20])[i]::text, 12, '0'))::uuid;

-- Cap nhat tong hoa don co them dich vu can lam sang.
UPDATE invoice inv
SET subtotal = inv.subtotal + ii.line_total,
    total_amount = inv.total_amount + ii.line_total,
    paid_amount = CASE WHEN inv.status = 'PAID' THEN inv.paid_amount + ii.line_total ELSE inv.paid_amount END,
    updated_at = NOW()
FROM invoice_item ii
WHERE ii.invoice_id = inv.invoice_id
  AND ii.item_id::text LIKE '54100000-%';

-- 10 giao dich thanh toan thanh cong.
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
       CASE WHEN i % 3 = 0 THEN 'VCB' || to_char(CURRENT_DATE, 'YYMMDD') || lpad(i::text, 6, '0') ELSE NULL END,
       CASE WHEN i % 3 = 0 THEN 'Thanh toán chuyển khoản đã đối soát'
            WHEN i % 3 = 1 THEN 'Thanh toán tiền mặt tại quầy'
            ELSE 'Thanh toán bằng thẻ tại quầy' END,
       '90000013-6666-6666-6666-666666666666', NOW(), NOW(), false
FROM generate_series(1, 10) AS g(i)
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
       CASE (ARRAY[2,2,1,4,5,6,1,2,3,3,2,4])[i]
           WHEN 1 THEN '33333333-3333-3333-3333-333333333333'::uuid
           WHEN 2 THEN '77777777-7777-7777-7777-777777777777'::uuid
           WHEN 3 THEN '66666666-6666-6666-6666-666666666666'::uuid
           WHEN 4 THEN '88888888-8888-8888-8888-888888888888'::uuid
           WHEN 5 THEN '99999999-9999-9999-9999-999999999999'::uuid
           WHEN 6 THEN 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'::uuid
           ELSE 'dddddddd-dddd-dddd-dddd-dddddddddddd'::uuid
       END,
       v.check_in_time::date, i,
       CASE WHEN i <= 4 THEN 'IN_PROGRESS' WHEN i <= 8 THEN 'WAITING' ELSE 'DONE' END,
       CASE WHEN i <= 4 OR i >= 9 THEN v.check_in_time + INTERVAL '10 minutes' ELSE NULL END,
       CASE WHEN i >= 9 THEN v.check_in_time + INTERVAL '50 minutes' ELSE NULL END,
       format('400000%s-0000-0000-0000-%s',
           lpad((ARRAY[2,2,1,4,5,6,1,2,3,3,2,4])[i]::text, 2, '0'),
           lpad((ARRAY[2,2,1,4,5,6,1,2,3,3,2,4])[i]::text, 12, '0'))::uuid,
       NOW(), NOW(), false
FROM generate_series(1, 12) AS g(i)
JOIN customer_visit v ON v.visit_id = format('52000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid;

-- 12 ho so benh an tuong ung hang cho kham.
INSERT INTO medical_record (
    record_id, record_version, record_code, visit_id, queue_ticket_id, doctor_id,
    chief_complaint, clinical_findings, diagnosis, prescription_note, conclusion,
    patient_instruction, status, completed_at, contact_requested,
    created_at, updated_at, deleted
)
SELECT format('56000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       0, 'MR-' || to_char(CURRENT_DATE, 'YYYY') || '-' || lpad(i::text, 5, '0'),
       q.visit_id, q.ticket_id, d.head_doctor_id,
        (ARRAY['Đau đầu kèm mệt mỏi trong ba ngày','Ho khan, đau rát họng và nghẹt mũi','Đau âm ỉ vùng thượng vị sau ăn','Nổi mẩn đỏ và ngứa hai cẳng tay','Đau vùng thắt lưng khi vận động','Hồi hộp và đau ngực nhẹ khi gắng sức','Khám sức khỏe định kỳ'])[(CASE WHEN i IN (3, 7) THEN 1 ELSE ((i - 1) % 7) + 1 END)],
        CASE WHEN i >= 9 THEN (ARRAY[
            'Tỉnh táo, niêm mạc hồng, không ghi nhận dấu hiệu thần kinh khu trú.',
            'Họng sung huyết nhẹ, phổi thông khí đều, chưa ghi nhận ran.',
            'Bụng mềm, ấn tức nhẹ vùng thượng vị, không phản ứng thành bụng.',
            'Sẩn đỏ khu trú hai cẳng tay, không rỉ dịch, không dấu nhiễm trùng.',
            'Đau cạnh sống thắt lưng, vận động hạn chế nhẹ, không yếu liệt chi.',
            'Nhịp tim đều, huyết áp ổn định, chưa ghi nhận dấu suy tim.',
            'Thể trạng tốt, các chỉ số sinh tồn trong giới hạn bình thường.'
        ])[(CASE WHEN i IN (3, 7) THEN 1 ELSE ((i - 1) % 7) + 1 END)] ELSE 'Đang chờ bác sĩ hoàn thiện kết quả khám.' END,
        CASE WHEN i >= 9 THEN (ARRAY[
            'Đau đầu căng thẳng', 'Viêm đường hô hấp trên cấp', 'Rối loạn tiêu hóa chức năng',
            'Viêm da tiếp xúc', 'Đau thắt lưng cơ học', 'Theo dõi rối loạn nhịp tim',
            'Khám sức khỏe tổng quát'
        ])[(CASE WHEN i IN (3, 7) THEN 1 ELSE ((i - 1) % 7) + 1 END)] ELSE NULL END,
        CASE WHEN i >= 9 THEN 'Sử dụng thuốc đúng liều ghi trong đơn; không tự ý tăng liều hoặc dùng thêm thuốc khác.' ELSE NULL END,
        CASE WHEN i >= 9 THEN 'Tình trạng ổn định, đủ điều kiện theo dõi và điều trị ngoại trú.' ELSE NULL END,
        CASE WHEN i >= 9 THEN 'Nghỉ ngơi, ăn uống phù hợp và tái khám ngay nếu triệu chứng tăng hoặc xuất hiện dấu hiệu bất thường.' ELSE NULL END,
       CASE WHEN i <= 4 THEN 'IN_PROGRESS' WHEN i <= 8 THEN 'DRAFT' ELSE 'COMPLETED' END,
       CASE WHEN i >= 9 THEN q.completed_at ELSE NULL END,
       false, NOW(), NOW(), false
FROM generate_series(1, 12) AS g(i)
JOIN queue_ticket q ON q.ticket_id = format('55000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid
JOIN department d ON d.department_id = q.department_id;

UPDATE invoice inv
SET medical_record_id = format('56000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
    updated_at = NOW()
FROM generate_series(1, 12) AS g(i)
WHERE inv.invoice_id = format('53000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid;

-- 12 bo chi so sinh hieu.
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
FROM generate_series(1, 12) AS g(i);

UPDATE medical_record mr
SET vital_signs_id = format('56100000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
    updated_at = NOW()
FROM generate_series(1, 12) AS g(i)
WHERE mr.record_id = format('56000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid;

-- ===================================================================
-- 12 hang cho va yeu cau can lam sang.
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
           WHEN 14 THEN 'cccccccc-cccc-cccc-cccc-cccccccccccc'::uuid
           WHEN 15 THEN '55555555-5555-5555-5555-555555555555'::uuid
           WHEN 16 THEN '55555555-5555-5555-5555-555555555555'::uuid
           WHEN 17 THEN 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'::uuid
           WHEN 18 THEN 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'::uuid
           WHEN 19 THEN '55555555-5555-5555-5555-555555555555'::uuid
           WHEN 20 THEN '44444444-4444-4444-4444-444444444444'::uuid
           ELSE '55555555-5555-5555-5555-555555555555'::uuid
       END,
       v.check_in_time::date, i,
       CASE WHEN i <= 2 THEN 'BLOCKED' WHEN i = 3 THEN 'WAITING'
            WHEN i = 4 THEN 'IN_PROGRESS' ELSE 'DONE' END,
       CASE WHEN i >= 4 THEN v.check_in_time + INTERVAL '60 minutes' ELSE NULL END,
       CASE WHEN i >= 5 THEN v.check_in_time + INTERVAL '85 minutes' ELSE NULL END,
       format('400000%s-0000-0000-0000-%s', lpad(service_no::text, 2, '0'), lpad(service_no::text, 12, '0'))::uuid,
       NOW(), NOW(), false
FROM (
    SELECT i, (ARRAY[8,12,15,17,19,8,10,12,15,17,19,20])[i] AS service_no
    FROM generate_series(1, 12) AS g(i)
) s
JOIN customer_visit v ON v.visit_id = format('52000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid;

INSERT INTO test_request (
    test_request_id, medical_record_id, service_id, performing_department,
    queue_ticket_id, description, status, requested_by, completed_at,
    performed_at, cancel_reason, invoice_item_id, created_at, updated_at, deleted
)
SELECT format('57000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       mr.record_id, q.service_id, q.department_id, q.ticket_id,
        'Chỉ định ' || lower(ms.name) || ' để hỗ trợ chẩn đoán và theo dõi điều trị.',
       CASE WHEN i <= 2 THEN 'BLOCKED' WHEN i = 3 THEN 'PENDING'
            WHEN i = 4 THEN 'IN_PROGRESS' ELSE 'COMPLETED' END,
       mr.doctor_id,
       CASE WHEN i >= 5 THEN q.completed_at ELSE NULL END,
       CASE WHEN i >= 4 THEN COALESCE(q.called_at, NOW()) ELSE NULL END,
       NULL,
       format('54100000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       NOW(), NOW(), false
 FROM generate_series(1, 12) AS g(i)
 JOIN medical_record mr ON mr.record_id = format('56000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid
 JOIN queue_ticket q ON q.ticket_id = format('55100000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid
 JOIN medical_service ms ON ms.service_id = q.service_id;

-- Ket qua cho cac yeu cau da hoan thanh.
INSERT INTO test_result (
    result_id, test_request_id, image_url, conclusion, sample_id,
    sample_type, sample_status, collected_at, collected_by,
    performed_by, performed_at, verified_by, verified_at,
    created_at, updated_at, deleted
)
SELECT format('58000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       tr.test_request_id,
       NULL,
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
FROM generate_series(5, 12) AS g(i)
JOIN test_request tr ON tr.test_request_id = format('57000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid
JOIN queue_ticket q ON q.ticket_id = tr.queue_ticket_id;

-- Bộ phiếu labo chuyên biệt để trình diễn đủ tám form kết quả. Các phiếu này
-- không tạo thêm hóa đơn: chúng mô phỏng chỉ định đã được thanh toán trong lượt.
INSERT INTO test_request (
    test_request_id, medical_record_id, service_id, performing_department,
    queue_ticket_id, description, status, requested_by, completed_at,
    performed_at, cancel_reason, invoice_item_id, created_at, updated_at, deleted
)
SELECT format('57200000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       mr.record_id,
       format('400000%s-0000-0000-0000-%s', lpad(service_no::text, 2, '0'), lpad(service_no::text, 12, '0'))::uuid,
       CASE WHEN service_no IN (8,20) THEN '44444444-4444-4444-4444-444444444444'::uuid
            ELSE 'cccccccc-cccc-cccc-cccc-cccccccccccc'::uuid END,
       NULL,
       'Phiếu trình diễn dữ liệu có cấu trúc cho ' || ms.name,
       CASE WHEN service_no = 20 THEN 'IN_PROGRESS' ELSE 'COMPLETED' END,
       mr.doctor_id,
       CASE WHEN service_no = 20 THEN NULL ELSE NOW() - INTERVAL '20 minutes' END,
       NOW() - INTERVAL '35 minutes', NULL, NULL, NOW(), NOW(), false
FROM (VALUES (1,8),(2,9),(3,10),(4,11),(5,12),(6,13),(7,14),(8,20)) seed(i,service_no)
JOIN medical_record mr ON mr.record_id = format('56000000-0000-0000-0000-%s', lpad((seed.i + 4)::text, 12, '0'))::uuid
JOIN medical_service ms ON ms.service_id = format('400000%s-0000-0000-0000-%s', lpad(service_no::text, 2, '0'), lpad(service_no::text, 12, '0'))::uuid
ON CONFLICT (test_request_id) DO NOTHING;

INSERT INTO test_result (
    result_id, test_request_id, image_url, conclusion, sample_id,
    sample_type, sample_status, collected_at, collected_by,
    performed_by, performed_at, verified_by, verified_at,
    created_at, updated_at, deleted
)
SELECT format('58200000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       req.test_request_id, NULL,
       CASE WHEN service_no = 20 THEN 'Bản nháp có control không hợp lệ, chưa được phép ký.'
            ELSE 'Kết quả mẫu được nhập bằng dữ liệu có cấu trúc.' END,
       'SMP-LAB-' || to_char(CURRENT_DATE, 'YYMMDD') || '-' || lpad(i::text, 3, '0'),
       CASE WHEN service_no = 13 THEN 'URINE' ELSE 'BLOOD' END,
       'ACCEPTED', NOW() - INTERVAL '45 minutes',
       '90000011-4444-4444-4444-444444444444'::uuid,
       CASE WHEN service_no IN (8,20) THEN '93000000-0000-0000-0000-000000000002'::uuid
            ELSE '93000000-0000-0000-0000-000000000003'::uuid END,
       req.performed_at,
       CASE WHEN service_no = 20 THEN NULL
            WHEN service_no = 8 THEN '93000000-0000-0000-0000-000000000002'::uuid
            ELSE '93000000-0000-0000-0000-000000000003'::uuid END,
       CASE WHEN service_no = 20 THEN NULL ELSE req.completed_at END,
       NOW(), NOW(), false
FROM (VALUES (1,8),(2,9),(3,10),(4,11),(5,12),(6,13),(7,14),(8,20)) seed(i,service_no)
JOIN test_request req ON req.test_request_id = format('57200000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid
ON CONFLICT (test_request_id) DO NOTHING;

-- Hai dong thuoc cho moi ho so da hoan thanh (benh nhan da xac minh di ung).
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
FROM generate_series(1, 8) AS g(n);

-- Thong bao trong ung dung cho 12 benh nhan.
INSERT INTO notification (
    notification_id, recipient_id, notification_type, channel, title, content,
    related_entity, related_entity_id, status, sent_at, read_at,
    failure_reason, created_at, updated_at, deleted
)
SELECT format('5a000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       format('22000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       CASE WHEN i <= 8 THEN 'APPOINTMENT_CONFIRMED'
            WHEN i <= 10 THEN 'PAYMENT_SUCCESS' ELSE 'GENERAL' END,
       'IN_APP',
       CASE WHEN i <= 8 THEN 'Lịch hẹn đã được xác nhận'
            WHEN i <= 10 THEN 'Thanh toán thành công'
            WHEN i = 11 THEN 'Lịch khám đã được hủy'
            ELSE 'Hóa đơn đang chờ thanh toán' END,
       CASE WHEN i <= 8 THEN 'Vui lòng đến trước giờ hẹn 15 phút để làm thủ tục.'
            WHEN i <= 10 THEN 'Hóa đơn dịch vụ của bạn đã được thanh toán.'
            WHEN i = 11 THEN 'Lịch khám đã được hủy theo yêu cầu.'
            ELSE 'Vui lòng hoàn tất thanh toán trước khi sử dụng dịch vụ.' END,
       CASE WHEN i <= 8 THEN 'Appointment' ELSE 'Invoice' END,
       CASE WHEN i <= 8
            THEN format('51000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid
            ELSE format('53000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid END,
       CASE WHEN i % 3 = 0 THEN 'READ' ELSE 'SENT' END,
       NOW() - (i || ' hours')::interval,
       CASE WHEN i % 3 = 0 THEN NOW() - ((i - 1) || ' hours')::interval ELSE NULL END,
       NULL, NOW(), NOW(), false
FROM generate_series(1, 12) AS g(i);

-- Audit log cho cac moc quan trong cua luong kham.
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
        '10.10.20.' || (10 + i), 'CareS Web/1.0', NULL, NULL,
        CASE WHEN i <= 6 THEN 'Thu ngân xác nhận thanh toán hóa đơn tại quầy'
             WHEN i <= 12 THEN 'Nhân viên y tế gọi bệnh nhân theo số thứ tự'
             WHEN i <= 16 THEN 'Bác sĩ bắt đầu tiếp nhận và khám bệnh'
             ELSE 'Bác sĩ hoàn tất hồ sơ khám và hướng dẫn điều trị' END,
       NOW() - ((12 - i) || ' minutes')::interval, false
FROM generate_series(1, 12) AS g(i);
-- ===================================================================
-- Published dynamic clinical forms. Eight laboratory forms receive a
-- controlled system version below; other forms remain manager-owned.
-- The finalized medical_service rows above are not changed.
-- ===================================================================
INSERT INTO clinical_form_template
(template_id, created_at, updated_at, deleted, code, name, context, description, active)
VALUES
('cf000006-0000-0000-0000-000000000006',NOW(),NOW(),false,'LAB_CBC','Kết quả Công thức máu','LAB_RESULT','RBC, HGB, HCT, WBC và PLT',true),
('cf000007-0000-0000-0000-000000000007',NOW(),NOW(),false,'LAB_GLUCOSE','Kết quả Đường huyết','LAB_RESULT','Glucose máu',true),
('cf000008-0000-0000-0000-000000000008',NOW(),NOW(),false,'LAB_BIOCHEM','Kết quả Sinh hóa máu','LAB_RESULT','Các chỉ số sinh hóa máu cơ bản',true),
('cf000009-0000-0000-0000-000000000009',NOW(),NOW(),false,'LAB_LIVER','Kết quả Chức năng gan','LAB_RESULT','AST, ALT, GGT, Bilirubin và Albumin',true),
('cf00000a-0000-0000-0000-00000000000a',NOW(),NOW(),false,'LAB_KIDNEY','Kết quả Chức năng thận','LAB_RESULT','Ure, Creatinine và eGFR ước tính',true),
('cf00000b-0000-0000-0000-00000000000b',NOW(),NOW(),false,'LAB_URINALYSIS','Kết quả Nước tiểu','LAB_RESULT','LEU, PRO, pH, BLD và GLU',true),
('cf00000c-0000-0000-0000-00000000000c',NOW(),NOW(),false,'LAB_CRP','Kết quả CRP','LAB_RESULT','CRP định lượng',true),
('cf00000d-0000-0000-0000-00000000000d',NOW(),NOW(),false,'LAB_RAPID_INFECTIOUS','Kết quả Test nhanh','LAB_RESULT','Các test nhanh bệnh truyền nhiễm',true),
('cf00000e-0000-0000-0000-00000000000e',NOW(),NOW(),false,'IMG_XRAY','Kết quả X-quang','IMAGING_RESULT','Mô tả tổn thương và kết luận X-quang',true),
('cf00000f-0000-0000-0000-00000000000f',NOW(),NOW(),false,'IMG_ABDOMINAL_US','Siêu âm ổ bụng','IMAGING_RESULT','Mô tả các tạng và kết luận',true),
('cf000010-0000-0000-0000-000000000010',NOW(),NOW(),false,'IMG_THYROID_US','Siêu âm tuyến giáp','IMAGING_RESULT','Nhân giáp và TI-RADS',true),
('cf000011-0000-0000-0000-000000000011',NOW(),NOW(),false,'IMG_OBSTETRIC_US','Siêu âm thai','IMAGING_RESULT','Sinh trắc thai, GA và EFW ước tính',true),
('cf000012-0000-0000-0000-000000000012',NOW(),NOW(),false,'ECG_STANDARD','Kết quả Điện tim','ECG_RESULT','Tần số tim, loại nhịp và kết luận',true)
ON CONFLICT (code) DO NOTHING;

INSERT INTO clinical_form_template_version
(version_id, created_at, updated_at, deleted, template_id, version_no, schema_json, status,
 change_reason, effective_from, created_by, published_by, published_at)
VALUES
('cf100006-0000-0000-0000-000000000006',NOW(),NOW(),false,'cf000006-0000-0000-0000-000000000006',1,$j${"fields":[{"key":"rbc","label":"RBC","type":"NUMBER","unit":"10^12/L","group":"Công thức máu","displayOrder":1,"required":true},{"key":"hgb","label":"HGB","type":"NUMBER","unit":"g/L","group":"Công thức máu","displayOrder":2,"required":true},{"key":"hct","label":"HCT","type":"NUMBER","unit":"%","group":"Công thức máu","displayOrder":3,"required":true},{"key":"wbc","label":"WBC","type":"NUMBER","unit":"10^9/L","group":"Công thức máu","displayOrder":4,"required":true},{"key":"plt","label":"PLT","type":"NUMBER","unit":"10^9/L","group":"Công thức máu","displayOrder":5,"required":true}]}$j$::jsonb,'PUBLISHED','Khoảng tham chiếu do quản lý phòng xét nghiệm cấu hình',CURRENT_DATE,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',NOW()),
('cf100007-0000-0000-0000-000000000007',NOW(),NOW(),false,'cf000007-0000-0000-0000-000000000007',1,$j${"fields":[{"key":"glucose","label":"Glucose","type":"NUMBER","unit":"mmol/L","group":"Đường huyết","displayOrder":1,"required":true}]}$j$::jsonb,'PUBLISHED','Khởi tạo dữ liệu mẫu chưa có ngưỡng vận hành',CURRENT_DATE,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',NOW()),
('cf100008-0000-0000-0000-000000000008',NOW(),NOW(),false,'cf000008-0000-0000-0000-000000000008',1,$j${"fields":[{"key":"glucose","label":"Glucose","type":"NUMBER","unit":"mmol/L","group":"Sinh hóa","displayOrder":1},{"key":"hba1c","label":"HbA1c","type":"NUMBER","unit":"%","group":"Sinh hóa","displayOrder":2},{"key":"cholesterol","label":"Cholesterol","type":"NUMBER","unit":"mmol/L","group":"Mỡ máu","displayOrder":3},{"key":"triglyceride","label":"Triglyceride","type":"NUMBER","unit":"mmol/L","group":"Mỡ máu","displayOrder":4},{"key":"hdlC","label":"HDL-C","type":"NUMBER","unit":"mmol/L","group":"Mỡ máu","displayOrder":5},{"key":"ldlC","label":"LDL-C","type":"NUMBER","unit":"mmol/L","group":"Mỡ máu","displayOrder":6},{"key":"acidUric","label":"Acid Uric","type":"NUMBER","unit":"umol/L","group":"Sinh hóa","displayOrder":7}]}$j$::jsonb,'PUBLISHED','Khởi tạo dữ liệu mẫu chưa có ngưỡng vận hành',CURRENT_DATE,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',NOW()),
('cf100009-0000-0000-0000-000000000009',NOW(),NOW(),false,'cf000009-0000-0000-0000-000000000009',1,$j${"fields":[{"key":"ast","label":"AST","type":"NUMBER","unit":"U/L","group":"Chức năng gan","displayOrder":1},{"key":"alt","label":"ALT","type":"NUMBER","unit":"U/L","group":"Chức năng gan","displayOrder":2},{"key":"ggt","label":"GGT","type":"NUMBER","unit":"U/L","group":"Chức năng gan","displayOrder":3},{"key":"bilirubinTotal","label":"Bilirubin Total","type":"NUMBER","unit":"umol/L","group":"Chức năng gan","displayOrder":4},{"key":"albumin","label":"Albumin","type":"NUMBER","unit":"g/L","group":"Chức năng gan","displayOrder":5}]}$j$::jsonb,'PUBLISHED','Khởi tạo dữ liệu mẫu chưa có ngưỡng vận hành',CURRENT_DATE,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',NOW()),
('cf10000a-0000-0000-0000-00000000000a',NOW(),NOW(),false,'cf00000a-0000-0000-0000-00000000000a',1,$j${"fields":[{"key":"ure","label":"Ure","type":"NUMBER","unit":"mmol/L","group":"Chức năng thận","displayOrder":1},{"key":"creatinine","label":"Creatinine","type":"NUMBER","unit":"umol/L","group":"Chức năng thận","displayOrder":2,"required":true,"min":1},{"key":"egfr","label":"eGFR ước tính","type":"NUMBER","unit":"mL/min/1.73m2","group":"Chức năng thận","displayOrder":3,"precision":2,"calculatorKey":"EGFR_CKD_EPI_2021_V1"}]}$j$::jsonb,'PUBLISHED','Khởi tạo công thức eGFR CKD-EPI 2021',CURRENT_DATE,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',NOW()),
('cf10000b-0000-0000-0000-00000000000b',NOW(),NOW(),false,'cf00000b-0000-0000-0000-00000000000b',1,$j${"fields":[{"key":"leu","label":"LEU","type":"TEXT","group":"Nước tiểu","displayOrder":1},{"key":"pro","label":"PRO","type":"TEXT","group":"Nước tiểu","displayOrder":2},{"key":"ph","label":"pH","type":"NUMBER","group":"Nước tiểu","displayOrder":3},{"key":"bld","label":"BLD","type":"TEXT","group":"Nước tiểu","displayOrder":4},{"key":"glu","label":"GLU","type":"TEXT","group":"Nước tiểu","displayOrder":5}]}$j$::jsonb,'PUBLISHED','Khởi tạo dữ liệu mẫu chưa có ngưỡng vận hành',CURRENT_DATE,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',NOW()),
('cf10000c-0000-0000-0000-00000000000c',NOW(),NOW(),false,'cf00000c-0000-0000-0000-00000000000c',1,$j${"fields":[{"key":"crp","label":"CRP","type":"NUMBER","unit":"mg/L","group":"Viêm","displayOrder":1,"required":true}]}$j$::jsonb,'PUBLISHED','Khởi tạo dữ liệu mẫu chưa có ngưỡng vận hành',CURRENT_DATE,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',NOW()),
('cf10000d-0000-0000-0000-00000000000d',NOW(),NOW(),false,'cf00000d-0000-0000-0000-00000000000d',1,$j${"fields":[{"key":"hbsAg","label":"HBsAg","type":"SELECT","group":"Test nhanh","displayOrder":1,"options":["NEGATIVE","POSITIVE","INDETERMINATE"]},{"key":"antiHcv","label":"Anti-HCV","type":"SELECT","group":"Test nhanh","displayOrder":2,"options":["NEGATIVE","POSITIVE","INDETERMINATE"]},{"key":"antiHiv","label":"Anti-HIV","type":"SELECT","group":"Test nhanh","displayOrder":3,"options":["NEGATIVE","POSITIVE","INDETERMINATE"]},{"key":"dengueNs1","label":"Dengue NS1","type":"SELECT","group":"Test nhanh","displayOrder":4,"options":["NEGATIVE","POSITIVE","INDETERMINATE"]},{"key":"influenzaAb","label":"Cúm A/B","type":"SELECT","group":"Test nhanh","displayOrder":5,"options":["NEGATIVE","POSITIVE","INDETERMINATE"]}]}$j$::jsonb,'PUBLISHED','Khởi tạo dữ liệu test nhanh',CURRENT_DATE,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',NOW()),
('cf10000e-0000-0000-0000-00000000000e',NOW(),NOW(),false,'cf00000e-0000-0000-0000-00000000000e',1,$j${"fields":[{"key":"lesionDescription","label":"Mô tả tổn thương","type":"TEXTAREA","group":"X-quang","displayOrder":1,"normalValue":"Không thấy tổn thương bất thường rõ trên phim."},{"key":"lesionLocation","label":"Vị trí tổn thương","type":"TEXT","group":"X-quang","displayOrder":2},{"key":"imagingConclusion","label":"Kết luận hình ảnh","type":"TEXTAREA","group":"Kết luận","displayOrder":3,"required":true,"normalValue":"Chưa ghi nhận bất thường."}]}$j$::jsonb,'PUBLISHED','Khởi tạo mẫu mô tả X-quang',CURRENT_DATE,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',NOW()),
('cf10000f-0000-0000-0000-00000000000f',NOW(),NOW(),false,'cf00000f-0000-0000-0000-00000000000f',1,$j${"fields":[{"key":"liverDescription","label":"Gan","type":"TEXTAREA","group":"Các tạng","displayOrder":1,"normalValue":"Kích thước và nhu mô chưa ghi nhận bất thường."},{"key":"gallbladderDescription","label":"Túi mật - đường mật","type":"TEXTAREA","group":"Các tạng","displayOrder":2,"normalValue":"Chưa ghi nhận bất thường."},{"key":"kidneyDescription","label":"Hai thận","type":"TEXTAREA","group":"Các tạng","displayOrder":3,"normalValue":"Chưa ghi nhận bất thường."},{"key":"abnormalFinding","label":"Phát hiện bất thường","type":"TEXTAREA","group":"Kết luận","displayOrder":4},{"key":"imagingConclusion","label":"Kết luận","type":"TEXTAREA","group":"Kết luận","displayOrder":5,"required":true,"normalValue":"Chưa ghi nhận bất thường trên siêu âm ổ bụng."}]}$j$::jsonb,'PUBLISHED','Khởi tạo mẫu siêu âm ổ bụng',CURRENT_DATE,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',NOW()),
('cf100010-0000-0000-0000-000000000010',NOW(),NOW(),false,'cf000010-0000-0000-0000-000000000010',1,$j${"fields":[{"key":"noduleLocation","label":"Vị trí nhân","type":"TEXT","group":"Nhân giáp","displayOrder":1},{"key":"noduleSizeMm","label":"Kích thước nhân","type":"NUMBER","unit":"mm","group":"Nhân giáp","displayOrder":2,"min":0},{"key":"noduleFeatures","label":"Đặc điểm nhân","type":"TEXTAREA","group":"Nhân giáp","displayOrder":3},{"key":"tirads","label":"TI-RADS","type":"SELECT","group":"Phân loại","displayOrder":4,"options":["TR1","TR2","TR3","TR4","TR5","NOT_APPLICABLE"]},{"key":"imagingConclusion","label":"Kết luận","type":"TEXTAREA","group":"Kết luận","displayOrder":5,"required":true}]}$j$::jsonb,'PUBLISHED','Khởi tạo mẫu siêu âm tuyến giáp',CURRENT_DATE,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',NOW()),
('cf100011-0000-0000-0000-000000000011',NOW(),NOW(),false,'cf000011-0000-0000-0000-000000000011',1,$j${"fields":[{"key":"crl","label":"CRL","type":"NUMBER","unit":"mm","group":"Sinh trắc thai","displayOrder":1,"min":0},{"key":"bpd","label":"BPD","type":"NUMBER","unit":"mm","group":"Sinh trắc thai","displayOrder":2,"min":0},{"key":"hc","label":"HC","type":"NUMBER","unit":"mm","group":"Sinh trắc thai","displayOrder":3,"min":0},{"key":"ac","label":"AC","type":"NUMBER","unit":"mm","group":"Sinh trắc thai","displayOrder":4,"min":0},{"key":"fl","label":"FL","type":"NUMBER","unit":"mm","group":"Sinh trắc thai","displayOrder":5,"min":0},{"key":"fhr","label":"FHR","type":"NUMBER","unit":"bpm","group":"Tim thai - nước ối","displayOrder":6,"min":0},{"key":"afi","label":"AFI","type":"NUMBER","unit":"cm","group":"Tim thai - nước ối","displayOrder":7,"min":0},{"key":"clinicalGaWeeks","label":"Tuổi thai lâm sàng","type":"NUMBER","unit":"tuần","group":"Tuổi thai","displayOrder":8,"min":0,"max":45},{"key":"gaByCrlDays","label":"GA ước tính theo CRL","type":"NUMBER","unit":"ngày","group":"Tuổi thai","displayOrder":9,"calculatorKey":"GA_CRL_ROBINSON_FLEMING_V1","precision":0},{"key":"gaByBpdFlDays","label":"GA ước tính theo BPD/FL","type":"NUMBER","unit":"ngày","group":"Tuổi thai","displayOrder":10,"calculatorKey":"GA_HADLOCK_BPD_FL_V1","precision":0},{"key":"efw","label":"EFW ước tính","type":"NUMBER","unit":"g","group":"Cân nặng thai","displayOrder":11,"calculatorKey":"EFW_HADLOCK_HC_AC_FL_V1","precision":0},{"key":"imagingConclusion","label":"Kết luận","type":"TEXTAREA","group":"Kết luận","displayOrder":12,"required":true}]}$j$::jsonb,'PUBLISHED','Khởi tạo công thức GA và EFW ước tính',CURRENT_DATE,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',NOW()),
('cf100012-0000-0000-0000-000000000012',NOW(),NOW(),false,'cf000012-0000-0000-0000-000000000012',1,$j${"fields":[{"key":"heartRate","label":"Tần số tim","type":"NUMBER","unit":"bpm","group":"Điện tim","displayOrder":1,"required":true,"min":0},{"key":"rhythmType","label":"Loại nhịp","type":"SELECT","group":"Điện tim","displayOrder":2,"options":["SINUS","ATRIAL_FIBRILLATION","OTHER"]},{"key":"ecgDescription","label":"Mô tả","type":"TEXTAREA","group":"Điện tim","displayOrder":3,"normalValue":"Nhịp xoang đều."},{"key":"ecgConclusion","label":"Kết luận","type":"TEXTAREA","group":"Kết luận","displayOrder":4,"required":true,"normalValue":"Điện tâm đồ trong giới hạn bình thường."}]}$j$::jsonb,'PUBLISHED','Khởi tạo mẫu ECG',CURRENT_DATE,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',NOW())
ON CONFLICT (template_id, version_no) DO NOTHING;

INSERT INTO clinical_form_template_version
(version_id,created_at,updated_at,deleted,template_id,version_no,schema_json,status,change_reason,effective_from,created_by,published_by,published_at)
VALUES
('cf200007-0000-0000-0000-000000000007',NOW(),NOW(),false,'cf000007-0000-0000-0000-000000000007',2,$j$
{"layout":"LAB_TABLE","systemManaged":true,"sourceName":"Public laboratory test catalogs","sourceVersion":"REFERENCE_CATALOG_V1","fields":[
{"key":"glucoseContext","code":"Loại mẫu","label":"Bối cảnh đo đường huyết","type":"SELECT","group":"Đường huyết","displayOrder":1,"requiredOnSign":true,"options":[{"value":"FASTING","label":"Lúc đói"},{"value":"RANDOM","label":"Bất kỳ"},{"value":"POSTPRANDIAL_2H","label":"Sau ăn 2 giờ"}]},
{"key":"fastingHours","code":"Giờ nhịn ăn","label":"Số giờ nhịn ăn","type":"NUMBER","unit":"giờ","group":"Đường huyết","displayOrder":2,"min":0,"max":24,"precision":1,"visibleWhen":{"field":"glucoseContext","equals":"FASTING"},"requiredWhen":{"field":"glucoseContext","equals":"FASTING"}},
{"key":"lastMealAt","code":"Bữa ăn gần nhất","label":"Thời điểm bữa ăn gần nhất","type":"TEXT","group":"Đường huyết","displayOrder":3,"visibleWhen":{"field":"glucoseContext","equals":"POSTPRANDIAL_2H"},"requiredWhen":{"field":"glucoseContext","equals":"POSTPRANDIAL_2H"}},
{"key":"glucoseQualifier","code":"Dấu","label":"Dấu định lượng","type":"SELECT","group":"Đường huyết","displayOrder":4,"requiredOnSign":true,"options":[{"value":"LESS_THAN","label":"<"},{"value":"EQUAL","label":"="},{"value":"GREATER_THAN","label":">"}]},
{"key":"glucose","code":"GLU","loincCode":"2345-7","label":"Glucose máu","type":"NUMBER","unit":"mmol/L","group":"Đường huyết","displayOrder":5,"requiredOnSign":true,"qualifierKey":"glucoseQualifier","min":0,"max":60,"precision":2,"referenceRanges":[{"sex":"ANY","minAge":0,"maxAge":200,"ageUnit":"YEARS","when":{"field":"glucoseContext","equals":"FASTING"},"low":3.9,"high":5.5},{"sex":"ANY","minAge":0,"maxAge":200,"ageUnit":"YEARS","when":{"field":"glucoseContext","equals":"RANDOM"},"high":7.8},{"sex":"ANY","minAge":0,"maxAge":200,"ageUnit":"YEARS","when":{"field":"glucoseContext","equals":"POSTPRANDIAL_2H"},"high":7.8}],"criticalRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","low":2.5,"high":25}]},
{"key":"sampleConditionNote","code":"Ghi chú","label":"Ghi chú tình trạng mẫu","type":"TEXTAREA","group":"Đường huyết","displayOrder":6}
]}$j$::jsonb,'PUBLISHED','Bổ sung bối cảnh đo, qualifier và khoảng tham chiếu',CURRENT_DATE,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',NOW()),

('cf200008-0000-0000-0000-000000000008',NOW(),NOW(),false,'cf000008-0000-0000-0000-000000000008',2,$j$
{"layout":"LAB_TABLE","systemManaged":true,"sourceName":"Public clinical chemistry laboratory catalogs","sourceVersion":"REFERENCE_CATALOG_V1","fields":[
{"key":"hba1c","code":"HbA1c","loincCode":"4548-4","label":"Hemoglobin A1c","type":"NUMBER","unit":"%","group":"Chuyển hóa đường","displayOrder":1,"requiredOnSign":true,"min":2,"max":20,"precision":1,"referenceRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","high":5.6}]},
{"key":"totalCholesterol","code":"TC","loincCode":"2093-3","label":"Cholesterol toàn phần","type":"NUMBER","unit":"mmol/L","group":"Lipid máu","displayOrder":2,"requiredOnSign":true,"min":0,"max":30,"precision":2,"referenceRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","high":5.2}]},
{"key":"triglyceride","code":"TG","loincCode":"2571-8","label":"Triglyceride","type":"NUMBER","unit":"mmol/L","group":"Lipid máu","displayOrder":3,"requiredOnSign":true,"min":0,"max":30,"precision":2,"referenceRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","high":1.7}]},
{"key":"hdlC","code":"HDL-C","loincCode":"2085-9","label":"HDL Cholesterol","type":"NUMBER","unit":"mmol/L","group":"Lipid máu","displayOrder":4,"requiredOnSign":true,"min":0,"max":10,"precision":2,"referenceRanges":[{"sex":"MALE","minAge":18,"maxAge":200,"ageUnit":"YEARS","low":1.0},{"sex":"FEMALE","minAge":18,"maxAge":200,"ageUnit":"YEARS","low":1.3}]},
{"key":"ldlC","code":"LDL-C","loincCode":"13457-7","label":"LDL Cholesterol","type":"NUMBER","unit":"mmol/L","group":"Lipid máu","displayOrder":5,"requiredOnSign":true,"min":0,"max":20,"precision":2,"referenceRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","high":3.4}]},
{"key":"nonHdlC","code":"Non-HDL-C","label":"Non-HDL Cholesterol","type":"NUMBER","unit":"mmol/L","group":"Lipid máu","displayOrder":6,"calculatorKey":"NON_HDL_C_V1","precision":2,"referenceRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","high":4.1}]},
{"key":"acidUric","code":"UA","loincCode":"3084-1","label":"Acid uric","type":"NUMBER","unit":"µmol/L","group":"Chuyển hóa khác","displayOrder":7,"requiredOnSign":true,"min":0,"max":1500,"precision":0,"referenceRanges":[{"sex":"MALE","minAge":18,"maxAge":200,"ageUnit":"YEARS","low":210,"high":420},{"sex":"FEMALE","minAge":18,"maxAge":200,"ageUnit":"YEARS","low":150,"high":350}]},
{"key":"totalProtein","code":"TP","loincCode":"2885-2","label":"Protein toàn phần","type":"NUMBER","unit":"g/L","group":"Chuyển hóa khác","displayOrder":8,"requiredOnSign":true,"min":10,"max":150,"precision":1,"referenceRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","low":64,"high":83}]}
]}$j$::jsonb,'PUBLISHED','Mở rộng sinh hóa máu cơ bản và Non-HDL-C',CURRENT_DATE,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',NOW()),

('cf200009-0000-0000-0000-000000000009',NOW(),NOW(),false,'cf000009-0000-0000-0000-000000000009',2,$j$
{"layout":"LAB_TABLE","systemManaged":true,"sourceName":"Public liver profile laboratory catalogs","sourceVersion":"REFERENCE_CATALOG_V1","fields":[
{"key":"ast","code":"AST","loincCode":"1920-8","label":"Aspartate aminotransferase","type":"NUMBER","unit":"U/L","group":"Enzyme gan mật","displayOrder":1,"requiredOnSign":true,"min":0,"max":5000,"precision":0,"referenceRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","high":40}]},
{"key":"alt","code":"ALT","loincCode":"1742-6","label":"Alanine aminotransferase","type":"NUMBER","unit":"U/L","group":"Enzyme gan mật","displayOrder":2,"requiredOnSign":true,"min":0,"max":5000,"precision":0,"referenceRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","high":41}]},
{"key":"alp","code":"ALP","loincCode":"6768-6","label":"Alkaline phosphatase","type":"NUMBER","unit":"U/L","group":"Enzyme gan mật","displayOrder":3,"requiredOnSign":true,"min":0,"max":3000,"precision":0,"referenceRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","low":40,"high":130}]},
{"key":"ggt","code":"GGT","loincCode":"2324-2","label":"Gamma-glutamyl transferase","type":"NUMBER","unit":"U/L","group":"Enzyme gan mật","displayOrder":4,"requiredOnSign":true,"min":0,"max":3000,"precision":0,"referenceRanges":[{"sex":"MALE","minAge":18,"maxAge":200,"ageUnit":"YEARS","high":60},{"sex":"FEMALE","minAge":18,"maxAge":200,"ageUnit":"YEARS","high":40}]},
{"key":"bilirubinTotal","code":"TBIL","loincCode":"1975-2","label":"Bilirubin toàn phần","type":"NUMBER","unit":"µmol/L","group":"Bilirubin","displayOrder":5,"requiredOnSign":true,"min":0,"max":1000,"precision":1,"referenceRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","high":21}]},
{"key":"bilirubinDirect","code":"DBIL","loincCode":"1968-7","label":"Bilirubin trực tiếp","type":"NUMBER","unit":"µmol/L","group":"Bilirubin","displayOrder":6,"requiredOnSign":true,"min":0,"max":1000,"precision":1,"referenceRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","high":5}]},
{"key":"bilirubinIndirect","code":"IBIL","label":"Bilirubin gián tiếp","type":"NUMBER","unit":"µmol/L","group":"Bilirubin","displayOrder":7,"calculatorKey":"INDIRECT_BILIRUBIN_V1","precision":1,"referenceRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","high":16}]},
{"key":"albumin","code":"ALB","loincCode":"1751-7","label":"Albumin","type":"NUMBER","unit":"g/L","group":"Protein","displayOrder":8,"requiredOnSign":true,"min":5,"max":80,"precision":1,"referenceRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","low":35,"high":52}]}
],"rules":[{"type":"LESS_THAN_OR_EQUAL","left":"bilirubinDirect","right":"bilirubinTotal","severity":"ERROR","message":"Bilirubin trực tiếp không được lớn hơn Bilirubin toàn phần."}]}
$j$::jsonb,'PUBLISHED','Mở rộng bộ chức năng gan và Bilirubin gián tiếp',CURRENT_DATE,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',NOW()),

('cf20000a-0000-0000-0000-00000000000a',NOW(),NOW(),false,'cf00000a-0000-0000-0000-00000000000a',2,$j$
{"layout":"LAB_TABLE","systemManaged":true,"sourceName":"NIDDK CKD-EPI 2021 and public renal profile catalogs","sourceVersion":"REFERENCE_CATALOG_V1","fields":[
{"key":"urea","code":"UREA","loincCode":"3094-0","label":"Urea","type":"NUMBER","unit":"mmol/L","group":"Chức năng lọc","displayOrder":1,"requiredOnSign":true,"min":0,"max":100,"precision":2,"referenceRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","low":2.5,"high":7.5}]},
{"key":"bun","code":"BUN","label":"Blood Urea Nitrogen (tự tính)","type":"NUMBER","unit":"mg/dL","group":"Chức năng lọc","displayOrder":2,"calculatorKey":"BUN_FROM_UREA_V1","precision":2,"referenceRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","low":7,"high":21}]},
{"key":"creatinine","code":"CREA","loincCode":"2160-0","label":"Creatinine","type":"NUMBER","unit":"µmol/L","group":"Chức năng lọc","displayOrder":3,"requiredOnSign":true,"min":1,"max":3000,"precision":0,"referenceRanges":[{"sex":"MALE","minAge":18,"maxAge":200,"ageUnit":"YEARS","low":62,"high":106},{"sex":"FEMALE","minAge":18,"maxAge":200,"ageUnit":"YEARS","low":44,"high":80}]},
{"key":"egfr","code":"eGFR","loincCode":"98979-8","label":"Mức lọc cầu thận ước tính","type":"NUMBER","unit":"mL/min/1.73m²","group":"Chức năng lọc","displayOrder":4,"calculatorKey":"EGFR_CKD_EPI_2021_V1","precision":2,"referenceRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","low":60}]},
{"key":"sodium","code":"Na+","loincCode":"2951-2","label":"Sodium","type":"NUMBER","unit":"mmol/L","group":"Điện giải","displayOrder":5,"requiredOnSign":true,"min":80,"max":200,"precision":1,"referenceRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","low":135,"high":145}],"criticalRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","low":120,"high":160}]},
{"key":"potassium","code":"K+","loincCode":"2823-3","label":"Potassium","type":"NUMBER","unit":"mmol/L","group":"Điện giải","displayOrder":6,"requiredOnSign":true,"min":1,"max":12,"precision":1,"referenceRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","low":3.5,"high":5.1}],"criticalRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","low":2.5,"high":6.5}]},
{"key":"chloride","code":"Cl-","loincCode":"2075-0","label":"Chloride","type":"NUMBER","unit":"mmol/L","group":"Điện giải","displayOrder":7,"requiredOnSign":true,"min":60,"max":160,"precision":1,"referenceRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","low":98,"high":107}]},
{"key":"bicarbonate","code":"HCO3-/TCO2","loincCode":"2028-9","label":"Bicarbonate / Total CO₂","type":"NUMBER","unit":"mmol/L","group":"Điện giải","displayOrder":8,"requiredOnSign":true,"min":5,"max":60,"precision":1,"referenceRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","low":22,"high":29}]},
{"key":"calcium","code":"Ca","loincCode":"17861-6","label":"Calcium toàn phần","type":"NUMBER","unit":"mmol/L","group":"Khoáng chất","displayOrder":9,"requiredOnSign":true,"min":0.5,"max":5,"precision":2,"referenceRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","low":2.15,"high":2.55}]},
{"key":"phosphate","code":"PO4","loincCode":"2777-1","label":"Phosphate","type":"NUMBER","unit":"mmol/L","group":"Khoáng chất","displayOrder":10,"requiredOnSign":true,"min":0.1,"max":6,"precision":2,"referenceRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","low":0.81,"high":1.45}]}
]}$j$::jsonb,'PUBLISHED','Mở rộng chức năng thận, điện giải, BUN và eGFR',CURRENT_DATE,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',NOW())
ON CONFLICT (template_id, version_no) DO NOTHING;

-- ===================================================================
-- System-managed laboratory forms. These schemas are version snapshots;
-- staff enter results manually, while units/ranges/calculations are owned
-- by the backend and cannot be edited from the Clinic Manager UI.
-- ===================================================================
INSERT INTO clinical_form_template_version
(version_id,created_at,updated_at,deleted,template_id,version_no,schema_json,status,change_reason,effective_from,created_by,published_by,published_at)
VALUES ('cf200006-0000-0000-0000-000000000006',NOW(),NOW(),false,'cf000006-0000-0000-0000-000000000006',2,$j$
{
  "layout":"LAB_TABLE","systemManaged":true,"sourceName":"LOINC CBC panel; public haematology laboratory handbooks","sourceVersion":"REFERENCE_CATALOG_V1",
  "fields":[
    {"key":"rbc","code":"RBC","loincCode":"789-8","label":"Số lượng hồng cầu","type":"NUMBER","unit":"10^12/L","group":"Hồng cầu","displayOrder":1,"requiredOnSign":true,"min":0,"max":10,"precision":2,"referenceRanges":[{"sex":"MALE","minAge":13,"maxAge":200,"ageUnit":"YEARS","low":4.5,"high":5.5},{"sex":"FEMALE","minAge":13,"maxAge":200,"ageUnit":"YEARS","low":3.8,"high":4.8},{"sex":"ANY","minAge":2,"maxAge":12,"ageUnit":"YEARS","low":4.0,"high":5.2}]},
    {"key":"hgb","code":"HGB","loincCode":"718-7","label":"Huyết sắc tố","type":"NUMBER","unit":"g/L","group":"Hồng cầu","displayOrder":2,"requiredOnSign":true,"min":20,"max":250,"precision":0,"referenceRanges":[{"sex":"MALE","minAge":13,"maxAge":200,"ageUnit":"YEARS","low":130,"high":170},{"sex":"FEMALE","minAge":13,"maxAge":200,"ageUnit":"YEARS","low":120,"high":150},{"sex":"ANY","minAge":2,"maxAge":6,"ageUnit":"YEARS","low":110,"high":140},{"sex":"ANY","minAge":7,"maxAge":12,"ageUnit":"YEARS","low":115,"high":155}],"criticalRanges":[{"sex":"ANY","minAge":13,"maxAge":200,"ageUnit":"YEARS","low":70,"high":200}]},
    {"key":"hct","code":"HCT","loincCode":"4544-3","label":"Hematocrit","type":"NUMBER","unit":"%","group":"Hồng cầu","displayOrder":3,"requiredOnSign":true,"min":5,"max":80,"precision":1,"referenceRanges":[{"sex":"MALE","minAge":13,"maxAge":200,"ageUnit":"YEARS","low":40,"high":50},{"sex":"FEMALE","minAge":13,"maxAge":200,"ageUnit":"YEARS","low":36,"high":46},{"sex":"ANY","minAge":2,"maxAge":6,"ageUnit":"YEARS","low":34,"high":40},{"sex":"ANY","minAge":7,"maxAge":12,"ageUnit":"YEARS","low":35,"high":45}]},
    {"key":"mcv","code":"MCV","loincCode":"787-2","label":"Thể tích trung bình hồng cầu","type":"NUMBER","unit":"fL","group":"Hồng cầu","displayOrder":4,"requiredOnSign":true,"min":30,"max":150,"precision":1,"referenceRanges":[{"sex":"ANY","minAge":13,"maxAge":200,"ageUnit":"YEARS","low":83,"high":101},{"sex":"ANY","minAge":2,"maxAge":6,"ageUnit":"YEARS","low":75,"high":87},{"sex":"ANY","minAge":7,"maxAge":12,"ageUnit":"YEARS","low":77,"high":95}]},
    {"key":"mch","code":"MCH","loincCode":"785-6","label":"Lượng HGB trung bình hồng cầu","type":"NUMBER","unit":"pg","group":"Hồng cầu","displayOrder":5,"requiredOnSign":true,"min":10,"max":60,"precision":1,"referenceRanges":[{"sex":"ANY","minAge":13,"maxAge":200,"ageUnit":"YEARS","low":27,"high":32},{"sex":"ANY","minAge":2,"maxAge":6,"ageUnit":"YEARS","low":24,"high":30},{"sex":"ANY","minAge":7,"maxAge":12,"ageUnit":"YEARS","low":25,"high":33}]},
    {"key":"mchc","code":"MCHC","loincCode":"786-4","label":"Nồng độ HGB trung bình hồng cầu","type":"NUMBER","unit":"g/L","group":"Hồng cầu","displayOrder":6,"requiredOnSign":true,"min":200,"max":450,"precision":0,"referenceRanges":[{"sex":"ANY","minAge":13,"maxAge":200,"ageUnit":"YEARS","low":315,"high":345},{"sex":"ANY","minAge":2,"maxAge":12,"ageUnit":"YEARS","low":310,"high":370}]},
    {"key":"rdwCv","code":"RDW-CV","loincCode":"788-0","label":"Độ phân bố hồng cầu CV","type":"NUMBER","unit":"%","group":"Hồng cầu","displayOrder":7,"requiredOnSign":true,"min":5,"max":40,"precision":1,"referenceRanges":[{"sex":"ANY","minAge":13,"maxAge":200,"ageUnit":"YEARS","low":11.5,"high":14.5}]},
    {"key":"rdwSd","code":"RDW-SD","label":"Độ phân bố hồng cầu SD","type":"NUMBER","unit":"fL","group":"Hồng cầu","displayOrder":8,"requiredOnSign":true,"min":20,"max":100,"precision":1,"referenceRanges":[{"sex":"ANY","minAge":13,"maxAge":200,"ageUnit":"YEARS","low":37,"high":54}]},
    {"key":"wbc","code":"WBC","loincCode":"6690-2","label":"Số lượng bạch cầu","type":"NUMBER","unit":"10^9/L","group":"Bạch cầu","displayOrder":9,"requiredOnSign":true,"min":0,"max":200,"precision":2,"referenceRanges":[{"sex":"ANY","minAge":13,"maxAge":200,"ageUnit":"YEARS","low":4,"high":10},{"sex":"ANY","minAge":2,"maxAge":6,"ageUnit":"YEARS","low":5,"high":15},{"sex":"ANY","minAge":7,"maxAge":12,"ageUnit":"YEARS","low":5,"high":13}],"criticalRanges":[{"sex":"ANY","minAge":13,"maxAge":200,"ageUnit":"YEARS","low":2,"high":30}]},
    {"key":"neutPercent","code":"NEUT%","label":"Bạch cầu trung tính","type":"NUMBER","unit":"%","group":"Bạch cầu","displayOrder":10,"requiredOnSign":true,"min":0,"max":100,"precision":1,"referenceRanges":[{"sex":"ANY","minAge":13,"maxAge":200,"ageUnit":"YEARS","low":40,"high":80}]},
    {"key":"neutAbsolute","code":"NEUT#","label":"Bạch cầu trung tính tuyệt đối","type":"NUMBER","unit":"10^9/L","group":"Bạch cầu","displayOrder":11,"requiredOnSign":true,"min":0,"max":100,"precision":2,"referenceRanges":[{"sex":"ANY","minAge":13,"maxAge":200,"ageUnit":"YEARS","low":2,"high":7}]},
    {"key":"lymphPercent","code":"LYMPH%","label":"Bạch cầu lympho","type":"NUMBER","unit":"%","group":"Bạch cầu","displayOrder":12,"requiredOnSign":true,"min":0,"max":100,"precision":1,"referenceRanges":[{"sex":"ANY","minAge":13,"maxAge":200,"ageUnit":"YEARS","low":20,"high":40}]},
    {"key":"lymphAbsolute","code":"LYMPH#","label":"Bạch cầu lympho tuyệt đối","type":"NUMBER","unit":"10^9/L","group":"Bạch cầu","displayOrder":13,"requiredOnSign":true,"min":0,"max":100,"precision":2,"referenceRanges":[{"sex":"ANY","minAge":13,"maxAge":200,"ageUnit":"YEARS","low":1,"high":3}]},
    {"key":"monoPercent","code":"MONO%","label":"Bạch cầu mono","type":"NUMBER","unit":"%","group":"Bạch cầu","displayOrder":14,"requiredOnSign":true,"min":0,"max":100,"precision":1,"referenceRanges":[{"sex":"ANY","minAge":13,"maxAge":200,"ageUnit":"YEARS","low":2,"high":10}]},
    {"key":"monoAbsolute","code":"MONO#","label":"Bạch cầu mono tuyệt đối","type":"NUMBER","unit":"10^9/L","group":"Bạch cầu","displayOrder":15,"requiredOnSign":true,"min":0,"max":100,"precision":2,"referenceRanges":[{"sex":"ANY","minAge":13,"maxAge":200,"ageUnit":"YEARS","low":0.2,"high":1}]},
    {"key":"eosPercent","code":"EOS%","label":"Bạch cầu ái toan","type":"NUMBER","unit":"%","group":"Bạch cầu","displayOrder":16,"requiredOnSign":true,"min":0,"max":100,"precision":1,"referenceRanges":[{"sex":"ANY","minAge":13,"maxAge":200,"ageUnit":"YEARS","low":1,"high":6}]},
    {"key":"eosAbsolute","code":"EOS#","label":"Bạch cầu ái toan tuyệt đối","type":"NUMBER","unit":"10^9/L","group":"Bạch cầu","displayOrder":17,"requiredOnSign":true,"min":0,"max":100,"precision":2,"referenceRanges":[{"sex":"ANY","minAge":13,"maxAge":200,"ageUnit":"YEARS","low":0.02,"high":0.5}]},
    {"key":"basoPercent","code":"BASO%","label":"Bạch cầu ái kiềm","type":"NUMBER","unit":"%","group":"Bạch cầu","displayOrder":18,"requiredOnSign":true,"min":0,"max":100,"precision":1,"referenceRanges":[{"sex":"ANY","minAge":13,"maxAge":200,"ageUnit":"YEARS","low":0,"high":2}]},
    {"key":"basoAbsolute","code":"BASO#","label":"Bạch cầu ái kiềm tuyệt đối","type":"NUMBER","unit":"10^9/L","group":"Bạch cầu","displayOrder":19,"requiredOnSign":true,"min":0,"max":100,"precision":2,"referenceRanges":[{"sex":"ANY","minAge":13,"maxAge":200,"ageUnit":"YEARS","low":0.02,"high":0.1}]},
    {"key":"plt","code":"PLT","loincCode":"777-3","label":"Số lượng tiểu cầu","type":"NUMBER","unit":"10^9/L","group":"Tiểu cầu","displayOrder":20,"requiredOnSign":true,"min":0,"max":2000,"precision":0,"referenceRanges":[{"sex":"ANY","minAge":13,"maxAge":200,"ageUnit":"YEARS","low":150,"high":410},{"sex":"ANY","minAge":2,"maxAge":6,"ageUnit":"YEARS","low":200,"high":490},{"sex":"ANY","minAge":7,"maxAge":12,"ageUnit":"YEARS","low":170,"high":450}],"criticalRanges":[{"sex":"ANY","minAge":13,"maxAge":200,"ageUnit":"YEARS","low":50,"high":1000}]},
    {"key":"mpv","code":"MPV","loincCode":"32623-1","label":"Thể tích trung bình tiểu cầu","type":"NUMBER","unit":"fL","group":"Tiểu cầu","displayOrder":21,"requiredOnSign":true,"min":2,"max":30,"precision":1,"referenceRanges":[{"sex":"ANY","minAge":13,"maxAge":200,"ageUnit":"YEARS","low":9,"high":13}]},
    {"key":"pdw","code":"PDW","label":"Độ phân bố tiểu cầu","type":"NUMBER","unit":"fL","group":"Tiểu cầu","displayOrder":22,"requiredOnSign":true,"min":1,"max":40,"precision":1},
    {"key":"pct","code":"PCT","label":"Plateletcrit","type":"NUMBER","unit":"%","group":"Tiểu cầu","displayOrder":23,"requiredOnSign":true,"min":0,"max":2,"precision":3},
    {"key":"pLcr","code":"P-LCR","label":"Tỷ lệ tiểu cầu kích thước lớn","type":"NUMBER","unit":"%","group":"Tiểu cầu","displayOrder":24,"requiredOnSign":true,"min":0,"max":100,"precision":1},
    {"key":"igPercent","code":"IG%","label":"Bạch cầu hạt non","type":"NUMBER","unit":"%","group":"Mở rộng / hình thái","displayOrder":25,"min":0,"max":100,"precision":1},
    {"key":"igAbsolute","code":"IG#","label":"Bạch cầu hạt non tuyệt đối","type":"NUMBER","unit":"10^9/L","group":"Mở rộng / hình thái","displayOrder":26,"min":0,"max":100,"precision":2},
    {"key":"nrbcPercent","code":"NRBC%","label":"Hồng cầu có nhân","type":"NUMBER","unit":"%","group":"Mở rộng / hình thái","displayOrder":27,"min":0,"max":100,"precision":1},
    {"key":"nrbcAbsolute","code":"NRBC#","label":"Hồng cầu có nhân tuyệt đối","type":"NUMBER","unit":"10^9/L","group":"Mở rộng / hình thái","displayOrder":28,"min":0,"max":100,"precision":2},
    {"key":"reticPercent","code":"RET%","label":"Hồng cầu lưới","type":"NUMBER","unit":"%","group":"Mở rộng / hình thái","displayOrder":29,"min":0,"max":30,"precision":1},
    {"key":"reticAbsolute","code":"RET#","label":"Hồng cầu lưới tuyệt đối","type":"NUMBER","unit":"10^9/L","group":"Mở rộng / hình thái","displayOrder":30,"min":0,"max":1000,"precision":2},
    {"key":"cellMorphology","code":"MORPH","label":"Nhận xét hình thái tế bào","type":"TEXTAREA","group":"Mở rộng / hình thái","displayOrder":31}
  ],
  "rules":[
    {"type":"SUM_BETWEEN","keys":["neutPercent","lymphPercent","monoPercent","eosPercent","basoPercent"],"min":98,"max":102,"severity":"WARNING","message":"Tổng tỷ lệ năm loại bạch cầu phải xấp xỉ 100%."},
    {"type":"ABSOLUTE_FROM_PERCENT","total":"wbc","pairs":[{"percent":"neutPercent","absolute":"neutAbsolute"},{"percent":"lymphPercent","absolute":"lymphAbsolute"},{"percent":"monoPercent","absolute":"monoAbsolute"},{"percent":"eosPercent","absolute":"eosAbsolute"},{"percent":"basoPercent","absolute":"basoAbsolute"}],"tolerancePercent":15,"severity":"WARNING","message":"Số lượng bạch cầu tuyệt đối chưa phù hợp với WBC và tỷ lệ phần trăm."}
  ]
}
$j$::jsonb,'PUBLISHED','Mở rộng CBC và khoảng tham chiếu có kiểm soát',CURRENT_DATE,
'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',NOW())
ON CONFLICT (template_id, version_no) DO NOTHING;

INSERT INTO clinical_form_template_version
(version_id,created_at,updated_at,deleted,template_id,version_no,schema_json,status,change_reason,effective_from,created_by,published_by,published_at)
VALUES
('cf20000b-0000-0000-0000-00000000000b',NOW(),NOW(),false,'cf00000b-0000-0000-0000-00000000000b',2,$j$
{"layout":"LAB_TABLE","systemManaged":true,"sourceName":"Siemens Multistix 10 SG IFU and public urinalysis catalogs","sourceVersion":"REFERENCE_CATALOG_V1","fields":[
{"key":"color","code":"COLOR","label":"Màu sắc","type":"SELECT","group":"Vật lý","displayOrder":1,"requiredOnSign":true,"options":["STRAW","YELLOW","AMBER","RED","BROWN","OTHER"]},
{"key":"clarity","code":"CLARITY","label":"Độ trong","type":"SELECT","group":"Vật lý","displayOrder":2,"requiredOnSign":true,"options":["CLEAR","SLIGHTLY_CLOUDY","CLOUDY","TURBID"]},
{"key":"specificGravity","code":"SG","loincCode":"2965-2","label":"Tỷ trọng","type":"NUMBER","group":"Vật lý","displayOrder":3,"requiredOnSign":true,"min":1,"max":1.06,"precision":3,"referenceRanges":[{"sex":"ANY","minAge":0,"maxAge":200,"ageUnit":"YEARS","low":1.005,"high":1.03}]},
{"key":"ph","code":"pH","loincCode":"2756-5","label":"pH nước tiểu","type":"NUMBER","group":"Hóa học","displayOrder":4,"requiredOnSign":true,"min":4,"max":9,"precision":1,"referenceRanges":[{"sex":"ANY","minAge":0,"maxAge":200,"ageUnit":"YEARS","low":5,"high":8}]},
{"key":"leukocyteEsterase","code":"LEU","loincCode":"5799-2","label":"Leukocyte Esterase","type":"SELECT","group":"Hóa học","displayOrder":5,"requiredOnSign":true,"options":["NEGATIVE","TRACE","1+","2+","3+","4+"],"referenceRanges":[{"sex":"ANY","minAge":0,"maxAge":200,"ageUnit":"YEARS","normalValues":["NEGATIVE"]}]},
{"key":"nitrite","code":"NIT","loincCode":"5802-4","label":"Nitrite","type":"SELECT","group":"Hóa học","displayOrder":6,"requiredOnSign":true,"options":["NEGATIVE","POSITIVE"],"referenceRanges":[{"sex":"ANY","minAge":0,"maxAge":200,"ageUnit":"YEARS","normalValues":["NEGATIVE"]}]},
{"key":"protein","code":"PRO","loincCode":"5804-0","label":"Protein","type":"SELECT","group":"Hóa học","displayOrder":7,"requiredOnSign":true,"options":["NEGATIVE","TRACE","1+","2+","3+","4+"],"referenceRanges":[{"sex":"ANY","minAge":0,"maxAge":200,"ageUnit":"YEARS","normalValues":["NEGATIVE","TRACE"]}]},
{"key":"urineGlucose","code":"GLU","loincCode":"5792-7","label":"Glucose","type":"SELECT","group":"Hóa học","displayOrder":8,"requiredOnSign":true,"options":["NEGATIVE","TRACE","1+","2+","3+","4+"],"referenceRanges":[{"sex":"ANY","minAge":0,"maxAge":200,"ageUnit":"YEARS","normalValues":["NEGATIVE"]}]},
{"key":"ketone","code":"KET","loincCode":"5797-6","label":"Ketone","type":"SELECT","group":"Hóa học","displayOrder":9,"requiredOnSign":true,"options":["NEGATIVE","TRACE","1+","2+","3+","4+"],"referenceRanges":[{"sex":"ANY","minAge":0,"maxAge":200,"ageUnit":"YEARS","normalValues":["NEGATIVE"]}]},
{"key":"urobilinogen","code":"URO","loincCode":"5818-0","label":"Urobilinogen","type":"SELECT","group":"Hóa học","displayOrder":10,"requiredOnSign":true,"options":["NEGATIVE","TRACE","1+","2+","3+","4+"],"referenceRanges":[{"sex":"ANY","minAge":0,"maxAge":200,"ageUnit":"YEARS","normalValues":["NEGATIVE","TRACE"]}]},
{"key":"urineBilirubin","code":"BIL","loincCode":"5770-3","label":"Bilirubin","type":"SELECT","group":"Hóa học","displayOrder":11,"requiredOnSign":true,"options":["NEGATIVE","TRACE","1+","2+","3+","4+"],"referenceRanges":[{"sex":"ANY","minAge":0,"maxAge":200,"ageUnit":"YEARS","normalValues":["NEGATIVE"]}]},
{"key":"blood","code":"BLD","loincCode":"5794-3","label":"Blood / Hemoglobin","type":"SELECT","group":"Hóa học","displayOrder":12,"requiredOnSign":true,"options":["NEGATIVE","TRACE","1+","2+","3+","4+"],"referenceRanges":[{"sex":"ANY","minAge":0,"maxAge":200,"ageUnit":"YEARS","normalValues":["NEGATIVE"]}]},
{"key":"microscopyPerformed","code":"Soi cặn","label":"Có thực hiện soi vi thể","type":"BOOLEAN","group":"Vi thể","displayOrder":13},
{"key":"urineRbc","code":"RBC/HPF","loincCode":"13945-1","label":"Hồng cầu vi thể","type":"NUMBER","unit":"/HPF","group":"Vi thể","displayOrder":14,"min":0,"max":1000,"precision":0,"visibleWhen":{"field":"microscopyPerformed","equals":true},"requiredWhen":{"field":"microscopyPerformed","equals":true},"referenceRanges":[{"sex":"ANY","minAge":0,"maxAge":200,"ageUnit":"YEARS","low":0,"high":2}]},
{"key":"urineWbc","code":"WBC/HPF","loincCode":"5821-4","label":"Bạch cầu vi thể","type":"NUMBER","unit":"/HPF","group":"Vi thể","displayOrder":15,"min":0,"max":1000,"precision":0,"visibleWhen":{"field":"microscopyPerformed","equals":true},"requiredWhen":{"field":"microscopyPerformed","equals":true},"referenceRanges":[{"sex":"ANY","minAge":0,"maxAge":200,"ageUnit":"YEARS","low":0,"high":5}]},
{"key":"epithelialCells","code":"EPI","label":"Tế bào biểu mô","type":"SELECT","group":"Vi thể","displayOrder":16,"options":["NONE","FEW","MODERATE","MANY"],"visibleWhen":{"field":"microscopyPerformed","equals":true},"requiredWhen":{"field":"microscopyPerformed","equals":true}},
{"key":"casts","code":"CAST","label":"Trụ niệu","type":"SELECT","group":"Vi thể","displayOrder":17,"options":["NONE","FEW","MODERATE","MANY"],"visibleWhen":{"field":"microscopyPerformed","equals":true},"requiredWhen":{"field":"microscopyPerformed","equals":true}},
{"key":"crystals","code":"CRYSTAL","label":"Tinh thể","type":"SELECT","group":"Vi thể","displayOrder":18,"options":["NONE","FEW","MODERATE","MANY"],"visibleWhen":{"field":"microscopyPerformed","equals":true},"requiredWhen":{"field":"microscopyPerformed","equals":true}},
{"key":"bacteria","code":"BACT","label":"Vi khuẩn","type":"SELECT","group":"Vi thể","displayOrder":19,"options":["NONE","FEW","MODERATE","MANY"],"visibleWhen":{"field":"microscopyPerformed","equals":true},"requiredWhen":{"field":"microscopyPerformed","equals":true}},
{"key":"yeast","code":"YEAST","label":"Nấm men","type":"SELECT","group":"Vi thể","displayOrder":20,"options":["NONE","FEW","MODERATE","MANY"],"visibleWhen":{"field":"microscopyPerformed","equals":true},"requiredWhen":{"field":"microscopyPerformed","equals":true}},
{"key":"mucus","code":"MUCUS","label":"Chất nhầy","type":"SELECT","group":"Vi thể","displayOrder":21,"options":["NONE","FEW","MODERATE","MANY"],"visibleWhen":{"field":"microscopyPerformed","equals":true}},
{"key":"parasites","code":"PARASITE","label":"Ký sinh trùng","type":"SELECT","group":"Vi thể","displayOrder":22,"options":["NONE","PRESENT"],"visibleWhen":{"field":"microscopyPerformed","equals":true},"requiredWhen":{"field":"microscopyPerformed","equals":true}},
{"key":"microscopyComment","code":"Nhận xét","label":"Nhận xét soi vi thể","type":"TEXTAREA","group":"Vi thể","displayOrder":23,"visibleWhen":{"field":"microscopyPerformed","equals":true}}
]}$j$::jsonb,'PUBLISHED','Mở rộng vật lý, hóa học và soi vi thể nước tiểu',CURRENT_DATE,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',NOW()),

('cf20000c-0000-0000-0000-00000000000c',NOW(),NOW(),false,'cf00000c-0000-0000-0000-00000000000c',2,$j$
{"layout":"LAB_TABLE","systemManaged":true,"sourceName":"Public quantitative CRP laboratory catalogs","sourceVersion":"REFERENCE_CATALOG_V1","fields":[
{"key":"crpAssayType","code":"Loại CRP","label":"Loại xét nghiệm","type":"SELECT","group":"CRP định lượng","displayOrder":1,"requiredOnSign":true,"options":[{"value":"STANDARD_CRP","label":"CRP thường (định lượng)"}]},
{"key":"crpQualifier","code":"Dấu","label":"Dấu định lượng","type":"SELECT","group":"CRP định lượng","displayOrder":2,"requiredOnSign":true,"options":[{"value":"LESS_THAN","label":"<"},{"value":"EQUAL","label":"="},{"value":"GREATER_THAN","label":">"}]},
{"key":"crp","code":"CRP","loincCode":"1988-5","label":"C-Reactive Protein","type":"NUMBER","unit":"mg/L","group":"CRP định lượng","displayOrder":3,"requiredOnSign":true,"qualifierKey":"crpQualifier","min":0,"max":1000,"precision":1,"referenceRanges":[{"sex":"ANY","minAge":0,"maxAge":200,"ageUnit":"YEARS","high":5}],"criticalRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","high":100}]},
{"key":"detectionLimit","code":"LoD","label":"Giới hạn phát hiện của phương pháp","type":"NUMBER","unit":"mg/L","group":"Thông tin phương pháp","displayOrder":4,"min":0,"max":100,"precision":2},
{"key":"methodNote","code":"Phương pháp","label":"Ghi chú phương pháp","type":"TEXTAREA","group":"Thông tin phương pháp","displayOrder":5}
]}$j$::jsonb,'PUBLISHED','Bổ sung CRP định lượng, qualifier và giới hạn phát hiện',CURRENT_DATE,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',NOW())
ON CONFLICT (template_id, version_no) DO NOTHING;

INSERT INTO clinical_form_template_version
(version_id,created_at,updated_at,deleted,template_id,version_no,schema_json,status,change_reason,effective_from,created_by,published_by,published_at)
VALUES ('cf20000d-0000-0000-0000-00000000000d',NOW(),NOW(),false,'cf00000d-0000-0000-0000-00000000000d',2,$j$
{"layout":"LAB_TABLE","systemManaged":true,"sourceName":"Manufacturer rapid-test instructions for use; result interpretation is kit-specific","sourceVersion":"REFERENCE_CATALOG_V1","fields":[{"key":"hbsAgPerformed","code":"HBsAg - thực hiện","label":"Thực hiện HBsAg","type":"BOOLEAN","group":"HBsAg","displayOrder":1},{"key":"hbsAgResult","code":"HBsAg","label":"Kết quả HBsAg","type":"SELECT","group":"HBsAg","displayOrder":2,"visibleWhen":{"field":"hbsAgPerformed","equals":true},"requiredWhen":{"field":"hbsAgPerformed","equals":true},"options":["NEGATIVE","POSITIVE","INDETERMINATE","INVALID"],"referenceRanges":[{"sex":"ANY","minAge":0,"maxAge":200,"ageUnit":"YEARS","normalValues":["NEGATIVE"]}]},{"key":"hbsAgSampleCode","code":"Mã mẫu","label":"Mã mẫu HBsAg","type":"TEXT","group":"HBsAg","displayOrder":3,"visibleWhen":{"field":"hbsAgPerformed","equals":true},"requiredWhen":{"field":"hbsAgPerformed","equals":true}},{"key":"hbsAgSampleType","code":"Loại mẫu","label":"Loại mẫu HBsAg","type":"SELECT","group":"HBsAg","displayOrder":4,"visibleWhen":{"field":"hbsAgPerformed","equals":true},"requiredWhen":{"field":"hbsAgPerformed","equals":true},"options":["WHOLE_BLOOD","SERUM","PLASMA","NASOPHARYNGEAL_SWAB","OTHER"]},{"key":"hbsAgKitName","code":"Kit","label":"Tên kit HBsAg","type":"TEXT","group":"HBsAg","displayOrder":5,"visibleWhen":{"field":"hbsAgPerformed","equals":true},"requiredWhen":{"field":"hbsAgPerformed","equals":true}},{"key":"hbsAgLotNumber","code":"Số lô","label":"Số lô kit HBsAg","type":"TEXT","group":"HBsAg","displayOrder":6,"visibleWhen":{"field":"hbsAgPerformed","equals":true},"requiredWhen":{"field":"hbsAgPerformed","equals":true}},{"key":"hbsAgKitExpiry","code":"Hạn dùng","label":"Hạn sử dụng kit HBsAg","type":"DATE","group":"HBsAg","displayOrder":7,"visibleWhen":{"field":"hbsAgPerformed","equals":true},"requiredWhen":{"field":"hbsAgPerformed","equals":true}},{"key":"hbsAgControlValid","code":"Control","label":"Control hợp lệ HBsAg","type":"BOOLEAN","group":"HBsAg","displayOrder":8,"visibleWhen":{"field":"hbsAgPerformed","equals":true},"requiredWhen":{"field":"hbsAgPerformed","equals":true}},{"key":"hbsAgReadAt","code":"Thời gian đọc","label":"Thời gian đọc HBsAg","type":"TEXT","group":"HBsAg","displayOrder":9,"visibleWhen":{"field":"hbsAgPerformed","equals":true},"requiredWhen":{"field":"hbsAgPerformed","equals":true},"pattern":"^\\\\d{4}-\\\\d{2}-\\\\d{2}[ T]\\\\d{2}:\\\\d{2}(:\\\\d{2})?$","patternMessage":"Thời gian đọc phải theo định dạng YYYY-MM-DD HH:mm"},{"key":"antiHcvPerformed","code":"Anti-HCV - thực hiện","label":"Thực hiện Anti-HCV","type":"BOOLEAN","group":"Anti-HCV","displayOrder":10},{"key":"antiHcvResult","code":"Anti-HCV","label":"Kết quả Anti-HCV","type":"SELECT","group":"Anti-HCV","displayOrder":11,"visibleWhen":{"field":"antiHcvPerformed","equals":true},"requiredWhen":{"field":"antiHcvPerformed","equals":true},"options":["NEGATIVE","POSITIVE","INDETERMINATE","INVALID"],"referenceRanges":[{"sex":"ANY","minAge":0,"maxAge":200,"ageUnit":"YEARS","normalValues":["NEGATIVE"]}]},{"key":"antiHcvSampleCode","code":"Mã mẫu","label":"Mã mẫu Anti-HCV","type":"TEXT","group":"Anti-HCV","displayOrder":12,"visibleWhen":{"field":"antiHcvPerformed","equals":true},"requiredWhen":{"field":"antiHcvPerformed","equals":true}},{"key":"antiHcvSampleType","code":"Loại mẫu","label":"Loại mẫu Anti-HCV","type":"SELECT","group":"Anti-HCV","displayOrder":13,"visibleWhen":{"field":"antiHcvPerformed","equals":true},"requiredWhen":{"field":"antiHcvPerformed","equals":true},"options":["WHOLE_BLOOD","SERUM","PLASMA","NASOPHARYNGEAL_SWAB","OTHER"]},{"key":"antiHcvKitName","code":"Kit","label":"Tên kit Anti-HCV","type":"TEXT","group":"Anti-HCV","displayOrder":14,"visibleWhen":{"field":"antiHcvPerformed","equals":true},"requiredWhen":{"field":"antiHcvPerformed","equals":true}},{"key":"antiHcvLotNumber","code":"Số lô","label":"Số lô kit Anti-HCV","type":"TEXT","group":"Anti-HCV","displayOrder":15,"visibleWhen":{"field":"antiHcvPerformed","equals":true},"requiredWhen":{"field":"antiHcvPerformed","equals":true}},{"key":"antiHcvKitExpiry","code":"Hạn dùng","label":"Hạn sử dụng kit Anti-HCV","type":"DATE","group":"Anti-HCV","displayOrder":16,"visibleWhen":{"field":"antiHcvPerformed","equals":true},"requiredWhen":{"field":"antiHcvPerformed","equals":true}},{"key":"antiHcvControlValid","code":"Control","label":"Control hợp lệ Anti-HCV","type":"BOOLEAN","group":"Anti-HCV","displayOrder":17,"visibleWhen":{"field":"antiHcvPerformed","equals":true},"requiredWhen":{"field":"antiHcvPerformed","equals":true}},{"key":"antiHcvReadAt","code":"Thời gian đọc","label":"Thời gian đọc Anti-HCV","type":"TEXT","group":"Anti-HCV","displayOrder":18,"visibleWhen":{"field":"antiHcvPerformed","equals":true},"requiredWhen":{"field":"antiHcvPerformed","equals":true},"pattern":"^\\\\d{4}-\\\\d{2}-\\\\d{2}[ T]\\\\d{2}:\\\\d{2}(:\\\\d{2})?$","patternMessage":"Thời gian đọc phải theo định dạng YYYY-MM-DD HH:mm"},{"key":"hivPerformed","code":"HIV - thực hiện","label":"Thực hiện HIV Ag/Ab","type":"BOOLEAN","group":"HIV Ag/Ab","displayOrder":19},{"key":"hivResult","code":"HIV","label":"Kết quả HIV Ag/Ab","type":"SELECT","group":"HIV Ag/Ab","displayOrder":20,"visibleWhen":{"field":"hivPerformed","equals":true},"requiredWhen":{"field":"hivPerformed","equals":true},"options":["NEGATIVE","POSITIVE","INDETERMINATE","INVALID"],"referenceRanges":[{"sex":"ANY","minAge":0,"maxAge":200,"ageUnit":"YEARS","normalValues":["NEGATIVE"]}]},{"key":"hivSampleCode","code":"Mã mẫu","label":"Mã mẫu HIV Ag/Ab","type":"TEXT","group":"HIV Ag/Ab","displayOrder":21,"visibleWhen":{"field":"hivPerformed","equals":true},"requiredWhen":{"field":"hivPerformed","equals":true}},{"key":"hivSampleType","code":"Loại mẫu","label":"Loại mẫu HIV Ag/Ab","type":"SELECT","group":"HIV Ag/Ab","displayOrder":22,"visibleWhen":{"field":"hivPerformed","equals":true},"requiredWhen":{"field":"hivPerformed","equals":true},"options":["WHOLE_BLOOD","SERUM","PLASMA","NASOPHARYNGEAL_SWAB","OTHER"]},{"key":"hivKitName","code":"Kit","label":"Tên kit HIV Ag/Ab","type":"TEXT","group":"HIV Ag/Ab","displayOrder":23,"visibleWhen":{"field":"hivPerformed","equals":true},"requiredWhen":{"field":"hivPerformed","equals":true}},{"key":"hivLotNumber","code":"Số lô","label":"Số lô kit HIV Ag/Ab","type":"TEXT","group":"HIV Ag/Ab","displayOrder":24,"visibleWhen":{"field":"hivPerformed","equals":true},"requiredWhen":{"field":"hivPerformed","equals":true}},{"key":"hivKitExpiry","code":"Hạn dùng","label":"Hạn sử dụng kit HIV Ag/Ab","type":"DATE","group":"HIV Ag/Ab","displayOrder":25,"visibleWhen":{"field":"hivPerformed","equals":true},"requiredWhen":{"field":"hivPerformed","equals":true}},{"key":"hivControlValid","code":"Control","label":"Control hợp lệ HIV Ag/Ab","type":"BOOLEAN","group":"HIV Ag/Ab","displayOrder":26,"visibleWhen":{"field":"hivPerformed","equals":true},"requiredWhen":{"field":"hivPerformed","equals":true}},{"key":"hivReadAt","code":"Thời gian đọc","label":"Thời gian đọc HIV Ag/Ab","type":"TEXT","group":"HIV Ag/Ab","displayOrder":27,"visibleWhen":{"field":"hivPerformed","equals":true},"requiredWhen":{"field":"hivPerformed","equals":true},"pattern":"^\\\\d{4}-\\\\d{2}-\\\\d{2}[ T]\\\\d{2}:\\\\d{2}(:\\\\d{2})?$","patternMessage":"Thời gian đọc phải theo định dạng YYYY-MM-DD HH:mm"},{"key":"dengueNs1Performed","code":"Dengue NS1 - thực hiện","label":"Thực hiện Dengue NS1","type":"BOOLEAN","group":"Dengue NS1","displayOrder":28},{"key":"dengueNs1Result","code":"Dengue NS1","label":"Kết quả Dengue NS1","type":"SELECT","group":"Dengue NS1","displayOrder":29,"visibleWhen":{"field":"dengueNs1Performed","equals":true},"requiredWhen":{"field":"dengueNs1Performed","equals":true},"options":["NEGATIVE","POSITIVE","INDETERMINATE","INVALID"],"referenceRanges":[{"sex":"ANY","minAge":0,"maxAge":200,"ageUnit":"YEARS","normalValues":["NEGATIVE"]}]},{"key":"dengueNs1SampleCode","code":"Mã mẫu","label":"Mã mẫu Dengue NS1","type":"TEXT","group":"Dengue NS1","displayOrder":30,"visibleWhen":{"field":"dengueNs1Performed","equals":true},"requiredWhen":{"field":"dengueNs1Performed","equals":true}},{"key":"dengueNs1SampleType","code":"Loại mẫu","label":"Loại mẫu Dengue NS1","type":"SELECT","group":"Dengue NS1","displayOrder":31,"visibleWhen":{"field":"dengueNs1Performed","equals":true},"requiredWhen":{"field":"dengueNs1Performed","equals":true},"options":["WHOLE_BLOOD","SERUM","PLASMA","NASOPHARYNGEAL_SWAB","OTHER"]},{"key":"dengueNs1KitName","code":"Kit","label":"Tên kit Dengue NS1","type":"TEXT","group":"Dengue NS1","displayOrder":32,"visibleWhen":{"field":"dengueNs1Performed","equals":true},"requiredWhen":{"field":"dengueNs1Performed","equals":true}},{"key":"dengueNs1LotNumber","code":"Số lô","label":"Số lô kit Dengue NS1","type":"TEXT","group":"Dengue NS1","displayOrder":33,"visibleWhen":{"field":"dengueNs1Performed","equals":true},"requiredWhen":{"field":"dengueNs1Performed","equals":true}},{"key":"dengueNs1KitExpiry","code":"Hạn dùng","label":"Hạn sử dụng kit Dengue NS1","type":"DATE","group":"Dengue NS1","displayOrder":34,"visibleWhen":{"field":"dengueNs1Performed","equals":true},"requiredWhen":{"field":"dengueNs1Performed","equals":true}},{"key":"dengueNs1ControlValid","code":"Control","label":"Control hợp lệ Dengue NS1","type":"BOOLEAN","group":"Dengue NS1","displayOrder":35,"visibleWhen":{"field":"dengueNs1Performed","equals":true},"requiredWhen":{"field":"dengueNs1Performed","equals":true}},{"key":"dengueNs1ReadAt","code":"Thời gian đọc","label":"Thời gian đọc Dengue NS1","type":"TEXT","group":"Dengue NS1","displayOrder":36,"visibleWhen":{"field":"dengueNs1Performed","equals":true},"requiredWhen":{"field":"dengueNs1Performed","equals":true},"pattern":"^\\\\d{4}-\\\\d{2}-\\\\d{2}[ T]\\\\d{2}:\\\\d{2}(:\\\\d{2})?$","patternMessage":"Thời gian đọc phải theo định dạng YYYY-MM-DD HH:mm"},{"key":"influenzaAPerformed","code":"Influenza A - thực hiện","label":"Thực hiện Cúm A","type":"BOOLEAN","group":"Cúm A","displayOrder":37},{"key":"influenzaAResult","code":"Influenza A","label":"Kết quả Cúm A","type":"SELECT","group":"Cúm A","displayOrder":38,"visibleWhen":{"field":"influenzaAPerformed","equals":true},"requiredWhen":{"field":"influenzaAPerformed","equals":true},"options":["NEGATIVE","POSITIVE","INDETERMINATE","INVALID"],"referenceRanges":[{"sex":"ANY","minAge":0,"maxAge":200,"ageUnit":"YEARS","normalValues":["NEGATIVE"]}]},{"key":"influenzaASampleCode","code":"Mã mẫu","label":"Mã mẫu Cúm A","type":"TEXT","group":"Cúm A","displayOrder":39,"visibleWhen":{"field":"influenzaAPerformed","equals":true},"requiredWhen":{"field":"influenzaAPerformed","equals":true}},{"key":"influenzaASampleType","code":"Loại mẫu","label":"Loại mẫu Cúm A","type":"SELECT","group":"Cúm A","displayOrder":40,"visibleWhen":{"field":"influenzaAPerformed","equals":true},"requiredWhen":{"field":"influenzaAPerformed","equals":true},"options":["WHOLE_BLOOD","SERUM","PLASMA","NASOPHARYNGEAL_SWAB","OTHER"]},{"key":"influenzaAKitName","code":"Kit","label":"Tên kit Cúm A","type":"TEXT","group":"Cúm A","displayOrder":41,"visibleWhen":{"field":"influenzaAPerformed","equals":true},"requiredWhen":{"field":"influenzaAPerformed","equals":true}},{"key":"influenzaALotNumber","code":"Số lô","label":"Số lô kit Cúm A","type":"TEXT","group":"Cúm A","displayOrder":42,"visibleWhen":{"field":"influenzaAPerformed","equals":true},"requiredWhen":{"field":"influenzaAPerformed","equals":true}},{"key":"influenzaAKitExpiry","code":"Hạn dùng","label":"Hạn sử dụng kit Cúm A","type":"DATE","group":"Cúm A","displayOrder":43,"visibleWhen":{"field":"influenzaAPerformed","equals":true},"requiredWhen":{"field":"influenzaAPerformed","equals":true}},{"key":"influenzaAControlValid","code":"Control","label":"Control hợp lệ Cúm A","type":"BOOLEAN","group":"Cúm A","displayOrder":44,"visibleWhen":{"field":"influenzaAPerformed","equals":true},"requiredWhen":{"field":"influenzaAPerformed","equals":true}},{"key":"influenzaAReadAt","code":"Thời gian đọc","label":"Thời gian đọc Cúm A","type":"TEXT","group":"Cúm A","displayOrder":45,"visibleWhen":{"field":"influenzaAPerformed","equals":true},"requiredWhen":{"field":"influenzaAPerformed","equals":true},"pattern":"^\\\\d{4}-\\\\d{2}-\\\\d{2}[ T]\\\\d{2}:\\\\d{2}(:\\\\d{2})?$","patternMessage":"Thời gian đọc phải theo định dạng YYYY-MM-DD HH:mm"},{"key":"influenzaBPerformed","code":"Influenza B - thực hiện","label":"Thực hiện Cúm B","type":"BOOLEAN","group":"Cúm B","displayOrder":46},{"key":"influenzaBResult","code":"Influenza B","label":"Kết quả Cúm B","type":"SELECT","group":"Cúm B","displayOrder":47,"visibleWhen":{"field":"influenzaBPerformed","equals":true},"requiredWhen":{"field":"influenzaBPerformed","equals":true},"options":["NEGATIVE","POSITIVE","INDETERMINATE","INVALID"],"referenceRanges":[{"sex":"ANY","minAge":0,"maxAge":200,"ageUnit":"YEARS","normalValues":["NEGATIVE"]}]},{"key":"influenzaBSampleCode","code":"Mã mẫu","label":"Mã mẫu Cúm B","type":"TEXT","group":"Cúm B","displayOrder":48,"visibleWhen":{"field":"influenzaBPerformed","equals":true},"requiredWhen":{"field":"influenzaBPerformed","equals":true}},{"key":"influenzaBSampleType","code":"Loại mẫu","label":"Loại mẫu Cúm B","type":"SELECT","group":"Cúm B","displayOrder":49,"visibleWhen":{"field":"influenzaBPerformed","equals":true},"requiredWhen":{"field":"influenzaBPerformed","equals":true},"options":["WHOLE_BLOOD","SERUM","PLASMA","NASOPHARYNGEAL_SWAB","OTHER"]},{"key":"influenzaBKitName","code":"Kit","label":"Tên kit Cúm B","type":"TEXT","group":"Cúm B","displayOrder":50,"visibleWhen":{"field":"influenzaBPerformed","equals":true},"requiredWhen":{"field":"influenzaBPerformed","equals":true}},{"key":"influenzaBLotNumber","code":"Số lô","label":"Số lô kit Cúm B","type":"TEXT","group":"Cúm B","displayOrder":51,"visibleWhen":{"field":"influenzaBPerformed","equals":true},"requiredWhen":{"field":"influenzaBPerformed","equals":true}},{"key":"influenzaBKitExpiry","code":"Hạn dùng","label":"Hạn sử dụng kit Cúm B","type":"DATE","group":"Cúm B","displayOrder":52,"visibleWhen":{"field":"influenzaBPerformed","equals":true},"requiredWhen":{"field":"influenzaBPerformed","equals":true}},{"key":"influenzaBControlValid","code":"Control","label":"Control hợp lệ Cúm B","type":"BOOLEAN","group":"Cúm B","displayOrder":53,"visibleWhen":{"field":"influenzaBPerformed","equals":true},"requiredWhen":{"field":"influenzaBPerformed","equals":true}},{"key":"influenzaBReadAt","code":"Thời gian đọc","label":"Thời gian đọc Cúm B","type":"TEXT","group":"Cúm B","displayOrder":54,"visibleWhen":{"field":"influenzaBPerformed","equals":true},"requiredWhen":{"field":"influenzaBPerformed","equals":true},"pattern":"^\\\\d{4}-\\\\d{2}-\\\\d{2}[ T]\\\\d{2}:\\\\d{2}(:\\\\d{2})?$","patternMessage":"Thời gian đọc phải theo định dạng YYYY-MM-DD HH:mm"}],"rules":[{"type":"AT_LEAST_ONE_TRUE","keys":["hbsAgPerformed","antiHcvPerformed","hivPerformed","dengueNs1Performed","influenzaAPerformed","influenzaBPerformed"],"onSignOnly":true,"severity":"ERROR","message":"Phải chọn ít nhất một test nhanh đã thực hiện."},{"type":"BOOLEAN_MUST_BE_TRUE_WHEN","field":"hbsAgControlValid","when":{"field":"hbsAgPerformed","equals":true},"onSignOnly":true,"severity":"ERROR","message":"Control của HBsAg phải hợp lệ trước khi ký."},{"type":"VALUE_NOT_ALLOWED_WHEN","field":"hbsAgResult","value":"INVALID","when":{"field":"hbsAgPerformed","equals":true},"onSignOnly":true,"severity":"ERROR","message":"Kết quả HBsAg INVALID không thể ký."},{"type":"BOOLEAN_MUST_BE_TRUE_WHEN","field":"antiHcvControlValid","when":{"field":"antiHcvPerformed","equals":true},"onSignOnly":true,"severity":"ERROR","message":"Control của Anti-HCV phải hợp lệ trước khi ký."},{"type":"VALUE_NOT_ALLOWED_WHEN","field":"antiHcvResult","value":"INVALID","when":{"field":"antiHcvPerformed","equals":true},"onSignOnly":true,"severity":"ERROR","message":"Kết quả Anti-HCV INVALID không thể ký."},{"type":"BOOLEAN_MUST_BE_TRUE_WHEN","field":"hivControlValid","when":{"field":"hivPerformed","equals":true},"onSignOnly":true,"severity":"ERROR","message":"Control của HIV Ag/Ab phải hợp lệ trước khi ký."},{"type":"VALUE_NOT_ALLOWED_WHEN","field":"hivResult","value":"INVALID","when":{"field":"hivPerformed","equals":true},"onSignOnly":true,"severity":"ERROR","message":"Kết quả HIV Ag/Ab INVALID không thể ký."},{"type":"BOOLEAN_MUST_BE_TRUE_WHEN","field":"dengueNs1ControlValid","when":{"field":"dengueNs1Performed","equals":true},"onSignOnly":true,"severity":"ERROR","message":"Control của Dengue NS1 phải hợp lệ trước khi ký."},{"type":"VALUE_NOT_ALLOWED_WHEN","field":"dengueNs1Result","value":"INVALID","when":{"field":"dengueNs1Performed","equals":true},"onSignOnly":true,"severity":"ERROR","message":"Kết quả Dengue NS1 INVALID không thể ký."},{"type":"BOOLEAN_MUST_BE_TRUE_WHEN","field":"influenzaAControlValid","when":{"field":"influenzaAPerformed","equals":true},"onSignOnly":true,"severity":"ERROR","message":"Control của Cúm A phải hợp lệ trước khi ký."},{"type":"VALUE_NOT_ALLOWED_WHEN","field":"influenzaAResult","value":"INVALID","when":{"field":"influenzaAPerformed","equals":true},"onSignOnly":true,"severity":"ERROR","message":"Kết quả Cúm A INVALID không thể ký."},{"type":"BOOLEAN_MUST_BE_TRUE_WHEN","field":"influenzaBControlValid","when":{"field":"influenzaBPerformed","equals":true},"onSignOnly":true,"severity":"ERROR","message":"Control của Cúm B phải hợp lệ trước khi ký."},{"type":"VALUE_NOT_ALLOWED_WHEN","field":"influenzaBResult","value":"INVALID","when":{"field":"influenzaBPerformed","equals":true},"onSignOnly":true,"severity":"ERROR","message":"Kết quả Cúm B INVALID không thể ký."}]}
$j$::jsonb,'PUBLISHED','Mở rộng sáu test nhanh, thông tin kit, control và thời gian đọc',CURRENT_DATE,
'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',NOW())
ON CONFLICT (template_id, version_no) DO NOTHING;

-- ===================================================================
-- Billable laboratory analytes. They reuse the published panel schema;
-- the backend exposes and validates only the field mapped by serviceCode.
-- Prices are demonstration values and can be edited by Clinic Manager.
-- Individual analytes are intentionally hidden from online booking while
-- remaining selectable by doctor and receptionist workflows.
-- ===================================================================
INSERT INTO medical_service (
    service_id, service_code, created_at, updated_at, deleted, description,
    status, is_point_of_care, name, price, department_type, duration_minutes,
    workflow_priority, requires_doctor_order, requires_return_to_doctor,
    requires_specimen, result_wait_minutes, allow_customer_booking,
    minimum_age, maximum_age, allowed_gender, department_id,
    required_specialization_id, required_capability_id
)
SELECT gen_random_uuid(), item.service_code, NOW(), NOW(), false,
       'Chỉ số lẻ thuộc gói ' || parent.name || '. Giá mẫu cần được rà soát trước vận hành.',
       'ACTIVE', parent.is_point_of_care, item.name, item.price, parent.department_type,
       parent.duration_minutes, parent.workflow_priority, true, true, true,
       parent.result_wait_minutes, false, parent.minimum_age, parent.maximum_age,
       parent.allowed_gender, parent.department_id, parent.required_specialization_id,
       parent.required_capability_id
FROM (VALUES
 ('AN-CBC-RBC','Số lượng hồng cầu (RBC)',5000,'LAB-001'),('AN-CBC-HGB','Huyết sắc tố (HGB)',5000,'LAB-001'),
 ('AN-CBC-HCT','Hematocrit (HCT)',5000,'LAB-001'),('AN-CBC-MCV','MCV',5000,'LAB-001'),
 ('AN-CBC-MCH','MCH',5000,'LAB-001'),('AN-CBC-MCHC','MCHC',5000,'LAB-001'),
 ('AN-CBC-RDWCV','RDW-CV',5000,'LAB-001'),('AN-CBC-RDWSD','RDW-SD',5000,'LAB-001'),
 ('AN-CBC-WBC','Số lượng bạch cầu (WBC)',10000,'LAB-001'),('AN-CBC-NEUTP','Bạch cầu trung tính (%)',5000,'LAB-001'),
 ('AN-CBC-NEUTA','Bạch cầu trung tính tuyệt đối',5000,'LAB-001'),('AN-CBC-LYMP','Bạch cầu lympho (%)',5000,'LAB-001'),
 ('AN-CBC-LYMA','Bạch cầu lympho tuyệt đối',5000,'LAB-001'),('AN-CBC-MONOP','Bạch cầu mono (%)',5000,'LAB-001'),
 ('AN-CBC-MONOA','Bạch cầu mono tuyệt đối',5000,'LAB-001'),('AN-CBC-EOSP','Bạch cầu ái toan (%)',5000,'LAB-001'),
 ('AN-CBC-EOSA','Bạch cầu ái toan tuyệt đối',5000,'LAB-001'),('AN-CBC-BASOP','Bạch cầu ái kiềm (%)',5000,'LAB-001'),
 ('AN-CBC-BASOA','Bạch cầu ái kiềm tuyệt đối',5000,'LAB-001'),('AN-CBC-PLT','Số lượng tiểu cầu (PLT)',10000,'LAB-001'),
 ('AN-CBC-MPV','MPV',5000,'LAB-001'),('AN-CBC-PDW','PDW',5000,'LAB-001'),
 ('AN-CBC-PCT','Plateletcrit (PCT)',5000,'LAB-001'),('AN-CBC-PLCR','P-LCR',5000,'LAB-001'),
 ('AN-BIO-HBA1C','Hemoglobin A1c',30000,'LAB-003'),('AN-BIO-TC','Cholesterol toàn phần',30000,'LAB-003'),
 ('AN-BIO-TG','Triglyceride',30000,'LAB-003'),('AN-BIO-HDL','HDL Cholesterol',30000,'LAB-003'),
 ('AN-BIO-LDL','LDL Cholesterol',30000,'LAB-003'),('AN-BIO-UA','Acid uric',30000,'LAB-003'),
 ('AN-BIO-TP','Protein toàn phần',30000,'LAB-003'),
 ('AN-LIV-AST','AST',25000,'LAB-004'),('AN-LIV-ALT','ALT',25000,'LAB-004'),
 ('AN-LIV-ALP','Alkaline phosphatase',25000,'LAB-004'),('AN-LIV-GGT','GGT',25000,'LAB-004'),
 ('AN-LIV-TBIL','Bilirubin toàn phần',25000,'LAB-004'),('AN-LIV-DBIL','Bilirubin trực tiếp',25000,'LAB-004'),
 ('AN-LIV-ALB','Albumin',25000,'LAB-004'),
 ('AN-REN-UREA','Urea',20000,'LAB-005'),('AN-REN-CREA','Creatinine',20000,'LAB-005'),
 ('AN-REN-NA','Sodium',20000,'LAB-005'),('AN-REN-K','Potassium',20000,'LAB-005'),
 ('AN-REN-CL','Chloride',20000,'LAB-005'),('AN-REN-HCO3','Bicarbonate / Total CO2',20000,'LAB-005'),
 ('AN-REN-CA','Calcium toàn phần',20000,'LAB-005'),('AN-REN-PO4','Phosphate',20000,'LAB-005'),
 ('AN-URI-SG','Tỷ trọng nước tiểu',10000,'LAB-006'),('AN-URI-PH','pH nước tiểu',10000,'LAB-006'),
 ('AN-URI-LEU','Leukocyte Esterase',10000,'LAB-006'),('AN-URI-NIT','Nitrite',10000,'LAB-006'),
 ('AN-URI-PRO','Protein nước tiểu',10000,'LAB-006'),('AN-URI-GLU','Glucose nước tiểu',10000,'LAB-006'),
 ('AN-URI-KET','Ketone',10000,'LAB-006'),('AN-URI-URO','Urobilinogen',10000,'LAB-006'),
 ('AN-URI-BIL','Bilirubin nước tiểu',10000,'LAB-006'),('AN-URI-BLD','Máu/Hemoglobin nước tiểu',10000,'LAB-006')
) AS item(service_code, name, price, panel_code)
JOIN medical_service parent ON parent.service_code = item.panel_code AND parent.deleted = false
ON CONFLICT (service_code) DO NOTHING;

INSERT INTO medical_service_form_template
(binding_id, created_at, updated_at, deleted, service_id, template_id)
SELECT gen_random_uuid(), NOW(), NOW(), false, mapping.service_id, mapping.template_id
FROM (VALUES
 ('40000008-0000-0000-0000-000000000008'::uuid,'cf000006-0000-0000-0000-000000000006'::uuid),('40000009-0000-0000-0000-000000000009','cf000007-0000-0000-0000-000000000007'),('40000010-0000-0000-0000-000000000010','cf000008-0000-0000-0000-000000000008'),('40000011-0000-0000-0000-000000000011','cf000009-0000-0000-0000-000000000009'),('40000012-0000-0000-0000-000000000012','cf00000a-0000-0000-0000-00000000000a'),('40000013-0000-0000-0000-000000000013','cf00000b-0000-0000-0000-00000000000b'),('40000014-0000-0000-0000-000000000014','cf00000c-0000-0000-0000-00000000000c'),('40000020-0000-0000-0000-000000000020','cf00000d-0000-0000-0000-00000000000d'),
 ('40000017-0000-0000-0000-000000000017','cf00000e-0000-0000-0000-00000000000e'),('40000018-0000-0000-0000-000000000018','cf00000e-0000-0000-0000-00000000000e'),('40000015-0000-0000-0000-000000000015','cf00000f-0000-0000-0000-00000000000f'),('40000016-0000-0000-0000-000000000016','cf000010-0000-0000-0000-000000000010'),('40000035-0000-0000-0000-000000000035','cf000011-0000-0000-0000-000000000011'),('40000019-0000-0000-0000-000000000019','cf000012-0000-0000-0000-000000000012')
) AS mapping(service_id, template_id)
ON CONFLICT (service_id) DO NOTHING;

-- Parent bindings now exist; bind every billable analyte to its panel's
-- published schema.
INSERT INTO medical_service_form_template
(binding_id, created_at, updated_at, deleted, service_id, template_id)
SELECT gen_random_uuid(), NOW(), NOW(), false, child.service_id, parent_binding.template_id
FROM medical_service child
JOIN (VALUES
 ('AN-CBC-%','LAB-001'),('AN-BIO-%','LAB-003'),('AN-LIV-%','LAB-004'),
 ('AN-REN-%','LAB-005'),('AN-URI-%','LAB-006')
) AS mapping(code_pattern, panel_code) ON child.service_code LIKE mapping.code_pattern
JOIN medical_service parent ON parent.service_code = mapping.panel_code AND parent.deleted = false
JOIN medical_service_form_template parent_binding
  ON parent_binding.service_id = parent.service_id AND parent_binding.deleted = false
WHERE child.deleted = false
ON CONFLICT (service_id) DO NOTHING;

-- Moi dich vu can lam sang dang hoat dong phai co dung mot mau va mot phien
-- ban PUBLISHED dang co hieu luc. Neu them dich vu CLS ma quen cau hinh form,
-- data.sql se dung ngay thay vi de man tra ket qua rong.
DO $clinical_form_check$
DECLARE
    invalid_services text;
BEGIN
    SELECT string_agg(ms.name, ', ' ORDER BY ms.name)
    INTO invalid_services
    FROM medical_service ms
    WHERE ms.deleted = false
      AND ms.status = 'ACTIVE'
      AND ms.department_type = 'PARACLINICAL'
      AND (
          (SELECT COUNT(*) FROM medical_service_form_template binding
           WHERE binding.service_id = ms.service_id AND binding.deleted = false) <> 1
          OR NOT EXISTS (
              SELECT 1
              FROM medical_service_form_template binding
              JOIN clinical_form_template template ON template.template_id = binding.template_id
              JOIN clinical_form_template_version version ON version.template_id = template.template_id
              WHERE binding.service_id = ms.service_id
                AND binding.deleted = false
                AND template.deleted = false AND template.active = true
                AND version.deleted = false
                AND version.status = 'PUBLISHED'
                AND version.effective_from <= CURRENT_DATE
          )
      );

    IF invalid_services IS NOT NULL THEN
        RAISE EXCEPTION 'Dịch vụ cận lâm sàng thiếu biểu mẫu kết quả đang áp dụng: %', invalid_services;
    END IF;
END
$clinical_form_check$;

-- Du lieu co cau truc cho ket qua can lam sang. _meta la snapshot canh bao/
-- phep tinh da tao tai thoi diem ky, giup man xem lich su hien thi dung.
UPDATE test_result tr
SET form_template_version_id = tv.version_id,
    result_data = CASE ms.service_code
        WHEN 'LAB-001' THEN
            $j${"rbc":4.62,"hgb":108,"hct":34.1,"mcv":78.2,"mch":25.1,"mchc":316,"rdwCv":15.8,"rdwSd":49.2,"wbc":12.8,"neutPercent":72,"neutAbsolute":9.22,"lymphPercent":20,"lymphAbsolute":2.56,"monoPercent":5,"monoAbsolute":0.64,"eosPercent":2,"eosAbsolute":0.26,"basoPercent":1,"basoAbsolute":0.13,"plt":268,"mpv":10.4,"pdw":12.3,"pct":0.278,"pLcr":28.1,"_meta":{"flags":{"hgb":{"status":"LOW","referenceRange":{"low":120,"high":150}},"wbc":{"status":"HIGH","referenceRange":{"low":4,"high":10}},"neutAbsolute":{"status":"HIGH","referenceRange":{"low":2,"high":7}},"plt":{"status":"NORMAL","referenceRange":{"low":150,"high":410}}},"calculations":{},"warnings":[]}}$j$::jsonb
        WHEN 'LAB-002' THEN
            $j${"glucoseContext":"RANDOM","glucoseQualifier":"EQUAL","glucose":28.4,"sampleConditionNote":"Mẫu huyết thanh không tan máu.","_meta":{"flags":{"glucose":{"status":"CRITICAL_HIGH","referenceRange":{"high":7.8},"criticalRange":{"low":2.5,"high":25}}},"calculations":{},"warnings":[]}}$j$::jsonb
        WHEN 'LAB-003' THEN
            $j${"hba1c":5.6,"totalCholesterol":5.8,"triglyceride":1.6,"hdlC":0.92,"ldlC":3.9,"nonHdlC":4.88,"acidUric":318,"totalProtein":71.2,"_meta":{"flags":{"hba1c":{"status":"NORMAL","referenceRange":{"high":5.6}},"totalCholesterol":{"status":"HIGH","referenceRange":{"high":5.2}},"hdlC":{"status":"LOW","referenceRange":{"low":1}},"ldlC":{"status":"HIGH","referenceRange":{"high":3.4}},"nonHdlC":{"status":"HIGH","referenceRange":{"high":4.1}}},"calculations":{"nonHdlC":{"calculatorKey":"NON_HDL_C_V1","status":"ESTIMATED","value":4.88}},"warnings":[]}}$j$::jsonb
        WHEN 'LAB-004' THEN
            $j${"ast":38,"alt":86,"alp":112,"ggt":74,"bilirubinTotal":24.5,"bilirubinDirect":6.2,"bilirubinIndirect":18.3,"albumin":42.6,"_meta":{"flags":{"ast":{"status":"NORMAL","referenceRange":{"high":40}},"alt":{"status":"HIGH","referenceRange":{"high":41}},"ggt":{"status":"HIGH","referenceRange":{"high":60}},"bilirubinTotal":{"status":"HIGH","referenceRange":{"high":21}},"albumin":{"status":"NORMAL","referenceRange":{"low":35,"high":52}}},"calculations":{"bilirubinIndirect":{"calculatorKey":"INDIRECT_BILIRUBIN_V1","status":"ESTIMATED","value":18.3}},"warnings":[]}}$j$::jsonb
        WHEN 'LAB-005' THEN
            $j${"urea":9.2,"bun":25.77,"creatinine":146,"egfr":48.62,"sodium":138,"potassium":6.8,"chloride":103,"bicarbonate":23,"calcium":2.28,"phosphate":1.18,"_meta":{"flags":{"urea":{"status":"HIGH","referenceRange":{"low":2.5,"high":7.5}},"creatinine":{"status":"HIGH","referenceRange":{"low":62,"high":106}},"egfr":{"status":"LOW","referenceRange":{"low":60}},"potassium":{"status":"CRITICAL_HIGH","referenceRange":{"low":3.5,"high":5.1},"criticalRange":{"low":2.5,"high":6.5}}},"calculations":{"bun":{"calculatorKey":"BUN_FROM_UREA_V1","status":"ESTIMATED","value":25.77},"egfr":{"calculatorKey":"EGFR_CKD_EPI_2021_V1","status":"ESTIMATED","value":48.62}},"warnings":[]}}$j$::jsonb
        WHEN 'LAB-006' THEN
            $j${"color":"YELLOW","clarity":"SLIGHTLY_CLOUDY","specificGravity":1.018,"ph":6.0,"leukocyteEsterase":"2+","nitrite":"POSITIVE","protein":"TRACE","urineGlucose":"NEGATIVE","ketone":"NEGATIVE","urobilinogen":"TRACE","urineBilirubin":"NEGATIVE","blood":"1+","microscopyPerformed":true,"urineRbc":4,"urineWbc":18,"epithelialCells":"FEW","casts":"NONE","crystals":"NONE","bacteria":"MANY","yeast":"NONE","mucus":"FEW","parasites":"NONE","microscopyComment":"Bạch cầu và vi khuẩn tăng.","_meta":{"flags":{"specificGravity":{"status":"NORMAL","referenceRange":{"low":1.005,"high":1.03}},"leukocyteEsterase":{"status":"ABNORMAL","referenceRange":{"normalValues":["NEGATIVE"]}},"nitrite":{"status":"ABNORMAL","referenceRange":{"normalValues":["NEGATIVE"]}},"urineWbc":{"status":"HIGH","referenceRange":{"low":0,"high":5}}},"calculations":{},"warnings":[]}}$j$::jsonb
        WHEN 'LAB-007' THEN
            $j${"crpAssayType":"STANDARD_CRP","crpQualifier":"EQUAL","crp":126.4,"detectionLimit":0.5,"methodNote":"Định lượng miễn dịch đo độ đục.","_meta":{"flags":{"crp":{"status":"CRITICAL_HIGH","referenceRange":{"high":5},"criticalRange":{"high":100}}},"calculations":{},"warnings":[]}}$j$::jsonb
        WHEN 'IMG-003' THEN
            $j${"liverDescription":"Gan kích thước bình thường, nhu mô đồng nhất.","gallbladderDescription":"Túi mật không sỏi, thành không dày.","kidneyDescription":"Hai thận kích thước bình thường, không ứ nước.","abnormalFinding":"","imagingConclusion":"Chưa ghi nhận bất thường trên siêu âm ổ bụng.","_meta":{"flags":{},"calculations":{}}}$j$::jsonb
        WHEN 'IMG-001' THEN
            $j${"lesionDescription":"Nhu mô phổi thông khí đều, không thấy đám mờ khu trú.","lesionLocation":"","imagingConclusion":"Chưa ghi nhận bất thường tim phổi cấp.","_meta":{"flags":{},"calculations":{}}}$j$::jsonb
        WHEN 'IMG-006' THEN
            $j${"heartRate":76,"rhythmType":"SINUS","ecgDescription":"Nhịp xoang đều, trục tim bình thường.","ecgConclusion":"Điện tâm đồ trong giới hạn bình thường.","_meta":{"flags":{"heartRate":{"status":"NORMAL"}},"calculations":{}}}$j$::jsonb
        WHEN 'LAB-008' THEN
            CASE WHEN req.status = 'IN_PROGRESS' THEN
              $j${"hbsAgPerformed":true,"hbsAgResult":"INVALID","hbsAgSampleCode":"SMP-RAPID-INVALID","hbsAgSampleType":"SERUM","hbsAgKitName":"Rapid HBsAg Demo","hbsAgLotNumber":"LOT-DEMO-01","hbsAgKitExpiry":"2027-12-31","hbsAgControlValid":false,"hbsAgReadAt":"2026-08-28 09:30","_meta":{"flags":{"hbsAgResult":{"status":"ABNORMAL","referenceRange":{"normalValues":["NEGATIVE"]}}},"calculations":{},"warnings":["Control không hợp lệ; không được ký kết quả."]}}$j$::jsonb
            ELSE
              $j${"hbsAgPerformed":true,"hbsAgResult":"NEGATIVE","hbsAgSampleCode":"SMP-RAPID-001","hbsAgSampleType":"SERUM","hbsAgKitName":"Rapid HBsAg Demo","hbsAgLotNumber":"LOT-260801","hbsAgKitExpiry":"2027-12-31","hbsAgControlValid":true,"hbsAgReadAt":"2026-08-28 09:30","antiHcvPerformed":true,"antiHcvResult":"NEGATIVE","antiHcvSampleCode":"SMP-RAPID-001","antiHcvSampleType":"SERUM","antiHcvKitName":"Rapid Anti-HCV Demo","antiHcvLotNumber":"LOT-260802","antiHcvKitExpiry":"2027-12-31","antiHcvControlValid":true,"antiHcvReadAt":"2026-08-28 09:35","hivPerformed":false,"dengueNs1Performed":false,"influenzaAPerformed":false,"influenzaBPerformed":false,"_meta":{"flags":{"hbsAgResult":{"status":"NORMAL","referenceRange":{"normalValues":["NEGATIVE"]}},"antiHcvResult":{"status":"NORMAL","referenceRange":{"normalValues":["NEGATIVE"]}}},"calculations":{},"warnings":[]}}$j$::jsonb END
        ELSE '{}'::jsonb
    END,
    conclusion = CASE ms.service_code
        WHEN 'LAB-001' THEN 'Thiếu máu nhẹ và tăng bạch cầu; cần đối chiếu lâm sàng.'
        WHEN 'LAB-002' THEN 'Glucose tăng mức cảnh báo nghiêm trọng; cần báo bác sĩ phụ trách.'
        WHEN 'LAB-003' THEN 'Rối loạn một số chỉ số lipid máu.'
        WHEN 'LAB-004' THEN 'Men gan và Bilirubin tăng; cần đối chiếu lâm sàng.'
        WHEN 'LAB-005' THEN 'eGFR giảm và Potassium ở mức cảnh báo nghiêm trọng; eGFR là giá trị ước tính.'
        WHEN 'LAB-006' THEN 'Kết quả nước tiểu có bạch cầu, Nitrite và vi khuẩn.'
        WHEN 'LAB-007' THEN 'CRP tăng mức cảnh báo nghiêm trọng.'
        WHEN 'IMG-003' THEN 'Chưa ghi nhận bất thường trên siêu âm ổ bụng.'
        WHEN 'IMG-001' THEN 'Chưa ghi nhận bất thường tim phổi cấp.'
        WHEN 'IMG-006' THEN 'Nhịp xoang, điện tâm đồ trong giới hạn bình thường.'
        WHEN 'LAB-008' THEN CASE WHEN req.status = 'IN_PROGRESS'
            THEN 'Control không hợp lệ; đây là bản nháp và chưa được ký.'
            ELSE 'Các test nhanh đã thực hiện đều âm tính.' END
        ELSE tr.conclusion
    END,
    performed_by = CASE WHEN ms.service_code = 'IMG-006'
                        THEN '91000000-0000-0000-0000-000000000010'::uuid
                        ELSE tr.performed_by END,
    verified_by = CASE WHEN ms.service_code = 'IMG-006'
                       THEN '91000000-0000-0000-0000-000000000007'::uuid
                       ELSE tr.verified_by END,
    updated_at = NOW()
FROM test_request req
JOIN medical_service ms ON ms.service_id = req.service_id
JOIN medical_service_form_template b ON b.service_id = ms.service_id AND b.deleted = false
JOIN clinical_form_template_version tv
  ON tv.template_id = b.template_id AND tv.status = 'PUBLISHED' AND tv.deleted = false
 AND tv.version_no = (SELECT MAX(latest.version_no)
                      FROM clinical_form_template_version latest
                      WHERE latest.template_id = b.template_id
                        AND latest.status = 'PUBLISHED' AND latest.deleted = false
                        AND latest.effective_from <= CURRENT_DATE)
WHERE tr.test_request_id = req.test_request_id;

-- Revision 1 la ban da ky; khong seed attachment neu khong co tep vat ly.
INSERT INTO test_result_revision (
    revision_id, created_at, updated_at, deleted, result_id, revision_no,
    status, result_data, conclusion, template_version_id, amendment_reason,
    entered_by, signed_by, signed_at
)
SELECT gen_random_uuid(), tr.created_at, tr.updated_at, false, tr.result_id, 1,
       CASE WHEN req.status = 'COMPLETED' THEN 'SIGNED' ELSE 'DRAFT' END,
       tr.result_data, tr.conclusion, tr.form_template_version_id, NULL,
       tr.performed_by,
       CASE WHEN req.status = 'COMPLETED' THEN tr.verified_by ELSE NULL END,
       CASE WHEN req.status = 'COMPLETED' THEN tr.verified_at ELSE NULL END
FROM test_result tr
JOIN test_request req ON req.test_request_id = tr.test_request_id;

-- Thong bao cong khai do Clinic Manager quan ly.
INSERT INTO public_announcement (
    announcement_id, created_at, updated_at, deleted, title, content,
    published, starts_at, ends_at, created_by_account_id
) VALUES
('5d000001-0000-0000-0000-000000000001', NOW(), NOW(), false,
 'Lịch khám trong tuần',
 'Khung giờ kiểm thử gồm ca sáng 00:00–08:00, ca chiều 08:00–16:00 và ca tối 16:00–23:59.',
 true, date_trunc('week', CURRENT_DATE), date_trunc('week', CURRENT_DATE) + INTERVAL '14 days',
 '30000013-3333-3333-3333-333333333333'),
('5d000002-0000-0000-0000-000000000002', NOW(), NOW(), false,
 'Chuẩn bị trước khi xét nghiệm',
 'Khách hàng vui lòng làm theo hướng dẫn nhịn ăn hoặc lấy mẫu được cung cấp trong phiếu chỉ định.',
 true, CURRENT_DATE - INTERVAL '1 day', CURRENT_DATE + INTERVAL '30 days',
 '30000013-3333-3333-3333-333333333333'),
('5d000003-0000-0000-0000-000000000003', NOW(), NOW(), false,
 'Thông báo đang soạn',
 'Nội dung nháp dành cho màn hình quản lý thông báo.',
 false, NULL, NULL, '30000013-3333-3333-3333-333333333333');

-- Thông tin công khai và pháp lý của phòng khám (một bản ghi duy nhất).
INSERT INTO clinic_information (
    clinic_information_id, created_at, updated_at, deleted, clinic_name, legal_name,
    tax_code, operating_license, short_description, support_email, phone, address,
    website_url, facebook_url, youtube_url, zalo_url, latitude, longitude
) VALUES (
    '00000000-0000-0000-0000-000000000100', NOW(), NOW(), false,
    'Phòng khám CareS', 'Công ty TNHH Phòng khám CareS', '0101234567', '000123/HNO-GPHD',
    'Phòng khám đa khoa cung cấp dịch vụ chăm sóc sức khỏe chất lượng và thuận tiện.',
    'lienhe@caresclinic.vn', '1900 1234',
    'Khu Công nghệ cao Hòa Lạc, Thạch Thất, Hà Nội', NULL,
    'https://www.facebook.com/profile.php?id=61593125259676', NULL, NULL,
    21.0128000, 105.5259000
)
ON CONFLICT (clinic_information_id) DO NOTHING;
