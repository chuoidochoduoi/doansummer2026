import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const path = String.raw`D:\gitlap\doAnSummer2026\outputs\019fc0c3-dee6-7a11-9b42-26e3805728ed\CareS_Report_5.4_Acceptance_Test_v2.xlsx`;
const dir = String.raw`D:\gitlap\doAnSummer2026\outputs\019fc0c3-dee6-7a11-9b42-26e3805728ed`;
const wb = await SpreadsheetFile.importXlsx(await FileBlob.load(path));
wb.recalculate();
for (const [range, name] of [["A1:H35","top"],["A34:H70","middle"],["A95:H129","bottom"]]) {
  const img = await wb.render({ sheetName: "Acceptance Test", range, scale: 1.1, format: "png" });
  await fs.writeFile(`${dir}\\preview-Acceptance-v2-${name}.png`, new Uint8Array(await img.arrayBuffer()));
}
const stats = await wb.inspect({ kind: "table", range: "Test Statistics!B10:H16", include: "values,formulas", tableMaxRows: 10, tableMaxCols: 8, maxChars: 3000 });
console.log(stats.ndjson);
const errors = await wb.inspect({ kind: "match", searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A|#NUM!|#NULL!|#SPILL!|#CALC!", options: { useRegex: true, maxResults: 100 }, summary: "v2 formula error scan" });
console.log(errors.ndjson);
