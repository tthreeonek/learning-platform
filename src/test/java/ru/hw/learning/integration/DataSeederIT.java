package ru.hw.learning.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import ru.hw.learning.seed.DataSeeder;
import ru.hw.learning.support.IntegrationTestBase;

/** Предзаполнение демо-данными: проект готов к проверке «из коробки». */
@TestPropertySource(properties = "app.seed.enabled=true")
class DataSeederIT extends IntegrationTestBase {

    @Autowired
    private DataSeeder seeder;

    @Test
    @DisplayName("Сидер наполняет все основные таблицы и не дублирует данные при повторном запуске")
    void seedsAllTablesOnce() {
        seeder.run(null);

        assertThat(count("users")).isEqualTo(7);
        assertThat(count("courses")).isEqualTo(3);
        assertThat(count("modules")).isEqualTo(7);
        assertThat(count("lessons")).isEqualTo(13);
        assertThat(count("assignments")).isEqualTo(5);
        assertThat(count("enrollments")).isEqualTo(6);
        assertThat(count("submissions")).isEqualTo(5);
        assertThat(count("quizzes")).isEqualTo(3);
        assertThat(count("quiz_submissions")).isEqualTo(5);
        assertThat(count("course_reviews")).isEqualTo(3);
        assertThat(count("certificates")).isEqualTo(1);
        assertThat(count("notifications")).isPositive();

        seeder.run(null);
        assertThat(count("courses")).isEqualTo(3);
    }
}
