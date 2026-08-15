$ErrorActionPreference='Continue'
$dir='D:\gitlap\doAnSummer2026\outputs\019ff5dc-080c-7230-b9b2-751838ad3212\unit_test_normalized'
$excel=New-Object -ComObject Excel.Application
$excel.Visible=$false
$excel.DisplayAlerts=$false
$results=@()
try{
 foreach($file in Get-ChildItem -LiteralPath $dir -Filter *.xlsx|Where-Object{$_.Name -notlike '~$*'}|Sort-Object Name){
  try{$b=$excel.Workbooks.Open($file.FullName,0,$true);$count=$b.Worksheets.Count;$b.Close($false);$results+=[pscustomobject]@{name=$file.Name;ok=$true;sheets=$count};Write-Output "OPEN_OK $($file.Name) sheets=$count"}
  catch{$results+=[pscustomobject]@{name=$file.Name;ok=$false;error=$_.Exception.Message};Write-Output "OPEN_FAIL $($file.Name)"}
  finally{$b=$null}
 }
}finally{$results|ConvertTo-Json -Depth 3|Set-Content 'D:\gitlap\doAnSummer2026\work\unit_excel_open_results.json' -Encoding UTF8;$excel.Quit();[System.Runtime.InteropServices.Marshal]::FinalReleaseComObject($excel)|Out-Null}
