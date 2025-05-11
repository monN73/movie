package org.example.movie.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.example.movie.entity.Movie;
import org.example.movie.mq.producer.CommentProducer;
import org.example.movie.repository.MovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class MovieService {

    private final MovieRepository movieRepository;
    @Autowired
    private ShowMovieService showMovieService;
    @Autowired
    private InfoImportService infoImportService;
    @Autowired
    private CommentProducer commentProducer;

    @Autowired
    public MovieService(MovieRepository movieRepository, RedisTemplate<String, Object> redisTemplate,HttpClient httpClient) {
        this.movieRepository = movieRepository;
        this.redisTemplate = redisTemplate;
        this.httpClient = httpClient;
    }

    @Autowired
    private final RedisTemplate<String, Object> redisTemplate;

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow(); // 立即终止所有任务
    }

    private final HttpClient httpClient;




    @Value("${tmdb.api.token}")
    private String apiToken;


    private static final int MAX_CONCURRENT_REQUESTS = 10;// 最大并发请求数

    private final ExecutorService executor = Executors.newFixedThreadPool(MAX_CONCURRENT_REQUESTS);

    public void fetchAllTrendingMovies(String when) {
        List<Integer> failedPages = Collections.synchronizedList(new ArrayList<>());

        // 构造临时 Redis Key
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String tmpKey = "rank:" + when + ":" + date + "_tmp";
        String finalKey = "rank:" + when + ":" + date;
        String latestPointerKey = "rank:" + when + ":latest";

        // 确保开始前清空 tmpKey
        redisTemplate.delete(tmpKey);

        int batchsize = 100;
        // 构建 fetch + Redis 存储任务列表
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int page = 1; page <= 500; page++) {
            int finalPage = page;
            List<Movie> batchMovies = Collections.synchronizedList(new ArrayList<>());
            tasks.add(() -> {
                try {
                    List<Movie> movies = fetchTrending(finalPage, when);
                    movieRepository.saveAll(movies);

                    synchronized (batchMovies) {
                        batchMovies.addAll(movies);
                        if (batchMovies.size() >= batchsize) {
                            List<Movie> batchToSend = new ArrayList<>(batchMovies);
                            batchMovies.clear();
                            commentProducer.sendWriteEsMessage(batchToSend);
                        }
                    }

                    showMovieService.storeMovieRank(movies, when);
                    showMovieService.storeMovieInfo(movies, when);

                } catch (Exception e) {
                    System.err.println("Error fetching page " + finalPage + ": " + e.getMessage());
                    failedPages.add(finalPage);
                }
                return null;
            });
        }

        // 执行所有任务
        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Initial fetching interrupted.");
        }

        // 重试逻辑
        int maxRetries = 3;
        int attempt = 0;
        while (!failedPages.isEmpty() && attempt < maxRetries) {
            attempt++;
            System.out.println("Retry attempt " + attempt + " for pages: " + failedPages);
            List<Integer> retryList = new ArrayList<>(failedPages);
            failedPages.clear();

            List<Callable<Void>> retryTasks = new ArrayList<>();
            for (int page : retryList) {
                int finalAttempt = attempt;
                retryTasks.add(() -> {
                    try {
                        List<Movie> movies = fetchTrending(page, when);
                        infoImportService.storeMovieInfoToES(movies);
                        movieRepository.saveAll(movies);
                        showMovieService.storeMovieRank(movies, when);
                        showMovieService.storeMovieInfo(movies, when);
                    } catch (Exception e) {
                        System.err.println("Retry " + finalAttempt + " failed for page " + page + ": " + e.getMessage());
                        failedPages.add(page);
                    }
                    return null;
                });
            }

            try {
                executor.invokeAll(retryTasks);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Retry interrupted.");
            }
        }

        // 所有数据写入完成后，重命名临时 Key 为正式 Key
        redisTemplate.rename(tmpKey, finalKey);
        redisTemplate.opsForValue().set(latestPointerKey, finalKey);
    }



    public List<Movie> fetchTrending(int page,String when) throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.themoviedb.org/3/trending/movie/"+when+"?language=en-US&page=" + page))
                .timeout(Duration.ofSeconds(5))
                .header("accept", "application/json")
                .header("Authorization", apiToken)
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(response.body());
        JsonNode result = jsonNode.get("results");
        ArrayList<Movie> movies = new ArrayList<>();

        List<String> movieIds = new ArrayList<>();
        for (JsonNode movieNode : result) {
            movieIds.add(movieNode.get("id").asText());
        }

// 一次性查出已有的电影
        List<Movie> existingMovies = movieRepository.findAllByUidIn(movieIds);
        Map<String, Movie> existingMovieMap = existingMovies.stream()
                .collect(Collectors.toMap(Movie::getUid, Function.identity()));

        for (int i = 0; i < result.size(); i++) {

            JsonNode movieNode = result.get(i);
            String id = movieNode.get("id").asText();
            Optional<Movie> existingMovie = movieRepository.findMovieByUid(id);
            int currentRank = (page - 1) * 20 + i + 1;
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")).trim();
            Movie movie;

            if (!existingMovieMap.containsKey(id)) {
                movie = new Movie();

                movie.setUid(id);
                movie.setType(movieNode.get("media_type").asText());
                movie.setTitle(movieNode.get("title").asText());
                movie.setOriginal_title(movieNode.get("original_title").asText(null));
                movie.setReleaseDate(movieNode.path("release_date").asText(null));
                movie.setOverview(movieNode.path("overview").asText(null));
                movie.setBackdropPath(movieNode.path("backdrop_path").asText(null));
                movie.setPosterPath(movieNode.path("poster_path").asText(null));
                movies.add(movie);

                if("day".equals(when)) {
                    movie.setRankToday(currentRank);
                    movie.setUpdateToday(today);
                }
                else if("week".equals(when)){
                    movie.setRankWeek(currentRank);
                    movie.setUpdateWeek(today);
                }

            } else {
                movie = existingMovie.get();
                if("day".equals(when)) {
                    movie.setRankToday(currentRank);
                    movie.setUpdateToday(today);
                }
                else if("week".equals(when)){
                    movie.setRankWeek(currentRank);
                    movie.setUpdateWeek(today);
                }
                movies.add(movie);
            }
        }
        return movies;
    }

    public String getMovieTitle(String id) {
        return movieRepository.findMovieByUid(id).get().getTitle();
    }




}
