package ru.hw.learning.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.hw.learning.entity.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {

    /** Курс с категорией, преподавателем и тегами — для карточки курса без LazyInitializationException. */
    @EntityGraph(attributePaths = {"category", "teacher", "tags"})
    Optional<Course> findDetailedById(Long id);

    /** Курс с модулями (JOIN FETCH). Уроки догружаются отдельным запросом, см. CourseModuleRepository. */
    @Query("select distinct c from Course c left join fetch c.modules where c.id = :id")
    Optional<Course> findWithModulesById(@Param("id") Long id);

    /** Поиск с необязательными фильтрами. Категория и преподаватель подгружаются сразу (без N+1). */
    @EntityGraph(attributePaths = {"category", "teacher"})
    @Query("""
            select distinct c from Course c
            left join c.tags t
            where (:categoryId is null or c.category.id = :categoryId)
              and (:teacherId is null or c.teacher.id = :teacherId)
              and (:tag is null or lower(t.name) = lower(cast(:tag as string)))
              and (:title is null or lower(c.title) like lower(concat('%', cast(:title as string), '%')))
            order by c.id
            """)
    List<Course> search(@Param("categoryId") Long categoryId,
                        @Param("teacherId") Long teacherId,
                        @Param("tag") String tag,
                        @Param("title") String title);

    boolean existsByTeacherId(Long teacherId);

    boolean existsByCategoryId(Long categoryId);
}
