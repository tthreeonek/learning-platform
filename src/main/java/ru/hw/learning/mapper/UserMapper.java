package ru.hw.learning.mapper;

import ru.hw.learning.entity.Profile;
import ru.hw.learning.entity.User;
import ru.hw.learning.web.dto.UserDtos.ProfileResponse;
import ru.hw.learning.web.dto.UserDtos.UserDetailsResponse;
import ru.hw.learning.web.dto.UserDtos.UserResponse;

/**
 * Преобразование пользователей и профилей в DTO.
 * Вызывается внутри транзакции сервиса, пока ленивые связи ещё можно загрузить.
 */
public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }

    public static UserDetailsResponse toDetails(User user) {
        return new UserDetailsResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt(),
                toProfile(user.getProfile()));
    }

    public static ProfileResponse toProfile(Profile profile) {
        if (profile == null) {
            return null;
        }
        return new ProfileResponse(profile.getBio(), profile.getAvatarUrl(), profile.getPhone(), profile.getCity());
    }
}
