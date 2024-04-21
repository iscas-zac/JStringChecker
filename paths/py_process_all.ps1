$a = @("antlr", "commons-lang", "efficient-boot-common", "gson", "junit", "junit-jupiter-api", "lombok", "slf4j")

foreach ($i in $a) {
    Remove-Item C:\Users\yyzha\Desktop\jars\$i\simple_statistics.json
    py D:\IdeaProjects\test_native_build\paths\statistic.py C:\Users\yyzha\Desktop\jars\$i\
}