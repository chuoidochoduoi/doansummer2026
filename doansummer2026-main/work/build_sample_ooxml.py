import copy, os, re, shutil, zipfile
from xml.etree import ElementTree as ET

SRC = r"C:\Users\Administrator\Downloads\_Report5_IntegrationTest_Sample.xlsx"
OUT = r"D:\gitlap\doAnSummer2026\outputs\Report5_IntegrationTest_Clinic_Sample.xlsx"
TMP = r"D:\gitlap\doAnSummer2026\work\xlsx_unpacked"
NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
RNS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
PKG = "http://schemas.openxmlformats.org/package/2006/relationships"
CT = "http://schemas.openxmlformats.org/package/2006/content-types"
ET.register_namespace('', NS); ET.register_namespace('r', RNS)

def q(tag): return f"{{{NS}}}{tag}"
def colnum(ref):
    n=0
    for ch in re.match(r"[A-Z]+", ref).group(): n=n*26+ord(ch)-64
    return n
def addr_col(n):
    s=''
    while n: n,r=divmod(n-1,26); s=chr(65+r)+s
    return s
def load(path): return ET.parse(path)
def save(tree,path): tree.write(path,encoding='utf-8',xml_declaration=True)

def cell(row, ref):
    for c in row.findall(q('c')):
        if c.get('r')==ref: return c
    c=ET.Element(q('c'),{'r':ref}); row.append(c)
    row[:] = sorted(row, key=lambda x: colnum(x.get('r')) if x.tag==q('c') else 99999)
    return c
def get_row(root,n,template=None):
    sd=root.find(q('sheetData'))
    for r in sd.findall(q('row')):
        if int(r.get('r'))==n:return r
    r=copy.deepcopy(template) if template is not None else ET.Element(q('row'))
    old=int(r.get('r','1')); r.set('r',str(n))
    for c in r.findall(q('c')):
        c.set('r',re.sub(r'\d+$',str(n),c.get('r')))
        for x in list(c): c.remove(x)
    sd.append(r); sd[:] = sorted(sd,key=lambda x:int(x.get('r')))
    return r
def clear(c):
    for x in list(c): c.remove(x)
    c.attrib.pop('t',None)
def set_text(root,ref,value,style=None):
    n=int(re.search(r'\d+',ref).group()); r=get_row(root,n); c=cell(r,ref); clear(c)
    if style is not None:c.set('s',str(style))
    c.set('t','inlineStr'); isel=ET.SubElement(c,q('is')); t=ET.SubElement(isel,q('t'))
    if value and (value[0].isspace() or value[-1].isspace()): t.set('{http://www.w3.org/XML/1998/namespace}space','preserve')
    t.text=str(value)
def set_num(root,ref,value,style=None):
    n=int(re.search(r'\d+',ref).group()); r=get_row(root,n); c=cell(r,ref); clear(c)
    if style is not None:c.set('s',str(style))
    ET.SubElement(c,q('v')).text=str(value)
def set_formula(root,ref,formula,cached=0,style=None):
    n=int(re.search(r'\d+',ref).group()); r=get_row(root,n); c=cell(r,ref); clear(c)
    if style is not None:c.set('s',str(style))
    ET.SubElement(c,q('f')).text=formula
    ET.SubElement(c,q('v')).text=str(cached)
def clear_range(root,r1,r2,c1,c2):
    sd=root.find(q('sheetData'))
    for r in sd.findall(q('row')):
        rn=int(r.get('r'))
        if r1<=rn<=r2:
            for c in list(r.findall(q('c'))):
                if c1<=colnum(c.get('r'))<=c2:r.remove(c)

def clone_sheet(base_path,new_path):
    shutil.copy2(base_path,new_path)
    relbase=os.path.join(os.path.dirname(base_path),'_rels',os.path.basename(base_path)+'.rels')
    if os.path.exists(relbase):
        os.makedirs(os.path.join(os.path.dirname(new_path),'_rels'),exist_ok=True)
        shutil.copy2(relbase,os.path.join(os.path.dirname(new_path),'_rels',os.path.basename(new_path)+'.rels'))

