package ru.hw.learning.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ru.hw.learning.seed.DataSeeder;
import ru.hw.learning.support.IntegrationTestBase;

/**
 * Все эндпоинты REST API на демо-данных (DataSeeder): чтение, обновление, удаление.
 * Идентификаторы предсказуемы: перед каждым тестом таблицы очищаются с RESTART IDENTITY.
 * Пользователи: 1 — админ, 2, 3 — преподаватели, 4 — Анна, 5 — Иван, 6 — Мария, 7 — Дмитрий.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.seed.enabled=true")
class RestEndpointsIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private DataSeeder seeder;

    @BeforeEach
    void seed() {
        seeder.run(null);
    }

    @Test
    @DisplayName("Все GET-эндпоинты отвечают 200 на демо-данных")
    void allReadEndpoints() throws Exception {
        List<String> urls = List.of(
                "/api/users", "/api/users?role=TEACHER", "/api/users/4", "/api/users/4/courses", "/api/users/4/enrollments",
                "/api/users/4/assignments", "/api/users/4/submissions", "/api/users/4/quiz-results",
                "/api/users/4/notifications", "/api/users/4/progress",
                "/api/categories", "/api/categories/1", "/api/tags",
                "/api/courses", "/api/courses?tag=Java", "/api/courses/1", "/api/courses/1/structure", "/api/courses/1/modules",
                "/api/courses/1/students", "/api/courses/1/enrollments", "/api/courses/1/reviews", "/api/courses/1/quiz-results",
                "/api/courses/1/stats", "/api/courses/1/leaderboard",
                "/api/modules/1", "/api/modules/1/lessons", "/api/lessons/2", "/api/lessons/2/assignments",
                "/api/assignments/1", "/api/assignments/1/submissions", "/api/submissions/1",
                "/api/quizzes/1", "/api/quizzes/1?withAnswers=true", "/api/quizzes/1/results", "/api/enrollments/1",
                "/v3/api-docs", "/actuator/health", "/");
        for (String url : urls) {
            mvc.perform(get(url)).andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Аналитика: рейтинг курса и прогресс студента считаются по демо-данным")
    void analytics() throws Exception {
        mvc.perform(get("/api/courses/1/leaderboard"))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].studentName").value("Анна Петрова"))
                .andExpect(jsonPath("$[0].overallPercent").value(97.5));
        mvc.perform(get("/api/courses/1/stats"))
                .andExpect(jsonPath("$.students").value(3))
                .andExpect(jsonPath("$.modules").value(3))
                .andExpect(jsonPath("$.averageRating").value(4.5));
        mvc.perform(get("/api/users/4/progress")).andExpect(jsonPath("$.courses", hasSize(2)));
    }

    @Test
    @DisplayName("Сертификат: проверка по коду")
    void certificate() throws Exception {
        String body = mvc.perform(post("/api/enrollments/1/complete"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", notNullValue()))
                .andReturn().getResponse().getContentAsString();
        String code = JsonPath.read(body, "$.code");
        mvc.perform(get("/api/certificates/{code}", code)).andExpect(jsonPath("$.studentName").value("Анна Петрова"));
        mvc.perform(post("/api/enrollments/1/complete")).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT: пользователь, профиль, курс, модуль, урок, задание, уведомление")
    void updateEndpoints() throws Exception {
        putJson("/api/users/4", "{\"name\":\"Анна П.\",\"email\":\"anna@new.local\",\"role\":\"STUDENT\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("anna@new.local"));
        putJson("/api/users/4/profile", "{\"city\":\"Самара\",\"phone\":\"+7 900 1234567\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.city").value("Самара"));
        putJson("/api/courses/1", "{\"title\":\"Hibernate 2.0\",\"categoryId\":2,\"teacherId\":3,\"tags\":[\"ORM\"]}")
                .andExpect(jsonPath("$.teacherName").value("Алексей Морозов"))
                .andExpect(jsonPath("$.tags", hasSize(1)));
        putJson("/api/modules/1", "{\"title\":\"Введение (обновлено)\"}").andExpect(jsonPath("$.title").value("Введение (обновлено)"));
        putJson("/api/lessons/1", "{\"title\":\"ORM: основы\",\"videoUrl\":\"https://v.example.com/1\"}")
                .andExpect(jsonPath("$.videoUrl").value("https://v.example.com/1"));
        putJson("/api/assignments/1", "{\"title\":\"Модель данных\",\"maxScore\":100}").andExpect(status().isOk());
        putJson("/api/categories/3", "{\"name\":\"DevOps и облака\"}").andExpect(status().isOk());
        mvc.perform(put("/api/notifications/1/read")).andExpect(jsonPath("$.read").value(true));
        putJson("/api/users/4/profile", "{\"phone\":\"abc\"}").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST/DELETE: модуль, урок, отписка, отзыв, решение, тест, урок, модуль, пользователь")
    void createAndDeleteEndpoints() throws Exception {
        mvc.perform(post("/api/courses/2/modules").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Новый модуль\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderIndex").value(3));
        mvc.perform(post("/api/modules/1/lessons").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Новый урок\"}"))
                .andExpect(status().isCreated());
        mvc.perform(delete("/api/submissions/3")).andExpect(status().isNoContent());
        mvc.perform(delete("/api/submissions/1")).andExpect(status().isConflict());
        mvc.perform(delete("/api/reviews/1")).andExpect(status().isNoContent());
        mvc.perform(delete("/api/quizzes/2")).andExpect(status().isNoContent());
        mvc.perform(delete("/api/assignments/2")).andExpect(status().isNoContent());
        mvc.perform(delete("/api/lessons/1")).andExpect(status().isNoContent());
        mvc.perform(delete("/api/modules/3")).andExpect(status().isNoContent());
        mvc.perform(delete("/api/courses/1/enroll").param("userId", "6")).andExpect(status().isNoContent());
        mvc.perform(delete("/api/courses/1")).andExpect(status().isConflict());
        mvc.perform(delete("/api/users/6")).andExpect(status().isNoContent());
        mvc.perform(delete("/api/users/2")).andExpect(status().isConflict());
        mvc.perform(delete("/api/tags/1")).andExpect(status().isNoContent());
        mvc.perform(get("/api/users/6")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("405 и 404 для неизвестных путей")
    void unknownRoutes() throws Exception {
        mvc.perform(delete("/api/categories")).andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/api/unknown")).andExpect(status().isNotFound());
    }

    private ResultActions putJson(String url, String json) throws Exception {
        return mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
