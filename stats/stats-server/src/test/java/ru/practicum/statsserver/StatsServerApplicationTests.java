package ru.practicum.statsserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class StatsServerApplicationTests {

    @Test
    void contextLoads() {
        // проверка успешного старта контекста Spring
    }
}

