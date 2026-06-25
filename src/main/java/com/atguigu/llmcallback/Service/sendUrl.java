package com.atguigu.llmcallback.Service;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class sendUrl {
    @Autowired
    RabbitTemplate rabbitTemplate;

    public void test(String url,String title){
        rabbitTemplate.convertAndSend("sendUrl",url+"|||"+title);
    }

//    @RabbitListener(queues = "sendUrl")
//    public void receiveUrl(String url, Channel channel, Message message) throws IOException {
//
//        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
//    }
}
