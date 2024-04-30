import soot.*
import soot.options.Options
import soot.toolkits.graph.ExceptionalBlockGraph
import java.io.File
import java.util.*


fun main(args: Array<String>) {
    val dataRoot = File(args[0])
    if (!dataRoot.isDirectory() && !dataRoot.mkdir()) return
    val jar = dataRoot.listFiles { _, name -> name.endsWith(".jar") }?.first()!!
    val smtFolder = File(dataRoot, "smt")
    if (!smtFolder.isDirectory() && !smtFolder.mkdir()) return
    for (pathsOfFunc in slice(jar.absolutePath)) { // write to .path files
        val dir = File(smtFolder, "method-" + pathsOfFunc.key.replace("<", "《").replace(">", "》"))
        if (dir.isDirectory() || dir.mkdir()) {
            val (b, slices) = pathsOfFunc.value
            slices.filter { it.isStringRelated() }.take(10000).forEachIndexed { index, slicer ->
                val (normal, deviants) = compatibleSmtlibTransformer(slicer)
                File(dir, "$index.path").writeText(normal)
                if (b.method.exceptions.isEmpty()) // TODO: for now only include deviants if not throws
                    deviants.forEachIndexed { num, text ->
                        File(dir, "$index-deviant-$num.path").writeText(text)
                    }
                File(dir, "statistics.txt").writeText(slicer.getStatistics())
                slicer.getApiTypes().forEach { (meth, cnt) -> stringStat.merge(meth, cnt) { acc, n -> acc + n } }
            }
            println(pathsOfFunc.key + "    " + slices.filter { it.isStringRelated() }.size + " / " + slices.size)
            println(slices.any { it.isStringRelated() })
        }
    }
    println(stringStat.toList().sortedBy { it.second }.joinToString("\n"))

//    try {
//        // Execute the PowerShell command
//        val process = Runtime.getRuntime().exec("pwsh.exe -File D:\\IdeaProjects\\test_native_build\\paths\\check.ps1")
//
//        // Get the output stream of the process
//        val reader = BufferedReader(InputStreamReader(process.inputStream))
//
//        // Read and print the output
//        var line: String?
//        while ((reader.readLine().also { line = it }) != null) {
//            println(line)
//        }
//
//        // Close the reader
//        reader.close()
//    } catch (e: Exception) {
//        e.printStackTrace()
//    }
}

fun compatibleSmtlibTransformer(slicer: Slicer) = slicer.smtExpand()

/// produce raw info of every method, which contains the program path, path conditions
// and some statistics
fun slice(classPath: String): HashMap<String, Pair<Body, List<Slicer>>> {
    // init soot
    G.reset()
    Options.v().set_prepend_classpath(true)

    Options.v().set_src_prec(Options.src_prec_class)
    //Options.v().set_output_format(Options.output_format_shimple)
    Options.v().set_process_dir(Collections.singletonList(classPath))
    Options.v().set_allow_phantom_refs(true)
    Scene.v().addBasicClass("java.lang.String", SootClass.BODIES)
    Scene.v().loadNecessaryClasses()
    val pathsOfFunc = HashMap<String, Pair<Body, List<Slicer>>>()

    PackManager.v().getPack("jtp").add(Transform("jtp.mySlicer", object : BodyTransformer() {
        override fun internalTransform(b: Body?, phaseName: String?, options: MutableMap<String, String>?) {
            val blockCFG = ExceptionalBlockGraph(b)
            var paths = constructPath(blockCFG)
//            val blocksWithStringOps = blockCFG.blocks.filter { hasStringOps(it) }
//            paths = paths.filterOutNotContainAny(blocksWithStringOps)
            val slicers = paths.map { Slicer(it) }
            if (b?.method?.signature != null)
                pathsOfFunc["${b.method.declaringClass.name}__${b.method.name}__${b.method?.signature.hashCode()}"] = b to slicers
        }
    }))
    PackManager.v().runPacks()
    return pathsOfFunc
}

/** a remake version of above `main` and `slice`, which pass the output function into the transform
 * and process them on the fly
 */
fun interpret(path: String) {
    val dataRoot = File(path)
    if (!dataRoot.isDirectory() && !dataRoot.mkdir()) return
    val jar = dataRoot.listFiles { _, name -> name.endsWith(".jar") }?.first()!!
    val smtFolder = File(dataRoot, "smt")
    if (!smtFolder.isDirectory() && !smtFolder.mkdir()) return
    val stringStat = mutableMapOf<SootMethod, Int>()
    sliceAndOutput(jar.absolutePath) { funcName, body, index, slicer ->
        val dir = File(smtFolder, "method-" + funcName.replace("<", "《").replace(">", "》"))
        if (dir.isDirectory() || dir.mkdir() && slicer.getApiTypes().values.sum() > 0) {
            val (normal, deviants) = compatibleSmtlibTransformer(slicer)
            File(dir, "$index.path").writeText(normal)
            if (body.method.exceptions.isEmpty()) // TODO: for now only include deviants if not throws
                deviants.forEachIndexed { num, text ->
                    File(dir, "$index-deviant-$num.path").writeText(text)
                }
        }
        slicer.getApiTypes().forEach { (meth, cnt) -> stringStat.merge(meth, cnt) { acc, n -> acc + n } }
    }
    println(stringStat.toList().sortedBy { it.second }.joinToString("\n"))
}

/** accept the class file path and directly emit the output during transform
 */
fun sliceAndOutput(classPath: String, output: (String, Body, Int, Slicer) -> Unit) {
    G.reset()
    Options.v().set_prepend_classpath(true)

    Options.v().set_src_prec(Options.src_prec_class)
    Options.v().set_process_dir(Collections.singletonList(classPath))
    Options.v().set_allow_phantom_refs(true)
    Scene.v().loadNecessaryClasses()
    var ind = 0

    PackManager.v().getPack("jtp").add(Transform("jtp.mySlicer", object : BodyTransformer() {
        override fun internalTransform(b: Body, phaseName: String?, options: MutableMap<String, String>?) {
//            if (!b.method.declaringClass.name.contains("MySQLAccess") || !b.method.name.contains("clinit"))
//                return
            val start = ind
            // unroll the list operation to sequence
            for (paths in pathYielder(ExceptionalBlockGraph(b))) {
                // TODO: limit the num for now
                if (ind > start + 500) break
                for (path in paths) {
                    if (b.method?.signature != null)
                        output(
                            "${b.method.declaringClass.name}__${b.method.name}__${b.method?.signature.hashCode()}",
                            b,
                            ind,
                            Slicer(path)
                        )
                    ind++
                }
            }
        }
    }))
    PackManager.v().runPacks()
}