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
            pathsOfFunc.value.filter { it.isStringRelated() }.take(10000).forEachIndexed { index, slicer ->
            val (b, slices) = pathsOfFunc.value
            slices.filter { it.isStringRelated() }.take(10000).forEachIndexed { index, slicer ->
                val (normal, deviants) = compatibleSmtlibTransformer(slicer)
                File(dir, "$index.path").writeText(normal)
                if (b.method.exceptions.isEmpty()) // TODO: for now only include deviants if not throws
                    deviants.forEachIndexed { num, text ->
                        File(dir, "$index-deviant-$num.path").writeText(text)
                    }
                File(dir, "statistics.txt").writeText(slicer.getStatistics())
            }
            println(pathsOfFunc.key + "    " + slices.filter { it.isStringRelated() }.size + " / " + slices.size)
            println(slices.any { it.isStringRelated() })
        }
    }
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