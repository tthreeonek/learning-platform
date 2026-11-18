package ru.hw.learning.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import ru.hw.learning.entity.Category;
import ru.hw.learning.entity.Course;
import ru.hw.learning.entity.CourseModule;
import ru.hw.learning.entity.Lesson;
import ru.hw.learning.entity.Role;
import ru.hw.learning.entity.Tag;
import ru.hw.learning.entity.User;
import ru.hw.learning.exception.ConflictException;
import ru.hw.learning.repository.CategoryRepository;
import ru.hw.learning.repository.CourseModuleRepository;
import ru.hw.learning.repository.CourseRepository;
import ru.hw.learning.repository.LessonRepository;
import ru.hw.learning.repository.TagRepository;
import ru.hw.learning.repository.UserRepository;
import ru.hw.learning.service.CatalogService;
import ru.hw.learning.service.CourseContentService;
import ru.hw.learning.service.CourseService;
import ru.hw.learning.support.IntegrationTestBase;
import ru.hw.learning.web.dto.CourseDtos.CourseResponse;
import ru.hw.learning.web.dto.CourseDtos.CourseStructureResponse;
import ru.hw.learning.web.dto.CourseDtos.CourseUpdateRequest;
import ru.hw.learning.web.dto.CourseDtos.LessonRequest;
import ru.hw.learning.web.dto.CourseDtos.ModuleRequest;

/** CRUD курсов и структуры: каскадное сохранение, чтение, обновление, удаление с зависимостями. */
class CourseCrudIT extends IntegrationTestBase {

    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private CourseModuleRepository moduleRepository;
    @Autowired
    private LessonRepository lessonRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private CourseService courseService;
    @Autowired
    private CourseContentService contentService;
    @Autowired
    private CatalogService catalogService;
    @Autowired
    private TransactionTemplate tx;

    @Test
    @DisplayName("Create: сохранение курса через репозиторий каскадно сохраняет модули и уроки")
    void cascadePersistThroughRepository() {
        Category category = categoryRepository.save(new Category("Программирование", null));
        User teacher = userRepository.save(new User("Преподаватель", "t@test.local", Role.TEACHER));
        Course course = new Course("Основы Hibernate", "ORM", category, teacher);
        CourseModule module = new CourseModule("Введение", null, 1);
        module.addLesson(new Lesson("Что такое ORM", "текст", null, 1));
        module.addLesson(new Lesson("Сущности", "текст", null, 2));
        course.addModule(module);
        course.addTag(tagRepository.save(new Tag("Java")));

        Course saved = courseRepository.save(course);

        assertThat(saved.getId()).isNotNull();
        assertThat(module.getId()).as("модуль сохранён каскадом").isNotNull();
        assertThat(count("modules")).isEqualTo(1);
        assertThat(count("lessons")).isEqualTo(2);
        assertThat(count("course_tag")).isEqualTo(1);
    }

    @Test
    @DisplayName("Read: структура курса читается целиком (модули по порядку, уроки, теги)")
    void readStructure() {
        CourseStructureResponse structure = data.courseWithStructure();

        CourseStructureResponse loaded = courseService.getStructure(structure.course().id());

        assertThat(loaded.modules()).hasSize(2);
        assertThat(loaded.modules()).extracting("title").containsExactly("Модуль 1", "Модуль 2");
        assertThat(loaded.modules().get(0).lessons()).extracting("title").containsExactly("Урок 1.1", "Урок 1.2");
        assertThat(loaded.course().tags()).containsExactly("Hibernate", "Java");
        assertThat(moduleRepository.countByCourseId(structure.course().id())).isEqualTo(2);
    }

    @Test
    @DisplayName("Read: поиск курсов по категории, тегу и части названия")
    void searchCourses() {
        CourseStructureResponse c1 = data.courseWithStructure();
        data.courseWithStructure();

        assertThat(courseService.search(c1.course().categoryId(), null, null, null)).hasSize(1);
        assertThat(courseService.search(null, null, "java", null)).hasSize(2);
        assertThat(courseService.search(null, null, "python", null)).isEmpty();
        assertThat(courseService.search(null, c1.course().teacherId(), null, "hiber")).hasSize(1);
    }

