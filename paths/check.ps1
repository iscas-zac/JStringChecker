$directoryPath = "D:\IdeaProjects\test_native_build\paths\"
$smt2Files = Get-ChildItem -Path $directoryPath -Recurse -File -Filter "*.path"

$validCount = 0
$invalidCount = 0

$slowest = 0
$slowestFile = $null
foreach ($file in $smt2Files) {
    $measure = Measure-Command { 
        $z3Output = & z3 -smt2 $file.FullName
    }
    # Write $file.FullName
    # Write $z3Output

    # Check if Z3 output indicates the file is valid (this is a placeholder condition)
    if ($z3Output | Select-String "\(error" | Select-String -NotMatch "unsat core" | Select-String -NotMatch "model is not available") {
        $invalidCount++
        Write-Host "Invalid file: $($file.FullName)"
    } else {
        $validCount++
    }

    if ($measure.TotalSeconds -gt $slowest) {
        $slowest = $measure.TotalSeconds
        $slowestFile = $file.FullName
    }
}

Write-Host "Total valid SMT2 files: $validCount"
Write-Host "Total invalid SMT2 files: $invalidCount"
Write-Host "Slowest file: $slowestFile with time ${slowest} seconds"