def add_sheet(wb_root, rel_root, ct_root, name, sheet_file, sheet_id, rel_id):
    sheets=wb_root.find(q('sheets'))
    ET.SubElement(sheets,q('sheet'),{'name':name,'sheetId':str(sheet_id),f'{{{RNS}}}id':rel_id})
    ET.SubElement(rel_root,f'{{{PKG}}}Relationship',{'Id':rel_id,'Type':'http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet','Target':f'worksheets/{sheet_file}'})
    ET.SubElement(ct_root,f'{{{CT}}}Override',{'PartName':f'/xl/worksheets/{sheet_file}','ContentType':'application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml'})

def rename_sheet(wb_root,old,new):
    for s in wb_root.find(q('sheets')):
        if s.get('name')==old:s.set('name',new);return

def prep_condition(path, rows):
    t=load(path); root=t.getroot(); clear_range(root,3,31,1,9)
    merges=root.find(q('mergeCells'))
    if merges is not None:
        for m in list(merges):
            if m.get('ref')!='A1:I1':merges.remove(m)
        merges.set('count',str(len(merges)))
    set_text(root,'A1','Test Condition Analysis',172)
    headers=['Condition','Valid Partitions','Tag','Invalid Partitions','Tag','Valid Boundaries','Tag','Invalid Boundaries','Tag']
    for i,v in enumerate(headers,1):set_text(root,f'{addr_col(i)}2',v,83)
    template=get_row(root,10)
    for rn,item in enumerate(rows,3):
        r=get_row(root,rn,template); r.set('ht','42'); r.set('customHeight','1')
        for i,v in enumerate(item,1):set_text(root,f'{addr_col(i)}{rn}',v,129 if i>1 else 130)
    set_text(root,'A27','* Notes:',140);set_text(root,'B27','Đối chiếu tag VP/IP/VB/IB với Test Design.',141)
    save(t,path)
def prep_design(path,cases):
    t=load(path);root=t.getroot();clear_range(root,3,21,1,4)
    set_text(root,'A1','Test case design',172)
    for i,v in enumerate(['Test-case No','Description','Expected result','TAG'],1):set_text(root,f'{addr_col(i)}2',v,83)
    template=get_row(root,5)
    for rn,item in enumerate(cases,3):
        r=get_row(root,rn,template);r.set('ht','82');r.set('customHeight','1')
        set_num(root,f'A{rn}',item[0],152)
        for i,v in enumerate(item[1:],2):set_text(root,f'{addr_col(i)}{rn}',v,151)
    save(t,path)
def prep_feature(path,title,requirement,groups,cases):
    t=load(path);root=t.getroot();clear_range(root,11,50,1,15)
    set_text(root,'B2',title,168);set_text(root,'B3',requirement,170);set_formula(root,'B4','COUNTIF(A12:A1000,"IT*")',len(cases),170)
    for rn,col,formula in [(6,'B','COUNTIF($F$12:$F$1000,B$5)'),(6,'C','COUNTIF($F$12:$F$1000,C$5)'),(6,'D','COUNTIF($F$12:$F$1000,D$5)'),(6,'E','COUNTIF($F$12:$F$1000,E$5)'),(7,'B','COUNTIF($I$12:$I$1000,B$5)'),(7,'C','COUNTIF($I$12:$I$1000,C$5)'),(7,'D','COUNTIF($I$12:$I$1000,D$5)'),(7,'E','COUNTIF($I$12:$I$1000,E$5)'),(8,'B','COUNTIF($L$12:$L$1000,B$5)'),(8,'C','COUNTIF($L$12:$L$1000,C$5)'),(8,'D','COUNTIF($L$12:$L$1000,D$5)'),(8,'E','COUNTIF($L$12:$L$1000,E$5)')]:
        set_formula(root,f'{col}{rn}',formula,6 if col=='D' else 0,85)
    h=['Test Case ID','Test Case Description','Test Case Procedure','Expected Results','Pre-conditions','Round 1','Test date','Tester','Round 2','Test date','Tester','Round 3','Test date','Tester','Note']
    for i,v in enumerate(h,1):set_text(root,f'{addr_col(i)}10',v,83)
    group_template=get_row(root,11); case_template=get_row(root,12); rn=11;idx=0
    for label,count in groups:
        r=get_row(root,rn,group_template);r.set('ht','18');set_text(root,f'A{rn}',label,79);rn+=1
        for _ in range(count):
            item=cases[idx];r=get_row(root,rn,case_template);r.set('ht','105');r.set('customHeight','1')
            for i,v in enumerate(item[:5],1):set_text(root,f'{addr_col(i)}{rn}',v,39 if i!=4 else 45)
            for col in ('F','I','L'):set_text(root,f'{col}{rn}','Pending',39)
            set_text(root,f'O{rn}',item[5],39);rn+=1;idx+=1
    dim=root.find(q('dimension'));dim.set('ref',f'A1:R{rn-1}')
    af=root.find(q('autoFilter'))
    if af is not None:af.set('ref',f'A10:O{rn-1}')
    dvs=root.find(q('dataValidations'))
    if dvs is not None:
        for dv in dvs:dv.set('sqref',f'F12:F{rn-1} I12:I{rn-1} L12:L{rn-1}')
    save(t,path)

