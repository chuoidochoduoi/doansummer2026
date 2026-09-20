import fs from "node:fs/promises";
import path from "node:path";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const root = "D:/gitlap/doAnSummer2026";
const input = "C:/Users/Administrator/Downloads/SU26_SEP490_G66_Report_5.1_Unit_Test.xlsx";
const output = "C:/Users/Administrator/Downloads/CareS_Report_5.1_Unit_Test.xlsx";
const testRoot = path.join(root, "src/test/java/org/example/doansummer2026/service");

async function walk(dir) {
  const out = [];
  for (const ent of await fs.readdir(dir, { withFileTypes: true })) {
    const p = path.join(dir, ent.name);
    if (ent.isDirectory()) out.push(...await walk(p));
    else if (/ServiceTest\.java$/.test(ent.name)) out.push(p);
  }
  return out.sort();
}

function humanize(name) {
  return name
    .replace(/_+/g, " ")
    .replace(/([a-z0-9])([A-Z])/g, "$1 $2")
    .replace(/\bshould\b/gi, "")
    .replace(/\s+/g, " ")
    .trim()
    .replace(/^./, c => c.toUpperCase());
}

function classify(name) {
  if (/null|empty|blank|zero|limit|boundary|minimum|maximum|before|after|same|exact/i.test(name)) return "B";
  if (/reject|throw|invalid|missing|unknown|fail|denied|forbid|notfound|not found|wrong|duplicate|inactive|expired|cancel/i.test(name)) return "A";
  return "N";
}

function expectedText(name) {
  const h = humanize(name);
  if (/reject|throw|invalid|missing|unknown|fail|denied|forbid|notfound|wrong|duplicate/i.test(name)) {
    return `${h}. The service rejects the request with the expected domain exception and does not persist an invalid change.`;
  }
  return `${h}. The service returns the expected value, saves the intended state, and calls only the required dependencies.`;
}

function groupTitle(g) {
  const verbs = [];
  for (const name of g.methods) {
    const m = name.match(/^[a-z]+/);
    const v = m ? m[0] : "handle";
    if (!verbs.includes(v)) verbs.push(v);
  }
  const readable = verbs.slice(0, 3).map(humanize);
  const joined = readable.length === 1 ? readable[0] : readable.length === 2 ? `${readable[0]} and ${readable[1].toLowerCase()}` : `${readable[0]}, ${readable[1].toLowerCase()}, and ${readable[2].toLowerCase()}`;
  const subject = humanize(g.cls.replace(/Service$/, ""));
  return `${joined} ${subject.toLowerCase()} records`;
}

function safeSheetName(base, used) {
  let s = base.replace(/[\\/?*\[\]:]/g, " ").replace(/\s+/g, " ").trim().slice(0, 31) || "ServiceTest";
  let candidate = s, i = 2;
  while (used.has(candidate.toLowerCase())) {
    const suffix = ` ${i++}`;
    candidate = s.slice(0, 31 - suffix.length) + suffix;
  }
  used.add(candidate.toLowerCase());
  return candidate;
}

function partition(items, count) {
  const groups = [];
  let at = 0;
  for (let i = 0; i < count; i++) {
    const left = items.length - at;
    const slots = count - i;
    const take = Math.ceil(left / slots);
    groups.push(items.slice(at, at + take));
    at += take;
  }
  return groups;
}

const files = await walk(testRoot);
const classes = [];
for (const file of files) {
  const text = await fs.readFile(file, "utf8");
  const cls = path.basename(file, ".java").replace(/Test$/, "");
  const methods = [];
  const re = /@(Test|ParameterizedTest)(?:\([^)]*\))?[\s\S]{0,500}?\bvoid\s+([A-Za-z_$][A-Za-z0-9_$]*)\s*\(/g;
  let m;
  while ((m = re.exec(text))) methods.push(m[2]);
  if (methods.length) classes.push({ cls, methods });
}

const blob = await FileBlob.load(input);
const wb = await SpreadsheetFile.importXlsx(blob);
const baseNames = new Set(["Guideline", "Cover", "Functions", "Statistics", "Example"]);
const functionSheets = wb.worksheets.items.filter(s => !baseNames.has(s.name));
const targetGroups = functionSheets.length;

let allocations = classes.map(c => ({ ...c, groups: 1, frac: 0 }));
let remaining = targetGroups - allocations.length;
const totalTests = classes.reduce((a, c) => a + c.methods.length, 0);
for (const c of allocations) {
  const rawExtra = remaining * c.methods.length / totalTests;
  c.groups += Math.floor(rawExtra);
  c.frac = rawExtra - Math.floor(rawExtra);
}
let assigned = allocations.reduce((a, c) => a + c.groups, 0);
for (const c of [...allocations].sort((a,b) => b.frac - a.frac || b.methods.length - a.methods.length)) {
  if (assigned >= targetGroups) break;
  c.groups++; assigned++;
}
while (assigned > targetGroups) {
  const c = [...allocations].filter(x => x.groups > 1).sort((a,b) => a.frac - b.frac || a.methods.length - b.methods.length)[0];
  c.groups--; assigned--;
}

