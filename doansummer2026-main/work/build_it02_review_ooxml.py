import os, re, shutil, zipfile
from xml.etree import ElementTree as ET

SRC=r"C:\Users\Administrator\Downloads\_Report5_IntegrationTest_Sample.xlsx"
OUT=r"D:\gitlap\doAnSummer2026\outputs\IT02_LOGIN_TOKEN_REVIEW_EXACT_TEMPLATE.xlsx"
TMP=r"D:\gitlap\doAnSummer2026\work\it02_review_unpack"
NS="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
RNS="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
ET.register_namespace('',NS); ET.register_namespace('r',RNS)
q=lambda x:f"{{{NS}}}{x}"

def colnum(ref):
    n=0
    for ch in re.match(r"[A-Z]+",ref).group(): n=n*26+ord(ch)-64
    return n
def col(n):
    s=''
    while n: n,r=divmod(n-1,26);s=chr(65+r)+s
    return s
def load(p):return ET.parse(p)
def save(t,p):t.write(p,encoding='utf-8',xml_declaration=True)
def get_row(root,n,template=None):
    sd=root.find(q('sheetData'))
    for r in sd.findall(q('row')):
        if int(r.get('r'))==n:return r
    import copy
    r=copy.deepcopy(template) if template is not None else ET.Element(q('row'))
    r.set('r',str(n))
    for c in r.findall(q('c')):
        c.set('r',re.sub(r'\d+$',str(n),c.get('r')))
        for x in list(c):c.remove(x)
    sd.append(r);sd[:]=sorted(sd,key=lambda x:int(x.get('r')))
    return r
def cell(row,ref):
    for c in row.findall(q('c')):
        if c.get('r')==ref:return c
    c=ET.Element(q('c'),{'r':ref});row.append(c)
    row[:]=sorted(row,key=lambda x:colnum(x.get('r')) if x.tag==q('c') else 999)
    return c
def clear_cell(c):
    for x in list(c):c.remove(x)
    c.attrib.pop('t',None)
def text(root,ref,value,style=None):
    r=get_row(root,int(re.search(r'\d+',ref).group()));c=cell(r,ref);clear_cell(c)
    if style is not None:c.set('s',str(style))
    c.set('t','inlineStr');i=ET.SubElement(c,q('is'));t=ET.SubElement(i,q('t'));t.text=str(value)
def num(root,ref,value,style=None):
    r=get_row(root,int(re.search(r'\d+',ref).group()));c=cell(r,ref);clear_cell(c)
    if style is not None:c.set('s',str(style))
    ET.SubElement(c,q('v')).text=str(value)
def formula(root,ref,value,cached=0,style=None):
    r=get_row(root,int(re.search(r'\d+',ref).group()));c=cell(r,ref);clear_cell(c)
    if style is not None:c.set('s',str(style))
    ET.SubElement(c,q('f')).text=value;ET.SubElement(c,q('v')).text=str(cached)
def clear_range(root,r1,r2,c1,c2):
    for r in root.find(q('sheetData')).findall(q('row')):
        rn=int(r.get('r'))
        if r1<=rn<=r2:
            for c in list(r.findall(q('c'))):
                if c1<=colnum(c.get('r'))<=c2:r.remove(c)

if os.path.exists(TMP):shutil.rmtree(TMP)
os.makedirs(TMP)
with zipfile.ZipFile(SRC) as z:z.extractall(TMP)

# Rename only existing template sheets; no worksheets or relationships are added.
wbp=os.path.join(TMP,'xl','workbook.xml');wt=load(wbp);wr=wt.getroot()
renames={'Feature 1':'IT02-LoginToken','ConditionAnalysis-UpdateProfile':'CA-IT02-LoginToken','TestDesign-UpdateProfile':'TD-IT02-LoginToken','Feature 2':'Template-Reference'}
for s in wr.find(q('sheets')):
    if s.get('name') in renames:s.set('name',renames[s.get('name')])
