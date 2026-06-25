import pandas as pd
import re
df = pd.read_excel(r"C:\Users\28188\Desktop\movies2.xlsx", sheet_name="movies")  # 替换为你的Sheet名 

def clean_introduction(text):
    # 1. 如果值是空值(NaN)或者空字符串，直接返回空字符串
    if pd.isna(text) or str(text) in ["未填写","错误","未找到(404)","未找到(502)"] :
        return ""
    
    return text
def clean_director(text):
    # 1. 如果值是空值(NaN)或者空字符串，直接返回空字符串
    if pd.isna(text) or str(text) in ["错误","未找到","未知导演"] :
        return ""
    
    return text

def clean_actors(text):
    # 1. 如果值是空值(NaN)或者空字符串，直接返回空字符串
    if pd.isna(text) or str(text) in ["未知演员","错误","未找到","未知导演"] :
        return ""
    
    # 2. 按 '/' 分割，并去除每个名字前后的空格
    names = [name.strip() for name in str(text).split('/')]
    
    # 3. 过滤掉分割后可能出现的空字符串（比如连续两个斜杠产生的）
    names = [name for name in names if name]
    
    # 4. 用逗号重新连接
    return ", ".join(names)

# 执行清洗，生成新列 'cleaned_actors'
df['演员'] = df['演员'].apply(clean_actors)
df["导演"]=df["导演"].apply(clean_director)
df["简介"]=df["简介"].apply(clean_introduction)
def has_chinese(text):
    if pd.isna(text):
        return False
    # 判断是否存在中文字符（CJK 统一汉字）
    return bool(re.search(r'[\u4e00-\u9fff]', str(text)))
for i, row in df.iterrows():
    if has_chinese(row['演员']):
        print(f"Row {i}: {row['演员'][:-1]}")
        print("-" * 50)
for i, row in df.iterrows():
    if has_chinese(row['导演']):
        print(f"Row {i}: {row['导演'][:-1]}")
        print("-" * 50)
for i, row in df.iterrows():
    if has_chinese(row['简介']):
        print(f"Row {i}: {row['简介'][:-1]}")
        print("-" * 50)
# 假设列名：'简介'、'导演'、'演员'（根据实际CSV列名调整！）
#df["combined_text"] ="Represent this movie:"+ df["title"]+"Directed by"+ df["导演"].fillna("")+" [SEP] "+"Starring "+df["演员"].fillna("")+" [SEP] "+"Plot summary:"+df["简介"].fillna("") 

def build_text(row):
    parts = [f"Represent this movie: {row['title']}."]
    
    if pd.notna(row['导演']) and row['导演']:
        parts.append(f"Directed by {row['导演']}.")
    if pd.notna(row['演员']) and row['演员']:
        parts.append(f"Starring {row['演员']}.")
    if pd.notna(row['简介']) and row['简介']:
        parts.append(f"Plot summary: {row['简介']}.")
    
    return " ".join(parts)

df["combined_text"] = df.apply(build_text, axis=1)
print(df["combined_text"].head())
import requests
import pandas as pd
import os
import time
from tqdm import tqdm
from concurrent.futures import ThreadPoolExecutor, as_completed
from openpyxl import Workbook

# ================= 配置 =================
OLLAMA_URL = "http://localhost:11434/api/embeddings"
MODEL_NAME = "bge-m3:latest"
TEXT_COLUMN = "combined_text"
OUTPUT_EXCEL_PATH = r"C:\Users\28188\Desktop\movies03.xlsx"
MAX_WORKERS = 16  # 👈 根据你机器调（8～16 都行）
# ========================================

print(f"🚀 开始处理 {len(df)} 条数据...")

results = []

def process_row(idx, text):
    if not text or str(text).strip() == "":
        return idx, None

    payload = {
        "model": MODEL_NAME,
        "prompt": str(text)
    }

    try:
        resp = requests.post(OLLAMA_URL, json=payload, timeout=120)
        resp.raise_for_status()
        embedding = resp.json().get("embedding", [])
        return idx, (text, embedding)
    except Exception as e:
        return idx, (text, f"ERROR: {e}")

# ========= 多线程执行 =========
with ThreadPoolExecutor(max_workers=MAX_WORKERS) as executor:
    futures = [
        executor.submit(process_row, idx, row[TEXT_COLUMN])
        for idx, row in df.iterrows()
    ]

    for future in tqdm(
        as_completed(futures),
        total=len(futures),
        desc="生成 embedding"
    ):
        results.append(future.result())

# ========= 排序，防止乱序 =========
results = sorted(results, key=lambda x: x[0])

#========= 一次性写 Excel =========
wb = Workbook()
ws = wb.active
ws.append(["combined_text", "embedding"])

for _, (text, emb) in results:
    ws.append([text, str(emb)])

wb.save(OUTPUT_EXCEL_PATH)

print(f"\n✅ 全部完成！共 {len(results)} 条")
print(f"📁 文件：{OUTPUT_EXCEL_PATH}")
