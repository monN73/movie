package org.example.movie.controller;

import org.example.movie.dto.CommentRequest;
import org.example.movie.dto.ResponseComment;
import org.example.movie.service.MovieCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/comments")
public class CommentController {
    @Autowired
    private MovieCommentService commentService;
    @Autowired
    private MovieCommentService movieCommentService;

    @GetMapping("/movie/{movieId}")
    public List<ResponseComment> findCommentsByMovie(@PathVariable String movieId) {
        return commentService.findCommentsByMovieId(movieId);
    }

    @GetMapping("/user/{userId}")
    public List<ResponseComment> findCommentsByUser(@PathVariable long userId) {
        return commentService.findCommentsByUserId(userId);
    }

    @PostMapping("/add")
    public ResponseEntity<String> addComment(@RequestParam long userId,@RequestBody CommentRequest commentRequest) {
        movieCommentService.addMovieComment(userId,commentRequest);
        return new ResponseEntity<>("评论提交成功", HttpStatus.CREATED);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteComment(@RequestParam long commentId,@RequestParam long userId) {
        boolean deleted = movieCommentService.deleteComment(commentId);
        if (deleted) {
            return ResponseEntity.ok("删除成功");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("评论不存在");
        }
    }
}
