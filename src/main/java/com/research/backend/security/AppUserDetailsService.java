package com.research.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Development UserDetailsService — loads users from application properties.
 *
 * PRODUCTION NOTE: Replace this with a JPA-backed UserDetailsService
 * that queries a `users` table. This in-memory implementation is provided
 * to make the project runnable without a user registration flow.
 */
@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;

    // In production: replace with UserRepository.findByUsername(...)
    private final Map<String, UserDetails> userStore = new ConcurrentHashMap<>();

    @Value("${app.dev.admin-password:admin123}")
    public void initDevUsers(String adminPassword) {
        userStore.put("admin", User.builder()
                .username("admin")
                .password(passwordEncoder.encode(adminPassword))
                .roles("ADMIN", "USER")
                .build());

        userStore.put("researcher", User.builder()
                .username("researcher")
                .password(passwordEncoder.encode("research123"))
                .roles("USER")
                .build());
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails user = userStore.get(username.toLowerCase());
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        return user;
    }
}
