package com.project.hsf.service.impl;

import com.project.hsf.dto.RegisterDTO;
import com.project.hsf.entity.User;
import com.project.hsf.repository.UserRepository;
import com.project.hsf.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers(org.springframework.data.domain.Sort sort) {
        return userRepository.findAll(sort);
    }

    @Override
    @Transactional
    public void toggleUserStatus(Long id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay nguoi dung voi id: " + id));
        user.setEnabled(enabled);
        user.setUpdatedDate(java.time.Instant.now());
        userRepository.save(user);
    }

    @Override
    public void registerUser(RegisterDTO registerDTO) throws IllegalArgumentException {
        if (userRepository.existsByUsername(registerDTO.getUsername())) {
            throw new IllegalArgumentException("Username đã tồn tại");
        }
        if (userRepository.existsByEmail(registerDTO.getEmail())) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        }

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setEmail(registerDTO.getEmail());
        user.setFullName(registerDTO.getFullName());
        user.setPhone(registerDTO.getPhone());
        user.setRole("CUSTOMER");

        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    @Transactional
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional
    public void changePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