ca1=[('identifier','Email hợp lệ hoặc SĐT VN 0/+84','VP1','Rỗng/sai định dạng','IP1','Chuỗi ngắn nhất hợp lệ','VB1','Thiếu chữ số/domain','IB1'),('OTP','Đúng 6 số, còn hạn','VP2','Sai/hết hạn','IP2','6 số','VB2','5 hoặc 7 số','IB2'),('OTP verified','Đã verify trong 15 phút','VP3','Chưa verify/quá hạn','IP3','Phút 15','VB3','Sau 15 phút','IB3'),('password','8-64 ký tự','VP4','<8, >64 hoặc rỗng','IP4','8 và 64','VB4','7 và 65','IB4'),('fullName','Không rỗng','VP5','Null/rỗng','IP5','1 ký tự','VB5','0 ký tự','IB5'),('dob','Không tương lai, tuổi <=150','VP6','Tương lai hoặc >150','IP6','Hôm nay/150 tuổi','VB6','Ngày mai/151 tuổi','IB6'),('identifier uniqueness','Chưa gắn account','VP7','Đã gắn account khác','IP7','N/A','VB7','N/A','IB7')]
td1=[(1,'Đăng ký bằng SĐT sau khi verify OTP; password 8 ký tự.','200; tạo Account CUSTOMER + Profile; consume OTP; trả JWT.','VP1,VP2,VP3,VB4,VP5,VP6,VP7'),(2,'Verify OTP 5 chữ số.','400; không tạo otp_verified.','IB2'),(3,'Đăng ký khi chưa verify OTP.','400; không tạo Account/Profile.','IP3'),(4,'Đăng ký với password 7 ký tự.','400 validation; không ghi DB.','IB4'),(5,'Đăng ký lại SĐT đã gắn account.','400; không tạo dữ liệu trùng.','IP7'),(6,'Đăng ký với ngày sinh ngày mai.','400; ngày sinh không thể ở tương lai.','IB6')]
ca2=[('customerId','Account CUSTOMER/STAFF có Profile','VP1','Không tồn tại/không Profile','IP1','N/A','VB1','Null','IB1'),('scheduledAt','Hiện tại hoặc tương lai','VP2','Quá khứ','IP2','Hiện tại','VB2','Hiện tại - 1 phút','IB2'),('appointment conflict','Không có PENDING gần thời gian','VP3','Có PENDING trong ±30 phút','IP3','Ngoài 30 phút','VB3','Trong 30 phút','IB3'),('serviceIds','Tồn tại, phù hợp tuổi/giới','VP4','Không tồn tại/không phù hợp','IP4','Tuổi min/max','VB4','min-1/max+1','IB4'),('check-in status','PENDING','VP5','CHECKED_IN/CANCELLED','IP5','N/A','VB5','N/A','IB5'),('check-in date','Đúng ngày hôm nay','VP6','Khác hôm nay','IP6','00:00/23:59','VB6','Ngày trước/sau','IB6'),('issuedById','Nhân viên tồn tại','VP7','Null/không tồn tại','IP7','N/A','VB7','Null','IB7'),('active visit','Không có Visit hoạt động','VP8','Có CHECKED_IN/IN_PROGRESS','IP8','N/A','VB8','N/A','IB8')]
td2=[(1,'Tạo lịch hợp lệ, không xung đột.','201; Appointment PENDING; notification lễ tân.','VP1,VP2,VP3,VP4'),(2,'scheduledAt ở quá khứ.','400; không tạo Appointment.','IP2,IB2'),(3,'Lịch thứ hai trong ±30 phút.','400; chỉ giữ lịch đầu.','IP3,IB3'),(4,'Check-in PENDING đúng ngày.','200; Appointment CHECKED_IN; tạo Visit và Invoice PENDING; queue sau thanh toán.','VP5,VP6,VP7,VP8'),(5,'Check-in lại lịch đã CHECKED_IN.','409; không tạo Visit/Invoice trùng.','IP5'),(6,'Check-in khi có Visit hoạt động.','409; giữ nguyên dữ liệu hiện tại.','IP8')]
ca3=[('medicalRecordId','Tồn tại và gắn Visit','VP1','Không tồn tại/không Visit','IP1','N/A','VB1','Null','IB1'),('serviceIds','Tồn tại, có khoa/capability','VP2','Rỗng/không có khoa phù hợp','IP2','1 phần tử','VB2','0 phần tử','IB2'),('duplicate request','Chưa có cùng service trong Visit','VP3','Đã có request chưa CANCELLED','IP3','N/A','VB3','N/A','IB3'),('request status','PENDING/IN_PROGRESS','VP4','COMPLETED/CANCELLED','IP4','N/A','VB4','N/A','IB4'),('execution queue','IN_PROGRESS hoặc DONE','VP5','Null/WAITING/BLOCKED','IP5','N/A','VB5','N/A','IB5'),('result PDF','.pdf hợp lệ <=10MB','VP6','Rỗng/sai loại/>10MB','IP6','10MB','VB6','10MB+1 byte','IB6'),('conclusion','Không rỗng','VP7','Null/rỗng','IP7','1 ký tự','VB7','0 ký tự','IB7'),('verifiedBy','Head doctor khoa thực hiện','VP8','Không đúng bác sĩ/khoa','IP8','N/A','VB8','Null','IB8'),('sampleStatus','Không REJECTED/RECOLLECT','VP9','REJECTED/RECOLLECT','IP9','N/A','VB9','N/A','IB9')]
td3=[(1,'Tạo chỉ định CLS hợp lệ.','201; TestRequest PENDING; tạo/dùng chung queue theo khoa.','VP1,VP2,VP3'),(2,'Tạo lại cùng dịch vụ trong Visit.','409; không tạo request/queue trùng.','IP3'),(3,'Lưu kết quả nháp.','Tạo Result; request PENDING -> IN_PROGRESS.','VP4'),(4,'Hoàn tất với PDF, conclusion và head doctor.','200; COMPLETED; lab queue DONE; queue khám TEST_DONE khi đủ kết quả.','VP4,VP5,VP6,VP7,VP8,VP9'),(5,'Hoàn tất thiếu PDF.','400; request chưa COMPLETED.','IP6'),(6,'Bác sĩ không phụ trách khoa ký.','400; không verified/complete.','IP8')]

