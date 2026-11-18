package ru.hw.learning.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Запись студента на курс — сущность-связка M-M между User и Course
 * с дополнительными полями (дата, статус, прогресс). Пара (student, course) уникальна.
 */
@Entity
@Table(name = "enrollments",
        uniqueConstraints = @UniqueConstraint(name = "uk_enrollment_student_course", columnNames = {"student_id", "course_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "enrolled_at", nullable = false)
    private Instant enrolledAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    @Column(name = "completed_at")
    private Instant completedAt;

    /** 1-1: сертификат выдаётся при завершении курса. */
    @OneToOne(mappedBy = "enrollment", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Certificate certificate;

    public Enrollment(User student, Course course) {
        this.student = student;
        this.course = course;
    }

    public void attachCertificate(Certificate newCertificate) {
        this.certificate = newCertificate;
        newCertificate.setEnrollment(this);
    }
}
