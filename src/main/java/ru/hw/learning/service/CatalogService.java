package ru.hw.learning.service;

import static ru.hw.learning.service.Guards.require;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hw.learning.entity.Category;
import ru.hw.learning.entity.Course;
import ru.hw.learning.entity.Tag;
import ru.hw.learning.exception.ConflictException;
import ru.hw.learning.mapper.CatalogMapper;
import ru.hw.learning.repository.CategoryRepository;
import ru.hw.learning.repository.CourseRepository;
import ru.hw.learning.repository.TagRepository;
import ru.hw.learning.web.dto.CatalogDtos.CategoryRequest;
import ru.hw.learning.web.dto.CatalogDtos.CategoryResponse;
import ru.hw.learning.web.dto.CatalogDtos.TagRequest;
import ru.hw.learning.web.dto.CatalogDtos.TagResponse;

/** Справочники: категории и теги курсов. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("Категория '" + request.name() + "' уже существует");
        }
        Category saved = categoryRepository.save(new Category(request.name().trim(), request.description()));
        log.info("Создана категория id={} '{}'", saved.getId(), saved.getName());
        return CatalogMapper.toResponse(saved);
    }

    public List<CategoryResponse> findAllCategories() {
        return categoryRepository.findAll(Sort.by("name")).stream().map(CatalogMapper::toResponse).toList();
    }

    public CategoryResponse getCategory(Long id) {
        return CatalogMapper.toResponse(require(categoryRepository.findById(id), "Категория", id));
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = require(categoryRepository.findById(id), "Категория", id);
        if (!category.getName().equalsIgnoreCase(request.name()) && categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("Категория '" + request.name() + "' уже существует");
        }
        category.setName(request.name().trim());
        category.setDescription(request.description());
        return CatalogMapper.toResponse(category);
    }

    /** Категорию с курсами удалить нельзя (иначе курсы останутся без категории). */
    @Transactional
    public void deleteCategory(Long id) {
        Category category = require(categoryRepository.findById(id), "Категория", id);
        if (courseRepository.existsByCategoryId(id)) {
            throw new ConflictException("В категории есть курсы — удаление запрещено");
        }
        categoryRepository.delete(category);
        log.info("Удалена категория id={}", id);
    }

    @Transactional
    public TagResponse createTag(TagRequest request) {
        if (tagRepository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("Тег '" + request.name() + "' уже существует");
        }
        return CatalogMapper.toResponse(tagRepository.save(new Tag(request.name().trim())));
    }

    public List<TagResponse> findAllTags() {
        return tagRepository.findAll(Sort.by("name")).stream().map(CatalogMapper::toResponse).toList();
    }

    /** Возвращает существующий тег по имени или создаёт новый. */
    @Transactional
    public Tag findOrCreateTag(String name) {
        String normalized = name.trim();
        return tagRepository.findByNameIgnoreCase(normalized).orElseGet(() -> tagRepository.save(new Tag(normalized)));
    }

    /** Удаление тега: строки course_tag удаляются со стороны владельца связи (Course.tags), сами курсы остаются. */
    @Transactional
    public void deleteTag(Long id) {
        Tag tag = require(tagRepository.findById(id), "Тег", id);
        for (Course course : new ArrayList<>(tag.getCourses())) {
            course.removeTag(tag);
        }
        tagRepository.delete(tag);
        log.info("Удалён тег id={}", id);
    }
}
