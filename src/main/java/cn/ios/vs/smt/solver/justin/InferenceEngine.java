package cn.ios.vs.smt.solver.justin;
public class InferenceEngine {

    private static final String ANY_REGEX = "[\\s\\S]";

    /**
     * 对应论文 Algorithm 3: Merge [cite: 300]
     * 将当前的 API 效果 (w2) 合并到累积的历史正则 (w1) 中
     * 注意：w2 是当前步骤（靠前的API），w1 是后续步骤（靠后的API，已处理过的）
     */
    public static RegexWrapper merge(RegexWrapper w2, RegexWrapper w1) {
//    	  System.out.println("##merge begin: ");
//    	  System.out.println("##RegexWrapper w2: " + w2.R);
//    	  System.out.println("##RegexWrapper w1: " + w1.R);
        RegexWrapper result = new RegexWrapper("", -1, null, 0);

        // 1. 处理后缀替换 (Algorithm 3, Line 2-3)
        // 如果当前API (w2) 对后缀有特殊限制 (S != NULL)，则替换 w1 中的通配符
        String w1_R_processed = w1.R;
        if (w2.S != null) {
            // 注意：这里简单的字符串替换可能不安全，实际生产中应解析正则树
            // 这里为了演示，假设 w1.R 开头通常是 [\s\S]
            if (w1_R_processed.contains(ANY_REGEX)) {
                w1_R_processed = w1_R_processed.replace(ANY_REGEX, w2.S);
            }
        }

        // 2. 处理长度限制 (Algorithm 3, Line 4-7)
        // 如果当前API (w2) 限制了后续部分的长度 (例如 substring)
        if (w2.L != -1) {
            int newLen = w2.L - w1.Lmin;
            if (newLen < 0) newLen = 0; // 边界保护
            
            // 将 w1 中的 * (无限) 具象化为具体的长度
            // 简单实现：假设 w1.R 是 [\s\S]* 这种形式，将其变为 [\s\S]{newLen}
            // 论文伪代码: w1.R = w1.R.replace("*", "{" + newLen + "}");
            // 实际需要更复杂的正则解析，这里做简化处理：
            if (w1_R_processed.contains("*")) {
                w1_R_processed = w1_R_processed.replace("*", "{" + newLen + "}");
            } else {
                // 如果没有 *，可能需要追加约束
            }
            // 更新当前节点的最小长度为限制长度
            result.Lmin = w2.L; 
        } else {
            result.Lmin = w1.Lmin + w2.Lmin; // Algorithm 3, Line 11
        }

        // 3. 拼接正则 (Algorithm 3, Line 8)
        // 新正则 = 当前API的前缀效果 + 后续API的处理结果
        result.R = w2.R + w1_R_processed;
        
        // 4. 重置状态 (Algorithm 3, Line 9-10)
        result.L = -1;
        result.S = null;

//  	    System.out.println("##Merged RegexWrapper : " + result.R);
        return result;
    }
}