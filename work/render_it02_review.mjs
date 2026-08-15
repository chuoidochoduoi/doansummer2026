import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";
const p="D:/gitlap/doAnSummer2026/work/it02_render_copy.xlsx";
const wb=await SpreadsheetFile.importXlsx(await FileBlob.load(p));
for(const [name,range,file] of [
  ["CA-IT02-LoginToken","A1:I10","it02_ca_review.png"],
  ["TD-IT02-LoginToken","A1:D10","it02_td_review.png"],
  ["IT02-LoginToken","A1:O21","it02_feature_review.png"]]){
  const pic=await wb.render({sheetName:name,range,scale:1,format:"png"});
  await fs.writeFile(`D:/gitlap/doAnSummer2026/work/${file}`,new Uint8Array(await pic.arrayBuffer()));
}
const err=await wb.inspect({kind:"match",searchTerm:"#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A",options:{useRegex:true,maxResults:100},maxChars:4000});
console.log(err.ndjson);
