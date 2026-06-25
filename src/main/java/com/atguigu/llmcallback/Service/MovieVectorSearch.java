package com.atguigu.llmcallback.Service;

import com.atguigu.llmcallback.DTO.Movie;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MovieVectorSearch {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    MovieSearchService movieSearchService;

    /**
     * 暴露为 MCP 工具。LLM 会根据 description 自动决定是否调用。
     * @param title 用户输入的查询词
     * @return Top 10 相似电影列表
     */
    @Tool(description = "如果数据库中未能找到用户寻找的电影,使用网络工具寻找")
    public List<Movie> findTop10Similar(@ToolParam(description = "用户想要查找的电影类型、关键词或描述") String title) {
        float[] queryVector=movieSearchService.generateMockEmbedding(title);
        String sql = """
            SELECT movie_id, title, introduction, genres,
                   1 - (embedding_text <=> ?::vector) AS similarity
            FROM movie
            ORDER BY embedding_text <=> ?::vector
            LIMIT 10
        """;

        // 转成 pgvector 字符串
        String vectorStr = toPgVector(queryVector);


        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new Movie(
                        rs.getInt("movie_id"),
                        rs.getString("title"),
                        rs.getString("introduction"),
                        rs.getString("genres"),
                        rs.getDouble("similarity")
                ),
                vectorStr, vectorStr
        );
    }

    private String toPgVector(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            sb.append(v[i]);
            if (i < v.length - 1) sb.append(",");
        }
        return sb.append("]").toString();
    }
}
