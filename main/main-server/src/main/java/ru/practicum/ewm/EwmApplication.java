package ru.practicum.ewm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class EwmApplication {

    public static void main(String[] args) {
        SpringApplication.run(EwmApplication.class, args);
    }
}
