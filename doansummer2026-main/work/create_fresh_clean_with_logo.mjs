import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const input = "D:/gitlap/doAnSummer2026/outputs/INTEGRATION_TEST_PHONG_KHAM_MO_KHONG_LOI.xlsx";
const output = "D:/gitlap/doAnSummer2026/outputs/INTEGRATION_TEST_PHONG_KHAM_FILE_MOI_HOAN_TOAN.xlsx";
const previewPath = "D:/gitlap/doAnSummer2026/work/fresh_cover_preview.png";
const logoPath = "D:/gitlap/doAnSummer2026/work/fixed_ui_unpack/xl/media/image1.png";

const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(input));
const cover = workbook.worksheets.getItem("Cover");
cover.deleteAllDrawings();
const logo = await fs.readFile(logoPath);
cover.images.add({
  dataUrl: `data:image/png;base64,${logo.toString("base64")}`,
  anchor: { from: { row: 1, col: 0, rowOffsetPx: 9 }, extent: { widthPx: 224, heightPx: 73 } },
});

const errors = await workbook.inspect({
  kind: "match",
  searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A|Unit Test",
  options: { useRegex: true, maxResults: 300 },
  maxChars: 5000,
});
console.log(errors.ndjson);

const preview = await workbook.render({ sheetName: "Cover", range: "A1:F11", scale: 1.5, format: "png" });
await fs.writeFile(previewPath, new Uint8Array(await preview.arrayBuffer()));
const xlsx = await SpreadsheetFile.exportXlsx(workbook);
await xlsx.save(output);
console.log(JSON.stringify({ output, previewPath }));
