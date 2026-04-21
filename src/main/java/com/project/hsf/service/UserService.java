package com.project.hsf.service;

import com.project.hsf.dto.RegisterDTO;
import com.project.hsf.entity.User;
import java.util.List;

public interface UserService {

    List<User> getAllUsers(org.springframework.data.domain.Sort sort);

    void toggleUserStatus(Long id, boolean enabled);

    void registerUser(RegisterDTO registerDTO) throws IllegalArgumentException;

    User findByUsername(String username);

    User updateUser(User user);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    void changePassword(User user, String newPassword);
}
