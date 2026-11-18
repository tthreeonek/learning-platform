package ru.hw.learning.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.hw.learning.entity.QuizAnswer;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {

    @EntityGraph(attributePaths = {"question", "selectedOptions"})
    List<QuizAnswer> findAllByQuizSubmissionId(Long quizSubmissionId);
}
