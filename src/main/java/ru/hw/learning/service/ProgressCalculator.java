package ru.hw.learning.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.hw.learning.repository.QuizSubmissionRepository;
import ru.hw.learning.repository.SubmissionRepository;

/** Расчёт успеваемости студента по курсу: средний % по заданиям и по лучшим попыткам тестов. */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgressCalculator {

    private final SubmissionRepository submissionRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;

    public Double assignmentsPercent(Long studentId, Long courseId) {
        return round(submissionRepository.averagePercent(studentId, courseId));
    }

    public Double quizzesPercent(Long studentId, Long courseId) {
        return round(average(quizSubmissionRepository.bestPercentsPerQuiz(studentId, courseId)));
    }

    /** Итог: среднее из двух показателей; если какого-то нет — берётся другой; если нет обоих — null. */
    public Double overallPercent(Long studentId, Long courseId) {
        return combine(assignmentsPercent(studentId, courseId), quizzesPercent(studentId, courseId));
    }

    public static Double combine(Double assignments, Double quizzes) {
        return round(average(Stream.of(assignments, quizzes).filter(Objects::nonNull).toList()));
    }

    static Double average(List<Double> values) {
        return values == null || values.isEmpty() ? null
                : values.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
    }

    static Double round(Double value) {
        return value == null ? null : Math.round(value * 10.0) / 10.0;
    }
}
