package cn.ios.vs.smt.solver;
import soot.Unit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PathUnroller {
    public static boolean has_api_flag = false;

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
                    has_api_flag = true;
//                    System.out.println(loopBodyFragment + "contains string api with loop count" + loopCount);
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

    public static class PathSegment {
        enum Type { SINGLE_UNIT, LOOP_FRAGMENT }
        final Type type;
        final Unit unit;               // 当 type = SINGLE_UNIT 时有效
        final List<Unit> fragment;      // 当 type = LOOP_FRAGMENT 时有效
        final boolean hasApi;           // 当 type = LOOP_FRAGMENT 时有效

        PathSegment(Unit unit) {
            this.type = Type.SINGLE_UNIT;
            this.unit = unit;
            this.fragment = null;
            this.hasApi = false;
        }

        PathSegment(List<Unit> fragment, boolean hasApi) {
            this.type = Type.LOOP_FRAGMENT;
            this.fragment = fragment;
            this.hasApi = hasApi;
            this.unit = null;
        }
    }

    public static List<PathSegment> preprocessPath(List<Unit> path, LoopAnalysis loopAnalysis) {
        List<PathSegment> segments = new ArrayList<>();
        int i = 0;
        int n = path.size();

        while (i < n) {
            Unit current = path.get(i);
            Unit header = loopAnalysis.getLoopHeader(current);
            if (header == null) {
                // 单条语句
                segments.add(new PathSegment(current));
                i++;
            } else {
                // 收集连续的同循环头语句块
                List<Unit> fragment = new ArrayList<>();
                while (i < n && header.equals(loopAnalysis.getLoopHeader(path.get(i)))) {
                    fragment.add(path.get(i));
                    i++;
                }
                boolean hasApi = StringPathExtractor.containsStringAPIInPath(fragment);
                segments.add(new PathSegment(fragment, hasApi));
            }
        }
        return segments;
    }

    public static List<Unit> unrollPathWithMetadata(List<PathSegment> segments, int loopCount) {
        List<Unit> unrolled = new ArrayList<>();
        boolean hasApiFlag = false;  // 如果需要记录全局标志

        for (PathSegment seg : segments) {
            if (seg.type == PathSegment.Type.SINGLE_UNIT) {
                unrolled.add(seg.unit);
            } else { // LOOP_FRAGMENT
                if (seg.hasApi) {
                    hasApiFlag = true;
                    for (int c = 0; c < loopCount; c++) {
                        unrolled.addAll(seg.fragment);
                    }
                }
                // 无API的循环片段按原逻辑直接丢弃，不加任何内容
            }
        }
        has_api_flag = hasApiFlag;

        return unrolled;
    }
}