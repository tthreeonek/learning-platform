package ru.hw.learning.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.hw.learning.entity.Lesson;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findAllByModuleIdOrderByOrderIndex(Long moduleId);

    /** Урок с модулем, курсом и преподавателем — для проверки прав преподавателя. */
    @Query("select l from Lesson l join fetch l.module m join fetch m.course c join fetch c.teacher where l.id = :id")
    Optional<Lesson> findWithCourseById(@Param("id") Long id);
}
