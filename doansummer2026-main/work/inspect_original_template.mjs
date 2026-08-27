import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const source = "C:/Users/Administrator/Downloads/_Report5_IntegrationTest_Sample.xlsx";
const wb = await SpreadsheetFile.importXlsx(await FileBlob.load(source));
const summary = await wb.inspect({ kind: "workbook,sheet,table", maxChars: 20000, tableMaxRows: 22, tableMaxCols: 16, tableMaxCellChars: 100 });
console.log(summary.ndjson);
await fs.mkdir("D:/gitlap/doAnSummer2026/work/original_template_render", { recursive: true });
const sheets = await wb.inspect({ kind: "sheet", include: "id,name", maxChars: 10000 });
let index = 1;
for (const line of sheets.ndjson.split(/\r?\n/)) {
  try {
    const item = JSON.parse(line);
    if (!item.name) continue;
    const blob = await wb.render({ sheetName: item.name, autoCrop: "all", scale: 1, format: "png" });
    await fs.writeFile(`D:/gitlap/doAnSummer2026/work/original_template_render/${String(index).padStart(2,"0")}-${item.name.replace(/[^A-Za-z0-9_-]/g,"_")}.png`, new Uint8Array(await blob.arrayBuffer()));
    index++;
  } catch {}
}
