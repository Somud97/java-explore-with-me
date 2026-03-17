package ru.practicum.ewm.util;

import ru.practicum.statsdto.ViewStatsDto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Из ответа stats-сервиса собирает просмотры по id события.
 * URI в формате /events/{id} -> hits считаем просмотрами события.
 */
public final class StatsViewHelper {

    private static final Pattern EVENT_URI = Pattern.compile("/events/(\\d+)$");

    private StatsViewHelper() {
    }

    /**
     * По списку ViewStatsDto (uri, hits) строит мапу eventId -> просмотры.
     */
    public static Map<Long, Long> eventViewsFromStats(List<ViewStatsDto> stats) {
        Map<Long, Long> result = new HashMap<>();
        if (stats == null) {
            return result;
        }
        for (ViewStatsDto dto : stats) {
            String uri = dto.getUri();
            if (uri == null) continue;
            var m = EVENT_URI.matcher(uri);
            if (m.find()) {
                long eventId = Long.parseLong(m.group(1));
                long hits = dto.getHits() != null ? dto.getHits() : 0L;
                result.merge(eventId, hits, Long::sum);
            }
        }
        return result;
    }

    public static String eventUri(Long eventId) {
        return "/events/" + eventId;
    }
}
