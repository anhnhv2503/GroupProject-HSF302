package com.project.hsf.service.impl;

import com.project.hsf.entity.Coupon;
import com.project.hsf.repository.CouponRepository;
import com.project.hsf.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

	private final CouponRepository couponRepository;

	@Override
	@Transactional(readOnly = true)
	public List<Coupon> findAll() {
		return couponRepository.findAll();
	}

	@Override
	@Transactional(readOnly = true)
	public Coupon findById(Long id) {
		return couponRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Khong tim thay coupon voi id: " + id));
	}

	@Override
	@Transactional
	public Coupon save(Coupon coupon) {
		normalizeAndValidate(coupon, false);

		if (couponRepository.existsByCodeIgnoreCase(coupon.getCode())) {
			throw new IllegalArgumentException("Ma coupon da ton tai.");
		}

		Instant now = Instant.now();
		if (coupon.getValidFrom() == null) {
			coupon.setValidFrom(now);
		}
		if (coupon.getUsedCount() == null) {
			coupon.setUsedCount(0);
		}
		if (coupon.getMaxUses() == null) {
			coupon.setMaxUses(100);
		}
		if (coupon.getActive() == null) {
			coupon.setActive(Boolean.TRUE);
		}

		coupon.setCreatedDate(now);
		coupon.setUpdatedDate(now);
		return couponRepository.save(coupon);
	}

	@Override
	@Transactional
	public Coupon update(Coupon coupon) {
		Coupon existing = findById(coupon.getId());
		normalizeAndValidate(coupon, true);

		if (couponRepository.existsByCodeIgnoreCaseAndIdNot(coupon.getCode(), existing.getId())) {
			throw new IllegalArgumentException("Ma coupon da ton tai.");
		}

		existing.setCode(coupon.getCode());
		existing.setDiscountType(coupon.getDiscountType());
		existing.setDiscountValue(coupon.getDiscountValue());
		existing.setMinOrderValue(coupon.getMinOrderValue());
		existing.setMaxUses(coupon.getMaxUses());
		existing.setUsedCount(coupon.getUsedCount());
		existing.setValidFrom(coupon.getValidFrom());
		existing.setValidUntil(coupon.getValidUntil());
		existing.setActive(coupon.getActive());
		existing.setUpdatedDate(Instant.now());

		return couponRepository.save(existing);
	}

	@Override
	@Transactional
	public void deleteById(Long id) {
		Coupon existing = findById(id);
		couponRepository.delete(existing);
	}

	private void normalizeAndValidate(Coupon coupon, boolean requireId) {
		if (coupon == null) {
			throw new IllegalArgumentException("Du lieu coupon khong hop le.");
		}

		if (requireId && coupon.getId() == null) {
			throw new IllegalArgumentException("Thieu id coupon can cap nhat.");
		}

		String normalizedCode = coupon.getCode() != null ? coupon.getCode().trim().toUpperCase(Locale.ROOT) : null;
		coupon.setCode(normalizedCode);

		String normalizedDiscountType = coupon.getDiscountType() != null
				? coupon.getDiscountType().trim().toUpperCase(Locale.ROOT)
				: null;
		coupon.setDiscountType(normalizedDiscountType);

		if (normalizedCode == null || normalizedCode.isBlank()) {
			throw new IllegalArgumentException("Ma coupon khong duoc de trong.");
		}

		if (!"PERCENT".equals(normalizedDiscountType) && !"FIXED".equals(normalizedDiscountType)) {
			throw new IllegalArgumentException("Loai giam gia chi chap nhan PERCENT hoac FIXED.");
		}

		if (coupon.getDiscountValue() == null || coupon.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Gia tri giam phai lon hon 0.");
		}

		if ("PERCENT".equals(normalizedDiscountType)) {
			if (coupon.getDiscountValue().compareTo(BigDecimal.valueOf(99)) > 0) {
				throw new IllegalArgumentException("Gia tri giam theo phan tram khong duoc vuot qua 99%.");
			}
		} else if ("FIXED".equals(normalizedDiscountType)) {
			BigDecimal halfMinOrder = coupon.getMinOrderValue().multiply(new BigDecimal("0.5"));
			if (coupon.getDiscountValue().compareTo(halfMinOrder) > 0) {
				throw new IllegalArgumentException("So tien giam khong duoc vuot qua 50% gia tri don toi thieu.");
			}
		}

		if (coupon.getMinOrderValue() == null || coupon.getMinOrderValue().compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("Don toi thieu khong duoc am.");
		}

		if (coupon.getMaxUses() == null || coupon.getMaxUses() <= 0) {
			throw new IllegalArgumentException("Tong luot su dung toi da phai lon hon 0.");
		}

		if (coupon.getUsedCount() == null || coupon.getUsedCount() < 0) {
			throw new IllegalArgumentException("So luot da dung khong duoc am.");
		}

		if (coupon.getUsedCount() > coupon.getMaxUses()) {
			throw new IllegalArgumentException("So luot da dung khong duoc lon hon tong luot toi da.");
		}

		if (coupon.getValidFrom() == null || coupon.getValidUntil() == null) {
			throw new IllegalArgumentException("Ngay bat dau va ngay het han la bat buoc.");
		}

		if (!coupon.getValidUntil().isAfter(coupon.getValidFrom())) {
			throw new IllegalArgumentException("Ngay het han phai sau ngay bat dau.");
		}

		if (coupon.getActive() == null) {
			coupon.setActive(Boolean.TRUE);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public java.util.Optional<Coupon> findByCode(String code) {
		return couponRepository.findByCode(code);
	}
}
