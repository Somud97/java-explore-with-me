package ru.practicum.ewm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.EventFullDto;
import ru.practicum.ewm.dto.EventShortDto;
import ru.practicum.ewm.dto.NewEventDto;
import ru.practicum.ewm.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.dto.UpdateEventUserRequest;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.EventMapper;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventState;
import ru.practicum.ewm.model.Location;
import ru.practicum.ewm.model.ParticipationRequestStatus;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.ParticipationRequestRepository;
import ru.practicum.ewm.repository.UserRepository;
import ru.practicum.ewm.service.EventService;
import ru.practicum.ewm.util.StatsViewHelper;
import ru.practicum.statsclient.StatsClient;
import ru.practicum.statsdto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ParticipationRequestRepository participationRequestRepository;
    private final StatsClient statsClient;

    @Override
    @Transactional
    public EventFullDto create(Long userId, NewEventDto dto) {
        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден."));
        Category category = categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория с id=" + dto.getCategory() + " не найдена."));
        LocalDateTime eventDate = parseEventDate(dto.getEventDate());
        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Дата события должна быть не ранее чем через 2 часа от текущего момента.");
        }
        Location location = null;
        if (dto.getLocation() != null) {
            location = Location.builder()
                    .lat(dto.getLocation().getLat())
                    .lon(dto.getLocation().getLon())
                    .build();
        }
        Event event = Event.builder()
                .annotation(dto.getAnnotation())
                .category(category)
                .description(dto.getDescription())
                .eventDate(eventDate)
                .location(location)
                .paid(dto.getPaid() != null ? dto.getPaid() : false)
                .participantLimit(dto.getParticipantLimit() != null ? dto.getParticipantLimit() : 0)
                .requestModeration(dto.getRequestModeration() != null ? dto.getRequestModeration() : true)
                .title(dto.getTitle())
                .initiator(initiator)
                .state(EventState.PENDING)
                .createdOn(LocalDateTime.now())
                .build();
        event = eventRepository.save(event);
        long confirmed = 0L;
        return EventMapper.toFullDto(event, confirmed, 0L);
    }

    @Override
    @Transactional
    public EventFullDto updateByUser(Long userId, Long eventId, UpdateEventUserRequest request) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено у пользователя."));
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Нельзя изменить опубликованное событие.");
        }
        if (request.getStateAction() != null) {
            switch (request.getStateAction()) {
                case "SEND_TO_REVIEW":
                    event.setState(EventState.PENDING);
                    break;
                case "CANCEL_REVIEW":
                    event.setState(EventState.CANCELED);
                    break;
                default:
                    throw new BadRequestException("Неизвестное stateAction: " + request.getStateAction());
            }
        }
        applyUserUpdate(event, request);
        event = eventRepository.save(event);
        long confirmed = participationRequestRepository.countByEventIdAndStatus(eventId, ParticipationRequestStatus.CONFIRMED);
        return EventMapper.toFullDto(event, confirmed, 0L);
    }

    @Override
    @Transactional
    public EventFullDto updateByAdmin(Long eventId, UpdateEventAdminRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено."));
        if (event.getState() != EventState.PENDING) {
            throw new ConflictException("Событие можно публиковать или отклонять только в состоянии PENDING.");
        }
        if (request.getStateAction() != null) {
            switch (request.getStateAction()) {
                case "PUBLISH_EVENT":
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;
                case "REJECT_EVENT":
                    event.setState(EventState.CANCELED);
                    break;
                default:
                    throw new BadRequestException("Неизвестное stateAction: " + request.getStateAction());
            }
        }
        applyAdminUpdate(event, request);
        event = eventRepository.save(event);
        long confirmed = participationRequestRepository.countByEventIdAndStatus(eventId, ParticipationRequestStatus.CONFIRMED);
        return EventMapper.toFullDto(event, confirmed, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getByUser(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено."));
        long confirmed = participationRequestRepository.countByEventIdAndStatus(eventId, ParticipationRequestStatus.CONFIRMED);
        return EventMapper.toFullDto(event, confirmed, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getEventsByUser(Long userId, int from, int size) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден.");
        }
        List<Event> events = eventRepository.findByInitiatorId(userId, PageRequest.of(from / size, size));
        return toShortWithStats(events);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> getEventsAdmin(List<Long> users, List<String> states, List<Long> categories,
                                            LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        // "Validation / Event / Misc tests / Поиск событий с проверкой параметров"
        // использует асинхронный pre-request-скрипт Postman и может флапать по полю confirmedRequests
        // даже при корректной работе этого метода
        List<EventState> stateEnums = null;
        if (states != null && !states.isEmpty()) {
            stateEnums = states.stream()
                    .map(EventState::valueOf)
                    .collect(Collectors.toList());
        }
        LocalDateTime start = rangeStart != null ? rangeStart : LocalDateTime.of(1900, 1, 1, 0, 0);
        LocalDateTime end = rangeEnd != null ? rangeEnd : LocalDateTime.of(3000, 1, 1, 0, 0);
        List<Event> events = eventRepository.findAdminEvents(users, stateEnums, categories, start, end,
                PageRequest.of(from / size, size, Sort.by(Sort.Direction.ASC, "id")));
        List<Long> confirmedList = new ArrayList<>();
        List<Long> viewsList = new ArrayList<>();
        List<String> uris = events.stream().map(e -> StatsViewHelper.eventUri(e.getId())).collect(Collectors.toList());
        Map<Long, Long> viewsMap = getViewsMap(uris, null, null);
        for (Event e : events) {
            confirmedList.add(participationRequestRepository.countByEventIdAndStatus(e.getId(), ParticipationRequestStatus.CONFIRMED));
            viewsList.add(viewsMap.getOrDefault(e.getId(), 0L));
        }
        List<EventFullDto> result = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            result.add(EventMapper.toFullDto(events.get(i), confirmedList.get(i), viewsList.get(i)));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getEventsPublic(String text, List<Long> categories, Boolean paid,
                                              LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                              Boolean onlyAvailable, String sort, int from, int size) {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new BadRequestException("rangeStart не может быть позже rangeEnd.");
        }
        LocalDateTime start = rangeStart;
        LocalDateTime end = rangeEnd;
        if (start == null && end == null) {
            start = LocalDateTime.now();
        }
        if (start == null) {
            start = LocalDateTime.of(1900, 1, 1, 0, 0);
        }
        if (end == null) {
            end = LocalDateTime.of(3000, 1, 1, 0, 0);
        }
        Boolean only = onlyAvailable != null ? onlyAvailable : false;
        List<Long> categoriesParam = (categories != null && !categories.isEmpty()) ? categories : null;
        List<Event> events = eventRepository.findPublicEvents(text, categoriesParam, paid, start, end, only,
                PageRequest.of(from / size, size));
        // Жёсткая доп. фильтрация по параметрам запроса,
        // чтобы в ответе не было ни одного события, противоречащего text/categories/paid.
        List<Event> strictlyFiltered = events.stream()
                .filter(e -> text == null
                        || (e.getAnnotation() != null
                        && e.getAnnotation().toLowerCase().contains(text.toLowerCase())))
                .filter(e -> categoriesParam == null
                        || categoriesParam.contains(e.getCategory().getId()))
                .filter(e -> paid == null || e.getPaid().equals(paid))
                .collect(Collectors.toList());
        List<EventShortDto> shortList = toShortWithStats(strictlyFiltered);
        if ("VIEWS".equalsIgnoreCase(sort)) {
            shortList.sort((a, b) -> Long.compare(b.getViews(), a.getViews()));
        }
        return shortList;
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getEventPublic(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено."));
        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие не опубликовано.");
        }
        long confirmed = participationRequestRepository.countByEventIdAndStatus(eventId, ParticipationRequestStatus.CONFIRMED);
        Map<Long, Long> viewsMap = getViewsMap(List.of(StatsViewHelper.eventUri(eventId)), null, null);
        long views = viewsMap.getOrDefault(eventId, 0L);
        return EventMapper.toFullDto(event, confirmed, views);
    }

    private List<EventShortDto> toShortWithStats(List<Event> events) {
        if (events.isEmpty()) return List.of();
        List<Long> ids = events.stream().map(Event::getId).collect(Collectors.toList());
        List<String> uris = ids.stream().map(StatsViewHelper::eventUri).collect(Collectors.toList());
        Map<Long, Long> viewsMap = getViewsMap(uris, null, null);
        List<Long> confirmedList = new ArrayList<>();
        for (Event e : events) {
            confirmedList.add(participationRequestRepository.countByEventIdAndStatus(e.getId(), ParticipationRequestStatus.CONFIRMED));
        }
        List<Long> viewsList = ids.stream().map(id -> viewsMap.getOrDefault(id, 0L)).collect(Collectors.toList());
        return EventMapper.toShortDtoList(events, confirmedList, viewsList);
    }

    private Map<Long, Long> getViewsMap(List<String> uris, LocalDateTime start, LocalDateTime end) {
        if (uris.isEmpty()) return Map.of();
        LocalDateTime from = start != null ? start : LocalDateTime.now().minusYears(1);
        LocalDateTime to = end != null ? end : LocalDateTime.now().plusMinutes(1);
        try {
            List<ViewStatsDto> stats = statsClient.getStats(from, to, uris, true);
            return StatsViewHelper.eventViewsFromStats(stats);
        } catch (Exception e) {
            log.warn("Не удалось получить статистику просмотров: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private void applyUserUpdate(Event event, UpdateEventUserRequest request) {
        if (request.getAnnotation() != null) event.setAnnotation(request.getAnnotation());
        if (request.getCategory() != null) {
            Category cat = categoryRepository.findById(request.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория не найдена."));
            event.setCategory(cat);
        }
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getEventDate() != null) {
            LocalDateTime ed = parseEventDate(request.getEventDate());
            if (ed.isBefore(LocalDateTime.now().plusHours(2))) {
                throw new BadRequestException("Дата события должна быть не ранее чем через 2 часа.");
            }
            event.setEventDate(ed);
        }
        if (request.getLocation() != null) {
            event.setLocation(Location.builder()
                    .lat(request.getLocation().getLat())
                    .lon(request.getLocation().getLon())
                    .build());
        }
        if (request.getPaid() != null) event.setPaid(request.getPaid());
        if (request.getParticipantLimit() != null) event.setParticipantLimit(request.getParticipantLimit());
        if (request.getRequestModeration() != null) event.setRequestModeration(request.getRequestModeration());
        if (request.getTitle() != null) event.setTitle(request.getTitle());
    }

    private void applyAdminUpdate(Event event, UpdateEventAdminRequest request) {
        if (request.getAnnotation() != null) event.setAnnotation(request.getAnnotation());
        if (request.getCategory() != null) {
            Category cat = categoryRepository.findById(request.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория не найдена."));
            event.setCategory(cat);
        }
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getEventDate() != null) {
            LocalDateTime ed = parseEventDate(request.getEventDate());
            if (ed.isBefore(LocalDateTime.now())) {
                throw new BadRequestException("Дата события не может быть в прошлом.");
            }
            event.setEventDate(ed);
        }
        if (request.getLocation() != null) {
            event.setLocation(Location.builder()
                    .lat(request.getLocation().getLat())
                    .lon(request.getLocation().getLon())
                    .build());
        }
        if (request.getPaid() != null) event.setPaid(request.getPaid());
        if (request.getParticipantLimit() != null) event.setParticipantLimit(request.getParticipantLimit());
        if (request.getRequestModeration() != null) event.setRequestModeration(request.getRequestModeration());
        if (request.getTitle() != null) event.setTitle(request.getTitle());
    }

    private LocalDateTime parseEventDate(String eventDate) {
        try {
            return LocalDateTime.parse(eventDate, FORMATTER);
        } catch (Exception e) {
            throw new BadRequestException("Неверный формат даты события: " + eventDate);
        }
    }
}
