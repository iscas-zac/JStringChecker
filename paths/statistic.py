import sys
import glob
import subprocess
import psutil
import time
import filelock
import multiprocessing
from ctypes import c_char_p
import json

# foreach ($f in "C:\Users\yyzha\Desktop\jars\antlr\", "C:\Users\yyzha\Desktop\jars\choco\", "C:\Users\yyzha\Desktop\jars\commons-io\", "C:\Users\yyzha\Desktop\jars\commons-lang\", "C:\Users\yyzha\Desktop\jars\commons-logging\", "C:\Users\yyzha\Desktop\jars\efficient-boot-common\", "C:\Users\yyzha\Desktop\jars\fastjson\", "C:\Users\yyzha\Desktop\jars\freemarker\", "C:\Users\yyzha\Desktop\jars\gson\", "C:\Users\yyzha\Desktop\jars\handlebars\", "C:\Users\yyzha\Desktop\jars\httpclient\", "C:\Users\yyzha\Desktop\jars\hutool\", "C:\Users\yyzha\Desktop\jars\javacc\", "C:\Users\yyzha\Desktop\jars\junit\", "C:\Users\yyzha\Desktop\jars\lombok\", "C:\Users\yyzha\Desktop\jars\mybatis\", "C:\Users\yyzha\Desktop\jars\okhttp\", "C:\Users\yyzha\Desktop\jars\slf4j\", "C:\Users\yyzha\Desktop\jars\tomcat\") { & python D:\IdeaProjects\test_native_build\paths\statistic.py $f }
directoryPath = sys.argv[1] if len(sys.argv) > 1 else "D:/IdeaProjects/paths/gson"
smt2Files = glob.glob(directoryPath + "/**/*.path", recursive=True)
timeout = 10
totalCount = len(smt2Files)

with open(directoryPath + f'/simple_statistics.json', 'w') as f:
    f.truncate(0)
lock = filelock.FileLock(directoryPath + f'/simple_statistics.lock')
def get_statistics(file, cnt, validCount, invalidCount, slowest, slowestFile, json_array):
    print(f"\r{cnt.value}/{totalCount} {file}", end="")
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
            json_array_item[solver] = {}
            try:
                process = psutil.Process(p.pid)
                json_array_item[solver]["memory"] = process.memory_info().rss
                for _ in range(timeout * 100):
                    if p.poll() is None:
                        try:
                            json_array_item[solver]["memory"] = max(json_array_item[solver]["memory"], process.memory_info().rss)
                        except psutil.NoSuchProcess:
                            if json_array_item[solver]["memory"] == None:
                                json_array_item[solver]["memory"] = 0
                        time.sleep(0.01)
                    else: break
                else:
                    raise subprocess.TimeoutExpired(cmd, 10)
            except psutil.NoSuchProcess:
                if "memory" not in json_array_item[solver]:
                    json_array_item[solver]["memory"] = 0
            out = p.stdout.read()
            err = p.stderr.read()
            try:
                result = out.decode("utf-8", errors="ignore").splitlines()[0] if p.stdout else ""
            except:
                result = ""
        except subprocess.TimeoutExpired:
            result = "timeout"
        finally:
            p.kill()
        elapsedTime = time.time() - startTime
        if len(err) > 0:
            print("\033[31m", end='') # red color
            print(err.decode("utf-8", errors="ignore"))
            print("\033[92m", end='') # green color
            print(out.decode("utf-8", errors="ignore"))
            print("\033[0m", end='') # reset color
            print(file)
            invalidCount.value += 1
            print(f"\nInvalid file for {solver}: {file}")
            err = err.decode("utf-8", errors="ignore")
        # error pattern for each solver
        elif any(error_pattern(line)
                for line in out.splitlines()):
            invalidCount.value += 1
            err = '\n'.join([line.decode("utf-8", errors="ignore") for line in out.splitlines() if error_pattern(line)])
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
        json_array_item["assertion_count"] = sum(1 for line in lines if line.startswith('(assert'))
        json_array_item["api_sequence"] = [line for line in lines if line.startswith(';seq')][0].removeprefix(';seq ').split(';\t')
        json_array_item["api_count"] = json.loads([line for line in lines if line.startswith(';cnt')][0].removeprefix(';cnt '))
        json_array_item["jimple_statements"] = [line for line in lines if line.startswith(';stmts')][0].removeprefix(';stmts ').split(';\t')

    json_array.append(json_array_item)
    cnt.value += 1

    if cnt.value % 100 == 0:
        with lock.acquire():
            with open(directoryPath + f'/simple_statistics.json', 'r') as f:
                try:
                    json_array.extend(json.load(f))
                except json.decoder.JSONDecodeError:
                    pass
            with open(directoryPath + f'/simple_statistics.json', 'w') as f:
                json.dump(list(json_array), f, indent=4)
                json_array = []


def run_file(vars):
    try:
        (f, cnt, validCount, invalidCount, slowest, slowestFile, json_array) = vars
        get_statistics(f, cnt, validCount, invalidCount, slowest, slowestFile, json_array)
    except KeyboardInterrupt:
        pass

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

