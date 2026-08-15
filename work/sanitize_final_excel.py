import os, shutil, zipfile, posixpath
from xml.etree import ElementTree as ET

SRC=r"D:\gitlap\doAnSummer2026\outputs\INTEGRATION_TEST_CONCRETE_DATA_ENGLISH.xlsx"
OUT=r"D:\gitlap\doAnSummer2026\outputs\INTEGRATION_TEST_ENGLISH_EXCEL_FIXED.xlsx"
TMP=r"D:\gitlap\doAnSummer2026\work\sanitized_final"
MAIN="http://schemas.openxmlformats.org/spreadsheetml/2006/main"

if os.path.exists(TMP): shutil.rmtree(TMP)
os.makedirs(TMP)
with zipfile.ZipFile(SRC) as z: z.extractall(TMP)

# Remove legacy comment drawings from worksheets and relationships.
wsdir=os.path.join(TMP,"xl","worksheets")
relsdir=os.path.join(wsdir,"_rels")
for f in os.listdir(wsdir):
    if not (f.startswith("sheet") and f.endswith(".xml")): continue
    p=os.path.join(wsdir,f); tree=ET.parse(p); root=tree.getroot(); changed=False
    for node in list(root):
        if node.tag in {f"{{{MAIN}}}legacyDrawing",f"{{{MAIN}}}legacyDrawingHF"}:
            root.remove(node); changed=True
    if changed: tree.write(p,encoding="utf-8",xml_declaration=True)
    rp=os.path.join(relsdir,f+".rels")
    if os.path.exists(rp):
        rt=ET.parse(rp); rr=rt.getroot()
        for rel in list(rr):
            typ=rel.get("Type","").lower()
            if typ.endswith("/comments") or typ.endswith("/vmldrawing") or "threadedcomment" in typ or "person" in typ:
                rr.remove(rel)
        if len(rr): rt.write(rp,encoding="utf-8",xml_declaration=True)
        else: os.remove(rp)

# Remove comment, VML, threaded-comment and person parts.
for root,dirs,files in os.walk(TMP):
    for f in files:
        low=f.lower(); rel=os.path.relpath(os.path.join(root,f),TMP).replace('\\','/').lower()
        if (low.startswith("comments") and low.endswith(".xml")) or low.startswith("vmldrawing") or "threadedcomments" in rel or rel.startswith("xl/persons/"):
            os.remove(os.path.join(root,f))

# Remove corresponding content-type entries and VML defaults.
ctp=os.path.join(TMP,"[Content_Types].xml"); ct=ET.parse(ctp); cr=ct.getroot()
for node in list(cr):
    part=node.get("PartName","").lower(); ext=node.get("Extension","").lower(); typ=node.get("ContentType","").lower()
    if "comment" in part or "persons" in part or "comment" in typ or "person" in typ or ext=="vml": cr.remove(node)
ct.write(ctp,encoding="utf-8",xml_declaration=True)

# Remove calcChain if present; Excel will calculate using fullCalcOnLoad.
chain=os.path.join(TMP,"xl","calcChain.xml")
if os.path.exists(chain): os.remove(chain)
wbrel=os.path.join(TMP,"xl","_rels","workbook.xml.rels")
rt=ET.parse(wbrel); rr=rt.getroot()
for rel in list(rr):
    typ=rel.get("Type","").lower(); target=rel.get("Target","").lower()
    if typ.endswith("/calcchain") or "person" in typ or "/persons/" in target: rr.remove(rel)
rt.write(wbrel,encoding="utf-8",xml_declaration=True)
wbp=os.path.join(TMP,"xl","workbook.xml"); wt=ET.parse(wbp); wr=wt.getroot()
calc=wr.find(f"{{{MAIN}}}calcPr")
if calc is None: calc=ET.SubElement(wr,f"{{{MAIN}}}calcPr")
calc.set("calcMode","auto"); calc.set("fullCalcOnLoad","1"); calc.set("forceFullCalc","1")
wt.write(wbp,encoding="utf-8",xml_declaration=True)

if os.path.exists(OUT): os.remove(OUT)
with zipfile.ZipFile(OUT,"w",zipfile.ZIP_DEFLATED) as z:
    for root,_,files in os.walk(TMP):
        for f in files:
            p=os.path.join(root,f); z.write(p,os.path.relpath(p,TMP))
print(OUT)
