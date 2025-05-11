package org.example.movie.controller;

import org.example.movie.dto.MovieItem;
import org.example.movie.service.MovieSearchService;
import org.example.movie.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@RequestMapping("/search")
@RestController
public class SearchController {

    @Autowired
    private MovieSearchService movieSearchService;

    @GetMapping("/keyword")
    public List<MovieItem> search(@RequestParam String keyword) {
        return movieSearchService.searchMovies(keyword);
    }
}
