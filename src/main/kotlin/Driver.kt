import java.io.File

fun main1() {
    val runOnAllProjects = false
    if (runOnAllProjects) {
        for (folder in File("C:\\Users\\yyzha\\Desktop\\jars\\byproduct\\dataset_rev\\top20_gh\\elasticsearch").listFiles { file -> file.isDirectory() }!!){
            println("processing ${folder.name}")
            try {
                File(folder, "smt").deleteRecursively()
                interpret(folder.absolutePath)
            } catch (e: Throwable) {
                println(e)
            }
        }
    } else {
        val file = "C:\\Users\\yyzha\\Desktop\\jars\\byproduct\\dataset_rev\\top20_gh\\elasticsearch"
//        val file = "C:\\Users\\yyzha\\Desktop\\jars\\hutool"
        File(file, "smt").deleteRecursively()
        interpret(file)
//        println(model_list.joinToString(",\n") { "\"${it.signature}\": ${if (it.definition != null) 2 else if (it.preCond != null || it.postCond != null) 1 else 0}" })
        print(model_list.filter { it.definition != null }.size)
    }
}

fun main() { interpret("C:\\Users\\yyzha\\Desktop\\path\\JustinStr-New\\小论文资料\\issre22-public-data\\experiment\\dataset for RQ1\\out");
    println(model_list.joinToString(",\n") { "\"${it.signature}\": ${if (it.definition != null) 2 else if (it.preCond != null || it.postCond != null) 1 else 0}" })}
