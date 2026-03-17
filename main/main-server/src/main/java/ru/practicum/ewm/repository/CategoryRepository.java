package ru.practicum.ewm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.model.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByName(String name);
}
