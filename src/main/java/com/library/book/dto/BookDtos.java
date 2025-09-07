package com.library.book.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class BookDtos {
    public record CreateBookRequest(
            @NotBlank String title,
            @NotBlank String author,
            @NotNull Integer publishYear,
            @NotNull Long categoryId,
            String extra,
            List<BookCopyRequest> bookCopies
    ) {}

    public record BookCopyRequest(
            @NotNull Long branchId,
            @NotNull Integer quantity
    ) {}

    public record BranchCopies(
            Long branchId,
            int totalCopies
    ) {}

    public record SearchRequest(
            String title,
            String author,
            Integer year,
            int page,
            int size
    ) {}

    public record BookAvailability(
            Long bookId,
            String title,
            String author,
            Integer publishYear,
            Long categoryId,
            String extra,
            List<BranchAvailability> branches
    ) {}

    public record SearchResponse(
            List<BookAvailability> books,
            int page,
            int size,
            long total,
            int totalPages
    ) {}

    public record BranchAvailability(
            Long branchId,
            String branchName,
            int total,
            int available
    ) {}

    public record UpdateBookRequest(
            String title,
            String author,
            Integer publishYear,
            Long categoryId
    ) {}
}


