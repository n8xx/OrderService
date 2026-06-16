package com.innowise.orderservice.dto;

import com.innowise.orderservice.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;

    private Long userId;

    private String userEmail;

    private OrderStatus status;

    private BigDecimal totalPrice;

    private List<OrderItemDto> orderItems;

    private UserDto user;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}