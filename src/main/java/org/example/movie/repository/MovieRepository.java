package org.example.movie.repository;

import org.example.movie.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    Optional<Movie> findMovieByUid(String uid);
    boolean existsByUid(String uid);
    List<Movie> findAllByUidIn(List<String> missedIds);
}
