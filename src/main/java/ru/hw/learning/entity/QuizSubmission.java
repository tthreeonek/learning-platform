package ru.hw.learning.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Результат прохождения теста студентом (попытка). */
@Entity
@Table(name = "quiz_submissions")
@Getter
@Setter
@NoArgsConstructor
public class QuizSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    /** Число верно отвеченных вопросов. */
    @Column(nullable = false)
    private int score;

    @Column(name = "max_score", nullable = false)
    private int maxScore;

    @Column(nullable = false)
    private double percent;

    @Column(nullable = false)
    private boolean passed;

    @Column(name = "taken_at", nullable = false)
    private Instant takenAt = Instant.now();

    /** Выбранные ответы по каждому вопросу. */
    @OneToMany(mappedBy = "quizSubmission", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizAnswer> answers = new ArrayList<>();

    public QuizSubmission(Quiz quiz, User student) {
        this.quiz = quiz;
        this.student = student;
    }

    public void addAnswer(QuizAnswer answer) {
        answers.add(answer);
        answer.setQuizSubmission(this);
    }
}
