package cn.ios.vs.smt.solver.justin;
import soot.Unit;
import soot.jimple.Stmt;
import java.util.List;
import java.util.Map;

public class JustinStrGenerator {
	
	public static final String NO_STRING_CONSTRAINT = "NO_STRING_CONSTRAINT_FOUND";
	
	public String generate(List<Unit> unitList) {
        // 1. 初始化工具
//        BriefUnitGraph graph = new BriefUnitGraph(linearizedBody);
//        List<Unit> unitList = new ArrayList<>(linearizedBody.getUnits());
        ValueResolver resolver = new ValueResolver(unitList);
        PathSAISBuilder saisBuilder = new PathSAISBuilder();

        // 2. 提取 SAIS (包含路径和归一化的条件)
        // extractSAISFromPath 返回的是正序路径 (Source -> Sink)
        PathSAISBuilder.SAIS sais = saisBuilder.extractSAISFromPath(unitList);
        
        if (sais == null || sais.trajectory.isEmpty()) {
            return NO_STRING_CONSTRAINT;
        }

//        System.out.println("Analyzing SAIS with " + sais.trajectory.size() + " APIs.");
//        System.out.println("Final Condition: Type=" + sais.conditionType + ", Value=" + sais.conditionValue);

        // 3. 执行推理 (Algorithm 4)
        return runInference(sais, resolver);
    }

    private String runInference(PathSAISBuilder.SAIS sais, ValueResolver resolver) {
        List<Stmt> trace = sais.trajectory;
        
        // 对应论文 Algorithm 4: Inference
        // 需要从后往前遍历 (Sink -> Source)
        // 只有 Sink (最后一个 API) 具有条件表达式，其他的 API 条件为 NULL
        
     // 1. 获取下标映射表 (由 PathSAISBuilder 生成)
        Map<Stmt, Integer> indexMap = sais.arrayIndexMap; 

        RegexWrapper accumulated = new RegexWrapper("", -1, null, 0); 
        boolean isFirst = true;

        for (int i = trace.size() - 1; i >= 0; i--) {
            Stmt stmt = trace.get(i);
            if (!stmt.containsInvokeExpr()) continue;

            // 确定条件
            int currentCondType;
            Object currentCondValue;

            if (isFirst) {
                // 只有最末尾的 API (Terminated API) 使用 SAIS 中提取的条件
                //  "For the terminated APIs... The S and L... are always NULL... 
                // And for the non-terminated APIs, their conditional expressions are always NULL"
                currentCondType = sais.conditionType;
                currentCondValue = sais.conditionValue;
            } else {
                // 对于前面的 API (Non-terminated)，如 split, substring，条件视为 NULL/无约束
                currentCondType = 0; // NULL condition
                currentCondValue = null;
            }
            
         // 如果 stmt 是 split 语句，这里会取到如 2；如果是 substring，则为 null
            Integer currentIndex = indexMap.get(stmt);

            // 调用 getWrapper 时传入 currentIndex
            RegexWrapper currentWrapper = FullApiRegexMapper.getWrapper(
                stmt.getInvokeExpr(), 
                currentCondType, 
                currentCondValue, 
                resolver, 
                stmt,
                currentIndex // <--- 传入参数
            );
            
//            System.out.println("##stmt: "+stmt +"      ### currentWrapper: "+currentWrapper.R);

            // 合并
            if (isFirst) {
                accumulated = currentWrapper;
                isFirst = false;
            } else {
                accumulated = InferenceEngine.merge(currentWrapper, accumulated);
            }
        }

        return accumulated.R;
    }
}