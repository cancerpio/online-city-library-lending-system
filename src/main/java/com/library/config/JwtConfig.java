package com.library.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Configuration
public class JwtConfig {

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${jwt.expiration:3600}")
    private int jwtExpiration; // 預設 1 小時

    @Bean
    public SecretKey jwtSecretKey() {
        String secret = "mySecretKeyForDevelopmentOnly123456789012345678901234567890";
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public int getJwtExpiration() {
        return jwtExpiration;
    }
}
