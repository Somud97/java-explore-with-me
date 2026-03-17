package ru.practicum.ewm.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class CommentDto {

    private Long id;
    private Long eventId;
    private Long authorId;
    private String authorName;
    private String text;
    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;
}
