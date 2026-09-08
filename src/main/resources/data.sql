-- CareS: dữ liệu GIẢ LẬP cho database demo riêng, không dùng cho khám chữa bệnh.
-- Không chạy khi backend đang hoạt động. Xem docs/demo/README.md.
ROLLBACK;
SET cares.demo_reset = 'yes';
BEGIN;
SET LOCAL TIME ZONE 'Asia/Ho_Chi_Minh';
DO $guard$
BEGIN
 IF current_setting('cares.demo_reset', true) IS DISTINCT FROM 'yes' THEN
  RAISE EXCEPTION 'Reset bị chặn. Dừng backend và SET cares.demo_reset = ''yes'' trong cùng session trước khi chạy data.sql.';
 END IF;
END $guard$;
CREATE TEMP TABLE demo_clock ON COMMIT DROP AS
SELECT COALESCE(NULLIF(current_setting('cares.demo_now',true),'')::timestamp,
               CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh') AS moment;
CREATE OR REPLACE FUNCTION pg_temp.demo_now() RETURNS timestamp LANGUAGE sql STABLE
AS 'SELECT moment FROM demo_clock';
CREATE OR REPLACE FUNCTION pg_temp.demo_date() RETURNS date LANGUAGE sql STABLE
AS 'SELECT moment::date FROM demo_clock';
CREATE OR REPLACE FUNCTION pg_temp.did(key text) RETURNS uuid LANGUAGE sql IMMUTABLE
AS 'SELECT md5(''CareS-demo-v2:'' || key)::uuid';

ALTER TABLE membership_card
    ADD COLUMN IF NOT EXISTS benefit_starts_at timestamp;

TRUNCATE TABLE
    clinic_information, membership_card_ledger, membership_card, membership_policy, family_member,
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
    '7c000001-0000-0000-0000-000000000001', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false,
    1000000, 15, 12, true
);

-- ===================================================================
-- Specialization (danh muc chuyen khoa - toi thieu de bo sung vao department)
-- ===================================================================
INSERT INTO specialization (specialization_id, created_at, updated_at, deleted, active, name, description) VALUES
('00000001-1111-1111-1111-111111111111', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, true, 'Nội khoa', 'Khám, chẩn đoán và điều trị các bệnh lý nội khoa thường gặp'),
('00000002-2222-2222-2222-222222222222', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, true, 'Nhi khoa', 'Khám và chăm sóc sức khỏe cho trẻ em'),
('00000003-3333-3333-3333-333333333333', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, true, 'Ngoại khoa', 'Khám và xử trí ban đầu các bệnh lý, chấn thương ngoại khoa'),
('00000004-4444-4444-4444-444444444444', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, true, 'Da liễu', 'Khám và điều trị các bệnh lý về da, tóc và móng'),
('00000008-8888-8888-8888-888888888888', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, true, 'Sản phụ khoa', 'Khám sức khỏe phụ nữ và thai kỳ');

-- ===================================================================
-- Department (phòng khám - toi thieu de he thong hoat dong)
-- ===================================================================
INSERT INTO department (department_id, created_at, updated_at, deleted, room_code, name, status, department_type, specialization_id, description, head_doctor_id) VALUES
('33333333-3333-3333-3333-333333333333', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'SUR-101', 'Phòng khám Ngoại 1', 'AVAILABLE', 'EXAMINATION', '00000003-3333-3333-3333-333333333333', 'Khám ngoại tổng quát và xử trí ban đầu', NULL),
('44444444-4444-4444-4444-444444444444', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'LAB-201', 'Phòng xét nghiệm huyết học', 'AVAILABLE', 'PARACLINICAL', NULL, 'Tiếp nhận và phân tích mẫu máu', NULL),
('55555555-5555-5555-5555-555555555555', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'IMG-301', 'Phòng siêu âm', 'AVAILABLE', 'PARACLINICAL', NULL, 'Thực hiện siêu âm chẩn đoán', NULL),
('66666666-6666-6666-6666-666666666666', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'PED-102', 'Phòng khám Nhi', 'AVAILABLE', 'EXAMINATION', '00000002-2222-2222-2222-222222222222', 'Khám và chăm sóc trẻ em', NULL),
('77777777-7777-7777-7777-777777777777', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'INT-103', 'Phòng khám Nội', 'AVAILABLE', 'EXAMINATION', '00000001-1111-1111-1111-111111111111', 'Khám nội khoa', NULL),
('88888888-8888-8888-8888-888888888888', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'DER-104', 'Phòng khám Da liễu', 'AVAILABLE', 'EXAMINATION', '00000004-4444-4444-4444-444444444444', 'Khám da liễu', NULL),
('99999999-9999-9999-9999-999999999999', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'SUR-102', 'Phòng thủ thuật Ngoại', 'AVAILABLE', 'EXAMINATION', '00000003-3333-3333-3333-333333333333', 'Khám vết thương, thay băng và chăm sóc ngoại khoa', NULL),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'INT-104', 'Phòng khám Nội 2', 'AVAILABLE', 'EXAMINATION', '00000001-1111-1111-1111-111111111111', 'Khám tim mạch cơ bản và các bệnh nội khoa', NULL),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'XR-302', 'Phòng X-quang', 'AVAILABLE', 'PARACLINICAL', NULL, 'Chụp X-quang kỹ thuật số', NULL),
('cccccccc-cccc-cccc-cccc-cccccccccccc', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'LAB-202', 'Phòng xét nghiệm sinh hóa', 'AVAILABLE', 'PARACLINICAL', NULL, 'Phân tích sinh hóa và nước tiểu', NULL),
('dddddddd-dddd-dddd-dddd-dddddddddddd', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'OBG-107', 'Phòng khám Sản phụ khoa', 'AVAILABLE', 'EXAMINATION', '00000008-8888-8888-8888-888888888888', 'Khám sức khỏe phụ nữ và thai kỳ', NULL);

-- ===================================================================
-- Account (tai khoan dang nhap) - 2 tai khoan chuẩn: admin + clinic manager
-- ===================================================================
INSERT INTO account (account_id, created_at, is_active, password_hash, role, username) VALUES
                                                                                           ('30000012-2222-2222-2222-222222222222', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'admin'),
                                                                                           ('30000013-3333-3333-3333-333333333333', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'clinicmanager');

