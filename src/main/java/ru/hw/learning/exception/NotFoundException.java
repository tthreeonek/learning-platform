package ru.hw.learning.exception;

/** Сущность не найдена (HTTP 404). */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException of(String entity, Object id) {
        return new NotFoundException(entity + " с id=" + id + " не найден(а)");
    }
}
