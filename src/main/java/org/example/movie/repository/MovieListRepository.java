package org.example.movie.repository;

import org.example.movie.entity.MovieList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieListRepository extends JpaRepository<MovieList, Long> {
    //List<MovieList> findByUserId(Long userId);
    boolean existsByUserIdAndMovieId(Long uid, String movieId);

    Optional<MovieList> findUserByUserIdAndMovieId(Long userId, String movieId);

    @Query(value = "SELECT movie_id FROM personal_movie_list WHERE user_id = :userId", nativeQuery = true)
    List<String> findMovieIdsByUserIdNative(@Param("userId") long userId);
}

