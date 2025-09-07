package com.library.branch;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/branches")
public class LibraryBranchController {
    private final LibraryBranchRepository repository;

    public LibraryBranchController(LibraryBranchRepository repository) {
        this.repository = repository;
    }

    public record CreateBranchRequest(@NotBlank String name) {}

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(repository.findAll());
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateBranchRequest request, 
                                   Authentication authentication) {
        LibraryBranch branch = LibraryBranch.builder()
                .name(request.name())
                .build();
        repository.save(branch);
        return ResponseEntity.ok(Map.of("id", branch.getId()));
    }
}


