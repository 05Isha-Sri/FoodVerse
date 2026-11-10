package com.foodverse.service.impl;

import com.foodverse.dto.coupon.CouponDto;
import com.foodverse.exception.BadRequestException;
import com.foodverse.model.Coupon;
import com.foodverse.model.enums.CouponType;
import com.foodverse.repository.CouponRepository;
import com.foodverse.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;

    @Override
    public CouponDto validateAndApply(Long customerId, String couponCode, Double orderAmount) {
        if (couponCode == null || couponCode.isBlank()) {
            throw new BadRequestException("Coupon code cannot be empty");
        }
        if (orderAmount == null || orderAmount <= 0) {
            throw new BadRequestException("Order amount must be greater than zero to apply coupon");
        }

        Coupon coupon = couponRepository.findByCode(couponCode)
                .orElseThrow(() -> new BadRequestException("Invalid coupon code: " + couponCode));

        if (!coupon.isActive()) {
            throw new BadRequestException("Coupon is inactive: " + couponCode);
        }

        if (coupon.getExpiry() != null && coupon.getExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Coupon has expired: " + couponCode);
        }

        if (coupon.getMinOrderAmount() != null && orderAmount < coupon.getMinOrderAmount()) {
            throw new BadRequestException("Order amount of ₹" + orderAmount +
                    " does not meet the minimum requirement of ₹" + coupon.getMinOrderAmount());
        }

        double discountAmount = 0.0;
        if (coupon.getType() == CouponType.PERCENTAGE) {
            discountAmount = orderAmount * (coupon.getDiscount() / 100.0);
            if (coupon.getMaxDiscount() != null && discountAmount > coupon.getMaxDiscount()) {
                discountAmount = coupon.getMaxDiscount();
            }
        } else if (coupon.getType() == CouponType.FIXED) {
            discountAmount = Math.min(coupon.getDiscount(), orderAmount);
        }

        discountAmount = Math.round(discountAmount * 100.0) / 100.0;

        return new CouponDto(
                coupon.getId(),
                coupon.getCode(),
                discountAmount,
                coupon.getType(),
                coupon.getMinOrderAmount(),
                coupon.getExpiry(),
                coupon.isActive(),
                coupon.getMaxDiscount()
        );
    }
}
