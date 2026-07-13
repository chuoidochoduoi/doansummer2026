-- Dữ liệu mẫu cho hệ thống quản lý khám bệnh
-- Đủ dữ liệu mẫu cho Appointment và Invoice (>=5 bản ghi mỗi bảng)

-- Xóa dữ liệu cũ
TRUNCATE TABLE invoice, payment_transaction, invoice_item, customer_visit, appointment_services, appointment, staff_info, profile, account, medical_service, specialization, service_category, department RESTART IDENTITY CASCADE;

-- ============================
-- Department (3 bản ghi)
-- ============================
INSERT INTO department (department_id, created_at, updated_at, deleted, name, description, department_type) VALUES
('11111111-1111-1111-1111-111111111111', NOW(), NOW(), false, 'Phòng Lễ Tân', 'Tiếp đón và đăng ký bệnh nhân', 'RECEPTION'),
('22222222-2222-2222-2222-222222222222', NOW(), NOW(), false, 'Quầy Thu Ngân', 'Thanh toán hóa đơn', 'CASHIER'),
('33333333-3333-3333-3333-333333333333', NOW(), NOW(), false, 'Khoa Khám Bệnh', 'Khám bệnh tổng quát', 'EXAMINATION'),
('44444444-4444-4444-4444-444444444444', NOW(), NOW(), false, 'Khoa Xét Nghiệm', 'Xét nghiệm máu, siêu âm', 'LABORATORY');

-- ============================
-- ServiceCategory (3 bản ghi)
-- ============================
INSERT INTO service_category (category_id, created_at, updated_at, deleted, name, description) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', NOW(), NOW(), false, 'Khám bệnh', 'Các gói khám bệnh'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', NOW(), NOW(), false, 'Xét nghiệm', 'Xét nghiệm cơ bản'),
('cccccccc-cccc-cccc-cccc-cccccccccccc', NOW(), NOW(), false, 'Chẩn đoán hình ảnh', 'X-quang, CT, MRI');

-- ============================
-- Specialization (3 bản ghi)
-- ============================
INSERT INTO specialization (specialization_id, created_at, updated_at, deleted, name, description) VALUES
('dddddddd-dddd-dddd-dddd-dddddddddddd', NOW(), NOW(), false, 'Bác sĩ Nội tổng quát', 'Chẩn đoán và điều trị bệnh nội'),
('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', NOW(), NOW(), false, 'Bác sĩ Nhi khoa', 'Chẩn đoán bệnh nhi'),
('11111111-1111-1111-1111-111111111122', NOW(), NOW(), false, 'Bác sĩ Chẩn đoán hình ảnh', 'X-quang, CT, MRI');

-- ============================
-- Profile (7 bản ghi) - 3 bệnh nhân + 4 nhân viên
-- ============================
-- Benh nhan
INSERT INTO profile (profile_id, created_at, updated_at, deleted, full_name, date_of_birth, gender, phone, email, address, blood_type) VALUES
('22222222-2222-2222-2222-222222222233', NOW(), NOW(), false, 'Nguyễn Thị Bệnh Nhân 1', '1990-05-15', 'FEMALE', '0912345678', 'patient1@example.com', 'Hà Nội', 'O_POSITIVE'),
('33333333-3333-3333-3333-333333333344', NOW(), NOW(), false, 'Trần Văn Bệnh Nhân 2', '1985-10-20', 'MALE', '0912345679', 'patient2@example.com', 'TP.HCM', 'A_POSITIVE'),
('44444444-4444-4444-4444-444444444455', NOW(), NOW(), false, 'Lê Thị Bệnh Nhân 3', '1995-03-25', 'FEMALE', '0912345680', 'patient3@example.com', 'Đà Nẵng', 'B_POSITIVE');

