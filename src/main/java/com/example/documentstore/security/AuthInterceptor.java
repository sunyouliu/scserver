package com.example.documentstore.security;

import com.example.documentstore.common.ApiException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String CURRENT_USER_ID = "currentUserId";

    private final JwtService jwtService;

    public AuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "请先完成微信登录");
        }

        try {
            Long userId = jwtService.parseUserId(authorization.substring(7));
            request.setAttribute(CURRENT_USER_ID, userId);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "登录状态已失效，请重新登录");
        }
    }
}
