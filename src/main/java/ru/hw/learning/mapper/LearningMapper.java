package ru.hw.learning.mapper;

import java.util.Comparator;
import java.util.List;
import ru.hw.learning.entity.AnswerOption;
import ru.hw.learning.entity.Assignment;
import ru.hw.learning.entity.Certificate;
import ru.hw.learning.entity.CourseReview;
import ru.hw.learning.entity.Enrollment;
import ru.hw.learning.entity.Question;
import ru.hw.learning.entity.Quiz;
import ru.hw.learning.entity.QuizSubmission;
import ru.hw.learning.entity.Submission;
import ru.hw.learning.web.dto.AssignmentDtos.AssignmentResponse;
import ru.hw.learning.web.dto.AssignmentDtos.SubmissionResponse;
import ru.hw.learning.web.dto.EnrollmentDtos.CertificateResponse;
import ru.hw.learning.web.dto.EnrollmentDtos.EnrollmentResponse;
import ru.hw.learning.web.dto.EnrollmentDtos.ReviewResponse;
import ru.hw.learning.web.dto.QuizDtos.OptionView;
import ru.hw.learning.web.dto.QuizDtos.QuestionView;
import ru.hw.learning.web.dto.QuizDtos.QuizResponse;
import ru.hw.learning.web.dto.QuizDtos.QuizResultResponse;

/** Преобразование учебного процесса (записи, задания, решения, тесты, отзывы) в DTO. */
public final class LearningMapper {

    private LearningMapper() {
    }

    public static EnrollmentResponse toResponse(Enrollment e) {
        return new EnrollmentResponse(e.getId(), e.getStudent().getId(), e.getStudent().getName(),
                e.getCourse().getId(), e.getCourse().getTitle(), e.getEnrolledAt(), e.getStatus(), e.getCompletedAt());
    }

    public static CertificateResponse toResponse(Certificate c) {
        Enrollment e = c.getEnrollment();
        return new CertificateResponse(c.getCode(), e.getId(), e.getStudent().getName(), e.getCourse().getTitle(),
                c.getIssuedAt(), c.getFinalScore());
    }

    public static AssignmentResponse toResponse(Assignment a) {
        return new AssignmentResponse(a.getId(), a.getLesson().getId(), a.getLesson().getTitle(), a.getTitle(),
                a.getDescription(), a.getDueDate(), a.getMaxScore());
    }

    public static SubmissionResponse toResponse(Submission s) {
        return new SubmissionResponse(s.getId(), s.getAssignment().getId(), s.getAssignment().getTitle(),
                s.getStudent().getId(), s.getStudent().getName(), s.getContent(), s.getSubmittedAt(), s.getScore(),
                s.getAssignment().getMaxScore(), s.getFeedback(), s.getStatus());
    }

    public static ReviewResponse toResponse(CourseReview r) {
        return new ReviewResponse(r.getId(), r.getCourse().getId(), r.getStudent().getId(), r.getStudent().getName(),
                r.getRating(), r.getComment(), r.getCreatedAt());
    }

    public static QuizResultResponse toResponse(QuizSubmission qs) {
        return new QuizResultResponse(qs.getId(), qs.getQuiz().getId(), qs.getQuiz().getTitle(), qs.getStudent().getId(),
                qs.getStudent().getName(), qs.getScore(), qs.getMaxScore(), qs.getPercent(), qs.isPassed(), qs.getTakenAt());
    }

    /**
     * Тест с вопросами. Для студента правильные ответы скрыты (correct = null),
     * преподаватель запрашивает их через withAnswers=true.
     */
    public static QuizResponse toResponse(Quiz quiz, List<Question> questions, boolean withAnswers) {
        List<QuestionView> views = questions.stream()
                .sorted(Comparator.comparingInt(Question::getOrderIndex))
                .map(q -> new QuestionView(q.getId(), q.getText(), q.getType(), q.getOptions().stream()
                        .sorted(Comparator.comparing(AnswerOption::getId))
                        .map(o -> new OptionView(o.getId(), o.getText(), withAnswers ? o.isCorrect() : null))
                        .toList()))
                .toList();
        return new QuizResponse(quiz.getId(), quiz.getModule().getId(), quiz.getTitle(), quiz.getTimeLimitMinutes(),
                quiz.getPassingScore(), views);
    }
}
