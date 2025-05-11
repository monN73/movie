package org.example.movie.entity;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

@Table(name = "personalMovieList",uniqueConstraints= @UniqueConstraint(columnNames={"user_id","movie_id"}))
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
// 用户添加的影片
public class MovieList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false,name = "user_Id")
    private long userId;

    @Column(nullable = false,name = "movie_Id")
    private String movieId;

    @Column(nullable = false)
    private Timestamp createdAt;


}
