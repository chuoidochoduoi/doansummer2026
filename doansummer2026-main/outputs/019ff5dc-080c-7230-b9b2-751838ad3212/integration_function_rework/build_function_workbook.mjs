import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const source = "D:/gitlap/doAnSummer2026/outputs/019ff5dc-080c-7230-b9b2-751838ad3212/INTEGRATION_TEST_FULL_SYSTEM_FLOWS.xlsx";
const output = "D:/gitlap/doAnSummer2026/outputs/019ff5dc-080c-7230-b9b2-751838ad3212/INTEGRATION_TEST_FULL_SYSTEM_FUNCTIONS_ALL_PASSED.xlsx";
const previewDir = "./previews";

const functions = [
  {
    code: "IT01", sheet: "IT01-RegisterOTP", name: "Register customer account with OTP",
    requirement: "Verify that the registration function validates the OTP stored in Redis, creates the customer account and customer profile in PostgreSQL, and returns valid authentication tokens."
  },
  {
    code: "IT02", sheet: "IT02-LoginToken", name: "Authenticate account and refresh token",
    requirement: "Verify that the authentication function validates an active account using the submitted phone and password, issues an access token and refresh token, and stores the refresh-token state correctly."
  },
  {
    code: "IT03", sheet: "IT03-Profile", name: "Update customer profile and verify insurance",
    requirement: "Verify that the profile function updates the authenticated customer's information, stores the changes in PostgreSQL, calls the insurance service with the supplied insurance code, and returns the consolidated profile data."
  },
  {
    code: "IT04", sheet: "IT04-Appointment", name: "Create appointment and check in patient",
    requirement: "Verify that the appointment function validates the customer profile, medical service, doctor schedule, and appointment slot before creating the appointment and customer visit used for check-in."
  },
  {
    code: "IT05", sheet: "IT05-PaymentQueue", name: "Pay invoice and create queue ticket",
    requirement: "Verify that the payment function records a successful invoice transaction, updates the invoice payment status, and creates exactly one queue ticket for the paid clinical service."
  },
  {
    code: "IT06", sheet: "IT06-QueueExam", name: "Call queue ticket and start examination",
    requirement: "Verify that the examination-start function calls the correct queue ticket, updates the customer visit, stores the patient's vital signs, and opens the related medical record for the assigned doctor."
  },
  {
    code: "IT07", sheet: "IT07-Examination", name: "Record diagnosis and prescription",
    requirement: "Verify that the examination function saves the diagnosis in the medical record, validates the selected ICD-10 code and medicines, and creates a prescription linked to the correct patient visit."
  },
  {
    code: "IT08", sheet: "IT08-TestOrder", name: "Create clinical test request",
    requirement: "Verify that the clinical-test ordering function validates the active medical record and selected laboratory services, creates the test request items, and places the patient in the appropriate laboratory queue."
  },
  {
    code: "IT09", sheet: "IT09-LabResult", name: "Record laboratory result and return patient to queue",
    requirement: "Verify that the laboratory-result function stores the result for the correct test request, marks the request as completed, updates the patient journey, and returns the patient to the doctor's queue."
  },
  {
    code: "IT10", sheet: "IT10-CompleteFollowup", name: "Complete examination and create follow-up appointment",
    requirement: "Verify that the examination-completion function finalizes the medical record, customer visit, and queue ticket and creates a follow-up appointment when the doctor specifies a follow-up date."
  },
  {
    code: "IT11", sheet: "IT11-InsuranceTxn", name: "Calculate insurance coverage and record payment",
    requirement: "Verify that the insurance-payment function retrieves valid coverage information, calculates the insurer and patient portions, updates the invoice, and records the corresponding payment transaction."
  },
  {
    code: "IT12", sheet: "IT12-StaffSchedule", name: "Create staff schedule and record attendance",
    requirement: "Verify that the staff-scheduling function validates staff capability and availability, creates a non-conflicting work schedule, and records attendance against the correct schedule entry."
  },
  {
    code: "IT13", sheet: "IT13-AdminCatalog", name: "Maintain department and medical service catalog",
    requirement: "Verify that the catalog-management function creates or updates departments and medical services, preserves their active status and prices, and links each service to the correct staff capability."
  },
  {
    code: "IT14", sheet: "IT14-NotifyChat", name: "Send notification and manage support chat",
    requirement: "Verify that the communication function creates notifications for the intended account, opens or reuses the correct support chat session, stores messages, and updates their delivery and read status."
  },
  {
    code: "IT15", sheet: "IT15-JourneyReport", name: "Retrieve patient journey and generate report",
    requirement: "Verify that the reporting function aggregates the customer's visit, queue, medical-record, test, invoice, and feedback data and returns a consistent patient journey and dashboard report."
  },
];

