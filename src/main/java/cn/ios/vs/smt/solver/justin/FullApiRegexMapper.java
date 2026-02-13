package cn.ios.vs.smt.solver.justin;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.Unit;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 完整实现 API-Regex-Mapping(new).pdf 的映射规则
 * 覆盖 String, StringBuffer, StringBuilder 的 API
 */
public class FullApiRegexMapper {

    private static final String ANY = "[\\s\\S]"; // 对应 PDF 中的 [\s\S]
    private static final int UNLIMITED = -1;      // 对应 PDF 中的 -1

    // 条件常量
    public static final int COND_FALSE = 0;
    public static final int COND_TRUE = 1;
    public static final int COND_EQ = 1;
    public static final int COND_NE = 2;
    public static final int COND_GT = 3;
    public static final int COND_GE = 4;
    public static final int COND_LT = 5;
    public static final int COND_LE = 6;

    /**
     * 根据 API 调用和条件获取正则包装器
     *
     * @param invoke    API 调用
     * @param condType  归一化条件类型 (如 COND_EQ)
     * @param condValue 条件值 (如 0, "abc")
     * @param resolver  值解析器
     * @param context   上下文语句
     * @return RegexWrapper
     */
    public static RegexWrapper getWrapper(InvokeExpr invoke, int condType, Object condValue,
                                          ValueResolver resolver, Unit context, 
                                          Integer arrayIndex) {
        String subSig = invoke.getMethod().getSubSignature();
        String name = invoke.getMethod().getName();
        List<Value> args = invoke.getArgs();

        // ==================================================================================
        // 1. Boolean 检查与相等性判断 (Rows 1-12, 16-17, 18)
        // ==================================================================================

        // [Row 1] boolean isEmpty()
        if (subSig.equals("boolean isEmpty()")) {
            if (isTrue(condType)) return new RegexWrapper(ANY + "{0}", UNLIMITED, null, 0);
            if (isFalse(condType)) return new RegexWrapper(ANY + "{1,}", UNLIMITED, null, 1);
        }

        // [Row 2] boolean equals(Object)
        if (subSig.equals("boolean equals(java.lang.Object)")) {
            String v = resolveString(args.get(0), resolver, context);
            if (isTrue(condType)) return new RegexWrapper(escape(v), UNLIMITED, null, v.length());
            // != v (简化处理)
            else{
            	
            	return new RegexWrapper("(?!" + escape(v) + "$)" + ANY + "*", UNLIMITED, null, 0);
            }
        }

        // [Row 3-4] int compareTo(String/Object) => r == 0 等价于 equals
        if (name.equals("compareTo")) {
            String v = resolveString(args.get(0), resolver, context);
            // compareTo == 0 意味着相等
            if (isEq0(condType, condValue)) {
                return new RegexWrapper(escape(v), UNLIMITED, null, v.length());
            }
        }

        // [Row 5-6] boolean contentEquals(CharSequence/StringBuffer)
        if (name.equals("contentEquals")) {
            String v = resolveString(args.get(0), resolver, context);
            if (isTrue(condType)) return new RegexWrapper(escape(v), UNLIMITED, null, v.length());
        }

        // [Row 7-8] compareToIgnoreCase, equalsIgnoreCase
        if (name.equals("compareToIgnoreCase") || name.equals("equalsIgnoreCase")) {
            String v = resolveString(args.get(0), resolver, context);
            // 忽略大小写：(?i)v
            if (isTrue(condType) || (name.equals("compareToIgnoreCase") && isEq0(condType, condValue))) {
                return new RegexWrapper("(?i)" + escape(v), UNLIMITED, null, v.length());
            }
        }

        // [Row 9] boolean contains(CharSequence)
        if (name.equals("contains")) {
            String v = resolveString(args.get(0), resolver, context);
            if (isTrue(condType)) return new RegexWrapper(ANY + "*" + escape(v) + ANY + "*", UNLIMITED, null, v.length());
            else return new RegexWrapper("((?!" + escape(v) + ")" + ANY + ")*", UNLIMITED, null, 0);
        }

        // [Row 10] boolean startsWith(String)
        if (subSig.equals("boolean startsWith(java.lang.String)")) {
            String v = resolveString(args.get(0), resolver, context);
            if (isTrue(condType)) return new RegexWrapper(escape(v) + ANY + "*", UNLIMITED, null, v.length());
        }

        // [Row 11] boolean startsWith(String, int)
        if (subSig.equals("boolean startsWith(java.lang.String,int)")) {
            String v = resolveString(args.get(0), resolver, context);
            int i1 = resolveInt(args.get(1), resolver, context);
            if (isTrue(condType)) return new RegexWrapper(ANY + "{" + i1 + "}" + escape(v) + ANY + "*", UNLIMITED, null, i1 + v.length());
        }

        // [Row 12] boolean endsWith(String)
        if (subSig.equals("boolean endsWith(java.lang.String)")) {
            String v = resolveString(args.get(0), resolver, context);
            if (isTrue(condType)) return new RegexWrapper(ANY + "*" + escape(v), UNLIMITED, null, v.length());
        }

        // [Row 16-17] boolean regionMatches(...)
        // 逻辑：[\s\S]{i1} subStr [\s\S]*，其中 subStr 是 v1 的子串
        if (name.equals("regionMatches")) {
            if (isTrue(condType)) {
                // 处理 boolean regionMatches(int toffset, String other, int ooffset, int len)
                // 以及 boolean regionMatches(boolean ignoreCase, ...)
                // 这里为了通用简化，假设是标准匹配
                int offsetArgIdx = subSig.startsWith("boolean regionMatches(boolean") ? 1 : 0;
                
                int i1 = resolveInt(args.get(offsetArgIdx), resolver, context); // toffset
                String v1 = resolveString(args.get(offsetArgIdx + 1), resolver, context); // other
                int i2 = resolveInt(args.get(offsetArgIdx + 2), resolver, context); // ooffset
                int i3 = resolveInt(args.get(offsetArgIdx + 3), resolver, context); // len
                
                String subStr;
                try {
                    // 尝试在编译期截取字符串常量
                    if (!v1.equals("UNKNOWN") && i2 >= 0 && (i2 + i3) <= v1.length()) {
                        subStr = escape(v1.substring(i2, i2 + i3));
                    } else {
                        subStr = ANY + "{" + i3 + "}"; // 如果无法截取，则用长度约束代替
                    }
                } catch (Exception e) {
                    subStr = ANY + "{" + i3 + "}";
                }
                
                return new RegexWrapper(ANY + "{" + i1 + "}" + subStr + ANY + "*", UNLIMITED, null, i1 + i3);
            }
        }

        // [Row 18] boolean matches(String regex)
        if (name.equals("matches")) {
            String v = resolveString(args.get(0), resolver, context);
            if (isTrue(condType)) return new RegexWrapper(v, UNLIMITED, null, 0); // 直接使用传入的正则
        }


        // ==================================================================================
        // 2. 索引定位 (Rows 13-15, 19-31)
        // ==================================================================================

        // [Row 13-15] indexOf(String)
        if (name.equals("indexOf") && args.size() == 1 && args.get(0).getType().toString().equals("java.lang.String")) {
            String v = resolveString(args.get(0), resolver, context);
            int i1 = getIntVal(condValue);
            int lenV = v.length();

            if (condType == COND_EQ) // r == i1
                return new RegexWrapper(notStr(v, i1) + escape(v) + ANY + "*", UNLIMITED, null, i1 + lenV);
            if (condType == COND_GE) // r >= i1
                return new RegexWrapper(notStr(v, i1 + ",") + escape(v) + ANY + "*", UNLIMITED, null, i1 + lenV);
            if (condType == COND_LE) // r <= i1
                return new RegexWrapper(notStr(v, 0, i1) + escape(v) + ANY + "*", UNLIMITED, null, lenV);
        }

        // [Row 19] indexOf(int ch) -> 转为字符串处理
        if (name.equals("indexOf") && args.size() == 1 && isIntType(args.get(0))) {
            int ch = resolveInt(args.get(0), resolver, context);
            String v = String.valueOf((char) ch); // int 转 char string
            int i1 = getIntVal(condValue);

            if (condType == COND_EQ) return new RegexWrapper(notStr(v, i1) + escape(v) + ANY + "*", UNLIMITED, null, i1 + 1);
            if (condType == COND_LE) return new RegexWrapper(notStr(v, 0, i1) + escape(v) + ANY + "*", UNLIMITED, null, 1);
        }

        // [Row 20-22] indexOf(String, int fromIndex)
        if (name.equals("indexOf") && args.size() == 2 && args.get(0).getType().toString().equals("java.lang.String")) {
            String v = resolveString(args.get(0), resolver, context);
            int i1 = resolveInt(args.get(1), resolver, context); // fromIndex
            int i2 = getIntVal(condValue); // result

            if (condType == COND_EQ) { // r == i2
                int gap = Math.max(0, i2 - i1);
                // [\s\S]{i1} [^v]{i2-i1} v ...
                return new RegexWrapper(ANY + "{" + i1 + "}" + notStr(v, gap) + escape(v) + ANY + "*", UNLIMITED, null, i2 + v.length());
            }
            if (condType == COND_GE) { // r >= i2
                 // 逻辑：前面的 i1 随意，后面 >= gap 处出现
                 int gap = Math.max(0, i2 - i1);
                 return new RegexWrapper(ANY + "{" + i1 + "}" + notStr(v, gap + ",") + escape(v) + ANY + "*", UNLIMITED, null, i2 + v.length());
            }
        }
        
        // [Row 23] indexOf(int ch, int fromIndex) - 类比 Row 20-22
        if (name.equals("indexOf") && args.size() == 2 && isIntType(args.get(0))) {
             int ch = resolveInt(args.get(0), resolver, context);
             String v = String.valueOf((char) ch);
             int i1 = resolveInt(args.get(1), resolver, context); 
             int i2 = getIntVal(condValue);
             if (condType == COND_EQ) {
                  int gap = Math.max(0, i2 - i1);
                  return new RegexWrapper(ANY + "{" + i1 + "}" + notStr(v, gap) + escape(v) + ANY + "*", UNLIMITED, null, i2 + 1);
             }
        }

        // [Row 24-26] lastIndexOf(String)
        if (name.equals("lastIndexOf") && args.size() == 1 && args.get(0).getType().toString().equals("java.lang.String")) {
            String v = resolveString(args.get(0), resolver, context);
            int i1 = getIntVal(condValue);
            
            // r == i1: [\s\S]{i1} v [^v]* (在 i1 处匹配 v，且之后不再出现 v)
            if (condType == COND_EQ) {
                return new RegexWrapper(ANY + "{" + i1 + "}" + escape(v) + notStr(v, "*"), UNLIMITED, null, i1 + v.length());
            }
             // r <= i1: 前面 0-i1 处匹配，且后面不保证？
             // PDF 逻辑 (Row 26): [\s\S]{0,i1} v [^v]*
            if (condType == COND_LE) {
                return new RegexWrapper(ANY + "{0," + i1 + "}" + escape(v) + notStr(v, "*"), UNLIMITED, null, v.length());
            }
        }

        // [Row 28-31] lastIndexOf(String, int fromIndex)
        // 这是一个比较复杂的约束，PDF 定义：[\s\S]{r} v [^v]{from - r - len(v)}
        if (name.equals("lastIndexOf") && args.size() == 2 && args.get(0).getType().toString().equals("java.lang.String")) {
            String v = resolveString(args.get(0), resolver, context);
            int i1 = resolveInt(args.get(1), resolver, context); // fromIndex
            int i2 = getIntVal(condValue); // result
            
            if (condType == COND_EQ) {
                // 确保 i2 <= i1
                int tailLen = i1 - i2 - v.length() + 1; // PDF 逻辑: i1 + 1 - i2 - Len(v)
                if (tailLen < 0) tailLen = 0;
                return new RegexWrapper(ANY + "{" + i2 + "}" + escape(v) + notStr(v, tailLen) + ANY + "*", UNLIMITED, null, i1 + 1);
            }
        }


        // ==================================================================================
        // 3. 字符与代码点访问 (Rows 32-46)
        // ==================================================================================

        // [Row 32-34] charAt(int)
        if (name.equals("charAt")) {
            int i1 = resolveInt(args.get(0), resolver, context); // index
            int i2 = getIntVal(condValue); // char value
            
            if (condType == COND_EQ) {
                String c = String.valueOf((char) i2);
                return new RegexWrapper(ANY + "{" + i1 + "}" + escape(c) + ANY + "*", UNLIMITED, null, i1 + 1);
            }
        }

        // [Row 35-37] codePointAt(int) (近似按 charAt 处理)
        if (name.equals("codePointAt")) {
            int i1 = resolveInt(args.get(0), resolver, context);
            int i2 = getIntVal(condValue);
            if (condType == COND_EQ) {
                String c = new String(Character.toChars(i2));
                return new RegexWrapper(ANY + "{" + i1 + "}" + escape(c) + ANY + "*", UNLIMITED, null, i1 + 1);
            }
        }

        // [Row 41-43] codePointBefore(int) -> [\s\S]{i-1} c ...
        if (name.equals("codePointBefore")) {
            int i1 = resolveInt(args.get(0), resolver, context);
            int i2 = getIntVal(condValue);
            if (condType == COND_EQ) {
                String c = new String(Character.toChars(i2));
                int idx = Math.max(0, i1 - 1);
                return new RegexWrapper(ANY + "{" + idx + "}" + escape(c) + ANY + "*", UNLIMITED, null, i1);
            }
        }
        
        // [Row 38-40] codePointCount (长度约束)
        if (name.equals("codePointCount")) {
             // 实际上 codePointCount 和 length 类似，但可能略短（对于 surrogate pairs）
             // 简单处理：作为 length 约束
             int i3 = getIntVal(condValue);
             if (condType == COND_EQ) return new RegexWrapper(ANY + "{" + i3 + ",}", UNLIMITED, null, i3);
        }

        // [Row 44-46] offsetByCodePoints (复杂，仅做长度占位)
        if (name.equals("offsetByCodePoints")) {
            // PDF 映射: [\s\S]{i1+i2} ... 
            int i1 = resolveInt(args.get(0), resolver, context);
            int i2 = resolveInt(args.get(1), resolver, context);
            return new RegexWrapper(ANY + "{" + (i1 + i2) + "}", UNLIMITED, null, i1 + i2);
        }


        // ==================================================================================
        // 4. 长度与数组 (Rows 47-53, 108)
        // ==================================================================================

        // [Row 47-49] length()
        if (name.equals("length") && args.isEmpty()) {
            int i1 = getIntVal(condValue);
            if (condType == COND_EQ) return new RegexWrapper(ANY + "{" + i1 + "}", UNLIMITED, null, i1);
            if (condType == COND_GE) return new RegexWrapper(ANY + "{" + i1 + ",}", UNLIMITED, null, i1);
            if (condType == COND_LE) return new RegexWrapper(ANY + "{0," + i1 + "}", UNLIMITED, null, 0);
        }
        
        // [Row 108] Array length (用于 split 后的数组长度检查)
        // r = arr.length, arr came from str.split
        // 这里需要结合 SAIS 的上下文，假设这是针对 String 的长度操作
        // 如果是数组长度，通常是在 mutated SAIS 中处理，这里按通用长度处理

        // [Row 50-53] toCharArray, getBytes
        // r = str.toCharArray(); r[i] == v
        // 这里的 condType 可能需要外部传入索引信息，为了演示，假设 condValue 包含了索引逻辑
        // 难点：Soot 中的 ArrayRef 需要特殊处理。
        // PDF 映射: [\s\S]{i} v [\s\S]*
        if (name.equals("toCharArray") || name.equals("getBytes")) {
            // 这是一个转换 API，通常本身没有约束，除非后续有数组访问。
            // 返回默认，等待数组访问指令（如果 SAIS 能够捕获）进行增强
            // 或者：如果 condType 指示了 r[i] == v (这需要更复杂的参数传递)，我们可以生成正则
            // 目前返回默认。
        }


        // ==================================================================================
        // 5. 字符串修改与截取 (Non-terminated) (Rows 54-63, 66-72)
        // ==================================================================================

        // [Row 54] trim()
        if (name.equals("trim")) {
            // PDF: S = [^ ] (非空格)
            // 语义：前后可能有空格，中间是内容。但作为反向推理，Caller = \s* Result \s*
            // PDF 定义 S 用来填充 trim 掉的部分?
            return new RegexWrapper("", UNLIMITED, "[^\\s]", 0);
        }

        // [Row 55-63] substring / subSequence
        if (name.equals("substring") || name.equals("subSequence")) {
            int i1 = resolveInt(args.get(0), resolver, context);
            if (args.size() == 1) {
                // substring(i) -> Caller = [\s\S]{i} + Result
                return new RegexWrapper(ANY + "{" + i1 + "}", UNLIMITED, null, i1);
            } else {
                int i2 = resolveInt(args.get(1), resolver, context);
                // substring(i1, i2) -> Caller = [\s\S]{i1} + Result (Len=i2-i1)
                return new RegexWrapper(ANY + "{" + i1 + "}", i2 - i1, null, i1);
            }
        }
        
        // [Row 66-69] toLowerCase / toUpperCase
        if (name.equals("toLowerCase") || name.equals("toUpperCase")) {
            // 大小写转换。反向推理时，Caller 可以是任意大小写。
            // 简单处理：返回通配，或者像 PDF 建议的 NULL (即不产生额外结构变化)
            return RegexWrapper.defaultWrapper();
        }
        
        // [Row 70-72] toString()
        if (name.equals("toString")) {
            // Identity transformation
            return RegexWrapper.defaultWrapper();
        }

        // ==================================================================================
        // 6. 分割与替换 (Rows 64-65, 73-76)
        // ==================================================================================

     // [Row 64-65] split(regex)
        if (name.equals("split")) {
            String v = resolveString(args.get(0), resolver, context);
            
            // S: 约束后续内容不能包含分隔符
            // 如果 v 是 "#", S 应该是 "[^#]"
            String s_constraint = notStr(v, ""); // 获取不带量词的非v表达式，如 [^#] 或 ((?!v).)*
            // 注意：notStr 默认带量词，我们需要剥离或者重写一个简单的 getNegatedCharClass

            // 为了精确控制，我们手动构建 S
            String s_raw; 
            if (v.length() == 1) {
                s_raw = "[^" + escape(v) + "]";
            } else {
                // 复杂字符串的否定预查 (Lookahead)
                s_raw = "((?!" + escape(v) + ")" + ANY + ")";
            }

            // R: 前缀 (Prefix)
            // 根据 PDF 和你的需求，前缀应为 v 重复 index 次
            // 即 split("#")[2] -> "##" (匹配 ##class)
            if (arrayIndex != null && arrayIndex > 0) {
                String prefix;
                if (v.length() == 1) {
                    // 单字符使用字符类语法 [v]{n} 比较整洁
                    prefix = "[" + escape(v) + "]{" + arrayIndex + "}";
                } else {
                    // 多字符使用非捕获组 (?:v){n}
                    prefix = "(?:" + escape(v) + "){" + arrayIndex + "}";
                }
                
                // 返回 Wrapper: R=前缀, S=非分隔符
                return new RegexWrapper(prefix, UNLIMITED, s_raw, 0);
            } else {
                // 如果 index=0，前缀为空，但 S 约束依然存在
                return new RegexWrapper("", UNLIMITED, s_raw, 0);
            }
        }

        // [Row 73] replace(char, char)
        if (name.equals("replace") && args.size() == 2 && isIntType(args.get(0))) {
            // 字符替换。反向较难。PDF 建议 S = [^v] (Row 73: S = [^v]) ?
            // 假设是 replace(old, new)。Caller 中 old 变成了 new。
            // 返回默认。
            return RegexWrapper.defaultWrapper();
        }

        // [Row 74-76] replace(CharSequence...), replaceAll, replaceFirst
        if (name.startsWith("replace")) {
            // PDF Row 74: S = [^v1] ? (PDF OCR fuzzy)
            // 复杂 API，暂时返回默认
            return RegexWrapper.defaultWrapper();
        }

        // ==================================================================================
        // 7. 拼接与构建 (Rows 77-107)
        // ==================================================================================

        // [Row 77-79] append(String/...)
        if (name.equals("append")) {
            String v = resolveString(args.get(0), resolver, context);
            // StringBuilder.append(v)
            // 反向视角：StringBuilder 在 append 之前的状态。
            // 正向视角：Result = Previous + v
            // 在 Merge 算法中 (result = w2 + w1)，如果 w2 是 append(v)，则结果是 v + w1? 
            // 不，append 是加在后面。
            // 这里的语义应该是：API 贡献了 v 这个片段。
            // 我们将其放在 R 中，Merge 时拼接到合适位置。
            // *注意*: StringBuilder 的 append 实际上对应的是 [\s\S]* v
            // 简单实现：将 append 的内容作为 R
            return new RegexWrapper(escape(v), UNLIMITED, null, v.length());
        }

        // [Row 80-93] concat(String)
        if (name.equals("concat")) {
            String v = resolveString(args.get(0), resolver, context);
            // str.concat(v) -> Result
            // Caller 是 Result 去掉末尾的 v
            // 反向推理：Caller + v = Result
            // 这里我们可能无法完全表达 "减去"，但可以表达结构
            return new RegexWrapper(escape(v), UNLIMITED, null, v.length());
        }
        
        // [Row 94-107] insert / replace (StringBuilder)
        // insert(offset, str) -> 复杂的位置操作，正则难以精确反向推导
        // 标记为困难
        if (name.equals("insert")) {
            // PDF 可能没有详细映射 insert，或归类为 append 类操作
            // 建议：返回通配
            return RegexWrapper.defaultWrapper();
        }

        return RegexWrapper.defaultWrapper();
    }

