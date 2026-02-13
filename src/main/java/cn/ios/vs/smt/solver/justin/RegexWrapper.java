package cn.ios.vs.smt.solver.justin;

/**
 * 对应论文 Definition III-B1. (Regex Wrapper) [cite: 279]
 * 结构: W(R, L, S, Lmin)
 */
public class RegexWrapper {
    public String R;      // 正则表达式
    public int L;         // 允许合并的长度 (-1 表示无限制)
    public String S;      // 特殊后缀 (用于替换下一层正则中的 [\s\S])
    public int Lmin;      // 最小长度

    public RegexWrapper(String r, int l, String s, int lmin) {
        this.R = r;
        this.L = l;
        this.S = s;
        this.Lmin = lmin;
    }

    // 默认空包装器 (即 [\s\S]*)
    public static RegexWrapper defaultWrapper() {
        return new RegexWrapper("[\\s\\S]*", -1, null, 0);
    }

    @Override
    public String toString() {
        return String.format("W(R='%s', L=%d, S='%s', Lmin=%d)", R, L, S, Lmin);
    }
}