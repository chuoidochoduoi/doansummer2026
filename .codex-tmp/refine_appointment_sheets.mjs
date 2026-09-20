import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const file = "C:/Users/Administrator/Downloads/CareS_Report_5.1_Unit_Test.xlsx";
const output = "C:/Users/Administrator/Downloads/CareS_Report_5.1_Unit_Test_Appointment_Review.xlsx";
const wb = await SpreadsheetFile.importXlsx(await FileBlob.load(file));
const example = wb.worksheets.getItem("Example");

const a1 = [
  {id:"UTCID01", name:"Account is missing", type:"A", c:{accountExists:"F",role:"N/A",profileExists:"N/A",serviceIds:"valid",hasConflict:"N/A",shiftExists:"N/A",serviceExists:"N/A",dateOfBirth:"N/A",ageEligible:"N/A",gender:"N/A",allowCustomerBooking:"N/A",scheduledAt:"requestedTime"}, ok:false, ex:"ResourceNotFoundException", msg:"Account does not exist"},
  {id:"UTCID02", name:"Account role is null", type:"B", c:{accountExists:"T",role:"null",profileExists:"N/A",serviceIds:"valid",hasConflict:"N/A",shiftExists:"N/A",serviceExists:"N/A",dateOfBirth:"N/A",ageEligible:"N/A",gender:"N/A",allowCustomerBooking:"N/A",scheduledAt:"requestedTime"}, ok:false, ex:"BadRequestException", msg:"Only customers or staff can book"},
  {id:"UTCID03", name:"Patient profile is missing", type:"A", c:{accountExists:"T",role:"CUSTOMER",profileExists:"F",serviceIds:"valid",hasConflict:"N/A",shiftExists:"N/A",serviceExists:"N/A",dateOfBirth:"N/A",ageEligible:"N/A",gender:"N/A",allowCustomerBooking:"N/A",scheduledAt:"requestedTime"}, ok:false, ex:"ResourceNotFoundException", msg:"Patient does not exist"},
  {id:"UTCID04", name:"Another active appointment conflicts", type:"A", c:{accountExists:"T",role:"CUSTOMER",profileExists:"T",serviceIds:"valid",hasConflict:"T",shiftExists:"N/A",serviceExists:"N/A",dateOfBirth:"valid",ageEligible:"T",gender:"FEMALE",allowCustomerBooking:"N/A",scheduledAt:"requestedTime"}, ok:false, ex:"BadRequestException", msg:"Appointment time conflicts"},
  {id:"UTCID05", name:"Selected shift is missing", type:"A", c:{accountExists:"T",role:"CUSTOMER",profileExists:"T",serviceIds:"valid",hasConflict:"F",shiftExists:"F",serviceExists:"N/A",dateOfBirth:"valid",ageEligible:"T",gender:"FEMALE",allowCustomerBooking:"N/A",scheduledAt:"requestedTime"}, ok:false, ex:"ResourceNotFoundException", msg:"Shift does not exist"},
  {id:"UTCID06", name:"Selected service is missing", type:"A", c:{accountExists:"T",role:"CUSTOMER",profileExists:"T",serviceIds:"valid",hasConflict:"F",shiftExists:"N/A",serviceExists:"F",dateOfBirth:"valid",ageEligible:"T",gender:"FEMALE",allowCustomerBooking:"N/A",scheduledAt:"requestedTime"}, ok:false, ex:"ResourceNotFoundException", msg:"Service does not exist"},
  {id:"UTCID07", name:"Birth date is missing for an age-restricted service", type:"B", c:{accountExists:"T",role:"CUSTOMER",profileExists:"T",serviceIds:"valid",hasConflict:"F",shiftExists:"N/A",serviceExists:"T",dateOfBirth:"null",ageEligible:"N/A",gender:"FEMALE",allowCustomerBooking:"N/A",scheduledAt:"requestedTime"}, ok:false, ex:"BadRequestException", msg:"Birth date is required"},
  {id:"UTCID08", name:"Patient is younger than the service limit", type:"B", c:{accountExists:"T",role:"CUSTOMER",profileExists:"T",serviceIds:"valid",hasConflict:"F",shiftExists:"N/A",serviceExists:"T",dateOfBirth:"valid",ageEligible:"F",gender:"FEMALE",allowCustomerBooking:"N/A",scheduledAt:"requestedTime"}, ok:false, ex:"BadRequestException", msg:"Patient is below the minimum age"},
  {id:"UTCID09", name:"Patient is older than the service limit", type:"B", c:{accountExists:"T",role:"CUSTOMER",profileExists:"T",serviceIds:"valid",hasConflict:"F",shiftExists:"N/A",serviceExists:"T",dateOfBirth:"valid",ageEligible:"F",gender:"FEMALE",allowCustomerBooking:"N/A",scheduledAt:"requestedTime"}, ok:false, ex:"BadRequestException", msg:"Patient is above the maximum age"},
  {id:"UTCID10", name:"Gender is missing for a restricted service", type:"B", c:{accountExists:"T",role:"CUSTOMER",profileExists:"T",serviceIds:"valid",hasConflict:"F",shiftExists:"N/A",serviceExists:"T",dateOfBirth:"valid",ageEligible:"T",gender:"null",allowCustomerBooking:"N/A",scheduledAt:"requestedTime"}, ok:false, ex:"BadRequestException", msg:"Gender is required"},
  {id:"UTCID11", name:"Gender is not accepted by the service", type:"A", c:{accountExists:"T",role:"CUSTOMER",profileExists:"T",serviceIds:"valid",hasConflict:"F",shiftExists:"N/A",serviceExists:"T",dateOfBirth:"valid",ageEligible:"T",gender:"notAllowed",allowCustomerBooking:"N/A",scheduledAt:"requestedTime"}, ok:false, ex:"BadRequestException", msg:"Service is not available for this gender"},
  {id:"UTCID12", name:"Customer appointment is created", type:"N", c:{accountExists:"T",role:"CUSTOMER",profileExists:"T",serviceIds:"valid",hasConflict:"F",shiftExists:"T",serviceExists:"T",dateOfBirth:"valid",ageEligible:"T",gender:"FEMALE",allowCustomerBooking:"N/A",scheduledAt:"normalizedShiftTime"}, ok:true},
  {id:"UTCID13", name:"Staff creates an appointment", type:"N", c:{accountExists:"T",role:"STAFF",profileExists:"T",serviceIds:"valid",hasConflict:"F",shiftExists:"N/A",serviceExists:"T",dateOfBirth:"valid",ageEligible:"T",gender:"FEMALE",allowCustomerBooking:"N/A",scheduledAt:"requestedTime"}, ok:true},
  {id:"UTCID14", name:"Service list is null", type:"B", c:{accountExists:"T",role:"CUSTOMER",profileExists:"T",serviceIds:"null",hasConflict:"N/A",shiftExists:"N/A",serviceExists:"N/A",dateOfBirth:"valid",ageEligible:"N/A",gender:"FEMALE",allowCustomerBooking:"N/A",scheduledAt:"requestedTime"}, ok:false, ex:"BadRequestException", msg:"At least one service is required"},
];

