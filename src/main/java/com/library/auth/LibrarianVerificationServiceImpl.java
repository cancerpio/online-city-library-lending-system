package com.library.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 實際的館員驗證服務
 * 呼叫外部 API 驗證館員身份
 */
@Service
@Profile("prod")
public class LibrarianVerificationServiceImpl implements LibrarianVerificationService {
    
    @Value("${library.external-api.url:https://todo.com.tw}")
    private String externalApiUrl;
    
    @Value("${library.external-api.auth-header:todo}")
    private String authHeader;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Override
    public boolean verifyLibrarian(String librarianId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authHeader);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // 呼叫外部 API by restTemplate
            ResponseEntity<String> response = restTemplate.exchange(
                externalApiUrl,
                HttpMethod.GET,
                entity,
                String.class
            );
            
            // 檢查回應狀態碼
            boolean isSuccess = response.getStatusCode().is2xxSuccessful();
            
            System.out.println("[REAL] 驗證館員 ID: " + librarianId +
                             " -> " + (isSuccess ? "成功" : "失敗") + 
                             " (HTTP " + response.getStatusCode() + ")");
            
            return isSuccess;
            
        } catch (Exception e) {
            System.err.println("[REAL] 驗證館員 ID: " + librarianId + " 時發生錯誤: " + e.getMessage());
            return false;
        }
    }
}