dn=wr.find(q('definedNames'))
if dn is not None:
    for x in list(dn):
        if x.text and '#REF!' in x.text:
            dn.remove(x);continue
        if x.text:
            for old,new in renames.items():x.text=x.text.replace("'"+old+"'","'"+new+"'").replace(old+'!',new+'!')
save(wt,wbp)

# Condition Analysis: exact values, unique flow-prefixed tags, boundaries only where meaningful.
cap=os.path.join(TMP,'xl','worksheets','sheet5.xml');t=load(cap);r=t.getroot();clear_range(r,1,31,1,9)
text(r,'A1','Test Condition Analysis',172)
for i,v in enumerate(['Variable','Valid Partition','Tag','Invalid Partition','Tag','Valid Boundary','Tag','Invalid Boundary','Tag'],1):text(r,f'{col(i)}2',v,83)
conditions=[
('account.phone','account.phone = "0944433222"','IT02-VP01','account.phone = "0900000000"','IT02-IP01','N/A','N/A','N/A','N/A'),
('password','password = "Clinic@123"','IT02-VP02','password = ""','IT02-IP02','password = "Abc@1234"\npassword.length = 8','IT02-VB02','password = "Abc@123"\npassword.length = 7','IT02-IB02'),
('account.isActive','account.isActive = true','IT02-VP03','account.isActive = false','IT02-IP03','N/A','N/A','N/A','N/A'),
('token.type','token.type = "REFRESH"','IT02-VP04','token.type = "ACCESS"','IT02-IP04','N/A','N/A','N/A','N/A'),
('token.expiresAt','token.expiresAt > requestTime','IT02-VP05','token.expiresAt < requestTime','IT02-IP05','token.expiresAt = "2026-08-12T09:59:59"\nrequestTime = "2026-08-12T09:59:58"','IT02-VB05','token.expiresAt = "2026-08-12T09:59:59"\nrequestTime = "2026-08-12T10:00:00"','IT02-IB05'),
('Authorization','Authorization = "Bearer eyJ.access.valid"','IT02-VP06','Authorization = null','IT02-IP06','N/A','N/A','N/A','N/A')]
template=get_row(r,10)
for rn,row in enumerate(conditions,3):
    rr=get_row(r,rn,template);rr.set('ht','58');rr.set('customHeight','1')
    for i,v in enumerate(row,1):text(r,f'{col(i)}{rn}',v,129 if i>1 else 130)
text(r,'A27','* Notes:',140);text(r,'B27','Each tag defines exactly one condition/value. Reuse in Test Design means coverage, not duplicate definition.',141)
save(t,cap)

