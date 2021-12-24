package com.example.mall.order;

import org.junit.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MallOrderApplicationTests {

    @Autowired
    AmqpAdmin amqpAdmin;

    @Test
    void createExchange() {


    }

}
