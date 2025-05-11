package org.example.movie.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private final String secret = "MySuperSecretKeyForJWTThatShouldBeLongEnough";
    private final long expiration = 86400000; // 1天

    private final Key key = Keys.hmacShaKeyFor(secret.getBytes());

    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public Date extractExpiration(String token) {
        return Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
    }

    public boolean isTokenExpiringSoon(String token) {
        Date expirationDate = extractExpiration(token);
        long now = System.currentTimeMillis();
        long diff = expirationDate.getTime() - now;
        // 如果剩下不到30分钟，就认为快到期
        return diff < (10 * 60 * 1000);
    }
}
