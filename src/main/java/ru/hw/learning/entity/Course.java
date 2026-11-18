package ru.hw.learning.entity;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Учебный курс. Все связи ленивые: категория и преподаватель (M-1), модули и отзывы (1-M),
 * теги (M-M через course_tag), записи студентов (M-M через сущность Enrollment).
 */
@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(name = "duration_hours")
    private Integer durationHours;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** Структура курса удаляется вместе с ним: модули -> уроки -> задания -> решения, тест модуля. */
    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<CourseModule> modules = new ArrayList<>();

    /** Записи не удаляются каскадом: курс с записанными студентами удалить нельзя (проверка в сервисе + FK). */
    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private List<Enrollment> enrollments = new ArrayList<>();

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseReview> reviews = new ArrayList<>();

    /** M-M без промежуточной сущности. Каскада нет: удаление курса удаляет только строки course_tag. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "course_tag",
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    public Course(String title, String description, Category category, User teacher) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.teacher = teacher;
    }

    public void addModule(CourseModule module) {
        modules.add(module);
        module.setCourse(this);
    }

    public void removeModule(CourseModule module) {
        modules.remove(module);
        module.setCourse(null);
    }

    public void addTag(Tag tag) {
        tags.add(tag);
        tag.getCourses().add(this);
    }

    public void removeTag(Tag tag) {
        tags.remove(tag);
        tag.getCourses().remove(this);
    }
}
