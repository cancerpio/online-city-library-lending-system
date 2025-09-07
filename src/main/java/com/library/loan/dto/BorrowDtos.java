package com.library.loan.dto;

import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDateTime;
import java.util.List;

public class BorrowDtos {
    
    public record BorrowItem(
            Long bookCopiesId,  // book_copies.id
            String barcode      // book_copies.barcode
    ) {}
    
    public record BorrowRequest(
            @NotEmpty(message = "借閱項目不能為空")
            List<BorrowItem> items
    ) {}
    
    public record BorrowLoan(
            Long loanId,
            Long copyId,
            LocalDateTime dueAt
    ) {}
    
    public record BorrowResponse(
            List<BorrowLoan> loans
    ) {}
    
    // 錯誤回應 DTOs
    public record BatchLimitExceededError(
            String error,
            Limits limits,
            Request request
    ) {
        public record Limits(int BOOK, int JOURNAL) {}
        public record Request(int BOOK, int JOURNAL) {}
    }
    
    public record CopyNotAvailableError(
            String error,
            List<Long> copyIds
    ) {}
    
    public record CopyNotFoundError(
            String error
    ) {}
    
    // 還書相關 DTOs
    public record ReturnItem(
            Long bookCopiesId,  // book_copies.id
            String barcode      // book_copies.barcode
    ) {}
    
    public record ReturnRequest(
            @NotEmpty(message = "還書項目不能為空")
            List<ReturnItem> items
    ) {}
    
    public record ReturnLoan(
            Long loanId,
            Long copyId,
            LocalDateTime returnedAt
    ) {}
    
    public record ReturnResponse(
            List<ReturnLoan> loans
    ) {}
}
