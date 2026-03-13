package ru.practicum.statsserver.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.statsdto.EndpointHitDto;
import ru.practicum.statsdto.ViewStatsDto;
import ru.practicum.statsserver.service.StatsService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping
public class StatsController {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void hit(@Valid @RequestBody EndpointHitDto dto) {
        statsService.saveHit(dto);
    }

    @GetMapping("/stats")
    public List<ViewStatsDto> getStats(@RequestParam String start,
                                       @RequestParam String end,
                                       @RequestParam(required = false) List<String> uris,
                                       @RequestParam(defaultValue = "false") boolean unique) {
        LocalDateTime startDt = LocalDateTime.parse(start, FORMATTER);
        LocalDateTime endDt = LocalDateTime.parse(end, FORMATTER);
        return statsService.getStats(startDt, endDt, uris, unique);
    }
}

