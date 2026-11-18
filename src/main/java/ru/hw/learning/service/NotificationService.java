package ru.hw.learning.service;

import static ru.hw.learning.service.Guards.require;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hw.learning.entity.Notification;
import ru.hw.learning.entity.NotificationType;
import ru.hw.learning.entity.User;
import ru.hw.learning.mapper.CatalogMapper;
import ru.hw.learning.repository.NotificationRepository;
import ru.hw.learning.repository.UserRepository;
import ru.hw.learning.web.dto.AnalyticsDtos.NotificationResponse;

/**
 * Уведомления пользователям (хранятся в БД). Создаются при выдаче задания, оценке решения,
 * прохождении теста и завершении курса.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void notify(User user, NotificationType type, String message) {
        notificationRepository.save(new Notification(user, type, message));
        log.debug("Уведомление пользователю id={}: {}", user.getId(), message);
    }

    public List<NotificationResponse> findForUser(Long userId) {
        require(userRepository.findById(userId), "Пользователь", userId);
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(CatalogMapper::toResponse)
                .toList();
    }

    @Transactional
    public NotificationResponse markRead(Long id) {
        Notification notification = require(notificationRepository.findById(id), "Уведомление", id);
        notification.setRead(true);
        return CatalogMapper.toResponse(notification);
    }
}
