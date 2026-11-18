package ru.hw.learning.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

/** Юнит-тест обработчика ошибок: формат ответа и коды статусов. */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");

    @Test
    @DisplayName("Нарушение ограничения БД -> 409 без технических подробностей")
    void dataIntegrity() {
        ResponseEntity<ApiError> response = handler.dataIntegrity(
                new DataIntegrityViolationException("x", new SQLException("duplicate key value violates unique constraint")), request);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().message()).doesNotContain("duplicate key");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("Непредвиденная ошибка -> 500 с общим сообщением")
    void unexpected() {
        ResponseEntity<ApiError> response = handler.unexpected(new IllegalStateException("секрет"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().message()).isEqualTo("Внутренняя ошибка сервера");
        assertThat(response.getBody().fieldErrors()).isNull();
    }

    @Test
    @DisplayName("Бизнес-правило -> 422, конфликт -> 409, не найдено -> 404")
    void domainExceptions() {
        assertThat(handler.businessRule(new BusinessRuleException("r"), request).getStatusCode().value()).isEqualTo(422);
        assertThat(handler.conflict(new ConflictException("c"), request).getStatusCode().value()).isEqualTo(409);
        assertThat(handler.notFound(NotFoundException.of("Курс", 1), request).getBody().message())
                .isEqualTo("Курс с id=1 не найден(а)");
    }
}
