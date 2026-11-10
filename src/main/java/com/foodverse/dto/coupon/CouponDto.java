package com.foodverse.dto.coupon;

import com.foodverse.model.enums.CouponType;
import java.time.LocalDateTime;

public record CouponDto(

        Long id,

        String code,

        Double discount,

        CouponType type,

        Double minOrderAmount,

        LocalDateTime expiry,

        boolean active,

        Double maxDiscount) {}