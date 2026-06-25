package com.atguigu.llmcallback.Service;

import com.atguigu.llmcallback.Util.TextUtil;

public class MiniBM25 {

    // ===== 可调参数（标准默认值）=====
    private final double k1 = 1.5;
    private final double b  = 0.75;

    // docId -> List<token>
    private final java.util.Map<Integer, java.util.List<String>> docs;
    // token -> df (多少个文档包含它)
    private final java.util.Map<String, Integer> df = new java.util.HashMap<>();
    private final int N;                     // 文档总数
    private final double avgdl;              // 平均文档长度
    private final java.util.Map<String, java.util.Set<Integer>> inv = new java.util.HashMap<>();

    public MiniBM25(java.util.Map<Integer, java.util.List<String>> docs) {
        this.docs = docs;
        this.N = docs.size();

        // 建倒排 + df
        long totalLen = 0;
        for (var e : docs.entrySet()) {
            int docId = e.getKey();
            var tokens = e.getValue();
            totalLen += tokens.size();
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (String tok : tokens) {
                inv.computeIfAbsent(tok, k -> new java.util.HashSet<>()).add(docId);
                if (seen.add(tok)) {
                    df.put(tok, df.getOrDefault(tok, 0) + 1);
                }
            }
        }
        this.avgdl = (double) totalLen / Math.max(N, 1);
    }

    // query: 精简英文搜索词（如 "toy story animation"）
    public java.util.List<ScoredDoc> search(String query, int topK) {
        var qTokens = TextUtil.tokenize(query);
        // 统计 query 词频（query 很短，但保留 tf 更标准）
        java.util.Map<String, Integer> qtf = new java.util.HashMap<>();
        for (String qt : qTokens) qtf.put(qt, qtf.getOrDefault(qt, 0) + 1);

        // 候选集：只扫包含任意 query token 的文档（加速）
        java.util.Set<Integer> candidates = new java.util.HashSet<>();
        for (String qt : qtf.keySet()) {
            var s = inv.get(qt);
            if (s != null) candidates.addAll(s);
        }

        java.util.PriorityQueue<ScoredDoc> pq = new java.util.PriorityQueue<>(
                topK, java.util.Comparator.comparingDouble(x -> x.score)
        );

        for (int docId : candidates) {
            var dTokens = docs.get(docId);
            if (dTokens == null) continue;
            // 文档 tf map
            java.util.Map<String, Integer> dtf = new java.util.HashMap<>();
            for (String t : dTokens) dtf.put(t, dtf.getOrDefault(t, 0) + 1);

            double score = 0.0;
            int dl = dTokens.size();
            for (var eq : qtf.entrySet()) {
                String qt = eq.getKey();
                int df_t = df.getOrDefault(qt, 0);
                if (df_t == 0) continue;
                int tf_t = dtf.getOrDefault(qt, 0);
                if (tf_t == 0) continue;

                double idf = Math.log((N - df_t + 0.5) / (df_t + 0.5) + 1.0);
                double numerator = tf_t * (k1 + 1);
                double denominator = tf_t + k1 * (1 - b + b * (dl / avgdl));
                score += idf * (numerator / denominator);
            }

            if (pq.size() < topK) {
                pq.offer(new ScoredDoc(docId, score));
            } else if (score > pq.peek().score) {
                pq.poll();
                pq.offer(new ScoredDoc(docId, score));
            }
        }

        // 按降序输出
        java.util.List<ScoredDoc> result = new java.util.ArrayList<>(pq);
        result.sort((a, b) -> Double.compare(b.score, a.score));
        return result;
    }

    public static class ScoredDoc {
        public final int docId;
        public final double score;
        public ScoredDoc(int docId, double score) {
            this.docId = docId;
            this.score = score;
        }
        @Override public String toString() { return docId + ":" + String.format("%.4f", score); }
    }
}
