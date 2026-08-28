-- Chay mot lan tren database hien co truoc khi deploy ban co rang buoc lich truc.
-- Giu ban tuy chinh/moi nhat va soft-delete cac ban trung cu.
-- Vai tro van hanh chung khong thuoc mot phong chuyen mon cu the.
UPDATE staff_info
SET department_id = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE deleted = false
  AND system_role IN ('ADMIN', 'CLINIC_MANAGER', 'RECEPTIONIST', 'CASHIER')
  AND department_id IS NOT NULL;

WITH ranked AS (
    SELECT schedule_id,
           ROW_NUMBER() OVER (
               PARTITION BY staff_id, work_date, shift_id
               ORDER BY is_custom DESC, updated_at DESC, created_at DESC
           ) AS rn
    FROM staff_schedule
    WHERE deleted = false
      AND shift_id IS NOT NULL
)
UPDATE staff_schedule schedule
SET deleted = true,
    updated_at = CURRENT_TIMESTAMP
FROM ranked duplicate
WHERE schedule.schedule_id = duplicate.schedule_id
  AND duplicate.rn > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uk_staff_schedule_active_slot
    ON staff_schedule (staff_id, work_date, shift_id)
    WHERE deleted = false AND shift_id IS NOT NULL;
