package com.innowise.orderservice.client;

import com.innowise.orderservice.dto.UserDto;

public interface UserClient {

    UserDto getUserById(Long userId);
}