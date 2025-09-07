package com.library.auth;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 館員驗證服務 - Mock
 * 在Dev環境中直接回傳 true，模擬驗證成功
 */
@Service
@Profile("dev")
public class MockLibrarianVerificationService implements LibrarianVerificationService {
    
    @Override
    public boolean verifyLibrarian(String librarianId) {
        // 開發環境：直接回傳 true，模擬驗證成功
        System.out.println("[MOCK] 驗證館員 ID: " + librarianId + " -> 成功");
        return true;
    }
}
