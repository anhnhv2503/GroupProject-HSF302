package com.project.hsf.service.impl;

import com.project.hsf.entity.User;
import com.project.hsf.repository.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);

        if (user != null) {
            if (!Boolean.TRUE.equals(user.getEnabled())) {
                throw new UsernameNotFoundException("Tai khoan cua ban bi khoa. Vui long lien he admin de duoc ho tro.");
            }
            return new org.springframework.security.core.userdetails.User(
                    user.getUsername(),
                    user.getPassword(),
                    user.getRole().equals("ADMIN") ?
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")) :
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );
        }else{
            throw new UsernameNotFoundException("Invalid email or password.");
        }
    }
}
