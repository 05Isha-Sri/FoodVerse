package com.foodverse.service;

import com.foodverse.dto.coupon.CouponDto;

public interface CouponService {

    CouponDto validateAndApply(Long customerId, String couponCode, Double orderAmount);
}