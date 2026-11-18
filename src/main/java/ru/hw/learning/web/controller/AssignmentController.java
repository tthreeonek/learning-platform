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
import ru.hw.learning.service.SubmissionService;
import ru.hw.learning.web.dto.AssignmentDtos.AssignmentRequest;
import ru.hw.learning.web.dto.AssignmentDtos.AssignmentResponse;
import ru.hw.learning.web.dto.AssignmentDtos.GradeRequest;
import ru.hw.learning.web.dto.AssignmentDtos.SubmissionRequest;
import ru.hw.learning.web.dto.AssignmentDtos.SubmissionResponse;

@Tag(name = "Assignments", description = "Задания, отправка решений и их проверка")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;

    @Operation(summary = "Задание")
    @GetMapping("/assignments/{id}")
    public AssignmentResponse get(@PathVariable Long id) {
        return assignmentService.get(id);
    }

    @Operation(summary = "Обновить задание")
    @PutMapping("/assignments/{id}")
    public AssignmentResponse update(@PathVariable Long id, @Valid @RequestBody AssignmentRequest request) {
        return assignmentService.update(id, request);
    }

    @Operation(summary = "Удалить задание (вместе с решениями)")
    @DeleteMapping("/assignments/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        assignmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Отправить решение (один раз на задание)")
    @PostMapping("/assignments/{id}/submit")
    public ResponseEntity<SubmissionResponse> submit(@PathVariable Long id, @Valid @RequestBody SubmissionRequest request) {
        SubmissionResponse created = submissionService.submit(id, request);
        return ResponseEntity.created(URI.create("/api/submissions/" + created.id())).body(created);
    }

    @Operation(summary = "Все решения по заданию (для преподавателя)")
    @GetMapping("/assignments/{id}/submissions")
    public List<SubmissionResponse> submissions(@PathVariable Long id) {
        return submissionService.findByAssignment(id);
    }

    @Operation(summary = "Решение")
    @GetMapping("/submissions/{id}")
    public SubmissionResponse submission(@PathVariable Long id) {
        return submissionService.get(id);
    }

    @Operation(summary = "Оценить решение")
    @PutMapping("/submissions/{id}/grade")
    public SubmissionResponse grade(@PathVariable Long id, @Valid @RequestBody GradeRequest request) {
        return submissionService.grade(id, request);
    }

    @Operation(summary = "Отозвать решение (только до проверки)")
    @DeleteMapping("/submissions/{id}")
    public ResponseEntity<Void> deleteSubmission(@PathVariable Long id) {
        submissionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
