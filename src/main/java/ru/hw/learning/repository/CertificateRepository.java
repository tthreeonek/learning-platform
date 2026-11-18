package ru.hw.learning.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.hw.learning.entity.Certificate;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    @EntityGraph(attributePaths = {"enrollment", "enrollment.student", "enrollment.course"})
    Optional<Certificate> findByCode(String code);

    Optional<Certificate> findByEnrollmentId(Long enrollmentId);
}
