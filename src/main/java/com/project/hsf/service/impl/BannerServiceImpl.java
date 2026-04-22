package com.project.hsf.service.impl;

import com.project.hsf.entity.Banner;
import com.project.hsf.repository.BannerRepository;
import com.project.hsf.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Banner> findAll() {
        return bannerRepository.findByOrderByDisplayOrderAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Banner> findActiveBanners() {
        return bannerRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Banner findById(Long id) {
        return bannerRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public Banner save(Banner banner) {
        return bannerRepository.save(banner);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        bannerRepository.deleteById(id);
    }
}
