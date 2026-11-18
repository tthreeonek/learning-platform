package ru.hw.learning.service;

import java.util.Arrays;
import java.util.Optional;
import ru.hw.learning.entity.Role;
import ru.hw.learning.entity.User;
import ru.hw.learning.exception.BusinessRuleException;
import ru.hw.learning.exception.NotFoundException;

/** Общие проверки сервисного слоя: наличие сущности и роль пользователя. */
public final class Guards {

    private Guards() {
    }

    public static <T> T require(Optional<T> value, String entity, Object id) {
        return value.orElseThrow(() -> NotFoundException.of(entity, id));
    }

    public static void requireRole(User user, String action, Role... allowed) {
        if (Arrays.stream(allowed).noneMatch(user::hasRole)) {
            throw new BusinessRuleException("Пользователь " + user.getName() + " (" + user.getRole() + ") не может "
                    + action + ": нужна роль " + Arrays.toString(allowed));
        }
    }
}
