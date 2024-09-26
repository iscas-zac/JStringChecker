import sys
import glob
import subprocess
import psutil
import time
import filelock
import multiprocessing
from ctypes import c_char_p
import json

# foreach ($f in "C:\Users\yyzha\Desktop\jars\dataset\antlr\", "C:\Users\yyzha\Desktop\jars\dataset\commons-io\", "C:\Users\yyzha\Desktop\jars\dataset\commons-lang\", "C:\Users\yyzha\Desktop\jars\dataset\commons-logging\", "C:\Users\yyzha\Desktop\jars\dataset\efficient-boot-common\", "C:\Users\yyzha\Desktop\jars\dataset\fastjson\", "C:\Users\yyzha\Desktop\jars\dataset\freemarker\", "C:\Users\yyzha\Desktop\jars\dataset\gson\", "C:\Users\yyzha\Desktop\jars\dataset\handlebars\", "C:\Users\yyzha\Desktop\jars\dataset\httpclient\", "C:\Users\yyzha\Desktop\jars\dataset\hutool\", "C:\Users\yyzha\Desktop\jars\dataset\javacc\", "C:\Users\yyzha\Desktop\jars\dataset\junit\", "C:\Users\yyzha\Desktop\jars\dataset\lombok\", "C:\Users\yyzha\Desktop\jars\dataset\mybatis\", "C:\Users\yyzha\Desktop\jars\dataset\okhttp\", "C:\Users\yyzha\Desktop\jars\dataset\slf4j\", "C:\Users\yyzha\Desktop\jars\dataset\tomcat\", "C:\Users\yyzha\Desktop\jars\dataset\junit-jupiter-api\", "C:\Users\yyzha\Desktop\jars\dataset\StringTemplate\", "C:\Users\yyzha\Desktop\jars\dataset\log4j\") { & python D:\IdeaProjects\test_native_build\paths\statistic.py $f }
#for subfolder in /path/to/root_folder/*; do echo "Processing folder: $subfolder"; [ -d "$subfolder" ] && ls "$subfolder"; done
directoryPath = sys.argv[1] if len(sys.argv) > 1 else "D:/IdeaProjects/paths/gson/"
if directoryPath[-1] != '\\':
    directoryPath += '\\'
smt2Files = glob.glob("**/*.smt2", root_dir=directoryPath, recursive=True)
timeout = 60
totalCount = len(smt2Files)

with open(directoryPath + f'/simple_statistics.json', 'w') as f:
    f.truncate(0)
lock = filelock.FileLock(directoryPath + f'/simple_statistics.lock')

import os
def get_statistics(file: str, cnt, validCount, invalidCount, slowest, slowestFile, json_array):
    print(f"\r{cnt.value}/{totalCount} {file}", end="")
    json_array_item = {"filename": file}
    file = directoryPath + file
    for cmd, solver, error_pattern in [
        (["z3", file], "z3", lambda line: b"(error \"line" in line and b"unsat core" not in line and b"model is not available" not in line),
        ([R"D:\learning\jars\cvc5.exe", file], "cvc5", lambda line: b"(error \"Parse Error" in line),
        ([R"D:\learning\z3-noodler\build\z3.exe", file], "z3-noodler", lambda line: b"(error \"line" in line and b"unsat core" not in line and b"model is not available" not in line),
        # ([R"D:\learning\mathsat-5.6.10-win64-msvc\bin\mathsat.exe", file], "mathsat", lambda line: b"(error" in line and b"(error \"no unsat" not in line and b"(error \"model" not in line),
        (["java", "-Xss20000k", "-Xmx2000m", "-cp", R"D:\learning\ostrich\target\scala-2.11\ostrich-assembly-1.3.5.jar", "ostrich.OstrichMain", "+incremental", file], "ostrich", lambda line: line == b"error" or (b"(error" in line and b"(error \"no unsat" not in line and b"(error \"no model" not in line)),
    ]:
        startTime = time.time()
        out = b""
        err = b""
        try:
            p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            json_array_item[solver] = {}
            try:
                process = psutil.Process(p.pid)
                json_array_item[solver]["memory"] = process.memory_info().rss
                while True:
                    if p.poll() is None:
                        try:
                            json_array_item[solver]["memory"] = max(json_array_item[solver]["memory"], process.memory_info().rss)
                        except psutil.NoSuchProcess:
                            if json_array_item[solver]["memory"] == None:
                                json_array_item[solver]["memory"] = 0
                        if time.time() - startTime > timeout:
                            raise subprocess.TimeoutExpired(cmd, timeout)
                        time.sleep(0.01)
                    else: break
            except psutil.NoSuchProcess:
                if "memory" not in json_array_item[solver]:
                    json_array_item[solver]["memory"] = 0
            out = p.stdout.read()
            err = p.stderr.read()
            try:
                result = [line for line in out.decode("utf-8", errors="ignore").splitlines() if not line.startswith("(error")][0] if p.stdout else ""
            except IndexError:
                result = ""
        except subprocess.TimeoutExpired:
            result = "timeout"
        finally:
            p.kill()
        elapsedTime = time.time() - startTime
        if len(err) > 0 and solver != "ostrich": # ostrich always print debug info to the stderr, leave check to later
            print("\033[31m", end='') # red color
            print(err.decode("utf-8", errors="ignore"))
            print("\033[92m", end='') # green color
            print(out.decode("utf-8", errors="ignore"))
            print("\033[0m", end='') # reset color
            print(file)
            invalidCount.value += 1
            print(f"\nInvalid file for {solver}: {file}")
            err = err.decode("utf-8", errors="ignore")
            result = "error"
        # error pattern for each solver
        elif any(error_pattern(line)
                for line in out.splitlines()):
            invalidCount.value += 1
            if solver == "ostrich" and len(err) > 0: # if output is 'error' and stderr is not empty, reserve ostrich error
                err = err.decode("utf-8", errors="ignore")
            else:
                err = '\n'.join(line.decode("utf-8", errors="ignore") for line in out.splitlines() if error_pattern(line))
            result = "error"
            print("\033[31m", end='') # red color
            print(err)
            print("\033[0m", end='') # reset color
            print(file)
            print(f"\nInvalid file for {solver}: {file}")
        else:
            validCount.value += 1
            err = ""
        if elapsedTime > slowest.value:
            slowest.value = elapsedTime
            slowestFile.value = file

        json_array_item[solver]["run_time"] = elapsedTime
        json_array_item[solver]["is_sat"] = result
        json_array_item[solver]["stdout"] = out.decode("utf-8", errors="ignore")
        json_array_item[solver]["stderr"] = err
        
    with open(file, 'r') as f:
        content = f.read()
        lines = content.splitlines()
        json_array_item["undefined_function_count"] = content.count("declare-fun")
        try:
            json_array_item["assertion_count"] = sum(1 for line in lines if line.startswith('(assert'))
            json_array_item["api_sequence"] = [line for line in lines if line.startswith(';seq')][0].removeprefix(';seq ').split(';\t')
            json_array_item["api_count"] = json.loads([line for line in lines if line.startswith(';cnt')][0].removeprefix(';cnt '))
            json_array_item["jimple_statements"] = [line for line in lines if line.startswith(';stmts')][0].removeprefix(';stmts ').split(';\t')
        except IndexError as e:
            print("\033[31m", end='') # red color
            print(f"{e} in processing {file}")
            print("\033[0m", end='') # reset color

    with lock.acquire():
        json_array.append(json_array_item)
        cnt.value += 1

        if cnt.value % 1000 == 0:
            with open(directoryPath + f'/simple_statistics.json', 'r') as f:
                try:
                    json_array.extend(json.load(f))
                except json.decoder.JSONDecodeError:
                    pass
            with open(directoryPath + f'/simple_statistics.json', 'w') as f:
                json.dump(list(json_array), f, indent=4)
                json_array[:] = []


