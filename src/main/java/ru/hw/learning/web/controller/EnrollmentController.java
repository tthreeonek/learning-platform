package ru.hw.learning.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.hw.learning.service.EnrollmentService;
import ru.hw.learning.service.NotificationService;
import ru.hw.learning.service.ReviewService;
import ru.hw.learning.web.dto.AnalyticsDtos.NotificationResponse;
import ru.hw.learning.web.dto.EnrollmentDtos.CertificateResponse;
import ru.hw.learning.web.dto.EnrollmentDtos.EnrollmentResponse;

@Tag(name = "Enrollments", description = "Записи на курс, сертификаты, отзывы, уведомления")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final ReviewService reviewService;
    private final NotificationService notificationService;

    @Operation(summary = "Запись на курс")
    @GetMapping("/enrollments/{id}")
    public EnrollmentResponse get(@PathVariable Long id) {
        return enrollmentService.get(id);
    }

    @Operation(summary = "Завершить курс и выдать сертификат")
    @PostMapping("/enrollments/{id}/complete")
    public ResponseEntity<CertificateResponse> complete(@PathVariable Long id) {
        CertificateResponse certificate = enrollmentService.complete(id);
        return ResponseEntity.created(URI.create("/api/certificates/" + certificate.code())).body(certificate);
    }

    @Operation(summary = "Проверить сертификат по коду")
    @GetMapping("/certificates/{code}")
    public CertificateResponse certificate(@PathVariable String code) {
        return enrollmentService.certificate(code);
    }

    @Operation(summary = "Удалить отзыв")
    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Отметить уведомление прочитанным")
    @PutMapping("/notifications/{id}/read")
    public NotificationResponse markRead(@PathVariable Long id) {
        return notificationService.markRead(id);
    }
}
