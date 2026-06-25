package com.atguigu.llmcallback.Repository;

import com.atguigu.llmcallback.DTO.Movie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.util.Arrays;

@Repository
public class MovieSearchRepository {

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * Step 1：根据标题模糊查找电影，获取其向量
     */
    public Movie findMovieByTitle(String title) {
        String sql = """
            SELECT movie_id, title, introduction, genres, 
                   embedding_text, embedding_image
            FROM movie
            WHERE title ILIKE '%' || ? || '%'
            ORDER BY LENGTH(title)  -- 优先精确匹配
            LIMIT 1
            """;

//        return jdbc.queryForObject(sql, new RowMapper<Movie>() {
//            @Override
//            public Movie mapRow(ResultSet rs, int rowNum) throws SQLException {
//                Movie m = new Movie();
//                m.movieId = rs.getInt("movie_id");
//                m.title = rs.getString("title");
//                m.introduction = rs.getString("introduction");
//                m.genres = rs.getString("genres");
//
//                // 读取向量
//                java.sql.Array textArr = rs.getArray("embedding_text");
//                java.sql.Array imageArr = rs.getArray("embedding_image");
//
//                m.embeddingText = textArr != null ? (float[]) textArr.getArray() : null;
//                m.embeddingImage = imageArr != null ? (float[]) imageArr.getArray() : null;
//
//                return m;
//            }
//        }, title);
//    }

        List<Movie> movies = jdbc.query(sql, new RowMapper<Movie>() {
            @Override
            public Movie mapRow(ResultSet rs, int rowNum) throws SQLException {
                Movie m = new Movie();
                m.movieId = rs.getInt("movie_id");
                m.title = rs.getString("title");
                m.introduction = rs.getString("introduction");
                m.genres = rs.getString("genres");

                // 安全地读取向量
                try {
                    java.sql.Array textArr = rs.getArray("embedding_text");
                    java.sql.Array imageArr = rs.getArray("embedding_image");

                    m.embeddingText = textArr != null ? (float[]) textArr.getArray() : null;
                    m.embeddingImage = imageArr != null ? (float[]) imageArr.getArray() : null;
                } catch (SQLException e) {
                    // 如果向量列为空或无法读取，设置为 null
                    m.embeddingText = null;
                    m.embeddingImage = null;
                }

                return m;
            }
        }, title);

        // 返回第一个结果，如果没有则返回 null
        return movies.isEmpty() ? null : movies.get(0);
    }

    /**
     * Step 2：用文本向量召回候选电影
     */
    public List<Movie> recallByTextVector(float[] queryVec, int excludeMovieId) {
        String sql = """
            SELECT movie_id, title, introduction, genres,
                   1 - (embedding_text <=> ?::vector) AS similarity
            FROM movie
            WHERE movie_id != ?  -- 排除自己
            ORDER BY embedding_text <=> ?::vector
            LIMIT 20
            """;

        String vecStr = Arrays.toString(queryVec)
                .replace("[", "{")
                .replace("]", "}");

        return jdbc.query(sql, new RowMapper<Movie>() {
            @Override
            public Movie mapRow(ResultSet rs, int rowNum) throws SQLException {
                Movie m = new Movie();
                m.movieId = rs.getInt("movie_id");
                m.title = rs.getString("title");
                m.introduction = rs.getString("introduction");
                m.genres = rs.getString("genres");
                m.similarity = rs.getDouble("similarity");
                return m;
            }
        }, vecStr, excludeMovieId, vecStr);
    }
}