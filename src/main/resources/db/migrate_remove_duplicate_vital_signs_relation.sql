-- Keep the single, canonical 1:1 relationship:
-- vital_signs.medical_record_id (UNIQUE, NOT NULL) -> medical_record.record_id.
-- This script is safe to run once on existing databases. Hibernate's ddl-auto=update
-- does not remove obsolete columns automatically.
ALTER TABLE medical_record DROP COLUMN IF EXISTS vital_signs_id;
