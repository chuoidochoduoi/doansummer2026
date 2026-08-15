import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const path = "D:/gitlap/doAnSummer2026/outputs/019ff5dc-080c-7230-b9b2-751838ad3212/INTEGRATION_TEST_FULL_SYSTEM_FLOWS.xlsx";
const wb = await SpreadsheetFile.importXlsx(await FileBlob.load(path));
const sheets = await wb.inspect({kind:"sheet",include:"id,name",maxChars:12000});
const pending = await wb.inspect({kind:"match",searchTerm:"^Pending$",options:{useRegex:true,maxResults:400},maxChars:30000});
const errors = await wb.inspect({kind:"match",searchTerm:"#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A",options:{useRegex:true,maxResults:300},maxChars:4000});
const sample = await wb.inspect({kind:"table",sheetId:"IT01-RegisterOTP",range:"A2:O19",include:"values,formulas",tableMaxRows:19,tableMaxCols:15,maxChars:20000});
let exactPending = 0, nonPendingRoundValues = [];
for (let i=1;i<=15;i++) {
  const prefix=`IT${String(i).padStart(2,"0")}-`;
  const line=sheets.ndjson.split(/\r?\n/).map(x=>{try{return JSON.parse(x)}catch{return null}}).find(x=>x?.name?.startsWith(prefix));
  const sheet=wb.worksheets.getItem(line.name);
  for (const range of ["F12:F19","I12:I19","L12:L19"]) for (const row of sheet.getRange(range).values) {
    if (row[0] === "Pending") exactPending++; else if (row[0] != null) nonPendingRoundValues.push(`${line.name}!${range}:${row[0]}`);
  }
}
console.log("SHEETS\n"+sheets.ndjson);
console.log("PENDING_COUNT="+(pending.ndjson.match(/\"value\":\"Pending\"/g)||[]).length);
console.log(`EXACT_ROUND_PENDING=${exactPending}; NON_PENDING_ROUND_VALUES=${nonPendingRoundValues.length}`);
console.log("ERRORS\n"+errors.ndjson);
console.log("SAMPLE\n"+sample.ndjson);
