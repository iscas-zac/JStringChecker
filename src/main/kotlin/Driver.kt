import java.io.File

fun main(args: Array<String> = arrayOf("D:\\learning\\JustinStr-New\\out\\fastcsv\\")) {
    val runOnAllProjects = false
    if (runOnAllProjects) {
        for (folder in File("D:\\learning\\JustinStr-New\\out\\fastcsv\\fastcsv-3.4.0.jar").listFiles { file -> file.isDirectory() }!!){
            println("processing ${folder.name}")
            try {
                File(folder, "smt").deleteRecursively()
                interpret(folder.absolutePath)
            } catch (e: Throwable) {
                println(e)
            }
        }
    } else {
        val file = if (args.isNotEmpty()) args[0] else "D:\\learning\\JustinStr-New\\out\\fastcsv\\"
//        val file = "C:\\Users\\yyzha\\Desktop\\jars\\hutool"
        File(file, "smt").deleteRecursively()
        interpret(file)
//        println(model_list.joinToString(",\n") { "\"${it.signature}\": ${if (it.definition != null) 2 else if (it.preCond != null || it.postCond != null) 1 else 0}" })
//        print(model_list.filter { it.definition != null }.size)
    }
}

fun main1() { interpret("C:\\Users\\yyzha\\Desktop\\path\\JustinStr-New\\小论文资料\\issre22-public-data\\experiment\\dataset for RQ1\\out");
    println(model_list.joinToString(",\n") { "\"${it.signature}\": ${if (it.definition != null) 2 else if (it.preCond != null || it.postCond != null) 1 else 0}" })}
