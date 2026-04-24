package cn.ios.vs.smt.solver;

import soot.tagkit.Tag;

public class LoopCountTag implements Tag {
    private final int loopCount;

    public LoopCountTag(int loopCount) {
        this.loopCount = loopCount;
    }

    public int getLoopCount() {
        return loopCount;
    }

    @Override
    public String getName() {
        return "LoopCountTag";
    }

    @Override
    public byte[] getValue() {
        return new byte[0]; // 不需要序列化
    }
}