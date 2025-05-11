package org.example.movie.controller;

import org.example.movie.dto.MovieInfo;
import org.example.movie.dto.MovieRank;
import org.example.movie.repository.MovieRepository;
import org.example.movie.service.MovieService;
import org.example.movie.service.ShowMovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RequestMapping("/movies")
@RestController
public class MovieController {
    private final MovieService movieService;
    private final MovieRepository movieRepository;
    private final ShowMovieService showMovieService;

    @Autowired
    public MovieController(MovieService movieService, MovieRepository movieRepository, ShowMovieService showMovieService) {
        this.movieService = movieService;
        this.movieRepository = movieRepository;
        this.showMovieService = showMovieService;
    }

    @PostMapping("/fetch-all")
    public ResponseEntity<String> fetchAll() {
        movieService.fetchAllTrendingMovies("day");
        movieService.fetchAllTrendingMovies("week");
        return ResponseEntity.accepted().body("Data fetching started in background.");
    }

    @PostMapping("/more/{uid}")
    public ResponseEntity<MovieInfo> more(@PathVariable String uid) throws IOException, InterruptedException {
        MovieInfo movieInfo = showMovieService.getMovieInfo(uid);
        return ResponseEntity.ok(movieInfo);
    }

    @GetMapping("/trending/{when}/{page}")
    public ResponseEntity<List<MovieRank>> trending(@PathVariable String when, @PathVariable int page) {
        try {
            // 验证参数
            if (!"day".equals(when) && !"week".equals(when)) {
                return ResponseEntity.badRequest().build();
            }

            if (page < 1) {
                return ResponseEntity.badRequest().build();
            }

            // 获取数据
            List<MovieRank> ranks = showMovieService.getMovieRank(when, page);

            // 处理空结果
            if (ranks.isEmpty()) {
                return ResponseEntity.noContent().build();
            }

            return ResponseEntity.ok(ranks);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
