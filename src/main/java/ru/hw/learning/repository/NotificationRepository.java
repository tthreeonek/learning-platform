package ru.hw.learning.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.hw.learning.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadFalse(Long userId);
}
