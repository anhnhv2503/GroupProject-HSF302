package com.project.hsf.service;

import com.project.hsf.dto.RegisterDTO;
import com.project.hsf.entity.User;

public interface UserService {

    void registerUser(RegisterDTO registerDTO) throws IllegalArgumentException;

    User findByUsername(String username);

    User updateUser(User user);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    void changePassword(User user, String newPassword);
}
