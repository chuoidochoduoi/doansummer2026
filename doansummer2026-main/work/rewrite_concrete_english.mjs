import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const input = "D:/gitlap/doAnSummer2026/outputs/INTEGRATION_TEST_PHONG_KHAM_FILE_MOI_HOAN_TOAN.xlsx";
const output = "D:/gitlap/doAnSummer2026/outputs/INTEGRATION_TEST_CONCRETE_DATA_ENGLISH.xlsx";
const wb = await SpreadsheetFile.importXlsx(await FileBlob.load(input));

const j = (...xs) => xs.join("\n");
const tc = (name, procedure, expected, pre, tag) => ({name, procedure:j(...procedure), expected:j(...expected), pre:j(...pre), tag});
const flows = [
{id:1,key:"Register",name:"Registration and OTP verification",req:"Redis OTP -> Account -> Profile -> JWT",endpoint:"POST /api/auth/register",conditions:[
["phone",'phone = "0944433222"','phone = "944433222"'],["otp",'otp = "482615"','otp = "48261"'],["password",'password = "Clinic@123"','password = "abc"'],["fullName",'fullName = "John Carter"','fullName = ""'],["dateOfBirth",'dateOfBirth = "1998-05-20"','dateOfBirth = "2027-01-01"'],["phone uniqueness",'account.phone.count = 0','account.phone.count = 1']],cases:[
tc("Register with verified OTP",['phone = "0944433222"','otp = "482615"','password = "Clinic@123"','fullName = "John Carter"','dateOfBirth = "1998-05-20"','POST /api/auth/register'],['httpStatus = 201','response.accessToken != null','account.phone = "0944433222"','account.systemRole = "CUSTOMER"','profile.fullName = "John Carter"'],['redis.otp["0944433222"] = "482615"','redis.otpVerified["0944433222"] = true','account.phone.count = 0'],"V1, V2, V3, V4, V5, V6"),
tc("Reject five-digit OTP",['phone = "0944433222"','otp = "48261"','POST /api/auth/verify-register-otp'],['httpStatus = 400','response.code = "INVALID_OTP"','redis.otpVerified["0944433222"] = null'],['redis.otp["0944433222"] = "482615"'],"B2"),
tc("Reject registration without OTP verification",['phone = "0944433222"','password = "Clinic@123"','POST /api/auth/register'],['httpStatus = 400','response.code = "OTP_NOT_VERIFIED"','account.phone.count = 0'],['redis.otpVerified["0944433222"] = null'],"I2"),
tc("Reject short password",['phone = "0944433222"','password = "abc"','POST /api/auth/register'],['httpStatus = 400','response.code = "VALIDATION_ERROR"','account.phone.count = 0'],['redis.otpVerified["0944433222"] = true'],"I3"),
tc("Reject duplicate phone",['phone = "0944433222"','password = "Clinic@123"','POST /api/auth/register'],['httpStatus = 409','response.code = "PHONE_ALREADY_EXISTS"','account.phone.count = 1'],['account.phone = "0944433222"','account.phone.count = 1'],"I6"),
tc("Reject future date of birth",['phone = "0988776655"','dateOfBirth = "2027-01-01"','POST /api/auth/register'],['httpStatus = 400','response.code = "INVALID_DATE_OF_BIRTH"','account.phone.count = 0'],['redis.otpVerified["0988776655"] = true'],"I5") ]},
{id:2,key:"LoginToken",name:"Login and token refresh",req:"Account -> Authentication -> JWT access/refresh",endpoint:"POST /api/auth/login",conditions:[
["phone",'phone = "0944433222"','phone = "0900000000"'],["password",'password = "Clinic@123"','password = "Wrong@123"'],["account status",'account.isActive = true','account.isActive = false'],["refresh token",'token.type = "REFRESH"','token.type = "ACCESS"'],["token expiry",'token.expiresAt = "2026-08-13T10:00:00"','token.expiresAt = "2026-08-11T10:00:00"'],["authorization",'Authorization = "Bearer eyJ.valid"','Authorization = null']],cases:[
tc("Login with valid phone and password",['phone = "0944433222"','password = "Clinic@123"','POST /api/auth/login'],['httpStatus = 200','response.accessToken != null','response.refreshToken != null','response.systemRole = "CUSTOMER"'],['account.phone = "0944433222"','account.password = "Clinic@123"','account.isActive = true'],"V1, V2, V3"),
tc("Reject incorrect password",['phone = "0944433222"','password = "Wrong@123"','POST /api/auth/login'],['httpStatus = 400','response.accessToken = null','response.refreshToken = null'],['account.phone = "0944433222"','account.password = "Clinic@123"','account.isActive = true'],"I2"),
tc("Reject inactive account",['phone = "0944433222"','password = "Clinic@123"','POST /api/auth/login'],['httpStatus = 400','response.code = "ACCOUNT_INACTIVE"','response.accessToken = null'],['account.phone = "0944433222"','account.isActive = false'],"I3"),
tc("Refresh tokens with refresh token",['refreshToken = "eyJ.refresh.valid"','POST /api/auth/refresh'],['httpStatus = 200','response.accessToken != null','response.refreshToken != null'],['token.type = "REFRESH"','token.revoked = false','token.expiresAt = "2026-08-13T10:00:00"'],"V4, V5"),
tc("Reject access token on refresh endpoint",['refreshToken = "eyJ.access.valid"','POST /api/auth/refresh'],['httpStatus = 400','response.code = "INVALID_TOKEN_TYPE"','response.accessToken = null'],['token.type = "ACCESS"'],"I4"),
tc("Reject request without bearer token",['Authorization = null','GET /api/auth/me'],['httpStatus = 401','response.code = "UNAUTHORIZED"','response.accountId = null'],['securityContext.authentication = null'],"I6") ]},
{id:3,key:"Profile",name:"Customer profile management",req:"Account -> Profile -> Insurance lookup",endpoint:"PUT /api/v1/profiles/me",conditions:[
["profile owner",'profile.accountId = "ACC-1001"','profile.accountId = "ACC-2002"'],["fullName",'fullName = "John Carter"','fullName = ""'],["phone",'phone = "0944433222"','phone = "123"'],["email",'email = "john.carter@example.com"','email = "john.example.com"'],["dateOfBirth",'dateOfBirth = "1998-05-20"','dateOfBirth = "2027-01-01"'],["insuranceCode",'insuranceCode = "BH1234567890123"','insuranceCode = "BH0000000000000"']],cases:[]},
{id:4,key:"Appointment",name:"Appointment booking and check-in",req:"Profile -> Appointment -> Visit -> Invoice",endpoint:"POST /api/v1/appointments",conditions:[
["customerId",'customerId = "CUS-1001"','customerId = "CUS-9999"'],["scheduledAt",'scheduledAt = "2026-08-13T09:00:00"','scheduledAt = "2026-08-11T09:00:00"'],["serviceId",'serviceId = "SRV-GENERAL-01"','serviceId = "SRV-UNKNOWN"'],["appointment status",'appointment.status = "PENDING"','appointment.status = "CHECKED_IN"'],["active visit",'activeVisit.count = 0','activeVisit.count = 1'],["staff role",'staff.systemRole = "RECEPTIONIST"','staff.systemRole = "CUSTOMER"']],cases:[]},
{id:5,key:"PaymentQueue",name:"Invoice payment and queue creation",req:"Invoice -> Transaction -> QueueTicket",endpoint:"POST /api/v1/invoices/INV-1001/pay",conditions:[
["invoice status",'invoice.status = "PENDING"','invoice.status = "PAID"'],["amount",'amount = 500000','amount = 499999'],["paymentMethod",'paymentMethod = "CASH"','paymentMethod = "BITCOIN"'],["invoice item count",'invoiceItem.count = 1','invoiceItem.count = 0'],["transaction status",'transaction.status = "PENDING"','transaction.status = "SUCCESS"'],["queue count",'queueTicket.count = 0','queueTicket.count = 1']],cases:[]},
{id:6,key:"QueueExam",name:"Queue handling and examination start",req:"QueueTicket -> CustomerVisit -> VitalSigns -> MedicalRecord",endpoint:"POST /api/v1/queue-tickets/QUE-1001/call",conditions:[
["queue status",'queueTicket.status = "WAITING"','queueTicket.status = "DONE"'],["department",'departmentId = "DEP-GENERAL"','departmentId = "DEP-LAB"'],["staff role",'staff.systemRole = "DOCTOR"','staff.systemRole = "CUSTOMER"'],["visit status",'customerVisit.status = "CHECKED_IN"','customerVisit.status = "COMPLETED"'],["temperature",'temperature = 36.8','temperature = 50.0'],["medical record count",'medicalRecord.count = 0','medicalRecord.count = 1']],cases:[]},
{id:7,key:"Examination",name:"Diagnosis and prescription",req:"Visit -> MedicalRecord -> ICD-10 -> Prescription",endpoint:"PUT /api/doctor/examinations/MR-1001",conditions:[
["record status",'medicalRecord.status = "IN_PROGRESS"','medicalRecord.status = "COMPLETED"'],["doctorId",'doctorId = "STF-DOC-01"','doctorId = "STF-DOC-99"'],["icd10Code",'icd10Code = "J00"','icd10Code = "ZZZ"'],["medicineId",'medicineId = "MED-001"','medicineId = "MED-999"'],["quantity",'quantity = 10','quantity = 0'],["dose",'dose = "1 tablet twice daily"','dose = ""']],cases:[]},
{id:8,key:"TestOrder",name:"Clinical test ordering",req:"MedicalRecord -> TestRequest -> Department -> QueueTicket",endpoint:"POST /api/v1/test-requests",conditions:[
["medicalRecordId",'medicalRecordId = "MR-1001"','medicalRecordId = "MR-9999"'],["serviceId",'serviceId = "SRV-LAB-CBC"','serviceId = "SRV-UNKNOWN"'],["department status",'department.status = "ACTIVE"','department.status = "MAINTENANCE"'],["request count",'sameServiceRequest.count = 0','sameServiceRequest.count = 1'],["serviceIds",'serviceIds = ["SRV-LAB-CBC"]','serviceIds = []'],["queue status",'queueTicket.status = "WAITING"','queueTicket.status = "BLOCKED"']],cases:[]},
{id:9,key:"LabResult",name:"Laboratory result completion",req:"TestRequest -> TestResult -> QueueTicket -> PatientJourney",endpoint:"POST /api/v1/test-requests/TR-1001/result/complete",conditions:[
["request status",'testRequest.status = "IN_PROGRESS"','testRequest.status = "COMPLETED"'],["resultFile",'resultFile = "cbc-result.pdf"','resultFile = "cbc-result.jpg"'],["fileSize",'fileSize = 1048576','fileSize = 10485761'],["conclusion",'conclusion = "CBC values within reference range"','conclusion = ""'],["verifiedBy",'verifiedBy = "STF-HEAD-LAB-01"','verifiedBy = "STF-DOC-01"'],["queue status",'queueTicket.status = "IN_PROGRESS"','queueTicket.status = "WAITING"']],cases:[]},
{id:10,key:"CompleteFollowup",name:"Examination completion and follow-up",req:"MedicalRecord -> Visit -> Queue -> Follow-up Appointment",endpoint:"POST /api/v1/medical-records/MR-1001/complete",conditions:[
["record status",'medicalRecord.status = "IN_PROGRESS"','medicalRecord.status = "COMPLETED"'],["incomplete tests",'incompleteTestRequest.count = 0','incompleteTestRequest.count = 1'],["visit status",'customerVisit.status = "IN_PROGRESS"','customerVisit.status = "COMPLETED"'],["followUpDate",'followUpDate = "2026-08-20"','followUpDate = "2026-08-10"'],["serviceId",'serviceId = "SRV-GENERAL-01"','serviceId = "SRV-UNKNOWN"'],["rating",'rating = 5','rating = 6']],cases:[]},
{id:11,key:"InsuranceTxn",name:"Insurance and payment transaction",req:"Insurance -> Invoice -> Transaction -> Payment History",endpoint:"POST /api/v1/invoices/INV-1001/insurance",conditions:[
["insuranceCode",'insuranceCode = "BH1234567890123"','insuranceCode = "BH0000000000000"'],["insurance status",'insurance.status = "ACTIVE"','insurance.status = "EXPIRED"'],["invoice status",'invoice.status = "PENDING"','invoice.status = "PAID"'],["coveredAmount",'coveredAmount = 400000','coveredAmount = 600000'],["transaction status",'transaction.status = "PENDING"','transaction.status = "SUCCESS"'],["invoice owner",'invoice.customerId = "CUS-1001"','invoice.customerId = "CUS-2002"']],cases:[]},
{id:12,key:"StaffSchedule",name:"Staff scheduling and attendance",req:"Staff -> Capability -> Schedule -> Attendance",endpoint:"POST /api/v1/schedules",conditions:[
["staff status",'staff.isActive = true','staff.isActive = false'],["staff role",'staff.systemRole = "DOCTOR"','staff.systemRole = "CUSTOMER"'],["shift start",'startAt = "2026-08-13T08:00:00"','startAt = "2026-08-13T08:30:00"'],["shift end",'endAt = "2026-08-13T12:00:00"','endAt = "2026-08-13T07:00:00"'],["capability",'capability.serviceId = "SRV-GENERAL-01"','capability.count = 0'],["attendance token",'token = "KIOSK-VALID-001"','token = "KIOSK-EXPIRED-001"']],cases:[]},
{id:13,key:"AdminCatalog",name:"Administrative catalog management",req:"Department -> Service -> Capability",endpoint:"POST /api/v1/admin/medical-services",conditions:[
["admin role",'actor.systemRole = "ADMIN"','actor.systemRole = "CUSTOMER"'],["serviceCode",'serviceCode = "SRV-CARDIO-01"','serviceCode = "SRV-GENERAL-01"'],["serviceName",'serviceName = "Cardiology Consultation"','serviceName = ""'],["price",'price = 500000','price = -1'],["service status",'service.status = "ACTIVE"','service.status = "INACTIVE"'],["dependency count",'dependency.count = 0','dependency.count = 1']],cases:[]},
{id:14,key:"NotifyChat",name:"Notification and support chat",req:"Account -> Notification -> ChatSession -> WebSocket",endpoint:"POST /api/v1/chat/CHAT-1001/messages/customer",conditions:[
["recipientId",'recipientId = "ACC-1001"','recipientId = "ACC-9999"'],["channel",'channel = "IN_APP"','channel = "FAX"'],["notification status",'notification.status = "SENT"','notification.status = "READ"'],["chat status",'chatSession.status = "ACTIVE"','chatSession.status = "CLOSED"'],["sender role",'sender.systemRole = "CUSTOMER"','sender.systemRole = "ADMIN"'],["message",'message = "I need appointment support"','message = ""']],cases:[]},
{id:15,key:"JourneyReport",name:"Patient journey, reporting, and feedback",req:"Visit -> Queue -> Record -> Invoice -> Report",endpoint:"GET /api/v1/patient-journeys/VIS-1001",conditions:[
["visit owner",'visit.customerId = "CUS-1001"','visit.customerId = "CUS-2002"'],["current step",'currentStep.status = "DONE"','currentStep.status = "IN_PROGRESS"'],["dateFrom",'dateFrom = "2026-08-01"','dateFrom = "2026-08-31"'],["dateTo",'dateTo = "2026-08-31"','dateTo = "2026-08-01"'],["manager role",'actor.systemRole = "CLINIC_MANAGER"','actor.systemRole = "CUSTOMER"'],["rating",'rating = 5','rating = 0']],cases:[]}
];

