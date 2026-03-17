package ru.practicum.ewm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Тело запроса на добавление заявки на участие (POST /users/{userId}/requests).
 * В спецификации — параметр eventId в query не описан явно; в примерах фигурирует eventId.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipationRequestEventDto {

    private Long eventId;
}
