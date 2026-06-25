package com.atguigu.llmcallback.Service;

import com.atguigu.llmcallback.Repository.movieRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;


@Service
@Slf4j
public class VectorImageInsert {

    @Autowired
    movieRepository movieRepository;

    @RabbitListener(queues = "vectorResult")
    public void receiveUrl(String text, Channel channel, Message message) throws IOException {

        try {
            ObjectMapper mapper = new ObjectMapper();

            // 1. 解析 payload
            JsonNode root = mapper.readTree(text);

            // 2. 提取 title
            String title = root.get("title").asText();
            String url = root.get("url").asText();

            JsonNode vectorNode = root.get("vector");

            // 2. 【核心修改】智能解析向量，兼容数组、字符串数组、字符串对象
            List<Double> vector = parseVector(vectorNode, mapper);

            // 4. 校验维度
            if (vector.size() != 512) {
                log.error("❌ 向量维度异常: {}, title: {}", vector.size(), title);
                return;
            }

            // 5. 入库
            movieRepository.upsertImageVector(title, vector);

            log.info("✅ 入库成功: {} | 维度: {}", title, vector.size());

            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) {
            log.error("❌ 处理消息失败", e);
        }
    }

    /**
     * 鲁棒的向量解析方法
     */
    private List<Double> parseVector(JsonNode vectorNode, ObjectMapper mapper) throws IOException {
        // 情况 1: 直接是 JSON 数组节点 [1.0, 2.0]
        if (vectorNode.isArray()) {
            return mapper.convertValue(vectorNode, new TypeReference<List<Double>>() {});
        }

        // 情况 2: 是文本节点 (可能是字符串化的数组或对象)
        if (vectorNode.isTextual()) {
            String text = vectorNode.asText().trim();

            // 2.1 尝试解析为数组 "[1.0, 2.0]"
            if (text.startsWith("[")) {
                return mapper.readValue(text, new TypeReference<List<Double>>() {});
            }
            // 2.2 尝试解析为包裹了数组的对象 "{\"vec\": [1.0, 2.0]}" -> 你的报错属于这种情况
            else if (text.startsWith("{")) {
                JsonNode objNode = mapper.readTree(text);
                if (objNode.isObject()) {
                    // 启发式查找：找到第一个数组字段
                    for (JsonNode child : objNode) {
                        if (child.isArray()) {
                            return mapper.convertValue(child, new TypeReference<List<Double>>() {});
                        }
                    }
                }
                throw new IOException("对象字符串中未找到向量数组: " + text);
            }

            throw new IOException("无法识别的向量文本格式: " + text);
        }

        throw new IOException("不支持的向量节点类型: " + vectorNode.getNodeType());
    }
}
