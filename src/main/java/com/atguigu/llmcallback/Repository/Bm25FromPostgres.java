package com.atguigu.llmcallback.Repository;

import com.atguigu.llmcallback.Service.MiniBM25;
import com.atguigu.llmcallback.Util.TextUtil;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;

public class Bm25FromPostgres {

    private static final String JDBC_URL = "jdbc:postgresql://localhost:5432/ai_test";
    private static final String USERNAME = "postgres";
    private static final String PASSWORD = "123456";

    public static void main(String[] args) throws Exception {

        List<String> lines = Files.readAllLines(Paths.get("C:/Users/28188/Desktop/bm25.txt"));

        Set<String> titleSet = new HashSet<>();
        List<String> queries = new ArrayList<>();

        for (String line : lines) {
            String[] parts = line.split("\t");
            if (parts.length >= 2) {
                titleSet.add(parts[0].trim());
                queries.add(parts[1].trim());
            }
        }


        Map<Integer, List<String>> corpus = loadCorpusFromPg();

        MiniBM25 bm25 = new MiniBM25(corpus);

        // Exp-C 示例：用你之前准备好的英文精简词
        for (String query : queries) {
            List<MiniBM25.ScoredDoc> results = bm25.search(query, 10);

            System.out.println("Top-10 results for query: " + query);

            for (MiniBM25.ScoredDoc doc : results) {
                System.out.println(doc);
            }
        }
    }

    private static Map<Integer, List<String>> loadCorpusFromPg() throws SQLException {
        Map<Integer, List<String>> docs = new HashMap<>();

        String sql = """
            SELECT movie_id, title, genres, introduction
            FROM movie
            WHERE introduction IS NOT NULL
            ORDER BY movie_id
            LIMIT 50
        """;

        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int movieId = rs.getInt("movie_id");

                String title = rs.getString("title");
                String genres = rs.getString("genres");
                String overview = rs.getString("introduction");

                // 拼接成 BM25 用的文本
                String fullText = String.join(" ",
                        safe(title),
                        safe(genres).replace("|", " "),
                        safe(overview)
                );

                List<String> tokens = TextUtil.tokenize(fullText);
                docs.put(movieId, tokens);
            }
        }

        System.out.println("Loaded " + docs.size() + " movies from PostgreSQL.");
        return docs;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}