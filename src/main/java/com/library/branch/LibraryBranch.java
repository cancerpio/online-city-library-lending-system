package com.library.branch;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "branches")
public class LibraryBranch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "branch_name", nullable = false, unique = true)
    private String name;

    // Constructors
    public LibraryBranch() {}

    public LibraryBranch(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    // Builder pattern
    public static LibraryBranchBuilder builder() {
        return new LibraryBranchBuilder();
    }

    public static class LibraryBranchBuilder {
        private Long id;
        private String name;

        public LibraryBranchBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public LibraryBranchBuilder name(String name) {
            this.name = name;
            return this;
        }

        public LibraryBranch build() {
            return new LibraryBranch(id, name);
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


}
