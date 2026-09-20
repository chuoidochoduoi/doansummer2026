import { Workbook } from "@oai/artifact-tool";
const wb = Workbook.create();
console.log(wb.help("cell text direction and rotation", { search: "textRotation|rotation|orientation|vertical", include: "index,examples,notes", maxChars: 7000 }).ndjson);
