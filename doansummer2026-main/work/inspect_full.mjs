import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";
const p = "D:/gitlap/doAnSummer2026/outputs/Report5_IntegrationTest_Clinic_Full.xlsx";
const wb = await SpreadsheetFile.importXlsx(await FileBlob.load(p));
for (const sheetName of ["Cover", "Test Cases", "Test Statistics"]) {
  const out = await wb.inspect({kind:"table", sheetId:sheetName, range:"A1:N25", include:"values,formulas", tableMaxRows:25, tableMaxCols:14, maxChars:12000});
  console.log("###", sheetName, "\n", out.ndjson);
}
