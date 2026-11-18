package ru.hw.learning.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hw.learning.entity.Category;
import ru.hw.learning.entity.Course;
import ru.hw.learning.entity.Role;
import ru.hw.learning.entity.User;
import ru.hw.learning.exception.BusinessRuleException;
import ru.hw.learning.exception.ConflictException;
import ru.hw.learning.exception.NotFoundException;
import ru.hw.learning.repository.CategoryRepository;
import ru.hw.learning.repository.CourseModuleRepository;
import ru.hw.learning.repository.CourseRepository;
import ru.hw.learning.repository.EnrollmentRepository;
import ru.hw.learning.repository.UserRepository;
import ru.hw.learning.web.dto.CourseDtos.CourseRequest;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CourseModuleRepository moduleRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private CatalogService catalogService;
    @Mock
    private CourseContentService contentService;
    @InjectMocks
    private CourseService service;

    @Test
    @DisplayName("Преподавателем курса не может быть студент")
    void teacherMustHaveTeacherRole() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(new Category("C", null)));
        when(userRepository.findById(2L)).thenReturn(Optional.of(new User("S", "s@t", Role.STUDENT)));

        assertThatThrownBy(() -> service.create(new CourseRequest("T", null, 1L, 2L, null, null, List.of(), List.of())))
                .isInstanceOf(BusinessRuleException.class);
        verify(courseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Несуществующая категория -> NotFoundException")
    void categoryNotFound() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CourseRequest("T", null, 1L, 2L, null, null, null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Курс с записанными студентами не удаляется")
    void deleteWithEnrollments() {
        when(courseRepository.findById(5L)).thenReturn(Optional.of(new Course()));
        when(enrollmentRepository.existsByCourseId(5L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(5L)).isInstanceOf(ConflictException.class);
        verify(courseRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Курс без студентов удаляется")
    void deleteWithoutEnrollments() {
        Course course = new Course();
        when(courseRepository.findById(5L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByCourseId(5L)).thenReturn(false);

        service.delete(5L);

        verify(courseRepository).delete(course);
    }
}
