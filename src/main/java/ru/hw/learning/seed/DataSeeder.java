package ru.hw.learning.seed;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.hw.learning.entity.QuestionType;
import ru.hw.learning.entity.Role;
import ru.hw.learning.repository.CourseRepository;
import ru.hw.learning.service.AssignmentService;
import ru.hw.learning.service.CatalogService;
import ru.hw.learning.service.CourseService;
import ru.hw.learning.service.EnrollmentService;
import ru.hw.learning.service.QuizService;
import ru.hw.learning.service.ReviewService;
import ru.hw.learning.service.SubmissionService;
import ru.hw.learning.service.UserService;
import ru.hw.learning.web.dto.AssignmentDtos.AssignmentRequest;
import ru.hw.learning.web.dto.AssignmentDtos.GradeRequest;
import ru.hw.learning.web.dto.AssignmentDtos.SubmissionRequest;
import ru.hw.learning.web.dto.CatalogDtos.CategoryRequest;
import ru.hw.learning.web.dto.CourseDtos.CourseRequest;
import ru.hw.learning.web.dto.CourseDtos.CourseStructureResponse;
import ru.hw.learning.web.dto.CourseDtos.LessonRequest;
import ru.hw.learning.web.dto.CourseDtos.ModuleRequest;
import ru.hw.learning.web.dto.EnrollmentDtos.ReviewRequest;
import ru.hw.learning.web.dto.QuizDtos.OptionRequest;
import ru.hw.learning.web.dto.QuizDtos.OptionView;
import ru.hw.learning.web.dto.QuizDtos.QuestionRequest;
import ru.hw.learning.web.dto.QuizDtos.QuestionView;
import ru.hw.learning.web.dto.QuizDtos.QuizRequest;
import ru.hw.learning.web.dto.QuizDtos.QuizResponse;
import ru.hw.learning.web.dto.QuizDtos.TakeQuizRequest;
import ru.hw.learning.web.dto.UserDtos.ProfileRequest;
import ru.hw.learning.web.dto.UserDtos.UserRequest;

