SET cares.demo_reset = 'yes';

BEGIN;
SET LOCAL TIME ZONE 'Asia/Ho_Chi_Minh';

DO $$
BEGIN
    IF to_regclass('pg_temp.demo2_clock') IS NOT NULL THEN
        DROP TABLE pg_temp.demo2_clock;
    END IF;
END
$$;
CREATE TEMP TABLE demo2_clock ON COMMIT DROP AS
SELECT CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh' moment;
CREATE OR REPLACE FUNCTION pg_temp.demo_now() RETURNS timestamp LANGUAGE sql STABLE AS 'SELECT moment FROM demo2_clock';
CREATE OR REPLACE FUNCTION pg_temp.demo_date() RETURNS date LANGUAGE sql STABLE AS 'SELECT moment::date FROM demo2_clock';
CREATE OR REPLACE FUNCTION pg_temp.did(key text) RETURNS uuid LANGUAGE sql IMMUTABLE AS 'SELECT md5(''CareS-demo:''||key)::uuid';
CREATE OR REPLACE FUNCTION pg_temp.event(c record, rel_offset float) RETURNS timestamp LANGUAGE plpgsql STABLE AS '
BEGIN
    RETURN c.started + (c.completed_at - c.started) * rel_offset;
END;
';

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
    medical_service_form_template, clinical_form_template_version, clinical_form_template,
    service_category, service_capability, medicine_catalog, icd_10_codes,
    insurance, department, specialization, shift_config, profile, account
RESTART IDENTITY CASCADE;

