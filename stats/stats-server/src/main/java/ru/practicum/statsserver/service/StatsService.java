package ru.practicum.statsserver.service;

import ru.practicum.statsdto.EndpointHitDto;
import ru.practicum.statsdto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsService {

    void saveHit(EndpointHitDto hitDto);

    List<ViewStatsDto> getStats(LocalDateTime start,
                                LocalDateTime end,
                                List<String> uris,
                                boolean unique);
}