    @Test
    @DisplayName("Update: изменение описания, замена преподавателя и тегов сохраняются в БД")
    void updateCourse() {
        CourseResponse course = data.courseWithStructure().course();
        Long newTeacher = data.teacher();

        courseService.update(course.id(), new CourseUpdateRequest("Hibernate PRO", "Новое описание",
                course.categoryId(), newTeacher, 48, null, List.of("ORM")));

        CourseResponse reloaded = courseService.get(course.id());
        assertThat(reloaded.title()).isEqualTo("Hibernate PRO");
        assertThat(reloaded.description()).isEqualTo("Новое описание");
        assertThat(reloaded.teacherId()).isEqualTo(newTeacher);
        assertThat(reloaded.tags()).containsExactly("ORM");
        assertThat(tagRepository.count()).as("старые теги остаются в справочнике").isEqualTo(3);
    }

    @Test
    @DisplayName("Update: добавление модуля и урока в существующий курс")
    void addModuleAndLesson() {
        Long courseId = data.courseWithStructure().course().id();

        Long moduleId = courseService.addModule(courseId, new ModuleRequest("Модуль 3", null, null, List.of())).id();
        contentService.addLesson(moduleId, new LessonRequest("Урок 3.1", "текст", null, null));

        CourseStructureResponse structure = courseService.getStructure(courseId);
        assertThat(structure.modules()).hasSize(3);
        assertThat(structure.modules().get(2).orderIndex()).isEqualTo(3);
        assertThat(structure.modules().get(2).lessons()).extracting("title").containsExactly("Урок 3.1");
    }

    @Test
    @DisplayName("Delete: удаление курса без студентов каскадно удаляет модули, уроки, задания; теги остаются")
    void deleteCourseCascades() {
        CourseStructureResponse structure = data.courseWithStructure();
        data.assignment(structure.modules().get(0).lessons().get(0).id());

        courseService.delete(structure.course().id());

        assertThat(courseRepository.count()).isZero();
        assertThat(count("modules")).isZero();
        assertThat(count("lessons")).isZero();
        assertThat(count("assignments")).isZero();
        assertThat(count("course_tag")).isZero();
        assertThat(tagRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Delete: курс с записанными студентами удалить нельзя (409), данные не тронуты")
    void deleteCourseWithStudentsForbidden() {
        Long courseId = data.courseWithStructure().course().id();
        data.enroll(courseId, data.student());

        assertThatThrownBy(() -> courseService.delete(courseId)).isInstanceOf(ConflictException.class);
        assertThat(courseRepository.existsById(courseId)).isTrue();
        assertThat(count("lessons")).isEqualTo(4);
    }

    @Test
    @DisplayName("Delete: orphanRemoval — модуль, убранный из коллекции курса, удаляется вместе с уроками")
    void orphanRemoval() {
        Long courseId = data.courseWithStructure().course().id();

        tx.executeWithoutResult(s -> {
            Course course = courseRepository.findWithModulesById(courseId).orElseThrow();
            course.removeModule(course.getModules().get(0));
        });

        assertThat(moduleRepository.countByCourseId(courseId)).isEqualTo(1);
        assertThat(lessonRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Delete: удаление тега убирает строки course_tag, курс остаётся")
    void deleteTagKeepsCourse() {
        CourseResponse course = data.courseWithStructure().course();
        Long javaTag = tagRepository.findByNameIgnoreCase("Java").orElseThrow().getId();

        catalogService.deleteTag(javaTag);

        assertThat(courseService.get(course.id()).tags()).containsExactly("Hibernate");
        assertThat(courseRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Delete: категорию с курсами удалить нельзя")
    void deleteCategoryWithCoursesForbidden() {
        CourseResponse course = data.courseWithStructure().course();

        assertThatThrownBy(() -> catalogService.deleteCategory(course.categoryId())).isInstanceOf(ConflictException.class);
    }
}