f1=[('IT01-001','Đăng ký thành công bằng SĐT','1. POST /send-otp 0912345678\n2. POST /verify-register-otp OTP 6 số\n3. POST /register password=Abc12345.','200; tạo Account CUSTOMER + Profile; consume OTP; trả access/refresh token.','Redis/PostgreSQL chạy; SĐT chưa có account.','VP1,VP2,VP3,VB4,VP5,VP6,VP7'),('IT01-002','Từ chối OTP 5 số','POST /verify-register-otp otp=12345.','400; không tạo otp_verified.','Đã gửi OTP.','IB2'),('IT01-003','Đăng ký khi chưa verify OTP','POST /register với dữ liệu hợp lệ.','400; không tạo Account/Profile.','Không có otp_verified.','IP3'),('IT01-004','Password 7 ký tự','Verify OTP rồi đăng ký password=Abc1234.','400 validation; không ghi DB.','OTP verified.','IB4'),('IT01-005','SĐT đã gắn tài khoản','Đăng ký lại cùng SĐT.','400; vẫn chỉ một Account/Profile.','SĐT đã có account.','IP7'),('IT01-006','Ngày sinh tương lai','Đăng ký với dob=ngày mai.','400; không tạo dữ liệu.','Identifier mới, OTP verified.','IB6')]
f2=[('IT04-001','Tạo lịch hợp lệ','POST /api/v1/appointments với customerId, ngày mai, serviceIds hợp lệ.','201; Appointment PENDING; gửi notification lễ tân.','Customer/Profile/Service hợp lệ.','VP1,VP2,VP3,VP4'),('IT04-002','Lịch ở quá khứ','POST scheduledAt=hiện tại-1 phút.','400; không tạo Appointment.','Customer hợp lệ.','IP2,IB2'),('IT04-003','Lịch xung đột','Tạo lịch thứ hai cách lịch PENDING 20 phút.','400; chỉ giữ lịch ban đầu.','Có lịch PENDING.','IP3,IB3'),('IT04-004','Check-in thành công','RECEPTIONIST POST /appointments/{id}/check-in.','200; Appointment CHECKED_IN; tạo Visit CHECKED_IN + Invoice PENDING; chưa tạo queue trước thanh toán.','Lịch PENDING hôm nay; không active visit.','VP5,VP6,VP7,VP8'),('IT04-005','Check-in trùng','Gọi lại check-in cùng appointmentId.','409; không tạo Visit/Invoice trùng.','Appointment CHECKED_IN.','IP5'),('IT04-006','Đang có lượt khám','Check-in lịch mới khi có Visit CHECKED_IN.','409; lịch mới chưa CHECKED_IN.','Có active Visit.','IP8')]
f3=[('IT09-001','Tạo chỉ định CLS','DOCTOR POST /api/v1/test-requests với record/service/staff hợp lệ.','201; TestRequest PENDING; chọn khoa; tạo/dùng chung QueueTicket.','MedicalRecord/Visit/Service hợp lệ.','VP1,VP2,VP3'),('IT09-002','Chỉ định trùng dịch vụ','POST cùng service lần hai.','409; không tạo request/ticket trùng.','Request cũ chưa CANCELLED.','IP3'),('IT09-003','Lưu kết quả nháp','POST /{id}/result với PDF, conclusion, performedById.','Tạo Result; request -> IN_PROGRESS; chưa verified.','Request PENDING.','VP4'),('IT09-004','Hoàn tất kết quả','POST /{id}/result/complete bằng head doctor, PDF và conclusion hợp lệ.','200; request COMPLETED; verifiedAt/By; lab queue DONE; queue khám TEST_DONE khi đủ.','Execution queue IN_PROGRESS.','VP4,VP5,VP6,VP7,VP8,VP9'),('IT09-005','Thiếu PDF','Complete với imageUrl rỗng/.jpg.','400; chưa COMPLETED.','Signer và queue hợp lệ.','IP6'),('IT09-006','Sai bác sĩ ký','Complete bằng bác sĩ không phụ trách khoa.','400; không verified/complete.','Kết quả khác hợp lệ.','IP8')]

