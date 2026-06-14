package com.innowise.orderservice.mapper;

import com.innowise.orderservice.dto.OrderRequest;
import com.innowise.orderservice.dto.OrderResponse;
import com.innowise.orderservice.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = OrderItemMapper.class)
public interface OrderMapper {

    @Mapping(target = "user", ignore = true)
    OrderResponse toDto(Order order);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "orderItems", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Order toEntity(OrderRequest orderRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "orderItems", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(OrderRequest orderRequestDto, @MappingTarget Order order);
}