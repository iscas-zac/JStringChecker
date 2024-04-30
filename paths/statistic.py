
import sys
import glob
import subprocess
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

json_array = []
for file in smt2Files:
    print(f"\r{cnt}/{totalCount} {file}", end="")
    startTime = time.time()
    try:
        p = subprocess.run(["z3", file], stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=10)
        z3_result = p.stdout.decode("utf-8").splitlines()[0] if p.stdout else ""
    except subprocess.TimeoutExpired:
        z3_result = "timeout"
    elapsedTime = time.time() - startTime
    z3_err = ""
    if p.stderr is not None and len(p.stderr) > 0:
        print("\033[31m", end='') # red color
        print(p.stderr.decode("utf-8"), end='')
        print("\033[92m", end='') # green color
        print(p.stdout.decode("utf-8"), end='')
        print("\033[0m", end='') # reset color
        print(file)
        z3_err = p.stderr.decode("utf-8")
    elif any(b"(error \"line" in line and b"unsat core" not in line and b"model is not available" not in line
            for line in p.stdout.splitlines()):
        invalidCount += 1
        print(f"Invalid file for z3: {file}")
        z3_err = p.stdout.splitlines()[0]
    else:
        validCount += 1
    if elapsedTime > slowest:
        slowest = elapsedTime
        slowestFile = file

    startTime = time.time()
    try:
        p_cvc = subprocess.run([r"C:\Users\yyzha\Desktop\jars\cvc5.exe", file], stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=10)
        cvc5_result = p.stdout.decode("utf-8").splitlines()[0] if p.stdout else ""
    except subprocess.TimeoutExpired:
        cvc5_result = "timeout"
    elapsedTime_cvc5 = time.time() - startTime
    cvc5_err = ""
    if p_cvc.stderr is not None and len(p.stderr) > 0:
        print("\033[31m", end='') # red color
        print(p_cvc.stderr.decode("utf-8"), end='')
        print("\033[92m", end='') # green color
        print(p_cvc.stdout.decode("utf-8"), end='')
        print("\033[0m", end='') # reset color
        print(file)
        cvc5_err = p_cvc.stderr.decode("utf-8")
    elif any(b"(error \"Parse Error" in line
            for line in p_cvc.stdout.splitlines()):
        invalidCount += 1
        print(f"Invalid file for cvc5: {file}")
        cvc5_err = p_cvc.stdout.splitlines()[0]
    else:
        validCount += 1

    euf = 0
    constraints = 0
    with open(file, 'r') as f:
        euf = f.read().count("declare-fun")
        constraints = sum(1 for line in f.readlines() if line.startswith('(assert'))

    try:
        json_array.append({
            "filename": file,
            "z3_run_time": elapsedTime,
            "cvc5_run_time": elapsedTime_cvc5,
            "assertion_count": constraints,
            "z3_is_sat": z3_result,
            "z3_stdout": p.stdout.decode("utf-8"),
            "z3_stderr": z3_err,
            "cvc5_is_sat": cvc5_result,
            "cvc5_stdout": p_cvc.stdout.decode("utf-8"),
            "cvc5_stderr": cvc5_err,
            "undefined_function_count": euf
        })
    except UnicodeDecodeError:
        pass
    cnt += 1
with open(directoryPath + f'/simple_statistics.json', 'w') as f:
    json.dump(json_array, f, indent=4)
    
print(f"\nTotal valid SMT2 files: {validCount}")
print(f"Total invalid SMT2 files: {invalidCount}")
print(f"Slowest file: {slowestFile} with time {slowest:.3f} seconds")