package ru.hw.learning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hw.learning.entity.Assignment;
import ru.hw.learning.entity.Course;
import ru.hw.learning.entity.CourseModule;
import ru.hw.learning.entity.Lesson;
import ru.hw.learning.entity.NotificationType;
import ru.hw.learning.entity.Role;
import ru.hw.learning.entity.Submission;
import ru.hw.learning.entity.SubmissionStatus;
import ru.hw.learning.entity.User;
import ru.hw.learning.exception.BusinessRuleException;
import ru.hw.learning.exception.ConflictException;
import ru.hw.learning.repository.AssignmentRepository;
import ru.hw.learning.repository.SubmissionRepository;
import ru.hw.learning.repository.UserRepository;
import ru.hw.learning.web.dto.AssignmentDtos.GradeRequest;
import ru.hw.learning.web.dto.AssignmentDtos.SubmissionRequest;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-19T10:00:00Z");

    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EnrollmentService enrollmentService;
    @Mock
    private NotificationService notificationService;

    private SubmissionService service;
    private Assignment assignment;
    private User student;

    @BeforeEach
    void setUp() {
        service = new SubmissionService(submissionRepository, assignmentRepository, userRepository, enrollmentService,
                notificationService, Clock.fixed(NOW, ZoneOffset.UTC));
        Course course = new Course();
        course.setId(1L);
        CourseModule module = new CourseModule("M", null, 1);
        course.addModule(module);
        Lesson lesson = new Lesson("L", null, null, 1);
        module.addLesson(lesson);
        assignment = new Assignment("A", null, LocalDateTime.of(2026, 11, 20, 0, 0), 10);
        assignment.setId(7L);
        lesson.addAssignment(assignment);
        student = new User("S", "s@t", Role.STUDENT);
        student.setId(2L);
    }

    @Test
    @DisplayName("Дубликат решения -> ConflictException")
    void duplicate() {
        stubAssignmentAndStudent();
        when(submissionRepository.existsByAssignmentIdAndStudentId(7L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> service.submit(7L, new SubmissionRequest(2L, "x"))).isInstanceOf(ConflictException.class);
        verify(submissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Дедлайн прошёл (часы зафиксированы) -> BusinessRuleException")
    void deadline() {
        assignment.setDueDate(LocalDateTime.of(2026, 11, 18, 23, 59));
        stubAssignmentAndStudent();

        assertThatThrownBy(() -> service.submit(7L, new SubmissionRequest(2L, "x")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Срок");
    }

    @Test
    @DisplayName("Студент не записан на курс -> ошибка из EnrollmentService пробрасывается")
    void notEnrolled() {
        stubAssignmentAndStudent();
        doThrow(new BusinessRuleException("не записан")).when(enrollmentService).requireActiveEnrollment(2L, 1L);

        assertThatThrownBy(() -> service.submit(7L, new SubmissionRequest(2L, "x"))).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Оценка: статус GRADED, уведомление студенту; балл выше максимума запрещён")
    void grade() {
        Submission submission = new Submission(assignment, student, "решение");
        when(submissionRepository.findDetailedById(3L)).thenReturn(Optional.of(submission));

        assertThatThrownBy(() -> service.grade(3L, new GradeRequest(11, null))).isInstanceOf(BusinessRuleException.class);

        service.grade(3L, new GradeRequest(8, "ok"));
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.GRADED);
        assertThat(submission.getScore()).isEqualTo(8);
        verify(notificationService).notify(eq(student), eq(NotificationType.SUBMISSION_GRADED), any());
    }

    private void stubAssignmentAndStudent() {
        when(assignmentRepository.findWithCourseById(7L)).thenReturn(Optional.of(assignment));
        when(userRepository.findById(2L)).thenReturn(Optional.of(student));
    }
}
