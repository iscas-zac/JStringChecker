package cn.ios.vs.smt.solver;

import java.util.ArrayList;
import java.util.List;

import soot.Local;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.Stmt;

public class MethodConstruct {
	
	static int loopCount = 1;
	
	/**
	 * loop1{
	 * 
	 *   loop2
	 * }
	 * 
	 * loop3
	 */
	
	/**
     * 对给定的 Unit 路径进行循环展开
     * * @param path 原路径 List
     * @param loopCount 展开次数 (例如 2 表示每个循环体在路径中重复出现 2 次)
     * @return 展开后的路径 List
     */
    public static List<Unit> unrollPath(List<Unit> path, int loopCount, LoopAnalysis loopAnalysis) {
        if (path == null || path.isEmpty() || loopCount <= 1) {
            return new ArrayList<>(path);
        }

        List<Unit> unrolledPath = new ArrayList<>();
        int i = 0;
        int n = path.size();

        while (i < n) {
            Unit currentUnit = path.get(i);
            Unit header = loopAnalysis.getLoopHeader(path.get(i));

            if (header == null) {
                // 1. 如果当前语句不属于任何循环，直接添加
                unrolledPath.add(currentUnit);
                i++;
            } else {
                // 2. 识别连续的循环体语句块
                // 我们认为在路径中，属于同一个 Loop Header 的连续 Unit 构成了一个待展开的“循环片段”
                List<Unit> loopBodyFragment = new ArrayList<>();
                
                // 贪婪匹配：找出所有属于当前这个 loop header 的连续语句
                while (i < n && header.equals(loopAnalysis.getLoopHeader(path.get(i)))) {
                    loopBodyFragment.add(path.get(i));
                    i++;
                }

                // 3. 根据 loopCount 进行展开
                for (int count = 0; count < loopCount; count++) {
                    // 注意：如果 Unit 对象在后续分析中有状态变化，这里可能需要 clone
                    // 但在 Soot 的路径表示中，通常直接引用原 Unit 即可
                    unrolledPath.addAll(loopBodyFragment);
                }
            }
        }

        return unrolledPath;
    }
	
	public static SootMethod newMethod(List<Unit> path, SootMethod originalMethod, int pathIndex) {
		
		   // 创建新方法名
        String newMethodName = originalMethod.getName() + "_path_" + pathIndex;
        
        // 获取原始方法的返回类型
        Type returnType = originalMethod.getReturnType();
        
        // 复制原始方法的参数列表
        List<Type> paramTypes = new ArrayList<>();
        for (int i = 0; i < originalMethod.getParameterCount(); i++) {
            paramTypes.add(originalMethod.getParameterType(i));
        }
        
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
        JimpleBody newBody = Jimple.v().newBody(newMethod);
        
        newMethod.setActiveBody(newBody);
        
        for (Local local : originalMethod.getActiveBody().getLocals()) {
        	newBody.getLocals().add(local);
        }
        
        // 3. 复制路径中的语句
        for (Unit originalUnit : path) {
        	 // 深度复制语句
            Stmt newStmt = null;
			try {
				newStmt = (Stmt) originalUnit.clone();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            if (newStmt != null) {
                newBody.getUnits().add(newStmt);
            }
        }
        
        for (Local local : originalMethod.getActiveBody().getParameterLocals()) {
        	newBody.getParameterLocals().add(local);
        }
        
        return newMethod;
	}

}
