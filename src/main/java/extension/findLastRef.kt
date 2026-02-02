package extension

import soot.Local
import soot.Unit

fun findLastDef(trace: List<Unit>, targetLocal: Local, usageUnit: Unit): Unit? {
    // 1. 找到当前指令在路径中的位置
    // 如果你在外部遍历时已经知道 index，可以直接传 index 进来以提升性能，避免 indexOf 的 O(N) 开销
    val startIndex = trace.indexOf(usageUnit)

    if (startIndex <= 0) {
        return null // 没找到，或者是第一条指令，前面不可能有定义
    }

    // 2. 从当前指令的前一条开始，倒序遍历 (Linear Backward Scan)
    for (i in (startIndex - 1) downTo 0) {
        val candidateUnit = trace[i]

        // 3. 检查 candidateUnit 是否对 targetLocal 进行了赋值
        // Soot 的 defBoxes 包含了所有被写入的 Value
        for (defBox in candidateUnit.defBoxes) {
            if (defBox.value == targetLocal) {
                // 找到了！直接返回，这就是最近的一次定义 (Reaching Definition)
                return candidateUnit
            }
        }
    }

    // 4. 遍历完前面所有指令都没找到定义
    return null
}