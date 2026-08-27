$ErrorActionPreference='Continue'
$node='C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe'
$builder='D:\gitlap\doAnSummer2026\work\normalize_unit_test_batch.mjs'
$out='D:\gitlap\doAnSummer2026\outputs\019ff5dc-080c-7230-b9b2-751838ad3212\unit_test_normalized'
$names=@(
'AccountService_UnitTestCase_FIXED.xlsx','AppointmentService_UnitTestCase_UPDATED_CheckIn.xlsx','AuditLogService_UnitTestCase.xlsx','AuthService_UnitTestCase.xlsx','ChatService_UnitTestCase.xlsx','CustomerVisitService_UnitTestCase.xlsx','DepartmentService_UnitTestCase.xlsx','EmailService_UnitTestCase.xlsx','InvoiceService_UnitTestCase.xlsx','InvoiceService_UnitTestCase_ACTUAL_46_OF_46_PASSED.xlsx','MedicalRecordService_UnitTestCase.xlsx','MedicalServiceService_UnitTestCase.xlsx','NotificationService_UnitTestCase.xlsx','PatientJourneyService_UnitTestCase.xlsx','ProfileService_UnitTestCase.xlsx','QueueTicketService_UnitTestCase.xlsx','ReportService_UnitTestCase.xlsx','ServiceCategoryService_UnitTestCase.xlsx','TestRequestService_UnitTestCase_UPDATED_CURRENT.xlsx','VitalSignsService_UnitTestCase_Template_FIXED_v2.xlsx')
foreach($name in $names){
  $target=Join-Path $out $name
  if(Test-Path -LiteralPath $target){Write-Output "SKIP $name";continue}
  & $node $builder $name
  if($LASTEXITCODE -ne 0){Write-Output "NODE_FAIL $name code=$LASTEXITCODE"}
}
