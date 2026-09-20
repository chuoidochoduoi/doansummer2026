-- CareS uses fixed clinical-field conventions in medical_record.specialty_data
-- and fixed laboratory-result conventions in test_result.result_data.
-- Dynamic clinical form templates and version history are no longer part of the product scope.

ALTER TABLE IF EXISTS medical_record
    DROP CONSTRAINT IF EXISTS fk_medical_record_form_template_version;
ALTER TABLE IF EXISTS test_result
    DROP CONSTRAINT IF EXISTS fk_test_result_form_template_version;

ALTER TABLE IF EXISTS medical_record
    DROP COLUMN IF EXISTS form_template_version_id;
ALTER TABLE IF EXISTS test_result
    DROP COLUMN IF EXISTS form_template_version_id;

DROP TABLE IF EXISTS medical_service_form_template;
DROP TABLE IF EXISTS clinical_form_template_version;
DROP TABLE IF EXISTS clinical_form_template;
