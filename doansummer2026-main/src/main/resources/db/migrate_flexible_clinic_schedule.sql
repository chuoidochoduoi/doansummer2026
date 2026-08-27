-- Manual production migration for flexible clinic scheduling.
-- Development uses ddl-auto=update; production uses ddl-auto=validate and must run this file first.

CREATE TABLE IF NOT EXISTS shift_version (
    shift_version_id uuid PRIMARY KEY,
    shift_id uuid NOT NULL REFERENCES shift_config(shift_id),
    start_time time NOT NULL,
    end_time time NOT NULL,
    effective_from date NOT NULL,
    effective_to date,
    change_reason varchar(500) NOT NULL,
    created_by uuid REFERENCES account(account_id),
    created_at timestamp(6) without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp(6) without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted boolean NOT NULL DEFAULT false,
    CONSTRAINT ck_shift_version_time CHECK (start_time < end_time),
    CONSTRAINT ck_shift_version_dates CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

CREATE INDEX IF NOT EXISTS idx_shift_version_effective
    ON shift_version(shift_id, effective_from, effective_to);

CREATE TABLE IF NOT EXISTS clinic_schedule_exception (
    exception_id uuid PRIMARY KEY,
    work_date date NOT NULL,
    shift_id uuid REFERENCES shift_config(shift_id),
    exception_type varchar(30) NOT NULL,
    special_start_time time,
    special_end_time time,
    reason varchar(500) NOT NULL,
    created_by uuid REFERENCES account(account_id),
    created_at timestamp(6) without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp(6) without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted boolean NOT NULL DEFAULT false,
    CONSTRAINT ck_clinic_exception_type CHECK
        (exception_type IN ('CLOSED_DAY', 'SHIFT_OFF', 'SPECIAL_HOURS')),
    CONSTRAINT ck_clinic_exception_shape CHECK (
        (exception_type = 'CLOSED_DAY' AND shift_id IS NULL
            AND special_start_time IS NULL AND special_end_time IS NULL)
        OR (exception_type = 'SHIFT_OFF' AND shift_id IS NOT NULL
            AND special_start_time IS NULL AND special_end_time IS NULL)
        OR (exception_type = 'SPECIAL_HOURS' AND shift_id IS NOT NULL
            AND special_start_time IS NOT NULL AND special_end_time IS NOT NULL
            AND special_start_time < special_end_time)
    )
);

CREATE INDEX IF NOT EXISTS idx_clinic_exception_date
    ON clinic_schedule_exception(work_date);

ALTER TABLE appointment ADD COLUMN IF NOT EXISTS shift_version_id uuid;
ALTER TABLE staff_schedule ADD COLUMN IF NOT EXISTS shift_version_id uuid;
ALTER TABLE staff_schedule ADD COLUMN IF NOT EXISTS actual_start_time time;
ALTER TABLE staff_schedule ADD COLUMN IF NOT EXISTS actual_end_time time;

DO $$ BEGIN
    ALTER TABLE appointment ADD CONSTRAINT fk_appointment_shift_version
        FOREIGN KEY (shift_version_id) REFERENCES shift_version(shift_version_id);
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    ALTER TABLE staff_schedule ADD CONSTRAINT fk_staff_schedule_shift_version
        FOREIGN KEY (shift_version_id) REFERENCES shift_version(shift_version_id);
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

INSERT INTO shift_version (
    shift_version_id, shift_id, start_time, end_time, effective_from,
    change_reason, created_at, updated_at, deleted
)
SELECT gen_random_uuid(), s.shift_id, s.start_time::time, s.end_time::time,
       DATE '1970-01-01', 'Initial version migrated from shift configuration',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false
FROM shift_config s
WHERE s.deleted = false
  AND NOT EXISTS (SELECT 1 FROM shift_version v WHERE v.shift_id = s.shift_id AND v.deleted = false);

UPDATE staff_schedule ss
SET shift_version_id = v.shift_version_id,
    actual_start_time = v.start_time,
    actual_end_time = v.end_time
FROM shift_version v
WHERE ss.shift_version_id IS NULL
  AND ss.shift_id = v.shift_id
  AND v.deleted = false
  AND v.effective_from <= ss.work_date
  AND (v.effective_to IS NULL OR v.effective_to >= ss.work_date);

UPDATE appointment a
SET shift_version_id = v.shift_version_id
FROM shift_version v
JOIN shift_config s ON s.shift_id = v.shift_id
WHERE a.shift_version_id IS NULL
  AND a.shift_name = s.name
  AND v.deleted = false
  AND v.effective_from <= a.scheduled_at::date
  AND (v.effective_to IS NULL OR v.effective_to >= a.scheduled_at::date);
