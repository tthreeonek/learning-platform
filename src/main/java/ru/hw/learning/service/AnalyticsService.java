package ru.hw.learning.service;

import static ru.hw.learning.service.Guards.require;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hw.learning.entity.Course;
import ru.hw.learning.entity.Enrollment;
import ru.hw.learning.entity.EnrollmentStatus;
import ru.hw.learning.entity.User;
import ru.hw.learning.repository.AssignmentRepository;
import ru.hw.learning.repository.CourseModuleRepository;
import ru.hw.learning.repository.CourseRepository;
import ru.hw.learning.repository.CourseReviewRepository;
import ru.hw.learning.repository.EnrollmentRepository;
import ru.hw.learning.repository.QuizSubmissionRepository;
import ru.hw.learning.repository.SubmissionRepository;
import ru.hw.learning.repository.UserRepository;
import ru.hw.learning.web.dto.AnalyticsDtos.CourseProgress;
import ru.hw.learning.web.dto.AnalyticsDtos.CourseStatsResponse;
import ru.hw.learning.web.dto.AnalyticsDtos.LeaderboardEntry;
import ru.hw.learning.web.dto.AnalyticsDtos.StudentProgressResponse;

/** Аналитика: успеваемость студента, статистика курса, рейтинг студентов курса. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseModuleRepository moduleRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;
    private final CourseReviewRepository reviewRepository;
    private final ProgressCalculator progress;

    /** Успеваемость студента по всем его курсам. */
    public StudentProgressResponse studentProgress(Long studentId) {
        User student = require(userRepository.findById(studentId), "Пользователь", studentId);
        List<CourseProgress> courses = enrollmentRepository.findAllByStudentIdOrderByEnrolledAt(studentId).stream()
                .map(this::courseProgress)
                .toList();
        Double overall = ProgressCalculator.average(courses.stream()
                .map(CourseProgress::overallPercent)
                .filter(Objects::nonNull)
                .toList());
        return new StudentProgressResponse(student.getId(), student.getName(), courses, ProgressCalculator.round(overall));
    }

    public CourseStatsResponse courseStats(Long courseId) {
        Course course = require(courseRepository.findById(courseId), "Курс", courseId);
        Double rating = reviewRepository.averageRating(courseId);
        return new CourseStatsResponse(course.getId(), course.getTitle(),
                enrollmentRepository.countByCourseId(courseId),
                enrollmentRepository.countByCourseIdAndStatus(courseId, EnrollmentStatus.COMPLETED),
                moduleRepository.countByCourseId(courseId),
                assignmentRepository.countByCourseId(courseId),
                submissionRepository.countByCourseId(courseId),
                quizSubmissionRepository.countByQuizModuleCourseId(courseId),
                ProgressCalculator.round(rating),
                reviewRepository.countByCourseId(courseId));
    }

    /** Рейтинг студентов курса по итоговому проценту (студенты без оценок — в конце). */
    public List<LeaderboardEntry> leaderboard(Long courseId) {
        require(courseRepository.findById(courseId), "Курс", courseId);
        List<LeaderboardEntry> rows = new ArrayList<>();
        enrollmentRepository.findAllByCourseIdOrderByEnrolledAt(courseId).forEach(e -> rows.add(new LeaderboardEntry(0,
                e.getStudent().getId(), e.getStudent().getName(), progress.overallPercent(e.getStudent().getId(), courseId))));
        rows.sort(Comparator.comparing(LeaderboardEntry::overallPercent, Comparator.nullsLast(Comparator.reverseOrder())));
        List<LeaderboardEntry> ranked = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            LeaderboardEntry r = rows.get(i);
            ranked.add(new LeaderboardEntry(i + 1, r.studentId(), r.studentName(), r.overallPercent()));
        }
        return ranked;
    }

    private CourseProgress courseProgress(Enrollment e) {
        Long studentId = e.getStudent().getId();
        Long courseId = e.getCourse().getId();
        long submitted = submissionRepository.countByStudentAndCourse(studentId, courseId);
        Double assignments = progress.assignmentsPercent(studentId, courseId);
        Double quizzes = progress.quizzesPercent(studentId, courseId);
        return new CourseProgress(courseId, e.getCourse().getTitle(), e.getStatus(),
                assignmentRepository.countByCourseId(courseId), submitted, assignments, quizzes,
                ProgressCalculator.combine(assignments, quizzes));
    }
}
