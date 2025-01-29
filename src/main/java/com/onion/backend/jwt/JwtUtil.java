package com.onion.backend.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {
    // SecretKey secretKey = Jwts.SIG.HS256.key().build();
    SecretKey secretKey;

    public JwtUtil() {
        // 서버 비밀키 지정
        this.secretKey = Keys.hmacShaKeyFor(base64toByte("WwJpw5GVnpYG+8ZqcbC/viTvuroCbFz4k6NRc1K6IPE="));
    }

    public String generateToken(String username) {
        Date now = new Date();
        long expirationTime = 60L * 60L * 24L * 365L * 1000L; // 만료시간 1년
        Date expirationDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String jws) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build().parseSignedClaims(jws);
            return true;
        } catch (JwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public String getUsernameFromToken(String jws) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build().parseSignedClaims(jws)
                .getPayload();
        return claims.getSubject();
    }

    public byte[] base64toByte(String base64) {
        return Arrays.toString(Base64.getDecoder().decode(base64)).getBytes();
    }

    /**
     * 토큰 만료일자
     * @param token
     * @return
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build().parseSignedClaims(token)
                .getPayload();
        return claims.getExpiration();
    }
}