if os.path.exists(TMP): shutil.rmtree(TMP)
os.makedirs(TMP); os.makedirs(os.path.dirname(OUT),exist_ok=True)
with zipfile.ZipFile(SRC) as z:z.extractall(TMP)
wbp=os.path.join(TMP,'xl','workbook.xml'); relp=os.path.join(TMP,'xl','_rels','workbook.xml.rels'); ctp=os.path.join(TMP,'[Content_Types].xml')
wbt=load(wbp);wbr=wbt.getroot();relt=load(relp);relroot=relt.getroot();ctt=load(ctp);ctroot=ctt.getroot()
rename_sheet(wbr,'Feature 1','IT01-Register');rename_sheet(wbr,'Feature 2','IT04-Appointment');rename_sheet(wbr,'ConditionAnalysis-UpdateProfile','CA-IT01-Register');rename_sheet(wbr,'TestDesign-UpdateProfile','TD-IT01-Register')
base_ca=os.path.join(TMP,'xl','worksheets','sheet5.xml');base_td=os.path.join(TMP,'xl','worksheets','sheet6.xml');base_f=os.path.join(TMP,'xl','worksheets','sheet7.xml')
for n,base in [(9,base_ca),(10,base_td),(11,base_ca),(12,base_td),(13,base_f)]:clone_sheet(base,os.path.join(TMP,'xl','worksheets',f'sheet{n}.xml'))
add_sheet(wbr,relroot,ctroot,'CA-IT04-Appointment','sheet9.xml',9,'rId20');add_sheet(wbr,relroot,ctroot,'TD-IT04-Appointment','sheet10.xml',10,'rId21');add_sheet(wbr,relroot,ctroot,'CA-IT09-LabResult','sheet11.xml',11,'rId22');add_sheet(wbr,relroot,ctroot,'TD-IT09-LabResult','sheet12.xml',12,'rId23');add_sheet(wbr,relroot,ctroot,'IT09-LabResult','sheet13.xml',13,'rId24')
ET.register_namespace('', NS); save(wbt,wbp)
ET.register_namespace('', PKG); save(relt,relp)
ET.register_namespace('', CT); save(ctt,ctp)
ET.register_namespace('', NS); ET.register_namespace('r', RNS)
prep_condition(base_ca,ca1);prep_design(base_td,td1);prep_feature(os.path.join(TMP,'xl','worksheets','sheet4.xml'),'IT01 - Đăng ký và xác thực OTP','Redis OTP -> Account -> Profile -> JWT.',[('OTP và đăng ký hợp lệ',1),('Validation và trạng thái OTP',3),('Ràng buộc dữ liệu',2)],f1)
prep_condition(os.path.join(TMP,'xl','worksheets','sheet9.xml'),ca2);prep_design(os.path.join(TMP,'xl','worksheets','sheet10.xml'),td2);prep_feature(base_f,'IT04 - Đặt lịch và check-in','Appointment -> CustomerVisit -> Invoice; QueueTicket sau thanh toán.',[('Đặt lịch',3),('Check-in thành công',1),('Check-in không hợp lệ',2)],f2)
prep_condition(os.path.join(TMP,'xl','worksheets','sheet11.xml'),ca3);prep_design(os.path.join(TMP,'xl','worksheets','sheet12.xml'),td3);prep_feature(os.path.join(TMP,'xl','worksheets','sheet13.xml'),'IT09 - Chỉ định và trả kết quả xét nghiệm','MedicalRecord -> TestRequest -> TestResult -> Queue/PatientJourney.',[('Tạo chỉ định',2),('Nhập và hoàn tất kết quả',2),('Validation hoàn tất',2)],f3)

