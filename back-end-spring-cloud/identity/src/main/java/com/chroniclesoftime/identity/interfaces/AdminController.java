package com.chroniclesoftime.identity.interfaces;

import com.chroniclesoftime.auth.AuthContext;
import com.chroniclesoftime.auth.PasswordHasher;
import com.chroniclesoftime.identity.application.IdentityApplicationService;
import com.chroniclesoftime.identity.domain.model.Account;
import com.chroniclesoftime.identity.domain.model.AuditLog;
import com.chroniclesoftime.identity.domain.repository.AccountRepository;
import com.chroniclesoftime.identity.domain.repository.AuditLogRepository;
import com.chroniclesoftime.shared.Result;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** 管理后台：用户与角色、内容状态、字典、审计日志、系统统计（所有写操作留痕）。 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final AccountRepository accounts;
    private final AuditLogRepository audits;
    private final IdentityApplicationService service;

    public AdminController(AccountRepository accounts, AuditLogRepository audits,
                           IdentityApplicationService service) {
        this.accounts = accounts; this.audits = audits; this.service = service;
    }

    @GetMapping("/users")
    public Result<?> users(@RequestParam(required = false) String keyword) {
        List<Account> list = accounts.findAll();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            list = list.stream().filter(a -> (a.getUsername() + a.getNickname()).toLowerCase().contains(kw)).toList();
        }
        return Result.ok(list);
    }

    @PostMapping("/users")
    public Result<?> createUser(@RequestBody Map<String, Object> body) {
        String username = String.valueOf(body.get("username"));
        if (!accounts.findBy("username", username).isEmpty()) return Result.fail(409, "账号已存在");
        Account a = new Account();
        a.setUsername(username);
        a.setNickname(String.valueOf(body.getOrDefault("nickname", username)));
        a.setPasswordHash(PasswordHasher.hash(String.valueOf(body.getOrDefault("password", "Cot@2026new"))));
        a.setRole(String.valueOf(body.getOrDefault("role", "USER")));
        a.setStatus("ACTIVE"); a.setTokenVersion(1); a.setLoginFailCount(0); a.setCreatedAt(now());
        Account saved = accounts.save(a);
        audit("CREATE_USER", "account", String.valueOf(saved.getId()), "", saved.getRole());
        return Result.ok(saved);
    }

    @PutMapping("/users/{id}")
    public Result<?> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Account a = accounts.findById(id);
        if (a == null) return Result.fail(404, "账号不存在");
        String before = a.getRole() + "/" + a.getStatus();
        if (body.containsKey("role")) a.setRole(String.valueOf(body.get("role")));
        if (body.containsKey("status")) a.setStatus(String.valueOf(body.get("status")));
        if (body.containsKey("nickname")) a.setNickname(String.valueOf(body.get("nickname")));
        accounts.save(a);
        audit("UPDATE_USER", "account", String.valueOf(id), before, a.getRole() + "/" + a.getStatus());
        return Result.ok(a);
    }

    @PostMapping("/users/{id}/reset-password")
    public Result<?> resetPassword(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Account a = accounts.findById(id);
        if (a == null) return Result.fail(404, "账号不存在");
        a.setPasswordHash(PasswordHasher.hash(String.valueOf(body.getOrDefault("password", "Cot@2026reset"))));
        a.setTokenVersion((a.getTokenVersion() == null ? 1 : a.getTokenVersion()) + 1);
        a.setLoginFailCount(0); a.setLockedUntil(null);
        accounts.save(a);
        audit("RESET_PASSWORD", "account", String.valueOf(id), "", "token revoked");
        return Result.ok(Map.of("reset", true, "reloginRequired", true));
    }

    @GetMapping("/audit")
    public Result<?> auditLog() { return Result.ok(audits.findAll()); }

    @GetMapping("/stats")
    public Result<?> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("accounts", accounts.count());
        m.put("auditLogs", audits.count());
        m.put("identity", service.stats());
        return Result.ok(m);
    }

    private void audit(String action, String targetType, String targetId, String before, String after) {
        AuditLog log = new AuditLog();
        log.setOperatorId(AuthContext.userId());
        log.setOperatorName(AuthContext.username());
        log.setAction(action); log.setTargetType(targetType); log.setTargetId(targetId);
        log.setBeforeValue(before); log.setAfterValue(after); log.setCreatedAt(now());
        audits.save(log);
    }

    private static String now() { return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); }
}
