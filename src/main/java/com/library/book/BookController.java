package com.library.book;

import com.library.book.dto.BookDtos;
import com.library.branch.LibraryBranch;
import com.library.branch.LibraryBranchRepository;
import com.library.inventory.InventoryItem;
import com.library.inventory.InventoryItemRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/books")
@Validated
public class BookController {
    private final BookRepository bookRepository;
    private final InventoryItemRepository inventoryRepository;
    private final LibraryBranchRepository branchRepository;

    public BookController(BookRepository bookRepository, InventoryItemRepository inventoryRepository, LibraryBranchRepository branchRepository) {
        this.bookRepository = bookRepository;
        this.inventoryRepository = inventoryRepository;
        this.branchRepository = branchRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@Valid @RequestBody BookDtos.CreateBookRequest request, 
                                   Authentication authentication) {
        // 1.標準化輸入參數
        String normalizedTitle = normalizeString(request.title());
        String normalizedAuthor = normalizeString(request.author());
        String uniqueBookKey = generateUniqueBookKey(normalizedTitle, normalizedAuthor, request.publishYear(), request.categoryId());
        
        // 2.Upsert並且獲取 bookId
        Long bookId = bookRepository.upsertBook(
            uniqueBookKey,
            normalizedTitle,
            normalizedAuthor,
            request.publishYear(),
            request.categoryId(),
            request.extra() != null ? request.extra() : "{}"
        );
        
        // 3.建立Book
        Book book = new Book();
        book.setId(bookId);
        
        // 4.驗證並建立book_copies
        if (request.bookCopies() == null || request.bookCopies().isEmpty()) {
            throw new IllegalArgumentException("至少需要指定一個分館的副本數量");
        }
        
        for (BookDtos.BookCopyRequest copyRequest : request.bookCopies()) {
            // 驗證分館存在
            LibraryBranch branch = branchRepository.findById(copyRequest.branchId())
                .orElseThrow(() -> new IllegalArgumentException("分館不存在: " + copyRequest.branchId()));
            
            // 驗證副本數量
            if (copyRequest.quantity() <= 0) {
                throw new IllegalArgumentException("副本數量必須大於 0");
            }
            
            // 建立指定數量的副本
            for (int i = 0; i < copyRequest.quantity(); i++) {
                String barcode = generateBarcode(bookId, copyRequest.branchId(), i);
                InventoryItem item = InventoryItem.builder()
                        .book(book)
                        .branch(branch)
                        .status("AVAILABLE")
                        .barcode(barcode)
                        .build();
                inventoryRepository.save(item);
            }
        }
        
        return ResponseEntity.ok(Map.of("id", bookId));
    }

