package com.project.hsf.service.impl;

import com.project.hsf.entity.User;
import com.project.hsf.enums.Role;
import com.project.hsf.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends OidcUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;



    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();
        User googleUser = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setUsername(email.substring(0, email.indexOf("@")));
            newUser.setEmail(email);
            newUser.setFullName(name);
            newUser.setPassword(passwordEncoder.encode(LocalDateTime.now().toString()));
            newUser.setRole(Role.CUSTOMER.name());
            return userRepository.save(newUser);
        });

        return new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority(googleUser.getRole())),
                oidcUser.getIdToken(),
                oidcUser.getUserInfo()
        );
    }
}
