package ru.hw.learning.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.hw.learning.entity.Enrollment;
import ru.hw.learning.entity.EnrollmentStatus;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    boolean existsByStudentIdAndCourseIdAndStatusNot(Long studentId, Long courseId, EnrollmentStatus status);

    @EntityGraph(attributePaths = {"student", "course"})
    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);

    @EntityGraph(attributePaths = {"student", "course", "certificate"})
    Optional<Enrollment> findDetailedById(Long id);

    /** Студенты курса: student подгружается тем же запросом. */
    @EntityGraph(attributePaths = "student")
    List<Enrollment> findAllByCourseIdOrderByEnrolledAt(Long courseId);

    /** Курсы студента: курс, категория и преподаватель подгружаются тем же запросом. */
    @EntityGraph(attributePaths = {"course", "course.category", "course.teacher"})
    List<Enrollment> findAllByStudentIdOrderByEnrolledAt(Long studentId);

    long countByCourseId(Long courseId);

    long countByCourseIdAndStatus(Long courseId, EnrollmentStatus status);

    boolean existsByCourseId(Long courseId);
}
