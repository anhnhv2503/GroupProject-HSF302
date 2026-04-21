package com.project.hsf.repository;

import com.project.hsf.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    Optional<Coupon> findByCode(String code);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Coupon c SET c.usedCount = c.usedCount + 1 WHERE c.code = :code AND c.usedCount < c.maxUses AND c.active = true AND c.validUntil > CURRENT_TIMESTAMP")
    int claimCoupon(@Param("code") String code);

}