INSERT INTO clinic_information (
    clinic_information_id, created_at, updated_at, deleted, clinic_name, legal_name,
    tax_code, operating_license, short_description, support_email, phone, address, facebook_url
) VALUES (
    '00000000-0000-0000-0000-000000000100', pg_temp.demo_now(), pg_temp.demo_now(), false,
    'Phòng khám CareS', 'Công ty TNHH Phòng khám CareS', '0101234567', '000123/HNO-GPHD',
    'Phòng khám đa khoa cung cấp dịch vụ chăm sóc sức khỏe chất lượng và thuận tiện.',
    'phongkhamcares@gmail.com', '0968161266',
    'Thôn 1, Canh Nậu, Thạch Thất, Hà Nội',
    'https://www.facebook.com/profile.php?id=61593125259676'
);

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
('4d7ac047-a699-7a37-b2c0-e8392593e7bc', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'INT-101', 'Phòng khám Nội 1', 'AVAILABLE', 'EXAMINATION', '00000001-1111-1111-1111-111111111111', 'Nội khoa', NULL),
('1d5dd991-2183-7e47-8540-7b71704b5dff', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'INT-102', 'Phòng khám Nội 2', 'AVAILABLE', 'EXAMINATION', '00000001-1111-1111-1111-111111111111', 'Nội khoa', NULL),
('b6b019fc-fd4c-a6ce-527b-f928546cef64', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'SUR-201', 'Phòng khám Ngoại 1', 'AVAILABLE', 'EXAMINATION', '00000003-3333-3333-3333-333333333333', 'Ngoại khoa', NULL),
('ba0338ee-d868-b5f0-3560-4e4f139794fc', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'SUR-202', 'Phòng khám Ngoại 2', 'AVAILABLE', 'EXAMINATION', '00000003-3333-3333-3333-333333333333', 'Ngoại khoa', NULL),
('65034e17-bcc4-7981-c637-7bfcd6dfa331', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'PED-301', 'Phòng khám Nhi', 'AVAILABLE', 'EXAMINATION', '00000002-2222-2222-2222-222222222222', 'Nhi khoa', NULL),
('98d87656-422c-787c-fc4b-6408c00b644c', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'DER-401', 'Phòng khám Da liễu', 'AVAILABLE', 'EXAMINATION', '00000004-4444-4444-4444-444444444444', 'Da liễu', NULL),
('177e189b-5501-d8a7-c9e5-32c9c6de8c3f', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'OBG-501', 'Phòng khám Sản phụ khoa', 'AVAILABLE', 'EXAMINATION', '00000008-8888-8888-8888-888888888888', 'Sản phụ khoa', NULL),
('760edff5-8292-2100-0b77-7fc8e316e913', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'LAB-601', 'Phòng xét nghiệm', 'AVAILABLE', 'LABORATORY', NULL, 'Xét nghiệm', NULL),
('41706ccd-49ed-d1e4-3767-9434f57ebe42', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'IMG-701', 'Phòng siêu âm', 'AVAILABLE', 'PARACLINICAL', NULL, 'Siêu âm, điện tim', NULL),
('4a0a5c21-f9d4-234b-9631-c80fa2db9881', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'XR-702', 'Phòng X-quang', 'AVAILABLE', 'PARACLINICAL', NULL, 'X-quang', NULL);
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
('b8523868-30ae-7eab-8d18-fb3f1a5488bc', 'a780d597-bf34-b2c4-df00-a72a966f6d4b', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Nguyễn Hoàng Nam', '1987-03-14', 'MALE', '0987654001', 'nguyenhoangnam87@gmail.com', 'Hà Nội', NULL),
('1088d62b-ff88-2b61-b142-c59ddd695087', '7017c9f1-ded6-46cd-0dd5-950d5c86c100', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Trần Thu Hà', '1994-11-26', 'FEMALE', '0987654002', 'tranthuha94@gmail.com', 'Hà Nội', NULL),
('b96b77f8-3078-38f6-cd3f-19135ce19b9b', 'b546d6f2-eb3d-cb3b-4b66-c68e6a64ecc9', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Lê Minh Anh', '1991-07-08', 'FEMALE', '0987654003', 'leminhanh91@gmail.com', 'Hà Nội', NULL),
('bb0c6566-5036-8202-3e09-e655b1876243', 'e3c9f2b2-1d4a-e8ed-d963-a1abccf2085b', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Phạm Đức Long', '1978-02-19', 'MALE', '0987654004', 'phamduclong78@gmail.com', 'Hà Nội', NULL),
('c959002e-b7c1-eb0c-1183-ae9dc3acad17', '1ac47263-98dc-ed58-a646-e51a07483f28', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Vũ Ngọc Lan', '2000-09-03', 'FEMALE', '0987654005', 'vungoclan2000@gmail.com', 'Hà Nội', NULL),
('d71ae2ca-269b-d0f9-cf15-56eb27687f79', '239ef41f-451e-2067-2026-313a1c1d2f3a', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Đỗ Quang Huy', '1983-12-21', 'MALE', '0987654006', 'doquanghuy83@gmail.com', 'Hà Nội', NULL),
('1d8db924-b0a8-6e8f-921c-59bb5d999acc', '18f88a0a-3d50-8759-e824-d86dca178007', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Bùi Thị Hương', '1969-05-17', 'FEMALE', '0987654007', 'buithihuong69@gmail.com', 'Hà Nội', NULL),
('cfd84ebf-ce0d-4052-7c06-0a850c1b1eb5', 'e251a5dc-687b-2317-31f8-527cce389c44', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Hoàng Văn Dũng', '1975-08-30', 'MALE', '0987654008', 'hoangvandung75@gmail.com', 'Hà Nội', NULL),
('687ecdf1-ac69-a2d9-1c39-8344988f19ef', '6252b4f2-fabe-5949-e56f-c94a9c85cde6', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Đặng Mai Phương', '1998-01-12', 'FEMALE', '0987654009', 'dangmaiphuong98@gmail.com', 'Hà Nội', NULL),
('80221d38-65d4-f424-966b-52340f8125fc', '83ef3d6a-4444-627c-0611-08c23dc83d25', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Ngô Thành Trung', '1989-06-25', 'MALE', '0987654010', 'ngothanhtrung89@gmail.com', 'Hà Nội', NULL),
('a0ea2744-0cd2-cb2b-5e0f-c60fd2453426', 'f8c0fa8e-85a7-c8f4-0a46-46c3d186bef0', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Dương Khánh Linh', '2003-04-09', 'FEMALE', '0987654011', 'duongkhanhlinh03@gmail.com', 'Hà Nội', NULL),
('d1841dfc-3567-7c11-fcec-a1a9dac19321', '24de58db-86c8-38be-5711-db3ba236026a', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Mai Quốc Bảo', '1996-10-18', 'MALE', '0987654012', 'maiquocbao96@gmail.com', 'Hà Nội', NULL),
('4435cd7e-3781-3220-6f2b-8588d1586188', 'a0073cc4-b720-2968-c468-8027a5622a31', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Nguyễn Anh Đức', '1992-02-28', 'MALE', '0987654013', 'nguyenanhduc92@gmail.com', 'Hà Nội', NULL),
('80f349be-5d65-eb4c-fd11-db9e11904823', 'ee80ca83-b6b5-d41b-fcf4-a200000200d4', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Trịnh Thu Trang', '1985-07-23', 'FEMALE', '0987654014', 'trinhthutrang85@gmail.com', 'Hà Nội', NULL),
('d9bb8bf7-7740-d7f8-b199-896d6fdb60d2', 'dafcb0a8-e0c2-fc31-5138-83f5e527f6e0', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Phan Mạnh Cường', '1972-09-15', 'MALE', '0987654015', 'phanmanhcuong72@gmail.com', 'Hà Nội', NULL),
('4da10e8d-fe0c-1bdf-6399-34d7a7beaa5b', 'ceed3b76-9b43-5b53-eafb-33f1076299a8', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Lý Ngọc Mai', '2001-12-05', 'FEMALE', '0987654016', 'lyngocmai01@gmail.com', 'Hà Nội', NULL),
('2c2e26aa-6dfb-a7e6-921b-d4eff532dca7', '2da4d59d-f8be-f54c-9201-40033b27e510', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Đinh Văn Sơn', '1965-03-31', 'MALE', '0987654017', 'dinhvanson65@gmail.com', 'Hà Nội', NULL),
('f6abb1a5-15a3-c329-6793-0422d668e8f5', '27ae040b-8920-e65f-3eb9-ec4b9ef8f2a4', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Tạ Thanh Thảo', '1999-08-16', 'FEMALE', '0987654018', 'tathanhthao99@gmail.com', 'Hà Nội', NULL),
('3681371b-5dac-8326-c1a4-2f3a8c75418b', 'df728b7e-6f20-19c6-b5e3-4b88ee7902fd', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Cao Xuân Thành', '1981-01-27', 'MALE', '0987654019', 'caoxuanthanh81@gmail.com', 'Hà Nội', NULL),
('b32c665b-780d-cb3d-3f72-009033a2e2be', '526b88ee-5562-d930-f0c2-2cc9ee7bbce8', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Hà Thị Minh Châu', '1993-06-11', 'FEMALE', '0987654020', 'hathiminhchau93@gmail.com', 'Hà Nội', NULL),
('fd110e54-3675-cdf3-3203-56c5e529f364', 'fb587594-8fae-d7b0-dbd2-1b8ff39abdab', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chu Đức Thắng', '1976-11-07', 'MALE', '0987654021', 'chuducthang76@gmail.com', 'Hà Nội', NULL),
('3bc5bf5c-0ef7-1913-53c9-3a1ab64bbd37', NULL, (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Quách Bảo Ngọc', '2004-05-20', 'FEMALE', '0987654022', 'quachbaongoc04@gmail.com', 'Hà Nội', NULL),
('e6219e8d-2a30-aa79-074d-0f0783b15242', NULL, (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Lương Gia Minh', '1988-09-29', 'MALE', '0987654023', 'luonggiaminh88@gmail.com', 'Hà Nội', NULL),
('92a7d21b-e18d-42c7-5cec-8168285a471f', NULL, (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Nông Thị Hạnh', '1970-04-04', 'FEMALE', '0987654024', 'nongthihanh70@gmail.com', 'Hà Nội', NULL),
('28e8e02e-8ab4-69f4-5105-b6aa9dee53a9', NULL, (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Tô Minh Khang', '2002-02-13', 'MALE', '0987654025', 'tominhkhang02@gmail.com', 'Hà Nội', NULL),
('c889150d-134e-086d-e07c-34a7a0a4330e', NULL, (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Kiều Diễm My', '1997-10-24', 'FEMALE', '0987654026', 'kieudiemmy97@gmail.com', 'Hà Nội', NULL);
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

-- Assign the fixed doctor to each room only after staff_info exists.
UPDATE department AS d SET head_doctor_id = v.staff_id
FROM (VALUES
    ('INT-101', '8c574ec0-98e1-ad37-3d64-f900c3211b35'::uuid),
    ('INT-102', 'a73ef1f7-a284-a69a-5d55-1f5f2aa58789'::uuid),
    ('SUR-201', '222d13de-2292-22ea-85b7-12b87f2c1604'::uuid),
    ('SUR-202', 'a195fff6-226e-7492-f75d-1c471eff523f'::uuid),
    ('PED-301', 'de4d04ed-d9b4-734b-4dd9-7a3048ffdf8a'::uuid),
    ('DER-401', '228d522a-424e-5037-c20e-325c43826a93'::uuid),
    ('OBG-501', 'c4882884-2185-448d-7d00-a65cfae000bc'::uuid),
    ('LAB-601', '74cf86f9-d830-e246-7dc3-538a0874779d'::uuid),
    ('IMG-701', '6058143b-c4d4-3840-262f-3aa52b388608'::uuid),
    ('XR-702', 'ceb1dbd4-ee6c-2065-99f8-cd32f616c3f6'::uuid)
) AS v(room_code, staff_id)
WHERE d.room_code = v.room_code;

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

-- Every paraclinical room has one doctor and one nurse with the capabilities
-- required by its services. Certificates are intentionally omitted because
-- this demo only validates operational assignment, not credential management.
INSERT INTO staff_capability
    (staff_capability_id, created_at, updated_at, deleted, staff_id, capability_id,
     certificate_number, issued_date, expiry_date, issuing_organization, status)
VALUES
('cb000001-0000-0000-0000-000000000001', pg_temp.demo_now(), pg_temp.demo_now(), false, '74cf86f9-d830-e246-7dc3-538a0874779d', 'ca000001-0000-0000-0000-000000000001', NULL, NULL, NULL, NULL, 'ACTIVE'),
('cb000002-0000-0000-0000-000000000002', pg_temp.demo_now(), pg_temp.demo_now(), false, '74cf86f9-d830-e246-7dc3-538a0874779d', 'ca000002-0000-0000-0000-000000000002', NULL, NULL, NULL, NULL, 'ACTIVE'),
('cb000003-0000-0000-0000-000000000003', pg_temp.demo_now(), pg_temp.demo_now(), false, '74cf86f9-d830-e246-7dc3-538a0874779d', 'ca000005-0000-0000-0000-000000000005', NULL, NULL, NULL, NULL, 'ACTIVE'),
('cb000004-0000-0000-0000-000000000004', pg_temp.demo_now(), pg_temp.demo_now(), false, '74cf86f9-d830-e246-7dc3-538a0874779d', 'ca000008-0000-0000-0000-000000000008', NULL, NULL, NULL, NULL, 'ACTIVE'),
('cb000005-0000-0000-0000-000000000005', pg_temp.demo_now(), pg_temp.demo_now(), false, '0f348c8d-74b9-3ef2-981b-61105eab787e', 'ca000001-0000-0000-0000-000000000001', NULL, NULL, NULL, NULL, 'ACTIVE'),
('cb000006-0000-0000-0000-000000000006', pg_temp.demo_now(), pg_temp.demo_now(), false, '0f348c8d-74b9-3ef2-981b-61105eab787e', 'ca000002-0000-0000-0000-000000000002', NULL, NULL, NULL, NULL, 'ACTIVE'),
('cb000007-0000-0000-0000-000000000007', pg_temp.demo_now(), pg_temp.demo_now(), false, '0f348c8d-74b9-3ef2-981b-61105eab787e', 'ca000005-0000-0000-0000-000000000005', NULL, NULL, NULL, NULL, 'ACTIVE'),
('cb000008-0000-0000-0000-000000000008', pg_temp.demo_now(), pg_temp.demo_now(), false, '0f348c8d-74b9-3ef2-981b-61105eab787e', 'ca000008-0000-0000-0000-000000000008', NULL, NULL, NULL, NULL, 'ACTIVE'),
('cb000009-0000-0000-0000-000000000009', pg_temp.demo_now(), pg_temp.demo_now(), false, '6058143b-c4d4-3840-262f-3aa52b388608', 'ca000003-0000-0000-0000-000000000003', NULL, NULL, NULL, NULL, 'ACTIVE'),
('cb000010-0000-0000-0000-000000000010', pg_temp.demo_now(), pg_temp.demo_now(), false, 'ff42c405-1351-450b-5d7a-f8db94acd9a8', 'ca000003-0000-0000-0000-000000000003', NULL, NULL, NULL, NULL, 'ACTIVE'),
('cb000011-0000-0000-0000-000000000011', pg_temp.demo_now(), pg_temp.demo_now(), false, 'ceb1dbd4-ee6c-2065-99f8-cd32f616c3f6', 'ca000004-0000-0000-0000-000000000004', NULL, NULL, NULL, NULL, 'ACTIVE'),
('cb000012-0000-0000-0000-000000000012', pg_temp.demo_now(), pg_temp.demo_now(), false, 'c0fb3910-cc9e-5c36-3662-64f097055b4a', 'ca000004-0000-0000-0000-000000000004', NULL, NULL, NULL, NULL, 'ACTIVE');
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
('40000003-0000-0000-0000-000000000003', 'EX-PE-001', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Khám tổng quát dành cho trẻ em.', 'ACTIVE', false, 'Khám Nhi tổng quát', 220000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 0, 15, NULL, NULL, '00000002-2222-2222-2222-222222222222', NULL),
('41000001-0000-0000-0000-000000000001', 'EX-IN-002', pg_temp.demo_now(), pg_temp.demo_now(), false, 'Khám sàng lọc tim mạch cơ bản.', 'ACTIVE', false, 'Khám Tim mạch cơ bản', 250000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 16, 120, NULL, NULL, '00000001-1111-1111-1111-111111111111', NULL),
('41000002-0000-0000-0000-000000000002', 'EX-IN-003', pg_temp.demo_now(), pg_temp.demo_now(), false, 'Khám các triệu chứng và bệnh lý tiêu hóa.', 'ACTIVE', false, 'Khám Tiêu hóa', 230000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 16, 120, NULL, NULL, '00000001-1111-1111-1111-111111111111', NULL),
('41000003-0000-0000-0000-000000000003', 'EX-IN-004', pg_temp.demo_now(), pg_temp.demo_now(), false, 'Khám các triệu chứng và bệnh lý hô hấp.', 'ACTIVE', false, 'Khám Hô hấp', 230000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 16, 120, NULL, NULL, '00000001-1111-1111-1111-111111111111', NULL),
('41000004-0000-0000-0000-000000000004', 'EX-IN-005', pg_temp.demo_now(), pg_temp.demo_now(), false, 'Khám đau và bệnh lý cơ xương khớp.', 'ACTIVE', false, 'Khám Cơ xương khớp', 230000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 16, 120, NULL, NULL, '00000001-1111-1111-1111-111111111111', NULL),
('41000005-0000-0000-0000-000000000005', 'EX-SU-002', pg_temp.demo_now(), pg_temp.demo_now(), false, 'Đánh giá chấn thương phần mềm.', 'ACTIVE', false, 'Khám chấn thương phần mềm', 250000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 0, 120, NULL, NULL, '00000003-3333-3333-3333-333333333333', NULL),
('41000006-0000-0000-0000-000000000006', 'EX-SU-003', pg_temp.demo_now(), pg_temp.demo_now(), false, 'Đánh giá và xử trí ban đầu vết thương.', 'ACTIVE', false, 'Khám vết thương', 220000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 0, 120, NULL, NULL, '00000003-3333-3333-3333-333333333333', NULL),
('41000007-0000-0000-0000-000000000007', 'EX-SU-004', pg_temp.demo_now(), pg_temp.demo_now(), false, 'Thay băng và chăm sóc vết thương.', 'ACTIVE', false, 'Thay băng, chăm sóc vết thương', 150000, 'EXAMINATION', 20, 1, false, false, false, 0, true, 0, 120, NULL, NULL, '00000003-3333-3333-3333-333333333333', NULL),
('41000008-0000-0000-0000-000000000008', 'EX-PE-002', pg_temp.demo_now(), pg_temp.demo_now(), false, 'Khám bệnh hô hấp thường gặp ở trẻ em.', 'ACTIVE', false, 'Khám bệnh hô hấp trẻ em', 220000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 0, 15, NULL, NULL, '00000002-2222-2222-2222-222222222222', NULL),
('41000009-0000-0000-0000-000000000009', 'EX-PE-003', pg_temp.demo_now(), pg_temp.demo_now(), false, 'Khám bệnh tiêu hóa thường gặp ở trẻ em.', 'ACTIVE', false, 'Khám tiêu hóa trẻ em', 220000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 0, 15, NULL, NULL, '00000002-2222-2222-2222-222222222222', NULL),
('41000010-0000-0000-0000-000000000010', 'EX-PE-004', pg_temp.demo_now(), pg_temp.demo_now(), false, 'Khám sốt và bệnh nhiễm khuẩn thông thường ở trẻ.', 'ACTIVE', false, 'Khám sốt và bệnh nhiễm khuẩn thông thường', 220000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 0, 15, NULL, NULL, '00000002-2222-2222-2222-222222222222', NULL),
('41000011-0000-0000-0000-000000000011', 'EX-OB-001', pg_temp.demo_now(), pg_temp.demo_now(), false, 'Khám sức khỏe phụ khoa.', 'ACTIVE', false, 'Khám Phụ khoa', 250000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 16, 120, 'FEMALE', NULL, '00000008-8888-8888-8888-888888888888', NULL),
('41000012-0000-0000-0000-000000000012', 'EX-OB-002', pg_temp.demo_now(), pg_temp.demo_now(), false, 'Khám và theo dõi thai kỳ.', 'ACTIVE', false, 'Khám Thai', 300000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 16, 55, 'FEMALE', NULL, '00000008-8888-8888-8888-888888888888', NULL),
('41000013-0000-0000-0000-000000000013', 'EX-OB-003', pg_temp.demo_now(), pg_temp.demo_now(), false, 'Khám viêm nhiễm phụ khoa.', 'ACTIVE', false, 'Khám viêm nhiễm phụ khoa', 250000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 16, 120, 'FEMALE', NULL, '00000008-8888-8888-8888-888888888888', NULL),
('41000014-0000-0000-0000-000000000014', 'EX-DER-001', pg_temp.demo_now(), pg_temp.demo_now(), false, 'Khám tổng quát các bệnh lý da.', 'ACTIVE', false, 'Khám Da liễu', 220000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 0, 120, NULL, NULL, '00000004-4444-4444-4444-444444444444', NULL),
('41000015-0000-0000-0000-000000000015', 'EX-DER-002', pg_temp.demo_now(), pg_temp.demo_now(), false, 'Khám và tư vấn điều trị mụn trứng cá.', 'ACTIVE', false, 'Khám mụn trứng cá', 220000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 0, 120, NULL, NULL, '00000004-4444-4444-4444-444444444444', NULL),
('41000016-0000-0000-0000-000000000016', 'EX-DER-003', pg_temp.demo_now(), pg_temp.demo_now(), false, 'Khám viêm da và các tình trạng dị ứng.', 'ACTIVE', false, 'Khám viêm da, dị ứng', 220000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 0, 120, NULL, NULL, '00000004-4444-4444-4444-444444444444', NULL),
('41000017-0000-0000-0000-000000000017', 'EX-DER-004', pg_temp.demo_now(), pg_temp.demo_now(), false, 'Khám và điều trị các bệnh nấm da.', 'ACTIVE', false, 'Khám nấm da', 200000, 'EXAMINATION', 30, 1, false, false, false, 0, true, 0, 120, NULL, NULL, '00000004-4444-4444-4444-444444444444', NULL),
('40000008-0000-0000-0000-000000000008', 'LAB-001', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Đánh giá các thành phần tế bào máu.', 'ACTIVE', false, 'Công thức máu', 120000, 'LABORATORY', 15, 1, true, true, true, 45, true, 0, 120, NULL, NULL, NULL, 'ca000001-0000-0000-0000-000000000001'),
('40000009-0000-0000-0000-000000000009', 'LAB-002', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Đo nồng độ glucose trong máu.', 'ACTIVE', false, 'Đường huyết', 70000, 'LABORATORY', 10, 1, true, true, true, 30, true, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('40000015-0000-0000-0000-000000000015', 'IMG-003', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Khảo sát các cơ quan trong ổ bụng.', 'ACTIVE', false, 'Siêu âm ổ bụng tổng quát', 250000, 'PARACLINICAL', 20, 1, true, true, false, 10, true, 0, 120, NULL, NULL, NULL, 'ca000003-0000-0000-0000-000000000003'),
('40000017-0000-0000-0000-000000000017', 'IMG-001', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chụp hình vùng ngực.', 'ACTIVE', false, 'X-quang ngực', 180000, 'PARACLINICAL', 15, 1, true, true, false, 15, true, 6, 120, NULL, NULL, NULL, 'ca000004-0000-0000-0000-000000000004'),
('41000018-0000-0000-0000-000000000018', 'IMG-002', pg_temp.demo_now(), pg_temp.demo_now(), false, 'Chụp X-quang đánh giá xương và khớp.', 'ACTIVE', false, 'X-quang xương khớp', 200000, 'PARACLINICAL', 20, 1, true, true, false, 15, true, 6, 120, NULL, NULL, NULL, 'ca000004-0000-0000-0000-000000000004'),
('41000019-0000-0000-0000-000000000019', 'IMG-004', pg_temp.demo_now(), pg_temp.demo_now(), false, 'Siêu âm đánh giá tuyến giáp.', 'ACTIVE', false, 'Siêu âm tuyến giáp', 220000, 'PARACLINICAL', 20, 1, true, true, false, 10, true, 0, 120, NULL, NULL, NULL, 'ca000003-0000-0000-0000-000000000003'),
('41000020-0000-0000-0000-000000000020', 'IMG-005', pg_temp.demo_now(), pg_temp.demo_now(), false, 'Siêu âm theo dõi thai.', 'ACTIVE', false, 'Siêu âm thai', 300000, 'PARACLINICAL', 25, 1, true, true, false, 10, true, 16, 55, 'FEMALE', NULL, NULL, 'ca000003-0000-0000-0000-000000000003'),
('4000000a-0065-0000-0000-000000000065', 'AN-CBC-RBC', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'Số lượng hồng cầu (RBC)', 5000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-0067-0000-0000-000000000067', 'AN-CBC-HGB', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'Huyết sắc tố (HGB)', 5000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-0069-0000-0000-000000000069', 'AN-CBC-HCT', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'Hematocrit (HCT)', 5000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-006b-0000-0000-00000000006b', 'AN-CBC-MCV', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'MCV', 5000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-0075-0000-0000-000000000075', 'AN-CBC-WBC', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'Số lượng bạch cầu (WBC)', 10000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-0077-0000-0000-000000000077', 'AN-CBC-NEUTP', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'Bạch cầu trung tính (%)', 5000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-007b-0000-0000-00000000007b', 'AN-CBC-LYMP', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'Bạch cầu lympho (%)', 5000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-008b-0000-0000-00000000008b', 'AN-CBC-PLT', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'Số lượng tiểu cầu (PLT)', 10000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-0095-0000-0000-000000000095', 'LAB-003', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Gói xét nghiệm', 'ACTIVE', false, 'Sinh hóa máu cơ bản', 250000, 'LABORATORY', 15, 1, true, true, true, 45, true, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-0096-0000-0000-000000000096', 'AN-BIO-HBA1C', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'Hemoglobin A1c', 30000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-0098-0000-0000-000000000098', 'AN-BIO-TC', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'Cholesterol toàn phần', 30000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-009a-0000-0000-00000000009a', 'AN-BIO-TG', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'Triglyceride', 30000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-009c-0000-0000-00000000009c', 'AN-BIO-HDL', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'HDL Cholesterol', 30000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-009e-0000-0000-00000000009e', 'AN-BIO-LDL', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'LDL Cholesterol', 30000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-00a0-0000-0000-0000000000a0', 'AN-BIO-UA', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'Acid uric', 30000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-00a2-0000-0000-0000000000a2', 'AN-BIO-TP', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'Protein toàn phần', 30000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-00a4-0000-0000-0000000000a4', 'LAB-004', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Gói xét nghiệm', 'ACTIVE', false, 'Chức năng gan', 200000, 'LABORATORY', 15, 1, true, true, true, 45, true, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-00a5-0000-0000-0000000000a5', 'AN-LIV-AST', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'AST', 25000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-00a7-0000-0000-0000000000a7', 'AN-LIV-ALT', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'ALT', 25000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-00a9-0000-0000-0000000000a9', 'AN-LIV-ALP', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'Alkaline phosphatase', 25000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-00ab-0000-0000-0000000000ab', 'AN-LIV-GGT', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'GGT', 25000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-00ad-0000-0000-0000000000ad', 'AN-LIV-TBIL', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'Bilirubin toàn phần', 25000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-00af-0000-0000-0000000000af', 'AN-LIV-DBIL', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'Bilirubin trực tiếp', 25000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-00b1-0000-0000-0000000000b1', 'AN-LIV-ALB', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'Albumin', 25000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-00b3-0000-0000-0000000000b3', 'LAB-005', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Gói xét nghiệm', 'ACTIVE', false, 'Chức năng thận', 180000, 'LABORATORY', 15, 1, true, true, true, 45, true, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-00b4-0000-0000-0000000000b4', 'AN-REN-UREA', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'Urea', 20000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-00b6-0000-0000-0000000000b6', 'AN-REN-CREA', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'Creatinine', 20000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-00b8-0000-0000-0000000000b8', 'AN-REN-NA', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'Sodium', 20000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-00ba-0000-0000-0000000000ba', 'AN-REN-K', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'Potassium', 20000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-00bc-0000-0000-0000000000bc', 'AN-REN-CL', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'Chloride', 20000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-00c0-0000-0000-0000000000c0', 'AN-REN-CA', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'Calcium toàn phần', 20000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-00c4-0000-0000-0000000000c4', 'LAB-006', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Gói xét nghiệm', 'ACTIVE', false, 'Tổng phân tích nước tiểu', 100000, 'LABORATORY', 15, 1, true, true, true, 45, true, 0, 120, NULL, NULL, NULL, 'ca000005-0000-0000-0000-000000000005'),
('4000000a-00c5-0000-0000-0000000000c5', 'AN-URI-SG', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'Tỷ trọng nước tiểu', 10000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-00c7-0000-0000-0000000000c7', 'AN-URI-PH', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'pH nước tiểu', 10000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-00c9-0000-0000-0000000000c9', 'AN-URI-LEU', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'Leukocyte Esterase', 10000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-00cb-0000-0000-0000000000cb', 'AN-URI-NIT', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'Nitrite', 10000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-00cd-0000-0000-0000000000cd', 'AN-URI-PRO', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'Protein nước tiểu', 10000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-00cf-0000-0000-0000000000cf', 'AN-URI-GLU', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'Glucose nước tiểu', 10000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-00d1-0000-0000-0000000000d1', 'AN-URI-KET', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'Ketone', 10000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000002-0000-0000-0000-000000000002'),
('4000000a-00d7-0000-0000-0000000000d7', 'AN-URI-BLD', (pg_temp.demo_now()-interval '60 days'), (pg_temp.demo_now()-interval '60 days'), false, 'Chỉ số lẻ', 'ACTIVE', false, 'Máu/Hemoglobin nước tiểu', 10000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000005-0000-0000-0000-000000000005'),
('41000021-0000-0000-0000-000000000021', 'LAB-007', pg_temp.demo_now(), pg_temp.demo_now(), false, 'Định lượng protein phản ứng C.', 'ACTIVE', false, 'Xét nghiệm CRP', 100000, 'LABORATORY', 10, 1, true, true, true, 30, true, 0, 120, NULL, NULL, NULL, 'ca000008-0000-0000-0000-000000000008'),
('41000022-0000-0000-0000-000000000022', 'AN-CRP', pg_temp.demo_now(), pg_temp.demo_now(), false, 'Chỉ số CRP định lượng.', 'ACTIVE', false, 'CRP định lượng', 100000, 'LABORATORY', 10, 1, true, true, true, 30, false, 0, 120, NULL, NULL, NULL, 'ca000008-0000-0000-0000-000000000008');

-- Fixed result form for the CBC package. This is a technical configuration,
-- not an administrator-editable feature in the current CareS scope.
INSERT INTO clinical_form_template (template_id, created_at, updated_at, deleted, code, name, context, description, active)
VALUES ('c1000000-0000-0000-0000-000000000001', pg_temp.demo_now(), pg_temp.demo_now(), false,
        'LAB-CBC-V1', 'Công thức máu', 'LAB_RESULT', 'Biểu mẫu nhập 8 chỉ số công thức máu chính.', true)
ON CONFLICT (template_id) DO UPDATE SET
    updated_at = EXCLUDED.updated_at, name = EXCLUDED.name, description = EXCLUDED.description, active = true, deleted = false;

INSERT INTO clinical_form_template_version (version_id, created_at, updated_at, deleted, template_id, version_no, schema_json, status, change_reason, effective_from, published_at)
VALUES ('c2000000-0000-0000-0000-000000000001', pg_temp.demo_now(), pg_temp.demo_now(), false,
        'c1000000-0000-0000-0000-000000000001', 1,
        $cbc${
  "fields": [
    {
      "key": "rbc",
      "label": "Số lượng hồng cầu (RBC)",
      "type": "NUMBER",
      "unit": "10^12/L",
      "requiredOnSign": true,
      "min": 0.5,
      "max": 10,
      "referenceRanges": [
        {
          "minAge": 0,
          "sex": "MALE",
          "low": 4.3,
          "high": 5.8
        },
        {
          "minAge": 0,
          "sex": "FEMALE",
          "low": 3.9,
          "high": 5.2
        }
      ]
    },
    {
      "key": "hgb",
      "label": "Huyết sắc tố (HGB)",
      "type": "NUMBER",
      "unit": "g/L",
      "requiredOnSign": true,
      "min": 50,
      "max": 200,
      "referenceRanges": [
        {
          "minAge": 0,
          "sex": "MALE",
          "low": 130,
          "high": 170
        },
        {
          "minAge": 0,
          "sex": "FEMALE",
          "low": 120,
          "high": 150
        }
      ]
    },
    {
      "key": "hct",
      "label": "Hematocrit (HCT)",
      "type": "NUMBER",
      "unit": "%",
      "requiredOnSign": true,
      "min": 10,
      "max": 70,
      "referenceRanges": [
        {
          "minAge": 0,
          "sex": "MALE",
          "low": 39,
          "high": 49
        },
        {
          "minAge": 0,
          "sex": "FEMALE",
          "low": 33,
          "high": 43
        }
      ]
    },
    {
      "key": "mcv",
      "label": "MCV",
      "type": "NUMBER",
      "unit": "fL",
      "requiredOnSign": true,
      "min": 50,
      "max": 120,
      "referenceRanges": [
        {
          "low": 80,
          "high": 100
        }
      ]
    },
    {
      "key": "wbc",
      "label": "Số lượng bạch cầu (WBC)",
      "type": "NUMBER",
      "unit": "10^9/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 50,
      "referenceRanges": [
        {
          "low": 4,
          "high": 10
        }
      ]
    },
    {
      "key": "neutPercent",
      "label": "Bạch cầu trung tính (%)",
      "type": "NUMBER",
      "unit": "%",
      "requiredOnSign": true,
      "min": 0,
      "max": 100,
      "referenceRanges": [
        {
          "low": 40,
          "high": 75
        }
      ]
    },
    {
      "key": "lymphPercent",
      "label": "Bạch cầu lympho (%)",
      "type": "NUMBER",
      "unit": "%",
      "requiredOnSign": true,
      "min": 0,
      "max": 100,
      "referenceRanges": [
        {
          "low": 20,
          "high": 45
        }
      ]
    },
    {
      "key": "plt",
      "label": "Số lượng tiểu cầu (PLT)",
      "type": "NUMBER",
      "unit": "10^9/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 1000,
      "referenceRanges": [
        {
          "low": 150,
          "high": 400
        }
      ]
    }
  ]
}$cbc$::jsonb,
        'PUBLISHED', 'Khởi tạo biểu mẫu cố định cho gói Công thức máu.', pg_temp.demo_date(), pg_temp.demo_now())
ON CONFLICT (version_id) DO UPDATE SET
    updated_at = EXCLUDED.updated_at, schema_json = EXCLUDED.schema_json, status = 'PUBLISHED',
    change_reason = EXCLUDED.change_reason, effective_from = EXCLUDED.effective_from, deleted = false;

INSERT INTO medical_service_form_template (binding_id, created_at, updated_at, deleted, service_id, template_id)
VALUES ('c3000000-0000-0000-0000-000000000001', pg_temp.demo_now(), pg_temp.demo_now(), false,
        '40000008-0000-0000-0000-000000000008', 'c1000000-0000-0000-0000-000000000001')
ON CONFLICT (service_id) DO UPDATE SET
    updated_at = EXCLUDED.updated_at, template_id = EXCLUDED.template_id, deleted = false;

-- Fixed result forms for blood glucose, CRP and all imaging services.
INSERT INTO clinical_form_template (template_id, created_at, updated_at, deleted, code, name, context, description, active)
VALUES
('c1000000-0000-0000-0000-000000000002', pg_temp.demo_now(), pg_temp.demo_now(), false, 'LAB-GLUCOSE-V1', 'Đường huyết', 'LAB_RESULT', 'Biểu mẫu nhập kết quả đường huyết.', true),
('c1000000-0000-0000-0000-000000000007', pg_temp.demo_now(), pg_temp.demo_now(), false, 'LAB-CRP-V1', 'Xét nghiệm CRP', 'LAB_RESULT', 'Biểu mẫu nhập kết quả CRP định lượng.', true),
('c1000000-0000-0000-0000-000000000011', pg_temp.demo_now(), pg_temp.demo_now(), false, 'IMG-XRAY-CHEST-V1', 'X-quang ngực', 'IMAGING_RESULT', 'Biểu mẫu kết quả X-quang ngực.', true),
('c1000000-0000-0000-0000-000000000012', pg_temp.demo_now(), pg_temp.demo_now(), false, 'IMG-XRAY-MSK-V1', 'X-quang xương khớp', 'IMAGING_RESULT', 'Biểu mẫu kết quả X-quang xương khớp.', true),
('c1000000-0000-0000-0000-000000000013', pg_temp.demo_now(), pg_temp.demo_now(), false, 'IMG-US-ABDOMEN-V1', 'Siêu âm ổ bụng tổng quát', 'IMAGING_RESULT', 'Biểu mẫu kết quả siêu âm ổ bụng.', true),
('c1000000-0000-0000-0000-000000000014', pg_temp.demo_now(), pg_temp.demo_now(), false, 'IMG-US-THYROID-V1', 'Siêu âm tuyến giáp', 'IMAGING_RESULT', 'Biểu mẫu kết quả siêu âm tuyến giáp.', true),
('c1000000-0000-0000-0000-000000000015', pg_temp.demo_now(), pg_temp.demo_now(), false, 'IMG-US-PREGNANCY-V1', 'Siêu âm thai', 'IMAGING_RESULT', 'Biểu mẫu kết quả siêu âm thai.', true)
ON CONFLICT (template_id) DO UPDATE SET
    updated_at = EXCLUDED.updated_at, name = EXCLUDED.name, context = EXCLUDED.context,
    description = EXCLUDED.description, active = true, deleted = false;

INSERT INTO clinical_form_template_version
    (version_id, created_at, updated_at, deleted, template_id, version_no, schema_json, status, change_reason, effective_from, published_at)
VALUES
('c2000000-0000-0000-0000-000000000002', pg_temp.demo_now(), pg_temp.demo_now(), false, 'c1000000-0000-0000-0000-000000000002', 1,
 $glucose${"fields":[{"key":"glucose","label":"Glucose máu","type":"NUMBER","unit":"mmol/L","requiredOnSign":true}]}$glucose$::jsonb,
 'PUBLISHED', 'Khởi tạo biểu mẫu cố định cho xét nghiệm đường huyết.', pg_temp.demo_date(), pg_temp.demo_now()),
('c2000000-0000-0000-0000-000000000007', pg_temp.demo_now(), pg_temp.demo_now(), false, 'c1000000-0000-0000-0000-000000000007', 1,
 $crp${"fields":[{"key":"crp","label":"CRP định lượng","type":"NUMBER","unit":"mg/L","requiredOnSign":true}]}$crp$::jsonb,
 'PUBLISHED', 'Khởi tạo biểu mẫu cố định cho xét nghiệm CRP.', pg_temp.demo_date(), pg_temp.demo_now()),
('c2000000-0000-0000-0000-000000000011', pg_temp.demo_now(), pg_temp.demo_now(), false, 'c1000000-0000-0000-0000-000000000011', 1,
 $xray1${"fields":[{"key":"technique","label":"Kỹ thuật chụp","type":"TEXT","requiredOnSign":true},{"key":"findings","label":"Mô tả hình ảnh","type":"TEXTAREA","requiredOnSign":true}]}$xray1$::jsonb,
 'PUBLISHED', 'Khởi tạo biểu mẫu cố định cho X-quang ngực.', pg_temp.demo_date(), pg_temp.demo_now()),
('c2000000-0000-0000-0000-000000000012', pg_temp.demo_now(), pg_temp.demo_now(), false, 'c1000000-0000-0000-0000-000000000012', 1,
 $xray2${"fields":[{"key":"technique","label":"Kỹ thuật chụp","type":"TEXT","requiredOnSign":true},{"key":"findings","label":"Mô tả hình ảnh","type":"TEXTAREA","requiredOnSign":true}]}$xray2$::jsonb,
 'PUBLISHED', 'Khởi tạo biểu mẫu cố định cho X-quang xương khớp.', pg_temp.demo_date(), pg_temp.demo_now()),
('c2000000-0000-0000-0000-000000000013', pg_temp.demo_now(), pg_temp.demo_now(), false, 'c1000000-0000-0000-0000-000000000013', 1,
 $us1${"fields":[{"key":"description","label":"Mô tả khảo sát","type":"TEXTAREA","requiredOnSign":true},{"key":"findings","label":"Phát hiện","type":"TEXTAREA","requiredOnSign":true}]}$us1$::jsonb,
 'PUBLISHED', 'Khởi tạo biểu mẫu cố định cho siêu âm ổ bụng.', pg_temp.demo_date(), pg_temp.demo_now()),
('c2000000-0000-0000-0000-000000000014', pg_temp.demo_now(), pg_temp.demo_now(), false, 'c1000000-0000-0000-0000-000000000014', 1,
 $us2${"fields":[{"key":"description","label":"Mô tả khảo sát","type":"TEXTAREA","requiredOnSign":true},{"key":"findings","label":"Phát hiện","type":"TEXTAREA","requiredOnSign":true}]}$us2$::jsonb,
 'PUBLISHED', 'Khởi tạo biểu mẫu cố định cho siêu âm tuyến giáp.', pg_temp.demo_date(), pg_temp.demo_now()),
('c2000000-0000-0000-0000-000000000015', pg_temp.demo_now(), pg_temp.demo_now(), false, 'c1000000-0000-0000-0000-000000000015', 1,
 $us3${"fields":[{"key":"gestationalAgeWeeks","label":"Tuổi thai","type":"NUMBER","unit":"tuần","requiredOnSign":true},{"key":"fetalHeartRate","label":"Tim thai","type":"NUMBER","unit":"lần/phút","requiredOnSign":true},{"key":"description","label":"Mô tả khảo sát","type":"TEXTAREA","requiredOnSign":true},{"key":"findings","label":"Phát hiện","type":"TEXTAREA","requiredOnSign":true}]}$us3$::jsonb,
 'PUBLISHED', 'Khởi tạo biểu mẫu cố định cho siêu âm thai.', pg_temp.demo_date(), pg_temp.demo_now())
ON CONFLICT (version_id) DO UPDATE SET
    updated_at = EXCLUDED.updated_at, schema_json = EXCLUDED.schema_json, status = 'PUBLISHED',
    change_reason = EXCLUDED.change_reason, effective_from = EXCLUDED.effective_from,
    published_at = EXCLUDED.published_at, deleted = false;

INSERT INTO medical_service_form_template (binding_id, created_at, updated_at, deleted, service_id, template_id)
VALUES
('c3000000-0000-0000-0000-000000000002', pg_temp.demo_now(), pg_temp.demo_now(), false, '40000009-0000-0000-0000-000000000009', 'c1000000-0000-0000-0000-000000000002'),
('c3000000-0000-0000-0000-000000000007', pg_temp.demo_now(), pg_temp.demo_now(), false, '41000021-0000-0000-0000-000000000021', 'c1000000-0000-0000-0000-000000000007'),
('c3000000-0000-0000-0000-000000000011', pg_temp.demo_now(), pg_temp.demo_now(), false, '40000017-0000-0000-0000-000000000017', 'c1000000-0000-0000-0000-000000000011'),
('c3000000-0000-0000-0000-000000000012', pg_temp.demo_now(), pg_temp.demo_now(), false, '41000018-0000-0000-0000-000000000018', 'c1000000-0000-0000-0000-000000000012'),
('c3000000-0000-0000-0000-000000000013', pg_temp.demo_now(), pg_temp.demo_now(), false, '40000015-0000-0000-0000-000000000015', 'c1000000-0000-0000-0000-000000000013'),
('c3000000-0000-0000-0000-000000000014', pg_temp.demo_now(), pg_temp.demo_now(), false, '41000019-0000-0000-0000-000000000019', 'c1000000-0000-0000-0000-000000000014'),
('c3000000-0000-0000-0000-000000000015', pg_temp.demo_now(), pg_temp.demo_now(), false, '41000020-0000-0000-0000-000000000020', 'c1000000-0000-0000-0000-000000000015')
ON CONFLICT (service_id) DO UPDATE SET
    updated_at = EXCLUDED.updated_at, template_id = EXCLUDED.template_id, deleted = false;

-- Fixed result forms for every remaining laboratory panel displayed by the
-- panel workbench. The field keys intentionally match LaboratoryAnalyteCatalog.
INSERT INTO clinical_form_template (template_id, created_at, updated_at, deleted, code, name, context, description, active)
VALUES
('c1000000-0000-0000-0000-000000000003', pg_temp.demo_now(), pg_temp.demo_now(), false, 'LAB-BIO-V1', 'Sinh hóa máu cơ bản', 'LAB_RESULT', 'Biểu mẫu nhập các chỉ số sinh hóa máu cơ bản.', true),
('c1000000-0000-0000-0000-000000000004', pg_temp.demo_now(), pg_temp.demo_now(), false, 'LAB-LIVER-V1', 'Chức năng gan', 'LAB_RESULT', 'Biểu mẫu nhập các chỉ số chức năng gan.', true),
('c1000000-0000-0000-0000-000000000005', pg_temp.demo_now(), pg_temp.demo_now(), false, 'LAB-RENAL-V1', 'Chức năng thận', 'LAB_RESULT', 'Biểu mẫu nhập các chỉ số chức năng thận.', true),
('c1000000-0000-0000-0000-000000000006', pg_temp.demo_now(), pg_temp.demo_now(), false, 'LAB-URINE-V1', 'Tổng phân tích nước tiểu', 'LAB_RESULT', 'Biểu mẫu nhập các chỉ số tổng phân tích nước tiểu.', true)
ON CONFLICT (template_id) DO UPDATE SET
    updated_at = EXCLUDED.updated_at, name = EXCLUDED.name, description = EXCLUDED.description,
    context = EXCLUDED.context, active = true, deleted = false;

INSERT INTO clinical_form_template_version
    (version_id, created_at, updated_at, deleted, template_id, version_no, schema_json, status, change_reason, effective_from, published_at)
VALUES
('c2000000-0000-0000-0000-000000000003', pg_temp.demo_now(), pg_temp.demo_now(), false,
 'c1000000-0000-0000-0000-000000000003', 1,
 $bio${
  "fields": [
    {
      "key": "hba1c",
      "label": "Hemoglobin A1c",
      "type": "NUMBER",
      "unit": "%",
      "requiredOnSign": true,
      "min": 3,
      "max": 20,
      "referenceRanges": [
        {
          "low": 4,
          "high": 5.6
        }
      ]
    },
    {
      "key": "totalCholesterol",
      "label": "Cholesterol toàn phần",
      "type": "NUMBER",
      "unit": "mmol/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 20,
      "referenceRanges": [
        {
          "high": 5.1
        }
      ]
    },
    {
      "key": "triglyceride",
      "label": "Triglyceride",
      "type": "NUMBER",
      "unit": "mmol/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 20,
      "referenceRanges": [
        {
          "high": 1.7
        }
      ]
    },
    {
      "key": "hdlC",
      "label": "HDL Cholesterol",
      "type": "NUMBER",
      "unit": "mmol/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 5,
      "referenceRanges": [
        {
          "minAge": 0,
          "sex": "MALE",
          "low": 1.03
        },
        {
          "minAge": 0,
          "sex": "FEMALE",
          "low": 1.29
        }
      ]
    },
    {
      "key": "ldlC",
      "label": "LDL Cholesterol",
      "type": "NUMBER",
      "unit": "mmol/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 10,
      "referenceRanges": [
        {
          "high": 3.3
        }
      ]
    },
    {
      "key": "acidUric",
      "label": "Acid uric",
      "type": "NUMBER",
      "unit": "µmol/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 1000,
      "referenceRanges": [
        {
          "minAge": 0,
          "sex": "MALE",
          "low": 202,
          "high": 416
        },
        {
          "minAge": 0,
          "sex": "FEMALE",
          "low": 142,
          "high": 339
        }
      ]
    },
    {
      "key": "totalProtein",
      "label": "Protein toàn phần",
      "type": "NUMBER",
      "unit": "g/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 150,
      "referenceRanges": [
        {
          "low": 66,
          "high": 87
        }
      ]
    }
  ]
}$bio$::jsonb, 'PUBLISHED', 'Khôi phục biểu mẫu cố định cho gói sinh hóa máu.', pg_temp.demo_date(), pg_temp.demo_now()),
('c2000000-0000-0000-0000-000000000004', pg_temp.demo_now(), pg_temp.demo_now(), false,
 'c1000000-0000-0000-0000-000000000004', 1,
 $liver${
  "fields": [
    {
      "key": "ast",
      "label": "AST",
      "type": "NUMBER",
      "unit": "U/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 1000,
      "referenceRanges": [
        {
          "minAge": 0,
          "sex": "MALE",
          "high": 37
        },
        {
          "minAge": 0,
          "sex": "FEMALE",
          "high": 31
        }
      ]
    },
    {
      "key": "alt",
      "label": "ALT",
      "type": "NUMBER",
      "unit": "U/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 1000,
      "referenceRanges": [
        {
          "minAge": 0,
          "sex": "MALE",
          "high": 41
        },
        {
          "minAge": 0,
          "sex": "FEMALE",
          "high": 31
        }
      ]
    },
    {
      "key": "alp",
      "label": "Alkaline phosphatase",
      "type": "NUMBER",
      "unit": "U/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 1000,
      "referenceRanges": [
        {
          "low": 40,
          "high": 129
        }
      ]
    },
    {
      "key": "ggt",
      "label": "GGT",
      "type": "NUMBER",
      "unit": "U/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 1000,
      "referenceRanges": [
        {
          "minAge": 0,
          "sex": "MALE",
          "high": 61
        },
        {
          "minAge": 0,
          "sex": "FEMALE",
          "high": 36
        }
      ]
    },
    {
      "key": "bilirubinTotal",
      "label": "Bilirubin toàn phần",
      "type": "NUMBER",
      "unit": "µmol/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 200,
      "referenceRanges": [
        {
          "high": 21
        }
      ]
    },
    {
      "key": "bilirubinDirect",
      "label": "Bilirubin trực tiếp",
      "type": "NUMBER",
      "unit": "µmol/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 100,
      "referenceRanges": [
        {
          "high": 5
        }
      ]
    },
    {
      "key": "albumin",
      "label": "Albumin",
      "type": "NUMBER",
      "unit": "g/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 100,
      "referenceRanges": [
        {
          "low": 35,
          "high": 52
        }
      ]
    }
  ]
}$liver$::jsonb, 'PUBLISHED', 'Khôi phục biểu mẫu cố định cho gói chức năng gan.', pg_temp.demo_date(), pg_temp.demo_now()),
('c2000000-0000-0000-0000-000000000005', pg_temp.demo_now(), pg_temp.demo_now(), false,
 'c1000000-0000-0000-0000-000000000005', 1,
 $renal${
  "fields": [
    {
      "key": "urea",
      "label": "Urea",
      "type": "NUMBER",
      "unit": "mmol/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 50,
      "referenceRanges": [
        {
          "low": 2.5,
          "high": 7.1
        }
      ]
    },
    {
      "key": "creatinine",
      "label": "Creatinine",
      "type": "NUMBER",
      "unit": "µmol/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 1000,
      "referenceRanges": [
        {
          "minAge": 0,
          "sex": "MALE",
          "low": 62,
          "high": 106
        },
        {
          "minAge": 0,
          "sex": "FEMALE",
          "low": 44,
          "high": 80
        }
      ]
    },
    {
      "key": "sodium",
      "label": "Sodium",
      "type": "NUMBER",
      "unit": "mmol/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 200,
      "referenceRanges": [
        {
          "low": 135,
          "high": 145
        }
      ]
    },
    {
      "key": "potassium",
      "label": "Potassium",
      "type": "NUMBER",
      "unit": "mmol/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 10,
      "referenceRanges": [
        {
          "low": 3.5,
          "high": 5.1
        }
      ]
    },
    {
      "key": "chloride",
      "label": "Chloride",
      "type": "NUMBER",
      "unit": "mmol/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 200,
      "referenceRanges": [
        {
          "low": 98,
          "high": 107
        }
      ]
    },
    {
      "key": "calcium",
      "label": "Calcium toàn phần",
      "type": "NUMBER",
      "unit": "mmol/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 5,
      "referenceRanges": [
        {
          "low": 2.15,
          "high": 2.55
        }
      ]
    }
  ]
}$renal$::jsonb, 'PUBLISHED', 'Khôi phục biểu mẫu cố định cho gói chức năng thận.', pg_temp.demo_date(), pg_temp.demo_now()),
('c2000000-0000-0000-0000-000000000006', pg_temp.demo_now(), pg_temp.demo_now(), false,
 'c1000000-0000-0000-0000-000000000006', 1,
 $urine${
  "fields": [
    {
      "key": "specificGravity",
      "label": "Tỷ trọng nước tiểu",
      "type": "NUMBER",
      "requiredOnSign": true,
      "min": 1,
      "max": 1.05,
      "referenceRanges": [
        {
          "low": 1.01,
          "high": 1.025
        }
      ]
    },
    {
      "key": "ph",
      "label": "pH nước tiểu",
      "type": "NUMBER",
      "requiredOnSign": true,
      "min": 4,
      "max": 9,
      "referenceRanges": [
        {
          "low": 4.8,
          "high": 7.4
        }
      ]
    },
    {
      "key": "leukocyteEsterase",
      "label": "Leukocyte Esterase",
      "type": "TEXT",
      "requiredOnSign": true,
      "referenceRanges": [
        {
          "text": "Negative"
        }
      ]
    },
    {
      "key": "nitrite",
      "label": "Nitrite",
      "type": "TEXT",
      "requiredOnSign": true,
      "referenceRanges": [
        {
          "text": "Negative"
        }
      ]
    },
    {
      "key": "protein",
      "label": "Protein nước tiểu",
      "type": "TEXT",
      "requiredOnSign": true,
      "referenceRanges": [
        {
          "text": "Negative"
        }
      ]
    },
    {
      "key": "urineGlucose",
      "label": "Glucose nước tiểu",
      "type": "TEXT",
      "requiredOnSign": true,
      "referenceRanges": [
        {
          "text": "Negative"
        }
      ]
    },
    {
      "key": "ketone",
      "label": "Ketone",
      "type": "TEXT",
      "requiredOnSign": true,
      "referenceRanges": [
        {
          "text": "Negative"
        }
      ]
    },
    {
      "key": "blood",
      "label": "Máu/Hemoglobin nước tiểu",
      "type": "TEXT",
      "requiredOnSign": true,
      "referenceRanges": [
        {
          "text": "Negative"
        }
      ]
    }
  ]
}$urine$::jsonb, 'PUBLISHED', 'Khôi phục biểu mẫu cố định cho gói tổng phân tích nước tiểu.', pg_temp.demo_date(), pg_temp.demo_now())
ON CONFLICT (version_id) DO UPDATE SET
    updated_at = EXCLUDED.updated_at, schema_json = EXCLUDED.schema_json, status = 'PUBLISHED',
    change_reason = EXCLUDED.change_reason, effective_from = EXCLUDED.effective_from,
    published_at = EXCLUDED.published_at, deleted = false;

INSERT INTO medical_service_form_template (binding_id, created_at, updated_at, deleted, service_id, template_id)
VALUES
('c3000000-0000-0000-0000-000000000003', pg_temp.demo_now(), pg_temp.demo_now(), false, '4000000a-0095-0000-0000-000000000095', 'c1000000-0000-0000-0000-000000000003'),
('c3000000-0000-0000-0000-000000000004', pg_temp.demo_now(), pg_temp.demo_now(), false, '4000000a-00a4-0000-0000-0000000000a4', 'c1000000-0000-0000-0000-000000000004'),
('c3000000-0000-0000-0000-000000000005', pg_temp.demo_now(), pg_temp.demo_now(), false, '4000000a-00b3-0000-0000-0000000000b3', 'c1000000-0000-0000-0000-000000000005'),
('c3000000-0000-0000-0000-000000000006', pg_temp.demo_now(), pg_temp.demo_now(), false, '4000000a-00c4-0000-0000-0000000000c4', 'c1000000-0000-0000-0000-000000000006')
ON CONFLICT (service_id) DO UPDATE SET
    updated_at = EXCLUDED.updated_at, template_id = EXCLUDED.template_id, deleted = false;

-- Medical Service Relations
/* The legacy medical_service_relation table was removed from the active schema.
   Service packages and laboratory panels are represented directly by medical_service. */
/*
INSERT INTO medical_service_relation (relation_id, source_service_id, target_service_id, relation_type, note) VALUES
('4000000a-0066-0000-0000-000000000066', '40000008-0000-0000-0000-000000000008', '4000000a-0065-0000-0000-000000000065', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-0068-0000-0000-000000000068', '40000008-0000-0000-0000-000000000008', '4000000a-0067-0000-0000-000000000067', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-006a-0000-0000-00000000006a', '40000008-0000-0000-0000-000000000008', '4000000a-0069-0000-0000-000000000069', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-006c-0000-0000-00000000006c', '40000008-0000-0000-0000-000000000008', '4000000a-006b-0000-0000-00000000006b', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-006e-0000-0000-00000000006e', '40000008-0000-0000-0000-000000000008', '4000000a-006d-0000-0000-00000000006d', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-0070-0000-0000-000000000070', '40000008-0000-0000-0000-000000000008', '4000000a-006f-0000-0000-00000000006f', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-0072-0000-0000-000000000072', '40000008-0000-0000-0000-000000000008', '4000000a-0071-0000-0000-000000000071', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-0074-0000-0000-000000000074', '40000008-0000-0000-0000-000000000008', '4000000a-0073-0000-0000-000000000073', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-0076-0000-0000-000000000076', '40000008-0000-0000-0000-000000000008', '4000000a-0075-0000-0000-000000000075', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-0078-0000-0000-000000000078', '40000008-0000-0000-0000-000000000008', '4000000a-0077-0000-0000-000000000077', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-007a-0000-0000-00000000007a', '40000008-0000-0000-0000-000000000008', '4000000a-0079-0000-0000-000000000079', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-007c-0000-0000-00000000007c', '40000008-0000-0000-0000-000000000008', '4000000a-007b-0000-0000-00000000007b', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-007e-0000-0000-00000000007e', '40000008-0000-0000-0000-000000000008', '4000000a-007d-0000-0000-00000000007d', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-0080-0000-0000-000000000080', '40000008-0000-0000-0000-000000000008', '4000000a-007f-0000-0000-00000000007f', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-0082-0000-0000-000000000082', '40000008-0000-0000-0000-000000000008', '4000000a-0081-0000-0000-000000000081', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-0084-0000-0000-000000000084', '40000008-0000-0000-0000-000000000008', '4000000a-0083-0000-0000-000000000083', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-0086-0000-0000-000000000086', '40000008-0000-0000-0000-000000000008', '4000000a-0085-0000-0000-000000000085', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-0088-0000-0000-000000000088', '40000008-0000-0000-0000-000000000008', '4000000a-0087-0000-0000-000000000087', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-008a-0000-0000-00000000008a', '40000008-0000-0000-0000-000000000008', '4000000a-0089-0000-0000-000000000089', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-008c-0000-0000-00000000008c', '40000008-0000-0000-0000-000000000008', '4000000a-008b-0000-0000-00000000008b', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-008e-0000-0000-00000000008e', '40000008-0000-0000-0000-000000000008', '4000000a-008d-0000-0000-00000000008d', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-0090-0000-0000-000000000090', '40000008-0000-0000-0000-000000000008', '4000000a-008f-0000-0000-00000000008f', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-0092-0000-0000-000000000092', '40000008-0000-0000-0000-000000000008', '4000000a-0091-0000-0000-000000000091', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-0094-0000-0000-000000000094', '40000008-0000-0000-0000-000000000008', '4000000a-0093-0000-0000-000000000093', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-0097-0000-0000-000000000097', '4000000a-0095-0000-0000-000000000095', '4000000a-0096-0000-0000-000000000096', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-0099-0000-0000-000000000099', '4000000a-0095-0000-0000-000000000095', '4000000a-0098-0000-0000-000000000098', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-009b-0000-0000-00000000009b', '4000000a-0095-0000-0000-000000000095', '4000000a-009a-0000-0000-00000000009a', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-009d-0000-0000-00000000009d', '4000000a-0095-0000-0000-000000000095', '4000000a-009c-0000-0000-00000000009c', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-009f-0000-0000-00000000009f', '4000000a-0095-0000-0000-000000000095', '4000000a-009e-0000-0000-00000000009e', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-00a1-0000-0000-0000000000a1', '4000000a-0095-0000-0000-000000000095', '4000000a-00a0-0000-0000-0000000000a0', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-00a3-0000-0000-0000000000a3', '4000000a-0095-0000-0000-000000000095', '4000000a-00a2-0000-0000-0000000000a2', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-00a6-0000-0000-0000000000a6', '4000000a-00a4-0000-0000-0000000000a4', '4000000a-00a5-0000-0000-0000000000a5', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-00a8-0000-0000-0000000000a8', '4000000a-00a4-0000-0000-0000000000a4', '4000000a-00a7-0000-0000-0000000000a7', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-00aa-0000-0000-0000000000aa', '4000000a-00a4-0000-0000-0000000000a4', '4000000a-00a9-0000-0000-0000000000a9', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-00ac-0000-0000-0000000000ac', '4000000a-00a4-0000-0000-0000000000a4', '4000000a-00ab-0000-0000-0000000000ab', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-00ae-0000-0000-0000000000ae', '4000000a-00a4-0000-0000-0000000000a4', '4000000a-00ad-0000-0000-0000000000ad', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-00b0-0000-0000-0000000000b0', '4000000a-00a4-0000-0000-0000000000a4', '4000000a-00af-0000-0000-0000000000af', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-00b2-0000-0000-0000000000b2', '4000000a-00a4-0000-0000-0000000000a4', '4000000a-00b1-0000-0000-0000000000b1', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-00b5-0000-0000-0000000000b5', '4000000a-00b3-0000-0000-0000000000b3', '4000000a-00b4-0000-0000-0000000000b4', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-00b7-0000-0000-0000000000b7', '4000000a-00b3-0000-0000-0000000000b3', '4000000a-00b6-0000-0000-0000000000b6', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-00b9-0000-0000-0000000000b9', '4000000a-00b3-0000-0000-0000000000b3', '4000000a-00b8-0000-0000-0000000000b8', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-00bb-0000-0000-0000000000bb', '4000000a-00b3-0000-0000-0000000000b3', '4000000a-00ba-0000-0000-0000000000ba', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-00bd-0000-0000-0000000000bd', '4000000a-00b3-0000-0000-0000000000b3', '4000000a-00bc-0000-0000-0000000000bc', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-00bf-0000-0000-0000000000bf', '4000000a-00b3-0000-0000-0000000000b3', '4000000a-00be-0000-0000-0000000000be', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-00c1-0000-0000-0000000000c1', '4000000a-00b3-0000-0000-0000000000b3', '4000000a-00c0-0000-0000-0000000000c0', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-00c3-0000-0000-0000000000c3', '4000000a-00b3-0000-0000-0000000000b3', '4000000a-00c2-0000-0000-0000000000c2', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-00c6-0000-0000-0000000000c6', '4000000a-00c4-0000-0000-0000000000c4', '4000000a-00c5-0000-0000-0000000000c5', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-00c8-0000-0000-0000000000c8', '4000000a-00c4-0000-0000-0000000000c4', '4000000a-00c7-0000-0000-0000000000c7', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-00ca-0000-0000-0000000000ca', '4000000a-00c4-0000-0000-0000000000c4', '4000000a-00c9-0000-0000-0000000000c9', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-00cc-0000-0000-0000000000cc', '4000000a-00c4-0000-0000-0000000000c4', '4000000a-00cb-0000-0000-0000000000cb', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-00ce-0000-0000-0000000000ce', '4000000a-00c4-0000-0000-0000000000c4', '4000000a-00cd-0000-0000-0000000000cd', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-00d0-0000-0000-0000000000d0', '4000000a-00c4-0000-0000-0000000000c4', '4000000a-00cf-0000-0000-0000000000cf', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-00d2-0000-0000-0000000000d2', '4000000a-00c4-0000-0000-0000000000c4', '4000000a-00d1-0000-0000-0000000000d1', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-00d4-0000-0000-0000000000d4', '4000000a-00c4-0000-0000-0000000000c4', '4000000a-00d3-0000-0000-0000000000d3', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-00d6-0000-0000-0000000000d6', '4000000a-00c4-0000-0000-0000000000c4', '4000000a-00d5-0000-0000-0000000000d5', 'PANEL_TO_ANALYTE', 'Bao gồm'),
('4000000a-00d8-0000-0000-0000000000d8', '4000000a-00c4-0000-0000-0000000000c4', '4000000a-00d7-0000-0000-0000000000d7', 'PANEL_TO_ANALYTE', 'Bao gồm');

*/
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

-- Staff Schedules (full week, all three shifts). Receptionists and cashiers
-- follow the same demo availability as doctors and nurses.

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
WHERE s.system_role IN ('DOCTOR', 'NURSE', 'RECEPTIONIST', 'CASHIER');

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
/* Legacy queue-ticket layout retained below only as seed history; the active schema uses queue_number. */
/*
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
*/
INSERT INTO queue_ticket (ticket_id, created_at, deleted, updated_at, called_at, completed_at, queue_number, status, work_date, department_id, service_id, visit_id)
SELECT pg_temp.did('historical-ticket-' || v.visit_id::text), v.created_at, false, v.updated_at,
       v.check_in_time, v.check_out_time, 1, 'DONE', v.check_in_time::date,
       '4d7ac047-a699-7a37-b2c0-e8392593e7bc',
       (SELECT service_id FROM medical_service WHERE service_code = 'EX-IN-001'), v.visit_id
FROM customer_visit v;

INSERT INTO medical_record (record_id, visit_id, status, doctor_id, created_at, updated_at, deleted) VALUES
('63425b1c-09cc-dfbc-5b82-6417fce75bb4', 'd5ee978d-cb75-979f-16ff-d348536a3a85', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '29 days', pg_temp.demo_now()-interval '29 days', false),
('d90b3fc2-2b39-d940-d65f-7e6ed2262008', '07ac9a53-0f97-2fb6-1f9d-34924096efc6', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '28 days', pg_temp.demo_now()-interval '28 days', false),
('a777e0f4-3af8-f685-7e4b-8c4eb3f70f3f', '41b2420a-d4b7-8804-1d26-035597505b0d', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '27 days', pg_temp.demo_now()-interval '27 days', false),
('81669eea-8014-0c23-cd6a-f89cd51fe8ef', '563593a1-6854-4e0f-2a22-a2558be66071', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '26 days', pg_temp.demo_now()-interval '26 days', false),
('e5803292-b455-c25d-b2b0-f4dfa5db76d2', '3f479e2a-b84e-4e6b-5f34-701b740d2c42', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '25 days', pg_temp.demo_now()-interval '25 days', false),
('770e9e34-c176-32df-7e15-fb33e8ed16e3', '58f43c00-7613-60c0-80b7-aa987cd1cdc7', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '24 days', pg_temp.demo_now()-interval '24 days', false),
('8cbb6eba-f9b6-d74c-e70e-7a664e112b9e', '01ab728f-81ca-9b16-01b0-829ee8afccd7', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '23 days', pg_temp.demo_now()-interval '23 days', false),
('f78f3b87-7909-4333-546f-e936c5b9205c', '4e75879e-ee16-2033-328e-14074267adf5', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '22 days', pg_temp.demo_now()-interval '22 days', false),
('7238f2fd-48e8-6246-7eee-34a4f84b5644', 'ce35c38b-b5f4-c14b-3cc6-9cdd5dcf62ea', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '21 days', pg_temp.demo_now()-interval '21 days', false),
('1a44135b-8b3b-682b-1be3-bd6d07930971', '472df76d-d7a1-4f37-23be-4625460788ef', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '20 days', pg_temp.demo_now()-interval '20 days', false),
('d52a681e-3db2-9eac-3ea1-8a5c612e51a1', '95f27f2f-a87f-9a2f-94d0-794b78bb874d', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '19 days', pg_temp.demo_now()-interval '19 days', false),
('5670089c-8fb8-4c82-83ad-2bce35b41526', '3b64222f-e79f-d4e5-b59c-238bd8e305f0', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '18 days', pg_temp.demo_now()-interval '18 days', false),
('07a54026-ffd1-bb90-b20c-b74fe9efaec7', 'acdfb5b3-9e44-edeb-0b33-a87b3061c910', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '17 days', pg_temp.demo_now()-interval '17 days', false),
('bfa52c92-98c2-fc4b-8f1c-c59cbd710c8e', '8fd00ef0-0301-e828-12aa-feb061baae9b', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '16 days', pg_temp.demo_now()-interval '16 days', false),
('27d23d7f-430e-7c84-650c-88a6b407ce0b', 'a6f52718-74ad-b4fb-2395-bd5a7273947d', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '15 days', pg_temp.demo_now()-interval '15 days', false),
('ef780c06-411d-e0b7-6300-60ae43106b38', '1a1dd74b-671a-007a-f820-17b1ed805085', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '14 days', pg_temp.demo_now()-interval '14 days', false),
('e327e6f5-de54-c46a-b410-edf78ea4a411', '4870f11b-a685-6595-aa05-df1eaf57b453', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '13 days', pg_temp.demo_now()-interval '13 days', false),
('50385829-f297-770e-9d80-e0ec4f591745', 'f2ae7fa2-f61e-628c-8fd6-a1e37ee28925', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '12 days', pg_temp.demo_now()-interval '12 days', false),
('b2670e4b-37a6-80f2-2719-207627c4eb93', 'c284d077-0220-f4e5-379b-f748001a517a', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '11 days', pg_temp.demo_now()-interval '11 days', false),
('86afe0dd-4428-4c21-bfa3-ba938295efeb', '9419485f-8220-72e4-d117-4f924aaf3d64', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '10 days', pg_temp.demo_now()-interval '10 days', false),
('9b1ae98e-6580-2259-ca6b-6d570e1d67f5', '6896b913-3282-13e0-507d-ba867aa88a9e', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '9 days', pg_temp.demo_now()-interval '9 days', false),
('fc2ab145-1bab-3661-d45d-9fc7f7395f42', '155226fc-7ea7-af74-8f79-6f301dacc813', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '8 days', pg_temp.demo_now()-interval '8 days', false),
('cdb42828-6ca7-0a2a-498d-cd9cc7f9d7b7', '7106a7e0-7222-4b2e-4070-28ee64525341', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '7 days', pg_temp.demo_now()-interval '7 days', false),
('adfa1da4-b8e1-0108-9022-e6a623fec95f', '2d27d8c8-6536-e798-cc59-c5ca623f32d7', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '6 days', pg_temp.demo_now()-interval '6 days', false),
('31b41c23-9ebb-da72-d02d-4fc548bd6e5c', '378f1bfa-d1ba-5fa5-9867-20d85332e09c', 'COMPLETED', '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '5 days', pg_temp.demo_now()-interval '5 days', false);
/* Legacy invoice column layout retained below only as seed history. */
/*
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
*/
INSERT INTO invoice (invoice_id, created_at, deleted, updated_at, discount, due_date, invoice_code, issue_date, note, paid_amount, status, subtotal, tax, total_amount, customer_id, issued_by, medical_record_id, visit_id)
SELECT pg_temp.did('historical-invoice-' || v.visit_id::text), v.created_at, false, v.updated_at,
       0, NULL, 'INV-DEMO-' || lpad(row_number() OVER (ORDER BY v.check_in_time)::text, 4, '0'),
       v.check_in_time::date, NULL, 220000, 'PAID', 220000, 0, 220000,
       v.customer_id, 'b693d136-402d-7de2-4835-117a5e2c5411',
       (SELECT r.record_id FROM medical_record r WHERE r.visit_id = v.visit_id), v.visit_id
FROM customer_visit v;

-- Snapshot one examination line for every historical paid invoice.
INSERT INTO invoice_item
    (item_id, created_at, updated_at, deleted, invoice_id, service_id,
     service_snapshot, service_code_snapshot, unit_price, quantity,
     discount_percent, discount_amount, final_price, line_total, note, bhyt_fund)
SELECT pg_temp.did('historical-exam-item-' || i.invoice_id::text), i.created_at, i.updated_at, false,
       i.invoice_id, '40000002-0000-0000-0000-000000000002',
       'Khám Nội tổng quát', 'EX-IN-001', 220000, 1, 0, 0, 220000, 220000, NULL, 0
FROM invoice i
WHERE i.invoice_code LIKE 'INV-DEMO-%';

-- A representative completed CRP and X-ray workflow is attached to the first
-- historical visit so the catalogue, billing, queues, forms and result pages
-- can be demonstrated without creating artificial standalone records.
INSERT INTO invoice_item
    (item_id, created_at, updated_at, deleted, invoice_id, service_id,
     service_snapshot, service_code_snapshot, unit_price, quantity,
     discount_percent, discount_amount, final_price, line_total, note, bhyt_fund)
VALUES
('d1000000-0000-0000-0000-000000000001', pg_temp.demo_now()-interval '29 days', pg_temp.demo_now()-interval '29 days', false,
 pg_temp.did('historical-invoice-d5ee978d-cb75-979f-16ff-d348536a3a85'), '41000021-0000-0000-0000-000000000021',
 'Xét nghiệm CRP', 'LAB-007', 100000, 1, 0, 0, 100000, 100000, 'Dữ liệu trình diễn CRP', 0),
('d1000000-0000-0000-0000-000000000002', pg_temp.demo_now()-interval '29 days', pg_temp.demo_now()-interval '29 days', false,
 pg_temp.did('historical-invoice-d5ee978d-cb75-979f-16ff-d348536a3a85'), '40000017-0000-0000-0000-000000000017',
 'X-quang ngực', 'IMG-001', 180000, 1, 0, 0, 180000, 180000, 'Dữ liệu trình diễn hình ảnh', 0);

UPDATE invoice
SET subtotal = 500000, total_amount = 500000, paid_amount = 500000, updated_at = pg_temp.demo_now()
WHERE invoice_id = pg_temp.did('historical-invoice-d5ee978d-cb75-979f-16ff-d348536a3a85');

INSERT INTO queue_ticket
    (ticket_id, created_at, updated_at, deleted, visit_id, department_id,
     work_date, queue_number, status, called_at, completed_at, service_id)
VALUES
('d2000000-0000-0000-0000-000000000001', pg_temp.demo_now()-interval '29 days', pg_temp.demo_now()-interval '29 days', false,
 'd5ee978d-cb75-979f-16ff-d348536a3a85', '760edff5-8292-2100-0b77-7fc8e316e913', pg_temp.demo_date()-29, 50,
 'DONE', pg_temp.demo_now()-interval '29 days', pg_temp.demo_now()-interval '29 days'+interval '25 minutes', '41000021-0000-0000-0000-000000000021'),
('d2000000-0000-0000-0000-000000000002', pg_temp.demo_now()-interval '29 days', pg_temp.demo_now()-interval '29 days', false,
 'd5ee978d-cb75-979f-16ff-d348536a3a85', '4a0a5c21-f9d4-234b-9631-c80fa2db9881', pg_temp.demo_date()-29, 50,
 'DONE', pg_temp.demo_now()-interval '29 days', pg_temp.demo_now()-interval '29 days'+interval '35 minutes', '40000017-0000-0000-0000-000000000017');

INSERT INTO test_request
    (test_request_id, created_at, updated_at, deleted, medical_record_id, service_id,
     performing_department, queue_ticket_id, description, status, requested_by,
     completed_at, cancel_reason, invoice_item_id)
VALUES
('d3000000-0000-0000-0000-000000000001', pg_temp.demo_now()-interval '29 days', pg_temp.demo_now()-interval '29 days', false,
 '63425b1c-09cc-dfbc-5b82-6417fce75bb4', '41000021-0000-0000-0000-000000000021',
 '760edff5-8292-2100-0b77-7fc8e316e913', 'd2000000-0000-0000-0000-000000000001', 'Kiểm tra tình trạng viêm', 'COMPLETED',
 '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '29 days'+interval '25 minutes', NULL, 'd1000000-0000-0000-0000-000000000001'),
('d3000000-0000-0000-0000-000000000002', pg_temp.demo_now()-interval '29 days', pg_temp.demo_now()-interval '29 days', false,
 '63425b1c-09cc-dfbc-5b82-6417fce75bb4', '40000017-0000-0000-0000-000000000017',
 '4a0a5c21-f9d4-234b-9631-c80fa2db9881', 'd2000000-0000-0000-0000-000000000002', 'Đánh giá hình ảnh ngực', 'COMPLETED',
 '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_now()-interval '29 days'+interval '35 minutes', NULL, 'd1000000-0000-0000-0000-000000000002');

INSERT INTO test_result
    (result_id, created_at, updated_at, deleted, test_request_id, image_url, conclusion,
     result_data, sample_id, sample_type, sample_status, collected_at, collected_by,
     performed_by, performed_at, verified_by, verified_at)
VALUES
('d4000000-0000-0000-0000-000000000001', pg_temp.demo_now()-interval '29 days', pg_temp.demo_now()-interval '29 days', false,
 'd3000000-0000-0000-0000-000000000001', NULL, 'CRP trong giới hạn tham chiếu.',
 '{"crp":3.2}'::jsonb, 'SMP-CRP-DEMO', 'BLOOD', 'ACCEPTED', pg_temp.demo_now()-interval '29 days',
 '0f348c8d-74b9-3ef2-981b-61105eab787e', '74cf86f9-d830-e246-7dc3-538a0874779d',
 pg_temp.demo_now()-interval '29 days'+interval '20 minutes', '74cf86f9-d830-e246-7dc3-538a0874779d', pg_temp.demo_now()-interval '29 days'+interval '25 minutes'),
('d4000000-0000-0000-0000-000000000002', pg_temp.demo_now()-interval '29 days', pg_temp.demo_now()-interval '29 days', false,
 'd3000000-0000-0000-0000-000000000002', '/demo-results/xray-chest-demo.png', 'Không ghi nhận tổn thương cấp tính.',
 '{"technique":"X-quang ngực thẳng","findings":"Hai phế trường sáng, không thấy đám mờ khu trú."}'::jsonb,
 NULL, NULL, NULL, NULL, NULL, 'ceb1dbd4-ee6c-2065-99f8-cd32f616c3f6',
 pg_temp.demo_now()-interval '29 days'+interval '30 minutes', 'ceb1dbd4-ee6c-2065-99f8-cd32f616c3f6', pg_temp.demo_now()-interval '29 days'+interval '35 minutes');

-- Complete historical examination for customer 0987654003. This visit is
-- intentionally rich enough for the customer and doctor history screens:
-- internal examination + complete blood count + abdominal ultrasound.
UPDATE medical_record
SET record_code = 'MR-DEMO-CBC-IMG-HISTORY',
    queue_ticket_id = pg_temp.did('historical-ticket-07ac9a53-0f97-2fb6-1f9d-34924096efc6'),
    chief_complaint = 'Mệt mỏi, ăn uống kém và đau âm ỉ vùng bụng trong ba ngày.',
    clinical_findings = 'Bệnh nhân tỉnh, niêm mạc hồng, bụng mềm, không phản ứng thành bụng.',
    diagnosis = 'Rối loạn tiêu hóa nhẹ; theo dõi thiếu máu.',
    prescription_note = 'Uống đủ nước, ăn thức ăn mềm và theo dõi triệu chứng.',
    conclusion = 'Chưa phát hiện bất thường cấp tính. Công thức máu và siêu âm ổ bụng trong giới hạn theo dõi ngoại trú.',
    patient_instruction = 'Tái khám nếu đau bụng tăng, sốt hoặc mệt nhiều.',
    specialty_data = '{"generalCondition":"Tỉnh táo, tiếp xúc tốt","abdomen":"Bụng mềm, không đề kháng"}'::jsonb,
    completed_at = pg_temp.demo_date()-interval '28 days' + time '09:00'
WHERE record_id = 'd90b3fc2-2b39-d940-d65f-7e6ed2262008';

INSERT INTO invoice_item
    (item_id, created_at, updated_at, deleted, invoice_id, service_id,
     service_snapshot, service_code_snapshot, unit_price, quantity,
     discount_percent, discount_amount, final_price, line_total, note, bhyt_fund)
VALUES
('f1000000-0000-0000-0000-000000000001', pg_temp.demo_now()-interval '28 days', pg_temp.demo_now()-interval '28 days', false,
 pg_temp.did('historical-invoice-07ac9a53-0f97-2fb6-1f9d-34924096efc6'), '40000008-0000-0000-0000-000000000008',
 'Công thức máu', 'LAB-001', 120000, 1, 0, 0, 120000, 120000, 'Xét nghiệm trong lịch sử bệnh án', 0),
('f1000000-0000-0000-0000-000000000002', pg_temp.demo_now()-interval '28 days', pg_temp.demo_now()-interval '28 days', false,
 pg_temp.did('historical-invoice-07ac9a53-0f97-2fb6-1f9d-34924096efc6'), '40000015-0000-0000-0000-000000000015',
 'Siêu âm ổ bụng tổng quát', 'IMG-003', 250000, 1, 0, 0, 250000, 250000, 'Chẩn đoán hình ảnh trong lịch sử bệnh án', 0);

UPDATE invoice
SET subtotal = 590000, total_amount = 590000, paid_amount = 590000,
    updated_at = pg_temp.demo_now()-interval '28 days'
WHERE invoice_id = pg_temp.did('historical-invoice-07ac9a53-0f97-2fb6-1f9d-34924096efc6');

INSERT INTO queue_ticket
    (ticket_id, created_at, updated_at, deleted, visit_id, department_id,
     work_date, queue_number, status, called_at, completed_at, service_id)
VALUES
('f2000000-0000-0000-0000-000000000001', pg_temp.demo_now()-interval '28 days', pg_temp.demo_now()-interval '28 days', false,
 '07ac9a53-0f97-2fb6-1f9d-34924096efc6', '760edff5-8292-2100-0b77-7fc8e316e913', pg_temp.demo_date()-28, 51,
 'DONE', pg_temp.demo_date()-interval '28 days'+time '08:25', pg_temp.demo_date()-interval '28 days'+time '08:45', '40000008-0000-0000-0000-000000000008'),
('f2000000-0000-0000-0000-000000000002', pg_temp.demo_now()-interval '28 days', pg_temp.demo_now()-interval '28 days', false,
 '07ac9a53-0f97-2fb6-1f9d-34924096efc6', '4a0a5c21-f9d4-234b-9631-c80fa2db9881', pg_temp.demo_date()-28, 51,
 'DONE', pg_temp.demo_date()-interval '28 days'+time '08:35', pg_temp.demo_date()-interval '28 days'+time '08:55', '40000015-0000-0000-0000-000000000015');

INSERT INTO test_request
    (test_request_id, created_at, updated_at, deleted, medical_record_id, service_id,
     performing_department, queue_ticket_id, description, status, requested_by,
     completed_at, cancel_reason, invoice_item_id)
VALUES
('f3000000-0000-0000-0000-000000000001', pg_temp.demo_now()-interval '28 days', pg_temp.demo_now()-interval '28 days', false,
 'd90b3fc2-2b39-d940-d65f-7e6ed2262008', '40000008-0000-0000-0000-000000000008',
 '760edff5-8292-2100-0b77-7fc8e316e913', 'f2000000-0000-0000-0000-000000000001', 'Đánh giá thiếu máu và tình trạng nhiễm trùng', 'COMPLETED',
 '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_date()-interval '28 days'+time '08:45', NULL, 'f1000000-0000-0000-0000-000000000001'),
('f3000000-0000-0000-0000-000000000002', pg_temp.demo_now()-interval '28 days', pg_temp.demo_now()-interval '28 days', false,
 'd90b3fc2-2b39-d940-d65f-7e6ed2262008', '40000015-0000-0000-0000-000000000015',
 '4a0a5c21-f9d4-234b-9631-c80fa2db9881', 'f2000000-0000-0000-0000-000000000002', 'Khảo sát đau bụng', 'COMPLETED',
 '8c574ec0-98e1-ad37-3d64-f900c3211b35', pg_temp.demo_date()-interval '28 days'+time '08:55', NULL, 'f1000000-0000-0000-0000-000000000002');

INSERT INTO test_result
    (result_id, created_at, updated_at, deleted, test_request_id, image_url, conclusion,
     result_data, sample_id, sample_type, sample_status, collected_at, collected_by,
     performed_by, performed_at, verified_by, verified_at)
VALUES
('f4000000-0000-0000-0000-000000000001', pg_temp.demo_now()-interval '28 days', pg_temp.demo_now()-interval '28 days', false,
 'f3000000-0000-0000-0000-000000000001', NULL, 'Các chỉ số huyết học trong giới hạn tham chiếu.',
 '{"rbc":4.62,"hgb":138,"hct":41.2,"mcv":89.2,"wbc":6.8,"neutPercent":58.4,"lymphPercent":32.1,"plt":265}'::jsonb,
 'SMP-CBC-HISTORY-003', 'BLOOD', 'ACCEPTED', pg_temp.demo_date()-interval '28 days'+time '08:25',
 '0f348c8d-74b9-3ef2-981b-61105eab787e', '74cf86f9-d830-e246-7dc3-538a0874779d',
 pg_temp.demo_date()-interval '28 days'+time '08:40', '74cf86f9-d830-e246-7dc3-538a0874779d', pg_temp.demo_date()-interval '28 days'+time '08:45'),
('f4000000-0000-0000-0000-000000000002', pg_temp.demo_now()-interval '28 days', pg_temp.demo_now()-interval '28 days', false,
 'f3000000-0000-0000-0000-000000000002', '/demo-results/abdominal-ultrasound-demo.png', 'Chưa ghi nhận bất thường trên siêu âm ổ bụng.',
 '{"description":"Gan, mật, tụy, lách và hai thận chưa thấy bất thường rõ.","findings":"Không có dịch tự do ổ bụng.","conclusion":"Siêu âm ổ bụng trong giới hạn bình thường."}'::jsonb,
 NULL, NULL, NULL, NULL, NULL, '6058143b-c4d4-3840-262f-3aa52b388608',
 pg_temp.demo_date()-interval '28 days'+time '08:50', '6058143b-c4d4-3840-262f-3aa52b388608', pg_temp.demo_date()-interval '28 days'+time '08:55');

INSERT INTO appointment (appointment_id, created_at, updated_at, deleted, scheduled_at, status, is_guest, customer_id, shift_name, shift_time, shift_version_id) VALUES ('1c555a61-e713-eecb-3a22-9cda3f8f7f9a', pg_temp.demo_now(), pg_temp.demo_now(), false, pg_temp.demo_date() + time '14:00', 'PENDING', false, 'b8523868-30ae-7eab-8d18-fb3f1a5488bc', 'Ca Chiều', '13:00-17:00', '71000002-2222-2222-2222-222222222222');

INSERT INTO appointment_services (appointment_id, service_id) VALUES
('1c555a61-e713-eecb-3a22-9cda3f8f7f9a', '40000002-0000-0000-0000-000000000002'),
('1c555a61-e713-eecb-3a22-9cda3f8f7f9a', '40000008-0000-0000-0000-000000000008');

-- Ready-to-use queues for the board demo. One patient waits in Internal
-- Medicine and another has paid for the complete CBC package and waits in LAB.
INSERT INTO customer_visit
    (visit_id, customer_id, appointment_id, check_in_time, check_out_time,
     checked_in_by, status, created_at, updated_at, deleted)
VALUES
('e1000000-0000-0000-0000-000000000001', '1088d62b-ff88-2b61-b142-c59ddd695087', NULL,
 pg_temp.demo_now()-interval '20 minutes', NULL, '50ec3aa8-ede0-2354-2df1-5a50e7a19729',
 'CHECKED_IN', pg_temp.demo_now()-interval '20 minutes', pg_temp.demo_now(), false),
('e1000000-0000-0000-0000-000000000002', 'b96b77f8-3078-38f6-cd3f-19135ce19b9b', NULL,
 pg_temp.demo_now()-interval '15 minutes', NULL, '50ec3aa8-ede0-2354-2df1-5a50e7a19729',
 'IN_PROGRESS', pg_temp.demo_now()-interval '15 minutes', pg_temp.demo_now(), false);

INSERT INTO queue_ticket
    (ticket_id, created_at, updated_at, deleted, visit_id, department_id,
     work_date, queue_number, status, called_at, completed_at, service_id)
VALUES
('e2000000-0000-0000-0000-000000000001', pg_temp.demo_now()-interval '20 minutes', pg_temp.demo_now(), false,
 'e1000000-0000-0000-0000-000000000001', '4d7ac047-a699-7a37-b2c0-e8392593e7bc',
 pg_temp.demo_date(), 1, 'WAITING', NULL, NULL, '40000002-0000-0000-0000-000000000002'),
('e2000000-0000-0000-0000-000000000002', pg_temp.demo_now()-interval '15 minutes', pg_temp.demo_now(), false,
 'e1000000-0000-0000-0000-000000000002', '760edff5-8292-2100-0b77-7fc8e316e913',
 pg_temp.demo_date(), 1, 'WAITING', NULL, NULL, '40000008-0000-0000-0000-000000000008');

INSERT INTO medical_record
    (record_id, record_code, visit_id, queue_ticket_id, doctor_id, status,
     created_at, updated_at, deleted)
VALUES
('e3000000-0000-0000-0000-000000000001', 'MR-DEMO-CBC-TODAY',
 'e1000000-0000-0000-0000-000000000002', NULL,
 '74cf86f9-d830-e246-7dc3-538a0874779d', 'IN_PROGRESS',
 pg_temp.demo_now()-interval '15 minutes', pg_temp.demo_now(), false);

INSERT INTO invoice
    (invoice_id, created_at, deleted, updated_at, discount, due_date, invoice_code,
     issue_date, note, paid_amount, status, subtotal, tax, total_amount,
     customer_id, issued_by, medical_record_id, visit_id)
VALUES
('e4000000-0000-0000-0000-000000000001', pg_temp.demo_now()-interval '15 minutes', false, pg_temp.demo_now()-interval '12 minutes',
 0, NULL, 'INV-DEMO-CBC-TODAY', pg_temp.demo_date(), 'Gói Công thức máu đầy đủ đã thanh toán',
 120000, 'PAID', 120000, 0, 120000,
 'b96b77f8-3078-38f6-cd3f-19135ce19b9b', 'b693d136-402d-7de2-4835-117a5e2c5411',
 'e3000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000002');

INSERT INTO invoice_item
    (item_id, created_at, updated_at, deleted, invoice_id, service_id,
     service_snapshot, service_code_snapshot, unit_price, quantity,
     discount_percent, discount_amount, final_price, line_total, note, bhyt_fund)
VALUES
('e5000000-0000-0000-0000-000000000001', pg_temp.demo_now()-interval '15 minutes', pg_temp.demo_now(), false,
 'e4000000-0000-0000-0000-000000000001', '40000008-0000-0000-0000-000000000008',
 'Công thức máu', 'LAB-001', 120000, 1, 0, 0, 120000, 120000,
 'Bao gồm đủ 8 chỉ số chính của gói CBC', 0);

INSERT INTO test_request
    (test_request_id, created_at, updated_at, deleted, medical_record_id, service_id,
     performing_department, queue_ticket_id, description, status, requested_by,
     completed_at, cancel_reason, invoice_item_id)
VALUES
('e6000000-0000-0000-0000-000000000001', pg_temp.demo_now()-interval '15 minutes', pg_temp.demo_now(), false,
 'e3000000-0000-0000-0000-000000000001', '40000008-0000-0000-0000-000000000008',
 '760edff5-8292-2100-0b77-7fc8e316e913', 'e2000000-0000-0000-0000-000000000002',
 'Thực hiện gói Công thức máu đầy đủ', 'PENDING', '74cf86f9-d830-e246-7dc3-538a0874779d',
 NULL, NULL, 'e5000000-0000-0000-0000-000000000001');

-- Phiếu CBC hôm nay chỉ mới thanh toán và đang chờ vào phòng nên chưa có
-- test_result. Dữ liệu CBC đã hoàn thành để xem lịch sử nằm ở phiếu f300...,
-- tránh trạng thái "vừa gọi bệnh nhân" nhưng kết quả đã hoàn thành 8/8.

-- ---------------------------------------------------------------------------
-- UI demo history: notifications, support chat, announcements and audit trail
-- ---------------------------------------------------------------------------
INSERT INTO notification
    (notification_id, recipient_id, notification_type, channel, title, content,
     related_entity, related_entity_id, status, sent_at, read_at,
     created_at, updated_at, deleted)
VALUES
('a1000000-0000-0000-0000-000000000001', 'b8523868-30ae-7eab-8d18-fb3f1a5488bc',
 'APPOINTMENT_CONFIRMED', 'IN_APP', 'Lịch hẹn đã được xác nhận',
 'Lịch khám Nội tổng quát hôm nay của bạn đã được ghi nhận.',
 'Appointment', '1c555a61-e713-eecb-3a22-9cda3f8f7f9a', 'SENT',
 pg_temp.demo_now()-interval '2 hours', NULL,
 pg_temp.demo_now()-interval '2 hours', pg_temp.demo_now()-interval '2 hours', false),
('a1000000-0000-0000-0000-000000000002', 'b8523868-30ae-7eab-8d18-fb3f1a5488bc',
 'APPOINTMENT_REMINDER', 'IN_APP', 'Nhắc lịch khám hôm nay',
 'Vui lòng có mặt trước giờ hẹn 15 phút để làm thủ tục tiếp nhận.',
 'Appointment', '1c555a61-e713-eecb-3a22-9cda3f8f7f9a', 'PENDING',
 NULL, NULL, pg_temp.demo_now()-interval '30 minutes', pg_temp.demo_now()-interval '30 minutes', false),
('a1000000-0000-0000-0000-000000000003', 'b96b77f8-3078-38f6-cd3f-19135ce19b9b',
 'TEST_RESULT_READY', 'IN_APP', 'Kết quả Công thức máu đã sẵn sàng',
 'Bạn có thể xem kết quả Công thức máu trong lịch sử khám bệnh.',
 'TestRequest', 'f3000000-0000-0000-0000-000000000001', 'READ',
 pg_temp.demo_now()-interval '28 days', pg_temp.demo_now()-interval '27 days',
 pg_temp.demo_now()-interval '28 days', pg_temp.demo_now()-interval '27 days', false),
('a1000000-0000-0000-0000-000000000004', 'b96b77f8-3078-38f6-cd3f-19135ce19b9b',
 'PAYMENT_SUCCESS', 'IN_APP', 'Thanh toán thành công',
 'Hóa đơn khám và cận lâm sàng đã được thanh toán thành công bằng thẻ CareS.',
 'Invoice', pg_temp.did('historical-invoice-07ac9a53-0f97-2fb6-1f9d-34924096efc6'), 'READ',
 pg_temp.demo_now()-interval '28 days', pg_temp.demo_now()-interval '27 days',
 pg_temp.demo_now()-interval '28 days', pg_temp.demo_now()-interval '27 days', false),
('a1000000-0000-0000-0000-000000000005', '5c1bb2a3-4cb5-9911-7fbb-aa9b22335b16',
 'GENERAL', 'IN_APP', 'Có khách hàng cần hỗ trợ',
 'Một cuộc hội thoại mới đang chờ lễ tân tiếp nhận.',
 'ChatSession', 'a2000000-0000-0000-0000-000000000003', 'SENT',
 pg_temp.demo_now()-interval '5 minutes', NULL,
 pg_temp.demo_now()-interval '5 minutes', pg_temp.demo_now()-interval '5 minutes', false),
('a1000000-0000-0000-0000-000000000006', '01f27858-d61c-12d5-13d5-f04475291f93',
 'TEST_RESULT_READY', 'IN_APP', 'Kết quả cận lâm sàng đã hoàn tất',
 'Kết quả Công thức máu của bệnh nhân đã được ký và có thể xem trong bệnh án.',
 'TestRequest', 'f3000000-0000-0000-0000-000000000001', 'SENT',
 pg_temp.demo_now()-interval '28 days', NULL,
 pg_temp.demo_now()-interval '28 days', pg_temp.demo_now()-interval '28 days', false),
('a1000000-0000-0000-0000-000000000007', '9e1e1b02-7c09-4a95-7a22-e3765a1f9f5f',
 'PAYMENT_SUCCESS', 'IN_APP', 'Đã ghi nhận thanh toán',
 'Hóa đơn INV-DEMO-CBC-TODAY đã được thanh toán đủ.',
 'Invoice', 'e4000000-0000-0000-0000-000000000001', 'READ',
 pg_temp.demo_now()-interval '12 minutes', pg_temp.demo_now()-interval '10 minutes',
 pg_temp.demo_now()-interval '12 minutes', pg_temp.demo_now()-interval '10 minutes', false),
('a1000000-0000-0000-0000-000000000008', 'b8523868-30ae-7eab-8d18-fb3f1a5488bc',
 'GENERAL', 'IN_APP', 'Chào mừng bạn đến với CareS',
 'CareS hỗ trợ đặt lịch, theo dõi hành trình khám và xem kết quả trực tuyến.',
 NULL, NULL, 'READ', pg_temp.demo_now()-interval '20 days', pg_temp.demo_now()-interval '19 days',
 pg_temp.demo_now()-interval '20 days', pg_temp.demo_now()-interval '19 days', false);

INSERT INTO chat_sessions
    (session_id, customer_id, status, assigned_receptionist_id, created_at, updated_at, deleted)
VALUES
('a2000000-0000-0000-0000-000000000001', 'b8523868-30ae-7eab-8d18-fb3f1a5488bc', 'CLOSED',
 '50ec3aa8-ede0-2354-2df1-5a50e7a19729', pg_temp.demo_now()-interval '7 days', pg_temp.demo_now()-interval '7 days'+interval '12 minutes', false),
('a2000000-0000-0000-0000-000000000002', 'b96b77f8-3078-38f6-cd3f-19135ce19b9b', 'IN_PROGRESS',
 '50ec3aa8-ede0-2354-2df1-5a50e7a19729', pg_temp.demo_now()-interval '25 minutes', pg_temp.demo_now()-interval '8 minutes', false),
('a2000000-0000-0000-0000-000000000003', '1088d62b-ff88-2b61-b142-c59ddd695087', 'WAITING_FOR_AGENT',
 NULL, pg_temp.demo_now()-interval '6 minutes', pg_temp.demo_now()-interval '4 minutes', false);

INSERT INTO chat_messages
    (message_id, session_id, sender_type, sender_id, content, created_at, updated_at, deleted)
VALUES
('a3000000-0000-0000-0000-000000000001', 'a2000000-0000-0000-0000-000000000001', 'CUSTOMER',
 'b8523868-30ae-7eab-8d18-fb3f1a5488bc', 'Tôi muốn hỏi cần đến trước giờ hẹn bao lâu?',
 pg_temp.demo_now()-interval '7 days', pg_temp.demo_now()-interval '7 days', false),
('a3000000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000001', 'BOT',
 NULL, 'Bạn nên có mặt trước giờ hẹn khoảng 15 phút. Tôi sẽ chuyển lễ tân hỗ trợ thêm.',
 pg_temp.demo_now()-interval '7 days'+interval '1 minute', pg_temp.demo_now()-interval '7 days'+interval '1 minute', false),
('a3000000-0000-0000-0000-000000000003', 'a2000000-0000-0000-0000-000000000001', 'RECEPTIONIST',
 '50ec3aa8-ede0-2354-2df1-5a50e7a19729', 'Anh vui lòng mang theo giấy tờ tùy thân và đến trước 15 phút nhé.',
 pg_temp.demo_now()-interval '7 days'+interval '8 minutes', pg_temp.demo_now()-interval '7 days'+interval '8 minutes', false),
('a3000000-0000-0000-0000-000000000004', 'a2000000-0000-0000-0000-000000000001', 'CUSTOMER',
 'b8523868-30ae-7eab-8d18-fb3f1a5488bc', 'Cảm ơn bạn, tôi đã rõ.',
 pg_temp.demo_now()-interval '7 days'+interval '11 minutes', pg_temp.demo_now()-interval '7 days'+interval '11 minutes', false),
('a3000000-0000-0000-0000-000000000005', 'a2000000-0000-0000-0000-000000000002', 'CUSTOMER',
 'b96b77f8-3078-38f6-cd3f-19135ce19b9b', 'Tôi muốn được hướng dẫn xem lại kết quả xét nghiệm.',
 pg_temp.demo_now()-interval '25 minutes', pg_temp.demo_now()-interval '25 minutes', false),
('a3000000-0000-0000-0000-000000000006', 'a2000000-0000-0000-0000-000000000002', 'BOT',
 NULL, 'Bạn có thể mở mục Lịch sử khám bệnh. Lễ tân đang tiếp nhận yêu cầu của bạn.',
 pg_temp.demo_now()-interval '24 minutes', pg_temp.demo_now()-interval '24 minutes', false),
('a3000000-0000-0000-0000-000000000007', 'a2000000-0000-0000-0000-000000000002', 'RECEPTIONIST',
 '50ec3aa8-ede0-2354-2df1-5a50e7a19729', 'Chị chọn lượt khám gần nhất rồi mở tab Kết quả cận lâm sàng nhé.',
 pg_temp.demo_now()-interval '8 minutes', pg_temp.demo_now()-interval '8 minutes', false),
('a3000000-0000-0000-0000-000000000008', 'a2000000-0000-0000-0000-000000000003', 'CUSTOMER',
 '1088d62b-ff88-2b61-b142-c59ddd695087', 'Tôi có thể đổi giờ khám trong ngày hôm nay không?',
 pg_temp.demo_now()-interval '6 minutes', pg_temp.demo_now()-interval '6 minutes', false),
('a3000000-0000-0000-0000-000000000009', 'a2000000-0000-0000-0000-000000000003', 'BOT',
 NULL, 'Yêu cầu cần lễ tân kiểm tra lịch trống. Bạn vui lòng chờ trong ít phút.',
 pg_temp.demo_now()-interval '4 minutes', pg_temp.demo_now()-interval '4 minutes', false);

INSERT INTO public_announcement
    (announcement_id, title, content, published, starts_at, ends_at, created_at, updated_at, deleted)
VALUES
('a4000000-0000-0000-0000-000000000001', 'CareS hỗ trợ đặt lịch khám trực tuyến',
 'Khách hàng có thể đặt lịch, theo dõi hành trình khám và xem kết quả ngay trên hệ thống CareS.',
 true, pg_temp.demo_now()-interval '2 days', pg_temp.demo_now()+interval '14 days',
 pg_temp.demo_now()-interval '3 days', pg_temp.demo_now()-interval '2 days', false),
('a4000000-0000-0000-0000-000000000002', 'Chương trình tư vấn sức khỏe cuối tuần',
 'Thông tin chương trình sẽ được hiển thị khi bắt đầu có hiệu lực.',
 true, pg_temp.demo_now()+interval '3 days', pg_temp.demo_now()+interval '10 days',
 pg_temp.demo_now()-interval '1 day', pg_temp.demo_now()-interval '1 day', false),
('a4000000-0000-0000-0000-000000000003', 'Thông báo bảo trì đã kết thúc',
 'Hệ thống đã hoàn tất đợt bảo trì định kỳ.',
 true, pg_temp.demo_now()-interval '20 days', pg_temp.demo_now()-interval '10 days',
 pg_temp.demo_now()-interval '21 days', pg_temp.demo_now()-interval '10 days', false),
('a4000000-0000-0000-0000-000000000004', 'Nội dung đang soạn thảo',
 'Bản nháp dùng để kiểm tra chức năng quản lý thông báo công khai.',
 false, NULL, NULL, pg_temp.demo_now()-interval '1 day', pg_temp.demo_now(), false);

INSERT INTO audit_log
    (audit_id, action, entity_name, entity_id, actor_account_id, ip_address,
     user_agent, old_value, new_value, description, created_at)
VALUES
('a5000000-0000-0000-0000-000000000001', 'LOGIN', 'Account',
 'a780d597-bf34-b2c4-df00-a72a966f6d4b', 'a780d597-bf34-b2c4-df00-a72a966f6d4b',
 '127.0.0.1', 'CareS Demo Browser', NULL, NULL, 'Khách hàng đăng nhập thành công', pg_temp.demo_now()-interval '3 hours'),
('a5000000-0000-0000-0000-000000000002', 'CREATE', 'Appointment',
 '1c555a61-e713-eecb-3a22-9cda3f8f7f9a', 'a780d597-bf34-b2c4-df00-a72a966f6d4b',
 '127.0.0.1', 'CareS Demo Browser', NULL, NULL, 'Khách hàng tạo lịch hẹn khám', pg_temp.demo_now()-interval '2 hours'),
('a5000000-0000-0000-0000-000000000003', 'STATUS_CHANGE', 'CustomerVisit',
 'e1000000-0000-0000-0000-000000000002', 'b8c70898-f359-5a82-a271-d7cc9544d309',
 '127.0.0.1', 'CareS Reception Desk', NULL, NULL, 'Lễ tân tiếp nhận bệnh nhân', pg_temp.demo_now()-interval '15 minutes'),
('a5000000-0000-0000-0000-000000000004', 'PAYMENT_CONFIRMED', 'Invoice',
 'e4000000-0000-0000-0000-000000000001', 'fdd9d836-6aa9-ca03-89bb-b5da1a2fe927',
 '127.0.0.1', 'CareS Cashier Desk', NULL, NULL, 'Thu ngân xác nhận hóa đơn đã thanh toán', pg_temp.demo_now()-interval '12 minutes'),
('a5000000-0000-0000-0000-000000000005', 'RECORD_COMPLETED', 'MedicalRecord',
 'd90b3fc2-2b39-d940-d65f-7e6ed2262008', 'f5bb1a27-4825-1522-b4bd-8397f4082fd2',
 '127.0.0.1', 'CareS Doctor Workspace', NULL, NULL, 'Bác sĩ hoàn thành bệnh án', pg_temp.demo_now()-interval '28 days'),
('a5000000-0000-0000-0000-000000000006', 'RESULT_SIGNED', 'TestResult',
 'f4000000-0000-0000-0000-000000000001', '6a02bdd4-6d56-2f89-dc86-1082628a2280',
 '127.0.0.1', 'CareS Laboratory Workspace', NULL, NULL, 'Nhân viên phụ trách ký kết quả xét nghiệm', pg_temp.demo_now()-interval '28 days');

-- ---------------------------------------------------------------------------
-- Complete billing, membership-card and historical clinical demo data
-- ---------------------------------------------------------------------------
INSERT INTO payment_transaction
    (transaction_id, invoice_id, transaction_code, amount, payment_method, status,
     paid_at, gateway_reference, note, received_by, created_at, updated_at, deleted)
SELECT pg_temp.did('payment-' || i.invoice_id::text), i.invoice_id,
       'PAY-DEMO-' || upper(substring(i.invoice_id::text, 1, 8)), i.paid_amount,
       CASE WHEN i.invoice_id = pg_temp.did('historical-invoice-07ac9a53-0f97-2fb6-1f9d-34924096efc6')
            THEN 'MEMBERSHIP_CARD' ELSE 'CASH' END,
       'SUCCESS', COALESCE(i.updated_at, pg_temp.demo_now()), NULL,
       CASE WHEN i.invoice_id = pg_temp.did('historical-invoice-07ac9a53-0f97-2fb6-1f9d-34924096efc6')
            THEN 'Thanh toán bằng số dư thẻ CareS' ELSE 'Thanh toán tại quầy' END,
       CASE WHEN i.invoice_id = pg_temp.did('historical-invoice-07ac9a53-0f97-2fb6-1f9d-34924096efc6')
            THEN NULL ELSE 'b693d136-402d-7de2-4835-117a5e2c5411'::uuid END,
       COALESCE(i.updated_at, pg_temp.demo_now()), COALESCE(i.updated_at, pg_temp.demo_now()), false
FROM invoice i
WHERE i.status = 'PAID' AND i.deleted = false AND i.paid_amount > 0;

INSERT INTO membership_card
    (card_id, card_code, owner_profile_id, status, balance, pin_hash, benefit_percent,
     activated_at, benefit_starts_at, benefit_expires_at, version,
     created_at, updated_at, deleted)
VALUES
('a6000000-0000-0000-0000-000000000001', 'CS-DEMO-MINHANH',
 'b96b77f8-3078-38f6-cd3f-19135ce19b9b', 'ACTIVE', 1410000,
 '$2a$10$DBBnrVLhrw3HMavDBJhJP.XmfA/NeBJVkzEdjCKL730thIeT0PZKa', 15,
 pg_temp.demo_now()-interval '35 days', pg_temp.demo_date()-interval '34 days',
 pg_temp.demo_date()+interval '11 months', 0,
 pg_temp.demo_now()-interval '35 days', pg_temp.demo_now()-interval '28 days', false);

INSERT INTO membership_card_ledger
    (ledger_id, card_id, type, amount, balance_before, balance_after, invoice_id,
     patient_profile_id, performed_by, payment_transaction_id, source_payment_method,
     idempotency_key, reference_code, benefit_discount, reversed_ledger_id, reason,
     created_at, updated_at, deleted)
VALUES
('a7000000-0000-0000-0000-000000000001', 'a6000000-0000-0000-0000-000000000001',
 'TOP_UP', 2000000, 0, 2000000, NULL, NULL,
 'b693d136-402d-7de2-4835-117a5e2c5411', NULL, 'CASH',
 'DEMO-TOPUP-MINHANH', 'NT-DEMO-0001', 0, NULL, 'Nạp tiền kích hoạt thẻ demo',
 pg_temp.demo_now()-interval '35 days', pg_temp.demo_now()-interval '35 days', false),
('a7000000-0000-0000-0000-000000000002', 'a6000000-0000-0000-0000-000000000001',
 'PAYMENT', 590000, 2000000, 1410000,
 pg_temp.did('historical-invoice-07ac9a53-0f97-2fb6-1f9d-34924096efc6'),
 'b96b77f8-3078-38f6-cd3f-19135ce19b9b', NULL,
 pg_temp.did('payment-' || pg_temp.did('historical-invoice-07ac9a53-0f97-2fb6-1f9d-34924096efc6')::text),
 'MEMBERSHIP_CARD', 'DEMO-PAY-MINHANH-HISTORY', 'TT-DEMO-0001', 0, NULL,
 'Thanh toán lượt khám lịch sử bằng thẻ CareS',
 pg_temp.demo_now()-interval '28 days', pg_temp.demo_now()-interval '28 days', false);

INSERT INTO family_member
    (family_member_id, owner_profile_id, member_profile_id, relationship, is_active,
     created_at, updated_at, deleted)
VALUES
('a8000000-0000-0000-0000-000000000001', 'b8523868-30ae-7eab-8d18-fb3f1a5488bc',
 '3bc5bf5c-0ef7-1913-53c9-3a1ab64bbd37', 'OTHER', true,
 pg_temp.demo_now()-interval '5 days', pg_temp.demo_now()-interval '5 days', false);

INSERT INTO vital_signs
    (vital_id, medical_record_id, blood_pressure, heart_rate, temperature, weight,
     height, recorded_at, recorded_by, created_at, updated_at, deleted)
VALUES
('a9000000-0000-0000-0000-000000000001', 'd90b3fc2-2b39-d940-d65f-7e6ed2262008',
 '118/76', 78, 36.7, 52.40, 160.00,
 pg_temp.demo_date()-interval '28 days'+time '08:10', 'd8dccd88-7bbc-a7b7-a014-572106a75b15',
 pg_temp.demo_now()-interval '28 days', pg_temp.demo_now()-interval '28 days', false);

INSERT INTO icd_10_selections
    (selection_id, record_id, code, code_name, note, created_at, updated_at, deleted)
VALUES
('aa000000-0000-0000-0000-000000000001', 'd90b3fc2-2b39-d940-d65f-7e6ed2262008',
 'Z00.0', 'Khám sức khỏe tổng quát', 'Theo dõi ngoại trú sau khi có kết quả cận lâm sàng.',
 pg_temp.demo_now()-interval '28 days', pg_temp.demo_now()-interval '28 days', false);

INSERT INTO prescription_item
    (prescription_item_id, record_id, medicine_name, quantity, unit, note,
     frequency_per_day, created_at, updated_at, deleted)
VALUES
('ab000000-0000-0000-0000-000000000001', 'd90b3fc2-2b39-d940-d65f-7e6ed2262008',
 'Paracetamol 500mg', 6, 'Viên', 'Uống sau ăn khi đau hoặc sốt, tối đa 2 viên mỗi ngày.', 2,
 pg_temp.demo_now()-interval '28 days', pg_temp.demo_now()-interval '28 days', false);

-- ---------------------------------------------------------------------------
-- Rolling 60-day reporting demo
-- ---------------------------------------------------------------------------
-- The operational reports filter each aggregate by its own business timestamp:
-- visit check-in/check-out, medical-record completion, invoice issue date and
-- payment paid_at. Keep those timestamps aligned and spread the historical
-- journeys over the latest two months so every report tab has meaningful data.
CREATE TEMP TABLE report_demo_visit_map ON COMMIT DROP AS
WITH ranked AS (
    SELECT v.visit_id,
           row_number() OVER (ORDER BY v.check_in_time, v.visit_id) AS sequence_no,
           count(*) OVER () AS visit_count
    FROM customer_visit v
    JOIN invoice i ON i.visit_id = v.visit_id
    WHERE i.invoice_code ~ '^INV-DEMO-[0-9]{4}$'
      AND v.visit_id NOT IN (
          'd5ee978d-cb75-979f-16ff-d348536a3a85'::uuid,
          '07ac9a53-0f97-2fb6-1f9d-34924096efc6'::uuid
      )
)
SELECT visit_id,
       sequence_no,
       pg_temp.demo_date()
           - round(58.0 * (visit_count - sequence_no) / greatest(visit_count - 1, 1))::integer AS work_date,
       CASE
           WHEN mod(sequence_no - 1, 5) = 0 THEN 'INT-101'
           WHEN mod(sequence_no - 1, 5) = 1 THEN 'INT-102'
           WHEN mod(sequence_no - 1, 5) = 2 THEN 'SUR-201'
           WHEN mod(sequence_no - 1, 5) = 3 THEN 'SUR-202'
           ELSE 'DER-401'
       END AS room_code,
       CASE
           WHEN mod(sequence_no - 1, 5) = 0 THEN 'EX-IN-001'
           WHEN mod(sequence_no - 1, 5) = 1 THEN 'EX-IN-002'
           WHEN mod(sequence_no - 1, 5) = 2 THEN 'EX-SU-001'
           WHEN mod(sequence_no - 1, 5) = 3 THEN 'EX-SU-002'
           ELSE 'EX-DER-001'
       END AS service_code
FROM ranked;

UPDATE customer_visit v
SET check_in_time = m.work_date + time '08:00',
    check_out_time = m.work_date + time '09:00',
    created_at = m.work_date + time '07:55',
    updated_at = m.work_date + time '09:05'
FROM report_demo_visit_map m
WHERE v.visit_id = m.visit_id;

UPDATE queue_ticket q
SET department_id = d.department_id,
    service_id = s.service_id,
    work_date = m.work_date,
    queue_number = 20 + m.sequence_no,
    called_at = m.work_date + time '08:05',
    completed_at = m.work_date + time '08:55',
    created_at = m.work_date + time '08:00',
    updated_at = m.work_date + time '08:55'
FROM report_demo_visit_map m
JOIN department d ON d.room_code = m.room_code
JOIN medical_service s ON s.service_code = m.service_code
WHERE q.ticket_id = pg_temp.did('historical-ticket-' || m.visit_id::text);

-- Every completed examination must point at its examination ticket and have a
-- completion time; otherwise the report correctly excludes it.
UPDATE medical_record mr
SET queue_ticket_id = q.ticket_id,
    doctor_id = d.head_doctor_id,
    completed_at = v.check_out_time - interval '5 minutes',
    rating_score = 3 + mod(m.sequence_no::integer, 3),
    rated_at = v.check_out_time + interval '2 hours',
    created_at = v.check_in_time,
    updated_at = v.check_out_time
FROM report_demo_visit_map m
JOIN customer_visit v ON v.visit_id = m.visit_id
JOIN queue_ticket q ON q.ticket_id = pg_temp.did('historical-ticket-' || m.visit_id::text)
JOIN department d ON d.department_id = q.department_id
WHERE mr.visit_id = m.visit_id;

UPDATE invoice i
SET issue_date = m.work_date,
    created_at = m.work_date + time '08:10',
    updated_at = m.work_date + time '09:00'
FROM report_demo_visit_map m
WHERE i.visit_id = m.visit_id
  AND i.invoice_code ~ '^INV-DEMO-[0-9]{4}$';

-- Keep the examination line on each historical invoice consistent with the
-- room/service used by that visit, including the snapshot price.
UPDATE invoice_item ii
SET service_id = s.service_id,
    service_snapshot = s.name,
    service_code_snapshot = s.service_code,
    unit_price = s.price,
    final_price = s.price,
    line_total = s.price,
    created_at = m.work_date + time '08:10',
    updated_at = m.work_date + time '08:10'
FROM report_demo_visit_map m
JOIN invoice i ON i.visit_id = m.visit_id AND i.invoice_code ~ '^INV-DEMO-[0-9]{4}$'
JOIN medical_service s ON s.service_code = m.service_code
WHERE ii.item_id = pg_temp.did('historical-exam-item-' || i.invoice_id::text);

-- Recalculate historical invoices after changing their representative service.
UPDATE invoice i
SET subtotal = totals.amount,
    total_amount = totals.amount,
    paid_amount = totals.amount
FROM (
    SELECT ii.invoice_id, sum(ii.line_total) AS amount
    FROM invoice_item ii
    WHERE ii.deleted = false
    GROUP BY ii.invoice_id
) totals
WHERE i.invoice_id = totals.invoice_id
  AND i.invoice_code ~ '^INV-DEMO-[0-9]{4}$';

-- Payment reports use paid_at, not invoice issue_date. Align both and rotate
-- common payment methods to make the payment-method breakdown demonstrable.
UPDATE payment_transaction pt
SET amount = i.paid_amount,
    payment_method = CASE
        WHEN pt.payment_method = 'MEMBERSHIP_CARD' THEN 'MEMBERSHIP_CARD'
        WHEN mod(m.sequence_no::integer, 3) = 0 THEN 'BANK_TRANSFER'
        WHEN mod(m.sequence_no::integer, 3) = 1 THEN 'CASH'
        ELSE 'CARD'
    END,
    paid_at = m.work_date + time '08:20',
    created_at = m.work_date + time '08:20',
    updated_at = m.work_date + time '08:20'
FROM report_demo_visit_map m
JOIN invoice i ON i.visit_id = m.visit_id AND i.invoice_code ~ '^INV-DEMO-[0-9]{4}$'
WHERE pt.invoice_id = i.invoice_id;

-- Keep the two rich clinical journeys at their original dates, but make their
-- report-facing timestamps complete as well.
UPDATE customer_visit v
SET created_at = v.check_in_time - interval '5 minutes',
    updated_at = v.check_out_time + interval '5 minutes'
WHERE v.visit_id IN (
    'd5ee978d-cb75-979f-16ff-d348536a3a85'::uuid,
    '07ac9a53-0f97-2fb6-1f9d-34924096efc6'::uuid
);

UPDATE medical_record mr
SET queue_ticket_id = q.ticket_id,
    completed_at = v.check_out_time - interval '5 minutes',
    rating_score = CASE WHEN v.visit_id = '07ac9a53-0f97-2fb6-1f9d-34924096efc6'::uuid THEN 5 ELSE 4 END,
    rated_at = v.check_out_time + interval '2 hours'
FROM customer_visit v
JOIN queue_ticket q ON q.ticket_id = pg_temp.did('historical-ticket-' || v.visit_id::text)
WHERE mr.visit_id = v.visit_id
  AND v.visit_id IN (
      'd5ee978d-cb75-979f-16ff-d348536a3a85'::uuid,
      '07ac9a53-0f97-2fb6-1f9d-34924096efc6'::uuid
  );

UPDATE payment_transaction pt
SET amount = i.paid_amount,
    paid_at = i.issue_date + time '08:20',
    created_at = i.issue_date + time '08:20',
    updated_at = i.issue_date + time '08:20'
FROM invoice i
WHERE pt.invoice_id = i.invoice_id
  AND i.invoice_code ~ '^INV-DEMO-[0-9]{4}$'
  AND i.visit_id IN (
      'd5ee978d-cb75-979f-16ff-d348536a3a85'::uuid,
      '07ac9a53-0f97-2fb6-1f9d-34924096efc6'::uuid
  );

-- Complete the original 25 historical journeys as real end-to-end workflows.
-- They were originally useful for invoice charts, but visits were not linked
-- back to their appointments and most records contained no clinical details.
UPDATE customer_visit v
SET appointment_id = a.appointment_id,
    checked_in_by = '50ec3aa8-ede0-2354-2df1-5a50e7a19729'
FROM appointment a
WHERE a.customer_id = v.customer_id
  AND a.status = 'CHECKED_IN' AND a.deleted = false
  AND v.status = 'COMPLETED' AND v.deleted = false
  AND EXISTS (SELECT 1 FROM invoice i WHERE i.visit_id = v.visit_id
              AND i.invoice_code ~ '^INV-DEMO-[0-9]{4}$');

UPDATE appointment a
SET scheduled_at = v.check_in_time - interval '30 minutes',
    created_at = v.check_in_time - interval '2 days',
    updated_at = v.check_out_time
FROM customer_visit v
WHERE v.appointment_id = a.appointment_id
  AND EXISTS (SELECT 1 FROM invoice i WHERE i.visit_id = v.visit_id
              AND i.invoice_code ~ '^INV-DEMO-[0-9]{4}$');

INSERT INTO appointment_services (appointment_id, service_id)
SELECT DISTINCT v.appointment_id, q.service_id
FROM customer_visit v
JOIN invoice i ON i.visit_id = v.visit_id AND i.invoice_code ~ '^INV-DEMO-[0-9]{4}$'
JOIN queue_ticket q ON q.visit_id = v.visit_id AND q.service_id IS NOT NULL AND q.deleted = false
WHERE v.appointment_id IS NOT NULL
ON CONFLICT DO NOTHING;

UPDATE medical_record mr
SET chief_complaint = COALESCE(mr.chief_complaint,
        CASE
            WHEN s.service_code LIKE 'EX-SU-%' THEN 'Đau và sưng nhẹ vùng phần mềm sau vận động.'
            WHEN s.service_code LIKE 'EX-DER-%' THEN 'Ngứa và nổi ban đỏ khu trú.'
            ELSE 'Mệt mỏi nhẹ, đến kiểm tra sức khỏe.'
        END),
    clinical_findings = COALESCE(mr.clinical_findings,
        CASE
            WHEN s.service_code LIKE 'EX-SU-%' THEN 'Sưng nhẹ, vận động còn tốt, chưa ghi nhận dấu hiệu gãy xương.'
            WHEN s.service_code LIKE 'EX-DER-%' THEN 'Mảng đỏ khu trú, không rỉ dịch, chưa có dấu hiệu nhiễm trùng.'
            ELSE 'Bệnh nhân tỉnh, tiếp xúc tốt, tim đều, phổi thông khí rõ.'
        END),
    diagnosis = COALESCE(mr.diagnosis,
        CASE
            WHEN s.service_code LIKE 'EX-SU-%' THEN 'Chấn thương phần mềm mức độ nhẹ.'
            WHEN s.service_code LIKE 'EX-DER-%' THEN 'Viêm da không đặc hiệu.'
            ELSE 'Khám sức khỏe định kỳ, chưa ghi nhận bất thường cấp tính.'
        END),
    prescription_note = COALESCE(mr.prescription_note, 'Dùng thuốc đúng hướng dẫn; không tự ý tăng liều.'),
    conclusion = COALESCE(mr.conclusion, 'Tình trạng ổn định, điều trị và theo dõi ngoại trú.'),
    patient_instruction = COALESCE(mr.patient_instruction,
        'Nghỉ ngơi, uống đủ nước và tái khám sớm khi triệu chứng tăng.'),
    specialty_data = COALESCE(mr.specialty_data, jsonb_build_object(
        'generalCondition', 'Tỉnh táo, tiếp xúc tốt',
        'serviceCode', s.service_code,
        'demoWorkflow', true
    )),
    follow_up_note = COALESCE(mr.follow_up_note, 'Tái khám nếu triệu chứng chưa cải thiện.'),
    follow_up_date = COALESCE(mr.follow_up_date, mr.completed_at::date + 14)
FROM customer_visit v
JOIN invoice i ON i.visit_id = v.visit_id AND i.invoice_code ~ '^INV-DEMO-[0-9]{4}$'
JOIN queue_ticket q ON q.visit_id = v.visit_id
JOIN medical_service s ON s.service_id = q.service_id
WHERE mr.visit_id = v.visit_id AND q.ticket_id = mr.queue_ticket_id;

INSERT INTO vital_signs
    (vital_id, medical_record_id, blood_pressure, heart_rate, temperature,
     weight, height, recorded_at, recorded_by, created_at, updated_at, deleted)
SELECT pg_temp.did('historical-vital-' || mr.record_id::text), mr.record_id,
       (116 + mod(abs(hashtext(mr.record_id::text)), 9))::text || '/' ||
           (72 + mod(abs(hashtext(mr.record_id::text)), 8))::text,
       68 + mod(abs(hashtext(mr.record_id::text)), 18),
       36.5 + (mod(abs(hashtext(mr.record_id::text)), 4) * 0.1),
       50.0 + mod(abs(hashtext(mr.record_id::text)), 25),
       155.0 + mod(abs(hashtext(mr.record_id::text)), 20),
       v.check_in_time + interval '5 minutes',
       COALESCE((SELECT si.staff_id FROM staff_info si
                 WHERE si.department_id = q.department_id
                   AND si.system_role = 'NURSE' AND si.deleted = false
                 ORDER BY si.staff_id LIMIT 1),
                'd8dccd88-7bbc-a7b7-a014-572106a75b15'::uuid),
       v.check_in_time + interval '5 minutes', v.check_in_time + interval '5 minutes', false
FROM medical_record mr
JOIN customer_visit v ON v.visit_id = mr.visit_id
JOIN invoice i ON i.visit_id = v.visit_id AND i.invoice_code ~ '^INV-DEMO-[0-9]{4}$'
JOIN queue_ticket q ON q.ticket_id = mr.queue_ticket_id
WHERE NOT EXISTS (SELECT 1 FROM vital_signs vs
                  WHERE vs.medical_record_id = mr.record_id AND vs.deleted = false);

INSERT INTO icd_10_selections
    (selection_id, record_id, code, code_name, note, created_at, updated_at, deleted)
SELECT pg_temp.did('historical-icd-' || mr.record_id::text), mr.record_id,
       CASE WHEN s.service_code LIKE 'EX-SU-%' THEN 'S09.9'
            WHEN s.service_code LIKE 'EX-DER-%' THEN 'L30.9' ELSE 'Z00.0' END,
       CASE WHEN s.service_code LIKE 'EX-SU-%' THEN 'Chấn thương phần mềm chưa xác định'
            WHEN s.service_code LIKE 'EX-DER-%' THEN 'Viêm da không đặc hiệu'
            ELSE 'Khám sức khỏe tổng quát' END,
       'Chẩn đoán của lượt khám lịch sử.', mr.completed_at, mr.completed_at, false
FROM medical_record mr
JOIN customer_visit v ON v.visit_id = mr.visit_id
JOIN invoice i ON i.visit_id = v.visit_id AND i.invoice_code ~ '^INV-DEMO-[0-9]{4}$'
JOIN queue_ticket q ON q.ticket_id = mr.queue_ticket_id
JOIN medical_service s ON s.service_id = q.service_id
WHERE NOT EXISTS (SELECT 1 FROM icd_10_selections x
                  WHERE x.record_id = mr.record_id AND x.deleted = false);

INSERT INTO prescription_item
    (prescription_item_id, record_id, medicine_name, quantity, unit, note,
     frequency_per_day, created_at, updated_at, deleted)
SELECT pg_temp.did('historical-prescription-' || mr.record_id::text), mr.record_id,
       CASE WHEN s.service_code LIKE 'EX-DER-%' THEN 'Cetirizine 10mg'
            ELSE 'Paracetamol 500mg' END,
       10, 'Viên', 'Uống sau ăn khi có triệu chứng, theo hướng dẫn của bác sĩ.',
       CASE WHEN s.service_code LIKE 'EX-DER-%' THEN 1 ELSE 2 END,
       mr.completed_at, mr.completed_at, false
FROM medical_record mr
JOIN customer_visit v ON v.visit_id = mr.visit_id
JOIN invoice i ON i.visit_id = v.visit_id AND i.invoice_code ~ '^INV-DEMO-[0-9]{4}$'
JOIN queue_ticket q ON q.ticket_id = mr.queue_ticket_id
JOIN medical_service s ON s.service_id = q.service_id
WHERE NOT EXISTS (SELECT 1 FROM prescription_item x
                  WHERE x.record_id = mr.record_id AND x.deleted = false);

-- Guarantee a complete daily baseline for every date in the latest 60 days.
-- Existing richer journeys remain in place, so the chart still has natural
-- high/low days while never showing an artificial zero caused by missing seed.
CREATE TEMP TABLE report_daily_baseline ON COMMIT DROP AS
WITH customer_pool AS (
    SELECT array_agg(p.profile_id ORDER BY p.phone) AS profile_ids
    FROM profile p
    JOIN account a ON a.account_id = p.account_id
    WHERE a.role = 'CUSTOMER' AND p.deleted = false
), days AS (
    SELECT offset_no,
           pg_temp.demo_date() - offset_no AS work_date
    FROM generate_series(0, 59) AS offset_no
)
SELECT d.offset_no,
       d.work_date,
       cp.profile_ids[1 + mod(d.offset_no, array_length(cp.profile_ids, 1))] AS customer_id,
       -- Rotate only services whose eligibility accepts the adult customer
       -- pool. Paediatrics/obstetrics keep their dedicated, suitable demos.
       CASE mod(d.offset_no, 5)
           WHEN 0 THEN 'INT-101'
           WHEN 1 THEN 'INT-102'
           WHEN 2 THEN 'SUR-201'
           WHEN 3 THEN 'SUR-202'
           ELSE 'DER-401'
       END AS room_code,
       CASE mod(d.offset_no, 5)
           WHEN 0 THEN 'EX-IN-001'
           WHEN 1 THEN 'EX-IN-002'
           WHEN 2 THEN 'EX-SU-001'
           WHEN 3 THEN 'EX-SU-002'
           ELSE 'EX-DER-001'
       END AS service_code
FROM days d
CROSS JOIN customer_pool cp;

-- Each reporting visit is a real booked workflow. This keeps the appointment,
-- reception and patient-history screens consistent with the report aggregates.
INSERT INTO appointment
    (appointment_id, created_at, updated_at, deleted, scheduled_at, status,
     is_guest, customer_id, shift_name, shift_time, shift_version_id)
SELECT pg_temp.did('report-daily-appointment-' || b.work_date::text),
       b.work_date + time '08:30', b.work_date + time '11:05', false,
       b.work_date + time '09:30', 'CHECKED_IN', false, b.customer_id,
       'Ca Sáng', '07:30-11:30', '71000001-1111-1111-1111-111111111111'
FROM report_daily_baseline b;

INSERT INTO appointment_services (appointment_id, service_id)
SELECT pg_temp.did('report-daily-appointment-' || b.work_date::text), s.service_id
FROM report_daily_baseline b
JOIN medical_service s ON s.service_code = b.service_code;

INSERT INTO customer_visit
    (visit_id, customer_id, appointment_id, status, check_in_time, check_out_time,
     checked_in_by, created_at, updated_at, deleted)
SELECT pg_temp.did('report-daily-visit-' || b.work_date::text), b.customer_id,
       pg_temp.did('report-daily-appointment-' || b.work_date::text), 'COMPLETED',
       b.work_date + time '10:00', b.work_date + time '11:00',
       '50ec3aa8-ede0-2354-2df1-5a50e7a19729',
       b.work_date + time '09:55', b.work_date + time '11:05', false
FROM report_daily_baseline b;

INSERT INTO queue_ticket
    (ticket_id, created_at, updated_at, deleted, visit_id, department_id,
     work_date, queue_number, status, called_at, completed_at, service_id)
SELECT pg_temp.did('report-daily-ticket-' || b.work_date::text),
       b.work_date + time '10:00', b.work_date + time '10:55', false,
       pg_temp.did('report-daily-visit-' || b.work_date::text), d.department_id,
       b.work_date, 90, 'DONE', b.work_date + time '10:05', b.work_date + time '10:55', s.service_id
FROM report_daily_baseline b
JOIN department d ON d.room_code = b.room_code
JOIN medical_service s ON s.service_code = b.service_code;

INSERT INTO medical_record
    (record_id, record_code, visit_id, queue_ticket_id, doctor_id,
     chief_complaint, clinical_findings, diagnosis, prescription_note,
     conclusion, patient_instruction, specialty_data, status, completed_at,
     rating_score, rated_at, follow_up_note, follow_up_date,
     created_at, updated_at, deleted)
SELECT pg_temp.did('report-daily-record-' || b.work_date::text),
       'MR-REPORT-' || to_char(b.work_date, 'YYYYMMDD'),
       pg_temp.did('report-daily-visit-' || b.work_date::text),
       pg_temp.did('report-daily-ticket-' || b.work_date::text),
       d.head_doctor_id,
       CASE
           WHEN b.service_code LIKE 'EX-SU-%' THEN 'Đau và sưng nhẹ vùng phần mềm sau vận động.'
           WHEN b.service_code LIKE 'EX-PE-%' THEN 'Ho, nghẹt mũi và sốt nhẹ trong hai ngày.'
           WHEN b.service_code LIKE 'EX-DER-%' THEN 'Ngứa và nổi ban đỏ khu trú.'
           ELSE 'Mệt mỏi nhẹ, cần kiểm tra sức khỏe tổng quát.'
       END,
       CASE
           WHEN b.service_code LIKE 'EX-SU-%' THEN 'Sưng nhẹ, vận động còn tốt, chưa ghi nhận dấu hiệu gãy xương.'
           WHEN b.service_code LIKE 'EX-PE-%' THEN 'Trẻ tỉnh, họng đỏ nhẹ, phổi thông khí đều.'
           WHEN b.service_code LIKE 'EX-DER-%' THEN 'Mảng đỏ khu trú, không rỉ dịch, chưa có dấu hiệu nhiễm trùng.'
           ELSE 'Bệnh nhân tỉnh, tiếp xúc tốt, tim đều, phổi thông khí rõ.'
       END,
       CASE
           WHEN b.service_code LIKE 'EX-SU-%' THEN 'Chấn thương phần mềm mức độ nhẹ.'
           WHEN b.service_code LIKE 'EX-PE-%' THEN 'Viêm đường hô hấp trên cấp.'
           WHEN b.service_code LIKE 'EX-DER-%' THEN 'Viêm da không đặc hiệu.'
           ELSE 'Khám sức khỏe định kỳ, chưa ghi nhận bất thường cấp tính.'
       END,
       'Dùng thuốc đúng hướng dẫn; không tự ý tăng liều.',
       'Tình trạng ổn định, điều trị và theo dõi ngoại trú.',
       'Nghỉ ngơi, uống đủ nước và tái khám sớm khi triệu chứng tăng.',
       jsonb_build_object(
           'generalCondition', 'Tỉnh táo, tiếp xúc tốt',
           'serviceCode', b.service_code,
           'demoWorkflow', true
       ),
       'COMPLETED', b.work_date + time '10:55',
       4 + mod(b.offset_no, 2), b.work_date + time '13:00',
       'Tái khám nếu triệu chứng chưa cải thiện.', b.work_date + 14,
       b.work_date + time '10:00', b.work_date + time '10:55', false
FROM report_daily_baseline b
JOIN department d ON d.room_code = b.room_code;

INSERT INTO vital_signs
    (vital_id, medical_record_id, blood_pressure, heart_rate, temperature,
     weight, height, recorded_at, recorded_by, created_at, updated_at, deleted)
SELECT pg_temp.did('report-daily-vital-' || b.work_date::text),
       pg_temp.did('report-daily-record-' || b.work_date::text),
       (118 + mod(b.offset_no, 7))::text || '/' || (74 + mod(b.offset_no, 6))::text,
       70 + mod(b.offset_no, 15), 36.5 + (mod(b.offset_no, 4) * 0.1),
       50.0 + mod(b.offset_no, 21), 155.0 + mod(b.offset_no, 20),
       b.work_date + time '10:05',
       COALESCE(
           (SELECT si.staff_id
            FROM staff_info si
            WHERE si.department_id = d.department_id
              AND si.system_role = 'NURSE' AND si.deleted = false
            ORDER BY si.staff_id LIMIT 1),
           'd8dccd88-7bbc-a7b7-a014-572106a75b15'::uuid
       ),
       b.work_date + time '10:05', b.work_date + time '10:05', false
FROM report_daily_baseline b
JOIN department d ON d.room_code = b.room_code;

INSERT INTO icd_10_selections
    (selection_id, record_id, code, code_name, note, created_at, updated_at, deleted)
SELECT pg_temp.did('report-daily-icd-' || b.work_date::text),
       pg_temp.did('report-daily-record-' || b.work_date::text),
       CASE
           WHEN b.service_code LIKE 'EX-SU-%' THEN 'S09.9'
           WHEN b.service_code LIKE 'EX-PE-%' THEN 'J06.9'
           WHEN b.service_code LIKE 'EX-DER-%' THEN 'L30.9'
           ELSE 'Z00.0'
       END,
       CASE
           WHEN b.service_code LIKE 'EX-SU-%' THEN 'Chấn thương phần mềm chưa xác định'
           WHEN b.service_code LIKE 'EX-PE-%' THEN 'Nhiễm khuẩn hô hấp trên cấp'
           WHEN b.service_code LIKE 'EX-DER-%' THEN 'Viêm da không đặc hiệu'
           ELSE 'Khám sức khỏe tổng quát'
       END,
       'Chẩn đoán phục vụ hồ sơ khám hoàn chỉnh.',
       b.work_date + time '10:45', b.work_date + time '10:45', false
FROM report_daily_baseline b;

INSERT INTO prescription_item
    (prescription_item_id, record_id, medicine_name, quantity, unit, note,
     frequency_per_day, created_at, updated_at, deleted)
SELECT pg_temp.did('report-daily-prescription-' || b.work_date::text),
       pg_temp.did('report-daily-record-' || b.work_date::text),
       CASE
           WHEN b.service_code LIKE 'EX-DER-%' THEN 'Cetirizine 10mg'
           WHEN b.service_code LIKE 'EX-PE-%' THEN 'Paracetamol 250mg'
           ELSE 'Paracetamol 500mg'
       END,
       CASE WHEN b.service_code LIKE 'EX-PE-%' THEN 6 ELSE 10 END,
       'Viên', 'Uống sau ăn khi có triệu chứng, tuân thủ hướng dẫn của bác sĩ.',
       CASE WHEN b.service_code LIKE 'EX-DER-%' THEN 1 ELSE 2 END,
       b.work_date + time '10:50', b.work_date + time '10:50', false
FROM report_daily_baseline b;

INSERT INTO invoice
    (invoice_id, created_at, deleted, updated_at, discount, due_date, invoice_code,
     issue_date, note, paid_amount, status, subtotal, tax, total_amount,
     customer_id, issued_by, medical_record_id, visit_id)
SELECT pg_temp.did('report-daily-invoice-' || b.work_date::text), b.work_date + time '10:10', false,
       b.work_date + time '10:20', 0, NULL, 'INV-REPORT-' || to_char(b.work_date, 'YYYYMMDD'),
       b.work_date, 'Dữ liệu báo cáo vận hành theo ngày', s.price, 'PAID', s.price, 0, s.price,
       b.customer_id, 'b693d136-402d-7de2-4835-117a5e2c5411',
       pg_temp.did('report-daily-record-' || b.work_date::text),
       pg_temp.did('report-daily-visit-' || b.work_date::text)
FROM report_daily_baseline b
JOIN medical_service s ON s.service_code = b.service_code;

INSERT INTO invoice_item
    (item_id, created_at, updated_at, deleted, invoice_id, service_id,
     service_snapshot, service_code_snapshot, unit_price, quantity,
     discount_percent, discount_amount, final_price, line_total, note, bhyt_fund)
SELECT pg_temp.did('report-daily-item-' || b.work_date::text), b.work_date + time '10:10',
       b.work_date + time '10:10', false,
       pg_temp.did('report-daily-invoice-' || b.work_date::text), s.service_id,
       s.name, s.service_code, s.price, 1, 0, 0, s.price, s.price,
       'Dịch vụ khám dùng cho báo cáo theo ngày', 0
FROM report_daily_baseline b
JOIN medical_service s ON s.service_code = b.service_code;

INSERT INTO payment_transaction
    (transaction_id, invoice_id, transaction_code, amount, payment_method, status,
     paid_at, gateway_reference, note, received_by, created_at, updated_at, deleted)
SELECT pg_temp.did('report-daily-payment-' || b.work_date::text),
       pg_temp.did('report-daily-invoice-' || b.work_date::text),
       'PAY-REPORT-' || to_char(b.work_date, 'YYYYMMDD'), s.price,
       CASE mod(b.offset_no, 3) WHEN 0 THEN 'CASH' WHEN 1 THEN 'CARD' ELSE 'BANK_TRANSFER' END,
       'SUCCESS', b.work_date + time '10:20', NULL, 'Thanh toán dữ liệu báo cáo theo ngày',
       'b693d136-402d-7de2-4835-117a5e2c5411',
       b.work_date + time '10:20', b.work_date + time '10:20', false
FROM report_daily_baseline b
JOIN medical_service s ON s.service_code = b.service_code;

-- Two cancelled visits make the cancellation indicator realistic without
-- fabricating invoices or medical records for workflows that never completed.
INSERT INTO customer_visit
    (visit_id, customer_id, status, check_in_time, check_out_time,
     created_at, updated_at, deleted)
VALUES
(pg_temp.did('report-cancelled-44-days'), '80221d38-65d4-f424-966b-52340f8125fc', 'CANCELLED',
 pg_temp.demo_date()-interval '44 days'+time '09:00', pg_temp.demo_date()-interval '44 days'+time '09:20',
 pg_temp.demo_date()-interval '44 days'+time '08:55', pg_temp.demo_date()-interval '44 days'+time '09:20', false),
(pg_temp.did('report-cancelled-12-days'), 'd1841dfc-3567-7c11-fcec-a1a9dac19321', 'CANCELLED',
 pg_temp.demo_date()-interval '12 days'+time '14:00', pg_temp.demo_date()-interval '12 days'+time '14:15',
 pg_temp.demo_date()-interval '12 days'+time '13:55', pg_temp.demo_date()-interval '12 days'+time '14:15', false);

-- Fail fast when a future edit leaves the board-demo catalogue incomplete.
DO $verify_catalog$
BEGIN
    IF (SELECT count(*) FROM medical_service
        WHERE status = 'ACTIVE' AND deleted = false
          AND service_code ~ '^(EX|LAB|IMG)-') <> 32 THEN
        RAISE EXCEPTION 'CareS demo seed must contain exactly 32 active primary services';
    END IF;
    IF (SELECT count(*) FROM medical_service
        WHERE status = 'ACTIVE' AND deleted = false AND service_code LIKE 'EX-%') <> 20 THEN
        RAISE EXCEPTION 'CareS demo seed must contain exactly 20 examination services';
    END IF;
    IF (SELECT count(*) FROM medical_service
        WHERE status = 'ACTIVE' AND deleted = false AND service_code LIKE 'LAB-%') <> 7 THEN
        RAISE EXCEPTION 'CareS demo seed must contain exactly 7 laboratory services';
    END IF;
    IF (SELECT count(*) FROM medical_service
        WHERE status = 'ACTIVE' AND deleted = false AND service_code LIKE 'IMG-%') <> 5 THEN
        RAISE EXCEPTION 'CareS demo seed must contain exactly 5 imaging services';
    END IF;
    IF (SELECT count(*) FROM medical_service
        WHERE status = 'ACTIVE' AND deleted = false
          AND service_code ~ '^(LAB|IMG)-'
          AND allow_customer_booking = true) <> 12 THEN
        RAISE EXCEPTION 'All 12 primary paraclinical services must be available for customer booking';
    END IF;
    IF EXISTS (SELECT 1 FROM medical_service
        WHERE status = 'ACTIVE' AND deleted = false
          AND service_code LIKE 'AN-%'
          AND allow_customer_booking = true) THEN
        RAISE EXCEPTION 'Internal laboratory analytes must not be directly bookable by customers';
    END IF;
    IF (SELECT count(*)
        FROM medical_service s
        JOIN medical_service_form_template b ON b.service_id = s.service_id AND b.deleted = false
        WHERE s.service_code ~ '^(LAB|IMG)-' AND s.deleted = false) <> 12 THEN
        RAISE EXCEPTION 'Every laboratory and imaging service must have a result form';
    END IF;
    IF (SELECT count(*) FROM appointment_services
        WHERE appointment_id = '1c555a61-e713-eecb-3a22-9cda3f8f7f9a') <> 2 THEN
        RAISE EXCEPTION 'Today appointment must include EX-IN-001 and LAB-001';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM queue_ticket q
        WHERE q.work_date = pg_temp.demo_date() AND q.status = 'WAITING'
          AND q.department_id = '4d7ac047-a699-7a37-b2c0-e8392593e7bc'
          AND q.service_id = '40000002-0000-0000-0000-000000000002'
    ) THEN
        RAISE EXCEPTION 'Internal Medicine must have one waiting demo patient today';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM test_request tr
        JOIN queue_ticket q ON q.ticket_id = tr.queue_ticket_id
        WHERE q.work_date = pg_temp.demo_date() AND q.status = 'WAITING'
          AND tr.status = 'PENDING' AND tr.completed_at IS NULL
          AND tr.service_id = '40000008-0000-0000-0000-000000000008'
          AND tr.invoice_item_id = 'e5000000-0000-0000-0000-000000000001'
          AND NOT EXISTS (
              SELECT 1 FROM test_result result
              WHERE result.test_request_id = tr.test_request_id AND result.deleted = false
          )
    ) THEN
        RAISE EXCEPTION 'Laboratory must have one paid full-CBC patient waiting without recorded results today';
    END IF;
    IF EXISTS (
        SELECT 1
        FROM staff_info s
        WHERE s.system_role IN ('RECEPTIONIST', 'CASHIER')
          AND (SELECT count(*) FROM staff_schedule ss WHERE ss.staff_id = s.staff_id AND ss.deleted = false)
              <> 45 * (SELECT count(*) FROM shift_version sv WHERE sv.deleted = false)
    ) THEN
        RAISE EXCEPTION 'Every receptionist and cashier must have a full three-shift demo schedule';
    END IF;
    IF EXISTS (
        SELECT 1 FROM invoice i
        WHERE i.invoice_code LIKE 'INV-DEMO-%'
          AND i.total_amount <> COALESCE((SELECT sum(ii.line_total) FROM invoice_item ii
                                          WHERE ii.invoice_id = i.invoice_id AND ii.deleted = false), 0)
    ) THEN
        RAISE EXCEPTION 'Demo invoice total does not match its invoice items';
    END IF;
    IF EXISTS (
        SELECT 1 FROM test_request tr
        WHERE (tr.status = 'COMPLETED' AND (tr.completed_at IS NULL OR NOT EXISTS (
                   SELECT 1 FROM test_result result
                   WHERE result.test_request_id = tr.test_request_id AND result.deleted = false
              )))
           OR (tr.status <> 'COMPLETED' AND tr.completed_at IS NOT NULL)
    ) THEN
        RAISE EXCEPTION 'Test-request status, completion time and result are inconsistent';
    END IF;
    IF EXISTS (
        SELECT 1 FROM queue_ticket q
        WHERE (q.status IN ('WAITING', 'CALLED', 'IN_PROGRESS', 'BLOCKED', 'WAITING_FOR_TEST', 'TEST_DONE')
                   AND q.completed_at IS NOT NULL)
           OR (q.status = 'DONE' AND q.completed_at IS NULL)
    ) THEN
        RAISE EXCEPTION 'Queue-ticket status and completion time are inconsistent';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM notification WHERE status = 'READ' AND deleted = false)
       OR NOT EXISTS (SELECT 1 FROM notification WHERE status IN ('SENT', 'PENDING') AND deleted = false) THEN
        RAISE EXCEPTION 'Demo notifications must include both read and unread items';
    END IF;
    IF EXISTS (
        SELECT 1 FROM notification n
        WHERE (n.status = 'READ' AND (n.sent_at IS NULL OR n.read_at IS NULL OR n.read_at < n.sent_at))
           OR (n.status = 'SENT' AND n.sent_at IS NULL)
           OR (n.status = 'PENDING' AND (n.sent_at IS NOT NULL OR n.read_at IS NOT NULL))
           OR (n.related_entity = 'Appointment' AND NOT EXISTS
               (SELECT 1 FROM appointment a WHERE a.appointment_id = n.related_entity_id))
           OR (n.related_entity = 'TestRequest' AND NOT EXISTS
               (SELECT 1 FROM test_request tr WHERE tr.test_request_id = n.related_entity_id))
           OR (n.related_entity = 'Invoice' AND NOT EXISTS
               (SELECT 1 FROM invoice i WHERE i.invoice_id = n.related_entity_id))
           OR (n.related_entity = 'ChatSession' AND NOT EXISTS
               (SELECT 1 FROM chat_sessions cs WHERE cs.session_id = n.related_entity_id))
    ) THEN
        RAISE EXCEPTION 'Demo notification status, timestamps or related entity is inconsistent';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM chat_sessions WHERE status = 'CLOSED' AND deleted = false)
       OR NOT EXISTS (SELECT 1 FROM chat_sessions WHERE status = 'IN_PROGRESS' AND deleted = false)
       OR NOT EXISTS (SELECT 1 FROM chat_sessions WHERE status = 'WAITING_FOR_AGENT' AND deleted = false) THEN
        RAISE EXCEPTION 'Demo chat must include CLOSED, IN_PROGRESS and WAITING_FOR_AGENT sessions';
    END IF;
    IF EXISTS (
        SELECT 1 FROM chat_messages m
        JOIN chat_sessions s ON s.session_id = m.session_id
        WHERE (m.sender_type = 'CUSTOMER' AND m.sender_id IS DISTINCT FROM s.customer_id)
           OR (m.sender_type = 'BOT' AND m.sender_id IS NOT NULL)
           OR (m.sender_type = 'RECEPTIONIST' AND NOT EXISTS (
               SELECT 1 FROM staff_info si
               WHERE si.staff_id = m.sender_id AND si.system_role = 'RECEPTIONIST'
           ))
           OR m.created_at < s.created_at
    ) THEN
        RAISE EXCEPTION 'Demo chat message sender or timeline is inconsistent with its session';
    END IF;
    IF EXISTS (
        SELECT 1 FROM chat_sessions s
        WHERE (s.status IN ('IN_PROGRESS', 'CLOSED') AND NOT EXISTS (
                   SELECT 1 FROM staff_info si
                   WHERE si.staff_id = s.assigned_receptionist_id
                     AND si.system_role = 'RECEPTIONIST'
              ))
           OR (s.status = 'WAITING_FOR_AGENT' AND s.assigned_receptionist_id IS NOT NULL)
           OR s.updated_at < COALESCE((
               SELECT max(m.created_at) FROM chat_messages m WHERE m.session_id = s.session_id
           ), s.created_at)
    ) THEN
        RAISE EXCEPTION 'Demo chat assignment, status or last-update time is inconsistent';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM public_announcement
        WHERE published = true AND deleted = false
          AND starts_at <= pg_temp.demo_now() AND (ends_at IS NULL OR ends_at > pg_temp.demo_now())
    ) THEN
        RAISE EXCEPTION 'Demo data must include one currently active public announcement';
    END IF;
    IF EXISTS (
        SELECT 1 FROM payment_transaction t
        LEFT JOIN invoice i ON i.invoice_id = t.invoice_id
        WHERE i.invoice_id IS NULL
    ) THEN
        RAISE EXCEPTION 'A demo payment transaction references a missing invoice';
    END IF;
    IF EXISTS (
        SELECT 1 FROM invoice i
        WHERE i.status = 'PAID' AND i.deleted = false
          AND i.paid_amount <> COALESCE((
              SELECT sum(t.amount) FROM payment_transaction t
              WHERE t.invoice_id = i.invoice_id AND t.status = 'SUCCESS' AND t.deleted = false
          ), 0)
    ) THEN
        RAISE EXCEPTION 'Paid invoice amount does not match its successful transactions';
    END IF;
    IF EXISTS (
        SELECT 1 FROM membership_card c
        WHERE c.deleted = false
          AND c.balance <> COALESCE((
              SELECT l.balance_after FROM membership_card_ledger l
              WHERE l.card_id = c.card_id AND l.deleted = false
              ORDER BY l.created_at DESC, l.ledger_id DESC LIMIT 1
          ), 0)
    ) THEN
        RAISE EXCEPTION 'CareS card balance does not match its latest ledger entry';
    END IF;
    IF EXISTS (
        SELECT 1 FROM membership_card_ledger l
        LEFT JOIN membership_card c ON c.card_id = l.card_id
        LEFT JOIN invoice i ON i.invoice_id = l.invoice_id
        LEFT JOIN payment_transaction pt ON pt.transaction_id = l.payment_transaction_id
        WHERE c.card_id IS NULL
           OR (l.invoice_id IS NOT NULL AND i.invoice_id IS NULL)
           OR (l.payment_transaction_id IS NOT NULL AND pt.transaction_id IS NULL)
           OR (l.type = 'PAYMENT' AND (l.balance_after <> l.balance_before - l.amount
                                      OR l.amount <> pt.amount))
    ) THEN
        RAISE EXCEPTION 'CareS card ledger references or running balance are inconsistent';
    END IF;
    IF EXISTS (
        SELECT 1 FROM family_member f
        LEFT JOIN profile owner ON owner.profile_id = f.owner_profile_id
        LEFT JOIN profile member ON member.profile_id = f.member_profile_id
        WHERE owner.profile_id IS NULL OR member.profile_id IS NULL
           OR f.owner_profile_id = f.member_profile_id
    ) THEN
        RAISE EXCEPTION 'Demo family-member relationship is invalid';
    END IF;
    IF EXISTS (
        SELECT 1 FROM audit_log l
        LEFT JOIN account a ON a.account_id = l.actor_account_id
        WHERE l.actor_account_id IS NOT NULL AND a.account_id IS NULL
    ) THEN
        RAISE EXCEPTION 'Demo audit log references a missing actor account';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM vital_signs WHERE medical_record_id = 'd90b3fc2-2b39-d940-d65f-7e6ed2262008' AND deleted = false)
       OR NOT EXISTS (SELECT 1 FROM icd_10_selections WHERE record_id = 'd90b3fc2-2b39-d940-d65f-7e6ed2262008' AND deleted = false)
       OR NOT EXISTS (SELECT 1 FROM prescription_item WHERE record_id = 'd90b3fc2-2b39-d940-d65f-7e6ed2262008' AND deleted = false) THEN
        RAISE EXCEPTION 'Historical medical record must include vital signs, ICD-10 and prescription data';
    END IF;
    IF (SELECT count(*) FROM customer_visit
        WHERE status = 'COMPLETED' AND deleted = false
          AND check_out_time::date BETWEEN pg_temp.demo_date()-59 AND pg_temp.demo_date()) < 25 THEN
        RAISE EXCEPTION 'The rolling 60-day report must include at least 25 completed visits';
    END IF;
    IF (SELECT count(*) FROM medical_record mr
        WHERE mr.status = 'COMPLETED' AND mr.deleted = false
          AND mr.queue_ticket_id IS NOT NULL AND mr.completed_at IS NOT NULL
          AND mr.completed_at::date BETWEEN pg_temp.demo_date()-59 AND pg_temp.demo_date()) < 25 THEN
        RAISE EXCEPTION 'The rolling 60-day report must include at least 25 completed examination records';
    END IF;
    IF (SELECT count(DISTINCT pt.paid_at::date) FROM payment_transaction pt
        WHERE pt.status = 'SUCCESS' AND pt.deleted = false
          AND pt.paid_at::date BETWEEN pg_temp.demo_date()-59 AND pg_temp.demo_date()) <> 60 THEN
        RAISE EXCEPTION 'Every day in the rolling 60-day report must have a successful payment';
    END IF;
    IF EXISTS (
        SELECT report_date
        FROM generate_series(pg_temp.demo_date()-59, pg_temp.demo_date(), interval '1 day') report_date
        WHERE NOT EXISTS (
            SELECT 1 FROM customer_visit v
            WHERE v.status = 'COMPLETED' AND v.deleted = false
              AND v.check_out_time::date = report_date::date
        )
    ) THEN
        RAISE EXCEPTION 'Every day in the rolling 60-day report must have a completed visit';
    END IF;
    IF (SELECT count(*)
        FROM customer_visit v
        JOIN appointment a ON a.appointment_id = v.appointment_id
        JOIN medical_record mr ON mr.visit_id = v.visit_id
        JOIN queue_ticket q ON q.ticket_id = mr.queue_ticket_id AND q.visit_id = v.visit_id
        JOIN appointment_services aps
          ON aps.appointment_id = a.appointment_id AND aps.service_id = q.service_id
        JOIN invoice i ON i.visit_id = v.visit_id AND i.medical_record_id = mr.record_id
        JOIN invoice_item ii ON ii.invoice_id = i.invoice_id AND ii.service_id = q.service_id
        JOIN payment_transaction pt ON pt.invoice_id = i.invoice_id AND pt.status = 'SUCCESS'
        WHERE v.visit_id = pg_temp.did('report-daily-visit-' || v.check_in_time::date::text)
          AND v.check_in_time::date BETWEEN pg_temp.demo_date()-59 AND pg_temp.demo_date()) <> 60 THEN
        RAISE EXCEPTION 'All 60 daily report visits must be complete end-to-end workflows';
    END IF;
    IF (SELECT count(*)
        FROM medical_record mr
        JOIN vital_signs vs ON vs.medical_record_id = mr.record_id AND vs.deleted = false
        JOIN icd_10_selections icd ON icd.record_id = mr.record_id AND icd.deleted = false
        JOIN prescription_item pi ON pi.record_id = mr.record_id AND pi.deleted = false
        WHERE mr.record_id = pg_temp.did('report-daily-record-' || mr.completed_at::date::text)
          AND mr.completed_at::date BETWEEN pg_temp.demo_date()-59 AND pg_temp.demo_date()) <> 60 THEN
        RAISE EXCEPTION 'All 60 daily report records must include vitals, ICD-10 and prescription data';
    END IF;
    IF (SELECT count(DISTINCT v.visit_id)
        FROM customer_visit v
        JOIN appointment a ON a.appointment_id = v.appointment_id
        JOIN queue_ticket q ON q.visit_id = v.visit_id
        JOIN appointment_services aps
          ON aps.appointment_id = a.appointment_id AND aps.service_id = q.service_id
        JOIN medical_record mr ON mr.visit_id = v.visit_id
        JOIN vital_signs vs ON vs.medical_record_id = mr.record_id AND vs.deleted = false
        JOIN icd_10_selections icd ON icd.record_id = mr.record_id AND icd.deleted = false
        JOIN prescription_item pi ON pi.record_id = mr.record_id AND pi.deleted = false
        JOIN invoice i ON i.visit_id = v.visit_id AND i.medical_record_id = mr.record_id
        JOIN invoice_item ii ON ii.invoice_id = i.invoice_id
        JOIN payment_transaction pt ON pt.invoice_id = i.invoice_id AND pt.status = 'SUCCESS'
        WHERE i.invoice_code ~ '^INV-DEMO-[0-9]{4}$'
          AND v.status = 'COMPLETED' AND mr.status = 'COMPLETED') <> 25 THEN
        RAISE EXCEPTION 'All 25 historical visits must be complete end-to-end workflows';
    END IF;
    IF (SELECT count(DISTINCT q.department_id)
        FROM medical_record mr
        JOIN queue_ticket q ON q.ticket_id = mr.queue_ticket_id
        JOIN department d ON d.department_id = q.department_id
        WHERE mr.status = 'COMPLETED' AND mr.deleted = false
          AND d.department_type = 'EXAMINATION'
          AND mr.completed_at::date BETWEEN pg_temp.demo_date()-59 AND pg_temp.demo_date()) < 5 THEN
        RAISE EXCEPTION 'All five adult general examination rooms must have completed report activity';
    END IF;
    IF (SELECT count(*) FROM customer_visit
        WHERE status = 'CANCELLED' AND deleted = false
          AND check_out_time::date BETWEEN pg_temp.demo_date()-59 AND pg_temp.demo_date()) < 2 THEN
        RAISE EXCEPTION 'The rolling 60-day report must include cancelled visits';
    END IF;
END
$verify_catalog$;
COMMIT;
