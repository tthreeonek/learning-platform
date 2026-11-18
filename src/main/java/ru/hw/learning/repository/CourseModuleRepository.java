package ru.hw.learning.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.hw.learning.entity.CourseModule;

public interface CourseModuleRepository extends JpaRepository<CourseModule, Long> {

    List<CourseModule> findAllByCourseIdOrderByOrderIndex(Long courseId);

    /**
     * Модули курса вместе с уроками. Загрузить modules и lessons одним запросом нельзя
     * (две коллекции-bag -> MultipleBagFetchException), поэтому уроки берутся вторым запросом.
     */
    @Query("select distinct m from CourseModule m left join fetch m.lessons where m.course.id = :courseId order by m.orderIndex")
    List<CourseModule> findWithLessonsByCourseId(@Param("courseId") Long courseId);

    /** Модуль с курсом и преподавателем курса — для проверки прав. */
    @Query("select m from CourseModule m join fetch m.course c join fetch c.teacher where m.id = :id")
    Optional<CourseModule> findWithCourseById(@Param("id") Long id);

    long countByCourseId(Long courseId);
}
