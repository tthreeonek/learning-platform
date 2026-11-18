package ru.hw.learning.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** Отзыв студента о курсе. Один отзыв на пару (course, student). */
@Entity
@Table(name = "course_reviews",
        uniqueConstraints = @UniqueConstraint(name = "uk_review_course_student", columnNames = {"course_id", "student_id"}))
@Getter
@Setter
@NoArgsConstructor
public class CourseReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false)
    private int rating;

    @Column(length = 2000)
    private String comment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public CourseReview(Course course, User student, int rating, String comment) {
        this.course = course;
        this.student = student;
        this.rating = rating;
        this.comment = comment;
    }
}
