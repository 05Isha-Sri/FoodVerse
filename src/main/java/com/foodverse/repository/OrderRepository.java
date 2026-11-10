package com.foodverse.repository;

import com.foodverse.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import com.foodverse.model.enums.OrderStatus;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerId(Long customerId);

    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    Page<Order> findByRestaurantOwnerId(Long ownerId, Pageable pageable);

    Page<Order> findByStatusAndDeliveryAgentIsNull(OrderStatus status, Pageable pageable);

    Page<Order> findByStatusInAndDeliveryAgentIsNull(List<OrderStatus> statuses, Pageable pageable);

    Page<Order> findByDeliveryAgentId(Long agentId, Pageable pageable);
}