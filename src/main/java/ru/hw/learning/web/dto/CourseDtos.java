package ru.hw.learning.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** DTO курсов, модулей и уроков. */
public final class CourseDtos {

    private CourseDtos() {
    }

    /** Создание курса. Можно сразу передать структуру: модули с уроками (сохраняются каскадно). */
    public record CourseRequest(
            @NotBlank(message = "Название курса обязательно") @Size(max = 200) String title,
            @Size(max = 10000) String description,
            @NotNull(message = "categoryId обязателен") Long categoryId,
            @NotNull(message = "teacherId обязателен") Long teacherId,
            @Positive(message = "Длительность должна быть больше 0") @Max(10000) Integer durationHours,
            LocalDate startDate,
            List<@NotBlank @Size(max = 50) String> tags,
            List<@Valid ModuleRequest> modules) {
    }

    /** Обновление курса: все поля, кроме структуры (модули меняются отдельными запросами). */
    public record CourseUpdateRequest(
            @NotBlank(message = "Название курса обязательно") @Size(max = 200) String title,
            @Size(max = 10000) String description,
            @NotNull(message = "categoryId обязателен") Long categoryId,
            @NotNull(message = "teacherId обязателен") Long teacherId,
            @Positive @Max(10000) Integer durationHours,
            LocalDate startDate,
            List<@NotBlank @Size(max = 50) String> tags) {
    }

    public record ModuleRequest(
            @NotBlank(message = "Название модуля обязательно") @Size(max = 200) String title,
            @Size(max = 2000) String description,
            @PositiveOrZero Integer orderIndex,
            List<@Valid LessonRequest> lessons) {
    }

    public record LessonRequest(
            @NotBlank(message = "Название урока обязательно") @Size(max = 200) String title,
            @Size(max = 100000) String content,
            @Size(max = 500) String videoUrl,
            @PositiveOrZero Integer orderIndex) {
    }

    public record CourseResponse(Long id, String title, String description, Long categoryId, String categoryName,
                                 Long teacherId, String teacherName, Integer durationHours, LocalDate startDate,
                                 List<String> tags) {
    }

    public record LessonResponse(Long id, Long moduleId, String title, String content, String videoUrl, int orderIndex) {
    }

    public record ModuleResponse(Long id, Long courseId, String title, String description, int orderIndex,
                                 List<LessonResponse> lessons, Long quizId) {
    }

    /** Полная структура курса: курс -> модули -> уроки. */
    public record CourseStructureResponse(CourseResponse course, List<ModuleResponse> modules) {
    }
}
