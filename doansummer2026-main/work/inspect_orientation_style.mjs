import {FileBlob,SpreadsheetFile} from "@oai/artifact-tool";
for(const [label,file] of [["ref","D:/gitlap/doAnSummer2026/work/unit_test_reference_converted.xlsx"],["out","D:/gitlap/doAnSummer2026/outputs/019ff5dc-080c-7230-b9b2-751838ad3212/unit_test_normalized/AccountService_UnitTestCase_FIXED.xlsx"]]){
 const wb=await SpreadsheetFile.importXlsx(await FileBlob.load(file));
 for(const range of ["L2:T2","F9:T9","A10:T10"]){const x=await wb.inspect({kind:"computedStyle",sheetId:"Function1",range,maxChars:5000});console.log(label,range,x.ndjson)}
}
