package ru.hw.learning.support;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * База интеграционных тестов: настоящий PostgreSQL 17 в Testcontainers.
 * Контейнер один на все тестовые классы (singleton), URL/логин/пароль подставляет @ServiceConnection.
 * Перед каждым тестом все таблицы очищаются (TRUNCATE ... RESTART IDENTITY CASCADE).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestDataFactory.class)
public abstract class IntegrationTestBase {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected TestDataFactory data;

    @BeforeEach
    void cleanDatabase() {
        List<String> tables = jdbc.queryForList(
                "select tablename from pg_tables where schemaname = 'public'", String.class);
        if (!tables.isEmpty()) {
            jdbc.execute("TRUNCATE TABLE " + String.join(", ", tables) + " RESTART IDENTITY CASCADE");
        }
    }

    protected long count(String table) {
        Long value = jdbc.queryForObject("select count(*) from " + table, Long.class);
        return value == null ? 0 : value;
    }
}
