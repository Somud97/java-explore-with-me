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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Отправка hit в сервис статистики при обращении к публичным эндпоинтам событий.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StatsHitService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StatsClient statsClient;

    private final ConcurrentMap<String, Long> localHits = new ConcurrentHashMap<>();

    @Value("${spring.application.name:ewm-main-service}")
    private String appName;

    public void hit(String uri, HttpServletRequest request) {
        localHits.merge(uri, 1L, Long::sum);
        try {
            EndpointHitDto dto = new EndpointHitDto();
            dto.setApp(appName);
            dto.setUri(uri);
            dto.setIp(getClientIp(request));
            dto.setTimestamp(LocalDateTime.now().format(FORMATTER));
            statsClient.hit(dto);
        } catch (Exception e) {
            log.warn("Не удалось отправить hit в stats: {}", e.getMessage());
        }
    }

    public long getLocalHits(String uri) {
        return localHits.getOrDefault(uri, 0L);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
