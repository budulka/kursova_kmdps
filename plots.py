import csv, sys, random
from collections import defaultdict
import numpy as np
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

d, out = sys.argv[1], sys.argv[2]
plt.rcParams.update({
    "font.family": "DejaVu Serif", "font.size": 12, "axes.spines.top": False, "axes.spines.right": False,
    "axes.edgecolor": "#444", "xtick.color": "#555", "ytick.color": "#555", "axes.grid": True,
    "grid.color": "#e3e3e3", "savefig.dpi": 200, "savefig.bbox": "tight",
})
ACC = "#e8673a"
BLUES = ["#1f5fb0", "#2a7ad6", "#5a9be3", "#8dbbee", "#bcd6f4"]
rows = lambda f: list(csv.DictReader(open(f"{d}/{f}")))

# 4.1 перехідний період
r = rows("transient.csv"); t = [float(x["time"]) for x in r]
fig, ax = plt.subplots(figsize=(7, 4.2))
for s in range(5):
    ax.plot(t, [float(x[f"seed{s}"]) for x in r], "-o", ms=3.5, lw=2, color=BLUES[s], label=f"прогін {s+1}")
ax.set_xscale("log"); ax.set_xlabel("Час моделювання, с"); ax.set_ylabel("Середній час очікування, с")
ax.legend(ncol=5, loc="lower center", bbox_to_anchor=(0.5, 1.0), frameon=False, fontsize=10)
fig.savefig(f"{out}/fig4_1.png"); plt.close(fig)

# 4.1 б: накопичене середнє в лінійній шкалі (крок 250 с)
tw = defaultdict(list)
for x in rows("transient_windows.csv"):
    tw[int(x["seed"])].append((float(x["time"]), int(x["arrivals"]), float(x["totalWait"]), float(x["cumulativeW"])))
fig, ax = plt.subplots(figsize=(7, 4.2))
for s in range(5):
    ax.plot([0] + [v[0] for v in tw[s]], [0] + [v[3] for v in tw[s]], lw=1.6, color=BLUES[s], label=f"прогін {s+1}")
ax.set_xlim(0, 100_000); ax.set_ylim(0, None)
ax.xaxis.set_major_formatter(matplotlib.ticker.FuncFormatter(lambda v, _: f"{int(v):,}".replace(",", " ")))
ax.set_xlabel("Час моделювання, с"); ax.set_ylabel("Середній час очікування, с")
ax.legend(ncol=5, loc="lower center", bbox_to_anchor=(0.5, 1.0), frameon=False, fontsize=10)
fig.savefig(f"{out}/fig4_1b.png"); plt.close(fig)

# 4.1 в: середнє за інтервалами 250 с (середнє 5 прогонів), перші 10 000 с
win = defaultdict(list)
for s, v in tw.items():
    pn, pw = 0, 0.0
    for t, n, w, _ in v:
        win[t].append((w - pw) / (n - pn)); pn, pw = n, w
ts = sorted(t for t in win if t <= 10_000)
steady = np.mean([np.mean(win[t]) for t in win if t > 2000])
fig, ax = plt.subplots(figsize=(7, 3.8))
ax.plot([t - 125 for t in ts], [np.mean(win[t]) for t in ts], "-o", ms=3.5, lw=1.8, color="#2a7ad6", label="середнє за інтервал 250 с")
ax.axhline(steady, color=ACC, lw=1.6, ls="--", label=f"сталий рівень ≈ {steady:.1f} с".replace(".", ","))
ax.set_xlim(0, 10_000); ax.set_ylim(0, None)
ax.xaxis.set_major_formatter(matplotlib.ticker.FuncFormatter(lambda v, _: f"{int(v):,}".replace(",", " ")))
ax.set_xlabel("Час моделювання, с"); ax.set_ylabel("Середній час очікування, с")
ax.legend(frameon=False, loc="lower right")
fig.savefig(f"{out}/fig4_1c.png"); plt.close(fig)

