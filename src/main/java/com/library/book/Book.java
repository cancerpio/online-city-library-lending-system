package com.library.book;

import com.library.inventory.InventoryItem;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Set;

@Entity
@Table(name = "books")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "unique_book_key", nullable = false, unique = true)
    private String uniqueBookKey;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @NotBlank
    @Column(nullable = false)
    private String author;

    @Column(name = "publish_year")
    private Integer publishYear;

    @NotNull
    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(columnDefinition = "text")
    private String extra;

    @OneToMany(mappedBy = "book")
    private Set<InventoryItem> inventoryItems;

    // Constructors
    public Book() {}

    public Book(Long id, String uniqueBookKey, String title, String author, Integer publishYear, Long categoryId, String extra, Set<InventoryItem> inventoryItems) {
        this.id = id;
        this.uniqueBookKey = uniqueBookKey;
        this.title = title;
        this.author = author;
        this.publishYear = publishYear;
        this.categoryId = categoryId;
        this.extra = extra;
        this.inventoryItems = inventoryItems;
    }

    // Builder pattern
    public static BookBuilder builder() {
        return new BookBuilder();
    }

    public static class BookBuilder {
        private Long id;
        private String uniqueBookKey;
        private String title;
        private String author;
        private Integer publishYear;
        private Long categoryId;
        private String extra;
        private Set<InventoryItem> inventoryItems;

        public BookBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public BookBuilder uniqueBookKey(String uniqueBookKey) {
            this.uniqueBookKey = uniqueBookKey;
            return this;
        }

        public BookBuilder title(String title) {
            this.title = title;
            return this;
        }

        public BookBuilder author(String author) {
            this.author = author;
            return this;
        }

        public BookBuilder publishYear(Integer publishYear) {
            this.publishYear = publishYear;
            return this;
        }

        public BookBuilder categoryId(Long categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public BookBuilder extra(String extra) {
            this.extra = extra;
            return this;
        }

        public BookBuilder inventoryItems(Set<InventoryItem> inventoryItems) {
            this.inventoryItems = inventoryItems;
            return this;
        }

        public Book build() {
            return new Book(id, uniqueBookKey, title, author, publishYear, categoryId, extra, inventoryItems);
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUniqueBookKey() {
        return uniqueBookKey;
    }

    public void setUniqueBookKey(String uniqueBookKey) {
        this.uniqueBookKey = uniqueBookKey;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Integer getPublishYear() {
        return publishYear;
    }

    public void setPublishYear(Integer publishYear) {
        this.publishYear = publishYear;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public Set<InventoryItem> getInventoryItems() {
        return inventoryItems;
    }

    public void setInventoryItems(Set<InventoryItem> inventoryItems) {
        this.inventoryItems = inventoryItems;
    }
}
