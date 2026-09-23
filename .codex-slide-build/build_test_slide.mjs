import fs from "node:fs/promises";
import path from "node:path";
import { pathToFileURL } from "node:url";
import { Presentation, PresentationFile } from "@oai/artifact-tool";

const SKILL_DIR = "C:/Users/Administrator/.codex/plugins/cache/openai-primary-runtime/presentations/26.909.12148/skills/presentations";
const workspaceDir = "D:/gitlap/doAnSummer2026";
const buildDir = path.join(workspaceDir, ".codex-slide-build");
const finalPath = path.join(workspaceDir, "output-slides", "Testing_Overview_1_Minute.pptx");
const referenceImage = "C:/Users/ADMINI~1/AppData/Local/Temp/codex-clipboard-67eb0ba2-e8b9-4d80-849d-d5aba908e351.png";
const pythonExecutable = "C:/Users/Administrator/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe";

const presentation = Presentation.create({ slideSize: { width: 1200, height: 675 } });
const slide = presentation.slides.add();
slide.background.fill = "#FFFFFF";
slide.images.add({
  blob: new Uint8Array(await fs.readFile(referenceImage)),
  contentType: "image/png",
  alt: "CareS presentation template background",
  fit: "cover",
  position: { left: 0, top: 0, width: 1200, height: 675 },
});

function box(left, top, width, height, fill = "#FFFFFF") {
  return slide.shapes.add({
    geometry: "rect",
    position: { left, top, width, height },
    fill,
    line: { fill: "none", width: 0 },
  });
}

function textBox(text, left, top, width, height, style = {}) {
  const shape = slide.shapes.add({
    geometry: "textbox",
    position: { left, top, width, height },
    fill: "none",
    line: { fill: "none", width: 0 },
  });
  shape.text = text;
  shape.text.style = {
    typeface: "Arial",
    fontSize: style.fontSize ?? 20,
    bold: style.bold ?? false,
    color: style.color ?? "#202020",
    alignment: style.alignment ?? "left",
    verticalAlignment: style.verticalAlignment ?? "top",
    autoFit: "shrinkText",
  };
  return shape;
}

// Cover the original title and add the revised one.
box(390, 46, 420, 74, "#FFFFFF");
textBox("Testing Overview", 340, 57, 520, 56, {
  fontSize: 34, bold: true, alignment: "center", verticalAlignment: "middle",
});

const items = [
  { no: "01", title: "Unit Test", body: "Service logic, validation\nand exception handling", result: "1,440 / 1,440 passed" },
  { no: "02", title: "Integration Test", body: "Component contracts and\nconnected workflows", result: "Round 2: 189 / 189 passed" },
  { no: "03", title: "System Test", body: "End-to-end workflows\nfor operational roles", result: "Round 2: 60 / 60 passed" },
  { no: "04", title: "Acceptance Test", body: "User-facing requirements\nand business scenarios", result: "110 / 110 TRUE" },
];

const startX = 122;
const gap = 18;
const colW = 225;
items.forEach((item, index) => {
  const x = startX + index * (colW + gap);
  textBox(item.no, x, 205, colW, 46, { fontSize: 29, bold: true, color: "#35B7C7" });
  textBox(item.title, x, 251, colW, 45, { fontSize: 23, bold: true, color: "#111111" });
  box(x, 303, 54, 4, "#35B7C7");
  textBox(item.body, x, 326, colW, 76, { fontSize: 17, color: "#4E5963" });
  textBox(item.result, x, 422, colW, 48, { fontSize: 17, bold: true, color: "#078E96" });
});

slide.speakerNotes.textFrame.setText(
  "Trong dự án CareS, nhóm thực hiện bốn cấp kiểm thử. Unit Test kiểm tra logic service, validation và xử lý exception, với 1.440 test đều đạt. Integration Test kiểm tra sự phối hợp giữa các thành phần và các luồng nghiệp vụ, Round 2 đạt 189 trên 189. System Test kiểm tra các luồng hoàn chỉnh theo từng vai trò như lễ tân, bác sĩ, y tá và thu ngân, Round 2 đạt 60 trên 60. Cuối cùng, Acceptance Test kiểm tra các chức năng người dùng trực tiếp thao tác trên giao diện, gồm 110 trường hợp và tất cả đều đạt."
);

await fs.mkdir(buildDir, { recursive: true });
await fs.mkdir(path.dirname(finalPath), { recursive: true });
const candidatePath = path.join(buildDir, "testing-overview-candidate.pptx");
await (await PresentationFile.exportPptx(presentation)).save(candidatePath);

const { finalizePresentation } = await import(pathToFileURL(
  path.join(SKILL_DIR, "container_tools/artifact_tool_utils.mjs")
).href);

await finalizePresentation({
  explicitTotalSlideCount: 1,
  requiredNativeTableOwnerSlides: [],
  requiredNativeChartOwnerSlides: [],
  workspaceDir,
  candidatePath,
  finalPath,
  pythonExecutable,
  integrityValidatorPath: path.join(SKILL_DIR, "container_tools/inspect_presentation_package_integrity.py"),
  layoutValidatorPath: path.join(SKILL_DIR, "container_tools/inspect_presentation_layout_geometry.py"),
  layoutArgs: ["--expected-slide-size-emu", "11430000,6429375", "--validate-heading-fit"],
  fontPolicy: { basis: "design", families: ["Arial"] },
  verifyArtifactToolImport: true,
  receiptPath: path.join(buildDir, "Testing_Overview_1_Minute.validation.json"),
});

const preview = await presentation.export({ slide, format: "png", scale: 1 });
await fs.writeFile(path.join(buildDir, "Testing_Overview_1_Minute.png"), new Uint8Array(await preview.arrayBuffer()));
console.log(finalPath);
