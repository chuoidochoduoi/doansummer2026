$ErrorActionPreference='Stop'
$dir='D:\gitlap\doAnSummer2026\outputs\019ff5dc-080c-7230-b9b2-751838ad3212\unit_test_normalized'
$out='D:\gitlap\doAnSummer2026\work\final_unit_visual_samples'
New-Item -ItemType Directory -Force -Path $out|Out-Null
$items=@(
@('AccountService_UnitTestCase_FIXED.xlsx','Function1','Account_Function1.pdf'),
@('MedicalRecordService_UnitTestCase.xlsx','Function23','MedicalRecord_Function23.pdf'),
@('TestRequestService_UnitTestCase_UPDATED_CURRENT.xlsx','Function1','TestRequest_Function1.pdf'),
@('QueueTicketService_UnitTestCase.xlsx','Test Report','QueueTicket_Report.pdf'),
@('VitalSignsService_UnitTestCase_Template_FIXED_v2.xlsx','Function5','VitalSigns_Function5.pdf'))
$excel=New-Object -ComObject Excel.Application;$excel.Visible=$false;$excel.DisplayAlerts=$false
try{foreach($x in $items){$b=$excel.Workbooks.Open((Join-Path $dir $x[0]),0,$true);$s=$b.Worksheets.Item($x[1]);$s.ExportAsFixedFormat(0,(Join-Path $out $x[2]),0,$true,$false);$b.Close($false);Write-Output "EXPORTED $($x[2])"}}finally{$excel.Quit();[System.Runtime.InteropServices.Marshal]::FinalReleaseComObject($excel)|Out-Null}