def run_file(vars):
    try:
        (f, cnt, validCount, invalidCount, slowest, slowestFile, json_array) = vars
        get_statistics(f, cnt, validCount, invalidCount, slowest, slowestFile, json_array)
    except KeyboardInterrupt:
        raise

if __name__ == '__main__':
    multiprocessing.freeze_support()
    manager = multiprocessing.Manager()
    validCount = manager.Value('i', 0)
    invalidCount = manager.Value('i', 0)
    slowest = manager.Value('d', 0.0)
    slowestFile = manager.Value(c_char_p, "")
    cnt = manager.Value('i', 0)
    json_array = manager.list()

    try:
        with multiprocessing.Pool() as pool:
            pool.map(run_file, [(f, cnt, validCount, invalidCount, slowest, slowestFile, json_array) for f in smt2Files])
    except KeyboardInterrupt:
        pool.terminate()
        pool.close()
        pool.join()
        
    with open(directoryPath + f'/simple_statistics.json', 'r') as f:
        try:
            json_array.extend(json.load(f))
        except json.decoder.JSONDecodeError:
            pass
    with open(directoryPath + f'/simple_statistics.json', 'w') as f:
        json.dump(list(json_array), f, indent=4)
        
    print(f"\nTotal valid SMT2 files: {validCount.value}")
    print(f"Total invalid SMT2 files: {invalidCount.value}")
    print(f"Slowest file: {slowestFile.value} with time {slowest.value:.3f} seconds")



''' solution: add freeze_support()
RuntimeError:
        An attempt has been made to start a new process before the
        current process has finished its bootstrapping phase.

        This probably means that you are not using fork to start your
        child processes and you have forgotten to use the proper idiom
        in the main module:

            if __name__ == '__main__':
                freeze_support()
                ...

        The "freeze_support()" line can be omitted if the program
        is not going to be frozen to produce an executable.
'''

''' solution: add one more catch to the psutil.Process
Traceback (most recent call last):
  File "D:\Programs\Scoop\apps\miniconda3\current\lib\threading.py", line 1016, in _bootstrap_inner
    self.run()
  File "D:\Programs\Scoop\apps\miniconda3\current\lib\threading.py", line 953, in run
    self._target(*self._args, **self._kwargs)
  File "D:\Programs\Scoop\apps\miniconda3\current\lib\multiprocessing\pool.py", line 579, in _handle_results
    task = get()
  File "D:\Programs\Scoop\apps\miniconda3\current\lib\multiprocessing\connection.py", line 251, in recv
    return _ForkingPickler.loads(buf.getbuffer())
TypeError: NoSuchProcess.__init__() missing 1 required positional argument: 'pid'
'''

