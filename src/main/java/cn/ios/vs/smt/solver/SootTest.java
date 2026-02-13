package cn.ios.vs.smt.solver;

import soot.*;
import soot.options.Options;
import java.util.Arrays;

public class SootTest {
    public static void main(String[] args) {
        // 设置处理jar
        Options.v().set_process_dir(Arrays.asList("D:\\learning\\jars\\byproduct\\dataset_rev\\awesome\\fastcsv\\fastcsv-3.4.0.jar"));
        Options.v().set_allow_phantom_refs(true);
        
        // 添加transform
        PackManager.v().getPack("jtp").add(
            new Transform("jtp.test", new BodyTransformer() {
                @Override
                protected void internalTransform(Body b, String phase, java.util.Map<String, String> options) {
                    System.out.println("123");
                }
            })
        );
        
        // 加载并运行
        Scene.v().loadNecessaryClasses();
        PackManager.v().runPacks();
    }
}