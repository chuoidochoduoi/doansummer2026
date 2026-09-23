const fs = require('fs');
const path = require('path');

const dataSqlPath = path.join(__dirname, 'src/main/resources/data.sql');
let content = fs.readFileSync(dataSqlPath, 'utf8');

const updates = [
  {
    tag: 'cbc',
    modifications: {
      'rbc': { min: 0.5, max: 10, referenceRanges: [{ minAge: 0, sex: "MALE", low: 4.3, high: 5.8 }, { minAge: 0, sex: "FEMALE", low: 3.9, high: 5.2 }] },
      'hgb': { min: 50, max: 200, referenceRanges: [{ minAge: 0, sex: "MALE", low: 130, high: 170 }, { minAge: 0, sex: "FEMALE", low: 120, high: 150 }] },
      'hct': { min: 10, max: 70, referenceRanges: [{ minAge: 0, sex: "MALE", low: 39, high: 49 }, { minAge: 0, sex: "FEMALE", low: 33, high: 43 }] },
      'mcv': { min: 50, max: 120, referenceRanges: [{ low: 80, high: 100 }] },
      'wbc': { min: 0, max: 50, referenceRanges: [{ low: 4.0, high: 10.0 }] },
      'neutPercent': { min: 0, max: 100, referenceRanges: [{ low: 40, high: 75 }] },
      'lymphPercent': { min: 0, max: 100, referenceRanges: [{ low: 20, high: 45 }] },
      'plt': { min: 0, max: 1000, referenceRanges: [{ low: 150, high: 400 }] }
    }
  },
  {
    tag: 'bio',
    modifications: {
      'hba1c': { min: 3, max: 20, referenceRanges: [{ low: 4.0, high: 5.6 }] },
      'totalCholesterol': { min: 0, max: 20, referenceRanges: [{ high: 5.1 }] },
      'triglyceride': { min: 0, max: 20, referenceRanges: [{ high: 1.7 }] },
      'hdlC': { min: 0, max: 5, referenceRanges: [{ minAge: 0, sex: "MALE", low: 1.03 }, { minAge: 0, sex: "FEMALE", low: 1.29 }] },
      'ldlC': { min: 0, max: 10, referenceRanges: [{ high: 3.3 }] },
      'acidUric': { min: 0, max: 1000, referenceRanges: [{ minAge: 0, sex: "MALE", low: 202, high: 416 }, { minAge: 0, sex: "FEMALE", low: 142, high: 339 }] },
      'totalProtein': { min: 0, max: 150, referenceRanges: [{ low: 66, high: 87 }] }
    }
  },
  {
    tag: 'liver',
    modifications: {
      'ast': { min: 0, max: 1000, referenceRanges: [{ minAge: 0, sex: "MALE", high: 37 }, { minAge: 0, sex: "FEMALE", high: 31 }] },
      'alt': { min: 0, max: 1000, referenceRanges: [{ minAge: 0, sex: "MALE", high: 41 }, { minAge: 0, sex: "FEMALE", high: 31 }] },
      'alp': { min: 0, max: 1000, referenceRanges: [{ low: 40, high: 129 }] },
      'ggt': { min: 0, max: 1000, referenceRanges: [{ minAge: 0, sex: "MALE", high: 61 }, { minAge: 0, sex: "FEMALE", high: 36 }] },
      'bilirubinTotal': { min: 0, max: 200, referenceRanges: [{ high: 21 }] },
      'bilirubinDirect': { min: 0, max: 100, referenceRanges: [{ high: 5 }] },
      'albumin': { min: 0, max: 100, referenceRanges: [{ low: 35, high: 52 }] }
    }
  },
  {
    tag: 'renal',
    modifications: {
      'urea': { min: 0, max: 50, referenceRanges: [{ low: 2.5, high: 7.1 }] },
      'creatinine': { min: 0, max: 1000, referenceRanges: [{ minAge: 0, sex: "MALE", low: 62, high: 106 }, { minAge: 0, sex: "FEMALE", low: 44, high: 80 }] },
      'sodium': { min: 0, max: 200, referenceRanges: [{ low: 135, high: 145 }] },
      'potassium': { min: 0, max: 10, referenceRanges: [{ low: 3.5, high: 5.1 }] },
      'chloride': { min: 0, max: 200, referenceRanges: [{ low: 98, high: 107 }] },
      'calcium': { min: 0, max: 5, referenceRanges: [{ low: 2.15, high: 2.55 }] }
    }
  },
  {
    tag: 'urine',
    modifications: {
      'specificGravity': { min: 1.000, max: 1.050, referenceRanges: [{ low: 1.010, high: 1.025 }] },
      'ph': { min: 4.0, max: 9.0, referenceRanges: [{ low: 4.8, high: 7.4 }] },
      'leukocyteEsterase': { referenceRanges: [{ text: "Negative" }] },
      'nitrite': { referenceRanges: [{ text: "Negative" }] },
      'protein': { referenceRanges: [{ text: "Negative" }] },
      'urineGlucose': { referenceRanges: [{ text: "Negative" }] },
      'ketone': { referenceRanges: [{ text: "Negative" }] },
      'blood': { referenceRanges: [{ text: "Negative" }] }
    }
  }
];

let sqlUpdateStatements = '';

for (const update of updates) {
  const regex = new RegExp(`\\$${update.tag}\\$([\\s\\S]*?)\\$${update.tag}\\$`);
  const match = content.match(regex);
  if (match) {
    let jsonStr = match[1];
    let jsonObj = JSON.parse(jsonStr);
    
    jsonObj.fields.forEach(field => {
      if (update.modifications[field.key]) {
        Object.assign(field, update.modifications[field.key]);
      }
    });

    const newJsonStr = JSON.stringify(jsonObj, null, 2);
    content = content.replace(match[0], `$${update.tag}$${newJsonStr}$${update.tag}$`);
    
    // Add to sql update statements
    sqlUpdateStatements += `UPDATE clinical_form_template_version SET schema_json = '${newJsonStr.replace(/'/g, "''")}'::jsonb WHERE schema_json->'fields' @> '[{"key":"${jsonObj.fields[0].key}"}]'::jsonb;\n`;
  }
}

fs.writeFileSync(dataSqlPath, content, 'utf8');
fs.writeFileSync(path.join(__dirname, 'sync_limits.sql'), sqlUpdateStatements, 'utf8');
console.log('Updated data.sql and created sync_limits.sql');