const a2 = [
  {id:"UTCID01", name:"Guest gender is OTHER", type:"A", c:{guestFullName:"valid",guestPhone:"valid",guestEmail:"null",guestAge:"valid",guestGender:"OTHER",scheduledAt:"valid",shiftId:"null",serviceIds:"valid",allowCustomerBooking:"T",hasGuestConflict:"N/A",conflictMatchedBy:"N/A"}, ok:false, ex:"BadRequestException",msg:"Only male or female is supported"},
  {id:"UTCID02", name:"Guest appointment conflicts by phone", type:"A", c:{guestFullName:"valid",guestPhone:"valid",guestEmail:"null",guestAge:"valid",guestGender:"FEMALE",scheduledAt:"valid",shiftId:"null",serviceIds:"valid",allowCustomerBooking:"T",hasGuestConflict:"T",conflictMatchedBy:"phone"}, ok:false, ex:"BadRequestException",msg:"Appointment time conflicts"},
  {id:"UTCID03", name:"Blank phone and email skip conflict check", type:"B", c:{guestFullName:"valid",guestPhone:"blank",guestEmail:"blank",guestAge:"valid",guestGender:"FEMALE",scheduledAt:"valid",shiftId:"null",serviceIds:"valid",allowCustomerBooking:"T",hasGuestConflict:"N/A",conflictMatchedBy:"N/A"}, ok:true},
  {id:"UTCID04", name:"Guest appointment is created", type:"N", c:{guestFullName:"valid",guestPhone:"valid",guestEmail:"valid",guestAge:"valid",guestGender:"FEMALE",scheduledAt:"valid",shiftId:"null",serviceIds:"valid",allowCustomerBooking:"T",hasGuestConflict:"F",conflictMatchedBy:"phone"}, ok:true},
  {id:"UTCID05", name:"Shift time is resolved before saving", type:"N", c:{guestFullName:"valid",guestPhone:"valid",guestEmail:"null",guestAge:"valid",guestGender:"FEMALE",scheduledAt:"valid",shiftId:"valid",serviceIds:"valid",allowCustomerBooking:"T",hasGuestConflict:"F",conflictMatchedBy:"phone"}, ok:true},
  {id:"UTCID06", name:"Service is not available for online booking", type:"A", c:{guestFullName:"valid",guestPhone:"valid",guestEmail:"null",guestAge:"valid",guestGender:"FEMALE",scheduledAt:"valid",shiftId:"null",serviceIds:"valid",allowCustomerBooking:"F",hasGuestConflict:"N/A",conflictMatchedBy:"N/A"}, ok:false, ex:"BadRequestException",msg:"Service cannot be booked online"},
  {id:"UTCID07", name:"Guest appointment conflicts by email", type:"A", c:{guestFullName:"valid",guestPhone:"blank",guestEmail:"valid",guestAge:"valid",guestGender:"MALE",scheduledAt:"valid",shiftId:"null",serviceIds:"empty",allowCustomerBooking:"N/A",hasGuestConflict:"T",conflictMatchedBy:"email"}, ok:false, ex:"BadRequestException",msg:"Appointment time conflicts"},
  {id:"UTCID08", name:"Selected shift is missing", type:"A", c:{guestFullName:"valid",guestPhone:"valid",guestEmail:"null",guestAge:"valid",guestGender:"MALE",scheduledAt:"valid",shiftId:"notFound",serviceIds:"valid",allowCustomerBooking:"T",hasGuestConflict:"N/A",conflictMatchedBy:"N/A"}, ok:false, ex:"ResourceNotFoundException",msg:"Shift does not exist"},
  {id:"UTCID09", name:"Null service list still creates a guest appointment", type:"B", c:{guestFullName:"valid",guestPhone:"null",guestEmail:"null",guestAge:"valid",guestGender:"MALE",scheduledAt:"valid",shiftId:"null",serviceIds:"null",allowCustomerBooking:"N/A",hasGuestConflict:"N/A",conflictMatchedBy:"N/A"}, ok:true},
];

