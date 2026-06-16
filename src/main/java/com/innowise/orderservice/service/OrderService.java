package com.innowise.orderservice.service;

import com.innowise.orderservice.dto.OrderRequest;
import com.innowise.orderservice.dto.OrderResponse;
import com.innowise.orderservice.entity.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    OrderResponse createOrder(OrderRequest orderRequestDto);

    OrderResponse getOrderById(Long id);

    Page<OrderResponse> getOrders(Long userId, LocalDateTime createdFrom, LocalDateTime createdTo,
                                     List<OrderStatus> statuses, Pageable pageable);


    OrderResponse updateOrder(Long id, OrderRequest orderRequestDto);

    void deleteOrder(Long id);
}