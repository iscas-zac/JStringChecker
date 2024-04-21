
import sys
import json
import matplotlib.pyplot as plt

directoryPath = sys.argv[1] if len(sys.argv) > 1 else "D:/IdeaProjects/paths/gson"
statistics = json.loads(open(directoryPath).read())

# suspicious = [dot for dot in statistics if dot['is_sat'] == 'sat' and 'deviant' in dot["filename"]]
# for s in suspicious:
#     print(s["filename"])

x = [dot['run_time'] for dot in statistics]
y = [int(dot['is_sat'] == 'unsat') for dot in statistics]
colors = ['green' if dot['is_sat'] == 'sat' else 'red' if dot['is_sat'] == 'unsat' else 'black' for dot in statistics]

# i = 0
# def get_EUF_count(filename):
#     global i
#     i += 1
#     print(f"\r{i}", end='')
#     with open(filename, 'r') as f:
#         return f.read().count("declare-fun")
# z = [get_EUF_count(dot['filename']) for dot in statistics]
z = [dot['euf_count'] for dot in statistics]

print(sum(x) / len(x))

# Plotting the dots
plt.scatter(x, z, c=colors)
# plt.xscale('linear')
# plt.xlim(0, 10)
plt.xlabel('z3 solve time (seconds)')
plt.ylabel('undefined function count')
plt.title('Dots Plot')
plt.show()