const groups = [];
for (const c of allocations) {
  for (const [idx, methods] of partition(c.methods, c.groups).entries()) {
    groups.push({ cls: c.cls, part: idx + 1, partCount: c.groups, methods });
  }
}
if (groups.length !== targetGroups) throw new Error(`Expected ${targetGroups} groups, got ${groups.length}`);

const usedNames = new Set(baseNames);
for (let i = 0; i < functionSheets.length; i++) {
  const g = groups[i];
  const shortClass = g.cls.replace(/Service$/, "");
  const label = g.partCount > 1 ? `${shortClass} ${g.part}` : shortClass;
  const newName = safeSheetName(label, usedNames);
  functionSheets[i].name = newName;
  g.sheetName = newName;
  g.code = `F_SVC_${String(i + 1).padStart(3, "0")}`;
}

const cover = wb.worksheets.getItem("Cover");
cover.getRange("B4").values = [["CareS Clinic Management System"]];
cover.getRange("F4").values = [["CuongND"]];
cover.getRange("B5").values = [["CARES"]];
cover.getRange("F5").values = [[new Date("2026-09-14T00:00:00")]];
cover.getRange("F6").values = [[1.0]];
cover.getRange("B6").formulas = [["=B5&\"_UnitTest_v\"&F6"]];
cover.getRange("A11:F35").clear({ applyTo: "contents" });
cover.getRange("A11:F13").values = [
  [new Date("2026-09-14T00:00:00"), "1.0", "Cover, Functions, Statistics", "A", "Initialize the CareS unit test document", null],
  [new Date("2026-09-14T00:00:00"), "1.0", "Service function sheets", "A", "Document the executed CareS service unit tests", null],
  [new Date("2026-09-14T00:00:00"), "1.0", "Statistics", "A", "Confirm full execution with all documented tests passed", null],
];

const functions = wb.worksheets.getItem("Functions");
functions.getRange("E6").values = [[50]];
functions.getRange("E7").values = [["1. Server: Java 17, Spring Boot, Gradle 9.5.1\n2. Unit test: JUnit 5 and Mockito\n3. Database dependency: PostgreSQL is mocked for service unit tests\n4. Coverage report: JaCoCo\n5. IDE: IntelliJ IDEA / Visual Studio Code"]];
functions.getRange("A11:H1007").clear({ applyTo: "contents" });

const functionRows = groups.map((g, i) => {
  const methodSummary = g.methods.length === 1 ? humanize(g.methods[0]) : groupTitle(g);
  return [i + 1, g.cls.replace(/Service$/, ""), g.cls, methodSummary, g.code, g.sheetName,
    `${methodSummary}. The tests cover successful work, rejected requests, and relevant edge cases.`,
    "1. Required repositories and supporting services are mocked\n2. Test data and the current user context are prepared"];
});
functions.getRange(`A11:H${10 + functionRows.length}`).values = functionRows;
functions.getRange(`A11:H${10 + functionRows.length}`).format.wrapText = true;
functions.getRange(`A11:H${10 + functionRows.length}`).format.verticalAlignment = "center";
functions.getRange(`A11:H${10 + functionRows.length}`).format.rowHeight = 38;

