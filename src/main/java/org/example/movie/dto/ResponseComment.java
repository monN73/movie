package org.example.movie.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ResponseComment {
    private String title;
    private String userName;
    private Timestamp createdAt;
    private String comment;
    private Long userId;
    private String movieId;


}
