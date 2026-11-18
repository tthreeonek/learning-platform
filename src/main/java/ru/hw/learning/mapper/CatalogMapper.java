package ru.hw.learning.mapper;

import ru.hw.learning.entity.Category;
import ru.hw.learning.entity.Notification;
import ru.hw.learning.entity.Tag;
import ru.hw.learning.web.dto.AnalyticsDtos.NotificationResponse;
import ru.hw.learning.web.dto.CatalogDtos.CategoryResponse;
import ru.hw.learning.web.dto.CatalogDtos.TagResponse;

/** Преобразование справочников (категории, теги) и уведомлений в DTO. */
public final class CatalogMapper {

    private CatalogMapper() {
    }

    public static CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription());
    }

    public static TagResponse toResponse(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName());
    }

    public static NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getMessage(), n.isRead(), n.getCreatedAt());
    }
}
