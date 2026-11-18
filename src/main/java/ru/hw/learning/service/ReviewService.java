package ru.hw.learning.service;

import static ru.hw.learning.service.Guards.require;
import static ru.hw.learning.service.Guards.requireRole;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hw.learning.entity.Course;
import ru.hw.learning.entity.CourseReview;
import ru.hw.learning.entity.Role;
import ru.hw.learning.entity.User;
import ru.hw.learning.exception.ConflictException;
import ru.hw.learning.mapper.LearningMapper;
import ru.hw.learning.repository.CourseRepository;
import ru.hw.learning.repository.CourseReviewRepository;
import ru.hw.learning.repository.UserRepository;
import ru.hw.learning.web.dto.EnrollmentDtos.ReviewRequest;
import ru.hw.learning.web.dto.EnrollmentDtos.ReviewResponse;

/** Отзывы о курсах: оставить может только записанный студент, один отзыв на курс. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final CourseReviewRepository reviewRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentService enrollmentService;

    @Transactional
    public ReviewResponse create(Long courseId, ReviewRequest request) {
        Course course = require(courseRepository.findById(courseId), "Курс", courseId);
        User student = require(userRepository.findById(request.studentId()), "Пользователь", request.studentId());
        requireRole(student, "оставлять отзывы", Role.STUDENT);
        enrollmentService.requireActiveEnrollment(student.getId(), courseId);
        if (reviewRepository.existsByCourseIdAndStudentId(courseId, student.getId())) {
            throw new ConflictException("Студент id=" + student.getId() + " уже оставил отзыв о курсе id=" + courseId);
        }
        CourseReview review = reviewRepository.save(new CourseReview(course, student, request.rating(), request.comment()));
        log.info("Отзыв id={} о курсе id={}: {}/5", review.getId(), courseId, review.getRating());
        return LearningMapper.toResponse(review);
    }

    public List<ReviewResponse> findByCourse(Long courseId) {
        require(courseRepository.findById(courseId), "Курс", courseId);
        return reviewRepository.findAllByCourseIdOrderByCreatedAtDesc(courseId).stream().map(LearningMapper::toResponse).toList();
    }

    @Transactional
    public void delete(Long id) {
        CourseReview review = require(reviewRepository.findById(id), "Отзыв", id);
        review.getCourse().getReviews().remove(review);
        reviewRepository.delete(review);
        log.info("Удалён отзыв id={}", id);
    }
}
