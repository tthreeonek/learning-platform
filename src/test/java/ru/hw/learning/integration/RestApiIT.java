package ru.hw.learning.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ru.hw.learning.support.IntegrationTestBase;

/** Сквозной сценарий через REST API и проверка кодов ответа/формата ошибок. */
@AutoConfigureMockMvc
class RestApiIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("Сценарий: курс -> запись -> задание -> решение -> оценка -> тест -> отзыв -> статистика")
    void fullLearningScenario() throws Exception {
        long category = id(postJson("/api/categories", "{\"name\":\"Программирование\"}").andExpect(status().isCreated()));
        long teacher = id(postJson("/api/users", "{\"name\":\"Ирина\",\"email\":\"irina@t.local\",\"role\":\"TEACHER\"}"));
        long student = id(postJson("/api/users", "{\"name\":\"Анна\",\"email\":\"anna@t.local\",\"role\":\"STUDENT\"}"));

        ResultActions course = postJson("/api/courses", """
                {"title":"Основы Hibernate","categoryId":%d,"teacherId":%d,"tags":["Java"],
                 "modules":[{"title":"Введение","lessons":[{"title":"ORM"},{"title":"JPA"}]}]}
                """.formatted(category, teacher))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/courses/")));
        long courseId = id(course);

        String structure = mvc.perform(get("/api/courses/{id}/structure", courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modules[0].lessons", hasSize(2)))
                .andReturn().getResponse().getContentAsString();
        long moduleId = ((Number) JsonPath.read(structure, "$.modules[0].id")).longValue();
        long lessonId = ((Number) JsonPath.read(structure, "$.modules[0].lessons[0].id")).longValue();

        mvc.perform(post("/api/courses/{id}/enroll", courseId).param("userId", String.valueOf(student)))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/courses/{id}/enroll", courseId).param("userId", String.valueOf(student)))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/courses/{id}/students", courseId)).andExpect(jsonPath("$", hasSize(1)));

        long assignment = id(postJson("/api/lessons/" + lessonId + "/assignments", "{\"title\":\"ДЗ 1\",\"maxScore\":10}"));
        long submission = id(postJson("/api/assignments/" + assignment + "/submit",
                "{\"studentId\":%d,\"content\":\"решение\"}".formatted(student)).andExpect(status().isCreated()));
        postJson("/api/assignments/" + assignment + "/submit", "{\"studentId\":%d,\"content\":\"ещё\"}".formatted(student))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/submissions/{id}/grade", submission).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":9,\"feedback\":\"Отлично\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("GRADED"));

        String quiz = postJson("/api/modules/" + moduleId + "/quiz", """
                {"title":"Тест","questions":[{"text":"ORM?","type":"SINGLE_CHOICE",
                 "options":[{"text":"да","correct":true},{"text":"нет","correct":false}]}]}
                """).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long quizId = ((Number) JsonPath.read(quiz, "$.id")).longValue();
        long questionId = ((Number) JsonPath.read(quiz, "$.questions[0].id")).longValue();
        long rightOption = ((Number) JsonPath.read(quiz, "$.questions[0].options[0].id")).longValue();
        postJson("/api/quizzes/" + quizId + "/take", "{\"studentId\":%d,\"answers\":{\"%d\":[%d]}}"
                .formatted(student, questionId, rightOption))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.percent").value(100.0));

        postJson("/api/courses/" + courseId + "/reviews", "{\"studentId\":%d,\"rating\":5}".formatted(student))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/courses/{id}/stats", courseId))
                .andExpect(jsonPath("$.students").value(1))
                .andExpect(jsonPath("$.submissions").value(1))
                .andExpect(jsonPath("$.averageRating").value(5.0));
        mvc.perform(get("/api/users/{id}/progress", student))
                .andExpect(jsonPath("$.courses[0].overallPercent").value(95.0));
        mvc.perform(get("/api/users/{id}/notifications", student)).andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    @DisplayName("404: несуществующий курс, понятное сообщение в едином формате")
    void notFound() throws Exception {
        mvc.perform(get("/api/courses/{id}", 12345))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Курс с id=12345 не найден(а)"))
                .andExpect(jsonPath("$.path").value("/api/courses/12345"));
    }

    @Test
    @DisplayName("400: ошибки валидации полей возвращаются списком")
    void validationErrors() throws Exception {
        postJson("/api/users", "{\"name\":\"\",\"email\":\"not-an-email\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ошибка валидации запроса"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(3)));
    }

    @Test
    @DisplayName("400: неверный JSON, неверный тип параметра, нет обязательного параметра")
    void badRequests() throws Exception {
        postJson("/api/categories", "{broken").andExpect(status().isBadRequest());
        mvc.perform(get("/api/courses/abc")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/courses/1/enroll")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/users").param("role", "GUEST")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("422: нарушение бизнес-правила (студент назначен преподавателем курса)")
    void businessRule() throws Exception {
        long category = id(postJson("/api/categories", "{\"name\":\"Cat\"}"));
        long student = id(postJson("/api/users", "{\"name\":\"S\",\"email\":\"s@t.local\",\"role\":\"STUDENT\"}"));

        postJson("/api/courses", "{\"title\":\"X\",\"categoryId\":%d,\"teacherId\":%d}".formatted(category, student))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", startsWith("Пользователь S (STUDENT) не может вести курс")));
    }

    @Test
    @DisplayName("CRUD справочников и удаление: 204, затем 404")
    void crudAndDelete() throws Exception {
        long category = id(postJson("/api/categories", "{\"name\":\"Old\"}"));
        mvc.perform(put("/api/categories/{id}", category).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"New\"}"))
                .andExpect(jsonPath("$.name").value("New"));
        mvc.perform(delete("/api/categories/{id}", category)).andExpect(status().isNoContent());
        mvc.perform(get("/api/categories/{id}", category)).andExpect(status().isNotFound());
        postJson("/api/tags", "{\"name\":\"Java\"}").andExpect(status().isCreated());
        postJson("/api/tags", "{\"name\":\"java\"}").andExpect(status().isConflict());
    }

    private ResultActions postJson(String url, String json) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static long id(ResultActions result) throws Exception {
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }
}
