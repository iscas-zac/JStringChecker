param(
    [string]
    $directoryPath = "D:\IdeaProjects\paths\gson"
)

$smt2Files = Get-ChildItem -Path $directoryPath -Recurse -File -Filter "*.path"

$validCount = 0
$invalidCount = 0

$slowest = 0
$slowestFile = $null

$totalCount = $smt2Files.Count
$cnt = 0
foreach ($file in $smt2Files) {
    $measure = Measure-Command {
        # $errOut = & { $global:z3Output = z3 -smt2 $file.FullName } 2>&1
        $z3Output = & z3 -smt2 $file.FullName -T:2
    }
    if ($errOut) {
        Write $errOut
        Write $file.FullName
        break
    }

    # Write $file.FullName
    # Write $z3Output

    # Check if Z3 output indicates the file is valid (this is a placeholder condition)
    if ($z3Output | Select-String '\(error "line' | Select-String -NotMatch "unsat core" | Select-String -NotMatch "model is not available") {
        $invalidCount++
        Write-Host "Invalid file: $($file.FullName)"
    } else {
        $validCount++
    }

    if ($measure.TotalSeconds -gt $slowest) {
        $slowest = $measure.TotalSeconds
        $slowestFile = $file.FullName
    }

    $cnt++
    Write-Host "`r$($cnt)/$totalCount" -NoNewline
    Write-Host "`r" -NoNewline
}

Write-Host "Total valid SMT2 files: $validCount"
Write-Host "Total invalid SMT2 files: $invalidCount"
Write-Host "Slowest file: $slowestFile with time ${slowest} seconds"