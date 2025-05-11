package org.example.movie.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ESIndexInitializer {

    private static final String INDEX_NAME = "movies";

    @Autowired
    private ElasticsearchClient client;

    @PostConstruct
    public void createIndexIfNotExists() {
        try {
            // 1. 判断索引是否已存在 - 新版API直接返回boolean
            boolean exists = client.indices().exists(ExistsRequest.of(e -> e.index(INDEX_NAME))).value();

            if (exists) {
                System.out.println("[ElasticSearch] 索引已存在: " + INDEX_NAME);
                return;
            }

            // 2. 创建索引结构
            CreateIndexResponse response = client.indices().create(c -> c
                    .index(INDEX_NAME)
                    .settings(s -> s
                            .numberOfShards("1")
                            .numberOfReplicas("1")
                    )
                    .mappings(m -> m
                            .properties("uid", p -> p.long_(v -> v))
                            .properties("title", p -> p.text(t -> t.analyzer("standard")))
                            .properties("posterPath", p -> p.keyword(k -> k))
                            .properties("overview", p -> p.text(t -> t.analyzer("standard")))
                            .properties("releaseDate", p -> p.date(d -> d.format("yyyy-MM-dd")))
                    )
            );

            System.out.println("[ElasticSearch] 索引创建成功: " + response.acknowledged());

        } catch (IOException e) {
            System.err.println("[ElasticSearch] 索引创建失败");
            e.printStackTrace();
        }
    }
}