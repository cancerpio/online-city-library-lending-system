package com.library.auth;

/**
 * 館員驗證服務
 * 用於驗證館員身份，支援開發環境和正式環境的Implementation
 */
public interface LibrarianVerificationService {
    
    /**
     * 驗證館員身份
     * @param librarianId 館員ID
     * @return true 如果驗證成功，false 如果驗證失敗
     */
    boolean verifyLibrarian(String librarianId);
}
