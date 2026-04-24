package cn.ios.vs.smt.solver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import soot.Body;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.IfStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.toolkits.graph.BriefUnitGraph;

public class StringPathExtractor {

	public boolean hasApiFlag = false;
	
	private SootMethod sootMethod;
	
	private List<List<Unit>> stringPaths = new ArrayList<>();
	public List<Integer> unroll_cnt = new ArrayList<>();
	
	private int loopCount = 1;
	
	public StringPathExtractor(SootMethod sootMethod, int loopCount){
		this.sootMethod = sootMethod;
		this.loopCount = loopCount;
	}
	
	public List<List<Unit>> allStringPaths(){
//		System.out.println("stage: splitting paths");
		List<List<Unit>> paths = new ArrayList<>();
		if(!sootMethod.hasActiveBody()) {
			System.out.println(sootMethod.getSignature()+" has no active body");
			return paths;
		}
//		System.out.println(sootMethod.getSignature()+" has an active body");
		Body body = sootMethod.getActiveBody();
		if(!containsIfStmtInBody(body)) {
			// System.out.println(sootMethod.getSignature()+" contains no IfStmt");
			return paths;
		}
//		System.out.println(sootMethod.getSignature()+" contains an IfStmt");
		if(!containsStringAPIInBody(body)) {
			// System.out.println(sootMethod.getSignature()+" contains no String API Invocation");
			return paths;
		}
//		System.out.println(sootMethod.getSignature()+" contains a String API Invocation");
		PathExtractor extractor = new PathExtractor(new BriefUnitGraph(body));
		paths = extractor.extractPaths();
		if (paths.size() > 500) paths = paths.subList(0, 500);
		PathUnroller.has_api_flag = false;
//		System.out.println(sootMethod.getSignature()+" paths number = "+paths.size());
		LoopAnalysis loopAnalysis = new LoopAnalysis(sootMethod);
        loopAnalysis.generation(new HashSet<>(Collections.singletonList(sootMethod)));
//		System.out.println("processing paths with size of " + paths.size());
		for(List<Unit> path:paths) {
			if(containsStringAPIInPath(path)) {
//				System.out.println("stage: looping on path " + path.hashCode());
				var memo = PathUnroller.preprocessPath(path, loopAnalysis);
				for (int loop_cnt = 1; loop_cnt <= 10; loop_cnt++) {
//					System.out.println("loop count " + loop_cnt);
					path = PathUnroller.unrollPathWithMetadata(memo, loop_cnt);
					if (!containsPath(stringPaths, path)) {
						List<Unit> tagged_path = new ArrayList<>(path);
						tagged_path.set(0, (Unit) path.get(0).clone());
						tagged_path.get(0).addTag(new LoopCountTag(loop_cnt));
						stringPaths.add(tagged_path);
					}
				}
//				System.out.println("finishing " + path.hashCode());
			}
		}
		if (PathUnroller.has_api_flag) hasApiFlag = true;
		return stringPaths;
	}
	
	public static boolean containsPath(List<List<Unit>> paths , List<Unit> path) {
		for (int i = 0; i < paths.size(); i++) {
			if(equalPath(paths.get(i),path )) {
//				System.out.println("#### equal paths");
				return true;
			}
		}
		return false;
	}
	
	 public static boolean equalPath(List<Unit> path1, List<Unit> path2) {
		 if(path1==null && path2==null) {
			 return true;
		 }
		 if(path1==null || path2==null) {
			 return false;
		 }
		 if(path1.size() != path2.size()) {
			 return false;
		 }
		for (int i = 0; i < path1.size(); i++) {
			if(!path1.get(i).toString().equals((path2.get(i).toString()))) {
				return false;
			}
		}
		return true;
	 }
	
	 public static boolean containsStringAPIInBody(Body body) {
	        for (Unit unit : body.getUnits()) {
	        	if (containsStringAPIInUnit(unit)) {
	            	return true;
	            }
	        }
	        return false;
	    }
	 
	 public static boolean containsIfStmtInBody(Body body) {
	        for (Unit unit : body.getUnits()) {
	        	if (unit instanceof IfStmt) {
	            	return true;
	            }
	        }
	        return false;
	    }
	
    // 检查路径是否包含字符串API调用
    public static boolean containsStringAPIInPath(List<Unit> path) {
        for (Unit unit : path) {
            if (containsStringAPIInUnit(unit)) {
            	return true;
            }
        }
        return false;
    }
    
    public static boolean containsStringAPIInUnit(Unit unit) {
    	if (unit instanceof Stmt) {
            Stmt stmt = (Stmt) unit;
            if (stmt.containsInvokeExpr()) {
                InvokeExpr invoke = stmt.getInvokeExpr();
                SootMethod calledMethod = invoke.getMethod();
                String className = calledMethod.getDeclaringClass().getName();

                if (!calledMethod.isStatic() && isStringClass(className)) {
                    return true;
                }
            }
        }
    	return false;
    }
    
    // 判断是否是String/StringBuffer/StringBuilder类
    public static boolean isStringClass(String className) {
        return className.equals("java.lang.String") ||
               className.equals("java.lang.StringBuffer") ||
               className.equals("java.lang.StringBuilder") ||
               className.startsWith("java.lang.String") ||
               className.startsWith("java.lang.StringBuffer") ||
               className.startsWith("java.lang.StringBuilder");
    }

}
