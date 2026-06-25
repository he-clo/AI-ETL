package com.atguigu.llmcallback.Controller;

import com.atguigu.llmcallback.Advisor.DatabaseSavingAdvisor;
import com.atguigu.llmcallback.DTO.Movie;
import com.atguigu.llmcallback.Service.MovieVectorSearch;
import com.atguigu.llmcallback.Service.SimilarMovieSearchService;
import com.atguigu.llmcallback.Service.ToolCallService;
import com.atguigu.llmcallback.Service.sendUrl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/search")
public class searchController {
    @Autowired
    ChatClient chatClient;

    @Autowired
    sendUrl sendUrl;

    @Autowired
    ToolCallService toolCallService;

//    @Autowired
//    DatabaseSavingAdvisor databaseSavingAdvisor;

    @Autowired
    MovieVectorSearch movieVectorSearch;

    @Autowired
    SimilarMovieSearchService similarMovieSearchService;

    @GetMapping("/top-10")
    public String top_10(@RequestParam String q) {
//        String text = chatClient
//                .prompt()
//                .advisors(databaseSavingAdvisor)
//                .messages(new SystemMessage("""
//                        你是一个电影搜索助手。
//                        规则：
//                        1. 获取工具结果后，**必须直接输出工具的原始返回字符串**。
//                        2. **绝对禁止**修改、总结、翻译或添加任何额外文本。
//                        3. 输出必须严格保持格式：ID-Title-Description-URL
//                        """))
//                .messages(new UserMessage(q))
//                .call()
//                .content();
               // log.info(text);   这里如果不用大模型的话，模糊语义是肯定搜不到的
        String text=toolCallService.callAdd(q);
        String[] split = text.split("-");
        log.warn(split[2]);
        //List<Movie> top10Similar = movieVectorSearch.findTop10Similar(split[2]);

        List<Movie> top10Similar = similarMovieSearchService.recommendByTitle(split[1]);
        if(top10Similar.isEmpty() ||  top10Similar.size() < 3){
            top10Similar=movieVectorSearch.findTop10Similar(split[2]);
        }
        if(Year.now().getValue()- Integer.valueOf(split[4]) <4) {
            sendUrl.test(split[3], split[1]);
        }
        //log.warn(top10Similar.toString());
        // 2. 排除指定 ID 的电影
        List<Movie> filtered = top10Similar.stream()
                .filter(movie -> !movie.getTitle().equals(split[1]))
                .collect(Collectors.toList());
        return   chatClient
                .prompt()
                .system(s -> s.text("你是一个专业的影评人。请根据提供的电影列表，为用户生成一段推荐语,推荐多部电影。只输出推荐语，不要列出ID。"))
                .user(u -> u.text("电影列表：{top10Similar}").param("top10Similar", filtered))
                .call()
                .content();

    }
}
