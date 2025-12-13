package test;

import soot.Unit;

import java.util.List;

// 辅助类：用于存储路径信息
public class PathInfo {
    public List<Unit> path;
    public boolean hasStringAPI;
    public boolean usesStringParams;
}
