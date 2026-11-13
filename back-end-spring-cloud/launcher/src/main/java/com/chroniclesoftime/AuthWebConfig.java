package com.chroniclesoftime;

import com.chroniclesoftime.auth.AccountLookup;
import com.chroniclesoftime.auth.AuthInterceptor;
import com.chroniclesoftime.auth.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AuthWebConfig implements WebMvcConfigurer {
    private final AccountLookup lookup;
    private final JdbcTemplate jdbc;

    @Value("${cot.auth.secret:cot-dev-secret-change-me-in-production-2026}")
    private String secret;
    @Value("${cot.auth.ttl-hours:12}")
    private long ttlHours;

    public AuthWebConfig(AccountLookup lookup, JdbcTemplate jdbc) { this.lookup = lookup; this.jdbc = jdbc; }

    @Bean
    public JwtUtil jwtUtil() { return new JwtUtil(secret, ttlHours * 3600_000L); }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor(jwtUtil(), lookup))
                .addPathPatterns("/api/**")
                .excludePathPatterns("/actuator/**", "/files/**", "/error");
        // 注册顺序在鉴权之后：preHandle 能读到 AuthContext，写操作统一留痕。
        registry.addInterceptor(new WorkflowAuditInterceptor(jdbc))
                .addPathPatterns("/api/**")
                .excludePathPatterns("/actuator/**", "/files/**", "/error");
    }
}
