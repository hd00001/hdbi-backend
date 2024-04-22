package com.hd.hdbibackend.bizmq;

import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * @auther hd
 * @Description
 */
@Component
@Slf4j
public class MyMessageConsumer {
    //指定程序监听的消息队列和确认机制
    @SneakyThrows
    @RabbitListener(queues = {"code_queue"},ackMode = "MANUAL")
    public void receiverMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag){
        log.info("message={}",message);
        channel.basicAck(deliveryTag,false);

    }
}
