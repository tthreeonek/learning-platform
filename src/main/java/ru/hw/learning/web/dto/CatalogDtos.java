package ru.hw.learning.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO справочников: категории и теги. */
public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record CategoryRequest(
            @NotBlank(message = "Название категории обязательно") @Size(max = 100) String name,
            @Size(max = 500) String description) {
    }

    public record CategoryResponse(Long id, String name, String description) {
    }

    public record TagRequest(@NotBlank(message = "Название тега обязательно") @Size(max = 50) String name) {
    }

    public record TagResponse(Long id, String name) {
    }
}
