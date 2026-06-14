package com.innowise.orderservice.dto;


import com.innowise.orderservice.entity.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    @NotNull(message = "User id is required")
    @Positive(message = "User id must be positive")
    private Long userId;

    @NotNull(message = "Status is required")
    private OrderStatus status;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemDto> orderItems;
}