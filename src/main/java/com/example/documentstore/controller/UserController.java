package com.example.documentstore.controller;

import com.example.documentstore.auth.AuthService;
import com.example.documentstore.auth.dto.UpdateUserProfileRequest;
import com.example.documentstore.auth.dto.UserProfileResponse;
import com.example.documentstore.security.AuthInterceptor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public UserProfileResponse getProfile(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ID) Long userId
    ) {
        return authService.getProfile(userId);
    }

    @PutMapping
    public UserProfileResponse updateProfile(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ID) Long userId,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        return authService.updateProfile(userId, request);
    }
}
