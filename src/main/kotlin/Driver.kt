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
    }
}
