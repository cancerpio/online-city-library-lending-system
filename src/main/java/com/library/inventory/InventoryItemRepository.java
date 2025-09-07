package com.library.inventory;

import com.library.book.Book;
import com.library.branch.LibraryBranch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    List<InventoryItem> findByBook(Book book);
    List<InventoryItem> findByBookId(Long bookId);
    Optional<InventoryItem> findByBookAndBranch(Book book, LibraryBranch branch);
    
    /**
     * 通過條碼查找副本
     */
    Optional<InventoryItem> findByBarcode(String barcode);
}


