package ru.hw.learning.exception;

/** Конфликт с текущим состоянием данных: дубликат, удаление при наличии зависимостей (HTTP 409). */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
