package com.onion.backend.jwt;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Arrays;
import java.util.Base64;

public class SecretKeyGenerator {

    public static void main(String[] args) {
        SecretKey secretKey = Jwts.SIG.HS256.key().build();
        SecretKey equalsSecretKey = Keys.hmacShaKeyFor(secretKey.getEncoded());
        String encodeKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());
        System.out.println("encodeKey = " + encodeKey);
        System.out.println("decodeKey = " + Arrays.toString(Base64.getDecoder().decode(encodeKey)));
        System.out.println("Generated Secret Key: " + encodeKey);
    }
}