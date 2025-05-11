package org.example.movie.mq.producer;

import org.example.movie.dto.CommentRequest;
import org.example.movie.entity.Movie;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CommentProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendAddCommentMessage(CommentRequest commentRequest) {
        rabbitTemplate.convertAndSend("comment_exchange", "comment_key", commentRequest);
    }

    public void sendWriteEsMessage(List<Movie> movies){
        rabbitTemplate.convertAndSend("movie_es_exchange", "movie_es_key", movies);
    }

}
