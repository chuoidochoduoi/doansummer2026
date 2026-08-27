-- Sửa check constraint cho table medical_record
-- Chạy lệnh này để cập nhật constraint cho phép các status mới

ALTER TABLE medical_record DROP CONSTRAINT IF EXISTS medical_record_status_check;
ALTER TABLE medical_record ADD CONSTRAINT medical_record_status_check
CHECK (status IN ('IN_PROGRESS', 'DRAFT', 'COMPLETED'));