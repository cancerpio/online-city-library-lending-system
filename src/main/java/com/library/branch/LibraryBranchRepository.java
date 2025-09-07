package com.library.branch;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LibraryBranchRepository extends JpaRepository<LibraryBranch, Long> {
    Optional<LibraryBranch> findByName(String name);
}


