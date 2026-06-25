package com.atguigu.llmcallback.Repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class movieRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 根据 title 插入或更新向量
     * 如果 title 已存在 → 更新 image_vector
     * 如果 title 不存在 → 插入新记录
     */
    public void upsertImageVector(String title, List<Double> vector) {
        // 1. 转换为 pgvector 字符串格式
        String vectorStr = toPgVector(vector);

        // 2. UPSERT SQL
        String sql = """
            INSERT INTO movie (title, embedding_image)
            VALUES (?, ?::vector)
            ON CONFLICT (title)
            DO UPDATE SET
                embedding_image = EXCLUDED.embedding_image
            """;

        jdbcTemplate.update(sql, title, vectorStr);
    }

    /**
     * 将 List<Double> 转换为 pgvector 字符串格式
     */
    private String toPgVector(List<Double> vector) {
        if (vector.size() != 512) {
            throw new IllegalArgumentException(
                    "向量维度必须为 512，当前为: " + vector.size()
            );
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(String.format("%.8f", vector.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }
}
