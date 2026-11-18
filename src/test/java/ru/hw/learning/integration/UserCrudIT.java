package ru.hw.learning.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import ru.hw.learning.entity.Role;
import ru.hw.learning.entity.User;
import ru.hw.learning.exception.ConflictException;
import ru.hw.learning.exception.NotFoundException;
import ru.hw.learning.repository.UserRepository;
import ru.hw.learning.service.SubmissionService;
import ru.hw.learning.service.UserService;
import ru.hw.learning.support.IntegrationTestBase;
import ru.hw.learning.web.dto.AssignmentDtos.SubmissionRequest;
import ru.hw.learning.web.dto.CourseDtos.CourseStructureResponse;
import ru.hw.learning.web.dto.UserDtos.ProfileRequest;
import ru.hw.learning.web.dto.UserDtos.UserDetailsResponse;
import ru.hw.learning.web.dto.UserDtos.UserRequest;
import ru.hw.learning.web.dto.UserDtos.UserResponse;

/** CRUD пользователей и профиля (1-1), уникальность email, каскадное удаление. */
class UserCrudIT extends IntegrationTestBase {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SubmissionService submissionService;

    @Test
    @DisplayName("Create + Read: пользователь и профиль (1-1) сохраняются и читаются вместе")
    void createUserWithProfile() {
        UserResponse created = userService.create(new UserRequest("Анна", "Anna@Test.local", Role.STUDENT));
        userService.updateProfile(created.id(), new ProfileRequest("bio", "https://a.png", "+7 900 000-00-00", "Казань"));

        UserDetailsResponse loaded = userService.get(created.id());

        assertThat(loaded.email()).isEqualTo("anna@test.local");
        assertThat(loaded.profile().city()).isEqualTo("Казань");
        assertThat(count("profiles")).isEqualTo(1);
    }

    @Test
    @DisplayName("Update: профиль обновляется, а не создаётся второй")
    void updateProfile() {
        Long id = data.student();
        userService.updateProfile(id, new ProfileRequest("v1", null, null, null));
        userService.updateProfile(id, new ProfileRequest("v2", null, null, "Омск"));

        assertThat(userService.get(id).profile().bio()).isEqualTo("v2");
        assertThat(count("profiles")).isEqualTo(1);
    }

    @Test
    @DisplayName("Уникальность email: проверка сервиса (409) и ограничение БД")
    void emailUnique() {
        userService.create(new UserRequest("A", "dup@test.local", Role.STUDENT));

        assertThatThrownBy(() -> userService.create(new UserRequest("B", "DUP@test.local", Role.STUDENT)))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> userRepository.saveAndFlush(new User("C", "dup@test.local", Role.TEACHER)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Delete: удаление студента каскадно удаляет профиль, записи, решения и уведомления")
    void deleteStudentCascades() {
        CourseStructureResponse course = data.courseWithStructure();
        Long student = data.student();
        userService.updateProfile(student, new ProfileRequest("bio", null, null, null));
        data.enroll(course.course().id(), student);
        Long assignment = data.assignment(course.modules().get(0).lessons().get(0).id());
        submissionService.submit(assignment, new SubmissionRequest(student, "решение"));

        userService.delete(student);

        assertThat(userRepository.existsById(student)).isFalse();
        assertThat(count("profiles")).isZero();
        assertThat(count("enrollments")).isZero();
        assertThat(count("submissions")).isZero();
        assertThat(count("notifications")).isZero();
        assertThat(count("assignments")).as("задание курса остаётся").isEqualTo(1);
    }

    @Test
    @DisplayName("Delete: преподавателя с курсами удалить нельзя")
    void deleteTeacherWithCoursesForbidden() {
        Long teacher = data.courseWithStructure().course().teacherId();

        assertThatThrownBy(() -> userService.delete(teacher)).isInstanceOf(ConflictException.class);
        assertThat(userRepository.existsById(teacher)).isTrue();
    }

    @Test
    @DisplayName("Read: несуществующий пользователь -> NotFoundException")
    void notFound() {
        assertThatThrownBy(() -> userService.get(999L)).isInstanceOf(NotFoundException.class);
    }
}
