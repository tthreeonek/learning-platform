package ru.hw.learning.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Тест модуля (1-1 с CourseModule, владелец связи — quizzes.module_id). */
@Entity
@Table(name = "quizzes")
@Getter
@Setter
@NoArgsConstructor
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false, unique = true)
    private CourseModule module;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;

    /** Порог прохождения теста, % правильных ответов. */
    @Column(name = "passing_score", nullable = false)
    private int passingScore = 60;

    @OneToMany(mappedBy = "quiz", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<Question> questions = new ArrayList<>();

    /**
     * Попытки прохождения теста. Поле названо attempts намеренно: Hibernate обходит атрибуты
     * в алфавитном порядке, и при каскадном удалении попытки (и ответы, ссылающиеся на вопросы)
     * должны удаляться раньше вопросов, иначе нарушится FK quiz_answers.question_id.
     */
    @OneToMany(mappedBy = "quiz", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizSubmission> attempts = new ArrayList<>();

    public Quiz(String title, Integer timeLimitMinutes, int passingScore) {
        this.title = title;
        this.timeLimitMinutes = timeLimitMinutes;
        this.passingScore = passingScore;
    }

    public void addQuestion(Question question) {
        questions.add(question);
        question.setQuiz(this);
    }
}
