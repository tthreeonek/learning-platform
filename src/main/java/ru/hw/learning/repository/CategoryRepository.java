package ru.hw.learning.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.hw.learning.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
