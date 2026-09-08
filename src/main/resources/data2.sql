-- CareS board-demo seed.
-- Run with psql from this directory after setting cares.demo2_reset=yes.
-- The canonical catalog is loaded from data.sql first so room/service/form data
-- cannot drift between the normal demo and the board-demo environment.
\set ON_ERROR_STOP on

DO $guard$
BEGIN
    IF current_setting('cares.demo2_reset', true) IS DISTINCT FROM 'yes' THEN
        RAISE EXCEPTION 'Reset bị chặn. Hãy SET cares.demo2_reset = ''yes'' trong cùng session.';
    END IF;
END
$guard$;

-- data.sql owns the canonical service, room, capability and clinical-form
-- catalog. It also creates the baseline journeys that data2 normalizes below.
SET cares.demo_reset = 'yes';
DO $clock_forward$
BEGIN
    IF NULLIF(current_setting('cares.demo2_now', true), '') IS NOT NULL THEN
        PERFORM set_config('cares.demo_now', current_setting('cares.demo2_now', true), false);
    END IF;
END
$clock_forward$;
\ir data.sql

BEGIN;
SET LOCAL TIME ZONE 'Asia/Ho_Chi_Minh';

CREATE TEMP TABLE demo2_clock ON COMMIT DROP AS
SELECT COALESCE(NULLIF(current_setting('cares.demo2_now', true), '')::timestamp,
                CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh') AS moment;
CREATE OR REPLACE FUNCTION pg_temp.d2_now() RETURNS timestamp LANGUAGE sql STABLE
AS 'SELECT moment FROM demo2_clock';
CREATE OR REPLACE FUNCTION pg_temp.d2_date() RETURNS date LANGUAGE sql STABLE
AS 'SELECT moment::date FROM demo2_clock';
CREATE OR REPLACE FUNCTION pg_temp.d2id(key text) RETURNS uuid LANGUAGE sql IMMUTABLE
AS 'SELECT md5(''CareS-board-demo-v1:'' || key)::uuid';
CREATE OR REPLACE FUNCTION pg_temp.oldid(key text) RETURNS uuid LANGUAGE sql IMMUTABLE
AS 'SELECT md5(''CareS-demo-v2:'' || key)::uuid';
CREATE OR REPLACE FUNCTION pg_temp.d2_is_open(moment timestamp) RETURNS boolean LANGUAGE sql IMMUTABLE
AS $$
SELECT EXTRACT(ISODOW FROM moment)::int BETWEEN 1 AND 6
   AND (moment::time >= time '07:30' AND moment::time < time '11:30'
        OR moment::time >= time '13:30' AND moment::time < time '17:30')
$$;

-- ============================================================================
-- Accounts: natural demo identities and one memorable password for the board DB
-- ============================================================================
UPDATE account
SET password_hash = '$2b$10$EMO3vTRt8EXwPPtNzm5gmuiWMy2m2WsR1sNZk7Tk9gU9rcROeWaAi';

UPDATE profile
SET phone = '090910' || lpad(regexp_replace(patient_code,'[^0-9]','','g'),4,'0'),
    email = 'patient.' || lpad(regexp_replace(patient_code,'[^0-9]','','g'),3,'0') || '@example.com',
    updated_at = pg_temp.d2_now()
WHERE patient_code LIKE 'BN-DEMO-%';

UPDATE account a
SET username = p.phone
FROM profile p
WHERE p.account_id = a.account_id AND a.role = 'CUSTOMER';

-- Never derive profile UUIDs from a display sequence. data.sql intentionally
-- keeps one legacy-compatible UUID, so every generated relation resolves the
-- actual persisted customer profile through this lookup table.
CREATE TEMP TABLE demo2_customers ON COMMIT DROP AS
SELECT row_number() OVER (ORDER BY p.patient_code)::int customer_no,
       p.profile_id AS patient_id
FROM profile p
JOIN account a ON a.account_id = p.account_id
WHERE a.role = 'CUSTOMER' AND p.patient_code LIKE 'BN-DEMO-%';

CREATE TEMP TABLE demo2_doctor_accounts(staff_id uuid PRIMARY KEY, username text, room_code text) ON COMMIT DROP;
INSERT INTO demo2_doctor_accounts VALUES
('91000000-0000-0000-0000-000000000001','doctor.int1','INT-103'),
('91000000-0000-0000-0000-000000000005','doctor.int2','INT-104'),
('90000010-3333-3333-3333-333333333333','doctor.sur1','SUR-101'),
('91000000-0000-0000-0000-000000000004','doctor.sur2','SUR-102'),
('91000000-0000-0000-0000-000000000002','doctor.ped','PED-102'),
('91000000-0000-0000-0000-000000000003','doctor.der','DER-104'),
('91000000-0000-0000-0000-000000000006','doctor.obg','OBG-107'),
('93000000-0000-0000-0000-000000000002','doctor.lab201','LAB-201'),
('93000000-0000-0000-0000-000000000003','doctor.lab202','LAB-202'),
('91000000-0000-0000-0000-000000000007','doctor.img301','IMG-301'),
('93000000-0000-0000-0000-000000000004','doctor.xr302','XR-302');

UPDATE account a
SET username = mapping.username
FROM profile p
JOIN staff_info s ON s.profile_id = p.profile_id
JOIN demo2_doctor_accounts mapping ON mapping.staff_id = s.staff_id
WHERE a.account_id = p.account_id;

CREATE TEMP TABLE demo2_operational_accounts(staff_id uuid PRIMARY KEY, username text) ON COMMIT DROP;
INSERT INTO demo2_operational_accounts VALUES
('90000012-5555-5555-5555-555555555555','receptionist.am'),
('91000000-0000-0000-0000-000000000011','receptionist.pm'),
('91000000-0000-0000-0000-000000000012','receptionist.backup'),
('90000013-6666-6666-6666-666666666666','cashier.am'),
('91000000-0000-0000-0000-000000000013','cashier.pm'),
('91000000-0000-0000-0000-000000000014','cashier.backup');
UPDATE account a
SET username = mapping.username
FROM profile p
JOIN staff_info s ON s.profile_id = p.profile_id
JOIN demo2_operational_accounts mapping ON mapping.staff_id = s.staff_id
WHERE a.account_id = p.account_id;

UPDATE account SET username='admin'
WHERE account_id='30000012-2222-2222-2222-222222222222';
UPDATE account SET username='clinicmanager'
WHERE account_id='30000013-3333-3333-3333-333333333333';

-- ============================================================================
-- Clinic schedule: two administrative shifts, Monday-Saturday, Sunday closed
-- ============================================================================
DELETE FROM staff_schedule;
DELETE FROM staff_schedule_template;
DELETE FROM clinic_schedule_exception;

UPDATE shift_config SET name='Ca Sáng', start_time='07:30', end_time='11:30', is_active=true,
 updated_at=pg_temp.d2_now() WHERE shift_id='70000001-1111-1111-1111-111111111111';
UPDATE shift_config SET name='Ca Chiều', start_time='13:30', end_time='17:30', is_active=true,
 updated_at=pg_temp.d2_now() WHERE shift_id='70000002-2222-2222-2222-222222222222';
UPDATE shift_config SET name='Ca Tối', start_time='18:00', end_time='21:30', is_active=true,
 updated_at=pg_temp.d2_now() WHERE shift_id='70000003-3333-3333-3333-333333333333';

UPDATE shift_version SET start_time='07:30', end_time='11:30', updated_at=pg_temp.d2_now()
WHERE shift_id='70000001-1111-1111-1111-111111111111';
UPDATE shift_version SET start_time='13:30', end_time='17:30', updated_at=pg_temp.d2_now()
WHERE shift_id='70000002-2222-2222-2222-222222222222';
UPDATE shift_version SET start_time='18:00', end_time='21:30', updated_at=pg_temp.d2_now()
WHERE shift_id='70000003-3333-3333-3333-333333333333';

-- data.sql contains rolling historical fixtures. When one lands on Sunday,
-- move that complete historical chain to Saturday instead of leaving hidden
-- Sunday activity in reports. Business content and identifiers stay intact.
CREATE TEMP TABLE demo2_sunday_visits ON COMMIT DROP AS
SELECT visit_id FROM customer_visit
WHERE EXTRACT(ISODOW FROM check_in_time)=7 AND NOT deleted;

UPDATE appointment SET scheduled_at=scheduled_at-interval '1 day',updated_at=pg_temp.d2_now()
WHERE EXTRACT(ISODOW FROM scheduled_at)=7 AND NOT deleted;
UPDATE customer_visit SET check_in_time=check_in_time-interval '1 day',
 check_out_time=check_out_time-interval '1 day',created_at=created_at-interval '1 day',
 updated_at=updated_at-interval '1 day'
WHERE visit_id IN(SELECT visit_id FROM demo2_sunday_visits);
WITH moved AS (
 SELECT q.ticket_id,q.work_date-1 AS target_date,
   COALESCE((SELECT max(existing.queue_number) FROM queue_ticket existing
      WHERE existing.department_id=q.department_id AND existing.work_date=q.work_date-1),0)
   +row_number() OVER(PARTITION BY q.department_id,q.work_date ORDER BY q.queue_number,q.ticket_id) AS target_number
 FROM queue_ticket q WHERE q.visit_id IN(SELECT visit_id FROM demo2_sunday_visits)
)
UPDATE queue_ticket q SET work_date=m.target_date,queue_number=m.target_number,
 called_at=q.called_at-interval '1 day',completed_at=q.completed_at-interval '1 day',
 created_at=q.created_at-interval '1 day',updated_at=q.updated_at-interval '1 day'
FROM moved m WHERE m.ticket_id=q.ticket_id;
UPDATE medical_record SET completed_at=completed_at-interval '1 day',
 doctor_confirmed_at=doctor_confirmed_at-interval '1 day',rated_at=rated_at-interval '1 day',
 responded_at=responded_at-interval '1 day',created_at=created_at-interval '1 day',
 updated_at=updated_at-interval '1 day'
WHERE visit_id IN(SELECT visit_id FROM demo2_sunday_visits);
UPDATE vital_signs v SET recorded_at=recorded_at-interval '1 day',
 created_at=v.created_at-interval '1 day',updated_at=v.updated_at-interval '1 day'
WHERE EXISTS(SELECT 1 FROM medical_record r JOIN demo2_sunday_visits s ON s.visit_id=r.visit_id
 WHERE r.record_id=v.medical_record_id);
UPDATE invoice SET issue_date=issue_date-interval '1 day',due_date=due_date-1,
 created_at=created_at-interval '1 day',updated_at=updated_at-interval '1 day'
WHERE visit_id IN(SELECT visit_id FROM demo2_sunday_visits);
UPDATE payment_transaction t SET paid_at=paid_at-interval '1 day',
 created_at=t.created_at-interval '1 day',updated_at=t.updated_at-interval '1 day'
WHERE EXISTS(SELECT 1 FROM invoice i JOIN demo2_sunday_visits s ON s.visit_id=i.visit_id
 WHERE i.invoice_id=t.invoice_id);
UPDATE test_request tr SET performed_at=performed_at-interval '1 day',
 completed_at=completed_at-interval '1 day',created_at=tr.created_at-interval '1 day',
 updated_at=tr.updated_at-interval '1 day'
WHERE EXISTS(SELECT 1 FROM medical_record r JOIN demo2_sunday_visits s ON s.visit_id=r.visit_id
 WHERE r.record_id=tr.medical_record_id);
UPDATE test_result result SET collected_at=collected_at-interval '1 day',
 performed_at=performed_at-interval '1 day',verified_at=verified_at-interval '1 day',
 created_at=result.created_at-interval '1 day',updated_at=result.updated_at-interval '1 day'
WHERE EXISTS(SELECT 1 FROM test_request tr JOIN medical_record r ON r.record_id=tr.medical_record_id
 JOIN demo2_sunday_visits s ON s.visit_id=r.visit_id WHERE tr.test_request_id=result.test_request_id);
UPDATE test_result_revision revision SET signed_at=signed_at-interval '1 day',
 created_at=revision.created_at-interval '1 day',updated_at=revision.updated_at-interval '1 day'
WHERE EXISTS(SELECT 1 FROM test_result result JOIN test_request tr ON tr.test_request_id=result.test_request_id
 JOIN medical_record r ON r.record_id=tr.medical_record_id JOIN demo2_sunday_visits s ON s.visit_id=r.visit_id
 WHERE result.result_id=revision.result_id);

-- Existing appointments are normalized to one of the two supported shifts.
UPDATE appointment a
SET scheduled_at = a.scheduled_at::date
        + CASE WHEN a.scheduled_at::time < time '12:00' THEN time '07:30' ELSE time '13:30' END,
    shift_name = CASE WHEN a.scheduled_at::time < time '12:00' THEN 'Ca Sáng' ELSE 'Ca Chiều' END,
    shift_time = CASE WHEN a.scheduled_at::time < time '12:00' THEN '07:30 - 11:30' ELSE '13:30 - 17:30' END,
    shift_version_id = CASE WHEN a.scheduled_at::time < time '12:00'
        THEN '71000001-1111-1111-1111-111111111111'::uuid
        ELSE '71000002-2222-2222-2222-222222222222'::uuid END,
    updated_at = pg_temp.d2_now();

-- Sunday appointments are cancelled rather than silently moved to another day.
UPDATE appointment
SET status='CANCELLED', cancel_reason='Phòng khám nghỉ Chủ nhật', updated_at=pg_temp.d2_now()
WHERE EXTRACT(ISODOW FROM scheduled_at)=7 AND status<>'CANCELLED';

CREATE TEMP TABLE demo2_calendar ON COMMIT DROP AS
SELECT d::date work_date,
       (ARRAY['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'])
          [EXTRACT(ISODOW FROM d)::int] day_name
FROM generate_series((date_trunc('month',pg_temp.d2_date())-interval '2 months')::date,
                     pg_temp.d2_date()+interval '21 days', interval '1 day') d;

-- Template can hold one shift per weekday. Morning is the recurring template;
-- actual schedules below contain both ticks for every fixed room doctor.
INSERT INTO staff_schedule_template(template_id,created_at,updated_at,deleted,staff_id,day_of_week,shift_id,is_active)
SELECT pg_temp.d2id('template:'||m.staff_id||':'||w.day_name),pg_temp.d2_now(),pg_temp.d2_now(),false,
       m.staff_id,w.day_name,'70000001-1111-1111-1111-111111111111',true
FROM demo2_doctor_accounts m
CROSS JOIN (VALUES('MONDAY'),('TUESDAY'),('WEDNESDAY'),('THURSDAY'),('FRIDAY'),('SATURDAY')) w(day_name);

INSERT INTO staff_schedule(schedule_id,created_at,updated_at,deleted,is_custom,note,status,work_date,
 shift_id,shift_version_id,actual_start_time,actual_end_time,staff_id,template_id)
SELECT pg_temp.d2id('schedule:doctor:'||m.staff_id||':'||c.work_date||':'||sh.code),
 pg_temp.d2_now(),pg_temp.d2_now(),false,sh.code='PM',
 'Lịch trình diễn: một bác sĩ cố định cho phòng, làm đủ hai ca','SCHEDULED',c.work_date,
 sh.shift_id,sh.version_id,sh.starts,sh.ends,m.staff_id,
 CASE WHEN sh.code='AM' THEN pg_temp.d2id('template:'||m.staff_id||':'||c.day_name) END
FROM demo2_doctor_accounts m
CROSS JOIN demo2_calendar c
CROSS JOIN (VALUES
 ('AM','70000001-1111-1111-1111-111111111111'::uuid,'71000001-1111-1111-1111-111111111111'::uuid,time '07:30',time '11:30'),
 ('PM','70000002-2222-2222-2222-222222222222'::uuid,'71000002-2222-2222-2222-222222222222'::uuid,time '13:30',time '17:30')
) sh(code,shift_id,version_id,starts,ends)
WHERE EXTRACT(ISODOW FROM c.work_date) BETWEEN 1 AND 6;

-- Reception and cashier coverage is split by shift; backup accounts remain
-- available but are not scheduled by default.
INSERT INTO staff_schedule(schedule_id,created_at,updated_at,deleted,is_custom,note,status,work_date,
 shift_id,shift_version_id,actual_start_time,actual_end_time,staff_id,template_id)
SELECT pg_temp.d2id('schedule:ops:'||o.staff_id||':'||c.work_date),pg_temp.d2_now(),pg_temp.d2_now(),false,
 true,'Lịch vận hành trình diễn','SCHEDULED',c.work_date,
 CASE WHEN o.username LIKE '%.am' THEN '70000001-1111-1111-1111-111111111111'::uuid ELSE '70000002-2222-2222-2222-222222222222'::uuid END,
 CASE WHEN o.username LIKE '%.am' THEN '71000001-1111-1111-1111-111111111111'::uuid ELSE '71000002-2222-2222-2222-222222222222'::uuid END,
 CASE WHEN o.username LIKE '%.am' THEN time '07:30' ELSE time '13:30' END,
 CASE WHEN o.username LIKE '%.am' THEN time '11:30' ELSE time '17:30' END,o.staff_id,NULL
FROM demo2_operational_accounts o CROSS JOIN demo2_calendar c
WHERE o.username NOT LIKE '%.backup' AND EXTRACT(ISODOW FROM c.work_date) BETWEEN 1 AND 6;

-- Hai tài khoản quầy chính dùng để trình diễn có đủ sáng, chiều và tối. Nhờ đó
-- chốt kiểm tra lịch trực ở backend vẫn cho phép test xuyên suốt trong ngày.
INSERT INTO staff_schedule(schedule_id,created_at,updated_at,deleted,is_custom,note,status,work_date,
 shift_id,shift_version_id,actual_start_time,actual_end_time,staff_id,template_id)
SELECT pg_temp.d2id('schedule:primary-ops:'||o.staff_id||':'||c.work_date||':'||sh.code),
 pg_temp.d2_now(),pg_temp.d2_now(),false,true,'Lịch ba ca cho tài khoản quầy trình diễn',
 'SCHEDULED',c.work_date,sh.shift_id,sh.version_id,sh.starts,sh.ends,o.staff_id,NULL
FROM demo2_operational_accounts o
CROSS JOIN demo2_calendar c
CROSS JOIN (VALUES
 ('PM','70000002-2222-2222-2222-222222222222'::uuid,'71000002-2222-2222-2222-222222222222'::uuid,time '13:30',time '17:30'),
 ('EVENING','70000003-3333-3333-3333-333333333333'::uuid,'71000003-3333-3333-3333-333333333333'::uuid,time '18:00',time '21:30')
) sh(code,shift_id,version_id,starts,ends)
WHERE o.username IN('receptionist.am','cashier.am')
  AND EXTRACT(ISODOW FROM c.work_date) BETWEEN 1 AND 6
  AND NOT EXISTS(
    SELECT 1 FROM staff_schedule existing
    WHERE existing.staff_id=o.staff_id AND existing.work_date=c.work_date
      AND existing.shift_id=sh.shift_id AND NOT existing.deleted
  );

-- Nurses/technicians keep one realistic shift. Both room doctors remain present
-- in both shifts, so a missing nurse never substitutes the mandatory doctor.
INSERT INTO staff_schedule(schedule_id,created_at,updated_at,deleted,is_custom,note,status,work_date,
 shift_id,shift_version_id,actual_start_time,actual_end_time,staff_id,template_id)
SELECT pg_temp.d2id('schedule:nurse:'||s.staff_id||':'||c.work_date),pg_temp.d2_now(),pg_temp.d2_now(),false,
 true,'Lịch hỗ trợ điều dưỡng/KTV','SCHEDULED',c.work_date,
 CASE WHEN row_number() OVER(PARTITION BY s.department_id ORDER BY s.staff_id)%2=1
      THEN '70000001-1111-1111-1111-111111111111'::uuid ELSE '70000002-2222-2222-2222-222222222222'::uuid END,
 CASE WHEN row_number() OVER(PARTITION BY s.department_id ORDER BY s.staff_id)%2=1
      THEN '71000001-1111-1111-1111-111111111111'::uuid ELSE '71000002-2222-2222-2222-222222222222'::uuid END,
 CASE WHEN row_number() OVER(PARTITION BY s.department_id ORDER BY s.staff_id)%2=1 THEN time '07:30' ELSE time '13:30' END,
 CASE WHEN row_number() OVER(PARTITION BY s.department_id ORDER BY s.staff_id)%2=1 THEN time '11:30' ELSE time '17:30' END,
 s.staff_id,NULL
FROM staff_info s CROSS JOIN demo2_calendar c
WHERE s.system_role='NURSE' AND NOT s.deleted AND EXTRACT(ISODOW FROM c.work_date) BETWEEN 1 AND 6;

INSERT INTO clinic_schedule_exception(exception_id,created_at,updated_at,deleted,work_date,shift_id,
 exception_type,special_start_time,special_end_time,reason,created_by)
SELECT pg_temp.d2id('closed:sunday:'||work_date),pg_temp.d2_now(),pg_temp.d2_now(),false,
 work_date,NULL,'CLOSED_DAY',NULL,NULL,'Phòng khám nghỉ Chủ nhật',
 '30000013-3333-3333-3333-333333333333'
FROM demo2_calendar WHERE EXTRACT(ISODOW FROM work_date)=7;

-- ============================================================================
-- Two-month report history (deterministic, Monday-Saturday)
-- ============================================================================
CREATE TEMP TABLE demo2_report_cases ON COMMIT DROP AS
WITH workdays AS (
 SELECT work_date,row_number() OVER(ORDER BY work_date) day_no,
        date_trunc('month',work_date)::date report_month
 FROM demo2_calendar
 WHERE work_date < pg_temp.d2_date() AND EXTRACT(ISODOW FROM work_date) BETWEEN 1 AND 6
), base_targets AS (
 SELECT w.*,
    5 + ((w.day_no + EXTRACT(ISODOW FROM w.work_date)::int) % 4)
      + CASE WHEN EXTRACT(ISODOW FROM w.work_date) IN (1,6) THEN 1 ELSE 0 END AS base_cases
 FROM workdays w
), first_month AS (
 SELECT sum(base_cases)::int first_month_total
 FROM base_targets
 WHERE report_month=(date_trunc('month',pg_temp.d2_date())-interval '2 months')::date
), month_targets AS (
 SELECT b.*,
        count(*) OVER(PARTITION BY report_month)::int AS workday_count,
        row_number() OVER(PARTITION BY report_month ORDER BY
          CASE WHEN EXTRACT(ISODOW FROM work_date) IN(1,6) THEN 0 ELSE 1 END,
          work_date)::int AS demand_rank,
        CASE
          WHEN report_month=(date_trunc('month',pg_temp.d2_date())-interval '1 month')::date
            THEN round((SELECT first_month_total FROM first_month)*1.10)::int
          ELSE sum(base_cases) OVER(PARTITION BY report_month)::int
        END AS month_target
 FROM base_targets b
), daily_targets AS (
 SELECT m.*,
        5 + ((month_target-5*workday_count)/workday_count)
          + CASE WHEN demand_rank<=((month_target-5*workday_count)%workday_count) THEN 1 ELSE 0 END
          AS daily_target,
        (SELECT count(*) FROM customer_visit existing
          WHERE existing.check_in_time::date=m.work_date AND NOT existing.deleted) AS existing_visits
 FROM month_targets m
), expanded AS (
 SELECT w.*,slot
 FROM daily_targets w
 CROSS JOIN LATERAL generate_series(1,GREATEST(daily_target-existing_visits,0)) slot
), numbered AS (
 SELECT *,row_number() OVER(ORDER BY work_date,slot) row_no FROM expanded
)
SELECT 'R'||to_char(work_date,'YYYYMMDD')||'-'||lpad(slot::text,2,'0') case_key,
       work_date,slot,row_no,
       (SELECT patient_id FROM demo2_customers
         WHERE customer_no=1+((row_no-1)%20)::int) patient_id,
       1+((row_no-1)%7)::int service_no,
       CASE 1+((row_no-1)%7)::int
        WHEN 1 THEN '33333333-3333-3333-3333-333333333333'::uuid
        WHEN 2 THEN '77777777-7777-7777-7777-777777777777'::uuid
        WHEN 3 THEN '66666666-6666-6666-6666-666666666666'::uuid
        WHEN 4 THEN '88888888-8888-8888-8888-888888888888'::uuid
        WHEN 5 THEN '99999999-9999-9999-9999-999999999999'::uuid
        WHEN 6 THEN 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'::uuid
        ELSE 'dddddddd-dddd-dddd-dddd-dddddddddddd'::uuid END room_id,
       CASE WHEN row_no%2=0 THEN (ARRAY['LAB-001','LAB-002','LAB-003','LAB-004','LAB-005','LAB-006','IMG-001','IMG-003'])[1+((row_no/2-1)%8)::int] END test_code,
       work_date + CASE WHEN row_no%2=0 THEN time '13:45' ELSE time '08:00' END
          + ((slot-1)*interval '12 minutes') started_at
FROM numbered;

CREATE TEMP VIEW demo2_report_enriched AS
SELECT c.*,p.account_id,d.head_doctor_id doctor_id,
 CASE WHEN c.started_at::time<time '12:00' THEN '90000012-5555-5555-5555-555555555555'::uuid
      ELSE '91000000-0000-0000-0000-000000000011'::uuid END receptionist_id,
 CASE WHEN c.started_at::time<time '12:00' THEN '90000013-6666-6666-6666-666666666666'::uuid
      ELSE '91000000-0000-0000-0000-000000000013'::uuid END cashier_id
FROM demo2_report_cases c JOIN profile p ON p.profile_id=c.patient_id
JOIN department d ON d.department_id=c.room_id;

INSERT INTO appointment(appointment_id,created_at,updated_at,deleted,scheduled_at,status,is_guest,
 customer_id,shift_name,shift_time,shift_version_id)
SELECT pg_temp.d2id('appointment:'||case_key),started_at-interval '2 days',started_at,false,
 date_trunc('day',started_at)+CASE WHEN started_at::time<time '12:00' THEN time '07:30' ELSE time '13:30' END,
 'CHECKED_IN',false,patient_id,
 CASE WHEN started_at::time<time '12:00' THEN 'Ca Sáng' ELSE 'Ca Chiều' END,
 CASE WHEN started_at::time<time '12:00' THEN '07:30 - 11:30' ELSE '13:30 - 17:30' END,
 CASE WHEN started_at::time<time '12:00' THEN '71000001-1111-1111-1111-111111111111'::uuid
      ELSE '71000002-2222-2222-2222-222222222222'::uuid END
FROM demo2_report_enriched;

INSERT INTO appointment_services(appointment_id,service_id)
SELECT pg_temp.d2id('appointment:'||case_key),
 format('400000%s-0000-0000-0000-%s',lpad(service_no::text,2,'0'),lpad(service_no::text,12,'0'))::uuid
FROM demo2_report_cases;

INSERT INTO customer_visit(visit_id,created_at,updated_at,deleted,check_in_time,check_out_time,status,
 appointment_id,checked_in_by,customer_id)
SELECT pg_temp.d2id('visit:'||case_key),started_at,started_at+interval '90 minutes',false,started_at,
 started_at+interval '90 minutes','COMPLETED',pg_temp.d2id('appointment:'||case_key),receptionist_id,patient_id
FROM demo2_report_enriched;

INSERT INTO queue_ticket(ticket_id,visit_id,department_id,work_date,queue_number,status,called_at,
 completed_at,service_id,created_at,updated_at,deleted)
SELECT pg_temp.d2id('queue:exam:'||case_key),pg_temp.d2id('visit:'||case_key),room_id,work_date,
 COALESCE((SELECT max(existing.queue_number) FROM queue_ticket existing
            WHERE existing.department_id=c.room_id AND existing.work_date=c.work_date),0)
   +row_number() OVER(PARTITION BY room_id,work_date ORDER BY started_at,case_key),
 'DONE',started_at+interval '5 minutes',
 started_at+interval '35 minutes',
 format('400000%s-0000-0000-0000-%s',lpad(service_no::text,2,'0'),lpad(service_no::text,12,'0'))::uuid,
 started_at,started_at+interval '35 minutes',false
FROM demo2_report_cases c;

INSERT INTO medical_record(record_id,record_version,record_code,visit_id,queue_ticket_id,doctor_id,
 chief_complaint,clinical_findings,diagnosis,prescription_note,conclusion,patient_instruction,status,
 completed_at,contact_requested,doctor_confirmed_by,doctor_confirmed_at,created_at,updated_at,deleted)
SELECT pg_temp.d2id('record:'||case_key),0,'MR-'||to_char(work_date,'YYYYMMDD')||'-'||right(case_key,2),
 pg_temp.d2id('visit:'||case_key),pg_temp.d2id('queue:exam:'||case_key),doctor_id,
 CASE service_no WHEN 3 THEN 'Khám định kỳ, tư vấn dinh dưỡng và phát triển cho trẻ.'
  WHEN 4 THEN 'Da khô và ngứa nhẹ tái diễn.' WHEN 5 THEN 'Đau nhẹ vùng khớp sau vận động.'
  WHEN 6 THEN 'Theo dõi huyết áp và sức khỏe tim mạch.' WHEN 7 THEN 'Khám phụ khoa định kỳ.'
  ELSE 'Khám sức khỏe định kỳ, chưa ghi nhận triệu chứng cấp tính.' END,
 'Bệnh nhân tỉnh, tiếp xúc tốt; thăm khám chưa ghi nhận dấu hiệu cấp cứu.',
 CASE service_no WHEN 4 THEN 'Viêm da không đặc hiệu - L30.9' ELSE 'Khám sức khỏe tổng quát - Z00.0' END,
 'Không chỉ định thuốc trong lần khám mô phỏng này.',
 'Tình trạng ổn định tại thời điểm khám.',
 'Duy trì sinh hoạt phù hợp và tái khám khi có triệu chứng bất thường.','COMPLETED',
 started_at+interval '35 minutes',false,doctor_id,started_at+interval '35 minutes',started_at,
 started_at+interval '35 minutes',false
FROM demo2_report_enriched;

INSERT INTO vital_signs(vital_id,medical_record_id,blood_pressure,heart_rate,temperature,height,weight,
 recorded_at,recorded_by,created_at,updated_at,deleted)
SELECT pg_temp.d2id('vital:'||case_key),pg_temp.d2id('record:'||case_key),
 CASE WHEN row_no%9=0 THEN '132/84' ELSE '118/76' END,68+(row_no%15)::int,
 36.5+((row_no%4)::numeric/10),p.height,p.weight,started_at+interval '3 minutes',doctor_id,
 started_at+interval '3 minutes',started_at+interval '3 minutes',false
FROM demo2_report_enriched c JOIN profile p ON p.profile_id=c.patient_id;
UPDATE medical_record r SET vital_signs_id=v.vital_id
FROM vital_signs v WHERE v.medical_record_id=r.record_id AND r.record_code LIKE 'MR-%';

UPDATE medical_record r
SET rating_score=3+(c.row_no%3)::int,
 rated_at=c.started_at+interval '2 hours',
 feedback_status=CASE WHEN c.row_no%2=0 THEN 'RESPONDED' ELSE 'NEW' END,
 rating_comment=(ARRAY[
  'Bác sĩ giải thích rõ tình trạng và hướng theo dõi tại nhà.',
  'Quy trình khám thuận tiện, nhân viên hướng dẫn dễ hiểu.',
  'Bác sĩ tư vấn cẩn thận; thời gian chờ ở mức hợp lý.',
  'Phòng khám sạch sẽ, kết quả được giải thích rõ ràng.'
 ])[1+(c.row_no%4)::int],
 manager_response=CASE WHEN c.row_no%2=0 THEN 'CareS cảm ơn góp ý và sẽ tiếp tục cải thiện chất lượng phục vụ.' END,
 responded_at=CASE WHEN c.row_no%2=0 THEN c.started_at+interval '1 day' END,
 responded_by=CASE WHEN c.row_no%2=0 THEN '90000009-2222-2222-2222-222222222222'::uuid END
FROM demo2_report_cases c WHERE r.record_id=pg_temp.d2id('record:'||c.case_key)
AND c.row_no%4<>0;

INSERT INTO icd_10_selections(selection_id,record_id,code,code_name,created_at,updated_at,deleted)
SELECT pg_temp.d2id('icd:'||case_key),pg_temp.d2id('record:'||case_key),
 CASE WHEN service_no=4 THEN 'L30.9' ELSE 'Z00.0' END,
 CASE WHEN service_no=4 THEN 'Viêm da, không đặc hiệu' ELSE 'Khám sức khỏe tổng quát' END,
 started_at+interval '35 minutes',started_at+interval '35 minutes',false
FROM demo2_report_cases;

-- One invoice contains the examination and, for alternate cases, one completed
-- paraclinical service. This creates at least fifteen report service lines.
INSERT INTO invoice(invoice_id,invoice_code,customer_id,visit_id,medical_record_id,issue_date,due_date,
 subtotal,discount,tax,total_amount,paid_amount,status,note,issued_by,created_at,updated_at,deleted)
SELECT pg_temp.d2id('invoice:'||c.case_key),'INV-'||to_char(c.work_date,'YYYYMMDD')||'-'||right(c.case_key,2),
 c.patient_id,pg_temp.d2id('visit:'||c.case_key),NULL,c.started_at,c.started_at::date,
 exam.price+COALESCE(test.price,0),
 CASE WHEN c.row_no%20 IN(18,19) THEN round((exam.price+COALESCE(test.price,0))*.15,2)
      WHEN c.row_no%4=0 THEN round((exam.price+COALESCE(test.price,0))*.20,2) ELSE 0 END,
 0,
 (exam.price+COALESCE(test.price,0))-CASE WHEN c.row_no%20 IN(18,19) THEN round((exam.price+COALESCE(test.price,0))*.15,2)
      WHEN c.row_no%4=0 THEN round((exam.price+COALESCE(test.price,0))*.20,2) ELSE 0 END,
 (exam.price+COALESCE(test.price,0))-CASE WHEN c.row_no%20 IN(18,19) THEN round((exam.price+COALESCE(test.price,0))*.15,2)
      WHEN c.row_no%4=0 THEN round((exam.price+COALESCE(test.price,0))*.20,2) ELSE 0 END,
 'PAID','Hóa đơn lịch sử mô phỏng phục vụ báo cáo',c.cashier_id,c.started_at,c.started_at+interval '5 minutes',false
FROM demo2_report_enriched c
JOIN medical_service exam ON exam.service_id=format('400000%s-0000-0000-0000-%s',lpad(c.service_no::text,2,'0'),lpad(c.service_no::text,12,'0'))::uuid
LEFT JOIN medical_service test ON test.service_code=c.test_code;

INSERT INTO invoice_item(item_id,invoice_id,service_id,service_snapshot,service_code_snapshot,unit_price,
 quantity,discount_percent,discount_amount,final_price,line_total,bhyt_fund,note,created_at,updated_at,deleted)
SELECT pg_temp.d2id('item:exam:'||c.case_key),pg_temp.d2id('invoice:'||c.case_key),s.service_id,s.name,s.service_code,
 s.price,1,0,0,
 s.price-CASE WHEN c.row_no%4=0 AND c.row_no%20 NOT IN(18,19) THEN round(s.price*.20,2) ELSE 0 END,
 s.price,CASE WHEN c.row_no%4=0 AND c.row_no%20 NOT IN(18,19) THEN round(s.price*.20,2) ELSE 0 END,
 'Dịch vụ khám lịch sử',c.started_at,c.started_at,false
FROM demo2_report_cases c JOIN medical_service s
 ON s.service_id=format('400000%s-0000-0000-0000-%s',lpad(c.service_no::text,2,'0'),lpad(c.service_no::text,12,'0'))::uuid;

INSERT INTO invoice_item(item_id,invoice_id,service_id,service_snapshot,service_code_snapshot,unit_price,
 quantity,discount_percent,discount_amount,final_price,line_total,bhyt_fund,note,created_at,updated_at,deleted)
SELECT pg_temp.d2id('item:test:'||c.case_key),pg_temp.d2id('invoice:'||c.case_key),s.service_id,s.name,s.service_code,
 s.price,1,0,0,
 s.price-CASE WHEN c.row_no%4=0 AND c.row_no%20 NOT IN(18,19) THEN round(s.price*.20,2) ELSE 0 END,
 s.price,CASE WHEN c.row_no%4=0 AND c.row_no%20 NOT IN(18,19) THEN round(s.price*.20,2) ELSE 0 END,
 'CLS đã hoàn thành trong lượt lịch sử',c.started_at,c.started_at,false
FROM demo2_report_cases c JOIN medical_service s ON s.service_code=c.test_code
WHERE c.test_code IS NOT NULL;

INSERT INTO payment_transaction(transaction_id,invoice_id,transaction_code,amount,payment_method,status,
 paid_at,gateway_reference,note,received_by,created_at,updated_at,deleted)
SELECT pg_temp.d2id('payment:'||c.case_key),i.invoice_id,'PAY-'||replace(right(i.invoice_id::text,20),'-',''),
 i.total_amount,
 CASE c.row_no%20 WHEN 0 THEN 'CASH' WHEN 1 THEN 'CASH' WHEN 2 THEN 'CASH' WHEN 3 THEN 'CASH'
  WHEN 4 THEN 'CASH' WHEN 5 THEN 'CASH' WHEN 6 THEN 'CASH'
  WHEN 7 THEN 'BANK_TRANSFER' WHEN 8 THEN 'BANK_TRANSFER' WHEN 9 THEN 'BANK_TRANSFER'
  WHEN 10 THEN 'BANK_TRANSFER' WHEN 11 THEN 'BANK_TRANSFER'
  WHEN 12 THEN 'CARD' WHEN 13 THEN 'CARD' WHEN 14 THEN 'CARD'
  WHEN 15 THEN 'VNPAY' WHEN 16 THEN 'VNPAY' WHEN 17 THEN 'MOMO' ELSE 'MEMBERSHIP_CARD' END,
 'SUCCESS',CASE WHEN c.row_no%31=0 THEN (c.started_at+interval '1 day') ELSE c.started_at+interval '5 minutes' END,
 NULL,'Giao dịch giả lập, không chuyển tiền thực tế',c.cashier_id,c.started_at,c.started_at+interval '5 minutes',false
FROM demo2_report_enriched c JOIN invoice i ON i.invoice_id=pg_temp.d2id('invoice:'||c.case_key);

-- Explicit non-revenue examples for the reconciliation tab. They are attached
-- to valid visits but never contribute to successful receipts.
INSERT INTO invoice(invoice_id,invoice_code,customer_id,visit_id,medical_record_id,issue_date,due_date,
 subtotal,discount,tax,total_amount,paid_amount,status,note,issued_by,created_at,updated_at,deleted)
SELECT pg_temp.d2id('invoice:cancelled'),'INV-CANCELLED-DEMO2',c.patient_id,
 pg_temp.d2id('visit:'||c.case_key),NULL,c.started_at+interval '10 minutes',c.work_date,
 s.price,0,0,s.price,0,'CANCELLED','Hóa đơn mô phỏng đã hủy trước khi thu tiền',c.cashier_id,
 c.started_at+interval '10 minutes',c.started_at+interval '12 minutes',false
FROM demo2_report_enriched c JOIN medical_service s
 ON s.service_id=format('400000%s-0000-0000-0000-%s',lpad(c.service_no::text,2,'0'),lpad(c.service_no::text,12,'0'))::uuid
ORDER BY c.started_at LIMIT 1;
INSERT INTO invoice_item(item_id,invoice_id,service_id,service_snapshot,service_code_snapshot,unit_price,
 quantity,discount_percent,discount_amount,final_price,line_total,bhyt_fund,note,created_at,updated_at,deleted)
SELECT pg_temp.d2id('item:cancelled'),pg_temp.d2id('invoice:cancelled'),s.service_id,s.name,s.service_code,
 s.price,1,0,0,s.price,s.price,0,'Dòng dịch vụ của hóa đơn đã hủy',i.issue_date,i.issue_date,false
FROM invoice i JOIN medical_service s ON s.service_code='EX-IN-001'
WHERE i.invoice_id=pg_temp.d2id('invoice:cancelled');
INSERT INTO payment_transaction(transaction_id,invoice_id,transaction_code,amount,payment_method,status,
 paid_at,gateway_reference,note,received_by,created_at,updated_at,deleted)
SELECT pg_temp.d2id('payment:cancelled'),i.invoice_id,'PAY-CANCELLED-DEMO2',i.total_amount,
 'CARD','CANCELLED',NULL,NULL,'Khách hủy thao tác thanh toán mô phỏng',i.issued_by,
 i.issue_date,i.issue_date+interval '2 minutes',false
FROM invoice i WHERE i.invoice_id=pg_temp.d2id('invoice:cancelled');
INSERT INTO payment_transaction(transaction_id,invoice_id,transaction_code,amount,payment_method,status,
 paid_at,gateway_reference,note,received_by,created_at,updated_at,deleted)
SELECT pg_temp.d2id('payment:failed'),i.invoice_id,'PAY-FAILED-DEMO2',i.total_amount,
 'VNPAY','FAILED',NULL,'VNPAY-DEMO-FAILED','Giao dịch mô phỏng thất bại, không phát sinh thu tiền',
 i.issued_by,i.issue_date,i.issue_date+interval '1 minute',false
FROM invoice i WHERE i.status='PENDING' AND NOT i.deleted
ORDER BY i.issue_date LIMIT 1;

-- Completed CLS requests and signed results for the eight report service groups.
CREATE TEMP VIEW demo2_test_cases AS
SELECT c.*,s.service_id test_service_id,
 CASE WHEN c.test_code='LAB-001' THEN '44444444-4444-4444-4444-444444444444'::uuid
      WHEN c.test_code LIKE 'LAB-%' THEN 'cccccccc-cccc-cccc-cccc-cccccccccccc'::uuid
      WHEN c.test_code='IMG-003' THEN '55555555-5555-5555-5555-555555555555'::uuid
      ELSE 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'::uuid END test_room
FROM demo2_report_cases c JOIN medical_service s ON s.service_code=c.test_code
WHERE c.test_code IS NOT NULL;

INSERT INTO queue_ticket(ticket_id,visit_id,department_id,work_date,queue_number,status,called_at,
 completed_at,service_id,created_at,updated_at,deleted)
SELECT pg_temp.d2id('queue:test:'||case_key),pg_temp.d2id('visit:'||case_key),test_room,work_date,
 COALESCE((SELECT max(existing.queue_number) FROM queue_ticket existing
            WHERE existing.department_id=c.test_room AND existing.work_date=c.work_date),0)
   +row_number() OVER(PARTITION BY test_room,work_date ORDER BY started_at,case_key),
 'DONE',
 started_at+interval '40 minutes',started_at+interval '70 minutes',test_service_id,
 started_at+interval '38 minutes',started_at+interval '70 minutes',false
FROM demo2_test_cases c;

INSERT INTO test_request(test_request_id,medical_record_id,service_id,performing_department,queue_ticket_id,
 description,status,requested_by,completed_at,performed_at,invoice_item_id,created_at,updated_at,deleted)
SELECT pg_temp.d2id('test:'||c.case_key),pg_temp.d2id('record:'||c.case_key),c.test_service_id,c.test_room,
 pg_temp.d2id('queue:test:'||c.case_key),'Chỉ định '||s.name||' phục vụ đánh giá trong ca mô phỏng.',
 'COMPLETED',e.doctor_id,c.started_at+interval '70 minutes',c.started_at+interval '45 minutes',
 pg_temp.d2id('item:test:'||c.case_key),c.started_at+interval '35 minutes',c.started_at+interval '70 minutes',false
FROM demo2_test_cases c JOIN demo2_report_enriched e ON e.case_key=c.case_key
JOIN medical_service s ON s.service_id=c.test_service_id;

INSERT INTO test_result(result_id,test_request_id,conclusion,sample_id,sample_type,sample_status,
 collected_at,collected_by,performed_by,performed_at,verified_by,verified_at,form_template_version_id,
 result_data,created_at,updated_at,deleted)
SELECT pg_temp.d2id('result:'||c.case_key),pg_temp.d2id('test:'||c.case_key),
 CASE WHEN c.test_code LIKE 'IMG-%' THEN 'Chưa ghi nhận bất thường rõ tại thời điểm khảo sát.'
      ELSE 'Các chỉ số đã thực hiện trong giới hạn tham chiếu của biểu mẫu demo.' END,
 CASE WHEN c.test_code LIKE 'LAB-%' THEN 'SMP-'||to_char(c.work_date,'YYYYMMDD')||'-'||upper(right(c.case_key,2)) END,
 CASE WHEN c.test_code LIKE 'LAB-%' THEN CASE WHEN c.test_code='LAB-006' THEN 'URINE' ELSE 'BLOOD' END END,
 CASE WHEN c.test_code LIKE 'LAB-%' THEN 'ACCEPTED' END,
 CASE WHEN c.test_code LIKE 'LAB-%' THEN c.started_at+interval '42 minutes' END,NULL,
 d.head_doctor_id,c.started_at+interval '50 minutes',d.head_doctor_id,c.started_at+interval '70 minutes',tv.version_id,
 CASE c.test_code
  WHEN 'LAB-001' THEN '{"rbc":4.6,"hgb":138,"hct":41.4,"mcv":90,"mch":30,"mchc":333,"wbc":6.1,"plt":250,"_meta":{"completionStatus":"COMPLETE"},"_omissions":{}}'::jsonb
  WHEN 'LAB-002' THEN '{"glucoseContext":"RANDOM","glucoseQualifier":"EQUAL","glucose":5.2,"_meta":{"completionStatus":"COMPLETE"},"_omissions":{}}'::jsonb
  WHEN 'LAB-003' THEN '{"hba1c":5.3,"totalCholesterol":4.5,"triglyceride":1.2,"hdl":1.4,"ldl":2.5,"uricAcid":320,"totalProtein":72,"_meta":{"completionStatus":"COMPLETE"},"_omissions":{}}'::jsonb
  WHEN 'LAB-004' THEN '{"ast":22,"alt":24,"alp":75,"ggt":28,"totalBilirubin":12,"directBilirubin":3,"albumin":43,"_meta":{"completionStatus":"COMPLETE"},"_omissions":{}}'::jsonb
  WHEN 'LAB-005' THEN '{"urea":5.1,"creatinine":82,"sodium":140,"potassium":4.1,"chloride":103,"bicarbonate":24,"calcium":2.3,"phosphate":1.1,"_meta":{"completionStatus":"COMPLETE"},"_omissions":{}}'::jsonb
  WHEN 'LAB-006' THEN '{"specificGravity":"1.015","ph":"6.0","leukocyte":"NEGATIVE","nitrite":"NEGATIVE","protein":"NEGATIVE","glucose":"NEGATIVE","ketone":"NEGATIVE","urobilinogen":"NORMAL","bilirubin":"NEGATIVE","blood":"NEGATIVE","_meta":{"completionStatus":"COMPLETE"},"_omissions":{}}'::jsonb
  WHEN 'IMG-003' THEN '{"liverDescription":"Kích thước và nhu mô chưa ghi nhận bất thường.","gallbladderDescription":"Thành không dày, chưa thấy sỏi.","kidneyDescription":"Hai thận không ứ nước.","imagingConclusion":"Chưa ghi nhận bất thường rõ.","_meta":{"completionStatus":"COMPLETE"},"_omissions":{}}'::jsonb
  ELSE '{"lesionDescription":"Hai phế trường sáng, chưa thấy tổn thương khu trú.","imagingConclusion":"Chưa ghi nhận bất thường trên phim ngực.","_meta":{"completionStatus":"COMPLETE"},"_omissions":{}}'::jsonb END,
 c.started_at+interval '50 minutes',c.started_at+interval '70 minutes',false
FROM demo2_test_cases c JOIN department d ON d.department_id=c.test_room
JOIN medical_service_form_template b ON b.service_id=c.test_service_id AND NOT b.deleted
JOIN LATERAL(SELECT v.version_id FROM clinical_form_template_version v
 WHERE v.template_id=b.template_id AND NOT v.deleted AND v.status='PUBLISHED'
 AND v.effective_from<=c.work_date ORDER BY v.version_no DESC LIMIT 1) tv ON true;

INSERT INTO test_result_revision(revision_id,result_id,revision_no,status,result_data,conclusion,
 template_version_id,entered_by,signed_by,signed_at,created_at,updated_at,deleted)
SELECT pg_temp.d2id('revision:'||c.case_key),r.result_id,1,'SIGNED',r.result_data,r.conclusion,
 r.form_template_version_id,r.performed_by,r.verified_by,r.verified_at,r.created_at,r.updated_at,false
FROM demo2_test_cases c JOIN test_result r ON r.result_id=pg_temp.d2id('result:'||c.case_key);

-- Rebuild CareS cards/ledger so every MEMBERSHIP_CARD payment is traceable and
-- no BHYT amount is overwritten by the membership benefit.
DELETE FROM membership_card_ledger;
DELETE FROM membership_card;
INSERT INTO membership_card(card_id,card_code,owner_profile_id,status,balance,pin_hash,benefit_percent,
 activated_at,benefit_expires_at,version,created_at,updated_at,deleted)
SELECT pg_temp.d2id('card:'||p.profile_id),'CS-DEMO-'||lpad(regexp_replace(p.patient_code,'[^0-9]','','g'),4,'0'),p.profile_id,'ACTIVE',
 5000000-COALESCE((SELECT sum(t.amount) FROM payment_transaction t JOIN invoice i ON i.invoice_id=t.invoice_id
                    WHERE i.customer_id=p.profile_id AND t.status='SUCCESS' AND t.payment_method='MEMBERSHIP_CARD'),0),
 '$2a$10$fl4JcFQUApjxYX0D1xqsmOpZ761fONcBHLozxvF85tUkybPyRFXKq',15,
 pg_temp.d2_date()-90,pg_temp.d2_date()+365,1,pg_temp.d2_date()-90,pg_temp.d2_now(),false
FROM profile p JOIN account a ON a.account_id=p.account_id WHERE a.role='CUSTOMER';

INSERT INTO membership_card_ledger(ledger_id,card_id,type,amount,balance_before,balance_after,
 benefit_discount,source_payment_method,idempotency_key,reference_code,patient_profile_id,performed_by,
 created_at,updated_at,deleted)
SELECT pg_temp.d2id('topup:'||p.profile_id),c.card_id,'TOP_UP',5000000,0,5000000,0,'CASH',
 'DEMO2-TOPUP-'||lpad(regexp_replace(p.patient_code,'[^0-9]','','g'),4,'0'),
 'CS-TOPUP-'||lpad(regexp_replace(p.patient_code,'[^0-9]','','g'),4,'0'),p.profile_id,
 '90000013-6666-6666-6666-666666666666',pg_temp.d2_date()-90,pg_temp.d2_date()-90,false
FROM membership_card c JOIN profile p ON p.profile_id=c.owner_profile_id;

WITH card_payments AS (
 SELECT t.*,i.customer_id,i.discount,
  COALESCE((SELECT sum(ii.bhyt_fund) FROM invoice_item ii WHERE ii.invoice_id=i.invoice_id),0) insurance,
  sum(t.amount) OVER(PARTITION BY i.customer_id ORDER BY t.paid_at,t.transaction_id
    ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING) previous_amount
 FROM payment_transaction t JOIN invoice i ON i.invoice_id=t.invoice_id
 WHERE t.status='SUCCESS' AND t.payment_method='MEMBERSHIP_CARD'
)
INSERT INTO membership_card_ledger(ledger_id,card_id,type,amount,balance_before,balance_after,
 benefit_discount,source_payment_method,idempotency_key,reference_code,invoice_id,patient_profile_id,
 payment_transaction_id,performed_by,created_at,updated_at,deleted)
SELECT pg_temp.d2id('card-payment:'||p.transaction_id),c.card_id,'PAYMENT',p.amount,
 5000000-COALESCE(p.previous_amount,0),5000000-COALESCE(p.previous_amount,0)-p.amount,
 GREATEST(p.discount-p.insurance,0),'MEMBERSHIP_CARD','DEMO2-PAY-'||p.transaction_id,
 'CS-PAY-'||upper(left(replace(p.transaction_id::text,'-',''),12)),p.invoice_id,p.customer_id,
 p.transaction_id,p.received_by,p.paid_at,p.paid_at,false
FROM card_payments p JOIN membership_card c ON c.owner_profile_id=p.customer_id;

-- ============================================================================
-- Current-shift load: room 2 has exactly four more active tickets than room 1
-- ============================================================================
CREATE TEMP TABLE demo2_load_cases(case_key text,patient_id uuid,room_id uuid,service_id uuid,ordinal int) ON COMMIT DROP;
CREATE TEMP TABLE demo2_load_counts ON COMMIT DROP AS
SELECT count(*) FILTER(WHERE department_id='aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa')
         -count(*) FILTER(WHERE department_id='77777777-7777-7777-7777-777777777777') AS int_delta,
       count(*) FILTER(WHERE department_id='99999999-9999-9999-9999-999999999999')
         -count(*) FILTER(WHERE department_id='33333333-3333-3333-3333-333333333333') AS sur_delta
FROM queue_ticket
WHERE work_date=pg_temp.d2_date() AND status NOT IN('DONE','SKIPPED');
INSERT INTO demo2_load_cases
SELECT 'LOAD-INT-'||n,
 (SELECT patient_id FROM demo2_customers WHERE customer_no=12+n),
 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa','40000002-0000-0000-0000-000000000002',n
FROM generate_series(1,(SELECT GREATEST(0,4-int_delta)::int FROM demo2_load_counts)) AS g(n)
WHERE pg_temp.d2_is_open(pg_temp.d2_now());
INSERT INTO demo2_load_cases
SELECT 'LOAD-SUR-'||n,
 (SELECT patient_id FROM demo2_customers WHERE customer_no=16+n),
 '99999999-9999-9999-9999-999999999999','40000001-0000-0000-0000-000000000001',n
FROM generate_series(1,(SELECT GREATEST(0,4-sur_delta)::int FROM demo2_load_counts)) AS g(n)
WHERE pg_temp.d2_is_open(pg_temp.d2_now());

INSERT INTO appointment(appointment_id,created_at,updated_at,deleted,scheduled_at,status,is_guest,customer_id,
 shift_name,shift_time,shift_version_id)
SELECT pg_temp.d2id('appointment:'||case_key),pg_temp.d2_now()-interval '1 day',pg_temp.d2_now(),false,
 pg_temp.d2_date()+CASE WHEN pg_temp.d2_now()::time<time '12:00' THEN time '07:30' ELSE time '13:30' END,
 'CHECKED_IN',false,patient_id,
 CASE WHEN pg_temp.d2_now()::time<time '12:00' THEN 'Ca Sáng' ELSE 'Ca Chiều' END,
 CASE WHEN pg_temp.d2_now()::time<time '12:00' THEN '07:30 - 11:30' ELSE '13:30 - 17:30' END,
 CASE WHEN pg_temp.d2_now()::time<time '12:00' THEN '71000001-1111-1111-1111-111111111111'::uuid
      ELSE '71000002-2222-2222-2222-222222222222'::uuid END
FROM demo2_load_cases;
INSERT INTO appointment_services SELECT pg_temp.d2id('appointment:'||case_key),service_id FROM demo2_load_cases;
INSERT INTO customer_visit(visit_id,created_at,updated_at,deleted,check_in_time,check_out_time,status,
 appointment_id,checked_in_by,customer_id)
SELECT pg_temp.d2id('visit:'||case_key),pg_temp.d2_now()-interval '20 minutes',pg_temp.d2_now(),false,
 pg_temp.d2_now()-interval '20 minutes',NULL,'CHECKED_IN',pg_temp.d2id('appointment:'||case_key),
 CASE WHEN pg_temp.d2_now()::time<time '12:00' THEN '90000012-5555-5555-5555-555555555555'::uuid
      ELSE '91000000-0000-0000-0000-000000000011'::uuid END,patient_id FROM demo2_load_cases;
INSERT INTO invoice(invoice_id,invoice_code,customer_id,visit_id,issue_date,due_date,subtotal,discount,tax,
 total_amount,paid_amount,status,note,issued_by,created_at,updated_at,deleted)
SELECT pg_temp.d2id('invoice:'||l.case_key),'INV-'||to_char(pg_temp.d2_date(),'YYYYMMDD')||'-'||l.case_key,
 l.patient_id,pg_temp.d2id('visit:'||l.case_key),pg_temp.d2_now()-interval '15 minutes',pg_temp.d2_date(),
 s.price,0,0,s.price,s.price,'PAID','Phiếu hợp lệ tạo tải trình diễn',
 CASE WHEN pg_temp.d2_now()::time<time '12:00' THEN '90000013-6666-6666-6666-666666666666'::uuid
      ELSE '91000000-0000-0000-0000-000000000013'::uuid END,
 pg_temp.d2_now()-interval '15 minutes',pg_temp.d2_now()-interval '10 minutes',false
FROM demo2_load_cases l JOIN medical_service s ON s.service_id=l.service_id;
INSERT INTO invoice_item(item_id,invoice_id,service_id,service_snapshot,service_code_snapshot,unit_price,
 quantity,discount_percent,discount_amount,final_price,line_total,bhyt_fund,note,created_at,updated_at,deleted)
SELECT pg_temp.d2id('item:'||l.case_key),pg_temp.d2id('invoice:'||l.case_key),s.service_id,s.name,s.service_code,
 s.price,1,0,0,s.price,s.price,0,'Dịch vụ tiếp nhận ban đầu',pg_temp.d2_now()-interval '15 minutes',pg_temp.d2_now(),false
FROM demo2_load_cases l JOIN medical_service s ON s.service_id=l.service_id;
INSERT INTO payment_transaction(transaction_id,invoice_id,transaction_code,amount,payment_method,status,
 paid_at,note,received_by,created_at,updated_at,deleted)
SELECT pg_temp.d2id('payment:'||l.case_key),pg_temp.d2id('invoice:'||l.case_key),
 'PAY-'||upper(replace(l.case_key,'-','')),s.price,'CASH','SUCCESS',pg_temp.d2_now()-interval '10 minutes',
 'Giao dịch tạo tải trình diễn',CASE WHEN pg_temp.d2_now()::time<time '12:00'
 THEN '90000013-6666-6666-6666-666666666666'::uuid ELSE '91000000-0000-0000-0000-000000000013'::uuid END,
 pg_temp.d2_now()-interval '10 minutes',pg_temp.d2_now()-interval '10 minutes',false
FROM demo2_load_cases l JOIN medical_service s ON s.service_id=l.service_id;
INSERT INTO queue_ticket(ticket_id,visit_id,department_id,work_date,queue_number,status,service_id,
 created_at,updated_at,deleted)
SELECT pg_temp.d2id('queue:'||case_key),pg_temp.d2id('visit:'||case_key),room_id,pg_temp.d2_date(),
 COALESCE((SELECT max(existing.queue_number) FROM queue_ticket existing
            WHERE existing.department_id=l.room_id AND existing.work_date=pg_temp.d2_date()),0)+ordinal,
 'WAITING',service_id,pg_temp.d2_now()-interval '8 minutes'+ordinal*interval '1 minute',
 pg_temp.d2_now(),false FROM demo2_load_cases l;

-- Outside opening hours the baseline same-day cases must not masquerade as an
-- active clinic. They remain visible as cancelled/skipped demonstration data.
DO $outside_hours$
BEGIN
 IF NOT pg_temp.d2_is_open(pg_temp.d2_now()) THEN
   UPDATE queue_ticket SET status='SKIPPED',updated_at=pg_temp.d2_now()
   WHERE work_date=pg_temp.d2_date() AND status NOT IN('DONE','SKIPPED');
   UPDATE test_request SET status='CANCELLED',updated_at=pg_temp.d2_now()
   WHERE queue_ticket_id IN(SELECT ticket_id FROM queue_ticket WHERE work_date=pg_temp.d2_date());
   UPDATE customer_visit v SET status=CASE WHEN EXISTS(
       SELECT 1 FROM medical_record r WHERE r.visit_id=v.visit_id AND r.status='COMPLETED')
       THEN 'COMPLETED' ELSE 'CANCELLED' END,
       check_out_time=COALESCE(check_out_time,pg_temp.d2_now()),updated_at=pg_temp.d2_now()
   WHERE v.check_in_time::date=pg_temp.d2_date() AND v.status IN('CHECKED_IN','IN_PROGRESS');
   UPDATE appointment SET status='CANCELLED',cancel_reason='Ngoài giờ hoạt động của phòng khám',
       updated_at=pg_temp.d2_now()
   WHERE scheduled_at::date=pg_temp.d2_date() AND status IN('PENDING','RESCHEDULED');
 END IF;
END
$outside_hours$;

-- ============================================================================
-- Assertions: any failure rolls back the data2 overlay
-- ============================================================================
DO $validate$
DECLARE bad text; int_delta int; sur_delta int; older_visits int; recent_visits int;
BEGIN
 IF (SELECT count(*) FROM demo2_customers)<>20 OR EXISTS(
   SELECT 1 FROM demo2_customers c JOIN profile p ON p.profile_id=c.patient_id
   WHERE p.phone<>'090910'||lpad(c.customer_no::text,4,'0'))
 THEN RAISE EXCEPTION 'Danh sách 20 tài khoản Customer không đúng dải 0909100001-0909100020'; END IF;

 IF EXISTS(
   SELECT d.department_id FROM department d
   LEFT JOIN staff_info s ON s.department_id=d.department_id AND s.system_role='DOCTOR' AND NOT s.deleted
   WHERE NOT d.deleted AND d.status='AVAILABLE' AND d.department_type IN('EXAMINATION','PARACLINICAL')
   GROUP BY d.department_id HAVING count(s.staff_id)<>1
 ) THEN RAISE EXCEPTION 'Mỗi phòng hoạt động phải có đúng một bác sĩ'; END IF;

 IF EXISTS(SELECT 1 FROM demo2_doctor_accounts m
   JOIN department d ON d.room_code=m.room_code
   JOIN staff_info s ON s.staff_id=m.staff_id
   WHERE s.department_id<>d.department_id OR d.head_doctor_id<>m.staff_id
      OR (d.specialization_id IS NOT NULL AND s.specialization_id IS DISTINCT FROM d.specialization_id)
      OR EXISTS(SELECT 1 FROM department_capability dc WHERE dc.department_id=d.department_id
          AND NOT EXISTS(SELECT 1 FROM staff_capability sc
             WHERE sc.staff_id=m.staff_id AND sc.capability_id=dc.capability_id)))
 THEN RAISE EXCEPTION 'Bác sĩ cố định không khớp phòng, chuyên khoa hoặc năng lực'; END IF;

 IF EXISTS(
   SELECT m.staff_id,c.work_date
   FROM demo2_doctor_accounts m CROSS JOIN demo2_calendar c
   LEFT JOIN staff_schedule ss ON ss.staff_id=m.staff_id AND ss.work_date=c.work_date
      AND NOT ss.deleted AND ss.status='SCHEDULED'
   WHERE EXTRACT(ISODOW FROM c.work_date) BETWEEN 1 AND 6
   GROUP BY m.staff_id,c.work_date HAVING count(DISTINCT ss.shift_id)<>2
 ) THEN RAISE EXCEPTION 'Bác sĩ cố định chưa có đủ hai ca từ thứ Hai đến thứ Bảy'; END IF;

 IF EXISTS(SELECT 1 FROM staff_schedule WHERE EXTRACT(ISODOW FROM work_date)=7 AND NOT deleted)
 OR EXISTS(SELECT 1 FROM appointment WHERE EXTRACT(ISODOW FROM scheduled_at)=7 AND status<>'CANCELLED' AND NOT deleted)
 OR EXISTS(SELECT 1 FROM customer_visit WHERE EXTRACT(ISODOW FROM check_in_time)=7 AND NOT deleted)
 OR EXISTS(SELECT 1 FROM queue_ticket WHERE EXTRACT(ISODOW FROM work_date)=7 AND NOT deleted)
 THEN RAISE EXCEPTION 'Chủ nhật vẫn còn lịch hoạt động'; END IF;

 IF EXISTS(SELECT 1 FROM demo2_doctor_accounts m WHERE NOT EXISTS(
      SELECT 1 FROM medical_record r WHERE r.doctor_id=m.staff_id AND r.status='COMPLETED' AND NOT r.deleted)
    AND NOT EXISTS(
      SELECT 1 FROM test_result r WHERE r.performed_by=m.staff_id AND r.verified_at IS NOT NULL AND NOT r.deleted))
 THEN RAISE EXCEPTION 'Bác sĩ cố định thiếu ca lịch sử hoàn thành'; END IF;

 IF NOT EXISTS(SELECT 1 FROM invoice WHERE status='CANCELLED' AND NOT deleted)
 OR NOT EXISTS(SELECT 1 FROM payment_transaction WHERE status='FAILED' AND NOT deleted)
 OR NOT EXISTS(SELECT 1 FROM payment_transaction WHERE status='CANCELLED' AND NOT deleted)
 THEN RAISE EXCEPTION 'Thiếu mẫu hóa đơn/giao dịch không thành công cho báo cáo'; END IF;

 IF (SELECT count(DISTINCT service_code_snapshot) FROM invoice_item ii JOIN invoice i ON i.invoice_id=ii.invoice_id
     WHERE i.issue_date::date BETWEEN (date_trunc('month',pg_temp.d2_date())-interval '2 months')::date
                                  AND pg_temp.d2_date()) < 15
 THEN RAISE EXCEPTION 'Dữ liệu báo cáo chưa đủ 15 dịch vụ'; END IF;

 IF EXISTS(SELECT 1 FROM demo2_calendar c WHERE c.work_date<pg_temp.d2_date()
   AND EXTRACT(ISODOW FROM c.work_date) BETWEEN 1 AND 6
   AND NOT EXISTS(SELECT 1 FROM customer_visit v WHERE v.check_in_time::date=c.work_date))
 THEN RAISE EXCEPTION 'Có ngày làm việc trong kỳ báo cáo chưa có lượt khám'; END IF;

 SELECT count(*) INTO older_visits FROM customer_visit
 WHERE check_in_time::date>=(date_trunc('month',pg_temp.d2_date())-interval '2 months')::date
   AND check_in_time::date<(date_trunc('month',pg_temp.d2_date())-interval '1 month')::date;
 SELECT count(*) INTO recent_visits FROM customer_visit
 WHERE check_in_time::date>=(date_trunc('month',pg_temp.d2_date())-interval '1 month')::date
   AND check_in_time::date<date_trunc('month',pg_temp.d2_date())::date;
 IF older_visits=0 OR recent_visits::numeric/older_visits NOT BETWEEN 1.08 AND 1.12
 THEN RAISE EXCEPTION 'Tăng trưởng lượt tháng gần nhất không trong khoảng 8-12%%: % -> %',older_visits,recent_visits; END IF;

 IF EXISTS(SELECT 1 FROM account a JOIN profile p ON p.account_id=a.account_id
   WHERE a.role='CUSTOMER' AND NOT EXISTS(
    SELECT 1 FROM customer_visit v JOIN medical_record r ON r.visit_id=v.visit_id
    JOIN invoice i ON i.visit_id=v.visit_id JOIN payment_transaction t ON t.invoice_id=i.invoice_id
    WHERE v.customer_id=p.profile_id AND v.status='COMPLETED' AND r.status='COMPLETED'
      AND i.status='PAID' AND t.status='SUCCESS' AND r.rating_score IS NOT NULL))
 THEN RAISE EXCEPTION 'Customer thiếu bệnh án/thanh toán/đánh giá lịch sử'; END IF;

 IF EXISTS(SELECT 1 FROM invoice i WHERE i.subtotal<>(SELECT COALESCE(sum(ii.unit_price*ii.quantity),0)
    FROM invoice_item ii WHERE ii.invoice_id=i.invoice_id)
    OR i.total_amount<>i.subtotal-i.discount+i.tax
    OR (i.status='PAID' AND i.paid_amount<>(SELECT COALESCE(sum(t.amount),0)
       FROM payment_transaction t WHERE t.invoice_id=i.invoice_id AND t.status='SUCCESS')))
 THEN RAISE EXCEPTION 'Đối soát hóa đơn/giao dịch thất bại'; END IF;

 IF EXISTS(SELECT 1 FROM membership_card c WHERE c.balance<>(5000000-COALESCE((
      SELECT sum(t.amount) FROM payment_transaction t JOIN invoice i ON i.invoice_id=t.invoice_id
      WHERE i.customer_id=c.owner_profile_id AND t.status='SUCCESS' AND t.payment_method='MEMBERSHIP_CARD'),0)))
 OR EXISTS(SELECT 1 FROM membership_card_ledger l
      WHERE l.type='PAYMENT' AND (l.invoice_id IS NULL OR l.payment_transaction_id IS NULL))
 THEN RAISE EXCEPTION 'Đối soát thẻ CareS hoặc ledger thất bại'; END IF;

 IF pg_temp.d2_is_open(pg_temp.d2_now()) THEN
   SELECT (SELECT count(*) FROM queue_ticket WHERE department_id='aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
          AND work_date=pg_temp.d2_date() AND status NOT IN('DONE','SKIPPED'))-
          (SELECT count(*) FROM queue_ticket WHERE department_id='77777777-7777-7777-7777-777777777777'
          AND work_date=pg_temp.d2_date() AND status NOT IN('DONE','SKIPPED')) INTO int_delta;
   SELECT (SELECT count(*) FROM queue_ticket WHERE department_id='99999999-9999-9999-9999-999999999999'
          AND work_date=pg_temp.d2_date() AND status NOT IN('DONE','SKIPPED'))-
          (SELECT count(*) FROM queue_ticket WHERE department_id='33333333-3333-3333-3333-333333333333'
          AND work_date=pg_temp.d2_date() AND status NOT IN('DONE','SKIPPED')) INTO sur_delta;
   IF int_delta<>4 OR sur_delta<>4 THEN
     RAISE EXCEPTION 'Chênh lệch tải không đúng: Nội %, Ngoại %',int_delta,sur_delta;
   END IF;
 ELSE
   IF EXISTS(SELECT 1 FROM queue_ticket WHERE work_date=pg_temp.d2_date() AND status NOT IN('DONE','SKIPPED'))
   THEN RAISE EXCEPTION 'Ngoài giờ vẫn còn hàng chờ hoạt động'; END IF;
 END IF;

 IF EXISTS(SELECT 1 FROM feedback_target) THEN RAISE EXCEPTION 'data2 không dùng FeedbackTarget cũ'; END IF;
END
$validate$;

COMMIT;

\echo 'data2.sql completed successfully.'
