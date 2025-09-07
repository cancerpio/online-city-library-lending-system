package com.library.auth;

import com.library.config.JwtConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
public class JwtTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtConfig jwtConfig;

    @Test
    public void testJwtTokenGeneration() {
        // 測試 產生JWT Token
        String token = jwtUtil.generateToken(1L, "testuser", "Member");
        assertNotNull(token);
        
        // 測試 JWT Token 驗證
        assertTrue(jwtUtil.validateToken(token));
        
        // 測試從JWT Token取得 UserId, Username, Role等需要的資訊
        assertEquals(1L, jwtUtil.extractUserId(token));
        assertEquals("testuser", jwtUtil.extractUsername(token));
        assertEquals("Member", jwtUtil.extractRole(token));
    }
}