# Cover, list, statistics, message list
cover=load(os.path.join(TMP,'xl','worksheets','sheet1.xml'));cr=cover.getroot();set_text(cr,'B4','Clinic Management System',157);set_text(cr,'B5','CMS',157);set_formula(cr,'B6','B5&"_IntegrationTest_v1.0"',0,158);set_text(cr,'F5','2026-08-12',103);set_text(cr,'F6','v1.0',103);set_text(cr,'A11','2026-08-12',114);set_text(cr,'B11','v1.0',115);set_text(cr,'C11','Initial sample',116);set_text(cr,'D11','A',116);set_text(cr,'E11','Mẫu 3 luồng Integration Test.',117);set_text(cr,'F11','Source code repository',118);save(cover,os.path.join(TMP,'xl','worksheets','sheet1.xml'))
lst=load(os.path.join(TMP,'xl','worksheets','sheet2.xml'));lr=lst.getroot();set_text(lr,'D3','Clinic Management System',164);set_text(lr,'D4','CMS',164);set_text(lr,'D5','Spring Boot API; PostgreSQL; Redis; Postman/Swagger',162);clear_range(lr,9,21,2,6)
for rn,row in enumerate([(1,'IT01 - Đăng ký và OTP','IT01-Register','Account/OTP/Profile/JWT','Redis và DB hoạt động'),(2,'IT04 - Đặt lịch và check-in','IT04-Appointment','Appointment/Visit/Invoice','Có customer/service/receptionist'),(3,'IT09 - Xét nghiệm','IT09-LabResult','Record/Request/Result/Queue','Có visit/record/service/head doctor')],9):
    set_num(lr,f'B{rn}',row[0],19)
    for c,v in zip('CDEF',row[1:]):set_text(lr,f'{c}{rn}',v,20 if c=='C' else 94 if c=='D' else 21)
