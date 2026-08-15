import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const file = "D:/gitlap/doAnSummer2026/outputs/019ff5dc-080c-7230-b9b2-751838ad3212/INTEGRATION_TEST_FULL_SYSTEM_FUNCTIONS_ALL_PASSED.xlsx";
const wb = await SpreadsheetFile.importXlsx(await FileBlob.load(file));
const functionSheets = wb.worksheets.items.filter(s => /^IT\d{2}-/.test(s.name));
const tcRows = [12, 13, 15, 16, 18, 19];
const problems = [];
for (const sheet of functionSheets) {
  const requirement = String(sheet.getRange("B3").values[0][0] ?? "");
  if (!/^Verify that /.test(requirement) || requirement.includes("->") || /\bflow\b/i.test(requirement)) {
    problems.push(`${sheet.name}: invalid requirement wording`);
  }
  const summary = sheet.getRange("B6:E8").values;
  if (summary.some(row => row.join("|") !== "6|0|0|0")) problems.push(`${sheet.name}: round summary is not all passed`);
  for (const row of tcRows) {
    for (const col of ["F", "I", "L"]) {
      if (sheet.getRange(`${col}${row}`).values[0][0] !== "Passed") problems.push(`${sheet.name}!${col}${row}: not Passed`);
    }
  }
}
const stats = wb.worksheets.getItem("Test Statistics").getRange("C6:H20").values;
if (stats.some(row => row.join("|") !== "6|0|0|0|6|1")) problems.push("Test Statistics: not all functions are 100% passed");
const flowMatches = await wb.inspect({kind:"match", searchTerm:"\\bflow\\b|->", options:{useRegex:true,maxResults:100}, summary:"flow wording scan", maxChars:3000});
if (!flowMatches.ndjson.includes("matched 0 entries")) problems.push(`Remaining flow wording: ${flowMatches.ndjson}`);
const formulaErrors = await wb.inspect({kind:"match", searchTerm:"#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A", options:{useRegex:true,maxResults:100}, summary:"formula error scan", maxChars:3000});
if (!formulaErrors.ndjson.includes("matched 0 entries")) problems.push(`Formula errors: ${formulaErrors.ndjson}`);
if (functionSheets.length !== 15) problems.push(`Expected 15 function sheets, found ${functionSheets.length}`);
if (wb.worksheets.items.length !== 19) problems.push(`Expected 19 sheets, found ${wb.worksheets.items.length}`);
if (problems.length) {
  console.error(problems.join("\n"));
  process.exit(1);
}
console.log(`VERIFIED: ${wb.worksheets.items.length} sheets; ${functionSheets.length} function sheets; 90 test cases; all three rounds Passed; no flow wording; no formula errors.`);
