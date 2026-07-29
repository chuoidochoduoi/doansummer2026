-- Migration: Add status column to medical_service, migrate from is_active, then drop is_active
-- Run this migration after code is updated to use status

-- Add status column (nullable first)
ALTER TABLE medical_service ADD COLUMN status VARCHAR(20);

-- Migrate data: true -> 'ACTIVE', false -> 'DRAFT' (assuming false means draft/not yet active)
UPDATE medical_service SET status = CASE
    WHEN is_active = true THEN 'ACTIVE'
    ELSE 'DRAFT'
END;

-- Set NOT NULL constraint
ALTER TABLE medical_service ALTER COLUMN status SET NOT NULL;

-- Drop old column (uncomment after verification)
-- ALTER TABLE medical_service DROP COLUMN is_active;