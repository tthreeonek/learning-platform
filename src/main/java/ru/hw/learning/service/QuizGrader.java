package ru.hw.learning.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import ru.hw.learning.entity.AnswerOption;
import ru.hw.learning.entity.Question;
import ru.hw.learning.entity.QuestionType;
import ru.hw.learning.exception.BusinessRuleException;

/**
 * Проверка ответов теста (чистая логика без БД — покрыта юнит-тестами).
 * Вопрос засчитывается, только если выбранное множество вариантов совпадает с множеством правильных.
 */
@Component
public class QuizGrader {

    /** Результат проверки одного вопроса. */
    public record QuestionResult(Question question, Set<AnswerOption> selected, boolean correct) {
    }

    /** Итог по тесту. */
    public record Result(List<QuestionResult> questions, int score, int maxScore, double percent, boolean passed) {
    }

    public Result grade(List<Question> questions, Map<Long, List<Long>> answers, int passingScore) {
        Map<Long, Question> byId = questions.stream().collect(Collectors.toMap(Question::getId, Function.identity()));
        answers.keySet().stream()
                .filter(questionId -> !byId.containsKey(questionId))
                .findFirst()
                .ifPresent(questionId -> {
                    throw new BusinessRuleException("Вопрос id=" + questionId + " не относится к этому тесту");
                });

        List<QuestionResult> results = questions.stream()
                .map(q -> gradeQuestion(q, answers.getOrDefault(q.getId(), List.of())))
                .toList();
        int score = (int) results.stream().filter(QuestionResult::correct).count();
        int max = questions.size();
        double percent = max == 0 ? 0 : Math.round(score * 1000.0 / max) / 10.0;
        return new Result(results, score, max, percent, percent >= passingScore);
    }

    private QuestionResult gradeQuestion(Question question, List<Long> selectedIds) {
        Map<Long, AnswerOption> options = question.getOptions().stream()
                .collect(Collectors.toMap(AnswerOption::getId, Function.identity()));
        Set<AnswerOption> selected = new HashSet<>();
        for (Long optionId : new HashSet<>(selectedIds)) {
            AnswerOption option = options.get(optionId);
            if (option == null) {
                throw new BusinessRuleException("Вариант id=" + optionId + " не относится к вопросу id=" + question.getId());
            }
            selected.add(option);
        }
        if (question.getType() == QuestionType.SINGLE_CHOICE && selected.size() > 1) {
            throw new BusinessRuleException("В вопросе id=" + question.getId() + " можно выбрать только один вариант");
        }
        Set<AnswerOption> correctOptions = question.getOptions().stream()
                .filter(AnswerOption::isCorrect)
                .collect(Collectors.toSet());
        boolean correct = !selected.isEmpty() && selected.equals(correctOptions);
        return new QuestionResult(question, selected, correct);
    }
}
