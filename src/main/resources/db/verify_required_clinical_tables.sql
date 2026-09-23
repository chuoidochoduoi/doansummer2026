-- Read-only guard for the laboratory form feature. Run after schema/seed changes.
DO $$
DECLARE
    required_table TEXT;
BEGIN
    FOREACH required_table IN ARRAY ARRAY[
        'clinical_form_template',
        'clinical_form_template_version',
        'medical_service_form_template',
        'test_request',
        'test_result'
    ]
    LOOP
        IF to_regclass('public.' || required_table) IS NULL THEN
            RAISE EXCEPTION 'Required CareS table is missing: %', required_table;
        END IF;
    END LOOP;
END
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM medical_service_form_template binding
        JOIN medical_service service ON service.service_id = binding.service_id
        JOIN clinical_form_template template ON template.template_id = binding.template_id
        JOIN clinical_form_template_version version ON version.template_id = template.template_id
        WHERE service.service_code = 'LAB-001'
          AND binding.deleted = FALSE
          AND template.deleted = FALSE
          AND template.active = TRUE
          AND version.deleted = FALSE
          AND version.status = 'PUBLISHED'
          AND jsonb_array_length(version.schema_json -> 'fields') = 24
    ) THEN
        RAISE EXCEPTION 'LAB-001 must be bound to a published 24-field clinical form';
    END IF;
END
$$;

SELECT service.service_code,
       template.code AS template_code,
       version.version_no,
       version.status,
       jsonb_array_length(version.schema_json -> 'fields') AS field_count
FROM medical_service_form_template binding
JOIN medical_service service ON service.service_id = binding.service_id
JOIN clinical_form_template template ON template.template_id = binding.template_id
JOIN clinical_form_template_version version ON version.template_id = template.template_id
ORDER BY service.service_code, version.version_no;
