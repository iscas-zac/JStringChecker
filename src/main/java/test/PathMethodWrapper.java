package test;

import soot.*;
import soot.options.Options;
import soot.toolkits.graph.*;
import soot.jimple.*;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.jimple.toolkits.annotation.logic.LoopFinder;
import soot.util.*;
import java.util.*;

public class PathMethodWrapper {
    
    // 包装路径为新方法
    public static SootMethod wrapPathAsNewMethod(PathInfo pathInfo, SootMethod originalMethod, int pathIndex) {
        // 创建新方法名
        String newMethodName = originalMethod.getName() + "_path_" + pathIndex;
        
        // 获取原始方法的返回类型
        Type returnType = originalMethod.getReturnType();
        
        // 复制原始方法的参数列表
        List<Type> paramTypes = new ArrayList<>();
        for (int i = 0; i < originalMethod.getParameterCount(); i++) {
            paramTypes.add(originalMethod.getParameterType(i));
        }
        
        // 创建新方法签名
        SootMethodRef methodRef = Scene.v().makeMethodRef(
            originalMethod.getDeclaringClass(),
            newMethodName,
            paramTypes,
            returnType,
            originalMethod.isStatic()
        );
        
        // 创建新方法
        SootMethod newMethod = new SootMethod(
            newMethodName,
            paramTypes,
            returnType,
            originalMethod.getModifiers()
        );
        
        // 将新方法添加到类中
        originalMethod.getDeclaringClass().addMethod(newMethod);
        
        // 创建方法体
        JimpleBody newBody = Jimple.v().newBody();
        newMethod.setActiveBody(newBody);
        
        // 添加参数局部变量
        List<Local> paramLocals = setupParameters(newMethod, newBody, originalMethod);
        System.out.println(newBody);
        
        // 添加路径中的语句到新方法
        List<Unit> path = pathInfo.path;
        Map<Unit, Unit> unitMapping = new HashMap<>(); // 原始语句到新语句的映射
        
        // 首先处理所有赋值语句和变量定义
        Map<Local, Local> localMapping = new HashMap<>();
        
        // 1. 收集所有使用的局部变量
        Set<Local> usedLocals = collectUsedLocals(path, paramLocals);
        
        // 2. 为每个局部变量创建新版本
        for (Local originalLocal : usedLocals) {
            // 如果是参数，使用参数局部变量
            if (paramLocals.contains(originalLocal)) {
                localMapping.put(originalLocal, originalLocal);
            } else {
                // 创建新的局部变量
                Local newLocal = Jimple.v().newLocal(
                    originalLocal.getName() + "_path" + pathIndex,
                    originalLocal.getType()
                );
                newBody.getLocals().add(newLocal);
                localMapping.put(originalLocal, newLocal);
                
                // 如果是路径中的第一个定义点，可能需要初始化
                if (isDefinedInPath(originalLocal, path)) {
                    // 如果定义了但不是在路径开头，在方法开始时初始化为默认值
                    addDefaultInitialization(newLocal, newBody);
                }
            }
        }
        
        // 3. 复制路径中的语句
        for (Unit originalUnit : path) {
            Unit newUnit = copyUnit(originalUnit, localMapping, paramLocals);
            if (newUnit != null) {
                if (newUnit instanceof IdentityStmt) {
                    System.out.println(newUnit);
//                    newBody.
                } else {
                    newBody.getUnits().add(newUnit);
                }
                unitMapping.put(originalUnit, newUnit);

            }
        }
        
        // 4. 添加返回语句（如果路径最后不是返回）
        addReturnStatement(newBody, returnType, path, unitMapping);
        
        // 5. 构建基本块和控制流
        rebuildControlFlow(newBody, path, unitMapping);
        
        // 6. 验证方法体
        newBody.validate();
        
        return newMethod;
    }
    
    // 设置参数
    private static List<Local> setupParameters(SootMethod newMethod, JimpleBody body, SootMethod originalMethod) {
        List<Local> paramLocals = new ArrayList<>();
        
        // 添加this参数（如果是实例方法）
        if (!newMethod.isStatic() && !originalMethod.getDeclaringClass().isInterface()) {
            Local thisLocal = Jimple.v().newLocal("this", 
                RefType.v(originalMethod.getDeclaringClass().getName()));
            body.getLocals().add(thisLocal);
            body.getUnits().add(Jimple.v().newIdentityStmt(
                thisLocal,
                Jimple.v().newThisRef(RefType.v(originalMethod.getDeclaringClass().getName()))
            ));
        }
        
        // 添加方法参数
        for (int i = 0; i < originalMethod.getParameterCount(); i++) {
            Type paramType = originalMethod.getParameterType(i);
            String paramName = "param" + i;
            
            Local paramLocal = Jimple.v().newLocal(paramName, paramType);
            body.getLocals().add(paramLocal);
            paramLocals.add(paramLocal);
            
            // 添加参数赋值语句
            body.getUnits().add(Jimple.v().newIdentityStmt(
                paramLocal,
                Jimple.v().newParameterRef(paramType, i)
            ));
        }
        
        return paramLocals;
    }
    
