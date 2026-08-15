$ErrorActionPreference = 'Stop'
$source = (Resolve-Path -LiteralPath 'C:\Users\Administrator\Downloads\6698a40e40a512e5b2ae20aa_Report5_UnitTestCase.xls').Path
$outDir = 'D:\gitlap\doAnSummer2026\work\unit_template_excel_review'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$excel = New-Object -ComObject Excel.Application
$excel.Visible = $false
$excel.DisplayAlerts = $false
try {
    $book = $excel.Workbooks.Open($source, 0, $true)
    $result = @()
    foreach ($sheet in $book.Worksheets) {
        $used = $sheet.UsedRange
        $columns = @()
        for ($c = 1; $c -le $used.Columns.Count; $c++) {
            $cell = $used.Cells.Item(1, $c)
            $columns += [ordered]@{ index = $c; letter = $cell.Address($false,$false) -replace '\d',''; width = [math]::Round([double]$used.Columns.Item($c).ColumnWidth,2); hidden = [bool]$used.Columns.Item($c).Hidden }
        }
        $rows = @()
        for ($r = 1; $r -le [math]::Min($used.Rows.Count, 80); $r++) {
            $rows += [ordered]@{ index = $r; height = [math]::Round([double]$used.Rows.Item($r).RowHeight,2); hidden = [bool]$used.Rows.Item($r).Hidden }
        }
        $merges = @()
        foreach ($area in $used.MergeAreas) { $merges += $area.Address($false,$false) }
        $styleSamples = @()
        foreach ($address in @('A1','A2','A3','A4','A5','A6','A8','A9','A10','B2','C2','F2','A32')) {
            $cell = $sheet.Range($address)
            $styleSamples += [ordered]@{
                address=$address; value=[string]$cell.Text; font=$cell.Font.Name; fontSize=$cell.Font.Size; bold=[bool]$cell.Font.Bold; italic=[bool]$cell.Font.Italic;
                fontColor=[long]$cell.Font.Color; fillColor=[long]$cell.Interior.Color; horizontal=[long]$cell.HorizontalAlignment; vertical=[long]$cell.VerticalAlignment;
                wrap=[bool]$cell.WrapText; numberFormat=[string]$cell.NumberFormat; style=[string]$cell.Style
            }
        }
        $result += [ordered]@{
            name=$sheet.Name; usedRange=$used.Address($false,$false); rows=$used.Rows.Count; columns=$used.Columns.Count;
            columnLayout=$columns; rowLayout=$rows; mergedRanges=($merges | Select-Object -Unique); freezePanes=[bool]$sheet.Application.ActiveWindow.FreezePanes;
            zoom=$sheet.Application.ActiveWindow.Zoom; orientation=$sheet.PageSetup.Orientation; printArea=$sheet.PageSetup.PrintArea; shapes=$sheet.Shapes.Count; styleSamples=$styleSamples
        }
        $safe = ($sheet.Name -replace '[^A-Za-z0-9_-]','_')
        $sheet.ExportAsFixedFormat(0, (Join-Path $outDir ($safe + '.pdf')), 0, $true, $false)
    }
    $result | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $outDir 'template_layout.json') -Encoding UTF8
    Write-Output "INSPECTED sheets=$($book.Worksheets.Count) output=$outDir"
    $book.Close($false)
}
finally {
    $excel.Quit()
    [System.Runtime.InteropServices.Marshal]::FinalReleaseComObject($excel) | Out-Null
}
