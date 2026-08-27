import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";
const path = "D:/gitlap/doAnSummer2026/work/preview_no_logo.xlsx";
const wb = await SpreadsheetFile.importXlsx(await FileBlob.load(path));
const sheets = await wb.inspect({kind:"sheet",include:"id,name",maxChars:12000});
await fs.writeFile("work/verify_sheets.ndjson",sheets.ndjson,"utf8");
const errors = await wb.inspect({kind:"match",searchTerm:"#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A",options:{useRegex:true,maxResults:100},summary:"formula errors"});
await fs.writeFile("work/verify_errors.ndjson",errors.ndjson,"utf8");
console.log(sheets.ndjson);
await fs.mkdir("work/rendered_sample",{recursive:true});
const names=[];
for(const line of sheets.ndjson.split(/\r?\n/)){
  try{const o=JSON.parse(line); if(o.name) names.push(o.name)}catch{}
}
for(let i=0;i<names.length;i++){
  const blob=await wb.render({sheetName:names[i],autoCrop:"all",scale:1,format:"png"});
  await fs.writeFile(`work/rendered_sample/${String(i+1).padStart(2,"0")}.png`,new Uint8Array(await blob.arrayBuffer()));
}
