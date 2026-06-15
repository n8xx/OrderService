package com.innowise.orderservice.client.impl;

import com.innowise.orderservice.client.UserClient;
import com.innowise.orderservice.dto.UserDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserClientImpl implements UserClient {

    private final RestClient userRestClient;

    @Override
    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByIdFallback")
    public UserDto getUserById(Long userId) {
        return userRestClient.get()
                .uri("/api/v1/users/{id}", userId)
                .retrieve()
                .body(UserDto.class);
    }

    private UserDto getUserByIdFallback(Long userId, Throwable throwable) {
        log.warn("Falling back for user id {}: {}", userId, throwable.getMessage());
        return UserDto.builder()
                .id(userId)
                .build();
    }
}