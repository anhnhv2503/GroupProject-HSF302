package com.project.hsf.service;

import com.project.hsf.entity.UserAddress;
import com.project.hsf.entity.User;

import java.util.List;

public interface AddressService {
    List<UserAddress> getAddressesByUser(User user);

    UserAddress getAddressById(Long id, User user);

    UserAddress createAddress(User user, String phone, String addressLine, String ward, String city, Boolean isDefault);

    UserAddress updateAddress(Long id, User user, String phone, String addressLine, String ward, String city, Boolean isDefault);

    void deleteAddress(Long id, User user);

    void setDefaultAddress(Long id, User user);
}
