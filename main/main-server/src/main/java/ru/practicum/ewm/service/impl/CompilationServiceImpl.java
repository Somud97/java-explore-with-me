package ru.practicum.ewm.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.CompilationDto;
import ru.practicum.ewm.dto.EventShortDto;
import ru.practicum.ewm.dto.NewCompilationDto;
import ru.practicum.ewm.dto.UpdateCompilationRequest;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CompilationMapper;
import ru.practicum.ewm.mapper.EventMapper;
import ru.practicum.ewm.model.Compilation;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.repository.CompilationRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.ParticipationRequestRepository;
import ru.practicum.ewm.service.CompilationService;
import ru.practicum.ewm.util.StatsViewHelper;
import ru.practicum.ewm.model.ParticipationRequestStatus;
import ru.practicum.statsclient.StatsClient;
import ru.practicum.statsdto.ViewStatsDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final ParticipationRequestRepository participationRequestRepository;
    private final StatsClient statsClient;

    @Override
    @Transactional
    public CompilationDto create(NewCompilationDto dto) {
        List<Event> events = (dto.getEvents() != null && !dto.getEvents().isEmpty())
                ? eventRepository.findAllById(dto.getEvents())
                : new ArrayList<>();
        Compilation compilation = Compilation.builder()
                .title(dto.getTitle())
                .pinned(dto.getPinned() != null ? dto.getPinned() : false)
                .events(events)
                .build();
        compilation = compilationRepository.save(compilation);
        return CompilationMapper.toDto(compilation, eventShortDtos(compilation.getEvents()));
    }

    @Override
    @Transactional
    public CompilationDto update(Long compId, UpdateCompilationRequest request) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id=" + compId + " не найдена."));
        if (request.getTitle() != null) compilation.setTitle(request.getTitle());
        if (request.getPinned() != null) compilation.setPinned(request.getPinned());
        if (request.getEvents() != null) {
            compilation.setEvents(eventRepository.findAllById(request.getEvents()));
        }
        compilation = compilationRepository.save(compilation);
        return CompilationMapper.toDto(compilation, eventShortDtos(compilation.getEvents()));
    }

    @Override
    @Transactional
    public void delete(Long compId) {
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Подборка с id=" + compId + " не найдена.");
        }
        compilationRepository.deleteById(compId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompilationDto> getAll(Boolean pinned, int from, int size) {
        List<Compilation> list = pinned != null
                ? compilationRepository.findByPinned(pinned, PageRequest.of(from / size, size))
                : compilationRepository.findAll(PageRequest.of(from / size, size)).getContent();
        return list.stream()
                .map(c -> CompilationMapper.toDto(c, eventShortDtos(c.getEvents())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CompilationDto getById(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id=" + compId + " не найдена."));
        return CompilationMapper.toDto(compilation, eventShortDtos(compilation.getEvents()));
    }

    private List<EventShortDto> eventShortDtos(List<Event> events) {
        if (events == null || events.isEmpty()) return List.of();
        List<Long> ids = events.stream().map(Event::getId).collect(Collectors.toList());
        List<String> uris = ids.stream().map(StatsViewHelper::eventUri).collect(Collectors.toList());
        Map<Long, Long> viewsMap = getViewsMap(uris);
        List<Long> confirmedList = new ArrayList<>();
        for (Event e : events) {
            confirmedList.add(participationRequestRepository.countByEventIdAndStatus(e.getId(), ParticipationRequestStatus.CONFIRMED));
        }
        List<Long> viewsList = ids.stream().map(id -> viewsMap.getOrDefault(id, 0L)).collect(Collectors.toList());
        return EventMapper.toShortDtoList(events, confirmedList, viewsList);
    }

    private Map<Long, Long> getViewsMap(List<String> uris) {
        if (uris.isEmpty()) return new HashMap<>();
        try {
            List<ViewStatsDto> stats = statsClient.getStats(
                    java.time.LocalDateTime.now().minusYears(1),
                    java.time.LocalDateTime.now().plusMinutes(1),
                    uris, true);
            return StatsViewHelper.eventViewsFromStats(stats);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}
