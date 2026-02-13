package cn.ios.vs.smt.solver.justin;
import soot.*;
import soot.jimple.*;
import java.util.*;

import cn.ios.vs.smt.solver.StringPathExtractor;

public class PathSAISBuilder {

    //常量定义保持不变: COND_TRUE, COND_EQ 
    public static final int COND_EQ = 1;
    public static final int COND_NE = 2;
    public static final int COND_GT = 3;
    public static final int COND_GE = 4;
    public static final int COND_LT = 5;
    public static final int COND_LE = 6;

    public static class SAIS {
        public List<Stmt> trajectory;
        public int conditionType;
        public Object conditionValue;
        // 记录导致该 Stmt 产生的数组访问下标 (例如 split 语句 -> index 2)
        public Map<Stmt, Integer> arrayIndexMap; 
        
        public SAIS(List<Stmt> t, int type, Object val, Map<Stmt, Integer> idxMap) {
            this.trajectory = t;
            this.conditionType = type;
            this.conditionValue = val;
            this.arrayIndexMap = idxMap;
        }
    }

    public SAIS extractSAISFromPath(List<Unit> unitList) {
//        BriefUnitGraph graph = new BriefUnitGraph(linearizedBody);
        LocalDef localDefs = new LocalDef(unitList);
//        List<Unit> unitList = new ArrayList<>(linearizedBody.getUnits());
        Map<Stmt, Integer> indexMap = new HashMap<>();
        
        for (int i = 0; i < unitList.size(); i++) {
            Unit u = unitList.get(i);
            if (u instanceof IfStmt) {
                IfStmt ifStmt = (IfStmt) u;
                
             // 1. 判断 Jimple 层面的分支走向
                // 如果是单条路径 Body，下一条指令要么是 Target (Jump)，要么是 Fall-through
                boolean isJimpleBranchTaken = false;
                
                // 检查下一条指令是否存在
                if (i + 1 < unitList.size()) {
                    Unit actualNext = unitList.get(i + 1);
                    // 如果实际走的下一条 == goto 的目标，说明 Jimple 条件成立
                    isJimpleBranchTaken = (actualNext.toString().equals(ifStmt.getTarget().toString()));
                } else {
                    // 路径结束，理论上不可能发生（IfStmt 后必有指令），除非是异常结束
                    break; 
                }
                
//                boolean isTrueBranch = (actualNext.toString().equals(ifStmt.getTarget().toString()));
                Value ifCondition = ifStmt.getCondition();
                // boolean b = ...; if(b) ==> 
                // Jimple: if(b == 0)
//                if(ifCondition instanceof EqExpr) {
//                	EqExpr eqExpr = (EqExpr) ifCondition;
//                	if(eqExpr.getOp1().getType() instanceof BooleanType && eqExpr.getOp2().toString().equals("0")) {
//                		isTrueBranch = !isTrueBranch;
//                	}else if(eqExpr.getOp2().getType() instanceof BooleanType && eqExpr.getOp1().toString().equals("0")) {
//                		isTrueBranch = !isTrueBranch;
//                	}
//                }
//                System.out.println("isTrueBranch: " + isJimpleBranchTaken);
             // 2. 归一化条件 (专门处理 b == 0)
                ConditionInfo condInfo = normalizeBooleanCondition(ifCondition, isJimpleBranchTaken);
                // 3. 执行切片
                if (condInfo.variable instanceof Local) {
                    List<Stmt> trajectory = sliceBackwards((Local) condInfo.variable, ifStmt, localDefs, indexMap);
                    
                    if (!trajectory.isEmpty()) {
                        // 调试日志
                        // System.out.println("Branch Logic: " + (isJimpleBranchTaken ? "Taken" : "Fall-through"));
                        // System.out.println("Condition: " + condInfo.type + " vs " + condInfo.value);
                        return new SAIS(trajectory, condInfo.type, condInfo.value, indexMap);
                    }
                }
            }
        }
        return null;
    }

