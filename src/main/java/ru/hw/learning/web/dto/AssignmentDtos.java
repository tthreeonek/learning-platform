package ru.hw.learning.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDateTime;
import ru.hw.learning.entity.SubmissionStatus;

/** DTO заданий и решений. */
public final class AssignmentDtos {

    private AssignmentDtos() {
    }

    public record AssignmentRequest(
            @NotBlank(message = "Название задания обязательно") @Size(max = 200) String title,
            @Size(max = 10000) String description,
            LocalDateTime dueDate,
            @Min(value = 1, message = "maxScore от 1 до 1000") @Max(value = 1000, message = "maxScore от 1 до 1000") Integer maxScore) {
    }

    public record AssignmentResponse(Long id, Long lessonId, String lessonTitle, String title, String description,
                                     LocalDateTime dueDate, int maxScore) {
    }

    public record SubmissionRequest(
            @NotNull(message = "studentId обязателен") Long studentId,
            @NotBlank(message = "Решение не может быть пустым") @Size(max = 20000) String content) {
    }

    public record GradeRequest(
            @NotNull(message = "Оценка обязательна") @Min(value = 0, message = "Оценка не может быть отрицательной") Integer score,
            @Size(max = 2000) String feedback) {
    }

    public record SubmissionResponse(Long id, Long assignmentId, String assignmentTitle, Long studentId, String studentName,
                                     String content, Instant submittedAt, Integer score, Integer maxScore, String feedback,
                                     SubmissionStatus status) {
    }
}