# Test Design with explicit inputs and exact outcomes.
tdp=os.path.join(TMP,'xl','worksheets','sheet6.xml');t=load(tdp);r=t.getroot();clear_range(r,1,21,1,4)
text(r,'A1','Test Case Design',172)
for i,v in enumerate(['Test Case','Description','Expected Outcome','New Tag Covered'],1):text(r,f'{col(i)}2',v,83)
design=[
(1,'phone = "0944433222"\npassword = "Clinic@123"\nPOST /api/auth/login','httpStatus = 200\nresponse.accessToken != null\nresponse.refreshToken != null\nresponse.systemRole = "CUSTOMER"','IT02-VP01, IT02-VP02, IT02-VP03'),
(2,'phone = "0900000000"\npassword = "Clinic@123"\nPOST /api/auth/login','httpStatus = 400\nresponse.code = "ACCOUNT_NOT_FOUND"\nresponse.accessToken = null','IT02-IP01'),
(3,'phone = "0944433222"\npassword = ""\nPOST /api/auth/login','httpStatus = 400\nresponse.code = "VALIDATION_ERROR"\nresponse.accessToken = null','IT02-IP02'),
(4,'phone = "0944433222"\npassword = "Clinic@123"\nPOST /api/auth/login','httpStatus = 400\nresponse.code = "ACCOUNT_INACTIVE"\nresponse.accessToken = null','IT02-IP03'),
(5,'refreshToken = "eyJ.refresh.valid"\nrequestTime = "2026-08-12T09:59:58"\nPOST /api/auth/refresh','httpStatus = 200\nresponse.accessToken != null\nresponse.refreshToken != null','IT02-VP04, IT02-VP05, IT02-VB05'),
(6,'refreshToken = "eyJ.access.valid"\nPOST /api/auth/refresh','httpStatus = 400\nresponse.code = "INVALID_TOKEN_TYPE"\nresponse.accessToken = null','IT02-IP04'),
(7,'refreshToken = "eyJ.refresh.expired"\nrequestTime = "2026-08-12T10:00:00"\nPOST /api/auth/refresh','httpStatus = 401\nresponse.code = "TOKEN_EXPIRED"\nresponse.accessToken = null','IT02-IP05, IT02-IB05'),
(8,'Authorization = null\nGET /api/auth/me','httpStatus = 401\nresponse.code = "UNAUTHORIZED"\nresponse.accountId = null','IT02-IP06')]
template=get_row(r,5)
for rn,row in enumerate(design,3):
    rr=get_row(r,rn,template);rr.set('ht','92');rr.set('customHeight','1')
    num(r,f'A{rn}',row[0],152)
    for i,v in enumerate(row[1:],2):text(r,f'{col(i)}{rn}',v,151)
save(t,tdp)

