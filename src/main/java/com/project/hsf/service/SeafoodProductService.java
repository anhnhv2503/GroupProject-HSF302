package com.project.hsf.service;

import com.project.hsf.entity.SeafoodProduct;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SeafoodProductService {
    List<SeafoodProduct> findAll();

    List<SeafoodProduct> search(String keyword, Long categoryId, Boolean active, Boolean lowStock, String sortBy,
            String sortDir);

    SeafoodProduct findById(Long id);

    SeafoodProduct save(SeafoodProduct seafoodProduct, Long categoryId, List<MultipartFile> imageFiles,
            Integer primaryImageIndex);

    void deleteById(Long id);

    List<SeafoodProduct> getNewestProducts();
}
