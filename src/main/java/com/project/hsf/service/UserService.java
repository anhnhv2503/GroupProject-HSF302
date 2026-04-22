package com.project.hsf.service;

import com.project.hsf.dto.RegisterDTO;
import com.project.hsf.entity.User;
import java.util.List;

public interface UserService {

    List<User> getAllUsers(org.springframework.data.domain.Sort sort);

    User findById(Long id);

    void toggleUserStatus(Long id, boolean enabled);

    void registerUser(RegisterDTO registerDTO) throws IllegalArgumentException;

    User findByUsername(String username);

    User updateUser(User user);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    void changePassword(User user, String newPassword);

    boolean canUserAddToCart(Long userId);

    boolean canUserReview(Long userId);

    void toggleCartPermission(Long userId, boolean enabled);

    void toggleReviewPermission(Long userId, boolean blocked);
}
