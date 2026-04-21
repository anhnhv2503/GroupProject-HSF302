package com.project.hsf.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.hsf.entity.User;

public interface UserRepository extends JpaRepository<User, Integer> {
    User findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
