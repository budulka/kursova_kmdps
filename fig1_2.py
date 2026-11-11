import matplotlib; matplotlib.use("Agg")
import matplotlib.pyplot as plt
plt.rcParams.update({"font.family": "DejaVu Serif", "font.size": 12})
G, R = "#1db07a", "#e24b4b"
segs = [(0, 3.3, G, "G1\nзелене\nнапрямку 1"), (3.3, 3.5, R, "R1\nчервоне\nв обох"),
        (6.8, 4.15, G, "G2\nзелене\nнапрямку 2"), (10.95, 3.5, R, "R2\nчервоне\nв обох")]
fig, ax = plt.subplots(figsize=(10.5, 3.2))
for x0, w, c, t in segs:
    ax.barh(0, w, left=x0, height=1, color=c, edgecolor="white", linewidth=2)
    ax.text(x0 + w / 2, 0.05, t, ha="center", va="center", color="white", fontsize=15, linespacing=1.3)
ticks = [0, 3.3, 6.8, 10.95, 14.45]
ax.set_xticks(ticks); ax.set_xticklabels(["0", "G1", "G1+R", "G1+R+G2", "T = G1+G2+2R"])
ax.set_xlim(-0.1, 15.0); ax.set_ylim(-0.55, 0.52); ax.set_yticks([])
for s in ("top", "right", "left"): ax.spines[s].set_visible(False)
ax.spines["bottom"].set_color("#444"); ax.tick_params(colors="#444", labelsize=17)
ax.set_xlabel("Час від початку циклу, с", fontsize=17)
fig.savefig("figs/fig1_2.png", dpi=200, bbox_inches="tight")
