package org.example.movie.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;




@Configuration
public class RabbitConfig {

    public static final String QUEUE_NAME = "comment_queue";
    public static final String EXCHANGE_NAME = "comment_exchange";
    public static final String ROUTING_KEY = "comment_key";

    public static final String MOVIE_QUEUE_NAME = "movie_es_queue";
    public static final String MOVIE_EXCHANGE_NAME = "movie_es_exchange";
    public static final String MOVIE_ROUTING_KEY = "movie_es_key";

    public static final String DLX_QUEUE = "comment_dlx_queue";
    public static final String DLX_EXCHANGE = "comment_dlx_exchange";
    public static final String DLX_ROUTING_KEY = "comment_dlx_key";

    @Bean
    public Queue movieQueue() {
        return QueueBuilder.durable(QUEUE_NAME)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange movieExchange() {
        return new DirectExchange(EXCHANGE_NAME,true,false);
    }

    @Bean
    public Binding binding(Queue movieQueue, DirectExchange movieExchange) {
        return BindingBuilder.bind(movieQueue).to(movieExchange).with(ROUTING_KEY);
    }

    @Bean
    public Queue movieEsQueue() {
        return new Queue(MOVIE_QUEUE_NAME, true);
    }

    @Bean
    public DirectExchange movieEsExchange() {
        return new DirectExchange(MOVIE_EXCHANGE_NAME,true,false);
    }

    @Bean
    public Binding movieEsBinding(Queue movieEsQueue, DirectExchange movieEsExchange) {
        return BindingBuilder.bind(movieEsQueue).to(movieEsExchange).with(MOVIE_ROUTING_KEY);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    @Bean
    public Queue dlxQueue() {
        return new Queue(DLX_QUEUE);
    }

    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(dlxQueue()).to(dlxExchange()).with(DLX_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jacksonMessageConverter());
        return rabbitTemplate;
    }


}
