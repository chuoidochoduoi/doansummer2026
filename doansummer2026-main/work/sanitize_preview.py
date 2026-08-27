import os, shutil, zipfile
from xml.etree import ElementTree as ET
src=r'D:\gitlap\doAnSummer2026\outputs\Report5_IntegrationTest_Clinic_Full.xlsx'
tmp=r'D:\gitlap\doAnSummer2026\work\preview_unpack';out=r'D:\gitlap\doAnSummer2026\work\preview_no_logo.xlsx'
if os.path.exists(tmp):shutil.rmtree(tmp)
os.makedirs(tmp)
with zipfile.ZipFile(src) as z:z.extractall(tmp)
ns='http://schemas.openxmlformats.org/spreadsheetml/2006/main'
p=os.path.join(tmp,'xl','worksheets','sheet1.xml');t=ET.parse(p);r=t.getroot()
for x in list(r):
    if x.tag==f'{{{ns}}}drawing':r.remove(x)
t.write(p,encoding='utf-8',xml_declaration=True)
for rel in ['xl/worksheets/_rels/sheet1.xml.rels']:
    rp=os.path.join(tmp,rel)
    if os.path.exists(rp):os.remove(rp)
# Keep detached drawing/media parts in the package so OpenXML content-type
# declarations still resolve; removing the sheet relationship is sufficient.
with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as z:
    for root,_,files in os.walk(tmp):
        for f in files:
            p=os.path.join(root,f);z.write(p,os.path.relpath(p,tmp))
print(out)