-- Nhan vien (3 profile)
INSERT INTO profile (profile_id, created_at, updated_at, deleted, full_name, date_of_birth, gender, phone, email, address, blood_type) VALUES
('55555555-5555-5555-5555-555555555567', NOW(), NOW(), false, 'Trần Thị Lễ Tân', '1995-01-10', 'FEMALE', '0987654321', 'reception@example.com', 'Hà Nội', null),
('66666666-6666-6666-6666-666666666678', NOW(), NOW(), false, 'Phạm Văn Thu Ngân', '1992-03-15', 'MALE', '0987654322', 'cashier@example.com', 'TP.HCM', null),
('77777777-7777-7777-7777-777777777789', NOW(), NOW(), false, 'Bác sĩ Nguyễn Khám Bệnh', '1985-06-20', 'MALE', '0987654323', 'doctor@example.com', 'Hà Nội', null),
('88888888-8888-8888-8888-888888888890', NOW(), NOW(), false, 'Y tá Trần Chăm Sóc', '1990-09-25', 'FEMALE', '0987654324', 'nurse@example.com', 'Đà Nẵng', null);

-- ============================
-- Account (7 bản ghi) - 3 bệnh nhân + 4 nhân viên
-- ============================
-- Benh nhan
INSERT INTO account (account_id, created_at, is_active, password_hash, role, username) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', NOW(), true, '$2a$10$hash1', 'PATIENT', 'patient1'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', NOW(), true, '$2a$10$hash2', 'PATIENT', 'patient2'),
('cccccccc-cccc-cccc-cccc-cccccccccccc', NOW(), true, '$2a$10$hash3', 'PATIENT', 'patient3'),
-- Nhan vien
('dddddddd-dddd-dddd-dddd-dddddddddddd', NOW(), true, '$2a$10$hash4', 'RECEPTIONIST', 'receptionist'),
('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', NOW(), true, '$2a$10$hash5', 'CASHIER', 'cashier'),
('11111111-1111-1111-1111-111111111121', NOW(), true, '$2a$10$hash6', 'DOCTOR', 'doctor'),
('22222222-2222-2222-2222-222222222232', NOW(), true, '$2a$10$hash7', 'NURSE', 'nurse');

-- Link profile - account (benh nhan)
UPDATE profile SET account_id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa' WHERE profile_id = '22222222-2222-2222-2222-222222222233';
UPDATE profile SET account_id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb' WHERE profile_id = '33333333-3333-3333-3333-333333333344';
UPDATE profile SET account_id = 'cccccccc-cccc-cccc-cccc-cccccccccccc' WHERE profile_id = '44444444-4444-4444-4444-444444444455';
-- Link profile - account (nhan vien)
UPDATE profile SET account_id = 'dddddddd-dddd-dddd-dddd-dddddddddddd' WHERE profile_id = '55555555-5555-5555-5555-555555555567';
UPDATE profile SET account_id = 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee' WHERE profile_id = '66666666-6666-6666-6666-666666666678';
UPDATE profile SET account_id = '11111111-1111-1111-1111-111111111121' WHERE profile_id = '77777777-7777-7777-7777-777777777789';
UPDATE profile SET account_id = '22222222-2222-2222-2222-222222222232' WHERE profile_id = '88888888-8888-8888-8888-888888888890';