const values1 = {
  accountExists:["T","F"], role:["CUSTOMER","STAFF","ADMIN","null","N/A"], profileExists:["T","F","N/A"],
  serviceIds:["valid","null","empty","duplicate"], hasConflict:["T","F","N/A"], shiftExists:["T","F","N/A"],
  serviceExists:["T","F","N/A"], dateOfBirth:["valid","null","N/A"], ageEligible:["T","F","N/A"],
  gender:["MALE","FEMALE","null","notAllowed","N/A"], allowCustomerBooking:["T","F","N/A"],
  scheduledAt:["requestedTime","normalizedShiftTime"]
};
const values2 = {
  guestFullName:["valid"], guestPhone:["valid","blank","null"], guestEmail:["valid","blank","null"], guestAge:["valid","outOfRange"],
  guestGender:["MALE","FEMALE","OTHER"], scheduledAt:["valid"], shiftId:["valid","notFound","null"], serviceIds:["valid","empty","null"],
  allowCustomerBooking:["T","F","N/A"], hasGuestConflict:["T","F","N/A"], conflictMatchedBy:["phone","email","N/A"]
};

function colName(index) { let s=""; for(let n=index+1;n>0;n=Math.floor((n-1)/26)) s=String.fromCharCode(65+(n-1)%26)+s; return s; }

