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
@SuppressWarnings("unused")
public class UserClientImpl implements UserClient {

    private final RestClient userRestClient;

    @Override
    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByEmailFallback")
    public UserDto getUserByEmail(String email) {
        return userRestClient.get()
                .uri("/api/v1/users/email/{email}", email)
                .retrieve()
                .body(UserDto.class);
    }

    private UserDto getUserByEmailFallback(String email, Throwable throwable) {
        log.warn("Falling back for user email {}: {}", email, throwable.getMessage());
        return UserDto.builder()
                .email(email)
                .build();
    }
}