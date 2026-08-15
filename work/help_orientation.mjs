import { Workbook } from "@oai/artifact-tool";
const wb=Workbook.create(); wb.worksheets.add("Sheet1");
console.log(wb.help("range.format",{search:"orientation|rotation|textOrientation",include:"index,examples,notes",maxChars:4000}).ndjson);
