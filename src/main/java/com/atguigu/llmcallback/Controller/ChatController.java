package com.atguigu.llmcallback.Controller;

import com.atguigu.llmcallback.Advisor.DatabaseSavingAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/ai")
public class ChatController {

    @Autowired
    DatabaseSavingAdvisor databaseSavingAdvisor;

    private final ChatClient chatClient;

    // ✅ 直接注入全局 Bean，无需手动 new 或 build
    public ChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostConstruct
    public void init() {
        log.info("✅ ChatClient 初始化完成。可通过 DEBUG 日志查看已注册的工具列表。");
    }



    @GetMapping("/chat")
    public String chat(@RequestParam String q) {
        return chatClient
                .prompt()
                .advisors(databaseSavingAdvisor)
                .messages(new SystemMessage("""
                    你是一个电影搜索助手。
                    规则：
                    1. 获取工具结果后，**必须直接输出工具的原始返回字符串**。
                    2. **绝对禁止**修改、总结、翻译或添加任何额外文本。
                    3. 输出必须严格保持格式：ID-Title-Description-URL
                    """))
                .messages(new UserMessage(q))
                .call()
                .content();
    }
}