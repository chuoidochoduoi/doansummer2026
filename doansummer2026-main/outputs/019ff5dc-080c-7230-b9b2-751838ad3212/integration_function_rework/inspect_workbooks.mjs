import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const files = [
  ["current", "D:/gitlap/doAnSummer2026/outputs/019ff5dc-080c-7230-b9b2-751838ad3212/INTEGRATION_TEST_FULL_SYSTEM_FLOWS.xlsx"],
  ["sample", "C:/Users/Administrator/Downloads/_Report5_IntegrationTest_Sample.xlsx"],
];

for (const [label, file] of files) {
  const wb = await SpreadsheetFile.importXlsx(await FileBlob.load(file));
  console.log(`FILE: ${label}`);
  console.log((await wb.inspect({kind:"sheet", include:"id,name", maxChars:12000})).ndjson);
  const firstNames = wb.worksheets.items.slice(0, 5).map(s => s.name);
  for (const name of firstNames) {
    console.log((await wb.inspect({kind:"table", sheetId:name, range:"A1:W25", tableMaxRows:25, tableMaxCols:23, tableMaxCellChars:140, maxChars:12000})).ndjson);
  }
  for (const name of firstNames) {
    const blob = await wb.render({sheetName:name, range:"A1:W25", scale:0.8, format:"png"});
    await fs.writeFile(`./${label}_${name.replace(/[^a-z0-9]/gi,"_")}.png`, new Uint8Array(await blob.arrayBuffer()));
  }
}
