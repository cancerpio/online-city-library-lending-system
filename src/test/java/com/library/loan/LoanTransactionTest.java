package com.library.loan;

import com.library.inventory.InventoryItem;
import com.library.inventory.InventoryItemRepository;
import com.library.loan.dto.BorrowDtos;
import com.library.user.User;
import com.library.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
class LoanTransactionTest {

    @Autowired
    private LoanService loanService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private InventoryItemRepository inventoryItemRepository;
    

    private User testUser;
    private Long availableCopyId;

    @BeforeEach
    void setUp() {
        // 每個測試都建立一個User
        testUser = new User();
        testUser.setUsername("testuser_" + System.currentTimeMillis());
        testUser.setPasswordHash("password");
        testUser.setRole("Member");
        testUser = userRepository.save(testUser);
        
        // 找到一個可用的副本
        availableCopyId = inventoryItemRepository.findAll().stream()
            .filter(item -> "AVAILABLE".equals(item.getStatus()))
            .findFirst()
            .map(InventoryItem::getId)
            .orElseThrow(() -> new RuntimeException("No available copy found"));
    }

    @Test
    @Transactional
    void testBorrowTransaction_AllOrNothing_Success() {
        // 測試正常借書: 預期全部成功
        BorrowDtos.BorrowRequest request = new BorrowDtos.BorrowRequest(
            List.of(new BorrowDtos.BorrowItem(availableCopyId, null))
        );
        
        BorrowDtos.BorrowResponse response = loanService.borrowBooks(testUser.getId(), request);
        
        assertThat(response.loans()).hasSize(1);
        assertThat(response.loans().get(0).copyId()).isEqualTo(availableCopyId);
        
        // 驗證副本狀態已更新
        InventoryItem item = inventoryItemRepository.findById(availableCopyId).orElseThrow();
        assertThat(item.getStatus()).isEqualTo("BORROWED");
    }

    @Test
    void testBorrowTransaction_AllOrNothing_Failure() {
        // 測試借不存在的副本 - 應該全部失敗
        Long nonExistentCopyId = 99999L;
        BorrowDtos.BorrowRequest request = new BorrowDtos.BorrowRequest(
            List.of(new BorrowDtos.BorrowItem(nonExistentCopyId, null))
        );
        
        assertThatThrownBy(() -> loanService.borrowBooks(testUser.getId(), request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Some copies not found");
        
        // 驗證沒有任何 loan 記錄
        List<Loan> loans = loanService.getUserLoans(testUser.getId());
        assertThat(loans).isEmpty();
    }

    @Test
    void testConcurrentBorrow_SameCopy_RowLock() throws InterruptedException {
        // 建立兩個用戶，測試同時借書時，只有一個會成功
        User user1 = createTestUser("user1");
        User user2 = createTestUser("user2");
        
        int threadCount = 2;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        // 兩個Thread同時嘗試借同一本書
        for (int i = 0; i < threadCount; i++) {
            final User user = (i == 0) ? user1 : user2;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    BorrowDtos.BorrowRequest request = new BorrowDtos.BorrowRequest(
                        List.of(new BorrowDtos.BorrowItem(availableCopyId, null))
                    );
                    
                    loanService.borrowBooks(user.getId(), request);
                    successCount.incrementAndGet();
                    
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.out.println("Thread failed: " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        // 同時開始
        startLatch.countDown();
        
        // 等待所有線程完成
        endLatch.await();
        executor.shutdown();
        
        // 驗證結果：只有一個成功，一個失敗
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);
        
        // 驗證副本狀態
        InventoryItem item = inventoryItemRepository.findById(availableCopyId).orElseThrow();
        assertThat(item.getStatus()).isEqualTo("BORROWED");
    }

    private User createTestUser(String username) {
        User user = new User();
        user.setUsername(username + "_" + System.currentTimeMillis());
        user.setPasswordHash("password");
        user.setRole("Member");
        return userRepository.save(user);
    }
}