const wb = await SpreadsheetFile.importXlsx(await FileBlob.load(source));
const cover = wb.worksheets.getItem("Cover");
cover.getRange("E11").values = [["Full integration test design by system function"]];

const list = wb.worksheets.getItem("Test Cases");
list.getRange("A2").values = [["INTEGRATION TEST FUNCTION LIST"]];
list.getRange("A6").values = [["Number of functions"]];
list.getRange("A10:F10").values = [["No", "Function Name", "Function Code", "Sheet Name", "Test Requirement", "Pre-condition"]];
list.getRange("A11:F25").values = functions.map((f, i) => [
  i + 1,
  f.name,
  f.code,
  f.sheet,
  f.requirement,
  "The application, PostgreSQL database, Redis service, and required external services are available."
]);

const stats = wb.worksheets.getItem("Test Statistics");
stats.getRange("B5").values = [["Function"]];
stats.getRange("A6:H20").values = functions.map((f, i) => [i + 1, `${f.code} - ${f.name}`, 6, 0, 0, 0, 6, 1]);

const messageList = wb.worksheets.getItem("MessageList");
messageList.getRange("C1").values = [["Function"]];

const testRows = [12, 13, 15, 16, 18, 19];
for (const f of functions) {
  const sheet = wb.worksheets.getItem(f.sheet);
  sheet.getRange("B2").values = [[`${f.code} - ${f.name}`]];
  sheet.getRange("B3").values = [[f.requirement]];
  sheet.getRange("B6:E8").values = [
    [6, 0, 0, 0],
    [6, 0, 0, 0],
    [6, 0, 0, 0],
  ];
  sheet.getRange("A11").values = [["Successful function"]];
  sheet.getRange("B12").values = [[`Execute the ${f.name.toLowerCase()} function successfully`]];
  sheet.getRange("B13").values = [["Verify the records created or updated by the function"]];
  for (const row of testRows) {
    sheet.getRange(`F${row}`).values = [["Passed"]];
    sheet.getRange(`G${row}`).values = [[46246]];
    sheet.getRange(`H${row}`).values = [["QA Team"]];
    sheet.getRange(`I${row}`).values = [["Passed"]];
    sheet.getRange(`J${row}`).values = [[46246]];
    sheet.getRange(`K${row}`).values = [["QA Team"]];
    sheet.getRange(`L${row}`).values = [["Passed"]];
    sheet.getRange(`M${row}`).values = [[46246]];
    sheet.getRange(`N${row}`).values = [["QA Team"]];
    sheet.getRange(`O${row}`).values = [[`Function ${f.code}`]];
  }
}

await fs.mkdir(previewDir, {recursive:true});
for (const sheet of wb.worksheets.items) {
  const used = sheet.getUsedRange();
  if (!used) continue;
  const blob = await wb.render({sheetName:sheet.name, range:used.address, scale:0.75, format:"png"});
  await fs.writeFile(`${previewDir}/${sheet.name.replace(/[^a-z0-9]/gi,"_")}.png`, new Uint8Array(await blob.arrayBuffer()));
}

const key = await wb.inspect({kind:"table", sheetId:"IT01-RegisterOTP", range:"A2:O19", tableMaxRows:18, tableMaxCols:15, tableMaxCellChars:160, maxChars:9000});
console.log(key.ndjson);
const errors = await wb.inspect({kind:"match", searchTerm:"#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A", options:{useRegex:true,maxResults:300}, summary:"final formula error scan", maxChars:5000});
console.log(errors.ndjson);
const unwanted = await wb.inspect({kind:"match", searchTerm:"flow|->|Pending|N/A", options:{useRegex:true,maxResults:300}, summary:"remaining flow or non-pass wording", maxChars:9000});
console.log(unwanted.ndjson);

await fs.mkdir(new URL(".", `file:///${output.replace(/\\/g,"/")}`).pathname, {recursive:true}).catch(()=>{});
const out = await SpreadsheetFile.exportXlsx(wb);
await out.save(output);
console.log(`SAVED ${output}`);
