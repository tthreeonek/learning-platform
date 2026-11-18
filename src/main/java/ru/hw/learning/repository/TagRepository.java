package ru.hw.learning.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.hw.learning.entity.Tag;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
