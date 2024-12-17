import soot.*
import soot.jimple.Stmt
import soot.options.Options
import soot.toolkits.graph.ExceptionalBlockGraph
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

const val path_limit = 10

fun main(args: Array<String>) {
    println(args[0])
    val dataRoot = File(args[0])
    if (!dataRoot.isDirectory() && !dataRoot.mkdir()) return
    val jar = dataRoot.listFiles { _, name -> name.endsWith(".jar") }?.first()!!
    val smtFolder = File(dataRoot, "smt")
    if (!smtFolder.isDirectory() && !smtFolder.mkdir()) return
    val stringStat = mutableMapOf<SootMethod, Int>()
    for (pathsOfFunc in slice(jar.absolutePath)) { // write to .path files
        val dir = File(smtFolder, "method-" + pathsOfFunc.key.replace("<", "《").replace(">", "》"))
        if (dir.isDirectory() || dir.mkdir()) {
            val (b, slices) = pathsOfFunc.value
            slices.filter { it.isStringRelated() }.take(path_limit).forEachIndexed { index, slicer ->
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
    val stringStat = ConcurrentHashMap<SootMethod, Int>()
    val meths = mutableListOf<SootMethod>()
    sliceAndOutput(jar.absolutePath) { funcName, body, index, slicer ->
        val dir = File(smtFolder, "method-" + funcName.replace("<", "\$lt;").replace(">", "\$gt;"))
        if (dir.isDirectory() || dir.mkdir() && slicer.getApiTypes().values.sum() > 0) {
            slicer.setMethodBody(body)
            val (normal, deviants) = compatibleSmtlibTransformer(slicer)
            val additional = "\n;seq ${slicer.getApisInvokeOrder().joinToString(";\t")}\n;cnt {${
                slicer.getApiTypes().map { (k, v) -> "\"$k\": $v" }.joinToString(",")
            }}\n;stmts ${slicer.stmts.joinToString(";\t")}\n;block_num ${slicer.programPath.size}"
            File(dir, "$index.smt2").writeText(normal + additional)
            if (body.method.exceptions.isEmpty()) // for now only include deviants with un-catchable exceptions
                deviants.forEachIndexed { num, text ->
                    File(dir, "$index-deviant-$num.smt2").writeText(text + additional)
                }
        }

        // delete empty folders
        if (dir.listFiles()?.isEmpty() == true) dir.delete()

        // string api usage statistics for each method
        if (body.method !in meths && dir.exists()) {
            File(dir, "flags.txt").writeText("is public: ${body.method.isPublic}")

            meths.add(body.method)
            body.units.mapNotNull { unit ->
                if ((unit as Stmt).containsInvokeExpr())
                    unit.invokeExpr.method
                else null
            }.filter {
                it.declaringClass.name.contains("java.lang.String") ||
                        it.declaringClass.name.contains("java.lang.CharSequence") ||
                        it.declaringClass.name.contains("StringUtils")
            }.groupBy { it }
                .mapValues { it.value.count() }
                .forEach { (meth, cnt) -> stringStat.merge(meth, cnt) { acc, n -> acc + n } }
        }
    }
    val signatureTableForDefinition = model_list.filter { it.isFullyModeled }.associate { it.signature to it }
    val signatureTableForAll = model_list.associate { it.signature to it }
//    println(stringStat.toList().sortedBy { it.second }
//        .joinToString("\n") { "$it ${it.first.toString() in signatureTableForAll}" })
    println("${stringStat.count { (meth, _) -> meth.toString() in signatureTableForDefinition }} / ${stringStat.count { (meth, _) -> meth.toString() in signatureTableForAll }} / ${stringStat.count()}")
    println("${stringStat.filter { (meth, _) -> meth.toString() in signatureTableForDefinition }.values.sum()} / ${stringStat.filter { (meth, _) -> meth.toString() in signatureTableForAll }.values.sum()} / ${stringStat.values.sum()}")
    println("${stringStat.filter { (meth, _) -> meth.toString().contains("StringUtils") }.values.sum()} / ${stringStat.filter { (meth, _) -> !meth.toString().contains("StringUtils") }.values.sum()}")
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
                if (ind > start + path_limit) break
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