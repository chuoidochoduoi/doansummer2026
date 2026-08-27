import fs from "node:fs/promises";
import path from "node:path";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";
const dir="D:/download/UnitTest";
const names=JSON.parse(await fs.readFile("D:/gitlap/doAnSummer2026/work/unit_batch_inspection.json","utf8")).filter(x=>x.ok).map(x=>x.name);
const out=[];
const letters="ABCDEFGHIJKLMNOPQRSTUVWXYZ";
for(const name of names){
  const wb=await SpreadsheetFile.importXlsx(await FileBlob.load(path.join(dir,name).replaceAll("\\","/")));
  const info=JSON.parse(await fs.readFile("D:/gitlap/doAnSummer2026/work/unit_batch_inspection.json","utf8")).find(x=>x.name===name);
  const functionNames=info.sheets.filter(s=>/^Function\d+$/.test(s.name)&&s.range).map(s=>s.name);
  const samples=[];
  for(const sn of [...new Set([functionNames[0],functionNames.at(-1)])].filter(Boolean)){
    const s=wb.worksheets.getItem(sn); const widths=[];
    for(let c=0;c<Math.min(23,(s.getUsedRange()?.getBoundingBox()?.cols||23));c++) widths.push({col:letters[c],width:s.getRange(`${letters[c]}:${letters[c]}`).format.columnWidth});
    const rows=[];for(const r of [1,2,3,4,5,6,7,8,9,10,11,32,33,43,48])rows.push({row:r,height:s.getRange(`${r}:${r}`).format.rowHeight});
    samples.push({sheet:sn,widths,rows,values:s.getRange("A1:W12").values});
  }
  const meta=[];
  for(const sn of ["Cover","FunctionList","Test Report"]){const s=wb.worksheets.getItem(sn);const box=s.getUsedRange()?.getBoundingBox();const widths=[];for(let c=0;c<Math.min(23,box?.cols||0);c++)widths.push({col:letters[c],width:s.getRange(`${letters[c]}:${letters[c]}`).format.columnWidth});meta.push({sheet:sn,widths});}
  out.push({name,functionCount:functionNames.length,samples,meta});
}
await fs.writeFile("D:/gitlap/doAnSummer2026/work/unit_batch_measurements.json",JSON.stringify(out,null,2),"utf8");
for(const x of out){const w=x.samples[0]?.widths.map(c=>`${c.col}:${c.width}`).join(",");console.log(`${x.name}\tfunctions=${x.functionCount}\t${w}`)}
