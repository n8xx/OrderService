package com.innowise.orderservice.service.impl;

import com.innowise.orderservice.client.UserClient;
import com.innowise.orderservice.exception.ItemNotFoundException;
import com.innowise.orderservice.exception.OrderNotFoundException;
import com.innowise.orderservice.mapper.OrderItemMapper;
import com.innowise.orderservice.mapper.OrderMapper;
import com.innowise.orderservice.dto.OrderItemDto;
import com.innowise.orderservice.dto.OrderRequest;
import com.innowise.orderservice.dto.OrderResponse;
import com.innowise.orderservice.dto.UserDto;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.entity.OrderItem;
import com.innowise.orderservice.entity.OrderStatus;
import com.innowise.orderservice.repository.ItemRepository;
import com.innowise.orderservice.repository.OrderRepository;
import com.innowise.orderservice.repository.specification.OrderSpecification;
import com.innowise.orderservice.service.OrderService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserClient userClient;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest orderRequest) {
        Order order = orderMapper.toEntity(orderRequest);
        order.setDeleted(false);

        List<OrderItem> orderItems = buildOrderItems(orderRequest.getOrderItems(), order);
        order.setOrderItems(orderItems);
        order.setTotalPrice(calculateTotalPrice(orderItems));

        Order savedOrder = orderRepository.save(order);
        return enrichWithUser(orderMapper.toDto(savedOrder));
    }

    @Override
    public OrderResponse getOrderById(Long id) {
        Order order = findOrderById(id);
        return enrichWithUser(orderMapper.toDto(order));
    }

    @Override
    public Page<OrderResponse> getOrders(LocalDateTime createdFrom, LocalDateTime createdTo,
                                            List<OrderStatus> statuses, Pageable pageable) {
        Specification<Order> specification = Specification
                .where(OrderSpecification.createdBetween(createdFrom, createdTo))
                .and(OrderSpecification.hasStatuses(statuses));

        return orderRepository.findAll(specification, pageable)
                .map(orderMapper::toDto)
                .map(this::enrichWithUser);
    }

    @Override
    public Page<OrderResponse> getOrdersByUserId(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable)
                .map(orderMapper::toDto)
                .map(this::enrichWithUser);
    }

    @Override
    @Transactional
    public OrderResponse updateOrder(Long id, OrderRequest orderRequest) {
        Order order = findOrderById(id);
        orderMapper.updateEntityFromDto(orderRequest, order);

        List<OrderItem> orderItems = buildOrderItems(orderRequest.getOrderItems(), order);
        order.getOrderItems().clear();
        order.getOrderItems().addAll(orderItems);
        order.setTotalPrice(calculateTotalPrice(orderItems));

        Order updatedOrder = orderRepository.save(order);
        return enrichWithUser(orderMapper.toDto(updatedOrder));
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        Order order = findOrderById(id);
        orderRepository.delete(order);
    }

    private Order findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
    }

    private List<OrderItem> buildOrderItems(List<OrderItemDto> orderItemDtos, Order order) {
        return orderItemDtos.stream()
                .map(dto -> toOrderItem(dto, order))
                .toList();
    }

    private OrderItem toOrderItem(OrderItemDto orderItemDto, Order order) {
        Item item = itemRepository.findById(orderItemDto.getItemId())
                .orElseThrow(() -> new ItemNotFoundException("Item not found with id: " + orderItemDto.getItemId()));

        OrderItem orderItem = orderItemMapper.toEntity(orderItemDto);
        orderItem.setOrder(order);
        orderItem.setItem(item);
        return orderItem;
    }

    private BigDecimal calculateTotalPrice(List<OrderItem> orderItems) {
        return orderItems.stream()
                .map(orderItem -> orderItem.getItem().getPrice()
                        .multiply(BigDecimal.valueOf(orderItem.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private OrderResponse enrichWithUser(OrderResponse orderResponse) {
        UserDto userDto = userClient.getUserById(orderResponse.getUserId());
        orderResponse.setUser(userDto);
        return orderResponse;
    }
}