# SMT-LIB Benchmark StriCT-BJ For SANER 2025

## Benchmark

The benchmark is on the release page as a single `.zip` file.
It includes the SMT-LIB files and various scripts to produce
the paper data.

## Generating tool

The latest tool is on the `soot-parse-smtlib` branch.
To use it, please modify the `src/main/kotlin/Driver.kt`
to add the `.jar` file path in it. For the `runOnAllProjects`
flag, every sub-folder under the path should contain a `.jar` file.
