package ru.hw.learning.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import ru.hw.learning.entity.Enrollment;
import ru.hw.learning.entity.EnrollmentStatus;
import ru.hw.learning.exception.BusinessRuleException;
import ru.hw.learning.exception.ConflictException;
import ru.hw.learning.exception.NotFoundException;
import ru.hw.learning.repository.CourseRepository;
import ru.hw.learning.repository.EnrollmentRepository;
import ru.hw.learning.repository.UserRepository;
import ru.hw.learning.service.EnrollmentService;
import ru.hw.learning.support.IntegrationTestBase;
import ru.hw.learning.web.dto.EnrollmentDtos.CertificateResponse;

/** Запись на курс (M-M через Enrollment): уникальность, отписка, списки, завершение и сертификат. */
class EnrollmentIT extends IntegrationTestBase {

    @Autowired
    private EnrollmentService enrollmentService;
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private UserRepository userRepository;

    private Long courseId;
    private Long studentId;

    @BeforeEach
    void setUp() {
        courseId = data.courseWithStructure().course().id();
        studentId = data.student();
    }

    @Test
    @DisplayName("Запись создаётся и ссылается на правильного студента и курс")
    void enroll() {
        Long id = enrollmentService.enroll(courseId, studentId).id();

        Enrollment e = enrollmentRepository.findDetailedById(id).orElseThrow();
        assertThat(e.getStudent().getId()).isEqualTo(studentId);
        assertThat(e.getCourse().getId()).isEqualTo(courseId);
        assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
    }

    @Test
    @DisplayName("Повторная запись запрещена: сервис (409) и уникальный индекс БД")
    void duplicateEnrollmentRejected() {
        enrollmentService.enroll(courseId, studentId);

        assertThatThrownBy(() -> enrollmentService.enroll(courseId, studentId)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> enrollmentRepository.saveAndFlush(new Enrollment(
                userRepository.getReferenceById(studentId), courseRepository.getReferenceById(courseId))))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(count("enrollments")).isEqualTo(1);
    }

    @Test
    @DisplayName("Записаться может только студент")
    void onlyStudentsCanEnroll() {
        Long teacher = data.teacher();

        assertThatThrownBy(() -> enrollmentService.enroll(courseId, teacher)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Списки: студенты курса и курсы студента")
    void lists() {
        Long second = data.student();
        Long otherCourse = data.courseWithStructure().course().id();
        enrollmentService.enroll(courseId, studentId);
        enrollmentService.enroll(courseId, second);
        enrollmentService.enroll(otherCourse, studentId);

        assertThat(enrollmentService.studentsOfCourse(courseId)).extracting("id").containsExactly(studentId, second);
        assertThat(enrollmentService.coursesOfStudent(studentId)).extracting("id").containsExactly(courseId, otherCourse);
        assertThat(enrollmentService.enrollmentsOfCourse(courseId)).hasSize(2);
    }

    @Test
    @DisplayName("Отписка удаляет запись; повторная отписка -> 404")
    void unenroll() {
        enrollmentService.enroll(courseId, studentId);

        enrollmentService.unenroll(courseId, studentId);

        assertThat(enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)).isFalse();
        assertThatThrownBy(() -> enrollmentService.unenroll(courseId, studentId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Завершение курса: статус COMPLETED, сертификат 1-1, повторно — 409")
    void completeIssuesCertificate() {
        Long enrollmentId = enrollmentService.enroll(courseId, studentId).id();

        CertificateResponse cert = enrollmentService.complete(enrollmentId);

        assertThat(cert.code()).isNotBlank();
        assertThat(enrollmentService.certificate(cert.code()).enrollmentId()).isEqualTo(enrollmentId);
        assertThat(enrollmentService.get(enrollmentId).status()).isEqualTo(EnrollmentStatus.COMPLETED);
        assertThat(count("certificates")).isEqualTo(1);
        assertThatThrownBy(() -> enrollmentService.complete(enrollmentId)).isInstanceOf(ConflictException.class);
    }
}
