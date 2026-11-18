package ru.hw.learning.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Сертификат об окончании курса (1-1 с Enrollment). */
@Entity
@Table(name = "certificates")
@Getter
@Setter
@NoArgsConstructor
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false, unique = true)
    private Enrollment enrollment;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt = Instant.now();

    /** Итоговый балл по курсу, % (среднее по заданиям и тестам). */
    @Column(name = "final_score")
    private Double finalScore;

    public Certificate(String code, Double finalScore) {
        this.code = code;
        this.finalScore = finalScore;
    }
}
