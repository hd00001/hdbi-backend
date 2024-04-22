package com.hd.hdbibackend.bizmq;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @auther hd
 * @Description
 */
@SpringBootTest
class MyMessageProducerTest {

    @Resource
    private MyMessageProducer myMessageProducer;

    @Test
    void sendMessage(){
        myMessageProducer.sendMessage("code_exchange","my_routKing","你好啊");
    }
}