-- ===================================================================
-- Profile (thong tin ca nhan lien ket voi account)
-- ===================================================================
INSERT INTO profile (profile_id, created_at, updated_at, deleted, full_name, date_of_birth, gender, phone, email, address, blood_type) VALUES
                                                                                                                                           ('20000009-9999-9999-9999-999999999999', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Phạm Đức Minh', '1985-03-18', 'MALE', '0903214567', 'ducminh.pham@cares.vn', 'Cầu Giấy, Hà Nội', NULL),
                                                                                                                                           ('20000010-0000-0000-0000-000000000001', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Nguyễn Thu Hương', '1988-09-24', 'FEMALE', '0912864537', 'thuhuong.nguyen@cares.vn', 'Thanh Xuân, Hà Nội', NULL);

-- ===================================================================
-- ServiceCapability (nang luc thuc hien dich vu - dung de gan voi medical_service/lab)
-- ===================================================================
INSERT INTO service_capability (capability_id, created_at, updated_at, deleted, code, name, description, active) VALUES
('ca000001-0000-0000-0000-000000000001', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'HEMATOLOGY', 'Xét nghiệm huyết học', 'Công thức máu và các xét nghiệm liên quan', true),
('ca000002-0000-0000-0000-000000000002', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'BIOCHEMISTRY', 'Xét nghiệm sinh hóa', 'Sinh hóa máu và chức năng cơ quan', true),
('ca000003-0000-0000-0000-000000000003', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'ULTRASOUND', 'Siêu âm', 'Các dịch vụ siêu âm hình ảnh', true),
('ca000004-0000-0000-0000-000000000004', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'XRAY', 'X-quang', 'Các dịch vụ X-quang', true),
('ca000005-0000-0000-0000-000000000005', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'URINALYSIS', 'Xét nghiệm nước tiểu', 'Phân tích nước tiểu thường quy', true),
('ca000006-0000-0000-0000-000000000006', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'ECG', 'Điện tim', 'Ghi và đọc điện tâm đồ', true),
('ca000007-0000-0000-0000-000000000007', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'MICROBIOLOGY', 'Xét nghiệm vi sinh', 'Soi và nuôi cấy vi sinh', true),
('ca000008-0000-0000-0000-000000000008', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'RAPID_TEST', 'Test nhanh', 'Thực hiện các kỹ thuật test nhanh bệnh truyền nhiễm', true);

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
('40000001-0000-0000-0000-000000000001', 'EX-SU-001', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Khám ban đầu các bệnh lý và tổn thương có khả năng cần can thiệp ngoại khoa.', 'ACTIVE', false, 'Khám Ngoại tổng quát', 220000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 0, 120, NULL, NULL, '00000003-3333-3333-3333-333333333333', NULL),
('40000002-0000-0000-0000-000000000002', 'EX-IN-001', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Khám và đánh giá tổng quát các bệnh lý nội khoa thường gặp ở người lớn.', 'ACTIVE', false, 'Khám Nội tổng quát', 220000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 16, 120, NULL, NULL, '00000001-1111-1111-1111-111111111111', NULL),
('40000003-0000-0000-0000-000000000003', 'EX-PE-001', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Khám tổng quát và đánh giá sức khỏe cho trẻ em.', 'ACTIVE', false, 'Khám Nhi tổng quát', 220000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 0, 15, NULL, NULL, '00000002-2222-2222-2222-222222222222', NULL),
('40000004-0000-0000-0000-000000000004', 'EX-DE-001', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Khám tổng quát các vấn đề về da, tóc và móng.', 'ACTIVE', false, 'Khám Da liễu', 230000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 0, 120, NULL, NULL, '00000004-4444-4444-4444-444444444444', NULL),
('40000005-0000-0000-0000-000000000005', 'EX-SU-003', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Kiểm tra mức độ tổn thương, nguy cơ nhiễm trùng và hướng xử trí vết thương.', 'ACTIVE', false, 'Khám vết thương', 180000, 'EXAMINATION', 25, 1, false, false, false, 0, true, 0, 120, NULL, NULL, '00000003-3333-3333-3333-333333333333', NULL),
('40000006-0000-0000-0000-000000000006', 'EX-IN-002', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Kiểm tra các triệu chứng tim mạch như đau ngực, hồi hộp, khó thở hoặc tăng huyết áp.', 'ACTIVE', false, 'Khám Tim mạch cơ bản', 280000, 'EXAMINATION', 35, 1, false, false, false, 0, true, 16, 120, NULL, NULL, '00000001-1111-1111-1111-111111111111', NULL),
('40000007-0000-0000-0000-000000000007', 'EX-OB-001', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Khám và tư vấn các vấn đề sức khỏe phụ khoa thường gặp.', 'ACTIVE', false, 'Khám Phụ khoa', 280000, 'EXAMINATION', 35, 1, false, false, false, 0, true, 16, 60, 'FEMALE', NULL, '00000008-8888-8888-8888-888888888888', NULL),
('40000008-0000-0000-0000-000000000008', 'LAB-001', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Đánh giá các thành phần tế bào máu, hỗ trợ phát hiện thiếu máu và nhiễm trùng.', 'ACTIVE', false, 'Công thức máu', 120000, 'PARACLINICAL', 15, 1, true, true, true, 45, false, 0, 120, NULL, NULL, NULL, 'ca000001-0000-0000-0000-000000000001'),
('40000009-0000-0000-0000-000000000009', 'LAB-002', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Đo nồng độ glucose trong máu, hỗ trợ sàng lọc và theo dõi đái tháo đường.', 'ACTIVE', false, 'Đường huyết', 70000, 'PARACLINICAL', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('40000010-0000-0000-0000-000000000010', 'LAB-003', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Đánh giá một số chỉ số sinh hóa quan trọng trong máu.', 'ACTIVE', false, 'Sinh hóa máu cơ bản', 190000, 'PARACLINICAL', 15, 1, true, true, true, 60, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('40000011-0000-0000-0000-000000000011', 'LAB-004', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Kiểm tra các chỉ số hỗ trợ đánh giá hoạt động và tổn thương gan.', 'ACTIVE', false, 'Chức năng gan', 140000, 'PARACLINICAL', 15, 1, true, true, true, 60, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('40000012-0000-0000-0000-000000000012', 'LAB-005', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Kiểm tra các chỉ số hỗ trợ đánh giá khả năng hoạt động của thận.', 'ACTIVE', false, 'Chức năng thận', 150000, 'PARACLINICAL', 15, 1, true, true, true, 60, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('40000013-0000-0000-0000-000000000013', 'LAB-006', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Phân tích các chỉ số nước tiểu, hỗ trợ phát hiện bệnh tiết niệu và chuyển hóa.', 'ACTIVE', false, 'Tổng phân tích nước tiểu', 90000, 'PARACLINICAL', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000005-0000-0000-0000-000000000005'),
('40000014-0000-0000-0000-000000000014', 'LAB-007', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Định lượng CRP, hỗ trợ đánh giá tình trạng viêm hoặc nhiễm trùng.', 'ACTIVE', false, 'Xét nghiệm CRP', 130000, 'PARACLINICAL', 15, 1, true, true, true, 60, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('40000015-0000-0000-0000-000000000015', 'IMG-003', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Khảo sát các cơ quan trong ổ bụng bằng phương pháp siêu âm.', 'ACTIVE', false, 'Siêu âm ổ bụng tổng quát', 250000, 'PARACLINICAL', 20, 1, true, true, false, 10, false, 0, 120, NULL, NULL, NULL, 'ca000003-0000-0000-0000-000000000003'),
('40000016-0000-0000-0000-000000000016', 'IMG-004', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Đánh giá kích thước, cấu trúc và bất thường của tuyến giáp.', 'ACTIVE', false, 'Siêu âm tuyến giáp', 220000, 'PARACLINICAL', 20, 1, true, true, false, 10, false, 0, 120, NULL, NULL, NULL, 'ca000003-0000-0000-0000-000000000003'),
('40000017-0000-0000-0000-000000000017', 'IMG-001', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chụp hình vùng ngực, hỗ trợ đánh giá phổi, tim và lồng ngực.', 'ACTIVE', false, 'X-quang ngực', 180000, 'PARACLINICAL', 15, 1, true, true, false, 15, false, 6, 120, NULL, NULL, NULL, 'ca000004-0000-0000-0000-000000000004'),
('40000018-0000-0000-0000-000000000018', 'IMG-002', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chụp hình xương khớp, hỗ trợ phát hiện tổn thương hoặc bất thường.', 'ACTIVE', false, 'X-quang xương khớp', 220000, 'PARACLINICAL', 20, 1, true, true, false, 15, false, 0, 120, NULL, NULL, NULL, 'ca000004-0000-0000-0000-000000000004'),
('40000019-0000-0000-0000-000000000019', 'IMG-006', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Ghi lại hoạt động điện của tim, hỗ trợ phát hiện rối loạn nhịp và bất thường tim mạch.', 'ACTIVE', false, 'Điện tim ECG', 150000, 'PARACLINICAL', 15, 1, true, true, false, 5, false, 16, 120, NULL, NULL, NULL, 'ca000006-0000-0000-0000-000000000006'),
('40000020-0000-0000-0000-000000000020', 'LAB-008', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Thực hiện test nhanh hỗ trợ sàng lọc một số bệnh truyền nhiễm.', 'ACTIVE', true, 'Test nhanh một số bệnh truyền nhiễm', 110000, 'PARACLINICAL', 15, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000008-0000-0000-0000-000000000008'),
('40000021-0000-0000-0000-000000000021', 'EX-IN-003', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Thăm khám các vấn đề về dạ dày, đường ruột, gan mật và rối loạn tiêu hóa.', 'ACTIVE', false, 'Khám Tiêu hóa', 230000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 16, 120, NULL, NULL, '00000001-1111-1111-1111-111111111111', NULL),
('40000022-0000-0000-0000-000000000022', 'EX-IN-004', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Đánh giá ho, khó thở, đau ngực và các bệnh lý đường hô hấp thường gặp.', 'ACTIVE', false, 'Khám Hô hấp', 230000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 16, 120, NULL, NULL, '00000001-1111-1111-1111-111111111111', NULL),
('40000023-0000-0000-0000-000000000023', 'EX-IN-005', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Khám đau khớp, đau lưng, hạn chế vận động và các vấn đề cơ xương khớp.', 'ACTIVE', false, 'Khám Cơ xương khớp', 240000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 16, 120, NULL, NULL, '00000001-1111-1111-1111-111111111111', NULL),
('40000024-0000-0000-0000-000000000024', 'EX-SU-002', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Đánh giá sưng, đau, bầm tím và tổn thương cơ, gân hoặc phần mềm.', 'ACTIVE', false, 'Khám chấn thương phần mềm', 220000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 0, 120, NULL, NULL, '00000003-3333-3333-3333-333333333333', NULL),
('40000025-0000-0000-0000-000000000025', 'EX-SU-004', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Làm sạch, thay băng và theo dõi quá trình hồi phục của vết thương.', 'ACTIVE', false, 'Thay băng, chăm sóc vết thương', 150000, 'EXAMINATION', 20, 1, false, false, false, 0, true, 0, 120, NULL, NULL, '00000003-3333-3333-3333-333333333333', NULL),
('40000026-0000-0000-0000-000000000026', 'EX-PE-002', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Khám ho, sổ mũi, khó thở và các bệnh đường hô hấp ở trẻ.', 'ACTIVE', false, 'Khám bệnh hô hấp trẻ em', 220000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 0, 15, NULL, NULL, '00000002-2222-2222-2222-222222222222', NULL),
('40000027-0000-0000-0000-000000000027', 'EX-PE-003', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Khám đau bụng, tiêu chảy, táo bón, nôn và rối loạn tiêu hóa ở trẻ.', 'ACTIVE', false, 'Khám tiêu hóa trẻ em', 220000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 0, 15, NULL, NULL, '00000002-2222-2222-2222-222222222222', NULL),
('40000028-0000-0000-0000-000000000028', 'EX-PE-004', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Đánh giá nguyên nhân sốt và các dấu hiệu nhiễm khuẩn thường gặp ở trẻ.', 'ACTIVE', false, 'Khám sốt và bệnh nhiễm khuẩn thông thường', 230000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 0, 15, NULL, NULL, '00000002-2222-2222-2222-222222222222', NULL),
('40000029-0000-0000-0000-000000000029', 'EX-OB-002', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Theo dõi tình trạng thai kỳ và sức khỏe của thai phụ.', 'ACTIVE', false, 'Khám Thai', 300000, 'EXAMINATION', 35, 1, false, false, false, 0, true, 16, 60, 'FEMALE', NULL, '00000008-8888-8888-8888-888888888888', NULL),
('40000030-0000-0000-0000-000000000030', 'EX-OB-003', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Tư vấn biện pháp tránh thai và chăm sóc sức khỏe sinh sản.', 'ACTIVE', false, 'Khám và tư vấn kế hoạch hóa gia đình', 250000, 'EXAMINATION', 35, 1, false, false, false, 0, true, 16, 60, 'FEMALE', NULL, '00000008-8888-8888-8888-888888888888', NULL),
('40000031-0000-0000-0000-000000000031', 'EX-OB-004', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Khám các dấu hiệu ngứa, đau, khí hư bất thường và viêm nhiễm phụ khoa.', 'ACTIVE', false, 'Khám viêm nhiễm phụ khoa', 280000, 'EXAMINATION', 35, 1, false, false, false, 0, true, 16, 60, 'FEMALE', NULL, '00000008-8888-8888-8888-888888888888', NULL),
('40000032-0000-0000-0000-000000000032', 'EX-DE-002', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Đánh giá mức độ mụn và tư vấn phương pháp chăm sóc, điều trị phù hợp.', 'ACTIVE', false, 'Khám mụn trứng cá', 220000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 10, 120, NULL, NULL, '00000004-4444-4444-4444-444444444444', NULL),
('40000033-0000-0000-0000-000000000033', 'EX-DE-003', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Khám ngứa, phát ban, mẩn đỏ và các biểu hiện dị ứng ngoài da.', 'ACTIVE', false, 'Khám viêm da, dị ứng', 230000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 0, 120, NULL, NULL, '00000004-4444-4444-4444-444444444444', NULL),
('40000034-0000-0000-0000-000000000034', 'EX-DE-004', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Kiểm tra các tổn thương nghi ngờ do nấm ở da, tóc hoặc móng.', 'ACTIVE', false, 'Khám nấm da', 230000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 0, 120, NULL, NULL, '00000004-4444-4444-4444-444444444444', NULL),
('40000035-0000-0000-0000-000000000035', 'IMG-005', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Khảo sát tình trạng thai và một số chỉ số phát triển của thai nhi.', 'ACTIVE', false, 'Siêu âm thai', 300000, 'PARACLINICAL', 25, 1, true, true, false, 10, false, 16, 60, 'FEMALE', NULL, NULL, 'ca000003-0000-0000-0000-000000000003');

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
                                                                                                                                                                                                                     ('90000008-1111-1111-1111-111111111111', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '20000009-9999-9999-9999-999999999999', 'STF-ADM-001', 'ADMIN', '9123456789', NULL, NULL, NULL, NULL, NULL, NULL),
('90000009-2222-2222-2222-222222222222', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '20000010-0000-0000-0000-000000000001', 'STF-CLM-001', 'CLINIC_MANAGER', '9223456789', NULL, NULL, NULL, NULL, NULL, NULL);

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
                                                                                           ('30000014-4444-4444-4444-444444444444', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'doctor1'),
                                                                                           ('30000015-5555-5555-5555-555555555555', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'nurse1'),
                                                                                           ('30000016-6666-6666-6666-666666666666', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'receptionist1'),
                                                                                           ('30000017-7777-7777-7777-777777777777', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'cashier1');

-- ===================================================================
-- Profile (Thêm profile cho các account trên)
-- ===================================================================
INSERT INTO profile (profile_id, account_id, created_at, updated_at, deleted, full_name, date_of_birth, gender, phone, email, address, blood_type) VALUES
                                                                                                                                                       ('20000011-1111-1111-1111-111111111111', '30000014-4444-4444-4444-444444444444', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Nguyễn Hoàng Minh', '1980-05-10', 'MALE', '0913517624', 'hoangminh.nguyen@cares.vn', 'Ba Đình, Hà Nội', NULL),
                                                                                                                                                       ('20000012-2222-2222-2222-222222222222', '30000015-5555-5555-5555-555555555555', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Trần Ngọc Hân', '1990-08-20', 'FEMALE', '0924186357', 'ngochan.tran@cares.vn', 'Đống Đa, Hà Nội', NULL),
                                                                                                                                                       ('20000013-3333-3333-3333-333333333333', '30000016-6666-6666-6666-666666666666', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Lê Quốc Bảo', '1995-12-01', 'MALE', '0935271468', 'quocbao.le@cares.vn', 'Hai Bà Trưng, Hà Nội', NULL),
                                                                                                                                                       ('20000014-4444-4444-4444-444444444444', '30000017-7777-7777-7777-777777777777', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Phạm Thùy Dương', '1992-03-15', 'FEMALE', '0946382517', 'thuyduong.pham@cares.vn', 'Hoàng Mai, Hà Nội', NULL);

-- ===================================================================
-- StaffInfo (Thêm staff_info cho các profile trên)
-- ===================================================================
INSERT INTO staff_info (staff_id, created_at, updated_at, deleted, profile_id, staff_code, system_role, national_id, bank_account, highest_degree, university, license_number, specialization_id, department_id) VALUES
                                                                                                                                                                                                                     ('90000010-3333-3333-3333-333333333333', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '20000011-1111-1111-1111-111111111111', 'STF-DOC-001', 'DOCTOR', '001080123456', NULL, 'Bác sĩ chuyên khoa', 'Đại học Y Hà Nội', 'CCHN-12345', '00000003-3333-3333-3333-333333333333', '33333333-3333-3333-3333-333333333333'),
                                                                                                                                                                                                                     ('90000011-4444-4444-4444-444444444444', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '20000012-2222-2222-2222-222222222222', 'STF-NUR-001', 'NURSE', '001090123456', NULL, 'Cử nhân điều dưỡng', 'Đại học Y Dược', 'CCHN-23456', NULL, '44444444-4444-4444-4444-444444444444'),
                                                                                                                                                                                                                     ('90000012-5555-5555-5555-555555555555', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '20000013-3333-3333-3333-333333333333', 'STF-REC-001', 'RECEPTIONIST', '001095123456', NULL, NULL, NULL, NULL, NULL, NULL),
                                                                                                                                                                                                                     ('90000013-6666-6666-6666-666666666666', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '20000014-4444-4444-4444-444444444444', 'STF-CAS-001', 'CASHIER', '001092123456', NULL, NULL, NULL, NULL, NULL, NULL);

-- Bo sung nhan su nen cho cac phong kham, CLS va vai tro van hanh.
-- Tat ca tai khoan trinh dien dung mat khau: 88888888.
INSERT INTO account (account_id, created_at, is_active, password_hash, role, username)
SELECT format('31000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF',
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
('33000000-0000-0000-0000-000000000002', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'doctor_lab'),
('33000000-0000-0000-0000-000000000003', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'doctor_biochem'),
('33000000-0000-0000-0000-000000000004', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'doctor_xray');

INSERT INTO profile (
    profile_id, account_id, created_at, updated_at, deleted,
    full_name, date_of_birth, gender, phone, email, address, blood_type
) VALUES
('23000000-0000-0000-0000-000000000002', '33000000-0000-0000-0000-000000000002', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Bác sĩ Nguyễn Hải Yến', '1986-07-22', 'FEMALE', '0968000002', 'doctor.lab@cares.vn', 'Hà Nội', NULL),
('23000000-0000-0000-0000-000000000003', '33000000-0000-0000-0000-000000000003', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Bác sĩ Trần Minh Sinh', '1985-09-12', 'MALE', '0968000003', 'doctor.biochem@cares.vn', 'Hà Nội', NULL),
('23000000-0000-0000-0000-000000000004', '33000000-0000-0000-0000-000000000004', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Bác sĩ Lê Thu Phương', '1987-01-18', 'FEMALE', '0968000004', 'doctor.xray@cares.vn', 'Hà Nội', NULL);

INSERT INTO staff_info (
    staff_id, created_at, updated_at, deleted, profile_id, staff_code,
    system_role, national_id, bank_account, highest_degree, university,
    license_number, specialization_id, department_id
) VALUES
('93000000-0000-0000-0000-000000000002', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '23000000-0000-0000-0000-000000000002', 'STF-DOC-LAB', 'DOCTOR', '001086000002', NULL, 'Bác sĩ chuyên khoa xét nghiệm', 'Đại học Y Hà Nội', 'CCHN-LAB-001', NULL, '44444444-4444-4444-4444-444444444444'),
('93000000-0000-0000-0000-000000000003', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '23000000-0000-0000-0000-000000000003', 'STF-DOC-BIO', 'DOCTOR', '001085000003', NULL, 'Bác sĩ chuyên khoa xét nghiệm', 'Đại học Y Hà Nội', 'CCHN-BIO-001', NULL, 'cccccccc-cccc-cccc-cccc-cccccccccccc'),
('93000000-0000-0000-0000-000000000004', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '23000000-0000-0000-0000-000000000004', 'STF-DOC-XRAY', 'DOCTOR', '001087000004', NULL, 'Bác sĩ chẩn đoán hình ảnh', 'Đại học Y Hà Nội', 'CCHN-XRAY-001', NULL, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb');

-- Ky thuat vien xet nghiem ca toi. Le tan 3, thu ngan 3 va dieu duong 3
-- da co trong nhom tai khoan sinh tu dong ben duoi.
INSERT INTO account (account_id, created_at, is_active, password_hash, role, username) VALUES
('34000000-0000-0000-0000-000000000002', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'lab_evening');

INSERT INTO profile (
    profile_id, account_id, created_at, updated_at, deleted,
    full_name, date_of_birth, gender, phone, email, address, blood_type
) VALUES
('24000000-0000-0000-0000-000000000002', '34000000-0000-0000-0000-000000000002', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false,
 'Kỹ thuật viên Phạm Ngọc Diệp', DATE '1993-03-18', 'FEMALE', '0827364159', 'ngocdiep.pham@cares.vn', 'Nam Từ Liêm, Hà Nội', NULL);

INSERT INTO staff_info (
    staff_id, created_at, updated_at, deleted, profile_id, staff_code,
    system_role, national_id, bank_account, highest_degree, university,
    license_number, specialization_id, department_id
) VALUES
('94000000-0000-0000-0000-000000000002', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '24000000-0000-0000-0000-000000000002',
 'STF-LAB-EVE', 'NURSE', '001093527184', NULL, 'Cử nhân kỹ thuật xét nghiệm y học', 'Đại học Y Hà Nội',
 NULL, NULL, '44444444-4444-4444-4444-444444444444');

INSERT INTO profile (
    profile_id, account_id, created_at, updated_at, deleted,
    full_name, date_of_birth, gender, phone, email, address, blood_type
)
SELECT format('21000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       format('31000000-0000-0000-0000-%s', lpad(i::text, 12, '0'))::uuid,
       (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false,
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
       (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false,
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
('35000000-0000-0000-0000-000000000007', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'technician_biochem_evening'),
('35000000-0000-0000-0000-000000000008', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'technician_ultrasound_evening'),
('35000000-0000-0000-0000-000000000009', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'technician_xray_pm'),
('35000000-0000-0000-0000-000000000010', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'technician_xray_evening');

INSERT INTO profile (
    profile_id, account_id, created_at, updated_at, deleted,
    full_name, date_of_birth, gender, phone, email, address, blood_type
) VALUES
('25000000-0000-0000-0000-000000000007','35000000-0000-0000-0000-000000000007',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'Kỹ thuật viên Bùi Đức Anh',DATE '1991-01-15','MALE','0837000007','duc.anh@cares.vn','Hà Nội',NULL),
('25000000-0000-0000-0000-000000000008','35000000-0000-0000-0000-000000000008',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'Kỹ thuật viên Nguyễn Thảo Vy',DATE '1992-04-08','FEMALE','0837000008','thao.vy@cares.vn','Hà Nội',NULL),
('25000000-0000-0000-0000-000000000009','35000000-0000-0000-0000-000000000009',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'Kỹ thuật viên Trần Hoàng Long',DATE '1990-09-23','MALE','0837000009','hoang.long@cares.vn','Hà Nội',NULL),
('25000000-0000-0000-0000-000000000010','35000000-0000-0000-0000-000000000010',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'Kỹ thuật viên Phạm Mai Chi',DATE '1993-12-02','FEMALE','0837000010','mai.chi@cares.vn','Hà Nội',NULL);

INSERT INTO staff_info (
    staff_id, created_at, updated_at, deleted, profile_id, staff_code,
    system_role, national_id, bank_account, highest_degree, university,
    license_number, specialization_id, department_id
) VALUES
('95000000-0000-0000-0000-000000000007',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'25000000-0000-0000-0000-000000000007','STF-TEC-BIO-EVE','NURSE','001091700007',NULL,'Cử nhân kỹ thuật xét nghiệm','Đại học Y Hà Nội',NULL,NULL,'cccccccc-cccc-cccc-cccc-cccccccccccc'),
('95000000-0000-0000-0000-000000000008',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'25000000-0000-0000-0000-000000000008','STF-TEC-US-EVE','NURSE','001092700008',NULL,'Cử nhân kỹ thuật hình ảnh','Đại học Y Hà Nội',NULL,NULL,'55555555-5555-5555-5555-555555555555'),
('95000000-0000-0000-0000-000000000009',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'25000000-0000-0000-0000-000000000009','STF-TEC-XR-PM','NURSE','001090700009',NULL,'Cử nhân kỹ thuật hình ảnh','Đại học Y Hà Nội',NULL,NULL,'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
('95000000-0000-0000-0000-000000000010',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'25000000-0000-0000-0000-000000000010','STF-TEC-XR-EVE','NURSE','001093700010',NULL,'Cử nhân kỹ thuật hình ảnh','Đại học Y Hà Nội',NULL,NULL,'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb');

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
('a1000000-0000-0000-0000-000000000001', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '90000011-4444-4444-4444-444444444444', 'ca000001-0000-0000-0000-000000000001', NULL, NULL, NULL, NULL, 'ACTIVE'),
('a1000000-0000-0000-0000-000000000002', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '91000000-0000-0000-0000-000000000008', 'ca000001-0000-0000-0000-000000000001', NULL, NULL, NULL, NULL, 'ACTIVE'),
('a1000000-0000-0000-0000-000000000003', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '91000000-0000-0000-0000-000000000008', 'ca000007-0000-0000-0000-000000000007', NULL, NULL, NULL, NULL, 'ACTIVE'),
('a1000000-0000-0000-0000-000000000004', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '91000000-0000-0000-0000-000000000009', 'ca000002-0000-0000-0000-000000000002', NULL, NULL, NULL, NULL, 'ACTIVE'),
('a1000000-0000-0000-0000-000000000005', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '91000000-0000-0000-0000-000000000009', 'ca000005-0000-0000-0000-000000000005', NULL, NULL, NULL, NULL, 'ACTIVE'),
('a1000000-0000-0000-0000-000000000006', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '91000000-0000-0000-0000-000000000010', 'ca000003-0000-0000-0000-000000000003', NULL, NULL, NULL, NULL, 'ACTIVE'),
('a1000000-0000-0000-0000-000000000007', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '91000000-0000-0000-0000-000000000007', 'ca000003-0000-0000-0000-000000000003', NULL, NULL, NULL, NULL, 'ACTIVE'),
('a1000000-0000-0000-0000-000000000008', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '91000000-0000-0000-0000-000000000005', 'ca000006-0000-0000-0000-000000000006', NULL, NULL, NULL, NULL, 'ACTIVE'),
('a1000000-0000-0000-0000-000000000009', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '91000000-0000-0000-0000-000000000008', 'ca000008-0000-0000-0000-000000000008', NULL, NULL, NULL, NULL, 'ACTIVE');

INSERT INTO staff_capability (
    staff_capability_id, created_at, updated_at, deleted, staff_id,
    capability_id, certificate_number, issued_date, expiry_date,
    issuing_organization, status
) VALUES
('a2000000-0000-0000-0000-000000000001', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '93000000-0000-0000-0000-000000000002', 'ca000001-0000-0000-0000-000000000001', NULL, NULL, NULL, NULL, 'ACTIVE'),
('a2000000-0000-0000-0000-000000000002', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '93000000-0000-0000-0000-000000000002', 'ca000002-0000-0000-0000-000000000002', NULL, NULL, NULL, NULL, 'ACTIVE'),
('a2000000-0000-0000-0000-000000000003', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '93000000-0000-0000-0000-000000000002', 'ca000005-0000-0000-0000-000000000005', NULL, NULL, NULL, NULL, 'ACTIVE'),
('a2000000-0000-0000-0000-000000000004', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '93000000-0000-0000-0000-000000000003', 'ca000002-0000-0000-0000-000000000002', NULL, NULL, NULL, NULL, 'ACTIVE'),
('a2000000-0000-0000-0000-000000000005', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '93000000-0000-0000-0000-000000000004', 'ca000004-0000-0000-0000-000000000004', NULL, NULL, NULL, NULL, 'ACTIVE');

-- Bo sung nang luc de cac ca can lam sang co nhan su du dieu kien.
INSERT INTO staff_capability (
    staff_capability_id, created_at, updated_at, deleted, staff_id,
    capability_id, certificate_number, issued_date, expiry_date,
    issuing_organization, status
) VALUES
('a2000000-0000-0000-0000-000000000006', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '93000000-0000-0000-0000-000000000002', 'ca000008-0000-0000-0000-000000000008', 'NL-TN-2024-006', pg_temp.demo_date()-365, pg_temp.demo_date()+730, 'Sở Y tế Hà Nội', 'ACTIVE'),
('a2000000-0000-0000-0000-000000000007', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '91000000-0000-0000-0000-000000000010', 'ca000006-0000-0000-0000-000000000006', 'NL-ECG-2024-007', pg_temp.demo_date()-365, pg_temp.demo_date()+730, 'Sở Y tế Hà Nội', 'ACTIVE'),
('a2000000-0000-0000-0000-000000000008', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '94000000-0000-0000-0000-000000000002', 'ca000001-0000-0000-0000-000000000001', 'NL-HH-2025-008', pg_temp.demo_date()-365, pg_temp.demo_date()+730, 'Sở Y tế Hà Nội', 'ACTIVE'),
('a2000000-0000-0000-0000-000000000009', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '94000000-0000-0000-0000-000000000002', 'ca000008-0000-0000-0000-000000000008', 'NL-TN-2025-009', pg_temp.demo_date()-365, pg_temp.demo_date()+730, 'Sở Y tế Hà Nội', 'ACTIVE');

-- Bo sung nang luc con thieu de tung phong CLS co nguoi phu trach hop le
-- trong ca sang, chieu va toi.
INSERT INTO staff_capability (
    staff_capability_id, created_at, updated_at, deleted, staff_id,
    capability_id, certificate_number, issued_date, expiry_date,
    issuing_organization, status
) VALUES
('a3000000-0000-0000-0000-000000000001',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'93000000-0000-0000-0000-000000000002','ca000007-0000-0000-0000-000000000007',NULL,NULL,NULL,NULL,'ACTIVE'),
('a3000000-0000-0000-0000-000000000002',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'94000000-0000-0000-0000-000000000002','ca000007-0000-0000-0000-000000000007',NULL,NULL,NULL,NULL,'ACTIVE'),
('a3000000-0000-0000-0000-000000000003',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'93000000-0000-0000-0000-000000000003','ca000005-0000-0000-0000-000000000005',NULL,NULL,NULL,NULL,'ACTIVE'),
('a3000000-0000-0000-0000-000000000004',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'91000000-0000-0000-0000-000000000007','ca000006-0000-0000-0000-000000000006',NULL,NULL,NULL,NULL,'ACTIVE'),
('a3000000-0000-0000-0000-000000000005',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'95000000-0000-0000-0000-000000000007','ca000002-0000-0000-0000-000000000002',NULL,NULL,NULL,NULL,'ACTIVE'),
('a3000000-0000-0000-0000-000000000006',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'95000000-0000-0000-0000-000000000007','ca000005-0000-0000-0000-000000000005',NULL,NULL,NULL,NULL,'ACTIVE'),
('a3000000-0000-0000-0000-000000000007',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'95000000-0000-0000-0000-000000000008','ca000003-0000-0000-0000-000000000003',NULL,NULL,NULL,NULL,'ACTIVE'),
('a3000000-0000-0000-0000-000000000008',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'95000000-0000-0000-0000-000000000008','ca000006-0000-0000-0000-000000000006',NULL,NULL,NULL,NULL,'ACTIVE'),
('a3000000-0000-0000-0000-000000000009',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'95000000-0000-0000-0000-000000000009','ca000004-0000-0000-0000-000000000004',NULL,NULL,NULL,NULL,'ACTIVE'),
('a3000000-0000-0000-0000-000000000010',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'95000000-0000-0000-0000-000000000010','ca000004-0000-0000-0000-000000000004',NULL,NULL,NULL,NULL,'ACTIVE');

UPDATE staff_capability
SET certificate_number = COALESCE(certificate_number, 'NL-' || upper(substr(replace(staff_capability_id::text, '-', ''), 1, 12))),
    issued_date = COALESCE(issued_date, pg_temp.demo_date()-365),
    expiry_date = COALESCE(expiry_date, pg_temp.demo_date()+730),
    issuing_organization = COALESCE(issuing_organization, 'Sở Y tế Hà Nội'),
    updated_at = (pg_temp.demo_now()-interval '60 days');

-- ===================================================================
-- Shift Config
-- ===================================================================
INSERT INTO shift_config (shift_id, created_at, updated_at, deleted, name, start_time, end_time, is_active) VALUES
                                                                                                                ('70000001-1111-1111-1111-111111111111', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Ca Sáng', '00:00', '08:00', true),
                                                                                                                ('70000002-2222-2222-2222-222222222222', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Ca Chiều', '08:00', '16:00', true),
                                                                                                                ('70000003-3333-3333-3333-333333333333', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Ca Tối', '16:00', '23:59:59', true);

-- Phien ban gio ca ban dau. Moi lich hen va lich nhan vien moi deu tham chieu
-- phien ban nay, trong khi shift_name/shift_time van duoc giu lam snapshot.
INSERT INTO shift_version (
    shift_version_id, shift_id, start_time, end_time, effective_from, effective_to,
    change_reason, created_by, created_at, updated_at, deleted
) VALUES
('71000001-1111-1111-1111-111111111111', '70000001-1111-1111-1111-111111111111', '00:00', '08:00', pg_temp.demo_date() - 365, NULL,
 'Khởi tạo giờ làm việc ca sáng', '30000013-3333-3333-3333-333333333333', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false),
('71000002-2222-2222-2222-222222222222', '70000002-2222-2222-2222-222222222222', '08:00', '16:00', pg_temp.demo_date() - 365, NULL,
 'Khởi tạo giờ làm việc ca chiều', '30000013-3333-3333-3333-333333333333', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false),
('71000003-3333-3333-3333-333333333333', '70000003-3333-3333-3333-333333333333', '16:00', '23:59:59', pg_temp.demo_date() - 365, NULL,
 'Khởi tạo giờ làm việc ca tối', '30000013-3333-3333-3333-333333333333', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false);

-- Ngoai le gio dac biet van giu ca hoat dong; bo du lieu nghi ca/nghi ngay
-- de 14 ngay trinh dien bao phu du ba ca va ca Chu nhat.

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
       gen_random_uuid(), (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, r.staff_id, w.day_name,
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
        (pg_temp.demo_date() - 30),
        pg_temp.demo_date() + 14,
        INTERVAL '1 day'
    ) d
)
INSERT INTO staff_schedule (
    schedule_id, created_at, updated_at, deleted, is_custom, note,
    status, work_date, shift_id, shift_version_id,
    actual_start_time, actual_end_time, staff_id, template_id
)
SELECT gen_random_uuid(), (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, false,
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
        (pg_temp.demo_date() - 30),
        pg_temp.demo_date() + 14,
        INTERVAL '1 day'
    ) d
)
INSERT INTO staff_schedule (
    schedule_id, created_at, updated_at, deleted, is_custom, note,
    status, work_date, shift_id, shift_version_id,
    actual_start_time, actual_end_time, staff_id, template_id
)
SELECT gen_random_uuid(), (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, true,
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

-- Lịch trùng sẽ bị phát hiện bởi assertion, không tự che bằng soft-delete.

-- Hai tài khoản quầy chính dùng khi trình diễn được phủ đủ ba ca để việc kiểm
-- thử tiếp nhận và thanh toán không bị gián đoạn khi đổi thời điểm trong ngày.
-- Các tài khoản quầy còn lại vẫn giữ đúng ca riêng đã phân công ở phía trên.
WITH primary_counter_staff(staff_id) AS (
    VALUES
    ('90000012-5555-5555-5555-555555555555'::uuid), -- receptionist1
    ('90000013-6666-6666-6666-666666666666'::uuid)  -- cashier1
), extra_shifts(shift_id) AS (
    VALUES
    ('70000002-2222-2222-2222-222222222222'::uuid),
    ('70000003-3333-3333-3333-333333333333'::uuid)
), calendar AS (
    SELECT d::date AS work_date
    FROM generate_series(pg_temp.demo_date() - 30, pg_temp.demo_date() + 14, interval '1 day') d
)
INSERT INTO staff_schedule (
    schedule_id, created_at, updated_at, deleted, is_custom, note,
    status, work_date, shift_id, shift_version_id,
    actual_start_time, actual_end_time, staff_id, template_id
)
SELECT gen_random_uuid(), pg_temp.demo_now()-interval '60 days', pg_temp.demo_now()-interval '60 days',
       false, true, 'Lịch ba ca dành cho tài khoản quầy kiểm thử', 'SCHEDULED', c.work_date,
       s.shift_id, v.shift_version_id, v.start_time, v.end_time, p.staff_id, NULL
FROM primary_counter_staff p
CROSS JOIN extra_shifts s
CROSS JOIN calendar c
JOIN shift_version v ON v.shift_id=s.shift_id
    AND v.effective_from<=c.work_date
    AND (v.effective_to IS NULL OR v.effective_to>=c.work_date)
WHERE NOT EXISTS (
    SELECT 1 FROM staff_schedule existing
    WHERE existing.staff_id=p.staff_id AND existing.work_date=c.work_date
      AND existing.shift_id=s.shift_id AND existing.deleted=false
);

-- Kiem tra du lieu trinh dien: moi dich vu ACTIVE phai co it nhat mot nhan su
-- dung phong, dung chuyen khoa/nang luc trong tung ca cua 14 ngay mau.
DO $coverage_check$
DECLARE
    missing_coverage text;
BEGIN
    WITH calendar AS (
        SELECT d::date AS work_date
        FROM generate_series(
            (pg_temp.demo_date() - 30),
            pg_temp.demo_date() + 14,
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
            (pg_temp.demo_date() - 30),
            pg_temp.demo_date() + 14,
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
         AND schedule.work_date BETWEEN (pg_temp.demo_date() - 30)
                                    AND pg_temp.demo_date() + 14
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
            (pg_temp.demo_date() - 30),
            pg_temp.demo_date() + 14,
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
          AND si.staff_id NOT IN (
              '90000012-5555-5555-5555-555555555555'::uuid,
              '90000013-6666-6666-6666-666666666666'::uuid
          )
          AND ss.work_date BETWEEN (pg_temp.demo_date() - 30)
                               AND pg_temp.demo_date() + 14
        GROUP BY ss.staff_id, si.staff_code, ss.work_date
        HAVING COUNT(DISTINCT ss.shift_id) > 1
    ) duplicate_staff_days;

    IF duplicate_daily_shifts IS NOT NULL THEN
        RAISE EXCEPTION 'Nhân sự vận hành/CLS bị xếp nhiều ca trong cùng ngày: %', duplicate_daily_shifts;
    END IF;
END
$room_coverage_check$;

INSERT INTO insurance (insurance_id, created_at, updated_at, deleted, code, name, description) VALUES
('62000000-0000-0000-0000-000000000001', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'BHYT', 'Bảo hiểm y tế', 'Quyền lợi BHYT theo kết quả xác minh'),
('62000000-0000-0000-0000-000000000002', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'BAOVIET', 'Bảo Việt', 'Bảo hiểm sức khỏe tư nhân'),
('62000000-0000-0000-0000-000000000003', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'PVI', 'PVI Care', 'Bảo hiểm sức khỏe doanh nghiệp');

INSERT INTO insurance_rule (rule_id, created_at, updated_at, deleted, department_type, discount_percent, insurance_id) VALUES
('62100000-0000-0000-0000-000000000001', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'EXAMINATION', 20, '62000000-0000-0000-0000-000000000001'),
('62100000-0000-0000-0000-000000000002', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'PARACLINICAL', 20, '62000000-0000-0000-0000-000000000001'),
('62100000-0000-0000-0000-000000000003', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'EXAMINATION', 15, '62000000-0000-0000-0000-000000000002'),
('62100000-0000-0000-0000-000000000004', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'PARACLINICAL', 10, '62000000-0000-0000-0000-000000000002'),
('62100000-0000-0000-0000-000000000005', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'EXAMINATION', 10, '62000000-0000-0000-0000-000000000003'),
('62100000-0000-0000-0000-000000000006', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'PARACLINICAL', 10, '62000000-0000-0000-0000-000000000003');

INSERT INTO clinical_form_template
(template_id, created_at, updated_at, deleted, code, name, context, description, active)
VALUES
('cf000006-0000-0000-0000-000000000006',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'LAB_CBC','Kết quả Công thức máu','LAB_RESULT','RBC, HGB, HCT, WBC và PLT',true),
('cf000007-0000-0000-0000-000000000007',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'LAB_GLUCOSE','Kết quả Đường huyết','LAB_RESULT','Glucose máu',true),
('cf000008-0000-0000-0000-000000000008',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'LAB_BIOCHEM','Kết quả Sinh hóa máu','LAB_RESULT','Các chỉ số sinh hóa máu cơ bản',true),
('cf000009-0000-0000-0000-000000000009',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'LAB_LIVER','Kết quả Chức năng gan','LAB_RESULT','AST, ALT, GGT, Bilirubin và Albumin',true),
('cf00000a-0000-0000-0000-00000000000a',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'LAB_KIDNEY','Kết quả Chức năng thận','LAB_RESULT','Ure, Creatinine và eGFR ước tính',true),
('cf00000b-0000-0000-0000-00000000000b',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'LAB_URINALYSIS','Kết quả Nước tiểu','LAB_RESULT','LEU, PRO, pH, BLD và GLU',true),
('cf00000c-0000-0000-0000-00000000000c',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'LAB_CRP','Kết quả CRP','LAB_RESULT','CRP định lượng',true),
('cf00000d-0000-0000-0000-00000000000d',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'LAB_RAPID_INFECTIOUS','Kết quả Test nhanh','LAB_RESULT','Các test nhanh bệnh truyền nhiễm',true),
('cf00000e-0000-0000-0000-00000000000e',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'IMG_XRAY','Kết quả X-quang','IMAGING_RESULT','Mô tả tổn thương và kết luận X-quang',true),
('cf00000f-0000-0000-0000-00000000000f',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'IMG_ABDOMINAL_US','Siêu âm ổ bụng','IMAGING_RESULT','Mô tả các tạng và kết luận',true),
('cf000010-0000-0000-0000-000000000010',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'IMG_THYROID_US','Siêu âm tuyến giáp','IMAGING_RESULT','Nhân giáp và TI-RADS',true),
('cf000011-0000-0000-0000-000000000011',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'IMG_OBSTETRIC_US','Siêu âm thai','IMAGING_RESULT','Sinh trắc thai, GA và EFW ước tính',true),
('cf000012-0000-0000-0000-000000000012',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'ECG_STANDARD','Kết quả Điện tim','ECG_RESULT','Tần số tim, loại nhịp và kết luận',true)
ON CONFLICT (code) DO NOTHING;

INSERT INTO clinical_form_template_version
(version_id, created_at, updated_at, deleted, template_id, version_no, schema_json, status,
 change_reason, effective_from, created_by, published_by, published_at)
VALUES
('cf100006-0000-0000-0000-000000000006',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'cf000006-0000-0000-0000-000000000006',1,$j${"fields":[{"key":"rbc","label":"RBC","type":"NUMBER","unit":"10^12/L","group":"Công thức máu","displayOrder":1,"required":true},{"key":"hgb","label":"HGB","type":"NUMBER","unit":"g/L","group":"Công thức máu","displayOrder":2,"required":true},{"key":"hct","label":"HCT","type":"NUMBER","unit":"%","group":"Công thức máu","displayOrder":3,"required":true},{"key":"wbc","label":"WBC","type":"NUMBER","unit":"10^9/L","group":"Công thức máu","displayOrder":4,"required":true},{"key":"plt","label":"PLT","type":"NUMBER","unit":"10^9/L","group":"Công thức máu","displayOrder":5,"required":true}]}$j$::jsonb,'PUBLISHED','Khoảng tham chiếu do quản lý phòng xét nghiệm cấu hình',pg_temp.demo_date() - 60,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',(pg_temp.demo_now()-interval '60 days')),
('cf100007-0000-0000-0000-000000000007',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'cf000007-0000-0000-0000-000000000007',1,$j${"fields":[{"key":"glucose","label":"Glucose","type":"NUMBER","unit":"mmol/L","group":"Đường huyết","displayOrder":1,"required":true}]}$j$::jsonb,'PUBLISHED','Khởi tạo dữ liệu mẫu chưa có ngưỡng vận hành',pg_temp.demo_date() - 60,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',(pg_temp.demo_now()-interval '60 days')),
('cf100008-0000-0000-0000-000000000008',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'cf000008-0000-0000-0000-000000000008',1,$j${"fields":[{"key":"glucose","label":"Glucose","type":"NUMBER","unit":"mmol/L","group":"Sinh hóa","displayOrder":1},{"key":"hba1c","label":"HbA1c","type":"NUMBER","unit":"%","group":"Sinh hóa","displayOrder":2},{"key":"cholesterol","label":"Cholesterol","type":"NUMBER","unit":"mmol/L","group":"Mỡ máu","displayOrder":3},{"key":"triglyceride","label":"Triglyceride","type":"NUMBER","unit":"mmol/L","group":"Mỡ máu","displayOrder":4},{"key":"hdlC","label":"HDL-C","type":"NUMBER","unit":"mmol/L","group":"Mỡ máu","displayOrder":5},{"key":"ldlC","label":"LDL-C","type":"NUMBER","unit":"mmol/L","group":"Mỡ máu","displayOrder":6},{"key":"acidUric","label":"Acid Uric","type":"NUMBER","unit":"umol/L","group":"Sinh hóa","displayOrder":7}]}$j$::jsonb,'PUBLISHED','Khởi tạo dữ liệu mẫu chưa có ngưỡng vận hành',pg_temp.demo_date() - 60,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',(pg_temp.demo_now()-interval '60 days')),
('cf100009-0000-0000-0000-000000000009',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'cf000009-0000-0000-0000-000000000009',1,$j${"fields":[{"key":"ast","label":"AST","type":"NUMBER","unit":"U/L","group":"Chức năng gan","displayOrder":1},{"key":"alt","label":"ALT","type":"NUMBER","unit":"U/L","group":"Chức năng gan","displayOrder":2},{"key":"ggt","label":"GGT","type":"NUMBER","unit":"U/L","group":"Chức năng gan","displayOrder":3},{"key":"bilirubinTotal","label":"Bilirubin Total","type":"NUMBER","unit":"umol/L","group":"Chức năng gan","displayOrder":4},{"key":"albumin","label":"Albumin","type":"NUMBER","unit":"g/L","group":"Chức năng gan","displayOrder":5}]}$j$::jsonb,'PUBLISHED','Khởi tạo dữ liệu mẫu chưa có ngưỡng vận hành',pg_temp.demo_date() - 60,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',(pg_temp.demo_now()-interval '60 days')),
('cf10000a-0000-0000-0000-00000000000a',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'cf00000a-0000-0000-0000-00000000000a',1,$j${"fields":[{"key":"ure","label":"Ure","type":"NUMBER","unit":"mmol/L","group":"Chức năng thận","displayOrder":1},{"key":"creatinine","label":"Creatinine","type":"NUMBER","unit":"umol/L","group":"Chức năng thận","displayOrder":2,"required":true,"min":1},{"key":"egfr","label":"eGFR ước tính","type":"NUMBER","unit":"mL/min/1.73m2","group":"Chức năng thận","displayOrder":3,"precision":2,"calculatorKey":"EGFR_CKD_EPI_2021_V1"}]}$j$::jsonb,'PUBLISHED','Khởi tạo công thức eGFR CKD-EPI 2021',pg_temp.demo_date() - 60,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',(pg_temp.demo_now()-interval '60 days')),
('cf10000b-0000-0000-0000-00000000000b',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'cf00000b-0000-0000-0000-00000000000b',1,$j${"fields":[{"key":"leu","label":"LEU","type":"TEXT","group":"Nước tiểu","displayOrder":1},{"key":"pro","label":"PRO","type":"TEXT","group":"Nước tiểu","displayOrder":2},{"key":"ph","label":"pH","type":"NUMBER","group":"Nước tiểu","displayOrder":3},{"key":"bld","label":"BLD","type":"TEXT","group":"Nước tiểu","displayOrder":4},{"key":"glu","label":"GLU","type":"TEXT","group":"Nước tiểu","displayOrder":5}]}$j$::jsonb,'PUBLISHED','Khởi tạo dữ liệu mẫu chưa có ngưỡng vận hành',pg_temp.demo_date() - 60,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',(pg_temp.demo_now()-interval '60 days')),
('cf10000c-0000-0000-0000-00000000000c',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'cf00000c-0000-0000-0000-00000000000c',1,$j${"fields":[{"key":"crp","label":"CRP","type":"NUMBER","unit":"mg/L","group":"Viêm","displayOrder":1,"required":true}]}$j$::jsonb,'PUBLISHED','Khởi tạo dữ liệu mẫu chưa có ngưỡng vận hành',pg_temp.demo_date() - 60,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',(pg_temp.demo_now()-interval '60 days')),
('cf10000d-0000-0000-0000-00000000000d',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'cf00000d-0000-0000-0000-00000000000d',1,$j${"fields":[{"key":"hbsAg","label":"HBsAg","type":"SELECT","group":"Test nhanh","displayOrder":1,"options":["NEGATIVE","POSITIVE","INDETERMINATE"]},{"key":"antiHcv","label":"Anti-HCV","type":"SELECT","group":"Test nhanh","displayOrder":2,"options":["NEGATIVE","POSITIVE","INDETERMINATE"]},{"key":"antiHiv","label":"Anti-HIV","type":"SELECT","group":"Test nhanh","displayOrder":3,"options":["NEGATIVE","POSITIVE","INDETERMINATE"]},{"key":"dengueNs1","label":"Dengue NS1","type":"SELECT","group":"Test nhanh","displayOrder":4,"options":["NEGATIVE","POSITIVE","INDETERMINATE"]},{"key":"influenzaAb","label":"Cúm A/B","type":"SELECT","group":"Test nhanh","displayOrder":5,"options":["NEGATIVE","POSITIVE","INDETERMINATE"]}]}$j$::jsonb,'PUBLISHED','Khởi tạo dữ liệu test nhanh',pg_temp.demo_date() - 60,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',(pg_temp.demo_now()-interval '60 days')),
('cf10000e-0000-0000-0000-00000000000e',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'cf00000e-0000-0000-0000-00000000000e',1,$j${"fields":[{"key":"lesionDescription","label":"Mô tả tổn thương","type":"TEXTAREA","group":"X-quang","displayOrder":1,"normalValue":"Không thấy tổn thương bất thường rõ trên phim."},{"key":"lesionLocation","label":"Vị trí tổn thương","type":"TEXT","group":"X-quang","displayOrder":2},{"key":"imagingConclusion","label":"Kết luận hình ảnh","type":"TEXTAREA","group":"Kết luận","displayOrder":3,"required":true,"normalValue":"Chưa ghi nhận bất thường."}]}$j$::jsonb,'PUBLISHED','Khởi tạo mẫu mô tả X-quang',pg_temp.demo_date() - 60,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',(pg_temp.demo_now()-interval '60 days')),
('cf10000f-0000-0000-0000-00000000000f',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'cf00000f-0000-0000-0000-00000000000f',1,$j${"fields":[{"key":"liverDescription","label":"Gan","type":"TEXTAREA","group":"Các tạng","displayOrder":1,"normalValue":"Kích thước và nhu mô chưa ghi nhận bất thường."},{"key":"gallbladderDescription","label":"Túi mật - đường mật","type":"TEXTAREA","group":"Các tạng","displayOrder":2,"normalValue":"Chưa ghi nhận bất thường."},{"key":"kidneyDescription","label":"Hai thận","type":"TEXTAREA","group":"Các tạng","displayOrder":3,"normalValue":"Chưa ghi nhận bất thường."},{"key":"abnormalFinding","label":"Phát hiện bất thường","type":"TEXTAREA","group":"Kết luận","displayOrder":4},{"key":"imagingConclusion","label":"Kết luận","type":"TEXTAREA","group":"Kết luận","displayOrder":5,"required":true,"normalValue":"Chưa ghi nhận bất thường trên siêu âm ổ bụng."}]}$j$::jsonb,'PUBLISHED','Khởi tạo mẫu siêu âm ổ bụng',pg_temp.demo_date() - 60,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',(pg_temp.demo_now()-interval '60 days')),
('cf100010-0000-0000-0000-000000000010',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'cf000010-0000-0000-0000-000000000010',1,$j${"fields":[{"key":"noduleLocation","label":"Vị trí nhân","type":"TEXT","group":"Nhân giáp","displayOrder":1},{"key":"noduleSizeMm","label":"Kích thước nhân","type":"NUMBER","unit":"mm","group":"Nhân giáp","displayOrder":2,"min":0},{"key":"noduleFeatures","label":"Đặc điểm nhân","type":"TEXTAREA","group":"Nhân giáp","displayOrder":3},{"key":"tirads","label":"TI-RADS","type":"SELECT","group":"Phân loại","displayOrder":4,"options":["TR1","TR2","TR3","TR4","TR5","NOT_APPLICABLE"]},{"key":"imagingConclusion","label":"Kết luận","type":"TEXTAREA","group":"Kết luận","displayOrder":5,"required":true}]}$j$::jsonb,'PUBLISHED','Khởi tạo mẫu siêu âm tuyến giáp',pg_temp.demo_date() - 60,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',(pg_temp.demo_now()-interval '60 days')),
('cf100011-0000-0000-0000-000000000011',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'cf000011-0000-0000-0000-000000000011',1,$j${"fields":[{"key":"crl","label":"CRL","type":"NUMBER","unit":"mm","group":"Sinh trắc thai","displayOrder":1,"min":0},{"key":"bpd","label":"BPD","type":"NUMBER","unit":"mm","group":"Sinh trắc thai","displayOrder":2,"min":0},{"key":"hc","label":"HC","type":"NUMBER","unit":"mm","group":"Sinh trắc thai","displayOrder":3,"min":0},{"key":"ac","label":"AC","type":"NUMBER","unit":"mm","group":"Sinh trắc thai","displayOrder":4,"min":0},{"key":"fl","label":"FL","type":"NUMBER","unit":"mm","group":"Sinh trắc thai","displayOrder":5,"min":0},{"key":"fhr","label":"FHR","type":"NUMBER","unit":"bpm","group":"Tim thai - nước ối","displayOrder":6,"min":0},{"key":"afi","label":"AFI","type":"NUMBER","unit":"cm","group":"Tim thai - nước ối","displayOrder":7,"min":0},{"key":"clinicalGaWeeks","label":"Tuổi thai lâm sàng","type":"NUMBER","unit":"tuần","group":"Tuổi thai","displayOrder":8,"min":0,"max":45},{"key":"gaByCrlDays","label":"GA ước tính theo CRL","type":"NUMBER","unit":"ngày","group":"Tuổi thai","displayOrder":9,"calculatorKey":"GA_CRL_ROBINSON_FLEMING_V1","precision":0},{"key":"gaByBpdFlDays","label":"GA ước tính theo BPD/FL","type":"NUMBER","unit":"ngày","group":"Tuổi thai","displayOrder":10,"calculatorKey":"GA_HADLOCK_BPD_FL_V1","precision":0},{"key":"efw","label":"EFW ước tính","type":"NUMBER","unit":"g","group":"Cân nặng thai","displayOrder":11,"calculatorKey":"EFW_HADLOCK_HC_AC_FL_V1","precision":0},{"key":"imagingConclusion","label":"Kết luận","type":"TEXTAREA","group":"Kết luận","displayOrder":12,"required":true}]}$j$::jsonb,'PUBLISHED','Khởi tạo công thức GA và EFW ước tính',pg_temp.demo_date() - 60,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',(pg_temp.demo_now()-interval '60 days')),
('cf100012-0000-0000-0000-000000000012',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'cf000012-0000-0000-0000-000000000012',1,$j${"fields":[{"key":"heartRate","label":"Tần số tim","type":"NUMBER","unit":"bpm","group":"Điện tim","displayOrder":1,"required":true,"min":0},{"key":"rhythmType","label":"Loại nhịp","type":"SELECT","group":"Điện tim","displayOrder":2,"options":["SINUS","ATRIAL_FIBRILLATION","OTHER"]},{"key":"ecgDescription","label":"Mô tả","type":"TEXTAREA","group":"Điện tim","displayOrder":3,"normalValue":"Nhịp xoang đều."},{"key":"ecgConclusion","label":"Kết luận","type":"TEXTAREA","group":"Kết luận","displayOrder":4,"required":true,"normalValue":"Điện tâm đồ trong giới hạn bình thường."}]}$j$::jsonb,'PUBLISHED','Khởi tạo mẫu ECG',pg_temp.demo_date() - 60,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',(pg_temp.demo_now()-interval '60 days'))
ON CONFLICT (template_id, version_no) DO NOTHING;

INSERT INTO clinical_form_template_version
(version_id,created_at,updated_at,deleted,template_id,version_no,schema_json,status,change_reason,effective_from,created_by,published_by,published_at)
VALUES
('cf200007-0000-0000-0000-000000000007',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'cf000007-0000-0000-0000-000000000007',2,$j$
{"layout":"LAB_TABLE","systemManaged":true,"sourceName":"Public laboratory test catalogs","sourceVersion":"REFERENCE_CATALOG_V1","fields":[
{"key":"glucoseContext","code":"Loại mẫu","label":"Bối cảnh đo đường huyết","type":"SELECT","group":"Đường huyết","displayOrder":1,"requiredOnSign":true,"options":[{"value":"FASTING","label":"Lúc đói"},{"value":"RANDOM","label":"Bất kỳ"},{"value":"POSTPRANDIAL_2H","label":"Sau ăn 2 giờ"}]},
{"key":"fastingHours","code":"Giờ nhịn ăn","label":"Số giờ nhịn ăn","type":"NUMBER","unit":"giờ","group":"Đường huyết","displayOrder":2,"min":0,"max":24,"precision":1,"visibleWhen":{"field":"glucoseContext","equals":"FASTING"},"requiredWhen":{"field":"glucoseContext","equals":"FASTING"}},
{"key":"lastMealAt","code":"Bữa ăn gần nhất","label":"Thời điểm bữa ăn gần nhất","type":"TEXT","group":"Đường huyết","displayOrder":3,"visibleWhen":{"field":"glucoseContext","equals":"POSTPRANDIAL_2H"},"requiredWhen":{"field":"glucoseContext","equals":"POSTPRANDIAL_2H"}},
{"key":"glucoseQualifier","code":"Dấu","label":"Dấu định lượng","type":"SELECT","group":"Đường huyết","displayOrder":4,"requiredOnSign":true,"options":[{"value":"LESS_THAN","label":"<"},{"value":"EQUAL","label":"="},{"value":"GREATER_THAN","label":">"}]},
{"key":"glucose","code":"GLU","loincCode":"2345-7","label":"Glucose máu","type":"NUMBER","unit":"mmol/L","group":"Đường huyết","displayOrder":5,"requiredOnSign":true,"qualifierKey":"glucoseQualifier","min":0,"max":60,"precision":2,"referenceRanges":[{"sex":"ANY","minAge":0,"maxAge":200,"ageUnit":"YEARS","when":{"field":"glucoseContext","equals":"FASTING"},"low":3.9,"high":5.5},{"sex":"ANY","minAge":0,"maxAge":200,"ageUnit":"YEARS","when":{"field":"glucoseContext","equals":"RANDOM"},"high":7.8},{"sex":"ANY","minAge":0,"maxAge":200,"ageUnit":"YEARS","when":{"field":"glucoseContext","equals":"POSTPRANDIAL_2H"},"high":7.8}],"criticalRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","low":2.5,"high":25}]},
{"key":"sampleConditionNote","code":"Ghi chú","label":"Ghi chú tình trạng mẫu","type":"TEXTAREA","group":"Đường huyết","displayOrder":6}
]}$j$::jsonb,'PUBLISHED','Bổ sung bối cảnh đo, qualifier và khoảng tham chiếu',pg_temp.demo_date() - 60,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',(pg_temp.demo_now()-interval '60 days')),

('cf200008-0000-0000-0000-000000000008',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'cf000008-0000-0000-0000-000000000008',2,$j$
{"layout":"LAB_TABLE","systemManaged":true,"sourceName":"Public clinical chemistry laboratory catalogs","sourceVersion":"REFERENCE_CATALOG_V1","fields":[
{"key":"hba1c","code":"HbA1c","loincCode":"4548-4","label":"Hemoglobin A1c","type":"NUMBER","unit":"%","group":"Chuyển hóa đường","displayOrder":1,"requiredOnSign":true,"min":2,"max":20,"precision":1,"referenceRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","high":5.6}]},
{"key":"totalCholesterol","code":"TC","loincCode":"2093-3","label":"Cholesterol toàn phần","type":"NUMBER","unit":"mmol/L","group":"Lipid máu","displayOrder":2,"requiredOnSign":true,"min":0,"max":30,"precision":2,"referenceRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","high":5.2}]},
{"key":"triglyceride","code":"TG","loincCode":"2571-8","label":"Triglyceride","type":"NUMBER","unit":"mmol/L","group":"Lipid máu","displayOrder":3,"requiredOnSign":true,"min":0,"max":30,"precision":2,"referenceRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","high":1.7}]},
{"key":"hdlC","code":"HDL-C","loincCode":"2085-9","label":"HDL Cholesterol","type":"NUMBER","unit":"mmol/L","group":"Lipid máu","displayOrder":4,"requiredOnSign":true,"min":0,"max":10,"precision":2,"referenceRanges":[{"sex":"MALE","minAge":18,"maxAge":200,"ageUnit":"YEARS","low":1.0},{"sex":"FEMALE","minAge":18,"maxAge":200,"ageUnit":"YEARS","low":1.3}]},
{"key":"ldlC","code":"LDL-C","loincCode":"13457-7","label":"LDL Cholesterol","type":"NUMBER","unit":"mmol/L","group":"Lipid máu","displayOrder":5,"requiredOnSign":true,"min":0,"max":20,"precision":2,"referenceRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","high":3.4}]},
{"key":"nonHdlC","code":"Non-HDL-C","label":"Non-HDL Cholesterol","type":"NUMBER","unit":"mmol/L","group":"Lipid máu","displayOrder":6,"calculatorKey":"NON_HDL_C_V1","precision":2,"referenceRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","high":4.1}]},
{"key":"acidUric","code":"UA","loincCode":"3084-1","label":"Acid uric","type":"NUMBER","unit":"µmol/L","group":"Chuyển hóa khác","displayOrder":7,"requiredOnSign":true,"min":0,"max":1500,"precision":0,"referenceRanges":[{"sex":"MALE","minAge":18,"maxAge":200,"ageUnit":"YEARS","low":210,"high":420},{"sex":"FEMALE","minAge":18,"maxAge":200,"ageUnit":"YEARS","low":150,"high":350}]},
{"key":"totalProtein","code":"TP","loincCode":"2885-2","label":"Protein toàn phần","type":"NUMBER","unit":"g/L","group":"Chuyển hóa khác","displayOrder":8,"requiredOnSign":true,"min":10,"max":150,"precision":1,"referenceRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","low":64,"high":83}]}
]}$j$::jsonb,'PUBLISHED','Mở rộng sinh hóa máu cơ bản và Non-HDL-C',pg_temp.demo_date() - 60,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',(pg_temp.demo_now()-interval '60 days')),

('cf200009-0000-0000-0000-000000000009',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'cf000009-0000-0000-0000-000000000009',2,$j$
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
$j$::jsonb,'PUBLISHED','Mở rộng bộ chức năng gan và Bilirubin gián tiếp',pg_temp.demo_date() - 60,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',(pg_temp.demo_now()-interval '60 days')),

('cf20000a-0000-0000-0000-00000000000a',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'cf00000a-0000-0000-0000-00000000000a',2,$j$
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
]}$j$::jsonb,'PUBLISHED','Mở rộng chức năng thận, điện giải, BUN và eGFR',pg_temp.demo_date() - 60,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',(pg_temp.demo_now()-interval '60 days'))
ON CONFLICT (template_id, version_no) DO NOTHING;

-- ===================================================================
-- System-managed laboratory forms. These schemas are version snapshots;
-- staff enter results manually, while units/ranges/calculations are owned
-- by the backend and cannot be edited from the Clinic Manager UI.
-- ===================================================================
INSERT INTO clinical_form_template_version
(version_id,created_at,updated_at,deleted,template_id,version_no,schema_json,status,change_reason,effective_from,created_by,published_by,published_at)
VALUES ('cf200006-0000-0000-0000-000000000006',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'cf000006-0000-0000-0000-000000000006',2,$j$
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
$j$::jsonb,'PUBLISHED','Mở rộng CBC và khoảng tham chiếu có kiểm soát',pg_temp.demo_date() - 60,
'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',(pg_temp.demo_now()-interval '60 days'))
ON CONFLICT (template_id, version_no) DO NOTHING;

INSERT INTO clinical_form_template_version
(version_id,created_at,updated_at,deleted,template_id,version_no,schema_json,status,change_reason,effective_from,created_by,published_by,published_at)
VALUES
('cf20000b-0000-0000-0000-00000000000b',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'cf00000b-0000-0000-0000-00000000000b',2,$j$
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
]}$j$::jsonb,'PUBLISHED','Mở rộng vật lý, hóa học và soi vi thể nước tiểu',pg_temp.demo_date() - 60,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',(pg_temp.demo_now()-interval '60 days')),

('cf20000c-0000-0000-0000-00000000000c',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'cf00000c-0000-0000-0000-00000000000c',2,$j$
{"layout":"LAB_TABLE","systemManaged":true,"sourceName":"Public quantitative CRP laboratory catalogs","sourceVersion":"REFERENCE_CATALOG_V1","fields":[
{"key":"crpAssayType","code":"Loại CRP","label":"Loại xét nghiệm","type":"SELECT","group":"CRP định lượng","displayOrder":1,"requiredOnSign":true,"options":[{"value":"STANDARD_CRP","label":"CRP thường (định lượng)"}]},
{"key":"crpQualifier","code":"Dấu","label":"Dấu định lượng","type":"SELECT","group":"CRP định lượng","displayOrder":2,"requiredOnSign":true,"options":[{"value":"LESS_THAN","label":"<"},{"value":"EQUAL","label":"="},{"value":"GREATER_THAN","label":">"}]},
{"key":"crp","code":"CRP","loincCode":"1988-5","label":"C-Reactive Protein","type":"NUMBER","unit":"mg/L","group":"CRP định lượng","displayOrder":3,"requiredOnSign":true,"qualifierKey":"crpQualifier","min":0,"max":1000,"precision":1,"referenceRanges":[{"sex":"ANY","minAge":0,"maxAge":200,"ageUnit":"YEARS","high":5}],"criticalRanges":[{"sex":"ANY","minAge":18,"maxAge":200,"ageUnit":"YEARS","high":100}]},
{"key":"detectionLimit","code":"LoD","label":"Giới hạn phát hiện của phương pháp","type":"NUMBER","unit":"mg/L","group":"Thông tin phương pháp","displayOrder":4,"min":0,"max":100,"precision":2},
{"key":"methodNote","code":"Phương pháp","label":"Ghi chú phương pháp","type":"TEXTAREA","group":"Thông tin phương pháp","displayOrder":5}
]}$j$::jsonb,'PUBLISHED','Bổ sung CRP định lượng, qualifier và giới hạn phát hiện',pg_temp.demo_date() - 60,'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',(pg_temp.demo_now()-interval '60 days'))
ON CONFLICT (template_id, version_no) DO NOTHING;

INSERT INTO clinical_form_template_version
(version_id,created_at,updated_at,deleted,template_id,version_no,schema_json,status,change_reason,effective_from,created_by,published_by,published_at)
VALUES ('cf20000d-0000-0000-0000-00000000000d',(pg_temp.demo_now()-interval '60 days'),(pg_temp.demo_now()-interval '60 days'),false,'cf00000d-0000-0000-0000-00000000000d',2,$j$
{"layout":"LAB_TABLE","systemManaged":true,"sourceName":"Manufacturer rapid-test instructions for use; result interpretation is kit-specific","sourceVersion":"REFERENCE_CATALOG_V1","fields":[{"key":"hbsAgPerformed","code":"HBsAg - thực hiện","label":"Thực hiện HBsAg","type":"BOOLEAN","group":"HBsAg","displayOrder":1},{"key":"hbsAgResult","code":"HBsAg","label":"Kết quả HBsAg","type":"SELECT","group":"HBsAg","displayOrder":2,"visibleWhen":{"field":"hbsAgPerformed","equals":true},"requiredWhen":{"field":"hbsAgPerformed","equals":true},"options":["NEGATIVE","POSITIVE","INDETERMINATE","INVALID"],"referenceRanges":[{"sex":"ANY","minAge":0,"maxAge":200,"ageUnit":"YEARS","normalValues":["NEGATIVE"]}]},{"key":"hbsAgSampleCode","code":"Mã mẫu","label":"Mã mẫu HBsAg","type":"TEXT","group":"HBsAg","displayOrder":3,"visibleWhen":{"field":"hbsAgPerformed","equals":true},"requiredWhen":{"field":"hbsAgPerformed","equals":true}},{"key":"hbsAgSampleType","code":"Loại mẫu","label":"Loại mẫu HBsAg","type":"SELECT","group":"HBsAg","displayOrder":4,"visibleWhen":{"field":"hbsAgPerformed","equals":true},"requiredWhen":{"field":"hbsAgPerformed","equals":true},"options":["WHOLE_BLOOD","SERUM","PLASMA","NASOPHARYNGEAL_SWAB","OTHER"]},{"key":"hbsAgKitName","code":"Kit","label":"Tên kit HBsAg","type":"TEXT","group":"HBsAg","displayOrder":5,"visibleWhen":{"field":"hbsAgPerformed","equals":true},"requiredWhen":{"field":"hbsAgPerformed","equals":true}},{"key":"hbsAgLotNumber","code":"Số lô","label":"Số lô kit HBsAg","type":"TEXT","group":"HBsAg","displayOrder":6,"visibleWhen":{"field":"hbsAgPerformed","equals":true},"requiredWhen":{"field":"hbsAgPerformed","equals":true}},{"key":"hbsAgKitExpiry","code":"Hạn dùng","label":"Hạn sử dụng kit HBsAg","type":"DATE","group":"HBsAg","displayOrder":7,"visibleWhen":{"field":"hbsAgPerformed","equals":true},"requiredWhen":{"field":"hbsAgPerformed","equals":true}},{"key":"hbsAgControlValid","code":"Control","label":"Control hợp lệ HBsAg","type":"BOOLEAN","group":"HBsAg","displayOrder":8,"visibleWhen":{"field":"hbsAgPerformed","equals":true},"requiredWhen":{"field":"hbsAgPerformed","equals":true}},{"key":"hbsAgReadAt","code":"Thời gian đọc","label":"Thời gian đọc HBsAg","type":"TEXT","group":"HBsAg","displayOrder":9,"visibleWhen":{"field":"hbsAgPerformed","equals":true},"requiredWhen":{"field":"hbsAgPerformed","equals":true},"pattern":"^\\\\d{4}-\\\\d{2}-\\\\d{2}[ T]\\\\d{2}:\\\\d{2}(:\\\\d{2})?$","patternMessage":"Thời gian đọc phải theo định dạng YYYY-MM-DD HH:mm"},{"key":"antiHcvPerformed","code":"Anti-HCV - thực hiện","label":"Thực hiện Anti-HCV","type":"BOOLEAN","group":"Anti-HCV","displayOrder":10},{"key":"antiHcvResult","code":"Anti-HCV","label":"Kết quả Anti-HCV","type":"SELECT","group":"Anti-HCV","displayOrder":11,"visibleWhen":{"field":"antiHcvPerformed","equals":true},"requiredWhen":{"field":"antiHcvPerformed","equals":true},"options":["NEGATIVE","POSITIVE","INDETERMINATE","INVALID"],"referenceRanges":[{"sex":"ANY","minAge":0,"maxAge":200,"ageUnit":"YEARS","normalValues":["NEGATIVE"]}]},{"key":"antiHcvSampleCode","code":"Mã mẫu","label":"Mã mẫu Anti-HCV","type":"TEXT","group":"Anti-HCV","displayOrder":12,"visibleWhen":{"field":"antiHcvPerformed","equals":true},"requiredWhen":{"field":"antiHcvPerformed","equals":true}},{"key":"antiHcvSampleType","code":"Loại mẫu","label":"Loại mẫu Anti-HCV","type":"SELECT","group":"Anti-HCV","displayOrder":13,"visibleWhen":{"field":"antiHcvPerformed","equals":true},"requiredWhen":{"field":"antiHcvPerformed","equals":true},"options":["WHOLE_BLOOD","SERUM","PLASMA","NASOPHARYNGEAL_SWAB","OTHER"]},{"key":"antiHcvKitName","code":"Kit","label":"Tên kit Anti-HCV","type":"TEXT","group":"Anti-HCV","displayOrder":14,"visibleWhen":{"field":"antiHcvPerformed","equals":true},"requiredWhen":{"field":"antiHcvPerformed","equals":true}},{"key":"antiHcvLotNumber","code":"Số lô","label":"Số lô kit Anti-HCV","type":"TEXT","group":"Anti-HCV","displayOrder":15,"visibleWhen":{"field":"antiHcvPerformed","equals":true},"requiredWhen":{"field":"antiHcvPerformed","equals":true}},{"key":"antiHcvKitExpiry","code":"Hạn dùng","label":"Hạn sử dụng kit Anti-HCV","type":"DATE","group":"Anti-HCV","displayOrder":16,"visibleWhen":{"field":"antiHcvPerformed","equals":true},"requiredWhen":{"field":"antiHcvPerformed","equals":true}},{"key":"antiHcvControlValid","code":"Control","label":"Control hợp lệ Anti-HCV","type":"BOOLEAN","group":"Anti-HCV","displayOrder":17,"visibleWhen":{"field":"antiHcvPerformed","equals":true},"requiredWhen":{"field":"antiHcvPerformed","equals":true}},{"key":"antiHcvReadAt","code":"Thời gian đọc","label":"Thời gian đọc Anti-HCV","type":"TEXT","group":"Anti-HCV","displayOrder":18,"visibleWhen":{"field":"antiHcvPerformed","equals":true},"requiredWhen":{"field":"antiHcvPerformed","equals":true},"pattern":"^\\\\d{4}-\\\\d{2}-\\\\d{2}[ T]\\\\d{2}:\\\\d{2}(:\\\\d{2})?$","patternMessage":"Thời gian đọc phải theo định dạng YYYY-MM-DD HH:mm"},{"key":"hivPerformed","code":"HIV - thực hiện","label":"Thực hiện HIV Ag/Ab","type":"BOOLEAN","group":"HIV Ag/Ab","displayOrder":19},{"key":"hivResult","code":"HIV","label":"Kết quả HIV Ag/Ab","type":"SELECT","group":"HIV Ag/Ab","displayOrder":20,"visibleWhen":{"field":"hivPerformed","equals":true},"requiredWhen":{"field":"hivPerformed","equals":true},"options":["NEGATIVE","POSITIVE","INDETERMINATE","INVALID"],"referenceRanges":[{"sex":"ANY","minAge":0,"maxAge":200,"ageUnit":"YEARS","normalValues":["NEGATIVE"]}]},{"key":"hivSampleCode","code":"Mã mẫu","label":"Mã mẫu HIV Ag/Ab","type":"TEXT","group":"HIV Ag/Ab","displayOrder":21,"visibleWhen":{"field":"hivPerformed","equals":true},"requiredWhen":{"field":"hivPerformed","equals":true}},{"key":"hivSampleType","code":"Loại mẫu","label":"Loại mẫu HIV Ag/Ab","type":"SELECT","group":"HIV Ag/Ab","displayOrder":22,"visibleWhen":{"field":"hivPerformed","equals":true},"requiredWhen":{"field":"hivPerformed","equals":true},"options":["WHOLE_BLOOD","SERUM","PLASMA","NASOPHARYNGEAL_SWAB","OTHER"]},{"key":"hivKitName","code":"Kit","label":"Tên kit HIV Ag/Ab","type":"TEXT","group":"HIV Ag/Ab","displayOrder":23,"visibleWhen":{"field":"hivPerformed","equals":true},"requiredWhen":{"field":"hivPerformed","equals":true}},{"key":"hivLotNumber","code":"Số lô","label":"Số lô kit HIV Ag/Ab","type":"TEXT","group":"HIV Ag/Ab","displayOrder":24,"visibleWhen":{"field":"hivPerformed","equals":true},"requiredWhen":{"field":"hivPerformed","equals":true}},{"key":"hivKitExpiry","code":"Hạn dùng","label":"Hạn sử dụng kit HIV Ag/Ab","type":"DATE","group":"HIV Ag/Ab","displayOrder":25,"visibleWhen":{"field":"hivPerformed","equals":true},"requiredWhen":{"field":"hivPerformed","equals":true}},{"key":"hivControlValid","code":"Control","label":"Control hợp lệ HIV Ag/Ab","type":"BOOLEAN","group":"HIV Ag/Ab","displayOrder":26,"visibleWhen":{"field":"hivPerformed","equals":true},"requiredWhen":{"field":"hivPerformed","equals":true}},{"key":"hivReadAt","code":"Thời gian đọc","label":"Thời gian đọc HIV Ag/Ab","type":"TEXT","group":"HIV Ag/Ab","displayOrder":27,"visibleWhen":{"field":"hivPerformed","equals":true},"requiredWhen":{"field":"hivPerformed","equals":true},"pattern":"^\\\\d{4}-\\\\d{2}-\\\\d{2}[ T]\\\\d{2}:\\\\d{2}(:\\\\d{2})?$","patternMessage":"Thời gian đọc phải theo định dạng YYYY-MM-DD HH:mm"},{"key":"dengueNs1Performed","code":"Dengue NS1 - thực hiện","label":"Thực hiện Dengue NS1","type":"BOOLEAN","group":"Dengue NS1","displayOrder":28},{"key":"dengueNs1Result","code":"Dengue NS1","label":"Kết quả Dengue NS1","type":"SELECT","group":"Dengue NS1","displayOrder":29,"visibleWhen":{"field":"dengueNs1Performed","equals":true},"requiredWhen":{"field":"dengueNs1Performed","equals":true},"options":["NEGATIVE","POSITIVE","INDETERMINATE","INVALID"],"referenceRanges":[{"sex":"ANY","minAge":0,"maxAge":200,"ageUnit":"YEARS","normalValues":["NEGATIVE"]}]},{"key":"dengueNs1SampleCode","code":"Mã mẫu","label":"Mã mẫu Dengue NS1","type":"TEXT","group":"Dengue NS1","displayOrder":30,"visibleWhen":{"field":"dengueNs1Performed","equals":true},"requiredWhen":{"field":"dengueNs1Performed","equals":true}},{"key":"dengueNs1SampleType","code":"Loại mẫu","label":"Loại mẫu Dengue NS1","type":"SELECT","group":"Dengue NS1","displayOrder":31,"visibleWhen":{"field":"dengueNs1Performed","equals":true},"requiredWhen":{"field":"dengueNs1Performed","equals":true},"options":["WHOLE_BLOOD","SERUM","PLASMA","NASOPHARYNGEAL_SWAB","OTHER"]},{"key":"dengueNs1KitName","code":"Kit","label":"Tên kit Dengue NS1","type":"TEXT","group":"Dengue NS1","displayOrder":32,"visibleWhen":{"field":"dengueNs1Performed","equals":true},"requiredWhen":{"field":"dengueNs1Performed","equals":true}},{"key":"dengueNs1LotNumber","code":"Số lô","label":"Số lô kit Dengue NS1","type":"TEXT","group":"Dengue NS1","displayOrder":33,"visibleWhen":{"field":"dengueNs1Performed","equals":true},"requiredWhen":{"field":"dengueNs1Performed","equals":true}},{"key":"dengueNs1KitExpiry","code":"Hạn dùng","label":"Hạn sử dụng kit Dengue NS1","type":"DATE","group":"Dengue NS1","displayOrder":34,"visibleWhen":{"field":"dengueNs1Performed","equals":true},"requiredWhen":{"field":"dengueNs1Performed","equals":true}},{"key":"dengueNs1ControlValid","code":"Control","label":"Control hợp lệ Dengue NS1","type":"BOOLEAN","group":"Dengue NS1","displayOrder":35,"visibleWhen":{"field":"dengueNs1Performed","equals":true},"requiredWhen":{"field":"dengueNs1Performed","equals":true}},{"key":"dengueNs1ReadAt","code":"Thời gian đọc","label":"Thời gian đọc Dengue NS1","type":"TEXT","group":"Dengue NS1","displayOrder":36,"visibleWhen":{"field":"dengueNs1Performed","equals":true},"requiredWhen":{"field":"dengueNs1Performed","equals":true},"pattern":"^\\\\d{4}-\\\\d{2}-\\\\d{2}[ T]\\\\d{2}:\\\\d{2}(:\\\\d{2})?$","patternMessage":"Thời gian đọc phải theo định dạng YYYY-MM-DD HH:mm"},{"key":"influenzaAPerformed","code":"Influenza A - thực hiện","label":"Thực hiện Cúm A","type":"BOOLEAN","group":"Cúm A","displayOrder":37},{"key":"influenzaAResult","code":"Influenza A","label":"Kết quả Cúm A","type":"SELECT","group":"Cúm A","displayOrder":38,"visibleWhen":{"field":"influenzaAPerformed","equals":true},"requiredWhen":{"field":"influenzaAPerformed","equals":true},"options":["NEGATIVE","POSITIVE","INDETERMINATE","INVALID"],"referenceRanges":[{"sex":"ANY","minAge":0,"maxAge":200,"ageUnit":"YEARS","normalValues":["NEGATIVE"]}]},{"key":"influenzaASampleCode","code":"Mã mẫu","label":"Mã mẫu Cúm A","type":"TEXT","group":"Cúm A","displayOrder":39,"visibleWhen":{"field":"influenzaAPerformed","equals":true},"requiredWhen":{"field":"influenzaAPerformed","equals":true}},{"key":"influenzaASampleType","code":"Loại mẫu","label":"Loại mẫu Cúm A","type":"SELECT","group":"Cúm A","displayOrder":40,"visibleWhen":{"field":"influenzaAPerformed","equals":true},"requiredWhen":{"field":"influenzaAPerformed","equals":true},"options":["WHOLE_BLOOD","SERUM","PLASMA","NASOPHARYNGEAL_SWAB","OTHER"]},{"key":"influenzaAKitName","code":"Kit","label":"Tên kit Cúm A","type":"TEXT","group":"Cúm A","displayOrder":41,"visibleWhen":{"field":"influenzaAPerformed","equals":true},"requiredWhen":{"field":"influenzaAPerformed","equals":true}},{"key":"influenzaALotNumber","code":"Số lô","label":"Số lô kit Cúm A","type":"TEXT","group":"Cúm A","displayOrder":42,"visibleWhen":{"field":"influenzaAPerformed","equals":true},"requiredWhen":{"field":"influenzaAPerformed","equals":true}},{"key":"influenzaAKitExpiry","code":"Hạn dùng","label":"Hạn sử dụng kit Cúm A","type":"DATE","group":"Cúm A","displayOrder":43,"visibleWhen":{"field":"influenzaAPerformed","equals":true},"requiredWhen":{"field":"influenzaAPerformed","equals":true}},{"key":"influenzaAControlValid","code":"Control","label":"Control hợp lệ Cúm A","type":"BOOLEAN","group":"Cúm A","displayOrder":44,"visibleWhen":{"field":"influenzaAPerformed","equals":true},"requiredWhen":{"field":"influenzaAPerformed","equals":true}},{"key":"influenzaAReadAt","code":"Thời gian đọc","label":"Thời gian đọc Cúm A","type":"TEXT","group":"Cúm A","displayOrder":45,"visibleWhen":{"field":"influenzaAPerformed","equals":true},"requiredWhen":{"field":"influenzaAPerformed","equals":true},"pattern":"^\\\\d{4}-\\\\d{2}-\\\\d{2}[ T]\\\\d{2}:\\\\d{2}(:\\\\d{2})?$","patternMessage":"Thời gian đọc phải theo định dạng YYYY-MM-DD HH:mm"},{"key":"influenzaBPerformed","code":"Influenza B - thực hiện","label":"Thực hiện Cúm B","type":"BOOLEAN","group":"Cúm B","displayOrder":46},{"key":"influenzaBResult","code":"Influenza B","label":"Kết quả Cúm B","type":"SELECT","group":"Cúm B","displayOrder":47,"visibleWhen":{"field":"influenzaBPerformed","equals":true},"requiredWhen":{"field":"influenzaBPerformed","equals":true},"options":["NEGATIVE","POSITIVE","INDETERMINATE","INVALID"],"referenceRanges":[{"sex":"ANY","minAge":0,"maxAge":200,"ageUnit":"YEARS","normalValues":["NEGATIVE"]}]},{"key":"influenzaBSampleCode","code":"Mã mẫu","label":"Mã mẫu Cúm B","type":"TEXT","group":"Cúm B","displayOrder":48,"visibleWhen":{"field":"influenzaBPerformed","equals":true},"requiredWhen":{"field":"influenzaBPerformed","equals":true}},{"key":"influenzaBSampleType","code":"Loại mẫu","label":"Loại mẫu Cúm B","type":"SELECT","group":"Cúm B","displayOrder":49,"visibleWhen":{"field":"influenzaBPerformed","equals":true},"requiredWhen":{"field":"influenzaBPerformed","equals":true},"options":["WHOLE_BLOOD","SERUM","PLASMA","NASOPHARYNGEAL_SWAB","OTHER"]},{"key":"influenzaBKitName","code":"Kit","label":"Tên kit Cúm B","type":"TEXT","group":"Cúm B","displayOrder":50,"visibleWhen":{"field":"influenzaBPerformed","equals":true},"requiredWhen":{"field":"influenzaBPerformed","equals":true}},{"key":"influenzaBLotNumber","code":"Số lô","label":"Số lô kit Cúm B","type":"TEXT","group":"Cúm B","displayOrder":51,"visibleWhen":{"field":"influenzaBPerformed","equals":true},"requiredWhen":{"field":"influenzaBPerformed","equals":true}},{"key":"influenzaBKitExpiry","code":"Hạn dùng","label":"Hạn sử dụng kit Cúm B","type":"DATE","group":"Cúm B","displayOrder":52,"visibleWhen":{"field":"influenzaBPerformed","equals":true},"requiredWhen":{"field":"influenzaBPerformed","equals":true}},{"key":"influenzaBControlValid","code":"Control","label":"Control hợp lệ Cúm B","type":"BOOLEAN","group":"Cúm B","displayOrder":53,"visibleWhen":{"field":"influenzaBPerformed","equals":true},"requiredWhen":{"field":"influenzaBPerformed","equals":true}},{"key":"influenzaBReadAt","code":"Thời gian đọc","label":"Thời gian đọc Cúm B","type":"TEXT","group":"Cúm B","displayOrder":54,"visibleWhen":{"field":"influenzaBPerformed","equals":true},"requiredWhen":{"field":"influenzaBPerformed","equals":true},"pattern":"^\\\\d{4}-\\\\d{2}-\\\\d{2}[ T]\\\\d{2}:\\\\d{2}(:\\\\d{2})?$","patternMessage":"Thời gian đọc phải theo định dạng YYYY-MM-DD HH:mm"}],"rules":[{"type":"AT_LEAST_ONE_TRUE","keys":["hbsAgPerformed","antiHcvPerformed","hivPerformed","dengueNs1Performed","influenzaAPerformed","influenzaBPerformed"],"onSignOnly":true,"severity":"ERROR","message":"Phải chọn ít nhất một test nhanh đã thực hiện."},{"type":"BOOLEAN_MUST_BE_TRUE_WHEN","field":"hbsAgControlValid","when":{"field":"hbsAgPerformed","equals":true},"onSignOnly":true,"severity":"ERROR","message":"Control của HBsAg phải hợp lệ trước khi ký."},{"type":"VALUE_NOT_ALLOWED_WHEN","field":"hbsAgResult","value":"INVALID","when":{"field":"hbsAgPerformed","equals":true},"onSignOnly":true,"severity":"ERROR","message":"Kết quả HBsAg INVALID không thể ký."},{"type":"BOOLEAN_MUST_BE_TRUE_WHEN","field":"antiHcvControlValid","when":{"field":"antiHcvPerformed","equals":true},"onSignOnly":true,"severity":"ERROR","message":"Control của Anti-HCV phải hợp lệ trước khi ký."},{"type":"VALUE_NOT_ALLOWED_WHEN","field":"antiHcvResult","value":"INVALID","when":{"field":"antiHcvPerformed","equals":true},"onSignOnly":true,"severity":"ERROR","message":"Kết quả Anti-HCV INVALID không thể ký."},{"type":"BOOLEAN_MUST_BE_TRUE_WHEN","field":"hivControlValid","when":{"field":"hivPerformed","equals":true},"onSignOnly":true,"severity":"ERROR","message":"Control của HIV Ag/Ab phải hợp lệ trước khi ký."},{"type":"VALUE_NOT_ALLOWED_WHEN","field":"hivResult","value":"INVALID","when":{"field":"hivPerformed","equals":true},"onSignOnly":true,"severity":"ERROR","message":"Kết quả HIV Ag/Ab INVALID không thể ký."},{"type":"BOOLEAN_MUST_BE_TRUE_WHEN","field":"dengueNs1ControlValid","when":{"field":"dengueNs1Performed","equals":true},"onSignOnly":true,"severity":"ERROR","message":"Control của Dengue NS1 phải hợp lệ trước khi ký."},{"type":"VALUE_NOT_ALLOWED_WHEN","field":"dengueNs1Result","value":"INVALID","when":{"field":"dengueNs1Performed","equals":true},"onSignOnly":true,"severity":"ERROR","message":"Kết quả Dengue NS1 INVALID không thể ký."},{"type":"BOOLEAN_MUST_BE_TRUE_WHEN","field":"influenzaAControlValid","when":{"field":"influenzaAPerformed","equals":true},"onSignOnly":true,"severity":"ERROR","message":"Control của Cúm A phải hợp lệ trước khi ký."},{"type":"VALUE_NOT_ALLOWED_WHEN","field":"influenzaAResult","value":"INVALID","when":{"field":"influenzaAPerformed","equals":true},"onSignOnly":true,"severity":"ERROR","message":"Kết quả Cúm A INVALID không thể ký."},{"type":"BOOLEAN_MUST_BE_TRUE_WHEN","field":"influenzaBControlValid","when":{"field":"influenzaBPerformed","equals":true},"onSignOnly":true,"severity":"ERROR","message":"Control của Cúm B phải hợp lệ trước khi ký."},{"type":"VALUE_NOT_ALLOWED_WHEN","field":"influenzaBResult","value":"INVALID","when":{"field":"influenzaBPerformed","equals":true},"onSignOnly":true,"severity":"ERROR","message":"Kết quả Cúm B INVALID không thể ký."}]}
$j$::jsonb,'PUBLISHED','Mở rộng sáu test nhanh, thông tin kit, control và thời gian đọc',pg_temp.demo_date() - 60,
'90000009-2222-2222-2222-222222222222','90000009-2222-2222-2222-222222222222',(pg_temp.demo_now()-interval '60 days'))
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
SELECT gen_random_uuid(), item.service_code, (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false,
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
SELECT gen_random_uuid(), (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, mapping.service_id, mapping.template_id
FROM (VALUES
 ('40000008-0000-0000-0000-000000000008'::uuid,'cf000006-0000-0000-0000-000000000006'::uuid),('40000009-0000-0000-0000-000000000009','cf000007-0000-0000-0000-000000000007'),('40000010-0000-0000-0000-000000000010','cf000008-0000-0000-0000-000000000008'),('40000011-0000-0000-0000-000000000011','cf000009-0000-0000-0000-000000000009'),('40000012-0000-0000-0000-000000000012','cf00000a-0000-0000-0000-00000000000a'),('40000013-0000-0000-0000-000000000013','cf00000b-0000-0000-0000-00000000000b'),('40000014-0000-0000-0000-000000000014','cf00000c-0000-0000-0000-00000000000c'),('40000020-0000-0000-0000-000000000020','cf00000d-0000-0000-0000-00000000000d'),
 ('40000017-0000-0000-0000-000000000017','cf00000e-0000-0000-0000-00000000000e'),('40000018-0000-0000-0000-000000000018','cf00000e-0000-0000-0000-00000000000e'),('40000015-0000-0000-0000-000000000015','cf00000f-0000-0000-0000-00000000000f'),('40000016-0000-0000-0000-000000000016','cf000010-0000-0000-0000-000000000010'),('40000035-0000-0000-0000-000000000035','cf000011-0000-0000-0000-000000000011'),('40000019-0000-0000-0000-000000000019','cf000012-0000-0000-0000-000000000012')
) AS mapping(service_id, template_id)
ON CONFLICT (service_id) DO NOTHING;

-- Parent bindings now exist; bind every billable analyte to its panel's
-- published schema.
INSERT INTO medical_service_form_template
(binding_id, created_at, updated_at, deleted, service_id, template_id)
SELECT gen_random_uuid(), (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, child.service_id, parent_binding.template_id
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
                AND version.effective_from <= pg_temp.demo_date()
          )
      );

    IF invalid_services IS NOT NULL THEN
        RAISE EXCEPTION 'Dịch vụ cận lâm sàng thiếu biểu mẫu kết quả đang áp dụng: %', invalid_services;
    END IF;
END
$clinical_form_check$;


-- 20 Customer, hai thành viên gia đình và hai Guest = 24 hồ sơ người bệnh.
CREATE TEMP TABLE demo_people ON COMMIT DROP AS
SELECT i,
 CASE WHEN i=13 THEN '22f00000-0000-0000-0000-000000000001'::uuid
 ELSE format('22000000-0000-0000-0000-%s',lpad(i::text,12,'0'))::uuid END patient_id,
 CASE WHEN i>20 THEN NULL WHEN i=13 THEN '32f00000-0000-0000-0000-000000000001'::uuid
 ELSE format('32000000-0000-0000-0000-%s',lpad(i::text,12,'0'))::uuid END account_id,
 (ARRAY['Nguyễn Thị Ánh','Trần Minh Anh','Lê Văn Khoa','Phạm Thị Huyền',
 'Hoàng Quốc Bảo','Vũ Thị Lan','Đặng Minh Tuấn','Nguyễn Ngọc Mai',
 'Trần Gia Hân','Lê Hoàng Phúc','Phạm Đức Anh','Bùi Thanh Thảo',
 'Nguyễn Anh Đức','Ngô Thu Trang','Dương Quốc Huy','Mai Phương Linh',
 'Phan Anh Khoa','Trịnh Mỹ Duyên','Lương Thành Đạt','Tạ Bảo Ngọc',
 'Nguyễn Gia An','Nguyễn Thu Hà','Đỗ Quang Huy','Trần Ngọc Diệp'])[i] full_name,
 (ARRAY['1994-04-12','1988-11-03','1976-07-21','1995-02-18','1982-09-30',
 '1999-06-14','1968-01-25','2001-12-08','2018-03-19','2014-10-05',
 '1991-05-27','1985-08-16','1993-08-22','1997-07-11','2006-04-23',
 '1993-09-09','1987-02-28','2000-01-17','1972-06-06','1996-11-29',
 '2019-04-15','1995-10-12','1986-06-03','1990-09-20'])[i]::date dob,
 CASE WHEN i IN (1,2,4,6,8,9,12,14,16,18,20,22,24) THEN 'FEMALE' ELSE 'MALE' END gender,
 '090900' || lpad(i::text,4,'0') phone
FROM generate_series(1,24) g(i);
INSERT INTO account(account_id,created_at,is_active,password_hash,role,username)
SELECT account_id,pg_temp.demo_now()-interval '60 days',true,
 '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m','CUSTOMER',phone
FROM demo_people WHERE account_id IS NOT NULL;
INSERT INTO profile(profile_id,account_id,created_at,updated_at,deleted,patient_code,
 full_name,date_of_birth,gender,phone,email,address,blood_type,height,weight)
SELECT patient_id,account_id,pg_temp.demo_now()-interval '60 days',pg_temp.demo_now(),false,
 'BN-DEMO-'||lpad(i::text,3,'0'),full_name,dob,gender,phone,
 'patient.'||lpad(i::text,2,'0')||'@example.com',
 'Số '||i||', phố An Bình (địa chỉ giả lập), Hà Nội',
 CASE WHEN i%3=0 THEN 'A_POSITIVE' ELSE 'O_POSITIVE' END,
 CASE WHEN i IN(9,10,21) THEN 130 ELSE 165 END,
 CASE WHEN i IN(9,10,21) THEN 28 ELSE 60 END FROM demo_people;
INSERT INTO family_member(family_member_id,owner_profile_id,member_profile_id,relationship,
 is_active,created_at,updated_at,deleted)
SELECT pg_temp.did('family:'||p.i),o.patient_id,p.patient_id,
 CASE WHEN p.i=21 THEN 'CHILD' ELSE 'SPOUSE' END,true,pg_temp.demo_now()-interval '60 days',pg_temp.demo_now(),false
FROM demo_people p CROSS JOIN demo_people o WHERE o.i=13 AND p.i IN(21,22);
-- Không gửi email/SMS; danh tính nhân viên cũng là giả lập.
UPDATE profile p SET full_name=regexp_replace(full_name,'^(Bác sĩ |Kỹ thuật viên |Điều dưỡng )',''),
 email='staff.'||left(p.profile_id::text,8)||'.'||right(p.profile_id::text,4)||'@example.com'
WHERE NOT EXISTS(SELECT 1 FROM demo_people d WHERE d.patient_id=p.profile_id);

-- Bảng ánh xạ tạm chỉ tồn tại trong phiên seed, không đổi schema ứng dụng.
CREATE TEMP TABLE demo_cases(
 key text PRIMARY KEY, patient_id uuid, service_no int, room uuid, nurse uuid,
 started timestamp, span interval, queue_status text, finished boolean,
 historical boolean, test_code text, test_room uuid, test_state text
) ON COMMIT DROP;
-- Một VIS lịch sử cho mỗi hồ sơ; trẻ em được khám tại phòng Nhi.
INSERT INTO demo_cases
SELECT 'H'||lpad(p.i::text,2,'0'),patient_id,
 CASE WHEN p.i IN(9,10,21) THEN 3 WHEN p.i=4 THEN 7
 WHEN p.i%5=0 THEN 4 WHEN p.i%5=1 THEN 1 WHEN p.i%5=2 THEN 5
 WHEN p.i%5=3 THEN 6 ELSE 2 END,
 NULL,NULL,pg_temp.demo_date()-p.i+time '09:00',interval '2 hours','DONE',true,true,NULL,NULL,NULL
FROM demo_people p;
CREATE TEMP TABLE demo_rooms(service_no int,room uuid) ON COMMIT DROP;
INSERT INTO demo_rooms VALUES
(1,'33333333-3333-3333-3333-333333333333'),(2,'77777777-7777-7777-7777-777777777777'),
(3,'66666666-6666-6666-6666-666666666666'),(4,'88888888-8888-8888-8888-888888888888'),
(5,'99999999-9999-9999-9999-999999999999'),(6,'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
(7,'dddddddd-dddd-dddd-dddd-dddddddddddd');
-- Mỗi điều dưỡng tham gia một ca lịch sử đúng phòng và ca trực của mình.
INSERT INTO demo_cases
SELECT 'N'||lpad(n.rn::text,2,'0'),p.patient_id,2,NULL,n.staff_id,
 pg_temp.demo_date()-(20+n.rn)::int + sv.start_time+interval '3 hours',
 interval '2 hours','DONE',true,true,
 CASE d.room_code WHEN 'LAB-201' THEN 'LAB-001' WHEN 'LAB-202' THEN 'LAB-002'
 WHEN 'IMG-301' THEN 'IMG-003' ELSE 'IMG-001' END,d.department_id,'COMPLETED'
FROM (SELECT si.*,row_number() OVER(ORDER BY si.staff_id) rn FROM staff_info si
 WHERE system_role='NURSE' AND NOT deleted) n
JOIN demo_people p ON p.i=1+(n.rn::int-1)%8
JOIN department d ON d.department_id=n.department_id
JOIN LATERAL(SELECT ss.shift_version_id FROM staff_schedule ss
 WHERE ss.staff_id=n.staff_id AND ss.work_date=pg_temp.demo_date()-(20+n.rn)::int
 AND NOT ss.deleted ORDER BY ss.schedule_id LIMIT 1) ss ON true
JOIN shift_version sv ON sv.shift_version_id=ss.shift_version_id;
-- Các kịch bản T01..T12 luôn thuộc ngày và ca đang chạy seed.
INSERT INTO demo_cases
SELECT 'T'||lpad(x.n::text,2,'0'),p.patient_id,x.svc,NULL,NULL,
 pg_temp.demo_now()-LEAST(pg_temp.demo_now()-(pg_temp.demo_date()+make_interval(hours => (extract(hour FROM pg_temp.demo_now())::int/8)*8)),interval '2 hours'),
 LEAST(pg_temp.demo_now()-(pg_temp.demo_date()+make_interval(hours => (extract(hour FROM pg_temp.demo_now())::int/8)*8)),interval '2 hours'),
 x.state,x.n=12,false,
 CASE WHEN x.n IN(8,9) THEN 'LAB-001' WHEN x.n=10 THEN 'LAB-002' END,
 CASE WHEN x.n IN(8,9) THEN '44444444-4444-4444-4444-444444444444'::uuid
 WHEN x.n=10 THEN 'cccccccc-cccc-cccc-cccc-cccccccccccc'::uuid END,
 CASE WHEN x.n=8 THEN 'UNPAID' WHEN x.n=9 THEN 'PENDING' WHEN x.n=10 THEN 'COMPLETED' END
FROM (VALUES(1,13,2,'APPOINTMENT'),(2,23,1,'APPOINTMENT'),(3,21,3,'APPOINTMENT'),
 (4,4,7,'UNPAID'),(5,5,1,'WAITING'),(6,6,4,'CALLED'),(7,7,6,'IN_PROGRESS'),
 (8,8,2,'WAITING_FOR_TEST'),(9,11,5,'WAITING_FOR_TEST'),(10,12,7,'TEST_DONE'),
 (11,9,3,'SKIPPED'),(12,3,2,'DONE')) x(n,person,svc,state)
JOIN demo_people p ON p.i=x.person;
UPDATE demo_cases c SET room=r.room FROM demo_rooms r WHERE r.service_no=c.service_no;
-- Lịch sử tại SUR-102 dùng khám Ngoại tổng quát, không bịa vết thương đã điều trị.
UPDATE demo_cases SET service_no=1 WHERE historical AND service_no=5;

CREATE FUNCTION pg_temp.event(c demo_cases, fraction numeric) RETURNS timestamp
LANGUAGE sql IMMUTABLE AS 'SELECT c.started+c.span*fraction::double precision';
CREATE TEMP VIEW demo_case_staff AS
SELECT c.*,d.head_doctor_id doctor_id,
 (SELECT ss.staff_id FROM staff_schedule ss JOIN staff_info s ON s.staff_id=ss.staff_id
  JOIN shift_version sv ON sv.shift_version_id=ss.shift_version_id
  WHERE s.system_role='RECEPTIONIST' AND NOT ss.deleted
  AND ss.work_date=c.started::date AND c.started::time>=sv.start_time
  AND (c.started::time<sv.end_time OR sv.end_time=time '23:59:59') ORDER BY ss.staff_id LIMIT 1) receptionist,
 (SELECT ss.staff_id FROM staff_schedule ss JOIN staff_info s ON s.staff_id=ss.staff_id
  JOIN shift_version sv ON sv.shift_version_id=ss.shift_version_id
  WHERE s.system_role='CASHIER' AND NOT ss.deleted
  AND ss.work_date=c.started::date AND c.started::time>=sv.start_time
  AND (c.started::time<sv.end_time OR sv.end_time=time '23:59:59') ORDER BY ss.staff_id LIMIT 1) cashier
FROM demo_cases c JOIN department d ON d.department_id=c.room;

INSERT INTO appointment(appointment_id,created_at,updated_at,deleted,scheduled_at,status,
 is_guest,customer_id,guest_full_name,guest_phone,guest_email,guest_age,guest_gender,
 guest_address,shift_name,shift_time,shift_version_id)
SELECT pg_temp.did('appointment:'||c.key),c.started-interval '2 days',c.started,false,
 c.started::date+sv.start_time,
 CASE WHEN c.queue_status='APPOINTMENT' THEN 'PENDING' ELSE 'CHECKED_IN' END,
 p.account_id IS NULL AND p.i>=23,
 CASE WHEN p.i<23 THEN p.patient_id END,
 CASE WHEN p.i>=23 THEN p.full_name END,CASE WHEN p.i>=23 THEN p.phone END,
 CASE WHEN p.i>=23 THEN 'guest.'||p.i||'@example.com' END,
 CASE WHEN p.i>=23 THEN extract(year FROM age(c.started,p.dob))::int END,
 CASE WHEN p.i>=23 THEN p.gender END,CASE WHEN p.i>=23 THEN 'Hà Nội (giả lập)' END,
 (SELECT name FROM shift_config WHERE shift_id=sv.shift_id),sv.start_time||' - '||sv.end_time,sv.shift_version_id
FROM demo_cases c JOIN demo_people p ON p.patient_id=c.patient_id
JOIN shift_version sv ON c.started::time>=sv.start_time AND (c.started::time<sv.end_time OR sv.end_time=time '23:59:59');
INSERT INTO appointment_services(appointment_id,service_id)
SELECT pg_temp.did('appointment:'||key),format('400000%s-0000-0000-0000-%s',
 lpad(service_no::text,2,'0'),lpad(service_no::text,12,'0'))::uuid FROM demo_cases;

INSERT INTO customer_visit(visit_id,created_at,updated_at,deleted,check_in_time,check_out_time,
 status,appointment_id,checked_in_by,customer_id)
SELECT pg_temp.did('visit:'||key),started,started+span,false,started,
 CASE WHEN finished THEN started+span*.9 END,
 CASE WHEN finished THEN 'COMPLETED' WHEN queue_status IN('UNPAID','WAITING','CALLED','SKIPPED')
 THEN 'CHECKED_IN' ELSE 'IN_PROGRESS' END,
 pg_temp.did('appointment:'||key),receptionist,patient_id FROM demo_case_staff WHERE queue_status<>'APPOINTMENT';

INSERT INTO queue_ticket(ticket_id,visit_id,department_id,work_date,queue_number,status,
 called_at,completed_at,service_id,created_at,updated_at,deleted)
SELECT pg_temp.did('queue:'||c.key),pg_temp.did('visit:'||c.key),c.room,c.started::date,
 row_number() OVER(PARTITION BY c.room,c.started::date ORDER BY c.started,c.key),c.queue_status,
 CASE WHEN c.queue_status NOT IN('WAITING','BLOCKED') THEN pg_temp.event(c,.1) END,
 CASE WHEN c.finished THEN pg_temp.event(c,.85) END,
 format('400000%s-0000-0000-0000-%s',lpad(c.service_no::text,2,'0'),lpad(c.service_no::text,12,'0'))::uuid,
 pg_temp.event(c,.05),pg_temp.event(c,.85),false
FROM demo_cases c WHERE c.queue_status NOT IN('APPOINTMENT','UNPAID');

INSERT INTO medical_record(record_id,record_version,record_code,visit_id,queue_ticket_id,doctor_id,
 chief_complaint,clinical_findings,diagnosis,prescription_note,conclusion,patient_instruction,
 status,completed_at,contact_requested,doctor_confirmed_by,doctor_confirmed_at,created_at,updated_at,deleted)
SELECT pg_temp.did('record:'||c.key),0,'MR-'||to_char(c.started,'YYYYMMDD')||'-'||c.key,
 pg_temp.did('visit:'||c.key),pg_temp.did('queue:'||c.key),d.head_doctor_id,
 CASE c.service_no WHEN 3 THEN 'Khám kiểm tra sức khỏe và tư vấn sinh hoạt cho trẻ.'
 WHEN 5 THEN 'Vết trầy cẳng chân đang theo dõi, cần đánh giá trước chăm sóc.'
 WHEN 4 THEN 'Khám kiểm tra da, tư vấn chăm sóc da.'
 WHEN 7 THEN 'Khám kiểm tra và tư vấn sức khỏe phụ khoa.'
 WHEN 6 THEN 'Kiểm tra sức khỏe tim mạch định kỳ.'
 ELSE 'Khám kiểm tra sức khỏe, tư vấn chế độ sinh hoạt.' END,
 CASE WHEN c.finished OR c.test_code IS NOT NULL THEN 'Tỉnh táo, tiếp xúc tốt. Khám tại thời điểm đánh giá chưa ghi nhận bất thường rõ.' END,
 CASE WHEN c.finished THEN 'Khám kiểm tra sức khỏe - Z00.0' END,
 CASE WHEN c.finished THEN 'Không chỉ định thuốc trong lần khám này.' END,
 CASE WHEN c.finished THEN 'Chưa ghi nhận bất thường cần điều trị tại thời điểm khám.' END,
 CASE WHEN c.finished THEN 'Duy trì sinh hoạt phù hợp; khám lại khi xuất hiện triệu chứng bất thường.' END,
 CASE WHEN c.finished THEN 'COMPLETED' ELSE 'IN_PROGRESS' END,
 CASE WHEN c.finished THEN pg_temp.event(c,.85) END,false,
 CASE WHEN c.finished THEN d.head_doctor_id END,CASE WHEN c.finished THEN pg_temp.event(c,.85) END,
 pg_temp.event(c,.1),pg_temp.event(c,.85),false
FROM demo_cases c JOIN department d ON d.department_id=c.room
WHERE c.queue_status IN('DONE','IN_PROGRESS','WAITING_FOR_TEST','TEST_DONE');

INSERT INTO vital_signs(vital_id,medical_record_id,blood_pressure,heart_rate,temperature,
 height,weight,recorded_at,recorded_by,created_at,updated_at,deleted)
SELECT pg_temp.did('vital:'||c.key),mr.record_id,CASE WHEN p.i IN(9,10,21) THEN '100/65' ELSE '118/76' END,
 CASE WHEN p.i IN(9,10,21) THEN 90 ELSE 74 END,36.7,
 pp.height,pp.weight,pg_temp.event(c,.12),mr.doctor_id,pg_temp.event(c,.12),pg_temp.event(c,.12),false
FROM demo_cases c JOIN medical_record mr ON mr.record_id=pg_temp.did('record:'||c.key)
JOIN demo_people p ON p.patient_id=c.patient_id JOIN profile pp ON pp.profile_id=p.patient_id;
UPDATE medical_record m SET vital_signs_id=v.vital_id FROM vital_signs v WHERE v.medical_record_id=m.record_id;
-- Đánh giá chung theo submitFeedback: không thêm FeedbackTarget hoặc điểm riêng.
UPDATE medical_record m SET rating_score=CASE WHEN right(c.key,1) IN('0','5') THEN 4 ELSE 5 END,
 rated_at=pg_temp.event(c,.95),feedback_status=CASE WHEN right(c.key,1) IN('0','2','4','6','8') THEN 'RESPONDED' ELSE 'NEW' END,
 rating_comment='Bác sĩ '||p.full_name||' '||
 (ARRAY['giải thích rõ nội dung khám và dặn dò dễ hiểu.',
 'tư vấn cẩn thận, trả lời đầy đủ thắc mắc của tôi.',
 'hướng dẫn theo dõi sức khỏe tại nhà cụ thể.',
 'trao đổi nhẹ nhàng; tôi mong thời gian chờ được rút ngắn.',
 'giải thích kết quả rõ ràng, quy trình thanh toán thuận tiện.'])[1+abs(hashtext(c.key)::bigint%5)::int] ||
 CASE WHEN c.nurse IS NOT NULL THEN ' Điều dưỡng '||np.full_name||' hỗ trợ chu đáo tại '||nd.name||'.' ELSE '' END,
 manager_response=CASE WHEN right(c.key,1) IN('0','2','4','6','8') THEN
 'CareS cảm ơn góp ý của bạn. Phòng khám đã ghi nhận để cải thiện chất lượng phục vụ.' END,
 responded_at=CASE WHEN right(c.key,1) IN('0','2','4','6','8') THEN c.started+c.span END,
 responded_by=CASE WHEN right(c.key,1) IN('0','2','4','6','8') THEN '90000009-2222-2222-2222-222222222222'::uuid END
FROM demo_cases c JOIN staff_info si ON si.staff_id=(SELECT head_doctor_id FROM department WHERE department_id=c.room)
JOIN profile p ON p.profile_id=si.profile_id
LEFT JOIN staff_info ns ON ns.staff_id=c.nurse LEFT JOIN profile np ON np.profile_id=ns.profile_id
LEFT JOIN department nd ON nd.department_id=ns.department_id
JOIN demo_people dp ON dp.patient_id=c.patient_id
WHERE m.record_id=pg_temp.did('record:'||c.key) AND c.historical AND dp.account_id IS NOT NULL;

-- Tổng tiền tính từ dòng dịch vụ và quyền lợi BHYT mô phỏng.
CREATE TEMP TABLE demo_items ON COMMIT DROP AS
SELECT c.key,'initial'::text bucket,pg_temp.did('item:exam:'||c.key) id,
 format('400000%s-0000-0000-0000-%s',lpad(c.service_no::text,2,'0'),lpad(c.service_no::text,12,'0'))::uuid service_id,
 CASE WHEN c.key IN('H03','T12') THEN 20 ELSE 0 END insurance_percent
FROM demo_cases c WHERE c.queue_status<>'APPOINTMENT';
INSERT INTO demo_items
SELECT c.key,'ordered',pg_temp.did('item:test:'||c.key),ms.service_id,0
FROM demo_cases c JOIN medical_service ms ON ms.service_code=c.test_code;
INSERT INTO demo_items
SELECT c.key,'initial',pg_temp.did('item:next:'||c.key),
 format('400000%s-0000-0000-0000-%s',lpad(x.svc::text,2,'0'),lpad(x.svc::text,12,'0'))::uuid,0
FROM demo_cases c JOIN(VALUES('H01',25),('T07',2),('T08',6),('T09',25))x(key,svc) ON x.key=c.key;

INSERT INTO invoice(invoice_id,invoice_code,customer_id,visit_id,medical_record_id,issue_date,due_date,
 subtotal,discount,tax,total_amount,paid_amount,status,note,issued_by,created_at,updated_at,deleted)
SELECT pg_temp.did('invoice:'||it.bucket||':'||c.key),'INV-'||to_char(c.started,'YYYYMMDD')||'-'||c.key||'-'||it.bucket,
 c.patient_id,pg_temp.did('visit:'||c.key),CASE WHEN it.bucket='ordered' THEN pg_temp.did('record:'||c.key) END,
 c.started+c.span*CASE WHEN it.bucket='ordered' THEN .2 ELSE .02 END,
 c.started::date,sum(ms.price),sum(round(ms.price*it.insurance_percent/100,2)),0,
 sum(ms.price-round(ms.price*it.insurance_percent/100,2)),
 CASE WHEN c.queue_status='UNPAID' OR (it.bucket='ordered' AND c.test_state='UNPAID') THEN 0
 ELSE sum(ms.price-round(ms.price*it.insurance_percent/100,2)) END,
 CASE WHEN c.queue_status='UNPAID' OR (it.bucket='ordered' AND c.test_state='UNPAID') THEN 'PENDING' ELSE 'PAID' END,
 CASE WHEN it.bucket='ordered' THEN 'Chỉ định sau khám' ELSE 'Dịch vụ tiếp nhận ban đầu' END,
 c.cashier,c.started,c.started+c.span,false
FROM demo_items it JOIN demo_case_staff c ON c.key=it.key JOIN medical_service ms ON ms.service_id=it.service_id
GROUP BY c.key,c.patient_id,c.started,c.span,c.queue_status,c.test_state,c.cashier,it.bucket;
INSERT INTO invoice_item(item_id,invoice_id,service_id,service_snapshot,service_code_snapshot,unit_price,
 quantity,discount_percent,discount_amount,final_price,line_total,bhyt_fund,note,created_at,updated_at,deleted)
SELECT it.id,pg_temp.did('invoice:'||it.bucket||':'||it.key),ms.service_id,ms.name,ms.service_code,
 ms.price,1,0,0,ms.price-round(ms.price*it.insurance_percent/100,2),
 ms.price-round(ms.price*it.insurance_percent/100,2),round(ms.price*it.insurance_percent/100,2),
 CASE WHEN it.insurance_percent>0 THEN 'BHYT mô phỏng đã xác minh, mức hỗ trợ theo cấu hình demo' ELSE 'Người bệnh tự chi trả' END,
 inv.issue_date,inv.issue_date,false
FROM demo_items it JOIN medical_service ms ON ms.service_id=it.service_id
JOIN invoice inv ON inv.invoice_id=pg_temp.did('invoice:'||it.bucket||':'||it.key);
UPDATE profile SET insurance_id='DEMO-BHYT-000003' WHERE profile_id=(SELECT patient_id FROM demo_people WHERE i=3);
-- T12: CareS giảm 15% trên phần bệnh nhân trả sau BHYT, không sửa InvoiceItem.
UPDATE invoice SET discount=discount+round(total_amount*.15,2),
 total_amount=round(total_amount*.85,2),paid_amount=round(total_amount*.85,2)
WHERE invoice_id=pg_temp.did('invoice:initial:T12');
INSERT INTO payment_transaction(transaction_id,invoice_id,transaction_code,amount,payment_method,status,
 paid_at,gateway_reference,note,received_by,created_at,updated_at,deleted)
SELECT pg_temp.did('payment:'||invoice_id),invoice_id,'PAY-'||left(replace(invoice_id::text,'-',''),20),
 paid_amount,CASE WHEN invoice_id=pg_temp.did('invoice:initial:T12') THEN 'MEMBERSHIP_CARD'
 WHEN right(invoice_code,2)='ed' THEN 'BANK_TRANSFER' ELSE 'CASH' END,
 'SUCCESS',issue_date,
 NULL,'Giao dịch mô phỏng, không chuyển tiền thực tế',issued_by,issue_date,issue_date,false
FROM invoice WHERE status='PAID';
INSERT INTO membership_card(card_id,card_code,owner_profile_id,status,balance,pin_hash,
 benefit_percent,activated_at,benefit_starts_at,benefit_expires_at,version,created_at,updated_at,deleted)
SELECT pg_temp.did('card:T12'),'CS-DEMO-0003',customer_id,'ACTIVE',1000000-total_amount,
 '$2a$10$fl4JcFQUApjxYX0D1xqsmOpZ761fONcBHLozxvF85tUkybPyRFXKq',15,
 pg_temp.demo_date()-10+time '06:30',pg_temp.demo_date()-9,pg_temp.demo_date()+365+time '06:30',2,
 pg_temp.demo_date()-10+time '06:30',pg_temp.demo_now(),false
FROM invoice WHERE invoice_id=pg_temp.did('invoice:initial:T12');
INSERT INTO membership_card_ledger(ledger_id,card_id,type,amount,balance_before,balance_after,
 benefit_discount,source_payment_method,idempotency_key,reference_code,patient_profile_id,performed_by,
 created_at,updated_at,deleted)
SELECT pg_temp.did('topup:T12'),card_id,'TOP_UP',1000000,0,1000000,0,'CASH','DEMO-TOPUP-T12','CS-TOPUP-T12',
 owner_profile_id,'90000013-6666-6666-6666-666666666666',created_at,created_at,false FROM membership_card;
INSERT INTO membership_card_ledger(ledger_id,card_id,type,amount,balance_before,balance_after,benefit_discount,
 source_payment_method,idempotency_key,reference_code,invoice_id,patient_profile_id,payment_transaction_id,
 performed_by,created_at,updated_at,deleted)
SELECT pg_temp.did('cardpayment:T12'),mc.card_id,'PAYMENT',inv.total_amount,1000000,mc.balance,
 inv.discount-(SELECT sum(bhyt_fund) FROM invoice_item WHERE invoice_id=inv.invoice_id),
 'MEMBERSHIP_CARD','DEMO-PAYMENT-T12','CS-PAYMENT-T12',inv.invoice_id,inv.customer_id,pt.transaction_id,
 inv.issued_by,pt.paid_at,pt.paid_at,false
FROM membership_card mc JOIN invoice inv ON inv.invoice_id=pg_temp.did('invoice:initial:T12')
JOIN payment_transaction pt ON pt.invoice_id=inv.invoice_id;

-- Các dịch vụ khám tiếp theo chưa bắt đầu không có bệnh án.
INSERT INTO queue_ticket(ticket_id,visit_id,department_id,work_date,queue_number,status,service_id,
 created_at,updated_at,deleted)
SELECT pg_temp.did('queue:next:'||c.key),pg_temp.did('visit:'||c.key),c.room,c.started::date,
 100+row_number() OVER(PARTITION BY c.room,c.started::date ORDER BY c.key),
 CASE WHEN c.key='H01' THEN 'SKIPPED' ELSE 'BLOCKED' END,it.service_id,
 pg_temp.event(c,.06),pg_temp.event(c,.9),false
FROM demo_cases c JOIN demo_items it ON it.id=pg_temp.did('item:next:'||c.key);

INSERT INTO queue_ticket(ticket_id,visit_id,department_id,work_date,queue_number,status,called_at,completed_at,
 service_id,created_at,updated_at,deleted)
SELECT pg_temp.did('queue:test:'||c.key),pg_temp.did('visit:'||c.key),c.test_room,c.started::date,
 row_number() OVER(PARTITION BY c.test_room,c.started::date ORDER BY c.started,c.key),
 CASE WHEN c.test_state='COMPLETED' THEN 'DONE' ELSE 'WAITING' END,
 CASE WHEN c.test_state='COMPLETED' THEN pg_temp.event(c,.35) END,
 CASE WHEN c.test_state='COMPLETED' THEN pg_temp.event(c,.7) END,
 ms.service_id,pg_temp.event(c,.25),pg_temp.event(c,.7),false
FROM demo_cases c JOIN medical_service ms ON ms.service_code=c.test_code WHERE c.test_state<>'UNPAID';
INSERT INTO test_request(test_request_id,medical_record_id,service_id,performing_department,queue_ticket_id,
 description,status,requested_by,completed_at,performed_at,invoice_item_id,created_at,updated_at,deleted)
SELECT pg_temp.did('test:'||c.key),pg_temp.did('record:'||c.key),q.service_id,c.test_room,q.ticket_id,
 'Chỉ định '||ms.name||' để bổ sung thông tin đánh giá sức khỏe.',
 c.test_state,m.doctor_id,CASE WHEN c.test_state='COMPLETED' THEN pg_temp.event(c,.7) END,
 CASE WHEN c.test_state='COMPLETED' THEN pg_temp.event(c,.35) END,
 pg_temp.did('item:test:'||c.key),pg_temp.event(c,.25),pg_temp.event(c,.7),false
FROM demo_cases c JOIN queue_ticket q ON q.ticket_id=pg_temp.did('queue:test:'||c.key)
JOIN medical_record m ON m.record_id=pg_temp.did('record:'||c.key)
JOIN medical_service ms ON ms.service_id=q.service_id;

CREATE TEMP TABLE demo_result_values(code text PRIMARY KEY,data jsonb) ON COMMIT DROP;
INSERT INTO demo_result_values VALUES
('LAB-001','{"rbc":4.6,"hgb":138,"hct":41.4,"mcv":90,"mch":30,"mchc":333.3,
 "rdwCv":12.5,"rdwSd":42,"wbc":6,"neutPercent":55,"lymphPercent":35,
 "monoPercent":6,"eosPercent":3,"basoPercent":1,"neutAbsolute":3.3,"lymphAbsolute":2.1,
 "monoAbsolute":0.36,"eosAbsolute":0.18,"basoAbsolute":0.06,"plt":250,"mpv":9.6,"pdw":12.2,
 "pct":0.24,"pLcr":25}'),
('LAB-002','{"glucoseContext":"RANDOM","glucoseQualifier":"EQUAL","glucose":5.2}'),
('IMG-001','{"lesionDescription":"Hai phế trường sáng, không thấy tổn thương khu trú. Bóng tim không to.",
 "imagingConclusion":"Chưa ghi nhận bất thường trên phim ngực."}'),
('IMG-003','{"liverDescription":"Kích thước và nhu mô chưa ghi nhận bất thường.",
 "gallbladderDescription":"Thành không dày, chưa thấy sỏi.",
 "kidneyDescription":"Hai thận không ứ nước, chưa thấy sỏi.",
 "abnormalFinding":"Chưa ghi nhận bất thường rõ.",
 "imagingConclusion":"Chưa ghi nhận bất thường rõ trên siêu âm ổ bụng."}');
INSERT INTO test_result(result_id,test_request_id,conclusion,sample_id,sample_type,sample_status,
 collected_at,collected_by,performed_by,performed_at,verified_by,verified_at,form_template_version_id,
 result_data,created_at,updated_at,deleted)
SELECT pg_temp.did('result:'||c.key),tr.test_request_id,
 CASE WHEN c.test_code LIKE 'LAB%' THEN 'Các chỉ số đã thực hiện trong khoảng tham chiếu của biểu mẫu demo.'
 ELSE 'Chưa ghi nhận bất thường rõ tại thời điểm khảo sát.' END,
 CASE WHEN c.test_code LIKE 'LAB%' THEN 'SMP-'||to_char(c.started,'YYYYMMDD')||'-'||upper(left(tr.test_request_id::text,8)) END,
 CASE WHEN c.test_code LIKE 'LAB%' THEN 'BLOOD' END,CASE WHEN c.test_code LIKE 'LAB%' THEN 'ACCEPTED' END,
 CASE WHEN c.test_code LIKE 'LAB%' THEN pg_temp.event(c,.35) END,
 CASE WHEN c.test_code LIKE 'LAB%' THEN COALESCE(c.nurse,d.head_doctor_id) END,
 CASE WHEN c.test_code LIKE 'IMG%' THEN COALESCE(c.nurse,d.head_doctor_id) ELSE d.head_doctor_id END,
 pg_temp.event(c,.4),d.head_doctor_id,pg_temp.event(c,.7),tv.version_id,
 v.data||'{"_meta":{"completionStatus":"COMPLETE"},"_omissions":{}}'::jsonb,
 pg_temp.event(c,.4),pg_temp.event(c,.7),false
FROM demo_cases c JOIN test_request tr ON tr.test_request_id=pg_temp.did('test:'||c.key)
JOIN department d ON d.department_id=c.test_room JOIN demo_result_values v ON v.code=c.test_code
JOIN medical_service_form_template b ON b.service_id=tr.service_id AND NOT b.deleted
JOIN LATERAL(SELECT * FROM clinical_form_template_version tv WHERE tv.template_id=b.template_id
 AND NOT tv.deleted AND tv.status='PUBLISHED' AND tv.effective_from<=c.started::date
 ORDER BY tv.version_no DESC LIMIT 1)tv ON true WHERE c.test_state='COMPLETED';
INSERT INTO test_result_revision(revision_id,result_id,revision_no,status,result_data,conclusion,
 template_version_id,entered_by,signed_by,signed_at,created_at,updated_at,deleted)
SELECT pg_temp.did('revision:'||result_id),result_id,1,'SIGNED',result_data,conclusion,
 form_template_version_id,performed_by,verified_by,verified_at,created_at,updated_at,false FROM test_result;
INSERT INTO icd_10_selections(selection_id,record_id,code,code_name,created_at,updated_at,deleted)
SELECT pg_temp.did('icd:'||record_id),record_id,'Z00.0','Khám sức khỏe tổng quát',completed_at,completed_at,false
FROM medical_record WHERE status='COMPLETED';

-- 14 ngày tới: mỗi ngày một lịch, không trùng lượt đang hoạt động hôm nay.
INSERT INTO appointment(appointment_id,created_at,updated_at,deleted,scheduled_at,status,
 is_guest,customer_id,shift_name,shift_time,shift_version_id)
SELECT pg_temp.did('future:'||g.i),pg_temp.demo_now(),pg_temp.demo_now(),false,
 pg_temp.demo_date()+g.i+time '08:00','PENDING',false,p.patient_id,'Ca Chiều','08:00 - 16:00',
 '71000002-2222-2222-2222-222222222222'::uuid
FROM generate_series(1,14)g(i) JOIN demo_people p ON p.i=14+(g.i-1)%7;
INSERT INTO appointment_services(appointment_id,service_id)
SELECT pg_temp.did('future:'||i),'40000002-0000-0000-0000-000000000002'::uuid FROM generate_series(1,14)g(i);
INSERT INTO notification(notification_id,recipient_id,title,content,notification_type,channel,status,
 related_entity,related_entity_id,sent_at,created_at,updated_at,deleted)
SELECT pg_temp.did('return:'||p.profile_id),p.profile_id,'Khách báo đã quay lại',
 'Trần Gia Hân - VIS-'||upper(left(pg_temp.did('visit:T11')::text,8))||
 ' đã báo quay lại. Vui lòng xác nhận có mặt tại quầy.',
 'GENERAL','IN_APP','PENDING','QUEUE_RETURN_REQUEST',pg_temp.did('queue:T11'),
 pg_temp.demo_now(),pg_temp.demo_now(),pg_temp.demo_now(),false
FROM profile p JOIN staff_info s ON s.profile_id=p.profile_id WHERE s.system_role IN('RECEPTIONIST','CLINIC_MANAGER');

INSERT INTO clinic_information(clinic_information_id,created_at,updated_at,deleted,clinic_name,
 legal_name,tax_code,operating_license,short_description,support_email,phone,address)
VALUES('00000000-0000-0000-0000-000000000100',pg_temp.demo_now(),pg_temp.demo_now(),false,
 'Phòng khám CareS — mô phỏng','CareS Demo (không phải cơ sở y tế thực tế)',
 'DEMO-NOT-LEGAL','DEMO-NOT-LICENSED',
 'Hệ thống trình diễn. Danh tính, số đo, giá và thông tin pháp lý đều là giả lập.',
 'cares-demo@example.com','0909000099','Khu trình diễn CareS, Hà Nội (địa chỉ giả lập)');
INSERT INTO public_announcement(announcement_id,created_at,updated_at,deleted,title,content,published,
 starts_at,ends_at,created_by_account_id)
VALUES(pg_temp.did('announcement'),pg_temp.demo_now(),pg_temp.demo_now(),false,'Môi trường trình diễn CareS',
 'Dữ liệu cá nhân và chuyên môn là giả lập. Ba ca liên tục chỉ phục vụ trình diễn, không phải lịch lao động thực tế.',
 true,pg_temp.demo_date(),pg_temp.demo_date()+interval '15 days','30000013-3333-3333-3333-333333333333');

-- Kiểm tra bắt buộc: lỗi sẽ hủy transaction, giữ nguyên dữ liệu trước reset.
DO $validate$
BEGIN
 IF (SELECT count(*) FROM demo_people)<>24 OR (SELECT count(*) FROM demo_cases WHERE NOT historical)<>12 THEN
  RAISE EXCEPTION 'Sai số hồ sơ/kịch bản'; END IF;
 IF EXISTS(SELECT 1 FROM account a JOIN profile p ON p.account_id=a.account_id
 WHERE a.role='CUSTOMER' AND NOT EXISTS(
 SELECT 1 FROM customer_visit v JOIN medical_record m ON m.visit_id=v.visit_id
 JOIN invoice i ON i.visit_id=v.visit_id JOIN payment_transaction t ON t.invoice_id=i.invoice_id
 WHERE v.customer_id=p.profile_id AND v.status='COMPLETED' AND v.check_out_time<pg_temp.demo_date()
 AND m.status='COMPLETED' AND m.rating_score IS NOT NULL AND i.status='PAID'
 AND t.status='SUCCESS' AND t.amount=i.paid_amount)) THEN
 RAISE EXCEPTION 'Customer thiếu lịch sử/đánh giá/thanh toán'; END IF;
 IF EXISTS(SELECT 1 FROM staff_info s JOIN profile p ON p.profile_id=s.profile_id
 JOIN account a ON a.account_id=p.account_id
 WHERE a.is_active AND s.system_role IN('DOCTOR','NURSE') AND NOT EXISTS(
 SELECT 1 FROM medical_record m WHERE m.rating_score IS NOT NULL AND
 (m.doctor_id=s.staff_id OR EXISTS(SELECT 1 FROM test_request tr JOIN test_result r ON r.test_request_id=tr.test_request_id
 WHERE tr.medical_record_id=m.record_id AND s.staff_id IN(r.collected_by,r.performed_by,r.verified_by))))) THEN
 RAISE EXCEPTION 'Bác sĩ/điều dưỡng thiếu tham gia lịch sử có đánh giá'; END IF;
 IF EXISTS(SELECT 1 FROM medical_record m JOIN customer_visit v ON v.visit_id=m.visit_id
 JOIN profile p ON p.profile_id=v.customer_id WHERE m.rating_score IS NOT NULL AND
 (p.account_id IS NULL OR m.status<>'COMPLETED' OR m.rated_at<m.completed_at OR
 m.responded_at<m.rated_at OR m.doctor_rating IS NOT NULL OR m.staff_rating IS NOT NULL OR m.waiting_rating IS NOT NULL)) THEN
 RAISE EXCEPTION 'Đánh giá sai thời gian/quyền/cơ chế'; END IF;
 IF EXISTS(SELECT 1 FROM feedback_target) THEN RAISE EXCEPTION 'Không được seed FeedbackTarget'; END IF;
 IF EXISTS(SELECT 1 FROM appointment a JOIN appointment_services aps ON aps.appointment_id=a.appointment_id
 JOIN medical_service ms ON ms.service_id=aps.service_id
 JOIN profile p ON p.profile_id=COALESCE(a.customer_id,(SELECT profile_id FROM profile WHERE phone=a.guest_phone))
 WHERE p.date_of_birth>a.scheduled_at::date
 OR extract(year FROM age(a.scheduled_at,p.date_of_birth))<COALESCE(ms.minimum_age,0)
 OR extract(year FROM age(a.scheduled_at,p.date_of_birth))>COALESCE(ms.maximum_age,200)
 OR (ms.allowed_gender IS NOT NULL AND ms.allowed_gender<>p.gender)) THEN
 RAISE EXCEPTION 'Tuổi/giới tính không phù hợp dịch vụ đặt'; END IF;
 IF EXISTS(SELECT 1 FROM invoice i WHERE i.subtotal<>(SELECT sum(unit_price*quantity) FROM invoice_item WHERE invoice_id=i.invoice_id)
 OR i.total_amount<>i.subtotal-i.discount+i.tax
 OR (i.status='PAID' AND (i.paid_amount<>i.total_amount OR i.paid_amount<>
 (SELECT COALESCE(sum(t.amount),0) FROM payment_transaction t WHERE t.invoice_id=i.invoice_id AND t.status='SUCCESS')))) THEN
 RAISE EXCEPTION 'Sai đối soát hóa đơn/giao dịch'; END IF;
 IF EXISTS(SELECT 1 FROM test_request t JOIN invoice_item it ON it.item_id=t.invoice_item_id
 JOIN invoice i ON i.invoice_id=it.invoice_id WHERE i.status<>'PAID') THEN
 RAISE EXCEPTION 'Xét nghiệm chưa trả tiền đã vào hàng chờ'; END IF;
 IF EXISTS(SELECT 1 FROM test_request t WHERE t.status='COMPLETED' AND NOT EXISTS(
 SELECT 1 FROM test_result r JOIN test_result_revision rev ON rev.result_id=r.result_id
 WHERE r.test_request_id=t.test_request_id AND rev.status='SIGNED' AND rev.signed_by IS NOT NULL
 AND rev.template_version_id IS NOT NULL AND rev.signed_at<=t.completed_at)) THEN
 RAISE EXCEPTION 'CLS hoàn thành thiếu phiên bản đã ký'; END IF;
 IF EXISTS(SELECT 1 FROM medical_record m JOIN queue_ticket q ON q.ticket_id=m.queue_ticket_id
 WHERE q.status IN('WAITING','CALLED','BLOCKED','SKIPPED')) THEN
 RAISE EXCEPTION 'Đã tạo bệnh án cho dịch vụ chưa bắt đầu'; END IF;
 IF EXISTS(SELECT 1 FROM demo_cases c JOIN queue_ticket q ON q.visit_id=pg_temp.did('visit:'||c.key)
 WHERE NOT c.historical AND q.work_date<>pg_temp.demo_date()) THEN RAISE EXCEPTION 'Phiếu hôm nay lệch ngày'; END IF;
 IF EXISTS(SELECT customer_id FROM customer_visit WHERE status IN('IN_PROGRESS','CHECKED_IN')
 GROUP BY customer_id HAVING count(*)>1) THEN RAISE EXCEPTION 'Hai VIS đang hoạt động cho một người'; END IF;
 IF EXISTS(SELECT room_code FROM department d WHERE NOT EXISTS(SELECT 1 FROM staff_info s WHERE s.staff_id=d.head_doctor_id AND s.system_role='DOCTOR'))
 THEN RAISE EXCEPTION 'Phòng thiếu bác sĩ'; END IF;
 IF EXISTS(SELECT 1 FROM test_result r JOIN test_request t ON t.test_request_id=r.test_request_id
 CROSS JOIN LATERAL(VALUES(r.collected_by,r.collected_at),(r.performed_by,r.performed_at),(r.verified_by,r.verified_at))actor(id,at)
 WHERE actor.id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM staff_schedule ss JOIN staff_info s ON s.staff_id=ss.staff_id
 JOIN shift_version sv ON sv.shift_version_id=ss.shift_version_id WHERE ss.staff_id=actor.id
 AND s.department_id=t.performing_department AND ss.work_date=actor.at::date
 AND actor.at::time>=sv.start_time AND (actor.at::time<sv.end_time OR sv.end_time=time '23:59:59') AND NOT ss.deleted)) THEN
 RAISE EXCEPTION 'Nhân sự CLS sai phòng hoặc ca'; END IF;
 IF EXISTS(SELECT 1 FROM test_result r JOIN clinical_form_template_version v ON v.version_id=r.form_template_version_id
 CROSS JOIN LATERAL jsonb_array_elements(v.schema_json->'fields') f
 WHERE COALESCE((f->>'requiredOnSign')::boolean,(f->>'required')::boolean,false)
 AND (NOT r.result_data ? (f->>'key') OR r.result_data->(f->>'key')='null'::jsonb)) THEN
 RAISE EXCEPTION 'Kết quả đã ký thiếu trường bắt buộc'; END IF;
 IF EXISTS(SELECT 1 FROM test_result r JOIN clinical_form_template_version v ON v.version_id=r.form_template_version_id
 CROSS JOIN LATERAL jsonb_each(r.result_data) kv WHERE left(kv.key,1)<>'_'
 AND NOT EXISTS(SELECT 1 FROM jsonb_array_elements(v.schema_json->'fields') f WHERE f->>'key'=kv.key)) THEN
 RAISE EXCEPTION 'Kết quả chứa trường không thuộc phiên bản biểu mẫu'; END IF;
 IF EXISTS(SELECT 1 FROM test_result r JOIN clinical_form_template_version v ON v.version_id=r.form_template_version_id
 WHERE v.published_at>r.verified_at OR r.collected_at>r.performed_at OR r.performed_at>r.verified_at) THEN
 RAISE EXCEPTION 'Sai trình tự công bố mẫu/lấy mẫu/nhập/ký'; END IF;
 IF EXISTS(SELECT 1 FROM demo_case_staff WHERE receptionist IS NULL OR cashier IS NULL) THEN
 RAISE EXCEPTION 'Thiếu người tiếp nhận hoặc thu ngân đúng ca'; END IF;
 IF EXISTS(SELECT staff_id,work_date,shift_id FROM staff_schedule WHERE NOT deleted
 GROUP BY staff_id,work_date,shift_id HAVING count(*)>1) THEN
 RAISE EXCEPTION 'Trùng lịch trực'; END IF;
 IF EXISTS(SELECT left(visit_id::text,8) FROM customer_visit GROUP BY left(visit_id::text,8) HAVING count(*)>1) THEN
 RAISE EXCEPTION 'Trùng mã tra cứu VIS'; END IF;
 IF EXISTS(SELECT 1 FROM membership_card_ledger l WHERE balance_after<>balance_before+
 CASE WHEN type='PAYMENT' THEN -amount ELSE amount END) THEN
 RAISE EXCEPTION 'Sai biến động số dư CareS'; END IF;
 IF EXISTS(SELECT 1 FROM invoice i WHERE i.discount<>
 (SELECT COALESCE(sum(bhyt_fund+discount_amount*quantity),0) FROM invoice_item WHERE invoice_id=i.invoice_id)+
 (SELECT COALESCE(sum(benefit_discount),0) FROM membership_card_ledger WHERE invoice_id=i.invoice_id AND type='PAYMENT')) THEN
 RAISE EXCEPTION 'BHYT và ưu đãi CareS sai hoặc bị tính trùng'; END IF;
 IF (SELECT count(*) FROM notification WHERE related_entity='QUEUE_RETURN_REQUEST')<>4 THEN
 RAISE EXCEPTION 'Thiếu yêu cầu quay lại gửi Lễ tân/Clinic Manager'; END IF;
END $validate$;
COMMIT;
