package com.library.loan;

import com.library.inventory.InventoryItem;
import com.library.user.User;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "loans")
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "borrowed_user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "copy_id")
    private InventoryItem inventoryItem;

    @Column(name = "borrowed_at", nullable = false)
    private LocalDate borrowDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "returned_at")
    private LocalDate returnedDate;

    // Constructors
    public Loan() {}

    public Loan(Long id, User user, InventoryItem inventoryItem, LocalDate borrowDate, LocalDate dueDate, LocalDate returnedDate) {
        this.id = id;
        this.user = user;
        this.inventoryItem = inventoryItem;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnedDate = returnedDate;
    }

    // Builder pattern
    public static LoanBuilder builder() {
        return new LoanBuilder();
    }

    public static class LoanBuilder {
        private Long id;
        private User user;
        private InventoryItem inventoryItem;
        private LocalDate borrowDate;
        private LocalDate dueDate;
        private LocalDate returnedDate;

        public LoanBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public LoanBuilder user(User user) {
            this.user = user;
            return this;
        }

        public LoanBuilder inventoryItem(InventoryItem inventoryItem) {
            this.inventoryItem = inventoryItem;
            return this;
        }

        public LoanBuilder borrowDate(LocalDate borrowDate) {
            this.borrowDate = borrowDate;
            return this;
        }

        public LoanBuilder dueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public LoanBuilder returnedDate(LocalDate returnedDate) {
            this.returnedDate = returnedDate;
            return this;
        }

        public Loan build() {
            return new Loan(id, user, inventoryItem, borrowDate, dueDate, returnedDate);
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    public void setInventoryItem(InventoryItem inventoryItem) {
        this.inventoryItem = inventoryItem;
    }

    public LocalDate getBorrowDate() {
        return borrowDate;
    }

    public void setBorrowDate(LocalDate borrowDate) {
        this.borrowDate = borrowDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getReturnedDate() {
        return returnedDate;
    }

    public void setReturnedDate(LocalDate returnedDate) {
        this.returnedDate = returnedDate;
    }
}
