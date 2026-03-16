package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.NewUserRequest;
import ru.practicum.ewm.dto.UserDto;

import java.util.List;

public interface UserService {

    UserDto create(NewUserRequest request);

    void deleteById(Long userId);

    List<UserDto> getByIds(List<Long> ids, int from, int size);
}
