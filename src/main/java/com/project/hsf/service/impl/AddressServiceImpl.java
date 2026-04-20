package com.project.hsf.service.impl;

import com.project.hsf.entity.User;
import com.project.hsf.entity.UserAddress;
import com.project.hsf.repository.UserAddressRepository;
import com.project.hsf.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final UserAddressRepository userAddressRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UserAddress> getAddressesByUser(User user) {
        return userAddressRepository.findByUserId(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public UserAddress getAddressById(Long id, User user) {
        return userAddressRepository.findById(id)
                .filter(address -> address.getUser().getId().equals(user.getId()))
                .orElse(null);
    }

    @Override
    @Transactional
    public UserAddress createAddress(User user, String phone, String addressLine, String ward, String city, Boolean isDefault) {
        if (Boolean.TRUE.equals(isDefault)) {
            clearDefaultAddresses(user);
        }

        UserAddress address = UserAddress.builder()
                .user(user)
                .phone(phone)
                .addressLine(addressLine)
                .ward(ward)
                .city(city)
                .isDefault(isDefault != null ? isDefault : false)
                .build();

        return userAddressRepository.save(address);
    }

    @Override
    @Transactional
    public UserAddress updateAddress(Long id, User user, String phone, String addressLine, String ward, String city, Boolean isDefault) {
        UserAddress address = getAddressById(id, user);
        if (address == null) {
            return null;
        }

        if (Boolean.TRUE.equals(isDefault) && !Boolean.TRUE.equals(address.getIsDefault())) {
            clearDefaultAddresses(user);
        }

        address.setPhone(phone);
        address.setAddressLine(addressLine);
        address.setWard(ward);
        address.setCity(city);
        address.setIsDefault(isDefault != null ? isDefault : false);

        return userAddressRepository.save(address);
    }

    @Override
    @Transactional
    public void deleteAddress(Long id, User user) {
        UserAddress address = getAddressById(id, user);
        if (address != null) {
            userAddressRepository.delete(address);
        }
    }

    @Override
    @Transactional
    public void setDefaultAddress(Long id, User user) {
        clearDefaultAddresses(user);
        UserAddress address = getAddressById(id, user);
        if (address != null) {
            address.setIsDefault(true);
            userAddressRepository.save(address);
        }
    }

    private void clearDefaultAddresses(User user) {
        List<UserAddress> addresses = userAddressRepository.findByUserId(user.getId());
        addresses.forEach(address -> {
            if (Boolean.TRUE.equals(address.getIsDefault())) {
                address.setIsDefault(false);
                userAddressRepository.save(address);
            }
        });
    }
}
