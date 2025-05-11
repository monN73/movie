package org.example.movie;

import org.example.movie.service.MovieService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class MovieApplication {
    public static void main(String[] args) {
        SpringApplication.run(MovieApplication.class, args);
    }
}
