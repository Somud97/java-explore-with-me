package ru.practicum.ewm.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.practicum.statsclient.StatsClient;
import ru.practicum.statsdto.EndpointHitDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Отправка hit в сервис статистики при обращении к публичным эндпоинтам событий.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StatsHitService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StatsClient statsClient;

    @Value("${spring.application.name:ewm-main-service}")
    private String appName;

    public void hit(String uri, HttpServletRequest request) {
        String safeApp = truncate(appName, 255);
        String safeUri = truncate(uri, 255);
        String ip = truncate(getClientIp(request), 255);
        EndpointHitDto dto = new EndpointHitDto();
        dto.setApp(safeApp);
        dto.setUri(safeUri);
        dto.setIp(ip);
        dto.setTimestamp(LocalDateTime.now().format(FORMATTER));

        for (int attempt = 1; attempt <= 4; attempt++) {
            try {
                statsClient.hit(dto);
                return;
            } catch (Exception e) {
                if (attempt == 4) {
                    log.warn("Не удалось отправить hit в stats после {} попыток: {}", attempt, e.getMessage());
                    return;
                }
                try {
                    Thread.sleep(80L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("Отправка hit прервана: {}", ie.getMessage());
                    return;
                }
            }
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