function buildSheet(sheetName, functionCode, functionName, requirement, tests, conditionValues) {
  const sheet = wb.worksheets.getItem(sheetName);
  const last = colName(5 + tests.length - 1);
  const after = colName(5 + tests.length);
  sheet.getRange("A1:HQ220").clear({applyTo:"contents"});
  sheet.getRange(`${after}9:HQ220`).clear({applyTo:"formats"});

  sheet.getRange("A2:C5").values = [
    ["Function Code",null,functionCode], ["Created By",null,"CuongND"], ["Lines of code",null,0], ["Test requirement",null,requirement]
  ];
  sheet.getRange("F2:L5").values = [
    ["Function Name",null,null,null,null,null,functionName], ["Executed By",null,null,null,null,null,"CuongND"],
    ["Lack of test cases",null,null,null,null,null,0], [null,null,null,null,null,null,null]
  ];
  const n=tests.filter(t=>t.type==="N").length, a=tests.filter(t=>t.type==="A").length, b=tests.filter(t=>t.type==="B").length;
  sheet.getRange("A6:O7").values = [
    ["Passed",null,"Failed",null,null,"Untested",null,null,null,null,null,"N/A/B",null,null,"Total Test Cases"],
    [tests.length,null,0,null,null,0,null,null,null,null,null,n,a,b,tests.length]
  ];

  sheet.getRange(`F9:${last}9`).copyFrom(example.getRange("F9"),"all");
  sheet.getRange(`F9:${last}9`).values = [tests.map(t=>t.id)];
  sheet.getRange(`F9:${last}9`).format.columnWidth = 5;
  sheet.getRange(`F9:${last}9`).format.rowHeight = 74;
  sheet.getRange(`F9:${last}9`).format.horizontalAlignment = "center";
  sheet.getRange(`F9:${last}9`).format.verticalAlignment = "center";

  const rows=[]; const matrix=[];
  const add=(a,b,marks=[])=>{ rows.push([a,b]); matrix.push(tests.map((_,i)=>marks[i]??null)); };
  add("Condition","Precondition");
  add(null,"Can connect to server",tests.map(()=>"O"));
  add(null,"Required mocks and test data are available",tests.map(()=>"O"));
  for (const [variable, vals] of Object.entries(conditionValues)) {
    add(null,variable);
    for (const value of vals) add(null,value,tests.map(t=>t.c[variable]===value?"O":null));
  }
  add("Confirm","Return");
  add(null,"T",tests.map(t=>t.ok?"O":null));
  add(null,"F",tests.map(t=>!t.ok?"O":null));
  const confirmations={
    status:["PENDING"], customerLinked:["T"], servicesLinked:["T"], isGuest:["T"], customerId:["null"],
    guestInformationSaved:["T"], "repository.save":["T","F"], notifyReceptionists:["T","F"]
  };
  for (const [key,vals] of Object.entries(confirmations)) {
    const relevant = sheetName==="Appointment 1" ? !["isGuest","customerId","guestInformationSaved"].includes(key) : !["customerLinked"].includes(key);
    if (!relevant) continue;
    add(null,key);
    for (const value of vals) add(null,value,tests.map(t=>{
      if (["status","customerLinked","servicesLinked","isGuest","customerId","guestInformationSaved"].includes(key)) return t.ok?"O":null;
      if (key==="repository.save" || key==="notifyReceptionists") return ((t.ok&&value==="T")||(!t.ok&&value==="F"))?"O":null;
      return null;
    }));
  }
  add(null,"Exception");
  for (const ex of ["ResourceNotFoundException","BadRequestException"]) add(null,ex,tests.map(t=>t.ex===ex?"O":null));
  add(null,"Log message");
  for (const t of tests.filter(t=>t.msg)) add(null,t.msg,tests.map(x=>x===t?"O":null));
  add("Result","Type (N : Normal, A : Abnormal, B : Boundary)",tests.map(t=>t.type));
  add(null,"Passed/Failed",tests.map(()=>"P"));
  add(null,"Executed Date",tests.map(()=>new Date("2026-09-14T00:00:00")));
  add(null,"Defect ID",tests.map(()=>null));

  const end=9+rows.length;
  sheet.getRange(`F10:${last}${end}`).copyFrom(example.getRange("F10"),"all");
  sheet.getRange(`A10:B${end}`).values=rows;
  sheet.getRange(`F10:${last}${end}`).values=matrix;
  sheet.getRange(`A10:${last}${end}`).format.borders={preset:"all",style:"thin",color:"#000000"};
  sheet.getRange(`B10:B${end}`).format.columnWidth=42;
  sheet.getRange(`B10:B${end}`).format.wrapText=true;
  sheet.getRange(`A10:${last}${end}`).format.verticalAlignment="center";
  sheet.getRange(`F10:${last}${end}`).format.horizontalAlignment="center";
  sheet.getRange(`F10:${last}${end}`).format.columnWidth=5;
  sheet.getRange(`A10:B${end}`).format.rowHeight=22;
}

buildSheet("Appointment 1","F_SVC_004","create","Create an appointment for an existing customer or staff account.",a1,values1);
buildSheet("Appointment 2","F_SVC_005","createForGuest","Create an appointment for a guest who does not have a CareS account.",a2,values2);

const functions=wb.worksheets.getItem("Functions");
functions.getRange("B14:H15").values=[
  ["Appointment - Create","AppointmentService","create","F_SVC_004","Appointment 1","Create an appointment for an existing customer or staff account.","Repositories, supporting services, request data, and the current account are prepared."],
  ["Appointment - Create for guest","AppointmentService","createForGuest","F_SVC_005","Appointment 2","Create an appointment for a guest without a CareS account.","Repositories, supporting services, guest details, and appointment data are prepared."]
];
functions.getRange("A14:H15").format.rowHeight=38;

const stats=wb.worksheets.getItem("Statistics");
stats.getRange("B15:I16").clear({applyTo:"contents"});
stats.getRange("B15:I16").values=[
  ["F_SVC_004",14,0,0,2,7,5,14],
  ["F_SVC_005",9,0,0,3,4,2,9]
];

wb.recalculate();
const out=await SpreadsheetFile.exportXlsx(wb);
await out.save(output);
console.log(JSON.stringify({output,appointment1:a1.length,appointment2:a2.length}));
