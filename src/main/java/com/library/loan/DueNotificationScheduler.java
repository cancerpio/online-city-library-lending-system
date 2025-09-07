package com.library.loan;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class DueNotificationScheduler {
    private final LoanRepository loanRepository;
    
    @Value("${overdue.check.interval}")
    private int checkIntervalMinutes;

    public DueNotificationScheduler(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    // 使用環境變數設定檢查間隔（預設1分鐘）
    @Scheduled(fixedRateString = "#{${overdue.check.interval} * 60 * 1000}")
    public void notifyDueSoon() {
        LocalDate target = LocalDate.now().plusDays(5);
        List<Loan> loans = loanRepository.findByDueDate(target);
        for (Loan loan : loans) {
            if (loan.getReturnedDate() == null) {
                System.out.println("[Notify] User " + loan.getUser().getUsername() + " - loan " + loan.getId() + " due on " + loan.getDueDate());
            }
        }
    }
}


