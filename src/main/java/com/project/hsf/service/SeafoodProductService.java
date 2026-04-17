package com.project.hsf.service;

import com.project.hsf.entity.SeafoodProduct;

import java.util.List;

public interface SeafoodProductService {
    List<SeafoodProduct> findAll();

    SeafoodProduct findById(Long id);

    SeafoodProduct save(SeafoodProduct seafoodProduct, Long categoryId);

    void deleteById(Long id);
}
