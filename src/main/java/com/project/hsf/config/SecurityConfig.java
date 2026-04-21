package com.project.hsf.config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final List<String> publicUrl =
            List.of(
                    "/",
                    "/home",
                    "/login/**",
                    "/register/**",
                    "/products/**",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/webjars/**",
                    "/register-user",
                    "/cart",
                    "/wishlist/**",
                    "/chatbot/**"
            );
//    private final List<String> privateUrl = new ArrayList<>();

    @Autowired
    private UserDetailsService userDetailsService;

    @Bean
    public static PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers(publicUrl.toArray(String[]::new)).permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin((form) -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler((request, response, authentication) -> {
                            boolean isAdmin = authentication.getAuthorities().stream()
                                    .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

                            if (isAdmin) {
                                response.sendRedirect("/admin/dashboard");
                                return;
                            }

                            response.sendRedirect("/home");
                        })
                        .failureUrl("/login?error=true")
                        .permitAll())
                .logout((logout) -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                        .clearAuthentication(true).permitAll());

        return http.build();
    }

    @Autowired
    public void configGlobal(AuthenticationManagerBuilder builder) throws Exception {
        builder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }
}