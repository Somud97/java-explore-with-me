package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.CategoryDto;
import ru.practicum.ewm.dto.NewCategoryDto;

import java.util.List;

public interface CategoryService {

    CategoryDto create(NewCategoryDto dto);

    void deleteById(Long catId);

    CategoryDto update(Long catId, CategoryDto dto);

    List<CategoryDto> getAll(int from, int size);

    CategoryDto getById(Long catId);
}
