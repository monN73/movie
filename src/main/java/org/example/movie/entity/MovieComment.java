package org.example.movie.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@Table(name = "moviecomments")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MovieComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false,name = "user_Id")
    private long userId;

    @Column(nullable = false,name = "movie_Id")
    private String movieId;

    @Column(nullable = false)
    private Timestamp createdAt;

    @Column(nullable = false,name = "comment")
    private String comment;
}
