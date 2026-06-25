from openpyxl import Workbook
import pandas as pd
df1 = pd.read_csv(r"C:\Users\28188\Desktop\movies_embedding.csv",dtype={"embedding": str})
df2 = pd.read_excel(r"C:\Users\28188\Desktop\movies2.xlsx")
df1 = df1.reset_index(drop=True)
df2 = df2.reset_index(drop=True)
df = pd.concat([df2, df1], axis=1)
if "embedding_image" not in df.columns:
    df["embedding_image"] = None
import numpy as np
from sqlalchemy import create_engine, text

import psycopg2
from io import StringIO
from tqdm import tqdm
# =========================
# 1. 数据库连接
# =========================
conn = psycopg2.connect(
    user="postgres",
    password="123456",
    host="localhost",
    port=5432,
    dbname="ai_test"
)
cur = conn.cursor()

# =========================
# 2. 读取数据
# =========================
# df = pd.read_csv("movie.csv")
# 示例：df 至少包含 ['title', 'embedding']

# =========================
# 3. embedding 清洗 & 转换
# =========================
# def parse_embedding(x):
#     """
#     支持多种 embedding 输入格式：
#     - list / ndarray
#     - 字符串形式的 list
#     """
#     if isinstance(x, str):
#         try:
#             x = eval(x)  # 或 ast.literal_eval
#         except Exception:
#             return None

#     if isinstance(x, (list, np.ndarray)):
#         return np.array(x, dtype=np.float32).tolist()

#     return None


import re
import numpy as np

def parse_embedding_with_sci(x):
    """
    支持：
    - 科学计数法：1.23e-05
    - 普通小数：-0.0012
    - 字符串：[1.2, 3.4e-5, 6.7]
    """
    if pd.isna(x) or x in ["nan", "None", "", "[]"]:
        return None

    s = str(x).strip()

    # 去掉首尾中括号
    if s.startswith("[") and s.endswith("]"):
        s = s[1:-1]

    # 正则：匹配所有数字（含科学计数法）
    nums = re.findall(r"[-+]?\d*\.?\d+(?:[eE][-+]?\d+)?", s)

    if not nums:
        return None

    try:
        return np.array([float(n) for n in nums], dtype=np.float32)
    except ValueError:
        return None

#df["embedding"] = df["embedding"].apply(parse_embedding_with_sci)

print(df["embedding"].apply(type).value_counts())
print("空值数量:", df["embedding"].isna().sum())

# 丢弃异常 embedding
#df = df.dropna(subset=["embedding"])

print(f"✅ 待写入：{len(df)} 条")
BATCH_SIZE = 5000  # COPY 可以更大
columns_to_write = [
    "title",
    "introduction",
    "genres",
    "embedding_text",
    "embedding_image"  # 虽然是空，但也要写在列名里
]

for start in tqdm(range(0, len(df), BATCH_SIZE), desc="COPY to DB"):
    batch = df.iloc[start:start + BATCH_SIZE]

    buffer = StringIO()
    for _, row in batch.iterrows():
        parts = []
        
        # 1. title
        parts.append(str(row.get("title", "")))
        
        # 2. 简介
        parts.append(str(row.get("简介", "")))
        
        # 3. genres
        parts.append(str(row.get("genres", "")))
        
        # 4. embedding_text
        parts.append(str(row["embedding"]))
        
        # ✅ 正确：numpy array → 逗号分隔字符串
        # emb = row["embedding_norm"]

        # if emb is None:
        #     parts.append(r"\N")
        # else:
        #     parts.append("[" + ",".join(map(str, emb)) + "]")
        
        # 5. embedding_image（可能为 None）
        if pd.isna(row["embedding_image"]):
            parts.append(r"\N")  # PostgreSQL 的 NULL 标记
        else:
            parts.append(str(row["embedding_image"]))
        
         # ✅ 关键修复：清洗数据，防止 Tab 和换行符破坏格式
        cleaned_parts = []
        for part in parts:
            # 1. 替换 Tab 键
            s = part.replace('\t', ' ')
            # 2. 替换换行符
            s = s.replace('\n', ' ').replace('\r', ' ')
            cleaned_parts.append(s)
        
        # ✅ 使用清洗后的数据写入
        buffer.write("\t".join(cleaned_parts) + "\n")
    
    buffer.seek(0)
    
    try:
        cur.copy_from(
            buffer,
            "movie2",
            columns=columns_to_write
        )
        conn.commit()
    except Exception as e:
        print(f"写入失败: {e}")
        conn.rollback()

cur.close()
conn.close()
print("🎉 数据写入完成")