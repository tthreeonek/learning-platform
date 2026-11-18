package ru.hw.learning.service;

import static ru.hw.learning.service.Guards.require;
import static ru.hw.learning.service.Guards.requireRole;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hw.learning.entity.AnswerOption;
import ru.hw.learning.entity.CourseModule;
import ru.hw.learning.entity.NotificationType;
import ru.hw.learning.entity.Question;
import ru.hw.learning.entity.QuestionType;
import ru.hw.learning.entity.Quiz;
import ru.hw.learning.entity.QuizAnswer;
import ru.hw.learning.entity.QuizSubmission;
import ru.hw.learning.entity.Role;
import ru.hw.learning.entity.User;
import ru.hw.learning.exception.BusinessRuleException;
import ru.hw.learning.exception.ConflictException;
import ru.hw.learning.mapper.LearningMapper;
import ru.hw.learning.repository.CourseModuleRepository;
import ru.hw.learning.repository.CourseRepository;
import ru.hw.learning.repository.QuestionRepository;
import ru.hw.learning.repository.QuizRepository;
import ru.hw.learning.repository.QuizSubmissionRepository;
import ru.hw.learning.repository.UserRepository;
import ru.hw.learning.web.dto.QuizDtos.OptionRequest;
import ru.hw.learning.web.dto.QuizDtos.QuestionRequest;
import ru.hw.learning.web.dto.QuizDtos.QuizRequest;
import ru.hw.learning.web.dto.QuizDtos.QuizResponse;
import ru.hw.learning.web.dto.QuizDtos.QuizResultResponse;
import ru.hw.learning.web.dto.QuizDtos.TakeQuizRequest;

/** Тесты: создание (Quiz -> Question -> AnswerOption), прохождение, результаты. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService {

    private static final int DEFAULT_PASSING_SCORE = 60;

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;
    private final CourseModuleRepository moduleRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentService enrollmentService;
    private final NotificationService notificationService;
    private final QuizGrader grader;

    /** Создаёт тест модуля (у модуля не больше одного теста — связь 1-1). Вопросы и варианты сохраняются каскадно. */
    @Transactional
    public QuizResponse create(Long moduleId, QuizRequest request) {
        CourseModule module = require(moduleRepository.findWithCourseById(moduleId), "Модуль", moduleId);
        if (quizRepository.existsByModuleId(moduleId)) {
            throw new ConflictException("У модуля id=" + moduleId + " уже есть тест");
        }
        Quiz quiz = new Quiz(request.title().trim(), request.timeLimitMinutes(),
                request.passingScore() == null ? DEFAULT_PASSING_SCORE : request.passingScore());
        for (int i = 0; i < request.questions().size(); i++) {
            quiz.addQuestion(buildQuestion(request.questions().get(i), i + 1));
        }
        module.attachQuiz(quiz);
        quizRepository.save(quiz);
        log.info("Модулю id={} добавлен тест id={} ({} вопросов)", moduleId, quiz.getId(), quiz.getQuestions().size());
        return LearningMapper.toResponse(quiz, quiz.getQuestions(), true);
    }

    /** Тест с вопросами и вариантами. Варианты загружаются JOIN FETCH-запросом. */
    public QuizResponse get(Long id, boolean withAnswers) {
        Quiz quiz = require(quizRepository.findWithCourseById(id), "Тест", id);
        return LearningMapper.toResponse(quiz, questionRepository.findWithOptionsByQuizId(id), withAnswers);
    }

    /**
     * Прохождение теста: answers = {questionId: [optionId, ...]}. Ответы проверяются по пометкам isCorrect,
     * результат и выбранные варианты сохраняются в QuizSubmission / QuizAnswer.
     */
    @Transactional
    public QuizResultResponse take(Long quizId, TakeQuizRequest request) {
        Quiz quiz = require(quizRepository.findWithCourseById(quizId), "Тест", quizId);
        User student = require(userRepository.findById(request.studentId()), "Пользователь", request.studentId());
        requireRole(student, "проходить тесты", Role.STUDENT);
        enrollmentService.requireActiveEnrollment(student.getId(), quiz.getModule().getCourse().getId());

        QuizGrader.Result result = grader.grade(questionRepository.findWithOptionsByQuizId(quizId), request.answers(),
                quiz.getPassingScore());
        QuizSubmission submission = new QuizSubmission(quiz, student);
        submission.setScore(result.score());
        submission.setMaxScore(result.maxScore());
        submission.setPercent(result.percent());
        submission.setPassed(result.passed());
        result.questions().forEach(r -> submission.addAnswer(new QuizAnswer(r.question(), r.selected(), r.correct())));
        quizSubmissionRepository.save(submission);

        notificationService.notify(student, NotificationType.QUIZ_RESULT, "Тест «" + quiz.getTitle() + "»: "
                + result.score() + "/" + result.maxScore() + (result.passed() ? " — пройден" : " — не пройден"));
        log.info("Студент id={} прошёл тест id={}: {}/{} ({}%)", student.getId(), quizId, result.score(), result.maxScore(),
                result.percent());
        return LearningMapper.toResponse(submission);
    }

    public List<QuizResultResponse> resultsByQuiz(Long quizId) {
        require(quizRepository.findById(quizId), "Тест", quizId);
        return quizSubmissionRepository.findAllByQuizIdOrderByTakenAtDesc(quizId).stream().map(LearningMapper::toResponse).toList();
    }

    public List<QuizResultResponse> resultsByStudent(Long studentId) {
        require(userRepository.findById(studentId), "Пользователь", studentId);
        return quizSubmissionRepository.findAllByStudentIdOrderByTakenAtDesc(studentId).stream()
                .map(LearningMapper::toResponse)
                .toList();
    }

    public List<QuizResultResponse> resultsByCourse(Long courseId) {
        require(courseRepository.findById(courseId), "Курс", courseId);
        return quizSubmissionRepository.findAllByCourseId(courseId).stream().map(LearningMapper::toResponse).toList();
    }

    /** Удаление теста: вопросы, варианты и все попытки удаляются каскадно. */
    @Transactional
    public void delete(Long id) {
        Quiz quiz = require(quizRepository.findById(id), "Тест", id);
        quiz.getModule().setQuiz(null);
        quizRepository.delete(quiz);
        log.info("Удалён тест id={}", id);
    }

    private Question buildQuestion(QuestionRequest request, int order) {
        long correct = request.options().stream().filter(OptionRequest::correct).count();
        if (correct == 0) {
            throw new BusinessRuleException("У вопроса «" + request.text() + "» нет правильного варианта");
        }
        if (request.type() == QuestionType.SINGLE_CHOICE && correct > 1) {
            throw new BusinessRuleException("У вопроса с одним ответом «" + request.text() + "» несколько правильных вариантов");
        }
        Question question = new Question(request.text().trim(), request.type(), order);
        request.options().forEach(o -> question.addOption(new AnswerOption(o.text().trim(), o.correct())));
        return question;
    }
}