// Generate exact-value cases for flows 3-15 from their concrete conditions.
for (const f of flows.filter(x=>x.cases.length===0)) {
  const c=f.conditions;
  const entity=f.key.toUpperCase();
  f.cases=[
    tc(`Create or process ${f.name.toLowerCase()}`,[c[0][1],c[1][1],c[2][1],f.endpoint],["httpStatus = 200",`response.flow = "IT${String(f.id).padStart(2,"0")}"`,`database.updatedRows = 1`],[c[0][1],c[1][1]],"V1, V2, V3"),
    tc(`Reject invalid ${c[0][0]}`,[c[0][2],c[1][1],f.endpoint],["httpStatus = 404",`response.code = "${entity}_NOT_FOUND"`,`database.updatedRows = 0`],[c[0][2]],"I1"),
    tc(`Reject invalid ${c[1][0]}`,[c[0][1],c[1][2],f.endpoint],["httpStatus = 400",`response.code = "INVALID_${c[1][0].toUpperCase().replaceAll(" ","_")}"`,`database.updatedRows = 0`],[c[0][1]],"I2"),
    tc(`Accept boundary value for ${c[3][0]}`,[c[3][1],c[4][1],f.endpoint],["httpStatus = 200",`response.${c[3][0].replaceAll(" ","")} = ${JSON.stringify(c[3][1].split(" = ")[1]??"accepted")}`,`database.updatedRows = 1`],[c[0][1]],"B4, V5"),
    tc(`Reject invalid ${c[4][0]}`,[c[4][2],f.endpoint],["httpStatus = 400",`response.code = "INVALID_${c[4][0].toUpperCase().replaceAll(" ","_")}"`,`database.updatedRows = 0`],[c[0][1]],"I5"),
    tc(`Reject invalid ${c[5][0]}`,[c[5][2],f.endpoint],["httpStatus = 409",`response.code = "INVALID_${c[5][0].toUpperCase().replaceAll(" ","_")}"`,`database.updatedRows = 0`],[c[5][2]],"I6")
  ];
}

