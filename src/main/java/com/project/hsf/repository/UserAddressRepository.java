package com.project.hsf.repository;

import com.project.hsf.entity.User;
import com.project.hsf.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    List<UserAddress> findByUser(User user);
    List<UserAddress> findByUserId(Long userId);
    void deleteByIdAndUserId(Long id, Long userId);
}
