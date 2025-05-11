package org.example.movie.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.movie.dto.MovieRank;
import org.example.movie.dto.MovieInfo;
import org.example.movie.entity.Movie;
import org.example.movie.repository.MovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Service
public class ShowMovieService {
    private final HttpClient httpClient; // 注入全局实例

    @Autowired
    public ShowMovieService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }
    @Autowired
    private RedisTemplate redisTemplate;


    @Autowired
    private MovieRepository movieRepository;

    @Value("${tmdb.api.token}")
    private String apiToken;

    @Value("${omdb.apikey}")
    private String apiKey;


    public void storeMovieRank(List<Movie> movies, String when) {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String rankKey = "rank:" + when + ":" + date + "_tmp";
        String posterKey = "movie:posters";
        String titleKey = "movie:title";

        // Pipeline 批量操作
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Movie movie : movies) {
                // 1. 存储排名(ZSET)
                int rank;
                if("day".equals(when)) rank = movie.getRankToday();
                else rank = movie.getRankWeek();
                connection.zAdd(
                        rankKey.getBytes(StandardCharsets.UTF_8),
                        rank,
                        movie.getUid().getBytes(StandardCharsets.UTF_8)
                );
                connection.expire(rankKey.getBytes(StandardCharsets.UTF_8), 86400L);

                // 2. 存储海报(HASH)
                connection.hSet(
                        posterKey.getBytes(StandardCharsets.UTF_8),
                        movie.getUid().getBytes(StandardCharsets.UTF_8),
                        (movie.getPosterPath() != null ? movie.getPosterPath() : "").getBytes(StandardCharsets.UTF_8)
                );
                connection.expire(posterKey.getBytes(StandardCharsets.UTF_8), 86400L);

                // 3. 存储标题(HASH)
                connection.hSet(
                        titleKey.getBytes(StandardCharsets.UTF_8),
                        movie.getUid().getBytes(StandardCharsets.UTF_8),
                        movie.getTitle().getBytes(StandardCharsets.UTF_8)
                );
            }
            connection.expire(rankKey.getBytes(StandardCharsets.UTF_8), 86400L);
            connection.expire(posterKey.getBytes(StandardCharsets.UTF_8), 86400L);
            connection.expire(titleKey.getBytes(StandardCharsets.UTF_8), 86400L);

            return null;
        });


    }

    public List<MovieRank> getMovieRank(String when, int page) {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String latestPointerKey = "rank:" + when + ":latest";
        String rankKey = redisTemplate.opsForValue().get(latestPointerKey).toString();
        String posterKey = "movie:posters";
        String titleKey = "movie:title";

        // 1. 获取指定页的排名数据（每页 20 条）
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .rangeWithScores(rankKey, (page - 1) * 20, page * 20 - 1);

        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 提取 movieId 列表
        List<String> movieIds = tuples.stream()
                .map(ZSetOperations.TypedTuple::getValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 3. 使用 pipelined + 序列化器统一处理获取海报和标题
        RedisSerializer<String> stringSerializer = redisTemplate.getStringSerializer();

        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (String movieId : movieIds) {
                byte[] idBytes = stringSerializer.serialize(movieId);
                connection.hGet(stringSerializer.serialize(posterKey), idBytes);
                connection.hGet(stringSerializer.serialize(titleKey), idBytes);
            }
            return null;
        });

        // 4. 组装结果
        List<MovieRank> ranks = new ArrayList<>();
        int index = 0;

        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String movieId = tuple.getValue();
            Double score = tuple.getScore();

            if (movieId == null || score == null) continue;

            MovieRank rank = new MovieRank();
            rank.setId(movieId);
            rank.setRank(score.intValue());

            // 从 results 中取出 poster 和 title
            if (index < results.size() && results.get(index) != null) {
                rank.setPosterPath((String) results.get(index));
            }
            index++;

            if (index < results.size() && results.get(index) != null) {
                rank.setTitle((String) results.get(index));
            }
            index++;

            ranks.add(rank);
        }

        return ranks;
    }

    public void storeMovieInfo(List<Movie> movies, String when) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Movie movie : movies) {
                String redisKey = "movie:info:" + movie.getUid();
                Map<byte[], byte[]> hash = new HashMap<>();

                putIfNotBlank(hash, "title", movie.getTitle());
                putIfNotBlank(hash, "posterPath", movie.getPosterPath());
                putIfNotBlank(hash, "backdropPath", movie.getBackdropPath());
                putIfNotBlank(hash, "overview", movie.getOverview());
                putIfNotBlank(hash, "releaseDate", movie.getReleaseDate());

                if (!hash.isEmpty()) {
                    connection.hMSet(redisKey.getBytes(StandardCharsets.UTF_8), hash);
                    connection.expire(redisKey.getBytes(StandardCharsets.UTF_8), 86400L);
                }

            }
            return null;
        });
    }

    private void putIfNotBlank(Map<byte[], byte[]> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key.getBytes(StandardCharsets.UTF_8), value.getBytes(StandardCharsets.UTF_8));
        }
    }

    // redis里的数据不全，对已有的数据进行补充，详情页使用
    public MovieInfo getMovieInfo(String uid) throws IOException, InterruptedException {
        String redisKey = "movie:info:" + uid;
        ReentrantReadWriteLock lock = getLock(uid);

        lock.readLock().lock();
        try {
            Map<String, String> info = redisTemplate.opsForHash().entries(redisKey);
            if (info.containsKey("imdbID")) {
                return buildMovieInfo(info);
            }
            if ("1".equals(info.get("not-exist"))) {
                return null;
            }
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            // 双重检查
            Map<String, String> info = redisTemplate.opsForHash().entries(redisKey);
            if (info.containsKey("imdbID")) {
                return buildMovieInfo(info);
            }

            Optional<Movie> movie = Optional.of(new Movie());
            movie = movieRepository.findMovieByUid(uid);

            if (movie.isEmpty()) {
                //  如果数据库也没有，缓存 "不存在" 标记，防止穿透
                redisTemplate.execute((RedisCallback<Object>) connection -> {
                    Map<byte[], byte[]> emptyHash = new HashMap<>();
                    emptyHash.put("not-exist".getBytes(StandardCharsets.UTF_8), "1".getBytes(StandardCharsets.UTF_8));
                    connection.hMSet(redisKey.getBytes(StandardCharsets.UTF_8), emptyHash);
                    connection.expire(redisKey.getBytes(StandardCharsets.UTF_8), 300L); // 5分钟
                    return null;
                });
                return null;
            }

            // 获取电影数据并写入缓存
            Movie currentMovie = getMovieByUidAndGetMore(uid);
            Map<byte[], byte[]> hash = new HashMap<>();
            putIfNotBlank(hash, "imdbID", currentMovie.getImdbID());
            putIfNotBlank(hash, "budget", currentMovie.getBudget());
            putIfNotBlank(hash, "imdbRating", currentMovie.getImdbRating());
            putIfNotBlank(hash, "imdbVotes", currentMovie.getImdbVotes());
            putIfNotBlank(hash, "boxOffice", currentMovie.getBoxOffice());

            if (!hash.isEmpty()) {
                redisTemplate.execute((RedisCallback<Object>) connection -> {
                    connection.hMSet(redisKey.getBytes(StandardCharsets.UTF_8), hash);
                    connection.expire(redisKey.getBytes(StandardCharsets.UTF_8), 86400L);
                    return null;
                });
            }

            info = redisTemplate.opsForHash().entries(redisKey);
            return buildMovieInfo(info);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private ReentrantReadWriteLock getLock(String uid) {
        return movieLocks.computeIfAbsent(uid, k -> new ReentrantReadWriteLock());
    }

    private final ConcurrentHashMap<String, ReentrantReadWriteLock> movieLocks = new ConcurrentHashMap<>();


    public MovieInfo buildMovieInfo(Map<String, String> info) {
        MovieInfo movieInfo = new MovieInfo();
        movieInfo.setBudget(info.get("budget"));
        movieInfo.setPosterPath(info.get("posterPath"));
        movieInfo.setTitle(info.get("title"));
        movieInfo.setBackdropPath(info.get("backdropPath"));
        movieInfo.setOverview(info.get("overview"));
        movieInfo.setReleaseDate(info.get("releaseDate"));
        movieInfo.setBoxOffice(info.get("boxOffice"));
        movieInfo.setImdbRating(info.get("imdbRating"));
        movieInfo.setImdbVotes(info.get("imdbVotes"));
        return movieInfo;
    }

    public Movie getMovieByUidAndGetMore(String Uid) throws IOException, InterruptedException {
        Optional<Movie> movieOptional = movieRepository.findMovieByUid(Uid);
        if (movieOptional.isEmpty()) {
            return null;
        }

        Movie movie = movieOptional.get();

        ObjectMapper mapper = new ObjectMapper();

        // 🔹 第一个请求：TMDB 获取 imdbID 和 budget
        CompletableFuture<JsonNode> tmdbFuture = CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.themoviedb.org/3/movie/" + Uid + "?language=en-US"))
                        .timeout(Duration.ofSeconds(5))
                        .header("accept", "application/json")
                        .header("Authorization", apiToken)
                        .method("GET", HttpRequest.BodyPublishers.noBody())
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                return mapper.readTree(response.body());
            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch TMDB data: " + e.getMessage());
            }
        });

        // 🔹 第二个请求：OMDb 获取 BoxOffice、imdbRating 和 imdbVotes
        CompletableFuture<JsonNode> omdbFuture = tmdbFuture.thenCompose(tmdbNode -> {
            String imdbID = tmdbNode.path("imdb_id").asText(null);
            if (imdbID == null) return CompletableFuture.completedFuture(null);

            return CompletableFuture.supplyAsync(() -> {
                try {
                    HttpRequest requestOMDB = HttpRequest.newBuilder()
                            .uri(URI.create("http://www.omdbapi.com/?i=" + imdbID + "&apikey=" + apiKey))
                            .timeout(Duration.ofSeconds(5))
                            .header("accept", "application/json")
                            .method("GET", HttpRequest.BodyPublishers.noBody())
                            .build();
                    HttpResponse<String> response1 = httpClient.send(requestOMDB, HttpResponse.BodyHandlers.ofString());
                    return mapper.readTree(response1.body());
                } catch (Exception e) {
                    throw new RuntimeException("Failed to fetch OMDb data: " + e.getMessage());
                }
            });
        });

        // 🔹 获取结果
        JsonNode tmdbData = tmdbFuture.join(); // 等待 TMDB API
        JsonNode omdbData = omdbFuture.join(); // 等待 OMDb API

        // 设置 TMDB 数据
        movie.setImdbID(tmdbData.path("imdb_id").asText(null));
        movie.setBudget(tmdbData.path("budget").asText(null));

        // 设置 OMDb 数据（如果有）
        if (omdbData != null) {
            movie.setBoxOffice(omdbData.path("BoxOffice").asText(null));
            movie.setImdbRating(omdbData.path("imdbRating").asText(null));
            movie.setImdbVotes(omdbData.path("imdbVotes").asText(null));
        }

        return movie;
    }






}
