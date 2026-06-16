package com.innowise.orderservice.mapper;

import com.innowise.orderservice.dto.ItemDto;
import com.innowise.orderservice.entity.Item;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ItemMapper {

    ItemDto toDto(Item item);

    Item toEntity(ItemDto itemDto);
}