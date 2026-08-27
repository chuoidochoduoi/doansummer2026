import fs from "node:fs/promises";
import path from "node:path";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const dir="D:/download/UnitTest";
const names=[
"AccountService_UnitTestCase_FIXED.xlsx","AppointmentService_UnitTestCase_UPDATED_CheckIn.xlsx","AuditLogService_UnitTestCase.xlsx","AuthService_UnitTestCase.xlsx","ChatService_UnitTestCase.xlsx","CustomerVisitService_UnitTestCase.xlsx","DepartmentService_UnitTestCase.xlsx","EmailService_UnitTestCase.xlsx","InvoiceService_UnitTestCase.xlsx","InvoiceService_UnitTestCase_ACTUAL_46_OF_46_PASSED.xlsx","MedicalRecordService_UnitTestCase.xlsx","MedicalServiceService_UnitTestCase.xlsx","NotificationService_UnitTestCase.xlsx","PatientJourneyService_UnitTestCase.xlsx","ProfileService_UnitTestCase.xlsx","QueueTicketService_UnitTestCase.xlsx","ReportService_UnitTestCase.xlsx","ServiceCategoryService_UnitTestCase.xlsx","TestRequestService_UnitTestCase_UPDATED_CURRENT.xlsx","VitalSignsService_UnitTestCase_Template_FIXED_v2.xlsx"];
const results=[];
for(const name of names){
  const file=path.join(dir,name).replaceAll("\\","/");
  try{
    const wb=await SpreadsheetFile.importXlsx(await FileBlob.load(file));
    const info=await wb.inspect({kind:"sheet",include:"id,name",maxChars:30000});
    const sheets=[];
    for(const line of info.ndjson.split(/\r?\n/)){try{const x=JSON.parse(line);if(x.name)sheets.push({name:x.name,range:x.range||x.address})}catch{}}
    const errors=await wb.inspect({kind:"match",searchTerm:"#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A",options:{useRegex:true,maxResults:100},maxChars:3000});
    results.push({name,ok:true,sheets,errorScan:errors.ndjson});
  }catch(e){results.push({name,ok:false,error:String(e).slice(-1000)});}
}
await fs.writeFile("D:/gitlap/doAnSummer2026/work/unit_batch_inspection.json",JSON.stringify(results,null,2),"utf8");
console.log(results.map(x=>`${x.ok?"OK":"FAIL"}\t${x.name}\t${x.ok?x.sheets.map(s=>`${s.name}:${s.range}`).join(" | "):x.error.split(/\r?\n/).at(-1)}`).join("\n"));
