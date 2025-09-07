package com.library.loan;

import com.library.loan.dto.BorrowDtos;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/loans")
public class LoanController {
    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping("/borrow")
    public ResponseEntity<BorrowDtos.BorrowResponse> borrow(@Valid @RequestBody BorrowDtos.BorrowRequest request, 
                                                           Authentication authentication) {
        // 從 JWT token 取得使用者ID
        Long userId = Long.parseLong(authentication.getName());
        
        BorrowDtos.BorrowResponse response = loanService.borrowBooks(userId, request);
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/return")
    public ResponseEntity<BorrowDtos.ReturnResponse> returnBooks(@Valid @RequestBody BorrowDtos.ReturnRequest request,
                                                                Authentication authentication) {
        // 從 JWT token 取得使用者ID
        Long userId = Long.parseLong(authentication.getName());
        
        BorrowDtos.ReturnResponse response = loanService.returnBooks(userId, request);
        return ResponseEntity.status(200).body(response);
    }

    @PostMapping("/{loanId}/return")
    public ResponseEntity<?> returnBook(@PathVariable Long loanId) {
        Loan loan = loanService.returnBook(loanId);
        return ResponseEntity.ok(java.util.Map.of("returnedDate", loan.getReturnedDate()));
    }

    @GetMapping("/debug/copies")
    public ResponseEntity<?> debugCopies() {
        return ResponseEntity.ok(loanService.getAllCopies());
    }
}


