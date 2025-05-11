package org.example.movie.mq.consumer;

import org.example.movie.dto.CommentRequest;
import org.example.movie.entity.Movie;
import org.example.movie.entity.MovieComment;
import org.example.movie.repository.MovieCommentRepository;
import org.example.movie.service.InfoImportService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;



import java.sql.Timestamp;
import java.util.List;

@Component
public class CommentConsumer {

    @Autowired
    private MovieCommentRepository movieCommentRepository;
    @Autowired
    private InfoImportService infoImportService;

    @RabbitListener(queues = "comment_queue", ackMode = "MANUAL")
    public void receiveAddCommentMessage(CommentRequest commentRequest, Message message, Channel channel) {
        try {
            // 1. 构建评论实体
            MovieComment comment = new MovieComment();
            comment.setUserId(commentRequest.getUserId());
            comment.setMovieId(commentRequest.getMovieId());
            comment.setComment(commentRequest.getComment());
            comment.setCreatedAt(new Timestamp(System.currentTimeMillis()));

            // 2. 保存到数据库
            movieCommentRepository.save(comment);

            // 3. 手动 ACK 确认消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            // 4. 捕获异常，打印日志
            e.printStackTrace();

            try {
                // 5. Nack 通知，设置重新入队（或配置为进入死信队列）
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }



    @RabbitListener(queues = "movie_es_queue")
    public void receiveWriteEsMessage(List<Movie> movies) {
        infoImportService.storeMovieInfoToES(movies);
    }
}

