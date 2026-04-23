package com.project.hsf.controller.admin;

import com.project.hsf.entity.Coupon;
import com.project.hsf.service.CouponService;
import com.project.hsf.service.impl.CouponServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Controller
@RequestMapping("/admin/coupon")
@RequiredArgsConstructor
public class CouponController {

	private final CouponService couponService;

	@GetMapping
	public String list(Model model) {
		List<Coupon> coupons = couponService.findAll();
		model.addAttribute("coupons", coupons);
		model.addAttribute("page", "coupons");
		return "admin/coupon";
	}

	@PostMapping("/save")
	public String save(
			@RequestParam String code,
			@RequestParam String discountType,
			@RequestParam BigDecimal discountValue,
			@RequestParam BigDecimal minOrderValue,
			@RequestParam Integer maxUses,
			@RequestParam(defaultValue = "0") Integer usedCount,
			@RequestParam String validFromDate,
			@RequestParam String validUntilDate,
			@RequestParam(defaultValue = "false") Boolean active,
			RedirectAttributes redirectAttributes) {
		try {
			Coupon coupon = new Coupon();
			coupon.setCode(code);
			coupon.setDiscountType(discountType);
			coupon.setDiscountValue(discountValue);
			coupon.setMinOrderValue(minOrderValue);
			coupon.setMaxUses(maxUses);
			coupon.setUsedCount(usedCount);
			coupon.setValidFrom(parseDateToInstant(validFromDate));
			coupon.setValidUntil(parseDateToInstant(validUntilDate));
			coupon.setActive(active);
			// System.out.println("Creating coupon: " + coupon);
			couponService.save(coupon);
			redirectAttributes.addFlashAttribute("successMessage", "Tạo coupon thành công!");
		} catch (Exception ex) {
			redirectAttributes.addFlashAttribute("errorMessage", "Tạo coupon thất bại: " + ex.getMessage());
		}
		return "redirect:/admin/coupon";
	}

	@PostMapping("/update")
	public String update(
			@RequestParam Long id,
			@RequestParam String code,
			@RequestParam String discountType,
			@RequestParam BigDecimal discountValue,
			@RequestParam BigDecimal minOrderValue,
			@RequestParam Integer maxUses,
			@RequestParam(defaultValue = "0") Integer usedCount,
			@RequestParam String validFromDate,
			@RequestParam String validUntilDate,
			@RequestParam(defaultValue = "false") Boolean active,
			RedirectAttributes redirectAttributes) {
		try {
			Coupon coupon = new Coupon();
			coupon.setId(id);
			coupon.setCode(code);
			coupon.setDiscountType(discountType);
			coupon.setDiscountValue(discountValue);
			coupon.setMinOrderValue(minOrderValue);
			coupon.setMaxUses(maxUses);
			coupon.setUsedCount(usedCount);
			coupon.setValidFrom(parseDateToInstant(validFromDate));
			coupon.setValidUntil(parseDateToInstant(validUntilDate));
			coupon.setActive(active);

			couponService.update(coupon);
			redirectAttributes.addFlashAttribute("successMessage", "Cập nhật coupon thành công!");
		} catch (Exception ex) {
			redirectAttributes.addFlashAttribute("errorMessage", "Cập nhật coupon thất bại: " + ex.getMessage());
		}
		return "redirect:/admin/coupon";
	}

	@PostMapping("/delete/{id}")
	public String delete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
		try {
			couponService.deleteById(id);
			redirectAttributes.addFlashAttribute("successMessage", "Xóa coupon thành công!");
		} catch (Exception ex) {
			redirectAttributes.addFlashAttribute("errorMessage", "Xóa coupon thất bại: " + ex.getMessage());
		}
		return "redirect:/admin/coupon";
	}

	private Instant parseDateToInstant(String date) {
		return LocalDate.parse(date).atStartOfDay().toInstant(ZoneOffset.UTC);
	}
}
