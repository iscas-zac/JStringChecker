package cn.ios.vs.smt.solver.justin;
import soot.*;
import soot.jimple.*;

import java.util.*;

public class ValueResolver {

    private final LocalDef localDefs;
    private final Random random = new Random();

    public ValueResolver(List<Unit> path) {
        // SimpleLocalDefs 用于查找 Local 变量在某处的定义语句
        this.localDefs = new LocalDef(path);
    }

    /**
     * 解析 Value 的具体值
     * @param v 要解析的 Value (可能是 Local, Constant, Expr 等)
     * @param context 使用该 Value 的语句 (用于确定上下文)
     * @return 解析出的 String 或 Integer 对象
     */
    public Object resolve(Value v, Unit context) {
        // 1. 如果直接是常量
        if (v instanceof StringConstant) return ((StringConstant) v).value;
        if (v instanceof IntConstant) return ((IntConstant) v).value;
        
        // 2. 如果是局部变量，回溯其定义
        if (v instanceof Local) {
            List<Unit> defs = localDefs.getDefsOfAt((Local) v, context);
            // 为简化演示，我们只取第一个定义点 (在这个路径上的定义)
            if (!defs.isEmpty()) {
                Unit defUnit = defs.get(0);
                if (defUnit instanceof AssignStmt) {
                    Value rhs = ((AssignStmt) defUnit).getRightOp();
                    return resolve(rhs, defUnit); // 递归解析 RHS
                }
            }
        }
        
        // 3. 处理转换 (Cast)
        if (v instanceof CastExpr) {
            return resolve(((CastExpr) v).getOp(), context);
        }

        // 4. 无法解析或无依赖的任意值 (Fallback)
        // 根据类型生成默认值
        if (v.getType().toString().equals("int") || v.getType().toString().equals("short") || v.getType().toString().equals("byte")) {
            return generateRandomInt();
        } else {
            return generateRandomString();
        }
    }

	private int generateRandomInt() {
        // 生成一个常用的正整数，避免边界问题导致正则过于奇怪
        return random.nextInt(10) + 1; 
    }

    private String generateRandomString() {
        return "random_" + Integer.toHexString(random.nextInt());
    }
}