-- ============================
-- MedicalService (3 bản ghi)
-- ============================
INSERT INTO medical_service (service_id, category_id, created_at, updated_at, deleted, description, duration_minutes, is_active, is_point_of_care, name, price, service_type, department_id) VALUES
('88888888-8888-8888-8888-888888888899', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', NOW(), NOW(), false, 'Khám bệnh cơ bản', 30, true, false, 'Khám bệnh tổng quát', 200000, 'CLINICAL_EXAM', '33333333-3333-3333-3333-333333333333'),
('99999999-9999-9999-9999-999999999900', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', NOW(), NOW(), false, 'Xét nghiệm máu cơ bản', 15, true, false, 'Xét nghiệm máu', 150000, 'LAB_TEST', '44444444-4444-4444-4444-444444444444'),
('11111111-1111-1111-1111-111111111122', 'cccccccc-cccc-cccc-cccc-cccccccccccc', NOW(), NOW(), false, 'X-quang ngực thẳng', 20, true, false, 'X-quang ngực', 180000, 'IMAGING', '33333333-3333-3333-3333-333333333333');

-- ============================
-- Appointment (5 bản ghi) - status: PENDING, CHECKED_IN, CANCELLED
-- ============================
INSERT INTO appointment (appointment_id, created_at, updated_at, deleted, scheduled_at, status, is_guest, customer_id, time_slot) VALUES
('22222222-2222-2222-2222-222222222234', NOW(), NOW(), false, '2026-07-15 08:00:00', 'PENDING', false, '22222222-2222-2222-2222-222222222233', 'MORNING'),
('33333333-3333-3333-3333-333333333345', NOW(), NOW(), false, '2026-07-16 09:30:00', 'PENDING', false, '22222222-2222-2222-2222-222222222233', 'MORNING'),
('44444444-4444-4444-4444-444444444456', NOW(), NOW(), false, '2026-07-17 10:00:00', 'CANCELLED', false, '33333333-3333-3333-3333-333333333344', 'MORNING'),
('55555555-5555-5555-5555-555555555567', NOW(), NOW(), false, '2026-07-18 14:00:00', 'PENDING', false, '33333333-3333-3333-3333-333333333344', 'AFTERNOON'),
('66666666-6666-6666-6666-666666666678', NOW(), NOW(), false, '2026-07-19 15:30:00', 'PENDING', true, '44444444-4444-4444-4444-444444444455', 'AFTERNOON');

-- Thêm dịch vụ vào appointment
INSERT INTO appointment_services (appointment_id, service_id) VALUES
('22222222-2222-2222-2222-222222222234', '88888888-8888-8888-8888-888888888899'),
('22222222-2222-2222-2222-222222222234', '99999999-9999-9999-9999-999999999900'),
('33333333-3333-3333-3333-333333333345', '11111111-1111-1111-1111-111111111122'),
('44444444-4444-4444-4444-444444444456', '88888888-8888-8888-8888-888888888899'),
('55555555-5555-5555-5555-555555555567', '99999999-9999-9999-9999-999999999900');

-- ============================
-- CustomerVisit (5 bản ghi)
-- ============================
INSERT INTO customer_visit (visit_id, created_at, updated_at, deleted, check_in_time, status, customer_id, appointment_id) VALUES
('77777777-7777-7777-7777-777777777789', NOW(), NOW(), false, NOW(), 'CHECKED_IN', '22222222-2222-2222-2222-222222222233', '22222222-2222-2222-2222-222222222234'),
('88888888-8888-8888-8888-888888888890', NOW(), NOW(), false, NOW(), 'CHECKED_IN', '22222222-2222-2222-2222-222222222233', '33333333-3333-3333-3333-333333333345'),
('99999999-9999-9999-9999-999999999901', NOW(), NOW(), false, NOW(), 'CHECKED_IN', '33333333-3333-3333-3333-333333333344', '44444444-4444-4444-4444-444444444456'),
('11111111-1111-1111-1111-111111111122', NOW(), NOW(), false, NOW(), 'CHECKED_IN', '33333333-3333-3333-3333-333333333344', '55555555-5555-5555-5555-555555555567'),
('22222222-2222-2222-2222-222222222235', NOW(), NOW(), false, NOW(), 'CHECKED_IN', '44444444-4444-4444-4444-444444444455', '66666666-6666-6666-6666-666666666678');

-- ============================
-- Invoice (5 bản ghi) - status: PENDING, PAID, CANCELLED
-- ============================
INSERT INTO invoice (invoice_id, created_at, updated_at, deleted, discount, due_date, invoice_code, issue_date, note, paid_amount, status, subtotal, tax, total_amount, customer_id, visit_id) VALUES
('33333333-3333-3333-3333-333333333346', NOW(), NOW(), false, 0, NULL, 'INV-20260711-001', NOW()::date, 'Khám + Xét nghiệm', 0, 'PENDING', 350000, 0, 350000, '22222222-2222-2222-2222-222222222233', '77777777-7777-7777-7777-777777777789'),
('44444444-4444-4444-4444-444444444457', NOW(), NOW(), false, 18000, NULL, 'INV-20260711-002', NOW()::date, 'X-quang ngực - có giảm', 0, 'PENDING', 180000, 0, 162000, '22222222-2222-2222-2222-222222222233', '88888888-8888-8888-8888-888888888890'),
('55555555-5555-5555-5555-555555555568', NOW(), NOW(), false, 0, NULL, 'INV-20260711-003', NOW()::date, 'Khám bệnh', 0, 'PENDING', 200000, 0, 200000, '33333333-3333-3333-3333-333333333344', '99999999-9999-9999-9999-999999999901'),
('66666666-6666-6666-6666-666666666679', NOW(), NOW(), false, 0, NULL, 'INV-20260711-004', NOW()::date, 'Xét nghiệm - đã thanh toán', 150000, 'PAID', 150000, 0, 150000, '33333333-3333-3333-3333-333333333344', '11111111-1111-1111-1111-111111111122'),
('77777777-7777-7777-7777-777777777780', NOW(), NOW(), false, 0, NULL, 'INV-20260711-005', NOW()::date, 'Khám bệnh - đã hủy', 0, 'CANCELLED', 200000, 0, 200000, '44444444-4444-4444-4444-444444444455', '22222222-2222-2222-2222-222222222235');

-- ============================
-- InvoiceItem (5 bản ghi)
-- ============================
INSERT INTO invoice_item (item_id, created_at, updated_at, deleted, line_total, note, quantity, service_code_snapshot, service_snapshot, unit_price, invoice_id, service_id) VALUES
('aaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaaaa', NOW(), NOW(), false, 200000, NULL, 1, 'KHB001', 'Khám bệnh tổng quát', 200000, '33333333-3333-3333-3333-333333333346', '88888888-8888-8888-8888-888888888899'),
('bbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbbbb', NOW(), NOW(), false, 150000, NULL, 1, 'XN001', 'Xét nghiệm máu', 150000, '33333333-3333-3333-3333-333333333346', '99999999-9999-9999-9999-999999999900'),
('cccccccc-cccc-cccc-cccc-ccccccccccccccc', NOW(), NOW(), false, 180000, NULL, 1, 'XQ001', 'X-quang ngực', 180000, '44444444-4444-4444-4444-444444444457', '11111111-1111-1111-1111-111111111122'),
('ddddddd-dddd-dddd-dddd-ddddddddddddddd', NOW(), NOW(), false, 200000, NULL, 1, 'KHB001', 'Khám bệnh tổng quát', 200000, '55555555-5555-5555-5555-555555555568', '88888888-8888-8888-8888-888888888899'),
('eeeeeee-eeee-eeee-eeee-eeeeeeeeeeeeeee', NOW(), NOW(), false, 150000, NULL, 1, 'XN001', 'Xét nghiệm máu', 150000, '66666666-6666-6666-6666-666666666679', '99999999-9999-9999-9999-999999999900');

-- ============================
-- StaffInfo (4 bản ghi) - le tan, thu ngan, bac si, y ta
-- ============================
INSERT INTO staff_info (staff_id, created_at, updated_at, deleted, profile_id, department_id, staff_code, system_role, national_id, bank_account, highest_degree, university, license_number, specialization_id) VALUES
('99999999-9999-9999-9999-999999999901', NOW(), NOW(), false, '55555555-5555-5555-5555-555555555567', '11111111-1111-1111-1111-111111111111', 'STF-20260713-LT01', 'RECEPTIONIST', '0123456789', NULL, NULL, NULL, NULL, NULL),
('99999999-9999-9999-9999-999999999902', NOW(), NOW(), false, '66666666-6666-6666-6666-666666666678', '22222222-2222-2222-2222-222222222222', 'STF-20260713-TN01', 'CASHIER', '0123456790', NULL, NULL, NULL, NULL, NULL),
('99999999-9999-9999-9999-999999999903', NOW(), NOW(), false, '77777777-7777-7777-7777-777777777789', '33333333-3333-3333-3333-333333333333', 'STF-20260713-BS01', 'GENERAL_DOCTOR', '0123456791', NULL, 'Bác sĩ đa khoa', NULL, NULL, 'dddddddd-dddd-dddd-dddd-dddddddddddd'),
('99999999-9999-9999-9999-999999999904', NOW(), NOW(), false, '88888888-8888-8888-8888-888888888890', '44444444-4444-4444-4444-444444444444', 'STF-20260713-YT01', 'NURSE', '0123456792', NULL, NULL, NULL, NULL, NULL);

-- ============================
-- QueueTicket (5 bản ghi) - status: WAITING, CALLED, IN_PROGRESS, DONE
-- ============================
INSERT INTO queue_ticket (ticket_id, created_at, updated_at, deleted, work_date, queue_number, status, called_at, completed_at, visit_id, department_id, service_id) VALUES
('11111111-1111-1111-1111-111111111111', NOW(), NOW(), false, NOW()::date, 1, 'WAITING', NULL, NULL, '77777777-7777-7777-7777-777777777789', '44444444-4444-4444-4444-444444444444', '99999999-9999-9999-9999-999999999900'),
('22222222-2222-2222-2222-222222222223', NOW(), NOW(), false, NOW()::date, 2, 'CALLED', NOW(), NULL, '88888888-8888-8888-8888-888888888890', '44444444-4444-4444-4444-444444444444', '99999999-9999-9999-9999-999999999900'),
('33333333-3333-3333-3333-333333333334', NOW(), NOW(), false, NOW()::date, 10, 'DONE', NOW(), NOW(), '99999999-9999-9999-9999-999999999901', '33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111122'),
('44444444-4444-4444-4444-444444444445', NOW(), NOW(), false, NOW()::date, 5, 'WAITING', NULL, NULL, '11111111-1111-1111-1111-111111111122', '33333333-3333-3333-3333-333333333333', '88888888-8888-8888-8888-888888888899'),
('55555555-5555-5555-5555-555555555556', NOW(), NOW(), false, NOW()::date, 6, 'WAITING', NULL, NULL, '22222222-2222-2222-2222-222222222235', '33333333-3333-3333-3333-333333333333', '88888888-8888-8888-8888-888888888899');

-- ============================
-- PaymentTransaction (5 bản ghi)
-- ============================
INSERT INTO payment_transaction (transaction_id, created_at, updated_at, deleted, amount, payment_method, status, transaction_code, paid_at, gateway_reference, note, invoice_id, received_by) VALUES
('88888888-8888-8888-8888-888888888881', NOW(), NOW(), false, 100000, 'CASH', 'SUCCESS', 'TXN-20260711-001', NOW(), NULL, 'Thanh toán góp 1', '33333333-3333-3333-3333-333333333346', NULL),
('99999999-9999-9999-9999-999999999982', NOW(), NOW(), false, 250000, 'BANK_TRANSFER', 'SUCCESS', 'TXN-20260711-002', NOW(), NULL, 'Thanh toán góp 2 - hoàn thành', '33333333-3333-3333-3333-333333333346', NULL),
('11111111-1111-1111-1111-111111111112', NOW(), NOW(), false, 162000, 'CASH', 'SUCCESS', 'TXN-20260711-003', NOW(), NULL, 'Thanh toán hoàn', '44444444-4444-4444-4444-444444444457', NULL),
('22222222-2222-2222-2222-222222222223', NOW(), NOW(), false, 150000, 'CARD', 'SUCCESS', 'TXN-20260711-004', NOW(), NULL, 'Thanh toán thẻ', '66666666-6666-6666-6666-666666666679', NULL),
('33333333-3333-3333-3333-333333333334', NOW(), NOW(), false, 50000, 'CASH', 'FAILED', 'TXN-20260711-005', NULL, NULL, 'Thanh toán thất bại - không đủ tiền', '77777777-7777-7777-7777-777777777780', NULL);