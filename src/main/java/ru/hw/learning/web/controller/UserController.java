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
import ru.hw.learning.entity.Role;
import ru.hw.learning.service.AnalyticsService;
import ru.hw.learning.service.AssignmentService;
import ru.hw.learning.service.EnrollmentService;
import ru.hw.learning.service.NotificationService;
import ru.hw.learning.service.QuizService;
import ru.hw.learning.service.SubmissionService;
import ru.hw.learning.service.UserService;
import ru.hw.learning.web.dto.AnalyticsDtos.NotificationResponse;
import ru.hw.learning.web.dto.AnalyticsDtos.StudentProgressResponse;
import ru.hw.learning.web.dto.AssignmentDtos.AssignmentResponse;
import ru.hw.learning.web.dto.AssignmentDtos.SubmissionResponse;
import ru.hw.learning.web.dto.CourseDtos.CourseResponse;
import ru.hw.learning.web.dto.EnrollmentDtos.EnrollmentResponse;
import ru.hw.learning.web.dto.QuizDtos.QuizResultResponse;
import ru.hw.learning.web.dto.UserDtos.ProfileRequest;
import ru.hw.learning.web.dto.UserDtos.UserDetailsResponse;
import ru.hw.learning.web.dto.UserDtos.UserRequest;
import ru.hw.learning.web.dto.UserDtos.UserResponse;

@Tag(name = "Users", description = "Пользователи, профили и всё, что относится к студенту")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final EnrollmentService enrollmentService;
    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;
    private final QuizService quizService;
    private final NotificationService notificationService;
    private final AnalyticsService analyticsService;

    @Operation(summary = "Создать пользователя")
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        UserResponse created = userService.create(request);
        return ResponseEntity.created(URI.create("/api/users/" + created.id())).body(created);
    }

    @Operation(summary = "Список пользователей (фильтр по роли)")
    @GetMapping
    public List<UserResponse> list(@RequestParam(required = false) Role role) {
        return userService.findAll(role);
    }

    @Operation(summary = "Пользователь с профилем")
    @GetMapping("/{id}")
    public UserDetailsResponse get(@PathVariable Long id) {
        return userService.get(id);
    }

    @Operation(summary = "Обновить пользователя")
    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        return userService.update(id, request);
    }

    @Operation(summary = "Удалить пользователя (каскадно: профиль, записи, решения, результаты)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Создать или обновить профиль (1-1)")
    @PutMapping("/{id}/profile")
    public UserDetailsResponse updateProfile(@PathVariable Long id, @Valid @RequestBody ProfileRequest request) {
        return userService.updateProfile(id, request);
    }

    @Operation(summary = "Курсы, на которые записан студент")
    @GetMapping("/{id}/courses")
    public List<CourseResponse> courses(@PathVariable Long id) {
        return enrollmentService.coursesOfStudent(id);
    }

    @Operation(summary = "Записи студента на курсы")
    @GetMapping("/{id}/enrollments")
    public List<EnrollmentResponse> enrollments(@PathVariable Long id) {
        return enrollmentService.enrollmentsOfStudent(id);
    }

    @Operation(summary = "Задания всех курсов студента")
    @GetMapping("/{id}/assignments")
    public List<AssignmentResponse> assignments(@PathVariable Long id) {
        return assignmentService.findForStudent(id);
    }

    @Operation(summary = "Решения студента с оценками")
    @GetMapping("/{id}/submissions")
    public List<SubmissionResponse> submissions(@PathVariable Long id) {
        return submissionService.findByStudent(id);
    }

    @Operation(summary = "Результаты тестов студента")
    @GetMapping("/{id}/quiz-results")
    public List<QuizResultResponse> quizResults(@PathVariable Long id) {
        return quizService.resultsByStudent(id);
    }

    @Operation(summary = "Уведомления пользователя")
    @GetMapping("/{id}/notifications")
    public List<NotificationResponse> notifications(@PathVariable Long id) {
        return notificationService.findForUser(id);
    }

    @Operation(summary = "Успеваемость студента по всем курсам")
    @GetMapping("/{id}/progress")
    public StudentProgressResponse progress(@PathVariable Long id) {
        return analyticsService.studentProgress(id);
    }
}
