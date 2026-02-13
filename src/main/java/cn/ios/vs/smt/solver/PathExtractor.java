package cn.ios.vs.smt.solver;
import soot.Unit;
import soot.jimple.GotoStmt;
import soot.toolkits.graph.UnitGraph;

import java.util.*;

public class PathExtractor {

    private UnitGraph cfg;
    private List<List<Unit>> allPaths;

    public PathExtractor(UnitGraph cfg) {
        this.cfg = cfg;
        this.allPaths = new ArrayList<>();
    }

    /**
     * 获取所有路径，循环展开一次
     */
    public List<List<Unit>> extractPaths() {
        allPaths.clear();
        // 获取 CFG 的所有入口点（通常只有一个）
        for (Unit head : cfg.getHeads()) {
            List<Unit> currentPath = new ArrayList<>();
            Map<Unit, Integer> visitCount = new HashMap<>();
            try {
                dfs(head, currentPath, visitCount);
            } catch (Error e) {
                e.printStackTrace();
            }
        }
        //remove GotoStmt
        for(List<Unit> path:allPaths) {
        	List<Unit>  newPath = new ArrayList<>(path);
        	for (int i = 0; i < path.size(); i++) {
				if(path.get(i) instanceof GotoStmt) {
					newPath.remove(path.get(i));
				}
			}
        	path.clear();
        	path.addAll(newPath);
        }
        return allPaths;
    }

    private void dfs(Unit current, List<Unit> currentPath, Map<Unit, Integer> visitCount) {
        // 记录当前节点访问次数
        int count = visitCount.getOrDefault(current, 0) + 1;
        
        // 终止条件：如果循环展开一次，意味着同一个 Unit 在路径中最多出现 2 次（进入->回到Header）
        // 如果您希望“只执行一次循环体就退出”，count > 2 是合适的
        if (count > 2) {
            // 到达展开上限，记录当前路径并截断
            allPaths.add(new ArrayList<>(currentPath));
            return;
        }

        // 添加到当前路径
        currentPath.add(current);
        visitCount.put(current, count);

        List<Unit> successors = cfg.getSuccsOf(current);

        if (successors.isEmpty()) {
            // 到达叶子节点（方法出口），保存路径
            allPaths.add(new ArrayList<>(currentPath));
        } else {
            for (Unit next : successors) {
                // 递归探测分支
                dfs(next, currentPath, visitCount);
            }
        }

        // --- 回溯 (Backtracking) ---
        // 移出路径，并将计数减 1，以便 DFS 能够遍历其他分支
        currentPath.remove(currentPath.size() - 1);
        visitCount.put(current, count - 1);
    }
}