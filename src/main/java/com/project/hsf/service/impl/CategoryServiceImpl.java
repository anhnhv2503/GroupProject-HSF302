package com.project.hsf.service.impl;

import com.project.hsf.entity.Category;
import com.project.hsf.repository.CategoryRepository;
import com.project.hsf.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public List<Category> findAll() {
        return this.categoryRepository.findAll();
    }

    @Override
    public Category findById(Long id) {
        return this.categoryRepository.findById(id).orElse(null);
    }

    @Override
    public Category save(Category category) {
        category.setCreatedDate(Instant.now());
        category.setUpdatedDate(Instant.now());
        return this.categoryRepository.save(category);
    }

    @Override
    public void deleteById(Long id) {
        this.categoryRepository.deleteById(id);
    }

    @Override
    public Category update(Category category) {
        Category existingCategory = this.categoryRepository.findById(category.getId()).orElse(null);
        if (existingCategory != null) {
            existingCategory.setName(category.getName());
            existingCategory.setDescription(category.getDescription());
            existingCategory.setUpdatedDate(Instant.now());
            existingCategory.setActive(category.getActive());
            return this.categoryRepository.save(existingCategory);
        }
        return null;
    }

    @Override
    public List<Category> findByActiveTrue() {
        return this.categoryRepository.findByActiveTrue();
    }
}
