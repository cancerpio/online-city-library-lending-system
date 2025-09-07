package com.library.book;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface BookRepository extends JpaRepository<Book, Long> {

    @Query(value = "select id, unique_book_key, title, author, publish_year, category_id from books where (:title is null or title like concat('%', :title, '%')) " +
            "and (:author is null or author like concat('%', :author, '%')) " +
            "and (:year is null or publish_year = :year)", nativeQuery = true)
    Page<Object[]> search(@Param("title") String title,
                          @Param("author") String author,
                          @Param("year") Integer year,
                          Pageable pageable);

    @Query(value = "select count(*) from books where (:title is null or title like concat('%', :title, '%')) " +
            "and (:author is null or author like concat('%', :author, '%')) " +
            "and (:year is null or publish_year = :year)", nativeQuery = true)
    long countSearch(@Param("title") String title,
                     @Param("author") String author,
                     @Param("year") Integer year);

    /**
     *
     * 如果書籍已存在，返回現有的ID；如果不存在，建立新書籍並返回新ID
     */
    @Query(value = """
        INSERT INTO books (unique_book_key, title, author, publish_year, category_id, extra)
        VALUES (:uniqueBookKey, :title, :author, :publishYear, :categoryId, :extra)
        ON CONFLICT (unique_book_key) DO UPDATE SET
            title = EXCLUDED.title,
            author = EXCLUDED.author,
            publish_year = EXCLUDED.publish_year,
            category_id = EXCLUDED.category_id,
            extra = EXCLUDED.extra
        RETURNING id
        """, nativeQuery = true)
    Long upsertBook(@Param("uniqueBookKey") String uniqueBookKey,
                    @Param("title") String title,
                    @Param("author") String author,
                    @Param("publishYear") Integer publishYear,
                    @Param("categoryId") Long categoryId,
                    @Param("extra") String extra);
}


