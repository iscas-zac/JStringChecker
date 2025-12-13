package test;

import soot.*;
import soot.options.Options;
import soot.toolkits.graph.*;
import soot.jimple.*;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.jimple.toolkits.annotation.logic.LoopFinder;
import soot.util.*;
import java.util.*;

public class EnhancedCFGPathExtractor {

    // 主方法：提取符合条件的路径
    public static List<PathInfo> extractPathsWithFilter(SootMethod method, int n) {
        List<PathInfo> filteredPaths = new ArrayList<>();
        
        // 首先检查整个方法是否包含字符串API调用
        if (!containsStringAPI(method)) {
            System.out.println("Method " + method.getName() + " does not contain string API calls. Skipping.");
            return filteredPaths;
        }
        
        Body body = method.retrieveActiveBody();
        UnitGraph cfg = new ExceptionalUnitGraph(body);
        
        // 获取方法参数中的字符串参数
        List<Local> stringParams = getStringParameters(method);
        
        // 获取循环信息
        LoopFinder loopFinder = new LoopFinder();
        Set<Loop> loops = loopFinder.getLoops(cfg);
        Map<Unit, Loop> loopHeaders = new HashMap<>();
        for (Loop loop : loops) {
            loopHeaders.put(loop.getHead(), loop);
        }
        
        // DFS提取所有路径
        List<List<Unit>> allPaths = extractAllPaths(cfg, loopHeaders, n);
        
        // 过滤路径
        for (List<Unit> path : allPaths) {
            // 检查路径是否包含字符串API调用
            boolean hasStringAPI = containsStringAPIInPath(path);
            
            // 检查路径是否包含字符串参数的使用
            boolean usesStringParams = usesStringParameters(path, stringParams);
            
            // 同时满足两个条件才保留
            if (hasStringAPI && usesStringParams) {
                PathInfo pathInfo = new PathInfo();
                pathInfo.path = path;
                pathInfo.hasStringAPI = true;
                pathInfo.usesStringParams = true;
                filteredPaths.add(pathInfo);
            }
        }
        
        return filteredPaths;
    }
    
    // 提取所有路径（不考虑过滤）
    private static List<List<Unit>> extractAllPaths(UnitGraph cfg, Map<Unit, Loop> loopHeaders, int n) {
        List<List<Unit>> allPaths = new ArrayList<>();
        Stack<PathState> stack = new Stack<>();
        
        PathState initState = new PathState();
        initState.path.add(cfg.getHeads().get(0));
        stack.push(initState);
        
        while (!stack.isEmpty()) {
            PathState current = stack.pop();
            Unit lastUnit = current.path.get(current.path.size() - 1);
            
            // 到达出口
            if (cfg.getSuccsOf(lastUnit).isEmpty()) {
                allPaths.add(new ArrayList<>(current.path));
                continue;
            }
            
            // 处理后继
            for (Unit succ : cfg.getSuccsOf(lastUnit)) {
                if (loopHeaders.containsKey(succ)) {
                    Loop loop = loopHeaders.get(succ);
                    
                    if (current.loopCounts.getOrDefault(loop, 0) < n) {
                        PathState newState = current.copy();
                        newState.path.add(succ);
                        newState.loopCounts.put(loop, 
                            newState.loopCounts.getOrDefault(loop, 0) + 1);
                        stack.push(newState);
                    } else {
                        // 跳过循环，找循环退出边
                        List<Unit> exits = findLoopExits(lastUnit, loop, cfg);
                        for (Unit exit : exits) {
                            PathState newState = current.copy();
                            newState.path.add(exit);
                            stack.push(newState);
                        }
                    }
                } else {
                    PathState newState = current.copy();
                    newState.path.add(succ);
                    stack.push(newState);
                }
            }
        }
        
        return allPaths;
    }
    
