package ru.practicum.ewm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewCompilationDto {

    @Builder.Default
    private List<Long> events = new ArrayList<>();

    @Builder.Default
    private Boolean pinned = false;

    @NotBlank
    @Size(min = 1, max = 50)
    private String title;
}
