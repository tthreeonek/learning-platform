package ru.hw.learning.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import ru.hw.learning.entity.Role;
import ru.hw.learning.entity.User;
import ru.hw.learning.exception.ConflictException;
import ru.hw.learning.repository.CourseRepository;
import ru.hw.learning.repository.UserRepository;
import ru.hw.learning.web.dto.UserDtos.UserRequest;
import ru.hw.learning.web.dto.UserDtos.UserResponse;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CourseRepository courseRepository;
    @InjectMocks
    private UserService service;

    @Test
    @DisplayName("Email нормализуется (trim + lower case) при создании")
    void emailNormalized() {
        when(userRepository.existsByEmailIgnoreCase(" Anna@Mail.RU ")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse created = service.create(new UserRequest(" Анна ", " Anna@Mail.RU ", Role.STUDENT));

        assertThat(created.email()).isEqualTo("anna@mail.ru");
        assertThat(created.name()).isEqualTo("Анна");
    }

    @Test
    @DisplayName("Занятый email -> ConflictException")
    void duplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("a@t")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new UserRequest("A", "a@t", Role.STUDENT))).isInstanceOf(ConflictException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Нельзя сменить роль преподавателя, у которого есть курсы")
    void teacherRoleChangeForbidden() {
        User teacher = new User("T", "t@t", Role.TEACHER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(courseRepository.existsByTeacherId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, new UserRequest("T", "t@t", Role.STUDENT)))
                .isInstanceOf(ConflictException.class);
        assertThat(teacher.getRole()).isEqualTo(Role.TEACHER);
    }
}
