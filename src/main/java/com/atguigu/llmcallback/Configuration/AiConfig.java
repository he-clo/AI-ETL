package com.atguigu.llmcallback.Configuration;

import com.atguigu.llmcallback.Advisor.DatabaseSavingAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AiConfig {


    /**
     * 定义全局 ChatClient Bean
     * Spring 会自动注入 ChatClient.Builder、List<ToolCallbackProvider> 和 DatabaseSavingAdvisor
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                 List<ToolCallbackProvider> toolCallbackProviders,
                                 DatabaseSavingAdvisor dbAdvisor) {
        return builder
                .defaultToolCallbacks(toolCallbackProviders.toArray(new ToolCallbackProvider[0]))
               // .defaultAdvisors(dbAdvisor)
                .build();
    }
}
