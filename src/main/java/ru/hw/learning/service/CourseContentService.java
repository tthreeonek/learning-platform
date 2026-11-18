package ru.hw.learning.service;

import static ru.hw.learning.service.Guards.require;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hw.learning.entity.CourseModule;
import ru.hw.learning.entity.Lesson;
import ru.hw.learning.mapper.CourseMapper;
import ru.hw.learning.repository.CourseModuleRepository;
import ru.hw.learning.repository.CourseRepository;
import ru.hw.learning.repository.LessonRepository;
import ru.hw.learning.web.dto.CourseDtos.LessonRequest;
import ru.hw.learning.web.dto.CourseDtos.LessonResponse;
import ru.hw.learning.web.dto.CourseDtos.ModuleRequest;
import ru.hw.learning.web.dto.CourseDtos.ModuleResponse;

/** Содержимое курса: модули и уроки. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseContentService {

    private final CourseModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;

    /** Собирает модуль с уроками из запроса (ещё не сохранён — сохранится каскадом от курса). */
    public CourseModule buildModule(ModuleRequest request, int defaultOrder) {
        int order = request.orderIndex() != null ? request.orderIndex() : defaultOrder;
        CourseModule module = new CourseModule(request.title().trim(), request.description(), order);
        List<LessonRequest> lessons = request.lessons() == null ? List.of() : request.lessons();
        for (int i = 0; i < lessons.size(); i++) {
            module.addLesson(buildLesson(lessons.get(i), i + 1));
        }
        return module;
    }

    /** Внутри транзакции ленивая коллекция lessons загружается при обращении — исключения нет. */
    public ModuleResponse getModule(Long id) {
        return CourseMapper.toResponse(require(moduleRepository.findById(id), "Модуль", id));
    }

    public List<ModuleResponse> getModules(Long courseId) {
        require(courseRepository.findById(courseId), "Курс", courseId);
        return moduleRepository.findWithLessonsByCourseId(courseId).stream().map(CourseMapper::toResponse).toList();
    }

    @Transactional
    public ModuleResponse updateModule(Long id, ModuleRequest request) {
        CourseModule module = require(moduleRepository.findById(id), "Модуль", id);
        module.setTitle(request.title().trim());
        module.setDescription(request.description());
        if (request.orderIndex() != null) {
            module.setOrderIndex(request.orderIndex());
        }
        log.info("Обновлён модуль id={}", id);
        return CourseMapper.toResponse(module);
    }

    /** Удаление модуля каскадно удаляет его уроки, задания, решения и тест. */
    @Transactional
    public void deleteModule(Long id) {
        CourseModule module = require(moduleRepository.findById(id), "Модуль", id);
        module.getCourse().removeModule(module);
        moduleRepository.delete(module);
        log.info("Удалён модуль id={}", id);
    }

    @Transactional
    public LessonResponse addLesson(Long moduleId, LessonRequest request) {
        CourseModule module = require(moduleRepository.findById(moduleId), "Модуль", moduleId);
        Lesson lesson = buildLesson(request, module.getLessons().size() + 1);
        module.addLesson(lesson);
        lessonRepository.save(lesson);
        log.info("В модуль id={} добавлен урок id={} '{}'", moduleId, lesson.getId(), lesson.getTitle());
        return CourseMapper.toResponse(lesson);
    }

    public LessonResponse getLesson(Long id) {
        return CourseMapper.toResponse(require(lessonRepository.findById(id), "Урок", id));
    }

    public List<LessonResponse> getLessons(Long moduleId) {
        require(moduleRepository.findById(moduleId), "Модуль", moduleId);
        return lessonRepository.findAllByModuleIdOrderByOrderIndex(moduleId).stream().map(CourseMapper::toResponse).toList();
    }

    @Transactional
    public LessonResponse updateLesson(Long id, LessonRequest request) {
        Lesson lesson = require(lessonRepository.findById(id), "Урок", id);
        lesson.setTitle(request.title().trim());
        lesson.setContent(request.content());
        lesson.setVideoUrl(request.videoUrl());
        if (request.orderIndex() != null) {
            lesson.setOrderIndex(request.orderIndex());
        }
        log.info("Обновлён урок id={}", id);
        return CourseMapper.toResponse(lesson);
    }

    @Transactional
    public void deleteLesson(Long id) {
        Lesson lesson = require(lessonRepository.findById(id), "Урок", id);
        lesson.getModule().getLessons().remove(lesson);
        lessonRepository.delete(lesson);
        log.info("Удалён урок id={}", id);
    }

    private Lesson buildLesson(LessonRequest request, int defaultOrder) {
        int order = request.orderIndex() != null ? request.orderIndex() : defaultOrder;
        return new Lesson(request.title().trim(), request.content(), request.videoUrl(), order);
    }
}
