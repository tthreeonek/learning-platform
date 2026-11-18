package ru.hw.learning.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import ru.hw.learning.entity.EnrollmentStatus;

/** DTO записей на курс, сертификатов и отзывов. */
public final class EnrollmentDtos {

    private EnrollmentDtos() {
    }

    public record EnrollmentResponse(Long id, Long studentId, String studentName, Long courseId, String courseTitle,
                                     Instant enrolledAt, EnrollmentStatus status, Instant completedAt) {
    }

    public record CertificateResponse(String code, Long enrollmentId, String studentName, String courseTitle,
                                      Instant issuedAt, Double finalScore) {
    }

    public record ReviewRequest(
            @NotNull(message = "studentId обязателен") Long studentId,
            @NotNull(message = "Оценка обязательна") @Min(value = 1, message = "Оценка от 1 до 5")
            @Max(value = 5, message = "Оценка от 1 до 5") Integer rating,
            @Size(max = 2000) String comment) {
    }

    public record ReviewResponse(Long id, Long courseId, Long studentId, String studentName, int rating, String comment,
                                 Instant createdAt) {
    }
}
