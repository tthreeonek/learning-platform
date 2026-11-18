package ru.hw.learning.web.dto;

import java.time.Instant;
import java.util.List;
import ru.hw.learning.entity.EnrollmentStatus;
import ru.hw.learning.entity.NotificationType;

/** DTO аналитики и уведомлений. */
public final class AnalyticsDtos {

    private AnalyticsDtos() {
    }

    /** Успеваемость студента по одному курсу. Проценты null, если оценок ещё нет. */
    public record CourseProgress(Long courseId, String courseTitle, EnrollmentStatus status, long assignmentsTotal,
                                 long assignmentsSubmitted, Double assignmentsAvgPercent, Double quizzesAvgPercent,
                                 Double overallPercent) {
    }

    public record StudentProgressResponse(Long studentId, String studentName, List<CourseProgress> courses, Double overallPercent) {
    }

    public record CourseStatsResponse(Long courseId, String title, long students, long completed, long modules,
                                      long assignments, long submissions, long quizAttempts, Double averageRating,
                                      long reviews) {
    }

    public record LeaderboardEntry(int place, Long studentId, String studentName, Double overallPercent) {
    }

    public record NotificationResponse(Long id, NotificationType type, String message, boolean read, Instant createdAt) {
    }
}
