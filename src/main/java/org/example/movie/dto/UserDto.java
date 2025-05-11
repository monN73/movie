package org.example.movie.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    String username;
    String avatarUrl;
    String posterUrl;
    Long id;
}
