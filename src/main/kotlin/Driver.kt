import java.io.File

fun main() {
    val runOnAllProjects = true
    if (runOnAllProjects) {
        for (folder in File("\\dataset").listFiles { file -> file.isDirectory() }!!){
            println("processing ${folder.name}")
            try {
                File(folder, "smt").deleteRecursively()
                interpret(folder.absolutePath)
            } catch (e: Throwable) {
                println(e)
            }
        }
    } else {
        val file = "\\hutool"
        File(file, "smt").deleteRecursively()
        interpret(file)
//        println(model_list.joinToString(",\n") { "\"${it.signature}\": ${if (it.definition != null) 2 else if (it.preCond != null || it.postCond != null) 1 else 0}" })
        print(model_list.filter { it.definition != null }.size)
    }
}
