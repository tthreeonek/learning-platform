package ru.hw.learning.entity;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Решение задания студентом. Связка M-M между User и Assignment с данными:
 * текст решения, время, оценка, отзыв. Одно решение на пару (assignment, student).
 */
@Entity
@Table(name = "submissions",
        uniqueConstraints = @UniqueConstraint(name = "uk_submission_assignment_student",
                columnNames = {"assignment_id", "student_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt = Instant.now();

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    private Integer score;

    @Column(length = 2000)
    private String feedback;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubmissionStatus status = SubmissionStatus.SUBMITTED;

    @Column(name = "graded_at")
    private Instant gradedAt;

    public Submission(Assignment assignment, User student, String content) {
        this.assignment = assignment;
        this.student = student;
        this.content = content;
    }

    public void grade(int newScore, String newFeedback) {
        this.score = newScore;
        this.feedback = newFeedback;
        this.status = SubmissionStatus.GRADED;
        this.gradedAt = Instant.now();
    }
}
