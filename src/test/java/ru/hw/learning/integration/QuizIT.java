package ru.hw.learning.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.hw.learning.exception.BusinessRuleException;
import ru.hw.learning.exception.ConflictException;
import ru.hw.learning.repository.QuizAnswerRepository;
import ru.hw.learning.service.CourseService;
import ru.hw.learning.service.EnrollmentService;
import ru.hw.learning.service.QuizService;
import ru.hw.learning.support.IntegrationTestBase;
import ru.hw.learning.web.dto.CourseDtos.CourseStructureResponse;
import ru.hw.learning.web.dto.QuizDtos.QuestionView;
import ru.hw.learning.web.dto.QuizDtos.QuizResponse;
import ru.hw.learning.web.dto.QuizDtos.QuizResultResponse;
import ru.hw.learning.web.dto.QuizDtos.TakeQuizRequest;

/** Тесты: Quiz -> Question -> AnswerOption, прохождение, сохранение результата и выбранных ответов. */
class QuizIT extends IntegrationTestBase {

    @Autowired
    private QuizService quizService;
    @Autowired
    private CourseService courseService;
    @Autowired
    private EnrollmentService enrollmentService;
    @Autowired
    private QuizAnswerRepository quizAnswerRepository;

    private CourseStructureResponse course;
    private QuizResponse quiz;
    private Long student;

    @BeforeEach
    void setUp() {
        course = data.courseWithStructure();
        quiz = data.quiz(course.modules().get(0).id());
        student = data.student();
        data.enroll(course.course().id(), student);
    }

    @Test
    @DisplayName("Тест сохраняется каскадно: 2 вопроса, 5 вариантов; студенту правильные ответы не показываются")
    void quizCreated() {
        assertThat(count("questions")).isEqualTo(2);
        assertThat(count("answer_options")).isEqualTo(5);
        assertThat(quizService.get(quiz.id(), false).questions().get(0).options()).allSatisfy(o -> assertThat(o.correct()).isNull());
        assertThat(courseService.getStructure(course.course().id()).modules().get(0).quizId()).isEqualTo(quiz.id());
    }

    @Test
    @DisplayName("Все ответы верные: 2/2, 100%, тест пройден; выбранные варианты сохранены")
    void takeAllCorrect() {
        QuizResultResponse result = quizService.take(quiz.id(), new TakeQuizRequest(student, answers(true, true)));

        assertThat(result.score()).isEqualTo(2);
        assertThat(result.percent()).isEqualTo(100.0);
        assertThat(result.passed()).isTrue();
        assertThat(quizAnswerRepository.findAllByQuizSubmissionId(result.id()))
                .hasSize(2)
                .allSatisfy(a -> assertThat(a.isCorrect()).isTrue());
        assertThat(count("quiz_answer_option")).isEqualTo(3);
    }

    @Test
    @DisplayName("Один ответ неверный: 1/2 = 50% — порог 50% пройден; результаты доступны по тесту, студенту и курсу")
    void takePartially() {
        quizService.take(quiz.id(), new TakeQuizRequest(student, answers(true, false)));

        assertThat(quizService.resultsByQuiz(quiz.id())).extracting("percent").containsExactly(50.0);
        assertThat(quizService.resultsByStudent(student)).hasSize(1);
        assertThat(quizService.resultsByCourse(course.course().id())).extracting("passed").containsExactly(true);
    }

    @Test
    @DisplayName("Пройти тест может только записанный на курс студент; чужие варианты отклоняются")
    void validation() {
        Long stranger = data.student();
        assertThatThrownBy(() -> quizService.take(quiz.id(), new TakeQuizRequest(stranger, answers(true, true))))
                .isInstanceOf(BusinessRuleException.class);

        Long q1 = quiz.questions().get(0).id();
        assertThatThrownBy(() -> quizService.take(quiz.id(), new TakeQuizRequest(student, Map.of(q1, List.of(-1L)))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("У модуля только один тест (1-1)")
    void oneQuizPerModule() {
        assertThatThrownBy(() -> data.quiz(course.modules().get(0).id())).isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Удаление курса после отписки удаляет тест, вопросы, попытки и выбранные ответы")
    void deleteCourseWithQuizAttempts() {
        quizService.take(quiz.id(), new TakeQuizRequest(student, answers(true, false)));
        enrollmentService.unenroll(course.course().id(), student);

        courseService.delete(course.course().id());

        assertThat(count("quizzes")).isZero();
        assertThat(count("questions")).isZero();
        assertThat(count("answer_options")).isZero();
        assertThat(count("quiz_submissions")).isZero();
        assertThat(count("quiz_answers")).isZero();
        assertThat(count("quiz_answer_option")).isZero();
    }

    /** Ответы: для каждого вопроса — правильные варианты или один неправильный. */
    private Map<Long, List<Long>> answers(boolean firstCorrect, boolean secondCorrect) {
        return Map.of(
                quiz.questions().get(0).id(), pick(quiz.questions().get(0), firstCorrect),
                quiz.questions().get(1).id(), pick(quiz.questions().get(1), secondCorrect));
    }

    private static List<Long> pick(QuestionView q, boolean correct) {
        List<Long> ids = q.options().stream().filter(o -> o.correct() == correct).map(o -> o.id()).toList();
        return correct ? ids : ids.subList(0, 1);
    }
}
