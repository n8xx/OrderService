package com.innowise.orderservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.innowise.orderservice.dao.ItemRepository;
import com.innowise.orderservice.dao.OrderRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    private OrderRequest orderRequest;
    private Order order;
    private Item item;
    private OrderResponse orderResponse;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        OrderItemDto orderItemDto = new OrderItemDto(null, 1L, 2);
        orderRequest = new OrderRequest(10L, "anna@example.com", OrderStatus.NEW, List.of(orderItemDto));

        item = new Item();
        item.setId(1L);
        item.setName("Keyboard");
        item.setPrice(BigDecimal.valueOf(50));

        order = new Order();
        order.setId(100L);
        order.setUserId(10L);
        order.setUserEmail("anna@example.com");
        order.setStatus(OrderStatus.NEW);
        order.setOrderItems(new java.util.ArrayList<>());

        orderResponse = OrderResponse.builder()
                .id(100L)
                .userId(10L)
                .userEmail("anna@example.com")
                .status(OrderStatus.NEW)
                .totalPrice(BigDecimal.valueOf(100))
                .build();

        userDto = UserDto.builder()
                .id(10L)
                .name("Anna")
                .surname("Ivanova")
                .email("anna@example.com")
                .active(true)
                .build();
    }

    @Test
    void createOrder_shouldBuildItemsCalculateTotalSaveAndEnrich() {
        OrderItem orderItem = new OrderItem();
        orderItem.setQuantity(2);

        when(orderMapper.toEntity(orderRequest)).thenReturn(order);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(orderItemMapper.toEntity(any(OrderItemDto.class))).thenReturn(orderItem);
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toDto(order)).thenReturn(orderResponse);
        when(userClient.getUserByEmail("anna@example.com")).thenReturn(userDto);

        OrderResponse result = orderService.createOrder(orderRequest);

        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(userDto);
        assertThat(order.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(order.isDeleted()).isFalse();
        verify(orderRepository).save(order);
        verify(userClient).getUserByEmail("anna@example.com");
    }

    @Test
    void createOrder_shouldThrowWhenItemNotFound() {
        when(orderMapper.toEntity(orderRequest)).thenReturn(order);
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(orderRequest))
                .isInstanceOf(ItemNotFoundException.class);

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getOrders_shouldReturnMappedAndEnrichedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(order));

        LocalDateTime createdFrom = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime createdTo = LocalDateTime.of(2026, 1, 2, 0, 0);

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(orderPage);
        when(orderMapper.toDto(order)).thenReturn(orderResponse);
        when(userClient.getUserByEmail("anna@example.com")).thenReturn(userDto);

        Page<OrderResponse> result = orderService.getOrders(10L, createdFrom, createdTo,
                List.of(OrderStatus.NEW), pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getUser()).isEqualTo(userDto);
        verify(orderRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getOrders_shouldWorkWithoutUserIdFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(order));

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(orderPage);
        when(orderMapper.toDto(order)).thenReturn(orderResponse);
        when(userClient.getUserByEmail("anna@example.com")).thenReturn(userDto);

        Page<OrderResponse> result = orderService.getOrders(null, null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(orderRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getOrderById_shouldReturnEnrichedOrder() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderMapper.toDto(order)).thenReturn(orderResponse);
        when(userClient.getUserByEmail("anna@example.com")).thenReturn(userDto);

        OrderResponse result = orderService.getOrderById(100L);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getUser()).isEqualTo(userDto);
        verify(userClient).getUserByEmail("anna@example.com");
    }

    @Test
    void getOrderById_shouldThrowWhenNotFound() {
        when(orderRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(100L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("100");

        verify(userClient, never()).getUserByEmail(anyString());
    }

    @Test
    void updateOrder_shouldRebuildItemsRecalculateTotalAndSave() {
        OrderItem orderItem = new OrderItem();
        orderItem.setQuantity(2);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(orderItemMapper.toEntity(any(OrderItemDto.class))).thenReturn(orderItem);
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toDto(order)).thenReturn(orderResponse);
        when(userClient.getUserByEmail("anna@example.com")).thenReturn(userDto);

        OrderResponse result = orderService.updateOrder(100L, orderRequest);

        assertThat(result).isNotNull();
        assertThat(order.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(100));
        verify(orderMapper).updateEntityFromDto(orderRequest, order);
        verify(orderRepository).save(order);
    }

    @Test
    void updateOrder_shouldThrowWhenNotFound() {
        when(orderRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrder(100L, orderRequest))
                .isInstanceOf(OrderNotFoundException.class);

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void deleteOrder_shouldDeleteWhenFound() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        orderService.deleteOrder(100L);

        verify(orderRepository, times(1)).delete(order);
    }

    @Test
    void deleteOrder_shouldThrowWhenNotFound() {
        when(orderRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.deleteOrder(100L))
                .isInstanceOf(OrderNotFoundException.class);

        verify(orderRepository, never()).delete(any(Order.class));
    }
}