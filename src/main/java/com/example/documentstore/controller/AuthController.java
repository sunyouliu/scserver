package com.example.documentstore.controller;

import com.example.documentstore.auth.AuthService;
import com.example.documentstore.auth.dto.WechatLoginRequest;
import com.example.documentstore.auth.dto.WechatLoginResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/wechat")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public WechatLoginResponse login(@Valid @RequestBody WechatLoginRequest request) {
        return authService.login(request);
    }
}