# 4.2 розкид прогонів
g = defaultdict(list)
for x in rows("replications.csv"): g[x["time"]].append(float(x["w"]))
fig, ax = plt.subplots(figsize=(6.2, 3.6)); rnd = random.Random(1)
for i, (k, v) in enumerate(g.items()):
    ax.scatter([i + rnd.uniform(-0.2, 0.2) for _ in v], v, s=22, color="#4a8fe0", alpha=0.85, edgecolor="white", lw=0.6, zorder=3)
    ax.hlines(np.mean(v), i - 0.3, i + 0.3, color="black", lw=2.2, zorder=4)
ax.set_xticks(range(len(g))); ax.set_xticklabels([f"{int(k):,}".replace(",", " ") for k in g])
ax.grid(axis="x", visible=False)
ax.set_xlabel("Час моделювання одного прогону, с"); ax.set_ylabel("Середній час очікування, с")
fig.savefig(f"{out}/fig4_2.png"); plt.close(fig)

def grid(f):
    v = [(int(x["g1"]), int(x["g2"]), float(x["w"])) for x in rows(f)]
    g1 = sorted({a for a, _, _ in v}); g2 = sorted({b for _, b, _ in v})
    W = np.full((len(g2), len(g1)), np.nan)
    for a, b, w in v: W[g2.index(b), g1.index(a)] = w
    return np.array(g1), np.array(g2), W

def heat(f, name, vmax, step, box_xy, ha):
    g1, g2, W = grid(f)
    j, i = np.unravel_index(np.nanargmin(W), W.shape)
    fig, ax = plt.subplots(figsize=(6.4, 5.2))
    cmap = plt.get_cmap("Blues_r").copy(); cmap.set_over("#f5f9fe")
    ext = [g1[0] - step / 2, g1[-1] + step / 2, g2[0] - step / 2, g2[-1] + step / 2]
    im = ax.imshow(W, origin="lower", extent=ext, cmap=cmap, vmin=np.nanmin(W), vmax=vmax, aspect="auto", interpolation="nearest")
    ax.grid(False)
    for s in ax.spines.values(): s.set_visible(True)
    cb = fig.colorbar(im, extend="max"); cb.set_label("Середній час очікування, с")
    ax.plot(g1[i], g2[j], "x", color=ACC, ms=14, mew=3)
    txt = f"мінімум: G1 = {g1[i]} с, G2 = {g2[j]} с\nW = {W[j, i]:.2f} с"
    kw = dict(xytext=box_xy, textcoords="offset points") if box_xy else dict(xytext=(0.02, 0.97), textcoords="axes fraction", va="top")
    ax.annotate(txt, (g1[i], g2[j]), **kw, ha=ha, fontsize=11, bbox=dict(fc="white", ec="#444", lw=0.8))
    ax.set_xlabel("Тривалість зеленого для напрямку 1, G1, с"); ax.set_ylabel("Тривалість зеленого для напрямку 2, G2, с")
    fig.savefig(f"{out}/{name}"); plt.close(fig)
    return g1[i], g2[j], W

b1, b2, W = (lambda r: (r[0], r[1], r[2]))(heat("grid_red55.csv", "fig4_3.png", 200, 5, (20, 20), "left"))
gw = {(a, b): w for a, b, w in [(int(x["g1"]), int(x["g2"]), float(x["w"])) for x in rows("grid_red55.csv")]}

# 4.4 перерізи
xs = sorted({a for a, _ in gw})
fig, ax = plt.subplots(figsize=(7, 3.8))
ax.plot(xs, [gw[(x, b2)] for x in xs], "-o", ms=4, lw=2.2, color="#2a7ad6", label=f"змінюється G1 при G2 = {b2} с")
ax.plot(xs, [gw[(b1, x)] for x in xs], "-o", ms=4, lw=2.2, color=ACC, label=f"змінюється G2 при G1 = {b1} с")
lo = min(gw.values()); ax.set_ylim(lo - 8, 235)
ax.set_xlabel("Тривалість зеленого інтервалу, с"); ax.set_ylabel("Середній час очікування, с")
ax.legend(frameon=False, loc="upper center"); fig.savefig(f"{out}/fig4_4.png"); plt.close(fig)

# 4.5 уточнення
_, _, Wr = grid("grid_refine.csv")
heat("grid_refine.csv", "fig4_5.png", np.nanmin(Wr) + 3, 1, None, "left")
print("ok")
