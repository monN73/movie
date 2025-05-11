package org.example.movie.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.example.movie.dto.MovieItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class MovieSearchService {

    @Autowired
    private ElasticsearchClient client;

    /**
     * 根据关键字搜索电影
     */
    public List<MovieItem> searchMovies(String keyword) {
        List<MovieItem> results = new ArrayList<>();
        try {
            SearchResponse<MovieItem> response = client.search(s -> s
                            .index("movies")
                            .query(q -> q
                                    .bool(b -> b
                                            .should(sh -> sh
                                                    .multiMatch(m -> m
                                                            .fields("title^4", "overview^2") // 设置高权重
                                                            .query(keyword)
                                                            .fuzziness("0") // 完全匹配（fuzziness=0）
                                                    )
                                            )
                                            .should(sh -> sh
                                                    .multiMatch(m -> m
                                                            .fields("title", "overview")
                                                            .query(keyword)
                                                            .fuzziness("AUTO") // 模糊匹配
                                                    )
                                            )
                                    )
                            )
                            .size(20),
                    MovieItem.class
            );

            for (Hit<MovieItem> hit : response.hits().hits()) {
                results.add(hit.source());
            }

        } catch (IOException e) {
            System.err.println("搜索失败：" + e.getMessage());
        }

        return results;
    }
}
