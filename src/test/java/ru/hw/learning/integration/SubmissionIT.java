package ru.hw.learning.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.hw.learning.entity.NotificationType;
import ru.hw.learning.entity.SubmissionStatus;
import ru.hw.learning.exception.BusinessRuleException;
import ru.hw.learning.exception.ConflictException;
import ru.hw.learning.service.AssignmentService;
import ru.hw.learning.service.NotificationService;
import ru.hw.learning.service.SubmissionService;
import ru.hw.learning.support.IntegrationTestBase;
import ru.hw.learning.web.dto.AssignmentDtos.AssignmentRequest;
import ru.hw.learning.web.dto.AssignmentDtos.GradeRequest;
import ru.hw.learning.web.dto.AssignmentDtos.SubmissionRequest;
import ru.hw.learning.web.dto.AssignmentDtos.SubmissionResponse;
import ru.hw.learning.web.dto.CourseDtos.CourseStructureResponse;

/** Полный цикл заданий: преподаватель добавляет, студент отправляет, преподаватель оценивает. */
class SubmissionIT extends IntegrationTestBase {

    @Autowired
    private AssignmentService assignmentService;
    @Autowired
    private SubmissionService submissionService;
    @Autowired
    private NotificationService notificationService;

    private Long lessonId;
    private Long student;
    private Long assignment;

    @BeforeEach
    void setUp() {
        CourseStructureResponse course = data.courseWithStructure();
        lessonId = course.modules().get(0).lessons().get(0).id();
        student = data.student();
        data.enroll(course.course().id(), student);
        assignment = data.assignment(lessonId, 50, LocalDateTime.now().plusDays(3));
    }

    @Test
    @DisplayName("Новое задание: записанный студент получает уведомление")
    void newAssignmentNotifiesStudents() {
        assertThat(notificationService.findForUser(student))
                .extracting("type").containsExactly(NotificationType.NEW_ASSIGNMENT);
        assertThat(assignmentService.findForStudent(student)).extracting("id").containsExactly(assignment);
    }

    @Test
    @DisplayName("Отправка, оценка и чтение решения")
    void submitAndGrade() {
        Long submission = submissionService.submit(assignment, new SubmissionRequest(student, "моё решение")).id();

        SubmissionResponse graded = submissionService.grade(submission, new GradeRequest(45, "Хорошо"));

        assertThat(graded.status()).isEqualTo(SubmissionStatus.GRADED);
        assertThat(submissionService.get(submission).score()).isEqualTo(45);
        assertThat(submissionService.findByAssignment(assignment)).hasSize(1);
        assertThat(submissionService.findByStudent(student)).extracting("feedback").containsExactly("Хорошо");
    }

    @Test
    @DisplayName("Повторная отправка решения запрещена")
    void secondSubmissionRejected() {
        submissionService.submit(assignment, new SubmissionRequest(student, "v1"));

        assertThatThrownBy(() -> submissionService.submit(assignment, new SubmissionRequest(student, "v2")))
                .isInstanceOf(ConflictException.class);
        assertThat(count("submissions")).isEqualTo(1);
    }

    @Test
    @DisplayName("Студент, не записанный на курс, не может сдать задание")
    void notEnrolledRejected() {
        Long stranger = data.student();

        assertThatThrownBy(() -> submissionService.submit(assignment, new SubmissionRequest(stranger, "x")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("не записан");
    }

    @Test
    @DisplayName("Оценка выше maxScore отклоняется")
    void gradeAboveMaxRejected() {
        Long submission = submissionService.submit(assignment, new SubmissionRequest(student, "x")).id();

        assertThatThrownBy(() -> submissionService.grade(submission, new GradeRequest(51, null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("После дедлайна сдать нельзя")
    void deadlinePassed() {
        Long late = data.assignment(lessonId, 100, LocalDateTime.now().minusDays(1));

        assertThatThrownBy(() -> submissionService.submit(late, new SubmissionRequest(student, "x")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Срок");
    }

    @Test
    @DisplayName("Оценённое решение отозвать нельзя; удаление задания удаляет решения")
    void deleteRules() {
        Long submission = submissionService.submit(assignment, new SubmissionRequest(student, "x")).id();
        submissionService.grade(submission, new GradeRequest(10, null));

        assertThatThrownBy(() -> submissionService.delete(submission)).isInstanceOf(ConflictException.class);

        assignmentService.delete(assignment);
        assertThat(count("assignments")).isZero();
        assertThat(count("submissions")).isZero();
    }

    @Test
    @DisplayName("Обновление задания")
    void updateAssignment() {
        assignmentService.update(assignment, new AssignmentRequest("Новое", "описание", null, 80));

        assertThat(assignmentService.get(assignment).maxScore()).isEqualTo(80);
        assertThat(assignmentService.findByLesson(lessonId)).extracting("title").containsExactly("Новое");
    }
}
