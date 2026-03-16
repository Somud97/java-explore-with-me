package ru.practicum.ewm.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.dto.ParticipationRequestDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.ParticipationRequestMapper;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventState;
import ru.practicum.ewm.model.ParticipationRequest;
import ru.practicum.ewm.model.ParticipationRequestStatus;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.ParticipationRequestRepository;
import ru.practicum.ewm.repository.UserRepository;
import ru.practicum.ewm.service.ParticipationRequestService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final ParticipationRequestRepository participationRequestRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь не найден.");
        }
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено."));
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Участие возможно только в опубликованных событиях.");
        }
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор не может подать заявку на своё событие.");
        }
        if (participationRequestRepository.findByEventIdAndRequesterId(eventId, userId).isPresent()) {
            throw new ConflictException("Заявка уже подана.");
        }
        long confirmed = participationRequestRepository.countByEventIdAndStatus(eventId, ParticipationRequestStatus.CONFIRMED);
        if (event.getParticipantLimit() != 0 && confirmed >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит участников.");
        }
        ParticipationRequestStatus status = ParticipationRequestStatus.PENDING;
        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            status = ParticipationRequestStatus.CONFIRMED;
        }
        ParticipationRequest request = ParticipationRequest.builder()
                .event(event)
                .requester(userRepository.getReferenceById(userId))
                .status(status)
                .created(java.time.LocalDateTime.now())
                .build();
        request = participationRequestRepository.save(request);
        return ParticipationRequestMapper.toDto(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getRequestsByRequester(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь не найден.");
        }
        return participationRequestRepository.findByRequesterId(userId).stream()
                .map(ParticipationRequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest request = participationRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Заявка не найдена."));
        if (!request.getRequester().getId().equals(userId)) {
            throw new NotFoundException("Заявка не найдена.");
        }
        request.setStatus(ParticipationRequestStatus.CANCELED);
        request = participationRequestRepository.save(request);
        return ParticipationRequestMapper.toDto(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getRequestsByEvent(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено."));
        return participationRequestRepository.findByEventId(event.getId()).stream()
                .map(ParticipationRequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest request) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено."));
        List<ParticipationRequest> requests = participationRequestRepository.findByIdIn(request.getRequestIds());
        if (requests.stream().anyMatch(r -> !r.getEvent().getId().equals(eventId))) {
            throw new ConflictException("Не все заявки относятся к данному событию.");
        }
        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();
        long currentConfirmed = participationRequestRepository.countByEventIdAndStatus(eventId, ParticipationRequestStatus.CONFIRMED);
        int limit = event.getParticipantLimit();

        for (ParticipationRequest pr : requests) {
            if (pr.getStatus() != ParticipationRequestStatus.PENDING) {
                continue;
            }
            if ("CONFIRMED".equals(request.getStatus())) {
                if (limit != 0 && currentConfirmed >= limit) {
                    pr.setStatus(ParticipationRequestStatus.REJECTED);
                    rejected.add(ParticipationRequestMapper.toDto(participationRequestRepository.save(pr)));
                } else {
                    pr.setStatus(ParticipationRequestStatus.CONFIRMED);
                    confirmed.add(ParticipationRequestMapper.toDto(participationRequestRepository.save(pr)));
                    currentConfirmed++;
                }
            } else if ("REJECTED".equals(request.getStatus())) {
                pr.setStatus(ParticipationRequestStatus.REJECTED);
                rejected.add(ParticipationRequestMapper.toDto(participationRequestRepository.save(pr)));
            }
        }
        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmed)
                .rejectedRequests(rejected)
                .build();
    }
}
