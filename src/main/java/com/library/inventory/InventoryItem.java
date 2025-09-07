package com.library.inventory;

import com.library.book.Book;
import com.library.branch.LibraryBranch;
import jakarta.persistence.*;

@Entity
@Table(name = "book_copies")
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "book_id")
    private Book book;

    @ManyToOne(optional = false)
    @JoinColumn(name = "branch_id")
    private LibraryBranch branch;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false, unique = true)
    private String barcode;

    // Constructors
    public InventoryItem() {}

    public InventoryItem(Long id, Book book, LibraryBranch branch, String status, String barcode) {
        this.id = id;
        this.book = book;
        this.branch = branch;
        this.status = status;
        this.barcode = barcode;
    }

    // Builder pattern
    public static InventoryItemBuilder builder() {
        return new InventoryItemBuilder();
    }

    public static class InventoryItemBuilder {
        private Long id;
        private Book book;
        private LibraryBranch branch;
        private String status;
        private String barcode;

        public InventoryItemBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public InventoryItemBuilder book(Book book) {
            this.book = book;
            return this;
        }

        public InventoryItemBuilder branch(LibraryBranch branch) {
            this.branch = branch;
            return this;
        }

        public InventoryItemBuilder status(String status) {
            this.status = status;
            return this;
        }

        public InventoryItemBuilder barcode(String barcode) {
            this.barcode = barcode;
            return this;
        }

        public InventoryItem build() {
            return new InventoryItem(id, book, branch, status, barcode);
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public LibraryBranch getBranch() {
        return branch;
    }

    public void setBranch(LibraryBranch branch) {
        this.branch = branch;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }
}
