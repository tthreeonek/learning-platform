package ru.hw.learning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.hw.learning.entity.AnswerOption;
import ru.hw.learning.entity.Question;
import ru.hw.learning.entity.QuestionType;
import ru.hw.learning.exception.BusinessRuleException;

/** Юнит-тесты проверки ответов теста (без Spring и БД). */
class QuizGraderTest {

    private final QuizGrader grader = new QuizGrader();
    private Question single;
    private Question multiple;

    @BeforeEach
    void setUp() {
        single = question(1L, QuestionType.SINGLE_CHOICE, option(11L, true), option(12L, false));
        multiple = question(2L, QuestionType.MULTIPLE_CHOICE, option(21L, true), option(22L, true), option(23L, false));
    }

    @Test
    @DisplayName("Все ответы верные -> 2/2, 100%, пройден")
    void allCorrect() {
        QuizGrader.Result r = grader.grade(List.of(single, multiple), Map.of(1L, List.of(11L), 2L, List.of(21L, 22L)), 60);

        assertThat(r.score()).isEqualTo(2);
        assertThat(r.maxScore()).isEqualTo(2);
        assertThat(r.percent()).isEqualTo(100.0);
        assertThat(r.passed()).isTrue();
    }

    @Test
    @DisplayName("Множественный выбор засчитывается только при точном совпадении множества")
    void multipleChoiceNeedsExactSet() {
        QuizGrader.Result partial = grader.grade(List.of(multiple), Map.of(2L, List.of(21L)), 60);
        QuizGrader.Result extra = grader.grade(List.of(multiple), Map.of(2L, List.of(21L, 22L, 23L)), 60);

        assertThat(partial.score()).isZero();
        assertThat(extra.score()).isZero();
    }

    @Test
    @DisplayName("Вопрос без ответа считается неверным; ниже порога — не пройден")
    void unansweredIsWrong() {
        QuizGrader.Result r = grader.grade(List.of(single, multiple), Map.of(1L, List.of(11L)), 60);

        assertThat(r.score()).isEqualTo(1);
        assertThat(r.percent()).isEqualTo(50.0);
        assertThat(r.passed()).isFalse();
        assertThat(r.questions().get(1).selected()).isEmpty();
    }

    @Test
    @DisplayName("Дубли выбранного варианта не ломают проверку")
    void duplicatesIgnored() {
        assertThat(grader.grade(List.of(single), Map.of(1L, List.of(11L, 11L)), 60).score()).isEqualTo(1);
    }

    @Test
    @DisplayName("В вопросе с одним ответом нельзя выбрать два варианта")
    void singleChoiceRejectsSeveral() {
        assertThatThrownBy(() -> grader.grade(List.of(single), Map.of(1L, List.of(11L, 12L)), 60))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Вариант от другого вопроса и чужой вопрос отклоняются")
    void foreignIdsRejected() {
        assertThatThrownBy(() -> grader.grade(List.of(single), Map.of(1L, List.of(21L)), 60))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Вариант id=21");
        assertThatThrownBy(() -> grader.grade(List.of(single), Map.of(99L, List.of(11L)), 60))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Вопрос id=99");
    }

    @Test
    @DisplayName("Процент округляется до десятых: 1 из 3 = 33.3%")
    void percentRounding() {
        Question third = question(3L, QuestionType.SINGLE_CHOICE, option(31L, true), option(32L, false));
        QuizGrader.Result r = grader.grade(List.of(single, multiple, third), Map.of(1L, List.of(11L)), 30);

        assertThat(r.percent()).isEqualTo(33.3);
        assertThat(r.passed()).isTrue();
    }

    private static Question question(Long id, QuestionType type, AnswerOption... options) {
        Question q = new Question("Q" + id, type, id.intValue());
        q.setId(id);
        for (AnswerOption o : options) {
            q.addOption(o);
        }
        return q;
    }

    private static AnswerOption option(Long id, boolean correct) {
        AnswerOption o = new AnswerOption("O" + id, correct);
        o.setId(id);
        return o;
    }
}