# Feature page: concrete English test cases, exact preconditions and three Pending rounds.
fp=os.path.join(TMP,'xl','worksheets','sheet4.xml');t=load(fp);r=t.getroot();clear_range(r,1,50,1,15)
text(r,'B2','IT02 - Login and Token Refresh',168);text(r,'B3','Account -> Authentication -> JWT access/refresh token.',170);formula(r,'B4','COUNTIF(A12:A1000,"IT02-*")',8,170)
for ref,fm,cached in [('B6','COUNTIF($F$12:$F$1000,B$5)',0),('C6','COUNTIF($F$12:$F$1000,C$5)',0),('D6','COUNTIF($F$12:$F$1000,D$5)',8),('E6','COUNTIF($F$12:$F$1000,E$5)',0),('B7','COUNTIF($I$12:$I$1000,B$5)',0),('C7','COUNTIF($I$12:$I$1000,C$5)',0),('D7','COUNTIF($I$12:$I$1000,D$5)',8),('E7','COUNTIF($I$12:$I$1000,E$5)',0),('B8','COUNTIF($L$12:$L$1000,B$5)',0),('C8','COUNTIF($L$12:$L$1000,C$5)',0),('D8','COUNTIF($L$12:$L$1000,D$5)',8),('E8','COUNTIF($L$12:$L$1000,E$5)',0)]:formula(r,ref,fm,cached,85)
headers=['Test Case ID','Test Case Description','Test Case Procedure','Expected Results','Pre-conditions','Round 1','Test date','Tester','Round 2','Test date','Tester','Round 3','Test date','Tester','Note']
for i,v in enumerate(headers,1):text(r,f'{col(i)}10',v,83)
cases=[
('IT02-001','Login with an active customer account','phone = "0944433222"\npassword = "Clinic@123"\nPOST /api/auth/login','httpStatus = 200\nresponse.accessToken != null\nresponse.refreshToken != null\nresponse.systemRole = "CUSTOMER"','account.phone = "0944433222"\naccount.passwordHash matches "Clinic@123"\naccount.isActive = true\naccount.systemRole = "CUSTOMER"','IT02-VP01, IT02-VP02, IT02-VP03'),
('IT02-002','Reject an unknown phone number','phone = "0900000000"\npassword = "Clinic@123"\nPOST /api/auth/login','httpStatus = 400\nresponse.code = "ACCOUNT_NOT_FOUND"\nresponse.accessToken = null','account.phone = "0900000000"\naccount.count = 0','IT02-IP01'),
('IT02-003','Reject an empty password','phone = "0944433222"\npassword = ""\nPOST /api/auth/login','httpStatus = 400\nresponse.code = "VALIDATION_ERROR"\nresponse.accessToken = null','account.phone = "0944433222"\naccount.isActive = true','IT02-IP02'),
('IT02-004','Reject an inactive account','phone = "0944433222"\npassword = "Clinic@123"\nPOST /api/auth/login','httpStatus = 400\nresponse.code = "ACCOUNT_INACTIVE"\nresponse.accessToken = null','account.phone = "0944433222"\naccount.isActive = false','IT02-IP03'),
('IT02-005','Refresh tokens before expiration','refreshToken = "eyJ.refresh.valid"\nrequestTime = "2026-08-12T09:59:58"\nPOST /api/auth/refresh','httpStatus = 200\nresponse.accessToken != null\nresponse.refreshToken != null','token.type = "REFRESH"\ntoken.expiresAt = "2026-08-12T09:59:59"\ntoken.revoked = false','IT02-VP04, IT02-VP05, IT02-VB05'),
('IT02-006','Reject an access token on refresh endpoint','refreshToken = "eyJ.access.valid"\nPOST /api/auth/refresh','httpStatus = 400\nresponse.code = "INVALID_TOKEN_TYPE"\nresponse.accessToken = null','token.type = "ACCESS"\ntoken.revoked = false','IT02-IP04'),
('IT02-007','Reject an expired refresh token','refreshToken = "eyJ.refresh.expired"\nrequestTime = "2026-08-12T10:00:00"\nPOST /api/auth/refresh','httpStatus = 401\nresponse.code = "TOKEN_EXPIRED"\nresponse.accessToken = null','token.type = "REFRESH"\ntoken.expiresAt = "2026-08-12T09:59:59"','IT02-IP05, IT02-IB05'),
('IT02-008','Reject account details request without authorization','Authorization = null\nGET /api/auth/me','httpStatus = 401\nresponse.code = "UNAUTHORIZED"\nresponse.accountId = null','securityContext.authentication = null','IT02-IP06')]
group_template=get_row(r,11);case_template=get_row(r,12);rn=11;idx=0
for label,count in [('Valid concrete values',2),('Invalid account credentials and state',2),('Token and authorization states',4)]:
    rr=get_row(r,rn,group_template);rr.set('ht','18');text(r,f'A{rn}',label,79);rn+=1
    for _ in range(count):
        item=cases[idx];rr=get_row(r,rn,case_template);rr.set('ht','112');rr.set('customHeight','1')
        for i,v in enumerate(item[:5],1):text(r,f'{col(i)}{rn}',v,39 if i!=4 else 45)
        for c in ('F','I','L'):text(r,f'{c}{rn}','Pending',39)
        text(r,f'O{rn}',item[5],39);rn+=1;idx+=1
dim=r.find(q('dimension'));dim.set('ref',f'A1:R{rn-1}')
af=r.find(q('autoFilter'))
if af is not None:af.set('ref',f'A10:O{rn-1}')
dvs=r.find(q('dataValidations'))
if dvs is not None:
    for dv in dvs:dv.set('sqref',f'F12:F{rn-1} I12:I{rn-1} L12:L{rn-1}')
save(t,fp)

