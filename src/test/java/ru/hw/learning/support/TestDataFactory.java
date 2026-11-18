package ru.hw.learning.support;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import ru.hw.learning.entity.QuestionType;
import ru.hw.learning.entity.Role;
import ru.hw.learning.service.AssignmentService;
import ru.hw.learning.service.CatalogService;
import ru.hw.learning.service.CourseService;
import ru.hw.learning.service.EnrollmentService;
import ru.hw.learning.service.QuizService;
import ru.hw.learning.service.UserService;
import ru.hw.learning.web.dto.AssignmentDtos.AssignmentRequest;
import ru.hw.learning.web.dto.CatalogDtos.CategoryRequest;
import ru.hw.learning.web.dto.CourseDtos.CourseRequest;
import ru.hw.learning.web.dto.CourseDtos.CourseStructureResponse;
import ru.hw.learning.web.dto.CourseDtos.LessonRequest;
import ru.hw.learning.web.dto.CourseDtos.ModuleRequest;
import ru.hw.learning.web.dto.QuizDtos.OptionRequest;
import ru.hw.learning.web.dto.QuizDtos.QuestionRequest;
import ru.hw.learning.web.dto.QuizDtos.QuizRequest;
import ru.hw.learning.web.dto.QuizDtos.QuizResponse;
import ru.hw.learning.web.dto.UserDtos.UserRequest;

/** Создание тестовых данных через сервисный слой. */
@TestComponent
public class TestDataFactory {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private CatalogService catalogService;
    @Autowired
    private UserService userService;
    @Autowired
    private CourseService courseService;
    @Autowired
    private EnrollmentService enrollmentService;
    @Autowired
    private AssignmentService assignmentService;
    @Autowired
    private QuizService quizService;

    public Long category() {
        return catalogService.createCategory(new CategoryRequest("Категория " + SEQ.incrementAndGet(), null)).id();
    }

    public Long user(Role role) {
        int n = SEQ.incrementAndGet();
        return userService.create(new UserRequest(role.name() + " " + n, "user" + n + "@test.local", role)).id();
    }

    public Long teacher() {
        return user(Role.TEACHER);
    }

    public Long student() {
        return user(Role.STUDENT);
    }

    /** Курс «Hibernate» с двумя модулями по два урока и тегами Java, Hibernate. */
    public CourseStructureResponse courseWithStructure() {
        Long courseId = courseService.create(new CourseRequest("Hibernate " + SEQ.incrementAndGet(), "Описание",
                category(), teacher(), 10, LocalDate.now(), List.of("Java", "Hibernate"), List.of(
                        new ModuleRequest("Модуль 1", null, null, List.of(lesson("Урок 1.1"), lesson("Урок 1.2"))),
                        new ModuleRequest("Модуль 2", null, null, List.of(lesson("Урок 2.1"), lesson("Урок 2.2")))))).id();
        return courseService.getStructure(courseId);
    }

    public Long enroll(Long courseId, Long studentId) {
        return enrollmentService.enroll(courseId, studentId).id();
    }

    public Long assignment(Long lessonId, int maxScore, LocalDateTime dueDate) {
        return assignmentService.create(lessonId, new AssignmentRequest("Задание", "Сделать", dueDate, maxScore)).id();
    }

    public Long assignment(Long lessonId) {
        return assignment(lessonId, 100, LocalDateTime.now().plusDays(7));
    }

    /** Тест из двух вопросов: Q1 — один ответ (первый вариант верный), Q2 — несколько (первые два верные). */
    public QuizResponse quiz(Long moduleId) {
        return quizService.create(moduleId, new QuizRequest("Тест", 10, 50, List.of(
                new QuestionRequest("Q1", QuestionType.SINGLE_CHOICE, List.of(
                        new OptionRequest("верно", true), new OptionRequest("неверно", false))),
                new QuestionRequest("Q2", QuestionType.MULTIPLE_CHOICE, List.of(
                        new OptionRequest("верно A", true), new OptionRequest("верно B", true),
                        new OptionRequest("неверно C", false))))));
    }

    private static LessonRequest lesson(String title) {
        return new LessonRequest(title, "Текст урока", null, null);
    }
}
