package ru.hw.learning.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Общие бины приложения. */
@Configuration
public class AppConfig {

    /** Часы выносятся в бин, чтобы в тестах можно было подменить текущее время (проверка дедлайнов). */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("Learning Platform API")
                .version("1.0.0")
                .description("Учебная платформа онлайн-курса по ORM и Hibernate: курсы, модули, уроки, "
                        + "запись на курсы, задания и решения, тесты, отзывы, аналитика."));
    }
}
