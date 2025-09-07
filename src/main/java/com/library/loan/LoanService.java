package com.library.loan;

import com.library.book.BookCategory;
import com.library.book.BookCategoryRepository;
import com.library.inventory.InventoryItem;
import com.library.inventory.InventoryItemRepository;
import com.library.loan.dto.BorrowDtos;
import com.library.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LoanService {
    private final LoanRepository loanRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final UserRepository userRepository;
    private final BookCategoryRepository bookCategoryRepository;

    private static final int MAX_BOOKS = 10; // for BOOK type (書籍)
    private static final int MAX_JOURNALS = 5; // for JOURNAL type (圖書)

    public LoanService(LoanRepository loanRepository, InventoryItemRepository inventoryItemRepository, 
                      UserRepository userRepository, BookCategoryRepository bookCategoryRepository) {
        this.loanRepository = loanRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.userRepository = userRepository;
        this.bookCategoryRepository = bookCategoryRepository;
    }

    /**
     * 借書
     */
    @Transactional
    public BorrowDtos.BorrowResponse borrowBooks(Long userId, BorrowDtos.BorrowRequest request) {
        // 確定使用者存在
        userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));
        
        // 正規化 payload（將條碼轉成 copy_id、去重）
        List<Long> copyIds = normalizePayload(request.items());
        
        // 前置檢查（交易外；只讀）
        validateBorrowRequest(userId, copyIds);
        
        // 鎖副本 + 寫入/更新
        List<BorrowDtos.BorrowLoan> loans = processBorrowTransaction(userId, copyIds);
        
        return new BorrowDtos.BorrowResponse(loans);
    }

    /**
     * 還書
     */
    @Transactional
    public BorrowDtos.ReturnResponse returnBooks(Long userId, BorrowDtos.ReturnRequest request) {
        // 確認使用者存在
        userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));
        
        // 正規化 payload（將條碼轉成 copy_id、去重）
        List<Long> copyIds = normalizeReturnPayload(request.items());
        
        // 前置檢查（交易外；只讀）
        validateReturnRequest(copyIds);
        
        // 還書
        List<BorrowDtos.ReturnLoan> loans = processReturnTransaction(userId, copyIds);
        
        return new BorrowDtos.ReturnResponse(loans);
    }

    /**
     * 正規化 payload：將條碼轉成 copy_id、去重
     */
    private List<Long> normalizePayload(List<BorrowDtos.BorrowItem> items) {
        Set<Long> copyIds = new HashSet<>();
        
        for (BorrowDtos.BorrowItem item : items) {
            if (item.bookCopiesId() != null) {
                copyIds.add(item.bookCopiesId());
            } else if (item.barcode() != null && !item.barcode().trim().isEmpty()) {
                Optional<InventoryItem> copy = inventoryItemRepository.findByBarcode(item.barcode().trim());
                if (copy.isPresent()) {
                    copyIds.add(copy.get().getId());
                } else {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Barcode not found: " + item.barcode());
                }
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each item must have either bookCopiesId or barcode");
            }
        }
        
        return new ArrayList<>(copyIds);
    }

    /**
     * 正規化還書 payload：將條碼轉成 copy_id、去重
     */
    private List<Long> normalizeReturnPayload(List<BorrowDtos.ReturnItem> items) {
        Set<Long> copyIds = new HashSet<>();
        
        for (BorrowDtos.ReturnItem item : items) {
            if (item.bookCopiesId() != null) {
                copyIds.add(item.bookCopiesId());
            } else if (item.barcode() != null && !item.barcode().trim().isEmpty()) {
                Optional<InventoryItem> copy = inventoryItemRepository.findByBarcode(item.barcode().trim());
                if (copy.isPresent()) {
                    copyIds.add(copy.get().getId());
                } else {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Barcode not found: " + item.barcode());
                }
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each item must have either bookCopiesId or barcode");
            }
        }
        
        return new ArrayList<>(copyIds);
    }

    /**
     * 前置檢查：驗證副本存在和分類計數
     */
    private void validateBorrowRequest(Long userId, List<Long> copyIds) {
        if (copyIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No valid copy IDs found");
        }
        
        System.out.println("Validating copyIds: " + copyIds);
        
        // 檢查副本是否存在
        long gotCnt = loanRepository.countExistingCopies(copyIds);
        int wantCnt = copyIds.size();
        
        System.out.println("Want count: " + wantCnt + ", Got count: " + gotCnt);
        
        if (gotCnt < wantCnt) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Some copies not found");
        }
        
        // 1.計算新借閱請求的分類數量
        int reqBook = 0;
        int reqJournal = 0;
        
        for (Long copyId : copyIds) {
            Optional<InventoryItem> item = inventoryItemRepository.findById(copyId);
            if (item.isPresent()) {
                Long categoryId = item.get().getBook().getCategoryId();
                if (isBookCategory(categoryId)) {
                    reqBook++;
                } else if (isJournalCategory(categoryId)) {
                    reqJournal++;
                }
            }
        }
        
        // 2.查詢使用者當前已借的數量
        List<Loan> currentLoans = loanRepository.findByUserIdAndReturnedDateIsNull(userId);
        int currentBookCount = 0;
        int currentJournalCount = 0;
        
        for (Loan loan : currentLoans) {
            Long categoryId = loan.getInventoryItem().getBook().getCategoryId();
            if (isBookCategory(categoryId)) {
                currentBookCount++;
            } else if (isJournalCategory(categoryId)) {
                currentJournalCount++;
            }
        }
        
        // 3.檢查總數是否超過限制
        if (currentBookCount + reqBook > MAX_BOOKS) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 
                "BOOK limit exceeded: current " + currentBookCount + ", requested " + reqBook + ", max " + MAX_BOOKS);
        }
        
        if (currentJournalCount + reqJournal > MAX_JOURNALS) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 
                "JOURNAL limit exceeded: current " + currentJournalCount + ", requested " + reqJournal + ", max " + MAX_JOURNALS);
        }
        
        System.out.println("Borrow validation passed - Current: BOOK=" + currentBookCount + ", JOURNAL=" + currentJournalCount + 
                          ", Request: BOOK=" + reqBook + ", JOURNAL=" + reqJournal);
    }
    
    /**
     * 判斷分類是否為書
     */
    private boolean isBookCategory(Long categoryId) {
        Optional<BookCategory> category = bookCategoryRepository.findById(categoryId);
        return category.isPresent() && "BOOK".equals(category.get().getCategory());
    }
    
    /**
     * 判斷分類是否為圖書
     */
    private boolean isJournalCategory(Long categoryId) {
        Optional<BookCategory> category = bookCategoryRepository.findById(categoryId);
        return category.isPresent() && "JOURNAL".equals(category.get().getCategory());
    }

    /**
     * 還書前置檢查：驗證副本存在性
     */
    private void validateReturnRequest(List<Long> copyIds) {
        if (copyIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No valid copy IDs found");
        }
        
        System.out.println("Validating return copyIds: " + copyIds);
        
        // 檢查每個副本是否存在
        for (Long copyId : copyIds) {
            Optional<InventoryItem> item = inventoryItemRepository.findById(copyId);
            if (!item.isPresent()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Copy not found: " + copyId);
            }
            System.out.println("Copy ID " + copyId + " exists: " + item.isPresent());
        }
        
        // 使用 JPA 查詢檢查副本存在性
        long gotCnt = loanRepository.countExistingCopies(copyIds);
        int wantCnt = copyIds.size();
        
        System.out.println("Return validation - Want count: " + wantCnt + ", Got count: " + gotCnt);
        
        // 檢查副本是否存在
        if (gotCnt < wantCnt) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Some copies not found");
        }
    }

    /**
     * Transaction: 鎖副本 + 寫入/更新
     * 將副本改成BORROWED
     */
    @Transactional
    private List<BorrowDtos.BorrowLoan> processBorrowTransaction(Long userId, List<Long> copyIds) {
        // 鎖定副本並檢查是否可借
        List<InventoryItem> lockedCopies = loanRepository.findAndLockCopies(copyIds);
        
        // 檢查是否有不可用的副本
        List<Long> notAvailableIds = new ArrayList<>();
        for (InventoryItem copy : lockedCopies) {
            if (!"AVAILABLE".equals(copy.getStatus())) {
                notAvailableIds.add(copy.getId());
            }
        }
        
        if (!notAvailableIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, 
                "Some copies are not available: " + notAvailableIds);
        }
        
        // 檢查鎖定的副本數量
        if (lockedCopies.size() != copyIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "Some copies not found during locking");
        }
        
        // 建立多個借閱記錄
        List<BorrowDtos.BorrowLoan> loans = new ArrayList<>();
        for (InventoryItem copy : lockedCopies) {
            Loan loan = new Loan();
            loan.setInventoryItem(copy);
            loan.setUser(userRepository.findById(userId).orElseThrow());
            loan.setBorrowDate(LocalDate.now());
            loan.setDueDate(LocalDate.now().plusDays(30));
            loan.setReturnedDate(null);
            
            Loan savedLoan = loanRepository.save(loan);
            
            // 更新副本狀態
            copy.setStatus("BORROWED");
            inventoryItemRepository.save(copy);

            LocalDateTime dueAt = LocalDateTime.now().plusMonths(1);
            loans.add(new BorrowDtos.BorrowLoan(savedLoan.getId(), copy.getId(), dueAt));
        }
        
        return loans;
    }

    /**
     * 還書
     *  鎖住此使用者在借中的副本Row
     *  return 歸還時間
     *  將副本改回AVAILABLE
     */
    @Transactional
    private List<BorrowDtos.ReturnLoan> processReturnTransaction(Long userId, List<Long> copyIds) {
        System.out.println("Processing return transaction for user " + userId + " with copyIds: " + copyIds);
        
        // 鎖住「此使用者在借中的副本Row
        List<Object[]> lockedLoans = loanRepository.findAndLockActiveLoans(userId, copyIds);
        
        System.out.println("Found " + lockedLoans.size() + " active loans for user " + userId);
        
        // 若筆數 < copy_ids 去重後數量: 交易終止 & Return Fail
        if (lockedLoans.size() < copyIds.size()) {
            // 找出沒有對應借閱記錄的副本
            Set<Long> foundCopyIds = lockedLoans.stream()
                .map(row -> (Long) row[1]) // copyId 是第二個欄位
                .collect(Collectors.toSet());
            
            List<Long> notFoundCopyIds = copyIds.stream()
                .filter(copyId -> !foundCopyIds.contains(copyId))
                .collect(Collectors.toList());
            
            throw new ResponseStatusException(HttpStatus.CONFLICT, 
                "Some copies are not currently borrowed by this user: " + notFoundCopyIds);
        }
        
        // 紀錄歸還時間
        int updatedLoans = loanRepository.updateReturnedDate(userId, copyIds);
        System.out.println("Updated " + updatedLoans + " loans with return date");
        
        // 將副本改回可借
        int updatedCopies = loanRepository.updateCopyStatusToAvailable(copyIds);
        System.out.println("Updated " + updatedCopies + " copies to AVAILABLE status");

        List<BorrowDtos.ReturnLoan> returnLoans = new ArrayList<>();
        LocalDateTime returnedAt = LocalDateTime.now();
        
        for (Object[] row : lockedLoans) {
            Long loanId = (Long) row[0]; // loanId 是第0個index
            Long copyId = (Long) row[1]; // copyId 是第1個index
            returnLoans.add(new BorrowDtos.ReturnLoan(loanId, copyId, returnedAt));
        }
        
        return returnLoans;
    }

    @Transactional
    public Loan returnBook(Long loanId) {
        if (loanId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loan ID cannot be null");
        }
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found: " + loanId));
        if (loan.getReturnedDate() != null) {
            return loan;
        }
        loan.setReturnedDate(LocalDate.now());
        InventoryItem item = loan.getInventoryItem();
        item.setStatus("AVAILABLE");
        inventoryItemRepository.save(item);
        return loanRepository.save(loan);
    }

    public List<Map<String, Object>> getAllCopies() {
        return inventoryItemRepository.findAll().stream()
            .map(item -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", item.getId());
                map.put("status", item.getStatus());
                map.put("bookId", item.getBook().getId());
                map.put("branchId", item.getBranch().getId());
                map.put("barcode", item.getBarcode() != null ? item.getBarcode() : "N/A");
                return map;
            })
            .collect(Collectors.toList());
    }

    /**
     * 使用者的所有借閱記錄
     */
    public List<Loan> getUserLoans(Long userId) {
        return loanRepository.findByUserIdAndReturnedDateIsNull(userId);
    }
}


