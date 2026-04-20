package com.project.hsf.service;

import com.project.hsf.dto.RegisterDTO;
import org.springframework.stereotype.Service;

@Service

public interface UserService {

    public void registerUser(RegisterDTO registerDTO) throws IllegalArgumentException;
}
