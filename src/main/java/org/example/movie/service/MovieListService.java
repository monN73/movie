package org.example.movie.service;

import org.example.movie.dto.MovieItem;
import org.example.movie.entity.Movie;
import org.example.movie.entity.MovieList;
import org.example.movie.repository.MovieListRepository;
import org.example.movie.repository.MovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class MovieListService {

    @Autowired
    private MovieListRepository repository;
    @Autowired
    private MovieListRepository movieListRepository;
    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private RedisTemplate redisTemplate;



    public boolean addMovieToUserList(Long userId, String movieId) {
        if (!repository.existsByUserIdAndMovieId(userId, movieId)) {
            MovieList entry = new MovieList();
            entry.setUserId(userId);
            entry.setMovieId(movieId);
            entry.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            repository.save(entry);
            return true;
        }
        return false;
    }

    public boolean deleteMovieFromUserList(Long userId, String movieId) {
        Optional<MovieList> existingList = movieListRepository.findUserByUserIdAndMovieId(userId,movieId);
        if (existingList.isPresent()) {
            movieListRepository.delete(existingList.get());
            return true;
        }
        return false;
    }

    public List<MovieItem> getMovieIdsByUserIdNative(long userId) {
        // Step 1: 获取用户收藏的电影 ID
        List<String> idList = movieListRepository.findMovieIdsByUserIdNative(userId);
        List<MovieItem> movieItemList = new ArrayList<>();

        if (idList.isEmpty()) return movieItemList;

        // Step 2: 构造 Redis key
        List<String> redisKeys = idList.stream()
                .map(id -> "movie:info:" + id)
                .collect(Collectors.toList());

        // Step 3: 批量从 Redis 获取数据
        List<Object> redisResults = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (String key : redisKeys) {
                connection.hGetAll(key.getBytes(StandardCharsets.UTF_8));
            }
            return null;
        });

        // Step 4: 解析 Redis 数据，同时记录 miss 的 ID
        List<String> missedIds = new ArrayList<>();
        for (int i = 0; i < redisResults.size(); i++) {
            Map<String, String> data = (Map<String, String>) redisResults.get(i);

            if (data == null || data.isEmpty()) {
                missedIds.add(idList.get(i));
            } else {
                MovieItem movieItem = new MovieItem();
                movieItem.setPosterPath(data.getOrDefault("posterPath", ""));
                movieItem.setTitle(data.getOrDefault("title", ""));
                movieItem.setOverview(data.getOrDefault("overview", ""));
                movieItem.setReleaseDate(data.getOrDefault("releaseDate", ""));
                movieItem.setMovieId(idList.get(i));
                movieItemList.add(movieItem);
            }
        }

        // Step 5: 批量从数据库补充未命中的数据
        if (!missedIds.isEmpty()) {
            List<Movie> missedMovies = movieRepository.findAllByUidIn(missedIds);

            // Step 6: 将 missed 数据添加到结果 &
            for (Movie movie : missedMovies) {
                MovieItem movieItem = new MovieItem();
                movieItem.setPosterPath(movie.getPosterPath());
                movieItem.setTitle(movie.getTitle());
                movieItem.setOverview(movie.getOverview());
                movieItem.setReleaseDate(movie.getReleaseDate());
                movieItem.setMovieId(movie.getUid());
                movieItemList.add(movieItem);

            }
        }

        return movieItemList;
    }



}
