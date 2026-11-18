package ru.hw.learning.service;

import static ru.hw.learning.service.Guards.require;
import static ru.hw.learning.service.Guards.requireRole;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hw.learning.entity.Assignment;
import ru.hw.learning.entity.NotificationType;
import ru.hw.learning.entity.Role;
import ru.hw.learning.entity.Submission;
import ru.hw.learning.entity.User;
import ru.hw.learning.exception.BusinessRuleException;
import ru.hw.learning.exception.ConflictException;
import ru.hw.learning.mapper.LearningMapper;
import ru.hw.learning.repository.AssignmentRepository;
import ru.hw.learning.repository.SubmissionRepository;
import ru.hw.learning.repository.UserRepository;
import ru.hw.learning.web.dto.AssignmentDtos.GradeRequest;
import ru.hw.learning.web.dto.AssignmentDtos.SubmissionRequest;
import ru.hw.learning.web.dto.AssignmentDtos.SubmissionResponse;

/** Решения заданий: отправка студентом, проверка преподавателем, просмотр. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final EnrollmentService enrollmentService;
    private final NotificationService notificationService;
    private final Clock clock;

    /**
     * Отправка решения. Проверки: студент записан на курс, срок не истёк,
     * решение по этому заданию ещё не отправлялось (плюс уникальный индекс assignment_id + student_id).
     */
    @Transactional
    public SubmissionResponse submit(Long assignmentId, SubmissionRequest request) {
        Assignment assignment = require(assignmentRepository.findWithCourseById(assignmentId), "Задание", assignmentId);
        User student = require(userRepository.findById(request.studentId()), "Пользователь", request.studentId());
        requireRole(student, "сдавать задания", Role.STUDENT);
        enrollmentService.requireActiveEnrollment(student.getId(), assignment.getLesson().getModule().getCourse().getId());
        if (assignment.getDueDate() != null && LocalDateTime.now(clock).isAfter(assignment.getDueDate())) {
            throw new BusinessRuleException("Срок сдачи задания истёк " + assignment.getDueDate());
        }
        if (submissionRepository.existsByAssignmentIdAndStudentId(assignmentId, student.getId())) {
            throw new ConflictException("Студент id=" + student.getId() + " уже отправил решение задания id=" + assignmentId);
        }
        Submission submission = submissionRepository.save(new Submission(assignment, student, request.content()));
        log.info("Студент id={} отправил решение id={} задания id={}", student.getId(), submission.getId(), assignmentId);
        return LearningMapper.toResponse(submission);
    }

    /** Оценка решения преподавателем. Балл не может превышать maxScore задания. */
    @Transactional
    public SubmissionResponse grade(Long submissionId, GradeRequest request) {
        Submission submission = require(submissionRepository.findDetailedById(submissionId), "Решение", submissionId);
        int max = submission.getAssignment().getMaxScore();
        if (request.score() > max) {
            throw new BusinessRuleException("Оценка " + request.score() + " больше максимальной (" + max + ")");
        }
        submission.grade(request.score(), request.feedback());
        notificationService.notify(submission.getStudent(), NotificationType.SUBMISSION_GRADED,
                "Решение задания «" + submission.getAssignment().getTitle() + "» оценено: " + request.score() + "/" + max);
        log.info("Решение id={} оценено: {}/{}", submissionId, request.score(), max);
        return LearningMapper.toResponse(submission);
    }

    public SubmissionResponse get(Long id) {
        return LearningMapper.toResponse(require(submissionRepository.findDetailedById(id), "Решение", id));
    }

    /** Все решения по заданию — для преподавателя. Студент и задание загружаются тем же запросом. */
    public List<SubmissionResponse> findByAssignment(Long assignmentId) {
        require(assignmentRepository.findById(assignmentId), "Задание", assignmentId);
        return submissionRepository.findAllByAssignmentIdOrderBySubmittedAt(assignmentId).stream()
                .map(LearningMapper::toResponse)
                .toList();
    }

    /** Все решения студента с оценками. */
    public List<SubmissionResponse> findByStudent(Long studentId) {
        require(userRepository.findById(studentId), "Пользователь", studentId);
        return submissionRepository.findAllByStudentIdOrderBySubmittedAtDesc(studentId).stream()
                .map(LearningMapper::toResponse)
                .toList();
    }

    /** Отзыв решения студентом возможен только до проверки. */
    @Transactional
    public void delete(Long id) {
        Submission submission = require(submissionRepository.findDetailedById(id), "Решение", id);
        if (submission.getScore() != null) {
            throw new ConflictException("Решение уже оценено — удалить его нельзя");
        }
        submission.getAssignment().getSubmissions().remove(submission);
        submissionRepository.delete(submission);
        log.info("Удалено решение id={}", id);
    }
}
