import fs from "node:fs/promises";
import path from "node:path";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const sourceDir="D:/download/UnitTest";
const outputDir="D:/gitlap/doAnSummer2026/outputs/019ff5dc-080c-7230-b9b2-751838ad3212/unit_test_normalized";
const previewDir="D:/gitlap/doAnSummer2026/work/unit_normalized_previews";
const inspection=JSON.parse(await fs.readFile("D:/gitlap/doAnSummer2026/work/unit_batch_inspection.json","utf8"));
let names=inspection.filter(x=>x.ok).map(x=>x.name);
if(process.argv[2]) names=names.filter(x=>x===process.argv[2]);
await fs.mkdir(outputDir,{recursive:true}); await fs.mkdir(previewDir,{recursive:true});
const results=[];

const setWidths=(sheet,map)=>{for(const [col,width] of Object.entries(map))sheet.getRange(`${col}:${col}`).format.columnWidth=width;};
const functionWidths={A:7.5,B:12.75,C:10.13,D:10.75,E:0,F:2.25,G:2.25,H:2.25,I:2.25,J:2.25,K:2.25,L:2.25,M:2.25,N:2.25,O:2.25,P:2.25,Q:2.25,R:2.25,S:2.25,T:2.25,U:2.25,V:8.38,W:8.38};
const coverWidths={A:27.63,B:9.38,C:13.75,D:7.38,E:37.38,F:47.63};
const listWidths={A:6.5,B:14.13,C:9.25,D:11.75,E:20.38,F:11.75,G:21.88,H:33.13};
const reportWidths={A:14.75,B:26,C:11.5,D:9,E:9.13,F:4.63,G:4.63,H:4.63,I:20.38};
const guidelineWidths={A:118.75,B:8.38,C:8.38,D:8.38};

for(const name of names){
  const input=path.join(sourceDir,name).replaceAll("\\","/"); const output=path.join(outputDir,name).replaceAll("\\","/");
  try{
    const wb=await SpreadsheetFile.importXlsx(await FileBlob.load(input));
    setWidths(wb.worksheets.getItem("Guideline"),guidelineWidths);
    setWidths(wb.worksheets.getItem("Cover"),coverWidths);
    setWidths(wb.worksheets.getItem("FunctionList"),listWidths);
    setWidths(wb.worksheets.getItem("Test Report"),reportWidths);
    const sheetInfo=inspection.find(x=>x.name===name).sheets;
    const functionSheets=sheetInfo.filter(s=>/^Function\d+$/.test(s.name)&&s.range).map(s=>s.name);
    for(const sn of functionSheets){
      const s=wb.worksheets.getItem(sn); setWidths(s,functionWidths);
      s.getRange("8:8").format.rowHeight=11.25; s.getRange("9:9").format.rowHeight=46.5; s.getRange("48:48").format.rowHeight=11.25;
      s.getRange("F9:T9").format={fill:"#000080",font:{name:"Tahoma",size:8,bold:true,color:"#FFFFFF"},horizontalAlignment:"center",verticalAlignment:"bottom",wrapText:false};
    }
    const errors=await wb.inspect({kind:"match",searchTerm:"#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A",options:{useRegex:true,maxResults:300},maxChars:5000});
    const out=await SpreadsheetFile.exportXlsx(wb); await out.save(output);
    const bookPreviewDir=path.join(previewDir,path.parse(name).name); await fs.mkdir(bookPreviewDir,{recursive:true});
    const previewSheets=["Cover","FunctionList","Test Report",functionSheets[0],functionSheets.at(-1)].filter((x,i,a)=>x&&a.indexOf(x)===i);
    for(const sn of previewSheets){
      const img=await wb.render({sheetName:sn,autoCrop:"all",scale:0.35,format:"png"});
      await fs.writeFile(path.join(bookPreviewDir,`${sn.replace(/[^A-Za-z0-9_-]/g,"_")}.png`),new Uint8Array(await img.arrayBuffer()));
    }
    results.push({name,ok:true,output,functionSheets:functionSheets.length,errorScan:errors.ndjson});
    console.log(`OK\t${name}\tfunctions=${functionSheets.length}`);
  }catch(e){results.push({name,ok:false,error:String(e)});console.error(`FAIL\t${name}\t${String(e).slice(-500)}`);}
}
await fs.writeFile("D:/gitlap/doAnSummer2026/work/unit_normalization_results.json",JSON.stringify(results,null,2),"utf8");
