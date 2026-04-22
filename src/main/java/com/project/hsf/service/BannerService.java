package com.project.hsf.service;

import com.project.hsf.entity.Banner;
import java.util.List;

public interface BannerService {
    List<Banner> findAll();
    List<Banner> findActiveBanners();
    Banner findById(Long id);
    Banner save(Banner banner);
    void deleteById(Long id);
}
