package com.project.hsf.service.impl;

import com.project.hsf.entity.Category;
import com.project.hsf.entity.SeafoodProduct;
import com.project.hsf.repository.CategoryRepository;
import com.project.hsf.repository.SeafoodProductRepository;
import com.project.hsf.service.SeafoodProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.hsf.specification.SeafoodProductSpecification;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;
import com.project.hsf.entity.ProductImage;
import com.project.hsf.util.FileUploadUtil;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;

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
    public List<SeafoodProduct> search(String keyword, Long categoryId, Boolean active, Boolean lowStock, String sortBy,
            String sortDir) {
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        return seafoodProductRepository
                .findAll(SeafoodProductSpecification.filter(keyword, categoryId, active, lowStock), sort);
    }

    @Override
    @Transactional(readOnly = true)
    public SeafoodProduct findById(Long id) {
        return seafoodProductRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay san pham voi id: " + id));
    }

    @Override
    @Transactional
    public SeafoodProduct save(SeafoodProduct seafoodProduct, Long categoryId, List<MultipartFile> imageFiles,
            Integer primaryImageIndex) {
        SeafoodProduct productToSave = seafoodProduct;

        // If it's an update, handle image replacement and property copying to managed
        // entity
        if (seafoodProduct.getId() != null) {
            SeafoodProduct existing = seafoodProductRepository.findById(seafoodProduct.getId()).orElse(null);
            if (existing != null) {
                // If new images provided, clean up the old ones physically and in DB
                if (imageFiles != null && !imageFiles.isEmpty()) {
                    if (existing.getImages() != null) {
                        for (ProductImage oldImg : existing.getImages()) {
                            FileUploadUtil.deleteFile(oldImg.getImageUrl());
                        }
                        existing.getImages().clear();
                    }
                }

                // Sync properties
                existing.setName(seafoodProduct.getName());
                existing.setDescription(seafoodProduct.getDescription());
                existing.setPrice(seafoodProduct.getPrice());
                existing.setStockQuantity(seafoodProduct.getStockQuantity());
                existing.setFreshnessStatus(seafoodProduct.getFreshnessStatus());
                existing.setImportedFrom(seafoodProduct.getImportedFrom());
                existing.setActive(seafoodProduct.getActive());
                existing.setImportedDate(seafoodProduct.getImportedDate());
                existing.setExpiryDate(seafoodProduct.getExpiryDate());

                productToSave = existing;
            }
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay danh muc voi id: " + categoryId));

        productToSave.setCategory(category);

        if (productToSave.getSoldCount() == null) {
            productToSave.setSoldCount(0);
        }
        if (productToSave.getCreatedDate() == null) {
            productToSave.setCreatedDate(Instant.now());
        }
        productToSave.setUpdatedDate(Instant.now());

        // Handle Images
        if (imageFiles != null && !imageFiles.isEmpty()) {
            if (productToSave.getImages() == null) {
                productToSave.setImages(new ArrayList<>());
            }
            int index = 0;
            String uploadDir = "uploads";
            for (MultipartFile file : imageFiles) {
                if (!file.isEmpty()) {
                    try {
                        String fileName = FileUploadUtil.saveFile(uploadDir, file);
                        ProductImage productImage = new ProductImage();
                        productImage.setImageUrl("/" + uploadDir + "/" + fileName);
                        productImage.setProduct(productToSave);
                        productImage.setIsPrimary(primaryImageIndex != null && index == primaryImageIndex);
                        productImage.setCreatedDate(Instant.now());

                        productToSave.getImages().add(productImage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                index++;
            }
        }

        return seafoodProductRepository.save(productToSave);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        SeafoodProduct seafoodProduct = seafoodProductRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay san pham voi id: " + id));

        // Delete associated files from disk
        if (seafoodProduct.getImages() != null) {
            for (ProductImage image : seafoodProduct.getImages()) {
                FileUploadUtil.deleteFile(image.getImageUrl());
            }
        }

        seafoodProductRepository.delete(seafoodProduct);
    }
}
