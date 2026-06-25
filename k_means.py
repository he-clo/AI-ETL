import pandas as pd
import numpy as np
from ast import literal_eval

CSV = r"C:\Users\28188\Desktop\movies_embedding.csv"

df = pd.read_csv(CSV)

# 兼容三种存法：已经是 list / ndarray / 字符串
def to_vec(x):
    if isinstance(x, (list, np.ndarray)):
        return np.asarray(x, dtype=np.float32)
    x = str(x).strip()
    if x.startswith("[") and x.endswith("]"):
        return np.array(literal_eval(x), dtype=np.float32)
    raise ValueError("无法解析 embedding，样例值: " + str(x[:80]))

X = np.stack(df["embedding"].apply(to_vec).values)  # (N, 1024)

# ✅ 单位化（让欧式距离等价于 cosine）
norms = np.linalg.norm(X, axis=1, keepdims=True)
norms[norms == 0] = 1e-9
Xn = X / norms

def farthest_point_sampling(Xn, k=5, start_idx=None, metric="cosine"):
    """
    metric="cosine": 用 1-cos 作为距离
    metric="euclidean": 用 L2
    """
    N = Xn.shape[0]
    chosen = []
    dist_to_set = np.full(N, np.inf)

    # 初始点
    if start_idx is None:
        start_idx = np.random.randint(0, N)
    chosen.append(start_idx)
    dist_to_set[start_idx] = -np.inf

    def sim(a, b):
        return float(a @ b)

    for _ in range(k - 1):
        last = chosen[-1]
        if metric == "cosine":
            d_new = 1 - Xn @ Xn[last]
        else:
            d_new = np.linalg.norm(Xn - Xn[last], axis=1)

        dist_to_set = np.minimum(dist_to_set, d_new)
        dist_to_set[chosen] = -np.inf
        nxt = int(np.argmax(dist_to_set))
        chosen.append(nxt)

    return chosen

sel5 = farthest_point_sampling(Xn, k=5, metric="cosine")
cols = [c for c in ["embedding","combined_text"] if c in df.columns]
print(df.iloc[sel5][cols])
out = df.iloc[sel5].copy()
out.to_excel(r"C:\Users\28188\Desktop\survey_5_diverse.xlsx", index=False)
print("✅ 问卷样本导出完成")