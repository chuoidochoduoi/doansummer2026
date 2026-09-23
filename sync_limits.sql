UPDATE clinical_form_template_version SET schema_json = '{
  "fields": [
    {
      "key": "rbc",
      "label": "Số lượng hồng cầu (RBC)",
      "type": "NUMBER",
      "unit": "10^12/L",
      "requiredOnSign": true,
      "min": 0.5,
      "max": 10,
      "referenceRanges": [
        {
          "minAge": 0,
          "sex": "MALE",
          "low": 4.3,
          "high": 5.8
        },
        {
          "minAge": 0,
          "sex": "FEMALE",
          "low": 3.9,
          "high": 5.2
        }
      ]
    },
    {
      "key": "hgb",
      "label": "Huyết sắc tố (HGB)",
      "type": "NUMBER",
      "unit": "g/L",
      "requiredOnSign": true,
      "min": 50,
      "max": 200,
      "referenceRanges": [
        {
          "minAge": 0,
          "sex": "MALE",
          "low": 130,
          "high": 170
        },
        {
          "minAge": 0,
          "sex": "FEMALE",
          "low": 120,
          "high": 150
        }
      ]
    },
    {
      "key": "hct",
      "label": "Hematocrit (HCT)",
      "type": "NUMBER",
      "unit": "%",
      "requiredOnSign": true,
      "min": 10,
      "max": 70,
      "referenceRanges": [
        {
          "minAge": 0,
          "sex": "MALE",
          "low": 39,
          "high": 49
        },
        {
          "minAge": 0,
          "sex": "FEMALE",
          "low": 33,
          "high": 43
        }
      ]
    },
    {
      "key": "mcv",
      "label": "MCV",
      "type": "NUMBER",
      "unit": "fL",
      "requiredOnSign": true,
      "min": 50,
      "max": 120,
      "referenceRanges": [
        {
          "low": 80,
          "high": 100
        }
      ]
    },
    {
      "key": "wbc",
      "label": "Số lượng bạch cầu (WBC)",
      "type": "NUMBER",
      "unit": "10^9/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 50,
      "referenceRanges": [
        {
          "low": 4,
          "high": 10
        }
      ]
    },
    {
      "key": "neutPercent",
      "label": "Bạch cầu trung tính (%)",
      "type": "NUMBER",
      "unit": "%",
      "requiredOnSign": true,
      "min": 0,
      "max": 100,
      "referenceRanges": [
        {
          "low": 40,
          "high": 75
        }
      ]
    },
    {
      "key": "lymphPercent",
      "label": "Bạch cầu lympho (%)",
      "type": "NUMBER",
      "unit": "%",
      "requiredOnSign": true,
      "min": 0,
      "max": 100,
      "referenceRanges": [
        {
          "low": 20,
          "high": 45
        }
      ]
    },
    {
      "key": "plt",
      "label": "Số lượng tiểu cầu (PLT)",
      "type": "NUMBER",
      "unit": "10^9/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 1000,
      "referenceRanges": [
        {
          "low": 150,
          "high": 400
        }
      ]
    }
  ]
}'::jsonb WHERE schema_json->'fields' @> '[{"key":"rbc"}]'::jsonb;
UPDATE clinical_form_template_version SET schema_json = '{
  "fields": [
    {
      "key": "hba1c",
      "label": "Hemoglobin A1c",
      "type": "NUMBER",
      "unit": "%",
      "requiredOnSign": true,
      "min": 3,
      "max": 20,
      "referenceRanges": [
        {
          "low": 4,
          "high": 5.6
        }
      ]
    },
    {
      "key": "totalCholesterol",
      "label": "Cholesterol toàn phần",
      "type": "NUMBER",
      "unit": "mmol/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 20,
      "referenceRanges": [
        {
          "high": 5.1
        }
      ]
    },
    {
      "key": "triglyceride",
      "label": "Triglyceride",
      "type": "NUMBER",
      "unit": "mmol/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 20,
      "referenceRanges": [
        {
          "high": 1.7
        }
      ]
    },
    {
      "key": "hdlC",
      "label": "HDL Cholesterol",
      "type": "NUMBER",
      "unit": "mmol/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 5,
      "referenceRanges": [
        {
          "minAge": 0,
          "sex": "MALE",
          "low": 1.03
        },
        {
          "minAge": 0,
          "sex": "FEMALE",
          "low": 1.29
        }
      ]
    },
    {
      "key": "ldlC",
      "label": "LDL Cholesterol",
      "type": "NUMBER",
      "unit": "mmol/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 10,
      "referenceRanges": [
        {
          "high": 3.3
        }
      ]
    },
    {
      "key": "acidUric",
      "label": "Acid uric",
      "type": "NUMBER",
      "unit": "µmol/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 1000,
      "referenceRanges": [
        {
          "minAge": 0,
          "sex": "MALE",
          "low": 202,
          "high": 416
        },
        {
          "minAge": 0,
          "sex": "FEMALE",
          "low": 142,
          "high": 339
        }
      ]
    },
    {
      "key": "totalProtein",
      "label": "Protein toàn phần",
      "type": "NUMBER",
      "unit": "g/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 150,
      "referenceRanges": [
        {
          "low": 66,
          "high": 87
        }
      ]
    }
  ]
}'::jsonb WHERE schema_json->'fields' @> '[{"key":"hba1c"}]'::jsonb;
UPDATE clinical_form_template_version SET schema_json = '{
  "fields": [
    {
      "key": "ast",
      "label": "AST",
      "type": "NUMBER",
      "unit": "U/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 1000,
      "referenceRanges": [
        {
          "minAge": 0,
          "sex": "MALE",
          "high": 37
        },
        {
          "minAge": 0,
          "sex": "FEMALE",
          "high": 31
        }
      ]
    },
    {
      "key": "alt",
      "label": "ALT",
      "type": "NUMBER",
      "unit": "U/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 1000,
      "referenceRanges": [
        {
          "minAge": 0,
          "sex": "MALE",
          "high": 41
        },
        {
          "minAge": 0,
          "sex": "FEMALE",
          "high": 31
        }
      ]
    },
    {
      "key": "alp",
      "label": "Alkaline phosphatase",
      "type": "NUMBER",
      "unit": "U/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 1000,
      "referenceRanges": [
        {
          "low": 40,
          "high": 129
        }
      ]
    },
    {
      "key": "ggt",
      "label": "GGT",
      "type": "NUMBER",
      "unit": "U/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 1000,
      "referenceRanges": [
        {
          "minAge": 0,
          "sex": "MALE",
          "high": 61
        },
        {
          "minAge": 0,
          "sex": "FEMALE",
          "high": 36
        }
      ]
    },
    {
      "key": "bilirubinTotal",
      "label": "Bilirubin toàn phần",
      "type": "NUMBER",
      "unit": "µmol/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 200,
      "referenceRanges": [
        {
          "high": 21
        }
      ]
    },
    {
      "key": "bilirubinDirect",
      "label": "Bilirubin trực tiếp",
      "type": "NUMBER",
      "unit": "µmol/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 100,
      "referenceRanges": [
        {
          "high": 5
        }
      ]
    },
    {
      "key": "albumin",
      "label": "Albumin",
      "type": "NUMBER",
      "unit": "g/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 100,
      "referenceRanges": [
        {
          "low": 35,
          "high": 52
        }
      ]
    }
  ]
}'::jsonb WHERE schema_json->'fields' @> '[{"key":"ast"}]'::jsonb;
UPDATE clinical_form_template_version SET schema_json = '{
  "fields": [
    {
      "key": "urea",
      "label": "Urea",
      "type": "NUMBER",
      "unit": "mmol/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 50,
      "referenceRanges": [
        {
          "low": 2.5,
          "high": 7.1
        }
      ]
    },
    {
      "key": "creatinine",
      "label": "Creatinine",
      "type": "NUMBER",
      "unit": "µmol/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 1000,
      "referenceRanges": [
        {
          "minAge": 0,
          "sex": "MALE",
          "low": 62,
          "high": 106
        },
        {
          "minAge": 0,
          "sex": "FEMALE",
          "low": 44,
          "high": 80
        }
      ]
    },
    {
      "key": "sodium",
      "label": "Sodium",
      "type": "NUMBER",
      "unit": "mmol/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 200,
      "referenceRanges": [
        {
          "low": 135,
          "high": 145
        }
      ]
    },
    {
      "key": "potassium",
      "label": "Potassium",
      "type": "NUMBER",
      "unit": "mmol/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 10,
      "referenceRanges": [
        {
          "low": 3.5,
          "high": 5.1
        }
      ]
    },
    {
      "key": "chloride",
      "label": "Chloride",
      "type": "NUMBER",
      "unit": "mmol/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 200,
      "referenceRanges": [
        {
          "low": 98,
          "high": 107
        }
      ]
    },
    {
      "key": "calcium",
      "label": "Calcium toàn phần",
      "type": "NUMBER",
      "unit": "mmol/L",
      "requiredOnSign": true,
      "min": 0,
      "max": 5,
      "referenceRanges": [
        {
          "low": 2.15,
          "high": 2.55
        }
      ]
    }
  ]
}'::jsonb WHERE schema_json->'fields' @> '[{"key":"urea"}]'::jsonb;
UPDATE clinical_form_template_version SET schema_json = '{
  "fields": [
    {
      "key": "specificGravity",
      "label": "Tỷ trọng nước tiểu",
      "type": "NUMBER",
      "requiredOnSign": true,
      "min": 1,
      "max": 1.05,
      "referenceRanges": [
        {
          "low": 1.01,
          "high": 1.025
        }
      ]
    },
    {
      "key": "ph",
      "label": "pH nước tiểu",
      "type": "NUMBER",
      "requiredOnSign": true,
      "min": 4,
      "max": 9,
      "referenceRanges": [
        {
          "low": 4.8,
          "high": 7.4
        }
      ]
    },
    {
      "key": "leukocyteEsterase",
      "label": "Leukocyte Esterase",
      "type": "TEXT",
      "requiredOnSign": true,
      "referenceRanges": [
        {
          "text": "Negative"
        }
      ]
    },
    {
      "key": "nitrite",
      "label": "Nitrite",
      "type": "TEXT",
      "requiredOnSign": true,
      "referenceRanges": [
        {
          "text": "Negative"
        }
      ]
    },
    {
      "key": "protein",
      "label": "Protein nước tiểu",
      "type": "TEXT",
      "requiredOnSign": true,
      "referenceRanges": [
        {
          "text": "Negative"
        }
      ]
    },
    {
      "key": "urineGlucose",
      "label": "Glucose nước tiểu",
      "type": "TEXT",
      "requiredOnSign": true,
      "referenceRanges": [
        {
          "text": "Negative"
        }
      ]
    },
    {
      "key": "ketone",
      "label": "Ketone",
      "type": "TEXT",
      "requiredOnSign": true,
      "referenceRanges": [
        {
          "text": "Negative"
        }
      ]
    },
    {
      "key": "blood",
      "label": "Máu/Hemoglobin nước tiểu",
      "type": "TEXT",
      "requiredOnSign": true,
      "referenceRanges": [
        {
          "text": "Negative"
        }
      ]
    }
  ]
}'::jsonb WHERE schema_json->'fields' @> '[{"key":"specificGravity"}]'::jsonb;
