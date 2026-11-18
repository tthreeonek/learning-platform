package ru.hw.learning.exception;

/** Нарушено правило предметной области: неверная роль, нет записи на курс, балл выше максимума (HTTP 422). */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
