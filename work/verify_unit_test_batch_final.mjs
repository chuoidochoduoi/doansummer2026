import fs from "node:fs/promises";
import path from "node:path";
import {FileBlob,SpreadsheetFile} from "@oai/artifact-tool";
const dir="D:/gitlap/doAnSummer2026/outputs/019ff5dc-080c-7230-b9b2-751838ad3212/unit_test_normalized";
const files=(await fs.readdir(dir)).filter(x=>x.endsWith(".xlsx"));
const results=[];
for(const name of files){
 try{
  const wb=await SpreadsheetFile.importXlsx(await FileBlob.load(path.join(dir,name).replaceAll("\\","/")));
  const sheets=await wb.inspect({kind:"sheet",include:"id,name",maxChars:40000});
  const errors=await wb.inspect({kind:"match",searchTerm:"#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A",options:{useRegex:true,maxResults:300},maxChars:5000});
  const list=[];for(const line of sheets.ndjson.split(/\r?\n/)){try{const x=JSON.parse(line);if(x.name)list.push(x)}catch{}}
  const funcs=list.filter(x=>/^Function\d+$/.test(x.name)&&x.range);
  let layoutOk=true;for(const f of funcs){const s=wb.worksheets.getItem(f.name);const expected={A:7.5,B:12.75,C:10.13,D:10.75,F:2.25,T:2.25};for(const [c,w] of Object.entries(expected)){if(Math.abs(s.getRange(`${c}:${c}`).format.columnWidth-w)>0.15)layoutOk=false;}}
  results.push({name,ok:true,sheetCount:list.length,functionCount:funcs.length,layoutOk,formulaErrors:!errors.ndjson.includes("matched 0 entries")});
  console.log(`OK ${name} sheets=${list.length} functions=${funcs.length} layout=${layoutOk}`);
 }catch(e){results.push({name,ok:false,error:String(e)});console.log(`FAIL ${name}`)}
}
await fs.writeFile("D:/gitlap/doAnSummer2026/work/unit_final_verification.json",JSON.stringify(results,null,2),"utf8");
