package com.atguigu.llmcallback.Advisor;


import com.atguigu.llmcallback.Service.AsyncSaveToDatabase;

import com.atguigu.llmcallback.Service.MovieSearchProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class DatabaseSavingAdvisor implements CallAdvisor {

//    @Autowired
//    public  AsyncSaveToDatabase asyncSaveToDatabase;

    private final MovieSearchProcessor processor;

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
//        ChatClientResponse response = chain.nextCall(request);
//
//        // 提取 LLM 最终输出或工具结果
//        String content = response.chatResponse().getResult().getOutput().getText();
//        processor.processToolResult(content);
//
//        return response;
        ChatClientResponse response = chain.nextCall(request);

        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null) {
            return response;
        }

        // ✅ 检查是否有工具调用
//        AssistantMessage output = chatResponse.getResult().getOutput();
//        if (output.hasToolCalls()) {
//            // 工具调用阶段，不保存
//            return response;
//        }


        // 遍历所有的 Generation（通常只有一个）
        for (Generation generation : chatResponse.getResults()) {
            AssistantMessage output = generation.getOutput();
            if (output == null) {
                continue;
            }

            // ✅ 关键：检查是否是工具调用的结果
            if (output.hasToolCalls()) {
                // 这是工具调用的请求，不是结果，跳过
                continue;
            }

            // ✅ 检查是否是工具响应的结果（即工具执行后的返回）
            // 在 Spring AI 中，工具响应的结果通常是 AssistantMessage 且带有 tool_call_id
            String content = output.getText();

            // ✅ 最终文本阶段
            //String content = output.getText();
            if (content != null) {
                processor.processToolResult(content);
            }

            return response;
        }
        return response;
    }


//    private final MovieRepository movieRepository; // 注入你的 Repository
//
//    public DatabaseSavingAdvisor(MovieRepository movieRepository) {
//        this.movieRepository = movieRepository;
//    }

//    @Override
//    public ChatClientResponse adviseCall(
//            ChatClientRequest request,
//            CallAdvisorChain chain) {
//
//        ChatClientResponse response = chain.nextCall(request);
//        ChatResponse chatResponse = response.chatResponse();
//
//        // 1. 判断是否包含工具调用
//        if (chatResponse == null) {
//            return response;
//        }
//
//        // 2. 从 metadata 拿工具结果
//        ChatResponseMetadata metadata = chatResponse.getMetadata();
//
//        Object toolResult = metadata.get("tool_result");
//        if (toolResult == null) {
//            return response;
//        }
//
//        // 3. 这才是真正的工具返回值
//        String toolOutput = toolResult.toString();
//        System.out.println("✅ 工具返回结果：" + toolOutput);
//
//        // 4. 保存数据库
//        asyncSaveToDatabase.splitToList(toolOutput);
//
//        return response;
//    }

    @Override
    public String getName() {
        return "dbAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }


}