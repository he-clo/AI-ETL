package com.atguigu.llmcallback.Configuration;

import org.springframework.amqp.core.*;
//import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
//import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;


@Configuration
public class RabbitmqConfig {


//    @Bean
//    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
//            SimpleRabbitListenerContainerFactoryConfigurer configurer,
//            ConnectionFactory connectionFactory) {
//
//        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
//        configurer.configure(factory, connectionFactory);
//
//        // 1. 在 Converter 之前补全缺失的 AMQP 属性（修 NPE 根因）
//        factory.setAfterReceivePostProcessors(message -> {
//            MessageProperties props = message.getMessageProperties();
//            if (props.getContentType() == null) {
//                props.setContentType("text/plain");   // Logstash 文本日志
//                // 如果是 JSON 格式改 "application/json"
//            }
//            if (props.getPriority() == null) {
//                props.setPriority(0);
//            }
//            return message;
//        });
//
//        // 2. Jackson 3 的 JSON 转换器（替换 Jackson2JsonMessageConverter）
//        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
//        factory.setMessageConverter(converter);
//
//        return factory;
//    }

    @Bean
    public Exchange sendExchange() {
        return ExchangeBuilder.fanoutExchange("sendUrl").build();
    }

    @Bean
    public Queue sendQueue() {
        return QueueBuilder.durable("sendUrl").build();
    }

    @Bean
    public Binding binding(Exchange sendExchange, Queue sendQueue) {
        return BindingBuilder.bind(sendQueue).to(sendExchange).with("").noargs();
    }
}
