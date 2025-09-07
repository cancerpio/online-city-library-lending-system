package com.library.notification;

import com.library.loan.Loan;
import com.library.loan.LoanRepository;
import com.library.user.User;
import com.library.book.Book;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class OverdueNotificationService {

    @Autowired
    private LoanRepository loanRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 檢查逾期書籍並發送通知
     * 每分鐘執行一次
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 60000) // 10 秒 = 10000 毫秒
    public void checkOverdueBooks() {
        try {
            // 查詢所有未歸還且到期日在未來5天內或已逾期的借閱記錄
            List<Loan> loansToNotify = loanRepository.findLoansToNotify();
            
            if (loansToNotify.isEmpty()) {
                System.out.println("[" + LocalDateTime.now().format(DATE_FORMATTER) + "] 逾期檢查完成 - 無需要通知的書籍");
                return;
            }

            System.out.println("[" + LocalDateTime.now().format(DATE_FORMATTER) + "] 發現 " + loansToNotify.size() + " 筆需要通知的借閱記錄");

            for (Loan loan : loansToNotify) {
                sendOverdueNotification(loan);
            }

        } catch (Exception e) {
            System.err.println("[" + LocalDateTime.now().format(DATE_FORMATTER) + "] 逾期檢查發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 發送逾期通知
     */
    private void sendOverdueNotification(Loan loan) {
        try {
            // 獲取用戶資訊
            User user = loan.getUser();
            if (user == null) {
                System.err.println("用戶不存在");
                return;
            }

            // 獲取書籍資訊 - 通過 inventoryItem 獲取
            Book book = loan.getInventoryItem().getBook();
            if (book == null) {
                System.err.println("書籍不存在");
                return;
            }

            // 格式化到期日期
            String dueDate = loan.getDueDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            // 計算今天與到期日的天數差
            LocalDate today = LocalDate.now();
            LocalDate dueDateLocal = loan.getDueDate();
            long daysUntilDue = java.time.temporal.ChronoUnit.DAYS.between(today, dueDateLocal);

            // 根據天數差決定通知內容
            if (daysUntilDue < 0) {
                // 已經逾期
                System.out.println("使用者: " + user.getUsername() + 
                                 ", 你借的書:" + book.getTitle() + 
                                 "已經逾期，請記得還書。原定還書日: " + dueDate);
            } else if (daysUntilDue <= 5) {
                // 即將逾期（5天內）
                System.out.println("使用者: " + user.getUsername() + 
                                 ", 你借的書:" + book.getTitle() + 
                                 "即將逾期，請記得還書。原定還書日: " + dueDate);
            }

        } catch (Exception e) {
            System.err.println("發送逾期通知時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
