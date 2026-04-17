package com.project.hsf.service.impl;

import com.project.hsf.entity.Category;
import com.project.hsf.entity.SeafoodProduct;
import com.project.hsf.repository.CategoryRepository;
import com.project.hsf.repository.SeafoodProductRepository;
import com.project.hsf.service.SeafoodProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeafoodProductServiceImpl implements SeafoodProductService {

    private final SeafoodProductRepository seafoodProductRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SeafoodProduct> findAll() {
        return seafoodProductRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public SeafoodProduct findById(Long id) {
        return seafoodProductRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay san pham voi id: " + id));
    }

    @Override
    @Transactional
    public SeafoodProduct save(SeafoodProduct seafoodProduct, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay danh muc voi id: " + categoryId));

        seafoodProduct.setCategory(category);

        if (seafoodProduct.getSoldCount() == null) {
            seafoodProduct.setSoldCount(0);
        }
        if (seafoodProduct.getCreatedDate() == null) {
            seafoodProduct.setCreatedDate(Instant.now());
        }
        seafoodProduct.setUpdatedDate(Instant.now());

        return seafoodProductRepository.save(seafoodProduct);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (!seafoodProductRepository.existsById(id)) {
            throw new IllegalArgumentException("Khong tim thay san pham voi id: " + id);
        }
        seafoodProductRepository.deleteById(id);
    }
}
