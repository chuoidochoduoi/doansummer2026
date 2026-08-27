import os,shutil,zipfile
from xml.etree import ElementTree as ET
src=r'D:\gitlap\doAnSummer2026\outputs\IT02_LOGIN_TOKEN_REVIEW_EXACT_TEMPLATE.xlsx'
tmp=r'D:\gitlap\doAnSummer2026\work\it02_render_unpack'
out=r'D:\gitlap\doAnSummer2026\work\it02_render_copy.xlsx'
if os.path.exists(tmp):shutil.rmtree(tmp)
os.makedirs(tmp)
with zipfile.ZipFile(src) as z:z.extractall(tmp)
ns='http://schemas.openxmlformats.org/spreadsheetml/2006/main'
p=os.path.join(tmp,'xl','worksheets','sheet1.xml');t=ET.parse(p);r=t.getroot()
for x in list(r):
    if x.tag==f'{{{ns}}}drawing':r.remove(x)
t.write(p,encoding='utf-8',xml_declaration=True)
rp=os.path.join(tmp,'xl','worksheets','_rels','sheet1.xml.rels')
if os.path.exists(rp):
    rt=ET.parse(rp);rr=rt.getroot()
    for rel in list(rr):
        if rel.get('Type','').endswith('/drawing'):rr.remove(rel)
    rt.write(rp,encoding='utf-8',xml_declaration=True)
# Artifact preview cannot import the template's legacy note drawings. Remove them
# only from this disposable render copy; the delivered workbook stays untouched.
for f in os.listdir(os.path.join(tmp,'xl','worksheets')):
    if not (f.startswith('sheet') and f.endswith('.xml')):continue
    p=os.path.join(tmp,'xl','worksheets',f);t=ET.parse(p);r=t.getroot();changed=False
    for x in list(r):
        if x.tag in {f'{{{ns}}}legacyDrawing',f'{{{ns}}}legacyDrawingHF'}:r.remove(x);changed=True
    if changed:t.write(p,encoding='utf-8',xml_declaration=True)
    rp=os.path.join(tmp,'xl','worksheets','_rels',f+'.rels')
    if os.path.exists(rp):
        rt=ET.parse(rp);rr=rt.getroot()
        for rel in list(rr):
            typ=rel.get('Type','').lower()
            if typ.endswith('/comments') or typ.endswith('/vmldrawing'):rr.remove(rel)
        rt.write(rp,encoding='utf-8',xml_declaration=True)
for root,_,files in os.walk(tmp):
    for f in files:
        low=f.lower()
        if (low.startswith('comments') and low.endswith('.xml')) or low.startswith('vmldrawing'):
            os.remove(os.path.join(root,f))
ct=os.path.join(tmp,'[Content_Types].xml');t=ET.parse(ct);r=t.getroot()
for x in list(r):
    if 'comment' in x.get('ContentType','').lower() or x.get('Extension','').lower()=='vml':r.remove(x)
t.write(ct,encoding='utf-8',xml_declaration=True)
with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as z:
    for root,_,files in os.walk(tmp):
        for f in files:
            p=os.path.join(root,f);z.write(p,os.path.relpath(p,tmp))
print(out)
