package org.example.movie.mq.consumer;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.movie.config.RabbitConfig;
import org.example.movie.dto.CommentRequest;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import com.rabbitmq.client.Channel;

import java.nio.charset.StandardCharsets;

@Component
public class DeadLetterConsumer {

    @Autowired
    private ObjectMapper objectMapper;// Spring Boot 会自动配置 Jackson 的 ObjectMapper

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = "comment_dlx_queue", ackMode = "MANUAL")
    public void handleDeadCommentMessage(Message message, Channel channel) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            CommentRequest request = objectMapper.readValue(body, CommentRequest.class);

            // 使用 Redis 记录重试次数
            String retryKey = "retry:comment:" + request.getUserId() + ":" + request.getMovieId();
            Long retryCount = redisTemplate.opsForValue().increment(retryKey);

            if (retryCount <= 3) {
                // 重新发送到原始队列
                rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, RabbitConfig.ROUTING_KEY, request);
                System.out.println("第 " + retryCount + " 次重试评论消息，已重新投递");
            } else {
                System.err.println("重试超限，记录日志/发告警/人工处理");
                // 可保存入日志表或通知开发运维
            }

            // 确认已消费死信
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

        } catch (Exception e) {
            e.printStackTrace();
            try {
                // 死信处理失败时再次 Nack（一般配置 false 不再重新入队）
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

}

