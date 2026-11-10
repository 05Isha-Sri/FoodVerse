package com.foodverse.service;

import com.foodverse.dto.auth.JwtResponse;
import com.foodverse.dto.auth.LoginRequest;
import com.foodverse.dto.auth.RegisterRequest;

public interface AuthService {

    void register(RegisterRequest req);

    JwtResponse login(LoginRequest req);
}