package ru.hw.learning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hw.learning.repository.QuizSubmissionRepository;
import ru.hw.learning.repository.SubmissionRepository;

@ExtendWith(MockitoExtension.class)
class ProgressCalculatorTest {

    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private QuizSubmissionRepository quizSubmissionRepository;
    @InjectMocks
    private ProgressCalculator calculator;

    @Test
    @DisplayName("Итог — среднее заданий и тестов; при отсутствии одного берётся другой; без оценок — null")
    void combine() {
        assertThat(ProgressCalculator.combine(90.0, 100.0)).isEqualTo(95.0);
        assertThat(ProgressCalculator.combine(null, 80.0)).isEqualTo(80.0);
        assertThat(ProgressCalculator.combine(70.0, null)).isEqualTo(70.0);
        assertThat(ProgressCalculator.combine(null, null)).isNull();
    }

    @Test
    @DisplayName("Тесты учитываются по лучшей попытке каждого теста, результат округляется")
    void overallFromRepositories() {
        when(submissionRepository.averagePercent(1L, 2L)).thenReturn(80.04);
        when(quizSubmissionRepository.bestPercentsPerQuiz(1L, 2L)).thenReturn(List.of(100.0, 50.0));

        assertThat(calculator.assignmentsPercent(1L, 2L)).isEqualTo(80.0);
        assertThat(calculator.quizzesPercent(1L, 2L)).isEqualTo(75.0);
        assertThat(calculator.overallPercent(1L, 2L)).isEqualTo(77.5);
    }
}
