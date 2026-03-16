package ru.practicum.ewm.mapper;

import ru.practicum.ewm.dto.CategoryDto;
import ru.practicum.ewm.dto.EventFullDto;
import ru.practicum.ewm.dto.EventShortDto;
import ru.practicum.ewm.dto.LocationDto;
import ru.practicum.ewm.dto.UserShortDto;
import ru.practicum.ewm.model.Event;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Маппинг Event -> DTO. Формат дат по спецификации: yyyy-MM-dd HH:mm:ss.
 */
public final class EventMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private EventMapper() {
    }

    public static EventShortDto toShortDto(Event event, long confirmedRequests, long views) {
        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(CategoryMapper.toDto(event.getCategory()))
                .confirmedRequests(confirmedRequests)
                .eventDate(event.getEventDate().format(FORMATTER))
                .initiator(UserMapper.toShortDto(event.getInitiator()))
                .paid(event.getPaid())
                .title(event.getTitle())
                .views(views)
                .build();
    }

    public static EventFullDto toFullDto(Event event, long confirmedRequests, long views) {
        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(CategoryMapper.toDto(event.getCategory()))
                .confirmedRequests(confirmedRequests)
                .createdOn(event.getCreatedOn().format(FORMATTER))
                .description(event.getDescription())
                .eventDate(event.getEventDate().format(FORMATTER))
                .initiator(UserMapper.toShortDto(event.getInitiator()))
                .location(event.getLocation() != null
                        ? LocationDto.builder()
                        .lat(event.getLocation().getLat())
                        .lon(event.getLocation().getLon())
                        .build()
                        : null)
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn() != null ? event.getPublishedOn().format(FORMATTER) : null)
                .requestModeration(event.getRequestModeration())
                .state(event.getState().name())
                .title(event.getTitle())
                .views(views)
                .build();
    }

    public static List<EventShortDto> toShortDtoList(List<Event> events, List<Long> confirmedCounts, List<Long> viewCounts) {
        if (events.isEmpty()) {
            return new ArrayList<>();
        }
        List<EventShortDto> result = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            long conf = i < confirmedCounts.size() ? confirmedCounts.get(i) : 0L;
            long v = i < viewCounts.size() ? viewCounts.get(i) : 0L;
            result.add(toShortDto(events.get(i), conf, v));
        }
        return result;
    }
}
