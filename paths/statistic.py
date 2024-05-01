
import sys
import glob
import subprocess
import psutil
import time

directoryPath = sys.argv[1] if len(sys.argv) > 1 else "D:/IdeaProjects/paths/gson"
smt2Files = glob.glob(directoryPath + "/**/*.path", recursive=True)
# smt2Files = [ directoryPath + "/11.path", directoryPath + "/22.path" ]
validCount = 0
invalidCount = 0
slowest = 0.0
slowestFile = ""

totalCount = len(smt2Files)
cnt = 0
import json

with open(directoryPath + f'/simple_statistics.json', 'w') as f:
    f.truncate(0)
json_array = []
for file in smt2Files:
    print(f"\r{cnt}/{totalCount} {file}", end="")
    json_array_item = {"filename": file}
    for cmd, solver, error_pattern in [ #TODO: move error pattern to here
        (["z3", file], "z3", lambda line: b"(error \"line" in line and b"unsat core" not in line and b"model is not available" not in line),
        ([r"C:\Users\yyzha\Desktop\jars\cvc5.exe", file], "cvc5", lambda line: b"(error \"Parse Error" in line)
    ]:
        startTime = time.time()
        out = b""
        err = b""
        try:
            p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            process = psutil.Process(p.pid)
            json_array_item[solver + "_memory"] = process.memory_info().rss
            for _ in range(5000): ## rough timeout
                if p.poll() is None:
                    try:
                        json_array_item[solver + "_memory"] = max(json_array_item[solver + "_memory"], process.memory_info().rss)
                    except psutil.NoSuchProcess:
                        if json_array_item[solver + "_memory"] == None:
                            json_array_item[solver + "_memory"] = 0
                    time.sleep(0.01)
                else: break
            else:
                raise subprocess.TimeoutExpired(cmd, 10)
            out = p.stdout.read()
            err = p.stderr.read()
            result = out.decode("utf-8", errors="ignore").splitlines()[0] if p.stdout else ""
        except subprocess.TimeoutExpired:
            result = "timeout"
        finally:
            p.kill()
        elapsedTime = time.time() - startTime
        if len(err) > 0:
            print("\033[31m", end='') # red color
            print(err.decode("utf-8", errors="ignore"), end='')
            print("\033[92m", end='') # green color
            print(out.decode("utf-8", errors="ignore"), end='')
            print("\033[0m", end='') # reset color
            print(file)
            invalidCount += 1
            print(f"\nInvalid file for {solver}: {file}")
            err = err.decode("utf-8", errors="ignore")
        # error pattern for each solver
        elif any(error_pattern(line)
                for line in out.splitlines()):
            invalidCount += 1
            print(f"\nInvalid file for {solver}: {file}")
            err = out.splitlines()[0].decode("utf-8", errors="ignore")
        else:
            validCount += 1
            err = ""
        if elapsedTime > slowest:
            slowest = elapsedTime
            slowestFile = file

        json_array_item[solver + "run_time"] = elapsedTime
        json_array_item[solver + "_is_sat"] = result
        json_array_item[solver + "_stdout"] = out.decode("utf-8", errors="ignore")
        json_array_item[solver + "_stderr"] = err
        

    with open(file, 'r') as f:
        content = f.read()
        lines = content.splitlines()
        json_array_item["undefined_function_count"] = content.count("declare-fun")
        json_array_item["assertion_count"] = sum(1 for line in lines if line.startswith('(assert'))
        json_array_item["api_sequence"] = [line for line in lines if line.startswith(';seq')][0].removeprefix(';seq ').split(';\t')
        json_array_item["api_count"] = json.loads([line for line in lines if line.startswith(';cnt')][0].removeprefix(';cnt '))
        json_array_item["jimple_statements"] = [line for line in lines if line.startswith(';stmts')][0].removeprefix(';stmts ').split(';\t')

    json_array.append(json_array_item)
    cnt += 1
    if cnt % 100 == 0:
        with open(directoryPath + f'/simple_statistics.json', 'a') as f:
            json.dump(json_array, f, indent=4)
            json_array = []
    
print(f"\nTotal valid SMT2 files: {validCount}")
print(f"Total invalid SMT2 files: {invalidCount}")
print(f"Slowest file: {slowestFile} with time {slowest:.3f} seconds")
