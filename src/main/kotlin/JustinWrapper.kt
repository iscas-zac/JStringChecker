import cn.ios.casegen.config.Config
import cn.ios.casegen.config.GlobalCons.*
import cn.ios.casegen.constraint.DTO.MethodCallDTO
import cn.ios.casegen.constraint.DTO.ParamConstraintDTO
import cn.ios.casegen.constraint.VO.ParamConstraintVO
import cn.ios.casegen.constraint.generate.GenConstraints
import cn.ios.casegen.constraint.generate.Trace.traceLocal
import cn.ios.casegen.enums.TraceTypeEnum
import cn.ios.casegen.generator.GenerationFactory
import cn.ios.casegen.util.ClassUtil.isIgnoredClass
import cn.ios.casegen.util.ClassUtil.isIgnoredMethod
import com.google.common.collect.Maps.newHashMap
import soot.Body
import soot.Local
import soot.Scene.v
import soot.SootMethod
import soot.jimple.*
import soot.toolkits.graph.Block
import soot.toolkits.graph.BriefBlockGraph
import soot.toolkits.graph.BriefUnitGraph
import java.io.File
import java.io.File.separator
import java.nio.file.Files.createDirectories
import java.nio.file.Paths.get
import java.nio.file.StandardOpenOption
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.io.path.writeText

class UselessTag(val i: Int) : soot.tagkit.Tag {
    override fun getName(): String {
        return i.toString()
    }

    override fun getValue(): ByteArray {
        return byteArrayOf(i.toByte())
    }
}

fun main() {
    val baseInput = "C:\\Users\\yyzha\\Desktop\\jars\\byproduct\\dataset_rev"
    val baseOutput = "D:\\learning\\JustinStr-New\\out\\results"

    var flag = false
    File(baseInput).walk().maxDepth(3).filter {
        it.isFile && it.extension.equals("jar", ignoreCase = true)
    }.forEach { jarFile ->
        // 获取相对路径（如 "awesome\fastcsv"）
        val relativePath = jarFile.parentFile.relativeTo(File(baseInput)).path
        if (relativePath.contains("httpclient")) { flag = true }
        if (flag) {
            runCatching {
                // 假设要覆写的文件名为 "marker.txt"，位于输出目录下
                File("$baseOutput\\$relativePath\\justinStr-result\\extension", "paths.txt").delete()
                File("$baseOutput\\$relativePath", "smt").deleteRecursively()
            }
            println(relativePath)
            val stringStat = ConcurrentHashMap<SootMethod, Int>()
            val meths = mutableListOf<SootMethod>()
            val strictProcess = { funcName: String, body: Body, index: Int, slicer: Slicer ->
                val dir = File(
                    "$baseOutput\\$relativePath\\smt",
                    "method-" + funcName.replace("<", "\$lt;").replace(">", "\$gt;")
                )
                createDirectories(dir.parentFile.toPath())
                if (slicer.getApiTypes().values.sum() > 0 && dir.isDirectory() || dir.mkdir()) {
                    slicer.setMethodBody(body)
                    val (normal, _) = compatibleSmtlibTransformer(slicer)
                    val additional = "\n;seq ${slicer.getApisInvokeOrder().joinToString(";\t")}\n;cnt {${
                        slicer.getApiTypes().map { (k, v) -> "\"$k\": $v" }.joinToString(",")
                    }}\n;stmts ${slicer.stmts.joinToString(";\t")}\n;block_num ${slicer.programPath.size}"
                    File(dir, "$index.smt2").writeText(normal + additional)
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
            init(jarFile.absolutePath, "$baseOutput\\$relativePath", strictProcess)
            return
        }
    }
//    init("C:\\Users\\yyzha\\Desktop\\jars\\byproduct\\dataset_rev\\awesome\\fastcsv\\fastcsv-3.4.0.jar",
//        "C:\\Users\\yyzha\\Desktop\\path\\JustinStr-New\\out\\results\\awesome\\fastcsv")
}

