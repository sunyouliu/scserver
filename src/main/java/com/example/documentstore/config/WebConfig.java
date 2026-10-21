package com.example.documentstore.config;

import com.example.documentstore.security.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties properties;
    private final AuthInterceptor authInterceptor;

    public WebConfig(AppProperties properties, AuthInterceptor authInterceptor) {
        this.properties = properties;
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String urlPrefix = normalizeUrlPrefix(properties.catalog().imageUrlPrefix());
        if (urlPrefix.isBlank()) {
            return;
        }
        String location = Path.of(properties.catalog().imageDirectory()).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler(urlPrefix + "/**").addResourceLocations(location);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/documents/**",
                        "/api/health",
                        "/api/mini-program/virtual-payment/callback"
                );
    }

    private String normalizeUrlPrefix(String urlPrefix) {
        if (urlPrefix == null || urlPrefix.isBlank()) {
            return "";
        }
        String normalized = urlPrefix.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized.replaceAll("/+$", "");
    }
}
