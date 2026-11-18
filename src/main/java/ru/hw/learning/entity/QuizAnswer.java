package ru.hw.learning.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ответ студента на один вопрос в рамках попытки теста.
 * Выбранные варианты — M-M с AnswerOption через таблицу quiz_answer_option.
 */
@Entity
@Table(name = "quiz_answers")
@Getter
@Setter
@NoArgsConstructor
public class QuizAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_submission_id", nullable = false)
    private QuizSubmission quizSubmission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "quiz_answer_option",
            joinColumns = @JoinColumn(name = "quiz_answer_id"),
            inverseJoinColumns = @JoinColumn(name = "answer_option_id"))
    private Set<AnswerOption> selectedOptions = new HashSet<>();

    @Column(nullable = false)
    private boolean correct;

    public QuizAnswer(Question question, Set<AnswerOption> selectedOptions, boolean correct) {
        this.question = question;
        this.selectedOptions = selectedOptions;
        this.correct = correct;
    }
}
