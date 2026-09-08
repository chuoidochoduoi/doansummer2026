BEGIN;

ALTER TABLE membership_card
    ADD COLUMN IF NOT EXISTS benefit_starts_at timestamp;

UPDATE membership_card
SET benefit_starts_at = date_trunc('day', activated_at) + interval '1 day'
WHERE benefit_starts_at IS NULL
  AND activated_at IS NOT NULL
  AND benefit_expires_at IS NOT NULL;

COMMIT;
