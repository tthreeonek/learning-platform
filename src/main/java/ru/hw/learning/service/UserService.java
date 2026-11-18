package ru.hw.learning.service;

import static ru.hw.learning.service.Guards.require;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hw.learning.entity.Profile;
import ru.hw.learning.entity.Role;
import ru.hw.learning.entity.User;
import ru.hw.learning.exception.ConflictException;
import ru.hw.learning.mapper.UserMapper;
import ru.hw.learning.repository.CourseRepository;
import ru.hw.learning.repository.UserRepository;
import ru.hw.learning.web.dto.UserDtos.ProfileRequest;
import ru.hw.learning.web.dto.UserDtos.UserDetailsResponse;
import ru.hw.learning.web.dto.UserDtos.UserRequest;
import ru.hw.learning.web.dto.UserDtos.UserResponse;

/** Пользователи и профили. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public UserResponse create(UserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Пользователь с email " + request.email() + " уже существует");
        }
        User user = userRepository.save(new User(request.name().trim(), request.email().trim().toLowerCase(), request.role()));
        log.info("Создан пользователь id={} email={} роль={}", user.getId(), user.getEmail(), user.getRole());
        return UserMapper.toResponse(user);
    }

    public List<UserResponse> findAll(Role role) {
        List<User> users = role == null ? userRepository.findAll() : userRepository.findAllByRoleOrderByName(role);
        return users.stream().map(UserMapper::toResponse).toList();
    }

    /** Пользователь с профилем: профиль загружается тем же запросом через @EntityGraph. */
    public UserDetailsResponse get(Long id) {
        return UserMapper.toDetails(require(userRepository.findWithProfileById(id), "Пользователь", id));
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User user = require(userRepository.findById(id), "Пользователь", id);
        String email = request.email().trim().toLowerCase();
        if (!user.getEmail().equalsIgnoreCase(email) && userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email " + email + " уже занят");
        }
        if (user.getRole() == Role.TEACHER && request.role() != Role.TEACHER && courseRepository.existsByTeacherId(id)) {
            throw new ConflictException("Нельзя сменить роль преподавателя, у которого есть курсы");
        }
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setRole(request.role());
        log.info("Обновлён пользователь id={}", id);
        return UserMapper.toResponse(user);
    }

    /** Создаёт или обновляет профиль (связь 1-1). */
    @Transactional
    public UserDetailsResponse updateProfile(Long userId, ProfileRequest request) {
        User user = require(userRepository.findWithProfileById(userId), "Пользователь", userId);
        Profile profile = user.getProfile();
        if (profile == null) {
            user.attachProfile(new Profile(request.bio(), request.avatarUrl(), request.phone(), request.city()));
        } else {
            profile.setBio(request.bio());
            profile.setAvatarUrl(request.avatarUrl());
            profile.setPhone(request.phone());
            profile.setCity(request.city());
        }
        return UserMapper.toDetails(user);
    }

    /**
     * Удаление пользователя. Профиль, записи, решения, результаты тестов, отзывы и уведомления
     * удаляются каскадно. Преподавателя с курсами удалить нельзя — сначала нужно передать курсы.
     */
    @Transactional
    public void delete(Long id) {
        User user = require(userRepository.findById(id), "Пользователь", id);
        if (courseRepository.existsByTeacherId(id)) {
            throw new ConflictException("Преподаватель ведёт курсы — сначала назначьте курсам другого преподавателя");
        }
        userRepository.delete(user);
        log.info("Удалён пользователь id={}", id);
    }
}
