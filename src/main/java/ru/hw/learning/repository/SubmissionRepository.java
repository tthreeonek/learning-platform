package ru.hw.learning.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.hw.learning.entity.Submission;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    boolean existsByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    @EntityGraph(attributePaths = {"student", "assignment"})
    List<Submission> findAllByAssignmentIdOrderBySubmittedAt(Long assignmentId);

    @EntityGraph(attributePaths = {"student", "assignment"})
    List<Submission> findAllByStudentIdOrderBySubmittedAtDesc(Long studentId);

    @EntityGraph(attributePaths = {"student", "assignment"})
    Optional<Submission> findDetailedById(Long id);

    /** Средний балл студента по проверенным работам курса, в % от максимума. */
    @Query("""
            select avg(s.score * 100.0 / a.maxScore) from Submission s join s.assignment a
            where s.student.id = :studentId and a.lesson.module.course.id = :courseId and s.score is not null
            """)
    Double averagePercent(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

    @Query("select count(s) from Submission s where s.student.id = :studentId and s.assignment.lesson.module.course.id = :courseId")
    long countByStudentAndCourse(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

    @Query("select count(s) from Submission s where s.assignment.lesson.module.course.id = :courseId")
    long countByCourseId(@Param("courseId") Long courseId);
}
