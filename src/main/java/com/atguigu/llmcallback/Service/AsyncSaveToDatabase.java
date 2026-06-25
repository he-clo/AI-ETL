package com.atguigu.llmcallback.Service;

import com.atguigu.llmcallback.DTO.Movie;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AsyncSaveToDatabase {

    private static final Logger log = LoggerFactory.getLogger(AsyncSaveToDatabase.class);

    @Autowired
    MovieSearchService movieSearchService;

    private  final Set<Integer> hashset=ConcurrentHashMap.newKeySet();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Async("httpExecutor")
    public void splitToList(String result) {
        if (result == null || result.isBlank()) return;

        //log.info("result {}",result);

        // 1. 【防御性检查】数据必须以数字开头（ID）
        // 如果 LLM 返回了自然语言（如 "The Matrix..."），直接跳过
        if (!Character.isDigit(result.charAt(0))) {
            log.warn("⚠️ LLM 未返回标准格式数据 (非数字开头)，跳过。内容片段: {}",
                    result.substring(0,  result.length()));
            return;
        }
        try{
            log.info("开始存储");
        String[] split = result.split("-");
        if(hashset.add(Integer.parseInt(split[0]))){
            saveMovie(split);
        }}catch (Exception e){
            e.printStackTrace();
        }
    }

    public void saveMovie(String[] split ) {
        float[] floats_1024 = movieSearchService.generateMockEmbedding(split[2]);
        Movie movie = new Movie(new float[512], floats_1024, split[2], split[1]);
        String sql = "INSERT INTO movie (embedding_image, embedding_text, title, introduction) VALUES (?::vector, ?::vector, ?, ?) ON CONFLICT (title) DO NOTHING;";

        rabbitTemplate.convertAndSend("sendUrl",split[3]);

        //确认异步
//        try {
//            // 暂停 15 秒 (单位为毫秒，1000毫秒 = 1秒)
//            Thread.sleep(15000);
//        } catch (InterruptedException e) {
//            // 当线程被中断时处理异常，通常恢复中断状态
//            Thread.currentThread().interrupt();
//            e.printStackTrace();
//        }

        // JdbcTemplate 会自动处理连接和异常
        int a = jdbcTemplate.update(sql,
                floatArrayToPgVector(movie.getEmbeddingImage()), // 会自动转为 PG Array
                floatArrayToPgVector(movie.getEmbeddingText()),
                movie.getTitle(),
                movie.getIntroduction()
        );
        if (a == 0) {
            log.info("已存在，未插入"); //多线程不打印，这一步没用
        } else {
            log.info("✅ JdbcTemplate 插入成功");
        }
    }

    private String floatArrayToPgVector(float[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            sb.append(arr[i]);
            if (i < arr.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
