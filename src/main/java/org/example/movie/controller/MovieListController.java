package org.example.movie.controller;

import org.example.movie.dto.MovieItem;
import org.example.movie.service.MovieListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/movieList")
public class MovieListController {
    @Autowired
    private MovieListService service;

    @PostMapping("/add")
    public ResponseEntity<?> addMovie(@RequestParam Long userId,
                                      @RequestParam String movieId) {
        boolean added = service.addMovieToUserList(userId, movieId);
        return added ?
                ResponseEntity.ok("Movie added.") :
                ResponseEntity.status(409).body("Already exists.");
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteMovie(@RequestParam Long userId,
                                      @RequestParam String movieId) {
        boolean deleted = service.deleteMovieFromUserList(userId, movieId);
        return deleted ?
                ResponseEntity.ok("Movie deleted.") :
                ResponseEntity.status(409).body("List doesn't exists.");
    }

    @GetMapping("/getList")
    public ResponseEntity<List<MovieItem>> getUserMovieList(@RequestParam Long userId) {
        if (userId == null || userId <= 0) {
            return ResponseEntity.badRequest().build();
        }

        List<MovieItem> movieItems = service.getMovieIdsByUserIdNative(userId);
        return ResponseEntity.ok(movieItems);
    }
}