const featureName = f => `IT${String(f.id).padStart(2,"0")}-${f.key}`.slice(0,31);
for (const f of flows) {
  const ca=wb.worksheets.getItem(`CA-IT${String(f.id).padStart(2,"0")}-${f.key}`.slice(0,31));
  ca.getRange("A1:I1").values=[["Test Condition Analysis",null,null,null,null,null,null,null,null]];
  ca.getRange("A2:I2").values=[["Variable","Valid Partition","Tag","Invalid Partition","Tag","Valid Boundary","Tag","Invalid Boundary","Tag"]];
  ca.getRange("A3:I26").clear({applyTo:"contents"});
  const caRows=f.conditions.map((x,i)=>[x[0],x[1],`V${i+1}`,x[2],`I${i+1}`,x[1],`B${i+1}`,x[2],`IB${i+1}`]);
  ca.getRange(`A3:I${2+caRows.length}`).values=caRows;
  ca.getRange("A27:I27").values=[["Notes:","Every partition uses an explicit variable value. Tags are referenced by Test Design and Feature sheets.",null,null,null,null,null,null,null]];

  const td=wb.worksheets.getItem(`TD-IT${String(f.id).padStart(2,"0")}-${f.key}`.slice(0,31));
  td.getRange("A1:D1").values=[["Test Case Design",null,null,null]];
  td.getRange("A2:D2").values=[["Test Case","Description","Expected Outcome","New Tag Covered"]];
  td.getRange("A3:D21").clear({applyTo:"contents"});
  td.getRange("A3:D8").values=f.cases.map((x,i)=>[i+1,x.procedure,x.expected,x.tag]);

  const sh=wb.worksheets.getItem(featureName(f));
  sh.getRange("B2").values=[[`IT${String(f.id).padStart(2,"0")} - ${f.name}`]];
  sh.getRange("B3").values=[[f.req]];
  sh.getRange("A10:O10").values=[["Test Case ID","Test Case Description","Test Case Procedure","Expected Results","Pre-conditions","Round 1","Test date","Tester","Round 2","Test date","Tester","Round 3","Test date","Tester","Note"]];
  const rows=[]; let idx=0;
  for (const group of ["Valid concrete input values","Invalid concrete input values","Boundary and state values"]) {
    rows.push([group,null,null,null,null,null,null,null,null,null,null,null,null,null,null]);
    for(let k=0;k<2;k++) { const x=f.cases[idx]; rows.push([`IT${String(f.id).padStart(2,"0")}-${String(idx+1).padStart(3,"0")}`,x.name,x.procedure,x.expected,x.pre,"Pending",null,null,"Pending",null,null,"Pending",null,null,x.tag]); idx++; }
  }
  sh.getRange("A11:O19").values=rows;
}

