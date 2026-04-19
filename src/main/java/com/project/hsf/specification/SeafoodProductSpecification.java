package com.project.hsf.specification;

import com.project.hsf.entity.SeafoodProduct;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class SeafoodProductSpecification {

    public static Specification<SeafoodProduct> filter(String keyword, Long categoryId, Boolean active, Boolean lowStock) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String likePattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), likePattern),
                        cb.like(root.get("description"), likePattern)
                ));
            }

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }

            if (lowStock != null && lowStock) {
                predicates.add(cb.lessThan(root.get("stockQuantity"), 10));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
