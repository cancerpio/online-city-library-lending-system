package com.library.book;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookCategoryRepository extends JpaRepository<BookCategory, Long> {
    
    /**
     * 根據分類名稱查詢分類
     */
    @Query("SELECT bc FROM BookCategory bc WHERE bc.category = :category")
    Optional<BookCategory> findByCategory(@Param("category") String category);
}