    /**
     * 【核心修正】: 专门处理 boolean 类型的 if(b) -> if(b == 0) 逻辑
     * * 逻辑推导表：
     * Jimple Expr | Branch Taken? | 含义 | b 的实际值 | 输出 Type/Val
     * ------------|--------------|-----|------------|---------------
     * if (b == 0) | Yes (Taken)  | b是0 | 0 (False)  | EQ, 0
     * if (b == 0) | No (Fall)    | b非0 | 1 (True)   | EQ, 1
     * * (虽然用户指出主要是 == 0，但为了稳健性，也涵盖 != 0 的情况，反之即可)
     */
    private ConditionInfo normalizeBooleanCondition(Value condition, boolean isJimpleBranchTaken) {
        ConditionInfo info = new ConditionInfo();

        if (condition instanceof BinopExpr) {
            BinopExpr binop = (BinopExpr) condition;
            Value op1 = binop.getOp1();
            Value op2 = binop.getOp2();

            // 1. 识别变量和常量 (通常 Jimple 中常量在右侧，但也可能反过来)
            Value variable = null;
            int constantVal = -1;

            if (op1 instanceof Local && op2 instanceof IntConstant) {
                variable = op1;
                constantVal = ((IntConstant) op2).value;
            } else if (op2 instanceof Local && op1 instanceof IntConstant) {
                variable = op2;
                constantVal = ((IntConstant) op1).value;
            } else {
                // 如果不是 变量 vs 常量，可能是两个变量比较，按常规处理
                return normalizeGeneralCondition(condition, isJimpleBranchTaken);
            }
            
            info.variable = variable;

            // 2. 专门处理 boolean 模式: if (b == 0) 或 if (b != 0)
            // 只要常量是 0 或 1，我们都将其视为 boolean 逻辑处理
            if (constantVal == 0 || constantVal == 1) {
                
                // 判断 Jimple 表达式本身是否为“真”
                // EqExpr (==): 如果 Taken，则相等；如果 Fall，则不等。
                // NeExpr (!=): 如果 Taken，则不等；如果 Fall，则相等。
                boolean isConditionTrue = isJimpleBranchTaken; 
                
                // 计算 b 的最终值
                // 假设表达式是 b == 0
                if (condition instanceof EqExpr) {
                    if (isConditionTrue) {
                        // (b == 0) is True => b is 0
                        info.type = COND_EQ;
//                        info.value = constantVal; 
                    } else {
                        // (b == 0) is False => b != 0
                        // 如果比较对象是 0，不等于 0 意味着 1 (True)
                        info.type = COND_NE;
//                        info.value = (constantVal == 0) ? 1 : 0;
                    }
                } 
                else if (condition instanceof NeExpr) {
                    if (isConditionTrue) {
                        // (b != 0) is True => b != 0
                        info.type = COND_EQ;
                        info.value = (constantVal == 0) ? 1 : 0;
                    } else {
                        // (b != 0) is False => b == 0
                        info.type = COND_EQ;
                        info.value = constantVal;
                    }
                }
                else {
                    // 如果是 > 0 等其他操作符，回退到通用处理
                    return normalizeGeneralCondition(condition, isJimpleBranchTaken);
                }
                
                return info;
            }
        }
        
        // 非 Boolean 模式 (如 indexOf > 5)，使用通用逻辑
        return normalizeGeneralCondition(condition, isJimpleBranchTaken);
    }

