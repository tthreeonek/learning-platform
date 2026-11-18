package ru.hw.learning.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.hw.learning.service.CatalogService;
import ru.hw.learning.web.dto.CatalogDtos.CategoryRequest;
import ru.hw.learning.web.dto.CatalogDtos.CategoryResponse;
import ru.hw.learning.web.dto.CatalogDtos.TagRequest;
import ru.hw.learning.web.dto.CatalogDtos.TagResponse;

@Tag(name = "Catalog", description = "Категории и теги курсов")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @Operation(summary = "Создать категорию")
    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse created = catalogService.createCategory(request);
        return ResponseEntity.created(URI.create("/api/categories/" + created.id())).body(created);
    }

    @Operation(summary = "Список категорий")
    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return catalogService.findAllCategories();
    }

    @Operation(summary = "Категория по id")
    @GetMapping("/categories/{id}")
    public CategoryResponse category(@PathVariable Long id) {
        return catalogService.getCategory(id);
    }

    @Operation(summary = "Обновить категорию")
    @PutMapping("/categories/{id}")
    public CategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return catalogService.updateCategory(id, request);
    }

    @Operation(summary = "Удалить категорию (если в ней нет курсов)")
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        catalogService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Создать тег")
    @PostMapping("/tags")
    public ResponseEntity<TagResponse> createTag(@Valid @RequestBody TagRequest request) {
        TagResponse created = catalogService.createTag(request);
        return ResponseEntity.created(URI.create("/api/tags/" + created.id())).body(created);
    }

    @Operation(summary = "Список тегов")
    @GetMapping("/tags")
    public List<TagResponse> tags() {
        return catalogService.findAllTags();
    }

    @Operation(summary = "Удалить тег (связи course_tag удаляются, курсы остаются)")
    @DeleteMapping("/tags/{id}")
    public ResponseEntity<Void> deleteTag(@PathVariable Long id) {
        catalogService.deleteTag(id);
        return ResponseEntity.noContent().build();
    }
}
