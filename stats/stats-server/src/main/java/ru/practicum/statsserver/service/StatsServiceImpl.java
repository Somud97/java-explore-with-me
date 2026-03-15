package ru.practicum.statsserver.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.statsdto.EndpointHitDto;
import ru.practicum.statsdto.ViewStatsDto;
import ru.practicum.statsserver.model.EndpointHit;
import ru.practicum.statsserver.repository.EndpointHitRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class StatsServiceImpl implements StatsService {

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private final EndpointHitRepository repository;

    public StatsServiceImpl(EndpointHitRepository repository) {
        this.repository = repository;
    }

    @Override
    public void saveHit(EndpointHitDto hitDto) {
        EndpointHit hit = new EndpointHit();
        hit.setApp(hitDto.getApp());
        hit.setUri(hitDto.getUri());
        hit.setIp(hitDto.getIp());
        hit.setTimestamp(LocalDateTime.parse(hitDto.getTimestamp(), DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));
        repository.save(hit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViewStatsDto> getStats(LocalDateTime start,
                                       LocalDateTime end,
                                       List<String> uris,
                                       boolean unique) {
        List<Object[]> rows;
        if (uris == null || uris.isEmpty()) {
            rows = unique ? repository.findUniqueStats(start, end) : repository.findStats(start, end);
        } else {
            rows = unique
                    ? repository.findUniqueStatsForUris(start, end, uris)
                    : repository.findStatsForUris(start, end, uris);
        }
        List<ViewStatsDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            String app = (String) row[0];
            String uri = (String) row[1];
            Long hits = (Long) row[2];
            result.add(new ViewStatsDto(app, uri, hits));
        }
        return result;
    }
}

