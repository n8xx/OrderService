package com.innowise.orderservice.mapper;

import com.innowise.orderservice.dto.OrderItemDto;
import com.innowise.orderservice.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderItemMapper {

    @Mapping(source = "item.id", target = "itemId")
    OrderItemDto toDto(OrderItem orderItem);

    @Mapping(target = "order", ignore = true)
    @Mapping(target = "item", ignore = true)
    OrderItem toEntity(OrderItemDto orderItemDto);
}