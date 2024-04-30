
import sys
import json
import matplotlib.pyplot as plt

directoryPath = sys.argv[1] if len(sys.argv) > 1 else "D:/IdeaProjects/paths/gson"
statistics = json.loads(open(directoryPath).read())

# suspicious = [dot for dot in statistics if dot['is_sat'] == 'sat' and 'deviant' in dot["filename"]]
# for s in suspicious:
#     print(s["filename"])

num = list(range(len(statistics)))
zruntime = [dot['z3_run_time'] for dot in statistics]
cruntime = [dot['cvc5_run_time'] for dot in statistics]
zcolors = ['blue' if (dot['z3_is_sat'] == 'sat') else 'purple' if (dot['z3_is_sat'] == 'unsat') else 'grey' for dot in statistics]
ccolors = ['green' if (dot['cvc5_is_sat'] == 'sat') else 'red' if (dot['cvc5_is_sat'] == 'unsat') else 'black' for dot in statistics]

# i = 0
# def get_EUF_count(filename):
#     global i
#     i += 1
#     print(f"\r{i}", end='')
#     with open(filename, 'r') as f:
#         return f.read().count("declare-fun")
# z = [get_EUF_count(dot['filename']) for dot in statistics]
z = [dot['undefined_function_count'] for dot in statistics]

print(f"z3: {sum(zruntime) / len(zruntime)}")
print(f"cvc5: {sum(cruntime) / len(cruntime)}")
# print([dot["filename"] for dot in statistics if 'deviant' in dot["filename"] and dot["z3_is_sat"] == 'sat'][:10])

print("cvc5 / z3 diff:")
print([f"z3 result: {dot['z3_is_sat']}, cvc5 result: {dot['cvc5_is_sat']}" for dot in statistics if dot['cvc5_is_sat'] != dot['z3_is_sat']])

# Plotting the dots
plt.scatter(num, cruntime, c=ccolors)
plt.scatter(num, zruntime, c=zcolors)
# plt.xscale('linear')
# plt.xlim(0, 10)
plt.xlabel('file number')
plt.ylabel('solve time (seconds)')
plt.title('Dots Plot')
plt.show()
