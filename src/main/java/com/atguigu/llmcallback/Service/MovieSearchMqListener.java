package com.atguigu.llmcallback.Service;

import com.atguigu.llmcallback.Service.MovieSearchProcessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
//import tools.jackson.databind.JsonNode;
//import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class MovieSearchMqListener {

    private final MovieSearchProcessor processor;

    @RabbitListener(queues = "log.raw.queue")
    public void handleSearchMessage(byte[] body,
                                    Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        String logs = new String(body, StandardCharsets.UTF_8);
        System.out.println("接收到数据" + logs);
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(logs);

            // 1. 获取 message 字段的完整字符串
            String messageContent = rootNode.path("message").asText();

            // 2. 按制表符分割
            String[] parts = messageContent.split("\t");

            // 3. 安全提取英文名 (索引 4)
            if (parts.length > 4) {
                String englishName = parts[4].trim();
                //System.out.println(englishName);
                // 立即返回，不阻塞 RabbitMQ 消费线程
                processor.triggerSearchFromMQ(englishName);
                channel.basicAck(deliveryTag, false);
            }else {
                log.warn("⚠️ 消息格式异常，直接丢弃");
                // ❌ 失败：拒绝消息，且不重新入队 (requeue=false)
                // 注意：格式错误的数据如果重新入队会导致死循环，所以必须设为 false
                channel.basicNack(deliveryTag, false, false);
            }


        }catch (Exception e){
        e.printStackTrace();
            log.error("❌ 处理消息异常", e);
            // ❌ 异常：拒绝消息，不重新入队
            channel.basicNack(deliveryTag, false, false);
        }
    }
    }
