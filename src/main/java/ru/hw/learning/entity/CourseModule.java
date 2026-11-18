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
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Модуль курса. Класс назван CourseModule, чтобы не конфликтовать с java.lang.Module;
 * таблица — modules.
 */
@Entity
@Table(name = "modules")
@Getter
@Setter
@NoArgsConstructor
public class CourseModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @OneToMany(mappedBy = "module", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<Lesson> lessons = new ArrayList<>();

    /** 1-1 (необязательная): итоговый тест модуля. */
    @OneToOne(mappedBy = "module", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Quiz quiz;

    public CourseModule(String title, String description, int orderIndex) {
        this.title = title;
        this.description = description;
        this.orderIndex = orderIndex;
    }

    public void addLesson(Lesson lesson) {
        lessons.add(lesson);
        lesson.setModule(this);
    }

    public void attachQuiz(Quiz newQuiz) {
        this.quiz = newQuiz;
        newQuiz.setModule(this);
    }
}