/**
 * Предзаполнение демо-данными через сервисный слой (те же проверки, что и в API).
 * Выполняется при старте, если app.seed.enabled=true и в базе ещё нет курсов.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements ApplicationRunner {

    private final CourseRepository courseRepository;
    private final CatalogService catalogService;
    private final UserService userService;
    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;
    private final QuizService quizService;
    private final ReviewService reviewService;

    private final Map<String, Long> ids = new HashMap<>();

    @Override
    public void run(ApplicationArguments args) {
        if (courseRepository.count() > 0) {
            log.info("Демо-данные уже есть — предзаполнение пропущено");
            return;
        }
        seedCatalogAndUsers();
        seedCourses();
        seedEnrollments();
        seedAssignments();
        seedQuizzes();
        seedLearningActivity();
        log.info("Демо-данные загружены: курсов {}, пользователей {}", courseRepository.count(), 7);
    }

    private void seedCatalogAndUsers() {
        ids.put("cat.prog", catalogService.createCategory(new CategoryRequest("Программирование", "Языки и фреймворки")).id());
        ids.put("cat.db", catalogService.createCategory(new CategoryRequest("Базы данных", "SQL, СУБД, ORM")).id());
        catalogService.createCategory(new CategoryRequest("DevOps", "Контейнеры, CI/CD, оркестрация"));

        user("admin", "Администратор Платформы", "admin@learning.local", Role.ADMIN, null);
        user("t.sokolova", "Ирина Соколова", "i.sokolova@learning.local", Role.TEACHER,
                new ProfileRequest("Java-разработчик, 10 лет с Hibernate", null, "+7 900 111-22-33", "Москва"));
        user("t.morozov", "Алексей Морозов", "a.morozov@learning.local", Role.TEACHER,
                new ProfileRequest("DBA PostgreSQL", null, null, "Санкт-Петербург"));
        user("anna", "Анна Петрова", "anna.petrova@learning.local", Role.STUDENT,
                new ProfileRequest("Начинающий backend-разработчик", "https://example.com/anna.png", null, "Казань"));
        user("ivan", "Иван Смирнов", "ivan.smirnov@learning.local", Role.STUDENT, null);
        user("maria", "Мария Кузнецова", "maria.kuznetsova@learning.local", Role.STUDENT, null);
        user("dmitry", "Дмитрий Волков", "dmitry.volkov@learning.local", Role.STUDENT, null);
    }

    private void seedCourses() {
        ids.put("c.hibernate", courseService.create(new CourseRequest("Основы Hibernate",
                "ORM, JPA-аннотации, связи между сущностями, ленивая загрузка и производительность запросов.",
                ids.get("cat.prog"), ids.get("t.sokolova"), 36, LocalDate.now().plusDays(7),
                List.of("Java", "Hibernate", "ORM", "Beginner"), List.of(
                        module("Введение в ORM и JPA", "Что такое ORM", "Сущности и аннотации JPA"),
                        module("Связи и ленивая загрузка", "Связи 1-1, 1-M, M-M", "Lazy loading и LazyInitializationException"),
                        module("Запросы и производительность", "JPQL и JOIN FETCH", "Проблема N+1 и её решение")))).id());
        ids.put("c.springdata", courseService.create(new CourseRequest("Spring Data JPA на практике",
                "Репозитории, производные запросы, @Query, @EntityGraph, транзакции.",
                ids.get("cat.prog"), ids.get("t.sokolova"), 24, LocalDate.now().plusDays(14),
                List.of("Java", "Spring", "Hibernate"), List.of(
                        module("Репозитории", "JpaRepository и CRUD", "Производные запросы"),
                        module("Транзакции", "@Transactional", "Open Session in View")))).id());
        ids.put("c.postgres", courseService.create(new CourseRequest("PostgreSQL для разработчиков",
                "Схема данных, индексы, ограничения целостности, планы запросов.",
                ids.get("cat.db"), ids.get("t.morozov"), 20, LocalDate.now().plusDays(3),
                List.of("SQL", "PostgreSQL"), List.of(
                        module("Схема и ограничения", "Таблицы и типы", "PK, FK, UNIQUE, ON DELETE"),
                        module("Индексы", "B-tree и EXPLAIN")))).id());
    }

    private void seedEnrollments() {
        enroll("c.hibernate", "anna");
        enroll("c.hibernate", "ivan");
        enroll("c.hibernate", "maria");
        enroll("c.springdata", "anna");
        enroll("c.postgres", "ivan");
        ids.put("e.dmitry.pg", enrollmentService.enroll(ids.get("c.postgres"), ids.get("dmitry")).id());
    }

    private void seedAssignments() {
        CourseStructureResponse hib = courseService.getStructure(ids.get("c.hibernate"));
        CourseStructureResponse pg = courseService.getStructure(ids.get("c.postgres"));
        CourseStructureResponse sd = courseService.getStructure(ids.get("c.springdata"));
        ids.put("m.hib1", hib.modules().get(0).id());
        ids.put("m.hib2", hib.modules().get(1).id());
        ids.put("m.pg1", pg.modules().get(0).id());
        ids.put("a.entities", assignment(hib.modules().get(0).lessons().get(1).id(), "Спроектировать сущности платформы",
                "Опишите 15–20 сущностей учебной платформы и связи между ними.", 30, 100));
        ids.put("a.lazy", assignment(hib.modules().get(1).lessons().get(1).id(), "Воспроизвести LazyInitializationException",
                "Получите курс вне транзакции, обратитесь к модулям и исправьте ошибку через JOIN FETCH.", 30, 50));
        assignment(hib.modules().get(2).lessons().get(1).id(), "Найти и исправить N+1",
                "Включите логирование SQL и уберите N+1 через @EntityGraph.", 45, 100);
        assignment(sd.modules().get(0).lessons().get(1).id(), "Написать производные запросы",
                "Реализуйте поиск курсов по категории и тегу.", 30, 100);
        ids.put("a.pg", assignment(pg.modules().get(0).lessons().get(1).id(), "Ограничения целостности",
                "Спроектируйте FK с ON DELETE для модели курса.", 20, 100));
    }

    private void seedQuizzes() {
        ids.put("q.hib1", saveQuiz("m.hib1", new QuizRequest("Тест: ORM и JPA", 15, 60, List.of(
                single("Что делает ORM?", "Отображает объекты на таблицы БД", "Компилирует Java в SQL", "Заменяет СУБД"),
                single("Какая аннотация помечает класс сущности?", "@Entity", "@Table", "@Column"),
                new QuestionRequest("Какие аннотации задают связи?", QuestionType.MULTIPLE_CHOICE, List.of(
                        new OptionRequest("@OneToMany", true), new OptionRequest("@ManyToOne", true),
                        new OptionRequest("@Id", false), new OptionRequest("@ManyToMany", true)))))));
        ids.put("q.hib2", saveQuiz("m.hib2", new QuizRequest("Тест: ленивая загрузка", 10, 60, List.of(
                single("FetchType по умолчанию у @OneToMany?", "LAZY", "EAGER"),
                single("Когда возникает LazyInitializationException?", "При обращении к ленивой связи после закрытия сессии",
                        "При сохранении сущности без id", "При дубликате уникального ключа"),
                single("Что НЕ решает проблему ленивой загрузки?", "Увеличение пула соединений", "JOIN FETCH",
                        "@EntityGraph")))));
        ids.put("q.pg1", saveQuiz("m.pg1", new QuizRequest("Тест: ограничения", 10, 50, List.of(
                single("Что гарантирует UNIQUE?", "Отсутствие повторяющихся значений", "Наличие значения"),
                single("ON DELETE CASCADE означает…", "Удаление дочерних строк вместе с родителем", "Запрет удаления")))));
    }

    private void seedLearningActivity() {
        Long anna = ids.get("anna");
        Long ivan = ids.get("ivan");
        Long maria = ids.get("maria");
        Long dmitry = ids.get("dmitry");
        grade(submit("a.entities", anna, "User, Profile, Course, Module, Lesson ... (18 сущностей), связи описаны."), 95,
                "Отличная модель, все типы связей на месте");
        grade(submit("a.entities", ivan, "Course, Module, Lesson, User, Enrollment."), 70, "Не хватает тестов и отзывов");
        submit("a.lazy", anna, "Исключение воспроизведено, исправлено через findWithModulesById (JOIN FETCH).");
        submit("a.entities", maria, "Черновик модели данных.");
        grade(submit("a.pg", dmitry, "FK modules.course_id ON DELETE CASCADE, enrollments.course_id RESTRICT."), 90, "Верно");

        takeQuiz("q.hib1", anna, 3);
        takeQuiz("q.hib1", ivan, 2);
        takeQuiz("q.hib1", maria, 1);
        takeQuiz("q.hib2", anna, 3);
        takeQuiz("q.pg1", dmitry, 2);

        reviewService.create(ids.get("c.hibernate"), new ReviewRequest(anna, 5, "Наконец-то поняла, откуда LazyInitializationException"));
        reviewService.create(ids.get("c.hibernate"), new ReviewRequest(ivan, 4, "Хорошо, но хочется больше практики"));
        reviewService.create(ids.get("c.postgres"), new ReviewRequest(dmitry, 5, "Коротко и по делу"));
        enrollmentService.complete(ids.get("e.dmitry.pg"));
    }

    private void user(String key, String name, String email, Role role, ProfileRequest profile) {
        Long id = userService.create(new UserRequest(name, email, role)).id();
        if (profile != null) {
            userService.updateProfile(id, profile);
        }
        ids.put(key, id);
    }

    private static ModuleRequest module(String title, String... lessons) {
        List<LessonRequest> lessonRequests = Arrays.stream(lessons)
                .map(l -> new LessonRequest(l, "Конспект урока «" + l + "».", "https://video.example.com/" + l.hashCode(), null))
                .toList();
        return new ModuleRequest(title, "Модуль: " + title, null, lessonRequests);
    }

    private void enroll(String course, String student) {
        enrollmentService.enroll(ids.get(course), ids.get(student));
    }

    private Long assignment(Long lessonId, String title, String description, int dueInDays, int maxScore) {
        return assignmentService.create(lessonId, new AssignmentRequest(title, description,
                LocalDateTime.now().plusDays(dueInDays).withNano(0), maxScore)).id();
    }

    private Long submit(String assignmentKey, Long studentId, String content) {
        return submissionService.submit(ids.get(assignmentKey), new SubmissionRequest(studentId, content)).id();
    }

    private void grade(Long submissionId, int score, String feedback) {
        submissionService.grade(submissionId, new GradeRequest(score, feedback));
    }

    private static QuestionRequest single(String text, String correct, String... wrong) {
        List<OptionRequest> options = new ArrayList<>();
        options.add(new OptionRequest(correct, true));
        for (String w : wrong) {
            options.add(new OptionRequest(w, false));
        }
        return new QuestionRequest(text, QuestionType.SINGLE_CHOICE, options);
    }

    private Long saveQuiz(String moduleKey, QuizRequest request) {
        QuizResponse quiz = quizService.create(ids.get(moduleKey), request);
        ids.put(moduleKey + ".quiz", quiz.id());
        return quiz.id();
    }

    /** Проходит тест, отвечая верно на первые correctCount вопросов и неверно на остальные. */
    private void takeQuiz(String quizKey, Long studentId, int correctCount) {
        QuizResponse quiz = quizService.get(ids.get(quizKey), true);
        Map<Long, List<Long>> answers = new HashMap<>();
        for (int i = 0; i < quiz.questions().size(); i++) {
            QuestionView q = quiz.questions().get(i);
            boolean answerCorrectly = i < correctCount;
            answers.put(q.id(), q.options().stream()
                    .filter(o -> o.correct() == answerCorrectly)
                    .limit(answerCorrectly ? Long.MAX_VALUE : 1)
                    .map(OptionView::id)
                    .toList());
        }
        quizService.take(quiz.id(), new TakeQuizRequest(studentId, answers));
    }
}
