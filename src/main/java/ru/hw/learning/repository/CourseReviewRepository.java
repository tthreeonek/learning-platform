package ru.hw.learning.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.hw.learning.entity.CourseReview;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {

    boolean existsByCourseIdAndStudentId(Long courseId, Long studentId);

    @EntityGraph(attributePaths = "student")
    List<CourseReview> findAllByCourseIdOrderByCreatedAtDesc(Long courseId);

    long countByCourseId(Long courseId);

    @Query("select avg(r.rating) from CourseReview r where r.course.id = :courseId")
    Double averageRating(@Param("courseId") Long courseId);
}
