package ru.hw.learning.service;

import static ru.hw.learning.service.Guards.require;
import static ru.hw.learning.service.Guards.requireRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hw.learning.entity.Certificate;
import ru.hw.learning.entity.Course;
import ru.hw.learning.entity.Enrollment;
import ru.hw.learning.entity.EnrollmentStatus;
import ru.hw.learning.entity.NotificationType;
import ru.hw.learning.entity.Role;
import ru.hw.learning.entity.User;
import ru.hw.learning.exception.BusinessRuleException;
import ru.hw.learning.exception.ConflictException;
import ru.hw.learning.exception.NotFoundException;
import ru.hw.learning.mapper.CourseMapper;
import ru.hw.learning.mapper.LearningMapper;
import ru.hw.learning.mapper.UserMapper;
import ru.hw.learning.repository.CertificateRepository;
import ru.hw.learning.repository.CourseRepository;
import ru.hw.learning.repository.EnrollmentRepository;
import ru.hw.learning.repository.UserRepository;
import ru.hw.learning.web.dto.CourseDtos.CourseResponse;
import ru.hw.learning.web.dto.EnrollmentDtos.CertificateResponse;
import ru.hw.learning.web.dto.EnrollmentDtos.EnrollmentResponse;
import ru.hw.learning.web.dto.UserDtos.UserResponse;

/** Запись студентов на курсы (M-M через Enrollment), отписка, списки, завершение курса и сертификат. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CertificateRepository certificateRepository;
    private final ProgressCalculator progressCalculator;
    private final NotificationService notificationService;

    /** Записывает студента на курс. Повторная запись запрещена (проверка + уникальный индекс в БД). */
    @Transactional
    public EnrollmentResponse enroll(Long courseId, Long studentId) {
        Course course = require(courseRepository.findById(courseId), "Курс", courseId);
        User student = require(userRepository.findById(studentId), "Пользователь", studentId);
        requireRole(student, "записаться на курс", Role.STUDENT);
        if (enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new ConflictException("Студент id=" + studentId + " уже записан на курс id=" + courseId);
        }
        Enrollment enrollment = enrollmentRepository.save(new Enrollment(student, course));
        log.info("Студент id={} записан на курс id={} (enrollment id={})", studentId, courseId, enrollment.getId());
        return LearningMapper.toResponse(enrollment);
    }

    /** Отписка: запись удаляется. Решения и результаты тестов студента сохраняются как история. */
    @Transactional
    public void unenroll(Long courseId, Long studentId) {
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new NotFoundException("Студент id=" + studentId + " не записан на курс id=" + courseId));
        enrollmentRepository.delete(enrollment);
        log.info("Студент id={} отписан от курса id={}", studentId, courseId);
    }

    public EnrollmentResponse get(Long id) {
        return LearningMapper.toResponse(require(enrollmentRepository.findDetailedById(id), "Запись на курс", id));
    }

    /** Студенты курса. Студент подгружается тем же запросом (@EntityGraph), без N+1. */
    public List<UserResponse> studentsOfCourse(Long courseId) {
        require(courseRepository.findById(courseId), "Курс", courseId);
        return enrollmentRepository.findAllByCourseIdOrderByEnrolledAt(courseId).stream()
                .map(e -> UserMapper.toResponse(e.getStudent()))
                .toList();
    }

    public List<EnrollmentResponse> enrollmentsOfCourse(Long courseId) {
        require(courseRepository.findById(courseId), "Курс", courseId);
        return enrollmentRepository.findAllByCourseIdOrderByEnrolledAt(courseId).stream()
                .map(LearningMapper::toResponse)
                .toList();
    }

    /** Курсы студента (курс, категория и преподаватель — одним запросом). */
    public List<CourseResponse> coursesOfStudent(Long studentId) {
        require(userRepository.findById(studentId), "Пользователь", studentId);
        return enrollmentRepository.findAllByStudentIdOrderByEnrolledAt(studentId).stream()
                .map(e -> CourseMapper.toResponse(e.getCourse()))
                .toList();
    }

    public List<EnrollmentResponse> enrollmentsOfStudent(Long studentId) {
        require(userRepository.findById(studentId), "Пользователь", studentId);
        return enrollmentRepository.findAllByStudentIdOrderByEnrolledAt(studentId).stream()
                .map(LearningMapper::toResponse)
                .toList();
    }

    /** Завершает обучение: статус COMPLETED и сертификат (1-1) с итоговым баллом. */
    @Transactional
    public CertificateResponse complete(Long enrollmentId) {
        Enrollment enrollment = require(enrollmentRepository.findDetailedById(enrollmentId), "Запись на курс", enrollmentId);
        if (enrollment.getStatus() == EnrollmentStatus.COMPLETED) {
            throw new ConflictException("Курс уже завершён, сертификат выдан");
        }
        if (enrollment.getStatus() == EnrollmentStatus.CANCELLED) {
            throw new BusinessRuleException("Запись отменена — завершить курс нельзя");
        }
        Double finalScore = progressCalculator.overallPercent(enrollment.getStudent().getId(), enrollment.getCourse().getId());
        enrollment.setStatus(EnrollmentStatus.COMPLETED);
        enrollment.setCompletedAt(Instant.now());
        enrollment.attachCertificate(new Certificate(UUID.randomUUID().toString(), finalScore));
        enrollmentRepository.flush();
        notificationService.notify(enrollment.getStudent(), NotificationType.COURSE_COMPLETED,
                "Курс «" + enrollment.getCourse().getTitle() + "» завершён, выдан сертификат");
        log.info("Enrollment id={} завершён, итоговый балл {}", enrollmentId, finalScore);
        return LearningMapper.toResponse(enrollment.getCertificate());
    }

    public CertificateResponse certificate(String code) {
        return LearningMapper.toResponse(require(certificateRepository.findByCode(code), "Сертификат", code));
    }

    /** Студент должен быть активно записан на курс (используется при сдаче заданий, тестов, отзывов). */
    public void requireActiveEnrollment(Long studentId, Long courseId) {
        if (!enrollmentRepository.existsByStudentIdAndCourseIdAndStatusNot(studentId, courseId, EnrollmentStatus.CANCELLED)) {
            throw new BusinessRuleException("Студент id=" + studentId + " не записан на курс id=" + courseId);
        }
    }
}
