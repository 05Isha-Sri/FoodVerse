package com.foodverse.model;

import com.foodverse.model.enums.CouponType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"expiry", "maxDiscount"})
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    private Double discount;

    @Enumerated(EnumType.STRING)
    private CouponType type;

    private Double minOrderAmount;

    private LocalDateTime expiry;

    private boolean active = true;

    private Double maxDiscount;
}