# Review index/statistics/message content, all English.
lp=os.path.join(TMP,'xl','worksheets','sheet2.xml');t=load(lp);r=t.getroot();clear_range(r,3,5,2,6);clear_range(r,9,40,2,6)
text(r,'B3','Project Name',163);text(r,'D3','Clinic Management System',164)
text(r,'B4','Project Code',163);text(r,'D4','CMS',164)
text(r,'B5','Test Environment Setup Description',163);text(r,'D5','Spring Boot API; PostgreSQL; Redis; Postman/Swagger',164)
num(r,'B9',1,19);text(r,'C9','IT02 - Login and Token Refresh',20);text(r,'D9','IT02-LoginToken',94);text(r,'E9','Account authentication and token lifecycle',21);text(r,'F9','Use the concrete account and token values in the Feature sheet.',21);save(t,lp)
links=r.find(q('hyperlinks'))
if links is not None:
    links.clear();ET.SubElement(links,q('hyperlink'),{'ref':'D9','location':"'IT02-LoginToken'!A1",'display':'IT02-LoginToken'})
save(t,lp)
sp=os.path.join(TMP,'xl','worksheets','sheet3.xml');t=load(sp);r=t.getroot();clear_range(r,11,40,2,8);num(r,'B11',1,62);text(r,'C11','IT02 - Login and Token Refresh',63);num(r,'D11',0,64);num(r,'E11',0,64);num(r,'F11',8,64);num(r,'G11',0,64);num(r,'H11',8,64);save(t,sp)
mp=os.path.join(TMP,'xl','worksheets','sheet8.xml');t=load(mp);r=t.getroot();clear_range(r,2,88,1,3)
msgs=[('IT02-ACCOUNT-NOT-FOUND','No account exists for phone = "0900000000".','IT02'),('IT02-VALIDATION-ERROR','password = "" does not satisfy the login request contract.','IT02'),('IT02-ACCOUNT-INACTIVE','account.isActive = false prevents authentication.','IT02'),('IT02-INVALID-TOKEN-TYPE','token.type = "ACCESS" cannot be used as refreshToken.','IT02'),('IT02-TOKEN-EXPIRED','token.expiresAt is earlier than requestTime.','IT02'),('IT02-UNAUTHORIZED','Authorization = null.','IT02')]
for rn,row in enumerate(msgs,2):
    for i,v in enumerate(row,1):text(r,f'{col(i)}{rn}',v,154)
save(t,mp)

# Keep the second feature tab as an English-only visual template reference.
rp=os.path.join(TMP,'xl','worksheets','sheet7.xml');t=load(rp);r=t.getroot();clear_range(r,1,50,1,15)
text(r,'B2','Template Reference - Not Part of IT02 Review',168);text(r,'B3','This sheet is intentionally reserved for the next approved integration flow.',170);num(r,'B4',0,170)
for i,v in enumerate(headers,1):text(r,f'{col(i)}10',v,83)
text(r,'A11','No test cases are defined on this reference sheet.',79)
af=r.find(q('autoFilter'))
if af is not None:af.set('ref','A10:O11')
save(t,rp)

# Formula cells changed, so remove the template's stale calculation chain.
chain=os.path.join(TMP,'xl','calcChain.xml')
if os.path.exists(chain):os.remove(chain)
relp=os.path.join(TMP,'xl','_rels','workbook.xml.rels');rt=load(relp);rr=rt.getroot()
for rel in list(rr):
    if rel.get('Type','').endswith('/calcChain'):rr.remove(rel)
save(rt,relp)
ctp=os.path.join(TMP,'[Content_Types].xml');ct=load(ctp);cr=ct.getroot()
for x in list(cr):
    if x.get('PartName','')=='/xl/calcChain.xml':cr.remove(x)
save(ct,ctp)
wt=load(wbp);wr=wt.getroot();calc=wr.find(q('calcPr'))
if calc is None:calc=ET.SubElement(wr,q('calcPr'))
calc.set('calcMode','auto');calc.set('fullCalcOnLoad','1');calc.set('forceFullCalc','1');save(wt,wbp)

if os.path.exists(OUT):os.remove(OUT)
with zipfile.ZipFile(OUT,'w',zipfile.ZIP_DEFLATED) as z:
    for root,_,files in os.walk(TMP):
        for f in files:
            p=os.path.join(root,f);z.write(p,os.path.relpath(p,TMP))
print(OUT)
