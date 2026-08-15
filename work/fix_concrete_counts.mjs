import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";
const p="D:/gitlap/doAnSummer2026/outputs/INTEGRATION_TEST_CONCRETE_DATA_ENGLISH.xlsx";
const wb=await SpreadsheetFile.importXlsx(await FileBlob.load(p));
const names=(await wb.inspect({kind:"sheet",include:"id,name",maxChars:12000})).ndjson;
const featureNames=[...names.matchAll(/"name":"(IT\d\d-[^"]+)"/g)].map(x=>x[1]);
for(const name of featureNames){ const s=wb.worksheets.getItem(name); s.getRange("B4").values=[[6]]; }
const out=await SpreadsheetFile.exportXlsx(wb); await out.save(p);
console.log(JSON.stringify({featureSheets:featureNames.length}));
