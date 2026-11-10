package com.foodverse.model;

import com.foodverse.model.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"customer", "restaurant", "items"})   
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    private Restaurant restaurant;

    @OneToMany(mappedBy = "order",
               cascade = CascadeType.ALL,
               orphanRemoval = true)
    private Set<OrderItem> items = new HashSet<>();

    private Double subtotal;

    private Double tax;

    private Double deliveryFee;

    private Double total;

    @Column(length = 500, nullable = false)
    private String deliveryAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    private User deliveryAgent;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;

    private LocalDateTime createdAt = LocalDateTime.now();
}