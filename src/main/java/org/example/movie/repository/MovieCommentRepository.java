package org.example.movie.repository;

import org.example.movie.entity.MovieComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieCommentRepository extends JpaRepository<MovieComment, Long> {
    //返回一个电影的所有评论
    @Query(value = "SELECT * FROM moviecomments WHERE movie_id = :movieId", nativeQuery = true)
    List<MovieComment> findCommentsByMovieIdNative(@Param("movieId") String movieId);
    //返回一个用户的所有评论
    @Query(value = "SELECT * FROM moviecomments WHERE user_id = :userId", nativeQuery = true)
    List<MovieComment> findCommentsByUserIdNative(@Param("userId") long userId);

    boolean existsByUserIdAndMovieId(Long userId, String movieId);


    boolean existsById(Long commentId);

    void deleteById(Long commentId);
}
