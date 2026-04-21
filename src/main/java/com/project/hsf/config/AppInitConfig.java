package com.project.hsf.config;

import com.project.hsf.entity.User;
import com.project.hsf.enums.Role;
import com.project.hsf.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AppInitConfig {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner applicationRunner(){
        return args -> {
            if(!userRepository.existsByUsername("admin")){
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin"));
                admin.setEmail("adminHSF@gmail.com");
                admin.setFullName("Admin HSF");
                admin.setPhone("09096670667");
                admin.setRole(Role.ADMIN.name());
                admin.setEnabled(true);
                userRepository.save(admin);
                log.info("Admin user created: {}", admin.getUsername());
            }
        };
    }
}
