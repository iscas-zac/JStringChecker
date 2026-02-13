package extension

import Slicer
import compatibleSmtlibTransformer
import pathYielder
import soot.SootMethod
import soot.toolkits.graph.ExceptionalBlockGraph
import summarizePath
import java.io.File


fun transformAndOutputAt(methodName: String, methodSig: String, method: SootMethod, outRoot: File, cnt: Int) {
    if (!method.hasActiveBody()) return
    val b = method.activeBody
    val units = b.units
    val slicer = Slicer(listOf(soot.toolkits.graph.Block(units.first, units.last, b, 0, 0, null)))
    val dir = File(outRoot, "method-" + "${methodName}__${methodSig.hashCode()}".replace("<", "\$lt;").replace(">", "\$gt;"))
    if (dir.isDirectory() || dir.mkdir() && slicer.getApiTypes().values.sum() > 0) {
        slicer.setMethodBody(b)
        val (normal, _) = compatibleSmtlibTransformer(slicer)
        val additional = "\n;seq ${slicer.getApisInvokeOrder().joinToString(";\t")}\n;cnt {${
            slicer.getApiTypes().map { (k, v) -> "\"$k\": $v" }.joinToString(",")
        }}\n;stmts ${summarizePath(slicer.stmts)}\n;block_num ${slicer.programPath.size}"
        File(dir, "$cnt.smt2").writeText(normal + additional)
    }
    if (dir.listFiles()?.isEmpty() == true) dir.delete()
}