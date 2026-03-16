package ru.practicum.ewm.mapper;

import ru.practicum.ewm.dto.CategoryDto;
import ru.practicum.ewm.model.Category;

public final class CategoryMapper {

    private CategoryMapper() {
    }

    public static CategoryDto toDto(Category category) {
        if (category == null) {
            return null;
        }
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
}
