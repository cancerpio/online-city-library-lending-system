package com.library.book;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "book_categories")
public class BookCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String category;

    @NotNull
    @Column(name = "rule_max_concurrent", nullable = false)
    private Integer ruleMaxConcurrent;

    @NotNull
    @Column(name = "rule_loan_period_days", nullable = false)
    private Integer ruleLoanPeriodDays;

    // Constructors
    public BookCategory() {}

    public BookCategory(Long id, String category, Integer ruleMaxConcurrent, Integer ruleLoanPeriodDays) {
        this.id = id;
        this.category = category;
        this.ruleMaxConcurrent = ruleMaxConcurrent;
        this.ruleLoanPeriodDays = ruleLoanPeriodDays;
    }

    // Builder pattern
    public static BookCategoryBuilder builder() {
        return new BookCategoryBuilder();
    }

    public static class BookCategoryBuilder {
        private Long id;
        private String category;
        private Integer ruleMaxConcurrent;
        private Integer ruleLoanPeriodDays;

        public BookCategoryBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public BookCategoryBuilder category(String category) {
            this.category = category;
            return this;
        }

        public BookCategoryBuilder ruleMaxConcurrent(Integer ruleMaxConcurrent) {
            this.ruleMaxConcurrent = ruleMaxConcurrent;
            return this;
        }

        public BookCategoryBuilder ruleLoanPeriodDays(Integer ruleLoanPeriodDays) {
            this.ruleLoanPeriodDays = ruleLoanPeriodDays;
            return this;
        }

        public BookCategory build() {
            return new BookCategory(id, category, ruleMaxConcurrent, ruleLoanPeriodDays);
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getRuleMaxConcurrent() {
        return ruleMaxConcurrent;
    }

    public void setRuleMaxConcurrent(Integer ruleMaxConcurrent) {
        this.ruleMaxConcurrent = ruleMaxConcurrent;
    }

    public Integer getRuleLoanPeriodDays() {
        return ruleLoanPeriodDays;
    }

    public void setRuleLoanPeriodDays(Integer ruleLoanPeriodDays) {
        this.ruleLoanPeriodDays = ruleLoanPeriodDays;
    }
}
