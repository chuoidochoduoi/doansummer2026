import os, shutil, zipfile
from xml.etree import ElementTree as ET

SRC = r"D:\gitlap\doAnSummer2026\outputs\Report5_IntegrationTest_Clinic_Full.xlsx"
OUT = r"D:\gitlap\doAnSummer2026\outputs\INTEGRATION_TEST_PHONG_KHAM_DUNG_GIAO_DIEN.xlsx"
TMP = r"D:\gitlap\doAnSummer2026\work\fixed_ui_unpack"
MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
PKG = "http://schemas.openxmlformats.org/package/2006/relationships"

if os.path.exists(TMP):
    shutil.rmtree(TMP)
os.makedirs(TMP)
with zipfile.ZipFile(SRC) as z:
    z.extractall(TMP)

wsdir = os.path.join(TMP, "xl", "worksheets")
relsdir = os.path.join(wsdir, "_rels")

# Legacy comments/VML copied from a template may not be shared by many sheets.
# Remove only those hidden relationships; retain cell styles, layout, print setup,
# validations, formulas, hyperlinks, logo and all visible workbook content.
for name in os.listdir(wsdir):
    if not (name.startswith("sheet") and name.endswith(".xml")):
        continue
    path = os.path.join(wsdir, name)
    tree = ET.parse(path)
    root = tree.getroot()
    changed = False
    for node in list(root):
        if node.tag in {f"{{{MAIN}}}legacyDrawing", f"{{{MAIN}}}legacyDrawingHF"}:
            root.remove(node)
            changed = True
    if changed:
        tree.write(path, encoding="utf-8", xml_declaration=True)

    relpath = os.path.join(relsdir, name + ".rels")
    if os.path.exists(relpath):
        rtree = ET.parse(relpath)
        rroot = rtree.getroot()
        for rel in list(rroot):
            typ = rel.get("Type", "")
            if typ.endswith("/comments") or typ.endswith("/vmlDrawing"):
                rroot.remove(rel)
        if len(rroot) == 0:
            os.remove(relpath)
        else:
            rtree.write(relpath, encoding="utf-8", xml_declaration=True)

# Remove detached comment/VML parts and their content-type declarations.
for folder, prefix in [(os.path.join(TMP, "xl"), "comments"),
                       (os.path.join(TMP, "xl", "drawings"), "vmlDrawing")]:
    if os.path.isdir(folder):
        for f in os.listdir(folder):
            if f.startswith(prefix):
                os.remove(os.path.join(folder, f))

ctpath = os.path.join(TMP, "[Content_Types].xml")
ctree = ET.parse(ctpath)
ctroot = ctree.getroot()
for node in list(ctroot):
    part = node.get("PartName", "")
    if "/comments" in part or "/vmlDrawing" in part:
        ctroot.remove(node)
ctree.write(ctpath, encoding="utf-8", xml_declaration=True)

if os.path.exists(OUT):
    os.remove(OUT)
with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as z:
    for root, _, files in os.walk(TMP):
        for f in files:
            p = os.path.join(root, f)
            z.write(p, os.path.relpath(p, TMP))
print(OUT)
