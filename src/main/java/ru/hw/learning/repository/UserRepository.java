package ru.hw.learning.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.hw.learning.entity.Role;
import ru.hw.learning.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findAllByRoleOrderByName(Role role);

    /** Пользователь вместе с ленивым профилем одним запросом (left join). */
    @EntityGraph(attributePaths = "profile")
    Optional<User> findWithProfileById(Long id);
}
