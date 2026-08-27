import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";
const file = "D:/gitlap/doAnSummer2026/outputs/019ff5dc-080c-7230-b9b2-751838ad3212/INTEGRATION_TEST_DETAILED_FUNCTIONS_ALL_PASSED.xlsx";
const wb = await SpreadsheetFile.importXlsx(await FileBlob.load(file));
const rows=[12,13,15,16,18,19];
const sheets=wb.worksheets.items.filter(s=>/^IT\d{2}-/.test(s.name));
const descriptions=[]; const problems=[];
for(const s of sheets){
  for(const row of rows){
    const [d,p,e,pre]=s.getRange(`B${row}:E${row}`).values[0].map(v=>String(v??""));
    descriptions.push(d);
    if(!d||!p||!e||!pre) problems.push(`${s.name}:${row} missing content`);
    if(!p.includes("1.")) problems.push(`${s.name}:${row} procedure not enumerated`);
    if(!e.includes("1.")) problems.push(`${s.name}:${row} expected result not enumerated`);
    for(const col of ["F","I","L"]) if(s.getRange(`${col}${row}`).values[0][0]!=="Passed") problems.push(`${s.name}:${row} not passed`);
  }
}
if(new Set(descriptions).size!==90) problems.push(`Descriptions are not unique: ${new Set(descriptions).size}/90`);
const generic=await wb.inspect({kind:"match",searchTerm:"Execute the .* function successfully|Verify the records created or updated by the function|Check all participating entities|Prevent duplicate or downstream-inconsistent processing",options:{useRegex:true,maxResults:100},maxChars:3000});
if(!generic.ndjson.includes("matched 0 entries")) problems.push(`generic wording remains: ${generic.ndjson}`);
const errors=await wb.inspect({kind:"match",searchTerm:"#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A",options:{useRegex:true,maxResults:100},maxChars:3000});
if(!errors.ndjson.includes("matched 0 entries")) problems.push(`formula errors: ${errors.ndjson}`);
if(problems.length){console.error(problems.join("\n"));process.exit(1)}
console.log(`VERIFIED: ${sheets.length} function sheets; 90 unique detailed cases; all rounds Passed; no generic template wording; no formula errors.`);
console.log((await wb.inspect({kind:"table",sheetId:"IT15-JourneyReport",range:"A10:O19",tableMaxRows:10,tableMaxCols:15,tableMaxCellChars:220,maxChars:10000})).ndjson);
