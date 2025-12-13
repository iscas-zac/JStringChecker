//import soot.Body
//import soot.BodyTransformer
//import soot.G
//import soot.Scene
//import soot.options.Options
//import soot.toolkits.graph.ExceptionalBlockGraph
//import java.util.HashMap
//
//fun main() {
//
//    G.reset()
//    Options.v().set_prepend_classpath(true)
//
//    Options.v().set_src_prec(Options.src_prec_class)
//    Options.v().set_process_dir(Collections.singletonList(classPath))
//    Options.v().set_allow_phantom_refs(true)
//    Scene.v().addBasicClass("java.lang.String", SootClass.BODIES)
//    Scene.v().loadNecessaryClasses()
//    val pathsOfFunc = HashMap<String, List<Slicer>>()
//
//    PackManager.v().getPack("jtp").add(Transform("jtp.mySlicer", object : BodyTransformer() {
//        override fun internalTransform(b: Body?, phaseName: String?, options: MutableMap<String, String>?) {
//            val blockCFG = ExceptionalBlockGraph(b)
//            var paths = constructPath(blockCFG)
//            val slicers = paths.map { Slicer(it) }
//            if (b?.method?.signature != null)
//                pathsOfFunc["${b.method.declaringClass.name}__${b.method.name}__${b.method?.signature.hashCode()}"] =
//                    slicers
//        }
//    }))
//    PackManager.v().runPacks()
//}