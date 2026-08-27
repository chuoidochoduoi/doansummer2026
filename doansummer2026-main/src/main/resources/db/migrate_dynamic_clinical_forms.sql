-- Run once on an existing PostgreSQL database before deploying dynamic clinical forms.
-- The script is idempotent and does not alter the finalized medical_service catalogue.

CREATE TABLE IF NOT EXISTS clinical_form_template (
    template_id UUID PRIMARY KEY,
    created_at TIMESTAMP(6), updated_at TIMESTAMP(6), deleted BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    context VARCHAR(30) NOT NULL,
    description VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS clinical_form_template_version (
    version_id UUID PRIMARY KEY,
    created_at TIMESTAMP(6), updated_at TIMESTAMP(6), deleted BOOLEAN NOT NULL DEFAULT FALSE,
    template_id UUID NOT NULL REFERENCES clinical_form_template(template_id),
    version_no INTEGER NOT NULL,
    schema_json JSONB NOT NULL,
    status VARCHAR(20) NOT NULL,
    change_reason VARCHAR(1000) NOT NULL,
    effective_from DATE,
    created_by UUID REFERENCES staff_info(staff_id),
    published_by UUID REFERENCES staff_info(staff_id),
    published_at TIMESTAMP(6),
    CONSTRAINT uk_clinical_template_version UNIQUE (template_id, version_no),
    CONSTRAINT ck_clinical_template_status CHECK (status IN ('DRAFT','PUBLISHED','RETIRED'))
);

CREATE TABLE IF NOT EXISTS medical_service_form_template (
    binding_id UUID PRIMARY KEY,
    created_at TIMESTAMP(6), updated_at TIMESTAMP(6), deleted BOOLEAN NOT NULL DEFAULT FALSE,
    service_id UUID NOT NULL REFERENCES medical_service(service_id),
    template_id UUID NOT NULL REFERENCES clinical_form_template(template_id),
    CONSTRAINT uk_service_form_template_service UNIQUE (service_id)
);

ALTER TABLE medical_record ADD COLUMN IF NOT EXISTS specialty_data JSONB;
ALTER TABLE medical_record ADD COLUMN IF NOT EXISTS form_template_version_id UUID;
ALTER TABLE medical_record DROP CONSTRAINT IF EXISTS fk_medical_record_form_template_version;
ALTER TABLE medical_record ADD CONSTRAINT fk_medical_record_form_template_version
    FOREIGN KEY (form_template_version_id) REFERENCES clinical_form_template_version(version_id);

ALTER TABLE test_result ADD COLUMN IF NOT EXISTS result_data JSONB;
ALTER TABLE test_result ADD COLUMN IF NOT EXISTS form_template_version_id UUID;
ALTER TABLE test_result DROP CONSTRAINT IF EXISTS fk_test_result_form_template_version;
ALTER TABLE test_result ADD CONSTRAINT fk_test_result_form_template_version
    FOREIGN KEY (form_template_version_id) REFERENCES clinical_form_template_version(version_id);

CREATE TABLE IF NOT EXISTS test_result_revision (
    revision_id UUID PRIMARY KEY,
    created_at TIMESTAMP(6), updated_at TIMESTAMP(6), deleted BOOLEAN NOT NULL DEFAULT FALSE,
    result_id UUID NOT NULL REFERENCES test_result(result_id),
    revision_no INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    result_data JSONB,
    conclusion TEXT,
    template_version_id UUID REFERENCES clinical_form_template_version(version_id),
    amendment_reason VARCHAR(1000),
    entered_by UUID REFERENCES staff_info(staff_id),
    signed_by UUID REFERENCES staff_info(staff_id),
    signed_at TIMESTAMP(6),
    CONSTRAINT uk_test_result_revision UNIQUE (result_id, revision_no),
    CONSTRAINT ck_test_result_revision_status CHECK (status IN ('DRAFT','SIGNED','SUPERSEDED'))
);

CREATE TABLE IF NOT EXISTS test_result_attachment (
    attachment_id UUID PRIMARY KEY,
    created_at TIMESTAMP(6), updated_at TIMESTAMP(6), deleted BOOLEAN NOT NULL DEFAULT FALSE,
    revision_id UUID NOT NULL REFERENCES test_result_revision(revision_id),
    storage_path VARCHAR(500) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    display_order INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_clinical_template_effective
    ON clinical_form_template_version(template_id, status, effective_from, version_no DESC);
CREATE INDEX IF NOT EXISTS idx_test_result_revision_result
    ON test_result_revision(result_id, revision_no DESC);
CREATE INDEX IF NOT EXISTS idx_test_result_attachment_revision
    ON test_result_attachment(revision_id, display_order);

-- Existing results become immutable signed revision 1. Re-running does not duplicate them.
INSERT INTO test_result_revision (
    revision_id, created_at, updated_at, deleted, result_id, revision_no, status,
    result_data, conclusion, template_version_id, entered_by, signed_by, signed_at
)
SELECT gen_random_uuid(), COALESCE(tr.created_at, CURRENT_TIMESTAMP), COALESCE(tr.updated_at, CURRENT_TIMESTAMP),
       FALSE, tr.result_id, 1,
       CASE WHEN tr.verified_at IS NULL THEN 'DRAFT' ELSE 'SIGNED' END,
       tr.result_data, tr.conclusion, tr.form_template_version_id,
       tr.performed_by, tr.verified_by, tr.verified_at
FROM test_result tr
WHERE NOT EXISTS (SELECT 1 FROM test_result_revision rev WHERE rev.result_id = tr.result_id);

-- Keep image_url compatible while registering it as the first attachment of revision 1.
INSERT INTO test_result_attachment (
    attachment_id, created_at, updated_at, deleted, revision_id, storage_path,
    original_name, content_type, file_size, display_order
)
SELECT gen_random_uuid(), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE, rev.revision_id,
       tr.image_url,
       regexp_replace(tr.image_url, '^.*/', ''),
       CASE WHEN lower(tr.image_url) ~ '\.(jpg|jpeg)$' THEN 'image/jpeg'
            WHEN lower(tr.image_url) ~ '\.png$' THEN 'image/png'
            WHEN lower(tr.image_url) ~ '\.webp$' THEN 'image/webp'
            ELSE 'application/pdf' END,
       0, 0
FROM test_result tr
JOIN test_result_revision rev ON rev.result_id = tr.result_id AND rev.revision_no = 1
WHERE tr.image_url IS NOT NULL AND tr.image_url <> ''
  AND NOT EXISTS (SELECT 1 FROM test_result_attachment a WHERE a.revision_id = rev.revision_id);
