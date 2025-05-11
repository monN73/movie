package org.example.movie.service;

import org.example.movie.dto.CommentRequest;
import org.example.movie.dto.ResponseComment;
import org.example.movie.entity.MovieComment;
import org.example.movie.mq.producer.CommentProducer;
import org.example.movie.repository.MovieCommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Service
public class MovieCommentService {
    @Autowired
    private MovieCommentRepository movieCommentRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private MovieService movieService;
    @Autowired
    private CommentProducer commentProducer;

    public void addMovieComment(Long userId, CommentRequest commentRequest) {
        commentProducer.sendAddCommentMessage(commentRequest);
    }

    public boolean deleteComment(Long commentId) {
        if (movieCommentRepository.existsById(commentId)) {
            movieCommentRepository.deleteById(commentId);
            return true;
        }
        return false;
    }

    public List<ResponseComment> findCommentsByMovieId(String movieId) {
        List<MovieComment> comments = movieCommentRepository.findCommentsByMovieIdNative(movieId);
        String title = movieService.getMovieTitle(movieId); // 假设 title 每条都一样，提前查一次

        List<ResponseComment> responseList = new ArrayList<>();
        for (MovieComment comment : comments) {
            String userName = userService.getUserName(comment.getUserId());
            ResponseComment rc = new ResponseComment(
                    title,
                    userName,
                    comment.getCreatedAt(),
                    comment.getComment(),
                    comment.getUserId(),
                    movieId
            );
            responseList.add(rc);
        }

        return responseList;
    }

    public List<ResponseComment> findCommentsByUserId(Long userId) {
        List<MovieComment> comments = movieCommentRepository.findCommentsByUserIdNative(userId);
        String userName = userService.getUserName(userId); // 假设 title 每条都一样，提前查一次

        List<ResponseComment> responseList = new ArrayList<>();
        for (MovieComment comment : comments) {
            String title = movieService.getMovieTitle(comment.getMovieId());
            ResponseComment rc = new ResponseComment(
                    title,
                    userName,
                    comment.getCreatedAt(),
                    comment.getComment(),
                    userId,
                    comment.getMovieId()
            );
            responseList.add(rc);
        }

        return responseList;
    }

}
