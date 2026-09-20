import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";
const file = "C:/Users/Administrator/Downloads/CareS_Report_5.1_Unit_Test_Appointment_Review.xlsx";
const wb = await SpreadsheetFile.importXlsx(await FileBlob.load(file));
for (const name of ["Appointment 1", "Appointment 2"]) {
  const png = await wb.render({ sheetName: name, range: name === "Appointment 1" ? "A1:S90" : "A1:N85", scale: 1, format: "png" });
  await fs.writeFile(`D:/gitlap/doAnSummer2026/.codex-tmp/${name.replace(/ /g,"_")}.png`, new Uint8Array(await png.arrayBuffer()));
  console.log(name);
}
