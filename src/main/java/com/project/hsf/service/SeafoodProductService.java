package com.project.hsf.service;

import com.project.hsf.entity.SeafoodProduct;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SeafoodProductService {
    List<SeafoodProduct> findAll();

    SeafoodProduct findById(Long id);

    SeafoodProduct save(SeafoodProduct seafoodProduct, Long categoryId, List<MultipartFile> imageFiles, Integer primaryImageIndex);

    void deleteById(Long id);
}
