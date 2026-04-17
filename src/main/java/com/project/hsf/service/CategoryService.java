package com.project.hsf.service;

import com.project.hsf.entity.Category;
import com.project.hsf.entity.SeafoodProduct;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CategoryService {

    List<Category> findAll();

    Category findById(Long id);

    Category save(Category category);

    void deleteById(Long id);
}
