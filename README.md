基于springboot 3.5.7 springAI 1.1.0实现。
接口: ChatController用于用户输入和日志拉取两个流程复用返回精准匹配 自动异步落盘
      searchController用于文本图片双模态多路召回top10，并对工具返回格式采取prompt注入和直接调用两种选项获取工具原值
工具：Fastmcp search_mcp.py利用网络接口补全相关信息
      pika   test_clip.py 分布式系统通过rabbitmq互相通信,多步补全图片向量
python代码: insert.py获取简介
            insert2.py获取演员和导演
            k_means.py聚出5个差异最大的数据 Farthest Point Sampling（k‑center greedy）
            move.py 数据清洗并向量化
            test.py 入库

文档: links_updated.csv 根据tmdbID获取简介
      links_updated2.csv获取演员和导演信息

第一步：数据清洗
https://files.grouplens.org/datasets/movielens/ml-32m.zip
因为下的是比较新的数据源（2023-10）所以多了一个links.csv，里面有imdbId和tmdbId，方便我们补全电影简介。简单看一下，里面有八万条电影，限制速度40次/10S，那就要六个小时。
第二步：写python+下载ollama+下载bge-m3
python要写FastAPI，原因是ollama图片向量模型本地没有，云端太贵，所以python提供接口直接调用
第三步：下载PostgreSQL + pgvector
第四步：使用python接口处理向量和文本，然后存储为向量
第五步：多路复用，文本*0.7+图片*0.3 如果没有图片就是文本*1.0
第六步：拉入近期搜索值，处理当天的nginx日志，提取有价值的指标
为了和java对接 引入rabbitmq
第七步：实时调整向量
应该要归一化 避免文本长度带来的影响
第八步：MCP搜索工具和agent智能体的引入
提示词写好 可以让他在用户提问后多次调用工具 不过感觉不算是真的agent
第九步 测验
建立对照实验，1.重新导入instruct向量，证明他有用2.下载bg25证明bge-m3多语言情况下与文本检索的bg25有同等性能
bg25 缩减版语句 HR@10 = 47 / 50 = 0.94
bge-m3 未加instruct的中文原文简介  HR@10 = 27 / 50 = 0.54
bge-m3 加instruct的中文原文简介  HR@10 = 24 / 50 = 0.48

补充: 
要建索引 
CREATE INDEX ON documents
USING hnsw (embedding_text vector_cosine_ops);

建表语句
CREATE TABLE movie (
    movie_id INTEGER,
    title CHARACTER VARYING(200),
    introduction TEXT,
    genres CHARACTER VARYING(100),
    embedding_text VECTOR(1024),
    embedding_image VECTOR(512)
);

title要有唯一标识
ALTER TABLE movie ADD CONSTRAINT uk_movie_title UNIQUE (title);


遇到的问题:
            @Autoweired后在别的地方new了它,交给spring管理后就不能new了 改为参数注入
            没有规划好类 导致多个类循环依赖 改为lazy懒加载
            AdvisorCall获取不到返回值 通过提示词要求返回原值 并再过一遍LLM回复用户偏好语言 或者通过手动调mcp工具获取原值 但是不能模糊匹配用户偏好

展望:
      通过设置偏移值实现让LLM翻页搜索的功能
      维护用户偏好标题链和用户向量 实现用户的持久化 (但是会增加写操作,所以要引入redis缓冲)
