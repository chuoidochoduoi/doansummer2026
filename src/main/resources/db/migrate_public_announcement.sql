-- Run once on an existing production database before deploying the version
-- that contains PublicAnnouncement. Production uses ddl-auto=validate.
CREATE TABLE IF NOT EXISTS public_announcement (
    announcement_id UUID PRIMARY KEY,
    created_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    title VARCHAR(150) NOT NULL,
    content TEXT NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    starts_at TIMESTAMP(6) WITHOUT TIME ZONE,
    ends_at TIMESTAMP(6) WITHOUT TIME ZONE,
    created_by_account_id UUID,
    CONSTRAINT public_announcement_period_check
        CHECK (ends_at IS NULL OR starts_at IS NULL OR ends_at > starts_at)
);

CREATE INDEX IF NOT EXISTS idx_public_announcement_visible
    ON public_announcement (published, starts_at, ends_at)
    WHERE deleted = FALSE;
