package com.library.loan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DueNotificationSchedulerTest {

    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private DueNotificationScheduler scheduler;

    @Test
    void testNotifyDueSoon_WithOverdueBooks() {
        // 測試Scheduler確實有去查詢Loans的過期日
        // 建立 Loan
        Loan testLoan = new Loan();
        testLoan.setId(1L);
        // 設定5天後就要還書了
        testLoan.setDueDate(LocalDate.now().plusDays(5));
        testLoan.setReturnedDate(null);
        
        // 建立 User
        com.library.user.User testUser = new com.library.user.User();
        testUser.setUsername("testuser");
        testLoan.setUser(testUser);

        List<Loan> overdueLoans = List.of(testLoan);
        when(loanRepository.findByDueDate(any(LocalDate.class)))
                .thenReturn(overdueLoans);

        // 執行測試
        assertDoesNotThrow(() -> scheduler.notifyDueSoon());

        // 驗證 repository 被調用，有去查過期日
        verify(loanRepository, times(1)).findByDueDate(any(LocalDate.class));
    }
}