links=lr.find(q('hyperlinks'))
if links is None:
    links=ET.SubElement(lr,q('hyperlinks'))
else:
    links.clear()
for rn,target in [(9,'IT01-Register'),(10,'IT04-Appointment'),(11,'IT09-LabResult')]:
    ET.SubElement(links,q('hyperlink'),{'ref':f'D{rn}','location':f"'{target}'!A1",'display':target,'tooltip':'Mở sheet test case'})
save(lst,os.path.join(TMP,'xl','worksheets','sheet2.xml'))
st=load(os.path.join(TMP,'xl','worksheets','sheet3.xml'));sr=st.getroot();set_text(sr,'C3','Clinic Management System',164);set_text(sr,'C4','CMS',164);set_formula(sr,'C5','C4&"_Integration Test Report_v1.0"',0,164);set_text(sr,'C6','Sample review: IT01, IT04, IT09',166);set_text(sr,'H5','2026-08-12',53)
mods=['IT01-Register','IT04-Appointment','IT09-LabResult']
for i,m in enumerate(mods,11):
    set_num(sr,f'B{i}',i-10,62);set_formula(sr,f'C{i}',f"'{m}'!B2",0,63)
    for col,src in zip('DEFGH','BCDEB'):set_formula(sr,f'{col}{i}',f"'{m}'!{src}{6 if col!='H' else 4}",6 if col in ('F','H') else 0,64)
for col in 'DEFGH':set_formula(sr,f'{col}14',f'SUM({col}11:{col}13)',18 if col in ('F','H') else 0,69)
set_formula(sr,'E16','IFERROR((D14+E14)*100/(H14-G14),0)',0,75);set_formula(sr,'E17','IFERROR(D14*100/(H14-G14),0)',0,75);save(st,os.path.join(TMP,'xl','worksheets','sheet3.xml'))
msg=load(os.path.join(TMP,'xl','worksheets','sheet8.xml'));mr=msg.getroot();clear_range(mr,2,88,1,3)
messages=[('AUTH-OTP-INVALID','OTP không hợp lệ hoặc hết hạn','IT01'),('AUTH-OTP-REQUIRED','Phải xác thực OTP trước đăng ký','IT01'),('APPT-CONFLICT','Lịch hẹn trùng hoặc quá gần','IT04'),('APPT-CHECKED-IN','Lịch đã được check-in','IT04'),('VISIT-ACTIVE','Bệnh nhân còn lượt khám hoạt động','IT04'),('TEST-DUPLICATE','Dịch vụ đã được chỉ định','IT09'),('RESULT-PDF-REQUIRED','Phải tải kết quả PDF','IT09'),('RESULT-VERIFIER','Chỉ head doctor được ký','IT09')]
for rn,row in enumerate(messages,2):
    for c,v in zip('ABC',row):set_text(mr,f'{c}{rn}',v,154)
save(msg,os.path.join(TMP,'xl','worksheets','sheet8.xml'))

with zipfile.ZipFile(OUT,'w',zipfile.ZIP_DEFLATED) as z:
    for root,_,files in os.walk(TMP):
        for f in files:
            p=os.path.join(root,f);z.write(p,os.path.relpath(p,TMP))
print(OUT)
