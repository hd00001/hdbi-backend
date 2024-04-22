package com.hd.hdbibackend.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.nio.charset.StandardCharsets;

public class SingleProducer {
//定义正在监听队列名称
    private final static String QUEUE_NAME = "hello";

    public static void main(String[] argv) throws Exception {
//        创建连接(工厂)
        ConnectionFactory factory = new ConnectionFactory();
//        主机名(本地RabbitMQ服务器)
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
//             创建新连接并获取新频道
        Channel channel = connection.createChannel();
//            创建队列,声明监听的队列
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
//            打印等待接收消息的信息
        System.out.println("[*]Waiting for messages. To exit press CTRL+C");
//            定义如何处理消息,创建新的DeliverCallback处理接受的信息
            DeliverCallback deliverCallback = (consumerTag, delivery) ->{
            String message = new String(delivery.getBody(),StandardCharsets.UTF_8);
            System.out.println(" [x] Sent '" + message + "'");
        };
//        在频道上开始消费队列的消息,接收到的消息会传递给delverCallback处理,会持续堵塞
        channel.basicConsume(QUEUE_NAME,true,deliverCallback,consumerTag -> { });


    }
}