package ru.hw.learning.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import ru.hw.learning.entity.Role;

/** DTO пользователей и профилей. */
public final class UserDtos {

    private UserDtos() {
    }

    public record UserRequest(
            @NotBlank(message = "Имя обязательно") @Size(max = 100, message = "Имя не длиннее 100 символов") String name,
            @NotBlank(message = "Email обязателен") @Email(message = "Некорректный email") @Size(max = 150) String email,
            @NotNull(message = "Роль обязательна (STUDENT, TEACHER, ADMIN)") Role role) {
    }

    public record ProfileRequest(
            @Size(max = 2000, message = "Биография не длиннее 2000 символов") String bio,
            @Size(max = 500) @Pattern(regexp = "^(https?://.*)?$", message = "avatarUrl должен начинаться с http:// или https://")
            String avatarUrl,
            @Pattern(regexp = "^(\\+?[0-9 ()-]{5,20})?$", message = "Некорректный номер телефона") String phone,
            @Size(max = 100) String city) {
    }

    public record UserResponse(Long id, String name, String email, Role role, Instant createdAt) {
    }

    public record ProfileResponse(String bio, String avatarUrl, String phone, String city) {
    }

    public record UserDetailsResponse(Long id, String name, String email, Role role, Instant createdAt, ProfileResponse profile) {
    }
}
