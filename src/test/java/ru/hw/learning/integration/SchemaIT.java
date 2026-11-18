package ru.hw.learning.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.hw.learning.support.IntegrationTestBase;

/** Схема БД создаётся Hibernate (ddl-auto) по моделям: таблицы, внешние ключи, уникальные ограничения. */
class SchemaIT extends IntegrationTestBase {

    private static final List<String> ENTITY_TABLES = List.of(
            "users", "profiles", "categories", "tags", "courses", "enrollments", "certificates", "modules", "lessons",
            "assignments", "submissions", "quizzes", "questions", "answer_options", "quiz_submissions", "quiz_answers",
            "course_reviews", "notifications");

    @Test
    @DisplayName("Созданы 18 таблиц сущностей и 2 таблицы связей M-M")
    void allTablesCreated() {
        List<String> tables = jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema = 'public'", String.class);
        assertThat(tables).containsAll(ENTITY_TABLES).contains("course_tag", "quiz_answer_option");
        assertThat(ENTITY_TABLES).hasSize(18);
    }

    @Test
    @DisplayName("Внешние ключи соответствуют связям модели")
    void foreignKeysCreated() {
        List<String> fks = jdbc.queryForList("""
                select tc.table_name || '.' || kcu.column_name || '->' || ccu.table_name
                from information_schema.table_constraints tc
                join information_schema.key_column_usage kcu on tc.constraint_name = kcu.constraint_name
                join information_schema.constraint_column_usage ccu on tc.constraint_name = ccu.constraint_name
                where tc.constraint_type = 'FOREIGN KEY' and tc.table_schema = 'public'
                """, String.class);
        assertThat(fks).contains(
                "profiles.user_id->users",
                "courses.category_id->categories",
                "courses.teacher_id->users",
                "modules.course_id->courses",
                "lessons.module_id->modules",
                "assignments.lesson_id->lessons",
                "submissions.assignment_id->assignments",
                "submissions.student_id->users",
                "enrollments.student_id->users",
                "enrollments.course_id->courses",
                "certificates.enrollment_id->enrollments",
                "quizzes.module_id->modules",
                "questions.quiz_id->quizzes",
                "answer_options.question_id->questions",
                "quiz_submissions.quiz_id->quizzes",
                "quiz_answers.quiz_submission_id->quiz_submissions",
                "course_reviews.course_id->courses",
                "notifications.user_id->users",
                "course_tag.course_id->courses",
                "course_tag.tag_id->tags",
                "quiz_answer_option.answer_option_id->answer_options");
    }

    @Test
    @DisplayName("Уникальные ограничения: email, запись на курс, решение, отзыв, 1-1 связи")
    void uniqueConstraintsCreated() {
        List<String> unique = jdbc.queryForList("""
                select tc.table_name || '(' || string_agg(kcu.column_name, ',' order by kcu.ordinal_position) || ')'
                from information_schema.table_constraints tc
                join information_schema.key_column_usage kcu on tc.constraint_name = kcu.constraint_name
                where tc.constraint_type = 'UNIQUE' and tc.table_schema = 'public'
                group by tc.table_name, tc.constraint_name
                """, String.class);
        assertThat(unique).contains(
                "users(email)",
                "profiles(user_id)",
                "quizzes(module_id)",
                "certificates(enrollment_id)",
                "enrollments(student_id,course_id)",
                "submissions(assignment_id,student_id)",
                "course_reviews(course_id,student_id)");
    }
}
