package ru.hw.learning.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.hw.learning.entity.Question;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    /** Вопросы теста вместе с вариантами ответов (JOIN FETCH, один запрос). */
    @Query("select distinct q from Question q left join fetch q.options where q.quiz.id = :quizId order by q.orderIndex")
    List<Question> findWithOptionsByQuizId(@Param("quizId") Long quizId);
}
