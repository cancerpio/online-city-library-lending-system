package com.library.loan.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class LoanDtos {
    
    public record BorrowRequest(
            @NotEmpty(message = "至少需要指定一個借閱項目")
            List<BorrowItem> items
    ) {}
    
    public record BorrowItem(
            Long bookCopiesId,
            String barcode
    ) {
        public BorrowItem {
            // 驗證至少提供一個識別符
            if (bookCopiesId == null && (barcode == null || barcode.trim().isEmpty())) {
                throw new IllegalArgumentException("必須提供 bookCopiesId 或 barcode 其中一個");
            }
            // 驗證不能同時提供兩個識別符
            if (bookCopiesId != null && barcode != null && !barcode.trim().isEmpty()) {
                throw new IllegalArgumentException("不能同時提供 bookCopiesId 和 barcode");
            }
        }
    }
    
    public record BorrowResponse(
            List<BorrowResult> results,
            int successCount,
            int failureCount
    ) {}
    
    public record BorrowResult(
            Long bookCopiesId,
            String barcode,
            boolean success,
            String message,
            Long loanId
    ) {}
    
    public record BorrowValidationResult(
            boolean valid,
            String message,
            Long bookCopiesId,
            String category,
            int currentBookCount,
            int currentJournalCount
    ) {}
}
