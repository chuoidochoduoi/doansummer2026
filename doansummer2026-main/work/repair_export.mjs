import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const input = "D:/gitlap/doAnSummer2026/work/preview_no_logo.xlsx";
const output = "D:/gitlap/doAnSummer2026/outputs/INTEGRATION_TEST_PHONG_KHAM_MO_KHONG_LOI.xlsx";
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(input));
const sheets = await workbook.inspect({ kind: "sheet", include: "id,name", maxChars: 12000 });
const errors = await workbook.inspect({
  kind: "match",
  searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A|Unit Test",
  options: { useRegex: true, maxResults: 200 },
  maxChars: 12000,
});
const preview = await workbook.render({ sheetName: "Test Cases", range: "A1:N18", scale: 1, format: "png" });
await fs.writeFile("D:/gitlap/doAnSummer2026/work/repaired_preview.png", new Uint8Array(await preview.arrayBuffer()));
const result = await SpreadsheetFile.exportXlsx(workbook);
await result.save(output);
console.log(sheets.ndjson);
console.log(errors.ndjson);
console.log(JSON.stringify({ output }));
