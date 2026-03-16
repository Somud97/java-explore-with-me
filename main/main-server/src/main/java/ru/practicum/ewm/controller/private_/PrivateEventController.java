package ru.practicum.ewm.controller.private_;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.dto.EventFullDto;
import ru.practicum.ewm.dto.EventShortDto;
import ru.practicum.ewm.dto.NewEventDto;
import ru.practicum.ewm.dto.UpdateEventUserRequest;
import ru.practicum.ewm.service.EventService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
public class PrivateEventController {

    private final EventService eventService;

    @GetMapping
    public List<EventShortDto> getEvents(@PathVariable Long userId,
                                        @RequestParam(defaultValue = "0") int from,
                                        @RequestParam(defaultValue = "10") int size) {
        return eventService.getEventsByUser(userId, from, size);
    }

    @PostMapping
    public EventFullDto addEvent(@PathVariable Long userId, @Valid @RequestBody NewEventDto dto) {
        return eventService.create(userId, dto);
    }

    @GetMapping("/{eventId}")
    public EventFullDto getEvent(@PathVariable Long userId, @PathVariable Long eventId) {
        return eventService.getByUser(userId, eventId);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEvent(@PathVariable Long userId, @PathVariable Long eventId,
                                   @RequestBody UpdateEventUserRequest request) {
        return eventService.updateByUser(userId, eventId, request);
    }
}
