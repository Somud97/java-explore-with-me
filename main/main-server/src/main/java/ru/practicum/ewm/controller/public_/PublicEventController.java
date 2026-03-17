package ru.practicum.ewm.controller.public_;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.dto.EventFullDto;
import ru.practicum.ewm.dto.EventShortDto;
import ru.practicum.ewm.service.EventService;
import ru.practicum.ewm.service.StatsHitService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class PublicEventController {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private final EventService eventService;
    private final StatsHitService statsHitService;

    @GetMapping
    public List<EventShortDto> getEvents_1(@RequestParam(required = false) String text,
                                           @RequestParam(required = false) List<Long> categories,
                                           @RequestParam(required = false) Boolean paid,
                                           @RequestParam(required = false) @DateTimeFormat(pattern = DATE_TIME_PATTERN) LocalDateTime rangeStart,
                                           @RequestParam(required = false) @DateTimeFormat(pattern = DATE_TIME_PATTERN) LocalDateTime rangeEnd,
                                           @RequestParam(defaultValue = "false") Boolean onlyAvailable,
                                           @RequestParam(required = false) String sort,
                                           @RequestParam(defaultValue = "0") int from,
                                           @RequestParam(defaultValue = "10") int size,
                                           HttpServletRequest request) {
        statsHitService.hit(request.getRequestURI(), request);
        int safeSize = size <= 0 ? 10 : size;
        List<EventShortDto> events = eventService.getEventsPublic(text, categories, paid, rangeStart,
                rangeEnd, onlyAvailable, sort, from, safeSize);
        return events;
    }

    @GetMapping("/{id}")
    public EventFullDto getEvent_1(@PathVariable Long id, HttpServletRequest request) {
        statsHitService.hit("/events/" + id, request);
        return eventService.getEventPublic(id);
    }
}
