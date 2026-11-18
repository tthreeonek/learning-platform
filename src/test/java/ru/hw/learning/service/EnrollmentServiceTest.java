package ru.hw.learning.service;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hw.learning.entity.Course;
import ru.hw.learning.entity.Enrollment;
import ru.hw.learning.entity.EnrollmentStatus;
import ru.hw.learning.entity.Role;
import ru.hw.learning.entity.User;
import ru.hw.learning.exception.BusinessRuleException;
import ru.hw.learning.exception.ConflictException;
import ru.hw.learning.exception.NotFoundException;
import ru.hw.learning.repository.CertificateRepository;
import ru.hw.learning.repository.CourseRepository;
import ru.hw.learning.repository.EnrollmentRepository;
import ru.hw.learning.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CertificateRepository certificateRepository;
    @Mock
    private ProgressCalculator progressCalculator;
    @Mock
    private NotificationService notificationService;
    @InjectMocks
    private EnrollmentService service;

    @Test
    @DisplayName("Повторная запись -> ConflictException, сохранения нет")
    void duplicateEnrollment() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(new Course()));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, Role.STUDENT)));
        when(enrollmentRepository.existsByStudentIdAndCourseId(2L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.enroll(1L, 2L)).isInstanceOf(ConflictException.class);
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Преподаватель не может записаться как студент")
    void teacherCannotEnroll() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(new Course()));
        when(userRepository.findById(3L)).thenReturn(Optional.of(user(3L, Role.TEACHER)));

        assertThatThrownBy(() -> service.enroll(1L, 3L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Несуществующий курс -> NotFoundException")
    void courseNotFound() {
        when(courseRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.enroll(9L, 2L)).isInstanceOf(NotFoundException.class).hasMessageContaining("id=9");
    }

    @Test
    @DisplayName("Отписка без записи -> NotFoundException")
    void unenrollWithoutEnrollment() {
        when(enrollmentRepository.findByStudentIdAndCourseId(2L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.unenroll(1L, 2L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Отменённую запись завершить нельзя")
    void completeCancelled() {
        Enrollment e = new Enrollment(user(2L, Role.STUDENT), new Course());
        e.setStatus(EnrollmentStatus.CANCELLED);
        when(enrollmentRepository.findDetailedById(5L)).thenReturn(Optional.of(e));

        assertThatThrownBy(() -> service.complete(5L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("requireActiveEnrollment пропускает записанного студента")
    void activeEnrollmentPasses() {
        when(enrollmentRepository.existsByStudentIdAndCourseIdAndStatusNot(2L, 1L, EnrollmentStatus.CANCELLED)).thenReturn(true);

        assertThatNoException().isThrownBy(() -> service.requireActiveEnrollment(2L, 1L));
    }

    private static User user(Long id, Role role) {
        User u = new User("U" + id, "u" + id + "@t", role);
        u.setId(id);
        return u;
    }
}