    // 检查方法是否包含字符串API调用
    public static boolean containsStringAPI(SootMethod method) {
        Body body = method.retrieveActiveBody();
        
        for (Unit unit : body.getUnits()) {
            if (unit instanceof Stmt) {
                Stmt stmt = (Stmt) unit;
                if (stmt.containsInvokeExpr()) {
                    InvokeExpr invoke = stmt.getInvokeExpr();
                    SootMethod calledMethod = invoke.getMethod();
                    String className = calledMethod.getDeclaringClass().getName();
                    
                    if (isStringClass(className)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    // 检查路径是否包含字符串API调用
    public static boolean containsStringAPIInPath(List<Unit> path) {
        for (Unit unit : path) {
            if (unit instanceof Stmt) {
                Stmt stmt = (Stmt) unit;
                if (stmt.containsInvokeExpr()) {
                    InvokeExpr invoke = stmt.getInvokeExpr();
                    SootMethod calledMethod = invoke.getMethod();
                    String className = calledMethod.getDeclaringClass().getName();
                    
                    if (isStringClass(className)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    // 判断是否是String/StringBuffer/StringBuilder类
    private static boolean isStringClass(String className) {
        return className.startsWith("java.lang.String") ||
                className.startsWith("java.lang.StringBuffer") ||
                className.startsWith("java.lang.StringBuilder");
    }
    
    // 获取方法中的字符串参数
    public static List<Local> getStringParameters(SootMethod method) {
        List<Local> stringParams = new ArrayList<>();
        Body body = method.retrieveActiveBody();
        
        // 获取参数对应的Local变量
        List<Local> paramLocals = body.getParameterLocals();
        for (int i = 0; i < method.getParameterCount(); i++) {
            Type paramType = method.getParameterType(i);
            if (paramType.toString().equals("java.lang.String") ||
                paramType.toString().equals("java.lang.StringBuffer") ||
                paramType.toString().equals("java.lang.StringBuilder")) {
                stringParams.add(paramLocals.get(i));
            }
        }
        return stringParams;
    }
    
    // 检查路径是否使用了字符串参数
    public static boolean usesStringParameters(List<Unit> path, List<Local> stringParams) {
        if (stringParams.isEmpty()) {
            return false; // 没有字符串参数
        }
        
        for (Unit unit : path) {
            // 检查该语句是否使用了字符串参数
            for (ValueBox useBox : unit.getUseBoxes()) {
                Value value = useBox.getValue();
                if (value instanceof Local) {
                    Local local = (Local) value;
                    if (stringParams.contains(local)) {
                        return true;
                    }
                }
            }
            
            // 检查调用表达式中的参数
            if (unit instanceof Stmt) {
                Stmt stmt = (Stmt) unit;
                if (stmt.containsInvokeExpr()) {
                    InvokeExpr invoke = stmt.getInvokeExpr();
                    // 检查调用参数
                    for (Value arg : invoke.getArgs()) {
                        if (arg instanceof Local && stringParams.contains((Local) arg)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    
    // 查找循环的退出边
    private static List<Unit> findLoopExits(Unit node, Loop loop, UnitGraph cfg) {
        List<Unit> exits = new ArrayList<>();
        for (Unit succ : cfg.getSuccsOf(node)) {
            if (!loop.getLoopStatements().contains(succ)) {
                exits.add(succ);
            }
        }
        return exits;
    }
	
	
    
    // 包装路径为新方法（条件满足时才调用）
    public static void wrapPathAsNewMethod(PathInfo pathInfo, SootMethod originalMethod, int index) {
        if (pathInfo.hasStringAPI && pathInfo.usesStringParams) {
            System.out.println("Wrapping path as new method...");
			SootMethod newMethod = PathMethodWrapper.wrapPathAsNewMethod(pathInfo, originalMethod, index);

            // newMethod balabala
            // ...
        }
    }
    
    // 示例主方法
    public static void main(String[] args) {
        // Soot初始化
        String classPath = ".;/path/to/your/classes";
        Options.v().set_soot_classpath(classPath);
        
        // 加载目标类和方法
        SootClass sc = Scene.v().loadClassAndSupport("TestClass");
        sc.setApplicationClass();
        SootMethod sm = sc.getMethodByName("testMethod");
        
        // 设置循环展开次数
        int n = 2;
        
        // 提取并过滤路径
        List<PathInfo> filteredPaths = extractPathsWithFilter(sm, n);
        
        // 输出结果
        System.out.println("Total paths found: " + filteredPaths.size());
        for (int i = 0; i < filteredPaths.size(); i++) {
            PathInfo pathInfo = filteredPaths.get(i);
            System.out.println("=== Path " + (i + 1) + " ===");
            System.out.println("Has String API: " + pathInfo.hasStringAPI);
            System.out.println("Uses String Params: " + pathInfo.usesStringParams);
            System.out.println("Path length: " + pathInfo.path.size());
            
            // 打印路径详情
            for (Unit unit : pathInfo.path) {
                System.out.println("  " + unit);
            }
            
            // 如果满足条件，包装为新方法
            if (pathInfo.hasStringAPI && pathInfo.usesStringParams) {
                wrapPathAsNewMethod(pathInfo, sm, i);
            }
            System.out.println();
        }
    }
    
    // 内部类：路径状态
    static class PathState {
        List<Unit> path;
        Map<Loop, Integer> loopCounts;
        
        PathState() {
            this.path = new ArrayList<>();
            this.loopCounts = new HashMap<>();
        }
        
        PathState copy() {
            PathState copy = new PathState();
            copy.path = new ArrayList<>(this.path);
            copy.loopCounts = new HashMap<>(this.loopCounts);
            return copy;
        }
    }
    
    // 实用工具：检测特定的字符串API调用模式
    public static class StringAPIDetector extends BodyTransformer {
        @Override
        protected void internalTransform(Body body, String phase, Map<String, String> options) {
            List<String> stringAPIs = new ArrayList<>();
            
            for (Unit unit : body.getUnits()) {
                if (unit instanceof Stmt) {
                    Stmt stmt = (Stmt) unit;
                    if (stmt.containsInvokeExpr()) {
                        InvokeExpr invoke = stmt.getInvokeExpr();
                        SootMethod method = invoke.getMethod();
                        String className = method.getDeclaringClass().getName();
                        
                        // 检测字符串API
                        if (isStringClass(className)) {
                            stringAPIs.add(className + "." + method.getName());
                        }
                    }
                }
            }
            
            if (!stringAPIs.isEmpty()) {
                System.out.println("Found String APIs in " + body.getMethod().getName() + ":");
                for (String api : stringAPIs) {
                    System.out.println("  - " + api);
                }
            }
        }
    }
    
    // 批量处理方法
    public static void processMethods(SootClass clazz, int n) {
        List<SootMethod> methodsToProcess = new ArrayList<>();
        
        // 筛选需要处理的方法
        for (SootMethod method : clazz.getMethods()) {
            if (method.isConcrete() && containsStringAPI(method)) {
                methodsToProcess.add(method);
            }
        }
        
        System.out.println("Processing " + methodsToProcess.size() + " methods with string APIs");
        
        for (SootMethod method : methodsToProcess) {
            System.out.println("\n=== Processing method: " + method.getName() + " ===");
            List<PathInfo> paths = extractPathsWithFilter(method, n);
            
            for (PathInfo pathInfo : paths) {
                if (pathInfo.hasStringAPI && pathInfo.usesStringParams) {
                    System.out.println("Valid path found (length: " + pathInfo.path.size() + ")");
                    // 处理该路径...
                }
            }
        }
    }
}