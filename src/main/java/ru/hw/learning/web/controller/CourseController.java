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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.hw.learning.service.AnalyticsService;
import ru.hw.learning.service.CourseContentService;
import ru.hw.learning.service.CourseService;
import ru.hw.learning.service.EnrollmentService;
import ru.hw.learning.service.QuizService;
import ru.hw.learning.service.ReviewService;
import ru.hw.learning.web.dto.AnalyticsDtos.CourseStatsResponse;
import ru.hw.learning.web.dto.AnalyticsDtos.LeaderboardEntry;
import ru.hw.learning.web.dto.CourseDtos.CourseRequest;
import ru.hw.learning.web.dto.CourseDtos.CourseResponse;
import ru.hw.learning.web.dto.CourseDtos.CourseStructureResponse;
import ru.hw.learning.web.dto.CourseDtos.CourseUpdateRequest;
import ru.hw.learning.web.dto.CourseDtos.ModuleRequest;
import ru.hw.learning.web.dto.CourseDtos.ModuleResponse;
import ru.hw.learning.web.dto.EnrollmentDtos.EnrollmentResponse;
import ru.hw.learning.web.dto.EnrollmentDtos.ReviewRequest;
import ru.hw.learning.web.dto.EnrollmentDtos.ReviewResponse;
import ru.hw.learning.web.dto.QuizDtos.QuizResultResponse;
import ru.hw.learning.web.dto.UserDtos.UserResponse;

@Tag(name = "Courses", description = "Курсы, их структура, запись студентов, отзывы и статистика")
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final CourseContentService contentService;
    private final EnrollmentService enrollmentService;
    private final ReviewService reviewService;
    private final QuizService quizService;
    private final AnalyticsService analyticsService;

    @Operation(summary = "Создать курс (можно сразу с модулями и уроками)")
    @PostMapping
    public ResponseEntity<CourseResponse> create(@Valid @RequestBody CourseRequest request) {
        CourseResponse created = courseService.create(request);
        return ResponseEntity.created(URI.create("/api/courses/" + created.id())).body(created);
    }

    @Operation(summary = "Поиск курсов: по категории, преподавателю, тегу, части названия")
    @GetMapping
    public List<CourseResponse> search(@RequestParam(required = false) Long categoryId,
                                       @RequestParam(required = false) Long teacherId,
                                       @RequestParam(required = false) String tag,
                                       @RequestParam(required = false) String title) {
        return courseService.search(categoryId, teacherId, tag, title);
    }

    @Operation(summary = "Карточка курса")
    @GetMapping("/{id}")
    public CourseResponse get(@PathVariable Long id) {
        return courseService.get(id);
    }

    @Operation(summary = "Полная структура курса: модули и уроки")
    @GetMapping("/{id}/structure")
    public CourseStructureResponse structure(@PathVariable Long id) {
        return courseService.getStructure(id);
    }

    @Operation(summary = "Обновить курс (описание, категорию, преподавателя, теги)")
    @PutMapping("/{id}")
    public CourseResponse update(@PathVariable Long id, @Valid @RequestBody CourseUpdateRequest request) {
        return courseService.update(id, request);
    }

    @Operation(summary = "Удалить курс (запрещено, если есть записанные студенты)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Модули курса с уроками")
    @GetMapping("/{id}/modules")
    public List<ModuleResponse> modules(@PathVariable Long id) {
        return contentService.getModules(id);
    }

    @Operation(summary = "Добавить модуль в курс")
    @PostMapping("/{id}/modules")
    public ResponseEntity<ModuleResponse> addModule(@PathVariable Long id, @Valid @RequestBody ModuleRequest request) {
        ModuleResponse created = courseService.addModule(id, request);
        return ResponseEntity.created(URI.create("/api/modules/" + created.id())).body(created);
    }

    @Operation(summary = "Записать студента на курс")
    @PostMapping("/{id}/enroll")
    public ResponseEntity<EnrollmentResponse> enroll(@PathVariable Long id, @RequestParam("userId") Long userId) {
        EnrollmentResponse created = enrollmentService.enroll(id, userId);
        return ResponseEntity.created(URI.create("/api/enrollments/" + created.id())).body(created);
    }

    @Operation(summary = "Отписать студента от курса")
    @DeleteMapping("/{id}/enroll")
    public ResponseEntity<Void> unenroll(@PathVariable Long id, @RequestParam("userId") Long userId) {
        enrollmentService.unenroll(id, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Студенты, записанные на курс")
    @GetMapping("/{id}/students")
    public List<UserResponse> students(@PathVariable Long id) {
        return enrollmentService.studentsOfCourse(id);
    }

    @Operation(summary = "Записи на курс (со статусами)")
    @GetMapping("/{id}/enrollments")
    public List<EnrollmentResponse> enrollments(@PathVariable Long id) {
        return enrollmentService.enrollmentsOfCourse(id);
    }

    @Operation(summary = "Отзывы о курсе")
    @GetMapping("/{id}/reviews")
    public List<ReviewResponse> reviews(@PathVariable Long id) {
        return reviewService.findByCourse(id);
    }

    @Operation(summary = "Оставить отзыв (только записанный студент, один раз)")
    @PostMapping("/{id}/reviews")
    public ResponseEntity<ReviewResponse> addReview(@PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        ReviewResponse created = reviewService.create(id, request);
        return ResponseEntity.created(URI.create("/api/reviews/" + created.id())).body(created);
    }

    @Operation(summary = "Результаты всех тестов курса")
    @GetMapping("/{id}/quiz-results")
    public List<QuizResultResponse> quizResults(@PathVariable Long id) {
        return quizService.resultsByCourse(id);
    }

    @Operation(summary = "Статистика курса")
    @GetMapping("/{id}/stats")
    public CourseStatsResponse stats(@PathVariable Long id) {
        return analyticsService.courseStats(id);
    }

    @Operation(summary = "Рейтинг студентов курса")
    @GetMapping("/{id}/leaderboard")
    public List<LeaderboardEntry> leaderboard(@PathVariable Long id) {
        return analyticsService.leaderboard(id);
    }
}
