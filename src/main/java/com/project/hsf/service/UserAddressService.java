package com.project.hsf.service;

import com.project.hsf.entity.User;
import com.project.hsf.entity.UserAddress;

import java.util.List;

public interface UserAddressService {

    List<UserAddress> findByUser(User user);
}
