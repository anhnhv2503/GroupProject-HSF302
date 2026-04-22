package com.project.hsf.service;

import com.project.hsf.entity.Coupon;

import java.util.List;

public interface CouponService {
	List<Coupon> findAll();

	Coupon findById(Long id);

	Coupon save(Coupon coupon);

	Coupon update(Coupon coupon);

    void deleteById(Long id);
    
    java.util.Optional<Coupon> findByCode(String code);
}
