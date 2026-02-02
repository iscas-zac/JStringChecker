package extension

import soot.Local
import soot.Unit

class FastTraceLocalDefs(trace: List<Unit>) {
    // Map<Unit, Map<Local, Unit>>
    // 记录每条指令处，每个 Local 对应的定义指令
    private val definitionsAtUnit = HashMap<Unit, Map<Local, Unit>>()

    init {
        val currentDefs = HashMap<Local, Unit>()

        for (unit in trace) {
            // 1. 记录进入当前指令时的定义状态 (Snapshot)
            // 注意：这里拷贝一份 Map，或者使用持久化数据结构
            definitionsAtUnit[unit] = HashMap(currentDefs)

            // 2. 更新定义状态
            for (defBox in unit.defBoxes) {
                val value = defBox.value
                if (value is Local) {
                    currentDefs[value] = unit
                }
            }
        }
    }

    fun getDefsOfAt(local: Local, unit: soot.Unit): List<soot.Unit> {
        val defMap = definitionsAtUnit[unit] ?: return emptyList()
        val defUnit = defMap[local] ?: return emptyList()
        return listOf(defUnit)
    }
}