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
import soot.jimple.BinopExpr
import soot.jimple.Constant
import soot.jimple.IfStmt
import soot.jimple.ReturnStmt
import soot.toolkits.graph.BriefUnitGraph
import soot.toolkits.graph.UnitGraph
import test.EnhancedCFGPathExtractor
import test.PathMethodWrapper.wrapPathAsNewMethod
import java.io.File.separator
import java.io.IOException
import java.nio.file.Files.createDirectories
import java.nio.file.Files.write
import java.nio.file.Paths.get
import java.nio.file.StandardOpenOption

class UselessTag(val i: Int) : soot.tagkit.Tag {
    override fun getName(): String {
        return i.toString()
    }

    override fun getValue(): ByteArray {
        return byteArrayOf(i.toByte())
    }
}

class SimpleUnitGraph(body: Body, units: List<soot.Unit>) : UnitGraph(body) {
    init {
        unitChain.removeAll(unitChain)
        units[0].defBoxes
        val us = units.mapIndexed { index, unit ->
            val u = unit.clone() as soot.Unit
            u.addTag(UselessTag(index))

            u
        }
        unitChain.addAll(us)
        if (heads != null) {
            heads.removeAll(heads)
            if (units.isNotEmpty()) {
                heads.add(units.first())
            }
        }
        if (tails != null) {
            tails.removeAll(tails)
            if (units.isNotEmpty()) {
                tails.add(units.last())
            }
        }

    }
}

fun main() {
    init("C:\\Users\\yyzha\\Desktop\\jars\\byproduct\\dataset_rev\\awesome\\fastcsv\\fastcsv-3.4.0.jar",
        "C:\\Users\\yyzha\\Desktop\\path\\JustinStr-New\\out\\results\\awesome\\fastcsv")
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

fun init(inputPath: String, outputPath: String) {
    Config.onceConfig("C:\\Program Files\\Eclipse Adoptium\\jdk-8.0.345.1-hotspot\\jre", outputPath, inputPath, -1, -1, -1, -1, -1)
    val constraintsForAllClasses: MutableMap<String?, MutableMap<String, MutableMap<Int, ParamConstraintVO>>> =
            newHashMap()

    val f = {
        val applicationClasses = HashSet(v().getApplicationClasses())
        for (sootClass in applicationClasses) {
            if (isIgnoredClass(sootClass) ||
                (pluginStart && !CLASS_NAME_UNDER_TEST.contains(sootClass.getName()))
            ) {
                continue
            } else {
                TEST_CLASS_NUM++
            }
            var res = ""

            val thread = Thread {
                val constraintsForEachClass: MutableMap<String, MutableMap<Int, ParamConstraintVO>> =
                    newHashMap()
                val sootMethods = HashSet(sootClass.methods)
                for (sootMethod in sootMethods) {
                    if (isIgnoredMethod(sootMethod) || !sootMethod.hasActiveBody()) {
                        continue
                    }

                    val body = sootMethod.getActiveBody()

                    val savedResult: MutableMap<Int, ParamConstraintVO> = newHashMap()
                    val filteredPaths = EnhancedCFGPathExtractor.extractPathsWithFilter(sootMethod, 2)
                    if (filteredPaths.isEmpty()) continue
                    val pathInfo = filteredPaths[0]
                    if (pathInfo.hasStringAPI && pathInfo.usesStringParams) {
                        val newMethod = wrapPathAsNewMethod(pathInfo, sootMethod, 0);
                        if (newMethod.getActiveBody() != null) {
                            newMethod.getActiveBody().validate();
                            println("Successfully created method: " + newMethod.getSignature());

                            // 打印新方法体
                            println("Method body:");
                            newMethod.activeBody.units.forEach {
                                println("  $it")
                            }
                        }
                    }
                    for (p in extractSootMethodPaths(body)) {
                            val unitGraph = SimpleUnitGraph(body, p)
                            for (unit in body.units) {
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
                                                    unitGraph,
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
                                                    unitGraph,
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
                                            unitGraph,
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
                                    res += ("invoked name:" +
                                            paramConstraintInfo.methodCallList.toList()
                                                .joinToString { obj: MethodCallDTO? -> obj?.methodName.toString() } +
                                            "\ncomparison:" +
                                            paramConstraintInfo.compareValue +
                                            "\ncaller name:" +
                                            body.getMethod().getName() +
                                            "\ncaller class:" +
                                            body.getMethod().getDeclaringClass() +
                                            "\nunit:" +
                                            unit +
                                            "\nparams:" +
                                            paramConstraintInfo.paramType + " " + paramConstraintInfo.paramIndex +
                                            "\nhash:" +
                                            p.hashCode() +
                                            "\nresult:" +
                                            savedResult.mapValues { it.value.possibleValuesForObject } +
                                            "\n\n"
                                            )
                                }
                        }
                    }
                    if (!savedResult.isEmpty()) {
                        constraintsForEachClass[sootMethod.signature] = savedResult
                    }
                }
                if (!constraintsForEachClass.isEmpty()) {
                    constraintsForAllClasses[sootClass.getName()] = constraintsForEachClass
                }
                try {
                    val path = get(TEST_OUTPUT_FOLDER + separator + "paths.txt")
                    createDirectories(path.parent)

                    write(
                        path, res.toByteArray(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND)
                } catch (e: IOException) {
                    println("not written $e")
                }
            }

            thread.start()
            try {
                val threadTime = 60L * 1000
                thread.join(threadTime)
            } catch (_: InterruptedException) {
                // ...
            }
            // 如果线程仍在执行，就中断它
            if (thread.isAlive) {
                thread.interrupt()
            }
        }
        PARAM_CONSTRAINTS_VOS = constraintsForAllClasses
    }
    f()
    GenerationFactory.generateClasses()
}
