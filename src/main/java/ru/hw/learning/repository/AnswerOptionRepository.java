package ru.hw.learning.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.hw.learning.entity.AnswerOption;

/** Репозиторий сущности AnswerOption (CRUD из JpaRepository). */
public interface AnswerOptionRepository extends JpaRepository<AnswerOption, Long> {
}
