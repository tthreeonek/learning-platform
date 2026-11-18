package ru.hw.learning.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.hw.learning.entity.QuizSubmission;

public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, Long> {

    @EntityGraph(attributePaths = {"student", "quiz"})
    List<QuizSubmission> findAllByQuizIdOrderByTakenAtDesc(Long quizId);

    @EntityGraph(attributePaths = {"student", "quiz"})
    List<QuizSubmission> findAllByStudentIdOrderByTakenAtDesc(Long studentId);

    /** Результаты всех тестов курса (для преподавателя). */
    @Query("""
            select qs from QuizSubmission qs join fetch qs.student join fetch qs.quiz q
            where q.module.course.id = :courseId order by qs.takenAt desc
            """)
    List<QuizSubmission> findAllByCourseId(@Param("courseId") Long courseId);

    /** Лучший процент студента по каждому тесту курса. */
    @Query("""
            select max(qs.percent) from QuizSubmission qs
            where qs.student.id = :studentId and qs.quiz.module.course.id = :courseId
            group by qs.quiz.id
            """)
    List<Double> bestPercentsPerQuiz(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

    long countByQuizId(Long quizId);

    long countByQuizModuleCourseId(Long courseId);
}