    @GetMapping("/search")
    public ResponseEntity<BookDtos.SearchResponse> search(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "author", required = false) String author,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "page", defaultValue = "1") int page,
            Authentication authentication) {
        
        // 確認分頁為正整數
        if (page < 1) {
            throw new IllegalArgumentException("Page number must be greater than 0");
        }
        
        // 固定每頁顯示5筆
        int size = 5;
        
        // 這裡需把client傳來的分頁-1
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Object[]> bookPage = bookRepository.search(title, author, year, pageable);
        
        // 轉換為回應格式
        List<BookDtos.BookAvailability> bookAvailabilities = bookPage.getContent().stream().map(row -> {
            Long bookId = (Long) row[0];
            String bookTitle = (String) row[2];
            String bookAuthor = (String) row[3];
            Integer publishYear = (Integer) row[4];
            Long categoryId = (Long) row[5];
            
            // 查詢副本
            Book tempBook = new Book();
            tempBook.setId(bookId);
            List<InventoryItem> items = inventoryRepository.findByBook(tempBook);
            
            // Group items by branch and calculate totals
            Map<Long, List<InventoryItem>> itemsByBranch = items.stream()
                    .collect(Collectors.groupingBy(item -> item.getBranch().getId()));
            
            List<BookDtos.BranchAvailability> branches = itemsByBranch.entrySet().stream()
                    .map(entry -> {
                        Long branchId = entry.getKey();
                        List<InventoryItem> branchItems = entry.getValue();
                        String branchName = branchItems.get(0).getBranch().getName();
                        
                        int total = branchItems.size();
                        int available = (int) branchItems.stream()
                                .filter(item -> "AVAILABLE".equals(item.getStatus()))
                                .count();
                        
                        return new BookDtos.BranchAvailability(branchId, branchName, total, available);
                    })
                    .collect(Collectors.toList());
            
            return new BookDtos.BookAvailability(
                bookId, 
                bookTitle, 
                bookAuthor, 
                publishYear, 
                categoryId,
                "{}",
                branches
            );
        }).collect(Collectors.toList());

        BookDtos.SearchResponse response = new BookDtos.SearchResponse(
            bookAvailabilities,
            page,
            size,
            bookPage.getTotalElements(),
            bookPage.getTotalPages()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 標準化：trim + 小寫
     */
    private String normalizeString(String input) {
        if (input == null) {
            throw new IllegalArgumentException("標題和作者不能為空");
        }
        String normalized = input.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("標題和作者不能為空白");
        }
        return normalized.toLowerCase();
    }
    
    /**
     * 產生書的唯一識別
     */
    private String generateUniqueBookKey(String title, String author, Integer publishYear, Long categoryId) {
        if (title == null || author == null || publishYear == null || categoryId == null) {
            throw new IllegalArgumentException("標題、作者、出版年份和類別ID都不能為空");
        }
        return String.format("%s|%s|%d|%d", title, author, publishYear, categoryId);
    }
    
    /**
     * 產生模擬條碼
     */
    private String generateBarcode(Long bookId, Long branchId, int copyIndex) {
        long timestamp = System.currentTimeMillis();
        return String.format("B%03d_%02d_%03d_%d", bookId % 1000, branchId % 100, copyIndex + 1, timestamp % 10000);
    }

    /**
     * 更新書籍資訊（部分更新）
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('Librarian')")
    public ResponseEntity<Map<String, Object>> updateBook(
            @PathVariable("id") Long id,
            @Valid @RequestBody BookDtos.UpdateBookRequest request
    ) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));

        // 只更新非 null 的欄位
        boolean updated = false;
        if (request.title() != null) {
            book.setTitle(request.title());
            updated = true;
        }
        if (request.author() != null) {
            book.setAuthor(request.author());
            updated = true;
        }
        if (request.publishYear() != null) {
            book.setPublishYear(request.publishYear());
            updated = true;
        }
        if (request.categoryId() != null) {
            book.setCategoryId(request.categoryId());
            updated = true;
        }

        // 如果有更新，重新生成 unique_book_key
        if (updated) {
            book.setUniqueBookKey(generateUniqueBookKey(
                    book.getTitle(), 
                    book.getAuthor(), 
                    book.getPublishYear(), 
                    book.getCategoryId()
            ));
            bookRepository.save(book);
        }

        return ResponseEntity.ok(Map.of("id", book.getId(), "updated", updated));
    }

    /**
     * 刪除副本（Soft delete）
     */
    @DeleteMapping("/copies/{copyId}")
    @PreAuthorize("hasRole('Librarian')")
    public ResponseEntity<Map<String, Object>> deleteBookCopy(
            @PathVariable("copyId") Long copyId
    ) {
        // 1.檢查副本是否存在
        InventoryItem copy = inventoryRepository.findById(copyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book copy not found"));

        // 2.檢查是否已被標記為刪除
        if ("DELETED".equals(copy.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Copy already deleted");
        }

        // 3.檢查是否被借出
        if ("BORROWED".equals(copy.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete borrowed copy");
        }

        // 4.標記為已刪除
        copy.setStatus("DELETED");
        inventoryRepository.save(copy);

        return ResponseEntity.ok(Map.of(
                "message", "Book copy deleted successfully",
                "copyId", copyId,
                "bookId", copy.getBook().getId(),
                "branchId", copy.getBranch().getId()
        ));
    }
}


