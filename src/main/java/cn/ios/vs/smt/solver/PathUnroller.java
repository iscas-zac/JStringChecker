package cn.ios.vs.smt.solver;
import soot.Unit;
import java.util.ArrayList;
import java.util.List;

public class PathUnroller {

    /**
     * 对给定的 Unit 路径进行循环展开
     * * @param path 原路径 List
     * @param loopCount 展开次数 (例如 2 表示每个循环体在路径中重复出现 2 次)
     * @return 展开后的路径 List
     */
    public static List<Unit> unrollPath(List<Unit> path, LoopAnalysis loopAnalysis, int loopCount) {
        if (path == null || path.isEmpty() || loopCount <= 1) {
            return new ArrayList<>(path);
        }

        List<Unit> unrolledPath = new ArrayList<>();
        int i = 0;
        int n = path.size();

        while (i < n) {
            Unit currentUnit = path.get(i);
            Unit header = loopAnalysis.getLoopHeader(currentUnit);

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
                
                if(StringPathExtractor.containsStringAPIInPath(loopBodyFragment)) {
                	// 3. 根据 loopCount 进行展开
                    for (int count = 0; count < loopCount; count++) {
                        // 注意：如果 Unit 对象在后续分析中有状态变化，这里可能需要 clone
                        // 但在 Soot 的路径表示中，通常直接引用原 Unit 即可
                        unrolledPath.addAll(loopBodyFragment);
                    }
                }

                
            }
        }

        return unrolledPath;
    }
}