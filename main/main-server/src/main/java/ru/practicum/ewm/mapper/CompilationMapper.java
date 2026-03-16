package ru.practicum.ewm.mapper;

import ru.practicum.ewm.dto.CompilationDto;
import ru.practicum.ewm.dto.EventShortDto;
import ru.practicum.ewm.model.Compilation;

import java.util.List;
import java.util.stream.Collectors;

public final class CompilationMapper {

    private CompilationMapper() {
    }

    /** Подборка с уже заполненными событиями (EventShortDto с views и confirmedRequests). */
    public static CompilationDto toDto(Compilation compilation, List<EventShortDto> events) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .pinned(compilation.getPinned())
                .title(compilation.getTitle())
                .events(events != null ? events : List.of())
                .build();
    }
}
