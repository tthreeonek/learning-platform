package ru.hw.learning.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Пользователь: студент, преподаватель или администратор.
 * Таблица называется users, потому что user — зарезервированное слово PostgreSQL.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** 1-1: профиль хранит редко используемые поля. Удаляется вместе с пользователем. */
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Profile profile;

    /** 1-M: курсы, которые ведёт преподаватель (без каскада: курс нельзя потерять вместе с преподавателем). */
    @OneToMany(mappedBy = "teacher", fetch = FetchType.LAZY)
    private List<Course> coursesTaught = new ArrayList<>();

    /** Записи студента на курсы (M-M User-Course через Enrollment). */
    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<Enrollment> enrollments = new ArrayList<>();

    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<Submission> submissions = new ArrayList<>();

    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<QuizSubmission> quizSubmissions = new ArrayList<>();

    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<CourseReview> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<Notification> notifications = new ArrayList<>();

    public User(String name, String email, Role role) {
        this.name = name;
        this.email = email;
        this.role = role;
    }

    /** Устанавливает профиль и синхронизирует обе стороны связи 1-1. */
    public void attachProfile(Profile newProfile) {
        this.profile = newProfile;
        if (newProfile != null) {
            newProfile.setUser(this);
        }
    }

    public boolean hasRole(Role expected) {
        return role == expected;
    }
}
