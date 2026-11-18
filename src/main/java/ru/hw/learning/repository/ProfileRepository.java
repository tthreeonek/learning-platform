package ru.hw.learning.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.hw.learning.entity.Profile;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Optional<Profile> findByUserId(Long userId);
}
