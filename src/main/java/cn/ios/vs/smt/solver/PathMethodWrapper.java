package cn.ios.vs.smt.solver;

import soot.*;
import soot.jimple.*;
import soot.options.Options;
import soot.util.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.io.*;

import com.github.curiousoddman.rgxgen.RgxGen;
import com.github.curiousoddman.rgxgen.config.RgxGenProperties;

import cn.ios.casegen.config.GlobalCons;
import cn.ios.casegen.enums.GenerationEnum;
import cn.ios.casegen.util.log.Log;
import cn.ios.vs.smt.solver.justin.JustinStrGenerator;

import java.util.Map.Entry;

import static extension.PluginStrictKt.transformAndOutputAt;

public class PathMethodWrapper {
    
    // 包装路径为新方法
    public static SootMethod wrapPathAsNewMethod(List<Unit> path, SootMethod originalMethod, int pathIndex) {
        // 创建新方法名
        String newMethodName = originalMethod.getName() + "_path_" + pathIndex;
        
        // 获取原始方法的返回类型
        Type returnType = originalMethod.getReturnType();
        
        // 复制原始方法的参数列表
        List<Type> paramTypes = new ArrayList<>();
        for (int i = 0; i < originalMethod.getParameterCount(); i++) {
            paramTypes.add(originalMethod.getParameterType(i));
        }
        
        // 创建新方法
        SootMethod newMethod = new SootMethod(
            newMethodName,
            paramTypes,
            returnType,
            originalMethod.getModifiers()
        );
        
        // 将新方法添加到类中
        originalMethod.getDeclaringClass().addMethod(newMethod);
        
        // 创建方法体
        JimpleBody newBody = Jimple.v().newBody(newMethod);
        
        newMethod.setActiveBody(newBody);
        
        // 添加参数局部变量
//        List<Local> paramLocals = setupParameters(newMethod, newBody, originalMethod);
        
        // 添加路径中的语句到新方法
        Map<Unit, Unit> unitMapping = new HashMap<>(); // 原始语句到新语句的映射
        
        for (Local local : originalMethod.getActiveBody().getLocals()) {
        	newBody.getLocals().add(local);
        }

        
        // 复制路径中的语句
        for (Unit originalUnit : path) {
        	 // 深度复制语句
            Stmt newStmt = null;
			try {
				newStmt = (Stmt) originalUnit.clone();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            if (newStmt != null) {
                newBody.getUnits().add(newStmt);
            }
        }
        
        for (Local local : originalMethod.getActiveBody().getParameterLocals()) {
            if (!newBody.getParameterLocals().contains(local))
        	    newBody.getParameterLocals().add(local);
        }
        
        // 添加返回语句（如果路径最后不是返回）
        addReturnStatement(newBody, returnType, path, unitMapping);
        
        // 构建基本块和控制流
        rebuildControlFlow(newBody, path, unitMapping);
        
        return newMethod;
    }
    

    
    // 添加返回语句
    private static void addReturnStatement(JimpleBody body, Type returnType, 
                                          List<Unit> path, Map<Unit, Unit> unitMapping) {
        Unit lastUnit = path.get(path.size() - 1);
        
        // 检查最后一条语句是否是返回语句
        if (!(lastUnit instanceof ReturnStmt || lastUnit instanceof ReturnVoidStmt)) {
            if (returnType instanceof VoidType) {
                body.getUnits().add(Jimple.v().newReturnVoidStmt());
            } else {
                // 根据返回类型返回默认值
                Value defaultValue = getDefaultValueForType(returnType);
                body.getUnits().add(Jimple.v().newReturnStmt(defaultValue));
            }
        }
    }
    
    // 获取类型的默认值
    private static Value getDefaultValueForType(Type type) {
        if (type instanceof PrimType) {
            if (type instanceof BooleanType) {
                return IntConstant.v(0);
            } else if (type instanceof ByteType) {
                return IntConstant.v(0);
            } else if (type instanceof CharType) {
                return IntConstant.v(0);
            } else if (type instanceof DoubleType) {
                return DoubleConstant.v(0);
            } else if (type instanceof FloatType) {
                return FloatConstant.v(0);
            } else if (type instanceof IntType) {
                return IntConstant.v(0);
            } else if (type instanceof LongType) {
                return LongConstant.v(0);
            } else if (type instanceof ShortType) {
                return IntConstant.v(0);
            }
        }
        return NullConstant.v();
    }
    
    // 重建控制流
    private static void rebuildControlFlow(JimpleBody body, List<Unit> path, Map<Unit, Unit> unitMapping) {
        Chain<Unit> units = body.getUnits();
        
        // 为跳转语句设置正确的目标
        for (Unit unit : units) {
            if (unit instanceof GotoStmt) {
                GotoStmt gotoStmt = (GotoStmt) unit;
                // 查找原始目标对应的新语句
                for (Map.Entry<Unit, Unit> entry : unitMapping.entrySet()) {
                    Unit originalTarget = entry.getKey();
                    if (originalTarget == gotoStmt.getTarget()) {
                        gotoStmt.setTarget(entry.getValue());
                        break;
                    }
                }
            } else if (unit instanceof IfStmt) {
                IfStmt ifStmt = (IfStmt) unit;
                // 查找原始目标对应的新语句
                for (Map.Entry<Unit, Unit> entry : unitMapping.entrySet()) {
                    Unit originalTarget = entry.getKey();
                    if (originalTarget == ifStmt.getTarget()) {
                        ifStmt.setTarget(entry.getValue());
                        break;
                    }
                }
            }
        }
        
        // 设置基本块
        setupBasicBlocks(body);
    }
    
    // 设置基本块
    private static void setupBasicBlocks(JimpleBody body) {
        // 创建一个简单的线性基本块
        // 在实际实现中，可能需要更复杂的控制流分析
//        List<Unit> units = new ArrayList<>(body.getUnits());
        
        // 清除现有的陷阱（trap）
        body.getTraps().clear();
        
        // 设置异常处理（如果有的话）
        // 这里简化处理，实际需要根据路径中的异常处理信息来设置
    }

    // 摘要路径：仅输出语句类型和所选分支编号，条目以分号分隔
    private static String summarizePath(List<Unit> path) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            Unit u = path.get(i);
            String type = u.getClass().getSimpleName().substring(1);
            String entry = "";
            if (u instanceof Stmt) {
                Stmt s = (Stmt) u;
                if (s instanceof IfStmt) {
                    int pos = i;
                    Unit next = (pos + 1 < path.size()) ? path.get(pos + 1) : null;
                    IfStmt ifs = (IfStmt) s;
                    // If: 1 表示 taken（跳转到 target），0 表示 not-taken（顺序执行）
                    String branchNum = (next != null && next == ifs.getTarget()) ? "1" : "0";
                    entry = type + ":" + branchNum;
                } else if (s instanceof LookupSwitchStmt) {
                    int pos = i;
                    Unit next = (pos + 1 < path.size()) ? path.get(pos + 1) : null;
                    LookupSwitchStmt ls = (LookupSwitchStmt) s;
                    List<Unit> targets = ls.getTargets();
                    int matchedIndex = -1;
                    for (int t = 0; t < targets.size(); t++) {
                        if (targets.get(t) == next) { matchedIndex = t; break; }
                    }
                    // matchedIndex stays -1 for default or no-match
                    entry = type + ":" + matchedIndex;
                } else if (s instanceof TableSwitchStmt) {
                    int pos = i;
                    Unit next = (pos + 1 < path.size()) ? path.get(pos + 1) : null;
                    TableSwitchStmt ts = (TableSwitchStmt) s;
                    List<Unit> targets = ts.getTargets();
                    int matchedIndex = -1;
                    for (int t = 0; t < targets.size(); t++) {
                        if (targets.get(t) == next) { matchedIndex = t; break; }
                    }
                    entry = type + ":" + matchedIndex;
                }
            }
            if (sb.length() > 0 && entry.length() > 0) sb.append(";");
            sb.append(entry);
        }
        return sb.toString();
    }
    
    // 主方法：处理所有符合条件的路径
    public static Map<SootMethod, List<Unit>> processAndWrapPaths(SootMethod method, int n) {
    	
    	Map<SootMethod, List<Unit>> methods = new HashMap<>();
    	StringPathExtractor extractor = new StringPathExtractor(method,n);
        // 提取路径
        List<List<Unit>> filteredPaths = extractor.allStringPaths();
        
        // System.out.println("Found " + filteredPaths.size() + " valid paths to wrap");
        
        // 包装每个符合条件的路径为新方法
        for (int i = 0; i < filteredPaths.size(); i++) {
        	List<Unit> pathInfo = filteredPaths.get(i);
//        	System.out.println("Wrapping path " + i + " as new method...");
            
            try {
                SootMethod newMethod = wrapPathAsNewMethod(pathInfo, method, i);
                
                // 验证新方法
                if (newMethod.getActiveBody() != null) {
//                    newMethod.getActiveBody().validate();
//                    System.out.println("Successfully created method: " + newMethod.getSignature());
                	methods.put(newMethod, pathInfo);
                    // 打印新方法体
//                    System.out.println("Method body:");
//                    for (Unit unit : newMethod.getActiveBody().getUnits()) {
//                        System.out.println("  " + unit);
//                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to wrap path " + i + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        return methods;
    }
    
    // 主函数示例
    public static void main(String[] args) {
        double common_startup = 0;
        double strict_time = 0;
        double justin_time = 0;
        long common_start = System.nanoTime();
        // Soot 初始化
        String classPath = "D:\\learning\\jars\\byproduct\\dataset_rev\\awesome\\fastcsv\\fastcsv-3.4.0.jar";
        if (args.length == 1) {
            classPath = args[0];
        }
        List<String> list = new ArrayList<>();
        if (System.getProperty("os.name").toLowerCase().contains("win"))
            list.add("C:\\Program Files\\Eclipse Adoptium\\jdk-8.0.345.1-hotspot\\jre");
        else if (System.getProperty("os.name").toLowerCase().contains("linux"))
            list.add("/usr/lib/jvm/java-8-openjdk-amd64/jre");
        list.add(classPath);
        G.reset();
        Options.v().set_prepend_classpath(true);
        Options.v().set_whole_program(true);
        Options.v().set_src_prec(Options.src_prec_class);
        Options.v().set_process_dir(list);
        Options.v().set_allow_phantom_refs(true);
		Options.v().set_output_format(Options.output_format_jimple);

		// Pack p1 = PackManager.v().getPack("jtp");
		// String phaseName = "jtp.bt";

		// Transform t1 = new Transform(phaseName, new BodyTransformer() {
		// 	@Override
		// 	protected void internalTransform(Body b, String phase, Map<String, String> options) {
		// 		try {
		// 			b.getMethod().setActiveBody(b);
		// 		} catch (Exception e) {
		// 			Log.e(e);
		// 		}
		// 	}
		// });

		// p1.add(t1);

		soot.Main.v().autoSetOptions();
		try {
			Scene.v().loadNecessaryClasses();
			PackManager.v().runPacks();
		} catch (Exception e) {
			Log.e(e);
		}
        
        // 写入文件：每个新方法的 regex 和 value
        String[] frags = classPath.split(File.separator.replace("\\", "\\\\"));;
        String name = frags[frags.length - 2];
        File base_dir = new File(classPath).getParentFile();
        File directory = new File(base_dir + "/smt");
        if (directory.exists()) {
            try {
                Files.walk(directory.toPath())
                        .sorted((a, b) -> -a.compareTo(b)) // 反向排序，先删除文件再删除目录
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            } catch (IOException ignored) {}
        }

        if (directory.mkdirs()) {
            System.out.println("Directory created successfully.");
        } else {
            System.out.println("Failed to create directory.");
        }
        System.out.println(directory);
        int cnt = 0;
        List<String> err_list = new ArrayList<>();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(base_dir + "/regex_values.txt"))) {
            // 加载类
            for (SootClass sc : Scene.v().getApplicationClasses()) {
                sc.setApplicationClass();
                // 获取目标方法
                for (SootMethod method : new ArrayList<>(sc.getMethods())) {
                    int loopCount = 1;
                    
                    // 处理并包装路径
                    try {
                        Map<SootMethod, List<Unit>> methods = processAndWrapPaths(method, loopCount);

                        // System.out.println("Paths in " + method.getSignature() + "  : # "+methods.size());


                        common_startup += (double)(System.nanoTime() - common_start) / 1e9;
                        for(Entry<SootMethod, List<Unit>> p:methods.entrySet()) {
                            long start = System.nanoTime();
                            SootMethod m = p.getKey();
                            List<Unit> path = p.getValue();
                            System.out.println(path);
                            transformAndOutputAt(method.getName(), method.getSignature(), m, directory, cnt);
                            cnt++;
                            strict_time += (double)(System.nanoTime() - start) / 1e9;
                            start = System.nanoTime();
                            String regex = null;
                            try {
                                JustinStrGenerator generator = new JustinStrGenerator();
                                regex = generator.generate(new ArrayList<>(m.getActiveBody().getUnits()));

                            } catch (Exception e) {
                                // 抽取的路径可能会有异常  暂时忽略
                                e.printStackTrace();
                //				 System.out.println(m.getActiveBody());
                                continue;
                            }

                            System.out.println("Regex: " + regex);
                            if(regex.equals(JustinStrGenerator.NO_STRING_CONSTRAINT)) {
                                String line = method.getDeclaringClass().getName() + "::" + method.getName() + "\n" +
                                        (cnt - 1) + ".smt2" +
                                    " \nNo-Regex: \nValue: " + "\n" + path.toString();
                                writer.write(line);
                                writer.newLine();
                                continue;
                            }
                            String value = "";
                            // [^\Q#\E]{4}(?!^\Qclass\E$)[^\Q#\E]*
                //        	regex = regex.replace("^", "");
                //        	regex = '^' + regex;
                //        	regex = regex.replace("?!", "");
                            try {
                                RgxGenProperties properties = new RgxGenProperties();
                                properties.put("generation.infinite.repeat", String.valueOf(GlobalCons.STRING_MAX_LENGTH));
                                RgxGen rgxGen = new RgxGen(regex);
                                rgxGen.setProperties(properties);
                                String rgxString = rgxGen.generate();
                //                value = rgxString.replaceAll(GenerationEnum.SPECIAL_REGEX_CHARS.getValue(), "" );
                                value = rgxString.replaceAll(GenerationEnum.SPECIAL_REGEX_CHARS.getValue(), "" );
                                if (value.length() > GlobalCons.STRING_MAX_LENGTH) {
                                    value = value.substring(0, GlobalCons.STRING_MAX_LENGTH);
                                }
                            } catch (Exception | Error e){
                                Log.e("RgxGen exception");
                                e.printStackTrace();
                //                Log.e(regex);
                            }

                            value = value.replace("\\", "\\\\");
                            value = value.replace("\"", "\\\"");
                            System.out.println("String Value: " + value);

                            String line = method.getDeclaringClass().getName() + "::" + method.getName() + "\n" +
                                    (cnt - 1) + ".smt2" +
                                " \nRegex: " + regex + "\nValue: " + value + "\n" + path.toString();//value;
                            // if (method.getName().contains("findHeaderIndex")) line = line + method.getActiveBody().toString();
                            writer.write(line);
                            writer.newLine();
                            justin_time += (double)(System.nanoTime() - start) / 1e9;
                        }
                    } catch (Error e) {
                        err_list.add(method.toString() + ": " + e);
                    }
                    common_start = System.nanoTime();
                }
            }
            Files.writeString(Paths.get(base_dir + "/statistics.json"), "{ \"justin\": " +
                    justin_time + ",\n \"strict\": " +
                    strict_time + ",\n \"common start\": " +
                    common_startup + ",\n \"error methods\": \"" +
                    err_list + " \"}");

        } catch (IOException e) {
            Log.e(e);
        }
    }
}