fun extractSootMethodPaths(
    body: Body,
    maxBlockVisits: Int = 2,
    maxPathLength: Int = 1000,
    timeoutMs: Long = 30000
): List<List<soot.Unit>> {
    val cfg = BriefUnitGraph(body)

    val paths = mutableListOf<List<soot.Unit>>()
    val blockVisitCounter = mutableMapOf<soot.Unit, Int>()
    val currentPath = mutableListOf<soot.Unit>()
    val startTime = System.currentTimeMillis()

    // 深度优先搜索遍历
    fun traverse(currentUnit: soot.Unit) {
        // 检查超时和路径长度限制
        if (System.currentTimeMillis() - startTime > timeoutMs) return
        if (currentPath.size >= maxPathLength) return

        // 检查循环展开限制
        val visitCount = blockVisitCounter.getOrDefault(currentUnit, 0)
        if (visitCount >= maxBlockVisits) return // 达到访问上限，剪枝

        // 标记当前节点访问
        blockVisitCounter[currentUnit] = visitCount + 1
        currentPath.add(currentUnit)

        // 如果是出口节点，保存完整路径
        if (cfg.tails.contains(currentUnit)) {
            paths.add(currentPath.toList()) // 保存副本
        } else {
            // 递归遍历所有后继节点
            cfg.getSuccsOf(currentUnit).forEach { successor ->
                traverse(successor)
            }
        }

        // 回溯：恢复状态
        currentPath.removeAt(currentPath.lastIndex)
        blockVisitCounter[currentUnit] = visitCount
    }

    // 从所有入口节点开始遍历
    cfg.heads.forEach { entryPoint ->
        traverse(entryPoint)
    }

    return paths.toList()
}

fun extractSootBlockPaths(
    body: Body,
    maxBlockVisits: Int = 2,
    maxPathLength: Int = 1000,
    timeoutMs: Long = 30000
): Sequence<List<Block>> = sequence {
    val cfg = BriefBlockGraph(body)
    val startTime = System.currentTimeMillis()

    // 栈帧存储遍历状态
    data class Frame(
        val block: Block,
        val path: List<Block>,
        val visitCounts: Map<Block, Int>,
        val iterator: Iterator<Block>? = null
    )

    // 初始化栈，从所有入口基本块开始
    val stack = ArrayDeque<Frame>().apply {
        cfg.heads.forEach { head ->
            add(Frame(head, emptyList(), emptyMap()))
        }
    }

    while (stack.isNotEmpty()) {
        if (System.currentTimeMillis() - startTime > timeoutMs) break

        val top = stack.last()
        val (block, path, visitCounts, iterator) = top

        // 如果是新节点（iterator为null），处理节点逻辑
        if (iterator == null) {
            // 检查约束条件
            if (path.size >= maxPathLength) {
                stack.removeLast()
                continue
            }

            val currentCount = visitCounts.getOrDefault(block, 0)
            if (currentCount >= maxBlockVisits) {
                stack.removeLast()
                continue
            }

            // 构建新路径和访问计数
            val newPath = path.plusElement(block)
            val newVisitCounts = visitCounts + (block to currentCount + 1)

            // 如果是出口块，产出路径
            if (cfg.tails.contains(block)) {
                yield(newPath)
                stack.removeLast()
                continue
            }

            // 获取后继块迭代器
            val succIter = block.succs.iterator()
            stack[stack.lastIndex] = top.copy(iterator = succIter)

            // 将第一个后继压栈
            if (succIter.hasNext()) {
                stack.add(Frame(succIter.next(), newPath, newVisitCounts))
            }
        } else {
            // 继续遍历当前块的后继
            if (iterator.hasNext()) {
                // 将下一个后继压栈
                val newPath = path.plusElement(block)
                val currentCount = visitCounts.getOrDefault(block, 0)
                val newVisitCounts = visitCounts + (block to currentCount + 1)
                stack.add(Frame(iterator.next(), newPath, newVisitCounts))
            } else {
                // 所有后继遍历完成，回溯
                stack.removeLast()
            }
        }
    }
}

