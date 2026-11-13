package com.chroniclesoftime;

import com.chroniclesoftime.auth.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

/**
 * 横切 · 工作流写操作审计：把批次 1~4 新增的 workflow / share-links / 审核等写请求
 * 统一记入 audit_log，避免在每个端点重复写审计代码。
 * 只拦截非 GET 的指定前缀；auth、files、admin（AdminController 已自行留痕）不在范围内。
 * 审计失败不影响业务响应。
 */
public class WorkflowAuditInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(WorkflowAuditInterceptor.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final List<String> AUDITED_PREFIXES = List.of(
            "/api/v1/timeline/workflow/", "/api/v1/timeline/milestones/", "/api/v1/timeline/connections",
            "/api/v1/learning/workflow/", "/api/v1/learning/practice/",
            "/api/v1/planning/workflow/", "/api/v1/planning/selection/", "/api/v1/planning/volunteer/",
            "/api/v1/planning/share-links", "/api/v1/archive/workflow/",
            "/api/v1/community/workflow/", "/api/v1/community/contents/"
    );
    private static final List<String> EXCLUDED_PREFIXES = List.of("/api/v1/auth/", "/api/v1/files/");

    private final JdbcTemplate jdbc;
    public WorkflowAuditInterceptor(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private boolean shouldAudit(HttpServletRequest request) {
        if (!METHODS.contains(request.getMethod().toUpperCase())) return false;
        String path = request.getRequestURI();
        for (String p : EXCLUDED_PREFIXES) if (path.startsWith(p)) return false;
        for (String p : AUDITED_PREFIXES) if (path.startsWith(p)) return true;
        return false;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (shouldAudit(request)) {
            // AuthInterceptor 先执行，此处能取到当前账号；afterCompletion 时上下文可能已清空。
            request.setAttribute("cot_audit_uid", AuthContext.userId());
            request.setAttribute("cot_audit_uname", AuthContext.username());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        if (!shouldAudit(request)) return;
        try {
            String path = request.getRequestURI();
            String action = "WORKFLOW_" + request.getMethod().toUpperCase();
            String result = "HTTP " + response.getStatus() + (ex == null ? "" : " | " + ex.getClass().getSimpleName());
            jdbc.update("INSERT INTO audit_log (operator_id, operator_name, action, target_type, target_id, "
                            + "before_value, after_value, created_at) VALUES (?,?,?,?,?,?,?,?)",
                    request.getAttribute("cot_audit_uid"), request.getAttribute("cot_audit_uname"),
                    action, "ENDPOINT", path.length() > 255 ? path.substring(0, 255) : path,
                    null, result, LocalDateTime.now().format(TS));
        } catch (Exception e) { log.warn("工作流审计写入失败（不影响业务响应）: {}", e.getMessage()); }
    }
}