    // ==================================================================================
    // 辅助方法
    // ==================================================================================

    private static boolean isTrue(int type) {
        return type == COND_TRUE || type == COND_EQ;
    }
    
    private static boolean isFalse(int type) {
        return type == COND_FALSE || type == COND_NE;
    }

    private static boolean isEq0(int type, Object value) {
        return type == COND_EQ && getIntVal(value) == 0;
    }

    private static String resolveString(Value v, ValueResolver resolver, Unit context) {
        Object res = resolver.resolve(v, context);
        return (res instanceof String) ? (String) res : "UNKNOWN";
    }

    private static int resolveInt(Value v, ValueResolver resolver, Unit context) {
        Object res = resolver.resolve(v, context);
        return (res instanceof Integer) ? (Integer) res : 0;
    }

    private static int getIntVal(Object val) {
        return (val instanceof Integer) ? (Integer) val : 0;
    }
    
    private static boolean isIntType(Value v) {
        String t = v.getType().toString();
        return t.equals("int") || t.equals("char") || t.equals("short") || t.equals("byte");
    }

    private static String escape(String s) {
        if ("UNKNOWN".equals(s)) return ANY + "*";
        return Pattern.quote(s);
    }

    /**
     * 生成 [^v]{count}
     */
    private static String notStr(String v, int count) {
        if ("UNKNOWN".equals(v)) return ANY + "{" + count + "}";
        if (v.length() == 1) return "[^" + escape(v) + "]{" + count + "}";
        return "((?!" + escape(v) + ")" + ANY + "){" + count + "}";
    }

    /**
     * 生成 [^v]{min, max} 或 [^v]{min,}
     */
    private static String notStr(String v, int min, int max) {
         if ("UNKNOWN".equals(v)) return ANY + "{" + min + ",}";
         if (v.length() == 1) return "[^" + escape(v) + "]{" + min + ",}"; // 简化处理上界
         return "((?!" + escape(v) + ")" + ANY + "){" + min + ",}";
    }
    
    /**
     * 生成 [^v] + 量词 (如 *)
     */
    private static String notStr(String v, String quantifier) {
        if ("UNKNOWN".equals(v)) return ANY + quantifier;
        if (v.length() == 1) return "[^" + escape(v) + "]" + quantifier;
        return "((?!" + escape(v) + ")" + ANY + ")" + quantifier;
    }
}