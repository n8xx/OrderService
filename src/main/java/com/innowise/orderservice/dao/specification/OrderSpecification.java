package com.innowise.orderservice.dao.specification;

import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.entity.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class OrderSpecification {
    private static final String CREATED_AT_LINE = "createdAt";

    private OrderSpecification() {
    }
    public static Specification<Order> hasUserId(Long userId){
        return((root,query,criteriaBuilder) -> {
            if(userId==null){
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("userId"),userId);
        });
    }
    public static Specification<Order> hasStatuses(List<OrderStatus> statuses) {
        return (root, query, criteriaBuilder) -> {
            if (statuses == null || statuses.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("status").in(statuses);
        };
    }

    public static Specification<Order> createdBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, criteriaBuilder) -> {
            if (from == null && to == null) {
                return criteriaBuilder.conjunction();
            }
            if (from == null) {
                return criteriaBuilder.lessThanOrEqualTo(root.get(CREATED_AT_LINE), to);
            }
            if (to == null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get(CREATED_AT_LINE), from);
            }
            return criteriaBuilder.between(root.get(CREATED_AT_LINE), from, to);
        };
    }
}