package com.foodverse.service.impl;

import com.foodverse.dto.auth.JwtResponse;
import com.foodverse.dto.auth.LoginRequest;
import com.foodverse.dto.auth.RegisterRequest;
import com.foodverse.model.User;
import com.foodverse.model.enums.Role;
import com.foodverse.exception.BadRequestException;
import com.foodverse.repository.UserRepository;
import com.foodverse.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements com.foodverse.service.AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public void register(RegisterRequest req) {
        String username = req.username().trim();
        String email = req.email().trim().toLowerCase();
        if (username.isBlank()) throw new BadRequestException("Username cannot be blank");
        if (req.role() == Role.ADMIN) {
            if (req.adminKey() == null || !req.adminKey().trim().equals("FoodVerseAdmin2026")) {
                throw new BadRequestException("Admin accounts require a valid Admin Security Key");
            }
        }
        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already exists");
        }
        
        User user = new User(
                null,
                username,
                passwordEncoder.encode(req.password()),
                email,
                req.role(),
                true
        );
        userRepository.save(user);
    }

    @Override
    public JwtResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.username().trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        String token = jwtService.generateToken(user);
        return new JwtResponse(token);
    }
}
