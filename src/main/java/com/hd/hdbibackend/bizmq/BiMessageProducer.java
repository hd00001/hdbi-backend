package com.hd.hdbibackend.bizmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @auther hd
 * @Description
 */
//定义成组建,便于框架扫描纳入管理
@Component
public class BiMessageProducer {
//    进行依赖注入
    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送消息的方法
     *   交换机名称,知道消息发送到哪个交换机
     *   路由键,指定消息要根据规则路由到相应队列
     * @param message     消息内容,要发送的具体消息
     */
    public void sendMessage(String message){
        rabbitTemplate.convertAndSend(BiMqConstant.BI_EXCHANGE_NAME,BiMqConstant.BI_ROUTING_KEY,message);
    }
}