fun init(inputPath: String, outputPath: String, strictPlugin: (String, Body, Int, Slicer) -> Unit) {
    connection = null
    sootConfig = false
    Config.onceConfig("C:\\Program Files\\Eclipse Adoptium\\jdk-8.0.345.1-hotspot\\jre", outputPath, inputPath, -1, -1, -1, -1, -1)
    var index = 0
    val constraintsForAllClasses: MutableMap<String?, MutableMap<String, MutableMap<Int, ParamConstraintVO>>> =
            newHashMap()


        val applicationClasses = HashSet(v().getApplicationClasses())
        for (sootClass in applicationClasses) {
            if (isIgnoredClass(sootClass) ||
                (pluginStart && !CLASS_NAME_UNDER_TEST.contains(sootClass.getName()))
            ) {
                continue
            } else {
                TEST_CLASS_NUM++
            }

            val justinIntegratedWithStrictPlugin = { ->
                val constraintsForEachClass: MutableMap<String, MutableMap<Int, ParamConstraintVO>> =
                    newHashMap()
                val sootMethods = HashSet(sootClass.methods)
                val filePath = get(TEST_OUTPUT_FOLDER + separator + "paths.txt")
                createDirectories(filePath.parent)
                java.nio.file.Files.newBufferedWriter(
                    filePath,
                    java.nio.charset.StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
                ).use { writer ->
                    for (sootMethod in sootMethods) {
                        if (isIgnoredMethod(sootMethod) || !sootMethod.hasActiveBody()) {
                            continue
                        }

                        val body = sootMethod.getActiveBody()

                        val savedResult: MutableMap<Int, ParamConstraintVO> = newHashMap()
    
                        for (blocks in extractSootBlockPaths(body)) {
                            if (sootMethod.name.contains("getIntProperty")) {
                                println(blocks.joinToString("\n"))
                            }
                            val start = System.nanoTime()
                            strictPlugin(
                                "${body.method.declaringClass.name}__${body.method.name}__${body.method?.signature.hashCode()}",
                                body, index, Slicer(blocks.reversed())
                            )
                            val strictTime = System.nanoTime()
                            val p = blocks.flatten()
                            for (unit in p) {
                                val paramConstraintInfo = ParamConstraintDTO()
                                if (unit is IfStmt) {
                                    val conditionExpr = unit.condition
                                    if (conditionExpr is BinopExpr) {
                                        paramConstraintInfo.operator = conditionExpr.symbol
                                        val leftValue = conditionExpr.op1
                                        val rightValue = conditionExpr.op2

                                        // constant is compareValue
                                        if (rightValue is Constant) {
                                            paramConstraintInfo.compareValue = rightValue.toString()
                                            if (leftValue is Local) {
                                                traceLocal(
                                                    p,
                                                    unit,
                                                    leftValue,
                                                    paramConstraintInfo,
                                                    newHashMap<String?, Int?>(),
                                                    TraceTypeEnum.IfStmt,
                                                    -1
                                                )
                                            }
                                        } else if (leftValue is Constant) {
                                            paramConstraintInfo.compareValue = leftValue.toString()
                                            if (rightValue is Local) {
                                                traceLocal(
                                                    p,
                                                    unit,
                                                    rightValue,
                                                    paramConstraintInfo,
                                                    newHashMap<String?, Int?>(),
                                                    TraceTypeEnum.IfStmt,
                                                    -1
                                                )
                                            }
                                        }
                                    }
                                } else if (unit is ReturnStmt) {
                                    // for : return str.contains("s")
                                    val returnValue = unit.op
                                    if (returnValue is Local) {
                                        traceLocal(
                                            p,
                                            unit,
                                            returnValue,
                                            paramConstraintInfo,
                                            newHashMap<String?, Int?>(),
                                            TraceTypeEnum.ReturnStmt,
                                            -1
                                        )
                                    }
                                }

                                if (paramConstraintInfo.paramIndex >= 0) {
                                    GenConstraints.dealMethodCallList(paramConstraintInfo)
                                    // the methodCallList has already reversed
                                    GenConstraints.addToParamConstraintVO(savedResult, paramConstraintInfo)
                                    writer.write(
                                            "\ncaller name:" +
                                            body.getMethod().getName() +
                                            "\ncaller class:" +
                                            body.getMethod().getDeclaringClass() +
                                            "\nunit:" +
                                            unit +
                                            "\nindex:" +
                                            index +
                                            "\nstrict time:" +
                                            strictTime.minus(start) +
                                            "\njustin time:" +
                                            System.nanoTime().minus(strictTime) +
                                            "\nresult:" +
                                            savedResult.mapValues { it.value.possibleValuesForSimpleType } +
                                            "\n\n"
                                    )
                                }
                            }
                            index++
                        }
                        if (!savedResult.isEmpty()) {
                            constraintsForEachClass[sootMethod.signature] = savedResult
                        }
                    }
                }
                if (!constraintsForEachClass.isEmpty()) {
                    constraintsForAllClasses[sootClass.getName()] = constraintsForEachClass
                }
            }

            justinIntegratedWithStrictPlugin()
        }
        PARAM_CONSTRAINTS_VOS = constraintsForAllClasses

    val start = System.nanoTime()
    GenerationFactory.generateClasses()
    val filePath = get(outputPath, "justinStr-result", "test_time.txt")
    createDirectories(filePath.parent)
    filePath.writeText((System.nanoTime() - start).toString())
}
