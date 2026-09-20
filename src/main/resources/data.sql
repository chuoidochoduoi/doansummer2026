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

-- Departments
INSERT INTO department (department_id, created_at, updated_at, deleted, room_code, name, status, department_type, specialization_id, description, head_doctor_id) VALUES
('4d7ac047-a699-7a37-b2c0-e8392593e7bc', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'INT-101', 'Phòng khám Nội 1', 'AVAILABLE', 'EXAMINATION', '00000001-1111-1111-1111-111111111111', 'Nội khoa', '8c574ec0-98e1-ad37-3d64-f900c3211b35'),
('1d5dd991-2183-7e47-8540-7b71704b5dff', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'INT-102', 'Phòng khám Nội 2', 'AVAILABLE', 'EXAMINATION', '00000001-1111-1111-1111-111111111111', 'Nội khoa', 'a73ef1f7-a284-a69a-5d55-1f5f2aa58789'),
('b6b019fc-fd4c-a6ce-527b-f928546cef64', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'SUR-201', 'Phòng khám Ngoại 1', 'AVAILABLE', 'EXAMINATION', '00000003-3333-3333-3333-333333333333', 'Ngoại khoa', '222d13de-2292-22ea-85b7-12b87f2c1604'),
('ba0338ee-d868-b5f0-3560-4e4f139794fc', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'SUR-202', 'Phòng khám Ngoại 2', 'AVAILABLE', 'EXAMINATION', '00000003-3333-3333-3333-333333333333', 'Ngoại khoa', 'a195fff6-226e-7492-f75d-1c471eff523f'),
('65034e17-bcc4-7981-c637-7bfcd6dfa331', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'PED-301', 'Phòng khám Nhi', 'AVAILABLE', 'EXAMINATION', '00000002-2222-2222-2222-222222222222', 'Nhi khoa', 'de4d04ed-d9b4-734b-4dd9-7a3048ffdf8a'),
('98d87656-422c-787c-fc4b-6408c00b644c', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'DER-401', 'Phòng khám Da liễu', 'AVAILABLE', 'EXAMINATION', '00000004-4444-4444-4444-444444444444', 'Da liễu', '228d522a-424e-5037-c20e-325c43826a93'),
('177e189b-5501-d8a7-c9e5-32c9c6de8c3f', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'OBG-501', 'Phòng khám Sản phụ khoa', 'AVAILABLE', 'EXAMINATION', '00000008-8888-8888-8888-888888888888', 'Sản phụ khoa', 'c4882884-2185-448d-7d00-a65cfae000bc'),
('760edff5-8292-2100-0b77-7fc8e316e913', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'LAB-601', 'Phòng xét nghiệm', 'AVAILABLE', 'PARACLINICAL', NULL, 'Xét nghiệm', '74cf86f9-d830-e246-7dc3-538a0874779d'),
('41706ccd-49ed-d1e4-3767-9434f57ebe42', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'IMG-701', 'Phòng siêu âm', 'AVAILABLE', 'PARACLINICAL', NULL, 'Siêu âm, điện tim', '6058143b-c4d4-3840-262f-3aa52b388608'),
('4a0a5c21-f9d4-234b-9631-c80fa2db9881', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'XR-702', 'Phòng X-quang', 'AVAILABLE', 'PARACLINICAL', NULL, 'X-quang', 'ceb1dbd4-ee6c-2065-99f8-cd32f616c3f6');
-- Accounts
INSERT INTO account (account_id, created_at, is_active, password_hash, role, username) VALUES
('715becb6-006f-7e78-2b64-3d4558884cbc', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'admin'),
('11b78289-9764-2008-b121-60aa3ebcbf8a', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'clinicmanager'),
('b8c70898-f359-5a82-a271-d7cc9544d309', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'receptionist'),
('fdd9d836-6aa9-ca03-89bb-b5da1a2fe927', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'cashier'),
('f5bb1a27-4825-1522-b4bd-8397f4082fd2', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'doctor_noi1'),
('bff5a26b-829b-1846-a539-521442bfd2bb', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'doctor_noi2'),
('f9762e6f-d85a-92a9-72a2-4622758ccd07', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'doctor_ngoai1'),
('2e7ad114-ff71-61c5-d25b-7f6e2facf04e', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'doctor_ngoai2'),
('bc950434-2208-9cc9-e6c1-ea0bdf80c018', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'doctor_nhi'),
('ee4de539-f1db-2637-626e-18db1828b4d8', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'doctor_dalieu'),
('f2dc2060-3a58-4279-0602-789ef34fbc7b', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'doctor_san'),
('6a02bdd4-6d56-2f89-dc86-1082628a2280', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'doctor_xn'),
('3091825d-92a0-b94a-1f21-9f2cb570a6d0', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'doctor_sa'),
('6de3ec82-c665-0175-ba56-1fee7b13e8e0', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'doctor_xq'),
('dbef007c-7a5c-bb86-24b0-5d591afe1aba', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'nurse_noi1'),
('f711860f-b58c-a180-1466-1b0c66e029e9', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'nurse_noi2'),
('6a23bc3b-8624-454f-9e57-517378fd8547', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'nurse_ngoai1'),
('761f433a-3e5d-c206-467b-ab0c9d3d3895', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'nurse_ngoai2'),
('e1430db0-3cab-a3bd-c744-6d345417b525', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'nurse_nhi'),
('97c0db9a-956d-6173-6176-bb3a0fcc96eb', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'nurse_dalieu'),
('31aadf92-a791-45d9-7845-74d80b7185eb', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'nurse_san'),
('46b1f075-3a66-2350-c411-023fdc6bdcce', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'ktv_xn'),
('e0ce249d-e3c3-e987-9099-b896ec2b6ec1', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'ktv_sa'),
('199ef9e3-631e-b43e-ce55-d073f4bffefd', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'STAFF', 'ktv_xq'),
('a780d597-bf34-b2c4-df00-a72a966f6d4b', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'CUSTOMER', '0987654001'),
('7017c9f1-ded6-46cd-0dd5-950d5c86c100', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'CUSTOMER', '0987654002'),
('b546d6f2-eb3d-cb3b-4b66-c68e6a64ecc9', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'CUSTOMER', '0987654003'),
('e3c9f2b2-1d4a-e8ed-d963-a1abccf2085b', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'CUSTOMER', '0987654004'),
('1ac47263-98dc-ed58-a646-e51a07483f28', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'CUSTOMER', '0987654005'),
('239ef41f-451e-2067-2026-313a1c1d2f3a', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'CUSTOMER', '0987654006'),
('18f88a0a-3d50-8759-e824-d86dca178007', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'CUSTOMER', '0987654007'),
('e251a5dc-687b-2317-31f8-527cce389c44', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'CUSTOMER', '0987654008'),
('6252b4f2-fabe-5949-e56f-c94a9c85cde6', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'CUSTOMER', '0987654009'),
('83ef3d6a-4444-627c-0611-08c23dc83d25', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'CUSTOMER', '0987654010'),
('f8c0fa8e-85a7-c8f4-0a46-46c3d186bef0', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'CUSTOMER', '0987654011'),
('24de58db-86c8-38be-5711-db3ba236026a', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'CUSTOMER', '0987654012'),
('a0073cc4-b720-2968-c468-8027a5622a31', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'CUSTOMER', '0987654013'),
('ee80ca83-b6b5-d41b-fcf4-a200000200d4', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'CUSTOMER', '0987654014'),
('dafcb0a8-e0c2-fc31-5138-83f5e527f6e0', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'CUSTOMER', '0987654015'),
('ceed3b76-9b43-5b53-eafb-33f1076299a8', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'CUSTOMER', '0987654016'),
('2da4d59d-f8be-f54c-9201-40033b27e510', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'CUSTOMER', '0987654017'),
('27ae040b-8920-e65f-3eb9-ec4b9ef8f2a4', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'CUSTOMER', '0987654018'),
('df728b7e-6f20-19c6-b5e3-4b88ee7902fd', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'CUSTOMER', '0987654019'),
('526b88ee-5562-d930-f0c2-2cc9ee7bbce8', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'CUSTOMER', '0987654020'),
('fb587594-8fae-d7b0-dbd2-1b8ff39abdab', (pg_temp.demo_now()-interval '60 days'), true, '$2a$10$j4R7VNxV3mXaMXcrv6PJmu2PsXLq/y1TOJXb2oI0yAF/86Qyy4T9m', 'CUSTOMER', '0987654021');
-- Profiles
INSERT INTO profile (profile_id, account_id, created_at, updated_at, deleted, full_name, date_of_birth, gender, phone, email, address, blood_type) VALUES
('29e9a058-84de-dcd8-f74e-b12b409a7df3', '715becb6-006f-7e78-2b64-3d4558884cbc', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Phạm Đức Minh', '1980-01-01', 'MALE', '0980000000', 'admin@cares.vn', 'Hà Nội', NULL),
('5114e08a-a82c-a581-3b37-6a107fa4a15c', '11b78289-9764-2008-b121-60aa3ebcbf8a', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Nguyễn Thu Hương', '1980-01-01', 'MALE', '0980000001', 'clinicmanager@cares.vn', 'Hà Nội', NULL),
('5c1bb2a3-4cb5-9911-7fbb-aa9b22335b16', 'b8c70898-f359-5a82-a271-d7cc9544d309', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Trịnh Thị Kiều Oanh', '1980-01-01', 'MALE', '0980000002', 'receptionist@cares.vn', 'Hà Nội', NULL),
('9e1e1b02-7c09-4a95-7a22-e3765a1f9f5f', 'fdd9d836-6aa9-ca03-89bb-b5da1a2fe927', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Đinh Văn Quang', '1980-01-01', 'MALE', '0980000003', 'cashier@cares.vn', 'Hà Nội', NULL),
('01f27858-d61c-12d5-13d5-f04475291f93', 'f5bb1a27-4825-1522-b4bd-8397f4082fd2', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'BS. Trần Minh Tuấn', '1980-01-01', 'MALE', '0980000004', 'doctor_noi1@cares.vn', 'Hà Nội', NULL),
('f71108bb-4766-dbe3-cba8-111b419cbfc2', 'bff5a26b-829b-1846-a539-521442bfd2bb', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'BS. Nguyễn Thị Lan Anh', '1980-01-01', 'MALE', '0980000005', 'doctor_noi2@cares.vn', 'Hà Nội', NULL),
('cd122477-83e0-7fd4-7e10-5407f500ed65', 'f9762e6f-d85a-92a9-72a2-4622758ccd07', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'BS. Nguyễn Đức Khoa', '1980-01-01', 'MALE', '0980000006', 'doctor_ngoai1@cares.vn', 'Hà Nội', NULL),
('8e36d38b-f5ea-cc6b-1a93-256d2409ab3f', '2e7ad114-ff71-61c5-d25b-7f6e2facf04e', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'BS. Lê Hoàng Nam', '1980-01-01', 'MALE', '0980000007', 'doctor_ngoai2@cares.vn', 'Hà Nội', NULL),
('a57d8709-bcd2-b391-d484-ca52114831a1', 'bc950434-2208-9cc9-e6c1-ea0bdf80c018', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'BS. Phạm Quốc Bảo', '1980-01-01', 'MALE', '0980000008', 'doctor_nhi@cares.vn', 'Hà Nội', NULL),
('19853712-f455-1465-9937-c31040d73675', 'ee4de539-f1db-2637-626e-18db1828b4d8', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'BS. Hoàng Thị Bích Ngọc', '1980-01-01', 'MALE', '0980000009', 'doctor_dalieu@cares.vn', 'Hà Nội', NULL),
('b945aed4-61fe-c960-dbac-a3f4ff267332', 'f2dc2060-3a58-4279-0602-789ef34fbc7b', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'BS. Đỗ Thị Phương Thảo', '1980-01-01', 'MALE', '0980000010', 'doctor_san@cares.vn', 'Hà Nội', NULL),
('5a9b2cef-cf38-a3fc-0079-7fd2a637a798', '6a02bdd4-6d56-2f89-dc86-1082628a2280', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'BS. Bùi Văn Thành', '1980-01-01', 'MALE', '0980000011', 'doctor_xn@cares.vn', 'Hà Nội', NULL),
('c0343908-daa1-4bf9-6c15-de0b478b90ed', '3091825d-92a0-b94a-1f21-9f2cb570a6d0', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'BS. Võ Đình Phúc', '1980-01-01', 'MALE', '0980000012', 'doctor_sa@cares.vn', 'Hà Nội', NULL),
('1ac31d90-f9d8-d334-1172-06a0c07fadba', '6de3ec82-c665-0175-ba56-1fee7b13e8e0', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'BS. Đoàn Minh Quân', '1980-01-01', 'MALE', '0980000013', 'doctor_xq@cares.vn', 'Hà Nội', NULL),
('2daf6591-2abb-fca4-5a0f-aea35f7659c5', 'dbef007c-7a5c-bb86-24b0-5d591afe1aba', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'ĐD. Lê Thị Hồng Nhung', '1980-01-01', 'MALE', '0980000014', 'nurse_noi1@cares.vn', 'Hà Nội', NULL),
('b2bb4656-fbfe-f2ab-c317-cff23397dfb0', 'f711860f-b58c-a180-1466-1b0c66e029e9', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'ĐD. Phạm Văn Đức', '1980-01-01', 'MALE', '0980000015', 'nurse_noi2@cares.vn', 'Hà Nội', NULL),
('da4a2289-3402-7d38-b947-47e8dbcf3ec2', '6a23bc3b-8624-454f-9e57-517378fd8547', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'ĐD. Vũ Thị Mai Linh', '1980-01-01', 'MALE', '0980000016', 'nurse_ngoai1@cares.vn', 'Hà Nội', NULL),
('59ada81f-ed02-1d6e-1585-4aadc5f0bec1', '761f433a-3e5d-c206-467b-ab0c9d3d3895', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'ĐD. Đặng Thị Thùy Dung', '1980-01-01', 'MALE', '0980000017', 'nurse_ngoai2@cares.vn', 'Hà Nội', NULL),
('900ac2df-0e35-86d4-2e17-85a543999e56', 'e1430db0-3cab-a3bd-c744-6d345417b525', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'ĐD. Nguyễn Thị Thanh Hà', '1980-01-01', 'MALE', '0980000018', 'nurse_nhi@cares.vn', 'Hà Nội', NULL),
('b52d3843-ef31-e545-da31-7b38be3ad19a', '97c0db9a-956d-6173-6176-bb3a0fcc96eb', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'ĐD. Trần Văn Hùng', '1980-01-01', 'MALE', '0980000019', 'nurse_dalieu@cares.vn', 'Hà Nội', NULL),
('f66e45f1-d610-1b77-14ac-22f42a258bab', '31aadf92-a791-45d9-7845-74d80b7185eb', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'ĐD. Lê Thị Ngọc Ánh', '1980-01-01', 'MALE', '0980000020', 'nurse_san@cares.vn', 'Hà Nội', NULL),
('fba61109-7f54-d270-6fa0-66490741c5d0', '46b1f075-3a66-2350-c411-023fdc6bdcce', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'KTV. Ngô Thị Hải Yến', '1980-01-01', 'MALE', '0980000021', 'ktv_xn@cares.vn', 'Hà Nội', NULL),
('0d4570c6-5de7-36df-7191-661793662543', 'e0ce249d-e3c3-e987-9099-b896ec2b6ec1', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'KTV. Phan Thị Mỹ Duyên', '1980-01-01', 'MALE', '0980000022', 'ktv_sa@cares.vn', 'Hà Nội', NULL),
('3d57548d-193d-4b79-0e00-ff27a47c1ebe', '199ef9e3-631e-b43e-ce55-d073f4bffefd', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'KTV. Hoàng Thị Kim Chi', '1980-01-01', 'MALE', '0980000023', 'ktv_xq@cares.vn', 'Hà Nội', NULL),
('b8523868-30ae-7eab-8d18-fb3f1a5488bc', 'a780d597-bf34-b2c4-df00-a72a966f6d4b', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Bệnh Nhân 1', '1990-01-01', 'FEMALE', '0987654001', 'patient0987654001@example.com', 'Hà Nội', NULL),
('1088d62b-ff88-2b61-b142-c59ddd695087', '7017c9f1-ded6-46cd-0dd5-950d5c86c100', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Bệnh Nhân 2', '1990-01-01', 'FEMALE', '0987654002', 'patient0987654002@example.com', 'Hà Nội', NULL),
('b96b77f8-3078-38f6-cd3f-19135ce19b9b', 'b546d6f2-eb3d-cb3b-4b66-c68e6a64ecc9', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Bệnh Nhân 3', '1990-01-01', 'FEMALE', '0987654003', 'patient0987654003@example.com', 'Hà Nội', NULL),
('bb0c6566-5036-8202-3e09-e655b1876243', 'e3c9f2b2-1d4a-e8ed-d963-a1abccf2085b', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Bệnh Nhân 4', '1990-01-01', 'FEMALE', '0987654004', 'patient0987654004@example.com', 'Hà Nội', NULL),
('c959002e-b7c1-eb0c-1183-ae9dc3acad17', '1ac47263-98dc-ed58-a646-e51a07483f28', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Bệnh Nhân 5', '1990-01-01', 'FEMALE', '0987654005', 'patient0987654005@example.com', 'Hà Nội', NULL),
('d71ae2ca-269b-d0f9-cf15-56eb27687f79', '239ef41f-451e-2067-2026-313a1c1d2f3a', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Bệnh Nhân 6', '1990-01-01', 'FEMALE', '0987654006', 'patient0987654006@example.com', 'Hà Nội', NULL),
('1d8db924-b0a8-6e8f-921c-59bb5d999acc', '18f88a0a-3d50-8759-e824-d86dca178007', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Bệnh Nhân 7', '1990-01-01', 'FEMALE', '0987654007', 'patient0987654007@example.com', 'Hà Nội', NULL),
('cfd84ebf-ce0d-4052-7c06-0a850c1b1eb5', 'e251a5dc-687b-2317-31f8-527cce389c44', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Bệnh Nhân 8', '1990-01-01', 'FEMALE', '0987654008', 'patient0987654008@example.com', 'Hà Nội', NULL),
('687ecdf1-ac69-a2d9-1c39-8344988f19ef', '6252b4f2-fabe-5949-e56f-c94a9c85cde6', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Bệnh Nhân 9', '1990-01-01', 'FEMALE', '0987654009', 'patient0987654009@example.com', 'Hà Nội', NULL),
('80221d38-65d4-f424-966b-52340f8125fc', '83ef3d6a-4444-627c-0611-08c23dc83d25', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Bệnh Nhân 10', '1990-01-01', 'FEMALE', '0987654010', 'patient0987654010@example.com', 'Hà Nội', NULL),
('a0ea2744-0cd2-cb2b-5e0f-c60fd2453426', 'f8c0fa8e-85a7-c8f4-0a46-46c3d186bef0', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Bệnh Nhân 11', '1990-01-01', 'FEMALE', '0987654011', 'patient0987654011@example.com', 'Hà Nội', NULL),
('d1841dfc-3567-7c11-fcec-a1a9dac19321', '24de58db-86c8-38be-5711-db3ba236026a', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Bệnh Nhân 12', '1990-01-01', 'FEMALE', '0987654012', 'patient0987654012@example.com', 'Hà Nội', NULL),
('4435cd7e-3781-3220-6f2b-8588d1586188', 'a0073cc4-b720-2968-c468-8027a5622a31', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Bệnh Nhân 13', '1990-01-01', 'FEMALE', '0987654013', 'patient0987654013@example.com', 'Hà Nội', NULL),
('80f349be-5d65-eb4c-fd11-db9e11904823', 'ee80ca83-b6b5-d41b-fcf4-a200000200d4', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Bệnh Nhân 14', '1990-01-01', 'FEMALE', '0987654014', 'patient0987654014@example.com', 'Hà Nội', NULL),
('d9bb8bf7-7740-d7f8-b199-896d6fdb60d2', 'dafcb0a8-e0c2-fc31-5138-83f5e527f6e0', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Bệnh Nhân 15', '1990-01-01', 'FEMALE', '0987654015', 'patient0987654015@example.com', 'Hà Nội', NULL),
('4da10e8d-fe0c-1bdf-6399-34d7a7beaa5b', 'ceed3b76-9b43-5b53-eafb-33f1076299a8', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Bệnh Nhân 16', '1990-01-01', 'FEMALE', '0987654016', 'patient0987654016@example.com', 'Hà Nội', NULL),
('2c2e26aa-6dfb-a7e6-921b-d4eff532dca7', '2da4d59d-f8be-f54c-9201-40033b27e510', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Bệnh Nhân 17', '1990-01-01', 'FEMALE', '0987654017', 'patient0987654017@example.com', 'Hà Nội', NULL),
('f6abb1a5-15a3-c329-6793-0422d668e8f5', '27ae040b-8920-e65f-3eb9-ec4b9ef8f2a4', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Bệnh Nhân 18', '1990-01-01', 'FEMALE', '0987654018', 'patient0987654018@example.com', 'Hà Nội', NULL),
('3681371b-5dac-8326-c1a4-2f3a8c75418b', 'df728b7e-6f20-19c6-b5e3-4b88ee7902fd', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Bệnh Nhân 19', '1990-01-01', 'FEMALE', '0987654019', 'patient0987654019@example.com', 'Hà Nội', NULL),
('b32c665b-780d-cb3d-3f72-009033a2e2be', '526b88ee-5562-d930-f0c2-2cc9ee7bbce8', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Bệnh Nhân 20', '1990-01-01', 'FEMALE', '0987654020', 'patient0987654020@example.com', 'Hà Nội', NULL),
('fd110e54-3675-cdf3-3203-56c5e529f364', 'fb587594-8fae-d7b0-dbd2-1b8ff39abdab', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Bệnh Nhân 21', '1990-01-01', 'FEMALE', '0987654021', 'patient0987654021@example.com', 'Hà Nội', NULL),
('3bc5bf5c-0ef7-1913-53c9-3a1ab64bbd37', NULL, (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Bệnh Nhân 22', '1990-01-01', 'FEMALE', '0987654022', 'patient0987654022@example.com', 'Hà Nội', NULL),
('e6219e8d-2a30-aa79-074d-0f0783b15242', NULL, (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Bệnh Nhân 23', '1990-01-01', 'FEMALE', '0987654023', 'patient0987654023@example.com', 'Hà Nội', NULL),
('92a7d21b-e18d-42c7-5cec-8168285a471f', NULL, (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Bệnh Nhân 24', '1990-01-01', 'FEMALE', '0987654024', 'patient0987654024@example.com', 'Hà Nội', NULL),
('28e8e02e-8ab4-69f4-5105-b6aa9dee53a9', NULL, (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Bệnh Nhân 25', '1990-01-01', 'FEMALE', '0987654025', 'patient0987654025@example.com', 'Hà Nội', NULL),
('c889150d-134e-086d-e07c-34a7a0a4330e', NULL, (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Bệnh Nhân 26', '1990-01-01', 'FEMALE', '0987654026', 'patient0987654026@example.com', 'Hà Nội', NULL);
-- StaffInfo
INSERT INTO staff_info (staff_id, created_at, updated_at, deleted, profile_id, staff_code, system_role, national_id, highest_degree, university, license_number, specialization_id, department_id) VALUES
('98c20755-90fd-8dc0-8764-00d47b6ee7f9', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '29e9a058-84de-dcd8-f74e-b12b409a7df3', 'STF-0001', 'ADMIN', '001000000000', NULL, NULL, NULL, NULL, NULL),
('676e6252-b216-fb9c-8b38-a50ea1aad20d', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '5114e08a-a82c-a581-3b37-6a107fa4a15c', 'STF-0002', 'CLINIC_MANAGER', '001000000001', NULL, NULL, NULL, NULL, NULL),
('50ec3aa8-ede0-2354-2df1-5a50e7a19729', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '5c1bb2a3-4cb5-9911-7fbb-aa9b22335b16', 'STF-0003', 'RECEPTIONIST', '001000000002', NULL, NULL, NULL, NULL, NULL),
('b693d136-402d-7de2-4835-117a5e2c5411', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '9e1e1b02-7c09-4a95-7a22-e3765a1f9f5f', 'STF-0004', 'CASHIER', '001000000003', NULL, NULL, NULL, NULL, NULL),
('8c574ec0-98e1-ad37-3d64-f900c3211b35', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '01f27858-d61c-12d5-13d5-f04475291f93', 'STF-D001', 'DOCTOR', '001000000004', 'Bác sĩ CKI', NULL, NULL, '00000001-1111-1111-1111-111111111111', '4d7ac047-a699-7a37-b2c0-e8392593e7bc'),
('a73ef1f7-a284-a69a-5d55-1f5f2aa58789', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'f71108bb-4766-dbe3-cba8-111b419cbfc2', 'STF-D002', 'DOCTOR', '001000000005', 'Bác sĩ CKI', NULL, NULL, '00000001-1111-1111-1111-111111111111', '1d5dd991-2183-7e47-8540-7b71704b5dff'),
('222d13de-2292-22ea-85b7-12b87f2c1604', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'cd122477-83e0-7fd4-7e10-5407f500ed65', 'STF-D003', 'DOCTOR', '001000000006', 'Bác sĩ CKII', NULL, NULL, '00000003-3333-3333-3333-333333333333', 'b6b019fc-fd4c-a6ce-527b-f928546cef64'),
('a195fff6-226e-7492-f75d-1c471eff523f', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '8e36d38b-f5ea-cc6b-1a93-256d2409ab3f', 'STF-D004', 'DOCTOR', '001000000007', 'Bác sĩ CKII', NULL, NULL, '00000003-3333-3333-3333-333333333333', 'ba0338ee-d868-b5f0-3560-4e4f139794fc'),
('de4d04ed-d9b4-734b-4dd9-7a3048ffdf8a', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'a57d8709-bcd2-b391-d484-ca52114831a1', 'STF-D005', 'DOCTOR', '001000000008', 'Bác sĩ CKI', NULL, NULL, '00000002-2222-2222-2222-222222222222', '65034e17-bcc4-7981-c637-7bfcd6dfa331'),
('228d522a-424e-5037-c20e-325c43826a93', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '19853712-f455-1465-9937-c31040d73675', 'STF-D006', 'DOCTOR', '001000000009', 'Bác sĩ CKI', NULL, NULL, '00000004-4444-4444-4444-444444444444', '98d87656-422c-787c-fc4b-6408c00b644c'),
('c4882884-2185-448d-7d00-a65cfae000bc', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'b945aed4-61fe-c960-dbac-a3f4ff267332', 'STF-D007', 'DOCTOR', '001000000010', 'Bác sĩ CKI', NULL, NULL, '00000008-8888-8888-8888-888888888888', '177e189b-5501-d8a7-c9e5-32c9c6de8c3f'),
('74cf86f9-d830-e246-7dc3-538a0874779d', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '5a9b2cef-cf38-a3fc-0079-7fd2a637a798', 'STF-D008', 'DOCTOR', '001000000011', 'Bác sĩ CKI', NULL, NULL, NULL, '760edff5-8292-2100-0b77-7fc8e316e913'),
('6058143b-c4d4-3840-262f-3aa52b388608', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'c0343908-daa1-4bf9-6c15-de0b478b90ed', 'STF-D009', 'DOCTOR', '001000000012', 'Bác sĩ CKI', NULL, NULL, NULL, '41706ccd-49ed-d1e4-3767-9434f57ebe42'),
('ceb1dbd4-ee6c-2065-99f8-cd32f616c3f6', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '1ac31d90-f9d8-d334-1172-06a0c07fadba', 'STF-D010', 'DOCTOR', '001000000013', 'Bác sĩ CKI', NULL, NULL, NULL, '4a0a5c21-f9d4-234b-9631-c80fa2db9881'),
('d8dccd88-7bbc-a7b7-a014-572106a75b15', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '2daf6591-2abb-fca4-5a0f-aea35f7659c5', 'STF-N001', 'NURSE', '001000000014', 'Cử nhân', NULL, NULL, NULL, '4d7ac047-a699-7a37-b2c0-e8392593e7bc'),
('69cd27b8-da82-9e51-3849-c86bac4e8091', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'b2bb4656-fbfe-f2ab-c317-cff23397dfb0', 'STF-N002', 'NURSE', '001000000015', 'Cử nhân', NULL, NULL, NULL, '1d5dd991-2183-7e47-8540-7b71704b5dff'),
('a021f4b7-3efc-03fa-8cdc-ab7499533d74', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'da4a2289-3402-7d38-b947-47e8dbcf3ec2', 'STF-N003', 'NURSE', '001000000016', 'Cử nhân', NULL, NULL, NULL, 'b6b019fc-fd4c-a6ce-527b-f928546cef64'),
('4f8bd60d-4cf5-158d-a2bd-b26f361722ef', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '59ada81f-ed02-1d6e-1585-4aadc5f0bec1', 'STF-N004', 'NURSE', '001000000017', 'Cử nhân', NULL, NULL, NULL, 'ba0338ee-d868-b5f0-3560-4e4f139794fc'),
('a1dc8e2c-c534-1b33-5710-5db2a6606fd7', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '900ac2df-0e35-86d4-2e17-85a543999e56', 'STF-N005', 'NURSE', '001000000018', 'Cử nhân', NULL, NULL, NULL, '65034e17-bcc4-7981-c637-7bfcd6dfa331'),
('8bb4c409-e900-e53e-d118-559d0e033c5f', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'b52d3843-ef31-e545-da31-7b38be3ad19a', 'STF-N006', 'NURSE', '001000000019', 'Cử nhân', NULL, NULL, NULL, '98d87656-422c-787c-fc4b-6408c00b644c'),
('aca1a060-d55b-c72c-120e-7ea9239e103e', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'f66e45f1-d610-1b77-14ac-22f42a258bab', 'STF-N007', 'NURSE', '001000000020', 'Cử nhân', NULL, NULL, NULL, '177e189b-5501-d8a7-c9e5-32c9c6de8c3f'),
('0f348c8d-74b9-3ef2-981b-61105eab787e', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'fba61109-7f54-d270-6fa0-66490741c5d0', 'STF-N008', 'NURSE', '001000000021', 'Cử nhân', NULL, NULL, NULL, '760edff5-8292-2100-0b77-7fc8e316e913'),
('ff42c405-1351-450b-5d7a-f8db94acd9a8', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '0d4570c6-5de7-36df-7191-661793662543', 'STF-N009', 'NURSE', '001000000022', 'Cử nhân', NULL, NULL, NULL, '41706ccd-49ed-d1e4-3767-9434f57ebe42'),
('c0fb3910-cc9e-5c36-3662-64f097055b4a', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, '3d57548d-193d-4b79-0e00-ff27a47c1ebe', 'STF-N010', 'NURSE', '001000000023', 'Cử nhân', NULL, NULL, NULL, '4a0a5c21-f9d4-234b-9631-c80fa2db9881');

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

-- Department Capabilities
INSERT INTO department_capability (department_id, capability_id) VALUES
('760edff5-8292-2100-0b77-7fc8e316e913', 'ca000001-0000-0000-0000-000000000001'),
('760edff5-8292-2100-0b77-7fc8e316e913', 'ca000002-0000-0000-0000-000000000002'),
('760edff5-8292-2100-0b77-7fc8e316e913', 'ca000005-0000-0000-0000-000000000005'),
('760edff5-8292-2100-0b77-7fc8e316e913', 'ca000007-0000-0000-0000-000000000007'),
('760edff5-8292-2100-0b77-7fc8e316e913', 'ca000008-0000-0000-0000-000000000008'),
('41706ccd-49ed-d1e4-3767-9434f57ebe42', 'ca000003-0000-0000-0000-000000000003'),
('41706ccd-49ed-d1e4-3767-9434f57ebe42', 'ca000006-0000-0000-0000-000000000006'),
('4a0a5c21-f9d4-234b-9631-c80fa2db9881', 'ca000004-0000-0000-0000-000000000004');
-- Medical Services (Keeping the core list)
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


-- Medicine & ICD 10
INSERT INTO medicine_catalog (medicine_id, medicine_code, name, active_ingredient, default_unit, default_usage, default_frequency_per_day, active, deleted) VALUES
('b0000000-0000-0000-0000-000000000001', 'MED-001', 'Paracetamol 500mg', 'Paracetamol', 'Viên', 'Uống sau ăn', 2, true, false);

INSERT INTO icd_10_codes (code, name, description, category, deleted) VALUES
('J00', 'Viêm mũi họng cấp', 'Cảm lạnh thông thường', 'Bệnh hệ hô hấp', false),
('I10', 'Tăng huyết áp vô căn', 'Tăng huyết áp nguyên phát', 'Bệnh hệ tuần hoàn', false),
('Z00.0', 'Khám sức khỏe tổng quát', 'Khám định kỳ', 'Yếu tố sức khỏe', false);


-- Shift Config
INSERT INTO shift_config (shift_id, created_at, updated_at, deleted, name, start_time, end_time, is_active) VALUES
('70000001-1111-1111-1111-111111111111', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Ca Sáng', '07:30', '11:30', true),
('70000002-2222-2222-2222-222222222222', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Ca Chiều', '13:00', '17:00', true),
('70000003-3333-3333-3333-333333333333', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Ca Tối', '17:30', '21:30', true);

INSERT INTO shift_version (shift_version_id, shift_id, start_time, end_time, effective_from, effective_to, change_reason, created_at, updated_at, deleted) VALUES
('71000001-1111-1111-1111-111111111111', '70000001-1111-1111-1111-111111111111', '07:30', '11:30', pg_temp.demo_date() - 365, NULL, 'Khởi tạo', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false),
('71000002-2222-2222-2222-222222222222', '70000002-2222-2222-2222-222222222222', '13:00', '17:00', pg_temp.demo_date() - 365, NULL, 'Khởi tạo', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false),
('71000003-3333-3333-3333-333333333333', '70000003-3333-3333-3333-333333333333', '17:30', '21:30', pg_temp.demo_date() - 365, NULL, 'Khởi tạo', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false);

-- Staff Schedules (Full tuần 3 ca)

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

-- History Visits (25 patients)
INSERT INTO appointment (appointment_id, created_at, updated_at, deleted, scheduled_at, status, is_guest, customer_id, shift_name, shift_time, shift_version_id) VALUES
('3ff11ade-6815-0776-4a36-17350edb23a4', pg_temp.demo_now()-interval '29 days', pg_temp.demo_now()-interval '29 days', false, pg_temp.demo_date()-interval '29 days' + time '08:00', 'CHECKED_IN', false, '1088d62b-ff88-2b61-b142-c59ddd695087', 'Ca Sáng', '07:30-11:30', '71000001-1111-1111-1111-111111111111'),
('a8271989-5d35-1390-afcd-fb8b47f9c1fa', pg_temp.demo_now()-interval '28 days', pg_temp.demo_now()-interval '28 days', false, pg_temp.demo_date()-interval '28 days' + time '08:00', 'CHECKED_IN', false, 'b96b77f8-3078-38f6-cd3f-19135ce19b9b', 'Ca Sáng', '07:30-11:30', '71000001-1111-1111-1111-111111111111'),
('f13d6118-3393-1696-df77-82f0cb28d64b', pg_temp.demo_now()-interval '27 days', pg_temp.demo_now()-interval '27 days', false, pg_temp.demo_date()-interval '27 days' + time '08:00', 'CHECKED_IN', false, 'bb0c6566-5036-8202-3e09-e655b1876243', 'Ca Sáng', '07:30-11:30', '71000001-1111-1111-1111-111111111111'),
('4a53ccb4-0ecd-ee88-ce24-b17fb47231f4', pg_temp.demo_now()-interval '26 days', pg_temp.demo_now()-interval '26 days', false, pg_temp.demo_date()-interval '26 days' + time '08:00', 'CHECKED_IN', false, 'c959002e-b7c1-eb0c-1183-ae9dc3acad17', 'Ca Sáng', '07:30-11:30', '71000001-1111-1111-1111-111111111111'),
('60360342-0138-d28d-2df1-b8935bcc91ba', pg_temp.demo_now()-interval '25 days', pg_temp.demo_now()-interval '25 days', false, pg_temp.demo_date()-interval '25 days' + time '08:00', 'CHECKED_IN', false, 'd71ae2ca-269b-d0f9-cf15-56eb27687f79', 'Ca Sáng', '07:30-11:30', '71000001-1111-1111-1111-111111111111'),
('2f91140f-c56d-a737-6293-ef5031e432ec', pg_temp.demo_now()-interval '24 days', pg_temp.demo_now()-interval '24 days', false, pg_temp.demo_date()-interval '24 days' + time '08:00', 'CHECKED_IN', false, '1d8db924-b0a8-6e8f-921c-59bb5d999acc', 'Ca Sáng', '07:30-11:30', '71000001-1111-1111-1111-111111111111'),
('0305f023-b934-755a-f74f-eb67321b104d', pg_temp.demo_now()-interval '23 days', pg_temp.demo_now()-interval '23 days', false, pg_temp.demo_date()-interval '23 days' + time '08:00', 'CHECKED_IN', false, 'cfd84ebf-ce0d-4052-7c06-0a850c1b1eb5', 'Ca Sáng', '07:30-11:30', '71000001-1111-1111-1111-111111111111'),
('e1554ced-95a3-fc01-4404-5d4c00d6779a', pg_temp.demo_now()-interval '22 days', pg_temp.demo_now()-interval '22 days', false, pg_temp.demo_date()-interval '22 days' + time '08:00', 'CHECKED_IN', false, '687ecdf1-ac69-a2d9-1c39-8344988f19ef', 'Ca Sáng', '07:30-11:30', '71000001-1111-1111-1111-111111111111'),
('11c05d04-4d42-a030-f259-60d1a70e8a8c', pg_temp.demo_now()-interval '21 days', pg_temp.demo_now()-interval '21 days', false, pg_temp.demo_date()-interval '21 days' + time '08:00', 'CHECKED_IN', false, '80221d38-65d4-f424-966b-52340f8125fc', 'Ca Sáng', '07:30-11:30', '71000001-1111-1111-1111-111111111111'),
('8d20d202-7319-fad9-2da0-70b0604a38c7', pg_temp.demo_now()-interval '20 days', pg_temp.demo_now()-interval '20 days', false, pg_temp.demo_date()-interval '20 days' + time '08:00', 'CHECKED_IN', false, 'a0ea2744-0cd2-cb2b-5e0f-c60fd2453426', 'Ca Sáng', '07:30-11:30', '71000001-1111-1111-1111-111111111111'),
('b992d6fc-7f19-9874-cbf6-d7836d5c2923', pg_temp.demo_now()-interval '19 days', pg_temp.demo_now()-interval '19 days', false, pg_temp.demo_date()-interval '19 days' + time '08:00', 'CHECKED_IN', false, 'd1841dfc-3567-7c11-fcec-a1a9dac19321', 'Ca Sáng', '07:30-11:30', '71000001-1111-1111-1111-111111111111'),
('729fac5f-db3b-c25e-56f0-4c46586e3e72', pg_temp.demo_now()-interval '18 days', pg_temp.demo_now()-interval '18 days', false, pg_temp.demo_date()-interval '18 days' + time '08:00', 'CHECKED_IN', false, '4435cd7e-3781-3220-6f2b-8588d1586188', 'Ca Sáng', '07:30-11:30', '71000001-1111-1111-1111-111111111111'),
('6d152925-cedf-7a94-95a6-917ec911f438', pg_temp.demo_now()-interval '17 days', pg_temp.demo_now()-interval '17 days', false, pg_temp.demo_date()-interval '17 days' + time '08:00', 'CHECKED_IN', false, '80f349be-5d65-eb4c-fd11-db9e11904823', 'Ca Sáng', '07:30-11:30', '71000001-1111-1111-1111-111111111111'),
('11744af7-9196-0b4d-5922-989d7624dbbf', pg_temp.demo_now()-interval '16 days', pg_temp.demo_now()-interval '16 days', false, pg_temp.demo_date()-interval '16 days' + time '08:00', 'CHECKED_IN', false, 'd9bb8bf7-7740-d7f8-b199-896d6fdb60d2', 'Ca Sáng', '07:30-11:30', '71000001-1111-1111-1111-111111111111'),
('1d61e310-0f3f-96e3-93e8-0e6afe58e182', pg_temp.demo_now()-interval '15 days', pg_temp.demo_now()-interval '15 days', false, pg_temp.demo_date()-interval '15 days' + time '08:00', 'CHECKED_IN', false, '4da10e8d-fe0c-1bdf-6399-34d7a7beaa5b', 'Ca Sáng', '07:30-11:30', '71000001-1111-1111-1111-111111111111'),
('3f4ab894-06d5-bb35-de74-b8ab5417c438', pg_temp.demo_now()-interval '14 days', pg_temp.demo_now()-interval '14 days', false, pg_temp.demo_date()-interval '14 days' + time '08:00', 'CHECKED_IN', false, '2c2e26aa-6dfb-a7e6-921b-d4eff532dca7', 'Ca Sáng', '07:30-11:30', '71000001-1111-1111-1111-111111111111'),
('ab6ac82b-78c9-762f-c5e1-80ae17ad5502', pg_temp.demo_now()-interval '13 days', pg_temp.demo_now()-interval '13 days', false, pg_temp.demo_date()-interval '13 days' + time '08:00', 'CHECKED_IN', false, 'f6abb1a5-15a3-c329-6793-0422d668e8f5', 'Ca Sáng', '07:30-11:30', '71000001-1111-1111-1111-111111111111'),
('16bb8e49-c081-54e7-2f3f-58ad7ddd9643', pg_temp.demo_now()-interval '12 days', pg_temp.demo_now()-interval '12 days', false, pg_temp.demo_date()-interval '12 days' + time '08:00', 'CHECKED_IN', false, '3681371b-5dac-8326-c1a4-2f3a8c75418b', 'Ca Sáng', '07:30-11:30', '71000001-1111-1111-1111-111111111111'),
('0294b1de-d7fa-00dd-49ce-b5393f8c531d', pg_temp.demo_now()-interval '11 days', pg_temp.demo_now()-interval '11 days', false, pg_temp.demo_date()-interval '11 days' + time '08:00', 'CHECKED_IN', false, 'b32c665b-780d-cb3d-3f72-009033a2e2be', 'Ca Sáng', '07:30-11:30', '71000001-1111-1111-1111-111111111111'),
('6fd3757e-de01-c947-cca3-fe95e38ae430', pg_temp.demo_now()-interval '10 days', pg_temp.demo_now()-interval '10 days', false, pg_temp.demo_date()-interval '10 days' + time '08:00', 'CHECKED_IN', false, 'fd110e54-3675-cdf3-3203-56c5e529f364', 'Ca Sáng', '07:30-11:30', '71000001-1111-1111-1111-111111111111'),
('16efedaf-4b1d-5db7-9104-9444bc676a36', pg_temp.demo_now()-interval '9 days', pg_temp.demo_now()-interval '9 days', false, pg_temp.demo_date()-interval '9 days' + time '08:00', 'CHECKED_IN', true, '3bc5bf5c-0ef7-1913-53c9-3a1ab64bbd37', 'Ca Sáng', '07:30-11:30', '71000001-1111-1111-1111-111111111111'),
('ba2c6dd0-8959-1765-9bf4-7f82cc5fac6f', pg_temp.demo_now()-interval '8 days', pg_temp.demo_now()-interval '8 days', false, pg_temp.demo_date()-interval '8 days' + time '08:00', 'CHECKED_IN', true, 'e6219e8d-2a30-aa79-074d-0f0783b15242', 'Ca Sáng', '07:30-11:30', '71000001-1111-1111-1111-111111111111'),
('f6509574-7d42-6dcb-424f-d3ae5186bb37', pg_temp.demo_now()-interval '7 days', pg_temp.demo_now()-interval '7 days', false, pg_temp.demo_date()-interval '7 days' + time '08:00', 'CHECKED_IN', true, '92a7d21b-e18d-42c7-5cec-8168285a471f', 'Ca Sáng', '07:30-11:30', '71000001-1111-1111-1111-111111111111'),
('07c81adf-147b-9918-6196-c0619d1fd671', pg_temp.demo_now()-interval '6 days', pg_temp.demo_now()-interval '6 days', false, pg_temp.demo_date()-interval '6 days' + time '08:00', 'CHECKED_IN', true, '28e8e02e-8ab4-69f4-5105-b6aa9dee53a9', 'Ca Sáng', '07:30-11:30', '71000001-1111-1111-1111-111111111111'),
('9bcc35a8-844c-b0b4-7a9b-ddb0ea9cc3f5', pg_temp.demo_now()-interval '5 days', pg_temp.demo_now()-interval '5 days', false, pg_temp.demo_date()-interval '5 days' + time '08:00', 'CHECKED_IN', true, 'c889150d-134e-086d-e07c-34a7a0a4330e', 'Ca Sáng', '07:30-11:30', '71000001-1111-1111-1111-111111111111');
INSERT INTO customer_visit (visit_id, customer_id, status, check_in_time, check_out_time, created_at, updated_at, deleted) VALUES
('d5ee978d-cb75-979f-16ff-d348536a3a85', '1088d62b-ff88-2b61-b142-c59ddd695087', 'COMPLETED', pg_temp.demo_date()-interval '29 days' + time '08:00', pg_temp.demo_date()-interval '29 days' + time '09:00', pg_temp.demo_now(), pg_temp.demo_now(), false),
('07ac9a53-0f97-2fb6-1f9d-34924096efc6', 'b96b77f8-3078-38f6-cd3f-19135ce19b9b', 'COMPLETED', pg_temp.demo_date()-interval '28 days' + time '08:00', pg_temp.demo_date()-interval '28 days' + time '09:00', pg_temp.demo_now(), pg_temp.demo_now(), false),
('41b2420a-d4b7-8804-1d26-035597505b0d', 'bb0c6566-5036-8202-3e09-e655b1876243', 'COMPLETED', pg_temp.demo_date()-interval '27 days' + time '08:00', pg_temp.demo_date()-interval '27 days' + time '09:00', pg_temp.demo_now(), pg_temp.demo_now(), false),
('563593a1-6854-4e0f-2a22-a2558be66071', 'c959002e-b7c1-eb0c-1183-ae9dc3acad17', 'COMPLETED', pg_temp.demo_date()-interval '26 days' + time '08:00', pg_temp.demo_date()-interval '26 days' + time '09:00', pg_temp.demo_now(), pg_temp.demo_now(), false),
('3f479e2a-b84e-4e6b-5f34-701b740d2c42', 'd71ae2ca-269b-d0f9-cf15-56eb27687f79', 'COMPLETED', pg_temp.demo_date()-interval '25 days' + time '08:00', pg_temp.demo_date()-interval '25 days' + time '09:00', pg_temp.demo_now(), pg_temp.demo_now(), false),
('58f43c00-7613-60c0-80b7-aa987cd1cdc7', '1d8db924-b0a8-6e8f-921c-59bb5d999acc', 'COMPLETED', pg_temp.demo_date()-interval '24 days' + time '08:00', pg_temp.demo_date()-interval '24 days' + time '09:00', pg_temp.demo_now(), pg_temp.demo_now(), false),
('01ab728f-81ca-9b16-01b0-829ee8afccd7', 'cfd84ebf-ce0d-4052-7c06-0a850c1b1eb5', 'COMPLETED', pg_temp.demo_date()-interval '23 days' + time '08:00', pg_temp.demo_date()-interval '23 days' + time '09:00', pg_temp.demo_now(), pg_temp.demo_now(), false),
('4e75879e-ee16-2033-328e-14074267adf5', '687ecdf1-ac69-a2d9-1c39-8344988f19ef', 'COMPLETED', pg_temp.demo_date()-interval '22 days' + time '08:00', pg_temp.demo_date()-interval '22 days' + time '09:00', pg_temp.demo_now(), pg_temp.demo_now(), false),
('ce35c38b-b5f4-c14b-3cc6-9cdd5dcf62ea', '80221d38-65d4-f424-966b-52340f8125fc', 'COMPLETED', pg_temp.demo_date()-interval '21 days' + time '08:00', pg_temp.demo_date()-interval '21 days' + time '09:00', pg_temp.demo_now(), pg_temp.demo_now(), false),
('472df76d-d7a1-4f37-23be-4625460788ef', 'a0ea2744-0cd2-cb2b-5e0f-c60fd2453426', 'COMPLETED', pg_temp.demo_date()-interval '20 days' + time '08:00', pg_temp.demo_date()-interval '20 days' + time '09:00', pg_temp.demo_now(), pg_temp.demo_now(), false),
('95f27f2f-a87f-9a2f-94d0-794b78bb874d', 'd1841dfc-3567-7c11-fcec-a1a9dac19321', 'COMPLETED', pg_temp.demo_date()-interval '19 days' + time '08:00', pg_temp.demo_date()-interval '19 days' + time '09:00', pg_temp.demo_now(), pg_temp.demo_now(), false),
('3b64222f-e79f-d4e5-b59c-238bd8e305f0', '4435cd7e-3781-3220-6f2b-8588d1586188', 'COMPLETED', pg_temp.demo_date()-interval '18 days' + time '08:00', pg_temp.demo_date()-interval '18 days' + time '09:00', pg_temp.demo_now(), pg_temp.demo_now(), false),
('acdfb5b3-9e44-edeb-0b33-a87b3061c910', '80f349be-5d65-eb4c-fd11-db9e11904823', 'COMPLETED', pg_temp.demo_date()-interval '17 days' + time '08:00', pg_temp.demo_date()-interval '17 days' + time '09:00', pg_temp.demo_now(), pg_temp.demo_now(), false),
('8fd00ef0-0301-e828-12aa-feb061baae9b', 'd9bb8bf7-7740-d7f8-b199-896d6fdb60d2', 'COMPLETED', pg_temp.demo_date()-interval '16 days' + time '08:00', pg_temp.demo_date()-interval '16 days' + time '09:00', pg_temp.demo_now(), pg_temp.demo_now(), false),
('a6f52718-74ad-b4fb-2395-bd5a7273947d', '4da10e8d-fe0c-1bdf-6399-34d7a7beaa5b', 'COMPLETED', pg_temp.demo_date()-interval '15 days' + time '08:00', pg_temp.demo_date()-interval '15 days' + time '09:00', pg_temp.demo_now(), pg_temp.demo_now(), false),
('1a1dd74b-671a-007a-f820-17b1ed805085', '2c2e26aa-6dfb-a7e6-921b-d4eff532dca7', 'COMPLETED', pg_temp.demo_date()-interval '14 days' + time '08:00', pg_temp.demo_date()-interval '14 days' + time '09:00', pg_temp.demo_now(), pg_temp.demo_now(), false),
('4870f11b-a685-6595-aa05-df1eaf57b453', 'f6abb1a5-15a3-c329-6793-0422d668e8f5', 'COMPLETED', pg_temp.demo_date()-interval '13 days' + time '08:00', pg_temp.demo_date()-interval '13 days' + time '09:00', pg_temp.demo_now(), pg_temp.demo_now(), false),
('f2ae7fa2-f61e-628c-8fd6-a1e37ee28925', '3681371b-5dac-8326-c1a4-2f3a8c75418b', 'COMPLETED', pg_temp.demo_date()-interval '12 days' + time '08:00', pg_temp.demo_date()-interval '12 days' + time '09:00', pg_temp.demo_now(), pg_temp.demo_now(), false),
('c284d077-0220-f4e5-379b-f748001a517a', 'b32c665b-780d-cb3d-3f72-009033a2e2be', 'COMPLETED', pg_temp.demo_date()-interval '11 days' + time '08:00', pg_temp.demo_date()-interval '11 days' + time '09:00', pg_temp.demo_now(), pg_temp.demo_now(), false),
('9419485f-8220-72e4-d117-4f924aaf3d64', 'fd110e54-3675-cdf3-3203-56c5e529f364', 'COMPLETED', pg_temp.demo_date()-interval '10 days' + time '08:00', pg_temp.demo_date()-interval '10 days' + time '09:00', pg_temp.demo_now(), pg_temp.demo_now(), false),
('6896b913-3282-13e0-507d-ba867aa88a9e', '3bc5bf5c-0ef7-1913-53c9-3a1ab64bbd37', 'COMPLETED', pg_temp.demo_date()-interval '9 days' + time '08:00', pg_temp.demo_date()-interval '9 days' + time '09:00', pg_temp.demo_now(), pg_temp.demo_now(), false),
('155226fc-7ea7-af74-8f79-6f301dacc813', 'e6219e8d-2a30-aa79-074d-0f0783b15242', 'COMPLETED', pg_temp.demo_date()-interval '8 days' + time '08:00', pg_temp.demo_date()-interval '8 days' + time '09:00', pg_temp.demo_now(), pg_temp.demo_now(), false),
('7106a7e0-7222-4b2e-4070-28ee64525341', '92a7d21b-e18d-42c7-5cec-8168285a471f', 'COMPLETED', pg_temp.demo_date()-interval '7 days' + time '08:00', pg_temp.demo_date()-interval '7 days' + time '09:00', pg_temp.demo_now(), pg_temp.demo_now(), false),
('2d27d8c8-6536-e798-cc59-c5ca623f32d7', '28e8e02e-8ab4-69f4-5105-b6aa9dee53a9', 'COMPLETED', pg_temp.demo_date()-interval '6 days' + time '08:00', pg_temp.demo_date()-interval '6 days' + time '09:00', pg_temp.demo_now(), pg_temp.demo_now(), false),
('378f1bfa-d1ba-5fa5-9867-20d85332e09c', 'c889150d-134e-086d-e07c-34a7a0a4330e', 'COMPLETED', pg_temp.demo_date()-interval '5 days' + time '08:00', pg_temp.demo_date()-interval '5 days' + time '09:00', pg_temp.demo_now(), pg_temp.demo_now(), false);
INSERT INTO queue_ticket (ticket_id, visit_id, department_id, status, ticket_number, created_at, updated_at, deleted, type, work_date, is_guest, customer_id) VALUES
('959df67f-eacb-f95e-661d-96a9b3890f14', 'd5ee978d-cb75-979f-16ff-d348536a3a85', '4d7ac047-a699-7a37-b2c0-e8392593e7bc', 'DONE', 1, pg_temp.demo_now()-interval '29 days', pg_temp.demo_now()-interval '29 days', false, 'EXAMINATION', pg_temp.demo_date()-interval '29 days', false, '1088d62b-ff88-2b61-b142-c59ddd695087'),
('339f4a88-3b15-f2e5-b9a5-a94de87bed35', '07ac9a53-0f97-2fb6-1f9d-34924096efc6', '4d7ac047-a699-7a37-b2c0-e8392593e7bc', 'DONE', 1, pg_temp.demo_now()-interval '28 days', pg_temp.demo_now()-interval '28 days', false, 'EXAMINATION', pg_temp.demo_date()-interval '28 days', false, 'b96b77f8-3078-38f6-cd3f-19135ce19b9b'),
('ea19ff68-3dae-b5ff-98b4-61c3bdc0467b', '41b2420a-d4b7-8804-1d26-035597505b0d', '4d7ac047-a699-7a37-b2c0-e8392593e7bc', 'DONE', 1, pg_temp.demo_now()-interval '27 days', pg_temp.demo_now()-interval '27 days', false, 'EXAMINATION', pg_temp.demo_date()-interval '27 days', false, 'bb0c6566-5036-8202-3e09-e655b1876243'),
('2261e207-c96f-deff-b08c-105a070890f4', '563593a1-6854-4e0f-2a22-a2558be66071', '4d7ac047-a699-7a37-b2c0-e8392593e7bc', 'DONE', 1, pg_temp.demo_now()-interval '26 days', pg_temp.demo_now()-interval '26 days', false, 'EXAMINATION', pg_temp.demo_date()-interval '26 days', false, 'c959002e-b7c1-eb0c-1183-ae9dc3acad17'),
('e61ed18f-371f-b4a2-64cb-d709c61d8cd5', '3f479e2a-b84e-4e6b-5f34-701b740d2c42', '4d7ac047-a699-7a37-b2c0-e8392593e7bc', 'DONE', 1, pg_temp.demo_now()-interval '25 days', pg_temp.demo_now()-interval '25 days', false, 'EXAMINATION', pg_temp.demo_date()-interval '25 days', false, 'd71ae2ca-269b-d0f9-cf15-56eb27687f79'),
('db39ffca-67d9-434c-7b30-6221cd0180df', '58f43c00-7613-60c0-80b7-aa987cd1cdc7', '4d7ac047-a699-7a37-b2c0-e8392593e7bc', 'DONE', 1, pg_temp.demo_now()-interval '24 days', pg_temp.demo_now()-interval '24 days', false, 'EXAMINATION', pg_temp.demo_date()-interval '24 days', false, '1d8db924-b0a8-6e8f-921c-59bb5d999acc'),
('faac3d2f-a864-315e-fb10-373134b838ec', '01ab728f-81ca-9b16-01b0-829ee8afccd7', '4d7ac047-a699-7a37-b2c0-e8392593e7bc', 'DONE', 1, pg_temp.demo_now()-interval '23 days', pg_temp.demo_now()-interval '23 days', false, 'EXAMINATION', pg_temp.demo_date()-interval '23 days', false, 'cfd84ebf-ce0d-4052-7c06-0a850c1b1eb5'),
('a64a9548-fffd-7d57-0e4d-d5a9b9ce99a6', '4e75879e-ee16-2033-328e-14074267adf5', '4d7ac047-a699-7a37-b2c0-e8392593e7bc', 'DONE', 1, pg_temp.demo_now()-interval '22 days', pg_temp.demo_now()-interval '22 days', false, 'EXAMINATION', pg_temp.demo_date()-interval '22 days', false, '687ecdf1-ac69-a2d9-1c39-8344988f19ef'),
('828967b2-c757-60bf-43e5-9cda67268b22', 'ce35c38b-b5f4-c14b-3cc6-9cdd5dcf62ea', '4d7ac047-a699-7a37-b2c0-e8392593e7bc', 'DONE', 1, pg_temp.demo_now()-interval '21 days', pg_temp.demo_now()-interval '21 days', false, 'EXAMINATION', pg_temp.demo_date()-interval '21 days', false, '80221d38-65d4-f424-966b-52340f8125fc'),
('e0df9999-cf60-6a6d-ea78-c72766f5f810', '472df76d-d7a1-4f37-23be-4625460788ef', '4d7ac047-a699-7a37-b2c0-e8392593e7bc', 'DONE', 1, pg_temp.demo_now()-interval '20 days', pg_temp.demo_now()-interval '20 days', false, 'EXAMINATION', pg_temp.demo_date()-interval '20 days', false, 'a0ea2744-0cd2-cb2b-5e0f-c60fd2453426'),
('ad46d18e-e311-9be7-623d-2db8640c330a', '95f27f2f-a87f-9a2f-94d0-794b78bb874d', '4d7ac047-a699-7a37-b2c0-e8392593e7bc', 'DONE', 1, pg_temp.demo_now()-interval '19 days', pg_temp.demo_now()-interval '19 days', false, 'EXAMINATION', pg_temp.demo_date()-interval '19 days', false, 'd1841dfc-3567-7c11-fcec-a1a9dac19321'),
('786e9e96-5b19-0336-efc8-6d0e7325e12f', '3b64222f-e79f-d4e5-b59c-238bd8e305f0', '4d7ac047-a699-7a37-b2c0-e8392593e7bc', 'DONE', 1, pg_temp.demo_now()-interval '18 days', pg_temp.demo_now()-interval '18 days', false, 'EXAMINATION', pg_temp.demo_date()-interval '18 days', false, '4435cd7e-3781-3220-6f2b-8588d1586188'),
('e41ac946-1a52-8953-3506-0e39cc5a0870', 'acdfb5b3-9e44-edeb-0b33-a87b3061c910', '4d7ac047-a699-7a37-b2c0-e8392593e7bc', 'DONE', 1, pg_temp.demo_now()-interval '17 days', pg_temp.demo_now()-interval '17 days', false, 'EXAMINATION', pg_temp.demo_date()-interval '17 days', false, '80f349be-5d65-eb4c-fd11-db9e11904823'),
('6cbc992f-32a9-3314-5383-556f782b9dfa', '8fd00ef0-0301-e828-12aa-feb061baae9b', '4d7ac047-a699-7a37-b2c0-e8392593e7bc', 'DONE', 1, pg_temp.demo_now()-interval '16 days', pg_temp.demo_now()-interval '16 days', false, 'EXAMINATION', pg_temp.demo_date()-interval '16 days', false, 'd9bb8bf7-7740-d7f8-b199-896d6fdb60d2'),
('dac1c60b-219c-2fca-3bdb-c503dd44a719', 'a6f52718-74ad-b4fb-2395-bd5a7273947d', '4d7ac047-a699-7a37-b2c0-e8392593e7bc', 'DONE', 1, pg_temp.demo_now()-interval '15 days', pg_temp.demo_now()-interval '15 days', false, 'EXAMINATION', pg_temp.demo_date()-interval '15 days', false, '4da10e8d-fe0c-1bdf-6399-34d7a7beaa5b'),
('8692b9dc-6bfb-d5f1-29ba-927587bf33fc', '1a1dd74b-671a-007a-f820-17b1ed805085', '4d7ac047-a699-7a37-b2c0-e8392593e7bc', 'DONE', 1, pg_temp.demo_now()-interval '14 days', pg_temp.demo_now()-interval '14 days', false, 'EXAMINATION', pg_temp.demo_date()-interval '14 days', false, '2c2e26aa-6dfb-a7e6-921b-d4eff532dca7'),
('55963e3f-3938-51a0-8c9a-e923eac8cba0', '4870f11b-a685-6595-aa05-df1eaf57b453', '4d7ac047-a699-7a37-b2c0-e8392593e7bc', 'DONE', 1, pg_temp.demo_now()-interval '13 days', pg_temp.demo_now()-interval '13 days', false, 'EXAMINATION', pg_temp.demo_date()-interval '13 days', false, 'f6abb1a5-15a3-c329-6793-0422d668e8f5'),
('57eed6c0-10e9-220e-99c2-4f107ea77aac', 'f2ae7fa2-f61e-628c-8fd6-a1e37ee28925', '4d7ac047-a699-7a37-b2c0-e8392593e7bc', 'DONE', 1, pg_temp.demo_now()-interval '12 days', pg_temp.demo_now()-interval '12 days', false, 'EXAMINATION', pg_temp.demo_date()-interval '12 days', false, '3681371b-5dac-8326-c1a4-2f3a8c75418b'),
('b4d37a4b-a246-36ea-ed0f-bd2962c4d886', 'c284d077-0220-f4e5-379b-f748001a517a', '4d7ac047-a699-7a37-b2c0-e8392593e7bc', 'DONE', 1, pg_temp.demo_now()-interval '11 days', pg_temp.demo_now()-interval '11 days', false, 'EXAMINATION', pg_temp.demo_date()-interval '11 days', false, 'b32c665b-780d-cb3d-3f72-009033a2e2be'),
('f57c1d71-e240-cee0-7c92-6c9fe46082b7', '9419485f-8220-72e4-d117-4f924aaf3d64', '4d7ac047-a699-7a37-b2c0-e8392593e7bc', 'DONE', 1, pg_temp.demo_now()-interval '10 days', pg_temp.demo_now()-interval '10 days', false, 'EXAMINATION', pg_temp.demo_date()-interval '10 days', false, 'fd110e54-3675-cdf3-3203-56c5e529f364'),
('80a61620-8424-48af-60cf-ff2f60679b6c', '6896b913-3282-13e0-507d-ba867aa88a9e', '4d7ac047-a699-7a37-b2c0-e8392593e7bc', 'DONE', 1, pg_temp.demo_now()-interval '9 days', pg_temp.demo_now()-interval '9 days', false, 'EXAMINATION', pg_temp.demo_date()-interval '9 days', true, '3bc5bf5c-0ef7-1913-53c9-3a1ab64bbd37'),
('bdf450f0-2861-d113-02e2-502be7108e3e', '155226fc-7ea7-af74-8f79-6f301dacc813', '4d7ac047-a699-7a37-b2c0-e8392593e7bc', 'DONE', 1, pg_temp.demo_now()-interval '8 days', pg_temp.demo_now()-interval '8 days', false, 'EXAMINATION', pg_temp.demo_date()-interval '8 days', true, 'e6219e8d-2a30-aa79-074d-0f0783b15242'),
('a0a948f7-d447-2b9e-2044-0ce737fc4be6', '7106a7e0-7222-4b2e-4070-28ee64525341', '4d7ac047-a699-7a37-b2c0-e8392593e7bc', 'DONE', 1, pg_temp.demo_now()-interval '7 days', pg_temp.demo_now()-interval '7 days', false, 'EXAMINATION', pg_temp.demo_date()-interval '7 days', true, '92a7d21b-e18d-42c7-5cec-8168285a471f'),
('8110ae19-ee36-ba45-d85e-b9dabe7a0cbb', '2d27d8c8-6536-e798-cc59-c5ca623f32d7', '4d7ac047-a699-7a37-b2c0-e8392593e7bc', 'DONE', 1, pg_temp.demo_now()-interval '6 days', pg_temp.demo_now()-interval '6 days', false, 'EXAMINATION', pg_temp.demo_date()-interval '6 days', true, '28e8e02e-8ab4-69f4-5105-b6aa9dee53a9'),
('764b4e94-4b5e-450f-d6b0-cf0c994a0598', '378f1bfa-d1ba-5fa5-9867-20d85332e09c', '4d7ac047-a699-7a37-b2c0-e8392593e7bc', 'DONE', 1, pg_temp.demo_now()-interval '5 days', pg_temp.demo_now()-interval '5 days', false, 'EXAMINATION', pg_temp.demo_date()-interval '5 days', true, 'c889150d-134e-086d-e07c-34a7a0a4330e');
INSERT INTO medical_record (record_id, visit_id, queue_ticket_id, status, doctor_id, created_at, updated_at, deleted) VALUES
('63425b1c-09cc-dfbc-5b82-6417fce75bb4', 'd5ee978d-cb75-979f-16ff-d348536a3a85', '959df67f-eacb-f95e-661d-96a9b3890f14', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '29 days', pg_temp.demo_now()-interval '29 days', false),
('d90b3fc2-2b39-d940-d65f-7e6ed2262008', '07ac9a53-0f97-2fb6-1f9d-34924096efc6', '339f4a88-3b15-f2e5-b9a5-a94de87bed35', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '28 days', pg_temp.demo_now()-interval '28 days', false),
('a777e0f4-3af8-f685-7e4b-8c4eb3f70f3f', '41b2420a-d4b7-8804-1d26-035597505b0d', 'ea19ff68-3dae-b5ff-98b4-61c3bdc0467b', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '27 days', pg_temp.demo_now()-interval '27 days', false),
('81669eea-8014-0c23-cd6a-f89cd51fe8ef', '563593a1-6854-4e0f-2a22-a2558be66071', '2261e207-c96f-deff-b08c-105a070890f4', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '26 days', pg_temp.demo_now()-interval '26 days', false),
('e5803292-b455-c25d-b2b0-f4dfa5db76d2', '3f479e2a-b84e-4e6b-5f34-701b740d2c42', 'e61ed18f-371f-b4a2-64cb-d709c61d8cd5', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '25 days', pg_temp.demo_now()-interval '25 days', false),
('770e9e34-c176-32df-7e15-fb33e8ed16e3', '58f43c00-7613-60c0-80b7-aa987cd1cdc7', 'db39ffca-67d9-434c-7b30-6221cd0180df', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '24 days', pg_temp.demo_now()-interval '24 days', false),
('8cbb6eba-f9b6-d74c-e70e-7a664e112b9e', '01ab728f-81ca-9b16-01b0-829ee8afccd7', 'faac3d2f-a864-315e-fb10-373134b838ec', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '23 days', pg_temp.demo_now()-interval '23 days', false),
('f78f3b87-7909-4333-546f-e936c5b9205c', '4e75879e-ee16-2033-328e-14074267adf5', 'a64a9548-fffd-7d57-0e4d-d5a9b9ce99a6', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '22 days', pg_temp.demo_now()-interval '22 days', false),
('7238f2fd-48e8-6246-7eee-34a4f84b5644', 'ce35c38b-b5f4-c14b-3cc6-9cdd5dcf62ea', '828967b2-c757-60bf-43e5-9cda67268b22', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '21 days', pg_temp.demo_now()-interval '21 days', false),
('1a44135b-8b3b-682b-1be3-bd6d07930971', '472df76d-d7a1-4f37-23be-4625460788ef', 'e0df9999-cf60-6a6d-ea78-c72766f5f810', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '20 days', pg_temp.demo_now()-interval '20 days', false),
('d52a681e-3db2-9eac-3ea1-8a5c612e51a1', '95f27f2f-a87f-9a2f-94d0-794b78bb874d', 'ad46d18e-e311-9be7-623d-2db8640c330a', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '19 days', pg_temp.demo_now()-interval '19 days', false),
('5670089c-8fb8-4c82-83ad-2bce35b41526', '3b64222f-e79f-d4e5-b59c-238bd8e305f0', '786e9e96-5b19-0336-efc8-6d0e7325e12f', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '18 days', pg_temp.demo_now()-interval '18 days', false),
('07a54026-ffd1-bb90-b20c-b74fe9efaec7', 'acdfb5b3-9e44-edeb-0b33-a87b3061c910', 'e41ac946-1a52-8953-3506-0e39cc5a0870', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '17 days', pg_temp.demo_now()-interval '17 days', false),
('bfa52c92-98c2-fc4b-8f1c-c59cbd710c8e', '8fd00ef0-0301-e828-12aa-feb061baae9b', '6cbc992f-32a9-3314-5383-556f782b9dfa', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '16 days', pg_temp.demo_now()-interval '16 days', false),
('27d23d7f-430e-7c84-650c-88a6b407ce0b', 'a6f52718-74ad-b4fb-2395-bd5a7273947d', 'dac1c60b-219c-2fca-3bdb-c503dd44a719', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '15 days', pg_temp.demo_now()-interval '15 days', false),
('ef780c06-411d-e0b7-6300-60ae43106b38', '1a1dd74b-671a-007a-f820-17b1ed805085', '8692b9dc-6bfb-d5f1-29ba-927587bf33fc', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '14 days', pg_temp.demo_now()-interval '14 days', false),
('e327e6f5-de54-c46a-b410-edf78ea4a411', '4870f11b-a685-6595-aa05-df1eaf57b453', '55963e3f-3938-51a0-8c9a-e923eac8cba0', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '13 days', pg_temp.demo_now()-interval '13 days', false),
('50385829-f297-770e-9d80-e0ec4f591745', 'f2ae7fa2-f61e-628c-8fd6-a1e37ee28925', '57eed6c0-10e9-220e-99c2-4f107ea77aac', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '12 days', pg_temp.demo_now()-interval '12 days', false),
('b2670e4b-37a6-80f2-2719-207627c4eb93', 'c284d077-0220-f4e5-379b-f748001a517a', 'b4d37a4b-a246-36ea-ed0f-bd2962c4d886', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '11 days', pg_temp.demo_now()-interval '11 days', false),
('86afe0dd-4428-4c21-bfa3-ba938295efeb', '9419485f-8220-72e4-d117-4f924aaf3d64', 'f57c1d71-e240-cee0-7c92-6c9fe46082b7', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '10 days', pg_temp.demo_now()-interval '10 days', false),
('9b1ae98e-6580-2259-ca6b-6d570e1d67f5', '6896b913-3282-13e0-507d-ba867aa88a9e', '80a61620-8424-48af-60cf-ff2f60679b6c', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '9 days', pg_temp.demo_now()-interval '9 days', false),
('fc2ab145-1bab-3661-d45d-9fc7f7395f42', '155226fc-7ea7-af74-8f79-6f301dacc813', 'bdf450f0-2861-d113-02e2-502be7108e3e', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '8 days', pg_temp.demo_now()-interval '8 days', false),
('cdb42828-6ca7-0a2a-498d-cd9cc7f9d7b7', '7106a7e0-7222-4b2e-4070-28ee64525341', 'a0a948f7-d447-2b9e-2044-0ce737fc4be6', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '7 days', pg_temp.demo_now()-interval '7 days', false),
('adfa1da4-b8e1-0108-9022-e6a623fec95f', '2d27d8c8-6536-e798-cc59-c5ca623f32d7', '8110ae19-ee36-ba45-d85e-b9dabe7a0cbb', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '6 days', pg_temp.demo_now()-interval '6 days', false),
('31b41c23-9ebb-da72-d02d-4fc548bd6e5c', '378f1bfa-d1ba-5fa5-9867-20d85332e09c', '764b4e94-4b5e-450f-d6b0-cf0c994a0598', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '5 days', pg_temp.demo_now()-interval '5 days', false);
INSERT INTO invoice (invoice_id, visit_id, invoice_code, subtotal, discount, tax, total_amount, status, paid_amount, created_at, updated_at, deleted) VALUES
('cee17749-d934-aaf6-9345-b0e0c5a9518a', 'd5ee978d-cb75-979f-16ff-d348536a3a85', 'INV-0001', 220000, 0, 0, 220000, 'PAID', 220000, pg_temp.demo_now()-interval '29 days', pg_temp.demo_now()-interval '29 days', false),
('9fff3b86-4962-ffcd-3fdb-b9ba8f964d51', '07ac9a53-0f97-2fb6-1f9d-34924096efc6', 'INV-0002', 220000, 0, 0, 220000, 'PAID', 220000, pg_temp.demo_now()-interval '28 days', pg_temp.demo_now()-interval '28 days', false),
('a47a7052-9ef6-f12c-a3f1-5d0bf60690e6', '41b2420a-d4b7-8804-1d26-035597505b0d', 'INV-0003', 220000, 0, 0, 220000, 'PAID', 220000, pg_temp.demo_now()-interval '27 days', pg_temp.demo_now()-interval '27 days', false),
('c82ec97a-29df-5950-d698-6c0744afbe66', '563593a1-6854-4e0f-2a22-a2558be66071', 'INV-0004', 220000, 0, 0, 220000, 'PAID', 220000, pg_temp.demo_now()-interval '26 days', pg_temp.demo_now()-interval '26 days', false),
('6c94fe13-1664-79ba-5840-9fa6169de7cc', '3f479e2a-b84e-4e6b-5f34-701b740d2c42', 'INV-0005', 220000, 0, 0, 220000, 'PAID', 220000, pg_temp.demo_now()-interval '25 days', pg_temp.demo_now()-interval '25 days', false),
('2f41c206-94aa-e4bc-5e94-99be274759d3', '58f43c00-7613-60c0-80b7-aa987cd1cdc7', 'INV-0006', 220000, 0, 0, 220000, 'PAID', 220000, pg_temp.demo_now()-interval '24 days', pg_temp.demo_now()-interval '24 days', false),
('de2b407a-297b-6a64-05be-afec5e1d30bc', '01ab728f-81ca-9b16-01b0-829ee8afccd7', 'INV-0007', 220000, 0, 0, 220000, 'PAID', 220000, pg_temp.demo_now()-interval '23 days', pg_temp.demo_now()-interval '23 days', false),
('7865527a-68c8-4793-8761-9a31296a8073', '4e75879e-ee16-2033-328e-14074267adf5', 'INV-0008', 220000, 0, 0, 220000, 'PAID', 220000, pg_temp.demo_now()-interval '22 days', pg_temp.demo_now()-interval '22 days', false),
('54667a3f-1959-5977-e89e-ee022efe8259', 'ce35c38b-b5f4-c14b-3cc6-9cdd5dcf62ea', 'INV-0009', 220000, 0, 0, 220000, 'PAID', 220000, pg_temp.demo_now()-interval '21 days', pg_temp.demo_now()-interval '21 days', false),
('45d3464b-54a4-74be-5db0-7750675bbc60', '472df76d-d7a1-4f37-23be-4625460788ef', 'INV-0010', 220000, 0, 0, 220000, 'PAID', 220000, pg_temp.demo_now()-interval '20 days', pg_temp.demo_now()-interval '20 days', false),
('6b851596-49c7-32b3-6d46-cc946130a930', '95f27f2f-a87f-9a2f-94d0-794b78bb874d', 'INV-0011', 220000, 0, 0, 220000, 'PAID', 220000, pg_temp.demo_now()-interval '19 days', pg_temp.demo_now()-interval '19 days', false),
('e76ccafb-16a6-51b3-055d-8d467e3f8323', '3b64222f-e79f-d4e5-b59c-238bd8e305f0', 'INV-0012', 220000, 0, 0, 220000, 'PAID', 220000, pg_temp.demo_now()-interval '18 days', pg_temp.demo_now()-interval '18 days', false),
('428ac4b5-dd30-c3ff-8c69-203eb59a8293', 'acdfb5b3-9e44-edeb-0b33-a87b3061c910', 'INV-0013', 220000, 0, 0, 220000, 'PAID', 220000, pg_temp.demo_now()-interval '17 days', pg_temp.demo_now()-interval '17 days', false),
('906f0680-f6a3-f282-a877-2bb65626ddc3', '8fd00ef0-0301-e828-12aa-feb061baae9b', 'INV-0014', 220000, 0, 0, 220000, 'PAID', 220000, pg_temp.demo_now()-interval '16 days', pg_temp.demo_now()-interval '16 days', false),
('a880745f-041d-c74b-95f2-3e83ffd95f0f', 'a6f52718-74ad-b4fb-2395-bd5a7273947d', 'INV-0015', 220000, 0, 0, 220000, 'PAID', 220000, pg_temp.demo_now()-interval '15 days', pg_temp.demo_now()-interval '15 days', false),
('ebafa9a5-d50c-629d-40c3-f176aae58aa4', '1a1dd74b-671a-007a-f820-17b1ed805085', 'INV-0016', 220000, 0, 0, 220000, 'PAID', 220000, pg_temp.demo_now()-interval '14 days', pg_temp.demo_now()-interval '14 days', false),
('d01d7923-9fde-b555-ca26-201069d02a85', '4870f11b-a685-6595-aa05-df1eaf57b453', 'INV-0017', 220000, 0, 0, 220000, 'PAID', 220000, pg_temp.demo_now()-interval '13 days', pg_temp.demo_now()-interval '13 days', false),
('4beb7bc0-43d9-e085-df12-ab9d96367b65', 'f2ae7fa2-f61e-628c-8fd6-a1e37ee28925', 'INV-0018', 220000, 0, 0, 220000, 'PAID', 220000, pg_temp.demo_now()-interval '12 days', pg_temp.demo_now()-interval '12 days', false),
('541704fc-f059-381c-1932-dc09d1b0b5f4', 'c284d077-0220-f4e5-379b-f748001a517a', 'INV-0019', 220000, 0, 0, 220000, 'PAID', 220000, pg_temp.demo_now()-interval '11 days', pg_temp.demo_now()-interval '11 days', false),
('5e949049-4fb2-feb1-ab36-c5df3127440b', '9419485f-8220-72e4-d117-4f924aaf3d64', 'INV-0020', 220000, 0, 0, 220000, 'PAID', 220000, pg_temp.demo_now()-interval '10 days', pg_temp.demo_now()-interval '10 days', false),
('85eff216-4b76-9d82-336e-34bb3636ac78', '6896b913-3282-13e0-507d-ba867aa88a9e', 'INV-0021', 220000, 0, 0, 220000, 'PAID', 220000, pg_temp.demo_now()-interval '9 days', pg_temp.demo_now()-interval '9 days', false),
('5802ffc9-9b46-0226-2044-623001adeab3', '155226fc-7ea7-af74-8f79-6f301dacc813', 'INV-0022', 220000, 0, 0, 220000, 'PAID', 220000, pg_temp.demo_now()-interval '8 days', pg_temp.demo_now()-interval '8 days', false),
('5abf8ebc-aa50-1ccf-f8f9-aa30fc8458f8', '7106a7e0-7222-4b2e-4070-28ee64525341', 'INV-0023', 220000, 0, 0, 220000, 'PAID', 220000, pg_temp.demo_now()-interval '7 days', pg_temp.demo_now()-interval '7 days', false),
('fdd23aca-85e1-f3ac-6892-d3f62052c1fc', '2d27d8c8-6536-e798-cc59-c5ca623f32d7', 'INV-0024', 220000, 0, 0, 220000, 'PAID', 220000, pg_temp.demo_now()-interval '6 days', pg_temp.demo_now()-interval '6 days', false),
('e2fa7364-8b91-c438-17d7-6ffde55cc415', '378f1bfa-d1ba-5fa5-9867-20d85332e09c', 'INV-0025', 220000, 0, 0, 220000, 'PAID', 220000, pg_temp.demo_now()-interval '5 days', pg_temp.demo_now()-interval '5 days', false);
INSERT INTO appointment (appointment_id, created_at, updated_at, deleted, scheduled_at, status, is_guest, customer_id, shift_name, shift_time, shift_version_id) VALUES ('1c555a61-e713-eecb-3a22-9cda3f8f7f9a', pg_temp.demo_now(), pg_temp.demo_now(), false, pg_temp.demo_date() + time '14:00', 'PENDING', false, 'b8523868-30ae-7eab-8d18-fb3f1a5488bc', 'Ca Chiều', '13:00-17:00', '71000002-2222-2222-2222-222222222222');
COMMIT;



