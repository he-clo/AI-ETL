package com.atguigu.llmcallback.Repository;

import com.atguigu.llmcallback.DTO.Movie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MovieVectorRepositoryByInstruct {

    @Autowired
    private JdbcTemplate jdbcTemplate;


    public List<Movie> findTop10SimilarByInstruct(float[] queryVector) {
        String sql = """
            SELECT movie_id, title, introduction, genres,
                   1 - (embedding_text <=> ?::vector) AS similarity
            FROM movie2
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
