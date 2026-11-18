package ru.hw.learning.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import ru.hw.learning.entity.Course;
import ru.hw.learning.entity.CourseModule;
import ru.hw.learning.repository.CourseModuleRepository;
import ru.hw.learning.repository.CourseRepository;
import ru.hw.learning.service.CourseService;
import ru.hw.learning.support.IntegrationTestBase;

/**
 * Демонстрация ленивой загрузки. open-in-view=false, поэтому сессия Hibernate закрывается сразу
 * после вызова репозитория: обращение к ленивой коллекции вне транзакции даёт LazyInitializationException.
 * Дальше — три способа это исправить.
 */
class LazyLoadingIT extends IntegrationTestBase {

    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private CourseModuleRepository moduleRepository;
    @Autowired
    private CourseService courseService;
    @Autowired
    private TransactionTemplate tx;

    private Long courseId;

    @BeforeEach
    void createCourse() {
        courseId = data.courseWithStructure().course().id();
    }

    @Test
    @DisplayName("Проблема: modules вне транзакции -> LazyInitializationException")
    void lazyCollectionOutsideSessionFails() {
        Course course = courseRepository.findById(courseId).orElseThrow();

        assertThat(Hibernate.isInitialized(course.getModules())).isFalse();
        assertThatThrownBy(() -> course.getModules().size())
                .isInstanceOf(LazyInitializationException.class)
                .hasMessageContaining("modules");
    }

    @Test
    @DisplayName("Проблема: @ManyToOne(LAZY) — преподаватель является прокси, его имя вне сессии недоступно")
    void lazyManyToOneProxyOutsideSessionFails() {
        Course course = courseRepository.findById(courseId).orElseThrow();

        assertThat(Hibernate.isInitialized(course.getTeacher())).isFalse();
        assertThatThrownBy(() -> course.getTeacher().getName()).isInstanceOf(LazyInitializationException.class);
    }

    @Test
    @DisplayName("Проблема: уроки модуля (вложенная ленивая коллекция) вне сессии")
    void nestedLazyCollectionFails() {
        Course course = courseRepository.findWithModulesById(courseId).orElseThrow();
        CourseModule first = course.getModules().get(0);

        assertThatThrownBy(() -> first.getLessons().size()).isInstanceOf(LazyInitializationException.class);
    }

    @Test
    @DisplayName("Решение 1: JOIN FETCH в запросе репозитория")
    void joinFetchSolves() {
        Course course = courseRepository.findWithModulesById(courseId).orElseThrow();

        assertThat(Hibernate.isInitialized(course.getModules())).isTrue();
        assertThat(course.getModules()).hasSize(2);
    }

    @Test
    @DisplayName("Решение 2: @EntityGraph загружает преподавателя, категорию и теги одним запросом")
    void entityGraphSolves() {
        Course course = courseRepository.findDetailedById(courseId).orElseThrow();

        assertThat(course.getTeacher().getName()).startsWith("TEACHER");
        assertThat(course.getCategory().getName()).startsWith("Категория");
        assertThat(course.getTags()).hasSize(2);
    }

    @Test
    @DisplayName("Решение 3: обращение к коллекции внутри транзакции (сервисный метод)")
    void transactionSolves() {
        int lessons = tx.execute(s -> courseRepository.findById(courseId).orElseThrow().getModules().stream()
                .mapToInt(m -> m.getLessons().size())
                .sum());

        assertThat(lessons).isEqualTo(4);
    }

    @Test
    @DisplayName("Сервис отдаёт DTO полной структуры: две выборки вместо MultipleBagFetchException")
    void serviceReturnsFullStructure() {
        assertThat(courseService.getStructure(courseId).modules())
                .allSatisfy(m -> assertThat(m.lessons()).hasSize(2));
        assertThat(moduleRepository.findWithLessonsByCourseId(courseId))
                .allSatisfy(m -> assertThat(Hibernate.isInitialized(m.getLessons())).isTrue());
    }
}
