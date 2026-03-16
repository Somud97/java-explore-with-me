package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.EventFullDto;
import ru.practicum.ewm.dto.EventShortDto;
import ru.practicum.ewm.dto.NewEventDto;
import ru.practicum.ewm.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.dto.UpdateEventUserRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {

    EventFullDto create(Long userId, NewEventDto dto);

    EventFullDto updateByUser(Long userId, Long eventId, UpdateEventUserRequest request);

    EventFullDto updateByAdmin(Long eventId, UpdateEventAdminRequest request);

    EventFullDto getByUser(Long userId, Long eventId);

    List<EventShortDto> getEventsByUser(Long userId, int from, int size);

    List<EventFullDto> getEventsAdmin(List<Long> users, List<String> states, List<Long> categories,
                                      LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size);

    List<EventShortDto> getEventsPublic(String text, List<Long> categories, Boolean paid,
                                        LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                        Boolean onlyAvailable, String sort, int from, int size);

    EventFullDto getEventPublic(Long eventId);
}
