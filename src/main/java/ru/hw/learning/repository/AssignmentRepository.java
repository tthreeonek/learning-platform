package ru.hw.learning.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.hw.learning.entity.Assignment;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findAllByLessonId(Long lessonId);

    /** Задание с уроком, модулем, курсом и преподавателем (для проверок записи и прав). */
    @Query("""
            select a from Assignment a
            join fetch a.lesson l join fetch l.module m join fetch m.course c join fetch c.teacher
            where a.id = :id
            """)
    Optional<Assignment> findWithCourseById(@Param("id") Long id);

    /** Все задания курсов, на которые записан студент (кроме отменённых записей). */
    @Query("""
            select a from Assignment a
            join fetch a.lesson l
            where l.module.course.id in (
                select e.course.id from Enrollment e
                where e.student.id = :studentId and e.status <> ru.hw.learning.entity.EnrollmentStatus.CANCELLED)
            order by a.dueDate
            """)
    List<Assignment> findAllForStudent(@Param("studentId") Long studentId);

    @Query("select count(a) from Assignment a where a.lesson.module.course.id = :courseId")
    long countByCourseId(@Param("courseId") Long courseId);
}