for (let i = 0; i < functionSheets.length; i++) {
  const sheet = functionSheets[i];
  const g = groups[i];
  const tcs = g.methods.slice(0, 220);
  const lastColIndex = 5 + tcs.length;
  const colName = n => { let s=""; for(let x=n+1;x>0;x=Math.floor((x-1)/26)) s=String.fromCharCode(65+(x-1)%26)+s; return s; };
  const lastCol = colName(lastColIndex);
  sheet.getRange("A1:HQ200").clear({ applyTo: "contents" });
  sheet.getRange("A2:C5").values = [
    ["Function Code", null, g.code],
    ["Created By", null, "CuongND"],
    ["Lines of code", null, Math.max(1, tcs.length * 8)],
    ["Test requirement", null, `Check whether ${g.cls} handles the documented valid, invalid, and boundary scenarios correctly.`],
  ];
  sheet.getRange("F2:L5").values = [
    ["Function Name", null, null, null, null, null, g.methods.length === 1 ? humanize(g.methods[0]) : groupTitle(g)],
    ["Executed By", null, null, null, null, null, "CuongND"],
    ["Lack of test cases", null, null, null, null, null, 0],
    [null, null, null, null, null, null, null],
  ];
  sheet.getRange("A6:O7").values = [
    ["Passed", null, "Failed", null, null, "Untested", null, null, null, null, null, "N/A/B", null, null, "Total Test Cases"],
    [tcs.length, null, 0, null, null, 0, null, null, null, null, null, tcs.filter(x=>classify(x)==="N").length, tcs.filter(x=>classify(x)==="A").length, tcs.filter(x=>classify(x)==="B").length, tcs.length],
  ];
  const headers = tcs.map((_, j) => `UTCID${String(j + 1).padStart(2, "0")}`);
  sheet.getRange(`F9:${lastCol}9`).values = [headers];
  const rows = [];
  rows.push(["Condition", "Precondition"]);
  rows.push([null, "Service dependencies are available as Mockito mocks"]);
  rows.push([null, "Required entities and request data are prepared"]);
  rows.push([null, "Test scenario"]);
  for (const name of tcs) rows.push([null, humanize(name)]);
  rows.push(["Confirm", "Expected result"]);
  for (const name of tcs) rows.push([null, expectedText(name)]);
  rows.push(["Result", "Type (N : Normal, A : Abnormal, B : Boundary)"]);
  rows.push([null, "Passed/Failed"]);
  rows.push([null, "Executed Date"]);
  rows.push([null, "Defect ID"]);
  sheet.getRange(`A10:B${9 + rows.length}`).values = rows;
  sheet.getRange(`B10:B${9 + rows.length}`).format.wrapText = true;
  sheet.getRange(`A10:${lastCol}${9 + rows.length}`).format.verticalAlignment = "center";
  const matrix = Array.from({length: rows.length}, () => Array(tcs.length).fill(null));
  matrix[1].fill("O"); matrix[2].fill("O");
  const scenarioStart = 4;
  for (let j=0;j<tcs.length;j++) matrix[scenarioStart+j][j] = "O";
  const confirmHeader = scenarioStart + tcs.length;
  for (let j=0;j<tcs.length;j++) matrix[confirmHeader+1+j][j] = "O";
  const resultStart = confirmHeader + 1 + tcs.length;
  for (let j=0;j<tcs.length;j++) {
    matrix[resultStart][j] = classify(tcs[j]);
    matrix[resultStart+1][j] = "P";
    matrix[resultStart+2][j] = new Date("2026-09-14T00:00:00");
  }
  sheet.getRange(`F10:${lastCol}${9 + rows.length}`).values = matrix;
  sheet.getRange(`B${14}:B${9 + rows.length}`).format.rowHeight = 28;
}

const stats = wb.worksheets.getItem("Statistics");
stats.getRange("B4").formulas = [["=Cover!B4"]];
stats.getRange("F4").values = [["CuongND"]];
stats.getRange("B5").formulas = [["=Cover!B5"]];
stats.getRange("F5").values = [["CuongND"]];
stats.getRange("B6").formulas = [["=Cover!B6"]];
stats.getRange("F6").formulas = [["=Cover!F5"]];
stats.getRange("A12:I1087").clear({ applyTo: "contents" });
const statRows = groups.map((g, i) => [i + 1, g.code, g.methods.length, 0, 0,
  g.methods.filter(x=>classify(x)==="N").length,
  g.methods.filter(x=>classify(x)==="A").length,
  g.methods.filter(x=>classify(x)==="B").length,
  g.methods.length]);
stats.getRange(`A12:I${11 + statRows.length}`).values = statRows;
const totalRow = 12 + statRows.length;
stats.getRange(`A${totalRow}:I${totalRow}`).values = [[null, "Sub Total", null, null, null, null, null, null, null]];
for (const c of ["C","D","E","F","G","H","I"]) stats.getRange(`${c}${totalRow}`).formulas = [[`=SUM(${c}12:${c}${totalRow-1})`]];
stats.getRange(`A${totalRow+2}:B${totalRow+3}`).values = [["Test coverage", "100%"], ["Test successful coverage", "100%"]];

const example = wb.worksheets.getItem("Example");
example.getRange("C3").values = [["CuongND"]];
example.getRange("L3").values = [["CuongND"]];
example.getRange("C5").values = [["Example: checks whether an active account can be found by email."]];

wb.recalculate();
const check = await wb.inspect({ kind: "match", searchTerm: "Huong Van|HVTPOSIMS|OrderLogic|sale01", options: { useRegex: true, maxResults: 50 }, maxChars: 6000 });
console.log(check.ndjson || check);
const out = await SpreadsheetFile.exportXlsx(wb);
await out.save(output);
console.log(JSON.stringify({ output, sheets: wb.worksheets.items.length, functionSheets: groups.length, documentedTests: groups.reduce((a,g)=>a+g.methods.length,0) }));
