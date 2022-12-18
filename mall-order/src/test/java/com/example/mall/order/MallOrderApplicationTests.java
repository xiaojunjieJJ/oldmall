package com.example.mall.order;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class MallOrderApplicationTests {

    @Autowired
    AmqpAdmin amqpAdmin;
    @Autowired
    RabbitTemplate template;

    /**
     * 创建Exchange、Queue、Binding
     * 1.AmqpAdmin
     * 2.RabbitTemplate
     */
    @Test
    public void createExchange() {
        DirectExchange exchange = new DirectExchange("hello-java-exchange", true, false);
        amqpAdmin.declareExchange(exchange);
        log.info("交换机创建完成");
    }

    @Test
    public void createQueues() {
        Queue queue = new Queue("hello-java-queue", true, false, false);
        amqpAdmin.declareQueue(queue);
        log.info("队列创建完成");
    }

    @Test
    public void createBinding() {
        Binding binding = new Binding("hello-java-queue",
                Binding.DestinationType.QUEUE,
                "hello-java-exchange",
                "hello-java", null);
        amqpAdmin.declareBinding(binding);
    }

    //发消息,如果发送的是对象，则该对象需要实现serializable接口
    @Test
    public void sendMessage() {
        template.convertAndSend("hello-java-exchange","hello-java","Hello,Rabbit");
    }


}
