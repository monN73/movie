package org.example.movie.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.MgetResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.mget.MultiGetResponseItem;
import org.example.movie.dto.MovieItem;
import org.example.movie.entity.Movie;
import org.example.movie.repository.MovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class InfoImportService {
    @Autowired
    private ElasticsearchClient client;

    @Autowired
    private MovieRepository movieRepository;

    public void storeMovieInfoToES(List<Movie> movies) {
        if (movies == null || movies.isEmpty()) return;

        List<String> ids = movies.stream()
                .map(movie -> String.valueOf(movie.getUid()))
                .toList();

        // 1. 查询已存在的文档 ID
        List<String> existingIds = new ArrayList<>();
        try {
            MgetResponse<MovieItem> mgetResponse = client.mget(
                    m -> m.index("movies").ids(ids),
                    MovieItem.class
            );

            for (MultiGetResponseItem<MovieItem> item : mgetResponse.docs()) {
                if (item.result().found()) {
                    existingIds.add(item.result().id());
                }
            }
        } catch (IOException e) {
            System.err.println("查询已有文档失败：" + e.getMessage());
            return;
        }

        // 2. 批量构建未存在的写入操作
        List<BulkOperation> operations = new ArrayList<>();
        for (Movie movie : movies) {
            String uid = String.valueOf(movie.getUid());
            if (existingIds.contains(uid)) continue;

            MovieItem item = new MovieItem();
            item.setTitle(movie.getTitle());
            item.setPosterPath(movie.getPosterPath());
            item.setOverview(movie.getOverview());
            item.setReleaseDate(movie.getReleaseDate());

            BulkOperation op = BulkOperation.of(b -> b
                    .index(idx -> idx
                            .index("movies")
                            .id(uid)
                            .document(item)
                    )
            );
            operations.add(op);
        }

        // 3. 执行 bulk 写入
        if (!operations.isEmpty()) {
            try {
                client.bulk(b -> b.operations(operations));
            } catch (IOException e) {
                System.err.println("批量导入失败：" + e.getMessage());
            }
        }
    }

}
