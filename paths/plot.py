
import sys
import json
import matplotlib.pyplot as plt
import glob

directoryPath = sys.argv[1] if len(sys.argv) > 1 else "D:/IdeaProjects/paths/gson"
arr = []
comp = []
questions = []
for folder in glob.glob(directoryPath + "/*/"):
    try:
        file = folder + "simple_statistics.json"
        with open(file, "r") as f:
            content = json.load(f)
            # print('"' + folder + '",', end=" ")
            print(folder.split("\\")[-2] + f' & {len(content)} & {len([item for item in content if item["z3"]["stderr"] != "" or item["cvc5"]["stderr"] != ""])} \\\\')
    except:
        pass
for folder in glob.glob(directoryPath + "/*/"):
    try:
        file = folder + "simple_statistics.json"
        # print(file)
        with open(file, "r") as f:
            content = json.load(f)
            # questions += [item for item in content if not os.path.exists(item["filename"])]
            # content = [item for item in content if os.path.exists(item["filename"])]
            for item in content:
                item['filename'] = (folder + item['filename']).replace("\\", "/")
            a = [item for item in content if item["undefined_function_count"] < 1 and len(item["api_count"]) > 0]
            print(folder.split("\\")[-2] + f' & {len(content)} & {len(a)} \\\\')
            comp += content
            arr += a
    except Exception as e:
        print(e)
with open(directoryPath + "/simple_statistics.json", "w") as f:
    json.dump(arr, f, indent=4)

def contains(filename, item):
    with open(filename, 'r') as f:
        return item in f.read()
neq = [f'filename: {dot["filename"]}, z3: {dot["z3"]["is_sat"]}, cvc5: {dot["cvc5"]["is_sat"]}' for dot in arr if dot["z3"]["is_sat"] != dot["cvc5"]["is_sat"]]
# neq = [f'filename: {dot["filename"]}, z3: {dot["z3"]["is_sat"]}, cvc5: {dot["cvc5"]["is_sat"]}' for dot in comp if (not contains(dot["filename"], "fun-rec") and not contains(dot["filename"], "replace_all"))
#        and (dot["z3"]["is_sat"] not in ['sat', 'unsat'] or dot["cvc5"]["is_sat"] not in ['sat', 'unsat'])]
# print(f"{neq[:100]=}")
print(len(arr))
print(len(comp))
labels = ['cvc5', 'z3', 'mathsat', 'ostrich']
# arr = comp
print(len(neq))
print(f'{len(neq)}/{len(arr)}')

a = {}
for item in comp:
    for k in item["api_count"]:
        if k in a:
            a[k] += item["api_count"][k]
        else:
            a[k] = item["api_count"][k]
a = [i for i in a if not ("String:" in i or "StringBuffer:" in i or "StringBuilder:" in i)]
print(len(a))
print(a)

# arr = [item for item in comp if all(item[solver]["stderr"] == '' for solver in labels)]
# print(len(questions))
# print([f'{q['filename']} err: {q["z3"]["stderr"]}, {q["cvc5"]["stderr"]}' for q in questions[:100]])
err = [f'{dot["filename"]}: z3: {dot["z3"]["stderr"]}, cvc5: {dot["cvc5"]["stderr"]}' for dot in comp if dot['z3']["stderr"] != "" or dot['cvc5']["stderr"] != ""]
print('\n'.join(err[:10]))

# Sample 1D data
runtimes = {label: [dot[label]['run_time'] for dot in arr] for label in labels}
memorys = {label: [dot[label]['memory'] / 1024**2 for dot in arr] for label in labels}
print("\n".join(f"{item['filename'].split('/')[6]}/{item['filename'].split('/')[-1].strip('.smt2')} & {item['z3']['run_time']:.3f} & {item['z3']['is_sat']} & {item['cvc5']['run_time']:.3f} & {item['cvc5']['is_sat']} \\\\" for item in arr if item["z3"]["run_time"] > 1 or item["cvc5"]["run_time"] > 1 or item["z3"]["is_sat"] not in ["sat", "unsat"] or item["cvc5"]["is_sat"] not in ["sat", "unsat"]))
# print("\n".join(f"{item['filename']} & {item['z3']['run_time']:.2f} & {item['z3']['is_sat']} & {item['cvc5']['run_time']:.2f} & {item['cvc5']['is_sat']} \\\\" for item in arr if item["z3"]["run_time"] > 1 or (item["cvc5"]["run_time"] > 1 and not contains(item["filename"], 'fun-rec'))))

print("\n".join(f"{solver} time < 0.05: {sum(c < 0.05 for c in runtimes[solver])}" for solver in labels))
print("\n".join(f"{solver} memory average: {sum(memorys[solver]) / len(memorys[solver])}" for solver in labels))

print("\n".join(f"{label} stderr: {len([dot[label]["stderr"] for dot in arr if dot[label]["stderr"] != ""])}" for label in labels))

# print([dot["filename"] for dot in comp if contains(dot["filename"], "charAt") and contains(dot["filename"], "cast-") and contains(dot["filename"], "")][:100])

# Create a histogram
plt.hist([runtimes[solver] for solver in labels], bins=50, label=labels)
plt.xlim(0, 4)
# plt.ylim(0, 5)
plt.legend()
# Add labels and title
plt.xlabel('run time (seconds)')
plt.ylabel('number of scripts')
plt.title('Solution time of solvers on the full dataset')

# Display the histogram
plt.show()

# Create a histogram
plt.hist([memorys[solver] for solver in labels], bins=50, label=labels)
plt.xlim(0, 200)
# plt.ylim(0, 5)
plt.legend()
# Add labels and title
plt.xlabel('Maximum memory usage (MB)')
plt.ylabel('number of scripts')
plt.title('Maximum memory usage of solvers on the full dataset')

# Display the histogram
plt.show()