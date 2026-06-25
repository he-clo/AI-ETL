package com.atguigu.llmcallback.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class MovieSearchProcessor {

    private final ChatClient chatClient;
    private final AsyncSaveToDatabase asyncSaveToDatabase;

    // ✅ 在 ChatClient 参数上添加 @Lazy 打破循环依赖
    public MovieSearchProcessor(@Lazy ChatClient chatClient, AsyncSaveToDatabase asyncSaveToDatabase) {
        this.chatClient = chatClient;
        this.asyncSaveToDatabase = asyncSaveToDatabase;
    }


    /** 供 Advisor 调用：处理已拦截到的工具返回结果 */
    public void processToolResult(String toolOutput) {
        if (toolOutput != null && !toolOutput.isBlank()) {
            log.debug("✅ Advisor 转发工具结果，长度: {}", toolOutput.length());
            asyncSaveToDatabase.splitToList(toolOutput);
        }
    }



    /** 供 MQ 调用：主动触发工具搜索（异步执行，不阻塞监听器线程） */
    @Async("mqExecutor") // 使用独立 MQ 线程池
    public CompletableFuture<Void> triggerSearchFromMQ(String query) {
        try {
            log.info("📥 MQ 触发搜索任务: {}", query);
            String result = chatClient.prompt()
                    .messages(new SystemMessage("""
                    你是一个电影搜索助手。
                    规则：
                    1. 获取工具结果后，**必须直接输出工具的原始返回字符串**。
                    2. **绝对禁止**修改、总结、翻译或添加任何额外文本。
                    3. 输出必须严格保持格式：ID-Title-Description-URL
                    """))
                    .messages(new UserMessage(query))
                    .call()
                    .content();
            processToolResult(result);
        } catch (Exception e) {
            log.error("❌ MQ 搜索任务失败", e);
        }
        return CompletableFuture.completedFuture(null);
    }
}
