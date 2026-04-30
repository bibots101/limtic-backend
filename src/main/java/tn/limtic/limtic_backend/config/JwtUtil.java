package tn.limtic.limtic_backend.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private final String SECRET = "limtic-secret-key-2024-very-long-string-must-be-256-bits";
    private final long EXPIRATION = 86400000;
    private Key getKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    public String generateToken(String email, String role) {
        return Jwts.builder()
            .setSubject(email)           
            .claim("role", role)         
            .setIssuedAt(new Date())     
            .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
            .signWith(getKey(), SignatureAlgorithm.HS256)
            .compact();                  
    }

    public String getEmail(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getKey()).build()
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
    }

    public boolean isValid(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getKey()).build()
                .parseClaimsJws(token); 
            return true;
        } catch (Exception e) {
            return false; 
        }
    }
}