package org.example.movie.task;

import org.example.movie.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MovieFetchTask {

    @Autowired
    private MovieService movieService;

    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨 2 点执行一次
    public void scheduledFetch() {
        movieService.fetchAllTrendingMovies("day");
        movieService.fetchAllTrendingMovies("week");
    }
}