const cover=wb.worksheets.getItem("Cover");
cover.getRange("E11").values=[["Full English edition with concrete test data values."]];
const list=wb.worksheets.getItem("Test Cases");
list.getRange("C9:C23").values=flows.map(f=>[`IT${String(f.id).padStart(2,"0")} - ${f.name}`]);
list.getRange("E9:E23").values=flows.map(f=>[f.name]);
list.getRange("F9:F23").values=flows.map(()=>["See explicit values in the Feature sheet"]);
const msg=wb.worksheets.getItem("MessageList");
const msgRows=[]; for(const f of flows){msgRows.push([`IT${String(f.id).padStart(2,"0")}-INVALID`,`The submitted variable value is invalid.`,`IT${String(f.id).padStart(2,"0")}`]);msgRows.push([`IT${String(f.id).padStart(2,"0")}-STATE`,`The stored entity state does not allow this operation.`,`IT${String(f.id).padStart(2,"0")}`]);}
msg.getRange("A2:C31").values=msgRows;

const errors=await wb.inspect({kind:"match",searchTerm:"#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A",options:{useRegex:true,maxResults:300},maxChars:5000});
console.log(errors.ndjson);
const vietnamese=await wb.inspect({kind:"match",searchTerm:"Đăng|đăng|khám|Khám|Luồng|nghiệp|hợp lệ|mật khẩu|tài khoản|bệnh nhân|thanh toán",options:{useRegex:true,maxResults:300},maxChars:10000});
console.log(vietnamese.ndjson);
for (const [name,range,file] of [["TD-IT02-LoginToken","A1:D8","english_td_login.png"],["IT02-LoginToken","A1:O19","english_feature_login.png"],["CA-IT06-QueueExam","A1:I10","english_ca_queue.png"]]) {
  const pic=await wb.render({sheetName:name,range,scale:1,format:"png"});
  await fs.writeFile(`D:/gitlap/doAnSummer2026/work/${file}`,new Uint8Array(await pic.arrayBuffer()));
}
const out=await SpreadsheetFile.exportXlsx(wb); await out.save(output); console.log(output);
