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

CREATE INDEX IF NOT EXISTS idx_clinical_template_effective
    ON clinical_form_template_version(template_id, status, effective_from, version_no DESC);

-- CareS stores one editable result directly on test_result. Revision and
-- attachment tables were intentionally removed from this deployment scope.
