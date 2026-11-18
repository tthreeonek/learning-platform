package ru.hw.learning.service;

import static ru.hw.learning.service.Guards.require;
import static ru.hw.learning.service.Guards.requireRole;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hw.learning.entity.Category;
import ru.hw.learning.entity.Course;
import ru.hw.learning.entity.CourseModule;
import ru.hw.learning.entity.Role;
import ru.hw.learning.entity.User;
import ru.hw.learning.exception.ConflictException;
import ru.hw.learning.mapper.CourseMapper;
import ru.hw.learning.repository.CategoryRepository;
import ru.hw.learning.repository.CourseModuleRepository;
import ru.hw.learning.repository.CourseRepository;
import ru.hw.learning.repository.EnrollmentRepository;
import ru.hw.learning.repository.UserRepository;
import ru.hw.learning.web.dto.CourseDtos.CourseRequest;
import ru.hw.learning.web.dto.CourseDtos.CourseResponse;
import ru.hw.learning.web.dto.CourseDtos.CourseStructureResponse;
import ru.hw.learning.web.dto.CourseDtos.CourseUpdateRequest;
import ru.hw.learning.web.dto.CourseDtos.ModuleRequest;
import ru.hw.learning.web.dto.CourseDtos.ModuleResponse;

/** Курсы: создание со структурой, поиск, обновление, удаление, получение полной структуры. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CourseModuleRepository moduleRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CatalogService catalogService;
    private final CourseContentService contentService;

    /** Создаёт курс; модули и уроки из запроса сохраняются каскадно (CascadeType.ALL). */
    @Transactional
    public CourseResponse create(CourseRequest request) {
        Course course = new Course(request.title().trim(), request.description(),
                findCategory(request.categoryId()), findTeacher(request.teacherId()));
        course.setDurationHours(request.durationHours());
        course.setStartDate(request.startDate());
        applyTags(course, request.tags());
        List<ModuleRequest> modules = request.modules() == null ? List.of() : request.modules();
        for (int i = 0; i < modules.size(); i++) {
            course.addModule(contentService.buildModule(modules.get(i), i + 1));
        }
        Course saved = courseRepository.save(course);
        log.info("Создан курс id={} '{}' (модулей: {}), преподаватель id={}", saved.getId(), saved.getTitle(),
                modules.size(), saved.getTeacher().getId());
        return CourseMapper.toResponse(saved);
    }

    public CourseResponse get(Long id) {
        return CourseMapper.toResponse(require(courseRepository.findDetailedById(id), "Курс", id));
    }

    public List<CourseResponse> search(Long categoryId, Long teacherId, String tag, String title) {
        return courseRepository.search(categoryId, teacherId, blankToNull(tag), blankToNull(title)).stream()
                .map(CourseMapper::toResponse)
                .toList();
    }

    /**
     * Полная структура курса без LazyInitializationException и без MultipleBagFetchException:
     * 1) курс с категорией, преподавателем и тегами (@EntityGraph);
     * 2) модули курса с уроками (JOIN FETCH).
     */
    public CourseStructureResponse getStructure(Long id) {
        Course course = require(courseRepository.findDetailedById(id), "Курс", id);
        List<ModuleResponse> modules = moduleRepository.findWithLessonsByCourseId(id).stream()
                .map(CourseMapper::toResponse)
                .toList();
        return new CourseStructureResponse(CourseMapper.toResponse(course), modules);
    }

    @Transactional
    public CourseResponse update(Long id, CourseUpdateRequest request) {
        Course course = require(courseRepository.findDetailedById(id), "Курс", id);
        course.setTitle(request.title().trim());
        course.setDescription(request.description());
        course.setDurationHours(request.durationHours());
        course.setStartDate(request.startDate());
        if (!course.getCategory().getId().equals(request.categoryId())) {
            course.setCategory(findCategory(request.categoryId()));
        }
        if (!course.getTeacher().getId().equals(request.teacherId())) {
            course.setTeacher(findTeacher(request.teacherId()));
            log.info("Курсу id={} назначен преподаватель id={}", id, request.teacherId());
        }
        if (request.tags() != null) {
            new ArrayList<>(course.getTags()).forEach(course::removeTag);
            applyTags(course, request.tags());
        }
        log.info("Обновлён курс id={}", id);
        return CourseMapper.toResponse(course);
    }

    /**
     * Удаление курса. Политика: если на курс записаны студенты — удаление запрещено (409),
     * иначе каскадно удаляются модули, уроки, задания, решения, тесты и отзывы; строки course_tag удаляются, теги остаются.
     */
    @Transactional
    public void delete(Long id) {
        Course course = require(courseRepository.findById(id), "Курс", id);
        if (enrollmentRepository.existsByCourseId(id)) {
            throw new ConflictException("На курс записаны студенты — удаление запрещено. Сначала отпишите студентов.");
        }
        courseRepository.delete(course);
        log.info("Удалён курс id={} вместе со структурой", id);
    }

    @Transactional
    public ModuleResponse addModule(Long courseId, ModuleRequest request) {
        Course course = require(courseRepository.findById(courseId), "Курс", courseId);
        int order = request.orderIndex() != null ? request.orderIndex() : (int) moduleRepository.countByCourseId(courseId) + 1;
        CourseModule module = contentService.buildModule(request, order);
        course.addModule(module);
        moduleRepository.save(module);
        log.info("В курс id={} добавлен модуль id={} '{}'", courseId, module.getId(), module.getTitle());
        return CourseMapper.toResponse(module);
    }

    private void applyTags(Course course, List<String> tags) {
        if (tags == null) {
            return;
        }
        tags.stream().map(String::trim).distinct().map(catalogService::findOrCreateTag).forEach(course::addTag);
    }

    private Category findCategory(Long id) {
        return require(categoryRepository.findById(id), "Категория", id);
    }

    private User findTeacher(Long id) {
        User teacher = require(userRepository.findById(id), "Пользователь", id);
        requireRole(teacher, "вести курс", Role.TEACHER, Role.ADMIN);
        return teacher;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
