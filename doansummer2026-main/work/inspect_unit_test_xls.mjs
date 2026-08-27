import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const input = "D:/gitlap/doAnSummer2026/work/unit_test_reference_converted.xlsx";
const wb = await SpreadsheetFile.importXlsx(await FileBlob.load(input));
const sheets = await wb.inspect({kind:"sheet",include:"id,name",maxChars:20000});
console.log(sheets.ndjson);
await fs.mkdir("D:/gitlap/doAnSummer2026/work/unit_template_previews",{recursive:true});
let i=1;
for(const line of sheets.ndjson.split(/\r?\n/)){
  try{
    const item=JSON.parse(line); if(!item.name) continue;
    const table=await wb.inspect({kind:"table",sheetId:item.name,range:item.range||"A1:Z50",include:"values,formulas",tableMaxRows:50,tableMaxCols:26,maxChars:16000});
    await fs.writeFile(`D:/gitlap/doAnSummer2026/work/unit_template_previews/${String(i).padStart(2,"0")}-${item.name.replace(/[^A-Za-z0-9_-]/g,"_")}.ndjson`,table.ndjson,"utf8");
    i++;
  }catch(e){console.error(String(e));}
}
