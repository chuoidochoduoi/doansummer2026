import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const source = String.raw`D:\gitlap\doAnSummer2026\outputs\019fc0c3-dee6-7a11-9b42-26e3805728ed\CareS_Report_5.4_Acceptance_Test.xlsx`;
const output = String.raw`D:\gitlap\doAnSummer2026\outputs\019fc0c3-dee6-7a11-9b42-26e3805728ed\CareS_Report_5.4_Acceptance_Test_v2.xlsx`;
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(source));
workbook.recalculate();
await fs.mkdir(String.raw`D:\gitlap\doAnSummer2026\outputs\019fc0c3-dee6-7a11-9b42-26e3805728ed`, { recursive: true });
const blob = await SpreadsheetFile.exportXlsx(workbook);
await blob.save(output);
console.log(output);
