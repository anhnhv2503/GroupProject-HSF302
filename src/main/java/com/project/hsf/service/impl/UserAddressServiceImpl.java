package com.project.hsf.service.impl;

import com.project.hsf.entity.User;
import com.project.hsf.entity.UserAddress;
import com.project.hsf.repository.UserAddressRepository;
import com.project.hsf.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl implements UserAddressService {

    private final UserAddressRepository userAddressRepository;

    @Override
    public List<UserAddress> findByUser(User user) {
        return userAddressRepository.findByUser(user);
    }
}
