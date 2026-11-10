package com.foodverse.service;

import com.foodverse.dto.auth.JwtResponse;
import com.foodverse.dto.auth.LoginRequest;
import com.foodverse.dto.auth.RegisterRequest;
import com.foodverse.exception.BadRequestException;
import com.foodverse.model.User;
import com.foodverse.model.enums.Role;
import com.foodverse.repository.UserRepository;
import com.foodverse.security.JwtService;
import com.foodverse.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User(1L, "food_lover", "encodedPass", "food@example.com", Role.CUSTOMER, true);
    }

    @Test
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest("new_user", "Secret123", "new@example.com", Role.CUSTOMER);

        when(userRepository.existsByUsername("new_user")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123")).thenReturn("encodedPassword");

        assertDoesNotThrow(() -> authService.register(request));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterRejectsAdminRole() {
        RegisterRequest request = new RegisterRequest("admin_candidate", "Secret123", "admin@example.com", Role.ADMIN);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.register(request));
        assertTrue(ex.getMessage().contains("Admin accounts require a valid Admin Security Key"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegisterAdminSuccessWithKey() {
        RegisterRequest request = new RegisterRequest("admin_user", "Secret123", "admin@example.com", Role.ADMIN, "FoodVerseAdmin2026");

        when(userRepository.existsByUsername("admin_user")).thenReturn(false);
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123")).thenReturn("encodedPassword");

        assertDoesNotThrow(() -> authService.register(request));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterDuplicateUsername() {
        RegisterRequest request = new RegisterRequest("existing_user", "Secret123", "new@example.com", Role.CUSTOMER);

        when(userRepository.existsByUsername("existing_user")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.register(request));
        assertTrue(ex.getMessage().contains("Username already exists"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegisterDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("unique_user", "Secret123", "taken@example.com", Role.CUSTOMER);

        when(userRepository.existsByUsername("unique_user")).thenReturn(false);
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.register(request));
        assertTrue(ex.getMessage().contains("Email already exists"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLoginSuccess() {
        LoginRequest request = new LoginRequest("food_lover", "Secret123");

        when(userRepository.findByUsername("food_lover")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("Secret123", "encodedPass")).thenReturn(true);
        when(jwtService.generateToken(sampleUser)).thenReturn("sample.jwt.token");

        JwtResponse response = authService.login(request);
        assertNotNull(response);
        assertEquals("sample.jwt.token", response.token());
    }

    @Test
    void testLoginInvalidUsername() {
        LoginRequest request = new LoginRequest("unknown_user", "Secret123");

        when(userRepository.findByUsername("unknown_user")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }

    @Test
    void testLoginInvalidPassword() {
        LoginRequest request = new LoginRequest("food_lover", "WrongPass");

        when(userRepository.findByUsername("food_lover")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("WrongPass", "encodedPass")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }
}
