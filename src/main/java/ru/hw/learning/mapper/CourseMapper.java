package ru.hw.learning.mapper;

import java.util.Comparator;
import java.util.List;
import ru.hw.learning.entity.Course;
import ru.hw.learning.entity.CourseModule;
import ru.hw.learning.entity.Lesson;
import ru.hw.learning.entity.Tag;
import ru.hw.learning.web.dto.CourseDtos.CourseResponse;
import ru.hw.learning.web.dto.CourseDtos.LessonResponse;
import ru.hw.learning.web.dto.CourseDtos.ModuleResponse;

/** Преобразование курса и его структуры в DTO. */
public final class CourseMapper {

    private CourseMapper() {
    }

    public static CourseResponse toResponse(Course c) {
        List<String> tags = c.getTags().stream().map(Tag::getName).sorted().toList();
        return new CourseResponse(c.getId(), c.getTitle(), c.getDescription(),
                c.getCategory().getId(), c.getCategory().getName(),
                c.getTeacher().getId(), c.getTeacher().getName(),
                c.getDurationHours(), c.getStartDate(), tags);
    }

    /** Модуль с уроками. Коллекция lessons должна быть уже загружена (JOIN FETCH) или доступна в транзакции. */
    public static ModuleResponse toResponse(CourseModule m) {
        List<LessonResponse> lessons = m.getLessons().stream()
                .sorted(Comparator.comparingInt(Lesson::getOrderIndex))
                .map(CourseMapper::toResponse)
                .toList();
        Long quizId = m.getQuiz() == null ? null : m.getQuiz().getId();
        return new ModuleResponse(m.getId(), m.getCourse().getId(), m.getTitle(), m.getDescription(), m.getOrderIndex(),
                lessons, quizId);
    }

    public static LessonResponse toResponse(Lesson l) {
        return new LessonResponse(l.getId(), l.getModule().getId(), l.getTitle(), l.getContent(), l.getVideoUrl(),
                l.getOrderIndex());
    }
}
