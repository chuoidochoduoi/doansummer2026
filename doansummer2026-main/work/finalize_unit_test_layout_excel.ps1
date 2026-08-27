$ErrorActionPreference = 'Stop'
$outDir = 'D:\gitlap\doAnSummer2026\outputs\019ff5dc-080c-7230-b9b2-751838ad3212\unit_test_normalized'
$excel = New-Object -ComObject Excel.Application
$excel.Visible = $false
$excel.DisplayAlerts = $false
$excel.ScreenUpdating = $false
$functionWidths = @(7.5,12.75,10.13,10.75,0,2.25,2.25,2.25,2.25,2.25,2.25,2.25,2.25,2.25,2.25,2.25,2.25,2.25,2.25,2.25,2.25,8.38,8.38)
$coverWidths = @(27.63,9.38,13.75,7.38,37.38,47.63)
$listWidths = @(6.5,14.13,9.25,11.75,20.38,11.75,21.88,33.13)
$reportWidths = @(14.75,26,11.5,9,9.13,4.63,4.63,4.63,20.38)
$results = @()
try {
    $files = Get-ChildItem -LiteralPath $outDir -Filter *.xlsx | Sort-Object Name
    if ($args.Count -gt 0) { $files = $files | Where-Object Name -eq $args[0] }
    foreach ($file in $files) {
        try {
            $book = $excel.Workbooks.Open($file.FullName, 0, $false)
            foreach ($sheet in $book.Worksheets) {
                if ($sheet.Name -match '^Function\d+$' -and $sheet.UsedRange.Cells.Count -gt 1) {
                    $sheet.Columns.Item(5).Hidden = $true
                    $sheet.Rows.Item(8).RowHeight = 11.25
                    $sheet.Rows.Item(9).RowHeight = 46.5
                    $sheet.Range('F9:T9').Orientation = 90
                    $sheet.Range('F9:T9').HorizontalAlignment = -4108
                    $sheet.Range('F9:T9').VerticalAlignment = -4160
                    $sheet.Range('F9:T9').Font.Name = 'Tahoma'
                    $sheet.Range('F9:T9').Font.Size = 8
                    $sheet.Range('F9:T9').Font.Bold = $true
                    $sheet.Range('F9:T9').Font.Color = 16777215
                    $sheet.Range('L2:T7').Orientation = 0
                    $sheet.Range('L2:T7').VerticalAlignment = -4107
                    $sheet.PageSetup.PrintArea = '$A$1:$T$' + [math]::Max(48,$sheet.UsedRange.Rows.Count)
                    $sheet.PageSetup.Orientation = 1
                    $sheet.PageSetup.Zoom = $false
                    $sheet.PageSetup.FitToPagesWide = 1
                    $sheet.PageSetup.FitToPagesTall = 1
                    $sheet.PageSetup.CenterHorizontally = $true
                }
            }
            $book.Save()
            $count=$book.Worksheets.Count
            $book.Close($true)
            $results += [pscustomobject]@{name=$file.Name;ok=$true;sheets=$count}
            Write-Output "OK $($file.Name) sheets=$count"
        }
        catch {
            if ($book) { $book.Close($false) }
            $results += [pscustomobject]@{name=$file.Name;ok=$false;error=$_.Exception.Message}
            Write-Output "FAIL $($file.Name) line=$($_.InvocationInfo.ScriptLineNumber): $($_.Exception.Message)"
        }
        finally { $book = $null }
    }
}
finally {
    $results | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath 'D:\gitlap\doAnSummer2026\work\unit_excel_finalize_results.json' -Encoding UTF8
    $excel.Quit()
    [System.Runtime.InteropServices.Marshal]::FinalReleaseComObject($excel) | Out-Null
}
