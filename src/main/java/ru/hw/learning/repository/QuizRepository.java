package ru.hw.learning.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.hw.learning.entity.Quiz;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    Optional<Quiz> findByModuleId(Long moduleId);

    boolean existsByModuleId(Long moduleId);

    /** Тест с модулем, курсом и преподавателем (для проверки записи студента на курс). */
    @Query("select q from Quiz q join fetch q.module m join fetch m.course c join fetch c.teacher where q.id = :id")
    Optional<Quiz> findWithCourseById(@Param("id") Long id);
}
