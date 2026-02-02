package extension

import UselessTag
import soot.Body
import soot.Local
import soot.Unit
import soot.Value
import soot.jimple.Constant
import soot.jimple.Expr
import soot.toolkits.graph.UnitGraph

class SimpleUnitGraph(body: Body, public val units: List<Unit>) : UnitGraph(body) {
    init {
        fun cloneValuePreservingLocals(originalValue: Value): Value {
            return when (originalValue) {
                is Local, is Constant -> {
                    originalValue
                }
                is Expr -> {
                    // 创建Expr的浅克隆
                    val clonedExpr = originalValue.clone() as Expr

                    // 递归修复Expr的所有use boxes（操作数）
                    clonedExpr.useBoxes.forEach { operandBox ->
                        val currentOperand = operandBox.value
                        val fixedOperand = cloneValuePreservingLocals(currentOperand)

                        // 如果修复后的操作数与当前不同，则更新
                        if (fixedOperand !== currentOperand) {
                            operandBox.value = fixedOperand
                        }
                    }

                    clonedExpr
                }
                else -> {
                    // 其他Value类型（如ArrayRef、FieldRef等）：默认克隆
                    originalValue.clone() as Value
                }
            }
        }

        unitChain.removeAll(unitChain)

        println(units.size)
        val clonedUnits = units.mapIndexed { index, unit ->
            (unit.clone() as Unit).apply {
                addTag(UselessTag(index))
            }
        }

//        // 遍历所有单元，收集并克隆被引用的值
//        units.zip(clonedUnits).forEach { (originalUnit, clonedUnit) ->
//            // 获取所有use/def boxes
//            val originalBoxes = originalUnit.useAndDefBoxes
//            val clonedBoxes = clonedUnit.useAndDefBoxes
//
//            // 确保box数量一致
//            check(originalBoxes.size == clonedBoxes.size) {
//                "Box数量不匹配: ${originalUnit.javaClass} vs ${clonedUnit.javaClass}"
//            }
//
//            originalBoxes.zip(clonedBoxes).forEach { (originalBox, clonedBox) ->
//                // 使用自定义逻辑克隆Value，确保Expr中的Local指向原始对象
//                val fixedValue = cloneValuePreservingLocals(originalBox.value)
//                clonedBox.value = fixedValue
//            }
//        }
        unitChain.addAll(clonedUnits)
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