    /**
     * 通用条件处理 (用于处理 indexOf > 5, length == 10 等非 boolean 检查)
     */
    private ConditionInfo normalizeGeneralCondition(Value condition, boolean isJimpleBranchTaken) {
        ConditionInfo info = new ConditionInfo();
        if (condition instanceof BinopExpr) {
            BinopExpr binop = (BinopExpr) condition;
            info.variable = binop.getOp1();
            Value constVal = binop.getOp2();
            
            if (constVal instanceof IntConstant) info.value = ((IntConstant) constVal).value;
            else if (constVal instanceof StringConstant) info.value = ((StringConstant) constVal).value;
            else info.value = 0;

            int rawOp = -1;
            if (condition instanceof EqExpr) rawOp = COND_EQ;
            else if (condition instanceof NeExpr) rawOp = COND_NE;
            else if (condition instanceof GtExpr) rawOp = COND_GT;
            else if (condition instanceof GeExpr) rawOp = COND_GE;
            else if (condition instanceof LtExpr) rawOp = COND_LT;
            else if (condition instanceof LeExpr) rawOp = COND_LE;

            // 如果分支没跳转，取反操作符
            info.type = isJimpleBranchTaken ? rawOp : invertOp(rawOp);
        }
        return info;
    }

    private List<Stmt> sliceBackwards(Local startVar, Unit contextIfStmt, LocalDef localDefs, Map<Stmt, Integer> indexMap) {
        List<Stmt> trace = new ArrayList<>();
        Set<Unit> visited = new HashSet<>();
        Queue<VarContext> queue = new LinkedList<>();
        queue.add(new VarContext(startVar, contextIfStmt, null));

        while (!queue.isEmpty()) {
            VarContext vc = queue.poll();
            List<Unit> defs = localDefs.getDefsOfAt(vc.var, vc.location);

            for (Unit defUnit : defs) {
                if (visited.contains(defUnit)) continue;
                visited.add(defUnit);
                Stmt defStmt = (Stmt) defUnit;

                if (defStmt.containsInvokeExpr()) {
                    InvokeExpr invoke = defStmt.getInvokeExpr();
                    if (StringPathExtractor.containsStringAPIInUnit(defUnit)) {
                        trace.add(defStmt);
                        if (vc.indexConstraint != null) {
                            indexMap.put(defStmt, vc.indexConstraint);
                        }
                        if (invoke instanceof InstanceInvokeExpr) {
                            Value base = ((InstanceInvokeExpr) invoke).getBase();
                            if (base instanceof Local) {
                                queue.add(new VarContext((Local) base, defStmt, null));
                            }
                        }
                    }
                } else if (defStmt instanceof AssignStmt) {
                    Value rhs = ((AssignStmt) defStmt).getRightOp();
                    if (rhs instanceof ArrayRef) {
                        ArrayRef arrayRef = (ArrayRef) rhs;
                        Value baseArray = arrayRef.getBase();
                        Value indexVal = arrayRef.getIndex();
                        Integer constIndex = (indexVal instanceof IntConstant) ? ((IntConstant) indexVal).value : null;
                        
                        if (baseArray instanceof Local) {
                            queue.add(new VarContext((Local) baseArray, defStmt, constIndex));
                        }
                    } else if (rhs instanceof Local) {
                        queue.add(new VarContext((Local) rhs, defStmt, vc.indexConstraint));
                    } else if (rhs instanceof CastExpr) {
                        Value op = ((CastExpr) rhs).getOp();
                        if (op instanceof Local) {
                            queue.add(new VarContext((Local) op, defStmt, vc.indexConstraint));
                        }
                    }
                }
            }
        }
        Collections.reverse(trace);
        return trace;
    }

    private int invertOp(int op) {
        switch (op) {
            case COND_EQ: return COND_NE;
            case COND_NE: return COND_EQ;
            case COND_GT: return COND_LE;
            case COND_GE: return COND_LT;
            case COND_LT: return COND_GE;
            case COND_LE: return COND_GT;
            default: return op;
        }
    }

    private static class VarContext {
        Local var;
        Unit location;
        Integer indexConstraint;
        VarContext(Local v, Unit l, Integer idx) { var = v; location = l; indexConstraint = idx; }
    }

    private static class ConditionInfo {
        int type;
        Object value;
        Value variable;
    }
}