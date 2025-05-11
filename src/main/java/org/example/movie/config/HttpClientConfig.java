package org.example.movie.config;

import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class HttpClientConfig {

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .executor(Executors.newFixedThreadPool(20)) // 统一线程池
                .version(HttpClient.Version.HTTP_1_1)       // 启用连接复用
                .build();
    }

    @Bean
    public ExecutorService httpClientExecutor() {
        return Executors.newFixedThreadPool(20); // 暴露线程池以便关闭
    }

    @PreDestroy
    public void cleanup() {
        httpClientExecutor().shutdownNow(); // 应用关闭时清理
    }
}