    // 收集路径中使用的所有局部变量
    private static Set<Local> collectUsedLocals(List<Unit> path, List<Local> paramLocals) {
        Set<Local> usedLocals = new HashSet<>();
        
        for (Unit unit : path) {
            // 收集使用的变量
            for (ValueBox useBox : unit.getUseBoxes()) {
                Value value = useBox.getValue();
                if (value instanceof Local) {
                    usedLocals.add((Local) value);
                }
            }
            
            // 收集定义的变量
            for (ValueBox defBox : unit.getDefBoxes()) {
                Value value = defBox.getValue();
                if (value instanceof Local) {
                    usedLocals.add((Local) value);
                }
            }
        }
        
        // 添加参数
        usedLocals.addAll(paramLocals);
        
        return usedLocals;
    }
    
    // 检查局部变量是否在路径中被定义
    private static boolean isDefinedInPath(Local local, List<Unit> path) {
        for (Unit unit : path) {
            for (ValueBox defBox : unit.getDefBoxes()) {
                if (defBox.getValue().equals(local)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    // 添加默认初始化
    private static void addDefaultInitialization(Local local, JimpleBody body) {
        Type type = local.getType();
        
        if (type instanceof PrimType) {
            PrimType primType = (PrimType) type;
            Value defaultValue;
            
            if (primType instanceof BooleanType) {
                defaultValue = IntConstant.v(0);
            } else if (primType instanceof ByteType) {
                defaultValue = IntConstant.v(0);
            } else if (primType instanceof CharType) {
                defaultValue = IntConstant.v(0);
            } else if (primType instanceof DoubleType) {
                defaultValue = DoubleConstant.v(0);
            } else if (primType instanceof FloatType) {
                defaultValue = FloatConstant.v(0);
            } else if (primType instanceof IntType) {
                defaultValue = IntConstant.v(0);
            } else if (primType instanceof LongType) {
                defaultValue = LongConstant.v(0);
            } else if (primType instanceof ShortType) {
                defaultValue = IntConstant.v(0);
            } else {
                defaultValue = NullConstant.v();
            }
            
            body.getUnits().add(Jimple.v().newAssignStmt(local, defaultValue));
        } else if (type instanceof RefType || type instanceof ArrayType) {
            body.getUnits().add(Jimple.v().newAssignStmt(local, NullConstant.v()));
        }
    }
    
    // 复制语句
    private static Unit copyUnit(Unit originalUnit, Map<Local, Local> localMapping, List<Local> paramLocals) {
        if (!(originalUnit instanceof Stmt)) {
            return null;
        }
        
        Stmt originalStmt = (Stmt) originalUnit;
        
        try {
            // 深度复制语句
            Stmt newStmt = (Stmt) originalStmt.clone();
            
            // 替换局部变量引用
            replaceLocalsInStmt(newStmt, localMapping, paramLocals);
            
            return newStmt;
        } catch (Exception e) {
            // 如果克隆失败，手动创建新语句
            return recreateStmt(originalStmt, localMapping, paramLocals);
        }
    }
    
    // 替换语句中的局部变量引用
    private static void replaceLocalsInStmt(Stmt stmt, Map<Local, Local> localMapping, List<Local> paramLocals) {
        List<ValueBox> useBoxes = new ArrayList<>(stmt.getUseBoxes());
        List<ValueBox> defBoxes = new ArrayList<>(stmt.getDefBoxes());
        
        // 替换使用的变量
        for (ValueBox useBox : useBoxes) {
            Value value = useBox.getValue();
            if (value instanceof Local) {
                Local originalLocal = (Local) value;
                Local newLocal = localMapping.get(originalLocal);
                if (newLocal != null) {
                    useBox.setValue(newLocal);
                }
            }
        }
        
        // 替换定义的变量
        for (ValueBox defBox : defBoxes) {
            Value value = defBox.getValue();
            if (value instanceof Local) {
                Local originalLocal = (Local) value;
                Local newLocal = localMapping.get(originalLocal);
                if (newLocal != null) {
                    defBox.setValue(newLocal);
                }
            }
        }
    }
    
    // 重新创建语句（当克隆失败时使用）
    private static Stmt recreateStmt(Stmt originalStmt, Map<Local, Local> localMapping, List<Local> paramLocals) {
        // 这里实现具体的语句创建逻辑
        // 由于比较复杂，这里只提供框架
        
        if (originalStmt instanceof AssignStmt) {
            return recreateAssignStmt((AssignStmt) originalStmt, localMapping);
        } else if (originalStmt instanceof InvokeStmt) {
            return recreateInvokeStmt((InvokeStmt) originalStmt, localMapping);
        } else if (originalStmt instanceof ReturnStmt) {
            return recreateReturnStmt((ReturnStmt) originalStmt, localMapping);
        } else if (originalStmt instanceof IfStmt) {
            return recreateIfStmt((IfStmt) originalStmt, localMapping);
        } else if (originalStmt instanceof GotoStmt) {
            return Jimple.v().newGotoStmt((Unit) null); // 目标稍后设置
        }
        
        return originalStmt;
    }
    
    private static AssignStmt recreateAssignStmt(AssignStmt stmt, Map<Local, Local> localMapping) {
        Value leftOp = replaceLocalInValue(stmt.getLeftOp(), localMapping);
        Value rightOp = replaceLocalInValue(stmt.getRightOp(), localMapping);
        return Jimple.v().newAssignStmt(leftOp, rightOp);
    }
    
    private static InvokeStmt recreateInvokeStmt(InvokeStmt stmt, Map<Local, Local> localMapping) {
        InvokeExpr invokeExpr = (InvokeExpr) replaceLocalInValue(stmt.getInvokeExpr(), localMapping);
        return Jimple.v().newInvokeStmt(invokeExpr);
    }
    
    private static ReturnStmt recreateReturnStmt(ReturnStmt stmt, Map<Local, Local> localMapping) {
        if (stmt.getOp() != null) {
            Value returnValue = replaceLocalInValue(stmt.getOp(), localMapping);
            return Jimple.v().newReturnStmt(returnValue);
        } else {
            return (ReturnStmt) Jimple.v().newReturnVoidStmt();
        }
    }
    
    private static IfStmt recreateIfStmt(IfStmt stmt, Map<Local, Local> localMapping) {
        ConditionExpr cond = (ConditionExpr) replaceLocalInValue(stmt.getCondition(), localMapping);
        return Jimple.v().newIfStmt(cond, (Unit) null); // 目标稍后设置
    }
    
    private static Value replaceLocalInValue(Value value, Map<Local, Local> localMapping) {
        if (value instanceof Local) {
            Local newLocal = localMapping.get(value);
            return newLocal != null ? newLocal : value;
        } else if (value instanceof BinopExpr) {
            BinopExpr binop = (BinopExpr) value;
            Value op1 = replaceLocalInValue(binop.getOp1(), localMapping);
            Value op2 = replaceLocalInValue(binop.getOp2(), localMapping);
            
            // 创建新的BinopExpr
            if (value instanceof AddExpr) {
                return Jimple.v().newAddExpr(op1, op2);
            } else if (value instanceof SubExpr) {
                return Jimple.v().newSubExpr(op1, op2);
            } else if (value instanceof MulExpr) {
                return Jimple.v().newMulExpr(op1, op2);
            } else if (value instanceof DivExpr) {
                return Jimple.v().newDivExpr(op1, op2);
            } else if (value instanceof AndExpr) {
                return Jimple.v().newAndExpr(op1, op2);
            } else if (value instanceof OrExpr) {
                return Jimple.v().newOrExpr(op1, op2);
            }
        } else if (value instanceof InvokeExpr) {
            InvokeExpr invoke = (InvokeExpr) value;
            List<Value> newArgs = new ArrayList<>();
            for (Value arg : invoke.getArgs()) {
                newArgs.add(replaceLocalInValue(arg, localMapping));
            }
            
            if (value instanceof StaticInvokeExpr) {
                return Jimple.v().newStaticInvokeExpr(
                    invoke.getMethodRef(),
                    newArgs
                );
            } else if (value instanceof VirtualInvokeExpr) {
                VirtualInvokeExpr virtInvoke = (VirtualInvokeExpr) value;
                Value base = replaceLocalInValue(virtInvoke.getBase(), localMapping);
                return Jimple.v().newVirtualInvokeExpr(
                        (Local) base,
                    invoke.getMethodRef(),
                    newArgs
                );
            }
        }
        
        return value;
    }
    
    // 添加返回语句
    private static void addReturnStatement(JimpleBody body, Type returnType, 
                                          List<Unit> path, Map<Unit, Unit> unitMapping) {
        Unit lastUnit = path.get(path.size() - 1);
        
        // 检查最后一条语句是否是返回语句
        if (!(lastUnit instanceof ReturnStmt || lastUnit instanceof ReturnVoidStmt)) {
            if (returnType instanceof VoidType) {
                body.getUnits().add(Jimple.v().newReturnVoidStmt());
            } else {
                // 根据返回类型返回默认值
                Value defaultValue = getDefaultValueForType(returnType);
                body.getUnits().add(Jimple.v().newReturnStmt(defaultValue));
            }
        }
    }
    
    // 获取类型的默认值
    private static Value getDefaultValueForType(Type type) {
        if (type instanceof PrimType) {
            if (type instanceof BooleanType) {
                return IntConstant.v(0);
            } else if (type instanceof ByteType) {
                return IntConstant.v(0);
            } else if (type instanceof CharType) {
                return IntConstant.v(0);
            } else if (type instanceof DoubleType) {
                return DoubleConstant.v(0);
            } else if (type instanceof FloatType) {
                return FloatConstant.v(0);
            } else if (type instanceof IntType) {
                return IntConstant.v(0);
            } else if (type instanceof LongType) {
                return LongConstant.v(0);
            } else if (type instanceof ShortType) {
                return IntConstant.v(0);
            }
        }
        return NullConstant.v();
    }
    
    // 重建控制流
    private static void rebuildControlFlow(JimpleBody body, List<Unit> path, Map<Unit, Unit> unitMapping) {
        Chain<Unit> units = body.getUnits();
        
        // 为跳转语句设置正确的目标
        for (Unit unit : units) {
            if (unit instanceof GotoStmt) {
                GotoStmt gotoStmt = (GotoStmt) unit;
                // 查找原始目标对应的新语句
                for (Map.Entry<Unit, Unit> entry : unitMapping.entrySet()) {
                    Unit originalTarget = entry.getKey();
                    if (originalTarget == gotoStmt.getTarget()) {
                        gotoStmt.setTarget(entry.getValue());
                        break;
                    }
                }
            } else if (unit instanceof IfStmt) {
                IfStmt ifStmt = (IfStmt) unit;
                // 查找原始目标对应的新语句
                for (Map.Entry<Unit, Unit> entry : unitMapping.entrySet()) {
                    Unit originalTarget = entry.getKey();
                    if (originalTarget == ifStmt.getTarget()) {
                        ifStmt.setTarget(entry.getValue());
                        break;
                    }
                }
            }
        }
        
        // 设置基本块
        setupBasicBlocks(body);
    }
    
    // 设置基本块
    private static void setupBasicBlocks(JimpleBody body) {
        // 创建一个简单的线性基本块
        // 在实际实现中，可能需要更复杂的控制流分析
        List<Unit> units = new ArrayList<>(body.getUnits());
        
        // 清除现有的陷阱（trap）
        body.getTraps().clear();
        
        // 设置异常处理（如果有的话）
        // 这里简化处理，实际需要根据路径中的异常处理信息来设置
    }
    
    // 主方法：处理所有符合条件的路径
    public static void processAndWrapPaths(SootMethod method, int n) {
        // 提取路径
        List<PathInfo> filteredPaths = EnhancedCFGPathExtractor.extractPathsWithFilter(method, n);
        
        System.out.println("Found " + filteredPaths.size() + " valid paths to wrap");
        
        // 包装每个符合条件的路径为新方法
        for (int i = 0; i < filteredPaths.size(); i++) {
            PathInfo pathInfo = filteredPaths.get(i);
            
            if (pathInfo.hasStringAPI && pathInfo.usesStringParams) {
                System.out.println("\nWrapping path " + i + " as new method...");
                
                try {
                    SootMethod newMethod = wrapPathAsNewMethod(pathInfo, method, i);
                    
                    // 验证新方法
                    if (newMethod.getActiveBody() != null) {
                        newMethod.getActiveBody().validate();
                        System.out.println("Successfully created method: " + newMethod.getSignature());
                        
                        // 打印新方法体
                        System.out.println("Method body:");
                        for (Unit unit : newMethod.getActiveBody().getUnits()) {
                            System.out.println("  " + unit);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to wrap path " + i + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
    
    // 主函数示例
    public static void main(String[] args) {
        // Soot 初始化
        String classPath = ".;/path/to/your/classes";
        Options.v().set_soot_classpath(classPath);
        
        // 加载类
        SootClass sc = Scene.v().loadClassAndSupport("ExampleClass");
        sc.setApplicationClass();
        
        // 获取目标方法
        SootMethod method = sc.getMethodByName("exampleMethod");
        
        // 处理并包装路径
        processAndWrapPaths(method, 2);
    }
}

