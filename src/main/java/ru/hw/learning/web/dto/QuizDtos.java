package ru.hw.learning.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import ru.hw.learning.entity.QuestionType;

/** DTO тестов: создание, прохождение, результаты. */
public final class QuizDtos {

    private QuizDtos() {
    }

    public record QuizRequest(
            @NotBlank(message = "Название теста обязательно") @Size(max = 200) String title,
            @Positive Integer timeLimitMinutes,
            @Min(0) @Max(100) Integer passingScore,
            @NotEmpty(message = "В тесте должен быть хотя бы один вопрос") List<@Valid QuestionRequest> questions) {
    }

    public record QuestionRequest(
            @NotBlank(message = "Текст вопроса обязателен") @Size(max = 1000) String text,
            @NotNull(message = "Тип вопроса обязателен") QuestionType type,
            @NotNull @Size(min = 2, message = "У вопроса должно быть минимум 2 варианта") List<@Valid OptionRequest> options) {
    }

    public record OptionRequest(
            @NotBlank(message = "Текст варианта обязателен") @Size(max = 500) String text,
            boolean correct) {
    }

    /** Прохождение теста: для каждого вопроса — список выбранных вариантов. */
    public record TakeQuizRequest(
            @NotNull(message = "studentId обязателен") Long studentId,
            @NotNull(message = "answers обязателен") Map<Long, List<Long>> answers) {
    }

    /** Вариант ответа. Поле correct заполняется только в режиме преподавателя (withAnswers=true). */
    public record OptionView(Long id, String text, Boolean correct) {
    }

    public record QuestionView(Long id, String text, QuestionType type, List<OptionView> options) {
    }

    public record QuizResponse(Long id, Long moduleId, String title, Integer timeLimitMinutes, int passingScore,
                               List<QuestionView> questions) {
    }

    public record QuizResultResponse(Long id, Long quizId, String quizTitle, Long studentId, String studentName,
                                     int score, int maxScore, double percent, boolean passed, Instant takenAt) {
    }
}
