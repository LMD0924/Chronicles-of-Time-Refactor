package com.chroniclesoftime.auth;

import com.chroniclesoftime.shared.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.Set;

/** 三级路径鉴权：PUBLIC / AUTHENTICATED / ADMIN；每次请求回查账号状态与 token_version。 */
public class AuthInterceptor implements HandlerInterceptor {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/verify",
            "/api/v1/system/overview", "/api/v1/system/modules", "/api/v1/system/health",
            "/api/v1/dashboard"
    );
    private static final Set<String> PUBLIC_GET_PREFIXES = Set.of(
            "/api/v1/community/contents", "/api/v1/planning/subject-options",
            "/api/v1/planning/major-requirements", "/api/v1/planning/grading-scales",
            "/api/v1/planning/subject-combinations", "/api/v1/timeline/share", "/api/v1/community/knowledge-graph",
            "/api/v1/community/tag-cloud", "/api/v1/community/category-stats", "/api/v1/community/related"
    );

    private final JwtUtil jwt;
    private final AccountLookup lookup;

    public AuthInterceptor(JwtUtil jwt, AccountLookup lookup) { this.jwt = jwt; this.lookup = lookup; }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) return true;
        if (PUBLIC_PATHS.contains(path)) return true;
        boolean readOnlyPublic = "GET".equalsIgnoreCase(request.getMethod())
                && PUBLIC_GET_PREFIXES.stream().anyMatch(path::startsWith);
        if (readOnlyPublic) return true;

        String auth = request.getHeader("Authorization");
        String token = auth != null && auth.startsWith("Bearer ") ? auth.substring(7) : null;
        Map<String, Object> payload = jwt.verify(token);
        if (payload == null) { reject(response, 401, "未登录或登录已失效"); return false; }

        Object uid = payload.get("uid");
        AccountLookup.AccountSnapshot account = uid instanceof Number
                ? lookup.findById(((Number) uid).longValue()) : null;
        if (account == null || !"ACTIVE".equals(account.getStatus())) {
            reject(response, 401, "账号不可用或登录已失效"); return false;
        }
        Object ver = payload.get("ver");
        if (!(ver instanceof Number) || ((Number) ver).intValue() != account.getTokenVersion()) {
            reject(response, 401, "登录已失效，请重新登录"); return false;
        }
        if (path.startsWith("/api/v1/admin") && !"ADMIN".equals(account.getRole())) {
            reject(response, 403, "需要管理员权限"); return false;
        }
        AuthContext.set(new AuthUser(account.getId(), account.getUsername(), account.getRole(), account.getTokenVersion()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private void reject(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(code == 401 ? 401 : 403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSON.writeValueAsString(Result.fail(code, message)));
    }
}
