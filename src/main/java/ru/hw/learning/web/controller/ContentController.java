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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.hw.learning.service.AssignmentService;
import ru.hw.learning.service.CourseContentService;
import ru.hw.learning.service.QuizService;
import ru.hw.learning.web.dto.AssignmentDtos.AssignmentRequest;
import ru.hw.learning.web.dto.AssignmentDtos.AssignmentResponse;
import ru.hw.learning.web.dto.CourseDtos.LessonRequest;
import ru.hw.learning.web.dto.CourseDtos.LessonResponse;
import ru.hw.learning.web.dto.CourseDtos.ModuleRequest;
import ru.hw.learning.web.dto.CourseDtos.ModuleResponse;
import ru.hw.learning.web.dto.QuizDtos.QuizRequest;
import ru.hw.learning.web.dto.QuizDtos.QuizResponse;

@Tag(name = "Content", description = "Модули и уроки курса, задания к урокам, тест модуля")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ContentController {

    private final CourseContentService contentService;
    private final AssignmentService assignmentService;
    private final QuizService quizService;

    @Operation(summary = "Модуль с уроками")
    @GetMapping("/modules/{id}")
    public ModuleResponse module(@PathVariable Long id) {
        return contentService.getModule(id);
    }

    @Operation(summary = "Обновить модуль")
    @PutMapping("/modules/{id}")
    public ModuleResponse updateModule(@PathVariable Long id, @Valid @RequestBody ModuleRequest request) {
        return contentService.updateModule(id, request);
    }

    @Operation(summary = "Удалить модуль (каскадно: уроки, задания, решения, тест)")
    @DeleteMapping("/modules/{id}")
    public ResponseEntity<Void> deleteModule(@PathVariable Long id) {
        contentService.deleteModule(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Уроки модуля")
    @GetMapping("/modules/{id}/lessons")
    public List<LessonResponse> lessons(@PathVariable Long id) {
        return contentService.getLessons(id);
    }

    @Operation(summary = "Добавить урок в модуль")
    @PostMapping("/modules/{id}/lessons")
    public ResponseEntity<LessonResponse> addLesson(@PathVariable Long id, @Valid @RequestBody LessonRequest request) {
        LessonResponse created = contentService.addLesson(id, request);
        return ResponseEntity.created(URI.create("/api/lessons/" + created.id())).body(created);
    }

    @Operation(summary = "Создать тест модуля (вопросы и варианты ответов)")
    @PostMapping("/modules/{id}/quiz")
    public ResponseEntity<QuizResponse> createQuiz(@PathVariable Long id, @Valid @RequestBody QuizRequest request) {
        QuizResponse created = quizService.create(id, request);
        return ResponseEntity.created(URI.create("/api/quizzes/" + created.id())).body(created);
    }

    @Operation(summary = "Урок")
    @GetMapping("/lessons/{id}")
    public LessonResponse lesson(@PathVariable Long id) {
        return contentService.getLesson(id);
    }

    @Operation(summary = "Обновить урок")
    @PutMapping("/lessons/{id}")
    public LessonResponse updateLesson(@PathVariable Long id, @Valid @RequestBody LessonRequest request) {
        return contentService.updateLesson(id, request);
    }

    @Operation(summary = "Удалить урок (каскадно: задания и решения)")
    @DeleteMapping("/lessons/{id}")
    public ResponseEntity<Void> deleteLesson(@PathVariable Long id) {
        contentService.deleteLesson(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Задания урока")
    @GetMapping("/lessons/{id}/assignments")
    public List<AssignmentResponse> assignments(@PathVariable Long id) {
        return assignmentService.findByLesson(id);
    }

    @Operation(summary = "Добавить задание к уроку (студенты курса получат уведомление)")
    @PostMapping("/lessons/{id}/assignments")
    public ResponseEntity<AssignmentResponse> addAssignment(@PathVariable Long id, @Valid @RequestBody AssignmentRequest request) {
        AssignmentResponse created = assignmentService.create(id, request);
        return ResponseEntity.created(URI.create("/api/assignments/" + created.id())).body(created);
    }
}
