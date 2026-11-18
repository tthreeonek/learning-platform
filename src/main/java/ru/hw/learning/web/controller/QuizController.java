package ru.hw.learning.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.hw.learning.service.QuizService;
import ru.hw.learning.web.dto.QuizDtos.QuizResponse;
import ru.hw.learning.web.dto.QuizDtos.QuizResultResponse;
import ru.hw.learning.web.dto.QuizDtos.TakeQuizRequest;

@Tag(name = "Quizzes", description = "Тесты: просмотр, прохождение, результаты")
@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @Operation(summary = "Тест с вопросами (withAnswers=true — с правильными ответами, для преподавателя)")
    @GetMapping("/{id}")
    public QuizResponse get(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean withAnswers) {
        return quizService.get(id, withAnswers);
    }

    @Operation(summary = "Пройти тест: answers = {questionId: [optionId, ...]}")
    @PostMapping("/{id}/take")
    public ResponseEntity<QuizResultResponse> take(@PathVariable Long id, @Valid @RequestBody TakeQuizRequest request) {
        QuizResultResponse result = quizService.take(id, request);
        return ResponseEntity.created(URI.create("/api/quizzes/" + id + "/results")).body(result);
    }

    @Operation(summary = "Результаты теста")
    @GetMapping("/{id}/results")
    public List<QuizResultResponse> results(@PathVariable Long id) {
        return quizService.resultsByQuiz(id);
    }

    @Operation(summary = "Удалить тест (вместе с вопросами и попытками)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        quizService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
