package ru.hw.learning.service;

import static ru.hw.learning.service.Guards.require;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hw.learning.entity.Assignment;
import ru.hw.learning.entity.Course;
import ru.hw.learning.entity.EnrollmentStatus;
import ru.hw.learning.entity.Lesson;
import ru.hw.learning.entity.NotificationType;
import ru.hw.learning.mapper.LearningMapper;
import ru.hw.learning.repository.AssignmentRepository;
import ru.hw.learning.repository.EnrollmentRepository;
import ru.hw.learning.repository.LessonRepository;
import ru.hw.learning.repository.UserRepository;
import ru.hw.learning.web.dto.AssignmentDtos.AssignmentRequest;
import ru.hw.learning.web.dto.AssignmentDtos.AssignmentResponse;

/** Домашние задания к урокам. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentService {

    private static final int DEFAULT_MAX_SCORE = 100;

    private final AssignmentRepository assignmentRepository;
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /** Преподаватель добавляет задание к уроку; записанные на курс студенты получают уведомление. */
    @Transactional
    public AssignmentResponse create(Long lessonId, AssignmentRequest request) {
        Lesson lesson = require(lessonRepository.findWithCourseById(lessonId), "Урок", lessonId);
        Assignment assignment = new Assignment(request.title().trim(), request.description(), request.dueDate(),
                request.maxScore() == null ? DEFAULT_MAX_SCORE : request.maxScore());
        lesson.addAssignment(assignment);
        assignmentRepository.save(assignment);
        notifyStudents(lesson.getModule().getCourse(), assignment);
        log.info("К уроку id={} добавлено задание id={} '{}'", lessonId, assignment.getId(), assignment.getTitle());
        return LearningMapper.toResponse(assignment);
    }

    public AssignmentResponse get(Long id) {
        return LearningMapper.toResponse(require(assignmentRepository.findWithCourseById(id), "Задание", id));
    }

    public List<AssignmentResponse> findByLesson(Long lessonId) {
        require(lessonRepository.findById(lessonId), "Урок", lessonId);
        return assignmentRepository.findAllByLessonId(lessonId).stream().map(LearningMapper::toResponse).toList();
    }

    /** Задания всех курсов, на которые записан студент. */
    public List<AssignmentResponse> findForStudent(Long studentId) {
        require(userRepository.findById(studentId), "Пользователь", studentId);
        return assignmentRepository.findAllForStudent(studentId).stream().map(LearningMapper::toResponse).toList();
    }

    @Transactional
    public AssignmentResponse update(Long id, AssignmentRequest request) {
        Assignment assignment = require(assignmentRepository.findWithCourseById(id), "Задание", id);
        assignment.setTitle(request.title().trim());
        assignment.setDescription(request.description());
        assignment.setDueDate(request.dueDate());
        if (request.maxScore() != null) {
            assignment.setMaxScore(request.maxScore());
        }
        log.info("Обновлено задание id={}", id);
        return LearningMapper.toResponse(assignment);
    }

    /** Удаление задания удаляет и все решения по нему (cascade + orphanRemoval). */
    @Transactional
    public void delete(Long id) {
        Assignment assignment = require(assignmentRepository.findById(id), "Задание", id);
        assignment.getLesson().getAssignments().remove(assignment);
        assignmentRepository.delete(assignment);
        log.info("Удалено задание id={}", id);
    }

    private void notifyStudents(Course course, Assignment assignment) {
        enrollmentRepository.findAllByCourseIdOrderByEnrolledAt(course.getId()).stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .forEach(e -> notificationService.notify(e.getStudent(), NotificationType.NEW_ASSIGNMENT,
                        "Новое задание «" + assignment.getTitle() + "» по курсу «" + course.getTitle() + "»"));
    }
}
